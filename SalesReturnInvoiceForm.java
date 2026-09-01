import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Vector;

public class SalesReturnInvoiceForm extends JFrame {
    private final JTextField txtReturnNumber = new JTextField();
    private final JTextField txtReturnDate = new JTextField(LocalDate.now().toString());
    private final JTextField txtCustomerAccount = new JTextField();
    private JCheckBox chkApplyTax = new JCheckBox("تطبيق الضريبة");
    private JTextField txtTaxRate = new JTextField("0.15");
    private DefaultTableModel tableModel;
    private JTable itemTable;
    private final JTextField totalField = new JTextField("0.00");
    private final JTextField taxAmountField = new JTextField("0.00");
    private final JTextField grandTotalField = new JTextField("0.00");

    private static final String[] COLUMNS = {"م", "نوع المخزون", "اسم الصنف", "رقم الصنف", "نوع الوحدة", "الكمية", "الجرام", "سعر الوحدة", "الإجمالي"};

    public SalesReturnInvoiceForm() {
        setTitle("نظام ERP المصنعي - فاتورة مردودات المبيعات");
        setSize(1300, 800);
        setMinimumSize(new Dimension(1100, 650));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        initUI();
        generateReturnNumber();
        installAutoComplete();
    }

    private void initUI() {
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(new Color(30, 41, 59));
        headerBar.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        JLabel title = new JLabel("إصدار وترحيل فاتورة مردودات المبيعات (Sales Return Invoice)");
        title.setFont(new Font("Tahoma", Font.BOLD, 14));
        title.setForeground(Color.WHITE);
        headerBar.add(title, BorderLayout.NORTH);
        add(headerBar, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JPanel headerPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        headerPanel.setBorder(BorderFactory.createTitledBorder("بيانات المردود"));
        headerPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        headerPanel.add(new JLabel("رقم فاتورة المردودات:"));
        txtReturnNumber.setEditable(false);
        headerPanel.add(txtReturnNumber);
        headerPanel.add(new JLabel("تاريخ المرتجع:"));
        headerPanel.add(txtReturnDate);
        headerPanel.add(new JLabel("حساب العميل:"));
        headerPanel.add(accountField(txtCustomerAccount, "123"));

        JPanel taxPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        taxPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        chkApplyTax.setSelected(false);
        txtTaxRate.setEnabled(false);
        chkApplyTax.addActionListener(e -> txtTaxRate.setEnabled(chkApplyTax.isSelected()));
        taxPanel.add(chkApplyTax);
        taxPanel.add(new JLabel("النسبة:"));
        taxPanel.add(txtTaxRate);
        headerPanel.add(new JLabel("احتساب الضريبة:"));
        headerPanel.add(taxPanel);

        JPanel tablePanel = new JPanel(new BorderLayout(10, 10));
        tablePanel.setBorder(BorderFactory.createTitledBorder("جدول الأصناف"));
        tablePanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public Class<?> getColumnClass(int column) { return String.class; }
            @Override public boolean isCellEditable(int row, int column) { return column>=1 && column<=8; }
        };
        itemTable = new JTable(tableModel) {
            @Override public TableCellEditor getCellEditor(int row, int column) {
                if (column==4) return new DefaultCellEditor(new JComboBox<>(new String[]{"COUNT","WEIGHT"}));
                return super.getCellEditor(row, column);
            }
        };
        itemTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        itemTable.setFont(new Font("Tahoma", Font.PLAIN, 12));
        itemTable.setRowHeight(25);
        itemTable.getModel().addTableModelListener(e -> calculateRowTotals());
        itemTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount()==2) {
                    int row=itemTable.getSelectedRow(); int col=itemTable.getSelectedColumn();
                    if (col==2 || col==3) {
                        AccountTreeDialog dlg=new AccountTreeDialog(SalesReturnInvoiceForm.this, "121");
                        dlg.setVisible(true);
                        if (dlg.isAccountSelected()) {
                            String code=dlg.getSelectedAccountCode();
                            String sql="SELECT item_code, item_name, unit_type FROM inventory_items WHERE inventory_account=? LIMIT 1";
                            try (Connection conn=DatabaseManager.getConnection(); PreparedStatement ps=conn.prepareStatement(sql)){
                                ps.setString(1,code);
                                try(ResultSet rs=ps.executeQuery()){ if(rs.next()){
                                    tableModel.setValueAt(rs.getString("item_name"),row,2);
                                    tableModel.setValueAt(rs.getString("item_code"),row,3);
                                    tableModel.setValueAt(rs.getString("unit_type"),row,4);
                                } else tableModel.setValueAt(code,row,3);}
                            } catch(Exception ignored){}
                        }
                    }
                }
            }
        });
        JScrollPane sp=new JScrollPane(itemTable);
        tablePanel.add(sp, BorderLayout.CENTER);
        JPanel tableButtons=new JPanel(new FlowLayout(FlowLayout.LEFT,10,5));
        tableButtons.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton addRowBtn=new JButton("إضافة سطر");
        JButton removeRowBtn=new JButton("حذف السطر المحدد");
        addRowBtn.addActionListener(e->{addTableRow(); renumberRows();});
        removeRowBtn.addActionListener(e->removeSelectedRow());
        tableButtons.add(addRowBtn); tableButtons.add(removeRowBtn);
        tablePanel.add(tableButtons, BorderLayout.SOUTH);

        JPanel bottomPanel=new JPanel(new GridLayout(0,3,10,10));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("المبالغ الإجمالية"));
        bottomPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        bottomPanel.add(new JLabel("الإجمالي الكلي:")); totalField.setEditable(false); totalField.setFont(new Font("Tahoma",Font.BOLD,14)); totalField.setHorizontalAlignment(SwingConstants.RIGHT); bottomPanel.add(totalField);
        bottomPanel.add(new JLabel("الإجمالي مع الضريبة:")); grandTotalField.setEditable(false); grandTotalField.setFont(new Font("Tahoma",Font.BOLD,14)); grandTotalField.setHorizontalAlignment(SwingConstants.RIGHT); bottomPanel.add(grandTotalField);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(tablePanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,15,8));
        JButton approve=new JButton("حفظ وترحيل");
        approve.setBackground(new Color(16,185,129)); approve.setForeground(Color.WHITE); approve.setFocusPainted(false);
        JButton view=new JButton("معاينة وطباعة");
        JButton clear=new JButton("مسح");
        JButton close=new JButton("إغلاق");
        approve.addActionListener(e->handlePosting());
        view.addActionListener(e->preview());
        clear.addActionListener(e->reset());
        close.addActionListener(e->dispose());
        actions.add(approve); actions.add(view); actions.add(clear); actions.add(close);

        add(mainPanel, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    private void installAutoComplete(){ AutoCompleteHelper.installAccountAutoComplete(txtCustomerAccount, "ASSET"); }
    private void generateReturnNumber(){ txtReturnNumber.setText(DocumentNumberService.next("SALES_RETURN","SRI-")); }
    private void addTableRow(){ int row=tableModel.getRowCount(); Vector<String> r=new Vector<>(); r.add(String.valueOf(row+1)); r.add(""); r.add(""); r.add(""); r.add("COUNT"); r.add(""); r.add(""); r.add(""); r.add("0.00"); tableModel.addRow(r); }
    private void removeSelectedRow(){ int r=itemTable.getSelectedRow(); if(r>=0){ tableModel.removeRow(r); renumberRows(); calculateRowTotals(); } }
    private void renumberRows(){ for(int i=0;i<tableModel.getRowCount();i++) tableModel.setValueAt(String.valueOf(i+1),i,0); }
    private void calculateRowTotals(){
        double total=0.0;
        for(int i=0;i<tableModel.getRowCount();i++){
            try{ double qty=parseDoubleSafe(tableModel.getValueAt(i,5)); double gram=parseDoubleSafe(tableModel.getValueAt(i,6)); double price=parseDoubleSafe(tableModel.getValueAt(i,7)); String ut=(String)tableModel.getValueAt(i,4); if("WEIGHT".equals(ut)) qty=qty+(gram/1000.0); double v=qty*price; tableModel.setValueAt(String.format("%,.2f",v),i,8); total+=v; }catch(Exception ignored){}
        }
        totalField.setText(String.format("%,.2f",total));
        double rate=parseDoubleSafe(txtTaxRate.getText()); double tax=chkApplyTax.isSelected()?total*rate:0.0;
        taxAmountField.setText(String.format("%,.2f",tax)); grandTotalField.setText(String.format("%,.2f",total+tax));
    }
    private double parseDoubleSafe(Object v){ if(v==null) return 0.0; try{return Double.parseDouble(v.toString().trim().replace(",",""));}catch(Exception e){return 0.0;} }
    private JPanel accountField(JTextField field, String prefix){
        JPanel w=new JPanel(new BorderLayout(5,0)); w.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton browse=new JButton("دليل الحسابات");
        browse.addActionListener(e->{ AccountTreeDialog d=new AccountTreeDialog(this,prefix); d.setVisible(true); if(d.isAccountSelected()){ field.setText(d.getSelectedAccountCode()); if(tableModel.getRowCount()==0) addTableRow(); }});
        w.add(field,BorderLayout.CENTER); w.add(browse,BorderLayout.EAST); return w;
    }
    private void handlePosting(){
        try{
            if(tableModel.getRowCount()==0) throw new IllegalArgumentException("يجب إضافة صنف واحد على الأقل في الجدول.");
            double total=parseDoubleSafe(totalField.getText());
            if(total <= 0) throw new IllegalArgumentException("قيمة المرتجع يجب أن تكون أكبر من الصفر");
            String custAcc=txtCustomerAccount.getText().trim().split(" - ")[0].trim();
            AccountResolver.requireAccount("العميل", custAcc);
            boolean taxApplied=chkApplyTax.isSelected();
            double taxRate=parseDoubleSafe(txtTaxRate.getText());
            java.util.List<SalesReturnInvoice.ReturnLine> lines=new java.util.ArrayList<>();
            try(Connection conn=DatabaseManager.getConnection()){
                conn.setAutoCommit(false);
                double inventoryCostTotal=0;
                for(int i=0;i<tableModel.getRowCount();i++){
                    String ic=tableModel.getValueAt(i,3).toString().trim().split(" - ")[0].trim();
                    if(ic==null || ic.isEmpty()) throw new IllegalArgumentException("رقم الصنف مطلوب في السطر "+(i+1));
                    String ut=(String)tableModel.getValueAt(i,4);
                    double qty=parseDoubleSafe(tableModel.getValueAt(i,5));
                    double gram=parseDoubleSafe(tableModel.getValueAt(i,6));
                    if("WEIGHT".equals(ut)) qty=qty+(gram/1000.0);
                    double price=parseDoubleSafe(tableModel.getValueAt(i,7));
                    if(qty <= 0 || price <= 0) throw new IllegalArgumentException("الكمية وسعر الوحدة يجب أن يكونا موجبين في السطر "+(i+1));
                    String sql="SELECT unit_cost FROM inventory_items WHERE item_code=? LIMIT 1";
                    double unitCost;
                    try(PreparedStatement ps=conn.prepareStatement(sql)){
                        ps.setString(1,ic);
                        try(ResultSet rs=ps.executeQuery()){ if(rs.next()) unitCost=rs.getDouble(1); else unitCost=price; }
                    }
                    inventoryCostTotal+=qty*unitCost;
                    String itemName=tableModel.getValueAt(i,2).toString();
                    lines.add(new SalesReturnInvoice.ReturnLine(ic, qty, price, unitCost));
                }
                String firstItemCode=lines.get(0).getItemCode();
                String revReturnAcc=AccountResolver.resolveSalesReturnAccount(conn); AccountResolver.requireAccount("مردودات المبيعات", revReturnAcc);
                String fgAcc=AccountResolver.resolveInventoryAccount(conn, firstItemCode); AccountResolver.requireAccount("مخزون الإنتاج التام", fgAcc);
                String cogsAcc=AccountResolver.resolveCogsAccount(conn, firstItemCode); AccountResolver.requireAccount("تكلفة المبيعات", cogsAcc);
                String taxAcc = taxApplied ? AccountResolver.resolveOutputTaxAccount(conn) : null;
                if(taxApplied) AccountResolver.requireAccount("الضريبة", taxAcc);
                SalesReturnInvoice invoice=new SalesReturnInvoice(txtReturnNumber.getText().trim(), "-", txtReturnDate.getText().trim(), custAcc, revReturnAcc, taxAcc, fgAcc, cogsAcc, taxApplied, taxRate, "مردود مبيعات", "-", lines);
                boolean success=SalesPostingService.postSalesReturn(conn, invoice);
                if(!success) throw new IllegalStateException("فشل ترحيل مردودات المبيعات");
                JOptionPane.showMessageDialog(this, "تم اعتماد وترحيل فاتورة مردودات المبيعات بنجاح.\nرقم السند: ["+invoice.getInvoiceCode()+"]\nإجمالي المستحق للعميل: "+invoice.getTotalCustomerCredit()+" YER", "نجاح", JOptionPane.INFORMATION_MESSAGE);
                generateReturnNumber(); reset();
            }
        }catch(Exception ex){ JOptionPane.showMessageDialog(this, ex.getMessage(), "خطأ في مردود المبيعات", JOptionPane.ERROR_MESSAGE); }
    }
    private void preview(){
        StringBuilder rows=new StringBuilder();
        for(int i=0;i<tableModel.getRowCount();i++) rows.append("<tr><td>").append(i+1).append("</td><td>").append(tableModel.getValueAt(i,1)).append("</td><td>").append(tableModel.getValueAt(i,2)).append("</td><td>").append(tableModel.getValueAt(i,3)).append("</td><td>").append(tableModel.getValueAt(i,4)).append("</td><td>").append(tableModel.getValueAt(i,5)).append("</td><td>").append(tableModel.getValueAt(i,6)).append("</td><td>").append(tableModel.getValueAt(i,7)).append("</td><td>").append(tableModel.getValueAt(i,8)).append("</td></tr>");
        String html="<html dir='rtl'><head><meta charset='UTF-8'><style>table{border-collapse:collapse;width:100%}th,td{border:1px solid #999;padding:6px}th{background:#1f2937;color:white}</style></head><body><h2>فاتورة مردودات مبيعات</h2><p>رقم المردود: "+txtReturnNumber.getText()+"</p><p>العميل: "+txtCustomerAccount.getText()+"</p><table><thead><tr><th>م</th><th>نوع المخزون</th><th>اسم الصنف</th><th>رقم الصنف</th><th>نوع الوحدة</th><th>الكمية</th><th>الجرام</th><th>سعر الوحدة</th><th>الإجمالي</th></tr></thead><tbody>"+rows.toString()+"</tbody><tfoot><tr><td colspan='8'>الإجمالي</td><td>"+totalField.getText()+"</td></tr></tfoot></table></body></html>";
        new DocumentPreviewDialog(this, "مردود المبيعات", html).setVisible(true);
    }
    private void reset(){ txtCustomerAccount.setText(""); chkApplyTax.setSelected(false); txtTaxRate.setText("0.15"); txtTaxRate.setEnabled(false); while(tableModel.getRowCount()>0) tableModel.removeRow(0); totalField.setText("0.00"); grandTotalField.setText("0.00"); taxAmountField.setText("0.00"); calculateRowTotals(); }
    public static void main(String[] args){ SwingUtilities.invokeLater(()->{ try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ignored){} new SalesReturnInvoiceForm().setVisible(true);}); }
}
