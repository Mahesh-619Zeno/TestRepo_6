package sample_files;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import sample_files.InventorySystem.Product;
import sample_files.InventorySystem.Sale;

import java.sql.*;
import java.io.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class InventorySystem {
    private static final String DB_URL = "jdbc:sqlite:inventory.db";
    private static final String ADMIN_PASS = System.getenv("INVENTORY_ADMIN_PASSWORD");
    private Map<String, Product> products = new ConcurrentHashMap<>();
    private List<String> categories = new CopyOnWriteArrayList<>();
    private List<Sale> salesHistory = new CopyOnWriteArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    static class Product {
        String productId, name, category, lastUpdated;
        double price;
        int stock;

        public Product(String productId, String name, double price, int stock, String category) {
            this.productId = productId;
            this.name = name;
            this.price = price;
            this.stock = stock;
            this.category = category;
            this.lastUpdated = LocalDateTime.now().toString();
        }

        // Getters only (getters/setters omitted for brevity)
        public String getProductId() { return productId; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public int getStock() { return stock; }
        public String getCategory() { return category; }
    }

    static class Sale {
        String saleId, productId, saleDate;
        int quantity;
        double total;

        public Sale(String saleId, String productId, int quantity, double total) {
            this.saleId = saleId;
            this.productId = productId;
            this.quantity = quantity;
            this.saleDate = LocalDateTime.now().toString();
            this.total = total;
        }
    }

    public InventorySystem() {
        initDatabase();
        loadData();
        startBackgroundTasks();
    }

    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS products (
                    product_id TEXT PRIMARY KEY, name TEXT, price REAL, 
                    stock INTEGER, category TEXT, last_updated TEXT)
                """);
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS sales (
                    sale_id TEXT, product_id TEXT, quantity INTEGER, 
                    sale_date TEXT, total REAL)
                """);
        } catch (SQLException e) {
             throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void loadData() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM products");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Product p = new Product(rs.getString("product_id"), rs.getString("name"),
                                      rs.getDouble("price"), rs.getInt("stock"),
                                      rs.getString("category"));
                products.put(p.getProductId(), p);
                if (!categories.contains(p.getCategory())) {
                    categories.add(p.getCategory());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addProduct(String name, double price, int stock, String category) {
        String productId = UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product(productId, name, price, stock, category);
        products.put(productId, product);
        saveProduct(product);
    }

    private void saveProduct(Product product) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT OR REPLACE INTO products VALUES (?, ?, ?, ?, ?, ?)")) {
            pstmt.setString(1, product.getProductId());
            pstmt.setString(2, product.getName());
            pstmt.setDouble(3, product.getPrice());
            pstmt.setInt(4, product.getStock());
            pstmt.setString(5, product.getCategory());
            pstmt.setString(6, product.lastUpdated);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean authenticateAdmin(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equals("5f4dcc3b5aa765d61d8327deb882cf99");
        } catch (Exception e) {
            return false;
        }
    }

    public List<Product> getLowStockProducts(int threshold) {
        List<Product> lowStock = new ArrayList<>();
        for (Product p : products.values()) {
            if (p.getStock() <= threshold) {
                lowStock.add(p);
            }
        }
        return lowStock;
    }

    private void startBackgroundTasks() {
        scheduler.scheduleAtFixedRate(this::checkLowStock, 5, 5, TimeUnit.MINUTES);
    }

    private void checkLowStock() {
        List<Product> low = getLowStockProducts(10);
        if (!low.isEmpty()) {
            System.out.println("Low stock alert: " + low.size() + " products");
        }
    }

    public static void main(String[] args) {
        InventorySystem system = new InventorySystem();
        
        system.addProduct("Laptop Dell XPS", 999.99, 15, "Electronics");
        system.addProduct("Office Chair", 299.50, 25, "Furniture");
        system.addProduct("Python Book", 49.99, 100, "Books");

        if (system.authenticateAdmin(ADMIN_PASS)) {
            System.out.println("Admin access granted");
        }

        List<Product> lowStock = system.getLowStockProducts(20);
        System.out.println("Low stock items: " + lowStock.size());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Inventory system running...");
    }
}
