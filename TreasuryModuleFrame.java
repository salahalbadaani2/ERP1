import javax.swing.*;
import java.awt.*;

public class TreasuryModuleFrame extends JFrame {
    public TreasuryModuleFrame() {
        setTitle("نظام ERP المصنعي - الخزينة");
        setSize(520, 260);
        setMinimumSize(new Dimension(420, 220));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel panel = new JPanel(new GridLayout(2, 1, 12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));
        JButton receipt = new JButton("سند قبض");
        JButton payment = new JButton("سند صرف");
        receipt.addActionListener(event -> new TreasuryVoucherForm(true).setVisible(true));
        payment.addActionListener(event -> new TreasuryVoucherForm(false).setVisible(true));
        panel.add(receipt);
        panel.add(payment);
        add(panel);
    }
}
