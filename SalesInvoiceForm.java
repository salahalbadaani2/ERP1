import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ============================================================================
 * نظام ERP المصنعي - شاشة إصدار وترحيل فاتورة المبيعات و COGS المزدوجة
 * ============================================================================
 * القيد المالي الأول: إثبات استحقاق العميل والإيراد وضريبة القيمة المضافة
 * القيد المالي الثاني: إثبات تكلفة البضاعة المباعة (COGS) وخفض مخزون الإنتاج التام
 * الرقابة المخزنية: فحص حد إعادة الطلب وتنبيهات النواقص الفورية
 */
public class SalesInvoiceForm extends JFrame {

    // عناصر الإدخال الأساسية
    private JTextField txtInvoiceNumber;
    private JTextField txtInvoiceDate;
    private JComboBox<String> cmbPaymentType; // نقدي / آجل
    private JTextField txtCustomerAccount;
    private JLabel lblCustomerName;
    private JButton btnBrowseCustomer;

    // عناصر اختيار الصنف والمخزن
    private JComboBox<String> cmbFinishedGoodsItem;
    private JTextField txtFinishedGoodsAccount;
    private JTextField txtCogsAccount;
    private JTextField txtRevenueAccount;
    private JTextField txtTaxAccount;

    private JTextField txtAvailableQty;
    private JTextField txtMinStockLevel;
    private JTextField txtQuantity;
    private JTextField txtKg;
    private JTextField txtGram;
    private JComboBox<String> cmbUnitType;
    private JTextField txtUnitPrice;
    private JTextField txtUnitCost; // تكلفة الإنتاج للوحدة
    private JLabel lblStockAlert;

    // المبالغ والحسابات المالية
    private JTextField txtBaseAmount;
    private JTextField txtTaxAmount;
    private JTextField txtTotalAmount;
    private JTextField txtTotalCogs;
    private JTextField txtExpectedProfit;
    private JCheckBox chkApplyTax;
    private JTextArea txtNarration;

    // أزرار التحكم والعمليات
    private JButton btnCalculate;
    private JButton btnPostInvoice;
    private JButton btnPreviewPrint;
    private JButton btnClear;
    private JButton btnClose;

    private static final Font FONT_TITLE = new Font("Tahoma", Font.BOLD, 15);
    private static final Font FONT_BOLD = new Font("Tahoma", Font.BOLD, 12);
    private static final Font FONT_PLAIN = new Font("Tahoma", Font.PLAIN, 12);

    public SalesInvoiceForm() {
        setTitle("نظام ERP المصنعي - فاتورة مبيعات");
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 600));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        initUI();
        generateInvoiceNumber();
        setupCalculations();
    }

    private void initUI() {
        // شريط العنوان العلوي
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42)); // Slate 900
        header.setBorder(new EmptyBorder(14, 22, 14, 22));

        JLabel title = new JLabel("فاتورة مبيعات");
        title.setFont(FONT_TITLE);
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("بيانات الفاتورة والحسابات والأصناف والمبالغ");
        sub.setFont(FONT_PLAIN);
        sub.setForeground(new Color(203, 213, 225));

        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // المحتوى الرئيسي
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBorder(new EmptyBorder(10, 15, 10, 15));

        JPanel formGrid = new JPanel(new GridLayout(1, 2, 12, 10));
        formGrid.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        formGrid.add(createInvoiceDetailsPanel());
        formGrid.add(createAccountingAndFinancialsPanel());

        mainContent.add(formGrid, BorderLayout.CENTER);
        mainContent.add(createBottomActionBar(), BorderLayout.SOUTH);

        add(mainContent, BorderLayout.CENTER);
    }

    // لوحة بيانات الفاتورة والأصناف والمخزن
    private JPanel createInvoiceDetailsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "بيانات الفاتورة والصنف",
                TitledBorder.RIGHT, TitledBorder.TOP, FONT_BOLD
        ));
        panel.setBackground(Color.WHITE);

        // سطر رقم الفاتورة والتاريخ
        JPanel row1 = new JPanel(new GridLayout(1, 4, 8, 5));
        row1.setOpaque(false);
        row1.add(new JLabel("رقم الفاتورة:"));
        txtInvoiceNumber = new JTextField();
        txtInvoiceNumber.setEditable(false);
        txtInvoiceNumber.setFont(FONT_BOLD);
        row1.add(txtInvoiceNumber);

        row1.add(new JLabel("تاريخ المبيعات:"));
        txtInvoiceDate = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        txtInvoiceDate.setFont(FONT_PLAIN);
        row1.add(txtInvoiceDate);
        panel.add(row1);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // سطر العميل ونوع الدفع
        JPanel row2 = new JPanel(new BorderLayout(5, 5));
        row2.setOpaque(false);
        row2.add(new JLabel("حساب العميل المدين:"), BorderLayout.EAST);
        txtCustomerAccount = new JTextField("123020001");
        txtCustomerAccount.setFont(FONT_PLAIN);
        btnBrowseCustomer = new JButton("دليل الحسابات");
        btnBrowseCustomer.addActionListener(e -> browseAccounts(txtCustomerAccount, "123"));
        
        JPanel custInput = new JPanel(new BorderLayout(4, 0));
        custInput.setOpaque(false);
        custInput.add(txtCustomerAccount, BorderLayout.CENTER);
        custInput.add(btnBrowseCustomer, BorderLayout.WEST);
        row2.add(custInput, BorderLayout.CENTER);

        lblCustomerName = new JLabel("شركة الأمل للتوزيع والتجارة");
        lblCustomerName.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblCustomerName.setForeground(new Color(37, 99, 235));
        row2.add(lblCustomerName, BorderLayout.SOUTH);
        panel.add(row2);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // سطر نوع السداد
        JPanel rowPay = new JPanel(new GridLayout(1, 2, 8, 5));
        rowPay.setOpaque(false);
        rowPay.add(new JLabel("طريقة السداد:"));
        cmbPaymentType = new JComboBox<>(new String[]{"آجل (على حساب العميل 12302)", "نقدي (الصندوق العام 11101)"});
        rowPay.add(cmbPaymentType);
        panel.add(rowPay);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // سطر الصنف والمخزون
        JPanel itemBox = new JPanel(new GridLayout(5, 2, 8, 6));
        itemBox.setBorder(BorderFactory.createTitledBorder("بيانات الصنف"));
        itemBox.setOpaque(false);

        itemBox.add(new JLabel("المنتج التام المصنع:"));
        cmbFinishedGoodsItem = new JComboBox<>(new String[]{
                    "ITEM-101 - عصير برتقال طبيعي 1 لتر (كرتون 12 عبوة)",
                    "ITEM-102 - بسكويت ويفر محشو شوكولاتة (كرتون 24 علبة)",
                    "ITEM-103 - مياه معدنية نقية 500 مل (كرتون 24 قارورة)"
        });
        itemBox.add(cmbFinishedGoodsItem);

        itemBox.add(new JLabel("الرصيد المخزني المتاح:"));
        txtAvailableQty = new JTextField("450");
        txtAvailableQty.setEditable(false);
        txtAvailableQty.setFont(FONT_BOLD);
        itemBox.add(txtAvailableQty);

        itemBox.add(new JLabel("الكمية المباعة:"));
        txtQuantity = new JTextField("50");
        txtQuantity.setFont(FONT_BOLD);
        itemBox.add(txtQuantity);

        itemBox.add(new JLabel("نوع الصنف:"));
        cmbUnitType = new JComboBox<>(new String[]{"COUNT", "WEIGHT"});
        itemBox.add(cmbUnitType);

        JPanel kgGramPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        kgGramPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        kgGramPanel.add(new JLabel("الكيلو:"));
        txtKg = new JTextField("0");
        kgGramPanel.add(txtKg);
        kgGramPanel.add(new JLabel("الجرام:"));
        txtGram = new JTextField("0");
        kgGramPanel.add(txtGram);
        itemBox.add(kgGramPanel);

        itemBox.add(new JLabel("سعر بيع الوحدة (YER):"));
        txtUnitPrice = new JTextField("3200.00");
        txtUnitPrice.setFont(FONT_BOLD);
        itemBox.add(txtUnitPrice);

        itemBox.add(new JLabel("تكلفة الإنتاج للوحدة COGS (YER):"));
        txtUnitCost = new JTextField("2100.00");
        txtUnitCost.setFont(FONT_BOLD);
        txtUnitCost.setEditable(false); // تسحب آلياً من بطاقة تكلفة الإنتاج
        itemBox.add(txtUnitCost);

        panel.add(itemBox);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // تنبيه المخزون الرقابي
        lblStockAlert = new JLabel("الرصيد المخزني كافٍ.");
        lblStockAlert.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblStockAlert.setForeground(new Color(16, 185, 129));
        panel.add(lblStockAlert);

        return panel;
    }

    // لوحة الحسابات والمبالغ
    private JPanel createAccountingAndFinancialsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "الحسابات والمبالغ",
                TitledBorder.RIGHT, TitledBorder.TOP, FONT_BOLD
        ));
        panel.setBackground(Color.WHITE);

        // الحسابات المرتبطة بالقيد الآلي
        JPanel accBox = new JPanel(new GridLayout(4, 2, 6, 5));
        accBox.setBorder(BorderFactory.createTitledBorder("الحسابات المرتبطة"));
        accBox.setOpaque(false);

        accBox.add(new JLabel("حساب الإيراد [دائن]:"));
        txtRevenueAccount = new JTextField("410101");
        accBox.add(txtRevenueAccount);

        accBox.add(new JLabel("حساب الضريبة [دائن]:"));
        txtTaxAccount = new JTextField("220301");
        accBox.add(txtTaxAccount);

        accBox.add(new JLabel("حساب تكلفة المبيعات COGS [مدين]:"));
        txtCogsAccount = new JTextField("510101");
        accBox.add(txtCogsAccount);

        accBox.add(new JLabel("حساب مخزون الإنتاج التام [دائن]:"));
        txtFinishedGoodsAccount = new JTextField("1210301");
        accBox.add(txtFinishedGoodsAccount);

        panel.add(accBox);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // المبالغ المالية
        JPanel finBox = new JPanel(new GridLayout(6, 2, 8, 6));
        finBox.setBorder(BorderFactory.createTitledBorder("ملخص الفاتورة"));
        finBox.setOpaque(false);

        finBox.add(new JLabel("قيمة المبيعات الأساسية (Net Sales):"));
        txtBaseAmount = new JTextField("0.00 YER");
        txtBaseAmount.setEditable(false);
        txtBaseAmount.setFont(FONT_BOLD);
        finBox.add(txtBaseAmount);

        chkApplyTax = new JCheckBox("خاضع لضريبة القيمة المضافة (15%)", true);
        chkApplyTax.setFont(FONT_PLAIN);
        finBox.add(chkApplyTax);

        txtTaxAmount = new JTextField("0.00 YER");
        txtTaxAmount.setEditable(false);
        txtTaxAmount.setFont(FONT_BOLD);
        finBox.add(txtTaxAmount);

        finBox.add(new JLabel("إجمالي الفاتورة المستحق على العميل:"));
        txtTotalAmount = new JTextField("0.00 YER");
        txtTotalAmount.setEditable(false);
        txtTotalAmount.setFont(new Font("Tahoma", Font.BOLD, 13));
        txtTotalAmount.setForeground(new Color(37, 99, 235));
        finBox.add(txtTotalAmount);

        finBox.add(new JLabel("إجمالي تكلفة البضاعة المباعة (COGS):"));
        txtTotalCogs = new JTextField("0.00 YER");
        txtTotalCogs.setEditable(false);
        txtTotalCogs.setFont(FONT_BOLD);
        txtTotalCogs.setForeground(new Color(217, 119, 6));
        finBox.add(txtTotalCogs);

        finBox.add(new JLabel("مجمل الربح اللحظي المحقق للفاتورة:"));
        txtExpectedProfit = new JTextField("0.00 YER");
        txtExpectedProfit.setEditable(false);
        txtExpectedProfit.setFont(new Font("Tahoma", Font.BOLD, 13));
        txtExpectedProfit.setForeground(new Color(16, 185, 129));
        finBox.add(txtExpectedProfit);

        panel.add(finBox);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // البيان والشرح
        JPanel narRow = new JPanel(new BorderLayout(5, 5));
        narRow.setOpaque(false);
        narRow.add(new JLabel("البيان والشرح المحاسبي:"), BorderLayout.NORTH);
        txtNarration = new JTextArea(2, 20);
        txtNarration.setText("مبيعات منتجات تامة مصنعة بموجب الفاتورة رقم " + txtInvoiceNumber.getText());
        narRow.add(new JScrollPane(txtNarration), BorderLayout.CENTER);
        panel.add(narRow);

        return panel;
    }

    // شريط أزرار الإجراءات والترحيل
    private JPanel createBottomActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bar.setBackground(new Color(248, 250, 252));
        bar.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));

        btnCalculate = new JButton("إعادة احتساب");
        btnCalculate.setFont(FONT_PLAIN);
        btnCalculate.addActionListener(e -> calculateTotals());

        btnPreviewPrint = new JButton("معاينة وطباعة");
        btnPreviewPrint.setFont(FONT_BOLD);
        btnPreviewPrint.addActionListener(e -> previewInvoice());

        btnPostInvoice = new JButton("حفظ وترحيل");
        btnPostInvoice.setFont(FONT_BOLD);
        btnPostInvoice.setBackground(new Color(16, 185, 129));
        btnPostInvoice.setForeground(Color.WHITE);
        btnPostInvoice.setFocusPainted(false);
        btnPostInvoice.addActionListener(e -> postInvoiceToDatabase());

        btnClear = new JButton("مسح");
        btnClear.setFont(FONT_PLAIN);
        btnClear.addActionListener(e -> clearForm());

        btnClose = new JButton("إغلاق");
        btnClose.setFont(FONT_PLAIN);
        btnClose.addActionListener(e -> dispose());

        bar.add(btnCalculate);
        bar.add(btnPreviewPrint);
        bar.add(btnPostInvoice);
        bar.add(btnClear);
        bar.add(btnClose);

        return bar;
    }

    private void generateInvoiceNumber() {
        txtInvoiceNumber.setText(DocumentNumberService.next("SALES_INVOICE", "INV-"));
    }

    private void setupCalculations() {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { calculateTotals(); }
            @Override
            public void removeUpdate(DocumentEvent e) { calculateTotals(); }
            @Override
            public void changedUpdate(DocumentEvent e) { calculateTotals(); }
        };

        txtQuantity.getDocument().addDocumentListener(listener);
        txtUnitPrice.getDocument().addDocumentListener(listener);
        chkApplyTax.addActionListener(e -> calculateTotals());

        cmbFinishedGoodsItem.addActionListener(e -> {
            int idx = cmbFinishedGoodsItem.getSelectedIndex();
            if (idx == 0) {
                txtAvailableQty.setText("450");
                txtUnitPrice.setText("3200.00");
                txtUnitCost.setText("2100.00");
            } else if (idx == 1) {
                txtAvailableQty.setText("1200");
                txtUnitPrice.setText("1800.00");
                txtUnitCost.setText("1200.00");
            } else {
                txtAvailableQty.setText("25");
                txtUnitPrice.setText("4500.00");
                txtUnitCost.setText("3100.00");
            }
            calculateTotals();
        });

        cmbUnitType.addActionListener(e -> calculateTotals());

        calculateTotals();
    }

    private void calculateTotals() {
        try {
            double qty;
            if ("WEIGHT".equals(cmbUnitType.getSelectedItem())) {
                double kg = Double.parseDouble(txtKg.getText().trim());
                double g = Double.parseDouble(txtGram.getText().trim());
                qty = kg + (g / 1000.0);
            } else {
                qty = Double.parseDouble(txtQuantity.getText().trim());
            }
            double price = Double.parseDouble(txtUnitPrice.getText().trim());
            double cost = Double.parseDouble(txtUnitCost.getText().trim());
            double available = Double.parseDouble(txtAvailableQty.getText().trim());

            // 1. فحص التنبيه المخزني
            double remaining = available - qty;
            if (remaining < 0) {
                lblStockAlert.setText("عجز مخزني: الكمية المطلوبة (" + qty + ") تتجاوز الرصيد المتاح (" + available + ").");
                lblStockAlert.setForeground(new Color(220, 38, 38));
                btnPostInvoice.setEnabled(false);
            } else if (remaining < 50) {
                lblStockAlert.setText("تحذير: الكمية المتبقية بعد البيع (" + remaining + ") ستكون تحت حد الأمان وإعادة الطلب.");
                lblStockAlert.setForeground(new Color(217, 119, 6));
                btnPostInvoice.setEnabled(true);
            } else {
                lblStockAlert.setText("الرصيد المخزني كافٍ (المتبقي: " + remaining + ").");
                lblStockAlert.setForeground(new Color(16, 185, 129));
                btnPostInvoice.setEnabled(true);
            }

            // 2. احتساب المبالغ المالية
            double baseAmount = qty * price;
            double taxAmount = chkApplyTax.isSelected() ? (baseAmount * 0.15) : 0.0;
            double totalAmount = baseAmount + taxAmount;

            double totalCogs = qty * cost;
            double grossProfit = baseAmount - totalCogs;

            txtBaseAmount.setText(String.format("%,.2f YER", baseAmount));
            txtTaxAmount.setText(String.format("%,.2f YER", taxAmount));
            txtTotalAmount.setText(String.format("%,.2f YER", totalAmount));
            txtTotalCogs.setText(String.format("%,.2f YER", totalCogs));
            txtExpectedProfit.setText(String.format("%,.2f YER", grossProfit));

        } catch (Exception ignored) {}
    }

    private void postInvoiceToDatabase() {
        try {
            double qty;
            if ("WEIGHT".equals(cmbUnitType.getSelectedItem())) {
                double kg = Double.parseDouble(txtKg.getText().trim());
                double g = Double.parseDouble(txtGram.getText().trim());
                qty = kg + (g / 1000.0);
            } else {
                qty = Double.parseDouble(txtQuantity.getText().trim());
            }
            double price = Double.parseDouble(txtUnitPrice.getText().trim());
            double unitCost = Double.parseDouble(txtUnitCost.getText().trim());
            String item = (String) cmbFinishedGoodsItem.getSelectedItem();
            String itemCode = extractItemCode(item);
            String itemName = extractItemName(item);
            boolean success = SalesPostingService.postSale(txtInvoiceNumber.getText().trim(), txtInvoiceDate.getText().trim(),
                    txtCustomerAccount.getText().trim(), txtRevenueAccount.getText().trim(), txtTaxAccount.getText().trim(),
                    txtCogsAccount.getText().trim(), txtFinishedGoodsAccount.getText().trim(), itemCode, itemName,
                    qty, price, unitCost, chkApplyTax.isSelected());
            if (!success) throw new IllegalStateException("تعذر اعتماد فاتورة المبيعات.");
            JOptionPane.showMessageDialog(this, "تم اعتماد فاتورة المبيعات وتسجيل حركة الصرف وتحديث المخزون.",
                    "نجاح", JOptionPane.INFORMATION_MESSAGE);
            generateInvoiceNumber();
            calculateTotals();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "خطأ في فاتورة المبيعات", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void postInvoiceToDatabaseLegacy() {
        calculateTotals();
        String invNo = txtInvoiceNumber.getText().trim();
        String invDate = txtInvoiceDate.getText().trim();
        String custAcc = txtCustomerAccount.getText().trim();
        String revAcc = txtRevenueAccount.getText().trim();
        String taxAcc = txtTaxAccount.getText().trim();
        String cogsAcc = txtCogsAccount.getText().trim();
        String fgAcc = txtFinishedGoodsAccount.getText().trim();

        double qty = Double.parseDouble(txtQuantity.getText().trim());
        double price = Double.parseDouble(txtUnitPrice.getText().trim());
        double cost = Double.parseDouble(txtUnitCost.getText().trim());

        double baseAmount = qty * price;
        double taxAmount = chkApplyTax.isSelected() ? (baseAmount * 0.15) : 0.0;
        double totalCustomer = baseAmount + taxAmount;
        double totalCogs = qty * cost;

        // تنفيذ المعاملة المالية المزدوجة الآمنة (ACID Transaction)
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // بدء المعاملة المترابطة

            // 1. إنشاء رأس قيد اليومية المزدوج (Double Entry Journal Voucher)
            String jvNo = "JV-SALES-" + invNo;
            String sqlJv = "INSERT INTO journal_entries (entry_number, entry_date, reference_doc, source_module, narration, total_debit, total_credit, is_posted) " +
                           "VALUES (?, ?, ?, 'SALES', ?, ?, ?, 1)";
            try (PreparedStatement pstmtJv = conn.prepareStatement(sqlJv)) {
                pstmtJv.setString(1, jvNo);
                pstmtJv.setString(2, invDate);
                pstmtJv.setString(3, invNo);
                pstmtJv.setString(4, "إثبات مبيعات الفاتورة " + invNo + " وتكلفة COGS واستنزاف المخزون");
                pstmtJv.setDouble(5, totalCustomer + totalCogs);
                pstmtJv.setDouble(6, totalCustomer + totalCogs);
                pstmtJv.executeUpdate();
            }

            // 2. أسطر القيد المالي الأول: الإيراد والعميل والضريبة
            String sqlLine = "INSERT INTO journal_entry_lines (entry_number, account_code, debit_amount, credit_amount, line_narration) VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmtLine = conn.prepareStatement(sqlLine)) {
                // من حـ/ العميل (مدين بإجمالي الفاتورة)
                pstmtLine.setString(1, jvNo);
                pstmtLine.setString(2, custAcc);
                pstmtLine.setDouble(3, totalCustomer);
                pstmtLine.setDouble(4, 0.0);
                pstmtLine.setString(5, "استحقاق مبيعات الفاتورة " + invNo);
                pstmtLine.executeUpdate();

                // إلى حـ/ إيراد المبيعات (دائن بقيمة المبيعات)
                pstmtLine.setString(1, jvNo);
                pstmtLine.setString(2, revAcc);
                pstmtLine.setDouble(3, 0.0);
                pstmtLine.setDouble(4, baseAmount);
                pstmtLine.setString(5, "إيراد مبيعات منتجات تامة");
                pstmtLine.executeUpdate();

                // إلى حـ/ أمانات الضريبة (دائن بقيمة 15%)
                if (taxAmount > 0) {
                    pstmtLine.setString(1, jvNo);
                    pstmtLine.setString(2, taxAcc);
                    pstmtLine.setDouble(3, 0.0);
                    pstmtLine.setDouble(4, taxAmount);
                    pstmtLine.setString(5, "ضريبة القيمة المضافة 15%");
                    pstmtLine.executeUpdate();
                }

                // 3. أسطر القيد المالي الثاني: تكلفة البضاعة المباعة COGS وخفض المخزون
                // من حـ/ تكلفة البضاعة المباعة COGS (مدين)
                pstmtLine.setString(1, jvNo);
                pstmtLine.setString(2, cogsAcc);
                pstmtLine.setDouble(3, totalCogs);
                pstmtLine.setDouble(4, 0.0);
                pstmtLine.setString(5, "إثبات تكلفة البضاعة المباعة للفاتورة " + invNo);
                pstmtLine.executeUpdate();

                // إلى حـ/ مخزون الإنتاج التام (دائن بخفض رصيد المخزن)
                pstmtLine.setString(1, jvNo);
                pstmtLine.setString(2, fgAcc);
                pstmtLine.setDouble(3, 0.0);
                pstmtLine.setDouble(4, totalCogs);
                pstmtLine.setString(5, "صرف بضاعة تامة مباعة من المستودع");
                pstmtLine.executeUpdate();
            }

            // 4. تحديث الأرصدة الدفترية في شجرة الحسابات
            updateAccountBalance(conn, custAcc, totalCustomer); // زيادة مديونية العميل
            updateAccountBalance(conn, revAcc, -baseAmount); // زيادة دائنية الإيراد
            updateAccountBalance(conn, cogsAcc, totalCogs); // زيادة مدين COGS
            updateAccountBalance(conn, fgAcc, -totalCogs); // خفض رصيد المخزون

            conn.commit(); // اعتماد المعاملة بالكامل بنجاح

            JOptionPane.showMessageDialog(this,
                    "تم ترحيل فاتورة المبيعات بنجاح.\n" +
                    "- رقم الفاتورة: " + invNo + "\n" +
                    "- القيد المالي المزدوج: " + jvNo + "\n" +
                    "- إجمالي المبيعات: " + String.format("%,.2f YER", totalCustomer) + "\n" +
                    "- تكلفة البضاعة المباعة (COGS): " + String.format("%,.2f YER", totalCogs) + "\n" +
                    "- مجمل الربح الصافي المحقق: " + String.format("%,.2f YER", (baseAmount - totalCogs)),
                    "اكتمال الترحيل المحاسبي", JOptionPane.INFORMATION_MESSAGE);

            generateInvoiceNumber();
            calculateTotals();

        } catch (Exception ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            JOptionPane.showMessageDialog(this, "فشل ترحيل الفاتورة: " + ex.getMessage(), "خطأ مالي", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        }
    }

    private void updateAccountBalance(Connection conn, String accountCode, double delta) {
        String sql = "UPDATE chart_of_accounts SET current_balance = current_balance + ? WHERE account_code = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, delta);
            pstmt.setString(2, accountCode);
            pstmt.executeUpdate();
        } catch (Exception ignored) {}
    }

    private void browseAccounts(JTextField targetField, String prefix) {
        AccountTreeDialog dialog = new AccountTreeDialog(this, prefix);
        dialog.setVisible(true);
        if (dialog.isAccountSelected()) {
            targetField.setText(dialog.getSelectedAccountCode());
        }
    }

    private void previewInvoice() {
        try {
            double baseAmount = Double.parseDouble(txtQuantity.getText().trim()) * Double.parseDouble(txtUnitPrice.getText().trim());
            double taxAmount = chkApplyTax.isSelected() ? (baseAmount * 0.15) : 0.0;
            double total = baseAmount + taxAmount;

            InvoicePrintPreviewDialog preview = new InvoicePrintPreviewDialog(
                    this,
                    txtInvoiceNumber.getText().trim(),
                    "مبيعات مباشرة",
                    txtInvoiceDate.getText().trim(),
                    txtCustomerAccount.getText().trim(),
                    lblCustomerName.getText(),
                    extractItemCode((String) cmbFinishedGoodsItem.getSelectedItem()),
                    extractItemName((String) cmbFinishedGoodsItem.getSelectedItem()) + " | حساب المخزون: " + txtFinishedGoodsAccount.getText().trim(),
                    "مبيعات تجارية معتمدة",
                    Double.parseDouble(txtQuantity.getText().trim()),
                    Double.parseDouble(txtUnitPrice.getText().trim()),
                    baseAmount,
                    taxAmount,
                    total
            );
            preview.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في فتح شاشة المعاينة: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String extractItemCode(String item) {
        if (item == null) return "";
        int separator = item.indexOf(" - ");
        return separator > 0 ? item.substring(0, separator).trim() : item.trim();
    }

    private String extractItemName(String item) {
        if (item == null) return "";
        int separator = item.indexOf(" - ");
        return separator > 0 ? item.substring(separator + 3).trim() : item.trim();
    }

    private void clearForm() {
        generateInvoiceNumber();
        txtQuantity.setText("10");
        txtKg.setText("0");
        txtGram.setText("0");
        calculateTotals();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new SalesInvoiceForm().setVisible(true);
        });
    }
}