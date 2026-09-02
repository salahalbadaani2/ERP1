import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

/** الترحيل الموحد للمبيعات ومردوداتها مع حركة المخزون الآلية. */
public final class SalesPostingService {
    private SalesPostingService() {
    }

    public static class SalesLine {
        private final String itemCode;
        private final String itemName;
        private final double quantity;
        private final double salePrice;
        private final double unitCost;
        public SalesLine(String itemCode, String itemName, double quantity, double salePrice, double unitCost) {
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.quantity = quantity;
            this.salePrice = salePrice;
            this.unitCost = unitCost;
        }
        public String getItemCode() { return itemCode; }
        public String getItemName() { return itemName; }
        public double getQuantity() { return quantity; }
        public double getSalePrice() { return salePrice; }
        public double getUnitCost() { return unitCost; }
        public double getLineAmount() { return quantity * salePrice; }
    }

    /**
     * ترحيل مبيعات باتصال داخلي: إنشاء اتصال + بدء معاملة + ترحيل كامل.
     */
    public static boolean postSale(String invoiceCode, String invoiceDate, String customerAccount,
                                        String revenueAccount, String taxAccount, String cogsAccount,
                                        String inventoryAccount, String itemCode, String itemName,
                                        double quantity, double salePrice, double unitCost, boolean taxApplied) {
        List<SalesLine> lines = new ArrayList<>();
        lines.add(new SalesLine(itemCode, itemName, quantity, salePrice, unitCost));
        return postSale(invoiceCode, invoiceDate, customerAccount, revenueAccount, taxAccount, cogsAccount, inventoryAccount, lines, taxApplied);
    }

    public static boolean postSale(String invoiceCode, String invoiceDate, String customerAccount,
                                   String revenueAccount, String taxAccount, String cogsAccount,
                                   String inventoryAccount, List<SalesLine> lines, boolean taxApplied) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            boolean success = postSale(conn, invoiceCode, invoiceDate, customerAccount,
                    revenueAccount, taxAccount, cogsAccount, inventoryAccount, lines, taxApplied);
            return success;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored2) { ignored2.printStackTrace(); JOptionPane.showMessageDialog(null, ignored2.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE); }
            }
            System.err.println("فشل ترحيل فاتورة المبيعات: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored2) { ignored2.printStackTrace(); JOptionPane.showMessageDialog(null, ignored2.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE); }
            }
        }
    }

    /**
     * ترحيل مبيعات باستخدام اتصال موحد مُمرَّر من الخارج.
     * يفترض أن الاتصال تم فتحه بـ setAutoCommit(false) من الخارج.
     */
    public static boolean postSale(Connection conn, String invoiceCode, String invoiceDate,
                                        String customerAccount, String revenueAccount, String taxAccount,
                                        String cogsAccount, String inventoryAccount, String itemCode,
                                        String itemName, double quantity, double salePrice,
                                        double unitCost, boolean taxApplied) {
        List<SalesLine> lines = new ArrayList<>();
        lines.add(new SalesLine(itemCode, itemName, quantity, salePrice, unitCost));
        return postSale(conn, invoiceCode, invoiceDate, customerAccount, revenueAccount, taxAccount, cogsAccount, inventoryAccount, lines, taxApplied);
    }

    public static boolean postSale(Connection conn, String invoiceCode, String invoiceDate,
                                   String customerAccount, String revenueAccount, String taxAccount,
                                   String cogsAccount, String inventoryAccount,
                                   List<SalesLine> lines, boolean taxApplied) {
        try {
            if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("قائمة الأصناف فارغة");
            double base = 0;
            double inventoryValue = 0;
            for (SalesLine l : lines) {
                requirePositive(l.getQuantity(), "الكمية للصنف " + l.getItemCode());
                requirePositive(l.getSalePrice(), "سعر البيع للصنف " + l.getItemCode());
                double actualUnitCost = averageCost(l.getItemCode(), l.getUnitCost());
                requirePositive(actualUnitCost, "متوسط تكلفة الصنف " + l.getItemCode());
                base += l.getQuantity() * l.getSalePrice();
                inventoryValue += l.getQuantity() * actualUnitCost;
            }
            double tax = taxApplied ? base * 0.15 : 0;
            double total = base + tax;

            JournalEntry entry = new JournalEntry("JV-SALES-" + invoiceCode, invoiceCode, "SALES",
                    "إثبات مبيعات وصرف مخزني: " + invoiceCode);
            entry.addDebitLine(customerAccount, "العميل", "إجمالي استحقاق المبيعات", total);
            entry.addCreditLine(revenueAccount, "إيراد المبيعات", "إيراد المبيعات", base);
            if (tax > 0) entry.addCreditLine(taxAccount, "ضريبة المبيعات", "ضريبة القيمة المضافة", tax);
            entry.addDebitLine(cogsAccount, "تكلفة المبيعات", "تكلفة البضاعة المباعة", inventoryValue);
            entry.addCreditLine(inventoryAccount, "مخزون المنتجات التامة", "صرف المنتج المباع", inventoryValue);

            // ترحيل القيد اليومي + حركة المخزون داخل اتصال واحد
            return PostingEngine.postJournalEntry(conn, entry, connection -> {
                for (SalesLine l : lines) {
                    double actualUnitCost = averageCost(l.getItemCode(), l.getUnitCost());
                    saveMovement(connection, invoiceCode, invoiceDate, "SALE", l.getItemCode(), l.getItemName(),
                            -l.getQuantity(), actualUnitCost, inventoryAccount, cogsAccount);
                }
            });
        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored2) { ignored2.printStackTrace(); JOptionPane.showMessageDialog(null, ignored2.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE); }
            throw new RuntimeException(e);
        }
    }

    /**
     * ترحيل مردودات مبيعات باتصال داخلي - أحادي (توافق خلفي).
     */
    public static boolean postSalesReturn(SalesReturnInvoice invoice, double quantity, String itemCode, String itemName) {
        List<SalesReturnInvoice.ReturnLine> lines = new ArrayList<>();
        double price = invoice.getReturnAmount() / quantity;
        double cost = invoice.getInventoryCost() / quantity;
        lines.add(new SalesReturnInvoice.ReturnLine(itemCode, quantity, price, cost));
        SalesReturnInvoice wrapped = new SalesReturnInvoice(invoice.getInvoiceCode(), invoice.getOriginalInvoiceCode(), invoice.getReturnDate(),
                invoice.getCustomerAccount(), invoice.getSalesReturnAccount(), invoice.getTaxAccount(),
                invoice.getFinishedGoodsAccount(), invoice.getCogsAccount(),
                invoice.isTaxApplied(), invoice.getTaxRate(), invoice.getReturnReason(), invoice.getBatchNo(), lines);
        return postSalesReturn(wrapped);
    }

    public static boolean postSalesReturn(SalesReturnInvoice invoice) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            boolean success = postSalesReturn(conn, invoice);
            return success;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored2) { ignored2.printStackTrace(); JOptionPane.showMessageDialog(null, ignored2.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE); }
            }
            System.err.println("فشل ترحيل مردودات المبيعات: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored2) { ignored2.printStackTrace(); JOptionPane.showMessageDialog(null, ignored2.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE); }
            }
        }
    }

    /**
     * ترحيل مردودات مبيعات باستخدام اتصال موحد مُمرَّر من الخارج - يدعم قائمة أسطر.
     */
    public static boolean postSalesReturn(Connection conn, SalesReturnInvoice invoice) {
        try {
            List<SalesReturnInvoice.ReturnLine> lines = invoice.getLines();
            if (lines != null && !lines.isEmpty()) {
                for (SalesReturnInvoice.ReturnLine l : lines) {
                    requirePositive(l.getQuantity(), "كمية المرتجع للصنف " + l.getItemCode());
                    requirePositive(l.getUnitPrice(), "سعر المرتجع للصنف " + l.getItemCode());
                }
            } else {
                // توافق مع الفواتير القديمة أحادية السطر
                requirePositive(invoice.getReturnAmount(), "قيمة المرتجع");
                requirePositive(invoice.getInventoryCost(), "قيمة التكلفة المخزنية");
            }

            JournalEntry entry = new JournalEntry("JV-SR-" + invoice.getInvoiceCode(), invoice.getInvoiceCode(),
                    "SALES_RETURN", "إثبات مردود مبيعات واستلام مخزني: " + invoice.getInvoiceCode());
            entry.addDebitLine(invoice.getSalesReturnAccount(), "مردودات المبيعات",
                    "عكس إيراد المبيعات", invoice.getReturnAmount());
            if (invoice.isTaxApplied() && invoice.getTaxAmount() > 0) {
                entry.addDebitLine(invoice.getTaxAccount(), "ضريبة المبيعات",
                        "عكس ضريبة المبيعات", invoice.getTaxAmount());
            }
            entry.addCreditLine(invoice.getCustomerAccount(), "العميل",
                    "تخفيض استحقاق العميل", invoice.getTotalCustomerCredit());
            entry.addDebitLine(invoice.getFinishedGoodsAccount(), "مخزون المنتجات التامة",
                    "استلام المرتجع", invoice.getInventoryCost());
            entry.addCreditLine(invoice.getCogsAccount(), "تكلفة المبيعات",
                    "عكس تكلفة البضاعة", invoice.getInventoryCost());

            return PostingEngine.postJournalEntry(conn, entry, connection -> {
                saveSalesReturnNote(connection, invoice);
                if (lines != null && !lines.isEmpty()) {
                    for (SalesReturnInvoice.ReturnLine l : lines) {
                        InventoryPostingService.receiveInTransaction(connection, invoice.getInvoiceCode(),
                                l.getItemCode(), l.getItemCode(), l.getQuantity(),
                                l.getUnitCost(), invoice.getFinishedGoodsAccount(), invoice.getCogsAccount(),
                                "", "", "استلام مردود مبيعات");
                    }
                }
            });
        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored2) { ignored2.printStackTrace(); JOptionPane.showMessageDialog(null, ignored2.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE); }
            throw new RuntimeException(e);
        }
    }

    public static boolean postSalesReturn(Connection conn, SalesReturnInvoice invoice,
                                              double quantity, String itemCode, String itemName) {
        return postSalesReturn(conn, new SalesReturnInvoice(invoice.getInvoiceCode(), invoice.getOriginalInvoiceCode(), invoice.getReturnDate(),
                invoice.getCustomerAccount(), invoice.getSalesReturnAccount(), invoice.getTaxAccount(),
                invoice.getFinishedGoodsAccount(), invoice.getCogsAccount(),
                invoice.getReturnAmount(), invoice.getInventoryCost(), invoice.isTaxApplied(), invoice.getTaxRate(),
                invoice.getReturnReason(), invoice.getBatchNo()));
    }

    private static void saveSalesReturnNote(Connection conn, SalesReturnInvoice invoice) throws SQLException {
        String sql = "INSERT INTO sales_return_notes (return_code, customer_account, sales_return_account, finished_goods_account, cogs_account, total_amount) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoice.getInvoiceCode());
            ps.setString(2, invoice.getCustomerAccount());
            ps.setString(3, invoice.getSalesReturnAccount());
            ps.setString(4, invoice.getFinishedGoodsAccount());
            ps.setString(5, invoice.getCogsAccount());
            ps.setDouble(6, invoice.getTotalCustomerCredit());
            ps.executeUpdate();
        }
    }

    private static void saveMovement(Connection connection, String documentNumber, String date, String type,
                                          String itemCode, String itemName, double quantity, double unitCost,
                                          String inventoryAccount, String counterAccount) throws SQLException {
        if (quantity > 0) {
            InventoryPostingService.receiveInTransaction(connection, documentNumber, itemCode, itemName,
                    quantity, unitCost, inventoryAccount, counterAccount, "", "", type);
        } else {
            InventoryPostingService.issueInTransaction(connection, documentNumber, itemCode, itemName,
                    -quantity, unitCost, inventoryAccount, counterAccount, "", "", type);
        }
    }

    private static void requirePositive(double value, String field) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
            throw new IllegalArgumentException(field + " يجب أن تكون موجبة.");
        }
    }

    private static double averageCost(String itemCode, double fallback) {
        String sql = "SELECT unit_cost FROM inventory_items WHERE item_code = ?";
        try (Connection connection = DatabaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemCode);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getDouble(1);
            }
            return fallback;
        } catch (SQLException exception) {
            throw new IllegalStateException("تعذر قراءة متوسط تكلفة الصنف: " + exception.getMessage(), exception);
        }
    }
}
