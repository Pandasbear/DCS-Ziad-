package Server;

import Common.UserRole;
import java.sql.*;

public class UsersRepository {

    public boolean exists(String userId) throws SQLException {
        String sql = "SELECT 1 FROM USERS WHERE USER_ID = ?";
        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    public void insert(String userId, UserRole role, String salt, String hash) throws SQLException {
        String sql = "INSERT INTO USERS (USER_ID, ROLE, SALT, HASH) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setString(2, role.name());
            ps.setString(3, salt);
            ps.setString(4, hash);

            ps.executeUpdate();
        }
    }

    public UserRow find(String userId) throws SQLException {
        String sql = "SELECT USER_ID, ROLE, SALT, HASH FROM USERS WHERE USER_ID = ?";
        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            return new UserRow(
                    rs.getString("USER_ID"),
                    UserRole.valueOf(rs.getString("ROLE")),
                    rs.getString("SALT"),
                    rs.getString("HASH")
            );
        }
    }

    public void updateCredentials(String userId, String salt, String hash) throws SQLException {
        String sql = "UPDATE USERS SET SALT = ?, HASH = ? WHERE USER_ID = ?";
        try (Connection c = DatabaseSocket.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, salt);
            ps.setString(2, hash);
            ps.setString(3, userId);
            ps.executeUpdate();
        }
    }

    public void deleteById(Connection c, String userId) throws SQLException {
        String sql = "DELETE FROM USERS WHERE USER_ID = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        }
    }

    public static class UserRow {
        public final String userId;
        public final UserRole role;
        public final String salt;
        public final String hash;

        public UserRow(String userId, UserRole role, String salt, String hash) {
            this.userId = userId;
            this.role = role;
            this.salt = salt;
            this.hash = hash;
        }
    }
}
