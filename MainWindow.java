import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================================
 * نظام ERP المصنعي - لوحة التحكم والواجهة الرئيسية الحديثة (Modern MainWindow)
 * ============================================================================
 */
public class MainWindow extends JFrame {

    private JLabel lblDbStatus;
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_DESC = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_STATUS = new Font("Segoe UI", Font.PLAIN, 12);

    public MainWindow() {
        setTitle("نظام ERP المصنعي - لوحة التحكم");
        setSize(1100, 750);
        setMinimumSize(new Dimension(950, 650));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        DatabaseManager.initializeDatabase();
        DatabaseAutoMigration.run();
        initUI();
        checkDatabaseConnection();
    }

    private void initUI() {
        // 1. الشريط العلوي
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. المحتوى المركزي (شبكة البطاقات العصرية)
        add(createDashboardGrid(), BorderLayout.CENTER);

        // 3. شريط الحالة السفلي (Status Bar)
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42)); // Slate 900
        header.setBorder(new EmptyBorder(18, 25, 18, 25));

        // الجانب الأيمن: عنوان النظام والشعار
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel lblApp = new JLabel("نظام ERP المصنعي");
        lblApp.setFont(FONT_TITLE);
        lblApp.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("المبيعات والمشتريات والحسابات والمخزون");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(148, 163, 184)); // Slate 400

        titlePanel.add(lblApp);
        titlePanel.add(Box.createRigidArea(new Dimension(0, 4)));
        titlePanel.add(lblSub);

        // الجانب الأيسر: شارات الحالة والتاريخ
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        infoPanel.setOpaque(false);

        lblDbStatus = new JLabel("● فحص الاتصال...");
        lblDbStatus.setFont(FONT_HEADER);
        lblDbStatus.setForeground(new Color(251, 191, 36));

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        JLabel lblDate = new JLabel("التاريخ: " + dateStr);
        lblDate.setFont(FONT_HEADER);
        lblDate.setForeground(new Color(226, 232, 240));

        infoPanel.add(lblDbStatus);
        infoPanel.add(lblDate);

        header.add(titlePanel, BorderLayout.EAST);
        header.add(infoPanel, BorderLayout.WEST);

        return header;
    }

    private JScrollPane createDashboardGrid() {
        JPanel grid = new JPanel(new GridLayout(0, 3, 16, 16));
        grid.setBackground(new Color(248, 250, 252)); // Slate 50
        grid.setBorder(new EmptyBorder(22, 25, 22, 25));
        grid.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // 1. المبيعات
        grid.add(createModuleCard(
            "المبيعات",
            "",
                new Color(13, 148, 136), // Teal
            () -> openSales()
        ));

        // 2. دفتر الأستاذ العام وميزان المراجعة
        grid.add(createModuleCard(
                "الأستاذ العام وميزان المراجعة",
                "",
                new Color(37, 99, 235), // Blue
                () -> openGeneralLedger()
        ));

        // 3. دليل وشجرة الحسابات
        grid.add(createModuleCard(
                "دليل الحسابات",
                "",
                new Color(124, 58, 237), // Violet
                () -> openAccountTree()
        ));

        // 4. المشتريات
        grid.add(createModuleCard(
            "المشتريات",
            "",
                new Color(2, 132, 199), // Sky
            () -> openPurchases()
        ));

        grid.add(createModuleCard(
            "المخازن",
            "",
            new Color(5, 150, 105), // Emerald
            () -> new InventoryMainFrame().setVisible(true)
        ));

        // 6. التقارير المالية والختامية
        grid.add(createModuleCard(
                "التقارير والقوائم المالية",
                "",
                new Color(225, 29, 72), // Rose
                () -> openReports()
        ));

        // 7. الخزينة
        grid.add(createModuleCard(
            "الخزينة",
            "",
                new Color(79, 70, 229), // Indigo
                () -> openTreasury()
        ));

        // 8. فحص وإدارة قاعدة البيانات
        grid.add(createModuleCard(
                "إدارة قاعدة البيانات",
                "",
                new Color(71, 85, 105), // Slate
                () -> openDatabaseAdmin()
        ));

        // 9. الإعدادات والتهيئة
        grid.add(createModuleCard(
                "الإعدادات والتهيئة",
                "",
                new Color(15, 118, 110), // Teal dark
                () -> new SettingsFrame().setVisible(true)
        ));

        // 10. تسجيل الخروج
        grid.add(createModuleCard(
                "تسجيل الخروج",
                "",
                new Color(220, 38, 38), // Red
                () -> System.exit(0)
        ));

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel createModuleCard(String title, String desc, Color accentColor, Runnable onClick) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(16, 18, 16, 18)
        ));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // شريط ملون جانبي
        JPanel accentBar = new JPanel();
        accentBar.setPreferredSize(new Dimension(5, 0));
        accentBar.setBackground(accentColor);
        card.add(accentBar, BorderLayout.EAST);

        // المحتوى
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(FONT_HEADER);
        lblTitle.setForeground(new Color(15, 23, 42));

        JLabel lblDesc = new JLabel("<html><body style='width: 180px;'>" + desc + "</body></html>");
        lblDesc.setFont(FONT_DESC);
        lblDesc.setForeground(new Color(100, 116, 139)); // Slate 500

        content.add(lblTitle);
        content.add(Box.createRigidArea(new Dimension(0, 6)));
        content.add(lblDesc);

        card.add(content, BorderLayout.CENTER);

        // تأثيرات الفأرة عند التحويم والنقر
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(241, 245, 249)); // Slate 100
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(accentColor, 1),
                        new EmptyBorder(16, 18, 16, 18)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(Color.WHITE);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                        new EmptyBorder(16, 18, 16, 18)
                ));
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }
        });

        return card;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(241, 245, 249));
        bar.setBorder(new EmptyBorder(8, 20, 8, 20));

        JLabel lblUser = new JLabel("المستخدم: مدير النظام (Admin) | الفرع: الإدارة العامة - صنعاء");
        lblUser.setFont(FONT_STATUS);
        lblUser.setForeground(new Color(71, 85, 105));

        JLabel lblVer = new JLabel("الإصدار 2.5");
        lblVer.setFont(FONT_STATUS);
        lblVer.setForeground(new Color(148, 163, 184));

        bar.add(lblUser, BorderLayout.EAST);
        bar.add(lblVer, BorderLayout.WEST);

        return bar;
    }

    private void checkDatabaseConnection() {
        SwingUtilities.invokeLater(() -> {
            try (Connection conn = DatabaseManager.getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    lblDbStatus.setText("● متصل بقاعدة البيانات (MySQL)");
                    lblDbStatus.setForeground(new Color(34, 197, 94)); // Emerald Green
                } else {
                    lblDbStatus.setText("● غير متصل بقاعدة البيانات");
                    lblDbStatus.setForeground(new Color(239, 68, 68)); // Red
                }
            } catch (Exception ex) {
                lblDbStatus.setText("● خطأ بالاتصال: " + ex.getMessage());
                lblDbStatus.setForeground(new Color(239, 68, 68));
            }
        });
    }

    private void openSalesReturnInvoice() {
        try {
            new SalesReturnInvoiceForm().setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في فتح الشاشة: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openSales() {
        new SalesModuleFrame().setVisible(true);
    }

    private void openPurchases() {
        new PurchasesModuleFrame().setVisible(true);
    }

    private void chooseWarehouseMovement() {
        // أبقيت الدالة للتوافق - تم تغيير طريقة العرض فقط من قائمة منبثقة إلى تبويبات داخل InventoryMainFrame
        new InventoryMainFrame().setVisible(true);
    }

    private void openGeneralLedger() {
        try {
            new GeneralLedgerViewer().setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في فتح شاشة الأستاذ العام: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openAccountTree() {
        try {
            AccountTreeDialog dialog = new AccountTreeDialog(this);
            dialog.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في فتح شجرة الحسابات: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openSalesInvoice() {
        try {
            new SalesInvoiceForm().setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في فتح شاشة المبيعات: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openStore() {
        try {
            new WarehouseInventoryManager().setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في فتح شاشة المخازن: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void openReports() {
        try {
            new ReportsWindow().setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في فتح شاشة التقارير: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void openTreasury() {
        try {
            new TreasuryModuleFrame().setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في فتح شاشة الخزينة: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openDatabaseAdmin() {
        try {
            new DatabaseAdminDialog(this).setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في فتح إدارة قاعدة البيانات: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                UIManager.put("Button.focus", new Color(0, 0, 0, 0));
                UIManager.put("Table.showGrid", true);
                UIManager.put("Table.gridColor", new Color(226, 232, 240));
                UIManager.put("Table.selectionBackground", new Color(219, 234, 254));
                UIManager.put("Table.selectionForeground", new Color(15, 23, 42));
                UIManager.put("TabbedPane.selected", Color.WHITE);
            } catch (Exception ignored) {}
            new MainWindow().setVisible(true);
        });
    }
}