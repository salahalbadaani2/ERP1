import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VanWarehouseForm extends JFrame {
    private final JComboBox<String> movementType = new JComboBox<>(new String[]{"تحويل إلى مخزن المندوب", "إرجاع من مخزن المندوب"});
    private final JTextField movementCode = new JTextField();
    private final JTextField finishedGoodsAccount = new JTextField("1210301");
    private final JTextField vanAccount = new JTextField("1210401");
    private final JTextField quantity = new JTextField("0");
    private final JTextField kgField = new JTextField("0");
    private final JTextField gramField = new JTextField("0");
    private final JComboBox<String> unitTypeCombo = new JComboBox<>(new String[]{"COUNT", "WEIGHT"});
    private final JTextField unitCost = new JTextField("0");

    public VanWarehouseForm() {
        setTitle("نظام ERP المصنعي - مخازن المندوبين");
        setSize(720, 430);
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        initUI();
        movementType.addActionListener(e -> updateCodePrefix());
        updateCodePrefix();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));
        JLabel header = new JLabel("إدارة تحويلات ومرتجعات مخازن المندوبين");
        header.setFont(new Font("Tahoma", Font.BOLD, 18));
        root.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(6, 3, 8, 8));
        addRow(form, "نوع الحركة", movementType, null);
        addRow(form, "رقم الحركة", movementCode, null);
        addRow(form, "حساب مخزن المنتجات التامة", finishedGoodsAccount, () -> browse(finishedGoodsAccount, "121"));
        addRow(form, "حساب مخزن المندوب", vanAccount, () -> browse(vanAccount, "12104"));
        addRow(form, "الكمية", quantity, null);
        addRow(form, "تكلفة الوحدة", unitCost, null);
        root.add(form, BorderLayout.CENTER);

        JButton post = new JButton("ترحيل الحركة والقيد");
        post.setBackground(new Color(16, 185, 129));
        post.setForeground(Color.WHITE);
        post.addActionListener(e -> postMovement());
        JButton close = new JButton("إغلاق");
        close.addActionListener(e -> dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(post);
        actions.add(close);
        root.add(actions, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void addRow(JPanel panel, String label, JComponent field, Runnable browseAction) {
        panel.add(new JLabel(label + ":"));
        panel.add(field);
        if (browseAction == null) {
            panel.add(new JLabel());
        } else {
            JButton browse = new JButton("شجرة الحسابات");
            browse.addActionListener(e -> browseAction.run());
            panel.add(browse);
        }
    }

    private void browse(JTextField field, String prefix) {
        AccountTreeDialog dialog = new AccountTreeDialog(this, prefix);
        dialog.setVisible(true);
        if (dialog.isAccountSelected()) field.setText(dialog.getSelectedAccountCode());
    }

    private void updateCodePrefix() {
        boolean transfer = movementType.getSelectedIndex() == 0;
        movementCode.setText(DocumentNumberService.next(
            transfer ? "VAN_TRANSFER" : "VAN_RETURN", transfer ? "VTN-" : "VRN-"));
    }

    private void postMovement() {
        try {
            double movementQuantity = Double.parseDouble(quantity.getText().trim());
            double movementUnitCost = Double.parseDouble(unitCost.getText().trim());
            if (movementQuantity <= 0 || movementUnitCost <= 0) throw new IllegalArgumentException("الكمية والتكلفة يجب أن تكونا موجبتين");

            boolean transfer = movementType.getSelectedIndex() == 0;
            boolean success = transfer
                    ? new VanTransferNote(movementCode.getText().trim(), finishedGoodsAccount.getText().trim(), vanAccount.getText().trim(), movementQuantity, movementUnitCost).postToDatabase()
                    : new VanReturnNote(movementCode.getText().trim(), vanAccount.getText().trim(), finishedGoodsAccount.getText().trim(), movementQuantity, movementUnitCost).postToDatabase();
            if (!success) throw new IllegalStateException("تعذر ترحيل القيد المحاسبي");
            JOptionPane.showMessageDialog(this, "تم ترحيل حركة مخزن المندوب والقيد المزدوج بنجاح.", "نجاح", JOptionPane.INFORMATION_MESSAGE);
            updateCodePrefix();
            quantity.setText("0");
            unitCost.setText("0");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "خطأ في حركة مخزن المندوب", JOptionPane.ERROR_MESSAGE);
        }
    }
}
