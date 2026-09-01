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
    private final JTextField txtRevenueAccount = new JTextField();
    private final JTextField txtTaxAccount = new JTextField("220301");
    private final JTextField txtCogsAccount = new JTextField();
    private final JTextField txtFinishedGoodsAccount = new JTextField();
    private JComboBox<String> cmbPaymentType;
    private DefaultTableModel tableModel;
    private JTable itemTable;
    private final JTextField txtBaseAmount = new JTextField();
    private final JTextField txtTotalAmount = new JTextField();
    private final JTextField txtTotalCogs = new JTextField();
    private final JTextField txtExpectedProfit = new JTextField();
    private final JTextField txtTaxAmount = new JTextField("0.00");

    private static final String[] COLUMNS = {"م", "كود الصنف", "اسم الصنف", "نوع الوحدة", "الكمية", "الجرام", "سعر الوحدة", "الإجمالي"};

    public SalesInvoiceForm() {
        setTitle("نظام ERP المصنعي - فاتورة مبيعات");
        setSize(1200, 750);
        setMinimumSize(new Dimension(1000, 600));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        initUI();
        generateInvoiceNumber();
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

        JPanel formGrid = new JPanel(new GridLayout(1, 2, 12, 10));
        formGrid.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        formGrid.add(createInputPanel());
        formGrid.add(createTotalsPanel());
        mainContent.add(formGrid, BorderLayout.CENTER);
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
        btnBrowseCustomer.addActionListener(e -> browseAccounts(txtCustomerAccount, "123"));
        JPanel custInput = new JPanel(new BorderLayout(4, 0));
        custInput.setOpaque(false);
        custInput.add(txtCustomerAccount, BorderLayout.CENTER);
        custInput.add(btnBrowseCustomer, BorderLayout.WEST);
        row2.add(custInput, BorderLayout.CENTER);
        panel.add(row2);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel rowPay = new JPanel(new GridLayout(1, 2, 8, 5));
        rowPay.setOpaque(false);
        rowPay.add(new JLabel("طريقة السداد:"));
        cmbPaymentType = new JComboBox<>(new String[]{"آجل (على حساب العميل 12302)", "نقدي (الصندوق العام 11101)"});
        rowPay.add(cmbPaymentType);
        panel.add(rowPay);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel accBox = new JPanel(new GridLayout(5, 2, 6, 5));
        accBox.setBorder(BorderFactory.createTitledBorder("الحسابات المرتبطة"));
        accBox.setOpaque(false);
        accBox.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        accBox.add(new JLabel("حساب الإيراد [دائن]:"));
        accBox.add(accountField(txtRevenueAccount, "4101"));
        accBox.add(new JLabel("حساب الضريبة [دائن]:"));
        accBox.add(accountField(txtTaxAccount, "2203"));
        accBox.add(new JLabel("حساب تكلفة المبيعات COGS [مدين]:"));
        accBox.add(accountField(txtCogsAccount, "5101"));
        accBox.add(new JLabel("حساب مخزون الإنتاج التام [دائن]:"));
        accBox.add(accountField(txtFinishedGoodsAccount, "12103"));
        panel.add(accBox);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel itemBox = new JPanel(new BorderLayout(10, 10));
        itemBox.setBorder(BorderFactory.createTitledBorder("جدول الأصناف"));
        itemBox.setOpaque(false);
        itemBox.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 1 && column <= 6;
            }
        };
        itemTable = new JTable(tableModel) {
            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                if (column == 3) {
                    return new DefaultCellEditor(new JComboBox<>(new String[]{"COUNT", "WEIGHT"}));
                }
                return super.getCellEditor(row, column);
            }
        };
        itemTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        itemTable.setFont(new Font("Tahoma", Font.PLAIN, 12));
        itemTable.setRowHeight(25);
        itemTable.getModel().addTableModelListener(e -> calculateRowTotals());
        JScrollPane tableScroll = new JScrollPane(itemTable);
        itemBox.add(tableScroll, BorderLayout.CENTER);

        JPanel tableBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        tableBtnPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton addRowBtn = new JButton("إضافة سطر");
        JButton removeRowBtn = new JButton("حذف السطر المحدد");
        addRowBtn.addActionListener(e -> addTableRow());
        removeRowBtn.addActionListener(e -> removeSelectedRow());
        tableBtnPanel.add(addRowBtn);
        tableBtnPanel.add(removeRowBtn);
        itemBox.add(tableBtnPanel, BorderLayout.SOUTH);
        panel.add(itemBox);

        return panel;
    }

    private JPanel createTotalsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "الحسابات والمبالغ",
                1, 0, new Font("Tahoma", Font.BOLD, 12)
        ));
        panel.setBackground(Color.WHITE);
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel finBox = new JPanel(new GridLayout(6, 2, 8, 6));
        finBox.setBorder(BorderFactory.createTitledBorder("ملخص الفاتورة"));
        finBox.setOpaque(false);
        finBox.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        finBox.add(new JLabel("قيمة المبيعات الأساسية (Net Sales):"));
        txtBaseAmount.setEditable(false);
        txtBaseAmount.setFont(new Font("Tahoma", Font.BOLD, 12));
        finBox.add(txtBaseAmount);

        finBox.add(new JLabel("الضريبة (15%):"));
        txtTaxAmount.setEditable(false);
        txtTaxAmount.setFont(new Font("Tahoma", Font.BOLD, 12));
        finBox.add(txtTaxAmount);

        finBox.add(new JLabel("إجمالي الفاتورة المستحق على العميل:"));
        txtTotalAmount.setEditable(false);
        txtTotalAmount.setFont(new Font("Tahoma", Font.BOLD, 13));
        txtTotalAmount.setForeground(new Color(37, 99, 235));
        finBox.add(txtTotalAmount);

        finBox.add(new JLabel("إجمالي تكلفة البضاعة المباعة (COGS):"));
        txtTotalCogs.setEditable(false);
        txtTotalCogs.setFont(new Font("Tahoma", Font.BOLD, 12));
        txtTotalCogs.setForeground(new Color(217, 119, 6));
        finBox.add(txtTotalCogs);

        finBox.add(new JLabel("مجمل الربح اللحظي المحقق للفاتورة:"));
        txtExpectedProfit.setEditable(false);
        txtExpectedProfit.setFont(new Font("Tahoma", Font.BOLD, 13));
        txtExpectedProfit.setForeground(new Color(16, 185, 129));
        finBox.add(txtExpectedProfit);

        panel.add(finBox);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel narRow = new JPanel(new BorderLayout(5, 5));
        narRow.setOpaque(false);
        narRow.add(new JLabel("البيان والشرح المحاسبي:"), BorderLayout.NORTH);
        JTextArea txtNarration = new JTextArea(2, 20);
        narRow.add(new JScrollPane(txtNarration), BorderLayout.CENTER);
        panel.add(narRow);

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
        btnPostInvoice.setFocusPainted(false);
        btnPostInvoice.addActionListener(e -> postInvoiceToDatabase());
        JButton btnClear = new JButton("مسح");
        btnClear.addActionListener(e -> clearForm());
        JButton btnClose = new JButton("إغلاق");
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

    private void addTableRow() {
        int row = tableModel.getRowCount();
        Vector<String> rowData = new Vector<>();
        rowData.add(String.valueOf(row + 1));
        rowData.add("");
        rowData.add("");
        rowData.add("COUNT");
        rowData.add("");
        rowData.add("");
        rowData.add("");
        rowData.add("0.00");
        tableModel.addRow(rowData);
    }

    private void removeSelectedRow() {
        int selectedRow = itemTable.getSelectedRow();
        if (selectedRow >= 0) {
            tableModel.removeRow(selectedRow);
            renumberRows();
            calculateRowTotals();
        }
    }

    private void renumberRows() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(String.valueOf(i + 1), i, 0);
        }
    }

    private void calculateRowTotals() {
        double total = 0.0;
        double totalCogs = 0.0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                double qty = parseDoubleSafe(tableModel.getValueAt(i, 4));
                double gram = parseDoubleSafe(tableModel.getValueAt(i, 5));
                double price = parseDoubleSafe(tableModel.getValueAt(i, 6));
                String unitType = (String) tableModel.getValueAt(i, 3);
                if ("WEIGHT".equals(unitType)) {
                    qty = qty + (gram / 1000.0);
                }
                double totalVal = qty * price;
                tableModel.setValueAt(String.format("%,.2f", totalVal), i, 7);
                total += totalVal;
                totalCogs += qty * price;
            } catch (Exception ignored) {}
        }
        txtBaseAmount.setText(String.format("%,.2f YER", total));
        txtTaxAmount.setText(String.format("%,.2f YER", 0.0));
        txtTotalAmount.setText(String.format("%,.2f YER", total));
        txtTotalCogs.setText(String.format("%,.2f YER", totalCogs));
        double profit = total - totalCogs;
        txtExpectedProfit.setText(String.format("%,.2f YER", profit));
    }

    private void calculateTotals() {
        calculateRowTotals();
    }

    private double parseDoubleSafe(Object value) {
        if (value == null) return 0.0;
        try {
            return Double.parseDouble(value.toString().trim().replace(",", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void postInvoiceToDatabase() {
        try {
            if (tableModel.getRowCount() == 0) {
                throw new IllegalArgumentException("يجب إضافة صنف واحد على الأقل في الجدول.");
            }
            String invoiceCode = txtInvoiceNumber.getText().trim();
            String invoiceDate = txtInvoiceDate.getText().trim();
            String custAcc = txtCustomerAccount.getText().trim();
            String revAcc = txtRevenueAccount.getText().trim();
            String taxAcc = txtTaxAccount.getText().trim();
            String cogsAcc = txtCogsAccount.getText().trim();
            String fgAcc = txtFinishedGoodsAccount.getText().trim();
            double total = parseDoubleSafe(txtTotalAmount.getText());
            String item = tableModel.getValueAt(0, 1).toString();
            String itemName = tableModel.getValueAt(0, 2).toString();
            double unitCost = parseDoubleSafe(tableModel.getValueAt(0, 6));
            boolean success = SalesPostingService.postSale(invoiceCode, invoiceDate,
                    custAcc, revAcc, taxAcc, cogsAcc, fgAcc, item, itemName,
                    1, total, unitCost, false);
            if (!success) throw new IllegalStateException("تعذر اعتماد فاتورة المبيعات.");
            JOptionPane.showMessageDialog(this, "تم اعتماد فاتورة المبيعات وتسجيل حركة الصرف وتحديث المخزون.",
                    "نجاح", JOptionPane.INFORMATION_MESSAGE);
            generateInvoiceNumber();
            calculateRowTotals();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "خطأ في فاتورة المبيعات", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void previewInvoice() {
        try {
            double total = parseDoubleSafe(txtTotalAmount.getText());
            StringBuilder rowsHtml = new StringBuilder();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String code = tableModel.getValueAt(i, 1).toString();
                String name = tableModel.getValueAt(i, 2).toString();
                String unitType = tableModel.getValueAt(i, 3).toString();
                String qty = tableModel.getValueAt(i, 4).toString();
                String gram = tableModel.getValueAt(i, 5).toString();
                String price = tableModel.getValueAt(i, 6).toString();
                String totalVal = tableModel.getValueAt(i, 7).toString();
                rowsHtml.append("<tr><td>").append(i + 1).append("</td><td>").append(code)
                        .append("</td><td>").append(name).append("</td><td>").append(unitType)
                        .append("</td><td>").append(qty).append("</td><td>").append(gram)
                        .append("</td><td>").append(price).append("</td><td>").append(totalVal).append("</td></tr>");
            }
            String html = "<html dir='rtl' lang='ar'><head><meta charset='UTF-8'>"
                    + "<style>body{font-family:Tahoma,sans-serif;font-size:12px;} table{border-collapse:collapse;width:100%;}"
                    + "th,td{border:1px solid #999;padding:6px;text-align:right;} th{background-color:#1f2937;color:white;}"
                    + "tr:nth-child(even){background-color:#f3f4f6;} .total-row{font-weight:bold;background-color:#d1fae5;}</style></head>"
                    + "<body><h2>فاتورة مبيعات</h2>"
                    + "<p>رقم الفاتورة: " + txtInvoiceNumber.getText() + "</p>"
                    + "<p>التاريخ: " + txtInvoiceDate.getText() + "</p>"
                    + "<p>حساب العميل: " + txtCustomerAccount.getText() + "</p>"
                    + "<table><thead><tr><th>م</th><th>كود الصنف</th><th>اسم الصنف</th><th>نوع الوحدة</th><th>الكمية</th><th>الجرام</th><th>سعر الوحدة</th><th>الإجمالي</th></tr></thead>"
                    + "<tbody>" + rowsHtml.toString() + "</tbody>"
                    + "<tfoot><tr class='total-row'><td colspan='7'>الإجمالي الكلي</td><td>" + String.format("%,.2f", total) + "</td></tr></tfoot>"
                    + "</table><p>التوقيع: ____________________</p></body></html>";
            InvoicePrintPreviewDialog preview = new InvoicePrintPreviewDialog(
                    this,
                    txtInvoiceNumber.getText().trim(),
                    "مبيعات مباشرة",
                    txtInvoiceDate.getText().trim(),
                    txtCustomerAccount.getText().trim(),
                    "العميل",
                    tableModel.getValueAt(0, 1).toString(),
                    tableModel.getValueAt(0, 2).toString(),
                    "مبيعات تجارية معتمدة",
                    1,
                    parseDoubleSafe(tableModel.getValueAt(0, 6)),
                    total,
                    0.0,
                    total,
                    "SALES"
            );
            preview.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في فتح شاشة المعاينة: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void browseAccounts(JTextField targetField, String prefix) {
        AccountTreeDialog dialog = new AccountTreeDialog(this, prefix);
        dialog.setVisible(true);
        if (dialog.isAccountSelected()) {
            targetField.setText(dialog.getSelectedAccountCode());
        }
    }

    private JPanel accountField(JTextField field, String prefix) {
        JPanel wrapper = new JPanel(new BorderLayout(5, 0));
        wrapper.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton browse = new JButton("شجرة الحسابات");
        browse.addActionListener(e -> {
            AccountTreeDialog dialog = new AccountTreeDialog(this, prefix);
            dialog.setVisible(true);
            if (dialog.isAccountSelected()) field.setText(dialog.getSelectedAccountCode());
        });
        wrapper.add(field, BorderLayout.CENTER);
        wrapper.add(browse, BorderLayout.EAST);
        return wrapper;
    }

    private void clearForm() {
        generateInvoiceNumber();
        txtCustomerAccount.setText("");
        txtRevenueAccount.setText("");
        txtTaxAccount.setText("220301");
        txtCogsAccount.setText("");
        txtFinishedGoodsAccount.setText("");
        txtBaseAmount.setText("0.00 YER");
        txtTotalAmount.setText("0.00 YER");
        txtTotalCogs.setText("0.00 YER");
        txtExpectedProfit.setText("0.00 YER");
        txtTaxAmount.setText("0.00 YER");
        while (tableModel.getRowCount() > 0) tableModel.removeRow(0);
        calculateRowTotals();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new SalesInvoiceForm().setVisible(true);
        });
    }
}
