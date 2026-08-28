import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class JournalEntryFrame extends JFrame {

    private final JTextField txtEntryNumber = new JTextField(12);
    private final JTextField txtEntryDate = new JTextField(10);
    private final JTextField txtReference = new JTextField(15);
    private final JTextField txtNarration = new JTextField(30);

    private DefaultTableModel model;
    private JTable table;

    private final JLabel lblTotalDebit = new JLabel("0.00");
    private final JLabel lblTotalCredit = new JLabel("0.00");
    private final JLabel lblDifference = new JLabel("0.00");

    private static final Font FONT_HEADER = new Font("Tahoma", Font.BOLD, 12);
    private static final Font FONT_PLAIN = new Font("Tahoma", Font.PLAIN, 12);

    public JournalEntryFrame() {
        setTitle("قيد يومية (جزئي ومركب)");
        setSize(900, 620);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setLayout(new BorderLayout(10, 10));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);

        // تهيئة رقم و تاريخ افتراضيين
        try {
            txtEntryNumber.setText(DocumentNumberService.next("JOURNAL_ENTRY", "JE-"));
        } catch (Exception ex) {
            txtEntryNumber.setText("JE-" + System.currentTimeMillis());
        }
        txtEntryNumber.setEditable(false);
        txtEntryNumber.setBackground(new Color(245, 245, 245));
        txtEntryDate.setText(LocalDate.now().toString());
        txtReference.setText("");
        txtNarration.setText("");

        // صفان ابتدائيان لسهولة الإدخال
        model.addRow(new Object[]{"", "", "0", "0", ""});
        model.addRow(new Object[]{"", "", "0", "0", ""});
        updateTotals();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new GridBagLayout());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(12, 12, 12, 12)
        ));
        header.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;

        // الصف الأول: رقم القيد و تاريخ القيد
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        header.add(new JLabel("رقم القيد:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5;
        txtEntryNumber.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        header.add(txtEntryNumber, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        header.add(new JLabel("تاريخ القيد (YYYY-MM-DD):"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5;
        txtEntryDate.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        header.add(txtEntryDate, gbc);

        // الصف الثاني: رقم المرجع و البيان العام
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        header.add(new JLabel("رقم المرجع / المستند:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5;
        txtReference.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        header.add(txtReference, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        header.add(new JLabel("البيان العام للقيد:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5;
        txtNarration.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        header.add(txtNarration, gbc);

        return header;
    }

    private JPanel createTablePanel() {
        String[] cols = {"كود الحساب", "اسم الحساب", "مدين", "دائن", "البيان التفصيلي"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // كود واسم يعبآن عبر الشجرة فقط، الباقي قابل للتحرير
                return column == 2 || column == 3 || column == 4;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        };

        table = new JTable(model);
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        table.getTableHeader().setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        table.setFont(FONT_PLAIN);
        table.getTableHeader().setFont(FONT_HEADER);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // تنسيق الأعمدة
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setPreferredWidth(200);

        // تحديث الاتزان عند أي تعديل
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                updateTotals();
            }
        });

        // نقر مزدوج على كود الحساب يفتح الشجرة
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if (row >= 0 && col == 0) {
                        openAccountTree(row);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        scroll.setBorder(BorderFactory.createTitledBorder("تفاصيل القيد - القيد المزدوج"));

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));
        panel.add(scroll, BorderLayout.CENTER);

        // أزرار التحكم في الأسطر
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        btnPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton btnAdd = new JButton("إضافة سطر");
        JButton btnDelete = new JButton("حذف السطر المحدد");
        btnAdd.addActionListener(ev -> {
            model.addRow(new Object[]{"", "", "0", "0", ""});
            updateTotals();
        });
        btnDelete.addActionListener(ev -> {
            int sel = table.getSelectedRow();
            if (sel >= 0) {
                model.removeRow(sel);
                updateTotals();
            } else {
                JOptionPane.showMessageDialog(JournalEntryFrame.this, "يرجى تحديد السطر المراد حذفه.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            }
        });
        btnPanel.add(btnAdd);
        btnPanel.add(btnDelete);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout(10, 10));
        footer.setBorder(new EmptyBorder(8, 12, 12, 12));
        footer.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // شريط الاتزان
        JPanel totals = new JPanel(new GridLayout(1, 3, 10, 10));
        totals.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        totals.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));

        JPanel pDebit = new JPanel(new BorderLayout());
        pDebit.setBorder(new EmptyBorder(8, 8, 8, 8));
        pDebit.add(new JLabel("إجمالي المدين:", SwingConstants.CENTER), BorderLayout.NORTH);
        lblTotalDebit.setHorizontalAlignment(SwingConstants.CENTER);
        lblTotalDebit.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblTotalDebit.setForeground(new Color(16, 185, 129));
        pDebit.add(lblTotalDebit, BorderLayout.CENTER);

        JPanel pCredit = new JPanel(new BorderLayout());
        pCredit.setBorder(new EmptyBorder(8, 8, 8, 8));
        pCredit.add(new JLabel("إجمالي الدائن:", SwingConstants.CENTER), BorderLayout.NORTH);
        lblTotalCredit.setHorizontalAlignment(SwingConstants.CENTER);
        lblTotalCredit.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblTotalCredit.setForeground(new Color(37, 99, 235));
        pCredit.add(lblTotalCredit, BorderLayout.CENTER);

        JPanel pDiff = new JPanel(new BorderLayout());
        pDiff.setBorder(new EmptyBorder(8, 8, 8, 8));
        pDiff.add(new JLabel("الفارق / عدم الاتزان:", SwingConstants.CENTER), BorderLayout.NORTH);
        lblDifference.setHorizontalAlignment(SwingConstants.CENTER);
        lblDifference.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblDifference.setForeground(new Color(220, 38, 38));
        pDiff.add(lblDifference, BorderLayout.CENTER);

        totals.add(pDebit);
        totals.add(pCredit);
        totals.add(pDiff);

        footer.add(totals, BorderLayout.CENTER);

        // زر الترحيل
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        actions.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton btnPost = new JButton("ترحيل وحفظ القيد");
        btnPost.setFont(new Font("Tahoma", Font.BOLD, 14));
        btnPost.setBackground(new Color(16, 185, 129));
        btnPost.setForeground(Color.WHITE);
        btnPost.setPreferredSize(new Dimension(180, 38));
        btnPost.addActionListener(e -> postEntry());
        actions.add(btnPost);
        footer.add(actions, BorderLayout.SOUTH);

        return footer;
    }

    private void openAccountTree(int row) {
        AccountTreeDialog dialog = new AccountTreeDialog(this);
        dialog.setVisible(true);
        if (dialog.isAccountSelected()) {
            String code = dialog.getSelectedAccountCode();
            String name = dialog.getSelectedAccountName();
            model.setValueAt(code, row, 0);
            model.setValueAt(name, row, 1);
            updateTotals();
        }
    }

    private double parseDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        String s = o.toString().trim().replace(",", "");
        if (s.isEmpty()) return 0.0;
        try { return Double.parseDouble(s); } catch (NumberFormatException ex) { return 0.0; }
    }

    private void updateTotals() {
        double totalDebit = 0.0;
        double totalCredit = 0.0;
        for (int i = 0; i < model.getRowCount(); i++) {
            totalDebit += parseDouble(model.getValueAt(i, 2));
            totalCredit += parseDouble(model.getValueAt(i, 3));
        }
        lblTotalDebit.setText(String.format("%,.2f", totalDebit));
        lblTotalCredit.setText(String.format("%,.2f", totalCredit));
        double diff = totalDebit - totalCredit;
        lblDifference.setText(String.format("%,.2f", diff));
        if (Math.abs(diff) < 0.001) {
            lblDifference.setForeground(new Color(16, 185, 129));
        } else {
            lblDifference.setForeground(new Color(220, 38, 38));
        }
    }

    private void postEntry() {
        String entryNumber = txtEntryNumber.getText().trim();
        String entryDateStr = txtEntryDate.getText().trim();
        String reference = txtReference.getText().trim();
        String narration = txtNarration.getText().trim();

        if (entryNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this, "رقم القيد مطلوب.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // تحقق تاريخ YYYY-MM-DD
        try {
            LocalDate.parse(entryDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "تاريخ القيد غير صالح. الصيغة المطلوبة YYYY-MM-DD.", "خطأ", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (narration.isEmpty()) {
            narration = "قيد يومية يدوي";
        }

        // جمع المجاميع والتحقق من شروط الاتزان
        double totalDebit = 0.0;
        double totalCredit = 0.0;
        for (int i = 0; i < model.getRowCount(); i++) {
            totalDebit += parseDouble(model.getValueAt(i, 2));
            totalCredit += parseDouble(model.getValueAt(i, 3));
        }
        double diff = totalDebit - totalCredit;
        if (Math.abs(diff) > 0.001) {
            JOptionPane.showMessageDialog(this, "القيد غير متزن. الفارق = " + String.format("%,.2f", diff) + "\nيجب أن يكون إجمالي المدين = إجمالي الدائن.", "خطأ اتزان", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (totalDebit <= 0.001) {
            JOptionPane.showMessageDialog(this, "إجمالي القيمة يجب أن يكون أكبر من صفر.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // بناء كائن القيد والتحقق من كل سطر
        JournalEntry entry = new JournalEntry(entryNumber, reference.isEmpty() ? entryNumber : reference, "MANUAL", narration);
        // تعيين التاريخ المدخل عبر الانعكاس
        try {
            java.lang.reflect.Field f = JournalEntry.class.getDeclaredField("entryDate");
            f.setAccessible(true);
            f.set(entry, entryDateStr);
        } catch (Exception ignored) {}

        for (int i = 0; i < model.getRowCount(); i++) {
            String code = model.getValueAt(i, 0) != null ? model.getValueAt(i, 0).toString().trim() : "";
            String name = model.getValueAt(i, 1) != null ? model.getValueAt(i, 1).toString().trim() : "";
            double debit = parseDouble(model.getValueAt(i, 2));
            double credit = parseDouble(model.getValueAt(i, 3));
            String lineNarr = model.getValueAt(i, 4) != null ? model.getValueAt(i, 4).toString().trim() : "";

            if (code.isEmpty() && debit == 0 && credit == 0) continue; // سطر فارغ يتم تجاهله

            if (code.isEmpty()) {
                JOptionPane.showMessageDialog(this, "السطر " + (i+1) + ": كود الحساب مطلوب.", "تنبيه", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if ((debit > 0 && credit > 0) || (debit == 0 && credit == 0)) {
                JOptionPane.showMessageDialog(this, "السطر " + (i+1) + ": يجب إدخال مبلغ في المدين أو الدائن فقط.", "تنبيه", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (name.isEmpty()) name = code;
            if (lineNarr.isEmpty()) lineNarr = narration;
            try {
                if (debit > 0) entry.addDebitLine(code, name, lineNarr, debit);
                else entry.addCreditLine(code, name, lineNarr, credit);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "السطر " + (i+1) + ": " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        if (entry.getLines().isEmpty()) {
            JOptionPane.showMessageDialog(this, "لا توجد سطور صالحة للترحيل.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean success = PostingEngine.postJournalEntry(entry);
        if (success) {
            JOptionPane.showMessageDialog(this, "تم ترحيل وحفظ القيد [" + entryNumber + "] بنجاح.\n" + entry.toString(), "نجاح", JOptionPane.INFORMATION_MESSAGE);
            // تهيئة رقم جديد وتفريغ الجدول
            try { txtEntryNumber.setText(DocumentNumberService.next("JOURNAL_ENTRY", "JE-")); } catch (Exception ex) { txtEntryNumber.setText("JE-" + System.currentTimeMillis()); }
            model.setRowCount(0);
            model.addRow(new Object[]{"", "", "0", "0", ""});
            model.addRow(new Object[]{"", "", "0", "0", ""});
            updateTotals();
        } else {
            JOptionPane.showMessageDialog(this, "فشل ترحيل القيد. قد يكون الرقم مكرراً أو الحساب غير فرعي.", "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new JournalEntryFrame().setVisible(true));
    }
}
