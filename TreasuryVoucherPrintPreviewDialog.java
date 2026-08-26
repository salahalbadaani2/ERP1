import javax.swing.*;
import java.awt.*;
import java.awt.print.PrinterJob;
import java.time.LocalDate;

public class TreasuryVoucherPrintPreviewDialog extends JDialog {
    private Rectangle normalBounds;
    private boolean maximized;

    public TreasuryVoucherPrintPreviewDialog(JFrame parent, String voucherCode, String voucherDate, 
                                             String voucherType, String paymentMethod, String beneficiary, 
                                             String accountCode, String counterAccount, double amount, 
                                             String referenceNo, String description) {
        super(parent, "معاينة السند والطباعة - " + voucherCode, true);
        setSize(850, 750);
        setResizable(true);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // تحويل المبلغ إلى كلمات عربي (التفقيط)
        String amountInWords = NumberToWordsConverter.convert(amount, "YER");
        boolean isReceipt = voucherType.contains("قبض") || voucherCode.startsWith("RV");
        
        String titleAr = isReceipt ? "سند قبض نقدي / بنكي" : "سند صرف";
        String titleEn = isReceipt ? "RECEIPT VOUCHER" : "PAYMENT VOUCHER";

        String htmlContent = "<html>" +
            "<body style='font-family: Tahoma, sans-serif; margin: 20px; color: #1e293b; line-height: 1.6;'>" +
            
            // Header
            "<div style='text-align: center; border-bottom: 3px double #0f172a; padding-bottom: 15px; margin-bottom: 20px;'>" +
                "<table width='100%' border='0'>" +
                    "<tr>" +
                        "<td width='30%' style='text-align: right; font-size: 12px; color: #475569;'>" +
                            "<img src='" + ReportHeaderBuilder.getLogoUrl() + "' width='120' height='55'><br>" +
                            "<b>Relish day | ريليش داي</b><br>" +
                            "المركز الرئيسي - صنعاء، اليمن<br>" +
                            "هاتف: 770000000 | 234567-01" +
                        "</td>" +
                        "<td width='40%' style='text-align: center;'>" +
                            "<div style='background-color: " + (isReceipt ? "#065f46" : "#991b1b") + "; color: white; padding: 10px; border-radius: 8px; font-weight: bold; font-size: 18px;'>" +
                                titleAr + "<br><span style='font-size: 12px; font-weight: normal;'>" + titleEn + "</span>" +
                            "</div>" +
                        "</td>" +
                        "<td width='30%' style='text-align: left; font-size: 12px; color: #475569;'>" +
                            "<b>الرقم الضريبي:</b> 300123456<br>" +
                            "<b>السجل التجاري:</b> 102938<br>" +
                            "<b>تاريخ الطباعة:</b> " + LocalDate.now() +
                        "</td>" +
                    "</tr>" +
                "</table>" +
            "</div>" +

            // Voucher Details Bar
            "<table width='100%' style='background-color: #f8fafc; border: 1px solid #cbd5e1; border-radius: 6px; padding: 10px; margin-bottom: 20px;'>" +
                "<tr>" +
                    "<td width='33%'><b>رقم السند:</b> <span style='color: #2563eb; font-weight: bold;'>" + voucherCode + "</span></td>" +
                    "<td width='33%'><b>تاريخ السند:</b> " + voucherDate + "</td>" +
                    "<td width='33%'><b>طريقة القبض/الصرف:</b> " + paymentMethod + "</td>" +
                "</tr>" +
            "</table>" +

            // Main Voucher Body
            "<table width='100%' border='1' cellspacing='0' cellpadding='10' style='border-collapse: collapse; border-color: #cbd5e1; margin-bottom: 20px;'>" +
                "<tr style='background-color: #f1f5f9;'>" +
                    "<td width='25%'><b>" + (isReceipt ? "استلمنا من السيد / الجهة:" : "صرفنا إلى السيد / الجهة:") + "</b></td>" +
                    "<td width='75%' style='font-size: 15px; font-weight: bold; color: #0f172a;'>" + beneficiary + " (" + counterAccount + ")</td>" +
                "</tr>" +
                "<tr>" +
                    "<td><b>المبلغ الرقمي:</b></td>" +
                    "<td style='font-size: 16px; font-weight: bold; color: " + (isReceipt ? "#047857" : "#b91c1c") + ";'>" +
                        String.format("%,.2f YER", amount) +
                    "</td>" +
                "</tr>" +
                "<tr style='background-color: #f8fafc;'>" +
                    "<td><b>المبلغ تفقيطاً (بالحروف):</b></td>" +
                    "<td style='font-size: 14px; font-weight: bold; color: #334155;'>" + amountInWords + "</td>" +
                "</tr>" +
                "<tr>" +
                    "<td><b>حساب الخزينة / البنك:</b></td>" +
                    "<td>" + accountCode + "</td>" +
                "</tr>" +
                "<tr>" +
                    "<td><b>رقم المرجع / الشيك:</b></td>" +
                    "<td>" + (referenceNo.isEmpty() ? "---" : referenceNo) + "</td>" +
                "</tr>" +
                "<tr style='background-color: #f8fafc;'>" +
                    "<td><b>وذلك مقابل (البيان):</b></td>" +
                    "<td style='font-size: 14px; color: #1e293b;'>" + description + "</td>" +
                "</tr>" +
            "</table>" +

            // Signatures
            "<br><br>" +
            "<table width='100%' style='text-align: center; margin-top: 30px; font-size: 13px;'>" +
                "<tr>" +
                    "<td width='25%'><b>توقيع المستلم / المورد</b><br><br>...........................................</td>" +
                    "<td width='25%'><b>أمين الصندوق / البنك</b><br><br>...........................................</td>" +
                    "<td width='25%'><b>المحاسب المسؤول</b><br><br>...........................................</td>" +
                    "<td width='25%'><b>مدير الحسابات / النظام</b><br><br>...........................................</td>" +
                "</tr>" +
            "</table>" +

            "</body>" +
            "</html>";

        JEditorPane previewPane = new JEditorPane();
        previewPane.setContentType("text/html");
        previewPane.setText(htmlContent);
        previewPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(previewPane);
        add(scrollPane, BorderLayout.CENTER);

        // Control Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setBackground(new Color(241, 245, 249));

        JButton btnPrint = new JButton("طباعة");
        btnPrint.setFont(new Font("Tahoma", Font.BOLD, 13));
        btnPrint.setBackground(new Color(15, 23, 42));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.addActionListener(e -> {
            try {
                previewPane.print();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "خطأ أثناء الطباعة: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnPageSetup = new JButton("إعداد الصفحة");
        btnPageSetup.setFont(new Font("Tahoma", Font.PLAIN, 13));
        btnPageSetup.addActionListener(e -> {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.pageDialog(job.defaultPage());
        });

        JButton btnMaximize = new JButton("تكبير / استعادة");
        btnMaximize.setFont(new Font("Tahoma", Font.PLAIN, 13));
        btnMaximize.addActionListener(e -> toggleMaximize());

        JButton btnClose = new JButton("إغلاق");
        btnClose.setFont(new Font("Tahoma", Font.PLAIN, 13));
        btnClose.addActionListener(e -> dispose());

        btnPanel.add(btnPrint);
        btnPanel.add(btnPageSetup);
        btnPanel.add(btnMaximize);
        btnPanel.add(btnClose);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void toggleMaximize() {
        if (!maximized) {
            normalBounds = getBounds();
            GraphicsConfiguration gc = getGraphicsConfiguration();
            if (gc != null) setBounds(gc.getBounds());
            maximized = true;
        } else {
            if (normalBounds != null) setBounds(normalBounds);
            maximized = false;
        }
    }
}