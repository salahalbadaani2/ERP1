import javax.swing.*;
import java.awt.*;

public class MainDashboard extends JFrame {

    public MainDashboard() {
        // فحص قاعدة البيانات وتنظيف أي تكرار وتطبيق القيد الهيكلي آلياً عند تشغيل الواجهة
        DatabaseManager.initializeDatabase();
        DatabaseAutoMigration.run();

        // إعدادات النافذة الرئيسية
        setTitle("نظام إدارة المصنع والإنتاج (ERP Factory System)");
        setSize(800, 500);
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // اللوحة الرئيسية
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ترويسة العنوان العلوي
        JLabel headerLabel = new JLabel("لوحة التحكم الرئيسية لنظام ERP المصنعي", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // شبكة أزرار أقسام النظام
        JPanel buttonsPanel = new JPanel(new GridLayout(3, 2, 15, 15));

        JButton btnStock = new JButton("1. إدارة المخازن والمواد الخام (12101)");
        JButton btnProduction = new JButton("2. خطوط الإنتاج والإنتاج تحت التشغيل (12102)");
        JButton btnSales = new JButton("3. المبيعات والتوزيع وسيارت الفان (12103)");
        JButton btnTreasury = new JButton("4. الخزينة والبنك وسندات القبض/الصرف");
        JButton btnReports = new JButton("5. شاشة تقارير الاستعلامات (SQL Reports)");
        JButton btnExit = new JButton("6. خروج من النظام");

        // ضبط خطوط الأزرار
        Font buttonFont = new Font("Segoe UI", Font.PLAIN, 15);
        btnStock.setFont(buttonFont);
        btnProduction.setFont(buttonFont);
        btnSales.setFont(buttonFont);
        btnTreasury.setFont(buttonFont);
        btnReports.setFont(buttonFont);
        btnExit.setFont(buttonFont);

        buttonsPanel.add(btnStock);
        buttonsPanel.add(btnProduction);
        buttonsPanel.add(btnSales);
        buttonsPanel.add(btnTreasury);
        buttonsPanel.add(btnReports);
        buttonsPanel.add(btnExit);

        mainPanel.add(buttonsPanel, BorderLayout.CENTER);

        // إضافة اللوحة للنافذة
        add(mainPanel);

        // أحداث الأزرار
        btnTreasury.addActionListener(e -> new TreasuryVoucherForm().setVisible(true));
        btnReports.addActionListener(e -> new ReportsWindow().setVisible(true));
        btnExit.addActionListener(e -> System.exit(0));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainDashboard().setVisible(true);
        });
    }
}