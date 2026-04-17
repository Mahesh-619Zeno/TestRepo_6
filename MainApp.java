public class MainApp {
    public static void main(String[] args) {
        // Load configuration from file
        AppConfig.load("config.properties");

        // Access global config values
        System.out.println("Application Name: " + AppConfig.APP_NAME);
        System.out.println("Max Users Allowed: " + AppConfig.MAX_USERS);
        System.out.println("Debug Mode: " + (AppConfig.DEBUG_MODE ? "ON" : "OFF"));
    }
}
