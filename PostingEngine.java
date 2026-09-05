import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 * ============================================================================
 * نظام ERP المصنعي - محرك الترحيل المحاسبي المركزي (PostingEngine)
 * ============================================================================
 * 1. ترحيل القيود إلى جداول journal_entries و journal_entry_lines.
 * 2. تحديث أرصدة الحسابات الفرعية في chart_of_accounts تلقائياً.
 * 3. تنفيذ العمليات داخل اتصال واحد ومعاملة ذرية موحدة (Single Transaction Scope).
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
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            return postJournalEntryInternal(conn, entry, transactionWork);
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored2) { ignored2.printStackTrace(); JOptionPane.showMessageDialog(null, ignored2.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE); }
            }
            System.err.println("فشل ترحيل القيد المحاسبي: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored2) { ignored2.printStackTrace(); JOptionPane.showMessageDialog(null, ignored2.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE); }
            }
        }
    }

    /**
     * ترحيل قيد باستخدام اتصال موحد مُمرَّر من الخارج.
     * يُدار الاتصال والمعاملة من الجهة المستدعي.
     */
    public static synchronized boolean postJournalEntry(Connection conn, JournalEntry entry) {
        return postJournalEntry(conn, entry, null);
    }

    /**
     * ترحيل قيد باستخدام اتصال موحد مُمرَّر من الخارج، مع تنفيذ عمل إضافي داخل نفس المعاملة.
     * يفترض أن الاتصال تم فتحه بـ setAutoCommit(false) من الخارج.
     */
    public static synchronized boolean postJournalEntry(Connection conn, JournalEntry entry, TransactionWork transactionWork) {
        try {
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
            String sqlUpdateBalance = "UPDATE chart_of_accounts SET current_balance = current_balance + ? WHERE account_code = ?";

            try (PreparedStatement duplicateCheck = conn.prepareStatement(
                    "SELECT COUNT(*) FROM journal_entries WHERE entry_number = ?")) {
                duplicateCheck.setString(1, entry.getEntryNumber());
                try (ResultSet rs = duplicateCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
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
                    pstmtLine.setLong(1, entryId);
                    pstmtLine.setString(2, line.getAccountCode());
                    pstmtLine.setString(3, line.getLineNarration());
                    pstmtLine.setDouble(4, line.getDebitAmount());
                    pstmtLine.setDouble(5, line.getCreditAmount());
                    pstmtLine.addBatch();

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

            conn.commit();
            System.out.println("تم ترحيل القيد المحاسبي [" + entry.getEntryNumber() + "] وتحديث الأرصدة بنجاح.");
            return true;

        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored2) { ignored2.printStackTrace(); JOptionPane.showMessageDialog(null, ignored2.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE); }
            System.err.println("فشل ترحيل القيد المحاسبي: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * ترحيل مبيعات موحد باتصال واحد: حفظ الفاتورة + القيد اليومي + حركة المخزون.
     */
    public static boolean postSalesReturn(Connection conn, SalesReturnInvoice invoice) {
        try {
            String entryNo = "JV-SR-" + invoice.getInvoiceCode();
            String narration = "إثبات مردودات مبيعات للفاتورة: " + invoice.getOriginalInvoiceCode()
                    + " - " + invoice.getReturnReason();

            JournalEntry jv = new JournalEntry(entryNo, invoice.getInvoiceCode(), "SALES_RETURN", narration);

            jv.addDebitLine(invoice.getSalesReturnAccount(), "مردودات ومسموحات المبيعات",
                    "قيمة المرتجع الأساسية", invoice.getReturnAmount());
            if (invoice.isTaxApplied() && invoice.getTaxAmount() > 0) {
                jv.addDebitLine(invoice.getTaxAccount(), "أمانات ضريبة المبيعات",
                        "ضريبة المرتجع المستردة", invoice.getTaxAmount());
            }
            jv.addCreditLine(invoice.getCustomerAccount(), "حساب العميل",
                    "استحقاق مرتجع للعميل", invoice.getTotalCustomerCredit());

            if (invoice.getInventoryCost() > 0) {
                jv.addDebitLine(invoice.getFinishedGoodsAccount(), "مخزن المنتجات التامة",
                        "استرداد بضاعة تامة للمخزن", invoice.getInventoryCost());
                jv.addCreditLine(invoice.getCogsAccount(), "تكلفة البضاعة المباعة COGS",
                        "تخفيض تكلفة المبيعات المستردة", invoice.getInventoryCost());
            }

            // حفظ الفاتورة في جدول sales_return_notes داخل نفس الاتصال
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO sales_return_notes (return_code, customer_account, sales_return_account, finished_goods_account, cogs_account, total_amount) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, invoice.getInvoiceCode());
                ps.setString(2, invoice.getCustomerAccount());
                ps.setString(3, invoice.getSalesReturnAccount());
                ps.setString(4, invoice.getFinishedGoodsAccount());
                ps.setString(5, invoice.getCogsAccount());
                ps.setDouble(6, invoice.getTotalCustomerCredit());
                ps.executeUpdate();
            }

            // ترحيل القيد وتحديث الأرصدة داخل نفس الاتصال والمعاملة
            return postJournalEntry(conn, jv);
        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored2) { ignored2.printStackTrace(); JOptionPane.showMessageDialog(null, ignored2.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE); }
            System.err.println("فشل ترحيل مردودات المبيعات: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * ترحيل تلقائي لفاتورة مردودات المبيعات (Sales Return Invoice) - باتصال داخلي.
     */
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
     * تجربة ترحيل ذاتية من الطرفية (Main Method)
     */
    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("    نظام ERP المصنعي - اختبار محرك الترحيل المحاسبي");
        System.out.println("==========================================================");

        SalesReturnInvoice sri = new SalesReturnInvoice(
                "SRI-TEST-01",
                "INV-1001",
                java.time.LocalDate.now().toString(),
                "123020002",
                "410201",
                "220301",
                "1210301",
                "510101",
                500.0,
                350.0,
                true,
                0.15,
                "تالف أثناء النقل",
                "BATCH-08"
        );

        boolean success = PostingEngine.postSalesReturn(sri);
        if (success) {
            System.out.println("\nنجحت عملية الترحيل وانعكست القيود على قاعدة البيانات والأرصدة الدفترية.");
        }
    }

    /**
     * تنفيذ المعاملة الكاملة داخل اتصال واحد: فتح + بدء + تنفيذ + التزام أو تراجع + تنظيف.
     */
    private static boolean postJournalEntryInternal(Connection conn, JournalEntry entry, TransactionWork transactionWork) {
        try {
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
            String sqlUpdateBalance = "UPDATE chart_of_accounts SET current_balance = current_balance + ? WHERE account_code = ?";

            try (PreparedStatement duplicateCheck = conn.prepareStatement(
                    "SELECT COUNT(*) FROM journal_entries WHERE entry_number = ?")) {
                duplicateCheck.setString(1, entry.getEntryNumber());
                try (ResultSet rs = duplicateCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return false;
                    }
                }
            }

            validatePostingAccounts(conn, entry);

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

            try (PreparedStatement pstmtLine = conn.prepareStatement(sqlLine);
                 PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdateBalance)) {

                for (JournalEntry.JournalLine line : entry.getLines()) {
                    pstmtLine.setLong(1, entryId);
                    pstmtLine.setString(2, line.getAccountCode());
                    pstmtLine.setString(3, line.getLineNarration());
                    pstmtLine.setDouble(4, line.getDebitAmount());
                    pstmtLine.setDouble(5, line.getCreditAmount());
                    pstmtLine.addBatch();

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

            conn.commit();
            System.out.println("تم ترحيل القيد المحاسبي [" + entry.getEntryNumber() + "] وتحديث الأرصدة بنجاح.");
            return true;

        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored2) { ignored2.printStackTrace(); JOptionPane.showMessageDialog(null, ignored2.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE); }
            System.err.println("فشل ترحيل القيد المحاسبي: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}