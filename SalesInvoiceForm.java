import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class SalesInvoiceForm extends JFrame {
    private final JTextField txtInvoiceNumber = new JTextField();
    private final JTextField txtInvoiceDate = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
    private final JTextField txtCustomerAccount = new JTextField();
    private JComboBox<String> cmbPaymentType;
    private JCheckBox chkApplyTax = new JCheckBox("تطبيق الضريبة");
    private JTextField txtTaxRate = new JTextField("0.15");
    private DefaultTableModel tableModel;
    private JTable itemTable;
    private final JTextField txtBaseAmount = new JTextField("0.00");
    private final JTextField txtTaxAmount = new JTextField("0.00");
    private final JTextField txtTotalAmount = new JTextField("0.00");
    private final JTextField txtGrandTotalAmount = new JTextField("0.00");
    private boolean updatingTotals = false;

    private static final String[] COLUMNS = {"م", "نوع المخزون", "اسم الصنف", "رقم الصنف", "نوع الوحدة", "الكمية", "الجرام", "سعر الوحدة", "الإجمالي"};

    public SalesInvoiceForm() {
        setTitle("نظام ERP المصنعي - فاتورة مبيعات");
        setSize(1300, 800);
        setMinimumSize(new Dimension(1100, 650));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        initUI();
        generateInvoiceNumber();
        installAutoComplete();
    }

    private void initUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        JLabel title = new JLabel("فاتورة مبيعات");
        title.setFont(new Font("Tahoma", Font.BOLD, 15));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("بيانات الفاتورة والحسابات والأصناف والمبالغ");
        sub.setFont(new Font("Tahoma", Font.PLAIN, 12));
        sub.setForeground(new Color(203, 213, 225));
        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        mainContent.add(createInputPanel(), BorderLayout.CENTER);
        mainContent.add(createBottomActionBar(), BorderLayout.SOUTH);
        add(mainContent, BorderLayout.CENTER);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "بيانات الفاتورة والصنف",
                1, 0, new Font("Tahoma", Font.BOLD, 12)
        ));
        panel.setBackground(Color.WHITE);
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel row1 = new JPanel(new GridLayout(1, 4, 8, 5));
        row1.setOpaque(false);
        row1.add(new JLabel("رقم الفاتورة:"));
        txtInvoiceNumber.setEditable(false);
        txtInvoiceNumber.setFont(new Font("Tahoma", Font.BOLD, 12));
        row1.add(txtInvoiceNumber);
        row1.add(new JLabel("تاريخ المبيعات:"));
        txtInvoiceDate.setFont(new Font("Tahoma", Font.PLAIN, 12));
        row1.add(txtInvoiceDate);
        panel.add(row1);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel row2 = new JPanel(new BorderLayout(5, 5));
        row2.setOpaque(false);
        row2.add(new JLabel("حساب العميل المدين:"), BorderLayout.EAST);
        txtCustomerAccount.setFont(new Font("Tahoma", Font.PLAIN, 12));
        JButton btnBrowseCustomer = new JButton("دليل الحسابات");
        btnBrowseCustomer.addActionListener(e -> {
            browseAccounts(txtCustomerAccount, "123");
            autoFillFromCustomer();
        });
        JPanel custInput = new JPanel(new BorderLayout(4, 0));
        custInput.setOpaque(false);
        custInput.add(txtCustomerAccount, BorderLayout.CENTER);
        custInput.add(btnBrowseCustomer, BorderLayout.WEST);
        row2.add(custInput, BorderLayout.CENTER);
        panel.add(row2);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel rowPay = new JPanel(new GridLayout(1, 3, 8, 5));
        rowPay.setOpaque(false);
        rowPay.add(new JLabel("طريقة السداد:"));
        cmbPaymentType = new JComboBox<>(new String[]{"آجل (على حساب العميل 12302)", "نقدي (الصندوق الرئيسي 1110101)"});
        cmbPaymentType.addActionListener(e -> {
            boolean cash = cmbPaymentType.getSelectedIndex() == 1;
            txtCustomerAccount.setEditable(!cash);
            if (cash) {
                txtCustomerAccount.setText("1110101 - الصندوق الرئيسي");
            } else {
                txtCustomerAccount.setEditable(true);
            }
        });
        rowPay.add(cmbPaymentType);
        panel.add(rowPay);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // مربع الضريبة فقط بدون إظهار حساب الضريبة
        JPanel taxPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        taxPanel.setOpaque(false);
        taxPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        chkApplyTax.setSelected(false);
        txtTaxRate.setPreferredSize(new Dimension(80, 25));
        txtTaxRate.setEnabled(false);
        chkApplyTax.addActionListener(e -> txtTaxRate.setEnabled(chkApplyTax.isSelected()));
        taxPanel.add(chkApplyTax);
        taxPanel.add(new JLabel("النسبة:"));
        taxPanel.add(txtTaxRate);
        panel.add(taxPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel itemBox = new JPanel(new BorderLayout(10, 10));
        itemBox.setBorder(BorderFactory.createTitledBorder("جدول الأصناف"));
        itemBox.setOpaque(false);
        itemBox.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public Class<?> getColumnClass(int column) { return String.class; }
            @Override public boolean isCellEditable(int row, int column) { return column >= 1 && column <= 8; }
        };
        itemTable = new JTable(tableModel) {
            @Override public TableCellEditor getCellEditor(int row, int column) {
                if (column == 4) return new DefaultCellEditor(new JComboBox<>(new String[]{"COUNT", "WEIGHT"}));
                return super.getCellEditor(row, column);
            }
        };
        itemTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        itemTable.setFont(new Font("Tahoma", Font.PLAIN, 12));
        itemTable.setRowHeight(25);
        itemTable.getModel().addTableModelListener(e -> calculateRowTotals());
        // دعم الشجرة للأصناف عبر النقر المزدوج
        itemTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = itemTable.getSelectedRow();
                    int col = itemTable.getSelectedColumn();
                    if (col == 2 || col == 3) {
                        AccountTreeDialog dlg = new AccountTreeDialog(SalesInvoiceForm.this, "121");
                        dlg.setVisible(true);
                        if (dlg.isAccountSelected()) {
                            String code = dlg.getSelectedAccountCode();
                            String sql = "SELECT item_code, item_name, unit_type FROM inventory_items WHERE inventory_account=? LIMIT 1";
                            try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                                ps.setString(1, code);
                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) {
                                        tableModel.setValueAt(rs.getString("item_name"), row, 2);
                                        tableModel.setValueAt(rs.getString("item_code"), row, 3);
                                        tableModel.setValueAt(rs.getString("unit_type"), row, 4);
                                    } else {
                                        tableModel.setValueAt(code, row, 3);
                                    }
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(SalesInvoiceForm.this, ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
            }
        });
        JScrollPane tableScroll = new JScrollPane(itemTable);
        itemBox.add(tableScroll, BorderLayout.CENTER);

        JPanel tableBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        tableBtnPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton addRowBtn = new JButton("إضافة سطر");
        JButton removeRowBtn = new JButton("حذف السطر المحدد");
        Font tableBtnFont = new Font("Tahoma", Font.PLAIN, 13);
        addRowBtn.setFont(tableBtnFont);
        removeRowBtn.setFont(tableBtnFont);
        addRowBtn.setPreferredSize(new Dimension(110, 32));
        removeRowBtn.setPreferredSize(new Dimension(130, 32));
        addRowBtn.addActionListener(e -> { addTableRow(); renumberRows(); });
        removeRowBtn.addActionListener(e -> removeSelectedRow());
        tableBtnPanel.add(addRowBtn);
        tableBtnPanel.add(removeRowBtn);
        itemBox.add(tableBtnPanel, BorderLayout.SOUTH);
        panel.add(itemBox);

        // شريط إجماليات مصغر بديل عن اللوحة اليسرى المحذوفة
        JPanel totalsBar = new JPanel(new GridLayout(1, 4, 10, 5));
        totalsBar.setOpaque(false);
        totalsBar.add(new JLabel("الإجمالي:"));
        txtTotalAmount.setEditable(false); totalsBar.add(txtTotalAmount);
        totalsBar.add(new JLabel("الإجمالي مع الضريبة:"));
        txtGrandTotalAmount.setEditable(false); totalsBar.add(txtGrandTotalAmount);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(totalsBar);

        return panel;
    }

    private JPanel createBottomActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bar.setBackground(new Color(248, 250, 252));
        bar.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        JButton btnCalculate = new JButton("إعادة احتساب");
        btnCalculate.addActionListener(e -> calculateTotals());
        JButton btnPreviewPrint = new JButton("معاينة وطباعة");
        btnPreviewPrint.addActionListener(e -> previewInvoice());
        JButton btnPostInvoice = new JButton("حفظ وترحيل");
        btnPostInvoice.setBackground(new Color(16, 185, 129));
        btnPostInvoice.setForeground(Color.WHITE);
        btnPostInvoice.setOpaque(true);
        btnPostInvoice.setFocusPainted(false);
        btnPostInvoice.addActionListener(e -> postInvoiceToDatabase());
        JButton btnClear = new JButton("مسح");
        btnClear.addActionListener(e -> clearForm());
        JButton btnClose = new JButton("إغلاق");
        btnClose.addActionListener(e -> dispose());
        Font actionFont = new Font("Tahoma", Font.PLAIN, 13);
        btnCalculate.setFont(actionFont);
        btnPreviewPrint.setFont(actionFont);
        btnPostInvoice.setFont(new Font("Tahoma", Font.BOLD, 13));
        btnClear.setFont(actionFont);
        btnClose.setFont(actionFont);
        btnCalculate.setPreferredSize(new Dimension(110, 32));
        btnPreviewPrint.setPreferredSize(new Dimension(130, 32));
        btnPostInvoice.setPreferredSize(new Dimension(120, 32));
        btnClear.setPreferredSize(new Dimension(80, 32));
        btnClose.setPreferredSize(new Dimension(80, 32));
        bar.add(btnCalculate); bar.add(btnPreviewPrint); bar.add(btnPostInvoice); bar.add(btnClear); bar.add(btnClose);
        return bar;
    }

    private void installAutoComplete() {
        AutoCompleteHelper.installAccountAutoComplete(txtCustomerAccount, "ASSET", true);
    }

    private void generateInvoiceNumber() { txtInvoiceNumber.setText(DocumentNumberService.next("SALES_INVOICE", "INV-")); }
    private void addTableRow() {
        int row = tableModel.getRowCount();
        Vector<String> rowData = new Vector<>();
        rowData.add(String.valueOf(row + 1)); rowData.add(""); rowData.add(""); rowData.add(""); rowData.add("COUNT"); rowData.add(""); rowData.add(""); rowData.add(""); rowData.add("0.00");
        tableModel.addRow(rowData);
        itemTable.setRowSelectionInterval(row, row);
        itemTable.scrollRectToVisible(itemTable.getCellRect(row, 1, true));
        itemTable.requestFocusInWindow();
    }
    private void removeSelectedRow() {
        int r = itemTable.getSelectedRow();
        if (r >= 0) { tableModel.removeRow(r); renumberRows(); calculateRowTotals(); }
    }
    private void renumberRows() { for (int i = 0; i < tableModel.getRowCount(); i++) tableModel.setValueAt(String.valueOf(i+1), i, 0); }
    private void autoFillFromCustomer() {
        String code = txtCustomerAccount.getText().trim().split(" - ")[0].trim();
        if (code.isEmpty()) return;
        String sql = "SELECT item_code, item_name, unit_type FROM inventory_items WHERE inventory_account=? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) {
                if (tableModel.getRowCount()==0) addTableRow();
                tableModel.setValueAt(rs.getString("item_code"), 0, 3);
                tableModel.setValueAt(rs.getString("item_name"), 0, 2);
                tableModel.setValueAt(rs.getString("unit_type"), 0, 4);
                renumberRows(); calculateRowTotals();
            }}                     } catch (SQLException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(this, e.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
                    }
    }
    private void calculateRowTotals() {
        if (updatingTotals) return;
        updatingTotals = true;
        try {
            double total = 0.0;
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                try {
                    double qty = parseDoubleSafe(tableModel.getValueAt(i, 5));
                    double gram = parseDoubleSafe(tableModel.getValueAt(i, 6));
                    double price = parseDoubleSafe(tableModel.getValueAt(i, 7));
                    String unitType = (String) tableModel.getValueAt(i, 4);
                    if ("WEIGHT".equals(unitType)) qty = qty + (gram/1000.0);
                    double totalVal = qty * price;
                    tableModel.setValueAt(String.format("%,.2f", totalVal), i, 8);
                    total += totalVal;
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(this, e.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
                    }
            }
            txtBaseAmount.setText(String.format("%,.2f", total));
            double taxRate = parseDoubleSafe(txtTaxRate.getText());
            double taxAmount = chkApplyTax.isSelected() ? total * taxRate : 0.0;
            txtTaxAmount.setText(String.format("%,.2f", taxAmount));
            txtTotalAmount.setText(String.format("%,.2f", total));
            txtGrandTotalAmount.setText(String.format("%,.2f", total + taxAmount));
        } finally {
            updatingTotals = false;
        }
    }
    private void calculateTotals() { calculateRowTotals(); }
    private double parseDoubleSafe(Object v) {
        if (v==null) return 0.0;
        try { return Double.parseDouble(v.toString().trim().replace(",","")); } catch (Exception e){ return 0.0; }
    }

    private void postInvoiceToDatabase() {
        try {
            if (tableModel.getRowCount()==0) throw new IllegalArgumentException("يجب إضافة صنف واحد على الأقل في الجدول.");
            double total = parseDoubleSafe(txtTotalAmount.getText());
            if (total <= 0) throw new IllegalArgumentException("إجمالي الفاتورة يجب أن يكون أكبر من الصفر.");
            String invoiceCode = txtInvoiceNumber.getText().trim();
            String invoiceDate = txtInvoiceDate.getText().trim();
            String custAcc = txtCustomerAccount.getText().trim().split(" - ")[0].trim();
            AccountResolver.requireAccount("العميل", custAcc);
            if (!DatabaseManager.isSubAccount(custAcc)) throw new IllegalArgumentException("حساب العميل يجب أن يكون حساباً فرعياً");
            java.util.List<SalesPostingService.SalesLine> lines = new java.util.ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String itemCode = tableModel.getValueAt(i, 3).toString().trim().split(" - ")[0].trim();
                if (itemCode == null || itemCode.isEmpty()) throw new IllegalArgumentException("رقم الصنف مطلوب في السطر " + (i+1));
                String itemName = tableModel.getValueAt(i, 2).toString();
                String unitType = (String) tableModel.getValueAt(i, 4);
                double qty = parseDoubleSafe(tableModel.getValueAt(i, 5));
                double gram = parseDoubleSafe(tableModel.getValueAt(i, 6));
                if ("WEIGHT".equals(unitType)) qty = qty + (gram/1000.0);
                double unitPrice = parseDoubleSafe(tableModel.getValueAt(i, 7));
                if (qty <= 0 || unitPrice <= 0) throw new IllegalArgumentException("الكمية وسعر الوحدة يجب أن يكونا موجبين في السطر " + (i+1));
                double unitCost = unitPrice;
                lines.add(new SalesPostingService.SalesLine(itemCode, itemName, qty, unitPrice, unitCost));
            }
            boolean taxApplied = chkApplyTax.isSelected();
            String firstItem = lines.get(0).getItemCode();
            // جلب الحسابات ديناميكياً بدون Hardcoded
            String revAcc, taxAcc, cogsAcc, fgAcc;
            try (Connection conn = DatabaseManager.getConnection()) {
                revAcc = AccountResolver.resolveRevenueAccount(conn, firstItem);
                AccountResolver.requireAccount("الإيراد", revAcc);
                cogsAcc = AccountResolver.resolveCogsAccount(conn, firstItem);
                AccountResolver.requireAccount("تكلفة المبيعات", cogsAcc);
                fgAcc = AccountResolver.resolveInventoryAccount(conn, firstItem);
                AccountResolver.requireAccount("مخزون الإنتاج التام", fgAcc);
                taxAcc = taxApplied ? AccountResolver.resolveOutputTaxAccount(conn) : null;
                if (taxApplied) AccountResolver.requireAccount("الضريبة", taxAcc);
            }
            boolean success = SalesPostingService.postSale(invoiceCode, invoiceDate, custAcc, revAcc, taxAcc, cogsAcc, fgAcc, lines, taxApplied);
            if (!success) throw new IllegalStateException("تعذر اعتماد فاتورة المبيعات.");
            JOptionPane.showMessageDialog(this, "تم اعتماد فاتورة المبيعات وتسجيل حركة الصرف وتحديث المخزون.", "نجاح", JOptionPane.INFORMATION_MESSAGE);
            generateInvoiceNumber(); calculateRowTotals();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "خطأ في فاتورة المبيعات", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void previewInvoice() {
        try {
            double total = parseDoubleSafe(txtTotalAmount.getText());
            double taxAmount = parseDoubleSafe(txtTaxAmount.getText());
            double grandTotal = parseDoubleSafe(txtGrandTotalAmount.getText());
            StringBuilder rowsHtml = new StringBuilder();
            for (int i=0;i<tableModel.getRowCount();i++) {
                rowsHtml.append("<tr><td>").append(i+1).append("</td><td>").append(tableModel.getValueAt(i,1)).append("</td><td>").append(tableModel.getValueAt(i,2)).append("</td><td>").append(tableModel.getValueAt(i,3)).append("</td><td>").append(tableModel.getValueAt(i,4)).append("</td><td>").append(tableModel.getValueAt(i,5)).append("</td><td>").append(tableModel.getValueAt(i,6)).append("</td><td>").append(tableModel.getValueAt(i,7)).append("</td><td>").append(tableModel.getValueAt(i,8)).append("</td></tr>");
            }
            new InvoicePrintPreviewDialog(this, txtInvoiceNumber.getText().trim(), "مبيعات مباشرة", txtInvoiceDate.getText().trim(), txtCustomerAccount.getText().trim(), "العميل", tableModel.getRowCount()>0?tableModel.getValueAt(0,3).toString():"", tableModel.getRowCount()>0?tableModel.getValueAt(0,2).toString():"", "مبيعات تجارية معتمدة", 1, tableModel.getRowCount()>0?parseDoubleSafe(tableModel.getValueAt(0,7)):0, total, taxAmount, grandTotal, "SALES").setVisible(true);
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "خطأ في فتح شاشة المعاينة: "+ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE); }
    }
    private void browseAccounts(JTextField targetField, String prefix) {
        AccountTreeDialog dialog = new AccountTreeDialog(this, prefix);
        dialog.setVisible(true);
        if (dialog.isAccountSelected()) targetField.setText(dialog.getSelectedAccountCode());
    }
    private void clearForm() {
        cmbPaymentType.setSelectedIndex(0);
        txtCustomerAccount.setEditable(true);
        generateInvoiceNumber(); txtCustomerAccount.setText(""); txtBaseAmount.setText("0.00"); txtTotalAmount.setText("0.00"); txtGrandTotalAmount.setText("0.00"); txtTaxAmount.setText("0.00"); chkApplyTax.setSelected(false); txtTaxRate.setText("0.15"); txtTaxRate.setEnabled(false); while(tableModel.getRowCount()>0) tableModel.removeRow(0); calculateRowTotals();
    }
    public static void main(String[] args) { SwingUtilities.invokeLater(() -> { try{ UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception e){ e.printStackTrace(); JOptionPane.showMessageDialog(null, e.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE); } new SalesInvoiceForm().setVisible(true); }); }
}
