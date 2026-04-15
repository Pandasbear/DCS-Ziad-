package Server;

import Common.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Session {

    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    private static final long SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes

    public UserSession create(UserRole role, String employeeId) {
        String token = UUID.randomUUID().toString();
        UserSession session = new UserSession(token, role, employeeId);
        sessions.put(token, session);
        return session;
    }

    public UserSession require(UserSession session) {
        if (session == null)
            throw new SecurityException("No session found!");

        UserSession stored = sessions.get(session.getTokenSession());

        if (stored == null)
            throw new SecurityException("Session invalid or expired.");

        long now = System.currentTimeMillis();
        long age = now - stored.getCreatedTime();

        if (age > SESSION_TIMEOUT) {
            sessions.remove(stored.getTokenSession());
            throw new SecurityException("Session expired. Please login again.");
        }

        return stored;
    }

    public void remove(UserSession session) {
        if (session != null)
            sessions.remove(session.getTokenSession());
    }
}
