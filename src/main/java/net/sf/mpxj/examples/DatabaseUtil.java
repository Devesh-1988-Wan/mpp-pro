package net.sf.mpxj.examples;

import net.sf.mpxj.Project;
import net.sf.mpxj.Task;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

public class DatabaseUtil {

    // Database connection details for SQL Server
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=mpp_pro";
    private static final String DB_USER = "your_username";
    private static final String DB_PASSWORD = "your_password";

    /**
     * Establishes a connection to the database.
     *
     * @return A Connection object.
     * @throws SQLException if a database access error occurs.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Updates the name of a project in the database.
     *
     * @param projectId The ID of the project to update.
     * @param newProjectName The new name for the project.
     */
    public void updateProjectName(int projectId, String newProjectName) {
        String sql = "UPDATE projects SET project_name = ? WHERE project_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newProjectName);
            pstmt.setInt(2, projectId);
            pstmt.executeUpdate();
            System.out.println("Project name updated successfully.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Imports a new task into the database.
     *
     * @param task The Task object to import.
     * @param projectId The ID of the project this task belongs to.
     */
    public void importTask(Task task, int projectId) {
        String sql = "INSERT INTO tasks(project_id, task_name, start_date, finish_date, duration) VALUES(?,?,?,?,?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, projectId);
            pstmt.setString(2, task.getName());

            // Handle nullable dates
            if (task.getStart() != null) {
                pstmt.setDate(3, new java.sql.Date(task.getStart().getTime()));
            } else {
                pstmt.setNull(3, java.sql.Types.DATE);
            }

            if (task.getFinish() != null) {
                pstmt.setDate(4, new java.sql.Date(task.getFinish().getTime()));
            } else {
                pstmt.setNull(4, java.sql.Types.DATE);
            }

            pstmt.setString(5, task.getDuration().toString());

            pstmt.executeUpdate();
            System.out.println("Task imported successfully.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        DatabaseUtil dbUtil = new DatabaseUtil();

        // Example: Update project name
        dbUtil.updateProjectName(1, "New Project Name");

        // Example: Import a new task
        Project project = new Project();
        Task newTask = project.addTask();
        newTask.setName("New Task from MPP");
        newTask.setStart(new Date());
        newTask.setFinish(new Date());

        dbUtil.importTask(newTask, 1);
    }
}
