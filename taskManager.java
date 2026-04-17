// TaskManager.java
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import com.google.gson.*;

class Task {
    int id;
    String title;
    boolean completed;
    List<String> tags;
    String created, updated;

    Task(int id, String title) {
        this.id = id;
        this.title = title;
        this.completed = false;
        this.tags = new ArrayList<>();
        this.created = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.updated = this.created;
    }
}

class TaskManager {
    private List<Task> tasks = new ArrayList<>();
    private int nextId = 1;
    private static final String DATA = "tasks.json";
    private static final String LOG = "task_manager.log";
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public TaskManager() { load(); }

    void log(String msg) {
        try (FileWriter fw = new FileWriter(LOG, true)) {
            fw.write(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                      + " " + msg + "\n");
        } catch(IOException e) {}
    }

    void save() {
        try (Writer w = Files.newBufferedWriter(Paths.get(DATA))) {
            JsonObject root = new JsonObject();
            root.add("tasks", gson.toJsonTree(tasks));
            root.addProperty("nextId", nextId);
            gson.toJson(root, w);
            log("Saved to disk");
        } catch(IOException e) { log("Save error"); }
    }

    void load() {
        if (!Files.exists(Paths.get(DATA))) {
            log("Starting new");
            return;
        }
        try (Reader r = Files.newBufferedReader(Paths.get(DATA))) {
            JsonObject root = gson.fromJson(r, JsonObject.class);
            nextId = root.get("nextId").getAsInt();
            Task[] arr = gson.fromJson(root.get("tasks"), Task[].class);
            tasks = new ArrayList<>(Arrays.asList(arr));
            log("Loaded from disk");
        } catch(IOException e) { log("Load error"); }
    }

    void addTask(String title) {
        Task t = new Task(nextId++, title);
        tasks.add(t);
        save();
        log("Added task " + t.id);
        System.out.println("Added: [" + t.id + "] " + t.title);
    }

    void listTasks(boolean all) {
        for (Task t : tasks) {
            if (!all && t.completed) continue;
            System.out.printf("[%d] %s %s Tags:%s%n", t.id,
                              t.completed ? "✅" : "❌", t.title, t.tags);
        }
    }

    void completeTask(int id) {
        for (Task t : tasks) {
            if (t.id == id) {
                t.completed = true;
                t.updated = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                save(); log("Completed " + id);
                System.out.println("Completed: [" + id + "]");
                return;
            }
        }
        System.out.println("Not found");
    }

    void deleteTask(int id) {
        tasks.removeIf(t -> t.id == id);
        save(); log("Deleted " + id);
        System.out.println("Deleted if existed");
    }

    void search(String term) {
        for (Task t : tasks) {
            if (t.title.toLowerCase().contains(term.toLowerCase()))
                System.out.println("[" + t.id + "] " + t.title);
        }
    }

    void tagTask(int id, List<String> tags) {
        for (Task t : tasks) {
            if (t.id == id) {
                Set<String> s = new HashSet<>(t.tags);
                s.addAll(tags);
                t.tags = new ArrayList<>(s);
                t.updated = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                save(); log("Tagged " + id);
                System.out.println("Tagged: " + t.tags);
                return;
            }
        }
        System.out.println("Not found");
    }

    // CLI menu
    void menu() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n-- Task Manager --");
            System.out.println("1: Add  2: List  3: List All  4: Complete");
            System.out.println("5: Delete 6: Search 7: Tag 8: Exit");
            System.out.print("> ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1":
                        System.out.print("Title: "); String title = sc.nextLine();
                        addTask(title);
                        break;
                    case "2":
                        listTasks(false); break;
                    case "3":
                        listTasks(true); break;
                    case "4":
                        System.out.print("ID: "); int idc = Integer.parseInt(sc.nextLine());
                        completeTask(idc); break;
                    case "5":
                        System.out.print("ID: "); int idd = Integer.parseInt(sc.nextLine());
                        deleteTask(idd); break;
                    case "6":
                        System.out.print("Search: "); String term = sc.nextLine();
                        search(term); break;
                    case "7":
                        System.out.print("ID: "); int idt = Integer.parseInt(sc.nextLine());
                        System.out.print("Tags (comma): "); String tagsLine = sc.nextLine();
                        List<String> tags = Arrays.asList(tagsLine.split(","));
                        tagTask(idt, tags); break;
                    case "8":
                        System.out.println("Bye!"); return;
                    default:
                        System.out.println("Invalid");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
    public static void main(String[] args) {
        new TaskManager().menu();
    }
}
