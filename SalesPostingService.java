import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** الترحيل الموحد للمبيعات ومردوداتها مع حركة المخزون الآلية. */
public final class SalesPostingService {
    private SalesPostingService() {
    }

    public static boolean postSale(String invoiceCode, String invoiceDate, String customerAccount,
                                   String revenueAccount, String taxAccount, String cogsAccount,
                                   String inventoryAccount, String itemCode, String itemName,
                                   double quantity, double salePrice, double unitCost, boolean taxApplied) {
        requirePositive(quantity, "الكمية");
        final double actualUnitCost = averageCost(itemCode, unitCost);
        requirePositive(actualUnitCost, "متوسط تكلفة الصنف");
        double base = quantity * salePrice;
        double tax = taxApplied ? base * 0.15 : 0;
        double total = base + tax;
        double inventoryValue = quantity * actualUnitCost;
        JournalEntry entry = new JournalEntry("JV-SALES-" + invoiceCode, invoiceCode, "SALES",
                "إثبات مبيعات وصرف مخزني: " + invoiceCode);
        entry.addDebitLine(customerAccount, "العميل", "إجمالي استحقاق المبيعات", total);
        entry.addCreditLine(revenueAccount, "إيراد المبيعات", "إيراد المبيعات", base);
        if (tax > 0) entry.addCreditLine(taxAccount, "ضريبة المبيعات", "ضريبة القيمة المضافة", tax);
        entry.addDebitLine(cogsAccount, "تكلفة المبيعات", "تكلفة البضاعة المباعة", inventoryValue);
        entry.addCreditLine(inventoryAccount, "مخزون المنتجات التامة", "صرف المنتج المباع", inventoryValue);
        return PostingEngine.postJournalEntry(entry, connection -> {
            saveMovement(connection, invoiceCode, invoiceDate, "SALE", itemCode, itemName, -quantity,
                    actualUnitCost, inventoryAccount, cogsAccount);
        });
    }

    public static boolean postSalesReturn(SalesReturnInvoice invoice, double quantity, String itemCode, String itemName) {
        if (invoice == null) throw new IllegalArgumentException("بيانات مردود المبيعات مطلوبة.");
        requirePositive(quantity, "كمية المرتجع");
        requirePositive(invoice.getInventoryCost(), "قيمة التكلفة المخزنية");
        double unitCost = invoice.getInventoryCost() / quantity;
        JournalEntry entry = new JournalEntry("JV-SR-" + invoice.getInvoiceCode(), invoice.getInvoiceCode(),
                "SALES_RETURN", "إثبات مردود مبيعات واستلام مخزني: " + invoice.getInvoiceCode());
        entry.addDebitLine(invoice.getSalesReturnAccount(), "مردودات المبيعات", "عكس إيراد المبيعات", invoice.getReturnAmount());
        if (invoice.isTaxApplied() && invoice.getTaxAmount() > 0) {
            entry.addDebitLine(invoice.getTaxAccount(), "ضريبة المبيعات", "عكس ضريبة المبيعات", invoice.getTaxAmount());
        }
        entry.addCreditLine(invoice.getCustomerAccount(), "العميل", "تخفيض استحقاق العميل", invoice.getTotalCustomerCredit());
        entry.addDebitLine(invoice.getFinishedGoodsAccount(), "مخزون المنتجات التامة", "استلام المرتجع", invoice.getInventoryCost());
        entry.addCreditLine(invoice.getCogsAccount(), "تكلفة المبيعات", "عكس تكلفة البضاعة", invoice.getInventoryCost());
        return PostingEngine.postJournalEntry(entry, connection -> saveMovement(connection, invoice.getInvoiceCode(),
                invoice.getReturnDate(), "SALES_RETURN", itemCode, itemName, quantity, unitCost,
                invoice.getFinishedGoodsAccount(), invoice.getCogsAccount()));
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
