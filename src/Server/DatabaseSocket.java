

package Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseSocket {

    private static final String DB_HOST = System.getProperty("crest.db.host", "127.0.0.1");
    private static final int DB_PORT = intProp("crest.db.port", 1527);
    private static final String DB_NAME = System.getProperty("crest.db.name", "crestDB");
    private static final boolean DB_CREATE = Boolean.parseBoolean(
            System.getProperty("crest.db.create", "true")
    );
    private static final int CONNECT_RETRIES = intProp("crest.db.connect.retries", 3);
    private static final long CONNECT_RETRY_DELAY_MS = longProp("crest.db.connect.retry.delay.ms", 250L);


    private static final String URL =
            "jdbc:derby://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + (DB_CREATE ? ";create=true" : "");

    static {
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Derby ClientDriver not found. Add derbyclient.jar", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        SQLException last = null;
        for (int attempt = 1; attempt <= CONNECT_RETRIES; attempt++) {
            try {
                return DriverManager.getConnection(URL);
            } catch (SQLException e) {
                last = e;
                if (attempt == CONNECT_RETRIES) break;
                sleepQuietly(CONNECT_RETRY_DELAY_MS * attempt);
            }
        }
        throw last;
    }

    private static int intProp(String name, int defaultValue) {
        try {
            return Integer.parseInt(System.getProperty(name, String.valueOf(defaultValue)));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static long longProp(String name, long defaultValue) {
        try {
            return Long.parseLong(System.getProperty(name, String.valueOf(defaultValue)));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
