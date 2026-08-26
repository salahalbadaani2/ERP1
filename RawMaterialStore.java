import java.util.ArrayList;
import java.util.List;

public class RawMaterialStore {
    // الحساب الرئيسي التابع له هذا المخزن في شجرة الحسابات (مخزن المواد الخام)
    private final String parentAccountCode = "12101"; 
    private List<RawMaterial> materials;

    public RawMaterialStore() {
        this.materials = new ArrayList<>();
    }

    // 1. إضافة صنف فرعي جديد مع التحقق من عدم تكرار رقم الحساب الفرعي
    public void addMaterial(RawMaterial material) {
        if (material == null) {
            throw new IllegalArgumentException("خطأ أمني: لا يمكن إضافة صنف فارغ!");
        }

        if (findMaterialByCode(material.getItemCode()) != null) {
            throw new IllegalArgumentException("خطأ محاسبي: رقم الحساب الفرعي (" + material.getItemCode() + ") موجود مسبقاً!");
        }

        materials.add(material);
        System.out.println("تم تسجيل الصنف (" + material.getItemName() + ") بالحساب الفرعي (" + material.getItemCode() + ") تحت الحساب الرئيسي (" + parentAccountCode + ").");
    }

    // 2. البحث عن صنف بواسطة رقم الحساب الفرعي
    public RawMaterial findMaterialByCode(String itemCode) {
        if (itemCode == null || itemCode.trim().isEmpty()) {
            return null;
        }
        for (RawMaterial mat : materials) {
            if (mat.getItemCode().equals(itemCode.trim())) {
                return mat;
            }
        }
        return null;
    }

    // 3. حساب إجمالي القيمة المالية لمخزن المواد الخام
    public double getTotalStoreValue() {
        double total = 0.0;
        for (RawMaterial mat : materials) {
            total += mat.getTotalValue();
        }
        return total;
    }

    // 4. عرض تقرير جرد مخزن المواد الخام
    public void displayStoreReport() {
        System.out.println("========================================");
        System.out.println("  تقرير جرد حساب (12101) - مخزن المواد الخام  ");
        System.out.println("========================================");

        if (materials.isEmpty()) {
            System.out.println("لا توجد أصناف مسجلة حالياً.");
        } else {
            for (RawMaterial mat : materials) {
                mat.displayInfo();
            }
            System.out.println("إجمالي رصيد حساب مخزن المواد الخام: " + getTotalStoreValue());
        }
        System.out.println("========================================");
    }
}