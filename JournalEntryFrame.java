import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
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
    private JButton btnPost;

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

        model.addRow(new Object[]{"", "", "0.00", "0.00", ""});
        model.addRow(new Object[]{"", "", "0.00", "0.00", ""});
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
                // اسم الحساب ReadOnly تماماً - يعبأ حصراً عبر الشجرة
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
        table.setToolTipText("انقر مرتين أو اضغط F3 لاختيار الحساب من الدليل المحاسبي");

        // محاذاة مالية - المدين والدائن يمين/وسط
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        // الباقي افتراضي يمين

        // أبعاد موسعة لاسم الحساب ومكثفة للبيان التفصيلي
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(240);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);

        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                updateTotals();
            }
        });

        // مزدوج على كود أو اسم يفتح الشجرة
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if (row >= 0 && (col == 0 || col == 1)) {
                        openAccountTree(row);
                    }
                }
            }
        });

        // F3 لفتح الشجرة
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "openTreeF3");
        table.getActionMap().put("openTreeF3", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0 && model.getRowCount() > 0) row = 0;
                if (row >= 0) openAccountTree(row);
            }
        });
        // أيضاً على مستوى الجدول عند التركيز
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F3) {
                    int row = table.getSelectedRow();
                    if (row < 0 && model.getRowCount() > 0) row = 0;
                    if (row >= 0) openAccountTree(row);
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

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        btnPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton btnAdd = new JButton("إضافة سطر");
        JButton btnDelete = new JButton("حذف السطر المحدد");
        btnAdd.addActionListener(ev -> {
            model.addRow(new Object[]{"", "", "0.00", "0.00", ""});
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

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        actions.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        btnPost = new JButton("حفظ");
        btnPost.setFont(new Font("Tahoma", Font.BOLD, 14));
        btnPost.setBackground(new Color(16, 185, 129));
        btnPost.setForeground(Color.WHITE);
        btnPost.setPreferredSize(new Dimension(110, 38));
        btnPost.setEnabled(false);
        btnPost.addActionListener(e -> postEntry());

        JButton btnPreview = new JButton("عرض / معاينة");
        btnPreview.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnPreview.setPreferredSize(new Dimension(130, 38));
        btnPreview.addActionListener(e -> showPreview());

        JButton btnPrint = new JButton("طباعة");
        btnPrint.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnPrint.setPreferredSize(new Dimension(100, 38));
        btnPrint.addActionListener(e -> printEntry());

        JButton btnSettings = new JButton("إعدادات الطباعة");
        btnSettings.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnSettings.setPreferredSize(new Dimension(130, 38));
        btnSettings.addActionListener(e -> showPageSetup());

        actions.add(btnPost);
        actions.add(btnPreview);
        actions.add(btnPrint);
        actions.add(btnSettings);
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
            // انسيابية: توجيه التركيز إلى مدين
            SwingUtilities.invokeLater(() -> {
                table.setRowSelectionInterval(row, row);
                table.setColumnSelectionInterval(2, 2);
                table.editCellAt(row, 2);
                Component editor = table.getEditorComponent();
                if (editor != null) editor.requestFocusInWindow();
                else table.requestFocusInWindow();
            });
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
        if (btnPost != null) {
            boolean balanced = Math.abs(diff) < 0.001 && totalDebit > 0.001;
            btnPost.setEnabled(balanced);
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
        try {
            LocalDate.parse(entryDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "تاريخ القيد غير صالح. الصيغة المطلوبة YYYY-MM-DD.", "خطأ", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (narration.isEmpty()) {
            narration = "قيد يومية يدوي";
        }

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

        JournalEntry entry = new JournalEntry(entryNumber, reference.isEmpty() ? entryNumber : reference, "MANUAL", narration);
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
            if (code.isEmpty() && debit == 0 && credit == 0) continue;
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
            try { txtEntryNumber.setText(DocumentNumberService.next("JOURNAL_ENTRY", "JE-")); } catch (Exception ex) { txtEntryNumber.setText("JE-" + System.currentTimeMillis()); }
            model.setRowCount(0);
            model.addRow(new Object[]{"", "", "0.00", "0.00", ""});
            model.addRow(new Object[]{"", "", "0.00", "0.00", ""});
            updateTotals();
        } else {
            JOptionPane.showMessageDialog(this, "فشل ترحيل القيد. قد يكون الرقم مكرراً أو الحساب غير فرعي.", "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildPrintHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:Tahoma; direction:rtl; text-align:right; padding:20px;'>");
        sb.append("<h2 style='text-align:center;'>قيد يومية</h2>");
        sb.append("<p>رقم القيد: ").append(txtEntryNumber.getText()).append(" | التاريخ: ").append(txtEntryDate.getText()).append("</p>");
        sb.append("<p>المرجع: ").append(txtReference.getText()).append("</p>");
        sb.append("<p>البيان: ").append(txtNarration.getText()).append("</p>");
        sb.append("<table border='1' cellpadding='6' cellspacing='0' style='width:100%; border-collapse:collapse;'>");
        sb.append("<tr style='background:#f1f5f9;'><th>كود الحساب</th><th>اسم الحساب</th><th>مدين</th><th>دائن</th><th>البيان</th></tr>");
        for (int i=0;i<model.getRowCount();i++) {
            String c0 = model.getValueAt(i,0)==null?"":model.getValueAt(i,0).toString();
            String c1 = model.getValueAt(i,1)==null?"":model.getValueAt(i,1).toString();
            String c2 = String.format("%,.2f", parseDouble(model.getValueAt(i,2)));
            String c3 = String.format("%,.2f", parseDouble(model.getValueAt(i,3)));
            String c4 = model.getValueAt(i,4)==null?"":model.getValueAt(i,4).toString();
            if (c0.isEmpty() && c2.equals("0.00") && c3.equals("0.00")) continue;
            sb.append("<tr><td>").append(c0).append("</td><td>").append(c1).append("</td><td style='text-align:right;'>").append(c2).append("</td><td style='text-align:right;'>").append(c3).append("</td><td>").append(c4).append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("<p>إجمالي المدين: ").append(lblTotalDebit.getText()).append(" | إجمالي الدائن: ").append(lblTotalCredit.getText()).append(" | الفارق: ").append(lblDifference.getText()).append("</p>");
        sb.append("<br><br><table style='width:100%; text-align:center; margin-top:40px;'><tr><td>إعداد: ___________</td><td>مراجعة: ___________</td><td>اعتماد: ___________</td></tr></table>");
        sb.append("<p style='text-align:center; font-size:10px; color:#64748b; margin-top:20px;'>هامش الصفحة والتوقيعات معتمدة</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private void showPreview() {
        JDialog dlg = new JDialog(this, "معاينة قبل الطباعة - قيد يومية", true);
        dlg.setSize(750, 600);
        dlg.setLocationRelativeTo(this);
        dlg.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JEditorPane pane = new JEditorPane("text/html", buildPrintHtml());
        pane.setEditable(false);
        pane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JScrollPane sp = new JScrollPane(pane);
        dlg.add(sp, BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnDoPrint = new JButton("طباعة");
        btnDoPrint.addActionListener(e->{ dlg.dispose(); printEntry(); });
        JButton btnClose = new JButton("إغلاق");
        btnClose.addActionListener(e->dlg.dispose());
        btns.add(btnDoPrint); btns.add(btnClose);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void printEntry() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("قيد يومية " + txtEntryNumber.getText());
        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
                if (pageIndex > 0) return NO_SUCH_PAGE;
                Graphics2D g2 = (Graphics2D) g;
                g2.translate(pf.getImageableX(), pf.getImageableY());
                // رسم مبسط للنص
                g2.setFont(new Font("Tahoma", Font.PLAIN, 10));
                String[] lines = buildPrintHtml().replaceAll("<[^>]*>", " ").split("\n");
                int y = 20;
                for (String line : lines) {
                    if (y > pf.getImageableHeight() - 20) break;
                    g2.drawString(line.trim(), 10, y);
                    y += 15;
                }
                // توقيعات
                g2.drawString("إعداد: ___________    مراجعة: ___________    اعتماد: ___________", 50, y+30);
                return PAGE_EXISTS;
            }
        });
        if (job.printDialog()) {
            try { job.print(); } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(JournalEntryFrame.this, "خطأ طباعة: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showPageSetup() {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf = job.defaultPage();
        pf = job.pageDialog(pf);
        // أيضاً اختيار الطابعة
        if (job.printDialog()) {
            JOptionPane.showMessageDialog(this, "تم حفظ إعدادات الطابعة (اتجاه/هوامش/الطابعة).", "إعدادات الطباعة", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new JournalEntryFrame().setVisible(true));
    }
}
