package Server;

import java.sql.Connection;
import java.sql.Statement;

public class Database {

    public static void init() {
        try (Connection c = DatabaseSocket.getConnection();
             Statement st = c.createStatement()) {

            try {
                st.executeUpdate(
                    "CREATE TABLE USERS (" +
                    "USER_ID VARCHAR(64) PRIMARY KEY, " +
                    "ROLE VARCHAR(20) NOT NULL, " +
                    "SALT VARCHAR(200) NOT NULL, " +
                    "HASH VARCHAR(200) NOT NULL)"
                );
            } catch (Exception ignored) {}

            try {
                st.executeUpdate(
                    "CREATE TABLE EMPLOYEES (" +
                    "EMPLOYEE_ID VARCHAR(64) PRIMARY KEY, " +
                    "FIRST_NAME VARCHAR(50) NOT NULL, " +
                    "LAST_NAME VARCHAR(50) NOT NULL, " +
                    "IC_PASSPORT VARCHAR(30) NOT NULL UNIQUE)"
                );
            } catch (Exception ignored) {}

            try {
                st.executeUpdate(
                    "CREATE TABLE EMPLOYEE_DETAILS (" +
                    "EMPLOYEE_ID VARCHAR(64) PRIMARY KEY, " +
                    "PHONE VARCHAR(30), " +
                    "EMERGENCY_NAME VARCHAR(50), " +
                    "EMERGENCY_NO VARCHAR(30), " +
                    "EMERGENCY_RELATIONSHIP VARCHAR(30), " +
                    "FOREIGN KEY (EMPLOYEE_ID) REFERENCES EMPLOYEES(EMPLOYEE_ID))"
                );
            } catch (Exception ignored) {}

            try {
                st.executeUpdate(
                    "CREATE TABLE LEAVE_BALANCE (" +
                    "EMPLOYEE_ID VARCHAR(64), " +
                    "LEAVE_YEAR INT, " +
                    "BALANCE INT NOT NULL, " +
                    "PRIMARY KEY (EMPLOYEE_ID, LEAVE_YEAR), " +
                    "FOREIGN KEY (EMPLOYEE_ID) REFERENCES EMPLOYEES(EMPLOYEE_ID))"
                );
            } catch (Exception ignored) {}

            try {
                st.executeUpdate(
                    "CREATE TABLE LEAVE_APPLICATIONS (" +
                    "LEAVE_ID INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "EMPLOYEE_ID VARCHAR(64) NOT NULL, " +
                    "LEAVE_TYPE VARCHAR(30) NOT NULL, " +
                    "START_DATE DATE NOT NULL, " +
                    "END_DATE DATE NOT NULL, " +
                    "DAYS_REQUESTED INT NOT NULL, " +
                    "REASON VARCHAR(200), " +
                    "STATUS VARCHAR(20) NOT NULL, " +
                    "APPLIED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "DECIDED_BY VARCHAR(64), " +
                    "DECIDED_AT TIMESTAMP, " +
                    "CONSTRAINT CK_DATES CHECK (END_DATE >= START_DATE), " +
                    "CONSTRAINT CK_STATUS CHECK (STATUS IN ('PENDING','APPROVED','REJECTED')), " +
                    "FOREIGN KEY (EMPLOYEE_ID) REFERENCES EMPLOYEES(EMPLOYEE_ID))"
                );
            } catch (Exception ignored) {}

            System.out.println("Database ready (5 tables).");

        } catch (Exception e) {
            System.out.println("DB init error: " + e.getMessage());
        }
    }
}
