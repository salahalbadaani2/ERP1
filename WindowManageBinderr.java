import javax.swing.*;
import java.awt.*;

public final class WindowManageBinderr {
    private WindowManageBinderr() {}

    /**
     * تطبيق النمط الهيكلي القياسي لأي شاشة JFrame بسطر واحد
     * يفعل: التكبير/التصغير، أزرار الويندوز، الإغلاق، التمركز، وتحديث الرسم
     */
    public static void bind(JFrame frame) {
        if (frame == null) return;
        frame.setResizable(true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        try { frame.setUndecorated(false); } catch (Exception ignored) {}
        frame.setLocationRelativeTo(null);
        frame.revalidate();
        frame.repaint();
    }

    public static void bind(JDialog dialog) {
        if (dialog == null) return;
        dialog.setResizable(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        try { dialog.setUndecorated(false); } catch (Exception ignored) {}
        dialog.setLocationRelativeTo(null);
        dialog.revalidate();
        dialog.repaint();
    }

    public static void apply(JFrame frame) {
        bind(frame);
    }

    public static void attach(JFrame frame) {
        bind(frame);
    }
    public static void apply(JDialog dialog) { bind(dialog); }
    public static void attach(JDialog dialog) { bind(dialog); }
}
