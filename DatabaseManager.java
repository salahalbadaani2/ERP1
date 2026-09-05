import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;

public class DatabaseManager {
    // تحميل تعريف مكتبة MySQL آلياً في الذاكرة عند تشغيل أي شاشة
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("خطأ: لم يتم العثور على مكتبة MySQL Connector: " + e.getMessage());
        }
    }

    // مسار الاتصال بقاعدة بيانات MySQL المعتمدة عبر XAMPP مع دعم اللغة العربية UTF-8
    private static final String URL = "jdbc:mysql://localhost:3306/erp_factory_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // خادم XAMPP الافتراضي بدون كلمة مرور

    /**
     * إنشاء الاتصال بقاعدة البيانات
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static void addUniqueIndexIfMissing(Connection connection, Statement statement,
                                                String tableName, String indexName, String columnName)
            throws SQLException {
        String sql = "SELECT 1 FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?";
        try (PreparedStatement check = connection.prepareStatement(sql)) {
            check.setString(1, tableName);
            check.setString(2, indexName);
            try (ResultSet resultSet = check.executeQuery()) {
                if (resultSet.next()) {
                    return;
                }
            }
        }
        statement.executeUpdate("ALTER TABLE " + tableName
                + " ADD UNIQUE KEY " + indexName + " (" + columnName + ")");
    }

    /**
     * فحص أمني ومحاسبي مركزي:
     * يمنع أي عمليات مالية أو مخزنية على الحسابات الرئيسية.
    * الحسابات المقبولة للحركة هي الحسابات الفرعية غير التجميعية فقط.
     */
    public static boolean isSubAccount(String accountCode) {
        if (accountCode == null || accountCode.trim().isEmpty()) {
            return false;
        }
        String sql = "SELECT is_sub_account FROM chart_of_accounts WHERE account_code = ?";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountCode.trim());
            try (java.sql.ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        } catch (SQLException exception) {
            // فشل الاتصال بقاعدة البيانات - يُحظر إرجاع true، يجب إرجاع false وإلقاء استثناء لإيقاف المعاملة
            throw new IllegalStateException("فشل الاتصال بقاعدة البيانات للتحقق من الحساب: " + accountCode, exception);
        }
    }

    /**
     * دالة التحقق الإلزامية قبل تنفيذ أي قيد أو حركة
     */
    public static void validateSubAccount(String accountCode) {
        if (!isSubAccount(accountCode)) {
            throw new IllegalArgumentException("خطأ أمني ومحاسبي: لا يمكن تسجيل حركة مالية على حساب رئيسي.");
        }
    }

    /**
     * تهيئة وإنشاء كافة جداول قاعدة البيانات وتعديل ترميزها للغة العربية آلية
     */
    public static void initializeDatabase() {
        // 1. إنشاء قاعدة البيانات بتشفير utf8mb4 إن لم تكن موجودة
        String serverUrl = "jdbc:mysql://localhost:3306/?useUnicode=true&characterEncoding=UTF-8";
        try (Connection serverConn = DriverManager.getConnection(serverUrl, USER, PASSWORD);
             Statement serverStmt = serverConn.createStatement()) {
            serverStmt.executeUpdate("CREATE DATABASE IF NOT EXISTS erp_factory_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            System.err.println("خطأ أثناء إنشاء قاعدة البيانات تلقائياً: " + e.getMessage());
        }

        // ================================================================
        // (M2) تعطيل أوامر CREATE/ALTER/DROP من هذا الملف بقرار خطة التوحيد:
        //  - بنية الجداول تُدار من schema.sql (يدوياً) + DatabaseAutoMigration.run().
        //  - DatabaseManager للاتصال وعمليات البيانات والمعاملات فقط.
        //  - النص الأصلي محفوظ داخل if(false) ويُستعاد من git tag:
        //    backup-before-schema-fix-*  عند الحاجة.
        // ================================================================
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeQuery("SELECT 1");
            }
            System.out.println("=== تم التأكد من الاتصال بقاعدة البيانات erp_factory_db (بدون DDL) ===");
        } catch (SQLException e) {
            System.err.println("خطأ أثناء فحص اتصال قاعدة البيانات: " + e.getMessage());
            return;
        }

        if (false) {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            
            // 2. تعديل ترميز قاعدة البيانات القائمة لدعم النصوص العربية بالكامل
            stmt.executeUpdate("ALTER DATABASE erp_factory_db CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci");

            // مسح الفهارس القديمة على عمود document_number في جدول حركات المخزون
            try { stmt.executeUpdate("ALTER TABLE inventory_movements DROP INDEX document_number"); } catch (SQLException ignored) {}

            // جدول حركات المخزون (inventory_movements) - إنشاء تلقائي عند بداية التشغيل
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS inventory_movements ("
                    + "movement_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "document_number VARCHAR(50), "
                    + "movement_type VARCHAR(20), "
                    + "item_code VARCHAR(50), "
                    + "item_name VARCHAR(255), "
                    + "quantity DECIMAL(12, 3), "
                    + "unit_cost DECIMAL(15, 2), "
                    + "inventory_account VARCHAR(20), "
                    + "counter_account VARCHAR(20), "
                    + "receiver VARCHAR(255), "
                    + "deliverer VARCHAR(255), "
                    + "narration TEXT, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            // بطاقة الأصناف والمخزون: تعتمد عليها شاشات الأصناف والمخازن وحركات البيع.
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS inventory_items ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, item_code VARCHAR(50) NOT NULL UNIQUE, item_name VARCHAR(255) NOT NULL, "
                    + "item_type VARCHAR(50) NOT NULL DEFAULT 'منتج تام', category VARCHAR(100) DEFAULT 'منتجات تامة', unit VARCHAR(50) NOT NULL DEFAULT 'وحدة', "
                    + "unit_type VARCHAR(10) NOT NULL DEFAULT 'COUNT', unit_weight DECIMAL(10,2) NOT NULL DEFAULT 0, default_price DECIMAL(12,2) NOT NULL DEFAULT 0, "
                    + "default_sale_price DECIMAL(18,4) NOT NULL DEFAULT 0, unit_cost DECIMAL(18,4) NOT NULL DEFAULT 0, "
                    + "current_stock DECIMAL(18,4) NOT NULL DEFAULT 0, inventory_account VARCHAR(20) DEFAULT '1210301', "
                    + "sales_revenue_account VARCHAR(20) DEFAULT '410101', cogs_account VARCHAR(20) DEFAULT '510101', "
                    + "conversion_factor DECIMAL(18,4) NOT NULL DEFAULT 1, min_stock_level DECIMAL(18,4) NOT NULL DEFAULT 0, "
                    + "expiry_date DATE NULL, batch_no VARCHAR(50) NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            // 1. جدول استلام المواد الخام (GRN)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS goods_receipt_notes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "grn_code VARCHAR(50), " +
                    "supplier_account VARCHAR(20), " +
                    "raw_material_account VARCHAR(20), " +
                    "total_amount DOUBLE, " +
                    "is_posted TINYINT(1) NOT NULL DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
            try { stmt.executeUpdate("ALTER TABLE goods_receipt_notes ADD COLUMN IF NOT EXISTS is_posted TINYINT(1) NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}

            // 2. جدول صرف المواد للإنتاج (Material Issue)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS material_issue_notes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "issue_code VARCHAR(50), " +
                    "wip_account VARCHAR(20), " +
                    "raw_material_account VARCHAR(20), " +
                    "total_amount DOUBLE, " +
                    "is_posted TINYINT(1) NOT NULL DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
            try { stmt.executeUpdate("ALTER TABLE material_issue_notes ADD COLUMN IF NOT EXISTS is_posted TINYINT(1) NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}

            // 3. جدول استلام المنتج التام (Finished Goods Receipt)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS finished_goods_notes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "note_code VARCHAR(50), " +
                    "finished_goods_account VARCHAR(20), " +
                    "wip_account VARCHAR(20), " +
                    "total_amount DOUBLE, " +
                    "is_posted TINYINT(1) NOT NULL DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
            try { stmt.executeUpdate("ALTER TABLE finished_goods_notes ADD COLUMN IF NOT EXISTS is_posted TINYINT(1) NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}

            // 4. جدول تحويلات سيارات التوزيع (Van Transfer)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS van_transfer_notes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "transfer_code VARCHAR(50), " +
                    "van_account VARCHAR(20), " +
                    "finished_goods_account VARCHAR(20), " +
                    "quantity DOUBLE, " +
                    "unit_cost DOUBLE, " +
                    "total_amount DOUBLE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            // 5. جدول مرتجعات سيارات التوزيع (Van Return)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS van_return_notes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "return_code VARCHAR(50), " +
                    "finished_goods_account VARCHAR(20), " +
                    "van_account VARCHAR(20), " +
                    "quantity DOUBLE, " +
                    "unit_cost DOUBLE, " +
                    "total_amount DOUBLE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            // 6. جدول فواتير المبيعات (Sales Invoice)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS sales_invoices (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "invoice_code VARCHAR(50), " +
                    "customer_account VARCHAR(20), " +
                    "sales_account VARCHAR(20), " +
                    "cogs_account VARCHAR(20), " +
                    "finished_goods_account VARCHAR(20), " +
                    "total_sales DOUBLE, " +
                    "total_cogs DOUBLE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
              // تنظيف الفواتير والسندات المكررة وتطبيق قيد الفرادة (UNIQUE)
            try {
                // حذف السجلات المكررة في المبيعات والإبقاء على سجل واحد
                stmt.executeUpdate("DELETE t1 FROM sales_invoices t1 INNER JOIN sales_invoices t2 WHERE t1.created_at > t2.created_at AND t1.invoice_code = t2.invoice_code");
                addUniqueIndexIfMissing(conn, stmt, "sales_invoices", "uq_invoice_code", "invoice_code");
                // 2. حذف السجلات المكررة في الخزينة والبنك والإبقاء على سجل واحد فريد
                stmt.executeUpdate("DELETE t1 FROM treasury_vouchers t1 INNER JOIN treasury_vouchers t2 WHERE t1.id > t2.id AND t1.voucher_number = t2.voucher_number");
                addUniqueIndexIfMissing(conn, stmt, "treasury_vouchers", "uq_voucher_number", "voucher_number");
                // 3. حذف السجلات المكررة في تنبيهات المخزون
            stmt.executeUpdate("DELETE t1 FROM stock_alerts t1 INNER JOIN stock_alerts t2 WHERE t1.id > t2.id AND t1.item_code = t2.item_code");
            addUniqueIndexIfMissing(conn, stmt, "stock_alerts", "uq_item_code", "item_code");
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // 7. جدول مرتجعات المبيعات (Sales Return)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS sales_return_notes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "return_code VARCHAR(50), " +
                    "customer_account VARCHAR(20), " +
                    "sales_return_account VARCHAR(20), " +
                    "finished_goods_account VARCHAR(20), " +
                    "cogs_account VARCHAR(20), " +
                    "total_amount DOUBLE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            // 8. جدول مرتجعات المشتريات (Purchase Return)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS purchase_return_notes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "return_code VARCHAR(50), " +
                    "supplier_account VARCHAR(20), " +
                    "raw_material_account VARCHAR(20), " +
                    "total_amount DOUBLE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

                    stmt.executeUpdate("CREATE TABLE IF NOT EXISTS purchase_invoices (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, invoice_code VARCHAR(50) UNIQUE, " +
                        "inventory_account VARCHAR(20), supplier_account VARCHAR(20), input_tax_account VARCHAR(20), " +
                        "amount DECIMAL(18,4), tax_amount DECIMAL(18,4), total_amount DECIMAL(18,4), " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

                    stmt.executeUpdate("CREATE TABLE IF NOT EXISTS purchase_return_invoices (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, return_code VARCHAR(50) UNIQUE, " +
                        "inventory_account VARCHAR(20), supplier_account VARCHAR(20), input_tax_account VARCHAR(20), " +
                        "amount DECIMAL(18,4), tax_amount DECIMAL(18,4), total_amount DECIMAL(18,4), " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            // 9. جدول إقفال المصاريف غير المباشرة (Overhead Closing)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS overhead_closings (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "closing_code VARCHAR(50), " +
                    "actual_account VARCHAR(20), " +
                    "applied_account VARCHAR(20), " +
                    "cogs_account VARCHAR(20), " +
                    "actual_amount DOUBLE, " +
                    "applied_amount DOUBLE, " +
                    "month_year VARCHAR(20), " +
                    "is_posted TINYINT(1) NOT NULL DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
            try { stmt.executeUpdate("ALTER TABLE overhead_closings ADD COLUMN IF NOT EXISTS is_posted TINYINT(1) NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}

            // 10. جدول سندات الخزينة والبنك (Treasury Vouchers)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS treasury_vouchers (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "voucher_number VARCHAR(50), " +
                    "voucher_date DATE, " +
                    "voucher_type VARCHAR(20), " +
                    "account_code VARCHAR(20), " +
                    "amount DOUBLE, " +
                    "reference_name VARCHAR(255), " +
                    "narration TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            // ترقية جدول الخزينة القديم إن كان منشأً بإصدار voucher_code/description
            addTreasuryColumn(stmt, "voucher_number VARCHAR(50)");
            addTreasuryColumn(stmt, "voucher_date DATE");
            addTreasuryColumn(stmt, "reference_name VARCHAR(255)");
            addTreasuryColumn(stmt, "narration TEXT");
            addTableColumn(stmt, "van_transfer_notes", "quantity DOUBLE");
            addTableColumn(stmt, "van_transfer_notes", "unit_cost DOUBLE");
            addTableColumn(stmt, "van_return_notes", "quantity DOUBLE");
            addTableColumn(stmt, "van_return_notes", "unit_cost DOUBLE");
            addTableColumn(stmt, "inventory_items", "conversion_factor DECIMAL(15,4) NOT NULL DEFAULT 1");
            addTableColumn(stmt, "inventory_items", "min_stock_level DECIMAL(15,4) NOT NULL DEFAULT 0");
            addTableColumn(stmt, "inventory_items", "expiry_date DATE NULL");
            addTableColumn(stmt, "inventory_items", "batch_no VARCHAR(50) NULL");
            try {
                stmt.executeUpdate("UPDATE treasury_vouchers SET voucher_number = voucher_code WHERE voucher_number IS NULL");
                stmt.executeUpdate("UPDATE treasury_vouchers SET reference_name = description WHERE reference_name IS NULL");
                stmt.executeUpdate("UPDATE treasury_vouchers SET narration = description WHERE narration IS NULL");
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // 11. جدول التنبيهات المخزنية (Stock Alerts)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS stock_alerts (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "item_code VARCHAR(50), " +
                    "item_name VARCHAR(100), " +
                    "current_stock DOUBLE, " +
                    "min_stock DOUBLE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            // 3. ترقية الترميز للجداول القائمة بالفعل إلى utf8mb4 لضمان قبول النصوص العربية
            stmt.executeUpdate("ALTER TABLE goods_receipt_notes CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.executeUpdate("ALTER TABLE material_issue_notes CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.executeUpdate("ALTER TABLE finished_goods_notes CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.executeUpdate("ALTER TABLE van_transfer_notes CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.executeUpdate("ALTER TABLE van_return_notes CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.executeUpdate("ALTER TABLE sales_invoices CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.executeUpdate("ALTER TABLE sales_return_notes CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.executeUpdate("ALTER TABLE purchase_return_notes CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.executeUpdate("ALTER TABLE overhead_closings CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.executeUpdate("ALTER TABLE treasury_vouchers CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.executeUpdate("ALTER TABLE stock_alerts CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

            System.out.println("=== تم إنشاء وفحص وتحديث ترميز جميع جداول قاعدة البيانات للغة العربية (مُعطّل - M2) ===");
        } catch (SQLException e) {
            System.err.println("خطأ أثناء تهيئة جداول قاعدة البيانات (مُعطّل - M2): " + e.getMessage());
        }
        }
    }

    // =========================================================================
    // 1. دورة المشتريات: حفظ مذكرة استلام الخامات (GRN)
    // ===========================================203==============================
    public static void insertGoodsReceiptNote(String grnCode, String supplierAcc, String rawMaterialAcc, double totalAmount) {
        validateSubAccount(supplierAcc);
        validateSubAccount(rawMaterialAcc);

        String sql = "INSERT INTO goods_receipt_notes (grn_code, supplier_account, raw_material_account, total_amount) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, grnCode);
            pstmt.setString(2, supplierAcc);
            pstmt.setString(3, rawMaterialAcc);
            pstmt.setDouble(4, totalAmount);
            pstmt.executeUpdate();
            System.out.println("=== [SQL Success] تم حفظ إذن استلام المواد الخام (GRN) بنجاح ===");
        } catch (SQLException e) {
            System.err.println("خطأ SQL في حفظ إذن استلام الخامات: " + e.getMessage());
        }
    }

    // =========================================================================
    // 2. دورة الإنتاج: حفظ إذن صرف المواد للإنتاج (Material Issue)
    // =========================================================================
    public static void insertMaterialIssueNote(String issueCode, String wipAcc, String rawMaterialAcc, double totalAmount) {
        validateSubAccount(wipAcc);
        validateSubAccount(rawMaterialAcc);

        String sql = "INSERT INTO material_issue_notes (issue_code, wip_account, raw_material_account, total_amount) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, issueCode);
            pstmt.setString(2, wipAcc);
            pstmt.setString(3, rawMaterialAcc);
            pstmt.setDouble(4, totalAmount);
            pstmt.executeUpdate();
            System.out.println("=== [SQL Success] تم حفظ إذن صرف الخامات للإنتاج بنجاح ===");
        } catch (SQLException e) {
            System.err.println("خطأ SQL في حفظ إذن صرف الخامات: " + e.getMessage());
        }
    }

    // =========================================================================
    // 3. دورة الإنتاج: حفظ إذن استلام المنتج التام (Finished Goods Receipt)
    // =========================================================================
    public static void insertFinishedGoodsNote(String noteCode, String finishedGoodsAcc, String wipAcc, double totalAmount) {
        validateSubAccount(finishedGoodsAcc);
        validateSubAccount(wipAcc);

        String sql = "INSERT INTO finished_goods_notes (note_code, finished_goods_account, wip_account, total_amount) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, noteCode);
            pstmt.setString(2, finishedGoodsAcc);
            pstmt.setString(3, wipAcc);
            pstmt.setDouble(4, totalAmount);
            pstmt.executeUpdate();
            System.out.println("=== [SQL Success] تم حفظ إذن إضافة المنتج التام بنجاح ===");
        } catch (SQLException e) {
            System.err.println("خطأ SQL في حفظ إذن المنتج التام: " + e.getMessage());
        }
    }

    // =========================================================================
    // 4. دورة التوزيع: حفظ تحويل البضاعة لسيارات التوزيع (Van Transfer)
    // =========================================================================
    public static void insertVanTransferNote(String transferCode, String vanAcc, String finishedGoodsAcc, double totalAmount) {
        validateSubAccount(vanAcc);
        validateSubAccount(finishedGoodsAcc);

        String sql = "INSERT INTO van_transfer_notes (transfer_code, van_account, finished_goods_account, quantity, unit_cost, total_amount) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transferCode);
            pstmt.setString(2, vanAcc);
            pstmt.setString(3, finishedGoodsAcc);
            pstmt.setDouble(4, 0.0);
            pstmt.setDouble(5, 0.0);
            pstmt.setDouble(6, totalAmount);
            pstmt.executeUpdate();
            System.out.println("=== [SQL Success] تم حفظ إذن تحويل سيارة التوزيع بنجاح ===");
        } catch (SQLException e) {
            System.err.println("خطأ SQL في حفظ تحويل سيارة التوزيع: " + e.getMessage());
        }
    }

    // =========================================================================
    // 5. دورة التوزيع: حفظ مرتجع البضاعة من سيارات التوزيع (Van Return)
    // =========================================================================
    public static void insertVanReturnNote(String returnCode, String finishedGoodsAcc, String vanAcc, double totalAmount) {
        validateSubAccount(finishedGoodsAcc);
        validateSubAccount(vanAcc);

        String sql = "INSERT INTO van_return_notes (return_code, finished_goods_account, van_account, quantity, unit_cost, total_amount) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, returnCode);
            pstmt.setString(2, finishedGoodsAcc);
            pstmt.setString(3, vanAcc);
            pstmt.setDouble(4, 0.0);
            pstmt.setDouble(5, 0.0);
            pstmt.setDouble(6, totalAmount);
            pstmt.executeUpdate();
            System.out.println("=== [SQL Success] تم حفظ إذن مرتجع سيارة التوزيع بنجاح ===");
        } catch (SQLException e) {
            System.err.println("خطأ SQL في حفظ مرتجع سيارة التوزيع: " + e.getMessage());
        }
    }

    public static void insertVanTransferNote(String transferCode, String vanAcc, String finishedGoodsAcc,
                                              double quantity, double unitCost) {
        validateSubAccount(vanAcc);
        validateSubAccount(finishedGoodsAcc);
        String sql = "INSERT INTO van_transfer_notes (transfer_code, van_account, finished_goods_account, quantity, unit_cost, total_amount) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transferCode);
            pstmt.setString(2, vanAcc);
            pstmt.setString(3, finishedGoodsAcc);
            pstmt.setDouble(4, quantity);
            pstmt.setDouble(5, unitCost);
            pstmt.setDouble(6, quantity * unitCost);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("خطأ SQL في حفظ تحويل سيارة التوزيع: " + e.getMessage(), e);
        }
    }

    public static void insertVanReturnNote(String returnCode, String finishedGoodsAcc, String vanAcc,
                                            double quantity, double unitCost) {
        validateSubAccount(finishedGoodsAcc);
        validateSubAccount(vanAcc);
        String sql = "INSERT INTO van_return_notes (return_code, finished_goods_account, van_account, quantity, unit_cost, total_amount) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, returnCode);
            pstmt.setString(2, finishedGoodsAcc);
            pstmt.setString(3, vanAcc);
            pstmt.setDouble(4, quantity);
            pstmt.setDouble(5, unitCost);
            pstmt.setDouble(6, quantity * unitCost);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("خطأ SQL في حفظ مرتجع سيارة التوزيع: " + e.getMessage(), e);
        }
    }

  // =========================================================================
    // 6. دورة المبيعات: حفظ فاتورة المبيعات وتكلفة المبيعات (منع التكرار)
    // =========================================================================
    public static void insertSalesInvoice(String invoiceCode, String customerAcc, String salesAcc, String cogsAcc, String finishedGoodsAcc, double totalSales, double totalCogs) {
        validateSubAccount(customerAcc);
        validateSubAccount(salesAcc);
        validateSubAccount(cogsAcc);
        validateSubAccount(finishedGoodsAcc);

        // فحص ما إذا كانت الفاتورة مسجلة مسبقاً لمنع التكرار
        String checkSql = "SELECT COUNT(*) FROM sales_invoices WHERE invoice_code = ?";
        String insertSql = "INSERT INTO sales_invoices (invoice_code, invoice_date, customer_account, sales_revenue_account, finished_goods_account, cogs_account, subtotal_amount, tax_amount, total_invoice_amount, inventory_cost_amount) VALUES (?, CURRENT_DATE, ?, ?, ?, ?, ?, 0, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setString(1, invoiceCode);
            java.sql.ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                // الفاتورة مسجلة من قبل، يتم يتجاهل الإعادة
                return;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, invoiceCode);
                pstmt.setString(2, customerAcc);
                pstmt.setString(3, salesAcc);
                pstmt.setString(4, cogsAcc);
                pstmt.setString(5, finishedGoodsAcc);
                pstmt.setDouble(6, totalSales);
                pstmt.setDouble(7, totalSales);
                pstmt.setDouble(8, totalCogs);
                pstmt.executeUpdate();
                System.out.println("=== [SQL Success] تم حفظ فاتورة المبيعات وتكلفة البضاعة المباعة بنجاح ===");
            }
        } catch (SQLException e) {
            System.err.println("خطأ SQL في حفظ فاتورة المبيعات: " + e.getMessage());
        }
    }

    // =========================================================================
    // 7. دورة المبيعات: حفظ إذن وفاتورة مرتجع المبيعات (Sales Return)
    // =========================================================================
    public static void insertSalesReturnNote(String returnCode, String customerAcc, String salesReturnAcc, String finishedGoodsAcc, String cogsAcc, double totalAmount) {
        validateSubAccount(customerAcc);
        validateSubAccount(salesReturnAcc);
        validateSubAccount(finishedGoodsAcc);
        validateSubAccount(cogsAcc);

        String sql = "INSERT INTO sales_return_notes (return_code, customer_account, sales_return_account, finished_goods_account, cogs_account, total_amount) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, returnCode);
            pstmt.setString(2, customerAcc);
            pstmt.setString(3, salesReturnAcc);
            pstmt.setString(4, finishedGoodsAcc);
            pstmt.setString(5, cogsAcc);
            pstmt.setDouble(6, totalAmount);
            pstmt.executeUpdate();
            System.out.println("=== [SQL Success] تم حفظ إذن فاتورة مرتجع المبيعات بنجاح ===");
        } catch (SQLException e) {
            System.err.println("خطأ SQL في حفظ مرتجع المبيعات: " + e.getMessage());
        }
    }

    // =========================================================================
    // 8. دورة المشتريات: حفظ إذن وفاتورة مرتجع المشتريات (Purchase Return)
    // =========================================================================
    public static void insertPurchaseReturnNote(String returnCode, String supplierAcc, String rawMaterialAcc, double totalAmount) {
        validateSubAccount(supplierAcc);
        validateSubAccount(rawMaterialAcc);

        String sql = "INSERT INTO purchase_return_notes (return_code, supplier_account, raw_material_account, total_amount) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, returnCode);
            pstmt.setString(2, supplierAcc);
            pstmt.setString(3, rawMaterialAcc);
            pstmt.setDouble(4, totalAmount);
            pstmt.executeUpdate();
            System.out.println("=== [SQL Success] تم حفظ إذن مرتجع المشتريات بنجاح ===");
        } catch (SQLException e) {
            System.err.println("خطأ SQL في حفظ مرتجع المشتريات: " + e.getMessage());
        }
    }

    // =========================================================================
    // 9. التكاليف والشهرية: حفظ إقفال المصاريف غير المباشرة (Overhead Closing)
    // =========================================================================
    public static void insertOverheadClosing(String closingCode, String actualAcc, String appliedAcc, String cogsAcc, double actualAmount, double appliedAmount, String monthYear) {
        validateSubAccount(actualAcc);
        validateSubAccount(appliedAcc);
        validateSubAccount(cogsAcc);

        String sql = "INSERT INTO overhead_closings (closing_code, actual_account, applied_account, cogs_account, actual_amount, applied_amount, month_year) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, closingCode);
            pstmt.setString(2, actualAcc);
            pstmt.setString(3, appliedAcc);
            pstmt.setString(4, cogsAcc);
            pstmt.setDouble(5, actualAmount);
            pstmt.setDouble(6, appliedAmount);
            pstmt.setString(7, monthYear);
            pstmt.executeUpdate();
            System.out.println("=== [SQL Success] تم حفظ إقفال المصاريف غير المباشرة والانحرافات بنجاح ===");
        } catch (SQLException e) {
            System.err.println("خطأ SQL في حفظ إقفال المصاريف: " + e.getMessage());
        }
    }

    // =========================================================================
    // 10. دورة الخزينة والبنك: حفظ سندات القبض والصرف (Treasury Vouchers)
    // =========================================================================
    public static void insertTreasuryVoucher(String voucherCode, String accountCode, double amount, String type, String description) {
        validateSubAccount(accountCode);

        String sql = "INSERT INTO treasury_vouchers (voucher_number, voucher_date, account_code, amount, voucher_type, reference_name, narration) VALUES (?, CURRENT_DATE, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, voucherCode);
            pstmt.setString(2, accountCode);
            pstmt.setDouble(3, amount);
            pstmt.setString(4, type);
            pstmt.setString(5, description);
            pstmt.setString(6, description);
            pstmt.executeUpdate();
            System.out.println("=== [SQL Success] تم حفظ سند الخزينة/البنك بنجاح ===");
        } catch (SQLException e) {
            System.err.println("خطأ SQL في حفظ سند الخزينة: " + e.getMessage());
        }
    }

    // =========================================================================
    // 11. إدارة المخاطر والمخزون: حفظ التنبيهات المخزنية (Stock Alerts)
    // =========================================================================
    public static void insertStockAlert(String itemCode, String itemName, double currentStock, double minStock) {
        validateSubAccount(itemCode);

        String sql = "INSERT INTO stock_alerts (item_code, item_name, current_stock, min_stock) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemCode);
            pstmt.setString(2, itemName);
            pstmt.setDouble(3, currentStock);
            pstmt.setDouble(4, minStock);
            pstmt.executeUpdate();
            System.out.println("=== [SQL Success] تم تسجيل التنبيه المخزني بنجاح ===");
        } catch (SQLException e) {
            System.err.println("خطأ SQL في حفظ التنبيه المخزني: " + e.getMessage());
        }
    }
    // =========================================================================
    // 12. تقارير الاستعلام: دالة قراءة وطباعة تقرير رصيد المخزون والتنبيهات
    // =========================================================================
    public static void printStockReport() {
        String sql = "SELECT DISTINCT item_code, item_name, current_stock, min_stock FROM stock_alerts";
        System.out.println("\n========= تقرير رصيد المخزون وحالة التنبيهات (SQL) =========");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String code = rs.getString("item_code");
                String name = rs.getString("item_name");
                double current = rs.getDouble("current_stock");
                double min = rs.getDouble("min_stock");
                String status = (current <= min) ? "تجاوز الحد الأدنى" : "طبيعي";

                System.out.println("رقم الصنف: " + code + " | الاسم: " + name + 
                                   " | الرصيد: " + current + " | الحد الأدنى: " + min + 
                                   " | الحالة: " + status);
            }
            System.out.println("============================================================\n");
        } catch (SQLException e) {
            System.err.println("خطأ SQL في استخراج تقرير المخزون: " + e.getMessage());
        }
    }
    // =========================================================================
    // 13. تقارير الاستعلام: دالة قراءة وطباعة تقرير سندات الخزينة والبنك
    // =========================================================================
    public static void printTreasuryReport() {
        String sql = "SELECT voucher_number, account_code, amount, voucher_type, narration FROM treasury_vouchers";
        System.out.println("\n========= تقرير حركة الخزينة والبنك (SQL) =========");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String code = rs.getString("voucher_number");
                String acc = rs.getString("account_code");
                double amount = rs.getDouble("amount");
                String type = rs.getString("voucher_type");
                String desc = rs.getString("narration");

                System.out.println("رقم السند: " + code + " | الحساب: " + acc + 
                                   " | المبلغ: " + amount + " | النوع: " + type + 
                                   " | البيان: " + desc);
            }
            System.out.println("===================================================\n");
        } catch (SQLException e) {
            System.err.println("خطأ SQL في استخراج تقرير الخزينة: " + e.getMessage());
        }
    }
    // =========================================================================
    // 14. تقارير الاستعلام: دالة قراءة وطباعة تقرير فواتير المبيعات وتكلفة المبيعات
    // =========================================================================
    public static void printSalesReport() {
        String sql = "SELECT invoice_code, customer_account, total_invoice_amount, inventory_cost_amount FROM sales_invoices";
        System.out.println("\n========= تقرير فواتير المبيعات وتكلفة المبيعات (SQL) =========");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String code = rs.getString("invoice_code");
                String cust = rs.getString("customer_account");
                double sales = rs.getDouble("total_invoice_amount");
                double cogs = rs.getDouble("inventory_cost_amount");

                System.out.println("رقم الفاتورة: " + code + " | العميل: " + cust + 
                                   " | إجمالي المبيعات: " + sales + " | التكلفة (COGS): " + cogs);
            }
            System.out.println("=================================================================\n");
        } catch (SQLException e) {
            System.err.println("خطأ SQL في استخراج تقرير المبيعات: " + e.getMessage());
        }
    }

    private static void addTreasuryColumn(Statement stmt, String definition) {
        try {
            stmt.executeUpdate("ALTER TABLE treasury_vouchers ADD COLUMN " + definition);
        } catch (SQLException ignored) {}
    }

    private static void addTableColumn(Statement stmt, String tableName, String definition) {
        try {
            stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + definition);
        } catch (SQLException ignored) {}
    }

    // =========================================================================
    // تهيئة جداول الموردين والعملاء والمفوضين والمرفقات
    // =========================================================================
    public static void initializeParties() {
        // (M2) لا DDL هنا بعد الآن؛ الجداول business_parties/party_delegates/document_attachments
        // تُدار من schema.sql. هذا فقط فحص اتصال.
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1");
            System.out.println("=== تم التأكد من الاتصال لجداول الموردين والعملاء (بدون DDL) ===");
        } catch (SQLException e) {
            System.err.println("فشل فحص اتصال تهيئة الموردين والعملاء: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "فشل الاتصال بقاعدة البيانات: " + e.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (false) {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS business_parties ("
                    + "code VARCHAR(20) PRIMARY KEY, ar_name VARCHAR(255) NOT NULL, "
                    + "en_name VARCHAR(255), party_type ENUM('supplier','customer') NOT NULL, "
                    + "status ENUM('active','suspended') NOT NULL DEFAULT 'active', "
                    + "owner_name VARCHAR(255), parent_account_code VARCHAR(20), "
                    + "sub_account_code VARCHAR(20), credit_limit DECIMAL(18,2) DEFAULT 0, "
                    + "credit_period_days INT DEFAULT 0, currency_code VARCHAR(10) DEFAULT 'YER', "
                    + "opening_balance DECIMAL(18,4) DEFAULT 0, balance_type ENUM('debit','credit') DEFAULT 'debit', "
                    + "vat_number VARCHAR(20) UNIQUE, cr_number VARCHAR(50) UNIQUE, "
                    + "cr_image_path VARCHAR(500), phone VARCHAR(20), mobile VARCHAR(20), "
                    + "email VARCHAR(100), address TEXT, contact_person VARCHAR(255), "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) "
                    + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS party_delegates ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, party_code VARCHAR(20) NOT NULL, "
                    + "delegate_name VARCHAR(255) NOT NULL, job_title VARCHAR(255), "
                    + "authorization_doc_path VARCHAR(500), "
                    + "FOREIGN KEY (party_code) REFERENCES business_parties(code) ON DELETE CASCADE) "
                    + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS document_attachments ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, party_code VARCHAR(20) NOT NULL, "
                    + "doc_type VARCHAR(50), file_path VARCHAR(500), description TEXT, "
                    + "uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (party_code) REFERENCES business_parties(code) ON DELETE CASCADE) "
                    + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            stmt.executeUpdate("ALTER TABLE business_parties ADD COLUMN IF NOT EXISTS sub_account_code VARCHAR(20) AFTER parent_account_code");
            stmt.executeUpdate("ALTER TABLE business_parties ADD COLUMN IF NOT EXISTS vat_number VARCHAR(20) AFTER balance_type");
            stmt.executeUpdate("ALTER TABLE business_parties ADD COLUMN IF NOT EXISTS cr_number VARCHAR(50) AFTER vat_number");
            stmt.executeUpdate("ALTER TABLE business_parties ADD COLUMN IF NOT EXISTS cr_image_path VARCHAR(500) AFTER cr_number");
            stmt.executeUpdate("ALTER TABLE business_parties ADD COLUMN IF NOT EXISTS phone VARCHAR(20) AFTER cr_image_path");
            stmt.executeUpdate("ALTER TABLE business_parties ADD COLUMN IF NOT EXISTS mobile VARCHAR(20) AFTER phone");
            stmt.executeUpdate("ALTER TABLE business_parties ADD COLUMN IF NOT EXISTS email VARCHAR(100) AFTER mobile");
            stmt.executeUpdate("ALTER TABLE business_parties ADD COLUMN IF NOT EXISTS address TEXT AFTER email");
            stmt.executeUpdate("ALTER TABLE business_parties ADD COLUMN IF NOT EXISTS contact_person VARCHAR(255) AFTER address");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "فشل تهيئة جداول الموردين والعملاء (مُعطّل - M2): " + e.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
        }
    }

    // =========================================================================
    // التحقق من تكرار الكود أو الاسم أو الرقم الضريبي
    // =========================================================================
    public static boolean isPartyCodeExists(String code) {
        String sql = "SELECT 1 FROM business_parties WHERE code = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { return false; }
    }

    public static boolean isPartyArNameExists(String arName, String excludeCode) {
        String sql = "SELECT 1 FROM business_parties WHERE ar_name = ?" + (excludeCode != null ? " AND code != ?" : "");
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, arName);
            if (excludeCode != null) ps.setString(2, excludeCode);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { return false; }
    }

    public static boolean isVatNumberExists(String vatNumber, String excludeCode) {
        String sql = "SELECT 1 FROM business_parties WHERE vat_number = ?" + (excludeCode != null ? " AND code != ?" : "");
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vatNumber);
            if (excludeCode != null) ps.setString(2, excludeCode);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { return false; }
    }

    // =========================================================================
    // توليد كود حساب فرعي تلقائي تحت الحساب الأب
    // =========================================================================
    public static String generatePartySubAccountCode(String parentCode) {
        String sql = "SELECT MAX(account_code) FROM chart_of_accounts WHERE account_code LIKE ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, parentCode + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getObject(1) != null) {
                    String maxCode = rs.getString(1);
                    String suffix = maxCode.substring(parentCode.length());
                    int nextNum = Integer.parseInt(suffix.isEmpty() ? "0" : suffix) + 1;
                    return parentCode + String.format("%02d", nextNum);
                }
            }
} catch (SQLException ignored) {
            return parentCode + "01";
        }
        return parentCode + "01";
    }

    // =========================================================================
    // حفظ بيانات الجهة + الحساب الفرعي + المفوضين + القيد الافتتاحي في معاملة واحدة
    // =========================================================================
    public static boolean savePartyWithAccount(Connection conn, String code, String arName, String enName,
            String partyType, String ownerName, String parentAccountCode, String subAccountCode,
            double creditLimit, int creditPeriodDays, String currencyCode,
            double openingBalance, String balanceType, String vatNumber, String crNumber,
            String crImagePath, String phone, String mobile, String email,
            String address, String contactPerson, java.util.List<String[]> delegates) throws SQLException {
        conn.setAutoCommit(false);
        try {
            String sqlParty = "INSERT INTO business_parties (code, ar_name, en_name, party_type, owner_name, "
                    + "parent_account_code, sub_account_code, credit_limit, credit_period_days, currency_code, "
                    + "opening_balance, balance_type, vat_number, cr_number, cr_image_path, phone, mobile, email, address, contact_person) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlParty)) {
                ps.setString(1, code); ps.setString(2, arName); ps.setString(3, enName);
                ps.setString(4, partyType); ps.setString(5, ownerName);
                ps.setString(6, parentAccountCode); ps.setString(7, subAccountCode);
                ps.setDouble(8, creditLimit); ps.setInt(9, creditPeriodDays); ps.setString(10, currencyCode);
                ps.setDouble(11, openingBalance); ps.setString(12, balanceType);
                ps.setString(13, emptyToNull(vatNumber)); ps.setString(14, emptyToNull(crNumber));
                ps.setString(15, emptyToNull(crImagePath));
                ps.setString(16, phone); ps.setString(17, mobile); ps.setString(18, email);
                ps.setString(19, address); ps.setString(20, contactPerson);
                ps.executeUpdate();
            }

            String sqlInsertAccount = "INSERT INTO chart_of_accounts (account_code, account_name, account_type, parent_code, account_level, is_sub_account, current_balance, currency) "
                    + "VALUES (?, ?, ?, ?, 6, 1, ?, ?)";
            String accountType = "supplier".equals(partyType) ? "LIABILITY" : "ASSET";
            String accountName = arName + " (" + partyType + ")";
            String accountExistsSql = "SELECT 1 FROM chart_of_accounts WHERE account_code = ?";
            try (PreparedStatement exists = conn.prepareStatement(accountExistsSql)) {
                exists.setString(1, subAccountCode);
                try (ResultSet rs = exists.executeQuery()) {
                    if (!rs.next()) {
                        try (PreparedStatement ps = conn.prepareStatement(sqlInsertAccount)) {
                            ps.setString(1, subAccountCode); ps.setString(2, accountName);
                            ps.setString(3, accountType); ps.setString(4, parentAccountCode);
                            ps.setDouble(5, openingBalance); ps.setString(6, currencyCode);
                            ps.executeUpdate();
                        }
                    }
                }
            }

            if (delegates != null) {
                String sqlDelegate = "INSERT INTO party_delegates (party_code, delegate_name, job_title, authorization_doc_path) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlDelegate)) {
                    for (String[] d : delegates) {
                        ps.setString(1, code); ps.setString(2, d[0]); ps.setString(3, d[1]); ps.setString(4, d[2]);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            if (openingBalance > 0) {
                String entryNo = "JV-OPEN-" + code;
                String narration = "قيد افتتاحي " + arName;
                String entrySql = "INSERT INTO journal_entries (entry_number, entry_date, reference_doc, source_module, narration, total_debit, total_credit, posted_by) "
                        + "VALUES (?, CURDATE(), ?, ?, ?, ?, ?, 'النظام الآلي')";
                String lineSql = "INSERT INTO journal_entry_lines (entry_id, account_code, line_narration, debit_amount, credit_amount) VALUES (?, ?, ?, ?, ?)";

                double amount = openingBalance;
                String OPENING_BALANCE_ACC = "3101";
                String debitAcc = "debit".equals(balanceType) ? subAccountCode : OPENING_BALANCE_ACC;
                String creditAcc = "debit".equals(balanceType) ? OPENING_BALANCE_ACC : subAccountCode;

                try (PreparedStatement psEntry = conn.prepareStatement(entrySql, Statement.RETURN_GENERATED_KEYS)) {
                    psEntry.setString(1, entryNo); psEntry.setString(2, entryNo);
                    psEntry.setString(3, "OPENING"); psEntry.setString(4, narration);
                    psEntry.setDouble(5, "debit".equals(balanceType) ? amount : 0);
                    psEntry.setDouble(6, "credit".equals(balanceType) ? amount : 0);
                    psEntry.executeUpdate();
                    try (ResultSet rs = psEntry.getGeneratedKeys()) {
                        if (rs.next()) {
                            long entryId = rs.getLong(1);
                            try (PreparedStatement psLine = conn.prepareStatement(lineSql)) {
                                psLine.setLong(1, entryId); psLine.setString(2, debitAcc);
                                psLine.setString(3, "رصيد افتتاحي - " + arName);
                                psLine.setDouble(4, "debit".equals(balanceType) ? amount : 0);
                                psLine.setDouble(5, "credit".equals(balanceType) ? amount : 0);
                                psLine.executeUpdate();
                                try (PreparedStatement psLine2 = conn.prepareStatement(lineSql)) {
                                    psLine2.setLong(1, entryId); psLine2.setString(2, creditAcc);
                                    psLine2.setString(3, "رصيد افتتاحي - " + arName);
                                    psLine2.setDouble(4, "credit".equals(balanceType) ? amount : 0);
                                    psLine2.setDouble(5, "debit".equals(balanceType) ? amount : 0);
                                    psLine2.executeUpdate();
                                }
                            }
                        }
                    }
                }
                try (PreparedStatement psUpdate = conn.prepareStatement("UPDATE chart_of_accounts SET current_balance = current_balance + ? WHERE account_code = ?")) {
                    psUpdate.setDouble(1, amount); psUpdate.setString(2, subAccountCode);
                    psUpdate.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // =========================================================================
    // تحديث بيانات الجهة + الحساب الفرعي + المفوضين + القيد الافتتاحي في معاملة واحدة
    // =========================================================================
    public static boolean updatePartyWithAccount(Connection conn, String code, String arName, String enName,
            String partyType, String ownerName, String parentAccountCode, String subAccountCode,
            double creditLimit, int creditPeriodDays, String currencyCode,
            double openingBalance, String balanceType, String vatNumber, String crNumber,
            String crImagePath, String phone, String mobile, String email,
            String address, String contactPerson, java.util.List<String[]> delegates) throws SQLException {
        conn.setAutoCommit(false);
        try {
            String sqlParty = "UPDATE business_parties SET ar_name = ?, en_name = ?, owner_name = ?, "
                    + "parent_account_code = ?, sub_account_code = ?, credit_limit = ?, credit_period_days = ?, "
                    + "currency_code = ?, opening_balance = ?, balance_type = ?, vat_number = ?, cr_number = ?, "
                    + "cr_image_path = ?, phone = ?, mobile = ?, email = ?, address = ?, contact_person = ? "
                    + "WHERE code = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlParty)) {
                ps.setString(1, arName); ps.setString(2, enName);
                ps.setString(3, ownerName);
                ps.setString(4, parentAccountCode); ps.setString(5, subAccountCode);
                ps.setDouble(6, creditLimit); ps.setInt(7, creditPeriodDays); ps.setString(8, currencyCode);
                ps.setDouble(9, openingBalance); ps.setString(10, balanceType);
                ps.setString(11, emptyToNull(vatNumber)); ps.setString(12, emptyToNull(crNumber));
                ps.setString(13, emptyToNull(crImagePath));
                ps.setString(14, phone); ps.setString(15, mobile); ps.setString(16, email);
                ps.setString(17, address); ps.setString(18, contactPerson);
                ps.setString(19, code);
                ps.executeUpdate();
            }

            String accountType = "supplier".equals(partyType) ? "LIABILITY" : "ASSET";
            String accountName = arName + " (" + partyType + ")";
            String sqlUpdateAccount = "UPDATE chart_of_accounts SET account_name = ?, account_type = ?, parent_code = ?, current_balance = ? "
                    + "WHERE account_code = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateAccount)) {
                ps.setString(1, accountName); ps.setString(2, accountType);
                ps.setString(3, parentAccountCode); ps.setDouble(4, openingBalance);
                ps.setString(5, subAccountCode);
                ps.executeUpdate();
            }

            String sqlDeleteDelegates = "DELETE FROM party_delegates WHERE party_code = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlDeleteDelegates)) {
                ps.setString(1, code); ps.executeUpdate();
            }
            if (delegates != null && !delegates.isEmpty()) {
                String sqlDelegate = "INSERT INTO party_delegates (party_code, delegate_name, job_title, authorization_doc_path) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlDelegate)) {
                    for (String[] d : delegates) {
                        ps.setString(1, code); ps.setString(2, d[0]); ps.setString(3, d[1]); ps.setString(4, d[2]);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            String sqlDeleteAttachments = "DELETE FROM document_attachments WHERE party_code = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlDeleteAttachments)) {
                ps.setString(1, code); ps.executeUpdate();
            }
            if (crImagePath != null && !crImagePath.isEmpty()) {
                String sqlAttach = "INSERT INTO document_attachments (party_code, doc_type, file_path, description) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlAttach)) {
                    ps.setString(1, code); ps.setString(2, "cr_image"); ps.setString(3, crImagePath);
                    ps.setString(4, "صورة السجل التجاري");
                    ps.executeUpdate();
                }
            }

            if (openingBalance > 0) {
                String entryNo = "JV-OPEN-" + code;
                String narration = "قيد افتتاحي " + arName;
                String checkSql = "SELECT entry_id FROM journal_entries WHERE entry_number = ?";
                long entryId = -1;
                try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                    psCheck.setString(1, entryNo);
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next()) { entryId = rs.getLong(1); }
                    }
                }
                String OPENING_BALANCE_ACC = "3101";
                String debitAcc = "debit".equals(balanceType) ? subAccountCode : OPENING_BALANCE_ACC;
                String creditAcc = "debit".equals(balanceType) ? OPENING_BALANCE_ACC : subAccountCode;

                if (entryId > 0) {
                    String updEntry = "UPDATE journal_entries SET total_debit = ?, total_credit = ?, narration = ? WHERE entry_id = ?";
                    try (PreparedStatement psUpd = conn.prepareStatement(updEntry)) {
                        psUpd.setDouble(1, "debit".equals(balanceType) ? openingBalance : 0);
                        psUpd.setDouble(2, "credit".equals(balanceType) ? openingBalance : 0);
                        psUpd.setString(3, narration);
                        psUpd.setLong(4, entryId);
                        psUpd.executeUpdate();
                    }
                    String delLines = "DELETE FROM journal_entry_lines WHERE entry_id = ?";
                    try (PreparedStatement psDel = conn.prepareStatement(delLines)) {
                        psDel.setLong(1, entryId); psDel.executeUpdate();
                    }
                    String insLine = "INSERT INTO journal_entry_lines (entry_id, account_code, line_narration, debit_amount, credit_amount) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement psL1 = conn.prepareStatement(insLine)) {
                        psL1.setLong(1, entryId); psL1.setString(2, debitAcc);
                        psL1.setString(3, "رصيد افتتاحي - " + arName);
                        psL1.setDouble(4, "debit".equals(balanceType) ? openingBalance : 0);
                        psL1.setDouble(5, "credit".equals(balanceType) ? openingBalance : 0);
                        psL1.executeUpdate();
                    }
                    try (PreparedStatement psL2 = conn.prepareStatement(insLine)) {
                        psL2.setLong(1, entryId); psL2.setString(2, creditAcc);
                        psL2.setString(3, "رصيد افتتاحي - " + arName);
                        psL2.setDouble(4, "credit".equals(balanceType) ? openingBalance : 0);
                        psL2.setDouble(5, "debit".equals(balanceType) ? openingBalance : 0);
                        psL2.executeUpdate();
                    }
                } else {
                    String entrySql = "INSERT INTO journal_entries (entry_number, entry_date, reference_doc, source_module, narration, total_debit, total_credit, posted_by) "
                            + "VALUES (?, CURDATE(), ?, ?, ?, ?, ?, 'النظام الآلي')";
                    String lineSql = "INSERT INTO journal_entry_lines (entry_id, account_code, line_narration, debit_amount, credit_amount) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement psEntry = conn.prepareStatement(entrySql, Statement.RETURN_GENERATED_KEYS)) {
                        psEntry.setString(1, entryNo); psEntry.setString(2, entryNo);
                        psEntry.setString(3, "OPENING"); psEntry.setString(4, narration);
                        psEntry.setDouble(5, "debit".equals(balanceType) ? openingBalance : 0);
                        psEntry.setDouble(6, "credit".equals(balanceType) ? openingBalance : 0);
                        psEntry.executeUpdate();
                        try (ResultSet rs = psEntry.getGeneratedKeys()) {
                            if (rs.next()) {
                                long newEntryId = rs.getLong(1);
                                try (PreparedStatement psL1 = conn.prepareStatement(lineSql)) {
                                    psL1.setLong(1, newEntryId); psL1.setString(2, debitAcc);
                                    psL1.setString(3, "رصيد افتتاحي - " + arName);
                                    psL1.setDouble(4, "debit".equals(balanceType) ? openingBalance : 0);
                                    psL1.setDouble(5, "credit".equals(balanceType) ? openingBalance : 0);
                                    psL1.executeUpdate();
                                }
                                try (PreparedStatement psL2 = conn.prepareStatement(lineSql)) {
                                    psL2.setLong(1, newEntryId); psL2.setString(2, creditAcc);
                                    psL2.setString(3, "رصيد افتتاحي - " + arName);
                                    psL2.setDouble(4, "credit".equals(balanceType) ? openingBalance : 0);
                                    psL2.setDouble(5, "debit".equals(balanceType) ? openingBalance : 0);
                                    psL2.executeUpdate();
                                }
                            }
                        }
                    }
                }
                try (PreparedStatement psUpdAcc = conn.prepareStatement("UPDATE chart_of_accounts SET current_balance = ? WHERE account_code = ?")) {
                    psUpdAcc.setDouble(1, openingBalance); psUpdAcc.setString(2, subAccountCode);
                    psUpdAcc.executeUpdate();
                }
            } else {
                String delEntry = "DELETE FROM journal_entry_lines WHERE entry_id IN (SELECT entry_id FROM journal_entries WHERE entry_number = ?)";
                try (PreparedStatement psDelL = conn.prepareStatement(delEntry)) {
                    psDelL.setString(1, "JV-OPEN-" + code); psDelL.executeUpdate();
                }
                try (PreparedStatement psDelE = conn.prepareStatement("DELETE FROM journal_entries WHERE entry_number = ?")) {
                    psDelE.setString(1, "JV-OPEN-" + code); psDelE.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // =========================================================================
    // قراءة جميع الموردين/العملاء مع الفلترة
    // =========================================================================
    public static ResultSet getPartyList(String partyType, String searchText) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM business_parties WHERE 1=1");
        java.util.ArrayList<String> params = new java.util.ArrayList<>();
        if (partyType != null && !partyType.isEmpty()) { sql.append(" AND party_type = ?"); params.add(partyType); }
        if (searchText != null && !searchText.isEmpty()) {
            sql.append(" AND (ar_name LIKE ? OR code LIKE ? OR vat_number LIKE ?)");
            params.add("%" + searchText + "%"); params.add("%" + searchText + "%"); params.add("%" + searchText + "%");
        }
        sql.append(" ORDER BY created_at DESC");
        Connection conn = getConnection();
        PreparedStatement ps = conn.prepareStatement(sql.toString());
        for (int i = 0; i < params.size(); i++) { ps.setString(i + 1, params.get(i)); }
        return ps.executeQuery();
    }

    public static ResultSet getDelegates(String partyCode) throws SQLException {
        String sql = "SELECT * FROM party_delegates WHERE party_code = ?";
        Connection conn = getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, partyCode);
        return ps.executeQuery();
    }

    public static ResultSet getAttachments(String partyCode) throws SQLException {
        String sql = "SELECT * FROM document_attachments WHERE party_code = ?";
        Connection conn = getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, partyCode);
        return ps.executeQuery();
    }

    // =========================================================================
    // حذف جهة
    // =========================================================================
    public static boolean deleteParty(String code) {
        String subCode = null;
        try (Connection conn = getConnection(); PreparedStatement psRead = conn.prepareStatement("SELECT sub_account_code FROM business_parties WHERE code = ?")) {
            psRead.setString(1, code);
            try (ResultSet rs = psRead.executeQuery()) { if (rs.next()) subCode = rs.getString(1); }
        } catch (SQLException e) { e.printStackTrace(); return false; }
        try (Connection conn = getConnection()) {
            try (PreparedStatement psDelLines = conn.prepareStatement("DELETE FROM journal_entry_lines WHERE entry_id IN (SELECT entry_id FROM journal_entries WHERE entry_number = ?)")) {
                psDelLines.setString(1, "JV-OPEN-" + code); psDelLines.executeUpdate();
            }
            try (PreparedStatement psDelEntry = conn.prepareStatement("DELETE FROM journal_entries WHERE entry_number = ?")) {
                psDelEntry.setString(1, "JV-OPEN-" + code); psDelEntry.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM business_parties WHERE code = ?")) {
                ps.setString(1, code);
                int rows = ps.executeUpdate();
                if (rows > 0 && subCode != null) {
                    try (PreparedStatement psAcc = conn.prepareStatement("DELETE FROM chart_of_accounts WHERE account_code = ?")) {
                        psAcc.setString(1, subCode); psAcc.executeUpdate();
                    }
                }
                conn.commit();
                return rows > 0;
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException rb) {}
                return false;
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
}
