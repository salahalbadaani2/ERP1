import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
     * يستخرج كود الحساب من الوصف ويتحقق من صلاحيته عبر قاعدة البيانات
     */
    public static void validatePostingAccount(String accountDescription) {
        if (accountDescription == null || accountDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ: لم يتم تحديد الحساب المالي.");
        }
        String code = extractAccountCode(accountDescription);
        validateSubAccount(code);
    }

    /**
     * يستخرج كود الحساب من نص الوصف (مثال: "1210301 - مخزن المنتجات (حساب فرعي)" -> "1210301")
     */
    private static String extractAccountCode(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "";
        }
        // التنسيق المتوقع: "كود - اسم (نوع)" أو "كود - اسم"
        String trimmed = description.trim();
        int dashIndex = trimmed.indexOf(" - ");
        if (dashIndex > 0) {
            return trimmed.substring(0, dashIndex).trim();
        }
        // إذا لم يوجد فاصل، نعتبر النص كله كود
        return trimmed;
    }

    /**
     * التحقق من صلاحية حساب للترحيل - استعلام مباشر لقاعدة البيانات
     * الحساب صالح فقط إذا كان موجوداً في chart_of_accounts وحالته is_sub_account = 1
     */
    public static void validateSubAccount(String accountCode, String contextMessage) {
        if (accountCode == null || accountCode.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ في [" + contextMessage + "]: لم يتم تحديد الحساب المالي.");
        }
        validateSubAccount(accountCode);
    }

    public static void validateSubAccount(String accountCode) {
        if (accountCode == null || accountCode.trim().isEmpty()) {
            throw new IllegalArgumentException("خطأ: لم يتم تحديد كود الحساب المالي.");
        }
        String code = accountCode.trim();

        String sql = "SELECT is_sub_account FROM chart_of_accounts WHERE account_code = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !result.getBoolean(1)) {
                    throw new IllegalArgumentException(
                        "خطأ أمني ومحاسبي: الحساب (" + code + ") غير موجود أو ليس حساباً فرعياً صالحاً للترحيل.");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("فشل الاتصال بقاعدة البيانات للتحقق من الحساب: " + code, e);
        }
    }

    /**
     * دالة الفحص البوليني المباشر - استعلام مباشر لقاعدة البيانات
     * الحساب صالح فقط إذا كان موجوداً في chart_of_accounts وحالته is_sub_account = 1
     */
    public static boolean isSubAccount(String accountCode) {
        if (accountCode == null || accountCode.trim().isEmpty()) {
            return false;
        }
        String code = accountCode.trim();

        String sql = "SELECT is_sub_account FROM chart_of_accounts WHERE account_code = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        } catch (SQLException e) {
            // فشل الاتصال بقاعدة البيانات - الحساب غير صالح
            return false;
        }
    }

    public static boolean isValidSubAccount(String accountCode) {
        return isSubAccount(accountCode);
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