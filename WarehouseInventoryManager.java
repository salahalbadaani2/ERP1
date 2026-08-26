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

    // جداول المخازن الثلاثة
    private JTable tblRawMaterials;
    private DefaultTableModel modelRawMaterials;
    private JLabel lblRawVal, lblRawCount;

    private JTable tblWip;
    private DefaultTableModel modelWip;
    private JLabel lblWipVal, lblWipCount;

    private JTable tblFinishedGoods;
    private DefaultTableModel modelFinishedGoods;
    private JLabel lblFgVal, lblFgCount;

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

        // التبويبات الثلاثة للمخازن
        tabbedPane = new JTabbedPane();
        tabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        tabbedPane.setFont(FONT_HEADER);

        tabbedPane.addTab("1. المنتجات التامة", createFinishedGoodsPanel());
        tabbedPane.addTab("🌾 2. مخزن المواد الخام والتعبئة (Raw Materials)", createRawMaterialsPanel());
        tabbedPane.addTab("3. الإنتاج تحت التشغيل", createWipPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // 1. لوحة مخزن المنتجات التامة
    private JPanel createFinishedGoodsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topKpi = new JPanel(new GridLayout(1, 3, 15, 10));
        topKpi.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        lblFgCount = createKpiCard("عدد الأصناف التامة", "0 صنف", new Color(2, 132, 199), topKpi);
        lblFgVal = createKpiCard("إجمالي القيمة الدفترية للمخزون التام", "0.00 YER", new Color(16, 185, 129), topKpi);
        createKpiCard("الحساب المرتبط في الدليل", "12103 - مخزون الإنتاج التام", new Color(71, 85, 105), topKpi);
        panel.add(topKpi, BorderLayout.NORTH);

        String[] cols = {"كود الصنف", "اسم المنتج التام", "الوحدة", "الرصيد المخزني الفعلي", "حد الطلب الأدنى", "تكلفة الوحدة (COGS)", "إجمالي القيمة (YER)", "الحالة"};
        modelFinishedGoods = new DefaultTableModel(cols, 0);
        tblFinishedGoods = new JTable(modelFinishedGoods);
        setupTable(tblFinishedGoods);

        panel.add(new JScrollPane(tblFinishedGoods), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
                JButton btnRefresh = new JButton("تحديث المخزون");
        btnRefresh.setFont(FONT_HEADER);
        btnRefresh.addActionListener(e -> loadFinishedGoodsData());
        bottom.add(btnRefresh);
        JButton btnReceipt = new JButton("الاستلام المخزني");
        btnReceipt.addActionListener(e -> new WarehouseOperationsFrame(true).setVisible(true));
        JButton btnIssue = new JButton("الصرف المخزني");
        btnIssue.addActionListener(e -> new WarehouseOperationsFrame(false).setVisible(true));
        JButton btnReports = new JButton("تقارير المخزون");
        btnReports.addActionListener(e -> new WarehouseReportsFrame().setVisible(true));
        JButton btnItemSetup = new JButton("تهيئة بطاقة صنف");
        btnItemSetup.addActionListener(e -> new ItemManagementForm().setVisible(true));
        bottom.add(btnReceipt);
        bottom.add(btnIssue);
        bottom.add(btnReports);
        bottom.add(btnItemSetup);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // 2. لوحة مخزن المواد الخام
    private JPanel createRawMaterialsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topKpi = new JPanel(new GridLayout(1, 3, 15, 10));
        topKpi.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        lblRawCount = createKpiCard("عدد أصناف المواد الخام", "0 مادة", new Color(217, 119, 6), topKpi);
        lblRawVal = createKpiCard("إجمالي قيمة المواد الخام المتاحة", "0.00 YER", new Color(13, 148, 136), topKpi);
        createKpiCard("الحساب المرتبط في الدليل", "12101 - مخزون المواد الخام", new Color(71, 85, 105), topKpi);
        panel.add(topKpi, BorderLayout.NORTH);

        String[] cols = {"كود المادة", "اسم المادة الخام / التعبئة", "الوحدة", "الكمية المتوفرة", "حد الأمان الأدنى", "سعر الشراء / التكلفة", "إجمالي القيمة (YER)", "حالة التوفر"};
        modelRawMaterials = new DefaultTableModel(cols, 0);
        tblRawMaterials = new JTable(modelRawMaterials);
        setupTable(tblRawMaterials);

        panel.add(new JScrollPane(tblRawMaterials), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
                JButton btnRefresh = new JButton("تحديث المخزون");
        btnRefresh.setFont(FONT_HEADER);
        btnRefresh.addActionListener(e -> loadRawMaterialsData());
        bottom.add(btnRefresh);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // 3. لوحة الإنتاج تحت التشغيل WIP
    private JPanel createWipPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topKpi = new JPanel(new GridLayout(1, 3, 15, 10));
        topKpi.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        lblWipCount = createKpiCard("أوامر الإنتاج قيد التشغيل", "0 أمر تصنيع", new Color(124, 58, 237), topKpi);
        lblWipVal = createKpiCard("تكلفة الإنتاج المحملة (WIP)", "0.00 YER", new Color(225, 29, 72), topKpi);
        createKpiCard("الحساب المرتبط في الدليل", "12102 - الإنتاج تحت التشغيل", new Color(71, 85, 105), topKpi);
        panel.add(topKpi, BorderLayout.NORTH);

        String[] cols = {"رقم أمر الإنتاج", "اسم الخلطة / مرحلة التشغيل", "الكمية قيد التصنيع", "المواد المحملة", "الأجور والتكاليف غير المباشرة", "إجمالي تكلفة المرحلة", "حالة التشغيل"};
        modelWip = new DefaultTableModel(cols, 0);
        tblWip = new JTable(modelWip);
        setupTable(tblWip);

        panel.add(new JScrollPane(tblWip), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
                JButton btnRefresh = new JButton("تحديث الإنتاج");
        btnRefresh.setFont(FONT_HEADER);
        btnRefresh.addActionListener(e -> loadWipData());
        bottom.add(btnRefresh);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
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
        loadFinishedGoodsData();
        loadRawMaterialsData();
        loadWipData();
    }

    private void loadFinishedGoodsData() {
        modelFinishedGoods.setRowCount(0);
        double totalVal = 0.0;
        int count = 0;

        // أصناف المنتجات التامة المصنعة الفعلية
        Object[][] sampleItems = {
                        {"FG-101", "عصير مانجو طبيعي 1 لتر", "كرتون (12 حبة)", 450.0, 50.0, 3200.0, 1440000.0, "رصيد كاف"},
                        {"FG-102", "مياه صحية نقية 500 مل", "بالتة (24 شد)", 1200.0, 100.0, 1800.0, 2160000.0, "رصيد كاف"},
                {"FG-103", "بسكويت شاي فاخر", "كرتون (48 باكت)", 25.0, 80.0, 4500.0, 112500.0, "تنبيه: تحت حد الطلب"}
        };

        for (Object[] row : sampleItems) {
            count++;
            totalVal += (double) row[6];
            modelFinishedGoods.addRow(row);
        }

        lblFgCount.setText(count + " أصناف معتمدة");
        lblFgVal.setText(String.format("%,.2f YER", totalVal));
    }

    private void loadRawMaterialsData() {
        modelRawMaterials.setRowCount(0);
        double totalVal = 0.0;
        int count = 0;

        Object[][] rawItems = {
                        {"RM-001", "سكر أبيض نقي مطحون", "شوال (50 كجم)", 300.0, 50.0, 22000.0, 6600000.0, "متوفر للإنتاج"},
                        {"RM-002", "مركز نكهة المانجو الطبيعية", "برميل (200 لتر)", 8.0, 2.0, 180000.0, 1440000.0, "متوفر للإنتاج"},
                        {"RM-003", "عبوات بلاستيكية فارغة 1 لتر", "بالتة (1000 عبوة)", 15.0, 5.0, 45000.0, 675000.0, "متوفر للإنتاج"}
        };

        for (Object[] row : rawItems) {
            count++;
            totalVal += (double) row[6];
            modelRawMaterials.addRow(row);
        }

        lblRawCount.setText(count + " مادة خام");
        lblRawVal.setText(String.format("%,.2f YER", totalVal));
    }

    private void loadWipData() {
        modelWip.setRowCount(0);
        double totalVal = 0.0;
        int count = 0;

        Object[][] wipItems = {
                {"WO-2026-01", "خلطة عصير مانجو - مرحلة البسترة والتجهيز", "3,000 لتر", "750,000 YER", "180,000 YER", 930000.0, "جاري التشغيل"},
                {"WO-2026-02", "عجينة البسكويت - مرحلة التشكيل والخبز", "500 كجم", "320,000 YER", "95,000 YER", 415000.0, "جاري التشغيل"}
        };

        for (Object[] row : wipItems) {
            count++;
            totalVal += (double) row[5];
            modelWip.addRow(new Object[]{
                    row[0], row[1], row[2], row[3], row[4],
                    String.format("%,.2f YER", (double) row[5]),
                    row[6]
            });
        }

        lblWipCount.setText(count + " أوامر تشغيل");
        lblWipVal.setText(String.format("%,.2f YER", totalVal));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new WarehouseInventoryManager().setVisible(true);
        });
    }
}