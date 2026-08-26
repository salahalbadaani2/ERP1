import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * ============================================================================
 * نظام ERP المصنعي - محرك الترحيل المحاسبي المركزي (PostingEngine)
 * ============================================================================
 * 1. ترحيل القيود إلى جداول journal_entries و journal_entry_lines.
 * 2. تحديث أرصدة الحسابات الفرعية في chart_of_accounts تلقائياً.
 * 3. تنفيذ العمليات داخل Atomic SQL Transaction (Commit / Rollback).
 * 4. دوال ترحيل سريعة لفواتير المبيعات والمردودات وسندات الخزينة.
 */
public class PostingEngine {

    @FunctionalInterface
    public interface TransactionWork {
        void execute(Connection connection) throws SQLException;
    }

    /**
     * ترحيل سند قيد عام إلى قاعدة البيانات وتحديث الأرصدة
     */
    public static synchronized boolean postJournalEntry(JournalEntry entry) {
        return postJournalEntry(entry, null);
    }

    /** ترحيل القيد مع حفظ المستند المرتبط داخل نفس المعاملة الذرية. */
    public static synchronized boolean postJournalEntry(JournalEntry entry, TransactionWork transactionWork) {
        if (entry == null) {
            throw new IllegalArgumentException("خطأ ترحيل: لا يمكن ترحيل قيد فارغ.");
        }
        if (!entry.isBalanced()) {
            System.err.println("خطأ ترحيل: القيد المحاسبي غير متزن. إجمالي المدين = "
                    + entry.getTotalDebit() + " لا يساوي إجمالي الدائن = " + entry.getTotalCredit());
            return false;
        }

        String sqlHeader = "INSERT INTO journal_entries (entry_number, entry_date, reference_doc, source_module, narration, total_debit, total_credit, posted_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'النظام الآلي')";

        String sqlLine = "INSERT INTO journal_entry_lines (entry_id, account_code, line_narration, debit_amount, credit_amount) " +
                "VALUES (?, ?, ?, ?, ?)";

        // استعلام تحديث رصيد الحساب بحسب نوعه (الأصول والمصروفات تزيد بالمدين، الخصوم والإيرادات تزيد بالدائن)
        String sqlUpdateBalance = "UPDATE chart_of_accounts SET current_balance = current_balance + ? WHERE account_code = ?";

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // بدء المعاملة الذرية (Transaction)

            try (PreparedStatement duplicateCheck = conn.prepareStatement(
                    "SELECT COUNT(*) FROM journal_entries WHERE entry_number = ?")) {
                duplicateCheck.setString(1, entry.getEntryNumber());
                try (ResultSet rs = duplicateCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            validatePostingAccounts(conn, entry);

            // 1. إدخال رأس القيد
            long entryId = 0;
            try (PreparedStatement pstmtHeader = conn.prepareStatement(sqlHeader, Statement.RETURN_GENERATED_KEYS)) {
                pstmtHeader.setString(1, entry.getEntryNumber());
                pstmtHeader.setString(2, entry.getEntryDate());
                pstmtHeader.setString(3, entry.getReferenceDoc());
                pstmtHeader.setString(4, entry.getSourceModule());
                pstmtHeader.setString(5, entry.getNarration());
                pstmtHeader.setDouble(6, entry.getTotalDebit());
                pstmtHeader.setDouble(7, entry.getTotalCredit());
                pstmtHeader.executeUpdate();

                ResultSet rs = pstmtHeader.getGeneratedKeys();
                if (rs.next()) {
                    entryId = rs.getLong(1);
                }
            }

            // 2. إدخال سطور القيد وتحديث أرصدة الحسابات
            try (PreparedStatement pstmtLine = conn.prepareStatement(sqlLine);
                 PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdateBalance)) {

                for (JournalEntry.JournalLine line : entry.getLines()) {
                    // إدراج السطر
                    pstmtLine.setLong(1, entryId);
                    pstmtLine.setString(2, line.getAccountCode());
                    pstmtLine.setString(3, line.getLineNarration());
                    pstmtLine.setDouble(4, line.getDebitAmount());
                    pstmtLine.setDouble(5, line.getCreditAmount());
                    pstmtLine.addBatch();

                    // حساب صافي التغيير في الرصيد
                    // الحسابات المدينة (1 الأصول، 5 المصروفات) تزيد بالمدين
                    // الحسابات الدائنة (2 الخصوم، 4 الإيرادات) تزيد بالدائن
                    double netImpact = 0;
                    String accCode = line.getAccountCode();
                    if (accCode.startsWith("1") || accCode.startsWith("5")) {
                        netImpact = line.getDebitAmount() - line.getCreditAmount();
                    } else {
                        netImpact = line.getCreditAmount() - line.getDebitAmount();
                    }

                    pstmtUpdate.setDouble(1, netImpact);
                    pstmtUpdate.setString(2, accCode);
                    pstmtUpdate.addBatch();
                }

                pstmtLine.executeBatch();
                pstmtUpdate.executeBatch();
            }

            if (transactionWork != null) {
                transactionWork.execute(conn);
            }

            conn.commit(); // اعتماد الترحيل نهائياً
            System.out.println("تم ترحيل القيد المحاسبي [" + entry.getEntryNumber() + "] وتحديث الأرصدة بنجاح.");
            return true;

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            System.err.println("فشل ترحيل القيد المحاسبي: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    private static void validatePostingAccounts(Connection conn, JournalEntry entry) throws SQLException {
        String sql = "SELECT account_level, is_sub_account FROM chart_of_accounts WHERE account_code = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            for (JournalEntry.JournalLine line : entry.getLines()) {
                statement.setString(1, line.getAccountCode());
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next() || rs.getInt("is_sub_account") != 1) {
                        throw new SQLException("الحساب " + line.getAccountCode()
                                + " غير موجود أو حساب رئيسي تجميعي.");
                    }
                }
            }
        }
    }

    /**
     * ترحيل تلقائي لفاتورة مردودات المبيعات (Sales Return Invoice)
     */
    public static boolean postSalesReturn(SalesReturnInvoice invoice) {
        String entryNo = "JV-SR-" + invoice.getInvoiceCode();
        String narration = "إثبات مردودات مبيعات للفاتورة: " + invoice.getOriginalInvoiceCode() + " - " + invoice.getReturnReason();

        JournalEntry jv = new JournalEntry(entryNo, invoice.getInvoiceCode(), "SALES_RETURN", narration);

        // 1. قيد تخفيض الإيراد والضريبة وقيد الاستحقاق للعميل
        jv.addDebitLine(invoice.getSalesReturnAccount(), "مردودات ومسموحات المبيعات", "قيمة المرتجع الأساسية", invoice.getReturnAmount());
        if (invoice.isTaxApplied() && invoice.getTaxAmount() > 0) {
            jv.addDebitLine(invoice.getTaxAccount(), "أمانات ضريبة المبيعات", "ضريبة المرتجع المستردة", invoice.getTaxAmount());
        }
        jv.addCreditLine(invoice.getCustomerAccount(), "حساب العميل", "استحقاق مرتجع للعميل", invoice.getTotalCustomerCredit());

        // 2. القيد المخزني لاسترداد المنتجات التامة وتخفيض COGS
        if (invoice.getInventoryCost() > 0) {
            jv.addDebitLine(invoice.getFinishedGoodsAccount(), "مخزن المنتجات التامة", "استرداد بضاعة تامة للمخزن", invoice.getInventoryCost());
            jv.addCreditLine(invoice.getCogsAccount(), "تكلفة البضاعة المباعة COGS", "تخفيض تكلفة المبيعات المستردة", invoice.getInventoryCost());
        }

        // حفظ الفاتورة في جدول sales_return_invoices أولاً
        invoice.saveToDatabase();

        // ترحيل القيد وتحديث الأرصدة
        return postJournalEntry(jv);
    }

    /**
     * تجربة ترحيل ذاتية من الطرفية (Main Method)
     */
    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("    نظام ERP المصنعي - اختبار محرك الترحيل المحاسبي");
        System.out.println("==========================================================");

        // إنشاء فاتورة مردودات تجريبية
        SalesReturnInvoice sri = new SalesReturnInvoice(
                "SRI-TEST-01",
                "INV-1001",
                java.time.LocalDate.now().toString(),
                "123020001", // شركة الأمل
                "410201",    // مردودات المبيعات
                "220301",    // ضريبة المبيعات
                "1210301",   // مخزن التام
                "510101",    // تكلفة المبيعات COGS
                500.0,       // قيمة المرتجع
                350.0,       // التكلفة المخزنية
                true,
                0.15,
                "تالف أثناء النقل",
                "BATCH-08"
        );

        // ترحيل القيد وتحديث شجرة الحسابات
        boolean success = PostingEngine.postSalesReturn(sri);
        if (success) {
            System.out.println("\nنجحت عملية الترحيل وانعكست القيود على قاعدة البيانات والأرصدة الدفترية.");
        }
    }
}