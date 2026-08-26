import java.io.File;
import java.io.PrintWriter;

public class PurchaseReturnNote {
    private String noteCode;             // رقم الإذن (PRN-1001)
    private String grirAccount;          // حساب وسيط استلام البضائع GR/IR الفرعي (220101)
    private String inventoryAccount;     // حساب مخزن المواد الخام الفرعي (1210101)
    private double quantity;             // الكمية المرتجعة
    private double unitCost;             // تكلفة الوحدة الواحدة

    public PurchaseReturnNote(String noteCode, String grirAccount, String inventoryAccount, double quantity, double unitCost) {
        if (noteCode == null || noteCode.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ أمني: رقم إذن مرتجع المشتريات مطلوب!");
        }
        if (quantity <= 0 || unitCost <= 0) {
            throw new IllegalArgumentException("خطأ منطقي: الكمية والتكلفة يجب أن تكون أكبر من الصفر!");
        }

        // فحص الحسابات الفرعية عبر حارس الحسابات المركزي
        AccountValidator.validateSubAccount(grirAccount, "حساب وسيط استلام البضائع GR/IR");
        AccountValidator.validateSubAccount(inventoryAccount, "حساب مخزن المواد الخام");

        this.noteCode = noteCode;
        this.grirAccount = grirAccount;
        this.inventoryAccount = inventoryAccount;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public double getTotalCost() {
        return quantity * unitCost;
    }

    // تصدير الإذن والقيد المخزني إلى ملف نصي
    public void exportToTextFile() {
        try (PrintWriter writer = new PrintWriter(new File("PurchaseReturnNoteLog.txt"), "UTF-8")) {
            writer.println("========================================");
            writer.println("   إذن مرتجع مشتريات مخزني (Purchase Return Note) ");
            writer.println("رقم الإذن: " + noteCode);
            writer.println("حساب وسيط استلام البضائع GR/IR: " + grirAccount);
            writer.println("حساب مخزن المواد الخام: " + inventoryAccount);
            writer.println("الكمية المرتجعة: " + quantity);
            writer.println("إجمالي التكلفة المرتجعة من المخزن: " + getTotalCost());
            writer.println("========================================");
            writer.println("   القيد المخزني الصادر:        ");
            writer.println("من حـ/ وسيط استلام البضائع GR/IR (" + grirAccount + "): " + getTotalCost());
            writer.println("إلى حـ/ مخزن المواد الخام (" + inventoryAccount + "): " + getTotalCost());
            writer.println("----------------------------------------");
            
            System.out.println("Success: Purchase Return Note Exported to PurchaseReturnNoteLog.txt");
        } catch (Exception e) {
            System.out.println("Error: Cannot create log file.");
        }
    }
}