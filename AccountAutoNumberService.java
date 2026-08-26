import java.util.List;

/**
 * ============================================================================
 * كلاس الخدمة: توليد الأرقام التلقائية للحسابات (Account Auto-Numbering)
 * ============================================================================
 * يعتمد على قياس طول المستوى (Level Depth) لضمان تسلسل الأبناء المباشرين
 * وتفادي القفز العشوائي لأعمق نقطة في الشجرة.
 */
public class AccountAutoNumberService {

    public static String generateNextCode(String selectedAccount, boolean isChild, List<String> masterList) {
        if (selectedAccount == null || selectedAccount.isEmpty() || selectedAccount.startsWith("اختر")) {
            return "";
        }

        // استخراج كود الحساب الصافي (مثال: من "12 - الأصول" نستخرج "12")
        String baseCode = selectedAccount.split(" - ")[0].trim();

        if (isChild) {
            return getNextChildCode(baseCode, masterList);
        } else {
            return getNextSiblingCode(baseCode, masterList);
        }
    }

    /**
     * توليد كود (الابن) بالبحث عن الأبناء المباشرين فقط (نفس الطول + 1)
     */
    private static String getNextChildCode(String parentCode, List<String> masterList) {
        // قاعدة الشجرة المعتمدة: إذا كان الأب مستوى 5 (طوله 5)، الابن مستوى 6 (طوله 7) بإضافة خانتين[cite: 8]
        // عدا ذلك، الابن يزيد بخانة واحدة فقط عن الأب.
        int expectedLen = (parentCode.length() == 5) ? parentCode.length() + 2 : parentCode.length() + 1;
        long maxVal = -1;

        for (String acc : masterList) {
            if (acc.startsWith("اختر") || acc.contains("---")) continue;
            
            String code = acc.split(" - ")[0].trim();
            
            // نبحث فقط عن الحسابات التي تبدأ برقم الأب ولها نفس الطول المستهدف للابن المباشر
            if (code.startsWith(parentCode) && code.length() == expectedLen) {
                try {
                    long val = Long.parseLong(code);
                    if (val > maxVal) {
                        maxVal = val;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // إذا لم يسبق وجود أي ابن لهذا الحساب
        if (maxVal == -1) {
            if (parentCode.length() == 5) {
                return parentCode + "01"; // تفريع المستوى السادس[cite: 8]
            } else {
                return parentCode + "1";  // تفريع المستويات العادية
            }
        } else {
            // إضافة 1 لأكبر ابن مباشر موجود
            return String.valueOf(maxVal + 1);
        }
    }

    /**
     * توليد كود (الأخ الموازي) بالبحث عن الحسابات التي تشترك في نفس الأب ونفس الطول
     */
    private static String getNextSiblingCode(String currentCode, List<String> masterList) {
        // إذا كان الحساب في المستوى الأول (رقم واحد مثل 1 أو 2)
        if (currentCode.length() == 1) {
            long max = Long.parseLong(currentCode);
            for (String acc : masterList) {
                if (acc.startsWith("اختر") || acc.contains("---")) continue;
                String c = acc.split(" - ")[0].trim();
                if (c.length() == 1) {
                    long v = Long.parseLong(c);
                    if (v > max) max = v;
                }
            }
            return String.valueOf(max + 1);
        }

        // استخراج رقم الأب للحساب الحالي
        String parentCode;
        if (currentCode.length() == 7) {
            parentCode = currentCode.substring(0, 5); // مثل 1210301 أبوه 12103
        } else {
            parentCode = currentCode.substring(0, currentCode.length() - 1);
        }

        int expectedLen = currentCode.length();
        long maxVal = -1;

        for (String acc : masterList) {
            if (acc.startsWith("اختر") || acc.contains("---")) continue;
            String c = acc.split(" - ")[0].trim();
            
            if (c.startsWith(parentCode) && c.length() == expectedLen) {
                try {
                    long val = Long.parseLong(c);
                    if (val > maxVal) {
                        maxVal = val;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        return String.valueOf(maxVal + 1);
    }
}