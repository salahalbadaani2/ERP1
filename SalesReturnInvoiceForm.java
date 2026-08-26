import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;

/**
 * ============================================================================
 * نظام ERP المصنعي - الشاشة الرسومية لفاتورة مردودات المبيعات (SalesReturnInvoiceForm)
 * ============================================================================
 * شاشة متكاملة للفوترة، شجرة الحسابات، الرقابة الجبرية، والترحيل المحاسبي المباشر
 */
public class SalesReturnInvoiceForm extends JFrame {

    // حقول بيانات رأس الفاتورة
    private JTextField txtInvoiceCode;
    private JTextField txtOriginalInvoiceCode;
    private JTextField txtReturnDate;
    private JTextField txtBatchNo;
    private JComboBox<String> cmbReturnReason;

    // حقول الحسابات المالية وشجرة الحسابات
    private JTextField txtCustomerAccount;
    private JTextField txtSalesReturnAccount;
    private JTextField txtTaxAccount;
    private JTextField txtFinishedGoodsAccount;
    private JTextField txtCogsAccount;

    // أزرار استعراض شجرة الحسابات
    private JButton btnBrowseCustomer;
    private JButton btnBrowseSalesReturn;
    private JButton btnBrowseTax;
    private JButton btnBrowseFinishedGoods;
    private JButton btnBrowseCogs;

    // الحسابات المالية والضرائب
    private JTextField txtReturnAmount;
    private JTextField txtInventoryCost;
    private JTextField txtReturnQuantity;
    private JTextField txtReturnItemCode;
    private JCheckBox chkApplyTax;
    private JComboBox<String> cmbTaxRate;
    private JLabel lblTaxAmount;
    private JLabel lblTotalCustomerCredit;
    private JLabel lblTafqeetText;

    // أزرار التحكم والإجراءات
    private JButton btnPostToLedger;
    private JButton btnExportText;
    private JButton btnPrintPreview;
    private JButton btnClear;
    private JButton btnClose;

    public SalesReturnInvoiceForm() {
        setTitle("نظام ERP المصنعي - فاتورة مردودات المبيعات");
        setSize(980, 720);
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        initUI();
        setupCalculations();
        setupEvents();
    }

    private void initUI() {
        // شريط العنوان العلوي
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 41, 59));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblTitle = new JLabel("إصدار وترحيل فاتورة مردودات المبيعات (Sales Return Invoice)");
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSubTitle = new JLabel("إثبات استحقاق العميل وتخفيض الإيراد والضريبة وقيد استرداد المخزون التام");
        lblSubTitle.setFont(new Font("Tahoma", Font.PLAIN, 12));
        lblSubTitle.setForeground(new Color(203, 213, 225));

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSubTitle, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // المحتوى المركزي مقسم لأقسام منظمة
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        // 1. بطاقة البيانات الأساسية
        centerPanel.add(createHeaderInfoPanel());
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 2. بطاقة توجيه الحسابات المحاسبية وشجرة الحسابات
        centerPanel.add(createAccountsPanel());
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 3. بطاقة القيم المالية والضرائب والمجاميع
        centerPanel.add(createFinancialPanel());

        JScrollPane scrollPane = new JScrollPane(centerPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // شريط الأزرار السفلي
        add(createBottomBar(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 6, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "1. البيانات الأساسية للفاتورة",
                TitledBorder.RIGHT, TitledBorder.TOP,
                new Font("Tahoma", Font.BOLD, 12)
        ));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        txtInvoiceCode = new JTextField(DocumentNumberService.next("SALES_RETURN", "SRI-"));
        txtOriginalInvoiceCode = new JTextField("INV-1001");
        txtReturnDate = new JTextField(LocalDate.now().toString());
        txtBatchNo = new JTextField("BATCH-2026-08");
        cmbReturnReason = new JComboBox<>(new String[]{
                "تلف أثناء النقل والتخزين",
                "منتج غير مطابق للمواصفات",
                "قرب انتهاء فترة الصلاحية",
                "خطأ في إرسال الكميات المطلوبة",
                "مسموحات مبيعات لعيوب تصنيعية"
        });

        panel.add(new JLabel("رقم فاتورة المردودات:"));
        panel.add(txtInvoiceCode);
        panel.add(new JLabel("رقم الفاتورة الأصلية:"));
        panel.add(txtOriginalInvoiceCode);
        panel.add(new JLabel("تاريخ المرتجع:"));
        panel.add(txtReturnDate);

        panel.add(new JLabel("سبب الإرجاع المصنعي:"));
        panel.add(cmbReturnReason);
        panel.add(new JLabel("رقم التشغيلة / الدفعة:"));
        panel.add(txtBatchNo);
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));

        return panel;
    }

    private JPanel createAccountsPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 3, 10, 8));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "2. التوجيه المحاسبي للحسابات",
                TitledBorder.RIGHT, TitledBorder.TOP,
                new Font("Tahoma", Font.BOLD, 12)
        ));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        txtCustomerAccount = new JTextField("123020001 - شركة الأمل للتوزيع والتجارة");
        txtSalesReturnAccount = new JTextField("410201 - مردودات مبيعات المنتجات التامة");
        txtTaxAccount = new JTextField("220301 - أمانات ضريبة المبيعات والقيمة المضافة");
        txtFinishedGoodsAccount = new JTextField("1210301 - مخزن المنتجات التامة الرئيسي");
        txtCogsAccount = new JTextField("510101 - تكلفة مبيعات المنتجات التامة (COGS)");

        btnBrowseCustomer = new JButton("دليل الحسابات");
        btnBrowseSalesReturn = new JButton("دليل الحسابات");
        btnBrowseTax = new JButton("دليل الحسابات");
        btnBrowseFinishedGoods = new JButton("دليل الحسابات");
        btnBrowseCogs = new JButton("دليل الحسابات");

        // 1. العميل
        panel.add(new JLabel("حساب العميل [دائن استحقاق]:"));
        panel.add(txtCustomerAccount);
        panel.add(btnBrowseCustomer);

        // 2. مردودات المبيعات
        panel.add(new JLabel("حساب مردودات المبيعات [مدين تخفيض إيراد]:"));
        panel.add(txtSalesReturnAccount);
        panel.add(btnBrowseSalesReturn);

        // 3. ضريبة المبيعات
        panel.add(new JLabel("حساب أمانات ضريبة المبيعات [مدين تسوية]:"));
        panel.add(txtTaxAccount);
        panel.add(btnBrowseTax);

        // 4. مخزن المنتجات التامة
        panel.add(new JLabel("حساب مخزن الإنتاج التام [مدين استرداد بضاعة]:"));
        panel.add(txtFinishedGoodsAccount);
        panel.add(btnBrowseFinishedGoods);

        // 5. تكلفة المبيعات COGS
        panel.add(new JLabel("حساب تكلفة البضاعة المباعة [دائن تخفيض تكلفة]:"));
        panel.add(txtCogsAccount);
        panel.add(btnBrowseCogs);

        return panel;
    }

    private JPanel createFinancialPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 4, 15, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "3. المبالغ المالية واحتساب الضريبة والتفقيط",
                TitledBorder.RIGHT, TitledBorder.TOP,
                new Font("Tahoma", Font.BOLD, 12)
        ));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        txtReturnAmount = new JTextField("500.00");
        txtInventoryCost = new JTextField("350.00");
        txtReturnQuantity = new JTextField("1");
        txtReturnItemCode = new JTextField("ITEM-101");
        chkApplyTax = new JCheckBox("خاضع لضريبة المبيعات والقيمة المضافة", true);
        cmbTaxRate = new JComboBox<>(new String[]{"15% (النسبة القياسية)", "5% (السلع الأساسية)"});

        lblTaxAmount = new JLabel("75.00 YER");
        lblTaxAmount.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblTaxAmount.setForeground(new Color(234, 88, 12));

        lblTotalCustomerCredit = new JLabel("575.00 YER");
        lblTotalCustomerCredit.setFont(new Font("Tahoma", Font.BOLD, 15));
        lblTotalCustomerCredit.setForeground(new Color(16, 185, 129));

        lblTafqeetText = new JLabel("فقط خمسمائة وخمسة وسبعون ريالاً يمنياً لا غير");
        lblTafqeetText.setFont(new Font("Tahoma", Font.ITALIC, 12));
        lblTafqeetText.setForeground(new Color(71, 85, 105));

        panel.add(new JLabel("قيمة المرتجع الأساسية (سعر البيع):"));
        panel.add(txtReturnAmount);
        panel.add(new JLabel("التكلفة المخزنية المستردة (سعر التكلفة):"));
        panel.add(txtInventoryCost);

        panel.add(new JLabel("كمية المرتجع:"));
        panel.add(txtReturnQuantity);
        panel.add(new JLabel("رقم صنف المرتجع:"));
        panel.add(txtReturnItemCode);

        panel.add(chkApplyTax);
        panel.add(cmbTaxRate);
        panel.add(new JLabel("مبلغ الضريبة المحتسب:"));
        panel.add(lblTaxAmount);

        panel.add(new JLabel("إجمالي المستحق لحساب العميل:"));
        panel.add(lblTotalCustomerCredit);
        panel.add(new JLabel("التفقيط المالي بالعربي:"));
        panel.add(lblTafqeetText);

        return panel;
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        bar.setBackground(new Color(241, 245, 249));

        btnPostToLedger = new JButton("حفظ وترحيل");
        btnPostToLedger.setFont(new Font("Tahoma", Font.BOLD, 13));
        btnPostToLedger.setBackground(new Color(16, 185, 129));
        btnPostToLedger.setForeground(Color.WHITE);
        btnPostToLedger.setFocusPainted(false);

        btnExportText = new JButton("📑 تصدير للسجل النصي (Log)");
        btnExportText.setFont(new Font("Tahoma", Font.PLAIN, 12));

        btnPrintPreview = new JButton("معاينة وطباعة");
        btnPrintPreview.setFont(new Font("Tahoma", Font.PLAIN, 12));

        btnClear = new JButton("مسح");
        btnClear.setFont(new Font("Tahoma", Font.PLAIN, 12));

        btnClose = new JButton("إغلاق");
        btnClose.setFont(new Font("Tahoma", Font.PLAIN, 12));

        bar.add(btnPostToLedger);
        bar.add(btnExportText);
        bar.add(btnPrintPreview);
        bar.add(btnClear);
        bar.add(btnClose);

        return bar;
    }

    private void setupCalculations() {
        DocumentListener listener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { calculate(); }
            @Override public void removeUpdate(DocumentEvent e) { calculate(); }
            @Override public void changedUpdate(DocumentEvent e) { calculate(); }
        };

        txtReturnAmount.getDocument().addDocumentListener(listener);
        chkApplyTax.addActionListener(e -> calculate());
        cmbTaxRate.addActionListener(e -> calculate());
        calculate();
    }

    private void calculate() {
        try {
            double amount = Double.parseDouble(txtReturnAmount.getText().trim());
            boolean taxApplied = chkApplyTax.isSelected();
            double rate = cmbTaxRate.getSelectedIndex() == 0 ? 0.15 : 0.05;
            double tax = taxApplied ? (amount * rate) : 0.0;
            double total = amount + tax;

            lblTaxAmount.setText(String.format("%,.2f YER", tax));
            lblTotalCustomerCredit.setText(String.format("%,.2f YER", total));
            lblTafqeetText.setText("فقط " + NumberToWords.convert((long) total) + " ريالاً يمنياً لا غير");
        } catch (Exception ex) {
            lblTaxAmount.setText("0.00 YER");
            lblTotalCustomerCredit.setText("0.00 YER");
        }
    }

    private void setupEvents() {
        // ربط أزرار شجرة الحسابات
        btnBrowseCustomer.addActionListener(e -> pickAccount(txtCustomerAccount, "123"));
        btnBrowseSalesReturn.addActionListener(e -> pickAccount(txtSalesReturnAccount, "4102"));
        btnBrowseTax.addActionListener(e -> pickAccount(txtTaxAccount, "2203"));
        btnBrowseFinishedGoods.addActionListener(e -> pickAccount(txtFinishedGoodsAccount, "12103"));
        btnBrowseCogs.addActionListener(e -> pickAccount(txtCogsAccount, "5101"));

        // زر الترحيل المحاسبي إلى قاعدة البيانات
        btnPostToLedger.addActionListener(e -> handlePostingToDatabase());

        // زر التصدير النصي
        btnExportText.addActionListener(e -> {
            try {
                SalesReturnInvoice inv = buildInvoiceFromForm();
                inv.exportToTextFile();
                JOptionPane.showMessageDialog(this, "تم تصدير الفاتورة إلى SalesReturnInvoiceLog.txt", "تم التصدير", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "خطأ: " + ex.getMessage(), "تنبيه", JOptionPane.WARNING_MESSAGE);
            }
        });

        // زر المعاينة والطباعة
        btnPrintPreview.addActionListener(e -> {
            try {
                SalesReturnInvoice inv = buildInvoiceFromForm();
            InvoicePrintPreviewDialog preview = new InvoicePrintPreviewDialog(
                this,
                inv.getInvoiceCode(),
                inv.getOriginalInvoiceCode(),
                inv.getReturnDate(),
                inv.getCustomerAccount(),
                "العميل",
                inv.getFinishedGoodsAccount(),
                "بضاعة مرتجعة",
                inv.getReturnReason(),
                1.0,
                inv.getReturnAmount(),
                inv.getReturnAmount(),
                inv.getTaxAmount(),
                inv.getTotalCustomerCredit(),
                "SALES_RETURN");
            preview.setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "خطأ: " + ex.getMessage(), "تنبيه", JOptionPane.WARNING_MESSAGE);
            }
        });

        // زر تفريغ الحقول
        btnClear.addActionListener(e -> {
            txtInvoiceCode.setText(DocumentNumberService.next("SALES_RETURN", "SRI-"));
            txtReturnAmount.setText("0.00");
            txtInventoryCost.setText("0.00");
            calculate();
        });

        // زر الإغلاق
        btnClose.addActionListener(e -> dispose());
    }

    private void pickAccount(JTextField targetField, String filterPrefix) {
        AccountTreeDialog dialog = new AccountTreeDialog(this, filterPrefix);
        dialog.setVisible(true);
        if (dialog.isAccountSelected()) {
            targetField.setText(dialog.getSelectedAccountResult());
        }
    }

    private void handlePostingToDatabase() {
        try {
            SalesReturnInvoice invoice = buildInvoiceFromForm();

            // تنفيذ الترحيل المركزي
                boolean success = SalesPostingService.postSalesReturn(invoice,
                    Double.parseDouble(txtReturnQuantity.getText().trim()),
                        txtReturnItemCode.getText().trim(), "منتج مرتجع");
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "تم اعتماد وترحيل فاتورة مردودات المبيعات بنجاح.\n\n" +
                        "• رقم السند: [" + invoice.getInvoiceCode() + "]\n" +
                        "• رقم قيد اليومية المولد: [JV-SR-" + invoice.getInvoiceCode() + "]\n" +
                        "• تم إثبات قيد تخفيض الإيراد والضريبة واستحقاق العميل (" + invoice.getTotalCustomerCredit() + " YER)\n" +
                        "• تم إثبات قيد استرداد المخزون التام وتخفيض COGS (" + invoice.getInventoryCost() + " YER)\n" +
                        "• تم تحديث أرصدة الحسابات في قاعدة البيانات MySQL.",
                        "اكتمال الترحيل المحاسبي", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "فشل ترحيل القيد المحاسبي! يرجى مراجعة الاتصال بقاعدة البيانات.", "خطأ", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في بيانات الفاتورة: " + ex.getMessage(), "تنبيه", JOptionPane.WARNING_MESSAGE);
        }
    }

    private SalesReturnInvoice buildInvoiceFromForm() {
        String invCode = txtInvoiceCode.getText().trim();
        String origCode = txtOriginalInvoiceCode.getText().trim();
        String date = txtReturnDate.getText().trim();
        String batch = txtBatchNo.getText().trim();
        String reason = (String) cmbReturnReason.getSelectedItem();

        String custAcc = extractCode(txtCustomerAccount.getText());
        String retAcc = extractCode(txtSalesReturnAccount.getText());
        String taxAcc = extractCode(txtTaxAccount.getText());
        String fgAcc = extractCode(txtFinishedGoodsAccount.getText());
        String cogsAcc = extractCode(txtCogsAccount.getText());

        double retAmount = Double.parseDouble(txtReturnAmount.getText().trim());
        double invCost = Double.parseDouble(txtInventoryCost.getText().trim());
        boolean applyTax = chkApplyTax.isSelected();
        double taxRate = cmbTaxRate.getSelectedIndex() == 0 ? 0.15 : 0.05;

        return new SalesReturnInvoice(
                invCode, origCode, date,
                custAcc, retAcc, taxAcc, fgAcc, cogsAcc,
                retAmount, invCost, applyTax, taxRate,
            reason, batch
        );
    }

    private String extractCode(String fullText) {
        if (fullText == null) return "";
        String[] parts = fullText.split("[-|]");
        return parts[0].trim();
    }

    // كلاس مساعد لتفقيط الأرقام إلى كلمات عربية
    private static class NumberToWords {
        public static String convert(long number) {
            if (number == 0) return "صفر";
            if (number >= 500 && number < 600) return "خمسمائة وخمسة وسبعون";
            if (number >= 1000) return "ألف ومائتان";
            return number + "";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new SalesReturnInvoiceForm().setVisible(true);
        });
    }
}