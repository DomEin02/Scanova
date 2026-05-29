package dk.easv.scanova.BLL;

import dk.easv.scanova.Model.User;
import dk.easv.scanova.utils.PasswordUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserManagerTest {

    // User model

    // Test that a new user is active by default
    @Test
    void newUser_shouldBeActiveByDefault() {
        User user = new User("JohnDoe", "password", "User");
        assertTrue(user.isActive());
    }

    // Test that a user with role Admin is correctly identified as admin
    @Test
    void userWithAdminRole_shouldBeAdmin() {
        User user = new User(1, "admin", "pass", "Admin");
        assertTrue(user.isAdmin());
    }

    // Test that a user with role User is not identified as admin
    @Test
    void userWithUserRole_shouldNotBeAdmin() {
        User user = new User(1, "regular", "pass", "User");
        assertFalse(user.isAdmin());
    }

    // Test that role check is case-insensitive
    @Test
    void adminRoleCheck_shouldBeCaseInsensitive() {
        User user = new User(1, "admin", "pass", "admin");
        assertTrue(user.isAdmin());
    }

    // Test that setActive correctly deactivates a user (deleted users stay in DB)
    @Test
    void setActive_false_shouldDeactivateUser() {
        User user = new User(1, "JohnDoe", "pass", "User");
        user.setActive(false);
        assertFalse(user.isActive());
    }

    // Test that setActive can reactivate a deactivated user
    @Test
    void setActive_true_shouldReactivateUser() {
        User user = new User(1, "JohnDoe", "pass", "User");
        user.setActive(false);
        user.setActive(true);
        assertTrue(user.isActive());
    }

    // Password utility

    // Test that hashing a password produces a non-null result
    @Test
    void hashPassword_shouldReturnNonNull() {
        String hash = PasswordUtil.hash("Password1!");
        assertNotNull(hash);
    }

    // Test that hashing the same password twice gives different hashes (salted)
    @Test
    void hashPassword_samePlaintext_shouldGiveDifferentHashes() {
        String hash1 = PasswordUtil.hash("Password1!");
        String hash2 = PasswordUtil.hash("Password1!");
        assertNotEquals(hash1, hash2);
    }

    // Test that a correct password verifies successfully against its hash
    @Test
    void verifyPassword_correctPassword_shouldReturnTrue() {
        String plain = "Password1!";
        String hash  = PasswordUtil.hash(plain);
        assertTrue(PasswordUtil.verify(plain, hash));
    }

    // Test that a wrong password does not verify against a hash
    @Test
    void verifyPassword_wrongPassword_shouldReturnFalse() {
        String hash = PasswordUtil.hash("Password1!");
        assertFalse(PasswordUtil.verify("WrongPass1!", hash));
    }

    // Test that verifying with a null password returns false instead of throwing
    @Test
    void verifyPassword_nullPassword_shouldReturnFalse() {
        String hash = PasswordUtil.hash("Password1!");
        assertFalse(PasswordUtil.verify(null, hash));
    }

    // UserManager validation

    // Test that creating a user with an empty username throws
    @Test
    void createUser_emptyUsername_shouldThrow() {
        UserManager manager = new UserManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createUser("", "Password1!", "User"));
        assertEquals("Username cannot be empty.", ex.getMessage());
    }

    // Test that a username that is too short throws
    @Test
    void createUser_tooShortUsername_shouldThrow() {
        UserManager manager = new UserManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createUser("a", "Password1!", "User"));
        assertEquals("Username must be between 2 and 20 characters.", ex.getMessage());
    }

    // Test that a username with special characters throws
    @Test
    void createUser_usernameWithSpecialChars_shouldThrow() {
        UserManager manager = new UserManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createUser("john!", "Password1!", "User"));
        assertEquals("Username can only contain letters and numbers.", ex.getMessage());
    }

    // Test that a password without uppercase throws
    @Test
    void createUser_passwordWithoutUppercase_shouldThrow() {
        UserManager manager = new UserManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createUser("JohnDoe", "password1!", "User"));
        assertEquals("Password must contain at least one uppercase letter.", ex.getMessage());
    }

    // Test that a password without a number throws
    @Test
    void createUser_passwordWithoutNumber_shouldThrow() {
        UserManager manager = new UserManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createUser("JohnDoe", "Password!", "User"));
        assertEquals("Password must contain at least one number.", ex.getMessage());
    }

    // Test that a password without a special character throws
    @Test
    void createUser_passwordWithoutSpecialChar_shouldThrow() {
        UserManager manager = new UserManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createUser("JohnDoe", "Password1", "User"));
        assertEquals("Password must contain at least one special character (!@#$%^&* etc.).", ex.getMessage());
    }

    // Test that a password shorter than 8 characters throws
    @Test
    void createUser_tooShortPassword_shouldThrow() {
        UserManager manager = new UserManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createUser("JohnDoe", "Pa1!", "User"));
        assertEquals("Password must be at least 8 characters.", ex.getMessage());
    }

    // Test that an invalid role throws
    @Test
    void createUser_invalidRole_shouldThrow() {
        UserManager manager = new UserManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createUser("JohnDoe", "Password1!", "Superuser"));
        assertEquals("Role must be 'Admin' or 'User'.", ex.getMessage());
    }
}
