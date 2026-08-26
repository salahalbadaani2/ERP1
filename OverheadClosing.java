import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OverheadClosing {
    private String closingCode;
    private String actualAccount;   // 520101 - المصاريف الفعلية (فرعي مستوى 6)
    private String appliedAccount;  // 520201 - المصاريف المحملة (فرعي مستوى 6)
    private String cogsAccount;     // 510101 - تكلفة البضاعة المباعة (فرعي مستوى 6)
    private double actualAmount;    // إجمالي المصاريف الفعلية (فواتير الكهرباء والصيانة)
    private double appliedAmount;   // إجمالي المصاريف المحملة تقديرياً خلال الشهر
    private String monthPeriod;     // فترة الإقفال (مثال: "2026-08")

    public OverheadClosing(String closingCode, String actualAccount, String appliedAccount, 
                           String cogsAccount, double actualAmount, double appliedAmount, String monthPeriod) {
        
        // تطبيق حارس التحقق الأمني والمحاسبي لمنع القيد على الحسابات الرئيسية
        if (!AccountValidator.isValidSubAccount(actualAccount) ||
            !AccountValidator.isValidSubAccount(appliedAccount) ||
            !AccountValidator.isValidSubAccount(cogsAccount)) {
            throw new IllegalArgumentException("خطأ أمني ومحاسبي: يجب أن تكون كافة حسابات الإقفال حسابات فرعية.");
        }
        if (closingCode == null || closingCode.trim().isEmpty() || monthPeriod == null || monthPeriod.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ محاسبي: رمز الإقفال والفترة المالية مطلوبان.");
        }
        if (actualAmount < 0 || appliedAmount < 0) {
            throw new IllegalArgumentException("خطأ محاسبي: قيم التكاليف لا يمكن أن تكون سالبة.");
        }

        this.closingCode = closingCode;
        this.actualAccount = actualAccount;
        this.appliedAccount = appliedAccount;
        this.cogsAccount = cogsAccount;
        this.actualAmount = actualAmount;
        this.appliedAmount = appliedAmount;
        this.monthPeriod = monthPeriod;
    }

    // احتساب صافي الانحراف
    public double getVarianceAmount() {
        return actualAmount - appliedAmount;
    }

    public String getClosingCode() { return closingCode; }
    public String getActualAccount() { return actualAccount; }
    public String getAppliedAccount() { return appliedAccount; }
    public String getCogsAccount() { return cogsAccount; }
    public double getActualAmount() { return actualAmount; }
    public double getAppliedAmount() { return appliedAmount; }
    public String getMonthPeriod() { return monthPeriod; }

    public boolean postToAccounting() {
        return ManufacturingPostingService.closeOverheadVariance(this);
    }

    public void exportToTextFile() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = now.format(formatter);
        double variance = getVarianceAmount();

        try (PrintWriter writer = new PrintWriter(new FileWriter("OverheadVarianceLog.txt", true))) {
            writer.println("[" + timestamp + "] === إقفال الشهر وتسوية انحرافات المصاريف الصناعية (" + monthPeriod + ") ===");
            writer.println("رمز عملية الإقفال: " + closingCode);
            writer.println("إجمالي المصاريف الصناعية الفعلية: " + actualAmount);
            writer.println("إجمالي المصاريف الصناعية المحملة (التقديرية): " + appliedAmount);
            writer.println("قيمة صافي الانحراف: " + variance + (variance > 0 ? " (انحراف غير ملائم - زيادة فعلية)" : " (انحراف ملائم)"));
            writer.println("--------------------------------------------------");
            writer.println("القيود المحاسبية الآلية للإقفال والتسوية:");
            writer.println("1. إقفال الحساب المحمل:");
            writer.println("   من حـ/ المصاريف المحملة (" + appliedAccount + "): " + appliedAmount + " [مدين]");
            writer.println("   إلى حـ/ المصاريف الفعلية (" + actualAccount + "): " + appliedAmount + " [دائن]");
            if (variance != 0) {
                writer.println("2. ترحيل فرق الانحراف إلى تكلفة البضاعة المباعة (COGS):");
                writer.println("   من حـ/ تكلفة البضاعة المباعة (" + cogsAccount + "): " + variance + " [مدين]");
                writer.println("   إلى حـ/ المصاريف الفعلية (" + actualAccount + "): " + variance + " [دائن]");
            }
            writer.println("==================================================\n");

            System.out.println("Success: Overhead Variance Closing Exported to OverheadVarianceLog.txt");
        } catch (IOException e) {
            System.out.println("Error: Cannot write to OverheadVarianceLog.txt");
        }
    }
}