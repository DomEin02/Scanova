package dk.easv.scanova.BLL;

import dk.easv.scanova.DAL.UserDAO;
import dk.easv.scanova.Model.User;
import dk.easv.scanova.utils.PasswordUtil;
import java.util.List;

public class UserManager {

    private final UserDAO userDAO;

    public UserManager() throws Exception {
        this.userDAO = new UserDAO();
    }

    public User login(String username, String password) throws Exception {
        if (username == null || username.isBlank()) return null;
        if (password == null || password.isBlank()) return null;

        User user = userDAO.getUserByUsername(username);

        if (user == null) return null;
        if (!PasswordUtil.verify(password, user.getPassword())) return null;
        return user;
    }

    public List<User> getAllUsers() throws Exception {
        return userDAO.getAllUsers();
    }

    public void createUser(String username, String password, String role) throws Exception {
        validateUsername(username);
        validatePassword(password);
        validateRole(role);


        String hashedPassword = PasswordUtil.hash(password);


        User newUser = new User(username, hashedPassword, role);
        userDAO.createUser(newUser);
    }

    public void updateUser(int id, String username, String newPassword, String role) throws Exception {
        // --- Validation ---
        validateUsername(username);
        validateRole(role);


        List<User> all = userDAO.getAllUsers();
        User existing = all.stream()
                .filter(u -> u.getId() == id)
                .findFirst()
                .orElseThrow(() -> new Exception("User not found with id: " + id));


        String passwordToSave;
        if (newPassword == null || newPassword.isBlank()) {
            passwordToSave = existing.getPassword(); // behold gamle hash
        } else {
            validatePassword(newPassword);
            passwordToSave = PasswordUtil.hash(newPassword); // hash den nye
        }

        existing.setUsername(username);
        existing.setPassword(passwordToSave);
        existing.setRole(role);

        userDAO.updateUser(existing);
    }

    public void deleteUser(int userId) throws Exception {
        userDAO.deleteUser(userId);
    }

    private void validateUsername(String username) throws Exception {
        if (username == null || username.isBlank())
            throw new Exception("Username cannot be empty.");
        if (username.length() < 2 || username.length() > 20)
            throw new Exception("Username must be between 2 and 20 characters.");
        if (!username.matches("[a-zA-Z0-9]+"))
            throw new Exception("Username can only contain letters and numbers.");
    }

    private void validatePassword(String password) throws Exception {
        if (password == null || password.isBlank())
            throw new Exception("Password cannot be empty.");
        if (password.length() < 6)
            throw new Exception("Password must be at least 6 characters.");
    }

    private void validateRole(String role) throws Exception {
        if (!"Admin".equals(role) && !"User".equals(role))
            throw new Exception("Role must be either 'Admin' or 'User'.");
    }
}
