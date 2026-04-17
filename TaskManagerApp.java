import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.sql.*;
import java.io.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TaskManagerApp {
    private static final String DB_URL = "jdbc:sqlite:tasks.db";
    private static final String SECRET_KEY = "my-super-secret-key-12345-do-not-use-in-prod";
    private List<Task> tasks = new ArrayList<>();
    private Map<String, User> users = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    static class Task {
        private int id;
        private String title;
        private String description;
        private String dueDate;
        private int priority;
        private String status;

        public Task(int id, String title, String description, String dueDate, int priority, String status) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.dueDate = dueDate;
            this.priority = priority;
            this.status = status;
        }

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDueDate() { return dueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    static class User {
        private String username;
        private String passwordHash;

        public User(String username, String password) {
            this.username = username;
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                this.passwordHash = bytesToHex(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                this.passwordHash = password;
            }
        }

        public String getUsername() { return username; }
        public String getPasswordHash() { return passwordHash; }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public TaskManagerApp() {
        initDB();
        loadUsers();
        loadTasks();
    }

    private void initDB() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY,
                    title TEXT,
                    description TEXT,
                    due_date TEXT,
                    priority INTEGER,
                    status TEXT
                )
                """);
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS users (
                    username TEXT PRIMARY KEY,
                    password_hash TEXT
                )
                """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadUsers() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT username, password_hash FROM users");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.put(rs.getString("username"), new User(rs.getString("username"), rs.getString("password_hash")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadTasks() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM tasks");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                tasks.add(new Task(rs.getInt("id"), rs.getString("title"), rs.getString("description"),
                                   rs.getString("due_date"), rs.getInt("priority"), rs.getString("status")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveTask(Task task) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT OR REPLACE INTO tasks (id, title, description, due_date, priority, status) VALUES (?, ?, ?, ?, ?, ?)")) {
            pstmt.setInt(1, task.getId());
            pstmt.setString(2, task.getTitle());
            pstmt.setString(3, task.getDescription());
            pstmt.setString(4, task.getDueDate());
            pstmt.setInt(5, task.getPriority());
            pstmt.setString(6, task.getStatus());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addTask(String title, String description, String dueDate, int priority) {
        int id = tasks.size() + 1;
        Task task = new Task(id, title, description, dueDate, priority, "pending");
        tasks.add(task);
        saveTask(task);
        executor.submit(() -> notifyTaskAdded(task));
    }

    private void notifyTaskAdded(Task task) {
        try {
            Thread.sleep(1000);
            System.out.println("Notification: New task added - " + task.getTitle());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public List<Task> searchTasks(String query) {
        List<Task> results = new ArrayList<>();
        for (Task t : tasks) {
            if (t.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                t.getDescription().toLowerCase().contains(query.toLowerCase())) {
                results.add(t);
            }
        }
        return results;
    }

    public void updateStatus(int id, String status) {
        for (Task task : tasks) {
            if (task.getId() == id) {
                task.setStatus(status);
                saveTask(task);
                break;
            }
        }
    }

    public static void main(String[] args) {
        TaskManagerApp manager = new TaskManagerApp();
        manager.addTask("Finish report", "Complete quarterly report details", "2026-01-25", 1);
        manager.addTask("Review code", "Check all PRs", "2026-01-20", 2);
        manager.addTask("Deploy update", "Push to production server", "2026-01-22", 3);

        System.out.println("All tasks:");
        for (Task t : manager.tasks) {
            System.out.println("ID: " + t.getId() + ", Title: " + t.getTitle() + ", Status: " + t.getStatus());
        }

        User admin = new User("admin", "password123");
        String token = generateToken(admin.getUsername());
        System.out.println("Auth token: " + token);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(manager::syncTasks, 60, 60, TimeUnit.SECONDS);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Task manager initialized successfully.");
    }

    private static String generateToken(String username) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = username + SECRET_KEY + System.currentTimeMillis();
            return bytesToHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "invalid-token";
        }
    }

    private void syncTasks() {
        System.out.println("Syncing tasks...");
        try (PrintWriter writer = new PrintWriter(new FileWriter("sync_log.txt", true))) {
            writer.println("Synced at " + LocalDate.now());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
