/**
 * ============================================================================
 * نظام ERP المصنعي - كائن بطاقة الصنف الغذائي (ItemMaster)
 * ============================================================================
 * - الباركود هو نفسه كود الحساب الفرعي بالشجرة (مستوى 6).
 * - يتضمن محددات الصناعات الغذائية الضرورية (تاريخ الانتهاء ورقم التشغيلة).
 */
public class ItemMaster {

    private String barcode;           // الباركود وكود الحساب الفرعي بالشجرة (مثال: 1210301)
    private String itemName;          // اسم الصنف
    private String itemType;          // نوع المخزون (خام / تشغيل / تام / مناديب)
    private String uom;               // وحدة القياس (حبة / كرتون / باكت)
    private double conversionFactor;  // معامل التحويل
    private double minStockLevel;     // حد الأمان البسيط
    private String expiryDate;        // تاريخ الانتهاء (YYYY-MM-DD)
    private String batchNo;           // رقم التشغيلة / الدفعة
    private double unitCost;          // تكلفة الوحدة
    private double defaultUnitPrice;  // سعر البيع الافتراضي

    public ItemMaster(String barcode, String itemName, String itemType, String uom, 
                      double conversionFactor, double minStockLevel, String expiryDate, 
                      String batchNo, double unitCost, double defaultUnitPrice) {
        this.barcode = barcode;
        this.itemName = itemName;
        this.itemType = itemType;
        this.uom = uom;
        this.conversionFactor = conversionFactor;
        this.minStockLevel = minStockLevel;
        this.expiryDate = expiryDate;
        this.batchNo = batchNo;
        this.unitCost = unitCost;
        this.defaultUnitPrice = defaultUnitPrice;
    }

    public String getBarcode() { return barcode; }
    public String getItemName() { return itemName; }
    public String getItemType() { return itemType; }
    public String getUom() { return uom; }
    public double getConversionFactor() { return conversionFactor; }
    public double getMinStockLevel() { return minStockLevel; }
    public String getExpiryDate() { return expiryDate; }
    public String getBatchNo() { return batchNo; }
    public double getUnitCost() { return unitCost; }
    public double getDefaultUnitPrice() { return defaultUnitPrice; }

    public String toLogLine() {
        return barcode + " | " + itemName + " | " + itemType + " | " + uom + " | " + 
               conversionFactor + " | " + minStockLevel + " | " + expiryDate + " | " + 
               batchNo + " | " + unitCost + " | " + defaultUnitPrice;
    }
}