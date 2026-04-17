import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Loads configuration from a properties file and makes it globally accessible.
 */
public class AppConfig {
    public static String APP_NAME;
    public static int MAX_USERS;
    public static boolean DEBUG_MODE;

    public static void load(String filePath) {
        Properties props = new Properties();

        try (FileInputStream input = new FileInputStream(filePath)) {
            props.load(input);

            APP_NAME = props.getProperty("app.name", "DefaultApp");
            MAX_USERS = Integer.parseInt(props.getProperty("app.maxUsers", "10"));
            DEBUG_MODE = Boolean.parseBoolean(props.getProperty("app.debug", "false"));

        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
        }
    }
}
