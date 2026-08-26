import java.util.ArrayList;
import java.util.List;

public class WorkInProcessStore {
    // الحساب الرئيسي: مخزن الإنتاج تحت التشغيل
    private final String parentAccountCode = "12102";
    private List<RawMaterial> wipMaterials;

    public WorkInProcessStore() {
        this.wipMaterials = new ArrayList<>();
    }

    // 1. استقبال الخامات المصروفة وتحويلها لحساب الإنتاج تحت التشغيل
    public void receiveMaterialFromIssueNote(MaterialIssueNote issueNote, String wipItemName) {
        if (issueNote == null) {
            throw new IllegalArgumentException("خطأ أمني: لا يمكن استقبال إذن صرف فارغ!");
        }

        // تحويل الرمز الفرعي تلقائياً من مخزن الخام (12101xx) إلى مخزن التشغيل (12102xx)
        String rawCode = issueNote.getItemCode();
        String wipCode = rawCode.replace("12101", "12102");

        RawMaterial existingItem = findItemByCode(wipCode);

        if (existingItem != null) {
            // تحديث الكمية في حال كان الصنف موجوداً سابقاً في خط الإنتاج
            existingItem.setQuantity(existingItem.getQuantity() + issueNote.getQuantity());
        } else {
            // إنشاء حساب فرعي جديد للتشغيل
            double unitPrice = issueNote.getTotalValue() / issueNote.getQuantity();
            RawMaterial newItem = new RawMaterial(wipCode, wipItemName, issueNote.getQuantity(), unitPrice);
            wipMaterials.add(newItem);
        }

        System.out.println("تم تحويل المواد بنجاح إلى حساب الإنتاج تحت التشغيل (" + parentAccountCode + ").");
    }

    // 2. البحث عن صنف فرعي داخل خط الإنتاج
    public RawMaterial findItemByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        for (RawMaterial item : wipMaterials) {
            if (item.getItemCode().equals(code.trim())) {
                return item;
            }
        }
        return null;
    }

    // 3. حساب إجمالي رصيد حساب الإنتاج تحت التشغيل
    public double getTotalWipValue() {
        double total = 0.0;
        for (RawMaterial item : wipMaterials) {
            total += item.getTotalValue();
        }
        return total;
    }

    // 4. عرض تقرير جرد مخزن الإنتاج تحت التشغيل
    public void displayWipReport() {
        System.out.println("========================================");
        System.out.println("  تقرير جرد حساب (12102) - الإنتاج تحت التشغيل  ");
        System.out.println("========================================");

        if (wipMaterials.isEmpty()) {
            System.out.println("لا توجد مواد قيد التشغيل حالياً.");
        } else {
            for (RawMaterial item : wipMaterials) {
                item.displayInfo();
            }
            System.out.println("إجمالي رصيد حساب الإنتاج تحت التشغيل: " + getTotalWipValue());
        }
        System.out.println("========================================");
    }
}