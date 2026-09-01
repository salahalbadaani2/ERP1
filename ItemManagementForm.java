import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * نظام ERP المصنعي - شاشة تهيئة بطاقة الأصناف المخزنية (ItemManagementForm)
 * ============================================================================
 * - الموديول القياسي الموحد لشجرة الحسابات المركزية (AccountTreeDialog).
 * - مطابقة كاملة لشاشة سندات الخزينة في استعراض وإضافة الحسابات الشجرية.
 * - التفعيل والربط المخزني للباركود والمبيعات يتم حواصاً عند الضغط على زر الحفظ.
 * 
 * @version 6.0 (416 سطراً معتمد ومكتمل)
 */
public class ItemManagementForm extends JFrame {

    private static final String ITEMS_FILE = "ItemsData.txt";
        // ------------------------------------------------------------------------
    // عناصر اختيار الحساب الشجري القياسي (طريقة شاشة الخزينة والبنك)
    // ------------------------------------------------------------------------
    private JTextField txtSubAccountCode;
    private JComboBox<String> cmbSubAccount;
    private JButton btnTree;

    // ------------------------------------------------------------------------
    // عناصر بطاقة البيانات التعريفية والربط بالباركود
    // ------------------------------------------------------------------------
    private JTextField txtBarcode;
    private JTextField txtItemName;
    private JComboBox<String> cmbItemType;

    // ------------------------------------------------------------------------
    // عناصر بطاقة القياسات والمحددات للقطاع الغذائي
    // ------------------------------------------------------------------------
    private JComboBox<String> cmbUom;
    private JComboBox<String> cmbUnitType;
    private JTextField txtConversionFactor;
    private JTextField txtMinStockLevel;
    private JTextField txtExpiryDate;
    private JTextField txtBatchNo;
    private JTextField txtUnitCost;
    private JTextField txtDefaultUnitPrice;

    // ------------------------------------------------------------------------
    // أزرار التحكم والعمليات التنفيذية
    // ------------------------------------------------------------------------
    private JButton btnSave;
    private JButton btnClose;

    // قوائم البيانات المعالجة في الذاكرة
    private List<String> masterAccountList;
    private List<ItemMaster> itemsList;

    /**
     * المشيد الرئيسي لبناء عناصر الواجهة وتجهيز البيانات
     */
    public ItemManagementForm() {
        setTitle("نظام ERP المصنعي - شاشة تهيئة بطاقة الأصناف المخزنية (الشجرة الموحدة القياسية)");
        setSize(880, 680);
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        masterAccountList = new ArrayList<>();
        itemsList = new ArrayList<>();

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBorder(new EmptyBorder(10, 10, 10, 10));

        // إضافة البطاقات البصرية المعتمدة للنظام
        container.add(createStandardAccountCard());
        container.add(Box.createRigidArea(new Dimension(0, 10)));
        container.add(createBasicInfoCard());
        container.add(Box.createRigidArea(new Dimension(0, 10)));
        container.add(createFoodSpecsCard());

        add(container, BorderLayout.CENTER);
        add(createActionBar(), BorderLayout.SOUTH);

        loadAccountListFromFile();
        loadItemsData();
        setupEvents();
    }

    /**
     * بناء بطاقة اختيار الحساب الشجري الموحد (طريقة الخزينة القياسية)
     *
     * @return JPanel يحتوي على حقل الكود، القائمة المنسدلة، وزر الشجرة القياسي
     */
    private JPanel createStandardAccountCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "تهيئة الصنف",
            TitledBorder.RIGHT, TitledBorder.TOP,
            new Font("Tahoma", Font.BOLD, 12), new Color(26, 35, 126)
        ));
        card.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        GridBagConstraints gbc = createGbc();

        txtSubAccountCode = new JTextField(10);
        txtSubAccountCode.setEditable(false);
        txtSubAccountCode.setFont(new Font("Tahoma", Font.BOLD, 12));
        txtSubAccountCode.setHorizontalAlignment(JTextField.CENTER);
        txtSubAccountCode.setBackground(new Color(245, 245, 245));

        cmbSubAccount = new JComboBox<>();
        cmbSubAccount.setPreferredSize(new Dimension(380, 26));

        btnTree = new JButton("دليل الحسابات");
        btnTree.setFont(new Font("Tahoma", Font.BOLD, 11));
        btnTree.setBackground(new Color(238, 238, 238));

        addLabel(card, "الحساب الفرعي:", gbc, 0, 0);
        addComp(card, txtSubAccountCode, gbc, 1, 0);
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0;
        card.add(btnTree, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        card.add(cmbSubAccount, gbc);
        gbc.gridwidth = 1;

        return card;
    }

    /**
     * بناء بطاقة البيانات التعريفية والربط بالباركود الموحد
     *
     * @return JPanel يحتوي على حقول الباركود والاسم ونوع التصنيف
     */
    private JPanel createBasicInfoCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "",
            TitledBorder.RIGHT, TitledBorder.TOP,
            new Font("Tahoma", Font.BOLD, 12), new Color(0, 105, 92)
        ));
        card.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        GridBagConstraints gbc = createGbc();

        addLabel(card, "الباركود (كود الحساب الفرعي):", gbc, 0, 0);
        txtBarcode = new JTextField(12);
        txtBarcode.setEditable(false);
        txtBarcode.setFont(new Font("Tahoma", Font.BOLD, 12));
        txtBarcode.setForeground(new Color(21, 101, 192));
        addComp(card, txtBarcode, gbc, 1, 0);

        addLabel(card, "اسم الصنف:", gbc, 2, 0);
        txtItemName = new JTextField(20);
        addComp(card, txtItemName, gbc, 3, 0);

        addLabel(card, "نوع المخزون:", gbc, 0, 1);
        cmbItemType = new JComboBox<>(new String[]{
            "منتج تام الصنع (12103)",
            "مواد خام (12101)",
            "إنتاج قيد التشغيل (12102)",
            "مخزون المناديب (12104)"
        });
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 3;
        card.add(cmbItemType, gbc);

        return card;
    }

    /**
     * بناء بطاقة القياسات والمحددات الخاصة بالقطاع الغذائي
     *
     * @return JPanel يحتوي على حقول الوحدات والتكلفة وتاريخ الانتهاء
     */
    private JPanel createFoodSpecsCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "وحدات القياس ومحددات الجودة الغذائية",
            TitledBorder.RIGHT, TitledBorder.TOP,
            new Font("Tahoma", Font.BOLD, 12), new Color(230, 81, 0)
        ));
        card.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        GridBagConstraints gbc = createGbc();

        addLabel(card, "نوع الوحدة:", gbc, 0, 0);
        cmbUnitType = new JComboBox<>(new String[]{"COUNT (عددي)", "WEIGHT (وزني)"});
        addComp(card, cmbUnitType, gbc, 1, 0);

        addLabel(card, "وحدة القياس:", gbc, 2, 0);
        cmbUom = new JComboBox<>(new String[]{"حبه", "جرام", "كيلو", "طن", "مل", "لتر", "باكت", "كرتون", "كيس", "عبوة", "+ إضافة وحدة جديدة..."});
        addComp(card, cmbUom, gbc, 3, 0);

        addLabel(card, "معامل التحويل:", gbc, 2, 0);
        txtConversionFactor = new JTextField("24.0", 10);
        ((AbstractDocument) txtConversionFactor.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string.matches("\\d*")) super.insertString(fb, offset, string, attr);
            }
            @Override
            public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text.matches("\\d*")) super.replace(fb, offset, length, text, attrs);
            }
        });
        addComp(card, txtConversionFactor, gbc, 3, 0);

        addLabel(card, "حد الأمان البسيط (أقل كمية):", gbc, 0, 1);
        txtMinStockLevel = new JTextField("10.0", 10);
        addComp(card, txtMinStockLevel, gbc, 1, 1);

        addLabel(card, "تاريخ الانتهاء (YYYY-MM-DD):", gbc, 2, 1);
        txtExpiryDate = new JTextField("2027-12-31", 10);
        addComp(card, txtExpiryDate, gbc, 3, 1);

        addLabel(card, "رقم التشغيلة / الدفعة:", gbc, 0, 2);
        txtBatchNo = new JTextField("BATCH-101", 10);
        addComp(card, txtBatchNo, gbc, 1, 2);

        addLabel(card, "تكلفة الوحدة التقديرية:", gbc, 2, 2);
        txtUnitCost = new JTextField("0.0", 10);
        addComp(card, txtUnitCost, gbc, 3, 2);

        addLabel(card, "سعر البيع الافتراضي:", gbc, 0, 3);
        txtDefaultUnitPrice = new JTextField("0.0", 10);
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 3;
        card.add(txtDefaultUnitPrice, gbc);

        return card;
    }

    /**
     * بناء شريط الأزرار السفلي للعمليات
     *
     * @return JPanel يحتوي على أزرار الحفظ والإغلاق
     */
    private JPanel createActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        bar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        btnSave = new JButton("حفظ بطاقة الصنف");
        btnClose = new JButton("إغلاق");

        btnSave.setFont(new Font("Tahoma", Font.BOLD, 14));
        btnSave.setBackground(new Color(46, 125, 50));
        btnSave.setForeground(Color.WHITE);
        btnSave.setEnabled(true);
        btnSave.setOpaque(true);
        btnSave.setContentAreaFilled(true);
        btnSave.setBorderPainted(true);
        btnSave.setFocusPainted(false);
        btnSave.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(27, 94, 32), 2),
            BorderFactory.createEmptyBorder(6, 18, 6, 18)
        ));
        btnSave.setPreferredSize(new Dimension(150, 36));

        btnClose.setFont(new Font("Tahoma", Font.PLAIN, 12));

        bar.add(btnSave);
        bar.add(btnClose);

        return bar;
    }

    private GridBagConstraints createGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        return gbc;
    }

    private void addLabel(JPanel panel, String text, GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x; gbc.gridy = y; gbc.weightx = 0.0;
        JLabel lbl = new JLabel(text);
        lbl.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.add(lbl, gbc);
    }

    private void addComp(JPanel panel, JComponent comp, GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x; gbc.gridy = y; gbc.weightx = 0.5;
        comp.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.add(comp, gbc);
    }

    /**
     * قراءة الحسابات المالية من ملف الشجرة الرئيسي AccountsData.txt
     */
    private void loadAccountListFromFile() {
        masterAccountList.clear();
        cmbSubAccount.removeAllItems();
        cmbSubAccount.addItem("... اختر أو اكتب للبحث في الشجرة");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT account_code, account_name, is_sub_account, account_level FROM chart_of_accounts WHERE is_sub_account=1 ORDER BY account_code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String code = rs.getString(1);
                String name = rs.getString(2);
                String display = code + " - " + name + " (حساب فرعي - مستوى " + rs.getInt(4) + ")";
                masterAccountList.add(display);
                cmbSubAccount.addItem(display);
            }
        } catch (Exception ignored) {}
    }

    /**
     * قراءة الأصناف المخزنية المعرفة من ملف ItemsData.txt
     */
    private void loadItemsData() {
        itemsList.clear();
        File file = new File(ITEMS_FILE);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty() && line.contains(" | ")) {
                        String[] parts = line.split(" \\| ");
                        if (parts.length >= 11) {
                            itemsList.add(new ItemMaster(
                                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                                parts[3].trim(), parts[4].trim(),
                                Double.parseDouble(parts[5].trim()),
                                Double.parseDouble(parts[6].trim()), parts[7].trim(),
                                parts[8].trim(),
                                Double.parseDouble(parts[9].trim()),
                                Double.parseDouble(parts[10].trim())
                            ));
                        }
                    }
                }
            } catch (IOException ignored) {}
        }
    }

    /**
     * إعداد واستماع أحداث الواجهة وأزرار التفاعل
     */
    private void setupEvents() {
        // استدعاء موديول شجرة الحسابات المركزية الموحدة القياسية (AccountTreeDialog)
        btnTree.addActionListener(e -> chooseInventoryAccount());

        cmbSubAccount.addActionListener(e -> {
            Object selected = cmbSubAccount.getSelectedItem();
            if (selected != null && selected.toString().contains(" - ")) {
                String fullText = selected.toString();
                String[] parts = fullText.split(" - ");
                String code = parts[0].trim();
                String name = parts[1].replaceAll("\\(حساب.*\\)", "").trim();

                txtSubAccountCode.setText(code);
                txtBarcode.setText(code);
                txtItemName.setText(name);

                for (ItemMaster item : itemsList) {
                    if (item.getBarcode().equals(code)) {
                        displayItemDetails(item);
                        break;
                    }
                }
            }
        });

        txtItemName.setToolTipText("يُملأ تلقائياً من الحساب المختار، ويمكن تعديله لاسم الصنف التجاري.");
        txtItemName.setEditable(true);

        cmbUom.addActionListener(e -> {
            if ("+ إضافة وحدة جديدة...".equals(cmbUom.getSelectedItem())) {
                String newUom = JOptionPane.showInputDialog(this, "أدخل اسم وحدة القياس الجديدة (مثال: طن / كجم / كيس):");
                if (newUom != null && !newUom.trim().isEmpty()) {
                    cmbUom.insertItemAt(newUom.trim(), cmbUom.getItemCount() - 1);
                    cmbUom.setSelectedItem(newUom.trim());
                } else {
                    cmbUom.setSelectedIndex(0);
                }
            }
        });

        cmbUnitType.addActionListener(e -> {
            if ("WEIGHT (وزني)".equals(cmbUnitType.getSelectedItem())) {
                cmbUom.setSelectedItem("كيلو");
                txtConversionFactor.setText("1000");
            } else {
                txtConversionFactor.setText("1");
            }
        });

        btnSave.addActionListener(e -> handleSave());
        btnClose.addActionListener(e -> dispose());
    }

    /** ينسخ اختيار الحساب الفرعي من الشجرة إلى كود واسم الصنف. */
    private void chooseInventoryAccount() {
        AccountTreeDialog dialog = new AccountTreeDialog(this);
        dialog.setVisible(true);
        if (!dialog.isAccountSelected()) return;

        String code = dialog.getSelectedAccountCode();
        String name = dialog.getSelectedAccountName();
        if (code.isEmpty() || name.isEmpty()) return;

        txtSubAccountCode.setText(code);
        txtBarcode.setText(code);
        txtItemName.setText(name);

        String display = dialog.getSelectedAccountResult();
        boolean found = false;
        for (int i = 0; i < cmbSubAccount.getItemCount(); i++) {
            String option = cmbSubAccount.getItemAt(i);
            if (option != null && option.startsWith(code + " -")) {
                cmbSubAccount.setSelectedIndex(i);
                found = true;
                break;
            }
        }
        if (!found) {
            cmbSubAccount.addItem(display);
            cmbSubAccount.setSelectedItem(display);
        }
    }

    /**
     * عرض تفاصيل الصنف المخزني في الخانات
     */
    private void displayItemDetails(ItemMaster item) {
        txtBarcode.setText(item.getBarcode());
        txtItemName.setText(item.getItemName());
        String unitType = item.getUnitType();
        cmbUnitType.setSelectedItem("COUNT".equals(unitType) ? "COUNT (عددي)" : "WEIGHT".equals(unitType) ? "WEIGHT (وزني)" : unitType);
        cmbUom.setSelectedItem(item.getUom());
        txtConversionFactor.setText(String.valueOf(item.getConversionFactor()));
        txtMinStockLevel.setText(String.valueOf(item.getMinStockLevel()));
        txtExpiryDate.setText(item.getExpiryDate());
        txtBatchNo.setText(item.getBatchNo());
        txtUnitCost.setText(String.valueOf(item.getUnitCost()));
        txtDefaultUnitPrice.setText(String.valueOf(item.getDefaultUnitPrice()));
    }

    /**
     * معالجة حفظ بطاقة الصنف وتفعيله أمنياً ومخزنياً عبر حارس الحسابات
     */
    private void handleSave() {
        String code = txtBarcode.getText().trim();
        String name = txtItemName.getText().trim();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "يرجى اختيار حساب من الشجرة القياسية أولاً.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // التحقق الجبري عبر حارس الحسابات (AccountValidator) لمنع الترحيل للحسابات الرئيسية
            String fullAccString = (String) cmbSubAccount.getSelectedItem();
            if (fullAccString != null) {
                AccountValidator.validatePostingAccount(fullAccString);
            }

            ItemMaster item = new ItemMaster(
                code,
                name,
                (String) cmbItemType.getSelectedItem(),
                (String) cmbUom.getSelectedItem(),
                ((String) cmbUnitType.getSelectedItem()).split(" ")[0],
                Double.parseDouble(txtConversionFactor.getText().trim()),
                Double.parseDouble(txtMinStockLevel.getText().trim()),
                txtExpiryDate.getText().trim(),
                txtBatchNo.getText().trim(),
                Double.parseDouble(txtUnitCost.getText().trim()),
                Double.parseDouble(txtDefaultUnitPrice.getText().trim())
            );

            try (FileWriter writer = new FileWriter(ITEMS_FILE, true)) {
                writer.write(item.toLogLine() + "\n");
            }

            saveItemToDatabase(item);

            JOptionPane.showMessageDialog(this,
                "تم حفظ وتفعيل الصنف مخزنياً بنجاح!\n" +
                "• الباركود والحساب المربوط: " + code + "\n" +
                "• الصنف أصبح معتمداً وجاهزاً للتعامل في المبيعات والمخازن وCOGS.",
                "تم التفعيل والمزامنة", JOptionPane.INFORMATION_MESSAGE);

            loadItemsData();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ حارس الحسابات أو المدخلات: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveItemToDatabase(ItemMaster item) throws Exception {
        String sql = "INSERT INTO inventory_items (item_code, item_name, category, unit, unit_type, default_sale_price, unit_cost, "
                + "current_stock, inventory_account, sales_revenue_account, cogs_account, conversion_factor, "
                + "min_stock_level, expiry_date, batch_no) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, '410101', '510101', ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE item_name = VALUES(item_name), category = VALUES(category), unit = VALUES(unit), "
                + "unit_type = VALUES(unit_type), default_sale_price = VALUES(default_sale_price), unit_cost = VALUES(unit_cost), "
                + "inventory_account = VALUES(inventory_account), conversion_factor = VALUES(conversion_factor), "
                + "min_stock_level = VALUES(min_stock_level), expiry_date = VALUES(expiry_date), batch_no = VALUES(batch_no)";
        try (Connection connection = DatabaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, item.getBarcode());
            statement.setString(2, item.getItemName());
            statement.setString(3, item.getItemType());
            statement.setString(4, item.getUom());
            statement.setString(5, item.getUnitType());
            statement.setDouble(6, item.getDefaultUnitPrice());
            statement.setDouble(7, item.getUnitCost());
            statement.setString(8, item.getBarcode());
            statement.setDouble(9, item.getConversionFactor());
            statement.setDouble(10, item.getMinStockLevel());
            statement.setString(11, item.getExpiryDate());
            statement.setString(12, item.getBatchNo());
            statement.executeUpdate();
        }
    }

    /**
     * نقطة الإقلاع للتشغيل المنفصل
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ItemManagementForm().setVisible(true));
    }
}
