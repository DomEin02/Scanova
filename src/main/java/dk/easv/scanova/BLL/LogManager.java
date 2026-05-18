package dk.easv.scanova.BLL;

import dk.easv.scanova.DAL.LogDAO;
import java.util.List;

public class LogManager {

    private final LogDAO logDAO = new LogDAO();

    public void log(String action, int userId, String details) {
        try {
            logDAO.insertLog(action, userId, details);
        } catch (Exception e) {
            System.out.println("Could not write log: " + e.getMessage());
        }
    }

    public List<String[]> getAllLogs() throws Exception {
        return logDAO.getAllLogs();
    }

    public List<String[]> getLogsByType(String action) throws Exception {
        return logDAO.getLogsByType(action);
    }
}