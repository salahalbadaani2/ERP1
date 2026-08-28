import javax.swing.*;
import java.awt.*;

/**
 * واجهة الإعدادات والتهيئة - فارغة بنفس نمط باقي الواجهات
 */
public class SettingsFrame extends JFrame {

    public SettingsFrame() {
        setTitle("نظام ERP المصنعي - الإعدادات والتهيئة");
        setSize(520, 320);
        setMinimumSize(new Dimension(420, 280));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(248, 250, 252));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        // فارغ تماماً - بدون أي أزرار أو محتوى حسب الطلب
        add(panel, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SettingsFrame().setVisible(true));
    }
}
