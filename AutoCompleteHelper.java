import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * مساعد الإملاء التلقائي وشجرة الحسابات
 * يربط أي JTextField بجدول chart_of_accounts أو inventory_items
 */
public final class AutoCompleteHelper {
    private AutoCompleteHelper() {}

    public static void installAccountAutoComplete(JTextField field, String accountTypeFilter) {
        installAccountAutoComplete(field, accountTypeFilter, false);
    }

    public static void installAccountAutoComplete(JTextField field, String accountTypeFilter, boolean keepName) {
        JPopupMenu popup = new JPopupMenu();
        Timer debounce = new Timer(150, e -> showAccountPopup(field, popup, accountTypeFilter, keepName));
        debounce.setRepeats(false);
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { debounce.restart(); }
            public void removeUpdate(DocumentEvent e) { debounce.restart(); }
            public void changedUpdate(DocumentEvent e) { debounce.restart(); }
        });
        // النقر المزدوج يفتح الشجرة بشكل آمن مع Window
        field.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!field.isEditable() || !field.isEnabled()) return;
                if (e.getClickCount() == 2) {
                    String prefix = guessPrefix(accountTypeFilter);
                    Window owner = SwingUtilities.getWindowAncestor(field);
                    AccountTreeDialog dlg = new AccountTreeDialog(owner, prefix);
                    dlg.setVisible(true);
                    if (dlg.isAccountSelected()) field.setText(extractCode(dlg.getSelectedAccountCode()));
                }
            }
        });
    }

    private static void showAccountPopup(JTextField field, JPopupMenu popup, String accountTypeFilter, boolean keepName) {
        SwingUtilities.invokeLater(() -> {
            if (!field.isEditable() || !field.isEnabled()) { popup.setVisible(false); return; }
            String text = field.getText().trim();
            if (text.length() < 1) { popup.setVisible(false); return; }
            List<String> suggestions = fetchAccountSuggestions(text, accountTypeFilter);
            if (suggestions.isEmpty()) { popup.setVisible(false); return; }
            popup.removeAll();
            for (String s : suggestions) {
                JMenuItem item = new JMenuItem(s);
                item.addActionListener(ev -> { field.setText(keepName ? s : extractCode(s)); popup.setVisible(false); });
                popup.add(item);
            }
            popup.show(field, 0, field.getHeight());
            field.requestFocus();
        });
    }

    public static void installItemAutoComplete(JTextField field) {
        JPopupMenu popup = new JPopupMenu();
        Timer debounce = new Timer(150, e -> showItemPopup(field, popup));
        debounce.setRepeats(false);
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { debounce.restart(); }
            public void removeUpdate(DocumentEvent e) { debounce.restart(); }
            public void changedUpdate(DocumentEvent e) { debounce.restart(); }
        });
        // دعم النقر المزدوج لفتح شجرة/جدول اختيار الأصناف
        field.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Window owner = SwingUtilities.getWindowAncestor(field);
                    AccountTreeDialog dlg = new AccountTreeDialog(owner, "121");
                    dlg.setVisible(true);
                    if (dlg.isAccountSelected()) {
                        String selectedCode = dlg.getSelectedAccountCode();
                        String sql = "SELECT item_code FROM inventory_items WHERE inventory_account=? LIMIT 1";
                        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, extractCode(selectedCode));
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) field.setText(rs.getString(1));
                                else field.setText(extractCode(selectedCode));
                            }
                        } catch (Exception ignored) {
                            field.setText(extractCode(selectedCode));
                        }
                    }
                }
            }
        });
    }

    private static void showItemPopup(JTextField field, JPopupMenu popup) {
        SwingUtilities.invokeLater(() -> {
            String text = field.getText().trim();
            if (text.length() < 1) { popup.setVisible(false); return; }
            List<String> suggestions = fetchItemSuggestions(text);
            if (suggestions.isEmpty()) { popup.setVisible(false); return; }
            popup.removeAll();
            for (String s : suggestions) {
                JMenuItem item = new JMenuItem(s);
                item.addActionListener(ev -> { field.setText(extractCode(s)); popup.setVisible(false); });
                popup.add(item);
            }
            popup.show(field, 0, field.getHeight());
        });
    }

    private static List<String> fetchAccountSuggestions(String text, String typeFilter) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT account_code, account_name FROM chart_of_accounts WHERE is_sub_account=1 AND (account_type = ? OR ? IS NULL OR ? = '') AND (account_code LIKE ? OR account_name LIKE ?) ORDER BY account_code LIMIT 8";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, typeFilter);
            ps.setString(2, typeFilter);
            ps.setString(3, typeFilter);
            ps.setString(4, "%" + text + "%");
            ps.setString(5, "%" + text + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString(1) + " - " + rs.getString(2));
            }
        } catch (Exception ignored) {}
        return list;
    }

    private static List<String> fetchItemSuggestions(String text) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT item_code, item_name FROM inventory_items WHERE item_code LIKE ? OR item_name LIKE ? LIMIT 8";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + text + "%");
            ps.setString(2, "%" + text + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString(1) + " - " + rs.getString(2));
            }
        } catch (Exception ignored) {}
        return list;
    }

    private static String extractCode(String s) { return s.split(" - ")[0].trim(); }
    private static String guessPrefix(String f) {
        if (f == null) return "";
        if (f.contains("REVENUE")) return "41";
        if (f.contains("ASSET")) return "12";
        return "";
    }
}
