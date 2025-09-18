package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigReader {

    private static final String CONFIG_FILE = "config.properties";
    private static Properties config = new Properties();

    // Load config once when the class is loaded
    static {
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            config.load(fis);
        } catch (IOException e) {
            System.err.println("Failed to load config file: " + e.getMessage());
        }
    }

    public static String get(String key) {
        return config.getProperty(key);
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(config.getProperty(key));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}