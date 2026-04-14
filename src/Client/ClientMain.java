package Client;

import Common.Authorization;
import Common.Employee;
import Common.UserRole;
import Common.UserSession;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.rmi.ssl.SslRMIClientSocketFactory;

import java.util.Scanner;
import java.util.regex.Pattern;

public class ClientMain {
    private static final Pattern PERSON_NAME_PATTERN = Pattern.compile("[A-Za-z]+(?:[ -][A-Za-z]+)*");
    private static final Pattern IC_PASSPORT_PATTERN = Pattern.compile("[A-Za-z0-9-]{6,20}");
    private static final Pattern USER_ID_PATTERN = Pattern.compile("(?:E|H)-\\d{6}");
    private static final Pattern EMPLOYEE_ID_PATTERN = Pattern.compile("E-\\d{6}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?\\d{8,15}");
    private static final Pattern RELATIONSHIP_PATTERN = Pattern.compile("[A-Za-z]+(?:[ -][A-Za-z]+)*");

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 1099;


    private static final String TRUSTSTORE_PATH =
            "C:\\Users\\User\\Documents\\NetBeansProjects\\DCS\\client-truststore.p12";
    private static final String TRUSTSTORE_PASS = "888888";

    public static void main(String[] args) {
        try {
     
            System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_PATH);
            System.setProperty("javax.net.ssl.trustStorePassword", TRUSTSTORE_PASS);
            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");

            System.clearProperty("javax.net.ssl.keyStore");
            System.clearProperty("javax.net.ssl.keyStorePassword");

            // ------------------------------------------------------------
            String ts = System.getProperty("javax.net.ssl.trustStore");
            System.out.println("java.version=" + System.getProperty("java.version"));
            System.out.println("java.home=" + System.getProperty("java.home"));
            System.out.println("user.dir=" + System.getProperty("user.dir"));
            System.out.println("trustStore=" + ts);
            System.out.println("trustStoreExists=" + new java.io.File(ts).exists());

            Registry reg = LocateRegistry.getRegistry(
                    SERVER_IP,
                    SERVER_PORT,
                    new SslRMIClientSocketFactory()
            );

            Authorization service = (Authorization) reg.lookup("Authorization");
            System.out.println("Connected: " + service.ping());

            Scanner sc = new Scanner(System.in);
            UserSession session = null;

            while (true) {
                while (session == null) {
                    session = doLogin(sc, service);
                }

                if (session.getRole() == UserRole.HR) {
                    session = hrMenu(sc, service, session);
                } else if (session.getRole() == UserRole.STAFF) {
                    session = staffMenu(sc, service, session);
                } else {
                    System.out.println("Unknown role: " + session.getRole());
                    session = null;
                }
            }

        } catch (Exception e) {
            System.out.println("Failed to connect or run client: " + e);
            e.printStackTrace();
        }
    }

    private static UserSession doLogin(Scanner sc, Authorization service) {
        try {
            System.out.println("\n==== LOGIN ====");
            System.out.println("Type CANCEL at any prompt to retry login.");
            String id = promptValidated(sc, "User ID: ", "User ID", USER_ID_PATTERN,
                    "User ID must follow the format H-000001 or E-000001.");
            if (id == null) {
                System.out.println("Login cancelled.");
                return null;
            }
            id = id.toUpperCase();
            String pw = promptRequired(sc, "Password: ", "Password");
            if (pw == null) {
                System.out.println("Login cancelled.");
                return null;
            }

            UserSession session = service.login(id, pw);
            System.out.println("Login success. Role: " + session.getRole());
            return session;

        } catch (Exception e) {
            System.out.println("Login failed: " + friendlyError(e));
            return null;
        }
    }

    private static UserSession hrMenu(Scanner sc, Authorization service, UserSession session) {
        while (true) {
            System.out.println("1) Register Employee");
            System.out.println("2) View Pending Leave Requests");
            System.out.println("3) Approve / Reject Leave");
            System.out.println("4) Generate Yearly Leave Report");
            System.out.println("5) Logout");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();
            if (choice.isEmpty()) {
                System.out.println("Please do not leave selection blank.");
                continue;
            }

            try {
                switch (choice) {

                    case "1": {
                        System.out.println("Type CANCEL at any prompt to go back.");

                        String fn = promptValidated(sc, "First Name: ", "First name", PERSON_NAME_PATTERN,
                                "First name must contain alphabet letters only.");
                        if (fn == null) {
                            System.out.println("Employee creation cancelled.");
                            break;
                        }

                        String ln = promptValidated(sc, "Last Name: ", "Last name", PERSON_NAME_PATTERN,
                                "Last name must contain alphabet letters only.");
                        if (ln == null) {
                            System.out.println("Employee creation cancelled.");
                            break;
                        }

                        String ic = promptValidated(sc, "IC/Passport: ", "IC/Passport", IC_PASSPORT_PATTERN,
                                "IC/Passport must be 6 to 20 characters using letters, numbers, or hyphen.");
                        if (ic == null) {
                            System.out.println("Employee creation cancelled.");
                            break;
                        }

                        String initPass = promptPassword(sc);
                        if (initPass == null) {
                            System.out.println("Employee creation cancelled.");
                            break;
                        }

                        Employee e = service.registerEmployee(session, fn, ln, ic, initPass);
                        System.out.println("Employee created: " + e.getEmployeeId());
                        break;
                    }

                    case "2": {
                        String pending = service.viewPendingLeaveApplications(session);
                        System.out.println(pending);
                        break;
                    }

                    case "3": {
                        String pending = service.viewPendingLeaveApplications(session);
                        System.out.println(pending);

                        System.out.print("\nEnter Leave ID to decide (or 0 to cancel): ");
                        String leaveIdInput = sc.nextLine().trim();
                        if (leaveIdInput.isEmpty()) {
                            System.out.println("Please do not leave selection blank.");
                            break;
                        }
                        int leaveId;
                        try {
                            leaveId = Integer.parseInt(leaveIdInput);
                        } catch (NumberFormatException nfe) {
                            System.out.println(" Invalid leave ID.");
                            break;
                        }

                        if (leaveId == 0) {
                            System.out.println("Cancelled.");
                            break;
                        }

                        System.out.print("Approve? (Y/N): ");
                        String decision = sc.nextLine().trim();
                        if (decision.isEmpty()) {
                            System.out.println("Please do not leave selection blank.");
                            break;
                        }

                        boolean approve;
                        if (decision.equalsIgnoreCase("Y")) {
                            approve = true;
                        } else if (decision.equalsIgnoreCase("N")) {
                            approve = false;
                        } else {
                            System.out.println(" Invalid choice. Enter Y or N.");
                            break;
                        }

                        service.decideLeave(session, leaveId, approve);
                        System.out.println(approve ? " Leave approved." : " Leave rejected.");
                        break;
                    }

                    case "4": {
                        System.out.println("Type CANCEL at any prompt to go back.");
                        String empId = promptValidated(sc, "Enter Employee ID: ", "Employee ID", EMPLOYEE_ID_PATTERN,
                                "Employee ID must follow the format E-000001.");
                        if (empId == null) {
                            System.out.println("Report generation cancelled.");
                            break;
                        }
                        empId = empId.toUpperCase();
                        int year = promptYear(sc);
                        if (year == -1) {
                            System.out.println("Report generation cancelled.");
                            break;
                        }

                        String report = service.generateYearlyLeaveReport(session, empId, year);
                        System.out.println(report);
                        break;
                    }

                    case "5": {
                        safeLogout(service, session);
                        System.out.println("Logged out.");
                        return null;
                    }

                    default:
                        System.out.println("Invalid option.");
                }

            } catch (Exception ex) {
                if (isSessionExpired(ex)) {
                    System.out.println(" Session expired. Please login again.");
                    return null;
                }
                System.out.println("Error: " + friendlyError(ex));
            }
        }
    }

    private static UserSession staffMenu(Scanner sc, Authorization service, UserSession session) {
        while (true) {
            System.out.println("\n==== STAFF MENU ====");
            System.out.println("1) Update Details");
            System.out.println("2) View Leave Balance");
            System.out.println("3) Apply Leave");
            System.out.println("4) View Leave Application");
            System.out.println("5) View Leave History");
            System.out.println("6) Logout");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();
            if (choice.isEmpty()) {
                System.out.println("Please do not leave selection blank.");
                continue;
            }

            try {
                switch (choice) {

                    case "1": {
                        Employee profile = service.getMyProfile(session);
                        System.out.println("Type CANCEL at any prompt to go back.");

                        while (true) {
                            System.out.println("\n=== UPDATE DETAILS ===");
                            System.out.println("1) Phone                   : " + nullSafe(profile.getPhoneNo()));
                            System.out.println("2) Emergency Family Name   : " + nullSafe(profile.getEmergencyName()));
                            System.out.println("3) Emergency Contact No    : " + nullSafe(profile.getEmergencyNo()));
                            System.out.println("4) Emergency Relationship  : " + nullSafe(profile.getEmergencyRelationship()));
                            System.out.println("5) Save & Exit");
                            System.out.print("Select a field to edit: ");

                            String pick = sc.nextLine().trim();
                            if (pick.isEmpty()) {
                                System.out.println("Please do not leave selection blank.");
                                continue;
                            }
                            if (pick.equals("5")) break;

                            switch (pick) {
                                case "1":
                                    String newPhone = promptValidated(sc, "New Phone: ", "Phone", PHONE_PATTERN,
                                            "Phone must contain 8 to 15 digits and may start with +.");
                                    if (newPhone == null) {
                                        System.out.println("Update cancelled.");
                                        break;
                                    }
                                    profile.setPhoneNo(newPhone);
                                    break;
                                case "2":
                                    String newEmergencyName = promptValidated(sc, "New Emergency Family Name: ",
                                            "Emergency family name", PERSON_NAME_PATTERN,
                                            "Emergency family name must contain alphabet letters only.");
                                    if (newEmergencyName == null) {
                                        System.out.println("Update cancelled.");
                                        break;
                                    }
                                    profile.setEmergencyName(newEmergencyName);
                                    break;
                                case "3":
                                    String newEmergencyPhone = promptValidated(sc, "New Emergency Contact No: ",
                                            "Emergency contact no", PHONE_PATTERN,
                                            "Emergency contact no must contain 8 to 15 digits and may start with +.");
                                    if (newEmergencyPhone == null) {
                                        System.out.println("Update cancelled.");
                                        break;
                                    }
                                    profile.setEmergencyPhoneNo(newEmergencyPhone);
                                    break;
                                case "4":
                                    String newRelationship = promptValidated(sc, "New Emergency Relationship: ",
                                            "Emergency relationship", RELATIONSHIP_PATTERN,
                                            "Emergency relationship must contain alphabet letters only.");
                                    if (newRelationship == null) {
                                        System.out.println("Update cancelled.");
                                        break;
                                    }
                                    profile.setEmergencyRelationship(newRelationship);
                                    break;
                                default:
                                    System.out.println("Invalid option");
                            }
                        }

                        Employee updated = new Employee();
                        updated.setEmployeeId(session.getUserId());
                        updated.setPhoneNo(profile.getPhoneNo());
                        updated.setEmergencyName(profile.getEmergencyName());
                        updated.setEmergencyPhoneNo(profile.getEmergencyNo());
                        updated.setEmergencyRelationship(profile.getEmergencyRelationship());

                        service.updateDetails(session, updated);
                        System.out.println("Details updated successfully!");
                        break;
                    }

                    case "2": {
                        int bal = service.leaveBalance(session);
                        System.out.println("Leave balance: " + bal + " day(s)");
                        break;
                    }

                    case "3": {
                        while (true) {
                            System.out.println("Type CANCEL at any prompt to go back.");
                            System.out.println("\nSelect Leave Type:");
                            System.out.println("1) Annual Leave");
                            System.out.println("2) Medical Leave");
                            System.out.println("3) Emergency Leave");
                            System.out.println("4) Unpaid Leave");
                            System.out.println("5) Cancel / Back");
                            System.out.print("Choice: ");

                            String typeChoice = sc.nextLine().trim();
                            if (typeChoice.equalsIgnoreCase("CANCEL")) {
                                System.out.println("Leave application cancelled.");
                                return session;
                            }
                            if (typeChoice.isEmpty()) {
                                System.out.println("Please do not leave selection blank.");
                                continue;
                            }
                            String type;

                            switch (typeChoice) {
                                case "1": type = "ANNUAL"; break;
                                case "2": type = "MEDICAL"; break;
                                case "3": type = "EMERGENCY"; break;
                                case "4": type = "UNPAID"; break;
                                case "5":
                                    System.out.println("Cancelled.");
                                    return session;
                                default:
                                    System.out.println(" Invalid option.");
                                    continue;
                            }

                            String start = promptRequired(sc, "Start Date (YYYY-MM-DD): ", "Start date");
                            if (start == null) {
                                System.out.println("Leave application cancelled.");
                                return session;
                            }
                            int requestedDays = promptPositiveInt(sc, "Number of Leave Days: ", "Leave days");
                            if (requestedDays == -1) {
                                System.out.println("Leave application cancelled.");
                                return session;
                            }
                            String reason = promptRequired(sc, "Reason: ", "Reason");
                            if (reason == null) {
                                System.out.println("Leave application cancelled.");
                                return session;
                            }

                            try {
                                java.time.LocalDate sDate = java.time.LocalDate.parse(start);

                                if (sDate.isBefore(java.time.LocalDate.now())) {
                                    System.out.println(" Start date cannot be in the past.");
                                    continue;
                                }

                                int availableBalance = service.leaveBalance(session);
                                int remainingBalance = "ANNUAL".equalsIgnoreCase(type)
                                        ? availableBalance - requestedDays
                                        : availableBalance;
                                java.time.LocalDate eDate = sDate.plusDays(requestedDays - 1L);

                                if ("ANNUAL".equalsIgnoreCase(type) && remainingBalance < 0) {
                                    System.out.println(" Requested days exceed available annual leave balance.");
                                    System.out.println(" Available Balance : " + availableBalance + " day(s)");
                                    System.out.println(" Requested Days    : " + requestedDays + " day(s)");
                                    continue;
                                }

                                System.out.println(" Remaining Leave Days: " + remainingBalance + " day(s)");

                                System.out.println("\n=== CONFIRM LEAVE ===");
                                System.out.println("Type  : " + type);
                                System.out.println("From  : " + sDate);
                                System.out.println("To    : " + eDate);
                                System.out.println("Days  : " + requestedDays);
                                System.out.println("Available Balance : " + availableBalance + " day(s)");
                                System.out.println("Remaining Balance : " + remainingBalance + " day(s)");
                                System.out.println("Reason: " + reason);
                                System.out.print("Confirm submit? (Y/N): ");

                                String confirm = sc.nextLine().trim();
                                if (confirm.equalsIgnoreCase("CANCEL")) {
                                    System.out.println("Leave application cancelled.");
                                    return session;
                                }
                                if (confirm.isEmpty()) {
                                    System.out.println("Please do not leave selection blank.");
                                    continue;
                                }
                                if (!confirm.equalsIgnoreCase("Y")) {
                                    System.out.println("Submission cancelled.");
                                    return session;
                                }

                                String end = eDate.toString();
                                int leaveId = service.applyLeave(session, type, start, end, reason);
                                System.out.println(" Leave submitted. ID: " + leaveId + " (Status: PENDING)");
                                return session;

                            } catch (Exception ex) {
                                System.out.println(" " + friendlyError(ex));
                            }
                        }
                    }

                    case "4": {
                        String apps = service.viewMyLeaveApplications(session);
                        System.out.println(apps);
                        break;
                    }

                    case "5": {
                        String history = service.viewMyLeaveHistory(session);
                        System.out.println(history);
                        break;
                    }

                    case "6": {
                        safeLogout(service, session);
                        System.out.println("Logged out.");
                        return null;
                    }

                    default:
                        System.out.println("Invalid option.");
                }
            } catch (Exception ex) {
                if (isSessionExpired(ex)) {
                    System.out.println("⚠ Session expired. Please login again.");
                    return null;
                }
                System.out.println("Error: " + friendlyError(ex));
            }
        }
    }

    private static void safeLogout(Authorization service, UserSession session) {
        try {
            service.logout(session);
        } catch (Exception ignored) {}
    }

    private static boolean isSessionExpired(Exception ex) {
        String msg = friendlyError(ex);
        if (msg == null) return false;
        msg = msg.toLowerCase();
        return msg.contains("expired") || msg.contains("invalid");
    }

    private static String promptValidated(Scanner sc, String prompt, String fieldName, Pattern pattern, String invalidMessage) {
        while (true) {
            System.out.print(prompt);
            String value = sc.nextLine().trim();
            if (value.equalsIgnoreCase("CANCEL")) {
                return null;
            }
            if (value.isEmpty()) {
                System.out.println("Please do not leave " + fieldName + " blank.");
                continue;
            }
            if (!pattern.matcher(value).matches()) {
                System.out.println(invalidMessage);
                continue;
            }
            return value;
        }
    }

    private static String promptPassword(Scanner sc) {
        while (true) {
            String password = promptRequired(sc, "Initial Password: ", "Initial password");
            if (password == null) {
                return null;
            }
            if (password.length() < 6) {
                System.out.println("Initial password must be at least 6 characters.");
                continue;
            }
            return password;
        }
    }

    private static String promptRequired(Scanner sc, String prompt, String fieldName) {
        while (true) {
            System.out.print(prompt);
            String value = sc.nextLine().trim();
            if (value.equalsIgnoreCase("CANCEL")) {
                return null;
            }
            if (value.isEmpty()) {
                System.out.println("Please do not leave " + fieldName + " blank.");
                continue;
            }
            return value;
        }
    }

    private static int promptPositiveInt(Scanner sc, String prompt, String fieldName) {
        while (true) {
            String value = promptRequired(sc, prompt, fieldName);
            if (value == null) {
                return -1;
            }
            try {
                int parsed = Integer.parseInt(value);
                if (parsed <= 0) {
                    System.out.println(fieldName + " must be at least 1.");
                    continue;
                }
                return parsed;
            } catch (NumberFormatException ex) {
                System.out.println(fieldName + " must be a whole number.");
            }
        }
    }

    private static int promptYear(Scanner sc) {
        while (true) {
            int year = promptPositiveInt(sc, "Enter Year: ", "Year");
            if (year == -1) {
                return -1;
            }
            if (year < 2000 || year > 2100) {
                System.out.println("Year must be between 2000 and 2100.");
                continue;
            }
            return year;
        }
    }

    private static String friendlyError(Throwable throwable) {
        Throwable current = throwable;
        String fallback = null;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && !msg.isBlank()) {
                if (!msg.startsWith("RemoteException occurred in server thread")
                        && !msg.contains("nested exception is")) {
                    fallback = msg;
                } else if (fallback == null) {
                    fallback = msg;
                }
            }
            current = current.getCause();
        }
        return fallback != null ? fallback : "Unexpected error occurred.";
    }

    private static String nullSafe(String s){
        return s == null || s.isBlank() ? "-" : s;
    }
}
