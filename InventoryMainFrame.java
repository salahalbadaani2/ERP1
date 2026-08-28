import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.time.LocalDate;

/**
 * نظام ERP المصنعي - المخازن (Hub مثل نظام المشتريات)
 * نافذة وسيطة بأزرار كبيرة تفتح كل مستند في شاشة مستقلة تماماً
 */
public class InventoryMainFrame extends JFrame {

    public InventoryMainFrame() {
        setTitle("نظام ERP المصنعي - المخازن");
        setSize(520, 320);
        setMinimumSize(new Dimension(420, 280));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel panel = new JPanel(new GridLayout(4, 1, 12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JButton btnReceipt = new JButton("الاستلام المخزني");
        JButton btnIssue = new JButton("الصرف المخزني");
        JButton btnReports = new JButton("تقارير المخزون");
        JButton btnClearance = new JButton("إخلاء عهد المندوبين");

        Font btnFont = new Font("Segoe UI", Font.PLAIN, 14);
        btnReceipt.setFont(btnFont);
        btnIssue.setFont(btnFont);
        btnReports.setFont(btnFont);
        btnClearance.setFont(btnFont);

        btnReceipt.addActionListener(e -> openReceiptWindow());
        btnIssue.addActionListener(e -> openIssueWindow());
        btnReports.addActionListener(e -> openReportsWindow());
        btnClearance.addActionListener(e -> openClearanceWindow());

        panel.add(btnReceipt);
        panel.add(btnIssue);
        panel.add(btnReports);
        panel.add(btnClearance);

        add(panel);
    }

    private void openReceiptWindow() {
        JFrame frame = new JFrame("المخازن - الاستلام المخزني");
        frame.setSize(1050, 620);
        frame.setMinimumSize(new Dimension(900, 550));
        frame.setLocationRelativeTo(this);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JCheckBox cbShowFinancial = new JCheckBox("إظهار الأعمدة المالية (واجهة المحاسب) / إخفاء (واجهة أمين المخزن)", true);
        cbShowFinancial.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbShowFinancial.setFont(new Font("Tahoma", Font.BOLD, 12));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        top.add(cbShowFinancial);

        JPanel content = createReceiptPanel(cbShowFinancial, frame);
        frame.add(top, BorderLayout.NORTH);
        frame.add(content, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void openIssueWindow() {
        JFrame frame = new JFrame("المخازن - الصرف المخزني");
        frame.setSize(1050, 620);
        frame.setMinimumSize(new Dimension(900, 550));
        frame.setLocationRelativeTo(this);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JCheckBox cbShowFinancial = new JCheckBox("إظهار الأعمدة المالية (واجهة المحاسب) / إخفاء (واجهة أمين المخزن)", true);
        cbShowFinancial.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbShowFinancial.setFont(new Font("Tahoma", Font.BOLD, 12));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        top.add(cbShowFinancial);

        JPanel content = createIssuePanel(cbShowFinancial, frame);
        frame.add(top, BorderLayout.NORTH);
        frame.add(content, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void openReportsWindow() {
        // شاشة مستقلة لتقارير المخزون بنفس ستايل المشتريات
        JFrame frame = new JFrame("المخازن - تقارير المخزون");
        frame.setSize(760, 520);
        frame.setMinimumSize(new Dimension(600, 420));
        frame.setLocationRelativeTo(this);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JTextArea report = new JTextArea("تقارير المخزون\n\n- أرصدة المواد الخام\n- أرصدة الإنتاج تحت التشغيل\n- أرصدة المنتجات التامة\n- تقرير مخزون المندوبين\n- حركة الاستلام والصرف");
        report.setEditable(false);
        report.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        report.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        report.setFont(new Font("Tahoma", Font.PLAIN, 13));

        JButton refresh = new JButton("تحديث التقرير");
        refresh.addActionListener(e -> report.append("\nتم تحديث التقرير: " + LocalDate.now()));
        JButton openFull = new JButton("فتح التقارير الكاملة");
        openFull.addActionListener(e -> new WarehouseReportsFrame().setVisible(true));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        actions.add(refresh);
        actions.add(openFull);

        frame.add(new JScrollPane(report), BorderLayout.CENTER);
        frame.add(actions, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void openClearanceWindow() {
        JFrame frame = new JFrame("المخازن - إخلاء عهد المندوبين");
        frame.setSize(1050, 620);
        frame.setMinimumSize(new Dimension(900, 550));
        frame.setLocationRelativeTo(this);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JCheckBox cbShowFinancial = new JCheckBox("إظهار الأعمدة المالية (واجهة المحاسب) / إخفاء (واجهة أمين المخزن)", true);
        cbShowFinancial.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cbShowFinancial.setFont(new Font("Tahoma", Font.BOLD, 12));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        top.add(cbShowFinancial);

        JPanel content = createClearancePanel(cbShowFinancial, frame);
        frame.add(top, BorderLayout.NORTH);
        frame.add(content, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    // ======================= لوحة الاستلام =======================
    private JPanel createReceiptPanel(JCheckBox cbShowFinancial, JFrame owner) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JLabel header = new JLabel("حركة الاستلام المخزني - فصل بيانات الحركات عن البيانات المالية", SwingConstants.CENTER);
        header.setFont(new Font("Tahoma", Font.BOLD, 14));
        JPanel form = new JPanel(new GridBagLayout());
        form.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        form.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "إضافة صنف للاستلام", 2, 2, new Font("Tahoma", Font.BOLD, 11), new Color(0, 105, 92)));
        JTextField txtDocNo = new JTextField(10);
        try { txtDocNo.setText(DocumentNumberService.next("WAREHOUSE_RECEIPT", "WR-")); } catch (Exception ex) { txtDocNo.setText("WR- auto"); }
        txtDocNo.setEditable(false);
        JTextField txtItemCode = new JTextField(10);
        JTextField txtItemName = new JTextField(14);
        JComboBox<String> cmbUnit = new JComboBox<>(new String[]{"حبة","كرتون","باكت","كيلو","لتر","كيس","علبة"});
        JTextField txtQty = new JTextField("0", 6);
        JTextField txtCost = new JTextField("0", 7);
        JTextField txtTotal = new JTextField("0", 8);
        txtTotal.setEditable(false); txtTotal.setBackground(new Color(245,245,245));
        Runnable recalc = () -> { try { double q=Double.parseDouble(txtQty.getText().trim()); double c=Double.parseDouble(txtCost.getText().trim()); txtTotal.setText(String.valueOf(q*c)); } catch(Exception ignored){ txtTotal.setText("0"); } };
        txtQty.addActionListener(e->recalc.run()); txtCost.addActionListener(e->recalc.run());
        GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(4,4,4,4); gbc.fill=GridBagConstraints.HORIZONTAL; gbc.anchor=GridBagConstraints.EAST;
        int row=0; addFormRow(form,gbc,row++,"رقم المستند:",txtDocNo,"رقم الصنف:",txtItemCode); addFormRow(form,gbc,row++,"اسم الصنف:",txtItemName,"الوحدة:",cmbUnit); addFormRow(form,gbc,row++,"الكمية (حركة):",txtQty,"التكلفة (مالي):",txtCost); addFormRow(form,gbc,row++,"الإجمالي (مالي):",txtTotal,null,null);
        JButton btnAdd=new JButton("إضافة إلى جدول الاستلام"); btnAdd.setBackground(new Color(46,125,50)); btnAdd.setForeground(Color.WHITE); gbc.gridx=0; gbc.gridy=row; gbc.gridwidth=4; gbc.anchor=GridBagConstraints.CENTER; form.add(btnAdd,gbc);
        DefaultTableModel model=new DefaultTableModel(new Object[]{"رقم الصنف [حركة]","اسم الصنف [حركة]","الوحدة [حركة]","الكمية [حركة]","التكلفة [مالي]","الإجمالي [مالي]"},0){@Override public boolean isCellEditable(int r,int c){return false;}};
        JTable table=new JTable(model); table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); table.getTableHeader().setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); table.setRowHeight(24);
        cbShowFinancial.addActionListener(e -> toggleFinancialColumns(table, new int[]{4,5}, cbShowFinancial.isSelected()));
        btnAdd.addActionListener(e->{ String code=txtItemCode.getText().trim(); String name=txtItemName.getText().trim(); String unit=(String)cmbUnit.getSelectedItem(); if(code.isEmpty()||name.isEmpty()){JOptionPane.showMessageDialog(owner,"رقم الصنف واسم الصنف مطلوبان.","تنبيه",JOptionPane.WARNING_MESSAGE);return;} try{double q=Double.parseDouble(txtQty.getText().trim()); double c=Double.parseDouble(txtCost.getText().trim()); if(q<=0||c<0) throw new NumberFormatException(); double tot=q*c; model.addRow(new Object[]{code,name,unit,q,c,tot}); txtItemCode.setText(""); txtItemName.setText(""); txtQty.setText("0"); txtCost.setText("0"); txtTotal.setText("0"); toggleFinancialColumns(table,new int[]{4,5},cbShowFinancial.isSelected());}catch(NumberFormatException ex){JOptionPane.showMessageDialog(owner,"الكمية موجبة والتكلفة غير سالبة.","خطأ",JOptionPane.ERROR_MESSAGE);}});
        JScrollPane scroll=new JScrollPane(table); scroll.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT)); actions.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton btnRemove=new JButton("حذف السطر المحدد"); JButton btnApprove=new JButton("اعتماد الاستلام"); btnApprove.setBackground(new Color(16,185,129)); btnApprove.setForeground(Color.WHITE); JButton btnPrint=new JButton("عرض وطباعة");
        actions.add(btnRemove); actions.add(btnApprove); actions.add(btnPrint);
        btnRemove.addActionListener(e->{int sel=table.getSelectedRow(); if(sel>=0) model.removeRow(table.convertRowIndexToModel(sel));});
        btnApprove.addActionListener(e->{if(model.getRowCount()==0){JOptionPane.showMessageDialog(owner,"لا توجد أصناف.","تنبيه",JOptionPane.WARNING_MESSAGE);return;} int count=model.getRowCount(); double sum=0; for(int i=0;i<count;i++) sum+=((Number)model.getValueAt(i,5)).doubleValue(); JOptionPane.showMessageDialog(owner,"تم اعتماد استلام "+count+" صنف بإجمالي "+sum,"نجاح",JOptionPane.INFORMATION_MESSAGE); txtDocNo.setText(safeNext("WAREHOUSE_RECEIPT","WR-")); model.setRowCount(0);});
        btnPrint.addActionListener(e->{StringBuilder html=new StringBuilder("<h2>الاستلام المخزني</h2><p>رقم المستند: "+txtDocNo.getText()+"</p><table border='1' cellpadding='4' cellspacing='0' style='border-collapse:collapse; width:100%'><tr>"); for(int c=0;c<model.getColumnCount();c++) if(cbShowFinancial.isSelected()||c<4) html.append("<th>").append(model.getColumnName(c)).append("</th>"); html.append("</tr>"); for(int r=0;r<model.getRowCount();r++){html.append("<tr>"); for(int c=0;c<model.getColumnCount();c++) if(cbShowFinancial.isSelected()||c<4) html.append("<td>").append(model.getValueAt(r,c)).append("</td>"); html.append("</tr>");} html.append("</table><p>التوقيع: ____________________</p>"); new DocumentPreviewDialog(owner,"الاستلام المخزني",html.toString()).setVisible(true);});
        JPanel north=new JPanel(new BorderLayout()); north.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); north.add(header,BorderLayout.NORTH); north.add(form,BorderLayout.CENTER);
        panel.add(north,BorderLayout.NORTH); panel.add(scroll,BorderLayout.CENTER); panel.add(actions,BorderLayout.SOUTH);
        SwingUtilities.invokeLater(()->toggleFinancialColumns(table,new int[]{4,5},cbShowFinancial.isSelected()));
        return panel;
    }

    private JPanel createIssuePanel(JCheckBox cbShowFinancial, JFrame owner) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JLabel header = new JLabel("حركة الصرف المخزني - فصل بيانات الحركات عن البيانات المالية", SwingConstants.CENTER);
        header.setFont(new Font("Tahoma", Font.BOLD, 14));
        JPanel form = new JPanel(new GridBagLayout()); form.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        form.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "إضافة صنف للصرف",2,2,new Font("Tahoma",Font.BOLD,11),new Color(183,28,28)));
        JTextField txtDocNo=new JTextField(10); try{txtDocNo.setText(DocumentNumberService.next("WAREHOUSE_ISSUE","WI-"));}catch(Exception ex){txtDocNo.setText("WI- auto");} txtDocNo.setEditable(false);
        JTextField txtItemCode=new JTextField(10); JTextField txtItemName=new JTextField(14); JComboBox<String> cmbUnit=new JComboBox<>(new String[]{"حبة","كرتون","باكت","كيلو","لتر","كيس","علبة"}); JTextField txtQty=new JTextField("0",6); JTextField txtCost=new JTextField("0",7); JTextField txtTotal=new JTextField("0",8); txtTotal.setEditable(false); txtTotal.setBackground(new Color(245,245,245));
        Runnable recalc=()->{try{double q=Double.parseDouble(txtQty.getText().trim()); double c=Double.parseDouble(txtCost.getText().trim()); txtTotal.setText(String.valueOf(q*c));}catch(Exception ignored){txtTotal.setText("0");}}; txtQty.addActionListener(e->recalc.run()); txtCost.addActionListener(e->recalc.run());
        GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(4,4,4,4); gbc.fill=GridBagConstraints.HORIZONTAL; gbc.anchor=GridBagConstraints.EAST; int row=0; addFormRow(form,gbc,row++,"رقم المستند:",txtDocNo,"رقم الصنف:",txtItemCode); addFormRow(form,gbc,row++,"اسم الصنف:",txtItemName,"الوحدة:",cmbUnit); addFormRow(form,gbc,row++,"الكمية (حركة):",txtQty,"التكلفة (مالي):",txtCost); addFormRow(form,gbc,row++,"الإجمالي (مالي):",txtTotal,null,null);
        JButton btnAdd=new JButton("إضافة إلى جدول الصرف"); btnAdd.setBackground(new Color(198,40,40)); btnAdd.setForeground(Color.WHITE); gbc.gridx=0; gbc.gridy=row; gbc.gridwidth=4; gbc.anchor=GridBagConstraints.CENTER; form.add(btnAdd,gbc);
        DefaultTableModel model=new DefaultTableModel(new Object[]{"رقم الصنف [حركة]","اسم الصنف [حركة]","الوحدة [حركة]","الكمية [حركة]","التكلفة [مالي]","الإجمالي [مالي]"},0){@Override public boolean isCellEditable(int r,int c){return false;}};
        JTable table=new JTable(model); table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); table.getTableHeader().setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); table.setRowHeight(24);
        cbShowFinancial.addActionListener(e -> toggleFinancialColumns(table, new int[]{4,5}, cbShowFinancial.isSelected()));
        btnAdd.addActionListener(e->{ String code=txtItemCode.getText().trim(); String name=txtItemName.getText().trim(); String unit=(String)cmbUnit.getSelectedItem(); try{double q=Double.parseDouble(txtQty.getText().trim()); double c=Double.parseDouble(txtCost.getText().trim()); if(code.isEmpty()||name.isEmpty()) throw new IllegalArgumentException("رقم الصنف واسم الصنف مطلوبان."); if(q<=0||c<0) throw new NumberFormatException(); double tot=q*c; model.addRow(new Object[]{code,name,unit,q,c,tot}); txtItemCode.setText(""); txtItemName.setText(""); txtQty.setText("0"); txtCost.setText("0"); txtTotal.setText("0"); toggleFinancialColumns(table,new int[]{4,5},cbShowFinancial.isSelected());}catch(IllegalArgumentException ex){JOptionPane.showMessageDialog(owner,ex.getMessage(),"تنبيه",JOptionPane.WARNING_MESSAGE);}catch(Exception ex){JOptionPane.showMessageDialog(owner,"الكمية موجبة والتكلفة غير سالبة.","خطأ",JOptionPane.ERROR_MESSAGE);}});
        JScrollPane scroll=new JScrollPane(table); scroll.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT)); actions.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); JButton btnRemove=new JButton("حذف السطر المحدد"); JButton btnApprove=new JButton("اعتماد الصرف"); btnApprove.setBackground(new Color(220,38,38)); btnApprove.setForeground(Color.WHITE); JButton btnPrint=new JButton("عرض وطباعة"); actions.add(btnRemove); actions.add(btnApprove); actions.add(btnPrint);
        btnRemove.addActionListener(e->{int sel=table.getSelectedRow(); if(sel>=0) model.removeRow(table.convertRowIndexToModel(sel));});
        btnApprove.addActionListener(e->{if(model.getRowCount()==0){JOptionPane.showMessageDialog(owner,"لا توجد أصناف.","تنبيه",JOptionPane.WARNING_MESSAGE);return;} int count=model.getRowCount(); double sum=0; for(int i=0;i<count;i++) sum+=((Number)model.getValueAt(i,5)).doubleValue(); JOptionPane.showMessageDialog(owner,"تم اعتماد صرف "+count+" صنف بإجمالي "+sum,"نجاح",JOptionPane.INFORMATION_MESSAGE); txtDocNo.setText(safeNext("WAREHOUSE_ISSUE","WI-")); model.setRowCount(0);});
        btnPrint.addActionListener(e->{StringBuilder html=new StringBuilder("<h2>الصرف المخزني</h2><p>رقم المستند: "+txtDocNo.getText()+"</p><table border='1' cellpadding='4' cellspacing='0' style='border-collapse:collapse; width:100%'><tr>"); for(int c=0;c<model.getColumnCount();c++) if(cbShowFinancial.isSelected()||c<4) html.append("<th>").append(model.getColumnName(c)).append("</th>"); html.append("</tr>"); for(int r=0;r<model.getRowCount();r++){html.append("<tr>"); for(int c=0;c<model.getColumnCount();c++) if(cbShowFinancial.isSelected()||c<4) html.append("<td>").append(model.getValueAt(r,c)).append("</td>"); html.append("</tr>");} html.append("</table><p>التوقيع: ____________________</p>"); new DocumentPreviewDialog(owner,"الصرف المخزني",html.toString()).setVisible(true);});
        JPanel north=new JPanel(new BorderLayout()); north.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); north.add(header,BorderLayout.NORTH); north.add(form,BorderLayout.CENTER);
        panel.add(north,BorderLayout.NORTH); panel.add(scroll,BorderLayout.CENTER); panel.add(actions,BorderLayout.SOUTH);
        SwingUtilities.invokeLater(()->toggleFinancialColumns(table,new int[]{4,5},cbShowFinancial.isSelected()));
        return panel;
    }

    private JPanel createClearancePanel(JCheckBox cbShowFinancial, JFrame owner) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JLabel header = new JLabel("إخلاء عهد المندوبين - مرتجعات المتبقي من سيارات الفان", SwingConstants.CENTER);
        header.setFont(new Font("Tahoma", Font.BOLD, 14));
        JPanel form = new JPanel(new GridBagLayout()); form.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        form.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "إضافة إخلاء عهدة",2,2,new Font("Tahoma",Font.BOLD,11),new Color(13,71,161)));
        JTextField txtDocNo=new JTextField(10); try{txtDocNo.setText(DocumentNumberService.next("VAN_RETURN","VRN-"));}catch(Exception ex){txtDocNo.setText("VRN- auto");} txtDocNo.setEditable(false);
        JTextField txtItemCode=new JTextField(10); JTextField txtItemName=new JTextField(12); JTextField txtDelegate=new JTextField(10); txtDelegate.setToolTipText("اسم المندوب / رقم السيارة"); JComboBox<String> cmbUnit=new JComboBox<>(new String[]{"حبة","كرتون","باكت","كيلو","علبة"}); JTextField txtQty=new JTextField("0",6); JTextField txtCost=new JTextField("0",7); JTextField txtTotal=new JTextField("0",8); txtTotal.setEditable(false); txtTotal.setBackground(new Color(245,245,245));
        Runnable recalc=()->{try{double q=Double.parseDouble(txtQty.getText().trim()); double c=Double.parseDouble(txtCost.getText().trim()); txtTotal.setText(String.valueOf(q*c));}catch(Exception ignored){txtTotal.setText("0");}}; txtQty.addActionListener(e->recalc.run()); txtCost.addActionListener(e->recalc.run());
        GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(4,4,4,4); gbc.fill=GridBagConstraints.HORIZONTAL; gbc.anchor=GridBagConstraints.EAST; int row=0; addFormRow(form,gbc,row++,"رقم الإخلاء:",txtDocNo,"رقم الصنف:",txtItemCode); addFormRow(form,gbc,row++,"اسم الصنف:",txtItemName,"المندوب/السيارة:",txtDelegate); addFormRow(form,gbc,row++,"الوحدة:",cmbUnit,"الكمية المرتجعة:",txtQty); addFormRow(form,gbc,row++,"التكلفة (مالي):",txtCost,"الإجمالي (مالي):",txtTotal);
        JButton btnAdd=new JButton("إضافة إلى جدول الإخلاء"); btnAdd.setBackground(new Color(13,71,161)); btnAdd.setForeground(Color.WHITE); gbc.gridx=0; gbc.gridy=row; gbc.gridwidth=4; gbc.anchor=GridBagConstraints.CENTER; form.add(btnAdd,gbc);
        DefaultTableModel model=new DefaultTableModel(new Object[]{"رقم الصنف [حركة]","اسم الصنف [حركة]","المندوب/السيارة [حركة]","الوحدة [حركة]","الكمية [حركة]","التكلفة [مالي]","الإجمالي [مالي]"},0){@Override public boolean isCellEditable(int r,int c){return false;}};
        JTable table=new JTable(model); table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); table.getTableHeader().setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); table.setRowHeight(24);
        cbShowFinancial.addActionListener(e -> toggleFinancialColumns(table, new int[]{5,6}, cbShowFinancial.isSelected()));
        btnAdd.addActionListener(e->{ String code=txtItemCode.getText().trim(); String name=txtItemName.getText().trim(); String delegate=txtDelegate.getText().trim(); String unit=(String)cmbUnit.getSelectedItem(); try{double q=Double.parseDouble(txtQty.getText().trim()); double c=Double.parseDouble(txtCost.getText().trim()); if(code.isEmpty()||name.isEmpty()) throw new IllegalArgumentException("رقم الصنف واسم الصنف مطلوبان."); if(delegate.isEmpty()) throw new IllegalArgumentException("اسم المندوب/السيارة مطلوب."); if(q<=0||c<0) throw new NumberFormatException(); double tot=q*c; model.addRow(new Object[]{code,name,delegate,unit,q,c,tot}); txtItemCode.setText(""); txtItemName.setText(""); txtQty.setText("0"); txtCost.setText("0"); txtTotal.setText("0"); toggleFinancialColumns(table,new int[]{5,6},cbShowFinancial.isSelected());}catch(IllegalArgumentException ex){JOptionPane.showMessageDialog(owner,ex.getMessage(),"تنبيه",JOptionPane.WARNING_MESSAGE);}catch(Exception ex){JOptionPane.showMessageDialog(owner,"الكمية موجبة والتكلفة غير سالبة.","خطأ",JOptionPane.ERROR_MESSAGE);}});
        JScrollPane scroll=new JScrollPane(table); scroll.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT)); actions.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); JButton btnRemove=new JButton("حذف السطر المحدد"); JButton btnApprove=new JButton("اعتماد الإخلاء"); btnApprove.setBackground(new Color(16,185,129)); btnApprove.setForeground(Color.WHITE); JButton btnPrint=new JButton("عرض وطباعة"); actions.add(btnRemove); actions.add(btnApprove); actions.add(btnPrint);
        btnRemove.addActionListener(e->{int sel=table.getSelectedRow(); if(sel>=0) model.removeRow(table.convertRowIndexToModel(sel));});
        btnApprove.addActionListener(e->{if(model.getRowCount()==0){JOptionPane.showMessageDialog(owner,"لا توجد أصناف.","تنبيه",JOptionPane.WARNING_MESSAGE);return;} int count=model.getRowCount(); double sum=0; for(int i=0;i<count;i++) sum+=((Number)model.getValueAt(i,6)).doubleValue(); JOptionPane.showMessageDialog(owner,"تم اعتماد إخلاء عهد "+count+" بند بإجمالي "+sum,"نجاح",JOptionPane.INFORMATION_MESSAGE); txtDocNo.setText(safeNext("VAN_RETURN","VRN-")); model.setRowCount(0);});
        btnPrint.addActionListener(e->{StringBuilder html=new StringBuilder("<h2>إخلاء عهد المندوبين</h2><p>رقم الإخلاء: "+txtDocNo.getText()+"</p><table border='1' cellpadding='4' cellspacing='0' style='border-collapse:collapse; width:100%'><tr>"); for(int c=0;c<model.getColumnCount();c++) if(cbShowFinancial.isSelected()||c<5) html.append("<th>").append(model.getColumnName(c)).append("</th>"); html.append("</tr>"); for(int r=0;r<model.getRowCount();r++){html.append("<tr>"); for(int c=0;c<model.getColumnCount();c++) if(cbShowFinancial.isSelected()||c<5) html.append("<td>").append(model.getValueAt(r,c)).append("</td>"); html.append("</tr>");} html.append("</table><p>توقيع المندوب: ____________________ &nbsp;&nbsp; توقيع أمين المخزن: ____________________</p>"); new DocumentPreviewDialog(owner,"إخلاء عهد المندوبين",html.toString()).setVisible(true);});
        JPanel north=new JPanel(new BorderLayout()); north.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); north.add(header,BorderLayout.NORTH); north.add(form,BorderLayout.CENTER);
        panel.add(north,BorderLayout.NORTH); panel.add(scroll,BorderLayout.CENTER); panel.add(actions,BorderLayout.SOUTH);
        SwingUtilities.invokeLater(()->toggleFinancialColumns(table,new int[]{5,6},cbShowFinancial.isSelected()));
        return panel;
    }

    private void addFormRow(JPanel form, GridBagConstraints gbc, int row, String label1, JComponent field1, String label2, JComponent field2) {
        gbc.gridy=row; gbc.gridwidth=1; gbc.gridx=0; gbc.weightx=0; if(label1!=null){JLabel l1=new JLabel(label1); l1.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); form.add(l1,gbc);} gbc.gridx=1; gbc.weightx=0.5; if(field1!=null){field1.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); form.add(field1,gbc);} gbc.gridx=2; gbc.weightx=0; if(label2!=null){JLabel l2=new JLabel(label2); l2.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); form.add(l2,gbc);} gbc.gridx=3; gbc.weightx=0.5; if(field2!=null){field2.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); form.add(field2,gbc);}
    }
    private void toggleFinancialColumns(JTable table,int[] cols,boolean show){for(int col:cols){if(col<0||col>=table.getColumnModel().getColumnCount()) continue; TableColumn tc=table.getColumnModel().getColumn(col); if(show){tc.setMinWidth(70); tc.setMaxWidth(500); tc.setPreferredWidth(110); tc.setWidth(110); tc.setResizable(true);} else{tc.setMinWidth(0); tc.setMaxWidth(0); tc.setPreferredWidth(0); tc.setWidth(0); tc.setResizable(false);}} table.getTableHeader().repaint(); table.repaint();}
    private String safeNext(String type,String prefix){try{return DocumentNumberService.next(type,prefix);}catch(Exception ex){return prefix+"auto";}}
    public static void main(String[] args){SwingUtilities.invokeLater(()->new InventoryMainFrame().setVisible(true));}
}
