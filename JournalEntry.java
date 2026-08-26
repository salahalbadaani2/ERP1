import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * نظام ERP المصنعي - كلاس سند قيد اليومية العامة (JournalEntry)
 * ============================================================================
 * يمثل سند القيد المحاسبي المزدوج المتزن مع سطوره التفصيلية
 */
public class JournalEntry {

    public static class JournalLine {
        private String accountCode;
        private String accountName;
        private String lineNarration;
        private double debitAmount;
        private double creditAmount;

        public JournalLine(String accountCode, String accountName, String lineNarration, double debitAmount, double creditAmount) {
            // التحقق الجبري عبر حارس الحسابات المركزي
            AccountValidator.validateSubAccount(accountCode, "سطر القيد المحاسبي: " + accountName);
            if (debitAmount < 0 || creditAmount < 0 || (debitAmount > 0 && creditAmount > 0)) {
                throw new IllegalArgumentException("خطأ محاسبي: يجب أن يكون سطر القيد مديناً أو دائناً فقط وبقيمة غير سالبة.");
            }
            
            this.accountCode = accountCode;
            this.accountName = accountName;
            this.lineNarration = lineNarration;
            this.debitAmount = debitAmount;
            this.creditAmount = creditAmount;
        }

        public String getAccountCode() { return accountCode; }
        public String getAccountName() { return accountName; }
        public String getLineNarration() { return lineNarration; }
        public double getDebitAmount() { return debitAmount; }
        public double getCreditAmount() { return creditAmount; }
    }

    private String entryNumber;       // رقم القيد (مثال: JV-2026-001)
    private String entryDate;         // تاريخ القيد (YYYY-MM-DD)
    private String referenceDoc;      // المرجع (رقم فاتورة المبيعات أو المردود)
    private String sourceModule;      // المصدر (SALES, SALES_RETURN, TREASURY, MANUAL)
    private String narration;         // البيان العام للقيد
    private List<JournalLine> lines;  // سطور القيد المحاسبي

    public JournalEntry(String entryNumber, String referenceDoc, String sourceModule, String narration) {
        this.entryNumber = entryNumber;
        this.entryDate = LocalDate.now().toString();
        this.referenceDoc = referenceDoc;
        this.sourceModule = sourceModule;
        this.narration = narration;
        this.lines = new ArrayList<>();
    }

    /**
     * إضافة سطر مدين
     */
    public void addDebitLine(String accountCode, String accountName, String lineNarration, double amount) {
        requirePositiveAmount(amount);
        lines.add(new JournalLine(accountCode, accountName, lineNarration, amount, 0.0));
    }

    /**
     * إضافة سطر دائن
     */
    public void addCreditLine(String accountCode, String accountName, String lineNarration, double amount) {
        requirePositiveAmount(amount);
        lines.add(new JournalLine(accountCode, accountName, lineNarration, 0.0, amount));
    }

    private void requirePositiveAmount(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("خطأ محاسبي: يجب أن تكون قيمة سطر القيد رقماً موجباً.");
        }
    }

    public double getTotalDebit() {
        return lines.stream().mapToDouble(JournalLine::getDebitAmount).sum();
    }

    public double getTotalCredit() {
        return lines.stream().mapToDouble(JournalLine::getCreditAmount).sum();
    }

    /**
     * التحقق من توازن القيد المحاسبي (المدين = الدائن)
     */
    public boolean isBalanced() {
        return Math.abs(getTotalDebit() - getTotalCredit()) < 0.001 && getTotalDebit() > 0;
    }

    public String getEntryNumber() { return entryNumber; }
    public String getEntryDate() { return entryDate; }
    public String getReferenceDoc() { return referenceDoc; }
    public String getSourceModule() { return sourceModule; }
    public String getNarration() { return narration; }
    public List<JournalLine> getLines() { return lines; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("قيد محاسبي رقم: %s | المرجع: %s | التاريخ: %s\n", entryNumber, referenceDoc, entryDate));
        sb.append(String.format("البيان: %s\n", narration));
        sb.append("------------------------------------------------------------------------\n");
        sb.append(String.format("%-15s %-30s %-12s %-12s\n", "رقم الحساب", "اسم الحساب", "مدين (Debit)", "دائن (Credit)"));
        sb.append("------------------------------------------------------------------------\n");
        for (JournalLine line : lines) {
            sb.append(String.format("%-15s %-30s %-12.2f %-12.2f\n",
                    line.getAccountCode(),
                    line.getAccountName(),
                    line.getDebitAmount(),
                    line.getCreditAmount()));
        }
        sb.append("------------------------------------------------------------------------\n");
        sb.append(String.format("الإجمالي: %44.2f %12.2f | الحالة: %s\n",
                getTotalDebit(), getTotalCredit(), (isBalanced() ? "متزن" : "غير متزن")));
        return sb.toString();
    }
}