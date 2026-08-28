import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * ============================================================================
 * نظام ERP المصنعي - إدارة المخازن والمستودعات الثلاثة (Warehouse Inventory)
 * ============================================================================
 * 1. مخزن المواد الخام ومواد التعبئة (Raw Materials - 12101)
 * 2. مخزن الإنتاج تحت التشغيل (Work In Progress - 12102)
 * 3. مخزن المنتجات التامة الصنع (Finished Goods - 12103)
 */
public class WarehouseInventoryManager extends JFrame {

    private JTabbedPane tabbedPane;

    private static final Font FONT_TITLE = new Font("Tahoma", Font.BOLD, 15);
    private static final Font FONT_HEADER = new Font("Tahoma", Font.BOLD, 12);
    private static final Font FONT_PLAIN = new Font("Tahoma", Font.PLAIN, 12);

    public WarehouseInventoryManager() {
        setTitle("نظام ERP المصنعي - إدارة المخزون");
        setSize(1150, 720);
        setMinimumSize(new Dimension(900, 550));
        setResizable(true); // تفعيل التكبير والتصغير التلقائي
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        initUI();
        loadAllWarehousesData();
    }

    private void initUI() {
        // شريط العنوان العلوي
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42)); // Slate 900
        header.setBorder(new EmptyBorder(15, 22, 15, 22));

        JLabel lblTitle = new JLabel("إدارة المخزون والمستودعات");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("مخزن المواد الخام (12101) • مخزن الإنتاج قيد التشغيل WIP (12102) • مخزن المنتجات التامة (12103)");
        lblSub.setFont(FONT_PLAIN);
        lblSub.setForeground(new Color(203, 213, 225));

        header.add(lblTitle, BorderLayout.NORTH);
        header.add(lblSub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // واجهة فارغة تماماً بعد نقل جميع التقارير إلى التقارير والقوائم المالية
        tabbedPane = new JTabbedPane();
        tabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        tabbedPane.setFont(FONT_HEADER);
        // لا توجد تبويبات - تم نقلها جميعاً
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JLabel createKpiCard(String title, String initialValue, Color color, JPanel parent) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(10, 15, 10, 15)
        ));

        JPanel bar = new JPanel();
        bar.setPreferredSize(new Dimension(4, 0));
        bar.setBackground(color);
        card.add(bar, BorderLayout.EAST);

        JPanel content = new JPanel(new GridLayout(2, 1));
        content.setOpaque(false);

        JLabel lblT = new JLabel(title);
        lblT.setFont(FONT_PLAIN);
        lblT.setForeground(new Color(100, 116, 139));

        JLabel lblV = new JLabel(initialValue);
        lblV.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblV.setForeground(color);

        content.add(lblT);
        content.add(lblV);
        card.add(content, BorderLayout.CENTER);

        parent.add(card);
        return lblV;
    }

    private void setupTable(JTable table) {
        table.setFont(FONT_PLAIN);
        table.getTableHeader().setFont(FONT_HEADER);
        table.setRowHeight(26);
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }
    }

    private void loadAllWarehousesData() {
        // فارغ - تم نقل جميع بيانات المخزون إلى التقارير
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new WarehouseInventoryManager().setVisible(true);
        });
    }
}