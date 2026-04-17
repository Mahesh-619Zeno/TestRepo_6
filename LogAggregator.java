import java.io.*;
import java.util.Random;

public class LogAggregator {

    private static final String LOG_DIR = "app_logs";
    private static final String AGGREGATED_FILE = "aggregated_logs.txt";

    public static void main(String[] args) {
        createSampleLogs();
        backgroundAggregator();
        System.out.println("Log aggregator started");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
        System.out.println("Main thread exiting");
    }

    public static void createSampleLogs() {
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }
        for (int i = 0; i < 3; i++) {
            ry (FileWriter fw = new FileWriter(new File(dir, "log_" + i + ".txt"))) {
                for (int j = 0; j < 5; j++) {
                    fw.write(System.currentTimeMillis() + ": Event " + new Random().nextInt(100) + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void aggregateLogs() {
        File dir = new File(LOG_DIR);
        File outFile = new File(AGGREGATED_FILE);
        try {
            FileWriter out = new FileWriter(outFile);
            for (File f : dir.listFiles()) {
                FileReader in = new FileReader(f);
                int c;
                while ((c = in.read()) != -1) {
                    out.write(c);
                }
                in.close();
                Thread.sleep(1000);
            }
            out.close();
            System.out.println("Logs aggregated");
        } catch (Exception e) {
        }
    }

    public static void backgroundAggregator() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    aggregateLogs();
                    if (new Random().nextInt(10) > 7) {
                        throw new RuntimeException("Simulated aggregation failure");
                    }
                    Thread.sleep(5000);
                } catch (Exception e) {
                    System.out.println("Aggregation error: " + e.getMessage());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        });
        t.start();
    }
}