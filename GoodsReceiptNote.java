public class GoodsReceiptNote {
    private String noteCode;       // رقم إذن التوريد (مثلاً: GRN-1001)
    private String rawItemCode;    // رقم حساب المواد الخام الفرعي (1210101)
    private String vendorCode;     // رقم حساب المورد أو الوسيط (يبدأ بـ 22: 220101)
    private double quantity;       // الكمية الموردة للمخزن
    private double unitPrice;      // سعر توريد الوحدة

    public GoodsReceiptNote(String noteCode, String rawItemCode, String vendorCode, double quantity, double unitPrice) {
        if (noteCode == null || noteCode.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ أمني: رقم إذن التوريد مطلوب!");
        }
        if (!vendorCode.startsWith("22")) {
            throw new IllegalArgumentException("خطأ محاسبي: حساب الموردين يجب أن يبدأ بالرمز 22!");
        }
        if (quantity <= 0 || unitPrice <= 0) {
            throw new IllegalArgumentException("خطأ محاسبي: الكمية والسعر يجب أن تكون أرقاماً موجبة أكبر من الصفر!");
        }
        this.noteCode = noteCode;
        this.rawItemCode = rawItemCode;
        this.vendorCode = vendorCode;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public double getTotalValue() {
        return this.quantity * this.unitPrice;
    }

    public String getNoteCode() { return noteCode; }
    public String getRawItemCode() { return rawItemCode; }
    public String getVendorCode() { return vendorCode; }
    public double getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public boolean postToAccounting() {
        return ManufacturingPostingService.postGoodsReceipt(this);
    }

    public void printNote() {
        System.out.println("========================================");
        System.out.println("        إذن توريد مخزني (GRN)          ");
        System.out.println("رقم الإذن: " + noteCode);
        System.out.println("حساب المواد الخام: " + rawItemCode);
        System.out.println("حساب المورد: " + vendorCode);
        System.out.println("الكمية الموردة: " + quantity);
        System.out.println("إجمالي القيمة الموردة: " + getTotalValue());
        System.out.println("========================================");
    }

    // تصدير إذن التوريد والقيد المحاسبي إلى ملف نصي بالعربية
    public void exportToTextFile() {
        try {
            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.File("GoodsReceiptLog.txt"), "UTF-8");
            
            writer.println("========================================");
            writer.println("        إذن توريد مخزني (GRN)          ");
            writer.println("رقم الإذن: " + noteCode);
            writer.println("حساب المواد الخام (سكر خام): " + rawItemCode);
            writer.println("حساب المورد / الوسيط: " + vendorCode);
            writer.println("الكمية الموردة: " + quantity);
            writer.println("إجمالي القيمة الموردة: " + getTotalValue());
            writer.println("========================================");
            writer.println("   القيد المحاسبي المالي الصادر:        ");
            writer.println("من حـ/ مخزن المواد الخام - سكر خام (" + rawItemCode + "): " + getTotalValue());
            writer.println("إلى حـ/ الموردين / وسيط استلام البضائع (" + vendorCode + "): " + getTotalValue());
            writer.println("----------------------------------------");
            
            writer.close();
            System.out.println("Success: Goods Receipt Note Exported to GoodsReceiptLog.txt");
        } catch (Exception e) {
            System.out.println("Error: Cannot create log file.");
        }
    }
}