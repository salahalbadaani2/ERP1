import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * ============================================================================
 * نظام ERP المصنعي - حارس الحسابات المركزي (AccountValidator)
 * ============================================================================
 * 1. حظر القيد المالي على الحسابات الرئيسية (المستويات 1-5).
 * 2. التحقق من الحسابات الفرعية للكلاسات السابقة (DeliveryNote, VanTransferNote).
 * 3. فحص الحركات المالية المسجلة لمنع التعديل الهيكلي للحسابات النشطة وفروعها.
 */
public class AccountValidator {
    /**
     * التحقق المحاسبي والأمني لمنع القيد على الحسابات الرئيسية
     */
    public static void validatePostingAccount(String accountDescription) {
        if (accountDescription == null || accountDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ: لم يتم تحديد الحساب المالي.");
        }
        if (accountDescription.contains("حساب رئيسي") || accountDescription.contains("رئيسي")) {
            throw new IllegalArgumentException("حظر محاسبي: لا يمكن إجراء معاملات مالية على الحسابات الرئيسية (" + accountDescription + "). القيود مسموحة فقط على الحسابات الفرعية.");
        }
    }

    /**
     * دالة التحقق للحسابات الفرعية رمياً للاستثناءات (مطلوبة لـ DeliveryNote و VanTransferNote)
     */
    public static void validateSubAccount(String accountDescription, String contextMessage) {
        if (accountDescription == null || accountDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ في [" + contextMessage + "]: لم يتم تحديد الحساب المالي.");
        }
        validatePostingAccount(accountDescription);
    }

    public static void validateSubAccount(String accountDescription) {
        validatePostingAccount(accountDescription);
    }

    /**
     * دالة الفحص البوليني المباشر (مطلوبة لـ TreasuryVoucher)
     */
    public static boolean isSubAccount(String accountDescription) {
        if (accountDescription == null || accountDescription.trim().isEmpty()) {
            return false;
        }
        return !accountDescription.contains("حساب رئيسي") && !accountDescription.contains("رئيسي")
            && accountDescription.trim().length() > 3;
    }

    public static boolean isValidSubAccount(String accountDescription) {
        return isSubAccount(accountDescription);
    }

    /**
     * دالة الفحص الأمني المعدلة بالبحث عن الكود الرئيسي أو أي كود متفرع منه
     * @param accountCode كود الحساب الصافي (مثال: "12302")
     * @return true إذا وجدت حركات مسجلة على الحساب أو أي من فروعه، false إذا لم توجد
     */
    public static boolean hasFinancialTransactions(String accountCode) {
        if (accountCode == null || accountCode.trim().isEmpty()) {
            return false;
        }

        String cleanCode = accountCode.trim();
        // التعبير النمطي يطابق كود الحساب المباشر أو أي كود فرعي يبدأ به (مثل 12302 و 123020001)
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(cleanCode) + "\\d*\\b");

        String[] logFiles = {
            "TreasuryLog.txt",
            "TreasuryVoucherLog.txt",
            "SalesInvoiceLog.txt",
            "SalesReturnInvoiceLog.txt"
        };

        for (String fileName : logFiles) {
            File file = new File(fileName);
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (pattern.matcher(line).find()) {
                            return true;
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        }

        return false;
    }
}