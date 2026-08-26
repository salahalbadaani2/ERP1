import java.io.File;
import java.io.PrintWriter;

public class PurchaseInvoice {
    private String invoiceCode;        // رقم فاتورة المشتريات (PINV-1001)
    private String grirAccount;        // حساب وسيط استلام البضائع GR/IR الفرعي (220101)
    private String vendorAccount;      // حساب المورد الفعلي الفرعي (220102)
    private String inputTaxAccount;    // حساب ضريبة المدخلات الفرعي (220301) - اختياري
    private double amount;             // مبلغ المشتريات قبل الضريبة
    private boolean isTaxApplied;      // خيار تفعيل/إلغاء الضريبة
    private double taxRate;            // نسبة الضريبة (0.0 إذا كانت غير مفعلة)
    private String itemCode;
    private double quantity;
    private double unitCost;

    public PurchaseInvoice(String invoiceCode, String grirAccount, String vendorAccount, 
                           String inputTaxAccount, double amount, boolean isTaxApplied, double taxRate) {
        this(invoiceCode, grirAccount, vendorAccount, inputTaxAccount, amount, isTaxApplied, taxRate, null, 0, 0);
    }

    public PurchaseInvoice(String invoiceCode, String grirAccount, String vendorAccount,
                           String inputTaxAccount, double amount, boolean isTaxApplied, double taxRate,
                           String itemCode, double quantity, double unitCost) {
        if (invoiceCode == null || invoiceCode.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ أمني: رقم فاتورة المشتريات مطلوب!");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("خطأ محاسبي: مبلغ الفاتورة يجب أن يكون أكبر من الصفر!");
        }

        // فحص الحسابات الفرعية بواسطة حارس الحسابات المركزي
        AccountValidator.validateSubAccount(grirAccount, "حساب وسيط استلام البضائع GR/IR");
        AccountValidator.validateSubAccount(vendorAccount, "حساب المورد الفعلي");
        
        if (isTaxApplied) {
            AccountValidator.validateSubAccount(inputTaxAccount, "حساب ضريبة المشتريات");
        }

        this.invoiceCode = invoiceCode;
        this.grirAccount = grirAccount;
        this.vendorAccount = vendorAccount;
        this.inputTaxAccount = inputTaxAccount;
        this.amount = amount;
        this.isTaxApplied = isTaxApplied;
        this.taxRate = isTaxApplied ? taxRate : 0.0;
        this.itemCode = itemCode;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public double getTaxAmount() {
        return isTaxApplied ? (amount * taxRate) : 0.0;
    }

    public double getTotalVendorCredit() {
        return amount + getTaxAmount();
    }

    public String getInvoiceCode() { return invoiceCode; }
    public String getGrirAccount() { return grirAccount; }
    public String getVendorAccount() { return vendorAccount; }
    public String getInputTaxAccount() { return inputTaxAccount; }
    public double getAmount() { return amount; }
    public boolean isTaxApplied() { return isTaxApplied; }
    public double getTaxRate() { return taxRate; }
    public String getItemCode() { return itemCode; }
    public double getQuantity() { return quantity; }
    public double getUnitCost() { return unitCost; }

    public boolean postToAccounting() {
        return PurchasingPostingService.postPurchase(this);
    }

    // تصدير الفاتورة وقيدها المالي إلى ملف نصي
    public void exportToTextFile() {
        try (PrintWriter writer = new PrintWriter(new File("PurchaseInvoiceLog.txt"), "UTF-8")) {
            writer.println("========================================");
            writer.println("        فاتورة مشتريات (Purchase Invoice) ");
            writer.println("رقم الفاتورة: " + invoiceCode);
            writer.println("حساب وسيط استلام البضائع GR/IR: " + grirAccount);
            writer.println("حساب المورد الفعلي: " + vendorAccount);
            writer.println("مبلغ المشتريات: " + amount);
            writer.println("وضع الضريبة: " + (isTaxApplied ? "مفعلة بنسبة " + (taxRate * 100) + "%" : "غير مفعلة (اختياري)"));
            writer.println("مبلغ الضريبة: " + getTaxAmount());
            writer.println("إجمالي المستحق للمورد: " + getTotalVendorCredit());
            writer.println("========================================");
            writer.println("   القيد المحاسبي المالي الصادر:        ");
            
            if (isTaxApplied && getTaxAmount() > 0) {
                writer.println("من مذكورين:");
                writer.println("  - حـ/ وسيط استلام البضائع GR/IR (" + grirAccount + "): " + amount);
                writer.println("  - حـ/ ضريبة المشتريات (" + inputTaxAccount + "): " + getTaxAmount());
            } else {
                writer.println("من حـ/ وسيط استلام البضائع GR/IR (" + grirAccount + "): " + amount);
            }
            
            writer.println("إلى حـ/ المورد الفعلي (" + vendorAccount + "): " + getTotalVendorCredit());
            writer.println("----------------------------------------");
            
            System.out.println("Success: Purchase Invoice Exported to PurchaseInvoiceLog.txt");
        } catch (Exception e) {
            System.out.println("Error: Cannot create log file.");
        }
    }
}