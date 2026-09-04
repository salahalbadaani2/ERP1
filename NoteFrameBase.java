import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * شاشة موحدة لإشعاري المدين والدائن مع رقم ونوع مستند مستقل لكل منهما.
 */
public abstract class NoteFrameBase extends JFrame {
    private final boolean debitNote;
    private final String documentType;
    private final String numberPrefix;
    private final JTextField documentNumber = new JTextField();
    private final JTextField documentDate = new JTextField(java.time.LocalDate.now().toString());
    private final JTextField debitAccount = new JTextField();
    private final JTextField creditAccount = new JTextField();
    private final JTextField amount = new JTextField();
    private final JTextArea narration = new JTextArea(3, 30);

    protected NoteFrameBase(boolean debitNote) {
        this.debitNote = debitNote;
        documentType = debitNote ? "DEBIT_NOTE" : "CREDIT_NOTE";
        numberPrefix = debitNote ? "DN-" : "CN-";
        setTitle(debitNote ? "إشعار مدين" : "إشعار دائن");
        setSize(900, 570);
        setMinimumSize(new Dimension(760, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        initUi();
        applyReadableFonts(getContentPane());
        resetForm();
    }

    private void initUi() {
        setLayout(new BorderLayout(10, 10));

        JPanel header = new JPanel(new BorderLayout(4, 4));
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel title = new JLabel(debitNote
                ? "إشعار مدين | Debit Note"
                : "إشعار دائن | Credit Note");
        title.setFont(new Font("Tahoma", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("إنشاء مستند محاسبي وترحيله بقيد مزدوج متزن");
        subtitle.setFont(new Font("Tahoma", Font.PLAIN, 12));
        subtitle.setForeground(new Color(203, 213, 225));
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(10, 15, 5, 15));
        content.add(createDocumentPanel(), BorderLayout.NORTH);
        content.add(createAccountingPanel(), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
        add(createActions(), BorderLayout.SOUTH);
    }

    private JPanel createDocumentPanel() {
        JPanel panel = titledPanel("بيانات المستند", new GridLayout(2, 4, 10, 8));
        panel.add(new JLabel("رقم المستند:"));
        documentNumber.setEditable(false);
        panel.add(documentNumber);
        panel.add(new JLabel("التاريخ:"));
        documentDate.setEditable(true);
        documentDate.setToolTipText("الصيغة المطلوبة: yyyy-MM-dd");
        panel.add(documentDate);
        panel.add(new JLabel("نوع المستند:"));
        panel.add(new JLabel(debitNote ? "إشعار مدين" : "إشعار دائن"));
        panel.add(new JLabel("العملة:"));
        panel.add(new JLabel("العملة المحلية"));
        return panel;
    }

    private JPanel createAccountingPanel() {
        JPanel panel = titledPanel("بيانات القيد", new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 7, 7, 7);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.2;

        addRow(panel, c, 0, "الحساب المدين:", debitAccount);
        addRow(panel, c, 1, "الحساب الدائن:", creditAccount);
        addAmountRow(panel, c, 2, "المبلغ:", amount);
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0.2;
        panel.add(new JLabel("البيان:"), c);
        c.gridx = 1;
        c.gridwidth = 3;
        c.weightx = 1;
        narration.setLineWrap(true);
        narration.setWrapStyleWord(true);
        narration.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.add(new JScrollPane(narration), c);

        JLabel hint = new JLabel("يجب اختيار حسابين فرعيين من المستوى التشغيلي قبل الترحيل.");
        hint.setForeground(new Color(71, 85, 105));
        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 3;
        panel.add(hint, c);
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, JTextField field) {
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0.2;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        panel.add(accountField(field), c);
    }

    private void addAmountRow(JPanel panel, GridBagConstraints c, int row, String label, JTextField field) {
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0.2;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        panel.add(field, c);
    }

    private JPanel accountField(JTextField field) {
        JPanel wrapper = new JPanel(new BorderLayout(6, 0));
        JButton browse = new JButton("دليل الحسابات");
        browse.addActionListener(e -> {
            AccountTreeDialog dialog = new AccountTreeDialog(this, (String) null);
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent event) {
                    if (dialog.isConfirmed()) {
                        field.setText(dialog.getSelectedAccount());
                        field.requestFocusInWindow();
                    }
                }
            });
            dialog.setVisible(true);
        });
        wrapper.add(field, BorderLayout.CENTER);
        wrapper.add(browse, BorderLayout.WEST);
        return wrapper;
    }

    private JPanel createActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        JButton post = new JButton("حفظ وترحيل");
        post.setBackground(new Color(16, 185, 129));
        post.setForeground(Color.WHITE);
        post.setFocusPainted(false);
        JButton preview = new JButton("معاينة القيد");
        JButton clear = new JButton("مستند جديد");
        JButton close = new JButton("إغلاق");
        post.addActionListener(e -> post());
        preview.addActionListener(e -> preview());
        clear.addActionListener(e -> resetForm());
        close.addActionListener(e -> dispose());
        actions.add(post);
        actions.add(preview);
        actions.add(clear);
        actions.add(close);
        return actions;
    }

    private JPanel titledPanel(String title, LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                title, TitledBorder.RIGHT, TitledBorder.TOP,
                new Font("Tahoma", Font.BOLD, 12)));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        return panel;
    }

    private void resetForm() {
        documentNumber.setText(DocumentNumberService.next(documentType, numberPrefix));
        documentDate.setText(java.time.LocalDate.now().toString());
        debitAccount.setText("");
        creditAccount.setText("");
        amount.setText("");
        narration.setText("");
    }

    private void post() {
        try {
            String debit = cleanCode(debitAccount.getText());
            String credit = cleanCode(creditAccount.getText());
            if (debit.equals(credit)) {
                throw new IllegalArgumentException("لا يجوز أن يكون الحساب المدين والدائن متساويين.");
            }
            double value = parseAmount();
            String date = parseDate();
            String note = narration.getText().trim();
            if (note.isEmpty()) {
                throw new IllegalArgumentException("البيان مطلوب قبل الترحيل.");
            }

            AccountValidator.validateSubAccount(debit, "الحساب المدين");
            AccountValidator.validateSubAccount(credit, "الحساب الدائن");
            JournalEntry entry = createEntry(debit, credit, value, date, note);
            if (!PostingEngine.postJournalEntry(entry)) {
                throw new IllegalStateException("تعذر ترحيل الإشعار. راجع سجل النظام أو رقم المستند.");
            }
            JOptionPane.showMessageDialog(this,
                    "تم حفظ وترحيل " + (debitNote ? "الإشعار المدين" : "الإشعار الدائن")
                            + " رقم " + documentNumber.getText(),
                    "تم بنجاح", JOptionPane.INFORMATION_MESSAGE);
            resetForm();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "تعذر حفظ المستند", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JournalEntry createEntry(String debit, String credit, double value, String date, String note) {
        JournalEntry entry = new JournalEntry(documentNumber.getText(), date, documentNumber.getText(),
                debitNote ? "DEBIT_NOTE" : "CREDIT_NOTE", note);
        entry.addDebitLine(debit, "الحساب المدين", note, value);
        entry.addCreditLine(credit, "الحساب الدائن", note, value);
        return entry;
    }

    private void preview() {
        try {
            double value = parseAmount();
            String html = "<html dir='rtl'><meta charset='UTF-8'>"
                    + "<h2>" + (debitNote ? "إشعار مدين" : "إشعار دائن") + "</h2>"
                    + "<p>رقم المستند: " + documentNumber.getText() + "</p>"
                    + "<p>التاريخ: " + documentDate.getText() + "</p>"
                    + "<table border='1' cellpadding='8' width='100%'><tr><th>الحساب</th>"
                    + "<th>مدين</th><th>دائن</th></tr><tr><td>" + debitAccount.getText()
                    + "</td><td>" + String.format("%,.2f", value) + "</td><td>0.00</td></tr>"
                    + "<tr><td>" + creditAccount.getText() + "</td><td>0.00</td><td>"
                    + String.format("%,.2f", value) + "</td></tr></table>"
                    + "<p>البيان: " + narration.getText().trim() + "</p></html>";
            new DocumentPreviewDialog(this, debitNote ? "معاينة إشعار مدين" : "معاينة إشعار دائن", html)
                    .setVisible(true);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "بيانات غير مكتملة", JOptionPane.WARNING_MESSAGE);
        }
    }

    private double parseAmount() {
        String raw = amount.getText().trim().replace(",", "");
        try {
            double value = Double.parseDouble(raw);
            if (!Double.isFinite(value) || value <= 0) {
                throw new NumberFormatException();
            }

            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("المبلغ يجب أن يكون رقماً موجباً.");
        }
    }

    private String parseDate() {
        String value = documentDate.getText().trim();
        try {
            return LocalDate.parse(value).toString();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("التاريخ غير صحيح. استخدم الصيغة yyyy-MM-dd.");
        }
    }

    private void applyReadableFonts(Component component) {
        if (component instanceof JComponent) {
            Font current = component.getFont();
            if (current != null && current.getSize() < 14) {
                component.setFont(current.deriveFont(14f));
            }
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                applyReadableFonts(child);
            }
        }
    }

    private String cleanCode(String value) {
        String code = value.trim();
        int separator = code.indexOf(" - ");
        return separator > 0 ? code.substring(0, separator).trim() : code;
    }
}
