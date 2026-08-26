import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** ترحيل فواتير المشتريات ومردوداتها في ظل الجرد المستمر. */
public final class PurchasingPostingService {
    private PurchasingPostingService() {
    }

    public static boolean postPurchase(PurchaseInvoice invoice) {
        if (invoice == null) throw new IllegalArgumentException("بيانات فاتورة المشتريات مطلوبة.");
        requireInventoryData(invoice.getItemCode(), invoice.getQuantity(), invoice.getUnitCost());
        requireAmountMatchesQuantity(invoice.getAmount(), invoice.getQuantity(), invoice.getUnitCost());
        JournalEntry entry = new JournalEntry("JV-PUR-" + invoice.getInvoiceCode(), invoice.getInvoiceCode(),
                "INVENTORY", "إثبات شراء مواد خام من المورد: " + invoice.getInvoiceCode());
        entry.addDebitLine(invoice.getGrirAccount(), "مخزون المواد الخام", "إضافة مواد خام للمخزون", invoice.getAmount());
        if (invoice.getTaxAmount() > 0) {
            entry.addDebitLine(invoice.getInputTaxAccount(), "ضريبة مشتريات مدخلات", "إثبات ضريبة المدخلات", invoice.getTaxAmount());
        }
        entry.addCreditLine(invoice.getVendorAccount(), "حساب المورد", "إثبات مستحقات المورد", invoice.getTotalVendorCredit());
        return PostingEngine.postJournalEntry(entry, connection -> {
            savePurchase(connection, invoice);
            InventoryPostingService.receiveInTransaction(connection, invoice.getInvoiceCode(), invoice.getItemCode(),
                invoice.getItemCode(), invoice.getQuantity(), invoice.getUnitCost(), invoice.getGrirAccount(),
                invoice.getVendorAccount(), "", "", "استلام مشتريات");
        });
    }

    public static boolean postPurchaseReturn(PurchaseReturnInvoice invoice) {
        if (invoice == null) throw new IllegalArgumentException("بيانات مردود المشتريات مطلوبة.");
        requireInventoryData(invoice.getItemCode(), invoice.getQuantity(), invoice.getUnitCost());
        requireAmountMatchesQuantity(invoice.getReturnAmount(), invoice.getQuantity(), invoice.getUnitCost());
        JournalEntry entry = new JournalEntry("JV-PR-" + invoice.getInvoiceCode(), invoice.getInvoiceCode(),
                "INVENTORY", "إثبات مردود مواد خام إلى المورد: " + invoice.getInvoiceCode());
        entry.addDebitLine(invoice.getVendorAccount(), "حساب المورد", "تخفيض مستحقات المورد", invoice.getTotalVendorDebit());
        entry.addCreditLine(invoice.getGrirAccount(), "مخزون المواد الخام", "إخراج المواد المرتجعة من المخزون", invoice.getReturnAmount());
        if (invoice.getTaxAmount() > 0) {
            entry.addCreditLine(invoice.getInputTaxAccount(), "ضريبة مشتريات مدخلات", "عكس ضريبة المدخلات", invoice.getTaxAmount());
        }
        return PostingEngine.postJournalEntry(entry, connection -> {
            savePurchaseReturn(connection, invoice);
            InventoryPostingService.issueInTransaction(connection, invoice.getInvoiceCode(), invoice.getItemCode(),
                invoice.getItemCode(), invoice.getQuantity(), invoice.getUnitCost(), invoice.getGrirAccount(),
                invoice.getVendorAccount(), "", "", "صرف مردود مشتريات");
        });
    }

    private static void savePurchase(Connection connection, PurchaseInvoice invoice) throws SQLException {
        createTables(connection);
        String sql = "INSERT INTO purchase_invoices (invoice_code, inventory_account, supplier_account, input_tax_account, amount, tax_amount, total_amount) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, invoice.getInvoiceCode());
            statement.setString(2, invoice.getGrirAccount());
            statement.setString(3, invoice.getVendorAccount());
            statement.setString(4, invoice.getInputTaxAccount());
            statement.setDouble(5, invoice.getAmount());
            statement.setDouble(6, invoice.getTaxAmount());
            statement.setDouble(7, invoice.getTotalVendorCredit());
            statement.executeUpdate();
        }
    }

    private static void savePurchaseReturn(Connection connection, PurchaseReturnInvoice invoice) throws SQLException {
        createTables(connection);
        String sql = "INSERT INTO purchase_return_invoices (return_code, inventory_account, supplier_account, input_tax_account, amount, tax_amount, total_amount) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, invoice.getInvoiceCode());
            statement.setString(2, invoice.getGrirAccount());
            statement.setString(3, invoice.getVendorAccount());
            statement.setString(4, invoice.getInputTaxAccount());
            statement.setDouble(5, invoice.getReturnAmount());
            statement.setDouble(6, invoice.getTaxAmount());
            statement.setDouble(7, invoice.getTotalVendorDebit());
            statement.executeUpdate();
        }
    }

    private static void createTables(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS purchase_invoices ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, invoice_code VARCHAR(50) UNIQUE, inventory_account VARCHAR(20), "
                + "supplier_account VARCHAR(20), input_tax_account VARCHAR(20), amount DECIMAL(18,4), tax_amount DECIMAL(18,4), "
                + "total_amount DECIMAL(18,4), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB")) {
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS purchase_return_invoices ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, return_code VARCHAR(50) UNIQUE, inventory_account VARCHAR(20), "
                + "supplier_account VARCHAR(20), input_tax_account VARCHAR(20), amount DECIMAL(18,4), tax_amount DECIMAL(18,4), "
                + "total_amount DECIMAL(18,4), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB")) {
            statement.executeUpdate();
        }
    }

    private static void requireInventoryData(String itemCode, double quantity, double unitCost) {
        if (itemCode == null || itemCode.trim().isEmpty() || quantity <= 0 || unitCost <= 0) {
            throw new IllegalArgumentException("يجب تحديد الصنف والكمية وتكلفة الوحدة قبل اعتماد المستند.");
        }
    }

    private static void requireAmountMatchesQuantity(double amount, double quantity, double unitCost) {
        if (Math.abs(amount - (quantity * unitCost)) > 0.01) {
            throw new IllegalArgumentException("قيمة المستند يجب أن تساوي الكمية مضروبة في تكلفة الوحدة.");
        }
    }
}
