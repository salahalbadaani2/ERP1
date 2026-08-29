import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class AccountTreeBinder {
    private AccountTreeBinder() {}

    public static void attach(JTextField txtAccountName, JTextField txtAccountCode, Window parentWindow, Runnable onSelectCallback) {
        if (txtAccountName == null || txtAccountCode == null) return;
        txtAccountCode.setEditable(true);
        txtAccountCode.setBackground(Color.WHITE);
        txtAccountName.setEditable(false);
        txtAccountName.setBackground(new Color(245,245,245));
        txtAccountName.setCursor(new Cursor(Cursor.HAND_CURSOR));

        txtAccountName.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
                    openDialog(txtAccountName, txtAccountCode, parentWindow, onSelectCallback);
                }
            }
        });
        txtAccountName.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    openDialog(txtAccountName, txtAccountCode, parentWindow, onSelectCallback);
                }
            }
        });
    }

    private static void openDialog(JTextField txtAccountName, JTextField txtAccountCode, Window parentWindow, Runnable onSelectCallback) {
        AccountTreeDialog dlg = new AccountTreeDialog(parentWindow);
        dlg.setAlwaysOnTop(true);
        dlg.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                if (dlg.isAccountSelected()) {
                    txtAccountCode.setText(dlg.getSelectedAccountCode());
                    txtAccountName.setText(dlg.getSelectedAccountName());
                    if (onSelectCallback != null) onSelectCallback.run();
                }
                // إعادة التركيز للنافذة الأم بعد الإغلاق
                if (parentWindow != null) {
                    parentWindow.toFront();
                    parentWindow.requestFocus();
                }
            }
        });
        dlg.setVisible(true);
        dlg.toFront();
        dlg.requestFocus();
    }
}
