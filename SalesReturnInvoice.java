import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * ============================================================================
 * نظام ERP المصنعي - كلاس فاتورة مردودات المبيعات (SalesReturnInvoice)
 * ============================================================================
 * 1. إثبات تخفيض إيراد المبيعات والضريبة وقيد الاستحقاق لحساب العميل.
 * 2. التحقق الجبري عبر حارس الحسابات المركزي (AccountValidator) لمنع القيد على الحسابات الرئيسية.
 * 3. التصدير النصي للسند والقيد المزدوج إلى ملف SalesReturnInvoiceLog.txt.
 * 4. الترحيل والحفظ المباشر في قاعدة البيانات (MySQL) عبر DatabaseManager.
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

        // 1. التحقق الجبري الأمني من الحسابات الفرعية عبر حارس الحسابات المركزي (مستوى 6)
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
    }

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
     * حفظ الفاتورة مباشرة في قاعدة بيانات MySQL عبر DatabaseManager
     */
    public void saveToDatabase() {
        try {
            DatabaseManager.insertSalesReturnNote(
                invoiceCode,
                customerAccount,
                salesReturnAccount,
                finishedGoodsAccount,
                cogsAccount,
                getTotalCustomerCredit()
            );
            System.out.println("Success: Sales Return Invoice saved to MySQL Database.");
        } catch (Exception e) {
            System.err.println("Error saving Sales Return Invoice to DB: " + e.getMessage());
        }
    }

    /**
     * الدالة الرئيسية (main) لتشغيل واختبار فاتورة مردودات المبيعات ذاتياً من الطرفية Terminal
     */
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("=== نظام ERP المتقدم - تجربة المردودات ===");
        System.out.println("==========================================");

        try {
            // إنشاء نموذج فاتورة مردودات تجريبية
            SalesReturnInvoice invoice = new SalesReturnInvoice(
                "SRI-1001",
                "INV_1001",
                java.time.LocalDate.now().toString(),
                "123020001",
                "410201",
                "220301",
                "1210301",
                "510101",
                250.00,
                180.00,
                true,
                0.15,
                "تلف أثناء النقل والتخزين",
                "BATCH-2026-08"
            );

            System.out.println("تم إنشاء الفاتورة: " + invoice.getInvoiceCode());
            System.out.println(" - قيمة المرتجع: " + invoice.getReturnAmount() + " YER");
            System.out.println(" - مبلغ الضريبة (15%): " + invoice.getTaxAmount() + " YER");
            System.out.println(" - إجمالي المستحق للعميل: " + invoice.getTotalCustomerCredit() + " YER");
            System.out.println(" - تكلفة المخزون المستردة: " + invoice.getInventoryCost() + " YER");

            // تصدير إلى الملف النصي
            invoice.exportToTextFile();

            System.out.println("\nتمت العملية بنجاح.");
        } catch (Exception ex) {
            System.err.println("خطأ أثناء التنفيذ: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}