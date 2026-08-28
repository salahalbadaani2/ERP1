import javax.swing.*;
import java.awt.*;

public class BankReconciliationFrame extends JFrame {
    public BankReconciliationFrame() {
        setTitle("بيان المطابقة البنكية");
        setSize(700, 500);
        setMinimumSize(new Dimension(600, 400));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JLabel lbl = new JLabel("بيان المطابقة البنكية - سكيليتون", SwingConstants.CENTER);
        lbl.setFont(new Font("Tahoma", Font.PLAIN, 14));
        add(lbl, BorderLayout.CENTER);
    }
}
