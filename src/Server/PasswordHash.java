package Server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHash {
    private static final SecureRandom rng = new SecureRandom();
    private static final int PBKDF2_ITERATIONS = Integer.parseInt(
            System.getProperty("crest.security.pbkdf2.iterations", "210000")
    );
    private static final int PBKDF2_KEY_BITS = 256;
    private static final String PBKDF2_ALGO = "PBKDF2WithHmacSHA256";
    private static final String PBKDF2_PREFIX = "PBKDF2$";

    public static String newSalt() {
        byte[] salt = new byte[16];
        rng.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String saltB64, String password) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltB64);
            byte[] digest = pbkdf2(salt, password, PBKDF2_ITERATIONS);
            return PBKDF2_PREFIX + PBKDF2_ITERATIONS + "$" + Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verification(String saltB64, String expectedHashB64, String password) {
        if (expectedHashB64 != null && expectedHashB64.startsWith(PBKDF2_PREFIX)) {
            return verifyPbkdf2(saltB64, expectedHashB64, password);
        }
        return legacySha256(saltB64, password).equals(expectedHashB64);
    }

    public static boolean isLegacyHash(String hash) {
        return hash == null || !hash.startsWith(PBKDF2_PREFIX);
    }

    private static boolean verifyPbkdf2(String saltB64, String expectedHash, String password) {
        try {
            String[] parts = expectedHash.split("\\$");
            if (parts.length != 3 || !"PBKDF2".equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(saltB64);
            byte[] digest = pbkdf2(salt, password, iterations);
            String actual = Base64.getEncoder().encodeToString(digest);
            return actual.equals(parts[2]);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(byte[] salt, String password, int iterations) throws Exception {
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGO);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, PBKDF2_KEY_BITS);
        return skf.generateSecret(spec).getEncoded();
    }

    private static String legacySha256(String saltB64, String password) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltB64);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PasswordHash() {}
}
