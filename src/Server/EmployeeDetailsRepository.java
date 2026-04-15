package Server;

import Common.Employee;
import java.sql.*;

public class EmployeeDetailsRepository {

    public void createIfNotExists(String employeeId) throws SQLException {
        String check = "SELECT 1 FROM EMPLOYEE_DETAILS WHERE EMPLOYEE_ID = ?";

        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(check)) {

            ps.setString(1, employeeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return;
        }

        String insert = "INSERT INTO EMPLOYEE_DETAILS (EMPLOYEE_ID) VALUES (?)";

        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(insert)) {

            ps.setString(1, employeeId);
            ps.executeUpdate();
        }
    }

    public void updateFromEmployee(Employee e) throws SQLException {
        try (Connection c = DatabaseSocket.getConnection()) {
            updateFromEmployee(c, e);
        }
    }

    public void updateFromEmployee(Connection c, Employee e) throws SQLException {
        createIfNotExists(c, e.getEmployeeId());

        String sql = "UPDATE EMPLOYEE_DETAILS " +
                "SET PHONE = ?, EMERGENCY_NAME = ?, EMERGENCY_NO = ?, EMERGENCY_RELATIONSHIP = ? " +
                "WHERE EMPLOYEE_ID = ?";

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, e.getPhoneNo());
            ps.setString(2, e.getEmergencyName());
            ps.setString(3, e.getEmergencyNo());
            ps.setString(4, e.getEmergencyRelationship());
            ps.setString(5, e.getEmployeeId());

            ps.executeUpdate();
        }
    }

    public void createIfNotExists(Connection c, String employeeId) throws SQLException {
        String check = "SELECT 1 FROM EMPLOYEE_DETAILS WHERE EMPLOYEE_ID = ?";
        try (PreparedStatement ps = c.prepareStatement(check)) {
            ps.setString(1, employeeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return;
        }

        String insert = "INSERT INTO EMPLOYEE_DETAILS (EMPLOYEE_ID) VALUES (?)";
        try (PreparedStatement ps = c.prepareStatement(insert)) {
            ps.setString(1, employeeId);
            ps.executeUpdate();
        }
    }

    public DetailsRow find(String employeeId) throws SQLException {
        String sql = "SELECT PHONE, EMERGENCY_NAME, EMERGENCY_NO, EMERGENCY_RELATIONSHIP " +
                     "FROM EMPLOYEE_DETAILS WHERE EMPLOYEE_ID = ?";

        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, employeeId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            return new DetailsRow(
                    rs.getString("PHONE"),
                    rs.getString("EMERGENCY_NAME"),
                    rs.getString("EMERGENCY_NO"),
                    rs.getString("EMERGENCY_RELATIONSHIP")
            );
        }
    }

    public void deleteByEmployee(Connection c, String employeeId) throws SQLException {
        String sql = "DELETE FROM EMPLOYEE_DETAILS WHERE EMPLOYEE_ID = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            ps.executeUpdate();
        }
    }

    public static class DetailsRow {
        public final String phone;
        public final String emergencyName;
        public final String emergencyNo;
        public final String emergencyRelationship;

        public DetailsRow(String phone, String emergencyName, String emergencyNo, String emergencyRelationship) {
            this.phone = phone;
            this.emergencyName = emergencyName;
            this.emergencyNo = emergencyNo;
            this.emergencyRelationship = emergencyRelationship;
        }
    }
}
