import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class StockAlert {
    private String itemCode;
    private String itemName;
    private double currentStock;
    private double minStock;

    public StockAlert() {}

    public StockAlert(String itemCode, String itemName, double currentStock, double minStock) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.currentStock = currentStock;
        this.minStock = minStock;
    }

    public boolean isAlertRequired() {
        return this.currentStock <= this.minStock;
    }

    public void exportToTextFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("StockAlertLog.txt", true))) {
            writer.println("=== تنبيه مخزني ===");
            writer.println("رقم الصنف: " + itemCode);
            writer.println("اسم الصنف: " + itemName);
            writer.println("الرصيد الحالي: " + currentStock);
            writer.println("الحد الأدنى: " + minStock);
            writer.println("حالة التنبيه: " + (isAlertRequired() ? "تجاوز الحد الأدنى" : "طبيعي"));
            writer.println("==================");
            System.out.println("Success: Stock Alert Exported to StockAlertLog.txt");
        } catch (IOException e) {
            System.err.println("Error exporting Stock Alert: " + e.getMessage());
        }
    }

}