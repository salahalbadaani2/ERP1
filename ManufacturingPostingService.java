import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/** عمليات التصنيع المحاسبية في ظل نظام الجرد المستمر. */
public final class ManufacturingPostingService {
    private ManufacturingPostingService() {
    }

    private static boolean isDocumentPosted(String tableName, String codeColumn, String documentCode) {
        String sql = "SELECT is_posted FROM " + tableName + " WHERE " + codeColumn + " = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, documentCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_posted");
                }
            }
        } catch (SQLException e) {
            System.err.println("خطأ في فحص حالة المستند: " + e.getMessage());
        }
        return false;
    }

    private static boolean checkAlreadyPosted(String tableName, String codeColumn, String documentCode) {
        if (isDocumentPosted(tableName, codeColumn, documentCode)) {
            JOptionPane.showMessageDialog(null, "المستند مرحل سابقاً ولا يمكن إعادة اعتماده", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return true;
        }
        return false;
    }

    private static boolean isJournalEntryPosted(String entryNumber) {
        String sql = "SELECT COUNT(*) as cnt FROM journal_entries WHERE entry_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entryNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt("cnt") > 0) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("خطأ في فحص حالة القيد: " + e.getMessage());
        }
        return false;
    }

    private static void markDocumentPosted(Connection connection, String tableName, String codeColumn, String documentCode) throws SQLException {
        String sql = "UPDATE " + tableName + " SET is_posted = TRUE WHERE " + codeColumn + " = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, documentCode);
            stmt.executeUpdate();
        }
    }

    public static boolean postMaterialIssue(MaterialIssueNote note, String wipAccount, String rawMaterialAccount) {
        requireNote(note);
        if (checkAlreadyPosted("material_issue_notes", "issue_code", note.getNoteCode())) {
            return false;
        }
        JournalEntry entry = new JournalEntry(
                "JV-MI-" + note.getNoteCode(), note.getNoteCode(), "INVENTORY",
                "صرف مواد خام إلى أمر الإنتاج: " + note.getNoteCode());
        entry.addDebitLine(wipAccount, "إنتاج تحت التشغيل", "تحميل مواد خام على الإنتاج", note.getTotalValue());
        entry.addCreditLine(rawMaterialAccount, "مخزون المواد الخام", "صرف المواد الخام للإنتاج", note.getTotalValue());

        return PostingEngine.postJournalEntry(entry, connection -> {
            insertMaterialIssue(connection, note, wipAccount, rawMaterialAccount);
            markDocumentPosted(connection, "material_issue_notes", "issue_code", note.getNoteCode());
        });
    }

    public static boolean postFinishedGoods(ProductionCompletionNote note) {
        if (note == null) {
            throw new IllegalArgumentException("خطأ ترحيل: إذن إضافة المنتج التام مطلوب.");
        }
        if (checkAlreadyPosted("finished_goods_notes", "note_code", note.getNoteCode())) {
            return false;
        }
        JournalEntry entry = new JournalEntry(
                "JV-PN-" + note.getNoteCode(), note.getNoteCode(), "INVENTORY",
                "إثبات إنتاج تام من أمر الإنتاج: " + note.getNoteCode());
        entry.addDebitLine(note.getFinishedCode(), "مخزون المنتجات التامة", "إضافة المنتج التام للمخزن", note.getTotalValue());
        entry.addCreditLine(note.getWipItemCode(), "إنتاج تحت التشغيل", "إقفال تكلفة أمر الإنتاج", note.getTotalValue());

        return PostingEngine.postJournalEntry(entry, connection -> {
            insertFinishedGoods(connection, note);
            markDocumentPosted(connection, "finished_goods_notes", "note_code", note.getNoteCode());
        });
    }
    public static boolean postGoodsReceipt(GoodsReceiptNote note) {
        if (note == null) {
            throw new IllegalArgumentException("خطأ ترحيل: إذن استلام المواد مطلوب.");
        }
        if (checkAlreadyPosted("goods_receipt_notes", "grn_code", note.getNoteCode())) {
            return false;
        }
        JournalEntry entry = new JournalEntry(
                "JV-GRN-" + note.getNoteCode(), note.getNoteCode(), "INVENTORY",
                "استلام مواد خام من المورد: " + note.getNoteCode());
        entry.addDebitLine(note.getRawItemCode(), "مخزون المواد الخام", "إضافة المواد الخام للمخزن", note.getTotalValue());
        entry.addCreditLine(note.getVendorCode(), "الموردون", "إثبات الالتزام بالمورد", note.getTotalValue());

        return PostingEngine.postJournalEntry(entry, connection -> {
            insertGoodsReceipt(connection, note);
            markDocumentPosted(connection, "goods_receipt_notes", "grn_code", note.getNoteCode());
        });
    }

    public static boolean applyOverhead(String productionOrder, String wipAccount,
                                            String appliedOverheadAccount, double amount) {
        requireText(productionOrder, "رقم أمر الإنتاج");
        requirePositive(amount, "قيمة التكاليف الصناعية غير المباشرة");
        String entryNumber = "JV-FOH-" + productionOrder;
        if (isJournalEntryPosted(entryNumber)) {
            JOptionPane.showMessageDialog(null, "المستند مرحل سابقاً ولا يمكن إعادة اعتماده", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        JournalEntry entry = new JournalEntry(
                entryNumber, productionOrder, "INVENTORY",
                "تحميل التكاليف الصناعية غير المباشرة على أمر الإنتاج: " + productionOrder);
        entry.addDebitLine(wipAccount, "إنتاج تحت التشغيل", "تحميل FOH على أمر الإنتاج", amount);
        entry.addCreditLine(appliedOverheadAccount, "التكاليف الصناعية غير المباشرة المحملة", "إثبات FOH المحمل", amount);
        return PostingEngine.postJournalEntry(entry);
    }

    public static boolean closeOverheadVariance(OverheadClosing closing) {
        if (closing == null) {
            throw new IllegalArgumentException("خطأ إقفال: بيانات إقفال التكاليف مطلوبة.");
        }
        if (checkAlreadyPosted("overhead_closings", "closing_code", closing.getClosingCode())) {
            return false;
        }
        double variance = closing.getVarianceAmount();
        JournalEntry entry = new JournalEntry(
                "JV-OH-" + closing.getClosingCode(), closing.getClosingCode(), "INVENTORY",
                "إقفال التكاليف الصناعية وانحرافاتها للفترة: " + closing.getMonthPeriod());

        if (closing.getAppliedAmount() > 0) {
            entry.addDebitLine(closing.getAppliedAccount(), "التكاليف الصناعية المحملة",
                    "إقفال التكاليف الصناعية المحملة", closing.getAppliedAmount());
            entry.addCreditLine(closing.getActualAccount(), "التكاليف الصناعية الفعلية",
                    "إقفال التكاليف الصناعية المحملة مقابل الفعلية", closing.getAppliedAmount());
        }
        if (variance > 0) {
            entry.addDebitLine(closing.getCogsAccount(), "تكلفة المبيعات",
                    "إقفال انحراف نقص التحميل", variance);
            entry.addCreditLine(closing.getActualAccount(), "التكاليف الصناعية الفعلية",
                    "إقفال الجزء غير المحمل من التكاليف", variance);
        } else if (variance < 0) {
            entry.addDebitLine(closing.getActualAccount(), "التكاليف الصناعية الفعلية",
                    "إقفال انحراف زيادة التحميل", -variance);
            entry.addCreditLine(closing.getCogsAccount(), "تكلفة المبيعات",
                    "تخفيض تكلفة المبيعات بفائض التحميل", -variance);
        }
        if (!entry.isBalanced()) {
            throw new IllegalArgumentException("خطأ إقفال: لا ينتج عن بيانات التكاليف قيد متزن.");
        }

        return PostingEngine.postJournalEntry(entry, connection -> {
            insertOverheadClosing(connection, closing);
            markDocumentPosted(connection, "overhead_closings", "closing_code", closing.getClosingCode());
        });
    }

    private static void insertMaterialIssue(Connection connection, MaterialIssueNote note,
                                            String wipAccount, String rawMaterialAccount) throws SQLException {
        String sql = "INSERT INTO material_issue_notes "
                + "(issue_code, wip_account, raw_material_account, total_amount) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, note.getNoteCode());
            statement.setString(2, wipAccount);
            statement.setString(3, rawMaterialAccount);
            statement.setDouble(4, note.getTotalValue());
            statement.executeUpdate();
        }
    }

    private static void insertFinishedGoods(Connection connection, ProductionCompletionNote note) throws SQLException {
        String sql = "INSERT INTO finished_goods_notes "
                + "(note_code, finished_goods_account, wip_account, total_amount) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, note.getNoteCode());
            statement.setString(2, note.getFinishedCode());
            statement.setString(3, note.getWipItemCode());
            statement.setDouble(4, note.getTotalValue());
            statement.executeUpdate();
        }
    }

    private static void insertGoodsReceipt(Connection connection, GoodsReceiptNote note) throws SQLException {
        String sql = "INSERT INTO goods_receipt_notes "
                + "(grn_code, supplier_account, raw_material_account, total_amount) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, note.getNoteCode());
            statement.setString(2, note.getVendorCode());
            statement.setString(3, note.getRawItemCode());
            statement.setDouble(4, note.getTotalValue());
            statement.executeUpdate();
        }
    }

    private static void insertOverheadClosing(Connection connection, OverheadClosing closing) throws SQLException {
        String sql = "INSERT INTO overhead_closings "
                + "(closing_code, actual_account, applied_account, cogs_account, actual_amount, applied_amount, month_year) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, closing.getClosingCode());
            statement.setString(2, closing.getActualAccount());
            statement.setString(3, closing.getAppliedAccount());
            statement.setString(4, closing.getCogsAccount());
            statement.setDouble(5, closing.getActualAmount());
            statement.setDouble(6, closing.getAppliedAmount());
            statement.setString(7, closing.getMonthPeriod());
            statement.executeUpdate();
        }
    }

    private static void requireNote(MaterialIssueNote note) {
        if (note == null) {
            throw new IllegalArgumentException("خطأ ترحيل: إذن صرف المواد مطلوب.");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ محاسبي: " + field + " مطلوب.");
        }
    }

    private static void requirePositive(double value, String field) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
            throw new IllegalArgumentException("خطأ محاسبي: " + field + " يجب أن تكون موجبة.");
        }
    }
}