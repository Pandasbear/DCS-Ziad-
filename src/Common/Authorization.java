package Common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Authorization extends Remote {

    String ping() throws RemoteException;

    UserSession login(String userId, String password) throws RemoteException;
    void logout(UserSession session) throws RemoteException;

    Employee registerEmployee(UserSession session, String firstName, String lastName, String icPassport, String initPass)
            throws RemoteException;

    Employee updateDetails(UserSession session, Employee updated) throws RemoteException;
    int leaveBalance(UserSession session) throws RemoteException;
    Employee getMyProfile(UserSession session) throws RemoteException;


    int applyLeave(UserSession session,
                   String leaveType,
                   String startDateYYYYMMDD,
                   String endDateYYYYMMDD,
                   String reason) throws RemoteException;

    String viewMyLeaveApplications(UserSession session) throws RemoteException;
    String viewMyLeaveHistory(UserSession session) throws RemoteException;


    String viewPendingLeaveApplications(UserSession session) throws RemoteException;
    List<Employee> listEmployees(UserSession session) throws RemoteException;
    Employee getEmployeeById(UserSession session, String employeeId) throws RemoteException;
    Employee updateEmployeeByHr(UserSession session, Employee updated) throws RemoteException;
    void deleteEmployee(UserSession session, String employeeId) throws RemoteException;

    void decideLeave(UserSession session,
                     int leaveId,
                     boolean approve) throws RemoteException;
    String generateYearlyLeaveReport(UserSession session, String employeeId, int year) throws RemoteException;
}
