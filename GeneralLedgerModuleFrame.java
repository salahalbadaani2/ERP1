import javax.swing.*;
import java.awt.*;

public class GeneralLedgerModuleFrame extends JFrame {
    public GeneralLedgerModuleFrame() {
        setTitle("نظام ERP المصنعي - الحسابات العامة والقيود");
        setSize(580, 420);
        setMinimumSize(new Dimension(500, 380));
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel panel = new JPanel(new GridLayout(6, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JButton btnJournal = new JButton("قيد يومية (جزئي ومركب)");
        JButton btnBank = new JButton("بيان المطابقة البنكية");
        JButton btnDebit = new JButton("إشعار مدين (Debit Note)");
        JButton btnCredit = new JButton("إشعار دائن (Credit Note)");
        JButton btnVendor = new JButton("تهيئة حساب مورد");
        JButton btnCustomer = new JButton("تهيئة حساب زبون (عميل)");

        Font f = new Font("Segoe UI", Font.PLAIN, 14);
        btnJournal.setFont(f);
        btnBank.setFont(f);
        btnDebit.setFont(f);
        btnCredit.setFont(f);
        btnVendor.setFont(f);
        btnCustomer.setFont(f);

        btnJournal.addActionListener(e -> new JournalEntryFrame().setVisible(true));
        btnBank.addActionListener(e -> new BankReconciliationFrame().setVisible(true));
        btnDebit.addActionListener(e -> new DebitNoteFrame().setVisible(true));
        btnCredit.addActionListener(e -> new CreditNoteFrame().setVisible(true));
        btnVendor.addActionListener(e -> new VendorSetupFrame().setVisible(true));
        btnCustomer.addActionListener(e -> new CustomerSetupFrame().setVisible(true));

        panel.add(btnJournal);
        panel.add(btnBank);
        panel.add(btnDebit);
        panel.add(btnCredit);
        panel.add(btnVendor);
        panel.add(btnCustomer);

        add(panel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GeneralLedgerModuleFrame().setVisible(true));
    }
}
