package Server;

import Common.Authorization;
import Common.Employee;
import Common.UserRole;
import Common.UserSession;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Year;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;


public class ServiceImpl extends UnicastRemoteObject implements Authorization {
    private static final Pattern PERSON_NAME_PATTERN = Pattern.compile("[A-Za-z]+(?:[ -][A-Za-z]+)*");
    private static final Pattern IC_PASSPORT_PATTERN = Pattern.compile("[A-Za-z0-9-]{6,20}");
    private static final Pattern EMPLOYEE_ID_PATTERN = Pattern.compile("E-\\d{6}");
    private static final Pattern HR_ID_PATTERN = Pattern.compile("H-\\d{6}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?\\d{8,15}");
    private static final Pattern RELATIONSHIP_PATTERN = Pattern.compile("[A-Za-z]+(?:[ -][A-Za-z]+)*");


    private final Session sessionManager = new Session();
    private final ActivityLog audit = new ActivityLog("audit.log");

    private final UsersRepository users = new UsersRepository();
    private final EmployeesRepository employees = new EmployeesRepository();
    private final EmployeeDetailsRepository details = new EmployeeDetailsRepository();
    private final LeaveBalanceRepository leaveBalanceRepo = new LeaveBalanceRepository();
    private final LeaveApplicationsRepository leaveRepo = new LeaveApplicationsRepository();
    
    protected ServiceImpl() throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());

        // Create default HR only ONCE
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
    }

    @Override
    public String ping() throws RemoteException {
        audit.log("ping()");
        return "Server is valid!";
    }

    @Override
    public UserSession login(String userId, String password) throws RemoteException {
        try {
            String normalizedUserId = requireUserId(userId);
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
        audit.log("logout: " + s.getUserId()); // ✅ fixed
    }

    @Override
    public Employee registerEmployee(UserSession session, String firstName, String lastName, String icPassport, String initPass)
            throws RemoteException {

        UserSession s = sessionManager.require(session);
        if (s.getRole() != UserRole.HR) throw new SecurityException("HR only");

        try {
            String normalizedFirstName = requirePersonName(firstName, "First name");
            String normalizedLastName = requirePersonName(lastName, "Last name");
            String normalizedIcPassport = requireIcPassport(icPassport);
            String normalizedInitPass = requireInitialPassword(initPass);

            if (employees.icPassportExists(normalizedIcPassport)) {
                throw new IllegalArgumentException("IC/Passport already exists");
            }

 
            String empId = IdGenerator.nextEmployeeCounter();


            Employee e = new Employee(normalizedFirstName, normalizedLastName, normalizedIcPassport);
            e.setEmployeeId(empId);

            employees.insertBasic(e);               
            details.createIfNotExists(empId);       


            String salt = PasswordHash.newSalt();
            String hash = PasswordHash.hash(salt, normalizedInitPass);
            users.insert(empId, UserRole.STAFF, salt, hash);


            int year = Year.now().getValue();
            leaveBalanceRepo.getYearBalanceOrCreate(empId, year, 15);

            audit.log("HR " + s.getUserId() + " registered " + empId); // 
            return e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteException("DB error during registerEmployee", e);
        }
    }

    @Override
    public Employee updateDetails(UserSession session, Employee updated) throws RemoteException {
        UserSession s = sessionManager.require(session);
        if (s.getRole() != UserRole.STAFF) throw new SecurityException("Staff only");
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
        UserSession s = sessionManager.require(session);
        if (s.getRole() != UserRole.STAFF) throw new SecurityException("Staff only");

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
        UserSession s = sessionManager.require(session);
        if (s.getRole() != UserRole.STAFF) throw new SecurityException("Staff only");

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

        UserSession s = sessionManager.require(session);
        if (s.getRole() != UserRole.STAFF)
            throw new SecurityException("Staff only");

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

   
            int daysRequested = (int) (java.time.temporal.ChronoUnit.DAYS.between(start, end)) + 1;

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
        UserSession s = sessionManager.require(session);
        if (s.getRole() != UserRole.STAFF) throw new SecurityException("Staff only");

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
        UserSession s = sessionManager.require(session);
        if (s.getRole() != UserRole.STAFF) throw new SecurityException("Staff only");

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
        UserSession s = sessionManager.require(session);
        if (s.getRole() != UserRole.HR) throw new SecurityException("HR only");

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
    public void decideLeave(UserSession session, int leaveId, boolean approve) throws RemoteException {
        UserSession s = sessionManager.require(session);
        if (s.getRole() != UserRole.HR) throw new SecurityException("HR only");

        try {
            if (leaveId <= 0) throw new IllegalArgumentException("Leave ID must be greater than 0");

            var row = leaveRepo.findById(leaveId);
            if (row == null) throw new IllegalArgumentException("Leave ID not found");
            if (!"PENDING".equalsIgnoreCase(row.status)) throw new IllegalStateException("Leave is not pending");

            if (approve) {
                int year = Year.now().getValue();
                int bal = leaveBalanceRepo.getYearBalanceOrCreate(row.employeeId, year, 15);
                if (row.daysRequested > bal) {
                    throw new IllegalStateException("Cannot approve: insufficient balance. Balance=" + bal);
                }
    
                leaveBalanceRepo.setBalance(row.employeeId, year, bal - row.daysRequested);
                leaveRepo.setDecision(leaveId, "APPROVED", s.getUserId());
                audit.log("decideLeave APPROVED: leaveId=" + leaveId + " by=" + s.getUserId());
            } else {
                leaveRepo.setDecision(leaveId, "REJECTED", s.getUserId());
                audit.log("decideLeave REJECTED: leaveId=" + leaveId + " by=" + s.getUserId());
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteException("DB error during decideLeave", e);
        }
    }
    
    @Override
    public String generateYearlyLeaveReport(UserSession session, String employeeId, int year) throws RemoteException {
        UserSession s = sessionManager.require(session);
        if (s.getRole() != UserRole.HR) throw new SecurityException("HR only");

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
        if (normalized.length() < 6) {
            throw new IllegalArgumentException("Initial password must be at least 6 characters");
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
        String normalized = requireNonBlank(value, "User ID").toUpperCase();
        if (!EMPLOYEE_ID_PATTERN.matcher(normalized).matches() && !HR_ID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("User ID format is invalid");
        }
        return normalized;
    }

    private static String requireEmployeeId(String value) {
        String normalized = requireNonBlank(value, "Employee ID").toUpperCase();
        if (!EMPLOYEE_ID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Employee ID format is invalid");
        }
        return normalized;
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
    
}
