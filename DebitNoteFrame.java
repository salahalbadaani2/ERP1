import javax.swing.*;
import java.awt.*;

public class DebitNoteFrame extends JFrame {
    public DebitNoteFrame() {
        setTitle("إشعار مدين (Debit Note)");
        setSize(700, 500);
        setMinimumSize(new Dimension(600, 400));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JLabel lbl = new JLabel("إشعار مدين (Debit Note) - سكيليتون", SwingConstants.CENTER);
        lbl.setFont(new Font("Tahoma", Font.PLAIN, 14));
        add(lbl, BorderLayout.CENTER);
    }
}
