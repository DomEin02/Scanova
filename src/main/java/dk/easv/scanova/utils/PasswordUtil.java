package dk.easv.scanova.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    private static final int COST = 12;

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(COST));
    }

    public static boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) return false;
        if (!hashedPassword.startsWith("$2")) return false;
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    private PasswordUtil() {}
}