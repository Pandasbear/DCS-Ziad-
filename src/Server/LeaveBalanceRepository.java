package Server;

import java.sql.*;

public class LeaveBalanceRepository {

    public int getYearBalanceOrCreate(String employeeId, int year, int defaultBalance) throws SQLException {
        try (Connection c = DatabaseSocket.getConnection()) {
            return getYearBalanceOrCreate(c, employeeId, year, defaultBalance);
        }
    }

    public int getYearBalanceOrCreate(Connection c, String employeeId, int year, int defaultBalance) throws SQLException {
        String check = "SELECT BALANCE FROM LEAVE_BALANCE WHERE EMPLOYEE_ID = ? AND LEAVE_YEAR = ?";

        try (PreparedStatement ps = c.prepareStatement(check)) {

            ps.setString(1, employeeId);
            ps.setInt(2, year);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("BALANCE");
            }
        }

        String insert = "INSERT INTO LEAVE_BALANCE (EMPLOYEE_ID, LEAVE_YEAR, BALANCE) VALUES (?, ?, ?)";

        try (PreparedStatement ps = c.prepareStatement(insert)) {

            ps.setString(1, employeeId);
            ps.setInt(2, year);
            ps.setInt(3, defaultBalance);

            ps.executeUpdate();
        }

        return defaultBalance;
    }

    public void setBalance(String employeeId, int year, int newBalance) throws SQLException {
        String sql = "UPDATE LEAVE_BALANCE SET BALANCE = ? WHERE EMPLOYEE_ID = ? AND LEAVE_YEAR = ?";

        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, newBalance);
            ps.setString(2, employeeId);
            ps.setInt(3, year);

            ps.executeUpdate();
        }
    }

    public boolean decreaseIfEnough(String employeeId, int year, int amount) throws SQLException {
        try (Connection c = DatabaseSocket.getConnection()) {
            return decreaseIfEnough(c, employeeId, year, amount);
        }
    }

    public boolean decreaseIfEnough(Connection c, String employeeId, int year, int amount) throws SQLException {
        String sql =
                "UPDATE LEAVE_BALANCE " +
                "SET BALANCE = BALANCE - ? " +
                "WHERE EMPLOYEE_ID = ? AND LEAVE_YEAR = ? AND BALANCE >= ?";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setString(2, employeeId);
            ps.setInt(3, year);
            ps.setInt(4, amount);
            return ps.executeUpdate() == 1;
        }
    }

    public void increaseBalance(String employeeId, int year, int amount) throws SQLException {
        try (Connection c = DatabaseSocket.getConnection()) {
            increaseBalance(c, employeeId, year, amount);
        }
    }

    public void increaseBalance(Connection c, String employeeId, int year, int amount) throws SQLException {
        String sql =
                "UPDATE LEAVE_BALANCE " +
                "SET BALANCE = BALANCE + ? " +
                "WHERE EMPLOYEE_ID = ? AND LEAVE_YEAR = ?";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setString(2, employeeId);
            ps.setInt(3, year);
            ps.executeUpdate();
        }
    }

    public boolean hasRow(String employeeId, int year) throws SQLException {
        String sql = "SELECT 1 FROM LEAVE_BALANCE WHERE EMPLOYEE_ID = ? AND LEAVE_YEAR = ?";
        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            ps.setInt(2, year);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    public void deleteByEmployee(Connection c, String employeeId) throws SQLException {
        String sql = "DELETE FROM LEAVE_BALANCE WHERE EMPLOYEE_ID = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            ps.executeUpdate();
        }
    }
}
