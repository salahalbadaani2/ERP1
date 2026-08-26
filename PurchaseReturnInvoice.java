import java.io.File;
import java.io.PrintWriter;

public class PurchaseReturnInvoice {
    private String invoiceCode;           // رقم فاتورة مرتجع المشتريات (PRINV-1001)
    private String vendorAccount;        // حساب المورد الفعلي الفرعي (220102)
    private String grirAccount;          // حساب وسيط استلام البضائع GR/IR الفرعي (220101)
    private String inputTaxAccount;      // حساب ضريبة المشتريات الفرعي (220301) - اختياري
    private double returnAmount;         // قيمة المرتجع الأساسية
    private boolean isTaxApplied;        // خيار تفعيل/إلغاء الضريبة
    private double taxRate;              // نسبة الضريبة (0.0 إذا كانت غير مفعلة)
    private String itemCode;
    private double quantity;
    private double unitCost;

    public PurchaseReturnInvoice(String invoiceCode, String vendorAccount, String grirAccount, 
                                 String inputTaxAccount, double returnAmount, boolean isTaxApplied, double taxRate) {
        this(invoiceCode, vendorAccount, grirAccount, inputTaxAccount, returnAmount, isTaxApplied, taxRate, null, 0, 0);
    }

    public PurchaseReturnInvoice(String invoiceCode, String vendorAccount, String grirAccount,
                                 String inputTaxAccount, double returnAmount, boolean isTaxApplied, double taxRate,
                                 String itemCode, double quantity, double unitCost) {
        if (invoiceCode == null || invoiceCode.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ أمني: رقم فاتورة مرتجع المشتريات مطلوب!");
        }
        if (returnAmount <= 0) {
            throw new IllegalArgumentException("خطأ محاسبي: قيمة المرتجع يجب أن تكون أكبر من الصفر!");
        }

        // فحص الحسابات الفرعية عبر حارس الحسابات المركزي
        AccountValidator.validateSubAccount(vendorAccount, "حساب المورد الفعلي");
        AccountValidator.validateSubAccount(grirAccount, "حساب وسيط استلام البضائع GR/IR");
        
        if (isTaxApplied) {
            AccountValidator.validateSubAccount(inputTaxAccount, "حساب ضريبة المشتريات");
        }

        this.invoiceCode = invoiceCode;
        this.vendorAccount = vendorAccount;
        this.grirAccount = grirAccount;
        this.inputTaxAccount = inputTaxAccount;
        this.returnAmount = returnAmount;
        this.isTaxApplied = isTaxApplied;
        this.taxRate = isTaxApplied ? taxRate : 0.0;
        this.itemCode = itemCode;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public double getTaxAmount() {
        return isTaxApplied ? (returnAmount * taxRate) : 0.0;
    }

    public double getTotalVendorDebit() {
        return returnAmount + getTaxAmount();
    }

    public String getInvoiceCode() { return invoiceCode; }
    public String getVendorAccount() { return vendorAccount; }
    public String getGrirAccount() { return grirAccount; }
    public String getInputTaxAccount() { return inputTaxAccount; }
    public double getReturnAmount() { return returnAmount; }
    public boolean isTaxApplied() { return isTaxApplied; }
    public double getTaxRate() { return taxRate; }
    public String getItemCode() { return itemCode; }
    public double getQuantity() { return quantity; }
    public double getUnitCost() { return unitCost; }

    public boolean postToAccounting() {
        return PurchasingPostingService.postPurchaseReturn(this);
    }

    // تصدير الفاتورة وقيدها المالي إلى ملف نصي
    public void exportToTextFile() {
        try (PrintWriter writer = new PrintWriter(new File("PurchaseReturnInvoiceLog.txt"), "UTF-8")) {
            writer.println("========================================");
            writer.println("   فاتورة مردودات مشتريات (Purchase Return Invoice) ");
            writer.println("رقم الفاتورة: " + invoiceCode);
            writer.println("حساب المورد الفعلي: " + vendorAccount);
            writer.println("حساب وسيط استلام البضائع GR/IR: " + grirAccount);
            writer.println("قيمة المرتجع: " + returnAmount);
            writer.println("وضع الضريبة: " + (isTaxApplied ? "مفعلة بنسبة " + (taxRate * 100) + "%" : "غير مفعلة (اختياري)"));
            writer.println("مبلغ الضريبة: " + getTaxAmount());
            writer.println("إجمالي المخصوم من حساب المورد: " + getTotalVendorDebit());
            writer.println("========================================");
            writer.println("   القيد المحاسبي المالي الصادر:        ");
            writer.println("من حـ/ المورد الفعلي (" + vendorAccount + "): " + getTotalVendorDebit());
            
            if (isTaxApplied && getTaxAmount() > 0) {
                writer.println("إلى مذكورين:");
                writer.println("  - حـ/ وسيط استلام البضائع GR/IR (" + grirAccount + "): " + returnAmount);
                writer.println("  - حـ/ ضريبة المشتريات (" + inputTaxAccount + "): " + getTaxAmount());
            } else {
                writer.println("إلى حـ/ وسيط استلام البضائع GR/IR (" + grirAccount + "): " + returnAmount);
            }
            writer.println("----------------------------------------");
            
            System.out.println("Success: Purchase Return Invoice Exported to PurchaseReturnInvoiceLog.txt");
        } catch (Exception e) {
            System.out.println("Error: Cannot create log file.");
        }
    }
}