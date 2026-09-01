import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.*;
import java.io.File;

public class InvoicePrintPreviewDialog extends JDialog implements Printable {

    private String invoiceCode;
    private String originalInvoice;
    private String invoiceDate;
    private String customerAccount;
    private String customerName;
    private String itemCode;
    private String itemName;
    private String reason;
    private double qty;
    private double unitPrice;
    private double baseAmount;
    private double taxAmount;
    private double totalAmount;
    private String documentType;
    private PageFormat printPageFormat;
    private Rectangle normalBounds;
    private boolean maximized;

    public InvoicePrintPreviewDialog(Window owner, String invoiceCode, String originalInvoice,
                                     String invoiceDate, String customerAccount, String customerName,
                                     String itemCode, String itemName, String reason, double qty,
                                     double unitPrice, double baseAmount, double taxAmount,
                                     double totalAmount) {
        this(owner, invoiceCode, originalInvoice, invoiceDate, customerAccount, customerName,
                itemCode, itemName, reason, qty, unitPrice, baseAmount, taxAmount, totalAmount,
                detectDocumentType(originalInvoice));
    }

    public InvoicePrintPreviewDialog(Window owner, String invoiceCode, String originalInvoice,
                                     String invoiceDate, String customerAccount, String customerName,
                                     String itemCode, String itemName, String reason, double qty,
                                     double unitPrice, double baseAmount, double taxAmount,
                                     double totalAmount, String documentType) {
        super(owner, "معاينة المستند - " + invoiceCode, ModalityType.APPLICATION_MODAL);
        this.invoiceCode = invoiceCode;
        this.originalInvoice = originalInvoice;
        this.invoiceDate = invoiceDate;
        this.customerAccount = customerAccount;
        this.customerName = customerName;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.reason = reason;
        this.qty = qty;
        this.unitPrice = unitPrice;
        this.baseAmount = baseAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.documentType = documentType;
        setSize(1100, 850);
        setResizable(true);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 0));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        initUI();
    }

    private static String detectDocumentType(String originalInvoice) {
        if (originalInvoice == null) return "SALES";
        String value = originalInvoice.toLowerCase();
        if (value.contains("قبض") || value.contains("receipt")) return "RECEIPT";
        if (value.contains("صرف") || value.contains("payment")) return "PAYMENT";
        if (value.contains("مردود") || value.contains("return")) return "SALES_RETURN";
        return "SALES";
    }

    private String getDocumentTitleArabic() {
        switch (documentType) {
            case "RECEIPT": return "سند قبض نقدي / بنكي";
            case "PAYMENT": return "سند صرف";
            case "SALES_RETURN": return "فاتورة مردودات مبيعات";
            default: return "فاتورة مبيعات";
        }
    }

    private String getDocumentTitleEnglish() {
        switch (documentType) {
            case "RECEIPT": return "Receipt Voucher";
            case "PAYMENT": return "Payment Voucher";
            case "SALES_RETURN": return "Sales Return Invoice";
            default: return "Sales Invoice";
        }
    }

    private String getPartyLabel() {
        return ("RECEIPT".equals(documentType) || "PAYMENT".equals(documentType))
                ? "الحساب المقابل: " : "العميل: ";
    }

    private String getItemLabel() {
        return "SALES_RETURN".equals(documentType) ? "الصنف المرتجع: " : "الصنف: ";
    }

    private String getSettlementLabel() {
        if ("RECEIPT".equals(documentType)) return "زيادة النقدية: ";
        if ("PAYMENT".equals(documentType)) return "نقص النقدية: ";
        return "طريقة السداد: ";
    }

    private String getReasonLabel() {
        return "SALES_RETURN".equals(documentType) ? "سبب الإرجاع: " : "البيان: ";
    }

    private void initUI() {
        JPanel topBar = new JPanel(new BorderLayout(10, 10));
        topBar.setBackground(new Color(30, 41, 59));
        topBar.setBorder(new EmptyBorder(12, 20, 12, 20));
        JLabel lblTitle = new JLabel(getDocumentTitleArabic() + " - " + invoiceCode);
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblTitle.setForeground(Color.WHITE);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controls.setOpaque(false);
        JButton btnPrint = new JButton("طباعة");
        btnPrint.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnPrint.setBackground(new Color(16, 185, 129));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.addActionListener(e -> printInvoice());
        JButton btnPageSetup = new JButton("إعداد الصفحة");
        btnPageSetup.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnPageSetup.addActionListener(e -> openPageSetup());
        JButton btnMaximize = new JButton("تكبير / استعادة");
        btnMaximize.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnMaximize.addActionListener(e -> toggleMaximize());
        JButton btnClose = new JButton("إغلاق");
        btnClose.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnClose.addActionListener(e -> dispose());
        controls.add(btnPrint);
        controls.add(btnPageSetup);
        controls.add(btnMaximize);
        controls.add(btnClose);
        topBar.add(lblTitle, BorderLayout.EAST);
        topBar.add(controls, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);

        String htmlContent = buildHtmlContent();
        JEditorPane editorPane = new JEditorPane("text/html", htmlContent);
        editorPane.setEditable(false);
        editorPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        editorPane.setFont(new Font("Tahoma", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private String buildHtmlContent() {
        String partyLabel = getPartyLabel();
        String itemLabel = getItemLabel();
        String reasonLabel = getReasonLabel();
        String settlementLabel = getSettlementLabel();
        String unitTypeStr = "COUNT".equals(documentType) ? "COUNT" : "WEIGHT";

        return "<html dir='rtl' lang='ar'><head><meta charset='UTF-8'>"
                + "<style>"
                + "body{font-family:Tahoma,sans-serif;font-size:12px;margin:0;padding:0;}"
                + ".header{background-color:#0f172a;color:white;padding:15px 20px;text-align:center;}"
                + ".header h1{margin:0;font-size:18px;}"
                + ".header p{margin:5px 0 0 0;font-size:11px;color:#cbd5e1;}"
                + ".invoice-info{width:100%;border-collapse:collapse;margin:10px 0;}"
                + ".invoice-info td{padding:5px 8px;border:1px solid #ccc;font-size:11px;}"
                + ".invoice-info td.label{background-color:#f1f5f9;font-weight:bold;width:20%;}"
                + "table.doc-table{width:100%;border-collapse:collapse;margin:15px 0;}"
                + "table.doc-table th{background-color:#1f2937;color:white;padding:8px;border:1px solid #999;"
                + "text-align:right;font-size:11px;}"
                + "table.doc-table td{padding:7px;border:1px solid #999;text-align:right;font-size:11px;}"
                + "table.doc-table tr:nth-child(even){background-color:#f8fafc;}"
                + "tfoot td{font-weight:bold;background-color:#d1fae5;font-size:12px;}"
                + ".total-box{margin:15px 0;padding:10px;border:2px solid #1f2937;background-color:#f8fafc;}"
                + ".total-box table{width:100%;border-collapse:collapse;}"
                + ".total-box td{padding:5px 8px;border:1px solid #ccc;font-size:12px;}"
                + ".total-box td.label{background-color:#e2e8f0;font-weight:bold;}"
                + ".signatures{margin-top:40px;width:100%;}"
                + ".signatures table{width:100%;border-collapse:collapse;}"
                + ".signatures td{padding:5px;border:1px solid #999;text-align:center;font-size:11px;}"
                + ".footer{margin-top:20px;text-align:center;font-size:10px;color:#94a3b8;}"
                + "</style></head>"
                + "<body>"
                + "<div class='header'>"
                + "<h1>" + getDocumentTitleArabic() + " | " + invoiceCode + "</h1>"
                + "<p>" + getDocumentTitleEnglish() + " | المركز الرئيسي - صنعاء، اليمن</p>"
                + "</div>"
                + "<table class='invoice-info'>"
                + "<tr><td class='label'>" + partyLabel + "</td><td>" + customerAccount + " - " + customerName + "</td>"
                + "<td class='label'>رقم السند:</td><td>" + invoiceCode + "</td></tr>"
                + "<tr><td class='label'>التاريخ:</td><td>" + invoiceDate + "</td>"
                + "<td class='label'>الفاتورة الأصلية:</td><td>" + originalInvoice + "</td></tr>"
                + "<tr><td class='label'>" + itemLabel + "</td><td>" + itemCode + " | " + itemName + "</td>"
                + "<td class='label'>" + settlementLabel + "</td><td>حسب القيد المحاسبي</td></tr>"
                + "</table>"
                + "<table class='doc-table'>"
                + "<thead><tr>"
                + "<th>م</th><th>كود الصنف</th><th>بيان الصنف</th><th>نوع الوحدة</th>"
                + "<th>الكمية</th><th>الجرام</th><th>سعر الوحدة</th><th>الإجمالي</th>"
                + "</tr></thead>"
                + "<tbody>"
                + "<tr>"
                + "<td>1</td><td>" + itemCode + "</td><td>" + itemName + "</td><td>" + unitTypeStr + "</td>"
                + "<td>" + String.format("%,.2f", qty) + "</td><td>-</td><td>" + String.format("%,.2f", unitPrice) + "</td><td>" + String.format("%,.2f", baseAmount) + "</td>"
                + "</tr>"
                + "</tbody>"
                + "<tfoot><tr><td colspan='7'>الإجمالي الكلي</td><td>" + String.format("%,.2f", totalAmount) + "</td></tr></tfoot>"
                + "</table>"
                + "<div class='total-box'>"
                + "<table>"
                + "<tr><td class='label'>قيمة المبيعات الأساسية:</td><td>" + String.format("%,.2f", baseAmount) + " YER</td></tr>"
                + "<tr><td class='label'>الضريبة (15%):</td><td>" + String.format("%,.2f", taxAmount) + " YER</td></tr>"
                + "<tr><td class='label'>الإجمالي الكلي:</td><td>" + String.format("%,.2f", totalAmount) + " YER</td></tr>"
                + "</table>"
                + "</div>"
                + "<div class='signatures'>"
                + "<table>"
                + "<tr>"
                + "<td>توقيع المحاسب المستمل<br>____________________</td>"
                + "<td>ختم واعتماد الإدارة<br>____________________</td>"
                + "<td>توقيع المسؤول<br>____________________</td>"
                + "</tr>"
                + "</table>"
                + "</div>"
                + "<div class='footer'>مستند محاسبي صادر من نظام ERP المصنعي</div>"
                + "</body></html>";
    }

    private void printInvoice() {
        PrinterJob job = PrinterJob.getPrinterJob();
        if (printPageFormat == null) printPageFormat = job.defaultPage();
        job.setPrintable(this, printPageFormat);
        if (job.printDialog()) {
            try { job.print(); } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "تعذر إرسال المستند إلى الطابعة.", "خطأ في الطباعة", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openPageSetup() {
        PrinterJob job = PrinterJob.getPrinterJob();
        printPageFormat = job.pageDialog(printPageFormat == null ? job.defaultPage() : printPageFormat);
    }

    private void toggleMaximize() {
        if (!maximized) {
            normalBounds = getBounds();
            Rectangle screen = getGraphicsConfiguration().getBounds();
            setBounds(screen);
            maximized = true;
        } else {
            if (normalBounds != null) setBounds(normalBounds);
            maximized = false;
        }
        revalidate(); repaint();
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        if (pageIndex > 0) return NO_SUCH_PAGE;
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        return PAGE_EXISTS;
    }
}
