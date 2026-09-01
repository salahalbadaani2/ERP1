import java.io.PrintWriter;
import java.time.LocalDate;

/**
 * ============================================================================
 * نظام ERP المصنعي - كلاس فاتورة مردودات المبيعات (SalesReturnInvoice)
 * ============================================================================
 * 1. إثبات تخفيض إيراد المبيعات والضريبة وقيد الاستحقاق لحساب العميل.
 * 2. التحقق الجبري عبر حارس الحسابات المركزي (AccountValidator) لمنع القيد على الحسابات الرئيسية.
 * 3. التصدير النصي للسند والقيد المزدوج إلى ملف SalesReturnInvoiceLog.txt.
 * 4. الترحيل عبر SalesPostingService حصراً (Single Transaction).
 */
public class SalesReturnInvoice {

    private String invoiceCode;           // رقم فاتورة المرتجع (مثال: SRI-1001)
    private String originalInvoiceCode;   // رقم فاتورة المبيعات الأصلية (مثال: INV-1001)
    private String returnDate;            // تاريخ المرتجع (YYYY-MM-DD)
    private String customerAccount;       // حساب العميل الفرعي (مثال: 123020001)
    private String salesReturnAccount;    // حساب مردودات المبيعات الفرعي (مثال: 410201)
    private String taxAccount;            // حساب أمانات ضريبة المبيعات الفرعي (مثال: 220301)
    private String finishedGoodsAccount;  // حساب مخزن المنتجات التامة الفرعي (مثال: 1210301)
    private String cogsAccount;           // حساب تكلفة البضاعة المباعة COGS (مثال: 510101)
    
    private double returnAmount;          // قيمة المرتجع الأساسية (الكمية × سعر البيع)
    private double inventoryCost;         // التكلفة المخزنية للبضاعة المستردة (الكمية × تكلفة الوحدة)
    private boolean isTaxApplied;         // خيار تفعيل/إلغاء الضريبة
    private double taxRate;               // نسبة الضريبة (0.15 أو 0.05)
    private String returnReason;          // سبب الإرجاع (تالف، منتهي، غير مطابق)
    private String batchNo;               // رقم التشغيلة/الدفعة
    private java.util.List<ReturnLine> lines = new java.util.ArrayList<>();

    public static class ReturnLine {
        private final String itemCode;
        private final double quantity;
        private final double unitPrice;
        private final double unitCost;
        public ReturnLine(String itemCode, double quantity, double unitPrice, double unitCost) {
            if (itemCode == null || itemCode.trim().isEmpty()) {
                throw new IllegalArgumentException("خطأ جبري: رقم الصنف مطلوب في سطر المرتجع!");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("خطأ جبري: كمية الصنف [" + itemCode + "] يجب أن تكون موجبة!");
            }
            if (unitPrice < 0 || unitCost < 0) {
                throw new IllegalArgumentException("خطأ جبري: سعر/تكلفة الصنف [" + itemCode + "] لا يمكن أن تكون سالبة!");
            }
            this.itemCode = itemCode.trim();
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.unitCost = unitCost;
        }
        public String getItemCode() { return itemCode; }
        public double getQuantity() { return quantity; }
        public double getUnitPrice() { return unitPrice; }
        public double getUnitCost() { return unitCost; }
        public double getLineAmount() { return quantity * unitPrice; }
        public double getLineCost() { return quantity * unitCost; }
    }

    public SalesReturnInvoice(String invoiceCode, String originalInvoiceCode, String returnDate,
                              String customerAccount, String salesReturnAccount, String taxAccount,
                              String finishedGoodsAccount, String cogsAccount,
                              double returnAmount, double inventoryCost,
                              boolean isTaxApplied, double taxRate,
                              String returnReason, String batchNo) {

        if (invoiceCode == null || invoiceCode.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ أمني: رقم فاتورة مردودات المبيعات مطلوب!");
        }
        if (returnAmount <= 0) {
            throw new IllegalArgumentException("خطأ محاسبي: قيمة المرتجع يجب أن تكون أكبر من الصفر!");
        }

        AccountValidator.validateSubAccount(customerAccount, "حساب العميل");
        AccountValidator.validateSubAccount(salesReturnAccount, "حساب مردودات ومسموحات المبيعات");
        AccountValidator.validateSubAccount(finishedGoodsAccount, "حساب مخزن المنتجات التامة");
        AccountValidator.validateSubAccount(cogsAccount, "حساب تكلفة البضاعة المباعة (COGS)");

        if (isTaxApplied) {
            AccountValidator.validateSubAccount(taxAccount, "حساب ضريبة المبيعات والقيمة المضافة");
        }

        this.invoiceCode = invoiceCode;
        this.originalInvoiceCode = (originalInvoiceCode != null && !originalInvoiceCode.trim().isEmpty()) ? originalInvoiceCode : "مباشر بدون فاتورة";
        this.returnDate = (returnDate != null && !returnDate.trim().isEmpty()) ? returnDate : LocalDate.now().toString();
        this.customerAccount = customerAccount;
        this.salesReturnAccount = salesReturnAccount;
        this.taxAccount = taxAccount;
        this.finishedGoodsAccount = finishedGoodsAccount;
        this.cogsAccount = cogsAccount;
        this.returnAmount = returnAmount;
        this.inventoryCost = inventoryCost;
        this.isTaxApplied = isTaxApplied;
        this.taxRate = isTaxApplied ? taxRate : 0.0;
        this.returnReason = (returnReason != null) ? returnReason : "غير محدد";
        this.batchNo = (batchNo != null) ? batchNo : "---";
        // ضمان عدم ترك القائمة فارغة للتوافق مع النماذج الأخرى
        this.lines.add(new ReturnLine("ITEM-DEFAULT", 1, returnAmount, inventoryCost));
    }

    public SalesReturnInvoice(String invoiceCode, String originalInvoiceCode, String returnDate,
                              String customerAccount, String salesReturnAccount, String taxAccount,
                              String finishedGoodsAccount, String cogsAccount,
                              boolean isTaxApplied, double taxRate,
                              String returnReason, String batchNo,
                              java.util.List<ReturnLine> lines) {
        if (invoiceCode == null || invoiceCode.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ أمني: رقم فاتورة مردودات المبيعات مطلوب!");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("خطأ محاسبي: يجب إضافة صنف واحد على الأقل!");
        }
        double total = 0;
        double totalCost = 0;
        for (ReturnLine l : lines) total += l.getLineAmount();
        for (ReturnLine l : lines) totalCost += l.getLineCost();
        if (total <= 0) throw new IllegalArgumentException("خطأ محاسبي: قيمة المرتجع يجب أن تكون أكبر من الصفر!");
        AccountValidator.validateSubAccount(customerAccount, "حساب العميل");
        AccountValidator.validateSubAccount(salesReturnAccount, "حساب مردودات ومسموحات المبيعات");
        AccountValidator.validateSubAccount(finishedGoodsAccount, "حساب مخزن المنتجات التامة");
        AccountValidator.validateSubAccount(cogsAccount, "حساب تكلفة البضاعة المباعة (COGS)");
        if (isTaxApplied) {
            AccountValidator.validateSubAccount(taxAccount, "حساب ضريبة المبيعات والقيمة المضافة");
        }
        this.invoiceCode = invoiceCode;
        this.originalInvoiceCode = (originalInvoiceCode != null && !originalInvoiceCode.trim().isEmpty()) ? originalInvoiceCode : "مباشر بدون فاتورة";
        this.returnDate = (returnDate != null && !returnDate.trim().isEmpty()) ? returnDate : LocalDate.now().toString();
        this.customerAccount = customerAccount;
        this.salesReturnAccount = salesReturnAccount;
        this.taxAccount = taxAccount;
        this.finishedGoodsAccount = finishedGoodsAccount;
        this.cogsAccount = cogsAccount;
        this.returnAmount = total;
        this.inventoryCost = totalCost;
        this.isTaxApplied = isTaxApplied;
        this.taxRate = isTaxApplied ? taxRate : 0.0;
        this.returnReason = (returnReason != null) ? returnReason : "غير محدد";
        this.batchNo = (batchNo != null) ? batchNo : "---";
        this.lines = new java.util.ArrayList<>(lines);
    }

    public java.util.List<ReturnLine> getLines() { return java.util.Collections.unmodifiableList(lines); }

    public double getTaxAmount() {
        return isTaxApplied ? (returnAmount * taxRate) : 0.0;
    }

    public double getTotalCustomerCredit() {
        return returnAmount + getTaxAmount();
    }

    // دوال القراءة (Getters)
    public String getInvoiceCode() { return invoiceCode; }
    public String getOriginalInvoiceCode() { return originalInvoiceCode; }
    public String getReturnDate() { return returnDate; }
    public String getCustomerAccount() { return customerAccount; }
    public String getSalesReturnAccount() { return salesReturnAccount; }
    public String getTaxAccount() { return taxAccount; }
    public String getFinishedGoodsAccount() { return finishedGoodsAccount; }
    public String getCogsAccount() { return cogsAccount; }
    public double getReturnAmount() { return returnAmount; }
    public double getInventoryCost() { return inventoryCost; }
    public boolean isTaxApplied() { return isTaxApplied; }
    public double getTaxRate() { return taxRate; }
    public String getReturnReason() { return returnReason; }
    public String getBatchNo() { return batchNo; }

    public boolean postToAccounting() {
        return SalesPostingService.postSalesReturn(this);
    }

    /**
     * تصدير فاتورة المردودات والقيدين المحاسبيين المزدوجين إلى الملف النصي SalesReturnInvoiceLog.txt
     */
    public void exportToTextFile() {
        try (PrintWriter writer = new PrintWriter(new java.io.FileWriter("SalesReturnInvoiceLog.txt", true))) {
            writer.println("========================================");
            writer.println("   فاتورة مردودات مبيعات (Sales Return Invoice) ");
            writer.println("رقم الفاتورة: " + invoiceCode + " | الفاتورة الأصلية: " + originalInvoiceCode);
            writer.println("التاريخ: " + returnDate);
            writer.println("حساب العميل [دائن]: " + customerAccount);
            writer.println("حساب مردودات المبيعات [مدين]: " + salesReturnAccount);
            writer.println("حساب مخزن المنتجات التامة [مدين]: " + finishedGoodsAccount);
            writer.println("حساب تكلفة المبيعات COGS [دائن]: " + cogsAccount);
            writer.println("قيمة المرتجع الأساسية: " + returnAmount + " YER");
            writer.println("وضع الضريبة: " + (isTaxApplied ? "مفعلة بنسبة " + (taxRate * 100) + "%" : "غير مفعلة"));
            writer.println("مبلغ الضريبة: " + getTaxAmount() + " YER");
            writer.println("إجمالي المستحق لحساب العميل: " + getTotalCustomerCredit() + " YER");
            writer.println("التكلفة المخزنية المستردة (COGS Reversal): " + inventoryCost + " YER");
            writer.println("سبب المرتجع: " + returnReason + " | رقم التشغيلة: " + batchNo);
            writer.println("--- تفاصيل الأصناف المرتجعة ---");
            for (ReturnLine l : lines) {
                writer.println("  * رمز الصنف: " + l.getItemCode() + " | الكمية: " + l.getQuantity() + " | سعر الوحدة: " + l.getUnitPrice() + " | التكلفة: " + l.getUnitCost() + " | إجمالي السطر: " + l.getLineAmount() + " | تكلفة السطر: " + l.getLineCost());
            }
            writer.println("----------------------------------------");
            writer.println("   1. القيد المالي لتخفيض الإيراد والذمم: ");
            if (isTaxApplied && getTaxAmount() > 0) {
                writer.println("من مذكورين:");
                writer.println("  - حـ/ مردودات المبيعات (" + salesReturnAccount + "): " + returnAmount + " [مدين]");
                writer.println("  - حـ/ ضريبة المبيعات (" + taxAccount + "): " + getTaxAmount() + " [مدين]");
            } else {
                writer.println("من حـ/ مردودات المبيعات (" + salesReturnAccount + "): " + returnAmount + " [مدين]");
            }
            writer.println("إلى حـ/ العميل (" + customerAccount + "): " + getTotalCustomerCredit() + " [دائن]");
            
            writer.println("----------------------------------------");
            writer.println("   2. القيد المخزني لاسترداد المنتجات التامة وتخفيض COGS: ");
            writer.println("من حـ/ مخزن المنتجات التامة (" + finishedGoodsAccount + "): " + inventoryCost + " [مدين]");
            writer.println("إلى حـ/ تكلفة البضاعة المباعة COGS (" + cogsAccount + "): " + inventoryCost + " [دائن]");
            writer.println("========================================\n");

            System.out.println("Success: Sales Return Invoice Exported to SalesReturnInvoiceLog.txt");
        } catch (Exception e) {
            System.err.println("Error: Cannot create SalesReturnInvoice log file: " + e.getMessage());
        }
    }

    /**
     * الدالة الرئيسية (main) لتشغيل واختبار فاتورة مردودات المبيعات ذاتياً من الطرفية Terminal
     */
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("=== نظام ERP المتقدم - تجربة المردودات متعددة الأصناف ===");
        System.out.println("==========================================");

        try {
            java.util.List<ReturnLine> lines = new java.util.ArrayList<>();
            lines.add(new ReturnLine("ITEM-101", 2, 250.00, 180.00));
            lines.add(new ReturnLine("ITEM-102", 3, 180.00, 120.00));
            lines.add(new ReturnLine("ITEM-103", 5, 80.00, 50.00));

            SalesReturnInvoice invoice = new SalesReturnInvoice(
                "SRI-1001",
                "INV_1001",
                java.time.LocalDate.now().toString(),
                "123020001",
                "410201",
                "220301",
                "1210301",
                "510101",
                true,
                0.15,
                "تلف أثناء النقل والتخزين",
                "BATCH-2026-08",
                lines
            );

            System.out.println("تم إنشاء الفاتورة: " + invoice.getInvoiceCode());
            System.out.println(" - عدد الأسطر: " + invoice.getLines().size());
            for (ReturnLine l : invoice.getLines()) {
                System.out.println("   * " + l.getItemCode() + " | qty=" + l.getQuantity() + " | price=" + l.getUnitPrice() + " | cost=" + l.getUnitCost() + " | amount=" + l.getLineAmount());
            }
            System.out.println(" - قيمة المرتجع: " + invoice.getReturnAmount() + " YER");
            System.out.println(" - مبلغ الضريبة (15%): " + invoice.getTaxAmount() + " YER");
            System.out.println(" - إجمالي المستحق للعميل: " + invoice.getTotalCustomerCredit() + " YER");
            System.out.println(" - تكلفة المخزون المستردة: " + invoice.getInventoryCost() + " YER");

            invoice.exportToTextFile();
            System.out.println("\nتمت العملية بنجاح.");
        } catch (Exception ex) {
            System.err.println("خطأ أثناء التنفيذ: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
