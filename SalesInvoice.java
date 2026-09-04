import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * نظام ERP المصنعي - شاشة فاتورة المبيعات وتكلفة البضاعة المباعة (SalesInvoice)
 * ============================================================================
 * - إثبات إيراد المبيعات وتخفيض مخزون المنتجات التامة مع ترحيل COGS آلياً.
 * - التزام تام بحارس الحسابات (AccountValidator) لمنع الترحيل للحسابات الرئيسية.
 * - دعم التنبيه المخزني المباشر عند تجاوز حد إعادة الطلب.
 * - دعم معاينة وطباعة الفاتورة الرسمية (A4 Document Preview & Print).
 * - ترحيل القيد المالي المزدوج المتوازن إلى MySQL وملف السجلات Log.
 */
public class SalesInvoice extends JFrame {

    private static final String LOG_FILE = "SalesInvoiceLog.txt";
        // مكونات الواجهة الرسومية
    private JTextField txtInvoiceNo;
    private JTextField txtDate;
    private JTextField txtCustomerCode;
    private JComboBox<String> cmbCustomerAccount;
    
    private JTextField txtProductCode;
    private JComboBox<String> cmbProductAccount;
    
    private JTextField txtAvailableStock;
    private JTextField txtQuantity;
    private JTextField txtUnitPrice;
    private JTextField txtUnitCost;
    
    private JCheckBox chkApplyTax;
    private JTextField txtTaxAmount;
    private JTextField txtTotalRevenue;
    private JTextField txtTotalCOGS;
    private JTextField txtGrossProfit;
    private JLabel lblStockAlert;

    private JButton btnCalculate;
    private JButton btnPreview;
    private JButton btnSave;
    private JButton btnClose;

    private List<String> masterAccountList;

    public SalesInvoice() {
        setTitle("نظام ERP المصنعي - فاتورة مبيعات");
        setSize(880, 680);
        setMinimumSize(new Dimension(800, 580));
        setResizable(true); // تفعيل التكبير والتصغير التلقائي للشاشة
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        masterAccountList = new ArrayList<>();
        loadAccountListFromFile();

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        mainPanel.add(createHeaderPanel());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(createDetailsPanel());

        add(mainPanel, BorderLayout.CENTER);
        add(createActionBar(), BorderLayout.SOUTH);

        refreshComboBoxes();
        setupEvents();
        calculateFinancials();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "بيانات الفاتورة والعميل وطريقة الاستحقاق",
            TitledBorder.RIGHT, TitledBorder.TOP, new Font("Tahoma", Font.BOLD, 12), new Color(26, 35, 126)
        ));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        GridBagConstraints gbc = createGbc();

        addLabel(panel, "رقم الفاتورة:", gbc, 0, 0);
        txtInvoiceNo = new JTextField(generateNextInvoiceNo(), 12);
        txtInvoiceNo.setEditable(false);
        txtInvoiceNo.setFont(new Font("Tahoma", Font.BOLD, 12));
        addComp(panel, txtInvoiceNo, gbc, 1, 0);

        addLabel(panel, "تاريخ الفاتورة:", gbc, 2, 0);
        txtDate = new JTextField("2026-08-22", 12);
        txtDate.setFont(new Font("Tahoma", Font.PLAIN, 12));
        addComp(panel, txtDate, gbc, 3, 0);

        addLabel(panel, "حساب العميل (حساب فرعي):", gbc, 0, 1);
        txtCustomerCode = new JTextField(8);
        txtCustomerCode.setFont(new Font("Tahoma", Font.PLAIN, 12));
        cmbCustomerAccount = new JComboBox<>();
        cmbCustomerAccount.setFont(new Font("Tahoma", Font.PLAIN, 12));
        
        JPanel pnlCustomer = new JPanel(new BorderLayout(5, 0));
        pnlCustomer.add(txtCustomerCode, BorderLayout.WEST);
        pnlCustomer.add(cmbCustomerAccount, BorderLayout.CENTER);
        
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 3;
        panel.add(pnlCustomer, gbc);

        return panel;
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "بيانات المنتج التام، التكلفة (COGS)، والرقابة المخزنية",
            TitledBorder.RIGHT, TitledBorder.TOP, new Font("Tahoma", Font.BOLD, 12), new Color(0, 105, 92)
        ));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        GridBagConstraints gbc = createGbc();

        // اختيار الصنف
        addLabel(panel, "صنف المنتج التام (12103):", gbc, 0, 0);
        txtProductCode = new JTextField(8);
        txtProductCode.setFont(new Font("Tahoma", Font.PLAIN, 12));
        cmbProductAccount = new JComboBox<>();
        cmbProductAccount.setFont(new Font("Tahoma", Font.PLAIN, 12));
        
        JPanel pnlProduct = new JPanel(new BorderLayout(5, 0));
        pnlProduct.add(txtProductCode, BorderLayout.WEST);
        pnlProduct.add(cmbProductAccount, BorderLayout.CENTER);
        
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 3;
        panel.add(pnlProduct, gbc);

        // الرصيد المخزني والكمية المباعة
        gbc.gridwidth = 1;
        addLabel(panel, "الرصيد المخزني المتاح:", gbc, 0, 1);
        txtAvailableStock = new JTextField("450", 10);
        txtAvailableStock.setEditable(false);
        txtAvailableStock.setFont(new Font("Tahoma", Font.BOLD, 12));
        addComp(panel, txtAvailableStock, gbc, 1, 1);

        addLabel(panel, "الكمية المباعة:", gbc, 2, 1);
        txtQuantity = new JTextField("100", 10);
        txtQuantity.setFont(new Font("Tahoma", Font.BOLD, 12));
        addComp(panel, txtQuantity, gbc, 3, 1);

        // الأسعار والتكاليف
        addLabel(panel, "سعر بيع الوحدة:", gbc, 0, 2);
        txtUnitPrice = new JTextField("25.0", 10);
        txtUnitPrice.setFont(new Font("Tahoma", Font.BOLD, 12));
        addComp(panel, txtUnitPrice, gbc, 1, 2);

        addLabel(panel, "تكلفة الوحدة (COGS):", gbc, 2, 2);
        txtUnitCost = new JTextField("15.0", 10);
        txtUnitCost.setFont(new Font("Tahoma", Font.BOLD, 12));
        addComp(panel, txtUnitCost, gbc, 3, 2);

        // ضريبة القيمة المضافة والإيراد
        chkApplyTax = new JCheckBox("تطبيق ضريبة المبيعات 15%:", true);
        chkApplyTax.setFont(new Font("Tahoma", Font.PLAIN, 11));
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        panel.add(chkApplyTax, gbc);

        txtTaxAmount = new JTextField("375.00 YER", 10);
        txtTaxAmount.setEditable(false);
        txtTaxAmount.setFont(new Font("Tahoma", Font.BOLD, 11));
        addComp(panel, txtTaxAmount, gbc, 1, 3);

        addLabel(panel, "إجمالي مستحق الفاتورة:", gbc, 2, 3);
        txtTotalRevenue = new JTextField("2,875.00 YER", 10);
        txtTotalRevenue.setEditable(false);
        txtTotalRevenue.setFont(new Font("Tahoma", Font.BOLD, 12));
        txtTotalRevenue.setForeground(new Color(26, 35, 126));
        addComp(panel, txtTotalRevenue, gbc, 3, 3);

        // إجمالي التكلفة ومجمل الربح
        addLabel(panel, "إجمالي تكلفة المبيعات (COGS):", gbc, 0, 4);
        txtTotalCOGS = new JTextField("1,500.00 YER", 10);
        txtTotalCOGS.setEditable(false);
        txtTotalCOGS.setFont(new Font("Tahoma", Font.BOLD, 12));
        txtTotalCOGS.setForeground(new Color(183, 28, 28));
        addComp(panel, txtTotalCOGS, gbc, 1, 4);

        addLabel(panel, "صافي مجمل الربح المحقق:", gbc, 2, 4);
        txtGrossProfit = new JTextField("1,000.00 YER", 10);
        txtGrossProfit.setEditable(false);
        txtGrossProfit.setFont(new Font("Tahoma", Font.BOLD, 12));
        txtGrossProfit.setForeground(new Color(16, 185, 129));
        addComp(panel, txtGrossProfit, gbc, 3, 4);

        // شريط التنبيه المخزني المباشر (Stock Alert)
        lblStockAlert = new JLabel("الرصيد المخزني متاح بعد الصرف.");
        lblStockAlert.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblStockAlert.setForeground(new Color(16, 185, 129));
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 4;
        panel.add(lblStockAlert, gbc);

        return panel;
    }

    private JPanel createActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        bar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
        btnCalculate = new JButton("إعادة احتساب");
        btnCalculate.setFont(new Font("Tahoma", Font.PLAIN, 12));

        btnPreview = new JButton("معاينة وطباعة");
        btnPreview.setFont(new Font("Tahoma", Font.BOLD, 12));

        btnSave = new JButton("حفظ وترحيل");
        btnSave.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnSave.setBackground(new Color(16, 185, 129));
        btnSave.setForeground(Color.WHITE);

        btnClose = new JButton("إغلاق");
        btnClose.setFont(new Font("Tahoma", Font.PLAIN, 12));

        bar.add(btnCalculate);
        bar.add(btnPreview);
        bar.add(btnSave);
        bar.add(btnClose);
        return bar;
    }

    private GridBagConstraints createGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        return gbc;
    }

    private void addLabel(JPanel p, String text, GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x; gbc.gridy = y; gbc.weightx = 0.0;
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Tahoma", Font.PLAIN, 12));
        p.add(lbl, gbc);
    }

    private void addComp(JPanel p, JComponent c, GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x; gbc.gridy = y; gbc.weightx = 0.5;
        p.add(c, gbc);
    }

    private void loadAccountListFromFile() {
        masterAccountList.clear();

        // 1. محاولة القراءة من MySQL
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT account_code, account_name, is_sub_account FROM chart_of_accounts WHERE is_sub_account = 1")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                masterAccountList.add(rs.getString("account_code") + " - " + rs.getString("account_name") + " (حساب فرعي)");
            }
        } catch (Exception ignored) {}

        // تم إلغاء القراءة من AccountsData.txt - المصدر الوحيد هو قاعدة البيانات

        // 3. القيم الافتراضية المعتمدة
        if (masterAccountList.isEmpty()) {
            masterAccountList.add("123020001 - شركة الأمل للتوزيع والتجارة (حساب فرعي)");
            masterAccountList.add("123020002 - مؤسسة النور والبركة للتجارة (حساب فرعي)");
            masterAccountList.add("1210301 - مخزن المنتجات التامة الرئيسي (حساب فرعي)");
            masterAccountList.add("410101 - إيراد مبيعات المنتجات التامة (حساب فرعي)");
            masterAccountList.add("510101 - تكلفة البضاعة المباعة COGS (حساب فرعي)");
            masterAccountList.add("220301 - أمانات ضريبة المبيعات والقيمة المضافة (حساب فرعي)");
        }
    }

    private void refreshComboBoxes() {
        cmbCustomerAccount.removeAllItems();
        cmbProductAccount.removeAllItems();
        for (String acc : masterAccountList) {
            if (acc.contains("12302") || acc.contains("عميل") || acc.contains("شركة") || acc.contains("مؤسسة")) {
                cmbCustomerAccount.addItem(acc);
            }
            if (acc.contains("12103") || acc.contains("عصير") || acc.contains("منتج") || acc.contains("مخزن")) {
                cmbProductAccount.addItem(acc);
            }
        }
        if (cmbCustomerAccount.getItemCount() > 0) syncCode(cmbCustomerAccount, txtCustomerCode);
        if (cmbProductAccount.getItemCount() > 0) syncCode(cmbProductAccount, txtProductCode);
    }

    private void setupEvents() {
        cmbCustomerAccount.addActionListener(e -> syncCode(cmbCustomerAccount, txtCustomerCode));
        cmbProductAccount.addActionListener(e -> {
            syncCode(cmbProductAccount, txtProductCode);
            calculateFinancials();
        });

        DocumentListener docListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { calculateFinancials(); }
            @Override public void removeUpdate(DocumentEvent e) { calculateFinancials(); }
            @Override public void changedUpdate(DocumentEvent e) { calculateFinancials(); }
        };

        txtQuantity.getDocument().addDocumentListener(docListener);
        txtUnitPrice.getDocument().addDocumentListener(docListener);
        txtUnitCost.getDocument().addDocumentListener(docListener);
        chkApplyTax.addActionListener(e -> calculateFinancials());

        btnCalculate.addActionListener(e -> calculateFinancials());
        btnPreview.addActionListener(e -> handlePreviewInvoice());
        btnSave.addActionListener(e -> handleSave());
        btnClose.addActionListener(e -> dispose());
    }

    private void syncCode(JComboBox<String> cmb, JTextField txt) {
        Object selected = cmb.getSelectedItem();
        if (selected != null && selected.toString().contains(" - ")) {
            txt.setText(selected.toString().split(" - ")[0].trim());
        }
    }

    private void calculateFinancials() {
        try {
            double qty = Double.parseDouble(txtQuantity.getText().trim());
            double price = Double.parseDouble(txtUnitPrice.getText().trim());
            double cost = Double.parseDouble(txtUnitCost.getText().trim());
            double available = Double.parseDouble(txtAvailableStock.getText().trim());

            // 1. فحص التنبيه المخزني المباشر (Stock Alert)
            double remaining = available - qty;
            if (remaining < 0) {
                lblStockAlert.setText("عجز مخزني: الكمية المطلوبة (" + qty + ") تتجاوز الرصيد المتاح (" + available + ").");
                lblStockAlert.setForeground(new Color(220, 38, 38));
                btnSave.setEnabled(false);
            } else if (remaining <= 50) {
                lblStockAlert.setText("تنبيه إعادة الطلب: الرصيد بعد البيع (" + remaining + ") سيصل لحد الأمان الأدنى.");
                lblStockAlert.setForeground(new Color(217, 119, 6));
                btnSave.setEnabled(true);
            } else {
                lblStockAlert.setText("الرصيد المخزني متاح بعد الصرف (المتبقي: " + remaining + ").");
                lblStockAlert.setForeground(new Color(16, 185, 129));
                btnSave.setEnabled(true);
            }

            // 2. الحسابات المالية
            double baseSales = qty * price;
            double tax = chkApplyTax.isSelected() ? (baseSales * 0.15) : 0.0;
            double totalDue = baseSales + tax;
            double totalCogs = qty * cost;
            double grossProfit = baseSales - totalCogs;

            txtTaxAmount.setText(String.format("%,.2f YER", tax));
            txtTotalRevenue.setText(String.format("%,.2f YER", totalDue));
            txtTotalCOGS.setText(String.format("%,.2f YER", totalCogs));
            txtGrossProfit.setText(String.format("%,.2f YER", grossProfit));

        } catch (Exception ignored) {}
    }

    private String generateNextInvoiceNo() {
        int max = 1000;
        File file = new File(LOG_FILE);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("INV-")) {
                        int idx = line.indexOf("INV-");
                        if (idx + 8 <= line.length()) {
                            try {
                                int num = Integer.parseInt(line.substring(idx + 4, idx + 8).trim());
                                if (num > max) max = num;
                            } catch (Exception ignored) {}
                        }
                    }
                }
            } catch (IOException ignored) {}
        }
        return "INV-" + (max + 1);
    }

    private void handlePreviewInvoice() {
        try {
            calculateFinancials();
            double qty = Double.parseDouble(txtQuantity.getText().trim());
            double price = Double.parseDouble(txtUnitPrice.getText().trim());
            double baseAmt = qty * price;
            double tax = chkApplyTax.isSelected() ? (baseAmt * 0.15) : 0.0;
            double total = baseAmt + tax;

            InvoicePrintPreviewDialog preview = new InvoicePrintPreviewDialog(
                    this,
                    txtInvoiceNo.getText().trim(),
                    "فاتورة مبيعات مباشرة",
                    txtDate.getText().trim(),
                    txtCustomerCode.getText().trim(),
                    String.valueOf(cmbCustomerAccount.getSelectedItem()),
                    txtProductCode.getText().trim(),
                    String.valueOf(cmbProductAccount.getSelectedItem()),
                    "مبيعات تجارية معتمدة",
                    qty,
                    price,
                    baseAmt,
                    tax,
                    total
            );
            preview.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في فتح شاشة المعاينة: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSave() {
        String customer = (String) cmbCustomerAccount.getSelectedItem();
        String product = (String) cmbProductAccount.getSelectedItem();

        try {
            // التحقق الرقابي الصارم من حارس الحسابات
            AccountValidator.validatePostingAccount(customer);
            AccountValidator.validatePostingAccount(product);

            double qty = Double.parseDouble(txtQuantity.getText().trim());
            double price = Double.parseDouble(txtUnitPrice.getText().trim());
            double cost = Double.parseDouble(txtUnitCost.getText().trim());

            double baseRevenue = qty * price;
            double taxAmount = chkApplyTax.isSelected() ? (baseRevenue * 0.15) : 0.0;
            double totalCustomerCredit = baseRevenue + taxAmount;
            double totalCOGS = qty * cost;
            double grossProfit = baseRevenue - totalCOGS;

            // 1. الترحيل الآمن إلى قاعدة بيانات MySQL (ACID Transaction)
            Connection conn = null;
            long entryId = -1;
            try {
                conn = DatabaseManager.getConnection();
                conn.setAutoCommit(false);

                String jvNo = "JV-SALES-" + txtInvoiceNo.getText().trim();
                String sqlJv = "INSERT INTO journal_entries (entry_number, entry_date, reference_doc, source_module, narration, total_debit, total_credit, posted_by) VALUES (?, ?, ?, 'SALES', ?, ?, ?, 'النظام الآلي')";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlJv, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, jvNo);
                    pstmt.setString(2, txtDate.getText().trim());
                    pstmt.setString(3, txtInvoiceNo.getText().trim());
                    pstmt.setString(4, "إثبات مبيعات الفاتورة " + txtInvoiceNo.getText().trim() + " وتكلفة COGS");
                    pstmt.setDouble(5, totalCustomerCredit + totalCOGS);
                    pstmt.setDouble(6, totalCustomerCredit + totalCOGS);
                    pstmt.executeUpdate();
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            entryId = keys.getLong(1);
                        }
                    }
                }
                if (entryId <= 0) {
                    throw new SQLException("لم يتم توليد entry_id للقيد المحاسبي");
                }

                // خطوط القيد المالي المزدوج
                String sqlLine = "INSERT INTO journal_entry_lines (entry_id, account_code, line_narration, debit_amount, credit_amount) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlLine)) {
                    // من حـ/ العميل (مدين)
                    pstmt.setLong(1, entryId);
                    pstmt.setString(2, txtCustomerCode.getText().trim());
                    pstmt.setString(3, "استحقاق مبيعات");
                    pstmt.setDouble(4, totalCustomerCredit);
                    pstmt.setDouble(5, 0.0);
                    pstmt.executeUpdate();

                    // إلى حـ/ إيراد المبيعات (دائن)
                    pstmt.setLong(1, entryId);
                    pstmt.setString(2, "410101");
                    pstmt.setString(3, "إيراد مبيعات تامة");
                    pstmt.setDouble(4, 0.0);
                    pstmt.setDouble(5, baseRevenue);
                    pstmt.executeUpdate();

                    // إلى حـ/ الضريبة (دائن)
                    if (taxAmount > 0) {
                        pstmt.setLong(1, entryId);
                        pstmt.setString(2, "220301");
                        pstmt.setString(3, "ضريبة القيمة المضافة 15%");
                        pstmt.setDouble(4, 0.0);
                        pstmt.setDouble(5, taxAmount);
                        pstmt.executeUpdate();
                    }

                    // من حـ/ COGS (مدين)
                    pstmt.setLong(1, entryId);
                    pstmt.setString(2, "510101");
                    pstmt.setString(3, "تكلفة البضاعة المباعة");
                    pstmt.setDouble(4, totalCOGS);
                    pstmt.setDouble(5, 0.0);
                    pstmt.executeUpdate();

                    // إلى حـ/ مخزون الإنتاج التام (دائن بخفض المخزن)
                    pstmt.setLong(1, entryId);
                    pstmt.setString(2, txtProductCode.getText().trim());
                    pstmt.setString(3, "صرف بضاعة تامة من المخزن");
                    pstmt.setDouble(4, 0.0);
                    pstmt.setDouble(5, totalCOGS);
                    pstmt.executeUpdate();
                }

                conn.commit();
            } catch (Exception ex) {
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException ignored) {}
                }
                throw ex;
            } finally {
                if (conn != null) {
                    try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                }
            }

            // 2. الحفظ الدائم في ملف السجل النصي Log File
            try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
                writer.write(txtInvoiceNo.getText() + " | " + txtDate.getText() + " | " + customer + " | " + product +
                        " | Qty: " + qty + " | Revenue: " + totalCustomerCredit + " | COGS: " + totalCOGS + " | Profit: " + grossProfit + "\n");
            }

            // رسالة إشعار النجاح المالي
            JOptionPane.showMessageDialog(this,
                "تم ترحيل فاتورة المبيعات وقيد التكلفة بنجاح.\n" +
                "• رقم الفاتورة: " + txtInvoiceNo.getText() + "\n" +
                "• إجمالي المستحق على العميل: " + String.format("%,.2f YER", totalCustomerCredit) + "\n" +
                "• تكلفة البضاعة المباعة (COGS): " + String.format("%,.2f YER", totalCOGS) + "\n" +
                "• صافي مجمل الربح المحقق: " + String.format("%,.2f YER", grossProfit),
                "نجاح الترحيل المالي والمخزني", JOptionPane.INFORMATION_MESSAGE);

            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "خطأ حارس الحسابات", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new SalesInvoice().setVisible(true);
        });
    }
}
