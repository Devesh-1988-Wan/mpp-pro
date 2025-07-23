package net.sf.mpxj.examples;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Task;
import net.sf.mpxj.Duration;
import net.sf.mpxj.TimeUnit;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

public class DatabaseUtil {

    private static final Properties properties = new Properties();

    static {
        try (InputStream input = new FileInputStream(".env")) {
            properties.load(input);
        } catch (IOException ex) {
            System.out.println("Sorry, unable to find .env file");
            ex.printStackTrace();
        }
    }

    // Database connection details loaded from .env file
    private static final String DB_URL = properties.getProperty("DB_URL");
    private static final String DB_USER = properties.getProperty("DB_USER");
    private static final String DB_PASSWORD = properties.getProperty("DB_PASSWORD");

    /**
     * Establishes a connection to the database.
     *
     * @return A Connection object.
     * @throws SQLException if a database access error occurs.
     */
    public static Connection getConnection() throws SQLException {
        if (DB_URL == null || DB_USER == null || DB_PASSWORD == null) {
            throw new SQLException("Database credentials are not set in the .env file.");
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
    
    /**
     * Inserts a new project into the database.
     *
     * @param project The ProjectFile object to insert.
     */
    public void insertProject(ProjectFile project) {
        String projectSql = "INSERT INTO projects(project_id, project_name) VALUES(?,?)";
        int projectId = project.getProjectHeader().getUniqueID();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(projectSql)) {

            pstmt.setInt(1, projectId);
            pstmt.setString(2, project.getProjectHeader().getProjectTitle());
            pstmt.executeUpdate();
            System.out.println("Project inserted successfully.");

            // Now insert all tasks for this project
            for (Task task : project.getAllTasks()) {
                importTask(task, projectId);
            }

        } catch (SQLException e) {
            System.out.println("Error inserting project: " + e.getMessage());
        }
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
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Project name updated successfully.");
            } else {
                System.out.println("Project with ID " + projectId + " not found.");
            }

        } catch (SQLException e) {
            System.out.println("Error updating project name: " + e.getMessage());
        }
    }

    /**
     * Imports a new task into the database.
     *
     * @param task The Task object to import.
     * @param projectId The ID of the project this task belongs to.
     */
    public void importTask(Task task, int projectId) {
        String sql = "INSERT INTO tasks(project_id, task_name, start_date, finish_date, duration_value, duration_units) VALUES(?,?,?,?,?,?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, projectId);
            pstmt.setString(2, task.getName());

            pstmt.setDate(3, task.getStart() != null ? new java.sql.Date(task.getStart().getTime()) : null);
            pstmt.setDate(4, task.getFinish() != null ? new java.sql.Date(task.getFinish().getTime()) : null);

            Duration duration = task.getDuration();
            if (duration != null) {
                pstmt.setDouble(5, duration.getDuration());
                pstmt.setString(6, duration.getUnits().toString());
            } else {
                pstmt.setNull(5, java.sql.Types.DOUBLE);
                pstmt.setString(6, null);
            }

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error importing task '" + task.getName() + "': " + e.getMessage());
        }
    }
    
    /**
     * Reads a project and its tasks from the database.
     *
     * @param projectId The ID of the project to read.
     * @return A ProjectFile object populated with data from the database, or null if not found.
     */
    public ProjectFile readProjectData(int projectId) {
        String projectSql = "SELECT project_name FROM projects WHERE project_id = ?";
        ProjectFile project = null;

        try (Connection conn = getConnection();
             PreparedStatement projectPstmt = conn.prepareStatement(projectSql)) {
            
            projectPstmt.setInt(1, projectId);
            try (ResultSet projectRs = projectPstmt.executeQuery()) {
                if (projectRs.next()) {
                    project = new ProjectFile();
                    String projectName = projectRs.getString("project_name");
                    project.getProjectHeader().setProjectTitle(projectName);
                    System.out.println("Found project: " + projectName);

                    // Now, read the tasks for this project
                    readTasksForProject(conn, project, projectId);
                } else {
                    System.out.println("Project with ID " + projectId + " not found.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error reading project data: " + e.getMessage());
        }
        return project;
    }

    /**
     * Helper method to read tasks for a given project.
     */
    private void readTasksForProject(Connection conn, ProjectFile project, int projectId) throws SQLException {
        String tasksSql = "SELECT task_name, start_date, finish_date, duration_value, duration_units FROM tasks WHERE project_id = ?";
        try (PreparedStatement tasksPstmt = conn.prepareStatement(tasksSql)) {
            tasksPstmt.setInt(1, projectId);
            try (ResultSet tasksRs = tasksPstmt.executeQuery()) {
                while (tasksRs.next()) {
                    Task task = project.addTask();
                    task.setName(tasksRs.getString("task_name"));
                    task.setStart(tasksRs.getDate("start_date"));
                    task.setFinish(tasksRs.getDate("finish_date"));

                    double durationValue = tasksRs.getDouble("duration_value");
                    String durationUnitsStr = tasksRs.getString("duration_units");
                    
                    if (durationUnitsStr != null) {
                        TimeUnit units = TimeUnit.valueOf(durationUnitsStr);
                        task.setDuration(Duration.getInstance(durationValue, units));
                    }
                }
                System.out.println(project.getAllTasks().size() + " tasks loaded for project ID " + projectId);
            }
        }
    }


    public static void main(String[] args) {
        DatabaseUtil dbUtil = new DatabaseUtil();

        try {
            // 1. Create a sample project and insert it into the database
            System.out.println("--- CREATING AND INSERTING NEW PROJECT ---");
            ProjectFile newProject = new ProjectFile();
            newProject.getProjectHeader().setUniqueID(101); // Set a unique ID for the project
            newProject.getProjectHeader().setProjectTitle("New Database Project");
            
            Task task1 = newProject.addTask();
            task1.setName("Design Database Schema");
            task1.setStart(new Date());
            task1.setDuration(Duration.getInstance(5, TimeUnit.DAYS));
            
            Task task2 = newProject.addTask();
            task2.setName("Develop Java Utility");
            task2.setStart(new Date());
            task2.setDuration(Duration.getInstance(10, TimeUnit.DAYS));

            dbUtil.insertProject(newProject);
            System.out.println("----------------------------------------\n");

            // 2. Read the project data back from the database
            System.out.println("--- READING PROJECT FROM DATABASE ---");
            ProjectFile readProject = dbUtil.readProjectData(101);
            if (readProject != null) {
                System.out.println("Successfully read project: " + readProject.getProjectHeader().getProjectTitle());
                for (Task task : readProject.getAllTasks()) {
                    System.out.println("  - Task: " + task.getName() + ", Duration: " + task.getDuration());
                }
            }
            System.out.println("-------------------------------------\n");

            // 3. Update the project name
            System.out.println("--- UPDATING PROJECT NAME ---");
            dbUtil.updateProjectName(101, "Updated Project Name");
            System.out.println("-----------------------------\n");
            
            // 4. Read the project again to verify the update
            System.out.println("--- VERIFYING UPDATE ---");
            ProjectFile updatedProject = dbUtil.readProjectData(101);
            if (updatedProject != null) {
                System.out.println("Verified updated project name: " + updatedProject.getProjectHeader().getProjectTitle());
            }
            System.out.println("------------------------\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
