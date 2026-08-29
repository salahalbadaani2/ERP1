import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class AccountTreeBinder {
    private static volatile boolean isDialogOpen = false;
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
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
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
        openDialog(txtAccountName, null, txtAccountCode, parentWindow, onSelectCallback);
    }

    public static void attach(JTextField txtAccountName, String filterCode, JTextField txtAccountCode, Window parentWindow, Runnable onSelectCallback) {
        if (txtAccountName == null || txtAccountCode == null) return;
        txtAccountCode.setEditable(true);
        txtAccountCode.setBackground(Color.WHITE);
        txtAccountName.setEditable(false);
        txtAccountName.setBackground(new Color(245,245,245));
        txtAccountName.setCursor(new Cursor(Cursor.HAND_CURSOR));
        txtAccountName.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    openDialog(txtAccountName, filterCode, txtAccountCode, parentWindow, onSelectCallback);
                }
            }
        });
        txtAccountName.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    openDialog(txtAccountName, filterCode, txtAccountCode, parentWindow, onSelectCallback);
                }
            }
        });
    }

    public static void attach(JTextField txtAccountName, int filterCode, JTextField txtAccountCode, Window parentWindow, Runnable onSelectCallback) {
        attach(txtAccountName, String.valueOf(filterCode), txtAccountCode, parentWindow, onSelectCallback);
    }

    private static void openDialog(JTextField txtAccountName, String filterCode, JTextField txtAccountCode, Window parentWindow, Runnable onSelectCallback) {
        if (isDialogOpen) return;
        isDialogOpen = true;
        SwingUtilities.invokeLater(() -> {
            AccountTreeDialog dlg = filterCode == null ? new AccountTreeDialog(parentWindow) : new AccountTreeDialog(parentWindow, filterCode);
            dlg.setAlwaysOnTop(true);
            dlg.addWindowListener(new WindowAdapter() {
                @Override public void windowClosed(WindowEvent e) {
                    isDialogOpen = false;
                    if (dlg.isAccountSelected()) {
                        txtAccountCode.setText(dlg.getSelectedAccountCode());
                        txtAccountName.setText(dlg.getSelectedAccountName());
                        if (onSelectCallback != null) onSelectCallback.run();
                    }
                    if (parentWindow != null) {
                        SwingUtilities.invokeLater(() -> {
                            parentWindow.toFront();
                            parentWindow.requestFocus();
                        });
                    }
                }
                @Override public void windowClosing(WindowEvent e) {
                    isDialogOpen = false;
                }
            });
            dlg.setVisible(true);
            dlg.toFront();
            dlg.requestFocus();
        });
    }
}
