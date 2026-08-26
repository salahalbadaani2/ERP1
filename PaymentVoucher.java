import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PaymentVoucher {
    private String voucherCode;
    private String voucherType;        // نوع السند: "سداد مورد" أو "تحصيل عميل"
    private String debitAccount;       // حساب فرعي مدين (مستوى 6)
    private String creditAccount;      // حساب فرعي دائن (مستوى 6)
    private double foreignAmount;      // المبلغ بالعملة الأجنبية (USD / SAR)
    private String currency;           // رمز العملة (USD / SAR)
    private double exchangeRate;       // سعر الصرف مقابل الريال اليمني (YER)
    private String description;        // البيان / الشرح

    public PaymentVoucher(String voucherCode, String voucherType, String debitAccount, 
                          String creditAccount, double foreignAmount, String currency, 
                          double exchangeRate, String description) {

        // فحص الحسابات عبر حارس التحقق لمنع القيد على الحسابات الرئيسية
        if (!AccountValidator.isValidSubAccount(debitAccount) || 
            !AccountValidator.isValidSubAccount(creditAccount)) {
            System.out.println("خطأ أمني ومحاسبي: يمنع القيد على حساب رئيسي. يجب أن تكون كافة الحسابات فرعية من المستوى 6.");
            return;
        }

        this.voucherCode = voucherCode;
        this.voucherType = voucherType;
        this.debitAccount = debitAccount;
        this.creditAccount = creditAccount;
        this.foreignAmount = foreignAmount;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.description = description;
    }

    // حساب المبلغ المعادل بالعملة الأساسية (الريال اليمني YER)
    public double getLocalAmount() {
        return foreignAmount * exchangeRate;
    }

    public void exportToTextFile() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = now.format(formatter);

        try (PrintWriter writer = new PrintWriter(new FileWriter("TreasuryLog.txt", true))) {
            writer.println("[" + timestamp + "] === سند مالية ومعاملة نقدية/بنكية (Treasury Voucher) ===");
            writer.println("رقم السند: " + voucherCode + " | نوع المعاملة: " + voucherType);
            writer.println("البيان: " + description);
            writer.println("المبلغ بالعملة الأجنبية: " + foreignAmount + " " + currency);
            writer.println("سعر الصرف المعتمد: " + exchangeRate + " YER / " + currency);
            writer.println("المبلغ المعادل بالعملة المحلية: " + getLocalAmount() + " YER");
            writer.println("--------------------------------------------------");
            writer.println("القيد المحاسبي الآلي (القيمة المعادل بالريال اليمني):");
            writer.println("   من حـ/ " + debitAccount + ": " + getLocalAmount() + " YER [مدين]");
            writer.println("   إلى حـ/ " + creditAccount + ": " + getLocalAmount() + " YER [دائن]");
            writer.println("==================================================\n");

            System.out.println("Success: Treasury Voucher Exported to TreasuryLog.txt");
        } catch (IOException e) {
            System.out.println("Error: Cannot write to TreasuryLog.txt");
        }
    }
}