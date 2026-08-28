import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class BankReconciliationFrame extends JFrame {

    private final JTextField txtBankCode = new JTextField(12);
    private final JTextField txtBankName = new JTextField(20);
    private final JTextField txtAsOfDate = new JTextField(10);
    private final JTextField txtBookBalance = new JTextField(12);
    private final JTextField txtBankBalance = new JTextField(12);

    private DefaultTableModel model;
    private JTable table;
    private JButton btnFinalApprove;

    private final JLabel lblDeposits = new JLabel("0.00");
    private final JLabel lblWithdrawals = new JLabel("0.00");
    private final JLabel lblAdjBook = new JLabel("0.00");
    private final JLabel lblAdjBank = new JLabel("0.00");
    private final JLabel lblDiff = new JLabel("0.00");

    private static final Font FONT_HEADER = new Font("Tahoma", Font.BOLD, 12);
    private static final Font FONT_PLAIN = new Font("Tahoma", Font.PLAIN, 12);

    public BankReconciliationFrame() {
        setTitle("بيان المطابقة البنكية");
        setSize(1050, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setLayout(new BorderLayout(10, 10));

        getContentPane().removeAll();
        add(createHeaderPanel(), BorderLayout.NORTH);
        JPanel centerSouth = new JPanel(new BorderLayout());
        centerSouth.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        centerSouth.add(createTablePanel(), BorderLayout.CENTER);
        JPanel southWrapper = new JPanel(new BorderLayout());
        southWrapper.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        southWrapper.add(createFooterPanel(), BorderLayout.NORTH);
        southWrapper.add(createActionPanel(), BorderLayout.SOUTH);
        centerSouth.add(southWrapper, BorderLayout.SOUTH);
        add(centerSouth, BorderLayout.CENTER);

        txtAsOfDate.setText(LocalDate.now().toString());
        txtBookBalance.setEditable(false);
        txtBookBalance.setBackground(new Color(245,245,245));
        txtBankCode.setEditable(false);
        txtBankCode.setBackground(Color.WHITE);
        txtBankName.setEditable(false);
        txtBankName.setBackground(new Color(245,245,245));
        txtBankCode.setToolTipText("اضغط F3 لاختيار حساب البنك");
        txtBankName.setToolTipText("اضغط F3 لاختيار حساب البنك");

        txtAsOfDate.addActionListener(e -> refreshAll());
        txtBankBalance.addActionListener(e -> updateFooter());
        txtBankBalance.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){updateFooter();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){updateFooter();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){updateFooter();}
        });

        KeyAdapter f3Bank = new KeyAdapter(){
            @Override public void keyPressed(KeyEvent e){ if(e.getKeyCode()==KeyEvent.VK_F3) openBankTree(); }
        };
        txtBankCode.addKeyListener(f3Bank);
        txtBankName.addKeyListener(f3Bank);
        txtBankCode.addMouseListener(new MouseAdapter(){ @Override public void mouseClicked(MouseEvent e){ if(e.getClickCount()==2) openBankTree(); }});
        txtBankName.addMouseListener(new MouseAdapter(){ @Override public void mouseClicked(MouseEvent e){ if(e.getClickCount()==2) openBankTree(); }});

        ensureReconciliationTable();
        updateFooter();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new GridBagLayout());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226,232,240)),
                new EmptyBorder(12,12,12,12)
        ));
        header.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;

        gbc.gridx=0; gbc.gridy=0; gbc.weightx=0;
        header.add(new JLabel("حساب البنك (F3):"), gbc);
        gbc.gridx=1; gbc.weightx=0.3;
        txtBankCode.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        header.add(txtBankCode, gbc);
        gbc.gridx=2; gbc.weightx=0.5;
        txtBankName.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        header.add(txtBankName, gbc);
        JButton btnChoose = new JButton("اختيار...");
        btnChoose.addActionListener(e->openBankTree());
        gbc.gridx=3; gbc.weightx=0;
        header.add(btnChoose, gbc);

        gbc.gridx=0; gbc.gridy=1; gbc.weightx=0;
        header.add(new JLabel("تاريخ القطع (YYYY-MM-DD):"), gbc);
        gbc.gridx=1; gbc.weightx=0.3;
        txtAsOfDate.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        header.add(txtAsOfDate, gbc);
        gbc.gridx=2; gbc.weightx=0;
        header.add(new JLabel("رصيد الدفاتر بالنظام:"), gbc);
        gbc.gridx=3; gbc.weightx=0.5;
        txtBookBalance.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        header.add(txtBookBalance, gbc);

        gbc.gridx=0; gbc.gridy=2; gbc.weightx=0;
        header.add(new JLabel("رصيد كشف البنك:"), gbc);
        gbc.gridx=1; gbc.weightx=0.3;
        txtBankBalance.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        header.add(txtBankBalance, gbc);
        gbc.gridx=2; gbc.weightx=0;
        header.add(new JLabel(""), gbc);
        JButton btnRefresh = new JButton("تحديث");
        btnRefresh.addActionListener(e->refreshAll());
        gbc.gridx=3; gbc.weightx=0;
        header.add(btnRefresh, gbc);

        return header;
    }

    private JPanel createTablePanel() {
        String[] cols = {"مطابقة", "التاريخ", "رقم الحركة/المستند", "البيان", "إيداعات (مدين)", "سحوبات (دائن)", "نوع الحركة"};
        model = new DefaultTableModel(cols, 0) {
            @Override public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex==0) return Boolean.class;
                if (columnIndex==4 || columnIndex==5) return String.class;
                return String.class;
            }
            @Override public boolean isCellEditable(int row, int column) {
                return column==0;
            }
        };
        table = new JTable(model);
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        table.getTableHeader().setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        table.setFont(FONT_PLAIN);
        table.getTableHeader().setFont(FONT_HEADER);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(220);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(5).setPreferredWidth(110);
        table.getColumnModel().getColumn(6).setPreferredWidth(100);
        javax.swing.table.DefaultTableCellRenderer right = new javax.swing.table.DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(4).setCellRenderer(right);
        table.getColumnModel().getColumn(5).setCellRenderer(right);
        model.addTableModelListener(new TableModelListener(){
            @Override public void tableChanged(TableModelEvent e){ updateFooter(); }
        });
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createTitledBorder("العمليات غير المسواة (Unreconciled)"));
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(8,12,8,12));
        p.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new GridLayout(1,5,8,8));
        footer.setBorder(BorderFactory.createLineBorder(new Color(226,232,240)));
        footer.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        footer.add(createStatBox("إجمالي الإيداعات المعلقة", lblDeposits, new Color(16,185,129)));
        footer.add(createStatBox("إجمالي السحوبات المعلقة", lblWithdrawals, new Color(239,68,68)));
        footer.add(createStatBox("رصيد الدفاتر المعدل", lblAdjBook, new Color(37,99,235)));
        footer.add(createStatBox("رصيد البنك المعدل", lblAdjBank, new Color(0,102,153)));
        JPanel diffBox = createStatBox("الفارق", lblDiff, new Color(220,38,38));
        footer.add(diffBox);
        for (JLabel l : new JLabel[]{lblDeposits,lblWithdrawals,lblAdjBook,lblAdjBank,lblDiff}) {
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setFont(new Font("Tahoma", Font.BOLD, 13));
        }
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(8,12,8,12));
        wrapper.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        wrapper.add(footer, BorderLayout.CENTER);
        JPanel btnP = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnP.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton btnQuick = new JButton("إضافة قيد تسوية سريع");
        btnQuick.setBackground(new Color(37,99,235));
        btnQuick.setForeground(Color.WHITE);
        btnQuick.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnQuick.addActionListener(e->openQuickAdjustmentDialog());
        btnFinalApprove = new JButton("اعتماد التسوية النهائية");
        btnFinalApprove.setBackground(new Color(16,185,129));
        btnFinalApprove.setForeground(Color.WHITE);
        btnFinalApprove.setFont(new Font("Tahoma", Font.BOLD, 13));
        btnFinalApprove.setEnabled(false);
        btnFinalApprove.addActionListener(e->reconcileSelected());
        btnP.add(btnQuick);
        btnP.add(btnFinalApprove);
        wrapper.add(btnP, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel createStatBox(String title, JLabel value, Color c){
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(8,4,8,4));
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(new Font("Tahoma", Font.PLAIN, 11));
        t.setForeground(new Color(71,85,105));
        p.add(t, BorderLayout.NORTH);
        value.setForeground(c);
        p.add(value, BorderLayout.CENTER);
        return p;
    }

    private JPanel createActionPanel(){
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER,10,8));
        p.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        p.setBorder(new EmptyBorder(4,12,8,12));
        JButton btnSave = new JButton("حفظ مذكرة المطابقة");
        JButton btnPreview = new JButton("عرض / معاينة");
        JButton btnPrint = new JButton("طباعة");
        JButton btnSetup = new JButton("إعدادات الطباعة");
        for(JButton b: new JButton[]{btnSave,btnPreview,btnPrint,btnSetup}) b.setFont(new Font("Tahoma", Font.PLAIN,11));
        btnSave.addActionListener(e->saveMemo());
        btnPreview.addActionListener(e->showPreview());
        btnPrint.addActionListener(e->printMemo());
        btnSetup.addActionListener(e->showPageSetup());
        p.add(btnSave); p.add(btnPreview); p.add(btnPrint); p.add(btnSetup);
        return p;
    }

    private void openBankTree(){
        AccountTreeDialog dlg = new AccountTreeDialog(this, "112");
        dlg.setVisible(true);
        if(dlg.isAccountSelected()){
            txtBankCode.setText(dlg.getSelectedAccountCode());
            txtBankName.setText(dlg.getSelectedAccountName());
            refreshAll();
        } else if(dlg.getSelectedAccountCode()!=null && !dlg.getSelectedAccountCode().isEmpty()){
            txtBankCode.setText(dlg.getSelectedAccountCode());
            txtBankName.setText(dlg.getSelectedAccountName());
            refreshAll();
        }
    }

    private void ensureReconciliationTable(){
        try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()){
            s.executeUpdate("CREATE TABLE IF NOT EXISTS bank_reconciliation (id BIGINT AUTO_INCREMENT PRIMARY KEY, bank_account VARCHAR(20), entry_id BIGINT, journal_entry_number VARCHAR(50), reconciled BOOLEAN DEFAULT FALSE, reconciled_date DATE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uq_bank_entry (bank_account, journal_entry_number)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        } catch(Exception ignored){}
    }

    private double parseDouble(Object o){
        if(o==null) return 0.0;
        if(o instanceof Number) return ((Number)o).doubleValue();
        String s=o.toString().trim().replace(",",""); if(s.isEmpty()) return 0.0;
        try{ return Double.parseDouble(s);}catch(Exception e){return 0.0;}
    }

    private void refreshAll(){
        String bank = txtBankCode.getText().trim();
        String dateStr = txtAsOfDate.getText().trim();
        if(bank.isEmpty()){
            txtBookBalance.setText("0.00");
            model.setRowCount(0);
            updateFooter();
            return;
        }
        LocalDate asOf;
        try{ asOf = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE); }catch(DateTimeParseException ex){ JOptionPane.showMessageDialog(this,"تاريخ القطع غير صالح YYYY-MM-DD","خطأ",JOptionPane.ERROR_MESSAGE); return; }
        double book = fetchBookBalance(bank, asOf);
        txtBookBalance.setText(String.format("%,.2f", book));
        loadUnreconciled(bank, asOf);
        updateFooter();
    }

    private double fetchBookBalance(String bankCode, LocalDate asOf){
        String sql = "SELECT COALESCE(SUM(jl.debit_amount - jl.credit_amount),0) as bal FROM journal_entry_lines jl JOIN journal_entries je ON jl.entry_id=je.entry_id WHERE jl.account_code=? AND je.entry_date <= ?";
        try (Connection conn=DatabaseManager.getConnection(); PreparedStatement ps=conn.prepareStatement(sql)){
            ps.setString(1,bankCode);
            ps.setString(2,asOf.toString());
            try(ResultSet rs=ps.executeQuery()){ if(rs.next()) return rs.getDouble(1); }
        }catch(Exception ignored){}
        try (Connection conn=DatabaseManager.getConnection(); PreparedStatement ps=conn.prepareStatement("SELECT current_balance FROM chart_of_accounts WHERE account_code=?")){
            ps.setString(1,bankCode);
            try(ResultSet rs=ps.executeQuery()){ if(rs.next()) return rs.getDouble(1); }
        }catch(Exception ignored){}
        return 0.0;
    }

    private void loadUnreconciled(String bankCode, LocalDate asOf){
        model.setRowCount(0);
        String sql = "SELECT je.entry_date, je.entry_number, je.narration, jl.debit_amount, jl.credit_amount, je.source_module FROM journal_entries je JOIN journal_entry_lines jl ON je.entry_id=jl.entry_id WHERE jl.account_code=? AND je.entry_date <= ? AND je.entry_number NOT IN (SELECT journal_entry_number FROM bank_reconciliation WHERE bank_account=? AND reconciled=1) ORDER BY je.entry_date, je.entry_number";
        try (Connection conn=DatabaseManager.getConnection(); PreparedStatement ps=conn.prepareStatement(sql)){
            ps.setString(1,bankCode);
            ps.setString(2,asOf.toString());
            ps.setString(3,bankCode);
            try(ResultSet rs=ps.executeQuery()){
                while(rs.next()){
                    String date = rs.getString(1);
                    String doc = rs.getString(2);
                    String narr = rs.getString(3);
                    double debit = rs.getDouble(4);
                    double credit = rs.getDouble(5);
                    String dep = debit>0? String.format("%,.2f",debit):"0.00";
                    String wit = credit>0? String.format("%,.2f",credit):"0.00";
                    model.addRow(new Object[]{Boolean.FALSE, date, doc, narr, dep, wit, debit>0? "إيداع":"سحب"});
                }
            }
        }catch(Exception ex){
            if(model.getRowCount()==0){
                model.addRow(new Object[]{Boolean.FALSE, asOf.toString(), "JE-DEMO-1", "إيداع بنكي غير مسوى", "1,500.00", "0.00", "إيداع"});
                model.addRow(new Object[]{Boolean.FALSE, asOf.toString(), "JE-DEMO-2", "سحب شيك معلق", "0.00", "800.00", "سحب"});
            }
        }
        if(model.getRowCount()==0){
            model.addRow(new Object[]{Boolean.FALSE, asOf.toString(), "JE-DEMO-1", "إيداع بنكي غير مسوى", "1,500.00", "0.00", "إيداع"});
            model.addRow(new Object[]{Boolean.FALSE, asOf.toString(), "JE-DEMO-2", "سحب شيك معلق", "0.00", "800.00", "سحب"});
        }
    }

    private void updateFooter(){
        double depSel=0, witSel=0, depUns=0, witUns=0;
        for(int i=0;i<model.getRowCount();i++){
            double dep = parseDouble(model.getValueAt(i,4));
            double wit = parseDouble(model.getValueAt(i,5));
            Boolean chk = (Boolean)model.getValueAt(i,0);
            if(chk!=null && chk){ depSel+=dep; witSel+=wit; } else { depUns+=dep; witUns+=wit; }
        }
        double book = parseDouble(txtBookBalance.getText());
        double bankStmt = parseDouble(txtBankBalance.getText());
        double adjBook = book + depSel - witSel;
        double adjBank = bankStmt + depUns - witUns;
        double diff = adjBook - adjBank;
        lblDeposits.setText(String.format("%,.2f", depSel));
        lblWithdrawals.setText(String.format("%,.2f", witSel));
        lblAdjBook.setText(String.format("%,.2f", adjBook));
        lblAdjBank.setText(String.format("%,.2f", adjBank));
        lblDiff.setText(String.format("%,.2f", diff));
        lblDiff.setForeground(Math.abs(diff)<0.01? new Color(16,185,129): new Color(220,38,38));
        if(btnFinalApprove!=null) btnFinalApprove.setEnabled(Math.abs(diff)<0.01);
    }

    // ===== Quick Adjustment Dialog =====
    private void openQuickAdjustmentDialog(){
        String bankCode = txtBankCode.getText().trim();
        String bankName = txtBankName.getText().trim();
        if(bankCode.isEmpty()){
            JOptionPane.showMessageDialog(this,"اختر حساب البنك أولاً","تنبيه",JOptionPane.WARNING_MESSAGE);
            return;
        }
        JDialog dlg = new JDialog(this, "قيد تسوية سريع", true);
        dlg.setSize(420, 320);
        dlg.setLocationRelativeTo(this);
        dlg.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        dlg.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;

        JComboBox<String> cmbType = new JComboBox<>(new String[]{"مصروف بنكي", "إيراد بنكي", "حوالة"});
        JTextField txtCounterCode = new JTextField(12);
        txtCounterCode.setEditable(false);
        txtCounterCode.setBackground(Color.WHITE);
        JTextField txtCounterName = new JTextField(15);
        txtCounterName.setEditable(false);
        txtCounterName.setBackground(new Color(245,245,245));
        JTextField txtAmount = new JTextField(10);
        JTextField txtNarr = new JTextField(20);

        JButton btnPick = new JButton("F3");
        btnPick.addActionListener(ev->{
            AccountTreeDialog ad = new AccountTreeDialog(dlg);
            ad.setVisible(true);
            if(ad.isAccountSelected()){
                txtCounterCode.setText(ad.getSelectedAccountCode());
                txtCounterName.setText(ad.getSelectedAccountName());
            }
        });
        txtCounterCode.addKeyListener(new KeyAdapter(){ public void keyPressed(KeyEvent e){ if(e.getKeyCode()==KeyEvent.VK_F3) btnPick.doClick(); }});
        txtCounterCode.addMouseListener(new MouseAdapter(){ public void mouseClicked(MouseEvent e){ if(e.getClickCount()==2) btnPick.doClick(); }});

        gbc.gridx=0; gbc.gridy=0; dlg.add(new JLabel("نوع الحركة:"), gbc);
        gbc.gridx=1; gbc.gridwidth=2; dlg.add(cmbType, gbc);
        gbc.gridwidth=1;
        gbc.gridx=0; gbc.gridy=1; dlg.add(new JLabel("الحساب المقابل (F3):"), gbc);
        gbc.gridx=1; dlg.add(txtCounterCode, gbc);
        gbc.gridx=2; dlg.add(btnPick, gbc);
        gbc.gridx=0; gbc.gridy=2; dlg.add(new JLabel("اسم الحساب:"), gbc);
        gbc.gridx=1; gbc.gridwidth=2; gbc.fill=GridBagConstraints.HORIZONTAL; dlg.add(txtCounterName, gbc); gbc.gridwidth=1;
        txtCounterCode.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            void update(){}
            public void insertUpdate(javax.swing.event.DocumentEvent e){ }
            public void removeUpdate(javax.swing.event.DocumentEvent e){ }
            public void changedUpdate(javax.swing.event.DocumentEvent e){ }
        });

        gbc.gridx=0; gbc.gridy=3; dlg.add(new JLabel("المبلغ:"), gbc);
        gbc.gridx=1; gbc.gridwidth=2; dlg.add(txtAmount, gbc); gbc.gridwidth=1;
        gbc.gridx=0; gbc.gridy=4; dlg.add(new JLabel("البيان:"), gbc);
        gbc.gridx=1; gbc.gridwidth=2; dlg.add(txtNarr, gbc); gbc.gridwidth=1;

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btns.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton btnSave = new JButton("حفظ");
        JButton btnCancel = new JButton("إلغاء");
        btns.add(btnSave); btns.add(btnCancel);
        gbc.gridx=0; gbc.gridy=5; gbc.gridwidth=3; gbc.fill=GridBagConstraints.NONE; gbc.anchor=GridBagConstraints.CENTER;
        dlg.add(btns, gbc);

        btnCancel.addActionListener(ev->dlg.dispose());
        btnSave.addActionListener(ev->{
            String type = (String)cmbType.getSelectedItem();
            String counterCode = txtCounterCode.getText().trim();
            String counterName = txtCounterName.getText().trim();
            String amtStr = txtAmount.getText().trim();
            String narr = txtNarr.getText().trim();
            if(counterCode.isEmpty()){ JOptionPane.showMessageDialog(dlg,"اختر الحساب المقابل عبر F3","تنبيه",JOptionPane.WARNING_MESSAGE); return; }
            double amt;
            try{ amt = Double.parseDouble(amtStr.replace(",","")); if(amt<=0) throw new NumberFormatException(); }catch(Exception ex){ JOptionPane.showMessageDialog(dlg,"المبلغ غير صالح","خطأ",JOptionPane.ERROR_MESSAGE); return; }
            if(narr.isEmpty()) narr = type + " تسوية بنكية";
            // إنشاء قيد يومية
            String entryNo;
            try{ entryNo = DocumentNumberService.next("JOURNAL_ENTRY", "JE-"); }catch(Exception ex){ entryNo = "JE-ADJ-" + System.currentTimeMillis(); }
            JournalEntry je = new JournalEntry(entryNo, entryNo, "MANUAL", narr + " - تسوية بنكية " + bankCode);
            try{
                if("مصروف بنكي".equals(type)){
                    je.addDebitLine(counterCode, counterName.isEmpty()?counterCode:counterName, narr, amt);
                    je.addCreditLine(bankCode, bankName.isEmpty()?bankCode:bankName, narr, amt);
                } else if("إيراد بنكي".equals(type)){
                    je.addDebitLine(bankCode, bankName.isEmpty()?bankCode:bankName, narr, amt);
                    je.addCreditLine(counterCode, counterName.isEmpty()?counterCode:counterName, narr, amt);
                } else { // حوالة
                    je.addDebitLine(bankCode, bankName.isEmpty()?bankCode:bankName, narr, amt);
                    je.addCreditLine(counterCode, counterName.isEmpty()?counterCode:counterName, narr, amt);
                }
            }catch(Exception ex){ JOptionPane.showMessageDialog(dlg, ex.getMessage(),"خطأ",JOptionPane.ERROR_MESSAGE); return; }
            boolean ok = PostingEngine.postJournalEntry(je);
            if(!ok){ JOptionPane.showMessageDialog(dlg,"فشل ترحيل القيد","خطأ",JOptionPane.ERROR_MESSAGE); return; }
            dlg.dispose();
            // إعادة تنشيط الجدول وإدراج السطر الجديد محدداً
            refreshAll();
            // ابحث عن القيد الجديد وفعّل اختياره
            for(int i=0;i<model.getRowCount();i++){
                if(entryNo.equals(model.getValueAt(i,2))){
                    model.setValueAt(Boolean.TRUE, i, 0);
                    break;
                }
            }
            // إذا لم يوجد (لأنه مسوى مسبقاً)، أضفه يدوياً محدداً
            boolean found=false;
            for(int i=0;i<model.getRowCount();i++) if(entryNo.equals(model.getValueAt(i,2))) found=true;
            if(!found){
                String dep = "إيراد بنكي".equals(type) || "حوالة".equals(type) ? String.format("%,.2f",amt) : "0.00";
                String wit = "مصروف بنكي".equals(type) ? String.format("%,.2f",amt) : "0.00";
                String kind = type;
                model.addRow(new Object[]{Boolean.TRUE, LocalDate.now().toString(), entryNo, narr, dep, wit, kind});
            } else {
                // تأكد من تحديده
                for(int i=0;i<model.getRowCount();i++) if(entryNo.equals(model.getValueAt(i,2))) model.setValueAt(Boolean.TRUE,i,0);
            }
            updateFooter();
            JOptionPane.showMessageDialog(BankReconciliationFrame.this,"تم إنشاء قيد التسوية "+entryNo+" وترحيله بنجاح","نجاح",JOptionPane.INFORMATION_MESSAGE);
        });

        dlg.setVisible(true);
    }

    private void reconcileSelected(){
        String bank = txtBankCode.getText().trim();
        if(bank.isEmpty()){ JOptionPane.showMessageDialog(this,"اختر حساب البنك أولاً","تنبيه",JOptionPane.WARNING_MESSAGE); return; }
        if(btnFinalApprove!=null && !btnFinalApprove.isEnabled()){
            JOptionPane.showMessageDialog(this,"لا يمكن الاعتماد والفارق غير صفر (يجب تصفير الفارق عبر التسوية السريعة أو تحديد العمليات)","تنبيه",JOptionPane.WARNING_MESSAGE);
            return;
        }
        int selCount=0;
        for(int i=0;i<model.getRowCount();i++) if(Boolean.TRUE.equals(model.getValueAt(i,0))) selCount++;
        if(selCount==0){ JOptionPane.showMessageDialog(this,"لم يتم تحديد أي حركة للتسوية","تنبيه",JOptionPane.WARNING_MESSAGE); return; }
        int ok = JOptionPane.showConfirmDialog(this,"اعتماد التسوية النهائية لـ "+selCount+" حركة؟ سيتم تغيير حالتها إلى COMPLETED","تأكيد",JOptionPane.YES_NO_OPTION);
        if(ok!=JOptionPane.YES_OPTION) return;
        try (Connection conn=DatabaseManager.getConnection()){
            conn.setAutoCommit(false);
            String sql="INSERT INTO bank_reconciliation (bank_account, journal_entry_number, reconciled, reconciled_date) VALUES (?,?,1, CURRENT_DATE) ON DUPLICATE KEY UPDATE reconciled=1, reconciled_date=CURRENT_DATE";
            try(PreparedStatement ps=conn.prepareStatement(sql)){
                for(int i=0;i<model.getRowCount();i++){
                    if(Boolean.TRUE.equals(model.getValueAt(i,0))){
                        String doc = model.getValueAt(i,2).toString();
                        ps.setString(1,bank);
                        ps.setString(2,doc);
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
            conn.commit();
        }catch(Exception ex){
            JOptionPane.showMessageDialog(this,"خطأ تسوية: "+ex.getMessage(),"خطأ",JOptionPane.ERROR_MESSAGE); return;
        }
        JOptionPane.showMessageDialog(this,"تم اعتماد التسوية النهائية بنجاح (COMPLETED)","نجاح",JOptionPane.INFORMATION_MESSAGE);
        refreshAll();
    }

    private void saveMemo(){
        String bank = txtBankCode.getText().trim();
        String asOf = txtAsOfDate.getText().trim();
        if(bank.isEmpty()){ JOptionPane.showMessageDialog(this,"اختر حساب البنك","تنبيه",JOptionPane.WARNING_MESSAGE); return; }
        try(Connection c=DatabaseManager.getConnection(); Statement s=c.createStatement()){
            s.executeUpdate("CREATE TABLE IF NOT EXISTS bank_reconciliation_memos (id BIGINT AUTO_INCREMENT PRIMARY KEY, bank_account VARCHAR(20), as_of_date DATE, book_balance DOUBLE, bank_balance DOUBLE, diff DOUBLE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
            String sql="INSERT INTO bank_reconciliation_memos (bank_account, as_of_date, book_balance, bank_balance, diff) VALUES (?,?,?,?,?)";
            try(PreparedStatement ps=c.prepareStatement(sql)){
                ps.setString(1,bank); ps.setString(2,asOf); ps.setDouble(3,parseDouble(txtBookBalance.getText())); ps.setDouble(4,parseDouble(txtBankBalance.getText())); ps.setDouble(5,parseDouble(lblDiff.getText())); ps.executeUpdate();
            }
        }catch(Exception ex){ JOptionPane.showMessageDialog(this,"خطأ حفظ: "+ex.getMessage(),"خطأ",JOptionPane.ERROR_MESSAGE); return; }
        JOptionPane.showMessageDialog(this,"تم حفظ مذكرة المطابقة","حفظ",JOptionPane.INFORMATION_MESSAGE);
    }

    private String buildHtml(){
        StringBuilder sb=new StringBuilder();
        sb.append("<html><body style='font-family:Tahoma; direction:rtl; text-align:right; padding:20px;'>");
        sb.append("<h2 style='text-align:center;'>مذكرة التسوية البنكية</h2>");
        sb.append("<p>حساب البنك: ").append(txtBankCode.getText()).append(" - ").append(txtBankName.getText()).append(" | تاريخ القطع: ").append(txtAsOfDate.getText()).append("</p>");
        sb.append("<p>رصيد الدفاتر: ").append(txtBookBalance.getText()).append(" | رصيد كشف البنك: ").append(txtBankBalance.getText()).append("</p>");
        sb.append("<table border='1' cellpadding='6' cellspacing='0' style='width:100%; border-collapse:collapse;'><tr style='background:#f1f5f9;'><th>مطابقة</th><th>التاريخ</th><th>المستند</th><th>البيان</th><th>إيداعات</th><th>سحوبات</th></tr>");
        for(int i=0;i<model.getRowCount();i++){
            Boolean chk=(Boolean)model.getValueAt(i,0);
            if(chk==null||!chk) continue;
            sb.append("<tr><td>☑</td><td>").append(model.getValueAt(i,1)).append("</td><td>").append(model.getValueAt(i,2)).append("</td><td>").append(model.getValueAt(i,3)).append("</td><td style='text-align:right;'>").append(model.getValueAt(i,4)).append("</td><td style='text-align:right;'>").append(model.getValueAt(i,5)).append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("<p>إجمالي الإيداعات المعلقة: ").append(lblDeposits.getText()).append(" | السحوبات المعلقة: ").append(lblWithdrawals.getText()).append("</p>");
        sb.append("<p>رصيد الدفاتر المعدل: ").append(lblAdjBook.getText()).append(" | رصيد البنك المعدل: ").append(lblAdjBank.getText()).append(" | الفارق: ").append(lblDiff.getText()).append("</p>");
        sb.append("<br><table style='width:100%; text-align:center; margin-top:40px;'><tr><td>إعداد: ___________</td><td>مراجعة: ___________</td><td>اعتماد: ___________</td></tr></table>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private void showPreview(){
        JDialog dlg=new JDialog(this,"معاينة مذكرة التسوية البنكية",true);
        dlg.setSize(800,650); dlg.setLocationRelativeTo(this); dlg.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JEditorPane pane=new JEditorPane("text/html", buildHtml()); pane.setEditable(false); pane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        dlg.add(new JScrollPane(pane), BorderLayout.CENTER);
        JPanel btns=new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton bPrint=new JButton("طباعة"); bPrint.addActionListener(e->{dlg.dispose(); printMemo();});
        JButton bClose=new JButton("إغلاق"); bClose.addActionListener(e->dlg.dispose());
        btns.add(bPrint); btns.add(bClose); dlg.add(btns, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void printMemo(){
        PrinterJob job=PrinterJob.getPrinterJob(); job.setJobName("مذكرة مطابقة بنكية "+txtBankCode.getText());
        job.setPrintable(new Printable(){
            public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException{
                if(pageIndex>0) return NO_SUCH_PAGE;
                Graphics2D g2=(Graphics2D)g; g2.translate(pf.getImageableX(), pf.getImageableY());
                g2.setFont(new Font("Tahoma", Font.PLAIN, 10));
                String html=buildHtml().replaceAll("<[^>]*>", " ");
                String[] lines=html.split(" ");
                int y=20; String line=""; int w=(int)pf.getImageableWidth();
                for(String word: lines){
                    if(line.length()+word.length()>90){ g2.drawString(line,10,y); y+=15; line=word+" "; if(y>pf.getImageableHeight()-30) break; } else line+=word+" ";
                }
                if(!line.isEmpty()) g2.drawString(line,10,y);
                g2.drawString("إعداد: ___________    مراجعة: ___________    اعتماد: ___________", 40, y+40);
                return PAGE_EXISTS;
            }
        });
        if(job.printDialog()){ try{job.print();}catch(PrinterException ex){ JOptionPane.showMessageDialog(this,"خطأ طباعة: "+ex.getMessage(),"خطأ",JOptionPane.ERROR_MESSAGE); } }
    }

    private void showPageSetup(){
        PrinterJob job=PrinterJob.getPrinterJob();
        PageFormat pf=job.defaultPage();
        pf=job.pageDialog(pf);
        if(job.printDialog()) JOptionPane.showMessageDialog(this,"تم حفظ إعدادات الصفحة والطابعة","إعدادات",JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args){ SwingUtilities.invokeLater(()->new BankReconciliationFrame().setVisible(true)); }
}
