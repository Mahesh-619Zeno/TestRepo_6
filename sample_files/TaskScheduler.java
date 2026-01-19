package sample_files;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class TaskScheduler {

    private static final String LOG_FILE = "tasks.log";

    public static void main(String[] args) {
        startBackgroundScheduler();
        System.out.println("Task scheduler started");
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
        }
        System.out.println("Scheduler shutting down");
    }

    public static void logTask(String taskName) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
          fw.write(System.currentTimeMillis() + ": Executed task - " + taskName + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startBackgroundScheduler() {
        Thread t = new Thread(() -> {
            String[] tasks = {"Backup", "Cleanup", "EmailReport"};
            Random rand = new Random();
            while (true) {
                try {
                    String task = tasks[rand.nextInt(tasks.length)];
                    logTask(task);
                    if (rand.nextDouble() > 0.85) {
                        throw new RuntimeException("Simulated task failure");
                    }
                    Thread.sleep(3000);
                } catch (Exception e) {
                    System.out.println("Scheduler error: " + e.getMessage());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }
}