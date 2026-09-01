import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

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

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            
            // 2. تعديل ترميز قاعدة البيانات القائمة لدعم النصوص العربية بالكامل
            stmt.executeUpdate("ALTER DATABASE erp_factory_db CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci");

            // مسح الفهارس القديمة على عمود document_number في جدول حركات المخزون
            try {
                stmt.executeUpdate("ALTER TABLE inventory_movements DROP INDEX document_number");
            } catch (SQLException ignored) {}

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
                    + "item_code VARCHAR(50) PRIMARY KEY, item_name VARCHAR(255) NOT NULL, "
                    + "category VARCHAR(100) DEFAULT 'منتجات تامة', unit VARCHAR(50) NOT NULL DEFAULT 'وحدة', "
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
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            // 2. جدول صرف المواد للإنتاج (Material Issue)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS material_issue_notes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "issue_code VARCHAR(50), " +
                    "wip_account VARCHAR(20), " +
                    "raw_material_account VARCHAR(20), " +
                    "total_amount DOUBLE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            // 3. جدول استلام المنتج التام (Finished Goods Receipt)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS finished_goods_notes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "note_code VARCHAR(50), " +
                    "finished_goods_account VARCHAR(20), " +
                    "wip_account VARCHAR(20), " +
                    "total_amount DOUBLE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

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
                stmt.executeUpdate("DELETE t1 FROM sales_invoices t1 INNER JOIN sales_invoices t2 WHERE t1.id > t2.id AND t1.invoice_code = t2.invoice_code");
                stmt.executeUpdate("ALTER TABLE sales_invoices ADD UNIQUE KEY uq_invoice_code (invoice_code)");
                // 2. حذف السجلات المكررة في الخزينة والبنك والإبقاء على سجل واحد فريد
                stmt.executeUpdate("DELETE t1 FROM treasury_vouchers t1 INNER JOIN treasury_vouchers t2 WHERE t1.id > t2.id AND t1.voucher_number = t2.voucher_number");
                stmt.executeUpdate("ALTER TABLE treasury_vouchers ADD UNIQUE KEY uq_voucher_number (voucher_number)");
                // 3. حذف السجلات المكررة في تنبيهات المخزون
            stmt.executeUpdate("DELETE t1 FROM stock_alerts t1 INNER JOIN stock_alerts t2 WHERE t1.id > t2.id AND t1.item_code = t2.item_code");
            stmt.executeUpdate("ALTER TABLE stock_alerts ADD UNIQUE KEY uq_item_code (item_code)");
            } catch (SQLException e) {
                // في حال كان القيد مُطبقاً مسبقاً يستمر التشغيل الطبيعي
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
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

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
            } catch (SQLException ignored) {}

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

            System.out.println("=== تم إنشاء وفحص وتحديث ترميز جميع جداول قاعدة البيانات للغة العربية بنجاح ===");
        } catch (SQLException e) {
            System.err.println("خطأ أثناء تهيئة جداول قاعدة البيانات: " + e.getMessage());
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
        String insertSql = "INSERT INTO sales_invoices (invoice_code, customer_account, sales_account, cogs_account, finished_goods_account, total_sales, total_cogs) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
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
                pstmt.setDouble(7, totalCogs);
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
        String sql = "SELECT invoice_code, customer_account, sales_account, cogs_account, finished_goods_account, total_sales, total_cogs FROM sales_invoices";
        System.out.println("\n========= تقرير فواتير المبيعات وتكلفة المبيعات (SQL) =========");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String code = rs.getString("invoice_code");
                String cust = rs.getString("customer_account");
                double sales = rs.getDouble("total_sales");
                double cogs = rs.getDouble("total_cogs");

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
        } catch (SQLException ignored) {
            // العمود موجود في الجداول المنشأة بالإصدار الحالي.
        }
    }

    private static void addTableColumn(Statement stmt, String tableName, String definition) {
        try {
            stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + definition);
        } catch (SQLException ignored) {
        }
    }
}
