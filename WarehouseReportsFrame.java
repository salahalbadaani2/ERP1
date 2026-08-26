import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class WarehouseReportsFrame extends JFrame {
    public WarehouseReportsFrame() {
        setTitle("نظام ERP المصنعي - تقارير المخزون");
        setSize(760, 520);
        setMinimumSize(new Dimension(600, 420));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JTextArea report = new JTextArea("تقارير المخزون\n\n- أرصدة المواد الخام\n- أرصدة الإنتاج تحت التشغيل\n- أرصدة المنتجات التامة\n- تقرير مخزون المندوبين\n- حركة الاستلام والصرف");
        report.setEditable(false);
        report.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JButton refresh = new JButton("تحديث التقرير");
        refresh.addActionListener(event -> report.append("\nتم تحديث التقرير: " + LocalDate.now()));
        add(new JScrollPane(report), BorderLayout.CENTER);
        add(refresh, BorderLayout.SOUTH);
    }
}
