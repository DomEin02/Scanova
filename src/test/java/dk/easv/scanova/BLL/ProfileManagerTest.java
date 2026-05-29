package dk.easv.scanova.BLL;

import dk.easv.scanova.Model.Profile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileManagerTest {

    // Profile model

    // Test that a profile stores its name correctly
    @Test
    void newProfile_shouldStoreNameCorrectly() {
        Profile profile = new Profile("Default", 0f, 1.0f, 1);
        assertEquals("Default", profile.getName());
    }

    // Test that a profile stores its rotation correctly
    @Test
    void newProfile_shouldStoreRotationCorrectly() {
        Profile profile = new Profile("Rotated", 90f, 1.0f, 1);
        assertEquals(90f, profile.getRotation());
    }

    // Test that a profile stores its brightness correctly
    @Test
    void newProfile_shouldStoreBrightnessCorrectly() {
        Profile profile = new Profile("Bright", 0f, 1.5f, 1);
        assertEquals(1.5f, profile.getBrightness());
    }

    // Test that a profile stores its client ID correctly
    @Test
    void newProfile_shouldStoreClientIdCorrectly() {
        Profile profile = new Profile("ClientProfile", 0f, 1.0f, 42);
        assertEquals(42, profile.getClientId());
    }

    // Test that toString returns the profile name
    @Test
    void profileToString_shouldReturnName() {
        Profile profile = new Profile("MyProfile", 0f, 1.0f, 1);
        assertEquals("MyProfile", profile.toString());
    }

    // ProfileManager validation

    // Test that creating a profile with an empty name throws
    @Test
    void createProfile_emptyName_shouldThrow() {
        ProfileManager manager = new ProfileManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createProfile("", 0f, 1.0f, 1));
        assertEquals("Profile name cannot be empty.", ex.getMessage());
    }

    // Test that creating a profile with a name over 100 chars throws
    @Test
    void createProfile_nameTooLong_shouldThrow() {
        ProfileManager manager = new ProfileManager();
        String longName = "A".repeat(101);
        Exception ex = assertThrows(Exception.class,
                () -> manager.createProfile(longName, 0f, 1.0f, 1));
        assertEquals("Profile name cannot exceed 100 characters.", ex.getMessage());
    }

    // Test that rotation below -180 throws
    @Test
    void createProfile_rotationTooLow_shouldThrow() {
        ProfileManager manager = new ProfileManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createProfile("Test", -181f, 1.0f, 1));
        assertEquals("Rotation must be between -180 and 180 degrees.", ex.getMessage());
    }

    // Test that rotation above 180 throws
    @Test
    void createProfile_rotationTooHigh_shouldThrow() {
        ProfileManager manager = new ProfileManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createProfile("Test", 181f, 1.0f, 1));
        assertEquals("Rotation must be between -180 and 180 degrees.", ex.getMessage());
    }

    // Test that rotation not in steps of 5 throws
    @Test
    void createProfile_rotationNotMultipleOf5_shouldThrow() {
        ProfileManager manager = new ProfileManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createProfile("Test", 7f, 1.0f, 1));
        assertEquals("Rotation must be in steps of 5 degrees.", ex.getMessage());
    }

    // Test that rotation boundaries -180 and 180 are stored correctly on the model
    @Test
    void profileRotation_atBoundaries_shouldBeStoredCorrectly() {
        Profile low  = new Profile("Low",  -180f, 1.0f, 1);
        Profile high = new Profile("High",  180f, 1.0f, 1);
        assertEquals(-180f, low.getRotation());
        assertEquals( 180f, high.getRotation());
    }

    // Test that brightness below 0.1 throws
    @Test
    void createProfile_brightnessTooLow_shouldThrow() {
        ProfileManager manager = new ProfileManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createProfile("Test", 0f, 0.09f, 1));
        assertEquals("Brightness must be between 0.1 and 3.0.", ex.getMessage());
    }

    // Test that brightness above 3.0 throws
    @Test
    void createProfile_brightnessTooHigh_shouldThrow() {
        ProfileManager manager = new ProfileManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createProfile("Test", 0f, 3.01f, 1));
        assertEquals("Brightness must be between 0.1 and 3.0.", ex.getMessage());
    }

    // Test that a clientId of 0 throws
    @Test
    void createProfile_invalidClientId_shouldThrow() {
        ProfileManager manager = new ProfileManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createProfile("Test", 0f, 1.0f, 0));
        assertEquals("Please select a client for this profile.", ex.getMessage());
    }

    // Test that a negative clientId throws
    @Test
    void createProfile_negativeClientId_shouldThrow() {
        ProfileManager manager = new ProfileManager();
        Exception ex = assertThrows(Exception.class,
                () -> manager.createProfile("Test", 0f, 1.0f, -1));
        assertEquals("Please select a client for this profile.", ex.getMessage());
    }

    // Log category mapping

    // Test that login success maps to LOGIN category
    @Test
    void getCategory_loginSuccess_shouldReturnLogin() {
        assertEquals("LOGIN", LogManager.getCategory("LOGIN_SUCCESS"));
    }

    // Test that login failed maps to LOGIN category
    @Test
    void getCategory_loginFailed_shouldReturnLogin() {
        assertEquals("LOGIN", LogManager.getCategory("LOGIN_FAILED"));
    }

    // Test that scan complete maps to SCANNING category
    @Test
    void getCategory_scanComplete_shouldReturnScanning() {
        assertEquals("SCANNING", LogManager.getCategory("SCAN_COMPLETE"));
    }

    // Test that user created maps to MANAGEMENT category
    @Test
    void getCategory_userCreated_shouldReturnManagement() {
        assertEquals("MANAGEMENT", LogManager.getCategory("USER_CREATED"));
    }

    // Test that archive deactivated maps to MANAGEMENT category
    @Test
    void getCategory_archiveDeactivated_shouldReturnManagement() {
        assertEquals("MANAGEMENT", LogManager.getCategory("ARCHIVE_DEACTIVATED"));
    }

    // Test that ERROR maps to ERROR category
    @Test
    void getCategory_error_shouldReturnError() {
        assertEquals("ERROR", LogManager.getCategory("ERROR"));
    }

    // Test that an unknown action maps to OTHER
    @Test
    void getCategory_unknownAction_shouldReturnOther() {
        assertEquals("OTHER", LogManager.getCategory("SOMETHING_UNKNOWN"));
    }

    // Test that null action maps to OTHER without throwing
    @Test
    void getCategory_nullAction_shouldReturnOther() {
        assertEquals("OTHER", LogManager.getCategory(null));
    }
}
