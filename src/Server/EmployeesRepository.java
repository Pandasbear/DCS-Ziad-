package Server;

import Common.Employee;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeesRepository {

    public void insertBasic(Employee e) throws SQLException {
        String sql = "INSERT INTO EMPLOYEES (EMPLOYEE_ID, FIRST_NAME, LAST_NAME, IC_PASSPORT) VALUES (?, ?, ?, ?)";

        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, e.getEmployeeId());
            ps.setString(2, e.getFirstName());
            ps.setString(3, e.getLastName());
            ps.setString(4, e.getIcPassport());

            ps.executeUpdate();
        }
    }

    public Employee findBasic(String employeeId) throws SQLException {
        String sql = "SELECT EMPLOYEE_ID, FIRST_NAME, LAST_NAME, IC_PASSPORT FROM EMPLOYEES WHERE EMPLOYEE_ID = ?";

        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, employeeId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            Employee e = new Employee(
                    rs.getString("FIRST_NAME"),
                    rs.getString("LAST_NAME"),
                    rs.getString("IC_PASSPORT")
            );
            e.setEmployeeId(rs.getString("EMPLOYEE_ID"));
            return e;
        }
    }

    public boolean icPassportExists(String icPassport) throws SQLException {
        String sql = "SELECT 1 FROM EMPLOYEES WHERE IC_PASSPORT = ?";

        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, icPassport);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    public List<String> findIdsByFullName(String fullName) throws SQLException {
        String sql =
                "SELECT EMPLOYEE_ID FROM EMPLOYEES " +
                "WHERE UPPER(TRIM(FIRST_NAME || ' ' || LAST_NAME)) = ? " +
                "ORDER BY EMPLOYEE_ID";

        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, fullName.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(rs.getString("EMPLOYEE_ID"));
                }
                return out;
            }
        }
    }

    public List<Employee> listAllBasic() throws SQLException {
        String sql = "SELECT EMPLOYEE_ID, FIRST_NAME, LAST_NAME, IC_PASSPORT FROM EMPLOYEES ORDER BY FIRST_NAME, LAST_NAME";

        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Employee> out = new ArrayList<>();
            while (rs.next()) {
                Employee e = new Employee(
                        rs.getString("FIRST_NAME"),
                        rs.getString("LAST_NAME"),
                        rs.getString("IC_PASSPORT")
                );
                e.setEmployeeId(rs.getString("EMPLOYEE_ID"));
                out.add(e);
            }
            return out;
        }
    }

    public void updateBasic(Employee e) throws SQLException {
        String sql = "UPDATE EMPLOYEES SET FIRST_NAME = ?, LAST_NAME = ?, IC_PASSPORT = ? WHERE EMPLOYEE_ID = ?";

        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, e.getFirstName());
            ps.setString(2, e.getLastName());
            ps.setString(3, e.getIcPassport());
            ps.setString(4, e.getEmployeeId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new SQLException("Employee not found for update: " + e.getEmployeeId());
            }
        }
    }

    public void deleteById(Connection c, String employeeId) throws SQLException {
        String sql = "DELETE FROM EMPLOYEES WHERE EMPLOYEE_ID = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            ps.executeUpdate();
        }
    }
}
