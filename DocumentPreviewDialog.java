import javax.swing.*;
import java.awt.*;
import java.awt.print.PrinterException;

/**
 * ============================================================================
 * نظام ERP المصنعي - نافذة معاينة المستند والطباعة (DocumentPreviewDialog)
 * ============================================================================
 * - تفعيل التكبير والتصغير الكامل للنافذة (Resizable / Maximize).
 * - معالجة وتنسيق سندات القبض والصرف بطابع ERP صناعي احترافي.
 */
public class DocumentPreviewDialog extends JDialog {

    private JEditorPane txtPreviewArea;
    private JComboBox<String> cmbPaperSize;
    private JComboBox<String> cmbOrientation;
    private JButton btnPrint;
    private JButton btnToggleMaximize;
    private JButton btnClose;
    private boolean isMaximized = false;

    public DocumentPreviewDialog(Frame owner, String title, String documentHtmlContent) {
        super(owner, "معاينة المستند والطباعة - " + title, true);

        // 1. تفعيل التكبير والتصغير وإعادة التحجيم
        setResizable(true);
        setSize(900, 700);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        // 2. شريط إعدادات الطباعة العلوية
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8));
        topPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        topPanel.setBorder(BorderFactory.createTitledBorder("إعدادات الطباعة والورق"));

        cmbPaperSize = new JComboBox<>(new String[]{"A4 (قياسي)", "A5 (سندات صغيرة)", "Letter"});
        cmbOrientation = new JComboBox<>(new String[]{"عمودي (Portrait)", "أفقي (Landscape)"});
        btnToggleMaximize = new JButton("تكبير / استعادة");

        topPanel.add(new JLabel("حجم الورق:"));
        topPanel.add(cmbPaperSize);
        topPanel.add(new JLabel("اتجاه الصفحة:"));
        topPanel.add(cmbOrientation);
        topPanel.add(btnToggleMaximize);

        add(topPanel, BorderLayout.NORTH);

        // 3. منطقة معاينة المستند
        txtPreviewArea = new JEditorPane();
        txtPreviewArea.setContentType("text/html");
        txtPreviewArea.setEditable(false);
        String content = documentHtmlContent != null ? documentHtmlContent : "<h2>لا يوجد محتوى للعرض</h2>";
        txtPreviewArea.setText("<html><body style='font-family: Tahoma, sans-serif;'>" +
            "<div style='text-align:center; border-bottom:2px solid #8b5e34; padding:8px 0 12px; margin-bottom:16px;'>" +
            "<img src='" + ReportHeaderBuilder.getLogoUrl() + "' width='150' height='70'><br>" +
            "<b style='font-size:14px; color:#6b4423;'>Relish day | ريليش داي</b>" +
            "</div>" + content + "</body></html>");

        JScrollPane scrollPane = new JScrollPane(txtPreviewArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        // 4. أزرار التحكم والطباعة
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        btnPrint = new JButton("طباعة");
        btnClose = new JButton("إلغاء وإغلاق");

        btnPrint.setFont(new Font("Tahoma", Font.BOLD, 13));
        btnClose.setFont(new Font("Tahoma", Font.PLAIN, 12));

        bottomPanel.add(btnPrint);
        bottomPanel.add(btnClose);

        add(bottomPanel, BorderLayout.SOUTH);

        // الأحداث
        btnToggleMaximize.addActionListener(e -> toggleMaximize());
        btnPrint.addActionListener(e -> handlePrint());
        btnClose.addActionListener(e -> dispose());
    }

    private void toggleMaximize() {
        if (!isMaximized) {
            setExtendedState(Frame.MAXIMIZED_BOTH);
            isMaximized = true;
        } else {
            setSize(900, 700);
            setLocationRelativeTo(getOwner());
            isMaximized = false;
        }
    }

    private void setExtendedState(int state) {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        Rectangle bounds = gc.getBounds();
        setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    private void handlePrint() {
        try {
            boolean complete = txtPreviewArea.print(null, null, true, null, null, true);
            if (complete) {
                JOptionPane.showMessageDialog(this, "تمت عملية إرسال المستند للطابعة بنجاح!", "نجاح الطباعة", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "خطأ في الاتصال بالطابعة: " + ex.getMessage(), "خطأ طباعة", JOptionPane.ERROR_MESSAGE);
        }
    }
}