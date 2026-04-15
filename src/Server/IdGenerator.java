package Server;

import java.util.UUID;

public class IdGenerator {

    public static String nextEmployeeCounter() {
        return uuidToken();
    }

    public static String nextHumanResourceCounter() {
        return uuidToken();
    }

    private static String uuidToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6).toLowerCase();
    }
}
