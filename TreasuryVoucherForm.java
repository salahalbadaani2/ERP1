import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ============================================================================
 * نظام ERP المصنعي - شاشة سندات القبض والصرف والسيولة النقدية والبنكية
 * ============================================================================
 * 1. سند قبض نقدية / شيك (Receipt Voucher): من حـ/ الصندوق أو البنك (مدين) إلى حـ/ العميل أو الإيراد (دائن).
 * 2. سند صرف نقدية / شيك (Payment Voucher): من حـ/ المورد أو المصروف (مدين) إلى حـ/ الصندوق أو البنك (دائن).
 * 3. الترحيل الآلي المزدوج إلى قيود اليومية العامة والأستاذ العام وميزان المراجعة.
 * 4. التوافق التام مع حارس الحسابات (AccountValidator) وشجرة الحسابات المركزية.
 */
public class TreasuryVoucherForm extends JFrame {

    private static final String LOG_FILE = "TreasuryVouchersLog.txt";
        // عناصر إدخال السند
    private JComboBox<String> cmbVoucherType; // سند قبض / سند صرف
    private JComboBox<String> cmbPaymentMethod; // نقداً (الصندوق) / شيك بنكي / تحويل
    private JTextField txtVoucherNumber;
    private JTextField txtVoucherDate;

    private JTextField txtTreasuryAccount; // حساب الصندوق / البنك
    private JButton btnBrowseTreasury;

    private JTextField txtTargetAccount; // حساب العميل / المورد / المصروف
    private JButton btnBrowseTarget;
    private JLabel lblTargetAccountName;

    private JTextField txtAmount;
    private JTextField txtReference; // رقم الشيك / رقم المرجع
    private JTextArea txtNarration; // البيان والشرح المحاسبي

    private JLabel lblDebitAccountEffect;
    private JLabel lblCreditAccountEffect;

    // أزرار التحكم
    private JButton btnSaveAndPost;
    private JButton btnPreviewPrint;
    private JButton btnClear;
    private JButton btnClose;

    private List<String> masterAccountList;

    public TreasuryVoucherForm() {
        this(-1);
    }

    public TreasuryVoucherForm(boolean receipt) {
        this(receipt ? 0 : 1);
    }

    private TreasuryVoucherForm(int voucherType) {
        setTitle("نظام ERP المصنعي - سند قبض وصرف");
        setSize(980, 680);
        setMinimumSize(new Dimension(850, 550));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        masterAccountList = new ArrayList<>();
        loadAccountsData();

        initUI();
        if (voucherType >= 0) {
            cmbVoucherType.setSelectedIndex(voucherType);
        }
        generateVoucherNumber();
        updateAccountingDirection();
        setupEvents();
    }

    private void initUI() {
        // شريط العنوان العلوي
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42)); // Slate 900
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("سند قبض وصرف");
        title.setFont(new Font("Tahoma", Font.BOLD, 15));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("تسجيل الحركة النقدية وترحيل القيد المحاسبي");
        sub.setFont(new Font("Tahoma", Font.PLAIN, 12));
        sub.setForeground(new Color(203, 213, 225));

        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // المحتوى الرئيسي
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        centerPanel.add(createVoucherHeaderPanel());
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerPanel.add(createAccountsAndAmountPanel());
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerPanel.add(createAccountingDirectionPreviewPanel());

        add(centerPanel, BorderLayout.CENTER);
        add(createActionBar(), BorderLayout.SOUTH);
    }

    private JPanel createVoucherHeaderPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 10, 8));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "بيانات السند",
                TitledBorder.RIGHT, TitledBorder.TOP, new Font("Tahoma", Font.BOLD, 12)
        ));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        panel.add(new JLabel("نوع السند المالي:"));
        cmbVoucherType = new JComboBox<>(new String[]{"سند قبض نقدية / بنك (Receipt Voucher)", "سند صرف نقدية / بنك (Payment Voucher)"});
        cmbVoucherType.setFont(new Font("Tahoma", Font.BOLD, 11));
        panel.add(cmbVoucherType);

        panel.add(new JLabel("طريقة القبض / الصرف:"));
        cmbPaymentMethod = new JComboBox<>(new String[]{"نقداً - الصندوق العام", "شيك بنكي", "تحويل مصرفي / إيداع"});
        panel.add(cmbPaymentMethod);

        panel.add(new JLabel("رقم السند:"));
        txtVoucherNumber = new JTextField();
        txtVoucherNumber.setEditable(false);
        txtVoucherNumber.setFont(new Font("Tahoma", Font.BOLD, 12));
        panel.add(txtVoucherNumber);

        panel.add(new JLabel("تاريخ السند:"));
        txtVoucherDate = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        txtVoucherDate.setFont(new Font("Tahoma", Font.PLAIN, 12));
        panel.add(txtVoucherDate);

        return panel;
    }

    private JPanel createAccountsAndAmountPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 8));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "الحسابات والمبلغ",
                TitledBorder.RIGHT, TitledBorder.TOP, new Font("Tahoma", Font.BOLD, 12)
        ));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // حساب الصندوق / البنك
        panel.add(new JLabel("حساب الخزينة / البنك:"));
        JPanel treasuryBox = new JPanel(new BorderLayout(5, 0));
        txtTreasuryAccount = new JTextField("1110101");
        btnBrowseTreasury = new JButton("دليل الحسابات");
        btnBrowseTreasury.addActionListener(e -> browseAccount(txtTreasuryAccount, "111"));
        treasuryBox.add(txtTreasuryAccount, BorderLayout.CENTER);
        treasuryBox.add(btnBrowseTreasury, BorderLayout.WEST);
        panel.add(treasuryBox);

        // الحساب المقابل (عميل / مورد / مصروف)
        panel.add(new JLabel("الحساب المقابل (العميل / المورد / الحساب الفرعي):"));
        JPanel targetBox = new JPanel(new BorderLayout(5, 0));
        txtTargetAccount = new JTextField("123020001");
        btnBrowseTarget = new JButton("دليل الحسابات");
        btnBrowseTarget.addActionListener(e -> browseAccount(txtTargetAccount, null));
        targetBox.add(txtTargetAccount, BorderLayout.CENTER);
        targetBox.add(btnBrowseTarget, BorderLayout.WEST);
        panel.add(targetBox);

        // المبلغ والمرجع
        panel.add(new JLabel("المبلغ المالي (YER):"));
        txtAmount = new JTextField("575.00");
        txtAmount.setFont(new Font("Tahoma", Font.BOLD, 13));
        txtAmount.setForeground(new Color(16, 185, 129));
        panel.add(txtAmount);

        panel.add(new JLabel("رقم المرجع / الشيك / المستلم:"));
        txtReference = new JTextField("INV-1001 - استلام مستحق مبيعات");
        panel.add(txtReference);

        // البيان والشرح
        panel.add(new JLabel("البيان والشرح المحاسبي:"));
        txtNarration = new JTextArea(2, 20);
        txtNarration.setText("استلام دفعة نقدية لحساب العميل بموجب سند القبض");
        panel.add(new JScrollPane(txtNarration));

        lblTargetAccountName = new JLabel("الطرف المقابل: شركة الأمل للتوزيع والتجارة (123020001)");
        lblTargetAccountName.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblTargetAccountName.setForeground(new Color(37, 99, 235));
        panel.add(lblTargetAccountName);

        return panel;
    }

    private JPanel createAccountingDirectionPreviewPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "ملخص القيد المحاسبي",
                TitledBorder.RIGHT, TitledBorder.TOP, new Font("Tahoma", Font.BOLD, 12)
        ));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.setBackground(new Color(248, 250, 252));

        lblDebitAccountEffect = new JLabel("• الطرف المدين (من حـ/): الصندوق العام (1110101) بمبلغ 575.00 YER [زيادة نقدية]");
        lblDebitAccountEffect.setFont(new Font("Tahoma", Font.BOLD, 12));
        lblDebitAccountEffect.setForeground(new Color(16, 185, 129));

        lblCreditAccountEffect = new JLabel("• الطرف الدائن (إلى حـ/): العملاء - شركة الأمل (123020001) بمبلغ 575.00 YER [تخفيض مديونية]");
        lblCreditAccountEffect.setFont(new Font("Tahoma", Font.BOLD, 12));
        lblCreditAccountEffect.setForeground(new Color(37, 99, 235));

        panel.add(lblDebitAccountEffect);
        panel.add(lblCreditAccountEffect);

        return panel;
    }

    private JPanel createActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bar.setBackground(new Color(241, 245, 249));

        btnPreviewPrint = new JButton("معاينة وطباعة");
        btnPreviewPrint.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnPreviewPrint.addActionListener(e -> previewVoucher());

        btnSaveAndPost = new JButton("حفظ وترحيل");
        btnSaveAndPost.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnSaveAndPost.setBackground(new Color(16, 185, 129));
        btnSaveAndPost.setForeground(Color.WHITE);
        btnSaveAndPost.addActionListener(e -> postVoucherToDatabase());

        btnClear = new JButton("مسح");
        btnClear.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnClear.addActionListener(e -> clearForm());

        btnClose = new JButton("إغلاق");
        btnClose.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnClose.addActionListener(e -> dispose());

        bar.add(btnPreviewPrint);
        bar.add(btnSaveAndPost);
        bar.add(btnClear);
        bar.add(btnClose);

        return bar;
    }

    private void generateVoucherNumber() {
        boolean isReceipt = cmbVoucherType.getSelectedIndex() == 0;
        txtVoucherNumber.setText(DocumentNumberService.next(
            isReceipt ? "RECEIPT_VOUCHER" : "PAYMENT_VOUCHER",
            isReceipt ? "RV-" : "PV-"));
    }

    private void setupEvents() {
        cmbVoucherType.addActionListener(e -> {
            generateVoucherNumber();
            updateAccountingDirection();
        });

        txtAmount.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateAccountingDirection();
            }
        });

        txtTreasuryAccount.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) { updateAccountingDirection(); }
        });

        txtTargetAccount.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) { updateAccountingDirection(); }
        });
    }

    private void updateAccountingDirection() {
        boolean isReceipt = cmbVoucherType.getSelectedIndex() == 0;
        String treasury = txtTreasuryAccount.getText().trim();
        String target = txtTargetAccount.getText().trim();
        String amt = txtAmount.getText().trim();

        if (isReceipt) {
            lblDebitAccountEffect.setText("• الطرف المدين (من حـ/): الخزينة/البنك (" + treasury + ") بمبلغ " + amt + " YER [زيادة السيولة]");
            lblDebitAccountEffect.setForeground(new Color(16, 185, 129));
            lblCreditAccountEffect.setText("• الطرف الدائن (إلى حـ/): الحساب المقابل (" + target + ") بمبلغ " + amt + " YER [تسديد واستحقاق]");
            lblCreditAccountEffect.setForeground(new Color(37, 99, 235));
            txtNarration.setText("استلام دفعة نقدية بموجب سند القبض رقم " + txtVoucherNumber.getText());
        } else {
            lblDebitAccountEffect.setText("• الطرف المدين (من حـ/): حساب المصروف/المورد (" + target + ") بمبلغ " + amt + " YER [إثبات الصرف]");
            lblDebitAccountEffect.setForeground(new Color(220, 38, 38));
            lblCreditAccountEffect.setText("• الطرف الدائن (إلى حـ/): الخزينة/البنك (" + treasury + ") بمبلغ " + amt + " YER [صرف السيولة]");
            lblCreditAccountEffect.setForeground(new Color(217, 119, 6));
            txtNarration.setText("صرف نقدية بموجب سند الصرف رقم " + txtVoucherNumber.getText());
        }
    }

    private void browseAccount(JTextField targetField, String prefix) {
        AccountTreeDialog dialog = new AccountTreeDialog(this, prefix);
        dialog.setVisible(true);
        if (dialog.isAccountSelected()) {
            targetField.setText(dialog.getSelectedAccountCode());
            updateAccountingDirection();
        }
    }

    private void postVoucherToDatabase() {
        String vNo = txtVoucherNumber.getText().trim();
        String vDate = txtVoucherDate.getText().trim();
        String treasuryAcc = txtTreasuryAccount.getText().trim();
        String targetAcc = txtTargetAccount.getText().trim();
        String ref = txtReference.getText().trim();
        String narration = txtNarration.getText().trim();
        boolean isReceipt = cmbVoucherType.getSelectedIndex() == 0;

        double amount = 0.0;
        try {
            amount = Double.parseDouble(txtAmount.getText().replaceAll("[^0-9.]", ""));
            if (amount <= 0) throw new Exception();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "يرجى إدخال مبلغ مالي صحيح وموجب!", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // معاملة ذرية متكاملة

            validatePostingAccount(conn, treasuryAcc);
            validatePostingAccount(conn, targetAcc);

            // 1. توليد قيد اليومية العامة المزدوج المتوازن (Journal Voucher)
            String jvNo = "JV-" + vNo;
            String sqlJv = "INSERT INTO journal_entries (entry_number, entry_date, reference_doc, source_module, narration, total_debit, total_credit, posted_by) " +
                           "VALUES (?, ?, ?, 'TREASURY', ?, ?, ?, 'النظام الآلي')";
            try (PreparedStatement pstmtJv = conn.prepareStatement(sqlJv, Statement.RETURN_GENERATED_KEYS)) {
                pstmtJv.setString(1, jvNo);
                pstmtJv.setString(2, vDate);
                pstmtJv.setString(3, vNo);
                pstmtJv.setString(4, narration);
                pstmtJv.setDouble(5, amount);
                pstmtJv.setDouble(6, amount);
                pstmtJv.executeUpdate();

                try (ResultSet keys = pstmtJv.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("تعذر الحصول على رقم القيد الداخلي");
                    long entryId = keys.getLong(1);

                    insertJournalLines(conn, entryId, treasuryAcc, targetAcc, amount, isReceipt, narration);
                }
            }

            // 2. تحديث الأرصدة الدفترية في شجرة الحسابات فورياً
            updateAccountBalance(conn, treasuryAcc, isReceipt ? amount : -amount);
            updateAccountBalance(conn, targetAcc, isReceipt ? -amount : amount);

            // 3. الحفظ في جدول سندات الخزينة
            String sqlTv = "INSERT INTO treasury_vouchers (voucher_number, voucher_date, voucher_type, account_code, amount, reference_name, narration) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmtTv = conn.prepareStatement(sqlTv)) {
                pstmtTv.setString(1, vNo);
                pstmtTv.setString(2, vDate);
                pstmtTv.setString(3, isReceipt ? "RECEIPT" : "PAYMENT");
                pstmtTv.setString(4, isReceipt ? treasuryAcc : targetAcc);
                pstmtTv.setDouble(5, amount);
                pstmtTv.setString(6, ref);
                pstmtTv.setString(7, narration);
                pstmtTv.executeUpdate();
            }

            conn.commit();

            // الحفظ في السجل النصي Log
            try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
                writer.write(vNo + " | " + vDate + " | " + (isReceipt ? "RECEIPT" : "PAYMENT") + " | " + amount + " | " + treasuryAcc + " <-> " + targetAcc + " | " + narration + "\n");
            } catch (Exception ignored) {}

            JOptionPane.showMessageDialog(this,
                    "تم ترحيل السند وإثبات القيد المحاسبي بنجاح.\n" +
                    "- رقم السند: " + vNo + "\n" +
                    "- قيد اليومية العامة المزدوج: " + jvNo + "\n" +
                    "- المبلغ المرحل: " + String.format("%,.2f YER", amount) + "\n" +
                    "- تم تحديث الأستاذ العام وميزان المراجعة آلياً.",
                    "اكتمال الترحيل المالي", JOptionPane.INFORMATION_MESSAGE);

            generateVoucherNumber();
            clearForm();

        } catch (Exception ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            JOptionPane.showMessageDialog(this, "خطأ في ترحيل السند: " + ex.getMessage(), "خطأ مالي", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        }
    }

    private void insertJournalLines(Connection conn, long entryId, String treasuryAcc, String targetAcc,
                                    double amount, boolean isReceipt, String narration) throws SQLException {
        String sqlLine = "INSERT INTO journal_entry_lines (entry_id, account_code, debit_amount, credit_amount, line_narration) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmtLine = conn.prepareStatement(sqlLine)) {
            addJournalLine(pstmtLine, entryId, isReceipt ? treasuryAcc : targetAcc,
                    isReceipt ? amount : amount, 0.0, isReceipt ? "قبض نقدية/بنك" : narration);
            addJournalLine(pstmtLine, entryId, isReceipt ? targetAcc : treasuryAcc,
                    0.0, amount, isReceipt ? narration : "صرف نقدية/بنك");
        }
    }

    private void addJournalLine(PreparedStatement statement, long entryId, String accountCode,
                                double debit, double credit, String narration) throws SQLException {
        statement.setLong(1, entryId);
        statement.setString(2, accountCode);
        statement.setDouble(3, debit);
        statement.setDouble(4, credit);
        statement.setString(5, narration);
        statement.executeUpdate();
    }

    private void validatePostingAccount(Connection conn, String accountCode) throws SQLException {
        String sql = "SELECT account_level, is_sub_account FROM chart_of_accounts WHERE account_code = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, accountCode);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !result.getBoolean("is_sub_account")) {
                    throw new SQLException("الحساب غير صالح للترحيل أو حساب رئيسي تجميعي: " + accountCode);
                }
            }
        }
    }

    private void updateAccountBalance(Connection conn, String accountCode, double delta) {
        String sql = "UPDATE chart_of_accounts SET current_balance = current_balance + ? WHERE account_code = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, delta);
            pstmt.setString(2, accountCode);
            if (pstmt.executeUpdate() != 1) {
                throw new SQLException("لم يتم تحديث رصيد الحساب: " + accountCode);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    private void previewVoucher() {
        try {
            double amt = Double.parseDouble(txtAmount.getText().replaceAll("[^0-9.]", ""));
            boolean isReceipt = cmbVoucherType.getSelectedIndex() == 0;
            TreasuryVoucherPrintPreviewDialog preview = new TreasuryVoucherPrintPreviewDialog(
                    this,
                    txtVoucherNumber.getText().trim(),
                    txtVoucherDate.getText().trim(),
                    isReceipt ? "سند قبض" : "سند صرف",
                    cmbPaymentMethod.getSelectedItem().toString(),
                    isReceipt ? "الجهة المستلمة" : "الجهة المستفيدة",
                    txtTreasuryAccount.getText().trim(),
                    txtTargetAccount.getText().trim(),
                    amt,
                    txtReference.getText().trim(),
                    txtNarration.getText().trim()
            );
            preview.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في المعاينة: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        txtAmount.setText("0.00");
        txtReference.setText("");
        generateVoucherNumber();
        updateAccountingDirection();
    }

    private void loadAccountsData() {
        masterAccountList.clear();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT account_code, account_name, is_sub_account, account_level FROM chart_of_accounts WHERE is_sub_account=1 ORDER BY account_code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String display = rs.getString(1) + " - " + rs.getString(2) + " (حساب فرعي - مستوى " + rs.getInt(4) + ")";
                masterAccountList.add(display);
            }
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new TreasuryVoucherForm().setVisible(true);
        });
    }
}
