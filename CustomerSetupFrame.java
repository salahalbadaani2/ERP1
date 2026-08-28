import javax.swing.*;
import java.awt.*;

public class CustomerSetupFrame extends JFrame {
    public CustomerSetupFrame() {
        setTitle("تهيئة حساب زبون (عميل)");
        setSize(700, 500);
        setMinimumSize(new Dimension(600, 400));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JLabel lbl = new JLabel("تهيئة حساب زبون (عميل) - سكيليتون", SwingConstants.CENTER);
        lbl.setFont(new Font("Tahoma", Font.PLAIN, 14));
        add(lbl, BorderLayout.CENTER);
    }
}
