import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // تشغيل الواجهة عبر خيط Swing التزمني (EDT)
        SwingUtilities.invokeLater(() -> {
            // إنشاء نافذة رئيسية مخفية كأب للـ Dialog
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // استدعاء وعرض شجرة الحسابات
            AccountTreeDialog dialog = new AccountTreeDialog(frame);
            dialog.setVisible(true);

            // طباعة نتيجة الاختيار في Terminal للتحقق
            if (dialog.isAccountSelected()) {
                System.out.println("تم اختيار الحساب: " + dialog.getSelectedAccountResult());
            } else {
                System.out.println("تم إغلاق النافذة بدون تحديد حساب.");
            }

            System.exit(0);
        });
    }
}