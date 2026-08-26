import javax.swing.*;
import java.awt.*;

public class SalesModuleFrame extends JFrame {
    public SalesModuleFrame() {
        setTitle("نظام ERP المصنعي - المبيعات");
        setSize(520, 260);
        setMinimumSize(new Dimension(420, 220));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel panel = new JPanel(new GridLayout(2, 1, 12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));
        JButton sales = new JButton("فاتورة مبيعات");
        JButton returns = new JButton("فاتورة مردود مبيعات");
        sales.addActionListener(event -> new SalesInvoiceForm().setVisible(true));
        returns.addActionListener(event -> new SalesReturnInvoiceForm().setVisible(true));
        panel.add(sales);
        panel.add(returns);
        add(panel);
    }
}
