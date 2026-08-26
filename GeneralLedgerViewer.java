import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

/**
 * ============================================================================
 * نظام ERP المصنعي - دفتر الأستاذ العام وميزان المراجعة (GeneralLedgerViewer)
 * ============================================================================
 */
public class GeneralLedgerViewer extends JFrame {

    private JTabbedPane tabbedPane;

    // عناصر تبويب قيود اليومية
    private JTable tblEntries;
    private DefaultTableModel modelEntries;
    private JTable tblLines;
    private DefaultTableModel modelLines;
    private JButton btnRefreshEntries;

    // عناصر تبويب ميزان المراجعة
    private JTable tblTrialBalance;
    private DefaultTableModel modelTrialBalance;
    private JButton btnRefreshTB;

    // شاشات عرض الإجماليات الأربعة في ميزان المراجعة
    private JLabel lblTotalDebitMovements;
    private JLabel lblTotalCreditMovements;
    private JLabel lblTotalDebitBalances;
    private JLabel lblTotalCreditBalances;
    private JLabel lblBalanceStatus;

    private JComboBox<AccountOption> cmbLedgerAccount;
    private JTextField txtLedgerFrom;
    private JTextField txtLedgerTo;
    private DefaultTableModel modelAccountLedger;
    private JTable tblAccountLedger;
    private JTextField txtTrialDate;
    private JTextField txtStatementFrom;
    private JTextField txtStatementTo;
    private JTextField txtStatementDate;
    private DefaultTableModel modelProfitLoss;
    private DefaultTableModel modelBalanceSheet;
    private JLabel lblBalanceSheetStatus;

    private static final Font ARABIC_FONT = new Font("Tahoma", Font.PLAIN, 12);
    private static final Font ARABIC_BOLD = new Font("Tahoma", Font.BOLD, 12);
    private static final Font ARABIC_TITLE = new Font("Tahoma", Font.BOLD, 13);

    public GeneralLedgerViewer() {
        setTitle("نظام ERP المصنعي - دفتر الأستاذ العام وميزان المراجعة (Trial Balance & General Ledger)");
        setSize(1150, 750);
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        initUI();
        loadJournalEntries();
        loadTrialBalance();
        loadProfitLoss();
        loadBalanceSheet();
    }

    private void initUI() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        tabbedPane.setFont(ARABIC_TITLE);

        // 1. تبويب قيود اليومية العامة
        tabbedPane.addTab("📖 قيود اليومية العامة (Journal Entries)", createJournalEntriesPanel());

        // 2. تبويب ميزان المراجعة بالمجاميع والأرصدة
        tabbedPane.addTab("ميزان المراجعة", createTrialBalancePanel());
        tabbedPane.addTab("كشف حساب تفصيلي", createAccountLedgerPanel());
        tabbedPane.addTab("قائمة الدخل", createProfitLossPanel());
        tabbedPane.addTab("الميزانية العمومية", createBalanceSheetPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==========================================
    // 1. لوحة قيود اليومية العامة
    // ==========================================
    private JPanel createJournalEntriesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // شريط علوي
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        topBar.setBackground(new Color(245, 247, 250));
        btnRefreshEntries = new JButton("تحديث القيود");
        btnRefreshEntries.setFont(ARABIC_BOLD);
        btnRefreshEntries.addActionListener(e -> loadJournalEntries());
        topBar.add(btnRefreshEntries);
        panel.add(topBar, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);

        // جدول رؤوس القيود
        String[] headerCols = {"معرف القيد", "رقم القيد", "التاريخ", "المرجع", "المصدر", "البيان والشرح", "إجمالي المدين", "إجمالي الدائن"};
        modelEntries = new DefaultTableModel(headerCols, 0);
        tblEntries = new JTable(modelEntries);
        tblEntries.setFont(ARABIC_FONT);
        tblEntries.getTableHeader().setFont(ARABIC_BOLD);
        tblEntries.setRowHeight(24);
        tblEntries.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        alignTableCells(tblEntries);

        JScrollPane spEntries = new JScrollPane(tblEntries);
        spEntries.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "سندات قيود اليومية المرحلة (journal_entries)",
                TitledBorder.RIGHT, TitledBorder.TOP, ARABIC_BOLD));
        splitPane.setTopComponent(spEntries);

        // جدول سطور القيد
        String[] lineCols = {"رقم الحساب", "اسم الحساب", "البيان", "مدين (Debit)", "دائن (Credit)"};
        modelLines = new DefaultTableModel(lineCols, 0);
        tblLines = new JTable(modelLines);
        tblLines.setFont(ARABIC_FONT);
        tblLines.getTableHeader().setFont(ARABIC_BOLD);
        tblLines.setRowHeight(24);
        tblLines.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        alignTableCells(tblLines);

        JScrollPane spLines = new JScrollPane(tblLines);
        spLines.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "تفاصيل أسطر القيد المحاسبي المزدوج (journal_entry_lines)",
                TitledBorder.RIGHT, TitledBorder.TOP, ARABIC_BOLD));
        splitPane.setBottomComponent(spLines);

        panel.add(splitPane, BorderLayout.CENTER);

        tblEntries.getSelectionModel().addListSelectionListener(e -> {
            int row = tblEntries.getSelectedRow();
            if (row >= 0) {
                long entryId = Long.parseLong(modelEntries.getValueAt(row, 0).toString());
                loadEntryLines(entryId);
            }
        });

        return panel;
    }

    // ==========================================
    // 2. لوحة ميزان المراجعة مع الإجماليات الأربعة
    // ==========================================
    private JPanel createTrialBalancePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // شريط علوي
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8));
        topBar.setBackground(new Color(245, 247, 250));
        btnRefreshTB = new JButton("تحديث ميزان المراجعة");
        btnRefreshTB.setFont(ARABIC_BOLD);
        btnRefreshTB.addActionListener(e -> loadTrialBalance());
        topBar.add(btnRefreshTB);
        topBar.add(new JLabel("حتى تاريخ (YYYY-MM-DD):"));
        txtTrialDate = new JTextField(LocalDate.now().toString(), 10);
        topBar.add(txtTrialDate);
        panel.add(topBar, BorderLayout.NORTH);

        // جدول ميزان المراجعة
        String[] tbCols = {"رقم الحساب", "اسم الحساب المحاسبي", "طبيعة الحساب", "مجموع حركات المدين", "مجموع حركات الدائن", "الرصيد المدين", "الرصيد الدائن"};
        modelTrialBalance = new DefaultTableModel(tbCols, 0);
        tblTrialBalance = new JTable(modelTrialBalance);
        tblTrialBalance.setFont(ARABIC_FONT);
        tblTrialBalance.getTableHeader().setFont(ARABIC_BOLD);
        tblTrialBalance.setRowHeight(26);
        tblTrialBalance.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        alignTableCells(tblTrialBalance);

        JScrollPane spTB = new JScrollPane(tblTrialBalance);
        spTB.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "ميزان المراجعة بالمجاميع والأرصدة (Trial Balance)",
                TitledBorder.RIGHT, TitledBorder.TOP, ARABIC_BOLD));
        panel.add(spTB, BorderLayout.CENTER);

        // لوحة الإجماليات الأربعة ومؤشر التوازن المحاسبي
        JPanel summaryPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        summaryPanel.setBackground(new Color(241, 245, 249));
        summaryPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                new EmptyBorder(8, 15, 8, 15)
        ));

        // الصف الأول: إجمالي المجاميع والحركات (مدين ودائن)
        JPanel rowMovements = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 4));
        rowMovements.setOpaque(false);
        lblTotalDebitMovements = new JLabel("إجمالي حركات المدين: 0.00 YER");
        lblTotalDebitMovements.setFont(ARABIC_BOLD);
        lblTotalDebitMovements.setForeground(new Color(30, 41, 59));

        lblTotalCreditMovements = new JLabel("إجمالي حركات الدائن: 0.00 YER");
        lblTotalCreditMovements.setFont(ARABIC_BOLD);
        lblTotalCreditMovements.setForeground(new Color(30, 41, 59));

        rowMovements.add(lblTotalDebitMovements);
        rowMovements.add(lblTotalCreditMovements);

        // الصف الثاني: إجمالي الأرصدة الختامية + حالة الاتزان
        JPanel rowBalances = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 4));
        rowBalances.setOpaque(false);

        lblTotalDebitBalances = new JLabel("إجمالي الأرصدة المدينة: 0.00 YER");
        lblTotalDebitBalances.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblTotalDebitBalances.setForeground(new Color(37, 99, 235));

        lblTotalCreditBalances = new JLabel("إجمالي الأرصدة الدائنة: 0.00 YER");
        lblTotalCreditBalances.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblTotalCreditBalances.setForeground(new Color(217, 119, 6));

        lblBalanceStatus = new JLabel("حالة الميزان: متزن");
        lblBalanceStatus.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblBalanceStatus.setForeground(new Color(16, 185, 129));

        rowBalances.add(lblTotalDebitBalances);
        rowBalances.add(lblTotalCreditBalances);
        rowBalances.add(lblBalanceStatus);

        summaryPanel.add(rowMovements);
        summaryPanel.add(rowBalances);

        panel.add(summaryPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createAccountLedgerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        filters.add(new JLabel("الحساب الفرعي:"));
        cmbLedgerAccount = new JComboBox<AccountOption>();
        cmbLedgerAccount.setPreferredSize(new Dimension(300, 28));
        filters.add(cmbLedgerAccount);
        filters.add(new JLabel("من تاريخ:"));
        txtLedgerFrom = new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 10);
        filters.add(txtLedgerFrom);
        filters.add(new JLabel("إلى تاريخ:"));
        txtLedgerTo = new JTextField(LocalDate.now().toString(), 10);
        filters.add(txtLedgerTo);
        JButton load = new JButton("عرض الكشف");
        load.addActionListener(e -> loadAccountLedger());
        filters.add(load);
        panel.add(filters, BorderLayout.NORTH);

        String[] columns = {"التاريخ", "نوع المستند", "رقم المستند", "البيان", "مدين", "دائن", "الرصيد التراكمي"};
        modelAccountLedger = new DefaultTableModel(columns, 0);
        tblAccountLedger = new JTable(modelAccountLedger);
        configureReportTable(tblAccountLedger);
        panel.add(new JScrollPane(tblAccountLedger), BorderLayout.CENTER);
        loadAccounts();
        return panel;
    }

    private JPanel createProfitLossPanel() {
        JPanel panel = createStatementPanel("قائمة الدخل");
        modelProfitLoss = new DefaultTableModel(new String[]{"البند", "مدين", "دائن", "الصافي"}, 0);
        JTable table = new JTable(modelProfitLoss);
        configureReportTable(table);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBalanceSheetPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        filters.add(new JLabel("كما في تاريخ (YYYY-MM-DD):"));
        txtStatementDate = new JTextField(LocalDate.now().toString(), 10);
        filters.add(txtStatementDate);
        JButton load = new JButton("احتساب الميزانية");
        load.addActionListener(e -> loadBalanceSheet());
        filters.add(load);
        panel.add(filters, BorderLayout.NORTH);
        modelBalanceSheet = new DefaultTableModel(new String[]{"التصنيف", "الرصيد"}, 0);
        JTable table = new JTable(modelBalanceSheet);
        configureReportTable(table);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        lblBalanceSheetStatus = new JLabel("المعادلة المحاسبية: لم تحتسب بعد");
        lblBalanceSheetStatus.setFont(ARABIC_BOLD);
        panel.add(lblBalanceSheetStatus, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createStatementPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        filters.add(new JLabel("من تاريخ:"));
        txtStatementFrom = new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 10);
        filters.add(txtStatementFrom);
        filters.add(new JLabel("إلى تاريخ:"));
        txtStatementTo = new JTextField(LocalDate.now().toString(), 10);
        filters.add(txtStatementTo);
        JButton load = new JButton("احتساب " + title);
        load.addActionListener(e -> loadProfitLoss());
        filters.add(load);
        panel.add(filters, BorderLayout.NORTH);
        return panel;
    }

    private void configureReportTable(JTable table) {
        table.setFont(ARABIC_FONT);
        table.getTableHeader().setFont(ARABIC_BOLD);
        table.setRowHeight(25);
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        alignTableCells(table);
    }

    private void loadAccounts() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement statement = conn.prepareStatement(
                     "SELECT account_code, account_name FROM chart_of_accounts " +
                     "WHERE is_sub_account = 1 ORDER BY account_code")) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    cmbLedgerAccount.addItem(new AccountOption(result.getString(1), result.getString(2)));
                }
            }
        } catch (Exception exception) {
            showReportError("تعذر تحميل الحسابات الفرعية", exception);
        }
    }

    private void loadAccountLedger() {
        AccountOption account = (AccountOption) cmbLedgerAccount.getSelectedItem();
        if (account == null) return;
        String from = txtLedgerFrom.getText().trim();
        String to = txtLedgerTo.getText().trim();
        if (!isValidDateRange(from, to)) return;
        modelAccountLedger.setRowCount(0);
        double balance = 0.0;
        String openingSql = "SELECT COALESCE(SUM(l.debit_amount - l.credit_amount), 0) " +
                "FROM journal_entry_lines l JOIN journal_entries e ON e.entry_id = l.entry_id " +
                "WHERE l.account_code = ? AND e.entry_date < ?";
        String movementSql = "SELECT e.entry_date, e.source_module, e.entry_number, " +
                "COALESCE(l.line_narration, e.narration), l.debit_amount, l.credit_amount " +
                "FROM journal_entry_lines l JOIN journal_entries e ON e.entry_id = l.entry_id " +
                "WHERE l.account_code = ? AND e.entry_date BETWEEN ? AND ? " +
                "ORDER BY e.entry_date, e.entry_id, l.line_id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement opening = conn.prepareStatement(openingSql);
             PreparedStatement movements = conn.prepareStatement(movementSql)) {
            opening.setString(1, account.code);
            opening.setString(2, from);
            try (ResultSet result = opening.executeQuery()) {
                if (result.next()) balance = result.getDouble(1);
            }
            if (Math.abs(balance) > 0.0001) {
                modelAccountLedger.addRow(new Object[]{from, "رصيد افتتاحي", "-", account.name,
                        "-", "-", formatAmount(balance)});
            }
            movements.setString(1, account.code);
            movements.setString(2, from);
            movements.setString(3, to);
            try (ResultSet result = movements.executeQuery()) {
                while (result.next()) {
                    double debit = result.getDouble(5);
                    double credit = result.getDouble(6);
                    balance += debit - credit;
                    modelAccountLedger.addRow(new Object[]{result.getString(1),
                            result.getString(2), result.getString(3), result.getString(4),
                            formatAmount(debit), formatAmount(credit), formatAmount(balance)});
                }
            }
        } catch (Exception exception) {
            showReportError("تعذر تحميل كشف الحساب", exception);
        }
    }

    private void loadProfitLoss() {
        if (modelProfitLoss == null || !isValidDateRange(txtStatementFrom.getText().trim(), txtStatementTo.getText().trim())) return;
        modelProfitLoss.setRowCount(0);
        double revenue = 0.0;
        double cogs = 0.0;
        double operating = 0.0;
        String sql = "SELECT a.account_type, a.account_code, a.account_name, " +
                "COALESCE(SUM(l.debit_amount), 0), COALESCE(SUM(l.credit_amount), 0) " +
                "FROM chart_of_accounts a JOIN journal_entry_lines l ON l.account_code = a.account_code " +
                "JOIN journal_entries e ON e.entry_id = l.entry_id " +
                "WHERE a.is_sub_account = 1 AND e.entry_date BETWEEN ? AND ? " +
                "AND a.account_type IN ('REVENUE', 'EXPENSE') " +
                "GROUP BY a.account_type, a.account_code, a.account_name ORDER BY a.account_code";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, txtStatementFrom.getText().trim());
            statement.setString(2, txtStatementTo.getText().trim());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    double debit = result.getDouble(4);
                    double credit = result.getDouble(5);
                    double net = "REVENUE".equals(result.getString(1)) ? credit - debit : debit - credit;
                    if ("REVENUE".equals(result.getString(1))) revenue += net;
                    else if (result.getString(2).startsWith("51")) cogs += net;
                    else operating += net;
                }
            }
            double gross = revenue - cogs;
            double net = gross - operating;
            modelProfitLoss.addRow(new Object[]{"إجمالي الإيرادات والمبيعات", "-", formatAmount(revenue), formatAmount(revenue)});
            modelProfitLoss.addRow(new Object[]{"تكلفة المبيعات (COGS)", formatAmount(cogs), "-", formatAmount(-cogs)});
            modelProfitLoss.addRow(new Object[]{"مجمل الربح", "-", "-", formatAmount(gross)});
            modelProfitLoss.addRow(new Object[]{"المصروفات التشغيلية والإدارية والعمومية", formatAmount(operating), "-", formatAmount(-operating)});
            modelProfitLoss.addRow(new Object[]{"صافي الربح / (الخسارة)", "-", "-", formatAmount(net)});
        } catch (Exception exception) {
            showReportError("تعذر احتساب قائمة الدخل", exception);
        }
    }

    private void loadBalanceSheet() {
        if (modelBalanceSheet == null || !isValidDate(txtStatementDate.getText().trim())) return;
        modelBalanceSheet.setRowCount(0);
        double assets = 0.0;
        double liabilities = 0.0;
        double equity = 0.0;
        String sql = "SELECT a.account_type, COALESCE(SUM(l.debit_amount - l.credit_amount), 0) " +
                "FROM chart_of_accounts a JOIN journal_entry_lines l ON l.account_code = a.account_code " +
                "JOIN journal_entries e ON e.entry_id = l.entry_id " +
                "WHERE a.is_sub_account = 1 AND e.entry_date <= ? " +
                "AND a.account_type IN ('ASSET', 'LIABILITY', 'EQUITY') GROUP BY a.account_type";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, txtStatementDate.getText().trim());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    double signed = result.getDouble(2);
                    if ("ASSET".equals(result.getString(1))) assets += signed;
                    else if ("LIABILITY".equals(result.getString(1))) liabilities -= signed;
                    else equity -= signed;
                }
            }
            double retainedProfit = calculateProfit(txtStatementDate.getText().trim());
            equity += retainedProfit;
            modelBalanceSheet.addRow(new Object[]{"الأصول المتداولة والثابتة", formatAmount(assets)});
            modelBalanceSheet.addRow(new Object[]{"الخصوم والالتزامات", formatAmount(liabilities)});
            modelBalanceSheet.addRow(new Object[]{"حقوق الملكية", formatAmount(equity)});
            modelBalanceSheet.addRow(new Object[]{"إجمالي الخصوم + حقوق الملكية", formatAmount(liabilities + equity)});
            boolean balanced = Math.abs(assets - liabilities - equity) < 0.01;
            lblBalanceSheetStatus.setText(balanced ? "المعادلة المحاسبية: متطابقة" :
                    String.format("المعادلة غير متطابقة، الفرق: %,.2f YER", assets - liabilities - equity));
            lblBalanceSheetStatus.setForeground(balanced ? new Color(16, 185, 129) : new Color(220, 38, 38));
        } catch (Exception exception) {
            showReportError("تعذر احتساب الميزانية العمومية", exception);
        }
    }

    private double calculateProfit(String to) throws Exception {
        String sql = "SELECT a.account_type, a.account_code, SUM(l.debit_amount), SUM(l.credit_amount) " +
                "FROM chart_of_accounts a JOIN journal_entry_lines l ON l.account_code = a.account_code " +
                "JOIN journal_entries e ON e.entry_id = l.entry_id WHERE a.is_sub_account = 1 " +
                "AND e.entry_date <= ? AND a.account_type IN ('REVENUE','EXPENSE') " +
                "GROUP BY a.account_type, a.account_code";
        double revenue = 0.0, expense = 0.0;
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, to);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    double debit = result.getDouble(3), credit = result.getDouble(4);
                    if ("REVENUE".equals(result.getString(1))) revenue += credit - debit;
                    else expense += debit - credit;
                }
            }
        }
        return revenue - expense;
    }

    private boolean isValidDate(String value) {
        try { LocalDate.parse(value); return true; }
        catch (Exception exception) { showReportError("صيغة التاريخ يجب أن تكون YYYY-MM-DD", exception); return false; }
    }

    private boolean isValidDateRange(String from, String to) {
        return isValidDate(from) && isValidDate(to) && !from.isEmpty() && from.compareTo(to) <= 0;
    }

    private String formatAmount(double amount) { return String.format("%,.2f", amount); }

    private void showReportError(String message, Exception exception) {
        JOptionPane.showMessageDialog(this, message + ": " + exception.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
    }

    private static class AccountOption {
        private final String code;
        private final String name;
        AccountOption(String code, String name) { this.code = code; this.name = name; }
        @Override public String toString() { return code + " - " + name; }
    }

    private void loadJournalEntries() {
        modelEntries.setRowCount(0);
        modelLines.setRowCount(0);
        String sql = "SELECT entry_id, entry_number, entry_date, reference_doc, source_module, narration, total_debit, total_credit " +
                     "FROM journal_entries ORDER BY entry_id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                modelEntries.addRow(new Object[]{
                        rs.getLong("entry_id"),
                        rs.getString("entry_number"),
                        rs.getString("entry_date"),
                        rs.getString("reference_doc"),
                        rs.getString("source_module"),
                        rs.getString("narration"),
                        String.format("%,.2f", rs.getDouble("total_debit")),
                        String.format("%,.2f", rs.getDouble("total_credit"))
                });
            }
            if (modelEntries.getRowCount() > 0) {
                tblEntries.setRowSelectionInterval(0, 0);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في قراءة القيود: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadEntryLines(long entryId) {
        modelLines.setRowCount(0);
        String sql = "SELECT l.account_code, a.account_name, l.line_narration, l.debit_amount, l.credit_amount " +
                     "FROM journal_entry_lines l " +
                     "LEFT JOIN chart_of_accounts a ON l.account_code = a.account_code " +
                     "WHERE l.entry_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, entryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    modelLines.addRow(new Object[]{
                            rs.getString("account_code"),
                            rs.getString("account_name"),
                            rs.getString("line_narration"),
                            String.format("%,.2f", rs.getDouble("debit_amount")),
                            String.format("%,.2f", rs.getDouble("credit_amount"))
                    });
                }
            }
        } catch (Exception ex) {
            System.err.println("خطأ في جلب سطور القيد: " + ex.getMessage());
        }
    }

    private void loadTrialBalance() {
        modelTrialBalance.setRowCount(0);

        String sql = "SELECT a.account_code, a.account_name, a.account_type, " +
                     "COALESCE(SUM(l.debit_amount), 0) AS total_debit, " +
                     "COALESCE(SUM(l.credit_amount), 0) AS total_credit " +
                     "FROM chart_of_accounts a " +
                     "LEFT JOIN journal_entry_lines l ON a.account_code = l.account_code " +
                     "AND EXISTS (SELECT 1 FROM journal_entries e WHERE e.entry_id = l.entry_id AND e.entry_date <= ?) " +
                     "WHERE a.is_sub_account = 1 " +
                     "GROUP BY a.account_code, a.account_name, a.account_type " +
                     "ORDER BY a.account_code ASC";

        double sumDebitMovements = 0.0;
        double sumCreditMovements = 0.0;
        double sumDebitBalances = 0.0;
        double sumCreditBalances = 0.0;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
               ResultSet rs = createTrialBalanceResultSet(pstmt)) {

            while (rs.next()) {
                String code = rs.getString("account_code");
                String name = rs.getString("account_name");
                String type = rs.getString("account_type");
                double debit = rs.getDouble("total_debit");
                double credit = rs.getDouble("total_credit");

                sumDebitMovements += debit;
                sumCreditMovements += credit;

                double balDebit = 0.0;
                double balCredit = 0.0;

                // احتساب الرصيد حسب طبيعة الحساب
                // الأصول (1) والمصروفات (5) طبيعتها مدينة
                // الخصوم (2) والإيرادات (4) طبيعتها دائنة
                if (code.startsWith("1") || code.startsWith("5")) {
                    double net = debit - credit;
                    if (net >= 0) balDebit = net;
                    else balCredit = -net;
                } else {
                    double net = credit - debit;
                    if (net >= 0) balCredit = net;
                    else balDebit = -net;
                }

                sumDebitBalances += balDebit;
                sumCreditBalances += balCredit;

                // إدراج السطر فقط إذا كان عليه حركة أو رصيد
                modelTrialBalance.addRow(new Object[]{
                        code,
                        name,
                        getArabicAccountType(type),
                        String.format("%,.2f", debit),
                        String.format("%,.2f", credit),
                        String.format("%,.2f", balDebit),
                        String.format("%,.2f", balCredit)
                });
            }

            // سطر الإجمالي الختامي في أسفل الجدول (Total Summary Row)
            modelTrialBalance.addRow(new Object[]{
                    "---",
                    "الإجمالي العام لميزان المراجعة",
                    "---",
                    String.format("%,.2f", sumDebitMovements),
                    String.format("%,.2f", sumCreditMovements),
                    String.format("%,.2f", sumDebitBalances),
                    String.format("%,.2f", sumCreditBalances)
            });

            // تحديث بطاقات الإجماليات الأربعة
            lblTotalDebitMovements.setText(String.format("إجمالي حركات المدين: %,.2f YER", sumDebitMovements));
            lblTotalCreditMovements.setText(String.format("إجمالي حركات الدائن: %,.2f YER", sumCreditMovements));
            lblTotalDebitBalances.setText(String.format("إجمالي الأرصدة المدينة: %,.2f YER", sumDebitBalances));
            lblTotalCreditBalances.setText(String.format("إجمالي الأرصدة الدائنة: %,.2f YER", sumCreditBalances));

            // فحص توازن الحركات وتوازن الأرصدة
            boolean movementsBalanced = Math.abs(sumDebitMovements - sumCreditMovements) < 0.01;
            boolean balancesBalanced = Math.abs(sumDebitBalances - sumCreditBalances) < 0.01;

            if (movementsBalanced && balancesBalanced) {
                lblBalanceStatus.setText("حالة الميزان: متزن");
                lblBalanceStatus.setForeground(new Color(16, 185, 129));
            } else {
                lblBalanceStatus.setText(String.format("فارق غير متزن: %,.2f YER", Math.abs(sumDebitBalances - sumCreditBalances)));
                lblBalanceStatus.setForeground(new Color(239, 68, 68));
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في احتساب ميزان المراجعة: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ResultSet createTrialBalanceResultSet(PreparedStatement statement) throws Exception {
        String date = txtTrialDate == null ? LocalDate.now().toString() : txtTrialDate.getText().trim();
        if (!isValidDate(date)) throw new IllegalArgumentException("تاريخ ميزان المراجعة غير صالح");
        statement.setString(1, date);
        return statement.executeQuery();
    }

    private String getArabicAccountType(String type) {
        if (type == null) return "";
        switch (type) {
            case "ASSET": return "أصول";
            case "LIABILITY": return "خصوم والتزامات";
            case "EQUITY": return "حقوق ملكية";
            case "REVENUE": return "إيرادات ومبيعات";
            case "EXPENSE": return "مصروفات وتكاليف";
            default: return type;
        }
    }

    private void alignTableCells(JTable table) {
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new GeneralLedgerViewer().setVisible(true);
        });
    }
}