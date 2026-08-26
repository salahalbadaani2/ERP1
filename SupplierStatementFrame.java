import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SupplierStatementFrame extends JFrame {
    private final JTextField supplierAccount = new JTextField("210101");
    private final JLabel currentBalance = new JLabel("الرصيد الحالي: 0.00 YER");
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] {"التاريخ", "رقم القيد", "المرجع", "البيان", "مدين", "دائن", "الرصيد"}, 0);

    public SupplierStatementFrame() {
        setTitle("نظام ERP المصنعي - كشف حساب المورد");
        setSize(980, 560);
        setMinimumSize(new Dimension(760, 440));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setBorder(BorderFactory.createEmptyBorder(14, 18, 8, 18));
        top.add(new JLabel("حساب المورد:"), BorderLayout.EAST);
        top.add(supplierAccount, BorderLayout.CENTER);
        JButton refresh = new JButton("عرض الكشف");
        refresh.addActionListener(event -> loadStatement());
        top.add(refresh, BorderLayout.WEST);
        add(top, BorderLayout.NORTH);

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(currentBalance);
        JButton close = new JButton("إغلاق");
        close.addActionListener(event -> dispose());
        bottom.add(close);
        add(bottom, BorderLayout.SOUTH);
        loadStatement();
    }

    private void loadStatement() {
        model.setRowCount(0);
        double balance = 0;
        String sql = "SELECT j.entry_date, j.entry_number, j.reference_doc, j.narration, "
                + "l.debit_amount, l.credit_amount FROM journal_entries j "
                + "JOIN journal_entry_lines l ON l.entry_id = j.entry_id "
                + "WHERE l.account_code = ? ORDER BY j.entry_date, j.entry_id, l.line_id";
        try (Connection connection = DatabaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, supplierAccount.getText().trim());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    double debit = result.getDouble("debit_amount");
                    double credit = result.getDouble("credit_amount");
                    balance += credit - debit;
                    model.addRow(new Object[] {result.getDate("entry_date"), result.getString("entry_number"),
                            result.getString("reference_doc"), result.getString("narration"),
                            String.format("%,.2f", debit), String.format("%,.2f", credit),
                            String.format("%,.2f", balance)});
                }
            }
            currentBalance.setText(String.format("الرصيد الحالي: %,.2f YER", balance));
        } catch (Exception exception) {
            currentBalance.setText("تعذر تحميل الكشف: " + exception.getMessage());
        }
    }
}
