import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;

/**
 * ============================================================================
 * نظام ERP المصنعي - نافذة إدارة واستحداث وتعديل الحسابات (QuickAccountDialog)
 * ============================================================================
 * نافذة معزولة لإضافة الحسابات الجديدة وتعديل الحسابات الحالية.
 */
public class QuickAccountDialog extends JDialog {

    private JRadioButton rdoChild;
    private JRadioButton rdoParent;
    private JTextField txtAccountCode;
    private JTextField txtAccountName;
    private JTextField txtParentCode = new JTextField();
    private JComboBox<String> cmbType;
    private JButton btnSave;
    private JButton btnCancel;
    private JLabel lblTransactionNotice;

    private boolean accountCreatedOrUpdated = false;
    private String formattedAccountResult = "";
    private String originalAccountString = "";
    private boolean isEditMode = false;

    public QuickAccountDialog(Frame owner, String selectedAccount, List<String> masterAccounts) {
        this(owner, selectedAccount, masterAccounts, false);
    }

    public QuickAccountDialog(Frame owner, String accountData, List<String> masterAccounts, boolean isEdit) {
        super(owner, isEdit ? "استعلام وتعديل بيانات الحساب" : "فتح حساب جديد في شجرة الحسابات", true);
        this.isEditMode = isEdit;
        this.originalAccountString = accountData;

        setSize(620, 380);
        setResizable(true);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // 1. مستوى الحساب
        JPanel pnlRow1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlRow1.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JLabel lblRelation = new JLabel("مستوى الحساب المراد فتحه: ");
        lblRelation.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
        rdoChild = new JRadioButton("ابن (متفرع تحته)", true);
        rdoParent = new JRadioButton("أب (موازٍ في نفس المستوى)", false);
        rdoChild.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        rdoParent.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rdoChild);
        bg.add(rdoParent);
        pnlRow1.add(lblRelation);
        pnlRow1.add(rdoChild);
        pnlRow1.add(rdoParent);
        mainPanel.add(pnlRow1);

        // 2. رقم الحساب واسم الحساب في صف واحد
        JPanel pnlRow2 = new JPanel(new GridBagLayout());
        pnlRow2.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblCode = new JLabel("رقم الحساب:");
        lblCode.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        gbc.gridx = 0; gbc.weightx = 0.0;
        pnlRow2.add(lblCode, gbc);

        txtAccountCode = new JTextField();
        txtAccountCode.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        txtAccountCode.setHorizontalAlignment(JTextField.RIGHT);
        txtAccountCode.setFont(new Font("Tahoma", Font.BOLD, 13));
        gbc.gridx = 1; gbc.weightx = 0.35;
        pnlRow2.add(txtAccountCode, gbc);

        JLabel lblName = new JLabel("اسم الحساب:");
        lblName.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        gbc.gridx = 2; gbc.weightx = 0.0;
        pnlRow2.add(lblName, gbc);

        txtAccountName = new JTextField();
        txtAccountName.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        txtAccountName.setHorizontalAlignment(JTextField.RIGHT);
        txtAccountName.setFont(new Font("Tahoma", Font.PLAIN, 12));
        gbc.gridx = 3; gbc.weightx = 0.65;
        pnlRow2.add(txtAccountName, gbc);

        mainPanel.add(pnlRow2);

        // 3. تصنيف الحساب
        JPanel pnlRow3 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlRow3.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JLabel lblType = new JLabel("تصنيف الحساب: ");
        lblType.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        cmbType = new JComboBox<>(new String[]{
            "حساب رئيسي (يتفرع منه حسابات أخرى)",
            "حساب فرعي (مخصص للعمليات والقيود المالية)"
        });
        cmbType.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cmbType.setFont(new Font("Tahoma", Font.PLAIN, 12));
        pnlRow3.add(lblType);
        pnlRow3.add(cmbType);
        mainPanel.add(pnlRow3);

        // 4. حالة الرقابة والأمان
        JPanel pnlRow4 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlRow4.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JLabel lblNoticeHeader = new JLabel("حالة الرقابة والأمان: ");
        lblNoticeHeader.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        lblTransactionNotice = new JLabel("");
        lblTransactionNotice.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblTransactionNotice.setForeground(Color.RED);
        lblTransactionNotice.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        pnlRow4.add(lblNoticeHeader);
        pnlRow4.add(lblTransactionNotice);
        mainPanel.add(pnlRow4);

        // 5. أزرار التحكم
        JPanel pnlRow5 = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        pnlRow5.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        btnSave = new JButton(isEditMode ? "تحديث وحفظ التعديلات" : "حفظ واعتتماد فوراً");
        btnCancel = new JButton("إلغاء");
        btnSave.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnCancel.setFont(new Font("Tahoma", Font.PLAIN, 12));

        pnlRow5.add(btnSave);
        pnlRow5.add(btnCancel);
        mainPanel.add(pnlRow5);

        add(mainPanel, BorderLayout.CENTER);

        // تهيئة البيانات حسب الوضع
        if (isEditMode) {
            setupEditMode(accountData);
        } else {
            lblTransactionNotice.setText("حساب جديد (لم تسجل عليه حركات بعد)");
            lblTransactionNotice.setForeground(new Color(0, 128, 0));
            updateAutoCode(accountData, masterAccounts);

            rdoChild.addActionListener(e -> updateAutoCode(accountData, masterAccounts));
            rdoParent.addActionListener(e -> updateAutoCode(accountData, masterAccounts));
        }

        btnSave.addActionListener(e -> handleSave());
        btnCancel.addActionListener(e -> dispose());
    }

    private void setupEditMode(String accountData) {
        if (accountData == null || accountData.isEmpty() || accountData.startsWith("اختر")) {
            return;
        }

        String[] parts = accountData.split(" - ");
        String code = parts[0].trim();
        String rest = parts.length > 1 ? parts[1].trim() : "";

        String name = rest;
        String typeStr = "";

        if (rest.contains(" (")) {
            int idx = rest.lastIndexOf(" (");
            name = rest.substring(0, idx).trim();
            typeStr = rest.substring(idx + 2, rest.length() - 1).trim();
        }

        txtAccountCode.setText(toAsciiDigits(code));
        txtAccountName.setText(name);

        if (typeStr.contains("فرعي")) {
            cmbType.setSelectedIndex(1);
        } else {
            cmbType.setSelectedIndex(0);
        }

        boolean hasTransactions = AccountValidator.hasFinancialTransactions(code);

        if (hasTransactions) {
            rdoChild.setEnabled(false);
            rdoParent.setEnabled(false);
            txtAccountCode.setEnabled(false);
            cmbType.setEnabled(false);
            txtAccountName.setEnabled(true);

            lblTransactionNotice.setText("تنبيه أمني: توجد حركات مالية. يُسمح بتعديل الاسم فقط.");
            lblTransactionNotice.setForeground(Color.RED);
        } else {
            rdoChild.setEnabled(true);
            rdoParent.setEnabled(true);
            txtAccountCode.setEnabled(true);
            cmbType.setEnabled(true);
            txtAccountName.setEnabled(true);

            lblTransactionNotice.setText("لا توجد حركات مالية مسجلة. يمكن تعديل كامل البيانات.");
            lblTransactionNotice.setForeground(new Color(0, 100, 0));
        }
    }

    private void updateAutoCode(String selectedAccount, List<String> masterAccounts) {
        boolean isChild = rdoChild.isSelected();
        String generatedCode = AccountAutoNumberService.generateNextCode(selectedAccount, isChild, masterAccounts);
        
        txtAccountCode.setText(toAsciiDigits(generatedCode));

        if (isChild && generatedCode.length() >= 6) {
            cmbType.setSelectedIndex(1);
        } else {
            cmbType.setSelectedIndex(0);
        }
    }

    private String toAsciiDigits(String input) {
        if (input == null) return "";
        StringBuilder builder = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if (ch >= '\u0660' && ch <= '\u0669') {
                builder.append((char) (ch - '\u0660' + '0'));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private void handleSave() {
        String name = txtAccountName.getText().trim();
        String code = txtParentCode.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "يرجى كتابة اسم الحساب الجديد.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // فحص الرقابة والأمان: منع تكرار رقم الحساب أو اسمه عبر قاعدة البيانات حصراً
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT account_code, account_name FROM chart_of_accounts WHERE account_code=? OR account_name=? LIMIT 1")) {
            ps.setString(1, code);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String existingCode = rs.getString(1);
                    String existingName = rs.getString(2);
                    if (existingCode.equalsIgnoreCase(code)) {
                        JOptionPane.showMessageDialog(this, "حظر أمني: رقم الحساب (" + code + ") موجود مسبقاً في دليل الحسابات.", "رفض الإضافة", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (existingName.equalsIgnoreCase(name)) {
                        JOptionPane.showMessageDialog(this, "حظر أمني: اسم الحساب (\"" + name + "\") موجود مسبقاً في الشجرة لمنع تكرار الحسابات.", "رفض الإضافة", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }
        } catch (Exception ignored) {}

        String classificationText = cmbType.getSelectedIndex() == 0 ? "حساب فرعي" : "حساب رئيسي";
        this.formattedAccountResult = String.format(Locale.US, "%s - %s (%s)", code, name, classificationText);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO chart_of_accounts (account_code, account_name, account_level, is_sub_account, parent_code, current_balance) VALUES (?, ?, ?, ?, ?, 0)")) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setInt(3, code.length());
            ps.setBoolean(4, cmbType.getSelectedIndex() == 0);
            ps.setString(5, txtParentCode.getText().trim());
            ps.executeUpdate();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ حفظ في قاعدة البيانات: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
            return;
        }

        this.accountCreatedOrUpdated = true;
        JOptionPane.showMessageDialog(this, "تم إضافة الحساب بالشجرة بنجاح برقم: " + code, "نجاح", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
    public boolean isAccountCreated() { return accountCreatedOrUpdated; }
    public String getNewAccountFormatted() { return formattedAccountResult; }
    public String getOriginalAccountString() { return originalAccountString; }
    }
