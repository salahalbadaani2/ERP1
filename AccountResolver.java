import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * حل الحسابات المحاسبية ديناميكياً من قاعدة البيانات بدون Hardcoded Strings
 * جميع الطرق Null-safe وتعيد استثناء واضح إذا لم يوجد حساب
 * مع ذاكرة مؤقتة Thread-safe لتقليل استعلامات DB
 */
public final class AccountResolver {
    private AccountResolver() {}

    private static final Map<String, String> ACCOUNT_CACHE = new ConcurrentHashMap<>();

    // ========== ضريبة المخرجات (المبيعات) ==========
    public static String resolveOutputTaxAccount(Connection conn) throws SQLException {
        String key = "TAX_OUTPUT";
        String cached = ACCOUNT_CACHE.get(key);
        if (cached != null) return cached;
        String sql = "SELECT account_code FROM chart_of_accounts WHERE is_sub_account=1 AND account_type='LIABILITY' AND (account_name LIKE '%ضريبة%' OR account_name LIKE '%ضريبه%') ORDER BY account_code LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String code = rs.getString(1);
                ACCOUNT_CACHE.put(key, code);
                return code;
            }
        }
        throw new IllegalStateException("تعذر تحديد حساب ضريبة المخرجات تلقائياً — تأكد من وجود حساب ضريبة فرعي نوعه LIABILITY في شجرة الحسابات");
    }

    public static String resolveOutputTaxAccount() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return resolveOutputTaxAccount(conn);
        }
    }

    // ========== ضريبة المدخلات (المشتريات) ==========
    public static String resolveInputTaxAccount(Connection conn) throws SQLException {
        String key = "TAX_INPUT";
        String cached = ACCOUNT_CACHE.get(key);
        if (cached != null) return cached;
        String sql = "SELECT account_code FROM chart_of_accounts WHERE is_sub_account=1 AND account_type='ASSET' AND (account_name LIKE '%ضريبة%' OR account_name LIKE '%ضريبه%') ORDER BY account_code LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String code = rs.getString(1);
                ACCOUNT_CACHE.put(key, code);
                return code;
            }
        }
        throw new IllegalStateException("تعذر تحديد حساب ضريبة المدخلات تلقائياً — تأكد من وجود حساب ضريبة فرعي نوعه ASSET في شجرة الحسابات");
    }

    public static String resolveInputTaxAccount() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return resolveInputTaxAccount(conn);
        }
    }

    // ========== حساب الإيراد للصنف ==========
    public static String resolveRevenueAccount(Connection conn, String itemCode) throws SQLException {
        String key = "REV_" + (itemCode != null ? itemCode.trim() : "DEFAULT");
        String cached = ACCOUNT_CACHE.get(key);
        if (cached != null) return cached;
        if (itemCode != null && !itemCode.trim().isEmpty()) {
            String sql = "SELECT sales_revenue_account FROM inventory_items WHERE item_code=? LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, itemCode.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String acc = rs.getString(1);
                        if (acc != null && !acc.trim().isEmpty()) {
                            String trimmed = acc.trim();
                            ACCOUNT_CACHE.put(key, trimmed);
                            return trimmed;
                        }
                    }
                }
            }
        }
        String sql = "SELECT account_code FROM chart_of_accounts WHERE is_sub_account=1 AND account_type='REVENUE' AND account_name LIKE '%مبيعات%' AND account_name NOT LIKE '%مردودات%' ORDER BY account_code LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String code = rs.getString(1);
                ACCOUNT_CACHE.put(key, code);
                return code;
            }
        }
        throw new IllegalStateException("تعذر تحديد حساب الإيراد تلقائياً");
    }

    public static String resolveCogsAccount(Connection conn, String itemCode) throws SQLException {
        String key = "COGS_" + (itemCode != null ? itemCode.trim() : "DEFAULT");
        String cached = ACCOUNT_CACHE.get(key);
        if (cached != null) return cached;
        if (itemCode != null && !itemCode.trim().isEmpty()) {
            String sql = "SELECT cogs_account FROM inventory_items WHERE item_code=? LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, itemCode.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String acc = rs.getString(1);
                        if (acc != null && !acc.trim().isEmpty()) {
                            String trimmed = acc.trim();
                            ACCOUNT_CACHE.put(key, trimmed);
                            return trimmed;
                        }
                    }
                }
            }
        }
        String sql = "SELECT account_code FROM chart_of_accounts WHERE is_sub_account=1 AND account_type='EXPENSE' AND account_name LIKE '%تكلفة%' ORDER BY account_code LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String code = rs.getString(1);
                ACCOUNT_CACHE.put(key, code);
                return code;
            }
        }
        throw new IllegalStateException("تعذر تحديد حساب تكلفة المبيعات COGS تلقائياً");
    }

    public static String resolveInventoryAccount(Connection conn, String itemCode) throws SQLException {
        String key = "INV_" + (itemCode != null ? itemCode.trim() : "DEFAULT");
        String cached = ACCOUNT_CACHE.get(key);
        if (cached != null) return cached;
        if (itemCode != null && !itemCode.trim().isEmpty()) {
            String sql = "SELECT inventory_account FROM inventory_items WHERE item_code=? LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, itemCode.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String acc = rs.getString(1);
                        if (acc != null && !acc.trim().isEmpty()) {
                            String trimmed = acc.trim();
                            ACCOUNT_CACHE.put(key, trimmed);
                            return trimmed;
                        }
                    }
                }
            }
        }
        String sql = "SELECT account_code FROM chart_of_accounts WHERE is_sub_account=1 AND account_type='ASSET' AND account_name LIKE '%مخزن%منتجات%تام%' ORDER BY account_code LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String code = rs.getString(1);
                ACCOUNT_CACHE.put(key, code);
                return code;
            }
        }
        String sql2 = "SELECT account_code FROM chart_of_accounts WHERE is_sub_account=1 AND account_type='ASSET' AND account_name LIKE '%مخزن%' ORDER BY account_code LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql2); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String code = rs.getString(1);
                ACCOUNT_CACHE.put(key, code);
                return code;
            }
        }
        throw new IllegalStateException("تعذر تحديد حساب المخزون تلقائياً");
    }

    public static String resolveSalesReturnAccount(Connection conn) throws SQLException {
        String key = "SALES_RETURN";
        String cached = ACCOUNT_CACHE.get(key);
        if (cached != null) return cached;
        String sql = "SELECT account_code FROM chart_of_accounts WHERE is_sub_account=1 AND account_name LIKE '%مردودات%مبيعات%' ORDER BY account_code LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String code = rs.getString(1);
                ACCOUNT_CACHE.put(key, code);
                return code;
            }
        }
        String sql2 = "SELECT account_code FROM chart_of_accounts WHERE is_sub_account=1 AND account_type='REVENUE' AND account_name LIKE '%مردودات%' ORDER BY account_code LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql2); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String code = rs.getString(1);
                ACCOUNT_CACHE.put(key, code);
                return code;
            }
        }
        throw new IllegalStateException("تعذر تحديد حساب مردودات المبيعات تلقائياً");
    }

    public static String resolveRawMaterialAccount(Connection conn, String itemCode) throws SQLException {
        String key = "RAW_" + (itemCode != null ? itemCode.trim() : "DEFAULT");
        String cached = ACCOUNT_CACHE.get(key);
        if (cached != null) return cached;
        if (itemCode != null && !itemCode.trim().isEmpty()) {
            String sql = "SELECT inventory_account FROM inventory_items WHERE item_code=? LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, itemCode.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String acc = rs.getString(1);
                        if (acc != null && !acc.trim().isEmpty()) {
                            String trimmed = acc.trim();
                            ACCOUNT_CACHE.put(key, trimmed);
                            return trimmed;
                        }
                    }
                }
            }
        }
        String sql = "SELECT account_code FROM chart_of_accounts WHERE is_sub_account=1 AND account_type='ASSET' AND account_name LIKE '%مواد%خام%' ORDER BY account_code LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String code = rs.getString(1);
                ACCOUNT_CACHE.put(key, code);
                return code;
            }
        }
        throw new IllegalStateException("تعذر تحديد حساب مخزون المواد الخام تلقائياً");
    }

    public static String resolveSupplierAccount(Connection conn) throws SQLException {
        String key = "SUPPLIER";
        String cached = ACCOUNT_CACHE.get(key);
        if (cached != null) return cached;
        String sql = "SELECT account_code FROM chart_of_accounts WHERE is_sub_account=1 AND account_type='LIABILITY' AND account_name LIKE '%مورد%' ORDER BY account_code LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String code = rs.getString(1);
                ACCOUNT_CACHE.put(key, code);
                return code;
            }
        }
        throw new IllegalStateException("تعذر تحديد حساب المورد تلقائياً");
    }

    // فحص Null-safe
    public static void requireAccount(String label, String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalStateException("الحساب المحاسبي [" + label + "] غير محدد تلقائياً");
        }
    }
}
