import java.time.LocalDate;
import java.io.File;

/**
 * ============================================================================
 * نظام ERP المصنعي - منشئ الترويسة الموحدة
 * ============================================================================
 * يولد هيدر HTML موحد لكافة السندات والتقارير والفواتير باللغة العربية.
 */
public class ReportHeaderBuilder {

    public static String getHeaderHtml(String documentTitle) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='text-align: center; border-bottom: 2px solid #1a237e; padding-bottom: 10px; margin-bottom: 15px;'>");
        html.append("<table width='100%' border='0' style='font-family: Tahoma, sans-serif;'>");
        html.append("<tr>");
        html.append("<td width='32%' style='text-align: right; font-size: 11px; color: #333;'>");
        html.append("<b>Relish day | ريليش داي</b><br>");
        html.append("المركز الرئيسي - صنعاء، اليمن<br>");
        html.append("هاتف: 01-234567 | 770000000");
        html.append("</td>");
        html.append("<td width='36%' style='text-align: center;'>");
        html.append("<img src='").append(getLogoUrl()).append("' width='150' height='70'><br>");
        html.append("<div style='background-color: #1a237e; color: #ffffff; padding: 4px 8px; border-radius: 4px; display: inline-block; margin-top: 5px; font-weight: bold; font-size: 13px;'>");
        html.append(documentTitle);
        html.append("</div>");
        html.append("</td>");
        html.append("<td width='32%' style='text-align: left; font-size: 11px; color: #333;'>");
        html.append("<b>الرقم الضريبي:</b> 300123456<br>");
        html.append("<b>السجل التجاري:</b> 102938<br>");
        html.append("<b>التاريخ:</b> ").append(LocalDate.now().toString());
        html.append("</td>");
        html.append("</tr>");
        html.append("</table>");
        html.append("</div>");
        return html.toString();
    }

    public static String getLogoUrl() {
        return new File("logo.png" + File.separator + "logo.png").toURI().toString();
    }
}