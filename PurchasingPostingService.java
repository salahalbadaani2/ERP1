import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** ترحيل فواتير المشتريات ومردوداتها في ظل الجرد المستمر. */
public final class PurchasingPostingService {
    private PurchasingPostingService() {
    }

    /**
     * ترحيل فاتورة مشتريات باتصال داخلي: إنشاء اتصال + بدء معاملة + ترحيل كامل.
     */
    public static boolean postPurchase(PurchaseInvoice invoice) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            boolean success = postPurchase(conn, invoice);
            return success;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            System.err.println("فشل ترحيل فاتورة المشتريات: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * ترحيل فاتورة مشتريات باستخدام اتصال موحد مُمرَّر من الخارج.
     * يفترض أن الاتصال تم فتحه بـ setAutoCommit(false) من الخارج.
     * يدعم كافة أسطر الجدول عبر قائمة invoice.getLines()
     */
    public static boolean postPurchase(Connection conn, PurchaseInvoice invoice) {
        try {
            java.util.List<PurchaseInvoice.PurchaseLine> lines = invoice.getLines();
            if (lines != null && !lines.isEmpty()) {
                double sum = 0;
                for (PurchaseInvoice.PurchaseLine l : lines) {
                    requireInventoryData(l.getItemCode(), l.getQuantity(), l.getUnitCost());
                    sum += l.getLineAmount();
                }
                if (Math.abs(sum - invoice.getAmount()) > 0.01) {
                    throw new IllegalArgumentException("إجمالي الفاتورة لا يطابق مجموع أسطر الأصناف.");
                }
            } else {
                requireInventoryData(invoice.getItemCode(), invoice.getQuantity(), invoice.getUnitCost());
                requireAmountMatchesQuantity(invoice.getAmount(), invoice.getQuantity(), invoice.getUnitCost());
            }

            JournalEntry entry = new JournalEntry("JV-PUR-" + invoice.getInvoiceCode(), invoice.getInvoiceCode(),
                    "INVENTORY", "إثبات شراء مواد خام من المورد: " + invoice.getInvoiceCode());
            entry.addDebitLine(invoice.getGrirAccount(), "مخزون المواد الخام",
                    "إضافة مواد خام للمخزون", invoice.getAmount());
            if (invoice.getTaxAmount() > 0) {
                entry.addDebitLine(invoice.getInputTaxAccount(), "ضريبة مشتريات مدخلات",
                        "إثبات ضريبة المدخلات", invoice.getTaxAmount());
            }
            entry.addCreditLine(invoice.getVendorAccount(), "حساب المورد",
                    "إثبات مستحقات المورد", invoice.getTotalVendorCredit());

            // ترحيل القيد اليومي + حفظ المستند + حركة المخزون داخل اتصال واحد
            return PostingEngine.postJournalEntry(conn, entry, connection -> {
                savePurchase(connection, invoice);
                if (lines != null && !lines.isEmpty()) {
                    for (PurchaseInvoice.PurchaseLine l : lines) {
                        InventoryPostingService.receiveInTransaction(connection, invoice.getInvoiceCode(),
                                l.getItemCode(), l.getItemCode(), l.getQuantity(),
                                l.getUnitCost(), invoice.getGrirAccount(), invoice.getVendorAccount(),
                                "", "", "استلام مشتريات");
                    }
                } else {
                    InventoryPostingService.receiveInTransaction(connection, invoice.getInvoiceCode(),
                            invoice.getItemCode(), invoice.getItemCode(), invoice.getQuantity(),
                            invoice.getUnitCost(), invoice.getGrirAccount(), invoice.getVendorAccount(),
                            "", "", "استلام مشتريات");
                }
            });
        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            throw new RuntimeException(e);
        }
    }

    /**
     * ترحيل مردود مشتريات باتصال داخلي.
     */
    public static boolean postPurchaseReturn(PurchaseReturnInvoice invoice) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            boolean success = postPurchaseReturn(conn, invoice);
            return success;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            System.err.println("فشل ترحيل مردود المشتريات: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * ترحيل مردود مشتريات باستخدام اتصال موحد مُمرَّر من الخارج.
     * يدعم كافة أسطر الجدول عبر قائمة invoice.getLines()
     */
    public static boolean postPurchaseReturn(Connection conn, PurchaseReturnInvoice invoice) {
        try {
            java.util.List<PurchaseReturnInvoice.ReturnLine> lines = invoice.getLines();
            if (lines != null && !lines.isEmpty()) {
                double sum = 0;
                for (PurchaseReturnInvoice.ReturnLine l : lines) {
                    requireInventoryData(l.getItemCode(), l.getQuantity(), l.getUnitCost());
                    sum += l.getLineAmount();
                }
                if (Math.abs(sum - invoice.getReturnAmount()) > 0.01) {
                    throw new IllegalArgumentException("إجمالي المرتجع لا يطابق مجموع أسطر الأصناف.");
                }
            } else {
                requireInventoryData(invoice.getItemCode(), invoice.getQuantity(), invoice.getUnitCost());
                requireAmountMatchesQuantity(invoice.getReturnAmount(), invoice.getQuantity(), invoice.getUnitCost());
            }

            JournalEntry entry = new JournalEntry("JV-PR-" + invoice.getInvoiceCode(), invoice.getInvoiceCode(),
                    "INVENTORY", "إثبات مردود مواد خام إلى المورد: " + invoice.getInvoiceCode());
            entry.addDebitLine(invoice.getVendorAccount(), "حساب المورد",
                    "تخفيض مستحقات المورد", invoice.getTotalVendorDebit());
            entry.addCreditLine(invoice.getGrirAccount(), "مخزون المواد الخام",
                    "إخراج المواد المرتجعة من المخزون", invoice.getReturnAmount());
            if (invoice.getTaxAmount() > 0) {
                entry.addCreditLine(invoice.getInputTaxAccount(), "ضريبة مشتريات مدخلات",
                        "عكس ضريبة المدخلات", invoice.getTaxAmount());
            }

            return PostingEngine.postJournalEntry(conn, entry, connection -> {
                savePurchaseReturn(connection, invoice);
                if (lines != null && !lines.isEmpty()) {
                    for (PurchaseReturnInvoice.ReturnLine l : lines) {
                        InventoryPostingService.issueInTransaction(connection, invoice.getInvoiceCode(),
                                l.getItemCode(), l.getItemCode(), l.getQuantity(),
                                l.getUnitCost(), invoice.getVendorAccount(), invoice.getGrirAccount(),
                                "", "", "صرف مردود مشتريات");
                    }
                } else {
                    InventoryPostingService.issueInTransaction(connection, invoice.getInvoiceCode(),
                            invoice.getItemCode(), invoice.getItemCode(), invoice.getQuantity(),
                            invoice.getUnitCost(), invoice.getVendorAccount(), invoice.getGrirAccount(),
                            "", "", "صرف مردود مشتريات");
                }
            });
        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            throw new RuntimeException(e);
        }
    }

    private static void savePurchase(Connection connection, PurchaseInvoice invoice) throws SQLException {
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