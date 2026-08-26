import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class WarehouseOperationsFrame extends JFrame {
    private final boolean receiptMode;
    private final JTextField documentNumber = new JTextField();

    public WarehouseOperationsFrame() {
        this(true);
    }

    public WarehouseOperationsFrame(boolean receiptMode) {
        this.receiptMode = receiptMode;
        setTitle("نظام ERP المصنعي - العمليات المخزنية والتقارير");
        setSize(900, 620);
        setMinimumSize(new Dimension(700, 500));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        add(movementPanel(receiptMode));
    }

    private JPanel movementPanel(boolean receipt) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 24, 12, 24));
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        JTextField number = documentNumber;
        number.setText(DocumentNumberService.next(receipt ? "WAREHOUSE_RECEIPT" : "WAREHOUSE_ISSUE", receipt ? "WR-" : "WI-"));
        number.setEditable(false);
        JTextField date = new JTextField(LocalDate.now().toString());
        JTextField itemCode = new JTextField();
        JTextField itemName = new JTextField();
        JTextField quantity = new JTextField("0");
        JTextField receiver = new JTextField();
        JTextField deliverer = new JTextField();
        JTextField narration = new JTextField();
        JTextField inventoryAccount = new JTextField("1210301");
        form.add(new JLabel("رقم المستند:")); form.add(number);
        form.add(new JLabel("التاريخ:")); form.add(date);
        form.add(new JLabel("رقم الصنف:")); form.add(itemCode);
        form.add(new JLabel("اسم الصنف:")); form.add(itemName);
        form.add(new JLabel("الكمية:")); form.add(quantity);
        form.add(new JLabel("اسم المستلم:")); form.add(receiver);
        form.add(new JLabel("اسم المسلم:")); form.add(deliverer);
        form.add(new JLabel("البيان:")); form.add(narration);
        form.add(new JLabel("حساب المخزون:")); form.add(inventoryAccount);
        panel.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton approve = new JButton("موافق");
        JButton view = new JButton("عرض وطباعة");
        JButton close = new JButton("إغلاق");
        approve.addActionListener(event -> {
            try {
                double qty = Double.parseDouble(quantity.getText().trim());
                if (qty <= 0 || itemCode.getText().trim().isEmpty() || itemName.getText().trim().isEmpty()) {
                    throw new IllegalArgumentException("رقم الصنف واسمه والكمية الموجبة مطلوبة.");
                }
                boolean success = receipt
                        ? InventoryPostingService.receive(number.getText(), itemCode.getText().trim(), itemName.getText().trim(), qty, inventoryAccount.getText().trim(), "210101", receiver.getText().trim(), deliverer.getText().trim(), narration.getText().trim())
                        : InventoryPostingService.issue(number.getText(), itemCode.getText().trim(), itemName.getText().trim(), qty, inventoryAccount.getText().trim(), "1210201", receiver.getText().trim(), deliverer.getText().trim(), narration.getText().trim());
                if (!success) throw new IllegalStateException("تعذر اعتماد الحركة.");
                JOptionPane.showMessageDialog(this, "تم اعتماد الحركة المخزنية والقيد المحاسبي.", "نجاح", JOptionPane.INFORMATION_MESSAGE);
                number.setText(DocumentNumberService.next(receipt ? "WAREHOUSE_RECEIPT" : "WAREHOUSE_ISSUE", receipt ? "WR-" : "WI-"));
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(this, exception.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
            }
        });
        view.addActionListener(event -> new DocumentPreviewDialog(this, receipt ? "الاستلام المخزني" : "الصرف المخزني",
            "<h2>" + (receipt ? "الاستلام المخزني" : "الصرف المخزني") + "</h2>"
                + "<p>رقم المستند: " + number.getText() + "</p>"
                + "<p>التاريخ: " + date.getText() + "</p>"
                + "<p>الصنف: " + itemName.getText() + " (" + itemCode.getText() + ")</p>"
                + "<p>الكمية: " + quantity.getText() + "</p>"
                + "<p>اسم المستلم: " + receiver.getText() + "</p>"
                + "<p>اسم المسلم: " + deliverer.getText() + "</p>"
                + "<p>التوقيع: ____________________</p>").setVisible(true));
        close.addActionListener(event -> dispose());
        actions.add(approve); actions.add(view); actions.add(close);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

}
