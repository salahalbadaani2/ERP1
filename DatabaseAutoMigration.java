import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/** تهيئة غير مدمرة للجداول والحسابات الجديدة عند تشغيل النظام. */
public final class DatabaseAutoMigration {
    private DatabaseAutoMigration() {
    }

    public static void run() {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                try {
                    statement.executeUpdate("ALTER TABLE inventory_movements DROP INDEX document_number");
                } catch (SQLException e) {
                    // الفهرس قد يكون محذوفاً بالفعل
                }
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS document_sequences ("
                        + "document_type VARCHAR(50) PRIMARY KEY, next_number INT NOT NULL) ENGINE=InnoDB");
                 statement.executeUpdate("CREATE TABLE IF NOT EXISTS chart_of_accounts ("
                         + "account_code VARCHAR(20) PRIMARY KEY, account_name VARCHAR(255) NOT NULL, "
                         + "account_type VARCHAR(20) NOT NULL, parent_code VARCHAR(20), account_level INT NOT NULL, "
                         + "is_sub_account TINYINT(1) NOT NULL DEFAULT 0, current_balance DECIMAL(18,4) NOT NULL DEFAULT 0) ENGINE=InnoDB");
                statement.executeUpdate("ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS unit_type VARCHAR(10) NOT NULL DEFAULT 'COUNT' AFTER unit");
                statement.executeUpdate("ALTER TABLE inventory_movements MODIFY COLUMN quantity DECIMAL(12, 3) NOT NULL");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS sales_invoice_details ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY, invoice_code VARCHAR(50) NOT NULL, "
                        + "item_code VARCHAR(50) NOT NULL, item_name VARCHAR(255) NOT NULL, "
                        + "quantity DECIMAL(12, 3) NOT NULL, unit_price DECIMAL(15, 2) NOT NULL, "
                        + "amount DECIMAL(15, 2) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "INDEX idx_sid_invoice (invoice_code)) ENGINE=InnoDB");
                statement.executeUpdate("ALTER TABLE sales_invoice_details MODIFY COLUMN quantity DECIMAL(12, 3) NOT NULL");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS purchase_invoice_details ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY, invoice_code VARCHAR(50) NOT NULL, "
                        + "item_code VARCHAR(50) NOT NULL, item_name VARCHAR(255) NOT NULL, "
                        + "quantity DECIMAL(12, 3) NOT NULL, unit_cost DECIMAL(15, 2) NOT NULL, "
                        + "amount DECIMAL(15, 2) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "INDEX idx_pid_invoice (invoice_code)) ENGINE=InnoDB");
                statement.executeUpdate("ALTER TABLE purchase_invoice_details MODIFY COLUMN quantity DECIMAL(12, 3) NOT NULL");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS production_details ("
                        + "id BIGINT AUTO_INCREMENT PRIMARY KEY, production_order VARCHAR(50) NOT NULL, "
                        + "item_code VARCHAR(50) NOT NULL, item_name VARCHAR(255) NOT NULL, "
                        + "quantity DECIMAL(12, 3) NOT NULL, unit_cost DECIMAL(15, 2) NOT NULL, "
                        + "total_cost DECIMAL(15, 2) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "INDEX idx_pd_order (production_order)) ENGINE=InnoDB");
                statement.executeUpdate("ALTER TABLE production_details MODIFY COLUMN quantity DECIMAL(12, 3) NOT NULL");
            }
            String sql = "INSERT INTO chart_of_accounts "
                    + "(account_code, account_name, account_type, parent_code, account_level, is_sub_account, current_balance) "
                    + "VALUES (?, ?, ?, ?, ?, ?, 0) ON DUPLICATE KEY UPDATE account_name = VALUES(account_name), "
                    + "parent_code = VALUES(parent_code), account_level = VALUES(account_level), "
                    + "is_sub_account = VALUES(is_sub_account)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (String[] account : accounts()) {
                    statement.setString(1, account[0]);
                    statement.setString(2, account[1]);
                    statement.setString(3, account[2]);
                    statement.setString(4, account[3]);
                    statement.setInt(5, Integer.parseInt(account[4]));
                    statement.setBoolean(6, "1".equals(account[5]));
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            connection.commit();
        } catch (SQLException exception) {
            System.err.println("تعذر تنفيذ الهجرة الآلية: " + exception.getMessage());
        }
    }

    private static String[][] accounts() {
        return new String[][] {
            {"1", "الأصول", "ASSET", "", "1", "0"},
            {"11", "النقدية والبنوك", "ASSET", "1", "2", "0"},
            {"111", "الصناديق والبنوك", "ASSET", "11", "3", "0"},
            {"1110101", "الصندوق الرئيسي", "ASSET", "111", "6", "1"},
            {"12", "المخزون", "ASSET", "1", "2", "0"},
            {"121", "المخزون والمستودعات", "ASSET", "12", "3", "0"},
            {"12101", "مخزون المواد الخام", "ASSET", "121", "4", "0"},
            {"1210101", "مخزن المواد الخام الرئيسي", "ASSET", "12101", "6", "1"},
            {"12102", "الإنتاج تحت التشغيل WIP", "ASSET", "121", "4", "0"},
            {"1210201", "أمر إنتاج قيد التشغيل الرئيسي", "ASSET", "12102", "6", "1"},
            {"12103", "مخزون المنتجات التامة", "ASSET", "121", "4", "0"},
            {"1210301", "مخزن المنتجات التامة الرئيسي", "ASSET", "12103", "6", "1"},
            {"123", "الذمم المدينة والعملاء", "ASSET", "1", "3", "0"},
            {"2", "الخصوم", "LIABILITY", "", "1", "0"},
            {"21", "الموردون", "LIABILITY", "2", "2", "0"},
            {"210101", "مورد عام", "LIABILITY", "21", "6", "1"},
            {"22", "الضرائب والالتزامات المتداولة", "LIABILITY", "2", "2", "0"},
            {"220301", "ضريبة القيمة المضافة", "LIABILITY", "22", "6", "1"},
            {"3", "حقوق الملكية", "EQUITY", "", "1", "0"},
            {"31", "رأس المال والأرباح المحتجزة", "EQUITY", "3", "2", "0"},
            {"310101", "رأس المال", "EQUITY", "31", "6", "1"},
            {"4", "الإيرادات", "REVENUE", "", "1", "0"},
            {"41", "إيرادات المبيعات", "REVENUE", "4", "2", "0"},
            {"410101", "مبيعات المنتجات التامة", "REVENUE", "41", "6", "1"},
            {"410201", "مردودات ومسموحات المبيعات", "REVENUE", "41", "6", "1"},
            {"5", "تكاليف ومصروفات التصنيع", "EXPENSE", "", "1", "0"},
            {"51", "تكلفة المبيعات COGS", "EXPENSE", "5", "2", "0"},
            {"510101", "تكلفة المنتجات المباعة", "EXPENSE", "51", "6", "1"},
            {"52", "التكاليف الصناعية غير المباشرة", "EXPENSE", "5", "2", "0"},
            {"520101", "التكاليف الصناعية الفعلية", "EXPENSE", "52", "6", "1"},
            {"520201", "التكاليف الصناعية المحملة", "EXPENSE", "52", "6", "1"},
            {"520901", "انحرافات التكاليف الصناعية", "EXPENSE", "52", "6", "1"},
            {"53", "الأجور المباشرة", "EXPENSE", "5", "2", "0"},
            {"530101", "أجور الإنتاج المباشرة", "EXPENSE", "53", "6", "1"},
            {"54", "المصروفات الإدارية والبيعية", "EXPENSE", "5", "2", "0"},
            {"540101", "مصروفات إدارية عامة", "EXPENSE", "54", "6", "1"},
            {"540201", "مصروفات بيع وتوزيع", "EXPENSE", "54", "6", "1"}
        };
    }
}
