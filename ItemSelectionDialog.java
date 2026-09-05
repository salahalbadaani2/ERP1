import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * نافذة اختيار صنف من جدول الأصناف المخزني المستقل (inventory_items).
 * كود الصنف هو المرجع الموحد الثابت عبر كافة المخازن ولا يُبحث عنه في chart_of_accounts.
 */
public class ItemSelectionDialog extends JDialog {
    private final JTextField txtSearch = new JTextField();
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> itemList = new JList<>(listModel);
    private final List<String[]> items = new ArrayList<>();
    private String[] selectedItem = null;
    private boolean confirmed = false;

    public ItemSelectionDialog(Window owner) {
        super(owner, "اختيار صنف مخزني", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(8, 8));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setSize(560, 420);
        setMinimumSize(new Dimension(480, 320));
        setLocationRelativeTo(owner);

        JPanel top = new JPanel(new BorderLayout(6, 6));
        top.setBorder(new EmptyBorder(10, 14, 4, 14));
        top.setBackground(new Color(245, 247, 250));
        JLabel lbl = new JLabel("بحث: (باسم الصنف أو الكود)");
        lbl.setFont(new Font("Tahoma", Font.BOLD, 12));
        txtSearch.setFont(new Font("Tahoma", Font.PLAIN, 12));
        txtSearch.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        top.add(lbl, BorderLayout.EAST);
        top.add(txtSearch, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        itemList.setFont(new Font("Tahoma", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(itemList);
        scroll.setBorder(BorderFactory.createTitledBorder("الأصناف المسجلة في جدول الأصناف المخزني"));
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        JButton btnSelect = new JButton("اختيار الصنف");
        btnSelect.setFont(new Font("Tahoma", Font.BOLD, 13));
        btnSelect.addActionListener(e -> chooseItem());
        JButton btnCancel = new JButton("إغلاق");
        btnCancel.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnCancel.addActionListener(e -> { confirmed = false; dispose(); });
        bottom.add(btnSelect);
        bottom.add(btnCancel);
        add(bottom, BorderLayout.SOUTH);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        itemList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) chooseItem();
            }
        });

        loadItems();
        filter();
    }

    private void loadItems() {
        String sql = "SELECT item_code, item_name, unit_type, unit_cost, default_sale_price FROM inventory_items ORDER BY item_code";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(new String[]{
                        rs.getString("item_code"),
                        rs.getString("item_name"),
                        rs.getString("unit_type"),
                        rs.getString("default_sale_price"),
                        rs.getString("unit_cost")
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "تعذر قراءة الأصناف: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filter() {
        listModel.clear();
        String q = txtSearch.getText().trim().toLowerCase();
        for (String[] it : items) {
            if (q.isEmpty() || it[0].toLowerCase().contains(q) || it[1].toLowerCase().contains(q)) {
                listModel.addElement(it[0] + " - " + it[1]
                        + "  [سعر البيع: " + it[3] + " | التكلفة: " + it[4] + "]");
            }
        }
    }

    private void chooseItem() {
        int idx = itemList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "يرجى اختيار صنف من القائمة أولاً.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String display = listModel.get(idx);
        String code = display.split(" - ")[0].trim();
        for (String[] it : items) {
            if (it[0].equals(code)) { selectedItem = it; break; }
        }
        confirmed = true;
        dispose();
    }

    public boolean isItemSelected() { return confirmed && selectedItem != null; }
    public String getSelectedItemCode() { return selectedItem[0]; }
    public String getSelectedItemName() { return selectedItem[1]; }
    public String getSelectedUnitType() { return selectedItem[2]; }
    public double getSelectedSalePrice() { return safeDouble(selectedItem[3]); }
    public double getSelectedUnitCost() { return safeDouble(selectedItem[4]); }

    private static double safeDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }
}