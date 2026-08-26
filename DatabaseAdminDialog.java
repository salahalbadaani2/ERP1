import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * ============================================================================
 * نظام ERP المصنعي - نافذة فحص وإدارة وصيانة قاعدة بيانات MySQL
 * ============================================================================
 */
public class DatabaseAdminDialog extends JDialog {

    private JTable tblTables;
    private DefaultTableModel modelTables;
    private JTable tblData;
    private DefaultTableModel modelData;
    private JLabel lblStatus;

    public DatabaseAdminDialog(Frame parent) {
        super(parent, "إدارة وصيانة قاعدة بيانات MySQL (erp_factory_db)", true);
        setSize(950, 620);
        setResizable(true);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        initUI();
        loadTablesList();
    }

    private void initUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel title = new JLabel("إدارة قاعدة البيانات");
        title.setFont(new Font("Tahoma", Font.BOLD, 14));
        title.setForeground(Color.WHITE);

        lblStatus = new JLabel("● متصل بنجاح مع erp_factory_db");
        lblStatus.setFont(new Font("Tahoma", Font.BOLD, 12));
        lblStatus.setForeground(new Color(34, 197, 94));

        header.add(title, BorderLayout.EAST);
        header.add(lblStatus, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.35);

        // الجانب الأيمن: قائمة الجداول وعدد السجلات
        String[] colsTbl = {"اسم الجدول في MySQL", "عدد السجلات"};
        modelTables = new DefaultTableModel(colsTbl, 0);
        tblTables = new JTable(modelTables);
        tblTables.setRowHeight(24);
        tblTables.setFont(new Font("Tahoma", Font.PLAIN, 12));
        tblTables.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        splitPane.setRightComponent(new JScrollPane(tblTables));

        // الجانب الأيسر: معاينة بيانات الجدول المحدد
        modelData = new DefaultTableModel();
        tblData = new JTable(modelData);
        tblData.setRowHeight(24);
        tblData.setFont(new Font("Tahoma", Font.PLAIN, 12));
        tblData.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        splitPane.setLeftComponent(new JScrollPane(tblData));

        add(splitPane, BorderLayout.CENTER);

        tblTables.getSelectionModel().addListSelectionListener(e -> {
            int row = tblTables.getSelectedRow();
            if (row >= 0) {
                String tableName = modelTables.getValueAt(row, 0).toString();
                previewTableData(tableName);
            }
        });

        // شريط الأزرار
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        JButton btnRefresh = new JButton("إعادة فحص الجداول");
        btnRefresh.addActionListener(e -> loadTablesList());

        JButton btnClose = new JButton("إغلاق");
        btnClose.addActionListener(e -> dispose());

        bar.add(btnRefresh);
        bar.add(btnClose);
        add(bar, BorderLayout.SOUTH);
    }

    private void loadTablesList() {
        modelTables.setRowCount(0);
        String[] tables = {
                "chart_of_accounts", "journal_entries", "journal_entry_lines",
                "sales_return_invoices", "treasury_vouchers", "items", "exchange_rates"
        };

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String tbl : tables) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tbl)) {
                    if (rs.next()) {
                        modelTables.addRow(new Object[]{tbl, rs.getInt(1) + " سجل"});
                    }
                } catch (Exception ex) {
                    modelTables.addRow(new Object[]{tbl, "غير منشأ"});
                }
            }
            if (modelTables.getRowCount() > 0) tblTables.setRowSelectionInterval(0, 0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ بالاتصال: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void previewTableData(String tableName) {
        modelData.setRowCount(0);
        modelData.setColumnCount(0);

        String sql = "SELECT * FROM " + tableName + " LIMIT 50";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            for (int i = 1; i <= colCount; i++) {
                modelData.addColumn(meta.getColumnName(i));
            }

            while (rs.next()) {
                Object[] row = new Object[colCount];
                for (int i = 1; i <= colCount; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                modelData.addRow(row);
            }
        } catch (Exception ex) {
            System.err.println("خطأ جلب البيانات: " + ex.getMessage());
        }
    }
}