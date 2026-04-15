package Server;

import Common.Authorization;
import Common.Employee;
import Common.UserRole;
import Common.UserSession;
import Common.ValidationPatterns;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.time.Year;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;


public class ServiceImpl extends UnicastRemoteObject implements Authorization {
    private static final boolean SSL = Boolean.parseBoolean(
            System.getProperty("crest.ssl", "true")
    );
    private static final boolean BOOTSTRAP_DEMO_STAFF = Boolean.parseBoolean(
            System.getProperty("crest.bootstrap.demo.staff", "false")
    );
    private static final Pattern PERSON_NAME_PATTERN = ValidationPatterns.PERSON_NAME;
    private static final Pattern IC_PASSPORT_PATTERN = ValidationPatterns.IC_PASSPORT;
    private static final Pattern EMPLOYEE_ID_PATTERN = ValidationPatterns.EMPLOYEE_ID;
    private static final Pattern HR_ID_PATTERN = ValidationPatterns.HR_ID;
    private static final Pattern LEGACY_USER_ID_PATTERN = ValidationPatterns.LEGACY_USER_ID;
    private static final Pattern LEGACY_EMPLOYEE_ID_PATTERN = ValidationPatterns.LEGACY_EMPLOYEE_ID;
    private static final Pattern PHONE_PATTERN = ValidationPatterns.PHONE;
    private static final Pattern RELATIONSHIP_PATTERN = ValidationPatterns.RELATIONSHIP;
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$"
    );


    private final Session sessionManager = new Session();
    private final ActivityLog audit = new ActivityLog("audit.log");

    private final UsersRepository users = new UsersRepository();
    private final EmployeesRepository employees = new EmployeesRepository();
    private final EmployeeDetailsRepository details = new EmployeeDetailsRepository();
    private final LeaveBalanceRepository leaveBalanceRepo = new LeaveBalanceRepository();
    private final LeaveApplicationsRepository leaveRepo = new LeaveApplicationsRepository();
    
    protected ServiceImpl() throws RemoteException {
        super(0, clientSocketFactory(), serverSocketFactory());

        try {
            String hrId = "H-000001";
            if (!users.exists(hrId)) {
                String salt = PasswordHash.newSalt();
                String hash = PasswordHash.hash(salt, "hr123");
                users.insert(hrId, UserRole.HR, salt, hash);
                audit.log("Default HR created: " + hrId);
                System.out.println("Default HR created: " + hrId + " pw=hr123");
            } else {
                System.out.println("Default HR exists: H-000001");
            }
        } catch (Exception e) {
            System.out.println("HR bootstrap error: " + e.getMessage());
        }

        if (BOOTSTRAP_DEMO_STAFF) {
            try {
                ensureDemoStaffAccount();
            } catch (Exception e) {
                System.out.println("Demo staff bootstrap error: " + e.getMessage());
            }
        }
    }

    private static RMIClientSocketFactory clientSocketFactory() {
        return SSL ? new SslRMIClientSocketFactory() : null;
    }

    private static RMIServerSocketFactory serverSocketFactory() {
        return SSL ? new SslRMIServerSocketFactory() : null;
    }

    @Override
    public String ping() throws RemoteException {
        audit.log("ping()");
        return "Server is valid!";
    }

    @Override
    public UserSession login(String userId, String password) throws RemoteException {
        try {
            String normalizedUserId = resolveLoginUserId(userId);
            String normalizedPassword = requireNonBlank(password, "Password");

            UsersRepository.UserRow u = users.find(normalizedUserId);
            if (u == null) {
                audit.log("login fail no user: " + normalizedUserId);
                throw new SecurityException("Invalid credentials");
            }

            if (!PasswordHash.verification(u.salt, u.hash, normalizedPassword)) {
                audit.log("login fail bad pw: " + normalizedUserId);
                throw new SecurityException("Invalid credentials");
            }

            if (PasswordHash.isLegacyHash(u.hash)) {
                String newSalt = PasswordHash.newSalt();
                String newHash = PasswordHash.hash(newSalt, normalizedPassword);
                users.updateCredentials(normalizedUserId, newSalt, newHash);
                audit.log("password hash upgraded: " + normalizedUserId);
            }

            UserSession s = sessionManager.create(u.role, normalizedUserId);
            audit.log("login ok: " + normalizedUserId + " role=" + u.role);
            return s;

        } catch (SecurityException se) {
            throw se;
        } catch (IllegalArgumentException ie) {
            throw ie;
        } catch (Exception e) {
            throw new RemoteException("DB error during login", e);
        }
    }

    @Override
    public void logout(UserSession session) throws RemoteException {
        UserSession s = sessionManager.require(session);
        sessionManager.remove(session);
        audit.log("logout: " + s.getUserId());
    }

    @Override
    public Employee registerEmployee(UserSession session, String firstName, String lastName, String icPassport, String initPass)
            throws RemoteException {

        UserSession s = requireRole(session, UserRole.HR, "HR only");

        try {
            String normalizedFirstName = requirePersonName(firstName, "First name");
            String normalizedLastName = requirePersonName(lastName, "Last name");
            String normalizedIcPassport = requireIcPassport(icPassport);
            String normalizedInitPass = requireInitialPassword(initPass);

            if (employees.icPassportExists(normalizedIcPassport)) {
                throw new IllegalArgumentException("IC/Passport already exists");
            }

 
            String empId = generateUniqueEmployeeId();


            Employee e = new Employee(normalizedFirstName, normalizedLastName, normalizedIcPassport);
            e.setEmployeeId(empId);

            employees.insertBasic(e);               
            details.createIfNotExists(empId);       


            String salt = PasswordHash.newSalt();
            String hash = PasswordHash.hash(salt, normalizedInitPass);
            users.insert(empId, UserRole.STAFF, salt, hash);


            int year = Year.now().getValue();
            leaveBalanceRepo.getYearBalanceOrCreate(empId, year, 15);

            audit.log("HR " + s.getUserId() + " registered " + empId);
            return e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteException("DB error during registerEmployee", e);
        }
    }

    @Override
    public Employee updateDetails(UserSession session, Employee updated) throws RemoteException {
        UserSession s = requireRole(session, UserRole.STAFF, "Staff only");
        if (updated == null) throw new IllegalArgumentException("Missing employee data");


        if (!s.getUserId().equals(updated.getEmployeeId()))
            throw new SecurityException("Cannot update other users");

        try {
            Employee basic = employees.findBasic(s.getUserId());
            if (basic == null) throw new IllegalStateException("Employee not found");

            updated.setPhoneNo(requirePhone(updated.getPhoneNo(), "Phone", true));
            updated.setEmergencyName(requirePersonNameOptional(updated.getEmergencyName(), "Emergency family name"));
            updated.setEmergencyPhoneNo(requirePhone(updated.getEmergencyNo(), "Emergency contact no", true));
            updated.setEmergencyRelationship(requireRelationship(updated.getEmergencyRelationship(), true));

            details.updateFromEmployee(updated);

            audit.log("Staff " + s.getUserId() + " updated details");
            return updated;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteException("DB error during updateDetails", e);
        }
    }

    @Override
    public int leaveBalance(UserSession session) throws RemoteException {
        UserSession s = requireRole(session, UserRole.STAFF, "Staff only");

        try {
            int year = Year.now().getValue();
            int bal = leaveBalanceRepo.getYearBalanceOrCreate(s.getUserId(), year, 15);
            audit.log("leaveBalance by " + s.getUserId() + " year=" + year + " bal=" + bal);
            return bal;

        } catch (Exception e) {
            throw new RemoteException("DB error during leaveBalance", e);
        }
    }
    
    @Override
    public Employee getMyProfile(UserSession session) throws RemoteException {
        UserSession s = requireRole(session, UserRole.STAFF, "Staff only");

        try {
            Employee basic = employees.findBasic(s.getUserId());
            if (basic == null) throw new IllegalStateException("Employee not found");


            EmployeeDetailsRepository.DetailsRow d = details.find(s.getUserId());
            if (d != null) {
                basic.setPhoneNo(d.phone);
                basic.setEmergencyName(d.emergencyName);
                basic.setEmergencyPhoneNo(d.emergencyNo);
                basic.setEmergencyRelationship(d.emergencyRelationship);
            }
            return basic;

        } catch (Exception e) {
            throw new RemoteException("DB error during getMyProfile", e);
        }
    }
    
    @Override
    public int applyLeave(UserSession session,
                          String leaveType,
                          String startDateYYYYMMDD,
                          String endDateYYYYMMDD,
                          String reason)
            throws RemoteException {

        UserSession s = requireRole(session, UserRole.STAFF, "Staff only");

        try {
            String normalizedLeaveType = requireLeaveType(leaveType);
            String normalizedReason = requireReason(reason);

        
            LocalDate start;
            LocalDate end;

            try {
                start = LocalDate.parse(requireNonBlank(startDateYYYYMMDD, "Start date"));
                end = LocalDate.parse(requireNonBlank(endDateYYYYMMDD, "End date"));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD.");
            }

            LocalDate today = LocalDate.now();


            if (start.isBefore(today))
                throw new IllegalArgumentException("Start date cannot be in the past.");

            if (end.isBefore(today))
                throw new IllegalArgumentException("End date cannot be in the past.");

 
            if (end.isBefore(start))
                throw new IllegalArgumentException("End date must be after or same as start date.");

            if (leaveRepo.hasOverlappingApplication(s.getUserId(), start, end)) {
                throw new IllegalStateException("Leave application overlaps an existing pending or approved leave.");
            }

   
            int daysRequested = (int) (ChronoUnit.DAYS.between(start, end)) + 1;

            if (daysRequested <= 0)
                throw new IllegalArgumentException("Invalid leave duration.");

            if (daysRequested > 60)   
                throw new IllegalArgumentException("Leave duration too long.");

    
            int year = Year.now().getValue();
            int bal = leaveBalanceRepo.getYearBalanceOrCreate(s.getUserId(), year, 15);

            if (normalizedLeaveType.equals("ANNUAL")) {
                if (daysRequested > bal)
                    throw new IllegalStateException(
                            "Not enough leave balance. Balance=" + bal + ", requested=" + daysRequested);
            }

            int leaveId = leaveRepo.insertApplication(
                    s.getUserId(),
                    normalizedLeaveType,
                    start,
                    end,
                    daysRequested,
                    normalizedReason
            );

            audit.log("applyLeave: emp=" + s.getUserId()
                    + " leaveId=" + leaveId
                    + " days=" + daysRequested);

            return leaveId;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteException("Error during applyLeave", e);
        }
    }

    @Override
    public String viewMyLeaveApplications(UserSession session) throws RemoteException {
        UserSession s = requireRole(session, UserRole.STAFF, "Staff only");

        try {
            List<LeaveApplicationsRepository.LeaveRow> rows = leaveRepo.listByEmployee(s.getUserId());
            if (rows.isEmpty()) return "No leave applications found.";

            StringBuilder sb = new StringBuilder();
            sb.append("=== MY LEAVE APPLICATIONS ===\n");
            for (var r : rows) {
                sb.append("ID: ").append(r.leaveId)
                  .append(" | ").append(r.leaveType)
                  .append(" | ").append(r.startDate).append(" -> ").append(r.endDate)
                  .append(" | Days: ").append(r.daysRequested)
                  .append(" | Status: ").append(r.status)
                  .append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            throw new RemoteException("DB error during viewMyLeaveApplications", e);
        }
    }

    @Override
    public String viewMyLeaveHistory(UserSession session) throws RemoteException {
        UserSession s = requireRole(session, UserRole.STAFF, "Staff only");

        try {
            List<LeaveApplicationsRepository.LeaveRow> rows = leaveRepo.listHistoryByEmployee(s.getUserId());
            if (rows.isEmpty()) return "No leave history found.";

            StringBuilder sb = new StringBuilder();
            sb.append("=== MY LEAVE HISTORY ===\n");
            for (var r : rows) {
                sb.append("ID: ").append(r.leaveId)
                  .append(" | ").append(r.leaveType)
                  .append(" | ").append(r.startDate).append(" -> ").append(r.endDate)
                  .append(" | Days: ").append(r.daysRequested)
                  .append(" | Status: ").append(r.status)
                  .append(" | DecidedBy: ").append(r.decidedBy == null ? "-" : r.decidedBy)
                  .append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            throw new RemoteException("DB error during viewMyLeaveHistory", e);
        }
    }

    @Override
    public String viewPendingLeaveApplications(UserSession session) throws RemoteException {
        UserSession s = requireRole(session, UserRole.HR, "HR only");

        try {
            List<LeaveApplicationsRepository.LeaveRow> rows = leaveRepo.listPending();
            if (rows.isEmpty()) return "No pending leave applications.";

            StringBuilder sb = new StringBuilder();
            sb.append("=== PENDING LEAVE APPLICATIONS ===\n");
            for (var r : rows) {
                sb.append("ID: ").append(r.leaveId)
                  .append(" | EMP: ").append(r.employeeId)
                  .append(" | ").append(r.leaveType)
                  .append(" | ").append(r.startDate).append(" -> ").append(r.endDate)
                  .append(" | Days: ").append(r.daysRequested)
                  .append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            throw new RemoteException("DB error during viewPendingLeaveApplications", e);
        }
    }

    @Override
    public List<Employee> listEmployees(UserSession session) throws RemoteException {
        UserSession s = requireRole(session, UserRole.HR, "HR only");

        try {
            List<Employee> all = employees.listAllBasic();
            for (Employee employee : all) {
                enrichWithDetails(employee);
            }
            audit.log("HR " + s.getUserId() + " listed employees count=" + all.size());
            return all;
        } catch (Exception e) {
            throw new RemoteException("DB error during listEmployees", e);
        }
    }

    @Override
    public Employee getEmployeeById(UserSession session, String employeeId) throws RemoteException {
        UserSession s = requireRole(session, UserRole.HR, "HR only");

        try {
            String normalizedEmployeeId = requireEmployeeId(employeeId);
            Employee basic = employees.findBasic(normalizedEmployeeId);
            if (basic == null) {
                throw new IllegalArgumentException("Employee not found: " + normalizedEmployeeId);
            }
            enrichWithDetails(basic);
            audit.log("HR " + s.getUserId() + " viewed employee " + normalizedEmployeeId);
            return basic;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteException("DB error during getEmployeeById", e);
        }
    }

    @Override
    public Employee updateEmployeeByHr(UserSession session, Employee updated) throws RemoteException {
        UserSession s = requireRole(session, UserRole.HR, "HR only");
        if (updated == null) throw new IllegalArgumentException("Missing employee data");

        try {
            String normalizedEmployeeId = requireEmployeeId(updated.getEmployeeId());
            Employee existing = employees.findBasic(normalizedEmployeeId);
            if (existing == null) {
                throw new IllegalArgumentException("Employee not found: " + normalizedEmployeeId);
            }

            String normalizedFirstName = requirePersonName(updated.getFirstName(), "First name");
            String normalizedLastName = requirePersonName(updated.getLastName(), "Last name");
            String normalizedIcPassport = requireIcPassport(updated.getIcPassport());
            if (!normalizedIcPassport.equalsIgnoreCase(existing.getIcPassport())
                    && employees.icPassportExists(normalizedIcPassport)) {
                throw new IllegalArgumentException("IC/Passport already exists");
            }

            Employee toSave = new Employee(normalizedFirstName, normalizedLastName, normalizedIcPassport);
            toSave.setEmployeeId(normalizedEmployeeId);
            toSave.setPhoneNo(requirePhone(updated.getPhoneNo(), "Phone", true));
            toSave.setEmergencyName(requirePersonNameOptional(updated.getEmergencyName(), "Emergency family name"));
            toSave.setEmergencyPhoneNo(requirePhone(updated.getEmergencyNo(), "Emergency contact no", true));
            toSave.setEmergencyRelationship(requireRelationship(updated.getEmergencyRelationship(), true));

            employees.updateBasic(toSave);
            details.updateFromEmployee(toSave);

            audit.log("HR " + s.getUserId() + " updated employee " + normalizedEmployeeId);
            return getEmployeeById(session, normalizedEmployeeId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteException("DB error during updateEmployeeByHr", e);
        }
    }

    @Override
    public void deleteEmployee(UserSession session, String employeeId) throws RemoteException {
        UserSession s = requireRole(session, UserRole.HR, "HR only");

        try {
            String normalizedEmployeeId = requireEmployeeId(employeeId);
            if (s.getUserId().equalsIgnoreCase(normalizedEmployeeId)) {
                throw new IllegalArgumentException("HR cannot delete own account.");
            }

            Employee existing = employees.findBasic(normalizedEmployeeId);
            if (existing == null) {
                throw new IllegalArgumentException("Employee not found: " + normalizedEmployeeId);
            }

            try (Connection c = DatabaseSocket.getConnection()) {
                c.setAutoCommit(false);
                try {
                    leaveRepo.deleteByEmployee(c, normalizedEmployeeId);
                    leaveBalanceRepo.deleteByEmployee(c, normalizedEmployeeId);
                    details.deleteByEmployee(c, normalizedEmployeeId);
                    employees.deleteById(c, normalizedEmployeeId);
                    users.deleteById(c, normalizedEmployeeId);
                    c.commit();
                } catch (Exception e) {
                    c.rollback();
                    throw e;
                } finally {
                    c.setAutoCommit(true);
                }
            }

            audit.log("HR " + s.getUserId() + " deleted employee " + normalizedEmployeeId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteException("DB error during deleteEmployee", e);
        }
    }

    @Override
    public void decideLeave(UserSession session, int leaveId, boolean approve) throws RemoteException {
        UserSession s = requireRole(session, UserRole.HR, "HR only");

        try {
            if (leaveId <= 0) throw new IllegalArgumentException("Leave ID must be greater than 0");

            try (Connection c = DatabaseSocket.getConnection()) {
                c.setAutoCommit(false);
                try {
                    var row = leaveRepo.findById(c, leaveId);
                    if (row == null) throw new IllegalArgumentException("Leave ID not found");
                    if (!"PENDING".equalsIgnoreCase(row.status)) throw new IllegalStateException("Leave is not pending");

                    if (approve) {
                        boolean annualLeave = "ANNUAL".equalsIgnoreCase(row.leaveType);
                        int year = row.startDate.toLocalDate().getYear();

                        if (annualLeave) {
                            leaveBalanceRepo.getYearBalanceOrCreate(c, row.employeeId, year, 15);
                            boolean deducted = leaveBalanceRepo.decreaseIfEnough(c, row.employeeId, year, row.daysRequested);
                            if (!deducted) {
                                throw new IllegalStateException("Cannot approve: insufficient balance.");
                            }
                        }

                        boolean decided = leaveRepo.setDecisionIfPending(c, leaveId, "APPROVED", s.getUserId());
                        if (!decided) throw new IllegalStateException("Leave is not pending");
                        audit.log("decideLeave APPROVED: leaveId=" + leaveId + " by=" + s.getUserId());
                    } else {
                        boolean decided = leaveRepo.setDecisionIfPending(c, leaveId, "REJECTED", s.getUserId());
                        if (!decided) throw new IllegalStateException("Leave is not pending");
                        audit.log("decideLeave REJECTED: leaveId=" + leaveId + " by=" + s.getUserId());
                    }
                    c.commit();
                } catch (Exception e) {
                    c.rollback();
                    throw e;
                } finally {
                    c.setAutoCommit(true);
                }
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteException("DB error during decideLeave", e);
        }
    }
    
    @Override
    public String generateYearlyLeaveReport(UserSession session, String employeeId, int year) throws RemoteException {
        UserSession s = requireRole(session, UserRole.HR, "HR only");

        try {
            String normalizedEmployeeId = requireEmployeeId(employeeId);

            if (year < 2000 || year > 2100)
                throw new IllegalArgumentException("Invalid year");


            Employee basic = employees.findBasic(normalizedEmployeeId);
            if (basic == null) return "Employee not found: " + normalizedEmployeeId;

            var approved = leaveRepo.listApprovedByEmployeeAndYear(normalizedEmployeeId, year);

            int totalApprovedDays = 0;
            for (var r : approved) totalApprovedDays += r.daysRequested;

            int balance = leaveBalanceRepo.getYearBalanceOrCreate(normalizedEmployeeId, year, 15);

            StringBuilder sb = new StringBuilder();
            sb.append("\n==== YEARLY LEAVE REPORT ====\n");
            sb.append("Employee ID : ").append(normalizedEmployeeId).append("\n");
            sb.append("Name        : ").append(basic.getFirstName()).append(" ").append(basic.getLastName()).append("\n");
            sb.append("Year        : ").append(year).append("\n");
            sb.append("-----------------------------------------\n");

            if (approved.isEmpty()) {
                sb.append("No APPROVED leave records for this year.\n");
            } else {
                for (var r : approved) {
                    sb.append("LeaveID: ").append(r.leaveId)
                      .append(" | ").append(r.leaveType)
                      .append(" | ").append(r.startDate).append(" → ").append(r.endDate)
                      .append(" | Days: ").append(r.daysRequested)
                      .append("\n");
                }
            }

            sb.append("-----------------------------------------\n");
            sb.append("Total Approved Days : ").append(totalApprovedDays).append("\n");
            sb.append("Remaining Balance   : ").append(balance).append("\n");

            audit.log("HR " + s.getUserId() + " generated yearly report for " + normalizedEmployeeId + " year=" + year);
            return sb.toString();

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteException("DB error during generateYearlyLeaveReport", e);
        }
    }

    private UserSession requireRole(UserSession session, UserRole requiredRole, String message) {
        UserSession s = sessionManager.require(session);
        if (s.getRole() != requiredRole) {
            throw new SecurityException(message);
        }
        return s;
    }

    private static String requirePersonName(String value, String fieldName) {
        String normalized = requireNonBlank(value, fieldName);

        if (!PERSON_NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(fieldName + " must contain alphabet letters only");
        }

        return normalized;
    }

    private static String requireIcPassport(String value) {
        String normalized = requireNonBlank(value, "IC/Passport").toUpperCase();
        if (!IC_PASSPORT_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("IC/Passport must be 6 to 20 characters using letters, numbers, or hyphen");
        }

        return normalized;
    }

    private static String requireInitialPassword(String value) {
        String normalized = requireNonBlank(value, "Initial password");
        if (!STRONG_PASSWORD_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Initial password must be 8-64 chars and include uppercase, lowercase, number, and symbol"
            );
        }

        return normalized;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Please do not leave " + fieldName + " blank.");
        }
        return value.trim();
    }

    private static String requireUserId(String value) {
        String normalized = normalizeUserId(requireNonBlank(value, "User ID"));
        if (!EMPLOYEE_ID_PATTERN.matcher(normalized).matches() && !HR_ID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("User ID format is invalid");
        }
        return normalized;
    }

    private String resolveLoginUserId(String value) throws Exception {
        String identifier = requireNonBlank(value, "User ID or Employee Name");
        String normalizedIdentifier = normalizeUserId(identifier);
        if (EMPLOYEE_ID_PATTERN.matcher(normalizedIdentifier).matches()
                || HR_ID_PATTERN.matcher(normalizedIdentifier).matches()) {
            return requireUserId(normalizedIdentifier);
        }

        if ("HR".equalsIgnoreCase(identifier) || "HR ADMIN".equalsIgnoreCase(identifier)) {
            return "H-000001";
        }

        List<String> matches = employees.findIdsByFullName(identifier);
        if (matches.isEmpty()) {
            throw new SecurityException("Invalid credentials");
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("Multiple employees found with that name. Use User ID.");
        }
        return normalizeEmployeeId(matches.get(0));
    }

    private static String requireEmployeeId(String value) {
        String normalized = normalizeEmployeeId(requireNonBlank(value, "Employee ID"));
        if (!EMPLOYEE_ID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Employee ID format is invalid");
        }
        return normalized;
    }

    private static String normalizeUserId(String value) {
        String trimmed = value.trim();
        return LEGACY_USER_ID_PATTERN.matcher(trimmed).matches() ? trimmed.toUpperCase() : trimmed.toLowerCase();
    }

    private static String normalizeEmployeeId(String value) {
        String trimmed = value.trim();
        return LEGACY_EMPLOYEE_ID_PATTERN.matcher(trimmed).matches() ? trimmed.toUpperCase() : trimmed.toLowerCase();
    }

    private static String requirePhone(String value, String fieldName, boolean allowBlank) {
        if (value == null || value.trim().isEmpty()) {
            if (allowBlank) return null;
            throw new IllegalArgumentException("Please do not leave " + fieldName + " blank.");
        }

        String normalized = value.replaceAll("[\\s-]+", "").trim();
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(fieldName + " must contain 8 to 15 digits and may start with +");
        }

        return normalized;
    }

    private static String requirePersonNameOptional(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return requirePersonName(value, fieldName);
    }

    private static String requireRelationship(String value, boolean allowBlank) {
        if (value == null || value.trim().isEmpty()) {
            if (allowBlank) return null;
            throw new IllegalArgumentException("Please do not leave Emergency relationship blank.");
        }

        String normalized = value.trim();
        if (!RELATIONSHIP_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Emergency relationship must contain alphabet letters only");
        }
        return normalized;
    }

    private static String requireLeaveType(String value) {
        String normalized = requireNonBlank(value, "Leave type").toUpperCase();
        switch (normalized) {
            case "ANNUAL":
            case "MEDICAL":
            case "EMERGENCY":
            case "UNPAID":
                return normalized;
            default:
                throw new IllegalArgumentException("Invalid leave type");
        }
    }

    private static String requireReason(String value) {
        String normalized = requireNonBlank(value, "Reason");
        if (normalized.length() > 200) {
            throw new IllegalArgumentException("Reason must not exceed 200 characters");
        }
        return normalized;
    }

    private String generateUniqueEmployeeId() throws Exception {
        for (int i = 0; i < 8; i++) {
            String candidate = IdGenerator.nextEmployeeCounter();
            if (!users.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate unique employee ID");
    }

    private void enrichWithDetails(Employee employee) throws Exception {
        EmployeeDetailsRepository.DetailsRow d = details.find(employee.getEmployeeId());
        if (d == null) {
            return;
        }
        employee.setPhoneNo(d.phone);
        employee.setEmergencyName(d.emergencyName);
        employee.setEmergencyPhoneNo(d.emergencyNo);
        employee.setEmergencyRelationship(d.emergencyRelationship);
    }

    private void ensureDemoStaffAccount() throws Exception {
        final String staffId = "E-000001";
        final String staffPassword = "staff123";
        Employee staff = employees.findBasic(staffId);
        if (staff == null) {
            String defaultIc = "DEMO-E000001";
            if (employees.icPassportExists(defaultIc)) {
                defaultIc = "DEMO-" + (System.currentTimeMillis() % 1_000_000);
            }
            Employee demo = new Employee("Demo", "Staff", defaultIc);
            demo.setEmployeeId(staffId);
            employees.insertBasic(demo);
        }
        details.createIfNotExists(staffId);
        int year = Year.now().getValue();
        leaveBalanceRepo.getYearBalanceOrCreate(staffId, year, 15);

        String salt = PasswordHash.newSalt();
        String hash = PasswordHash.hash(salt, staffPassword);
        if (users.exists(staffId)) {
            users.updateCredentials(staffId, salt, hash);
        } else {
            users.insert(staffId, UserRole.STAFF, salt, hash);
        }
        audit.log("Default staff ensured: " + staffId);
    }
    
}
