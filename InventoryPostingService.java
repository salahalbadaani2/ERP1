import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class InventoryPostingService {
    private InventoryPostingService() {
    }

        public static void receiveInTransaction(Connection connection, String documentNumber, String itemCode,
                            String itemName, double quantity, double unitCost,
                            String inventoryAccount, String sourceAccount,
                            String receiver, String deliverer, String narration) throws SQLException {
        requireMovementData(itemCode, quantity, unitCost);
        saveMovement(connection, documentNumber, "RECEIPT", itemCode, itemName, quantity, unitCost,
            inventoryAccount, sourceAccount, receiver, deliverer, narration);
        }

        public static void issueInTransaction(Connection connection, String documentNumber, String itemCode,
                          String itemName, double quantity, double unitCost,
                          String inventoryAccount, String destinationAccount,
                          String receiver, String deliverer, String narration) throws SQLException {
        requireMovementData(itemCode, quantity, unitCost);
        saveMovement(connection, documentNumber, "ISSUE", itemCode, itemName, -quantity, unitCost,
            inventoryAccount, destinationAccount, receiver, deliverer, narration);
        }

    public static boolean receive(String documentNumber, String itemCode, String itemName,
                                  double quantity, String inventoryAccount, String sourceAccount,
                                  String receiver, String deliverer, String narration) {
        double unitCost = averageCost(itemCode);
        if (unitCost <= 0) {
            throw new IllegalArgumentException("لا توجد تكلفة متوسطة للصنف. يجب تعريف تكلفة الصنف أولاً.");
        }
        double amount = quantity * unitCost;
        JournalEntry entry = new JournalEntry("JV-WR-" + documentNumber, documentNumber, "INVENTORY", narration);
        entry.addDebitLine(inventoryAccount, "المخزون", "استلام مخزني", amount);
        entry.addCreditLine(sourceAccount, "مصدر الاستلام", "إثبات مصدر الاستلام", amount);
        return PostingEngine.postJournalEntry(entry, connection -> saveMovement(connection, documentNumber,
                "RECEIPT", itemCode, itemName, quantity, unitCost, inventoryAccount, sourceAccount,
                receiver, deliverer, narration));
    }

    public static boolean issue(String documentNumber, String itemCode, String itemName,
                                double quantity, String inventoryAccount, String destinationAccount,
                                String receiver, String deliverer, String narration) {
        double unitCost = averageCost(itemCode);
        if (unitCost <= 0) {
            throw new IllegalArgumentException("لا توجد تكلفة متوسطة للصنف. يجب تعريف تكلفة الصنف أولاً.");
        }
        if (availableQuantity(itemCode) < quantity) {
            throw new IllegalArgumentException("الكمية المطلوبة تتجاوز الرصيد المخزني المتاح.");
        }
        double amount = quantity * unitCost;
        JournalEntry entry = new JournalEntry("JV-WI-" + documentNumber, documentNumber, "INVENTORY", narration);
        entry.addDebitLine(destinationAccount, "الجهة المستلمة", "تحميل الصرف المخزني", amount);
        entry.addCreditLine(inventoryAccount, "المخزون", "صرف مخزني", amount);
        return PostingEngine.postJournalEntry(entry, connection -> saveMovement(connection, documentNumber,
                "ISSUE", itemCode, itemName, -quantity, unitCost, inventoryAccount, destinationAccount,
                receiver, deliverer, narration));
    }

    private static double averageCost(String itemCode) {
        String sql = "SELECT unit_cost FROM inventory_items WHERE item_code = ?";
        try (Connection connection = DatabaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemCode);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getDouble(1);
            }
            throw new IllegalArgumentException("الصنف غير موجود في بطاقة الأصناف.");
        } catch (SQLException exception) {
            throw new IllegalStateException("تعذر قراءة متوسط تكلفة الصنف: " + exception.getMessage(), exception);
        }
    }

    private static double availableQuantity(String itemCode) {
        String sql = "SELECT current_stock FROM inventory_items WHERE item_code = ?";
        try (Connection connection = DatabaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemCode);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getDouble(1);
            }
            throw new IllegalArgumentException("الصنف غير موجود في بطاقة الأصناف.");
        } catch (SQLException exception) {
            throw new IllegalStateException("تعذر قراءة رصيد الصنف: " + exception.getMessage(), exception);
        }
    }

    private static void saveMovement(Connection connection, String documentNumber, String type,
                                     String itemCode, String itemName, double quantity, double unitCost,
                                     String inventoryAccount, String counterAccount, String receiver,
                                     String deliverer, String narration) throws SQLException {
        try (PreparedStatement create = connection.prepareStatement("CREATE TABLE IF NOT EXISTS inventory_movements ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, document_number VARCHAR(50) UNIQUE, movement_type VARCHAR(20), "
                + "item_code VARCHAR(50), item_name VARCHAR(255), quantity DECIMAL(15,2), unit_cost DECIMAL(15,2), "
                + "inventory_account VARCHAR(20), counter_account VARCHAR(20), receiver VARCHAR(255), deliverer VARCHAR(255), "
                + "narration TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB")) {
            create.executeUpdate();
        }
        String insert = "INSERT INTO inventory_movements (document_number, movement_type, item_code, item_name, quantity, unit_cost, inventory_account, counter_account, receiver, deliverer, narration) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setString(1, documentNumber);
            statement.setString(2, type);
            statement.setString(3, itemCode);
            statement.setString(4, itemName);
            statement.setDouble(5, quantity);
            statement.setDouble(6, unitCost);
            statement.setString(7, inventoryAccount);
            statement.setString(8, counterAccount);
            statement.setString(9, receiver);
            statement.setString(10, deliverer);
            statement.setString(11, narration);
            statement.executeUpdate();
        }
        try (PreparedStatement update = connection.prepareStatement("UPDATE inventory_items SET current_stock = current_stock + ?, unit_cost = CASE WHEN current_stock > 0 THEN ((current_stock * unit_cost) + (? * ?)) / (current_stock + ?) ELSE ? END WHERE item_code = ? AND current_stock + ? >= 0")) {
            update.setDouble(1, quantity);
            update.setDouble(2, quantity);
            update.setDouble(3, unitCost);
            update.setDouble(4, quantity);
            update.setDouble(5, unitCost);
            update.setString(6, itemCode);
            update.setDouble(7, quantity);
            if (update.executeUpdate() != 1) throw new SQLException("تعذر تحديث رصيد الصنف أو أن الرصيد غير كاف.");
        }
    }

    private static void requireMovementData(String itemCode, double quantity, double unitCost) {
        if (itemCode == null || itemCode.trim().isEmpty() || quantity <= 0 || unitCost <= 0) {
            throw new IllegalArgumentException("بيانات الصنف والكمية والتكلفة الموجبة مطلوبة للحركة المخزنية.");
        }
    }
}
