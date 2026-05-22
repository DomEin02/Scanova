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

    public List<String[]> getFilteredLogs(List<String> types, String fromDate, String toDate) throws Exception {
        return logDAO.getFilteredLogs(types, fromDate, toDate);
    }

    // Map action string to a display category
    public static String getCategory(String action) {
        if (action == null) return "OTHER";
        return switch (action) {
            case "LOGIN_SUCCESS", "LOGIN_FAILED" -> "LOGIN";
            case "SCAN_COMPLETE", "FILE_DELETED" -> "SCANNING";
            case "USER_CREATED", "USER_DEACTIVATED",
                 "USER_REACTIVATED", "CLIENT_CREATED",
                 "CLIENT_UPDATED", "CLIENT_DEACTIVATED",
                 "CLIENT_REACTIVATED" -> "MANAGEMENT";
            case "ERROR" -> "ERROR";
            default -> "OTHER";
        };
    }
}