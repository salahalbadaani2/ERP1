import java.io.File;
import java.io.PrintWriter;

public class DeliveryNote {
    private String deliveryCode;       // رقم إذن التسليم (DN-1001)
    private String cogsAccount;        // حساب تكلفة المبيعات الفرعي (510101)
    private String finishedGoodAccount;// حساب المنتجات التامة الفرعي (1210301)
    private double quantity;           // الكمية المسلمة
    private double unitCost;           // تكلفة الوحدة

    public DeliveryNote(String deliveryCode, String cogsAccount, String finishedGoodAccount, double quantity, double unitCost) {
        if (deliveryCode == null || deliveryCode.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ أمني: رقم إذن التسليم مطلوب!");
        }
        if (quantity <= 0 || unitCost <= 0) {
            throw new IllegalArgumentException("خطأ محاسبي: الكمية والتكلفة يجب أن تكون أرقاماً موجبة أكبر من الصفر!");
        }

        // التحقق الآلي من الحسابات الفرعية بواسطة حارس الحسابات المركزي
        AccountValidator.validateSubAccount(cogsAccount, "حساب تكلفة المبيعات");
        AccountValidator.validateSubAccount(finishedGoodAccount, "حساب المنتجات التامة");

        this.deliveryCode = deliveryCode;
        this.cogsAccount = cogsAccount;
        this.finishedGoodAccount = finishedGoodAccount;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public double getTotalCost() {
        return this.quantity * this.unitCost;
    }

    // تصدير إذن التسليم والقيد المحاسبي إلى ملف نصي
    public void exportToTextFile() {
        try (PrintWriter writer = new PrintWriter(new File("DeliveryNoteLog.txt"), "UTF-8")) {
            writer.println("========================================");
            writer.println("      إذن تسليم مبيعات (Delivery Note)  ");
            writer.println("رقم الإذن: " + deliveryCode);
            writer.println("حساب تكلفة المبيعات: " + cogsAccount);
            writer.println("حساب المنتجات التامة: " + finishedGoodAccount);
            writer.println("الكمية المسلمة: " + quantity);
            writer.println("إجمالي التكلفة: " + getTotalCost());
            writer.println("========================================");
            writer.println("   القيد المحاسبي المالي الصادر:        ");
            writer.println("من حـ/ تكلفة المبيعات COGS (" + cogsAccount + "): " + getTotalCost());
            writer.println("إلى حـ/ مخزن المنتجات التامة (" + finishedGoodAccount + "): " + getTotalCost());
            writer.println("----------------------------------------");
            
            System.out.println("Success: Delivery Note Exported to DeliveryNoteLog.txt");
        } catch (Exception e) {
            System.out.println("Error: Cannot create log file.");
        }
    }
}