import javax.swing.*;
import java.awt.*;

public class PurchasesModuleFrame extends JFrame {
    public PurchasesModuleFrame() {
        setTitle("نظام ERP المصنعي - المشتريات");
        setSize(520, 220);
        setMinimumSize(new Dimension(420, 190));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel panel = new JPanel(new GridLayout(3, 1, 12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));
        JButton invoice = new JButton("فتح فاتورة مشتريات");
        JButton returns = new JButton("فتح مردود مشتريات");
        JButton statement = new JButton("كشف حساب المورد والمستحقات");
        invoice.addActionListener(event -> new PurchaseInvoiceForm().setVisible(true));
        returns.addActionListener(event -> new PurchaseReturnInvoiceForm().setVisible(true));
        statement.addActionListener(event -> new SupplierStatementFrame().setVisible(true));
        panel.add(invoice);
        panel.add(returns);
        panel.add(statement);
        add(panel);
    }
}
