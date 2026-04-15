# DCS HRM Report Context Pack (for Claude)

Use this file as source context when asking Claude to write the final assignment report.

---

## 1. Project summary

- Project: Distributed HRM Leave Management System
- Tech stack: Java RMI, Derby (network mode), Swing GUI client
- Architecture: Client (`CrestGuiApp`) → RMI interface (`Authorization`) → Server (`ServiceImpl`) → Derby repositories
- Security: SSL-enabled RMI communication (`SslRMIClientSocketFactory`, `SslRMIServerSocketFactory`)
- Data storage: centralized Derby database (`crestDB`)

---

## 2. Assignment requirement mapping (code-level)

### Met in implementation

1. HR employee registration  
2. Employee self-service:
   - update personal/family details
   - check leave balance
   - apply leave
   - view application status
   - view history
3. Centralized storage
4. Yearly leave report generation
5. Secure communication (SSL RMI)
6. Client-server architecture using Java RMI

### Partially met / report-side work still needed

1. Full documentation sections (abstract/introduction/research/conclusion/references/workload matrix)
2. Use case diagram artifact
3. Gantt chart artifact
4. Protocol write-up section
5. Fault-tolerance discussion with explicit evaluation
6. Cloud/virtualization recommendations
7. Distributed vs centralized justification write-up
8. Testing manual write-up

---

## 3. Current runtime credentials (after reseed)

### HR
- User ID: `H-000001`
- Password: `hr123`

### Seeded staff users (UUID-derived compact IDs)
- `5ffc65` / `Seed#123A` (Alice Wong)
- `81ba63` / `Seed#123B` (Brian Tan)
- `0df6ff` / `Seed#123C` (Chloe Lim)

---

## 4. Run and verify commands

```bash
# compile
mkdir -p build/classes
javac -cp "lib/*" -d build/classes $(find src -name "*.java")

# start derby
java -cp "lib/*" org.apache.derby.drda.NetworkServerControl start -h 127.0.0.1 -p 1527

# start rmi server
java -cp "build/classes:lib/*" Server.ServerSocket

# start gui
java -cp "build/classes:lib/*" Client.ClientMain
```

---

## 5. Core design notes for report

### Security and auth
- Session-based authorization (`UserSession` token + role checks in service layer)
- Password hashing with PBKDF2 + legacy compatibility migration
- SSL truststore/keystore configuration via runtime properties

### Fault handling implemented
- DB connection retry/backoff (`DatabaseSocket`)
- RMI connection retry (`GuiServiceClient.connect`)
- RMI call-level retry + reconnect (`GuiServiceClient.invokeWithRetry`)
- Transactional leave decision (approve/reject with rollback on failure)
- Conditional decision update to avoid race conflicts (`setDecisionIfPending`)
- Structured audit log writer with async single-threaded logging and rotation
- Server startup registry recovery (reuse existing registry if already running)

### Data consistency behavior
- HR register enforces unique IC/passport
- Leave overlap checks for pending/approved leave windows
- Annual leave balance checked before approve and at apply time
- Delete employee uses transaction and removes related leave/detail records first

---

## 6. Key code snippets (paste-ready)

### 6.1 RMI contract (excerpt)

```java
public interface Authorization extends Remote {
    UserSession login(String userId, String password) throws RemoteException;
    void logout(UserSession session) throws RemoteException;

    Employee registerEmployee(UserSession session, String firstName, String lastName, String icPassport, String initPass)
            throws RemoteException;
    Employee updateDetails(UserSession session, Employee updated) throws RemoteException;
    int leaveBalance(UserSession session) throws RemoteException;
    Employee getMyProfile(UserSession session) throws RemoteException;

    int applyLeave(UserSession session, String leaveType, String startDate, String endDate, String reason)
            throws RemoteException;
    String viewMyLeaveApplications(UserSession session) throws RemoteException;
    String viewMyLeaveHistory(UserSession session) throws RemoteException;
    String viewPendingLeaveApplications(UserSession session) throws RemoteException;

    List<Employee> listEmployees(UserSession session) throws RemoteException;
    Employee getEmployeeById(UserSession session, String employeeId) throws RemoteException;
    Employee updateEmployeeByHr(UserSession session, Employee updated) throws RemoteException;
    void deleteEmployee(UserSession session, String employeeId) throws RemoteException;

    void decideLeave(UserSession session, int leaveId, boolean approve) throws RemoteException;
    String generateYearlyLeaveReport(UserSession session, String employeeId, int year) throws RemoteException;
}
```

### 6.2 UUID-derived compact ID generation (6 chars)

```java
public class IdGenerator {
    public static String nextEmployeeCounter() { return uuidToken(); }
    public static String nextHumanResourceCounter() { return uuidToken(); }

    private static String uuidToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6).toLowerCase();
    }
}
```

### 6.3 DB retry/backoff

```java
public static Connection getConnection() throws SQLException {
    SQLException last = null;
    for (int attempt = 1; attempt <= CONNECT_RETRIES; attempt++) {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            last = e;
            if (attempt == CONNECT_RETRIES) break;
            sleepQuietly(CONNECT_RETRY_DELAY_MS * attempt);
        }
    }
    throw last;
}
```

### 6.4 Client-side RMI retry/reconnect

```java
private <T> T invokeWithRetry(RemoteCall<T> call, boolean recoverConnection) throws Exception {
    Exception last = null;
    for (int attempt = 1; attempt <= CALL_RETRIES; attempt++) {
        try {
            ensureConnected();
            return call.run();
        } catch (RemoteException e) {
            last = e;
            if (!recoverConnection || attempt == CALL_RETRIES) break;
            service = null;
            Thread.sleep(CALL_RETRY_DELAY_MS * attempt);
            connect();
        }
    }
    throw last;
}
```

### 6.5 Transaction-safe leave decision

```java
try (Connection c = DatabaseSocket.getConnection()) {
    c.setAutoCommit(false);
    try {
        var row = leaveRepo.findById(c, leaveId);
        if (row == null) throw new IllegalArgumentException("Leave ID not found");
        if (!"PENDING".equalsIgnoreCase(row.status)) throw new IllegalStateException("Leave is not pending");

        if (approve && "ANNUAL".equalsIgnoreCase(row.leaveType)) {
            int year = row.startDate.toLocalDate().getYear();
            leaveBalanceRepo.getYearBalanceOrCreate(c, row.employeeId, year, 15);
            boolean deducted = leaveBalanceRepo.decreaseIfEnough(c, row.employeeId, year, row.daysRequested);
            if (!deducted) throw new IllegalStateException("Cannot approve: insufficient balance.");
        }

        boolean decided = leaveRepo.setDecisionIfPending(c, leaveId, approve ? "APPROVED" : "REJECTED", s.getUserId());
        if (!decided) throw new IllegalStateException("Leave is not pending");
        c.commit();
    } catch (Exception e) {
        c.rollback();
        throw e;
    } finally {
        c.setAutoCommit(true);
    }
}
```

### 6.6 Registry recovery on server startup

```java
try {
    reg = LocateRegistry.createRegistry(PORT, csf, ssf);
} catch (ExportException alreadyRunning) {
    reg = LocateRegistry.getRegistry(SERVER_IP, PORT, csf);
    reg.list();
}
```

---

## 7. Suggested Claude prompt for full report writing

```text
Use REPORT_CONTEXT.md as the primary source of truth.
Write a complete DCS assignment report in a formal academic style, mapped to the assignment structure:
- Abstract
- Introduction
- Problem Background and Requirements
- Research and Evaluation
- Role of multithreading, serialization, and OOP
- Use case model explanation
- Implementation details
- Protocols used
- Testing manual
- Fault tolerance and quality evaluation (usability, maintainability, heterogeneity)
- Distributed vs centralized justification
- Cloud/virtualization recommendations
- Conclusion
- References (APA placeholders where needed)
- Appendix and workload matrix template

Include evidence-driven explanations using the provided code snippets and explicitly map each requirement to implemented modules.
```

---

## 8. Notes for final report completion

- Add your own use case diagram image and gantt chart image/table to the report.
- Add team/member workload matrix from your actual contribution data.
- Replace placeholder references with real scholarly/official sources.
- Keep submission formatting aligned with assignment instructions (font, spacing, sections).
