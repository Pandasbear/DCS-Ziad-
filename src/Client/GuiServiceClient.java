package Client;

import Common.Authorization;
import Common.Employee;
import Common.UserRole;
import Common.UserSession;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import javax.rmi.ssl.SslRMIClientSocketFactory;

public class GuiServiceClient {
    private static final String SERVER_IP = System.getProperty("crest.server.host", "localhost");
    private static final int SERVER_PORT = Integer.parseInt(System.getProperty("crest.server.port", "1099"));
    private static final boolean SSL = Boolean.parseBoolean(System.getProperty("crest.ssl", "true"));
    private static final String TRUSTSTORE_PATH = System.getProperty(
            "crest.truststore.path",
            Paths.get("server.keystore").toAbsolutePath().toString()
    );
    private static final String TRUSTSTORE_PASS = System.getProperty("crest.truststore.password", "888888");
    private static final int CONNECT_RETRIES = Integer.parseInt(System.getProperty("crest.rmi.connect.retries", "3"));
    private static final long CONNECT_RETRY_DELAY_MS = Long.parseLong(
            System.getProperty("crest.rmi.connect.retry.delay.ms", "300")
    );
    private static final int CALL_RETRIES = Integer.parseInt(System.getProperty("crest.rmi.call.retries", "3"));
    private static final long CALL_RETRY_DELAY_MS = Long.parseLong(
            System.getProperty("crest.rmi.call.retry.delay.ms", "250")
    );

    private Authorization service;
    private UserSession session;

    public void connect() throws Exception {
        if (SSL) {
            System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_PATH);
            System.setProperty("javax.net.ssl.trustStorePassword", TRUSTSTORE_PASS);
            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
            System.clearProperty("javax.net.ssl.keyStore");
            System.clearProperty("javax.net.ssl.keyStorePassword");
        }

        Exception last = null;
        for (int attempt = 1; attempt <= CONNECT_RETRIES; attempt++) {
            try {
                Registry reg = SSL
                        ? LocateRegistry.getRegistry(SERVER_IP, SERVER_PORT, new SslRMIClientSocketFactory())
                        : LocateRegistry.getRegistry(SERVER_IP, SERVER_PORT);
                service = (Authorization) reg.lookup("Authorization");
                service.ping();
                return;
            } catch (Exception e) {
                last = e;
                service = null;
                if (attempt == CONNECT_RETRIES) break;
                Thread.sleep(CONNECT_RETRY_DELAY_MS * attempt);
            }
        }
        throw last;
    }

    public UserRole login(String userId, String password) throws Exception {
        session = invokeWithRetry(() -> {
            ensureConnected();
            return service.login(userId, password);
        }, false);
        return session.getRole();
    }

    public void logout() throws Exception {
        if (session == null) {
            return;
        }
        invokeWithRetry(() -> {
            ensureConnected();
            service.logout(session);
            return null;
        }, true);
        session = null;
    }

    public String currentUserId() {
        return session == null ? null : session.getUserId();
    }

    public UserRole currentRole() {
        return session == null ? null : session.getRole();
    }

    public Employee registerEmployee(String firstName, String lastName, String icPassport, String initPassword) throws Exception {
        requireSession();
        return invokeWithRetry(() -> service.registerEmployee(session, firstName, lastName, icPassport, initPassword), true);
    }

    public String viewPendingLeaveApplications() throws Exception {
        requireSession();
        return invokeWithRetry(() -> service.viewPendingLeaveApplications(session), true);
    }

    public List<Employee> listEmployees() throws Exception {
        requireSession();
        return invokeWithRetry(() -> service.listEmployees(session), true);
    }

    public Employee getEmployeeById(String employeeId) throws Exception {
        requireSession();
        return invokeWithRetry(() -> service.getEmployeeById(session, employeeId), true);
    }

    public Employee updateEmployeeByHr(Employee updated) throws Exception {
        requireSession();
        return invokeWithRetry(() -> service.updateEmployeeByHr(session, updated), true);
    }

    public void deleteEmployee(String employeeId) throws Exception {
        requireSession();
        invokeWithRetry(() -> {
            service.deleteEmployee(session, employeeId);
            return null;
        }, true);
    }

    public void decideLeave(int leaveId, boolean approve) throws Exception {
        requireSession();
        invokeWithRetry(() -> {
            service.decideLeave(session, leaveId, approve);
            return null;
        }, true);
    }

    public String generateYearlyLeaveReport(String employeeId, int year) throws Exception {
        requireSession();
        return invokeWithRetry(() -> service.generateYearlyLeaveReport(session, employeeId, year), true);
    }

    public Employee getMyProfile() throws Exception {
        requireSession();
        return invokeWithRetry(() -> service.getMyProfile(session), true);
    }

    public Employee updateDetails(String phone, String emergencyName, String emergencyNo, String relationship) throws Exception {
        requireSession();
        Employee updated = new Employee();
        updated.setEmployeeId(session.getUserId());
        updated.setPhoneNo(phone);
        updated.setEmergencyName(emergencyName);
        updated.setEmergencyPhoneNo(emergencyNo);
        updated.setEmergencyRelationship(relationship);
        return invokeWithRetry(() -> service.updateDetails(session, updated), true);
    }

    public int leaveBalance() throws Exception {
        requireSession();
        return invokeWithRetry(() -> service.leaveBalance(session), true);
    }

    public int applyLeave(String leaveType, String startDate, String endDate, String reason) throws Exception {
        requireSession();
        return invokeWithRetry(() -> service.applyLeave(session, leaveType, startDate, endDate, reason), true);
    }

    public String viewMyLeaveApplications() throws Exception {
        requireSession();
        return invokeWithRetry(() -> service.viewMyLeaveApplications(session), true);
    }

    public String viewMyLeaveHistory() throws Exception {
        requireSession();
        return invokeWithRetry(() -> service.viewMyLeaveHistory(session), true);
    }

    private <T> T invokeWithRetry(RemoteCall<T> call, boolean recoverConnection) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= CALL_RETRIES; attempt++) {
            try {
                ensureConnected();
                return call.run();
            } catch (RemoteException e) {
                last = e;
                if (!recoverConnection || attempt == CALL_RETRIES) {
                    break;
                }
                service = null;
                Thread.sleep(CALL_RETRY_DELAY_MS * attempt);
                connect();
            }
        }
        throw last;
    }

    private void ensureConnected() throws Exception {
        if (service == null) {
            connect();
        }
    }

    private void requireSession() {
        if (session == null) {
            throw new IllegalStateException("Please login first.");
        }
    }

    @FunctionalInterface
    private interface RemoteCall<T> {
        T run() throws Exception;
    }
}
