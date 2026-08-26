public class RawMaterial {
    private String itemCode;
    private String itemName;
    private double quantity;
    private double unitPrice;

    public RawMaterial(String itemCode, String itemName, double quantity, double unitPrice) {
        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ أمني: رمز الصنف مطلوب!");
        }
        if (quantity < 0 || unitPrice < 0) {
            throw new IllegalArgumentException("خطأ محاسبي: الكمية والسعر يجب ألا تكون أرقاماً سالبة!");
        }
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public double getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getTotalValue() { return quantity * unitPrice; }

    // الدالة المضافة لتعديل الكمية مع الفحص الأمني
    public void setQuantity(double quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("خطأ محاسبي: لا يمكن جعل الكمية رقماً سالباً!");
        }
        this.quantity = quantity;
    }

    public void displayInfo() {
        System.out.println("رمز الصنف الفرعي: " + itemCode + " | اسم الصنف: " + itemName + " | الكمية: " + quantity + " | السعر: " + unitPrice + " | إجمالي القيمة: " + getTotalValue());
    }
}