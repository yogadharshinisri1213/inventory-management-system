import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Inventory Management System
 * Single-file Java Swing application.
 *
 * Compile: javac Inventory.java
 * Run:     java Inventory
 *
 * Product data is saved automatically in data/inventory.csv
 */
public class Inventory extends JFrame {
    private static final Path DATA_FILE = Paths.get("data", "inventory.csv");

    private final List<Product> products = new ArrayList<>();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Product ID", "Name", "Category", "Unit Cost",
                    "Selling Price", "Quantity", "Reorder Level", "Stock Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(20);
    private final JLabel statusLabel = new JLabel("Ready");

    public Inventory() {
        super("Inventory Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setMinimumSize(new Dimension(800, 450));
        setLocationRelativeTo(null);

        buildInterface();
        loadProducts();
        refreshTable();
    }

    private void buildInterface() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        JLabel heading = new JLabel("Inventory Management System");
        heading.setFont(new Font("SansSerif", Font.BOLD, 24));

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.add(heading, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");
        searchPanel.add(searchButton);
        searchPanel.add(showAllButton);
        top.add(searchPanel, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);

        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add Product");
        JButton editButton = new JButton("Edit Selected");
        JButton deleteButton = new JButton("Delete Selected");
        JButton lowStockButton = new JButton("Low Stock");
        JButton saveButton = new JButton("Save");
        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        buttons.add(lowStockButton);
        buttons.add(saveButton);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(buttons, BorderLayout.NORTH);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 4, 0, 4));
        bottom.add(statusLabel, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addProduct());
        editButton.addActionListener(e -> editSelectedProduct());
        deleteButton.addActionListener(e -> deleteSelectedProduct());
        lowStockButton.addActionListener(e -> showLowStock());
        saveButton.addActionListener(e -> saveProducts());
        searchButton.addActionListener(e -> searchProducts());
        showAllButton.addActionListener(e -> refreshTable());
        searchField.addActionListener(e -> searchProducts());
    }

    private void addProduct() {
        Product product = showProductDialog(null);
        if (product == null) return;

        if (findProduct(product.id) != null) {
            showError("A product with this ID already exists.");
            return;
        }
        products.add(product);
        refreshTable();
        saveProducts();
        statusLabel.setText("Product added: " + product.name);
    }

    private void editSelectedProduct() {
        Product selected = getSelectedProduct();
        if (selected == null) {
            showError("Please select a product to edit.");
            return;
        }

        Product updated = showProductDialog(selected);
        if (updated == null) return;

        selected.name = updated.name;
        selected.category = updated.category;
        selected.unitCost = updated.unitCost;
        selected.sellingPrice = updated.sellingPrice;
        selected.quantity = updated.quantity;
        selected.reorderLevel = updated.reorderLevel;

        refreshTable();
        saveProducts();
        statusLabel.setText("Product updated: " + selected.name);
    }

    private void deleteSelectedProduct() {
        Product selected = getSelectedProduct();
        if (selected == null) {
            showError("Please select a product to delete.");
            return;
        }

        int answer = JOptionPane.showConfirmDialog(this,
                "Delete product \"" + selected.name + "\"?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (answer == JOptionPane.YES_OPTION) {
            products.remove(selected);
            refreshTable();
            saveProducts();
            statusLabel.setText("Product deleted.");
        }
    }

    private void searchProducts() {
        String query = searchField.getText().trim().toLowerCase();
        tableModel.setRowCount(0);

        for (Product p : products) {
            if (p.id.toLowerCase().contains(query)
                    || p.name.toLowerCase().contains(query)
                    || p.category.toLowerCase().contains(query)) {
                addProductRow(p);
            }
        }
        statusLabel.setText("Found " + tableModel.getRowCount() + " product(s).");
    }

    private void showLowStock() {
        tableModel.setRowCount(0);
        int count = 0;
        for (Product p : products) {
            if (p.quantity <= p.reorderLevel) {
                addProductRow(p);
                count++;
            }
        }
        statusLabel.setText("Low-stock products: " + count);
    }

    private Product showProductDialog(Product existing) {
        JTextField idField = new JTextField(existing == null ? "" : existing.id);
        JTextField nameField = new JTextField(existing == null ? "" : existing.name);
        JTextField categoryField = new JTextField(existing == null ? "" : existing.category);
        JTextField costField = new JTextField(existing == null ? "0.00" : String.valueOf(existing.unitCost));
        JTextField priceField = new JTextField(existing == null ? "0.00" : String.valueOf(existing.sellingPrice));
        JTextField quantityField = new JTextField(existing == null ? "0" : String.valueOf(existing.quantity));
        JTextField reorderField = new JTextField(existing == null ? "5" : String.valueOf(existing.reorderLevel));

        if (existing != null) idField.setEditable(false);

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Product ID:")); panel.add(idField);
        panel.add(new JLabel("Product Name:")); panel.add(nameField);
        panel.add(new JLabel("Category:")); panel.add(categoryField);
        panel.add(new JLabel("Unit Cost:")); panel.add(costField);
        panel.add(new JLabel("Selling Price:")); panel.add(priceField);
        panel.add(new JLabel("Quantity:")); panel.add(quantityField);
        panel.add(new JLabel("Reorder Level:")); panel.add(reorderField);

        int result = JOptionPane.showConfirmDialog(this, panel,
                existing == null ? "Add Product" : "Edit Product",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return null;

        try {
            String id = required(idField.getText(), "Product ID");
            String name = required(nameField.getText(), "Product name");
            String category = required(categoryField.getText(), "Category");
            double cost = Double.parseDouble(costField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());
            int quantity = Integer.parseInt(quantityField.getText().trim());
            int reorder = Integer.parseInt(reorderField.getText().trim());

            if (!Double.isFinite(cost) || !Double.isFinite(price) || cost < 0 || price < 0) {
                throw new IllegalArgumentException("Prices must be valid non-negative numbers.");
            }
            if (quantity < 0 || reorder < 0) {
                throw new IllegalArgumentException("Quantity and reorder level cannot be negative.");
            }

            return new Product(id, name, category, cost, price, quantity, reorder);
        } catch (NumberFormatException ex) {
            showError("Enter valid numbers for cost, selling price, quantity and reorder level.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
        return null;
    }

    private String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private Product getSelectedProduct() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        String id = String.valueOf(tableModel.getValueAt(modelRow, 0));
        return findProduct(id);
    }

    private Product findProduct(String id) {
        for (Product p : products) {
            if (p.id.equalsIgnoreCase(id)) return p;
        }
        return null;
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Product p : products) addProductRow(p);
        statusLabel.setText("Total products: " + products.size());
    }

    private void addProductRow(Product p) {
        String stockStatus = p.quantity <= p.reorderLevel ? "LOW STOCK" : "In Stock";
        tableModel.addRow(new Object[]{
                p.id, p.name, p.category,
                String.format(Locale.US, "%.2f", p.unitCost),
                String.format(Locale.US, "%.2f", p.sellingPrice),
                p.quantity, p.reorderLevel, stockStatus
        });
    }

    private void loadProducts() {
        if (!Files.exists(DATA_FILE)) return;

        try {
            List<String> lines = Files.readAllLines(DATA_FILE, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).trim().isEmpty()) continue;
                List<String> fields = parseCsvLine(lines.get(i));
                if (fields.size() != 7) continue;

                try {
                    Product p = new Product(
                            fields.get(0), fields.get(1), fields.get(2),
                            Double.parseDouble(fields.get(3)),
                            Double.parseDouble(fields.get(4)),
                            Integer.parseInt(fields.get(5)),
                            Integer.parseInt(fields.get(6)));
                    if (findProduct(p.id) == null) products.add(p);
                } catch (RuntimeException ignored) {
                    // Skip malformed rows but continue loading the remaining data.
                }
            }
        } catch (IOException ex) {
            showError("Could not read saved inventory:\n" + ex.getMessage());
        }
    }

    private void saveProducts() {
        try {
            Files.createDirectories(DATA_FILE.getParent());
            Path temp = DATA_FILE.resolveSibling("inventory.csv.tmp");

            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                writer.write("Product ID,Name,Category,Unit Cost,Selling Price,Quantity,Reorder Level");
                writer.newLine();

                for (Product p : products) {
                    writer.write(csv(p.id) + "," + csv(p.name) + "," + csv(p.category) + ","
                            + p.unitCost + "," + p.sellingPrice + ","
                            + p.quantity + "," + p.reorderLevel);
                    writer.newLine();
                }
            }

            try {
                Files.move(temp, DATA_FILE, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveNotSupported) {
                Files.move(temp, DATA_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
            statusLabel.setText("Saved " + products.size() + " product(s).");
        } catch (IOException ex) {
            showError("Could not save inventory:\n" + ex.getMessage());
        }
    }

    private String csv(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(value.toString());
                value.setLength(0);
            } else {
                value.append(c);
            }
        }
        fields.add(value.toString());
        return fields;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Inventory Management System",
                JOptionPane.ERROR_MESSAGE);
    }

    private static class Product {
        final String id;
        String name;
        String category;
        double unitCost;
        double sellingPrice;
        int quantity;
        int reorderLevel;

        Product(String id, String name, String category, double unitCost,
                double sellingPrice, int quantity, int reorderLevel) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.unitCost = unitCost;
            this.sellingPrice = sellingPrice;
            this.quantity = quantity;
            this.reorderLevel = reorderLevel;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Use the default Swing look and feel.
            }
            new Inventory().setVisible(true);
        });
    }
}
