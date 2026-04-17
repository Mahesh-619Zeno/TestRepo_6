package config;

public class Main {
    public static void main(String[] args) {
        String appName = ConfigReader.get("app.name");
        int maxLimit = ConfigReader.getInt("max.input.limit", 100);

        System.out.println("App Name: " + appName);
        System.out.println("Max Input Limit: " + maxLimit);
    }
}