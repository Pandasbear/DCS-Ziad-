package Server;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LeaveApplicationsRepository {

    public static class LeaveRow {
        public int leaveId;
        public String employeeId;
        public String leaveType;
        public Date startDate;
        public Date endDate;
        public int daysRequested;
        public String reason;
        public String status;
        public Timestamp appliedAt;
        public String decidedBy;
        public Timestamp decidedAt;
    }

    public int insertApplication(String employeeId,
                                 String leaveType,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 int daysRequested,
                                 String reason) throws SQLException {

        String sql =
                "INSERT INTO LEAVE_APPLICATIONS " +
                "(EMPLOYEE_ID, LEAVE_TYPE, START_DATE, END_DATE, DAYS_REQUESTED, REASON, STATUS) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'PENDING')";

        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, employeeId);
            ps.setString(2, leaveType);
            ps.setDate(3, Date.valueOf(startDate));
            ps.setDate(4, Date.valueOf(endDate));
            ps.setInt(5, daysRequested);
            ps.setString(6, reason);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Failed to create leave application (no generated key).");
    }

    public LeaveRow findById(int leaveId) throws SQLException {
        try (Connection c = DatabaseSocket.getConnection()) {
            return findById(c, leaveId);
        }
    }

    public LeaveRow findById(Connection c, int leaveId) throws SQLException {
        String sql = "SELECT * FROM LEAVE_APPLICATIONS WHERE LEAVE_ID = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, leaveId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public List<LeaveRow> listByEmployee(String employeeId) throws SQLException {
        String sql = "SELECT * FROM LEAVE_APPLICATIONS WHERE EMPLOYEE_ID = ? ORDER BY APPLIED_AT DESC";
        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                List<LeaveRow> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public List<LeaveRow> listHistoryByEmployee(String employeeId) throws SQLException {
        String sql =
                "SELECT * FROM LEAVE_APPLICATIONS " +
                "WHERE EMPLOYEE_ID = ? AND STATUS IN ('APPROVED','REJECTED') " +
                "ORDER BY DECIDED_AT DESC";
        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                List<LeaveRow> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public List<LeaveRow> listPending() throws SQLException {
        String sql = "SELECT * FROM LEAVE_APPLICATIONS WHERE STATUS = 'PENDING' ORDER BY APPLIED_AT ASC";
        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<LeaveRow> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        }
    }

    public void setDecision(int leaveId, String status, String decidedBy) throws SQLException {
        String sql =
                "UPDATE LEAVE_APPLICATIONS " +
                "SET STATUS = ?, DECIDED_BY = ?, DECIDED_AT = CURRENT_TIMESTAMP " +
                "WHERE LEAVE_ID = ?";

        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, decidedBy);
            ps.setInt(3, leaveId);
            ps.executeUpdate();
        }
    }

    public boolean setDecisionIfPending(int leaveId, String status, String decidedBy) throws SQLException {
        try (Connection c = DatabaseSocket.getConnection()) {
            return setDecisionIfPending(c, leaveId, status, decidedBy);
        }
    }

    public boolean setDecisionIfPending(Connection c, int leaveId, String status, String decidedBy) throws SQLException {
        String sql =
                "UPDATE LEAVE_APPLICATIONS " +
                "SET STATUS = ?, DECIDED_BY = ?, DECIDED_AT = CURRENT_TIMESTAMP " +
                "WHERE LEAVE_ID = ? AND STATUS = 'PENDING'";

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, decidedBy);
            ps.setInt(3, leaveId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean hasOverlappingApplication(String employeeId, LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql =
                "SELECT 1 FROM LEAVE_APPLICATIONS " +
                "WHERE EMPLOYEE_ID = ? " +
                "AND STATUS IN ('PENDING','APPROVED') " +
                "AND START_DATE <= ? " +
                "AND END_DATE >= ?";

        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, employeeId);
            ps.setDate(2, Date.valueOf(endDate));
            ps.setDate(3, Date.valueOf(startDate));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private LeaveRow map(ResultSet rs) throws SQLException {
        LeaveRow r = new LeaveRow();
        r.leaveId = rs.getInt("LEAVE_ID");
        r.employeeId = rs.getString("EMPLOYEE_ID");
        r.leaveType = rs.getString("LEAVE_TYPE");
        r.startDate = rs.getDate("START_DATE");
        r.endDate = rs.getDate("END_DATE");
        r.daysRequested = rs.getInt("DAYS_REQUESTED");
        r.reason = rs.getString("REASON");
        r.status = rs.getString("STATUS");
        r.appliedAt = rs.getTimestamp("APPLIED_AT");
        r.decidedBy = rs.getString("DECIDED_BY");
        r.decidedAt = rs.getTimestamp("DECIDED_AT");
        return r;
    }
    
    public List<LeaveRow> listApprovedByEmployeeAndYear(String employeeId, int year) throws SQLException {

        Date from = Date.valueOf(LocalDate.of(year, 1, 1));
        Date to = Date.valueOf(LocalDate.of(year, 12, 31));

        String sql =
                "SELECT * FROM LEAVE_APPLICATIONS " +
                "WHERE EMPLOYEE_ID = ? " +
                "AND STATUS = 'APPROVED' " +
                "AND START_DATE >= ? AND START_DATE <= ? " +
                "ORDER BY START_DATE ASC";

        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, employeeId);
            ps.setDate(2, from);
            ps.setDate(3, to);

            try (ResultSet rs = ps.executeQuery()) {
                List<LeaveRow> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public void deleteByEmployee(Connection c, String employeeId) throws SQLException {
        String sql = "DELETE FROM LEAVE_APPLICATIONS WHERE EMPLOYEE_ID = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, employeeId);
            ps.executeUpdate();
        }
    }
}
