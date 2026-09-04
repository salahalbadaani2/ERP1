import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class BusinessPartySetupFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Color SCREEN_BACKGROUND = new Color(244, 246, 249);
    private static final Color CARD_BORDER = new Color(226, 232, 240);
    private static final Color TEXT_COLOR = new Color(30, 41, 59);
    private static final Color PRIMARY_COLOR = new Color(2, 132, 199);
    private static final Color SECONDARY_COLOR = new Color(100, 116, 139);
    private String partyType = "supplier";
    private String editCode = null;
    private boolean isEditMode = false;

    private JTextField txtCode, txtEnName, txtOwnerName, txtCity;
    private JComboBox<String> cmbAccountName;
    private boolean updatingAccountCombo;
    private JComboBox<String> cmbStatus, cmbBalanceType, cmbCurrency, cmbPartyType;
    private JTextField txtParentAccount, txtSubAccount, txtOpeningBalance;
    private JTextField txtCreditLimit, txtCreditPeriod, txtVatNumber, txtCrNumber;
    private JTextField txtPhone, txtMobile, txtEmail, txtAddress, txtContactPerson;
    private JTextField txtCrImagePath, txtDelegateName, txtDelegateJob, txtDelegateDocPath;
    private JTable delegateTable, partyTable;
    private DefaultTableModel delegateModel, partyModel;
    private JButton btnBrowseParent, btnBrowseCrImage, btnBrowseDelegateDoc, btnSave, btnNew, btnDelete;
    private JTextField txtSearch;

    public BusinessPartySetupFrame(String type) {
        this.partyType = type;
        this.partyModel = new DefaultTableModel(new String[]{"الكود", "الاسم التجاري", "النوع", "الحالة", "الرقم الضريبي"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        this.partyTable = new JTable(partyModel);
        this.delegateModel = new DefaultTableModel(new String[]{"الاسم", "الوظيفة", "صورة التفويض"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        this.delegateTable = new JTable(delegateModel);
        this.txtCode = new JTextField();
        this.txtEnName = new JTextField();
        this.txtOwnerName = new JTextField();
        this.txtCity = new JTextField();
        this.txtParentAccount = new JTextField();
        this.txtSubAccount = new JTextField();
        this.txtOpeningBalance = new JTextField();
        this.txtCreditLimit = new JTextField();
        this.txtCreditPeriod = new JTextField();
        this.txtVatNumber = new JTextField();
        this.txtCrNumber = new JTextField();
        this.txtPhone = new JTextField();
        this.txtMobile = new JTextField();
        this.txtEmail = new JTextField();
        this.txtAddress = new JTextField();
        this.txtContactPerson = new JTextField();
        this.txtCrImagePath = new JTextField();
        this.txtDelegateName = new JTextField();
        this.txtDelegateJob = new JTextField();
        this.txtDelegateDocPath = new JTextField();
        this.cmbStatus = new JComboBox<>(new String[]{"active", "suspended"});
        this.cmbBalanceType = new JComboBox<>(new String[]{"debit", "credit"});
        this.cmbCurrency = new JComboBox<>(new String[]{"YER", "USD", "SAR", "EUR"});
        this.cmbPartyType = new JComboBox<>(new String[]{"supplier", "customer"});
        setTitle((("supplier".equals(type)) ? "الموردين" : "العملاء") + " - تهيئة وتكوين");
        setSize(1300, 900);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        initUI();
        loadPartyList();
        generateCode();
    }

    private void initUI() {
        getContentPane().setBackground(SCREEN_BACKGROUND);
        setLayout(new BorderLayout(0, 10));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 9));
        toolbar.setBackground(Color.WHITE);
        toolbar.setPreferredSize(new Dimension(0, 50));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, CARD_BORDER));
        toolbar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        btnNew = createToolbarButton("جديد", "icons/add.png");
        btnSave = createToolbarButton("حفظ", "icons/save.png");
        JButton btnEdit = createToolbarButton("تعديل", "icons/edit.png");
        JButton btnClose = createToolbarButton("إغلاق", "icons/close.png");
        styleToolbarButton(btnNew, new Color(147, 197, 253), Color.BLACK);
        styleToolbarButton(btnSave, new Color(125, 211, 252), Color.BLACK);
        styleToolbarButton(btnEdit, new Color(203, 213, 225), Color.BLACK);
        styleToolbarButton(btnClose, new Color(226, 232, 240), Color.BLACK);

        toolbar.add(btnNew);
        toolbar.add(btnSave);
        toolbar.add(btnEdit);
        toolbar.add(Box.createHorizontalGlue());
        JLabel searchLabel = new JLabel("بحث:");
        searchLabel.setFont(new Font("Tahoma", Font.BOLD, 14));
        searchLabel.setForeground(TEXT_COLOR);
        toolbar.add(searchLabel);
        txtSearch = new JTextField(15);
        styleField(txtSearch);
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterPartyList(); }
            public void removeUpdate(DocumentEvent e) { filterPartyList(); }
            public void changedUpdate(DocumentEvent e) { filterPartyList(); }
        });
        toolbar.add(txtSearch);
        toolbar.add(btnClose);
        add(toolbar, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(SCREEN_BACKGROUND);
        centerPanel.setBorder(new EmptyBorder(8, 12, 8, 12));
        JScrollPane formScroll = new JScrollPane(createSinglePagePanel());
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        centerPanel.add(formScroll, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        btnNew.addActionListener(e -> newParty());
        btnSave.addActionListener(e -> saveParty());
        btnEdit.addActionListener(e -> editParty());
        btnClose.addActionListener(e -> dispose());
    }

    private JButton createToolbarButton(String text, String iconPath) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(82, 32));
        try {
            BufferedImage img = ImageIO.read(new File(iconPath));
            if (img != null) btn.setIcon(new ImageIcon(img.getScaledInstance(18, 18, Image.SCALE_SMOOTH)));
        } catch (Exception ignored) {}
        return btn;
    }

    private void styleToolbarButton(JButton button, Color background, Color foreground) {
        button.setFont(new Font("Tahoma", Font.BOLD, 14));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(background.darker(), 6), new EmptyBorder(4, 10, 4, 10)));
    }

    private void styleField(JComponent field) {
        field.setFont(new Font("Tahoma", Font.BOLD, 14));
        field.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        if (field instanceof JTextField) {
            ((JTextField) field).setHorizontalAlignment(JTextField.RIGHT);
        }
        int width = field.getPreferredSize().width;
        field.setPreferredSize(new Dimension(Math.max(width, 220), 32));
        field.setMinimumSize(new Dimension(160, 32));
    }

    private JPanel createCardPanel(LayoutManager layout) {
        JPanel card = new JPanel(layout);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(CARD_BORDER, 8), new EmptyBorder(14, 16, 14, 16)));
        card.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        return card;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setForeground(TEXT_COLOR);
        table.setGridColor(CARD_BORDER);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setBackground(new Color(15, 23, 42));
        table.getTableHeader().setOpaque(true);
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                    boolean focused, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focused, row, column);
                c.setBackground(selected ? new Color(219, 234, 254) : (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));
                c.setForeground(TEXT_COLOR);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return c;
            }
        });
    }

    // =========================================================================
    // التبويب الأول: البيانات العامة
    // =========================================================================
    private JPanel createSinglePagePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setPreferredSize(new Dimension(1200, 760));
        panel.setMinimumSize(new Dimension(0, 0));
        panel.setBackground(SCREEN_BACKGROUND);
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel form = createCardPanel(new GridBagLayout());
        form.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.insets = new Insets(8, 12, 8, 12);

        int row = 0;
        addLabelField(form, gc, row++, "كود الجهة:", txtCode = new JTextField(18));
        cmbAccountName = new JComboBox<>();
        cmbAccountName.setEditable(true);
        cmbAccountName.addItem("... اختر أو اكتب للبحث في الشجرة");
        cmbAccountName.setSelectedIndex(0);
        styleField(cmbAccountName);
        cmbAccountName.addActionListener(e -> {
            if (!updatingAccountCombo) {
                selectAccountFromCombo();
            }
        });

        JPanel namePanel = new JPanel(new BorderLayout(8, 0));
        namePanel.setOpaque(false);
        namePanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        namePanel.add(cmbAccountName, BorderLayout.CENTER);
        JButton btnDirectory = new JButton("دليل الحسابات");
        styleToolbarButton(btnDirectory, PRIMARY_COLOR, new Color(15, 23, 42));
        btnDirectory.addActionListener(e -> openAccountTree());
        namePanel.add(btnDirectory, BorderLayout.EAST);
        addLabelField(form, gc, row++, "الاسم التجاري *:", namePanel);

        JPanel accountCodePanel = new JPanel(new BorderLayout(8, 0));
        accountCodePanel.setOpaque(false);
        accountCodePanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        txtSubAccount = new JTextField(18);
        txtSubAccount.setEditable(false);
        styleField(txtSubAccount);
        accountCodePanel.add(txtSubAccount, BorderLayout.CENTER);
        JButton btnClearAccount = new JButton("إفراغ الحساب");
        styleToolbarButton(btnClearAccount, SECONDARY_COLOR, new Color(15, 23, 42));
        btnClearAccount.addActionListener(e -> clearAccountSelection());
        accountCodePanel.add(btnClearAccount, BorderLayout.EAST);
        addLabelField(form, gc, row++, "رقم الحساب:", accountCodePanel);

        addLabelField(form, gc, row++, "اسم المالك:", txtOwnerName = new JTextField(20));
        addLabelField(form, gc, row++, "المدينة:", txtCity = new JTextField(20));
        addLabelField(form, gc, row++, "العنوان:", txtAddress = new JTextField(20));
        addLabelField(form, gc, row++, "رقم التواصل:", txtPhone = new JTextField(18));
        addLabelField(form, gc, row++, "الإيميل:", txtEmail = new JTextField(18));

        addLabelField(form, gc, row++, "اسم المفوض:", txtDelegateName = new JTextField(18));
        addLabelField(form, gc, row++, "الوظيفة:", txtDelegateJob = new JTextField(18));

        addLabelField(form, gc, row++, "رقم السجل التجاري:", txtCrNumber = new JTextField(18));

        JPanel imagesPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        imagesPanel.setOpaque(false);
        imagesPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        imagesPanel.add(createImagePanel("صورة السجل التجاري:", false));
        imagesPanel.add(createImagePanel("صورة التفويض:", true));
        gc.gridx = 0;
        gc.gridy = row++;
        gc.gridwidth = 2;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(imagesPanel, gc);
        gc.gridwidth = 1;

        panel.add(form, BorderLayout.CENTER);
        installPartyNameSearch();
        return panel;
    }

    private void addLabelField(JPanel form, GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridy = row;
        gc.gridx = 0; gc.weightx = 0.0; gc.gridwidth = 1;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Tahoma", Font.BOLD, 14));
        lbl.setForeground(TEXT_COLOR);
        form.add(lbl, gc);
        gc.gridx = 1; gc.weightx = 1.0;
        styleField(field);
        form.add(field, gc);
    }

    private JComboBox<String> createPartyTypeCombo() {
        cmbPartyType = new JComboBox<>(new String[]{"supplier", "customer"});
        cmbPartyType.addActionListener(e -> {
            partyType = (String) cmbPartyType.getSelectedItem();
            generateCode();
        });
        return cmbPartyType;
    }

    private void installPartyNameSearch() {
        Timer debounce = new Timer(250, e -> searchAccounts(getCommercialName()));
        debounce.setRepeats(false);
        JTextField editor = getAccountEditor();
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (!updatingAccountCombo) debounce.restart();
            }
            public void removeUpdate(DocumentEvent e) {
                if (!updatingAccountCombo) debounce.restart();
            }
            public void changedUpdate(DocumentEvent e) {
                if (!updatingAccountCombo) debounce.restart();
            }
        });
    }

    private JTextField getAccountEditor() {
        return (JTextField) cmbAccountName.getEditor().getEditorComponent();
    }

    private String getCommercialName() {
        String value = getAccountEditor().getText().trim();
        if (value.contains(" - ")) {
            return value.substring(value.indexOf(" - ") + 3).trim();
        }
        return value;
    }

    private void setCommercialName(String name) {
        getAccountEditor().setText(name == null ? "" : name);
    }

    private void clearAccountSelection() {
        updatingAccountCombo = true;
        cmbAccountName.removeAllItems();
        cmbAccountName.addItem("... اختر أو اكتب للبحث في الشجرة");
        cmbAccountName.setSelectedIndex(0);
        setCommercialName("");
        txtSubAccount.setText("");
        updatingAccountCombo = false;
    }

    private void setAccountSelection(String code, String name) {
        String display = code + " - " + name;
        boolean found = false;
        for (int i = 0; i < cmbAccountName.getItemCount(); i++) {
            if (display.equals(cmbAccountName.getItemAt(i))) {
                found = true;
                break;
            }
        }
        if (!found) cmbAccountName.addItem(display);
        cmbAccountName.setSelectedItem(display);
        txtSubAccount.setText(code);
        setCommercialName(name);
    }

    private void selectAccountFromCombo() {
        Object selected = cmbAccountName.getSelectedItem();
        if (selected == null) return;
        String value = selected.toString();
        if (!value.contains(" - ")) return;
        String[] parts = value.split(" - ", 2);
        setAccountSelection(parts[0].trim(), parts[1].trim());
        loadPartyByAccountCode(parts[0].trim());
    }

    private void openAccountTree() {
        String prefix = "supplier".equals(partyType) ? "2" : "12";
        AccountTreeDialog dialog = new AccountTreeDialog(this, prefix);
        dialog.setVisible(true);
        if (dialog.isAccountSelected()) {
            String code = dialog.getSelectedAccountCode();
            loadAccountName(code);
        }
    }

    private void loadAccountName(String accountCode) {
        String sql = "SELECT account_name FROM chart_of_accounts "
                + "WHERE account_code = ? AND is_sub_account = 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    setAccountSelection(accountCode, rs.getString("account_name"));
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "فشل جلب اسم الحساب: " + ex.getMessage(),
                    "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createImagePanel(String label, boolean authorization) {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setPreferredSize(new Dimension(0, 100));
        panel.setOpaque(false);
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                new EmptyBorder(6, 8, 6, 8)));
        JLabel title = new JLabel(label);
        title.setFont(new Font("Tahoma", Font.BOLD, 14));
        panel.add(title, BorderLayout.NORTH);

        JTextField pathField;
        if (authorization) {
            txtDelegateDocPath = new JTextField(18);
            pathField = txtDelegateDocPath;
        } else {
            txtCrImagePath = new JTextField(18);
            pathField = txtCrImagePath;
        }
        pathField.setEditable(false);
        styleField(pathField);
        JPanel actions = new JPanel(new BorderLayout(8, 0));
        actions.setOpaque(false);
        actions.add(pathField, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttons.setOpaque(false);

        JButton choose = new JButton("اختيار...");
        styleToolbarButton(choose, PRIMARY_COLOR, new Color(15, 23, 42));
        choose.addActionListener(e -> chooseImage(pathField, label));
        buttons.add(choose);

        JButton view = new JButton("عرض");
        styleToolbarButton(view, SECONDARY_COLOR, new Color(15, 23, 42));
        view.addActionListener(e -> previewImage(pathField.getText().trim(), label));
        buttons.add(view);

        JButton print = new JButton("طباعة");
        styleToolbarButton(print, SECONDARY_COLOR, new Color(15, 23, 42));
        print.addActionListener(e -> printImage(pathField.getText().trim(), label));
        buttons.add(print);
        actions.add(buttons, BorderLayout.EAST);
        panel.add(actions, BorderLayout.CENTER);
        return panel;
    }

    private void chooseImage(JTextField pathField, String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("اختيار " + title);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File source = chooser.getSelectedFile();
        try {
            BufferedImage image = ImageIO.read(source);
            if (image == null) {
                throw new IllegalArgumentException("الملف المحدد ليس صورة قابلة للقراءة");
            }
            Path uploads = new File("uploads").toPath();
            Files.createDirectories(uploads);
            String safeName = System.currentTimeMillis() + "_" + source.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
            Path destination = uploads.resolve(safeName);
            Files.copy(source.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            pathField.setText(destination.toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "فشل حفظ الصورة: " + ex.getMessage(),
                    "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void previewImage(String imagePath, String title) {
        if (imagePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "لم يتم اختيار صورة", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            BufferedImage image = ImageIO.read(new File(imagePath));
            if (image == null) {
                throw new IllegalArgumentException("تعذر قراءة الصورة");
            }
            int width = Math.min(800, Math.max(300, image.getWidth()));
            int height = Math.min(600, Math.max(200, image.getHeight()));
            double scale = Math.min((double) width / image.getWidth(), (double) height / image.getHeight());
            Image scaled = image.getScaledInstance((int) (image.getWidth() * scale),
                    (int) (image.getHeight() * scale), Image.SCALE_SMOOTH);
            JDialog dialog = new JDialog(this, title, true);
            dialog.add(new JScrollPane(new JLabel(new ImageIcon(scaled))));
            dialog.setSize(width + 40, height + 60);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "فشل عرض الصورة: " + ex.getMessage(),
                    "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printImage(String imagePath, String title) {
        if (imagePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "لم يتم اختيار صورة", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            BufferedImage image = ImageIO.read(new File(imagePath));
            if (image == null) {
                throw new IllegalArgumentException("تعذر قراءة الصورة");
            }
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName(title);
            job.setPrintable((graphics, pageFormat, pageIndex) -> {
                if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
                double scale = Math.min(
                        (pageFormat.getImageableWidth() - 10) / image.getWidth(),
                        (pageFormat.getImageableHeight() - 10) / image.getHeight());
                int width = (int) (image.getWidth() * scale);
                int height = (int) (image.getHeight() * scale);
                graphics.drawImage(image, (int) pageFormat.getImageableX(),
                        (int) pageFormat.getImageableY(), width, height, null);
                return Printable.PAGE_EXISTS;
            });
            if (job.printDialog()) {
                job.print();
            }
        } catch (PrinterException | RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "فشل طباعة الصورة: " + ex.getMessage(),
                    "خطأ", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "فشل قراءة الصورة: " + ex.getMessage(),
                    "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void lookupPartyByName() {
        String value = getCommercialName();
        if (value.isEmpty()) {
            JOptionPane.showMessageDialog(this, "يرجى إدخال اسم تجاري للبحث", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String sql = "SELECT code, ar_name FROM business_parties WHERE party_type = ? AND ar_name LIKE ? ORDER BY ar_name LIMIT 10";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, partyType);
            ps.setString(2, "%" + value + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "لا توجد جهة مطابقة", "بحث", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                String code = rs.getString("code");
                loadPartyByCode(code);
                JOptionPane.showMessageDialog(this, "تم جلب البيانات من الجهة: " + rs.getString("ar_name"), "نجاح", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "فشل البحث: " + e.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchAccounts(String value) {
        if (value.isEmpty()) {
            return;
        }
        final String accountType = "supplier".equals(partyType) ? "LIABILITY" : "ASSET";
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                String sql = "SELECT account_code, account_name FROM chart_of_accounts "
                        + "WHERE is_sub_account = 1 AND account_type = ? "
                        + "AND (account_code LIKE ? OR account_name LIKE ?) "
                        + "ORDER BY account_name LIMIT 8";
                List<String> options = new ArrayList<>();
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    String pattern = "%" + value + "%";
                    ps.setString(1, accountType);
                    ps.setString(2, pattern);
                    ps.setString(3, pattern);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            options.add(rs.getString("account_code") + " - "
                                    + rs.getString("account_name"));
                        }
                    }
                }
                return options;
            }

            @Override
            protected void done() {
                try {
                    if (!value.equals(getCommercialName())) {
                        return;
                    }
                    List<String> options = get();
                    String editorText = getAccountEditor().getText();
                    updatingAccountCombo = true;
                    cmbAccountName.removeAllItems();
                    cmbAccountName.addItem("... اختر أو اكتب للبحث في الشجرة");
                    for (String option : options) {
                        cmbAccountName.addItem(option);
                    }
                    getAccountEditor().setText(editorText);
                    updatingAccountCombo = false;
                    if (!options.isEmpty() && getAccountEditor().hasFocus()) {
                        cmbAccountName.showPopup();
                    }
                } catch (Exception ex) {
                    updatingAccountCombo = false;
                    JOptionPane.showMessageDialog(BusinessPartySetupFrame.this,
                            "فشل البحث في شجرة الحسابات: " + ex.getMessage(),
                            "خطأ", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void loadPartyByCode(String code) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM business_parties WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtCode.setText(rs.getString("code"));
                    setCommercialName(rs.getString("ar_name"));
                    txtEnName.setText(rs.getString("en_name") != null ? rs.getString("en_name") : "");
                    partyType = rs.getString("party_type");
                    if (cmbPartyType != null) cmbPartyType.setSelectedItem(partyType);
                    txtOwnerName.setText(rs.getString("owner_name") != null ? rs.getString("owner_name") : "");
                    txtParentAccount.setText(rs.getString("parent_account_code") != null ? rs.getString("parent_account_code") : "");
                    txtSubAccount.setText(rs.getString("sub_account_code") != null ? rs.getString("sub_account_code") : "");
                    txtOpeningBalance.setText(rs.getDouble("opening_balance") != 0
                            ? String.valueOf(rs.getDouble("opening_balance")) : "");
                    txtCreditLimit.setText(rs.getDouble("credit_limit") != 0
                            ? String.valueOf(rs.getDouble("credit_limit")) : "");
                    txtCreditPeriod.setText(rs.getInt("credit_period_days") != 0
                            ? String.valueOf(rs.getInt("credit_period_days")) : "");
                    txtVatNumber.setText(rs.getString("vat_number") != null ? rs.getString("vat_number") : "");
                    txtPhone.setText(rs.getString("phone") != null ? rs.getString("phone") : "");
                    txtMobile.setText(rs.getString("mobile") != null ? rs.getString("mobile") : "");
                    txtEmail.setText(rs.getString("email") != null ? rs.getString("email") : "");
                    String storedAddress = rs.getString("address") != null ? rs.getString("address") : "";
                    applyAddressValue(storedAddress);
                    txtCrNumber.setText(rs.getString("cr_number") != null ? rs.getString("cr_number") : "");
                    txtCrImagePath.setText(rs.getString("cr_image_path") != null ? rs.getString("cr_image_path") : "");
                    txtContactPerson.setText(rs.getString("contact_person") != null
                            ? rs.getString("contact_person") : "");
                    if (cmbStatus != null && rs.getString("status") != null) {
                        cmbStatus.setSelectedItem(rs.getString("status"));
                    }
                    if (cmbBalanceType != null && rs.getString("balance_type") != null) {
                        cmbBalanceType.setSelectedItem(rs.getString("balance_type"));
                    }
                    if (cmbCurrency != null && rs.getString("currency_code") != null) {
                        cmbCurrency.setSelectedItem(rs.getString("currency_code"));
                    }
                    txtDelegateName.setText("");
                    txtDelegateJob.setText("");
                    txtDelegateDocPath.setText("");
                    editCode = code;
                    isEditMode = true;
                    loadDelegates(code);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadPartyByAccountCode(String accountCode) {
        if (accountCode == null || accountCode.trim().isEmpty()) return;
        String sql = "SELECT code FROM business_parties WHERE sub_account_code = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountCode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    loadPartyByCode(rs.getString("code"));
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "فشل جلب بيانات الجهة المرتبطة بالحساب: "
                            + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // التبويب الثاني: المالية والضريبية
    // =========================================================================
    private JPanel createFinancialTab() {
        JPanel panel = createCardPanel(new GridBagLayout());
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(8, 12, 8, 12);

        int row = 0;
        addLabelField2(panel, gc, row++, "سقف الائتمان:", txtCreditLimit = new JTextField(14));
        addLabelField2(panel, gc, row++, "فترة السداد (أيام):", txtCreditPeriod = new JTextField(10));
        addLabelField2(panel, gc, row++, "العملة:", cmbCurrency = new JComboBox<>(new String[]{"YER", "USD", "SAR", "EUR"}));
        addLabelField2(panel, gc, row++, "الرصيد الافتتاحي:", txtOpeningBalance = new JTextField(12));
        addLabelField2(panel, gc, row++, "نوع الرصيد:", cmbBalanceType = new JComboBox<>(new String[]{"debit", "credit"}));
        addLabelField2(panel, gc, row++, "الرقم الضريبي (ضريبة القيمة المضافة):", txtVatNumber = new JTextField(18));
        addLabelField2(panel, gc, row++, "السجل التجاري:", txtCrNumber = new JTextField(20));
        JPanel crImgPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        crImgPanel.setOpaque(false); crImgPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        crImgPanel.add(new JLabel("مسار صورة السجل التجاري:"));
        crImgPanel.add(txtCrImagePath = new JTextField(15));
        styleField(txtCrImagePath);
        btnBrowseCrImage = new JButton("استعراض");
        styleToolbarButton(btnBrowseCrImage, SECONDARY_COLOR, Color.WHITE);
        btnBrowseCrImage.addActionListener(e -> browseCrImage());
        crImgPanel.add(btnBrowseCrImage);
        gc.gridwidth = 2; panel.add(crImgPanel, gc); row++;

        return panel;
    }

    private void addLabelField2(JPanel form, GridBagConstraints gc, int row, String label, JComponent field) {
        addLabelField(form, gc, row, label, field);
    }

    // =========================================================================
    // التبويب الثالث: المفوضين
    // =========================================================================
    private JPanel createDelegatesTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));
        panel.setBackground(SCREEN_BACKGROUND);
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        String[] dCols = {"الاسم", "الوظيفة", "مسار التفويض"};
        delegateModel = new DefaultTableModel(dCols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        delegateTable = new JTable(delegateModel);
        delegateTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        styleTable(delegateTable);
        JScrollPane scroll = new JScrollPane(delegateTable);
        scroll.setBorder(BorderFactory.createTitledBorder("قائمة المفوضين"));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel form = createCardPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        form.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        form.add(new JLabel("اسم المفوض:"));
        txtDelegateName = new JTextField(12);
        styleField(txtDelegateName);
        form.add(txtDelegateName);
        form.add(new JLabel("الوظيفة:"));
        txtDelegateJob = new JTextField(10);
        styleField(txtDelegateJob);
        form.add(txtDelegateJob);
        form.add(new JLabel("مسار التفويض:"));
        txtDelegateDocPath = new JTextField(15);
        styleField(txtDelegateDocPath);
        form.add(txtDelegateDocPath);
        JButton btnAddDelegate = new JButton("إضافة");
        styleToolbarButton(btnAddDelegate, PRIMARY_COLOR, Color.WHITE);
        btnAddDelegate.addActionListener(e -> addDelegate());
        form.add(btnAddDelegate);
        JButton btnDelDelegate = new JButton("حذف");
        styleToolbarButton(btnDelDelegate, SECONDARY_COLOR, Color.WHITE);
        btnDelDelegate.addActionListener(e -> deleteDelegate());
        form.add(btnDelDelegate);
        JButton btnViewDoc = new JButton("عرض التفويض");
        styleToolbarButton(btnViewDoc, SECONDARY_COLOR, Color.WHITE);
        btnViewDoc.addActionListener(e -> viewDelegateDoc());
        form.add(btnViewDoc);
        JButton btnBrowseDoc = new JButton("استعراض");
        styleToolbarButton(btnBrowseDoc, SECONDARY_COLOR, Color.WHITE);
        btnBrowseDoc.addActionListener(e -> browseDelegateDoc());
        form.add(btnBrowseDoc);
        panel.add(form, BorderLayout.SOUTH);

        return panel;
    }

    private void browseParentAccount() {
        String prefix = "supplier".equals(partyType) ? "2" : "12";
        AccountTreeDialog dialog = new AccountTreeDialog(this, prefix);
        dialog.setVisible(true);
        if (dialog.isAccountSelected()) {
            txtParentAccount.setText(dialog.getSelectedAccountCode());
            txtSubAccount.setText(DatabaseManager.generatePartySubAccountCode(dialog.getSelectedAccountCode()));
        }
    }

    private void browseCrImage() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("اختر صورة السجل التجاري");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtCrImagePath.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void browseDelegateDoc() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("اختر مستند التفويض");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtDelegateDocPath.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void viewDelegateDoc() {
        int row = delegateTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "يرجى تحديد مفوض", "تنبيه", JOptionPane.WARNING_MESSAGE); return; }
        String path = (String) delegateModel.getValueAt(row, 2);
        if (path != null && !path.isEmpty()) {
            try {
                BufferedImage img = ImageIO.read(new File(path));
                if (img != null) {
                    JLabel lbl = new JLabel(new ImageIcon(img.getScaledInstance(400, 300, Image.SCALE_SMOOTH)));
                    JOptionPane.showMessageDialog(this, lbl, "صورة التفويض", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "لا يمكن عرض الملف: " + path, "خطأ", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "خطأ في عرض الصورة: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void generateCode() {
        if (isEditMode) return;
        try {
            String prefix = "supplier".equals(partyType) ? "SUP" : "CUS";
            String sql = "SELECT MAX(code) FROM business_parties WHERE code LIKE ?";
            try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, prefix + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getObject(1) != null) {
                        String max = rs.getString(1);
                        String num = max.substring(prefix.length());
                        int next = Integer.parseInt(num.isEmpty() ? "0" : num) + 1;
                        txtCode.setText(prefix + String.format(Locale.ENGLISH, "%04d", next));
                    } else {
                        txtCode.setText(prefix + "0001");
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void newParty() {
        isEditMode = false;
        editCode = null;
        clearForm();
        generateCode();
        getAccountEditor().requestFocus();
    }

    private void saveParty() {
        if (!validateInput()) return;
        if (hasSimilarAccountName()) return;
        if (DatabaseManager.isPartyCodeExists(txtCode.getText().trim()) && !isEditMode) {
            JOptionPane.showMessageDialog(this, "كود الجهة موجود مسبقاً", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (DatabaseManager.isPartyArNameExists(getCommercialName(),  isEditMode ? editCode : null)) {
            JOptionPane.showMessageDialog(this, "الاسم التجاري موجود مسبقاً", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (txtVatNumber.getText().trim().length() > 0 && DatabaseManager.isVatNumberExists(txtVatNumber.getText().trim(), isEditMode ? editCode : null)) {
            JOptionPane.showMessageDialog(this, "الرقم الضريبي موجود مسبقاً", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (txtSubAccount.getText().trim().isEmpty()) {
            txtSubAccount.setText(DatabaseManager.generatePartySubAccountCode("121"));
        }

        try {
            Connection conn = DatabaseManager.getConnection();
            List<String[]> delegates = new ArrayList<>();
            String delegateName = txtDelegateName.getText().trim();
            String delegateJob = txtDelegateJob.getText().trim();
            String delegatePath = txtDelegateDocPath.getText().trim();
            if (!delegateName.isEmpty() || !delegateJob.isEmpty() || !delegatePath.isEmpty()) {
                delegates.add(new String[]{delegateName, delegateJob, delegatePath});
            }
            for (int i = 0; i < delegateModel.getRowCount(); i++) {
                delegates.add(new String[]{
                        (String) delegateModel.getValueAt(i, 0),
                        (String) delegateModel.getValueAt(i, 1),
                        (String) delegateModel.getValueAt(i, 2)
                });
            }
            String fullAddress = buildAddressValue();
            boolean success;
            if (isEditMode) {
                success = DatabaseManager.updatePartyWithAccount(conn,
                        txtCode.getText().trim(), getCommercialName(), txtEnName.getText().trim(),
                        partyType, txtOwnerName.getText().trim(), "121",
                        txtSubAccount.getText().trim(),
                        parseDouble(txtCreditLimit.getText()), parseInt(txtCreditPeriod.getText()),
                        (String) cmbCurrency.getSelectedItem(),
                        parseDouble(txtOpeningBalance.getText()), (String) cmbBalanceType.getSelectedItem(),
                        txtVatNumber.getText().trim(), txtCrNumber.getText().trim(),
                        txtCrImagePath.getText().trim(), txtPhone.getText().trim(),
                        txtMobile.getText().trim(), txtEmail.getText().trim(),
                        fullAddress, txtContactPerson.getText().trim(),
                        delegates);
            } else {
                success = DatabaseManager.savePartyWithAccount(conn,
                        txtCode.getText().trim(), getCommercialName(), txtEnName.getText().trim(),
                        partyType, txtOwnerName.getText().trim(), "121",
                        txtSubAccount.getText().trim(),
                        parseDouble(txtCreditLimit.getText()), parseInt(txtCreditPeriod.getText()),
                        (String) cmbCurrency.getSelectedItem(),
                        parseDouble(txtOpeningBalance.getText()), (String) cmbBalanceType.getSelectedItem(),
                        txtVatNumber.getText().trim(), txtCrNumber.getText().trim(),
                        txtCrImagePath.getText().trim(), txtPhone.getText().trim(),
                        txtMobile.getText().trim(), txtEmail.getText().trim(),
                        fullAddress, txtContactPerson.getText().trim(),
                        delegates);
            }
            if (success) {
                JOptionPane.showMessageDialog(this, "تم حفظ بيانات " + (("supplier".equals(partyType)) ? "المورد" : "الزبون") + " بنجاح مع الربط المحاسبي", "نجاح", JOptionPane.INFORMATION_MESSAGE);
                loadPartyList();
                clearForm();
                generateCode();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "فشل الحفظ: " + e.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean hasSimilarAccountName() {
        String term = getCommercialName();
        if (term.isEmpty()) return false;

        StringBuilder names = new StringBuilder();
        String selectedAccountCode = txtSubAccount.getText().trim();
        String accountSql = "SELECT account_code, account_name FROM chart_of_accounts "
                + "WHERE is_sub_account = 1 AND account_name LIKE ? "
                + "AND account_code <> ? ORDER BY account_name LIMIT 10";
        String partySql = "SELECT code, ar_name FROM business_parties WHERE ar_name LIKE ? "
                + (isEditMode ? "AND code <> ? " : "") + "ORDER BY ar_name LIMIT 10";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement account = conn.prepareStatement(accountSql);
             PreparedStatement party = conn.prepareStatement(partySql)) {
            String pattern = "%" + term + "%";
            account.setString(1, pattern);
            account.setString(2, selectedAccountCode);
            try (ResultSet rs = account.executeQuery()) {
                while (rs.next()) {
                    String accountCode = rs.getString("account_code");
                    if (accountCode.equals(selectedAccountCode)) {
                        continue;
                    }
                    names.append("حساب: ").append(accountCode)
                            .append(" - ").append(rs.getString("account_name")).append("\n");
                }
            }
            party.setString(1, pattern);
            if (isEditMode) party.setString(2, editCode);
            try (ResultSet rs = party.executeQuery()) {
                while (rs.next()) {
                    names.append("جهة: ").append(rs.getString("code"))
                            .append(" - ").append(rs.getString("ar_name")).append("\n");
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "فشل التحقق من تشابه الاسم: " + ex.getMessage(),
                    "خطأ", JOptionPane.ERROR_MESSAGE);
            return true;
        }
        if (names.length() == 0) return false;
        JOptionPane.showMessageDialog(this,
                "يوجد اسم مطابق أو مشابه. راجع النتائج قبل فتح حساب جديد:\n\n" + names,
                "تحذير: اسم مكرر أو مشابه", JOptionPane.WARNING_MESSAGE);
        return true;
    }

    private void editParty() {
        if (editCode == null || editCode.isEmpty()) {
            JOptionPane.showMessageDialog(this, "يرجى تحديد جهة من القائمة لتعديلها", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }
        isEditMode = true;
        saveParty();
    }

    private void deleteParty() {
        if (editCode == null || editCode.isEmpty()) {
            JOptionPane.showMessageDialog(this, "يرجى تحديد جهة من القائمة لحذفها", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "هل أنت متأكد من حذف الجهة [" + editCode + "]؟", "تأكيد الحذف", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (DatabaseManager.deleteParty(editCode)) {
                JOptionPane.showMessageDialog(this, "تم حذف الجهة بنجاح", "نجاح", JOptionPane.INFORMATION_MESSAGE);
                loadPartyList();
                clearForm();
                generateCode();
            } else {
                JOptionPane.showMessageDialog(this, "فشل حذف الجهة", "خطأ", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void addDelegate() {
        if (txtDelegateName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "يرجى إدخال اسم المفوض", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }
        delegateModel.addRow(new Object[]{txtDelegateName.getText().trim(), txtDelegateJob.getText().trim(), txtDelegateDocPath.getText().trim()});
        txtDelegateName.setText(""); txtDelegateJob.setText(""); txtDelegateDocPath.setText("");
    }

    private void deleteDelegate() {
        int row = delegateTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "يرجى تحديد مفوض", "تنبيه", JOptionPane.WARNING_MESSAGE); return; }
        delegateModel.removeRow(row);
    }



    private void loadPartyList() {
        partyModel.setRowCount(0);
        StringBuilder sql = new StringBuilder("SELECT * FROM business_parties WHERE 1=1");
        java.util.ArrayList<String> params = new java.util.ArrayList<>();
        if (partyType != null && !partyType.isEmpty()) { sql.append(" AND party_type = ?"); params.add(partyType); }
        String searchValue = (txtSearch != null) ? txtSearch.getText().trim() : "";
        if (!searchValue.isEmpty()) {
            sql.append(" AND (ar_name LIKE ? OR code LIKE ? OR vat_number LIKE ?)");
            params.add("%" + searchValue + "%"); params.add("%" + searchValue + "%"); params.add("%" + searchValue + "%");
        }
        sql.append(" ORDER BY created_at DESC");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) { ps.setString(i + 1, params.get(i)); }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    partyModel.addRow(new Object[]{
                            rs.getString("code"), rs.getString("ar_name"),
                            rs.getString("party_type"), rs.getString("status"),
                            rs.getString("vat_number") != null ? rs.getString("vat_number") : ""
                    });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void filterPartyList() {
        loadPartyList();
    }

    private void loadSelectedParty() {
        int row = partyTable.getSelectedRow();
        if (row < 0) return;
        editCode = (String) partyModel.getValueAt(row, 0);
        isEditMode = true;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM business_parties WHERE code = ?")) {
            ps.setString(1, editCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtCode.setText(rs.getString("code"));
                    setCommercialName(rs.getString("ar_name"));
                    txtEnName.setText(rs.getString("en_name") != null ? rs.getString("en_name") : "");
                    partyType = rs.getString("party_type");
                    if (rs.getString("party_type") != null) cmbPartyType.setSelectedItem(rs.getString("party_type"));
                    txtOwnerName.setText(rs.getString("owner_name") != null ? rs.getString("owner_name") : "");
                    txtParentAccount.setText(rs.getString("parent_account_code") != null ? rs.getString("parent_account_code") : "");
                    txtSubAccount.setText(rs.getString("sub_account_code") != null ? rs.getString("sub_account_code") : "");
                    txtOpeningBalance.setText(rs.getDouble("opening_balance") != 0 ? String.valueOf(rs.getDouble("opening_balance")) : "");
                    txtCreditLimit.setText(rs.getDouble("credit_limit") != 0 ? String.valueOf(rs.getDouble("credit_limit")) : "");
                    txtCreditPeriod.setText(rs.getInt("credit_period_days") != 0 ? String.valueOf(rs.getInt("credit_period_days")) : "");
                    txtVatNumber.setText(rs.getString("vat_number") != null ? rs.getString("vat_number") : "");
                    txtCrNumber.setText(rs.getString("cr_number") != null ? rs.getString("cr_number") : "");
                    txtCrImagePath.setText(rs.getString("cr_image_path") != null ? rs.getString("cr_image_path") : "");
                    txtPhone.setText(rs.getString("phone") != null ? rs.getString("phone") : "");
                    txtMobile.setText(rs.getString("mobile") != null ? rs.getString("mobile") : "");
                    txtEmail.setText(rs.getString("email") != null ? rs.getString("email") : "");
                    String storedAddress = rs.getString("address") != null ? rs.getString("address") : "";
                    applyAddressValue(storedAddress);
                    txtContactPerson.setText(rs.getString("contact_person") != null ? rs.getString("contact_person") : "");
                    if (rs.getString("status") != null) cmbStatus.setSelectedItem(rs.getString("status"));
                    if (rs.getString("balance_type") != null) cmbBalanceType.setSelectedItem(rs.getString("balance_type"));
                    if (rs.getString("currency_code") != null) cmbCurrency.setSelectedItem(rs.getString("currency_code"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        loadDelegates(editCode);
    }

    private void loadDelegates(String code) {
        delegateModel.setRowCount(0);
        String sql = "SELECT * FROM party_delegates WHERE party_code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                boolean firstDelegate = true;
                while (rs.next()) {
                    String delegateName = rs.getString("delegate_name");
                    String jobTitle = rs.getString("job_title");
                    String documentPath = rs.getString("authorization_doc_path");
                    delegateModel.addRow(new Object[]{delegateName, jobTitle, documentPath});
                    if (firstDelegate) {
                        txtDelegateName.setText(delegateName != null ? delegateName : "");
                        txtDelegateJob.setText(jobTitle != null ? jobTitle : "");
                        txtDelegateDocPath.setText(documentPath != null ? documentPath : "");
                        firstDelegate = false;
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void clearForm() {
        isEditMode = false;
        editCode = null;
        txtCode.setText(""); setCommercialName(""); txtEnName.setText("");
        txtOwnerName.setText(""); txtParentAccount.setText(""); txtSubAccount.setText("");
        txtOpeningBalance.setText(""); txtCreditLimit.setText(""); txtCreditPeriod.setText("");
        txtVatNumber.setText(""); txtCrNumber.setText(""); txtCrImagePath.setText("");
        txtPhone.setText(""); txtMobile.setText(""); txtEmail.setText("");
        txtCity.setText(""); txtAddress.setText(""); txtContactPerson.setText("");
        txtDelegateName.setText(""); txtDelegateJob.setText(""); txtDelegateDocPath.setText("");
        delegateModel.setRowCount(0);
        cmbStatus.setSelectedIndex(0);
        cmbBalanceType.setSelectedIndex(0);
        cmbCurrency.setSelectedIndex(0);
        if (cmbPartyType != null) cmbPartyType.setSelectedIndex("supplier".equals(partyType) ? 0 : 1);
        partyType = "supplier";
    }

    private boolean validateInput() {
        if (getCommercialName().isEmpty()) {
            JOptionPane.showMessageDialog(this, "يجب إدخال الاسم التجاري", "تنبيه", JOptionPane.WARNING_MESSAGE);
            getAccountEditor().requestFocus();
            return false;
        }
        return true;
    }

    private void showReport() {
        JOptionPane.showMessageDialog(this, "تم فتح تقرير " + (("supplier".equals(partyType)) ? "الموردين" : "العملاء"), "تقرير", JOptionPane.INFORMATION_MESSAGE);
    }

    private String buildAddressValue() {
        StringBuilder sb = new StringBuilder();
        String city = txtCity != null ? txtCity.getText().trim() : "";
        String address = txtAddress != null ? txtAddress.getText().trim() : "";
        if (!city.isEmpty()) sb.append(city);
        if (!address.isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(address);
        }
        return sb.toString();
    }

    private void applyAddressValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            txtCity.setText("");
            txtAddress.setText("");
            return;
        }
        int sep = value.indexOf(" - ");
        if (sep >= 0) {
            txtCity.setText(value.substring(0, sep).trim());
            txtAddress.setText(value.substring(sep + 3).trim());
        } else {
            txtCity.setText(value.trim());
            txtAddress.setText("");
        }
    }

    private double parseDouble(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        try { return Double.parseDouble(s.trim().replace(",", "")); } catch (NumberFormatException e) { return 0; }
    }

    private int parseInt(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private static class RoundedBorder extends javax.swing.border.AbstractBorder {
        private static final long serialVersionUID = 1L;
        private final Color color;
        private final int radius;

        RoundedBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setColor(color);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new BusinessPartySetupFrame("supplier").setVisible(true);
        });
    }
}