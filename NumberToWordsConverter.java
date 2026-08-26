/**
 * ============================================================================
 * نظام ERP المصنعي - محول المبالغ إلى كلمات عربية (NumberToWordsConverter)
 * ============================================================================
 * يتولى التفقيط الآلي للمبالغ المالية وتحويل الأرقام إلى كلمات بالريال اليمني.
 */
public class NumberToWordsConverter {

    private static final String[] ones = {
        "", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة", "عشرة",
        "إحدى عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر", "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر"
    };

    private static final String[] tens = {
        "", "", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون"
    };

    private static final String[] hundreds = {
        "", "مائة", "مائتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة"
    };

    public static String convert(double amount, String currencyCode) {
        if (amount <= 0) return "صفر";

        long integerPart = (long) amount;
        int decimalPart = (int) Math.round((amount - integerPart) * 100);

        String currencyName = "ريال يمني";
        if ("USD".equalsIgnoreCase(currencyCode)) currencyName = "دولار أمريكي";
        else if ("SAR".equalsIgnoreCase(currencyCode)) currencyName = "ريال سعودي";

        StringBuilder result = new StringBuilder("فقط ");
        result.append(convertNumber(integerPart)).append(" ").append(currencyName);

        if (decimalPart > 0) {
            result.append(" و ").append(convertNumber(decimalPart)).append(" سنت/فلس");
        }

        result.append(" لا غير.");
        return result.toString();
    }

    private static String convertNumber(long number) {
        if (number == 0) return "";
        if (number < 20) return ones[(int) number];
        if (number < 100) {
            long unit = number % 10;
            long ten = number / 10;
            return (unit > 0 ? ones[(int) unit] + " و " : "") + tens[(int) ten];
        }
        if (number < 1000) {
            long hundred = number / 100;
            long remainder = number % 100;
            return hundreds[(int) hundred] + (remainder > 0 ? " و " + convertNumber(remainder) : "");
        }
        if (number < 1000000) {
            long thousands = number / 1000;
            long remainder = number % 1000;
            String thousandStr = (thousands == 1) ? "ألف" : (thousands == 2) ? "ألفان" : convertNumber(thousands) + " آلاف";
            return thousandStr + (remainder > 0 ? " و " + convertNumber(remainder) : "");
        }
        if (number < 1000000000) {
            long millions = number / 1000000;
            long remainder = number % 1000000;
            String millionStr = (millions == 1) ? "مليون" : (millions == 2) ? "مليونان" : convertNumber(millions) + " ملايين";
            return millionStr + (remainder > 0 ? " و " + convertNumber(remainder) : "");
        }
        return String.valueOf(number);
    }
}