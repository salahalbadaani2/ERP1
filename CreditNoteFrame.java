import javax.swing.*;
import java.awt.*;

public class CreditNoteFrame extends JFrame {
    public CreditNoteFrame() {
        setTitle("إشعار دائن (Credit Note)");
        setSize(700, 500);
        setMinimumSize(new Dimension(600, 400));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JLabel lbl = new JLabel("إشعار دائن (Credit Note) - سكيليتون", SwingConstants.CENTER);
        lbl.setFont(new Font("Tahoma", Font.PLAIN, 14));
        add(lbl, BorderLayout.CENTER);
    }
}
