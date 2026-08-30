import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** لوحة المخزون الفعلية، وتقرأ بياناتها من جدول inventory_items. */
public class WarehouseInventoryManager extends JFrame {
    private final DefaultTableModel model = new DefaultTableModel(new String[]{
            "كود الصنف", "اسم الصنف", "التصنيف", "الوحدة", "الكمية المتاحة",
            "حد إعادة الطلب", "تكلفة الوحدة", "قيمة المخزون", "الحالة"
    }, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JLabel itemCount = metricLabel();
    private final JLabel inventoryValue = metricLabel();
    private final JLabel lowStockCount = metricLabel();
    private final JLabel status = new JLabel(" ");

    public WarehouseInventoryManager() {
        setTitle("نظام ERP المصنعي - إدارة المخزون");
        setSize(1150, 720);
        setMinimumSize(new Dimension(900, 550));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        buildUi();
        loadInventory();
    }

    private void buildUi() {
        setLayout(new BorderLayout(12, 12));
        getRootPane().setBorder(new EmptyBorder(14, 18, 14, 18));
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(new EmptyBorder(15, 20, 15, 20));
        JLabel title = new JLabel("إدارة المخزون والمستودعات");
        title.setFont(new Font("Segoe UI", Font.BOLD, 19));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("بيانات حية من قاعدة البيانات • متابعة الرصيد والتكلفة وحد إعادة الطلب");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(203, 213, 225));
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.add(metricsPanel(), BorderLayout.NORTH);
        JTable table = new JTable(model);
        configureTable(table);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton refresh = new JButton("تحديث البيانات");
        refresh.addActionListener(e -> loadInventory());
        JButton manageItems = new JButton("إدارة الأصناف");
        manageItems.addActionListener(e -> new ItemManagementForm().setVisible(true));
        actions.add(manageItems);
        actions.add(refresh);
        status.setForeground(new Color(71, 85, 105));
        actions.add(status);
        content.add(actions, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
    }

    private JPanel metricsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 12, 0));
        panel.add(metricCard("عدد الأصناف", itemCount, new Color(2, 132, 199)));
        panel.add(metricCard("القيمة الدفترية للمخزون", inventoryValue, new Color(5, 150, 105)));
        panel.add(metricCard("أصناف تحتاج إعادة طلب", lowStockCount, new Color(217, 119, 6)));
        return panel;
    }

    private JPanel metricCard(String title, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)), new EmptyBorder(10, 14, 10, 14)));
        JLabel label = new JLabel(title);
        label.setForeground(new Color(100, 116, 139));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        value.setForeground(accent);
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JLabel metricLabel() {
        JLabel label = new JLabel("0");
        label.setFont(new Font("Segoe UI", Font.BOLD, 17));
        return label;
    }

    private void configureTable(JTable table) {
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 0; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(renderer);
    }

    private void loadInventory() {
        model.setRowCount(0);
        String sql = "SELECT item_code, item_name, category, unit, current_stock, min_stock_level, unit_cost "
                + "FROM inventory_items ORDER BY item_code";
        int count = 0;
        int lowCount = 0;
        double totalValue = 0;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                double stock = result.getDouble("current_stock");
                double minStock = result.getDouble("min_stock_level");
                double unitCost = result.getDouble("unit_cost");
                double value = stock * unitCost;
                boolean low = stock <= minStock;
                if (low) lowCount++;
                count++;
                totalValue += value;
                model.addRow(new Object[]{
                        result.getString("item_code"), result.getString("item_name"), result.getString("category"),
                        result.getString("unit"), format(stock), format(minStock), formatMoney(unitCost),
                        formatMoney(value), low ? "تنبيه: تحت الحد" : "متوفر"
                });
            }
            status.setText("آخر تحديث: " + java.time.LocalTime.now().withNano(0));
        } catch (Exception exception) {
            status.setText("تعذر تحميل المخزون: " + exception.getMessage());
            JOptionPane.showMessageDialog(this, "تعذر قراءة بيانات المخزون: " + exception.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
        itemCount.setText(count + " صنف");
        inventoryValue.setText(formatMoney(totalValue));
        lowStockCount.setText(lowCount + " صنف");
    }

    private String format(double amount) { return String.format("%,.2f", amount); }
    private String formatMoney(double amount) { return String.format("%,.2f YER", amount); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WarehouseInventoryManager().setVisible(true));
    }
}
