import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * ============================================================================
 * نظام ERP المصنعي - تسوية انحرافات التكاليف الصناعية وإقفال الشهر المالي
 * ============================================================================
 * معالجة الفروقات بين التكاليف الصناعية التقديرية والفعلية وترحيل الانحراف إلى COGS
 */
public class CostVarianceClosingManager extends JFrame {

    private JTextField txtClosingMonth;
    private JTextField txtAppliedOverhead; // المحملة التقديرية
    private JTextField txtActualOverhead;  // الفعلية الحقيقية
    private JTextField txtVarianceAmount;  // مبلغ الانحراف
    private JLabel lblVarianceStatus;

    public CostVarianceClosingManager() {
        setTitle("نظام ERP المصنعي - تسوية انحرافات التكاليف وإقفال الشهر المالي");
        setSize(850, 520);
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        initUI();
        calculateVariance();
    }

    private void initUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("تسوية انحرافات التكاليف وإقفال الفترة");
        title.setFont(new Font("Tahoma", Font.BOLD, 14));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("مقارنة المصاريف الصناعية المحملة مع الفعلية وترحيل الفروق إلى تكلفة المبيعات COGS");
        sub.setFont(new Font("Tahoma", Font.PLAIN, 12));
        sub.setForeground(new Color(203, 213, 225));

        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 10));
        form.setBorder(new EmptyBorder(20, 25, 20, 25));

        form.add(new JLabel("الشهر المالي للإقفال:"));
        txtClosingMonth = new JTextField("2026-08 (أغسطس)");
        form.add(txtClosingMonth);

        form.add(new JLabel("إجمالي التكاليف الصناعية المحملة التقديرية (Applied MOH):"));
        txtAppliedOverhead = new JTextField("1,450,000.00 YER");
        txtAppliedOverhead.setEditable(false);
        form.add(txtAppliedOverhead);

        form.add(new JLabel("إجمالي التكاليف الصناعية الفعلية المنفقة (Actual MOH):"));
        txtActualOverhead = new JTextField("1,580,000.00 YER");
        txtActualOverhead.setEditable(false);
        form.add(txtActualOverhead);

        form.add(new JLabel("صافي الانحراف المحاسبي (Variance):"));
        txtVarianceAmount = new JTextField("130,000.00 YER (نقص تحميل Under-applied)");
        txtVarianceAmount.setFont(new Font("Tahoma", Font.BOLD, 12));
        txtVarianceAmount.setForeground(new Color(220, 38, 38));
        txtVarianceAmount.setEditable(false);
        form.add(txtVarianceAmount);

        lblVarianceStatus = new JLabel("الأثر المالي: سيتم زيادة تكلفة البضاعة المباعة COGS بمقدار 130,000 YER لضبط الأرباح الفعلية.");
        lblVarianceStatus.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblVarianceStatus.setForeground(new Color(37, 99, 235));
        form.add(lblVarianceStatus);

        add(form, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCloseMonth = new JButton("حفظ وترحيل الإقفال");
        btnCloseMonth.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnCloseMonth.setBackground(new Color(16, 185, 129));
        btnCloseMonth.setForeground(Color.WHITE);
        btnCloseMonth.addActionListener(e -> executeClosingEntry());

        JButton btnCancel = new JButton("إلغاء");
        btnCancel.addActionListener(e -> dispose());

        bottom.add(btnCloseMonth);
        bottom.add(btnCancel);
        add(bottom, BorderLayout.SOUTH);
    }

    private void calculateVariance() {
        // محاكاة حساب الفروق المحاسبية
    }

    private void executeClosingEntry() {
        int res = JOptionPane.showConfirmDialog(this,
                "هل أنت متأكد من ترحيل قيد تسوية انحراف التكاليف وإقفال شهر " + txtClosingMonth.getText() + "؟\n" +
                "سيتم توليد قيد اليومية التالي:\n" +
                "- من حـ/ تكلفة البضاعة المباعة COGS (510101): 130,000 YER\n" +
                "- إلى حـ/ انحراف التكاليف الصناعية غير المباشرة (520901): 130,000 YER",
                "تأكيد إقفال الشهر", JOptionPane.YES_NO_OPTION);

        if (res == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, "تم ترحيل قيد الإقفال بنجاح.");
            dispose();
        }
    }
}