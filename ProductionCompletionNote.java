public class ProductionCompletionNote {
    private String noteCode;       // رقم إذن إضافة المنتج التام (مثلاً: PN-1001)
    private String wipItemCode;    // رقم حساب التشغيل (1210201)
    private String finishedCode;   // رقم حساب المنتج التام (1210301)
    private double quantity;       // الكمية المصنعة
    private double unitCost;       // تكلفة الوحدة

    public ProductionCompletionNote(String noteCode, String wipItemCode, String finishedCode, double quantity, double unitCost) {
        if (noteCode == null || noteCode.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ أمني: رقم إذن الإضافة مطلوب!");
        }
        if (quantity <= 0 || unitCost <= 0) {
            throw new IllegalArgumentException("خطأ محاسبي: الكمية والتكلفة يجب أن تكون أكبر من الصفر!");
        }
        this.noteCode = noteCode;
        this.wipItemCode = wipItemCode;
        this.finishedCode = finishedCode;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public double getTotalValue() {
        return this.quantity * this.unitCost;
    }

    public String getWipItemCode() { return wipItemCode; }
    public String getFinishedCode() { return finishedCode; }
    public double getQuantity() { return quantity; }
    public double getUnitCost() { return unitCost; }
    public String getNoteCode() { return noteCode; }

    public boolean postToAccounting() {
        return ManufacturingPostingService.postFinishedGoods(this);
    }

    public void printNote() {
        System.out.println("========================================");
        System.out.println("     إذن إضافة منتج تام (PN)           ");
        System.out.println("رقم الإذن: " + noteCode);
        System.out.println("من حساب التشغيل: " + wipItemCode);
        System.out.println("إلى حساب المنتج التام: " + finishedCode);
        System.out.println("الكمية المكتملة: " + quantity);
        System.out.println("إجمالي تكلفة الإنتاج: " + getTotalValue());
        System.out.println("========================================");
    }

    // الإضافة الجديدة الوحيدة: دالة تصدير القيد والتقرير لملف نصي بالعربية
    public void exportToTextFile() {
        try {
            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.File("ProductionCompletionLog.txt"), "UTF-8");
            
            writer.println("========================================");
            writer.println("     إذن إضافة منتج تام (PN)           ");
            writer.println("رقم الإذن: " + noteCode);
            writer.println("من حساب التشغيل (سكر قيد التشغيل): " + wipItemCode);
            writer.println("إلى حساب المنتج التام (عصير جاهز): " + finishedCode);
            writer.println("الكمية المكتملة: " + quantity);
            writer.println("إجمالي تكلفة الإنتاج: " + getTotalValue());
            writer.println("========================================");
            writer.println("   القيد المحاسبي المالي الصادر:        ");
            writer.println("من حـ/ مخزن الإنتاج التام - صنف عصير جاهز (" + finishedCode + "): " + getTotalValue());
            writer.println("إلى حـ/ مخزن الإنتاج تحت التشغيل - صنف سكر قيد التشغيل (" + wipItemCode + "): " + getTotalValue());
            writer.println("----------------------------------------");
            
            writer.close();
            System.out.println("Success: Production Completion Note Exported to ProductionCompletionLog.txt");
        } catch (Exception e) {
            System.out.println("Error: Cannot create log file.");
        }
    }
}