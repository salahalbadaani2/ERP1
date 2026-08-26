import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.*;
import java.io.File;

/**
 * ============================================================================
 * نظام ERP المصنعي - معاينة وطباعة المستندات
 * ============================================================================
 */
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

    public InvoicePrintPreviewDialog(Window owner, String invoiceCode, String originalInvoice, String invoiceDate,
                                     String customerAccount, String customerName, String itemCode, String itemName,
                                     String reason, double qty, double unitPrice, double baseAmount,
                                     double taxAmount, double totalAmount) {
        this(owner, invoiceCode, originalInvoice, invoiceDate, customerAccount, customerName, itemCode, itemName,
                reason, qty, unitPrice, baseAmount, taxAmount, totalAmount, detectDocumentType(originalInvoice));
    }

    public InvoicePrintPreviewDialog(Window owner, String invoiceCode, String originalInvoice, String invoiceDate,
                                     String customerAccount, String customerName, String itemCode, String itemName,
                                     String reason, double qty, double unitPrice, double baseAmount,
                                     double taxAmount, double totalAmount, String documentType) {
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

        setSize(850, 750);
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
        // شريط الأدوات
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

        // ورقة الفاتورة الرسمية (ورقة بيضاء تشبه الـ A4)
        InvoicePaperPanel paperPanel = new InvoicePaperPanel();
        JScrollPane scrollPane = new JScrollPane(paperPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(new Color(241, 245, 249));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void printInvoice() {
        PrinterJob job = PrinterJob.getPrinterJob();
        if (printPageFormat == null) printPageFormat = job.defaultPage();
        job.setPrintable(this, printPageFormat);
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                String message = ex.getMessage() == null ? "تعذر إرسال المستند إلى الطابعة." : ex.getMessage();
                JOptionPane.showMessageDialog(this, message, "خطأ في الطباعة", JOptionPane.ERROR_MESSAGE);
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
        revalidate();
        repaint();
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        InvoicePaperPanel paper = new InvoicePaperPanel();
        paper.setSize((int) pageFormat.getImageableWidth(), (int) pageFormat.getImageableHeight());
        paper.printAll(g2d);
        return PAGE_EXISTS;
    }

    // مكون رسم ورقة الفاتورة
    private class InvoicePaperPanel extends JPanel {
        public InvoicePaperPanel() {
            setLayout(null);
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(750, 950));
            setBorder(BorderFactory.createCompoundBorder(
                    new EmptyBorder(25, 25, 25, 25),
                    BorderFactory.createLineBorder(new Color(203, 213, 225), 1)
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int margin = 35;

            Image logo = new ImageIcon(new File("logo.png" + File.separator + "logo.png").getPath()).getImage();
            if (logo.getWidth(null) > 0) {
                g2.drawImage(logo, margin, 10, 190, 82, null);
            }

            // 1. الترويسة الرئيسية
            g2.setFont(new Font("Tahoma", Font.BOLD, 14));
            g2.setColor(new Color(30, 41, 59));
            g2.drawString("Relish day | ريليش داي", w - margin - 255, 28);
            g2.setFont(new Font("Tahoma", Font.PLAIN, 11));
            g2.setColor(new Color(100, 116, 139));
            g2.drawString("المركز الرئيسي - صنعاء، اليمن", w - margin - 255, 47);
            g2.drawString("هاتف: 01-234567 | 770000000", w - margin - 255, 64);
            g2.drawString("رقم الترخيص: 300123456 | السجل التجاري: 102938", w - margin - 255, 81);
            g2.drawString("التاريخ: " + invoiceDate, margin + 20, 108);

            // مربع العنوان في المنتصف
            g2.setColor(new Color(49, 46, 129));
            g2.fillRoundRect(w / 2 - 120, 40, 240, 40, 8, 8);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Tahoma", Font.BOLD, 13));
            g2.drawString(getDocumentTitleArabic(), w / 2 - 100, 58);
            g2.setFont(new Font("Tahoma", Font.PLAIN, 10));
            g2.drawString("(" + getDocumentTitleEnglish() + ")", w / 2 - 55, 73);

            // 2. شريط بيانات الفاتورة
            g2.setColor(new Color(30, 41, 59));
            g2.fillRect(margin, 105, w - (margin * 2), 30);
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("Tahoma", Font.BOLD, 12));
                g2.drawString(("SALES_RETURN".equals(documentType) ? "رقم فاتورة المرتجع: " :
                    ("SALES".equals(documentType) ? "رقم فاتورة المبيعات: " : "رقم السند: ")) + invoiceCode,
                    w - margin - 220, 125);
            g2.setColor(Color.WHITE);
                String referenceLabel = "SALES_RETURN".equals(documentType) ? "الفاتورة الأصلية: " :
                    ("SALES".equals(documentType) ? "نوع البيع: " : "المرجع: ");
                g2.drawString(referenceLabel + originalInvoice, margin + 20, 125);

            // 3. جدول معلومات العميل والسبب
            int y = 145;
            g2.setColor(new Color(248, 250, 252));
            g2.fillRect(margin, y, w - (margin * 2), 65);
            g2.setColor(new Color(226, 232, 240));
            g2.drawRect(margin, y, w - (margin * 2), 65);

            g2.setColor(new Color(30, 41, 59));
            g2.setFont(new Font("Tahoma", Font.BOLD, 11));
            g2.drawString(getPartyLabel() + customerAccount + " - " + customerName, margin + 20, y + 25);
            g2.drawString("رقم الصنف: " + itemCode + " | " + getItemLabel() + itemName, margin + 20, y + 50);
            g2.setColor("SALES_RETURN".equals(documentType) ? new Color(220, 38, 38) : new Color(30, 41, 59));
            g2.drawString(getReasonLabel() + reason, w - margin - 260, y + 25);
            g2.setColor(new Color(79, 70, 229));
                g2.drawString(getSettlementLabel() + ("SALES_RETURN".equals(documentType)
                    ? "آجل (خصم من رصيد العميل)" : "حسب القيد المحاسبي"), w - margin - 260, y + 50);

            // 4. جدول الأصناف والماليات
            y = 225;
            g2.setColor(new Color(241, 245, 249));
            g2.fillRect(margin, y, w - (margin * 2), 28);
            g2.setColor(new Color(203, 213, 225));
            g2.drawRect(margin, y, w - (margin * 2), 28);

            g2.setColor(new Color(30, 41, 59));
            g2.setFont(new Font("Tahoma", Font.BOLD, 11));
            g2.drawString("البيان / تفاصيل الصنف", w - margin - 150, y + 18);
            g2.drawString("الكمية", w - margin - 240, y + 18);
            g2.drawString("سعر الوحدة", w - margin - 320, y + 18);
            g2.drawString("SALES_RETURN".equals(documentType) ? "قيمة المرتجع" : "القيمة", w - margin - 420, y + 18);
            g2.drawString("الضريبة (15%)", w - margin - 520, y + 18);
            g2.drawString("صافي المستحق (YER)", margin + 20, y + 18);

            // سطر الصنف
            y += 28;
            g2.setColor(Color.WHITE);
            g2.fillRect(margin, y, w - (margin * 2), 40);
            g2.setColor(new Color(226, 232, 240));
            g2.drawRect(margin, y, w - (margin * 2), 40);

            g2.setColor(new Color(15, 23, 42));
            g2.setFont(new Font("Tahoma", Font.PLAIN, 11));
            g2.drawString(itemName, w - margin - 180, y + 24);
            g2.drawString(String.format("%.0f", qty), w - margin - 235, y + 24);
            g2.drawString(String.format("%,.2f", unitPrice), w - margin - 320, y + 24);
            g2.drawString(String.format("%,.2f", baseAmount), w - margin - 420, y + 24);
            g2.drawString(String.format("%,.2f", taxAmount), w - margin - 510, y + 24);

            g2.setFont(new Font("Tahoma", Font.BOLD, 12));
            g2.setColor(new Color(16, 185, 129));
            g2.drawString(String.format("%,.2f", totalAmount), margin + 20, y + 24);

            // 5. الإجمالي الكلي النهائي
            y += 40;
            g2.setColor(new Color(248, 250, 252));
            g2.fillRect(margin, y, w - (margin * 2), 35);
            g2.setColor(new Color(203, 213, 225));
            g2.drawRect(margin, y, w - (margin * 2), 35);

            g2.setColor(new Color(30, 41, 59));
            g2.setFont(new Font("Tahoma", Font.BOLD, 12));
            g2.drawString("الإجمالي الكلي النهائي:", w - margin - 150, y + 22);
            g2.drawString(String.format("YER %,.2f", baseAmount), w - margin - 430, y + 22);
            g2.drawString(String.format("YER %,.2f", taxAmount), w - margin - 530, y + 22);
            g2.setColor(new Color(16, 185, 129));
            g2.drawString(String.format("YER %,.2f", totalAmount), margin + 20, y + 22);

            // 6. التذييل والاعتماد
            y += 80;
            g2.setColor(new Color(148, 163, 184));
            g2.setFont(new Font("Tahoma", Font.PLAIN, 10));
            g2.drawString("مستند محاسبي صادر من نظام ERP المصنعي", w / 2 - 120, y);

            y += 60;
            g2.setColor(new Color(71, 85, 105));
            g2.setFont(new Font("Tahoma", Font.BOLD, 11));
            g2.drawString("توقيع المحاسب المستلم: .........................", margin + 40, y);
            g2.drawString("توقيع المسؤول: .........................", w - margin - 220, y);
        }
    }
}