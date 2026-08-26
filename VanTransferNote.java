import java.io.File;
import java.io.PrintWriter;

public class VanTransferNote {
    private String transferCode;             // رقم إذن التحويل (VTN-1001)
    private String fromFinishedGoodsAccount; // حساب مخزن المنتجات التامة الفرعي (1210301)
    private String toVanAccount;             // حساب مخزن سيارة التوزيع الفرعي (1210401)
    private double quantity;                 // الكمية المحولة
    private double unitCost;                 // تكلفة الوحدة

    public VanTransferNote(String transferCode, String fromFinishedGoodsAccount, String toVanAccount, double quantity, double unitCost) {
        if (transferCode == null || transferCode.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ أمني: رقم إذن التحويل مطلوب!");
        }
        if (quantity <= 0 || unitCost <= 0) {
            throw new IllegalArgumentException("خطأ محاسبي: الكمية والتكلفة يجب أن تكون أرقاماً موجبة أكبر من الصفر!");
        }

        // التحقق الآلي من الحسابات الفرعية بواسطة حارس الحسابات المركزي
        AccountValidator.validateSubAccount(fromFinishedGoodsAccount, "حساب مخزن المنتجات التامة المحول منه");
        AccountValidator.validateSubAccount(toVanAccount, "حساب مخزن سيارة المندوب المحول إليه");

        this.transferCode = transferCode;
        this.fromFinishedGoodsAccount = fromFinishedGoodsAccount;
        this.toVanAccount = toVanAccount;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public double getTotalValue() {
        return this.quantity * this.unitCost;
    }

    public boolean postToDatabase() {
        JournalEntry entry = new JournalEntry("JV-VT-" + transferCode, transferCode, "INVENTORY",
                "تحويل بضاعة إلى مخزن المندوب: " + transferCode);
        entry.addDebitLine(toVanAccount, "مخزن سيارة المندوب", "إضافة بضاعة للمندوب", getTotalValue());
        entry.addCreditLine(fromFinishedGoodsAccount, "مخزن المنتجات التامة", "تحويل بضاعة إلى المندوب", getTotalValue());
        if (!PostingEngine.postJournalEntry(entry)) return false;
        DatabaseManager.insertVanTransferNote(transferCode, toVanAccount, fromFinishedGoodsAccount, quantity, unitCost);
        return true;
    }

    // تصدير إذن التحويل والقيد المحاسبي إلى ملف نصي
    public void exportToTextFile() {
        try (PrintWriter writer = new PrintWriter(new File("VanTransferNoteLog.txt"), "UTF-8")) {
            writer.println("========================================");
            writer.println("  إذن تحويل لمخزن سيارة المندوب (Van Transfer) ");
            writer.println("رقم الإذن: " + transferCode);
            writer.println("من حساب مخزن المنتجات التامة: " + fromFinishedGoodsAccount);
            writer.println("إلى حساب مخزن سيارة المندوب: " + toVanAccount);
            writer.println("الكمية المحولة: " + quantity);
            writer.println("إجمالي قيمة التحويل: " + getTotalValue());
            writer.println("========================================");
            writer.println("   القيد المحاسبي المالي الصادر:        ");
            writer.println("من حـ/ مخزن سيارة المندوب (" + toVanAccount + "): " + getTotalValue());
            writer.println("إلى حـ/ مخزن المنتجات التامة (" + fromFinishedGoodsAccount + "): " + getTotalValue());
            writer.println("----------------------------------------");
            
            System.out.println("Success: Van Transfer Note Exported to VanTransferNoteLog.txt");
        } catch (Exception e) {
            System.out.println("Error: Cannot create log file.");
        }
    }
}