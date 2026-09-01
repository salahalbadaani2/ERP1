import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Vector;

public class PurchaseInvoiceForm extends JFrame {
    private final JTextField invoiceNumber = new JTextField();
    private final JTextField invoiceDate = new JTextField(LocalDate.now().toString());
    private JTextField supplierAccount = new JTextField();
    private JCheckBox chkApplyTax = new JCheckBox("تطبيق الضريبة");
    private JTextField txtTaxRate = new JTextField("0.15");
    private DefaultTableModel tableModel;
    private JTable itemTable;
    private final JTextField totalField = new JTextField("0.00");
    private final JTextField taxAmountField = new JTextField("0.00");
    private final JTextField grandTotalField = new JTextField("0.00");

    private static final String[] COLUMNS = {"م", "نوع المخزون", "اسم الصنف", "رقم الصنف", "نوع الوحدة", "الكمية", "الجرام", "سعر الوحدة", "الإجمالي"};

    public PurchaseInvoiceForm() {
        setTitle("نظام ERP المصنعي - فاتورة المشتريات");
        setSize(1300, 800);
        setMinimumSize(new Dimension(1100, 650));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        invoiceNumber.setText(DocumentNumberService.next("PURCHASE_INVOICE", "PUR-"));
        invoiceNumber.setEditable(false);
        initUI();
        installAutoComplete();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JPanel headerPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        headerPanel.setBorder(BorderFactory.createTitledBorder("بيانات فاتورة المشتريات"));
        headerPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        headerPanel.add(new JLabel("رقم الفاتورة:"));
        headerPanel.add(invoiceNumber);
        headerPanel.add(new JLabel("التاريخ:"));
        headerPanel.add(invoiceDate);
        headerPanel.add(new JLabel("حساب المورد:"));
        headerPanel.add(accountField(supplierAccount, "21"));

        JPanel taxPanel = new JPanel(new GridLayout(1, 4, 10, 5));
        taxPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        chkApplyTax.setSelected(false);
        txtTaxRate.setEnabled(false);
        chkApplyTax.addActionListener(e -> txtTaxRate.setEnabled(chkApplyTax.isSelected()));
        taxPanel.add(chkApplyTax);
        taxPanel.add(new JLabel("النسبة:"));
        taxPanel.add(txtTaxRate);
        taxPanel.add(new JLabel("مبلغ الضريبة:"));
        taxPanel.add(taxAmountField);
        headerPanel.add(taxPanel);

        JPanel tablePanel = new JPanel(new BorderLayout(10, 10));
        tablePanel.setBorder(BorderFactory.createTitledBorder("جدول الأصناف"));
        tablePanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public Class<?> getColumnClass(int column) { return String.class; }
            @Override public boolean isCellEditable(int row, int column) { return column >= 1 && column <= 8; }
        };
        itemTable = new JTable(tableModel) {
            @Override public TableCellEditor getCellEditor(int row, int column) {
                if (column == 4) return new DefaultCellEditor(new JComboBox<>(new String[]{"COUNT", "WEIGHT"}));
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
                        AccountTreeDialog dlg=new AccountTreeDialog(PurchaseInvoiceForm.this, "121");
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
        JScrollPane scrollPane = new JScrollPane(itemTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        JPanel tableButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        tableButtons.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton addRowBtn = new JButton("إضافة سطر");
        JButton removeRowBtn = new JButton("حذف السطر المحدد");
        addRowBtn.addActionListener(e -> { addTableRow(); renumberRows(); });
        removeRowBtn.addActionListener(e -> removeSelectedRow());
        tableButtons.add(addRowBtn); tableButtons.add(removeRowBtn);
        tablePanel.add(tableButtons, BorderLayout.SOUTH);

        JPanel bottomPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("المبالغ الإجمالية"));
        bottomPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        bottomPanel.add(new JLabel("الإجمالي الكلي:"));
        totalField.setEditable(false); totalField.setFont(new Font("Tahoma", Font.BOLD, 14)); totalField.setHorizontalAlignment(SwingConstants.RIGHT); bottomPanel.add(totalField);
        bottomPanel.add(new JLabel("الإجمالي مع الضريبة:"));
        grandTotalField.setEditable(false); grandTotalField.setFont(new Font("Tahoma", Font.BOLD, 14)); grandTotalField.setHorizontalAlignment(SwingConstants.RIGHT); bottomPanel.add(grandTotalField);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(tablePanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8));
        JButton approve = new JButton("موافق واعتماد");
        JButton view = new JButton("عرض وطباعة");
        JButton clear = new JButton("فاتورة جديدة");
        JButton close = new JButton("إغلاق");
        approve.addActionListener(e -> post()); view.addActionListener(e -> preview()); clear.addActionListener(e -> reset()); close.addActionListener(e -> dispose());
        actions.add(approve); actions.add(view); actions.add(clear); actions.add(close);
        add(mainPanel, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    private void installAutoComplete() {
        AutoCompleteHelper.installAccountAutoComplete(supplierAccount, "LIABILITY");
    }

    private void addTableRow() {
        int row = tableModel.getRowCount();
        Vector<String> rowData = new Vector<>();
        rowData.add(String.valueOf(row+1)); rowData.add(""); rowData.add(""); rowData.add(""); rowData.add("COUNT"); rowData.add(""); rowData.add(""); rowData.add(""); rowData.add("0.00");
        tableModel.addRow(rowData);
    }
    private void removeSelectedRow() { int r=itemTable.getSelectedRow(); if(r>=0){ tableModel.removeRow(r); renumberRows(); calculateRowTotals(); } }
    private void renumberRows() { for(int i=0;i<tableModel.getRowCount();i++) tableModel.setValueAt(String.valueOf(i+1), i, 0); }
    private void calculateRowTotals() {
        double total=0.0;
        for(int i=0;i<tableModel.getRowCount();i++){
            try{
                double qty=parseDoubleSafe(tableModel.getValueAt(i,5));
                double gram=parseDoubleSafe(tableModel.getValueAt(i,6));
                double price=parseDoubleSafe(tableModel.getValueAt(i,7));
                String unitType=(String)tableModel.getValueAt(i,4);
                if("WEIGHT".equals(unitType)) qty=qty+(gram/1000.0);
                double totalVal=qty*price;
                tableModel.setValueAt(String.format("%,.2f",totalVal),i,8);
                total+=totalVal;
            }catch(Exception ignored){}
        }
        totalField.setText(String.format("%,.2f",total));
        double taxRate=parseDoubleSafe(txtTaxRate.getText());
        double taxAmount=chkApplyTax.isSelected()?total*taxRate:0.0;
        taxAmountField.setText(String.format("%,.2f",taxAmount));
        grandTotalField.setText(String.format("%,.2f",total+taxAmount));
    }
    private double parseDoubleSafe(Object v){ if(v==null) return 0.0; try{return Double.parseDouble(v.toString().trim().replace(",",""));}catch(Exception e){return 0.0;}}
    private JPanel accountField(JTextField field, String prefix){
        JPanel wrapper=new JPanel(new BorderLayout(5,0));
        wrapper.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton browse=new JButton("شجرة الحسابات");
        browse.addActionListener(e->{
            AccountTreeDialog dialog=new AccountTreeDialog(this, prefix);
            dialog.setVisible(true);
            if(dialog.isAccountSelected()){ field.setText(dialog.getSelectedAccountCode()); autoFillItemCode(field); }
        });
        wrapper.add(field,BorderLayout.CENTER); wrapper.add(browse,BorderLayout.EAST);
        return wrapper;
    }
    private void autoFillItemCode(JTextField accountField){
        String code=accountField.getText().trim().split(" - ")[0].trim();
        if(code.isEmpty()) return;
        String sql="SELECT item_code, item_name, unit_type FROM inventory_items WHERE item_code=? OR inventory_account=? LIMIT 1";
        try(Connection conn=DatabaseManager.getConnection(); PreparedStatement ps=conn.prepareStatement(sql)){
            ps.setString(1,code); ps.setString(2,code);
            try(ResultSet rs=ps.executeQuery()){ if(rs.next()){
                if(tableModel.getRowCount()==0) addTableRow();
                tableModel.setValueAt(rs.getString("item_code"),0,3);
                tableModel.setValueAt(rs.getString("item_name"),0,2);
                tableModel.setValueAt(rs.getString("unit_type"),0,4);
                renumberRows(); calculateRowTotals();
            }}
        }catch(SQLException ignored){}
    }
    private void post(){
        try{
            if(tableModel.getRowCount()==0) throw new IllegalArgumentException("يجب إضافة صنف واحد على الأقل في الجدول.");
            double total=parseDoubleSafe(totalField.getText());
            if(total <= 0) throw new IllegalArgumentException("إجمالي الفاتورة يجب أن يكون أكبر من الصفر.");
            boolean taxApplied=chkApplyTax.isSelected();
            double taxRate=parseDoubleSafe(txtTaxRate.getText());
            String supplierAcc=supplierAccount.getText().trim().split(" - ")[0].trim();
            AccountResolver.requireAccount("المورد", supplierAcc);
            java.util.List<PurchaseInvoice.PurchaseLine> lines=new java.util.ArrayList<>();
            for(int i=0;i<tableModel.getRowCount();i++){
                String ic=tableModel.getValueAt(i,3).toString().trim().split(" - ")[0].trim();
                if(ic==null || ic.isEmpty()) throw new IllegalArgumentException("رقم الصنف مطلوب في السطر "+(i+1));
                String ut=(String)tableModel.getValueAt(i,4);
                double q=parseDoubleSafe(tableModel.getValueAt(i,5));
                double g=parseDoubleSafe(tableModel.getValueAt(i,6));
                if("WEIGHT".equals(ut)) q=q+(g/1000.0);
                double uc=parseDoubleSafe(tableModel.getValueAt(i,7));
                if(q<=0 || uc<=0) throw new IllegalArgumentException("الكمية وسعر الوحدة يجب أن يكونا موجبين في السطر "+(i+1));
                lines.add(new PurchaseInvoice.PurchaseLine(ic, q, uc));
            }
            String firstItemCode=lines.get(0).getItemCode();
            String grirAcc, taxAcc;
            try(Connection conn=DatabaseManager.getConnection()){
                grirAcc=AccountResolver.resolveRawMaterialAccount(conn, firstItemCode);
                AccountResolver.requireAccount("مخزون المواد الخام", grirAcc);
                taxAcc = taxApplied ? AccountResolver.resolveInputTaxAccount(conn) : null;
                if(taxApplied) AccountResolver.requireAccount("الضريبة", taxAcc);
            }
            PurchaseInvoice invoice=new PurchaseInvoice(invoiceNumber.getText(), grirAcc, supplierAcc, taxAcc, taxApplied, taxRate, lines);
            if(!invoice.postToAccounting()) throw new IllegalStateException("تعذر اعتماد فاتورة المشتريات.");
            JOptionPane.showMessageDialog(this, "تم اعتماد الفاتورة وترحيل قيد المخزون والمورد وتحديث رصيده.", "نجاح", JOptionPane.INFORMATION_MESSAGE);
            reset();
        }catch(Exception ex){ JOptionPane.showMessageDialog(this, ex.getMessage(), "خطأ في فاتورة المشتريات", JOptionPane.ERROR_MESSAGE); }
    }
    private void preview(){
        StringBuilder rowsHtml=new StringBuilder();
        for(int i=0;i<tableModel.getRowCount();i++){
            rowsHtml.append("<tr><td>").append(i+1).append("</td><td>").append(tableModel.getValueAt(i,1)).append("</td><td>").append(tableModel.getValueAt(i,2)).append("</td><td>").append(tableModel.getValueAt(i,3)).append("</td><td>").append(tableModel.getValueAt(i,4)).append("</td><td>").append(tableModel.getValueAt(i,5)).append("</td><td>").append(tableModel.getValueAt(i,6)).append("</td><td>").append(tableModel.getValueAt(i,7)).append("</td><td>").append(tableModel.getValueAt(i,8)).append("</td></tr>");
        }
        String html="<html dir='rtl' lang='ar'><head><meta charset='UTF-8'>"+"<style>body{font-family:Tahoma,sans-serif;font-size:12px;} table{border-collapse:collapse;width:100%;}"+"th,td{border:1px solid #999;padding:6px;text-align:right;} th{background-color:#1f2937;color:white;}"+"tr:nth-child(even){background-color:#f3f4f6;} .total-row{font-weight:bold;background-color:#d1fae5;}</style></head>"+"<body><h2>فاتورة مشتريات</h2>"+"<p>رقم الفاتورة: "+invoiceNumber.getText()+"</p>"+"<p>التاريخ: "+invoiceDate.getText()+"</p>"+"<p>حساب المورد: "+supplierAccount.getText()+"</p>"+"<p>الضريبة المطبقة: "+(chkApplyTax.isSelected()?"نعم ("+txtTaxRate.getText()+")":"لا")+"</p>"+"<table><thead><tr><th>م</th><th>نوع المخزون</th><th>اسم الصنف</th><th>رقم الصنف</th><th>نوع الوحدة</th><th>الكمية</th><th>الجرام</th><th>سعر الوحدة</th><th>الإجمالي</th></tr></thead>"+"<tbody>"+rowsHtml.toString()+"</tbody>"+"<tfoot><tr class='total-row'><td colspan='8'>الإجمالي الكلي</td><td>"+totalField.getText()+"</td></tr>"+"<tr class='total-row'><td colspan='8'>الإجمالي مع الضريبة</td><td>"+grandTotalField.getText()+"</td></tr></tfoot>"+"</table><p>التوقيع: ____________________</p></body></html>";
        new DocumentPreviewDialog(this, "فاتورة المشتريات", html).setVisible(true);
    }
    private void reset(){
        invoiceNumber.setText(DocumentNumberService.next("PURCHASE_INVOICE", "PUR-"));
        invoiceDate.setText(LocalDate.now().toString());
        supplierAccount.setText("");
        chkApplyTax.setSelected(false); txtTaxRate.setText("0.15"); txtTaxRate.setEnabled(false);
        taxAmountField.setText("0.00"); grandTotalField.setText("0.00");
        while(tableModel.getRowCount()>0) tableModel.removeRow(0);
        totalField.setText("0.00");
    }
}
