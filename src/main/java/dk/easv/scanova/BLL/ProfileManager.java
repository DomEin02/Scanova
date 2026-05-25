package dk.easv.scanova.BLL;

import dk.easv.scanova.DAL.ProfileDAO;
import dk.easv.scanova.Model.Profile;

import java.util.List;

public class ProfileManager {

    private final ProfileDAO profileDAO = new ProfileDAO();

    public List<Profile> getAllProfiles() throws Exception {
        return profileDAO.getAllProfiles();
    }

    public void createProfile(String name, float rotation,
                              float brightness, int clientId) throws Exception {
        validateName(name);
        validateRotation(rotation);
        validateBrightness(brightness);
        validateClient(clientId);
        profileDAO.createProfile(new Profile(name, rotation, brightness, clientId));
    }

    public void updateProfile(int id, String name, float rotation,
                              float brightness, int clientId) throws Exception {
        validateName(name);
        validateRotation(rotation);
        validateBrightness(brightness);
        validateClient(clientId);
        profileDAO.updateProfile(
                new Profile(id, name, rotation, brightness, clientId, true));
    }

    public void deleteProfile(int id) throws Exception {
        profileDAO.deleteProfile(id);
    }

    public List<Profile> getAllProfilesIncludingInactive() throws Exception {
        return profileDAO.getAllProfilesIncludingInactive();
    }

    public void reactivateProfile(int id) throws Exception {
        profileDAO.reactivateProfile(id);
    }

    private void validateName(String name) throws Exception {
        if (name == null || name.isBlank())
            throw new Exception("Profile name cannot be empty.");
        if (name.length() > 100)
            throw new Exception("Profile name cannot exceed 100 characters.");
    }

    private void validateRotation(float rotation) throws Exception {
        if (rotation < -180 || rotation > 180)
            throw new Exception("Rotation must be between -180 and 180 degrees.");
        if (rotation % 5 != 0)
            throw new Exception("Rotation must be in steps of 5 degrees.");
    }

    private void validateBrightness(float brightness) throws Exception {
        if (brightness < 0.1f || brightness > 3.0f)
            throw new Exception("Brightness must be between 0.1 and 3.0.");
    }

    private void validateClient(int clientId) throws Exception {
        if (clientId <= 0)
            throw new Exception("Please select a client for this profile.");
    }

}