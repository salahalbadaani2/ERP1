import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class PurchaseInvoiceForm extends JFrame {
    private final JTextField invoiceNumber = new JTextField();
    private final JTextField invoiceDate = new JTextField(LocalDate.now().toString());
    private final JTextField inventoryAccount = new JTextField("1210101");
    private final JTextField supplierAccount = new JTextField("210101");
    private final JTextField inputTaxAccount = new JTextField("220301");
    private final JTextField itemCode = new JTextField("ITEM-101");
    private final JTextField quantity = new JTextField("1");
    private final JTextField kgField = new JTextField("0");
    private final JTextField gramField = new JTextField("0");
    private final JTextField unitCost = new JTextField("180");
    private final JTextField amount = new JTextField("180");
    private final JCheckBox taxApplied = new JCheckBox("ضريبة مدخلات", false);
    private final JTextField taxRate = new JTextField("0.15");
    private final JComboBox<String> unitTypeCombo = new JComboBox<>(new String[]{"COUNT", "WEIGHT"});

    public PurchaseInvoiceForm() {
        setTitle("نظام ERP المصنعي - فاتورة المشتريات");
        setSize(760, 520);
        setMinimumSize(new Dimension(620, 420));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        invoiceNumber.setText(DocumentNumberService.next("PURCHASE_INVOICE", "PUR-"));
        invoiceNumber.setEditable(false);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));
        addRow(form, "رقم الفاتورة", invoiceNumber);
        addRow(form, "التاريخ", invoiceDate);
        addRow(form, "حساب مخزون المواد الخام", accountField(inventoryAccount, "121"));
        addRow(form, "حساب المورد", accountField(supplierAccount, "21"));
        addRow(form, "حساب ضريبة المدخلات", accountField(inputTaxAccount, "22"));
        addRow(form, "رقم الصنف", itemCode);
        addRow(form, "نوع الصنف", unitTypeCombo);
        JPanel qtyPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        qtyPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        qtyPanel.add(new JLabel("الكمية (كرتون/عدد):"));
        qtyPanel.add(quantity);
        qtyPanel.add(new JLabel("كجم / غرام"));
        JPanel weightPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        weightPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        weightPanel.add(new JLabel("الكيلو:"));
        weightPanel.add(kgField);
        weightPanel.add(new JLabel("الجرام:"));
        weightPanel.add(gramField);
        addRow(form, "الكمية العددية/الكرتون", qtyPanel);
        addRow(form, "الأوزان (للأصناف الوزنية)", weightPanel);
        addRow(form, "تكلفة الوحدة", unitCost);
        addRow(form, "قيمة المشتريات قبل الضريبة", amount);
        addRow(form, "تطبيق الضريبة", taxApplied);
        addRow(form, "نسبة الضريبة", taxRate);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton approve = new JButton("موافق واعتماد");
        JButton view = new JButton("عرض وطباعة");
        JButton clear = new JButton("فاتورة جديدة");
        JButton close = new JButton("إغلاق");
        approve.addActionListener(event -> post());
        view.addActionListener(event -> preview());
        clear.addActionListener(event -> reset());
        close.addActionListener(event -> dispose());
        actions.add(approve); actions.add(view); actions.add(clear); actions.add(close);
        add(new JScrollPane(form), BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    private void addRow(JPanel panel, String label, Component field) {
        panel.add(new JLabel(label + ":"));
        panel.add(field);
    }

    private JPanel accountField(JTextField field, String prefix) {
        JPanel wrapper = new JPanel(new BorderLayout(5, 0));
        JButton browse = new JButton("شجرة الحسابات");
        browse.addActionListener(event -> {
            AccountTreeDialog dialog = new AccountTreeDialog(this, prefix);
            dialog.setVisible(true);
            if (dialog.isAccountSelected()) field.setText(dialog.getSelectedAccountCode());
        });
        wrapper.add(field, BorderLayout.CENTER);
        wrapper.add(browse, BorderLayout.WEST);
        return wrapper;
    }

    private void post() {
        try {
            double base = Double.parseDouble(amount.getText().trim());
            double rate = Double.parseDouble(taxRate.getText().trim());
            String item = itemCode.getText().trim();
            double qty;
            if ("WEIGHT".equals(unitTypeCombo.getSelectedItem())) {
                double kg = Double.parseDouble(kgField.getText().trim());
                double g = Double.parseDouble(gramField.getText().trim());
                qty = kg + (g / 1000.0);
            } else {
                qty = Double.parseDouble(quantity.getText().trim());
            }
            PurchaseInvoice invoice = new PurchaseInvoice(invoiceNumber.getText(), inventoryAccount.getText().trim(),
                    supplierAccount.getText().trim(), inputTaxAccount.getText().trim(), base, taxApplied.isSelected(), rate,
                    item, qty, Double.parseDouble(unitCost.getText().trim()));
            if (!invoice.postToAccounting()) throw new IllegalStateException("تعذر اعتماد فاتورة المشتريات.");
            JOptionPane.showMessageDialog(this, "تم اعتماد الفاتورة وترحيل قيد المخزون والمورد وتحديث رصيده.", "نجاح", JOptionPane.INFORMATION_MESSAGE);
            reset();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "خطأ في فاتورة المشتريات", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void preview() {
        String html = "<h2>فاتورة مشتريات</h2><p>رقم الفاتورة: " + invoiceNumber.getText()
                + "</p><p>التاريخ: " + invoiceDate.getText() + "</p><p>حساب المورد: " + supplierAccount.getText()
                + "</p><p>قيمة المشتريات: " + amount.getText() + "</p><p>التوقيع: ____________________</p>";
        new DocumentPreviewDialog(this, "فاتورة المشتريات", html).setVisible(true);
    }

    private void reset() {
        invoiceNumber.setText(DocumentNumberService.next("PURCHASE_INVOICE", "PUR-"));
        amount.setText("0");
        kgField.setText("0");
        gramField.setText("0");
        quantity.setText("1");
        taxApplied.setSelected(false);
    }

    private String getItemUnitType(String itemCode) {
        String sql = "SELECT unit_type FROM inventory_items WHERE item_code = ?";
        try (Connection connection = DatabaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemCode);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getString(1);
            }
        } catch (SQLException ignored) {}
        return "COUNT";
    }
}
