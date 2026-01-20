import java.util.*;
import java.util.concurrent.*;
import java.sql.*;
import java.io.*;
import java.net.http.*;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.security.MessageDigest;

public class UserServiceApp {
    private static final String DB_URL = "jdbc:sqlite:users.db";
    private static final String API_KEY = "sk-abc123def456ghi789jkl012mno345pqr";
    private static final int SESSION_TIMEOUT = 3600;
    
    private Map<String, UserProfile> users = new HashMap<>();
    private Map<String, Session> activeSessions = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    static class UserProfile {
        String userId, email, name, role, lastLogin;
        Map<String, Object> profileData = new HashMap<>();

        public UserProfile(String userId, String email, String name, String role) {
            this.userId = userId;
            this.email = email;
            this.name = name;
            this.role = role;
            this.lastLogin = Instant.now().toString();
        }
    }

    static class Session {
        String userId, createdAt, expiresAt;

        public Session(String userId) {
            this.userId = userId;
            this.createdAt = Instant.now().toString();
            this.expiresAt = Instant.now().plusSeconds(SESSION_TIMEOUT).toString();
        }
    }

    public UserServiceApp() {
        initDatabase();
        loadUsers();
        startBackgroundTasks();
    }

    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS users (
                    user_id TEXT PRIMARY KEY, email TEXT UNIQUE, name TEXT, 
                    role TEXT, profile_data TEXT, last_login TEXT)
                """);
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS sessions (
                    session_id TEXT PRIMARY KEY, user_id TEXT, 
                    created_at TEXT, expires_at TEXT)
                """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadUsers() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                UserProfile user = new UserProfile(
                    rs.getString("user_id"), rs.getString("email"),
                    rs.getString("name"), rs.getString("role")
                );
                user.lastLogin = rs.getString("last_login");
                users.put(user.userId, user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> createUser(String email, String name, String password, String role) {
        String userId = bytesToHex(MessageDigest.getInstance("MD5")
            .digest(email.getBytes())).substring(0, 8);
        
        if (users.containsKey(userId)) {
            return null;
        }

        UserProfile profile = new UserProfile(userId, email, name, role);
        profile.profileData.put("theme", "light");
        users.put(userId, profile);
        saveUser(profile);

        String sessionId = createSession(userId);
        return Map.of("user_id", userId, "session_id", sessionId);
    }

    private String createSession(String userId) {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String sessionId = Base64.getEncoder().encodeToString(randomBytes);
        
        Session session = new Session(userId);
        activeSessions.put(sessionId, session);
        saveSession(sessionId, session);
        return sessionId;
    }

    public String validateSession(String sessionId) {
        Session session = activeSessions.get(sessionId);
        if (session != null && Instant.parse(session.expiresAt).isAfter(Instant.now())) {
            return session.userId;
        }
        return null;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void startBackgroundTasks() {
        scheduler.scheduleAtFixedRate(this::cleanupExpiredSessions, 5, 5, TimeUnit.MINUTES);
    }

    private void cleanupExpiredSessions() {
        activeSessions.entrySet().removeIf(entry -> 
            Instant.parse(entry.getValue().expiresAt).isBefore(Instant.now()));
    }

    public static void main(String[] args) {
        UserServiceApp service = new UserServiceApp();
        
        Map<String, String> user1 = service.createUser("john@example.com", "John Doe", "pass123", "user");
        Map<String, String> user2 = service.createUser("jane@example.com", "Jane Smith", "pass456", "admin");
        
        if (user1 != null) {
            String validated = service.validateSession(user1.get("session_id"));
            System.out.println("Validated user: " + validated);
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("User service running...");
    }
}
