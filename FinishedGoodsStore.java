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
 * نظام ERP المصنعي - شاشة إدارة مخازن المنتجات التامة (FinishedGoodsStore)
 * ============================================================================
 */
public class FinishedGoodsStore extends JFrame {

    private JTable tblStore;
    private DefaultTableModel modelStore;
    private JLabel lblTotalStockValue, lblTotalFinishedItems;

    public FinishedGoodsStore() {
        setTitle("نظام ERP المصنعي - إدارة مخازن المنتجات التامة الصنع");
        setSize(1050, 620);
        setMinimumSize(new Dimension(850, 500));
        setResizable(true); // تفعيل التكبير والتصغير
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        initUI();
        loadStoreData();
    }

    private void initUI() {
        // شريط العنوان العلوي
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(217, 119, 6)); // Amber 600
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("مخزون المنتجات التامة");
        title.setFont(new Font("Tahoma", Font.BOLD, 15));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("متابعة الأرصدة المخزنية، استلام المنتجات التامة، ومراقبة تكاليف المخزون");
        sub.setFont(new Font("Tahoma", Font.PLAIN, 12));
        sub.setForeground(new Color(254, 243, 199));

        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // جدول المخزون التام
        String[] cols = {"كود الحساب / الصنف", "اسم المنتج التام / المستودع", "الوحدة", "الرصيد المتاح", "تكلفة الوحدة (COGS)", "إجمالي القيمة الدفترية", "حالة المخزون"};
        modelStore = new DefaultTableModel(cols, 0);
        tblStore = new JTable(modelStore);
        tblStore.setRowHeight(26);
        tblStore.setFont(new Font("Tahoma", Font.PLAIN, 12));
        tblStore.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 12));
        tblStore.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 0; i < tblStore.getColumnCount(); i++) {
            tblStore.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }

        JScrollPane sp = new JScrollPane(tblStore);
        sp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "سجل أرصدة المنتجات التامة الصنع في المستودعات",
                TitledBorder.RIGHT, TitledBorder.TOP,
                new Font("Tahoma", Font.BOLD, 12)
        ));
        add(sp, BorderLayout.CENTER);

        // شريط الحالة والإجماليات السفلي
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 25, 10));
        bottom.setBackground(new Color(248, 250, 252));
        bottom.setBorder(new EmptyBorder(5, 15, 5, 15));

        lblTotalFinishedItems = new JLabel("عدد المستودعات / الأصناف: 0");
        lblTotalFinishedItems.setFont(new Font("Tahoma", Font.BOLD, 12));

        lblTotalStockValue = new JLabel("إجمالي قيمة المخزون التام: 0.00 YER");
        lblTotalStockValue.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblTotalStockValue.setForeground(new Color(217, 119, 6));

        JButton btnRefresh = new JButton("تحديث المخزون");
        btnRefresh.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnRefresh.addActionListener(e -> loadStoreData());

        bottom.add(lblTotalFinishedItems);
        bottom.add(lblTotalStockValue);
        bottom.add(btnRefresh);
        add(bottom, BorderLayout.SOUTH);
    }

    private void loadStoreData() {
        modelStore.setRowCount(0);
        double totalVal = 0.0;
        int count = 0;

        // قراءة حسابات مخزون المنتجات التامة من دليل الحسابات
        String sql = "SELECT account_code, account_name, current_balance FROM chart_of_accounts WHERE account_code LIKE '121%' AND is_sub_account = 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                count++;
                String code = rs.getString("account_code");
                String name = rs.getString("account_name");
                double bal = rs.getDouble("current_balance");
                totalVal += bal;

                modelStore.addRow(new Object[]{
                        code,
                        name,
                        "كرتون / عبوة",
                        "1,000.00",
                        String.format("%,.2f YER", bal > 0 ? (bal / 1000.0) : 350.0),
                        String.format("%,.2f YER", bal),
                        "رصيد متاح"
                });
            }
        } catch (Exception ex) {
            System.err.println("خطأ جلب بيانات المخزن: " + ex.getMessage());
        }

        lblTotalFinishedItems.setText("عدد المستودعات / الأصناف: " + count);
        lblTotalStockValue.setText(String.format("إجمالي قيمة المخزون التام: %,.2f YER", totalVal));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new FinishedGoodsStore().setVisible(true);
        });
    }
}