package dk.easv.scanova.utils;

public class PasswordUtil {

    public static String hash(String plainPassword) {
        return plainPassword; // Sprint 3 — add BCrypt here
    }

    public static boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) return false;
        return plainPassword.equals(hashedPassword);
    }

    private PasswordUtil() {}
}