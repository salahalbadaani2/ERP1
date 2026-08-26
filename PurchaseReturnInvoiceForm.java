import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class PurchaseReturnInvoiceForm extends JFrame {
    private final JTextField returnNumber = new JTextField();
    private final JTextField returnDate = new JTextField(LocalDate.now().toString());
    private final JTextField inventoryAccount = new JTextField("1210101");
    private final JTextField supplierAccount = new JTextField("210101");
    private final JTextField inputTaxAccount = new JTextField("220301");
    private final JTextField itemCode = new JTextField("ITEM-101");
    private final JTextField quantity = new JTextField("1");
    private final JTextField unitCost = new JTextField("180");
    private final JTextField amount = new JTextField("180");
    private final JCheckBox taxApplied = new JCheckBox("عكس ضريبة المدخلات", false);
    private final JTextField taxRate = new JTextField("0.15");

    public PurchaseReturnInvoiceForm() {
        setTitle("نظام ERP المصنعي - مردود المشتريات");
        setSize(760, 520);
        setMinimumSize(new Dimension(620, 420));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        returnNumber.setText(DocumentNumberService.next("PURCHASE_RETURN", "PRI-"));
        returnNumber.setEditable(false);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));
        addRow(form, "رقم المرتجع", returnNumber);
        addRow(form, "التاريخ", returnDate);
        addRow(form, "حساب مخزون المواد الخام", accountField(inventoryAccount, "121"));
        addRow(form, "حساب المورد", accountField(supplierAccount, "21"));
        addRow(form, "حساب ضريبة المدخلات", accountField(inputTaxAccount, "22"));
        addRow(form, "رقم الصنف", itemCode);
        addRow(form, "الكمية", quantity);
        addRow(form, "تكلفة الوحدة", unitCost);
        addRow(form, "قيمة المرتجع قبل الضريبة", amount);
        addRow(form, "عكس الضريبة", taxApplied);
        addRow(form, "نسبة الضريبة", taxRate);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton approve = new JButton("موافق واعتماد");
        JButton view = new JButton("عرض وطباعة");
        JButton clear = new JButton("مستند جديد");
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
            PurchaseReturnInvoice invoice = new PurchaseReturnInvoice(returnNumber.getText(), supplierAccount.getText().trim(),
                    inventoryAccount.getText().trim(), inputTaxAccount.getText().trim(), base, taxApplied.isSelected(), rate,
                    itemCode.getText().trim(), Double.parseDouble(quantity.getText().trim()), Double.parseDouble(unitCost.getText().trim()));
            if (!invoice.postToAccounting()) throw new IllegalStateException("تعذر اعتماد مردود المشتريات.");
            JOptionPane.showMessageDialog(this, "تم اعتماد المرتجع وتخفيض رصيد المورد والمخزون.", "نجاح", JOptionPane.INFORMATION_MESSAGE);
            reset();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "خطأ في مردود المشتريات", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void preview() {
        String html = "<h2>مردود مشتريات</h2><p>رقم المستند: " + returnNumber.getText()
                + "</p><p>التاريخ: " + returnDate.getText() + "</p><p>حساب المورد: " + supplierAccount.getText()
                + "</p><p>قيمة المرتجع: " + amount.getText() + "</p><p>التوقيع: ____________________</p>";
        new DocumentPreviewDialog(this, "مردود المشتريات", html).setVisible(true);
    }

    private void reset() {
        returnNumber.setText(DocumentNumberService.next("PURCHASE_RETURN", "PRI-"));
        amount.setText("0");
        taxApplied.setSelected(false);
    }
}
