import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.List;

/**
 * ============================================================================
 * نظام ERP المصنعي - نافذة شجرة الحسابات التفاعلية والمركزية (AccountTreeDialog)
 * ============================================================================
 * تدعم:
 * 1. الإضافة (حساب موازي بنفس المستوى / حساب متفرع ابن)
 * 2. التعديل والحذف الآمن برمجياً
 * 3. التكبير والتصغير المتجاوب
 * 4. التوافقية الشاملة مع ملف AccountsData.txt وقاعدة بيانات MySQL
 */
public class AccountTreeDialog extends JDialog {

    private static final String ACCOUNTS_FILE = "AccountsData.txt";

    // كائن يمثل بيانات الحساب في الشجرة
    public static class AccountNodeData {
        private String code;
        private String name;
        private int level;
        private String type; // "رئيسي" أو "فرعي"
        private String parentCode;
        private double balance;

        public AccountNodeData(String code, String name, int level, String type, String parentCode, double balance) {
            this.code = code;
            this.name = name;
            this.level = level;
            this.type = type;
            this.parentCode = parentCode;
            this.balance = balance;
        }

        public String getCode() { return code; }
        public void setName(String name) { this.name = name; }
        public String getName() { return name; }
        public int getLevel() { return level; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getParentCode() { return parentCode; }
        public double getBalance() { return balance; }
        public boolean isSubAccount() { return "فرعي".equals(type); }

        @Override
        public String toString() {
            return code + " - " + name;
        }
    }

    private JTree treeAccounts;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    
    private JTextField txtSearch;
    private JLabel lblSelectedCode;
    private JLabel lblSelectedName;
    private JLabel lblSelectedLevel;
    private JLabel lblSelectedType;
    private JLabel lblSelectedBalance;
    private JLabel lblValidationStatus;

    private JButton btnCancel;

    private List<AccountNodeData> accountList;
    private AccountNodeData selectedAccount;
    private boolean confirmed = false;
    private String filterRootCode = null;
    private boolean managementMode = true;
    private boolean maximized = false;
    private Rectangle normalBounds;

    // =========================================================================
    // المشيدات الشاملة لجميع أنواع المكونات والنوافذ (Constructors)
    // =========================================================================

    public AccountTreeDialog() {
        this((Window) null, (String) null);
    }

    public AccountTreeDialog(Frame parent) {
        this((Window) parent, (String) null);
    }

    public AccountTreeDialog(Dialog parent) {
        this((Window) parent, (String) null);
    }

    public AccountTreeDialog(Component parent) {
        this(getWindowForComponent(parent), (String) null);
    }

    public AccountTreeDialog(Frame parent, String filterPrefix) {
        this((Window) parent, filterPrefix);
    }

    public AccountTreeDialog(Dialog parent, String filterPrefix) {
        this((Window) parent, filterPrefix);
    }

    public AccountTreeDialog(Component parent, String filterPrefix) {
        this(getWindowForComponent(parent), filterPrefix);
    }

    /**
     * مشيد مخصص لاستقبال القوائم الخارجية من ItemManagementForm و TreasuryForm و MainWindow
     */
    public AccountTreeDialog(Component parent, List<?> rawList) {
        this(getWindowForComponent(parent), (String) null);
        managementMode = false;
        if (rawList != null && !rawList.isEmpty()) {
            parseAndMergeExternalList(rawList);
            buildTree("");
        }
    }

    /**
     * المشيد الأساسي الذي يستقبل Window المباشر
     */
    public AccountTreeDialog(Window owner, String filterPrefix) {
        super(owner, "دليل شجرة الحسابات المركزية (Chart of Accounts)", ModalityType.MODELESS);
        this.filterRootCode = filterPrefix;
        this.managementMode = filterPrefix == null;
        this.accountList = new ArrayList<>();

        // ضبط الحجم ليتناسب مع شاشة العرض دون أن يختفي الشريط السفلي
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(1020, screenSize.width - 60);
        int height = Math.min(640, screenSize.height - 100);
        setSize(width, height);
        setMinimumSize(new Dimension(800, 480));
        setResizable(true); // تفعيل خاصية التكبير والتصغير للشاشة
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        initUI();
        loadAccountsData();
        buildTree("");
        setupEvents();
    }

    private static Window getWindowForComponent(Component comp) {
        if (comp == null) return null;
        if (comp instanceof Window) return (Window) comp;
        return SwingUtilities.getWindowAncestor(comp);
    }

    private void initUI() {
        // الشريط العلوي (البحث والتصفية + شريط أزرار العمليات لضمان ظهوره دائماً)
        JPanel topContainer = new JPanel(new BorderLayout(6, 6));
        topContainer.setBorder(new EmptyBorder(8, 12, 6, 12));
        topContainer.setBackground(new Color(245, 247, 250));

        JPanel searchRow = new JPanel(new BorderLayout(8, 8));
        searchRow.setOpaque(false);
        JLabel lblSearch = new JLabel("بحث في شجرة الحسابات:");
        lblSearch.setFont(new Font("Tahoma", Font.BOLD, 12));

        txtSearch = new JTextField();
        txtSearch.setFont(new Font("Tahoma", Font.PLAIN, 12));
        txtSearch.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        searchRow.add(lblSearch, BorderLayout.EAST);
        searchRow.add(txtSearch, BorderLayout.CENTER);
        topContainer.add(searchRow, BorderLayout.NORTH);

        JButton btnMaximize = new JButton("□");
        btnMaximize.setToolTipText("تكبير / استعادة");
        btnMaximize.setFont(new Font("Tahoma", Font.BOLD, 14));
        btnMaximize.setPreferredSize(new Dimension(36, 28));
        btnMaximize.setMargin(new Insets(0, 0, 0, 0));
        btnMaximize.addActionListener(e -> toggleMaximize());
        topContainer.add(btnMaximize, BorderLayout.WEST);
        add(topContainer, BorderLayout.NORTH);

        // المحتوى الرئيسي (الشجرة + تفاصيل الحساب المحدد)
        rootNode = new DefaultMutableTreeNode("دليل الحسابات المصنعي");
        treeModel = new DefaultTreeModel(rootNode);
        treeAccounts = new JTree(treeModel);
        treeAccounts.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        treeAccounts.setFont(new Font("Tahoma", Font.PLAIN, 12));
        treeAccounts.setRowHeight(24);
        treeAccounts.setCellRenderer(new AccountTreeCellRenderer());

        JScrollPane scrollTree = new JScrollPane(treeAccounts);
        scrollTree.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 205, 215)),
                "الهيكل الشجري للحسابات",
                TitledBorder.RIGHT,
                TitledBorder.TOP,
                new Font("Tahoma", Font.BOLD, 12)
        ));
        add(scrollTree, BorderLayout.CENTER);

        // الشريط السفلي يقتصر على الإغلاق
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setBackground(new Color(248, 250, 252));
        bottomBar.setBorder(new EmptyBorder(4, 15, 6, 15));

        btnCancel = new JButton("إغلاق");
        btnCancel.setFont(new Font("Tahoma", Font.PLAIN, 12));
        bottomBar.add(btnCancel);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel("إدارة الحساب");
        title.setFont(new Font("Tahoma", Font.BOLD, 14));
        title.setForeground(new Color(30, 41, 59));
        title.setAlignmentX(Component.RIGHT_ALIGNMENT);

        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        lblSelectedCode = createDetailRow(panel, "كود الحساب:", "---");
        lblSelectedName = createDetailRow(panel, "اسم الحساب:", "لم يتم التحديد");
        lblSelectedLevel = createDetailRow(panel, "المستوى:", "---");
        lblSelectedType = createDetailRow(panel, "تصنيف الحساب:", "---");
        lblSelectedBalance = createDetailRow(panel, "الرصيد الدفتري:", "0.00 YER");

        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        lblValidationStatus = new JLabel(" ");
        lblValidationStatus.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblValidationStatus.setForeground(new Color(100, 116, 139));
        lblValidationStatus.setAlignmentX(Component.RIGHT_ALIGNMENT);
        panel.add(lblValidationStatus);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JLabel createDetailRow(JPanel parent, String labelText, String defaultValue) {
        JPanel row = new JPanel(new BorderLayout(5, 5));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row.setBackground(Color.WHITE);
        row.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JLabel lblTitle = new JLabel(labelText);
        lblTitle.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblTitle.setForeground(new Color(71, 85, 105));

        JLabel lblValue = new JLabel(defaultValue);
        lblValue.setFont(new Font("Tahoma", Font.PLAIN, 12));
        lblValue.setForeground(new Color(15, 23, 42));

        row.add(lblTitle, BorderLayout.EAST);
        row.add(lblValue, BorderLayout.CENTER);
        parent.add(row);
        parent.add(Box.createRigidArea(new Dimension(0, 6)));
        return lblValue;
    }

    private void setupEvents() {
        treeAccounts.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) treeAccounts.getLastSelectedPathComponent();
            if (selectedNode == null || !(selectedNode.getUserObject() instanceof AccountNodeData)) {
                resetDetails();
                return;
            }
            selectedAccount = (AccountNodeData) selectedNode.getUserObject();
            updateDetailsPanel(selectedAccount);
        });

        treeAccounts.addMouseListener(new MouseAdapter() {
            private void showActions(MouseEvent e) {
                TreePath path = treeAccounts.getPathForLocation(e.getX(), e.getY());
                if (path != null && ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject() instanceof AccountNodeData) {
                    treeAccounts.setSelectionPath(path);
                    showAccountActionsMenu(treeAccounts, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (managementMode && e.getClickCount() == 1 && !e.isPopupTrigger()) {
                    showActions(e);
                } else if (!managementMode && e.getClickCount() == 2 && selectedAccount != null) {
                    handleSelectAccount();
                }
            }
        });

        btnCancel.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filter(); }

            private void filter() {
                buildTree(txtSearch.getText().trim());
            }
        });
    }

    private void updateDetailsPanel(AccountNodeData acc) {
        lblSelectedCode.setText(acc.getCode());
        lblSelectedName.setText(acc.getName());
        lblSelectedLevel.setText("مستوى " + acc.getLevel());
        lblSelectedType.setText(acc.getType() + (acc.isSubAccount() ? " (يقبل القيود)" : " (تجميعي)"));
        lblSelectedBalance.setText(String.format("%,.2f YER", acc.getBalance()));

        if (acc.isSubAccount()) {
            lblValidationStatus.setText(" ");
            lblValidationStatus.setForeground(new Color(16, 185, 129));
        } else {
            lblValidationStatus.setText(" ");
            lblValidationStatus.setForeground(new Color(100, 116, 139));
        }
    }

    private void resetDetails() {
        selectedAccount = null;
        lblSelectedCode.setText("---");
        lblSelectedName.setText("لم يتم التحديد");
        lblSelectedLevel.setText("---");
        lblSelectedType.setText("---");
        lblSelectedBalance.setText("0.00 YER");
        lblValidationStatus.setText(" ");
        lblValidationStatus.setForeground(new Color(100, 116, 139));
    }

    private void handleSelectAccount() {
        if (selectedAccount == null) {
            JOptionPane.showMessageDialog(this, "يرجى تحديد حساب من الشجرة أولاً.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!selectedAccount.isSubAccount()) {
            JOptionPane.showMessageDialog(this,
                    "لا يمكن استخدام الحساب الرئيسي في حركة مالية.",
                    "تنبيه",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        confirmed = true;
        dispose();
    }

    private void showAccountActionsMenu(Component invoker, int x, int y) {
        if (!managementMode || selectedAccount == null || !isShowing()) return;
        JPopupMenu menu = new JPopupMenu();
        JMenuItem add = new JMenuItem("إضافة");
        JMenuItem edit = new JMenuItem("تعديل");
        JMenuItem delete = new JMenuItem("حذف");
        add.addActionListener(e -> openAddAccountDialog());
        edit.addActionListener(e -> openEditAccountDialog());
        delete.addActionListener(e -> handleDeleteAccount());
        menu.add(add);
        menu.add(edit);
        menu.add(delete);
        menu.show(invoker, x, y + 4);
    }

    private void toggleMaximize() {
        if (!maximized) {
            normalBounds = getBounds();
            GraphicsConfiguration configuration = getGraphicsConfiguration();
            setBounds(configuration.getBounds());
            maximized = true;
        } else {
            if (normalBounds != null) setBounds(normalBounds);
            maximized = false;
        }
    }

    private void parseAndMergeExternalList(List<?> list) {
        for (Object item : list) {
            if (item == null) continue;
            String text = item.toString();
            String[] parts = text.split("[-|]");
            if (parts.length >= 2) {
                String code = parts[0].trim();
                String name = parts[1].trim();
                boolean exists = accountList.stream().anyMatch(a -> a.getCode().equals(code));
                if (!exists) {
                    accountList.add(new AccountNodeData(code, name, 6, "فرعي", "", 0.0));
                }
            }
        }
    }

    private void loadAccountsData() {
        accountList.clear();

        // 1. محاولة القراءة من MySQL أولاً
        boolean loadedFromDb = false;
        String sql = "SELECT account_code, account_name, account_level, is_sub_account, parent_code, current_balance FROM chart_of_accounts ORDER BY account_code ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String code = rs.getString("account_code");
                String name = rs.getString("account_name");
                int level = rs.getInt("account_level");
                boolean isSub = rs.getBoolean("is_sub_account");
                String parent = rs.getString("parent_code");
                double bal = rs.getDouble("current_balance");

                if (filterRootCode == null || code.startsWith(filterRootCode)) {
                    accountList.add(new AccountNodeData(code, name, level, isSub ? "فرعي" : "رئيسي", parent, bal));
                }
                loadedFromDb = true;
            }
        } catch (Exception ignored) {}

        // 2. إذا لم تكن قاعدة البيانات متاحة، نقرأ من ملف AccountsData.txt
        if (!loadedFromDb || accountList.isEmpty()) {
            File file = new File(ACCOUNTS_FILE);
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty() || line.startsWith("#")) continue;
                        String[] parts = line.split("\\|");
                        if (parts.length >= 6) {
                            String code = parts[0].trim();
                            String name = parts[1].trim();
                            int level = Integer.parseInt(parts[2].trim());
                            String type = parts[3].trim();
                            String parentCode = parts[4].trim();
                            double balance = Double.parseDouble(parts[5].trim());

                            if (filterRootCode == null || code.startsWith(filterRootCode)) {
                                accountList.add(new AccountNodeData(code, name, level, type, parentCode, balance));
                            }
                        } else if (line.contains(" - ")) {
                            String[] displayParts = line.split(" - ", 2);
                            String code = displayParts[0].trim();
                            String displayName = displayParts[1].trim();
                            if (code.isEmpty() || !code.matches("\\d+")) continue;

                            boolean isSub = displayName.contains("حساب فرعي");
                            int level = displayName.contains("مستوى ")
                                    ? parseLevel(displayName)
                                    : code.length();
                            String name = displayName.replaceFirst("\\s*\\(حساب.*\\)$", "").trim();
                            String parentCode = inferParentCode(code);
                            if (filterRootCode == null || code.startsWith(filterRootCode)) {
                                accountList.add(new AccountNodeData(code, name, level, isSub ? "فرعي" : "رئيسي", parentCode, 0.0));
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("تحذير: خطأ في قراءة AccountsData.txt: " + e.getMessage());
                }
            }
        }

        if (accountList.isEmpty()) {
            populateDefaultAccounts();
        }
    }

    private void populateDefaultAccounts() {
        accountList.add(new AccountNodeData("1", "الأصول", 1, "رئيسي", "", 0));
        accountList.add(new AccountNodeData("12", "الأصول المتداولة", 2, "رئيسي", "1", 0));
        accountList.add(new AccountNodeData("121", "المخزون", 3, "رئيسي", "12", 0));
        accountList.add(new AccountNodeData("12103", "مخزون الإنتاج التام", 4, "رئيسي", "121", 0));
        accountList.add(new AccountNodeData("1210301", "مخزن المنتجات التامة الرئيسي", 6, "فرعي", "12103", 5500000));

        accountList.add(new AccountNodeData("123", "العملاء والمدينون", 3, "رئيسي", "12", 0));
        accountList.add(new AccountNodeData("12302", "عملاء التجاريين محلي", 4, "رئيسي", "123", 0));
        accountList.add(new AccountNodeData("123020001", "شركة الأمل للتوزيع والتجارة", 6, "فرعي", "12302", 845000));
        accountList.add(new AccountNodeData("123020002", "مؤسسة النور والبركة للتجارة", 6, "فرعي", "12302", 320000));

        accountList.add(new AccountNodeData("2", "الالتزامات وحقوق الملكية", 1, "رئيسي", "", 0));
        accountList.add(new AccountNodeData("22", "الالتزامات المتداولة", 2, "رئيسي", "2", 0));
        accountList.add(new AccountNodeData("2203", "الأمانات الضريبية", 3, "رئيسي", "22", 0));
        accountList.add(new AccountNodeData("220301", "أمانات ضريبة المبيعات والقيمة المضافة", 6, "فرعي", "2203", 145000));

        accountList.add(new AccountNodeData("4", "الإيرادات والمبيعات", 1, "رئيسي", "", 0));
        accountList.add(new AccountNodeData("41", "إيرادات النشاط الجاري", 2, "رئيسي", "4", 0));
        accountList.add(new AccountNodeData("4101", "مبيعات المنتجات التامة", 3, "رئيسي", "41", 0));
        accountList.add(new AccountNodeData("410101", "مبيعات محليات وأغذية تامة", 6, "فرعي", "4101", 12500000));
        accountList.add(new AccountNodeData("4102", "مردودات ومسموحات المبيعات", 3, "رئيسي", "41", 0));
        accountList.add(new AccountNodeData("410201", "مردودات مبيعات المنتجات التامة", 6, "فرعي", "4102", 0));

        accountList.add(new AccountNodeData("5", "المصروفات وتكلفة الإنتاج", 1, "رئيسي", "", 0));
        accountList.add(new AccountNodeData("51", "تكلفة البضاعة والإنتاج المباع", 2, "رئيسي", "5", 0));
        accountList.add(new AccountNodeData("5101", "تكلفة البضاعة المباعة COGS", 3, "رئيسي", "51", 0));
        accountList.add(new AccountNodeData("510101", "تكلفة مبيعات المنتجات التامة المصنعة", 6, "فرعي", "5101", 8200000));
    }

    private int parseLevel(String displayLine) {
        try {
            int start = displayLine.indexOf("مستوى ") + 6;
            int end = displayLine.indexOf(')', start);
            return Integer.parseInt(displayLine.substring(start, end > start ? end : displayLine.length()).trim());
        } catch (Exception ignored) {
            return 1;
        }
    }

    private String inferParentCode(String code) {
        String parent = "";
        for (AccountNodeData account : accountList) {
            if (code.startsWith(account.getCode()) && account.getCode().length() < code.length()
                    && account.getCode().length() > parent.length()) {
                parent = account.getCode();
            }
        }
        return parent;
    }

    private void buildTree(String searchText) {
        rootNode.removeAllChildren();
        Map<String, DefaultMutableTreeNode> nodeMap = new HashMap<>();

        for (AccountNodeData acc : accountList) {
            boolean matches = searchText.isEmpty() ||
                    acc.getCode().contains(searchText) ||
                    acc.getName().toLowerCase().contains(searchText.toLowerCase());

            if (!matches && !searchText.isEmpty()) continue;

            DefaultMutableTreeNode node = new DefaultMutableTreeNode(acc);
            nodeMap.put(acc.getCode(), node);

            if (acc.getParentCode() == null || acc.getParentCode().isEmpty() || !nodeMap.containsKey(acc.getParentCode())) {
                rootNode.add(node);
            } else {
                DefaultMutableTreeNode parentNode = nodeMap.get(acc.getParentCode());
                parentNode.add(node);
            }
        }

        treeModel.reload();
        for (int i = 0; i < treeAccounts.getRowCount(); i++) {
            treeAccounts.expandRow(i);
        }
    }

    private void openAddAccountDialog() {
        if (selectedAccount == null) {
            JOptionPane.showMessageDialog(this, "يرجى تحديد الحساب المرجعي أولاً.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> accountDescriptions = new ArrayList<>();
        for (AccountNodeData account : accountList) {
            accountDescriptions.add(account.getCode() + " - " + account.getName());
        }

        String selectedDescription = selectedAccount.getCode() + " - " + selectedAccount.getName();
        JComboBox<String> cmbRelation = new JComboBox<>(new String[]{"حساب موازي", "حساب ابن"});
        JTextField txtCode = new JTextField();
        txtCode.setToolTipText("يمكن تعديل الرقم المقترح");
        JTextField txtName = new JTextField();
        JComboBox<String> cmbType = new JComboBox<>(new String[]{"حساب رئيسي", "حساب فرعي"});
        cmbRelation.addActionListener(e -> {
            boolean isChild = cmbRelation.getSelectedIndex() == 1;
            txtCode.setText(AccountAutoNumberService.generateNextCode(selectedDescription, isChild, accountDescriptions));
            cmbType.setSelectedItem(isChild && selectedAccount.getLevel() + 1 >= 6 ? "حساب فرعي" : "حساب رئيسي");
        });
        cmbRelation.setSelectedIndex(selectedAccount.isSubAccount() ? 0 : 0);
        cmbRelation.getActionListeners()[0].actionPerformed(null);

        JPanel form = new JPanel(new GridLayout(5, 2, 6, 6));
        form.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        form.add(new JLabel("نوع الإضافة:"));
        form.add(cmbRelation);
        form.add(new JLabel("كود الحساب الأب:"));
        form.add(new JLabel(selectedAccount.getParentCode().isEmpty() ? "جذر عام" : selectedAccount.getParentCode()));
        form.add(new JLabel("كود الحساب الجديد:"));
        form.add(txtCode);
        form.add(new JLabel("اسم الحساب:"));
        form.add(txtName);
        form.add(new JLabel("طبيعة الحساب:"));
        form.add(cmbType);

        int result = JOptionPane.showConfirmDialog(this, form, "إضافة حساب", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            boolean isChild = cmbRelation.getSelectedIndex() == 1;
            if (isChild && selectedAccount.isSubAccount()) {
                JOptionPane.showMessageDialog(this, "لا يمكن إضافة حساب ابن تحت حساب فرعي مخصص للقيود.", "تنبيه", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String parentCode = isChild ? selectedAccount.getCode() : selectedAccount.getParentCode();
            int level = isChild ? selectedAccount.getLevel() + 1 : selectedAccount.getLevel();
            String code = txtCode.getText().trim();
            String name = txtName.getText().trim();
            String type = "حساب فرعي".equals(cmbType.getSelectedItem()) ? "فرعي" : "رئيسي";

            if (code.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "يرجى ملء كافة الحقول!", "خطأ", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (accountList.stream().anyMatch(account -> account.getCode().equals(code))) {
                JOptionPane.showMessageDialog(this, "رقم الحساب المولد موجود مسبقاً، يرجى إعادة المحاولة.", "خطأ", JOptionPane.ERROR_MESSAGE);
                return;
            }

            AccountNodeData newAcc = new AccountNodeData(code, name, level, type, parentCode, 0.0);
            accountList.add(newAcc);
            saveAccountToStorage(newAcc);
            appendAccountToFile(newAcc);
            buildTree("");
            JOptionPane.showMessageDialog(this, "تمت إضافة الحساب: " + code + " - " + name);
        }
    }

    private void openEditAccountDialog() {
        if (selectedAccount == null) {
            JOptionPane.showMessageDialog(this, "يرجى تحديد حساب لتعديله.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField txtName = new JTextField(selectedAccount.getName());
        JComboBox<String> cmbType = new JComboBox<>(new String[]{"حساب رئيسي", "حساب فرعي"});
        cmbType.setSelectedItem(selectedAccount.isSubAccount() ? "حساب فرعي" : "حساب رئيسي");

        JPanel form = new JPanel(new GridLayout(3, 2, 6, 6));
        form.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        form.add(new JLabel("كود الحساب (ثابت):"));
        form.add(new JLabel(selectedAccount.getCode()));
        form.add(new JLabel("اسم الحساب:"));
        form.add(txtName);
        form.add(new JLabel("نوع الحساب:"));
        form.add(cmbType);

        int result = JOptionPane.showConfirmDialog(this, form, "تعديل بيانات الحساب", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            selectedAccount.setName(txtName.getText().trim());
            selectedAccount.setType("حساب فرعي".equals(cmbType.getSelectedItem()) ? "فرعي" : "رئيسي");
            updateAccountInStorage(selectedAccount);
            rewriteAccountsFile();
            buildTree("");
            updateDetailsPanel(selectedAccount);
            JOptionPane.showMessageDialog(this, "تم تعديل الحساب");
        }
    }

    private void handleDeleteAccount() {
        if (selectedAccount == null) {
            JOptionPane.showMessageDialog(this, "يرجى تحديد حساب لحذفه.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // فحص رقابي: هل الحساب عليه حركات أو له أبناء؟
        boolean hasChildren = accountList.stream().anyMatch(a -> selectedAccount.getCode().equals(a.getParentCode()));
        if (hasChildren || hasDatabaseChildren(selectedAccount.getCode())) {
            JOptionPane.showMessageDialog(this, "حظر رقابي: لا يمكن حذف حساب رئيسي يحتوي على حسابات متفرعة تحته.", "حظر", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (AccountValidator.hasFinancialTransactions(selectedAccount.getCode()) || hasDatabaseTransactions(selectedAccount.getCode())) {
            JOptionPane.showMessageDialog(this, "حظر رقابي: لا يمكن حذف حساب له قيود أو حركات مالية مسجلة.", "حظر", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "هل أنت متأكد من حذف الحساب [" + selectedAccount.getCode() + " - " + selectedAccount.getName() + "]؟", "تأكيد الحذف", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            accountList.remove(selectedAccount);
            deleteAccountFromStorage(selectedAccount.getCode());
            rewriteAccountsFile();
            resetDetails();
            buildTree("");
            JOptionPane.showMessageDialog(this, "تم حذف الحساب");
        }
    }

    private void saveAccountToStorage(AccountNodeData acc) {
        // حفظ بـ MySQL
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO chart_of_accounts (account_code, account_name, account_type, account_level, is_sub_account, parent_code, current_balance) VALUES (?, ?, ?, ?, ?, ?, 0.0)")) {
            pstmt.setString(1, acc.getCode());
            pstmt.setString(2, acc.getName());
            pstmt.setString(3, databaseAccountType(acc.getCode()));
            pstmt.setInt(4, acc.getLevel());
            pstmt.setBoolean(5, acc.isSubAccount());
            pstmt.setString(6, acc.getParentCode());
            pstmt.executeUpdate();
        } catch (Exception ignored) {}
    }

    private void updateAccountInStorage(AccountNodeData acc) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE chart_of_accounts SET account_name = ?, is_sub_account = ? WHERE account_code = ?")) {
            pstmt.setString(1, acc.getName());
            pstmt.setBoolean(2, acc.isSubAccount());
            pstmt.setString(3, acc.getCode());
            pstmt.executeUpdate();
        } catch (Exception ignored) {}
    }

    private String databaseAccountType(String code) {
        if (code == null || code.isEmpty()) return "ASSET";
        switch (code.charAt(0)) {
            case '2': return "LIABILITY";
            case '3': return "EQUITY";
            case '4': return "REVENUE";
            case '5': return "EXPENSE";
            default: return "ASSET";
        }
    }

    private boolean hasDatabaseChildren(String code) {
        String sql = "SELECT 1 FROM chart_of_accounts WHERE parent_code = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        } catch (Exception ignored) { return false; }
    }

    private boolean hasDatabaseTransactions(String code) {
        String sql = "SELECT 1 FROM journal_entry_lines WHERE account_code = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        } catch (Exception ignored) { return false; }
    }

    private void appendAccountToFile(AccountNodeData account) {
        try (FileWriter writer = new FileWriter(ACCOUNTS_FILE, true)) {
            writer.write(account.getCode() + " - " + account.getName() + " (حساب " + account.getType() + " - مستوى " + account.getLevel() + ")\n");
        } catch (IOException ignored) {}
    }

    private void rewriteAccountsFile() {
        try (FileWriter writer = new FileWriter(ACCOUNTS_FILE, false)) {
            writer.write("اختر أو اكتب للبحث...\n");
            for (AccountNodeData account : accountList) {
                writer.write(account.getCode() + " - " + account.getName() + " (حساب " + account.getType() + " - مستوى " + account.getLevel() + ")\n");
            }
        } catch (IOException ignored) {}
    }

    private void deleteAccountFromStorage(String code) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM chart_of_accounts WHERE account_code = ?")) {
            pstmt.setString(1, code);
            pstmt.executeUpdate();
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // دوال الاسترجاع والفحص المتوافقة
    // =========================================================================

    public boolean isAccountSelected() {
        return confirmed && selectedAccount != null;
    }

    public boolean isConfirmed() {
        return isAccountSelected();
    }

    public String getSelectedAccountResult() {
        return selectedAccount != null ? (selectedAccount.getCode() + " - " + selectedAccount.getName()) : "";
    }

    public String getSelectedAccount() {
        return getSelectedAccountResult();
    }

    public String getSelectedAccountCode() {
        return selectedAccount != null ? selectedAccount.getCode() : "";
    }

    public String getSelectedAccountName() {
        return selectedAccount != null ? selectedAccount.getName() : "";
    }

    public AccountNodeData getSelectedAccountNode() {
        return selectedAccount;
    }

    private static class AccountTreeCellRenderer extends DefaultTreeCellRenderer {
        private AccountTreeCellRenderer() {
            setLeafIcon(null);
            setOpenIcon(null);
            setClosedIcon(null);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                if (node.getUserObject() instanceof AccountNodeData) {
                    AccountNodeData acc = (AccountNodeData) node.getUserObject();
                    setText(acc.getCode() + " - " + acc.getName());

                    if (acc.isSubAccount()) {
                        setForeground(new Color(16, 185, 129));
                        setFont(getFont().deriveFont(Font.PLAIN));
                    } else {
                        setForeground(new Color(30, 41, 59));
                        setFont(getFont().deriveFont(Font.BOLD));
                    }
                }
            }
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            AccountTreeDialog dialog = new AccountTreeDialog();
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);

            if (dialog.isAccountSelected()) {
                System.out.println("تم اختيار الحساب: " + dialog.getSelectedAccountResult());
            }
        });
    }
}