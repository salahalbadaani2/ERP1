import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CurrencyManager {

    // عملة التقرير الأساسية الثابتة للنظام
    public static final String BASE_CURRENCY = "YER";

    private String currencyCode; // رمز العملة (USD, SAR, YER)
    private double foreignAmount; // المبلغ بالعملة الأجنبية
    private double exchangeRate;  // سعر الصرف مقابل الريال اليمني

    public CurrencyManager() {}

    public CurrencyManager(String currencyCode, double foreignAmount, double exchangeRate) {
        this.currencyCode = currencyCode;
        this.foreignAmount = foreignAmount;
        // إذا كانت العملة هي YER يكون سعر الصرف تلقائياً 1.0
        this.exchangeRate = currencyCode.equalsIgnoreCase(BASE_CURRENCY) ? 1.0 : exchangeRate;
    }

    /**
     * احتساب القيمة المعادلة بالريال اليمني آلياً
     */
    public double getAmountInYER() {
        return this.foreignAmount * this.exchangeRate;
    }

    /**
     * دالة ثابتة للتحويل المباشر
     */
    public static double convertToYER(double amount, double rate) {
        return amount * rate;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public double getForeignAmount() {
        return foreignAmount;
    }

    public double getExchangeRate() {
        return exchangeRate;
    }

    /**
     * تصدير سجل عمليات الصرف والتحويل المالي للعملات إلى ملف نصي
     */
    public void exportToTextFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("CurrencyLog.txt", true))) {
            writer.println("=== معاملة متعددة العملات ===");
            writer.println("العملة الأصلية: " + currencyCode);
            writer.println("المبلغ بالعملة الأصلية: " + foreignAmount);
            writer.println("سعر الصرف: " + exchangeRate + " YER");
            writer.println("المعادل بالريال اليمني (YER): " + getAmountInYER());
            writer.println("===============================");
            System.out.println("Success: Currency Transaction Exported to CurrencyLog.txt");
        } catch (IOException e) {
            System.err.println("Error exporting Currency Log: " + e.getMessage());
        }
    }
}