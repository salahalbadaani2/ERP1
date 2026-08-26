import java.io.File;
import java.io.PrintWriter;

public class VanReturnNote {
    private String returnCode;             // رقم إذن الارتجاع (VRN-1001)
    private String fromVanAccount;         // حساب مخزن سيارة المندوب الفرعي (1210401)
    private String toFinishedGoodsAccount; // حساب مخزن المنتجات التامة الفرعي (1210301)
    private double quantity;               // الكمية المرتجعة
    private double unitCost;               // تكلفة الوحدة

    public VanReturnNote(String returnCode, String fromVanAccount, String toFinishedGoodsAccount, double quantity, double unitCost) {
        if (returnCode == null || returnCode.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ أمني: رقم إذن ارتجاع المندوب مطلوب!");
        }
        if (quantity <= 0 || unitCost <= 0) {
            throw new IllegalArgumentException("خطأ محاسبي: الكمية والتكلفة يجب أن تكون أرقاماً موجبة أكبر من الصفر!");
        }

        // فحص الحسابات الفرعية بواسطة حارس الحسابات المركزي
        AccountValidator.validateSubAccount(fromVanAccount, "حساب مخزن سيارة المندوب المحول منه");
        AccountValidator.validateSubAccount(toFinishedGoodsAccount, "حساب مخزن المنتجات التامة المحول إليه");

        this.returnCode = returnCode;
        this.fromVanAccount = fromVanAccount;
        this.toFinishedGoodsAccount = toFinishedGoodsAccount;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public double getTotalValue() {
        return this.quantity * this.unitCost;
    }

    public boolean postToDatabase() {
        JournalEntry entry = new JournalEntry("JV-VR-" + returnCode, returnCode, "INVENTORY",
                "إرجاع بضاعة من مخزن المندوب: " + returnCode);
        entry.addDebitLine(toFinishedGoodsAccount, "مخزن المنتجات التامة", "استلام مرتجع المندوب", getTotalValue());
        entry.addCreditLine(fromVanAccount, "مخزن سيارة المندوب", "خصم المرتجع من المندوب", getTotalValue());
        if (!PostingEngine.postJournalEntry(entry)) return false;
        DatabaseManager.insertVanReturnNote(returnCode, toFinishedGoodsAccount, fromVanAccount, quantity, unitCost);
        return true;
    }

    // تصدير إذن الارتجاع والقيد المحاسبي إلى ملف نصي
    public void exportToTextFile() {
        try (PrintWriter writer = new PrintWriter(new File("VanReturnNoteLog.txt"), "UTF-8")) {
            writer.println("========================================");
            writer.println("  إذن ارتجاع متبقي سيارة المندوب (Van Return) ");
            writer.println("رقم الإذن: " + returnCode);
            writer.println("من حساب مخزن سيارة المندوب: " + fromVanAccount);
            writer.println("إلى حساب مخزن المنتجات التامة الرئيسي: " + toFinishedGoodsAccount);
            writer.println("الكمية المرتجعة: " + quantity);
            writer.println("إجمالي القيمة المرتجعة: " + getTotalValue());
            writer.println("========================================");
            writer.println("   القيد المحاسبي المالي الصادر:        ");
            writer.println("من حـ/ مخزن المنتجات التامة (" + toFinishedGoodsAccount + "): " + getTotalValue());
            writer.println("إلى حـ/ مخزن سيارة المندوب (" + fromVanAccount + "): " + getTotalValue());
            writer.println("----------------------------------------");
            
            System.out.println("Success: Van Return Note Exported to VanReturnNoteLog.txt");
        } catch (Exception e) {
            System.out.println("Error: Cannot create log file.");
        }
    }
}