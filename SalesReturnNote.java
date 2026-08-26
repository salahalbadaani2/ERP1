import java.io.File;
import java.io.PrintWriter;

public class SalesReturnNote {
    private String noteCode;             // رقم إذن المرتجع (SRN-1001)
    private String inventoryAccount;     // حساب مخزن المنتجات التامة الفرعي (1210301)[cite: 10]
    private String cogsAccount;          // حساب تكلفة البضاعة المباعة الفرعي (510101)[cite: 9]
    private double quantity;             // الكمية المرتجعة
    private double unitCost;             // تكلفة الوحدة الواحدة

    public SalesReturnNote(String noteCode, String inventoryAccount, String cogsAccount, double quantity, double unitCost) {
        if (noteCode == null || noteCode.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ أمني: رقم إذن المرتجع مطلوب!");
        }
        if (quantity <= 0 || unitCost <= 0) {
            throw new IllegalArgumentException("خطأ منطقي: الكمية والتكلفة يجب أن تكون أكبر من الصفر!");
        }

        // فحص الحسابات الفرعية عبر حارس الحسابات المركزي
        AccountValidator.validateSubAccount(inventoryAccount, "حساب مخزن المنتجات التامة");
        AccountValidator.validateSubAccount(cogsAccount, "حساب تكلفة البضاعة المباعة");

        this.noteCode = noteCode;
        this.inventoryAccount = inventoryAccount;
        this.cogsAccount = cogsAccount;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public double getTotalCost() {
        return quantity * unitCost;
    }

    // تصدير الإذن والقيد المخزني إلى ملف نصي
    public void exportToTextFile() {
        try (PrintWriter writer = new PrintWriter(new File("SalesReturnNoteLog.txt"), "UTF-8")) {
            writer.println("========================================");
            writer.println("   إذن مرتجع مبيعات مخزني (Sales Return Note) ");
            writer.println("رقم الإذن: " + noteCode);
            writer.println("حساب مخزن المنتجات التامة: " + inventoryAccount);
            writer.println("حساب تكلفة البضاعة المباعة: " + cogsAccount);
            writer.println("الكمية المرتجعة: " + quantity);
            writer.println("إجمالي التكلفة المرتجعة للمخزن: " + getTotalCost());
            writer.println("========================================");
            writer.println("   القيد المخزني الصادر:        ");
            writer.println("من حـ/ مخزن المنتجات التامة (" + inventoryAccount + "): " + getTotalCost());
            writer.println("إلى حـ/ تكلفة البضاعة المباعة (" + cogsAccount + "): " + getTotalCost());
            writer.println("----------------------------------------");
            
            System.out.println("Success: Sales Return Note Exported to SalesReturnNoteLog.txt");
        } catch (Exception e) {
            System.out.println("Error: Cannot create log file.");
        }
    }
}