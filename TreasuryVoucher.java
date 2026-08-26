import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class TreasuryVoucher {
    public static final String TREASURY_ACCOUNT_SANAA = "1110101 - خزينة رئيسي صنعاء";
    private String voucherCode;
    private String voucherDate;
    private String voucherType;        // سند قبض / سند صرف
    private String paymentMethod;      // نقداً، حوالة بنكية/صراف، شيك بنكي
    private String beneficiary;        // اسم المستفيد / الجهة
    private String accountCode;        // الحساب الأساسي للسند (مستوى 6)
    private String counterpartAccount; // الطرف المقابل من شجرة الحسابات (عند الحوالة/الشيك)
    private double foreignAmount;
    private String currencyCode;
    private double exchangeRate;
    private double amountInYER;
    private String description;

    public TreasuryVoucher() {}

    // المشيّد الرئيسي الشامل (11 مدخلاً) مع حارس الحسابات الفرعية (مستوى 6)
    public TreasuryVoucher(String voucherCode, String voucherDate, String voucherType, String paymentMethod, 
                           String beneficiary, String accountCode, String counterpartAccount, 
                           double foreignAmount, String currencyCode, double exchangeRate, String description) {
        
        // التحقق أمنياً من الحساب الأساسي (مستوى 6)
        if (accountCode == null || !AccountValidator.isSubAccount(accountCode)) {
            throw new IllegalArgumentException("خطأ محاسبي: يمنع القيد على الحسابات الرئيسية! يجب اختيار حساب فرعي (مستوى 6) للحساب الأساسي.");
        }

        // التحقق من الطرف المقابل للحوالات والشيكات (مستوى 6)
        if ((paymentMethod.contains("حوالة") || paymentMethod.contains("شيك")) && 
            (counterpartAccount == null || counterpartAccount.isEmpty() || !AccountValidator.isSubAccount(counterpartAccount))) {
            throw new IllegalArgumentException("خطأ محاسبي: في حالة الحوالة أو الشيك، يجب اختيار حساب فرعي صحيح للطرف المقابل من شجرة الحسابات.");
        }

        this.voucherCode = voucherCode;
        this.voucherDate = voucherDate;
        this.voucherType = voucherType;
        this.paymentMethod = paymentMethod;
        this.beneficiary = beneficiary;
        this.accountCode = accountCode;
        this.counterpartAccount = counterpartAccount;
        this.foreignAmount = foreignAmount;
        this.currencyCode = (currencyCode != null && !currencyCode.isEmpty()) ? currencyCode : "YER";
        this.exchangeRate = (exchangeRate > 0) ? exchangeRate : 1.0;
        this.amountInYER = CurrencyManager.convertToYER(foreignAmount, this.exchangeRate);
        this.description = description;
    }

    // مشيّدات التوافق مع الاستدعاءات القديمة
    public TreasuryVoucher(String voucherCode, String accountCode, double amount, String type, String paymentMethod, String description, String currencyCode, double exchangeRate) {
        this(voucherCode, "2026-08-16", type, paymentMethod, "عام", accountCode, "", amount, currencyCode, exchangeRate, description);
    }

    public TreasuryVoucher(String voucherCode, String accountCode, double amount, String type, String description, String currencyCode, double exchangeRate) {
        this(voucherCode, "2026-08-16", type, "نقداً (خزينة)", "عام", accountCode, "", amount, currencyCode, exchangeRate, description);
    }

    public TreasuryVoucher(String voucherCode, String accountCode, double amount, String type, String description) {
        this(voucherCode, "2026-08-16", type, "نقداً (خزينة)", "عام", accountCode, "", amount, "YER", 1.0, description);
    }

    // دوال الاسترجاع (Getters)
    public String getVoucherCode() { return voucherCode; }
    public String getVoucherDate() { return voucherDate; }
    public String getVoucherType() { return voucherType; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getBeneficiary() { return beneficiary; }
    public String getAccountCode() { return accountCode; }
    public String getCounterpartAccount() { return counterpartAccount; }
    public double getForeignAmount() { return foreignAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public double getExchangeRate() { return exchangeRate; }
    public double getAmountInYER() { return amountInYER; }
    public String getDescription() { return description; }

    // دالة التصدير النصي
    public void exportToTextFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("TreasuryLog.txt", true))) {
            writer.println("=== سند خزينة / بنك متكامل ===");
            writer.println("رقم السند: " + voucherCode);
            writer.println("التاريخ: " + voucherDate);
            writer.println("نوع السند: " + voucherType);
            writer.println("طريقة السداد: " + paymentMethod);
            writer.println("اسم المستفيد: " + beneficiary);
            writer.println("الحساب المالي الأساسي: " + accountCode);
            writer.println("الطرف المقابل (الحوالة/الشيك): " + (counterpartAccount != null && !counterpartAccount.isEmpty() ? counterpartAccount : "لا يوجد"));
            writer.println("المبلغ: " + foreignAmount + " " + currencyCode);
            writer.println("سعر الصرف: " + exchangeRate + " YER");
            writer.println("المعادل بالريال اليمني: " + amountInYER + " YER");
            writer.println("البيان: " + description);
            writer.println("===============================");
            System.out.println("Success: Treasury Voucher Exported to TreasuryLog.txt");
        } catch (IOException e) {
            System.err.println("Error exporting Treasury Voucher: " + e.getMessage());
        }
    }
}