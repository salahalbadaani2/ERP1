import javax.swing.*;
import java.awt.*;

public class JournalEntryFrame extends JFrame {
    public JournalEntryFrame() {
        setTitle("قيد يومية (جزئي ومركب)");
        setSize(700, 500);
        setMinimumSize(new Dimension(600, 400));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JLabel lbl = new JLabel("قيد يومية (جزئي ومركب) - سكيليتون", SwingConstants.CENTER);
        lbl.setFont(new Font("Tahoma", Font.PLAIN, 14));
        add(lbl, BorderLayout.CENTER);
    }
}
