import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // تشغيل الواجهة الرئيسية عبر خيط Swing التزمني (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // استخدام المظهر الافتراضي إذا لم يتوفر مظهر النظام.
            }
            new MainWindow().setVisible(true);
        });
    }
}
