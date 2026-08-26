import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * ============================================================================
 * نظام ERP المصنعي - موحد تنسيق المبالغ والعملات (CurrencyFormatter)
 * ============================================================================
 * يتولى تنسيق المبالغ المالية بأرقام إنجليزية موحدة (Locale.US) وفواصل ألفية.
 */
public class CurrencyFormatter {

    // تثبيت Locale.US لمنع تحويل الأرقام إلى عربية طبقاً لنظام التشغيل
    private static final DecimalFormat formatter = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));
    private static final DecimalFormat integerFormatter = new DecimalFormat("#,##0", new DecimalFormatSymbols(Locale.US));

    public static String format(double amount) {
        return formatter.format(amount);
    }

    public static String formatInteger(double amount) {
        return integerFormatter.format(amount);
    }

    /**
     * تنقية النص المدخل وتحويل الأرقام العربية إلى إنجليزية قبل الحسابات
     */
    public static double parse(String formattedAmount) {
        if (formattedAmount == null || formattedAmount.trim().isEmpty()) {
            return 0.0;
        }
        try {
            String clean = formattedAmount.trim()
                    .replaceAll("[،,]", "")
                    .replace('٫', '.');

            StringBuilder asciiStr = new StringBuilder();
            for (char ch : clean.toCharArray()) {
                if (ch >= '\u0660' && ch <= '\u0669') {
                    asciiStr.append((char) (ch - '\u0660' + '0'));
                } else if (ch >= '\u06f0' && ch <= '\u06f9') {
                    asciiStr.append((char) (ch - '\u06f0' + '0'));
                } else {
                    asciiStr.append(ch);
                }
            }
            return Double.parseDouble(asciiStr.toString().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}