import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class MaterialIssueNote {
    private String noteCode;     // رقم إذن الصرف (مثلاً: IN-1001)
    private String itemCode;     // رقم الحساب الفرعي للصنف
    private double quantity;     // الكمية المصروفة لصالة الإنتاج
    private double unitPrice;    // سعر الوحدة

    // منشئ إذن الصرف مع الفحص الأمني
    public MaterialIssueNote(String noteCode, String itemCode, double quantity, double unitPrice) {
        if (noteCode == null || noteCode.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ أمني: رقم إذن الصرف مطلوب!");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("خطأ محاسبي: يجب أن تكون الكمية المصروفة أكبر من الصفر!");
        }

        this.noteCode = noteCode;
        this.itemCode = itemCode;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // حساب إجمالي قيمة المواد التحويلية
    public double getTotalValue() {
        return this.quantity * this.unitPrice;
    }

    public String getItemCode() { return itemCode; }
    public double getQuantity() { return quantity; }
    public String getNoteCode() { return noteCode; }

    public boolean isPosted() {
        String sql = "SELECT is_posted FROM material_issue_notes WHERE issue_code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, noteCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_posted");
                }
            }
        } catch (SQLException e) {
            System.err.println("خطأ في فحص حالة الإذن: " + e.getMessage());
        }
        return false;
    }

    public boolean postToAccounting(String wipAccount, String rawMaterialAccount) {
        if (isPosted()) {
            JOptionPane.showMessageDialog(null, "المستند مرحل سابقاً ولا يمكن إعادة اعتماده", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        boolean success = ManufacturingPostingService.postMaterialIssue(this, wipAccount, rawMaterialAccount);
        if (!success) {
            JOptionPane.showMessageDialog(null, "المستند مرحل سابقاً ولا يمكن إعادة اعتماده", "تنبيه", JOptionPane.WARNING_MESSAGE);
        }
        return success;
    }

    // عرض تفاصيل إذن الصرف
    public void printNote() {
        System.out.println("========================================");
        System.out.println("      إذن صرف مواد للإنتاج (IN)        ");
        System.out.println("رقم الإذن: " + noteCode);
        System.out.println("رقم الصنف الفرعي: " + itemCode);
        System.out.println("الكمية المصروفة: " + quantity);
        System.out.println("إجمالي القيمة المحولة: " + getTotalValue());
        System.out.println("========================================");
    }

    // 1. طباعة القيد المحاسبي المالي لصرف المواد الخام للإنتاج
    public void printAccountingEntries() {
        System.out.println("----------------------------------------");
        System.out.println("   قيد صرف المواد للإنتاج (WIP Log)     ");
        System.out.println("من حـ/ مخزن الإنتاج تحت التشغيل (1210201): " + getTotalValue());
        System.out.println("إلى حـ/ مخزن المواد الخام - سكر خام (" + itemCode + "): " + getTotalValue());
        System.out.println("----------------------------------------");
    }

    // 2. تصدير إذن الصرف والقيد إلى ملف نصي لعرضه بالعربية بوضوح
    public void exportToTextFile() {
        try {
            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.File("IssueNoteLog.txt"), "UTF-8");
            
            writer.println("========================================");
            writer.println("      إذن صرف مواد للإنتاج (IN)        ");
            writer.println("رقم الإذن: " + noteCode);
            writer.println("رقم الصنف الفرعي: " + itemCode);
            writer.println("الكمية المصروفة: " + quantity);
            writer.println("إجمالي القيمة المحولة: " + getTotalValue());
            writer.println("========================================");
            writer.println("   القيد المحاسبي المالي الصادر:        ");
            writer.println("من حـ/ مخزن الإنتاج تحت التشغيل (1210201): " + getTotalValue());
            writer.println("إلى حـ/ مخزن المواد الخام - سكر خام (" + itemCode + "): " + getTotalValue());
            writer.println("----------------------------------------");
            
            writer.close();
            System.out.println("Success: Material Issue Note Exported to IssueNoteLog.txt");
        } catch (Exception e) {
            System.out.println("Error: Cannot create issue note log file.");
        }
    }
}