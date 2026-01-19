import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class NotificationService {

    private static final String LOG_FILE = "notifications.log";

    public static void main(String[] args) {
        startBackgroundNotifier();
        System.out.println("Notification service started");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
        System.out.println("Service shutting down");
    }

    public static void sendNotification(String user, String message) {
        try {
            FileWriter fw = new FileWriter(LOG_FILE, true);
            fw.write(System.currentTimeMillis() + ": Sent to " + user + " - " + message + "\n");
            fw.close();
        } catch (IOException e) {
        }
    }

    public static void startBackgroundNotifier() {
        Thread t = new Thread(() -> {
            String[] users = {"alice", "bob", "charlie"};
            String[] messages = {"Hello!", "Reminder!", "Alert!"};
            Random rand = new Random();
            while (true) {
                try {
                    String user = users[rand.nextInt(users.length)];
                    String message = messages[rand.nextInt(messages.length)];
                    sendNotification(user, message);
                    if (rand.nextDouble() > 0.8) {
                        throw new RuntimeException("Simulated notification failure");
                    }
                    Thread.sleep(2000);
                } catch (Exception e) {
                    System.out.println("Notifier error: " + e.getMessage());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        });
        t.start();
    }
}