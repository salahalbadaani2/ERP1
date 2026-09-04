-- ============================================================================
-- نظام ERP المصنعي المتكامل - سكربت قاعدة البيانات الشامل (schema.sql)
-- متوافق مع: MySQL 5.7+ / MySQL 8.0+ / MariaDB (XAMPP 10.4.32)
-- الترميز المعتمد: UTF-8 Unicode (utf8mb4_unicode_ci)
--
-- ملاحظة الأمان (M3 - خطة توحيد قاعدة البيانات):
--   * هذا الملف = المرجع الوثائقي الوحيد للبنية، ويمكن إعادة تشغيله بأمان
--     عدة مرات (كل CREATE غالباً IF NOT EXISTS) ولا يحتوي أي DROP TABLE.
--   * لا يحذف أي بيانات عند إعادة التشغيل.
--   * البنية القائمة في قاعدة الإنتاج (erp_factory_db) هي مصدر الحقيقة.
-- ============================================================================

-- 1. إنشاء قاعدة البيانات وضبط الترميز المعتمد
CREATE DATABASE IF NOT EXISTS `erp_factory_db` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `erp_factory_db`;

-- تعطيل التحقق من المفاتيح الأجنبية مؤقتاً أثناء التأسيس
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------------
-- 2. جدول دليل وشجرة الحسابات (chart_of_accounts)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chart_of_accounts` (
  `account_code` VARCHAR(20) NOT NULL COMMENT 'رقم الحساب (مثال: 123020001)',
  `account_name` VARCHAR(255) NOT NULL COMMENT 'اسم الحساب المحاسبي بالعربي',
  `account_type` ENUM('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE') NOT NULL COMMENT 'النوع الرئيسي',
  `parent_code` VARCHAR(20) DEFAULT NULL COMMENT 'رقم الحساب الأب',
  `account_level` INT NOT NULL DEFAULT 6 COMMENT 'مستوى الحساب في الشجرة (1 إلى 6)',
  `is_sub_account` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 = فرعي يقبل الحركة، 0 = رئيسي تجميعي',
  `current_balance` DECIMAL(18, 4) NOT NULL DEFAULT 0.0000 COMMENT 'الرصيد الحالي',
  `currency` VARCHAR(10) NOT NULL DEFAULT 'YER' COMMENT 'العملة (YER, USD, SAR)',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`account_code`),
  INDEX `idx_parent_code` (`parent_code`),
  INDEX `idx_is_sub` (`is_sub_account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='شجرة ودليل الحسابات المالية';

-- ----------------------------------------------------------------------------
-- 3. جدول بطاقة الأصناف والمخزون التام والمواد الخام (inventory_items)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inventory_items` (
  `item_code` VARCHAR(50) NOT NULL COMMENT 'كود الصنف الفريد',
  `item_name` VARCHAR(255) NOT NULL COMMENT 'اسم الصنف التجاري/المصنعي',
  `category` VARCHAR(100) DEFAULT 'منتجات تامة' COMMENT 'تصنيف الصنف',
  `unit` VARCHAR(50) NOT NULL DEFAULT 'كرتون' COMMENT 'وحدة القياس',
  `unit_type` VARCHAR(10) NOT NULL DEFAULT 'COUNT' COMMENT 'نوع الوحدة: COUNT (عددي) أو WEIGHT (وزني)',
  `default_sale_price` DECIMAL(15, 2) NOT NULL DEFAULT 0.00 COMMENT 'سعر البيع الافتراضي',
  `unit_cost` DECIMAL(15, 2) NOT NULL DEFAULT 0.00 COMMENT 'تكلفة الوحدة المعيارية',
  `current_stock` DECIMAL(15, 2) NOT NULL DEFAULT 0.00 COMMENT 'الرصيد المخزني الحالي',
  `inventory_account` VARCHAR(20) NOT NULL DEFAULT '1210301' COMMENT 'حساب مخزن الصنف الفرعي',
  `sales_revenue_account` VARCHAR(20) NOT NULL DEFAULT '410101' COMMENT 'حساب إيراد مبيعات الصنف',
  `cogs_account` VARCHAR(20) NOT NULL DEFAULT '510101' COMMENT 'حساب تكلفة مبيعات الصنف (COGS)',
  `conversion_factor` DECIMAL(15, 4) NOT NULL DEFAULT 1.0000 COMMENT 'معامل التحويل للوحدة الأساسية',
  `min_stock_level` DECIMAL(15, 4) NOT NULL DEFAULT 0.0000 COMMENT 'حد إعادة الطلب',
  `expiry_date` DATE DEFAULT NULL COMMENT 'تاريخ انتهاء الصلاحية',
  `batch_no` VARCHAR(50) DEFAULT NULL COMMENT 'رقم التشغيلة أو الدفعة',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`item_code`),
  CONSTRAINT `fk_item_inv_acc` FOREIGN KEY (`inventory_account`) REFERENCES `chart_of_accounts` (`account_code`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='دليل الأصناف والمخزون';

-- ----------------------------------------------------------------------------
-- 4. جدول فواتير المبيعات الصادرة (sales_invoices)
-- المفتاح الأساسي الحقيقي: invoice_code
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sales_invoices` (
  `invoice_code` VARCHAR(50) NOT NULL COMMENT 'رقم فاتورة المبيعات (مثال: INV-1001)',
  `invoice_date` DATE NOT NULL COMMENT 'تاريخ الفاتورة',
  `customer_account` VARCHAR(20) NOT NULL COMMENT 'حساب العميل (123020001)',
  `sales_revenue_account` VARCHAR(20) NOT NULL DEFAULT '410101' COMMENT 'حساب الإيراد',
  `tax_account` VARCHAR(20) NOT NULL DEFAULT '220301' COMMENT 'حساب ضريبة المبيعات',
  `finished_goods_account` VARCHAR(20) NOT NULL DEFAULT '1210301' COMMENT 'حساب مخزن المنتجات التامة',
  `cogs_account` VARCHAR(20) NOT NULL DEFAULT '510101' COMMENT 'حساب تكلفة البضاعة المباعة',
  `subtotal_amount` DECIMAL(15, 2) NOT NULL DEFAULT 0.00 COMMENT 'المبلغ الصافي قبل الضريبة',
  `tax_rate` DECIMAL(5, 4) NOT NULL DEFAULT 0.1500 COMMENT 'نسبة الضريبة (مثال: 0.15)',
  `tax_amount` DECIMAL(15, 2) NOT NULL DEFAULT 0.00 COMMENT 'مبلغ الضريبة المحتسب',
  `total_invoice_amount` DECIMAL(15, 2) NOT NULL DEFAULT 0.00 COMMENT 'إجمالي الفاتورة مع الضريبة',
  `inventory_cost_amount` DECIMAL(15, 2) NOT NULL DEFAULT 0.00 COMMENT 'إجمالي تكلفة المخزون (COGS)',
  `payment_terms` VARCHAR(50) DEFAULT 'آجل 30 يوم' COMMENT 'شروط السداد',
  `notes` TEXT DEFAULT NULL COMMENT 'ملاحظات إضافية',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`invoice_code`),
  INDEX `idx_cust_inv` (`customer_account`),
  CONSTRAINT `fk_inv_cust` FOREIGN KEY (`customer_account`) REFERENCES `chart_of_accounts` (`account_code`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='فواتير المبيعات الصادرة';

-- ----------------------------------------------------------------------------
-- 5. جدول حركات المخزون (inventory_movements)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inventory_movements` (
  `movement_id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'معرف الحركة المخزنية',
  `document_number` VARCHAR(50) DEFAULT NULL COMMENT 'رقم المستند/الفاتورة',
  `movement_type` VARCHAR(20) DEFAULT NULL COMMENT 'نوع الحركة (RECEIPT/ISSUE)',
  `item_code` VARCHAR(50) DEFAULT NULL COMMENT 'كود الصنف',
  `item_name` VARCHAR(255) DEFAULT NULL COMMENT 'اسم الصنف',
  `quantity` DECIMAL(12, 3) NOT NULL COMMENT 'الكمية',
  `unit_cost` DECIMAL(15,2) DEFAULT NULL COMMENT 'تكلفة الوحدة',
  `inventory_account` VARCHAR(20) DEFAULT NULL COMMENT 'حساب المخزون',
  `counter_account` VARCHAR(20) DEFAULT NULL COMMENT 'الحساب المقابل',
  `receiver` VARCHAR(255) DEFAULT NULL COMMENT 'المستلم',
  `deliverer` VARCHAR(255) DEFAULT NULL COMMENT 'المورد',
  `narration` TEXT DEFAULT NULL COMMENT 'البيان والشرح',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'تاريخ الإنشاء'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='جدول حركات المخزون';

-- ----------------------------------------------------------------------------
-- 5أ. جدول فواتير وأذون مردودات المبيعات (sales_return_invoices)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sales_return_invoices` (
  `return_code` VARCHAR(50) NOT NULL COMMENT 'رقم فاتورة المرتجع (مثال: SRI-1001)',
  `original_invoice_code` VARCHAR(50) DEFAULT 'مباشر بدون فاتورة' COMMENT 'رقم الفاتورة الأصلية',
  `return_date` DATE NOT NULL COMMENT 'تاريخ المرتجع',
  `customer_account` VARCHAR(20) NOT NULL COMMENT 'حساب العميل الدائن (123020001)',
  `sales_return_account` VARCHAR(20) NOT NULL DEFAULT '410201' COMMENT 'حساب مردودات المبيعات المدين',
  `tax_account` VARCHAR(20) NOT NULL DEFAULT '220301' COMMENT 'حساب ضريبة المبيعات المدين المسترد',
  `finished_goods_account` VARCHAR(20) NOT NULL DEFAULT '1210301' COMMENT 'حساب مخزن التام المسترد المدين',
  `cogs_account` VARCHAR(20) NOT NULL DEFAULT '510101' COMMENT 'حساب تكلفة المبيعات المخفض الدائن',
  `return_amount` DECIMAL(15, 2) NOT NULL DEFAULT 0.00 COMMENT 'قيمة المرتجع الأساسية',
  `is_tax_applied` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 = خاضع للضريبة، 0 = معفى',
  `tax_rate` DECIMAL(5, 4) NOT NULL DEFAULT 0.1500 COMMENT 'نسبة الضريبة المطبقة',
  `tax_amount` DECIMAL(15, 2) NOT NULL DEFAULT 0.00 COMMENT 'مبلغ الضريبة المسترد',
  `total_customer_credit` DECIMAL(15, 2) NOT NULL DEFAULT 0.00 COMMENT 'إجمالي المستحق لحساب العميل الدائن',
  `inventory_cost` DECIMAL(15, 2) NOT NULL DEFAULT 0.00 COMMENT 'التكلفة المخزنية المستردة للإنتاج التام',
  `return_reason` VARCHAR(255) DEFAULT 'تلف أثناء النقل والتخزين' COMMENT 'سبب الإرجاع الفني',
  `batch_no` VARCHAR(50) DEFAULT '---' COMMENT 'رقم التشغيلة / الدفعة المصنعية',
  `status` ENUM('DRAFT', 'POSTED', 'CANCELLED') NOT NULL DEFAULT 'POSTED' COMMENT 'حالة الترحيل المحاسبي',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`return_code`),
  INDEX `idx_ret_cust` (`customer_account`),
  INDEX `idx_ret_date` (`return_date`),
  CONSTRAINT `fk_ret_cust` FOREIGN KEY (`customer_account`) REFERENCES `chart_of_accounts` (`account_code`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='فواتير وأذون مردودات ومسموحات المبيعات';

-- جدول التوافق لدوال DatabaseManager.insertSalesReturnNote
CREATE TABLE IF NOT EXISTS `sales_return_notes` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `return_code` VARCHAR(50) NOT NULL UNIQUE,
  `customer_account` VARCHAR(20) NOT NULL,
  `sales_return_account` VARCHAR(20) NOT NULL,
  `finished_goods_account` VARCHAR(20) NOT NULL,
  `cogs_account` VARCHAR(20) NOT NULL,
  `total_amount` DECIMAL(15, 2) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5ب. جدول تفاصيل فواتير المبيعات (sales_invoice_details)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sales_invoice_details` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `invoice_code` VARCHAR(50) NOT NULL,
  `item_code` VARCHAR(50) NOT NULL,
  `item_name` VARCHAR(255) NOT NULL,
  `quantity` DECIMAL(12, 3) NOT NULL COMMENT 'الكمية (كيلو وجرام للمواد الوزنية)',
  `unit_price` DECIMAL(15, 2) NOT NULL COMMENT 'سعر الوحدة',
  `amount` DECIMAL(15, 2) NOT NULL COMMENT 'المبلغ',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_sid_invoice` (`invoice_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='تفاصيل فواتير المبيعات';

-- ----------------------------------------------------------------------------
-- 5ج. جدول تفاصيل فواتير المشتريات (purchase_invoice_details)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `purchase_invoice_details` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `invoice_code` VARCHAR(50) NOT NULL,
  `item_code` VARCHAR(50) NOT NULL,
  `item_name` VARCHAR(255) NOT NULL,
  `quantity` DECIMAL(12, 3) NOT NULL COMMENT 'الكمية (كيلو وجرام للمواد الوزنية)',
  `unit_cost` DECIMAL(15, 2) NOT NULL COMMENT 'تكلفة الوحدة',
  `amount` DECIMAL(15, 2) NOT NULL COMMENT 'المبلغ',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_pid_invoice` (`invoice_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='تفاصيل فواتير المشتريات';

-- ----------------------------------------------------------------------------
-- 5د. جدول تفاصيل الإنتاج (production_details)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `production_details` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `production_order` VARCHAR(50) NOT NULL,
  `item_code` VARCHAR(50) NOT NULL,
  `item_name` VARCHAR(255) NOT NULL,
  `quantity` DECIMAL(12, 3) NOT NULL COMMENT 'الكمية المنتجة (كيلو وجرام للمواد الوزنية)',
  `unit_cost` DECIMAL(15, 2) NOT NULL COMMENT 'تكلفة الوحدة',
  `total_cost` DECIMAL(15, 2) NOT NULL COMMENT 'التكلفة الإجمالية',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_pd_order` (`production_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='تفاصيل أوامر الإنتاج';

-- ----------------------------------------------------------------------------
-- 5هـ. جدول فواتير المشتريات (purchase_invoices)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `purchase_invoices` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `invoice_code` VARCHAR(50) NOT NULL UNIQUE,
  `inventory_account` VARCHAR(20) NOT NULL,
  `supplier_account` VARCHAR(20) NOT NULL,
  `input_tax_account` VARCHAR(20) DEFAULT NULL,
  `amount` DECIMAL(18,4) NOT NULL,
  `tax_amount` DECIMAL(18,4) NOT NULL DEFAULT 0,
  `total_amount` DECIMAL(18,4) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5و. جدول فواتير مرتجعات المشتريات (purchase_return_invoices)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `purchase_return_invoices` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `return_code` VARCHAR(50) NOT NULL UNIQUE,
  `inventory_account` VARCHAR(20) NOT NULL,
  `supplier_account` VARCHAR(20) NOT NULL,
  `input_tax_account` VARCHAR(20) DEFAULT NULL,
  `amount` DECIMAL(18,4) NOT NULL,
  `tax_amount` DECIMAL(18,4) NOT NULL DEFAULT 0,
  `total_amount` DECIMAL(18,4) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 6. جدول قيود اليومية العامة المزدوجة (journal_entries & lines)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `journal_entries` (
  `entry_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `entry_number` VARCHAR(50) NOT NULL UNIQUE COMMENT 'رقم القيد اليومي (مثال: JV-2026-001)',
  `entry_date` DATE NOT NULL COMMENT 'تاريخ القيد',
  `reference_doc` VARCHAR(50) DEFAULT NULL COMMENT 'المرجع (رقم الفاتورة أو المرتجع)',
  `source_module` ENUM('SALES', 'SALES_RETURN', 'TREASURY', 'MANUAL', 'INVENTORY') NOT NULL DEFAULT 'SALES_RETURN',
  `narration` TEXT NOT NULL COMMENT 'البيان والشرح المحاسبي للقيد',
  `total_debit` DECIMAL(18, 4) NOT NULL DEFAULT 0.0000 COMMENT 'إجمالي المدين',
  `total_credit` DECIMAL(18, 4) NOT NULL DEFAULT 0.0000 COMMENT 'إجمالي الدائن',
  `is_balanced` TINYINT(1) GENERATED ALWAYS AS (total_debit = total_credit) STORED COMMENT 'حالة اتزان القيد',
  `posted_by` VARCHAR(100) DEFAULT 'النظام الآلي',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_entry_date` (`entry_date`),
  INDEX `idx_ref_doc` (`reference_doc`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='رأس سند قيد اليومية العامة';

CREATE TABLE IF NOT EXISTS `journal_entry_lines` (
  `line_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `entry_id` BIGINT NOT NULL,
  `account_code` VARCHAR(20) NOT NULL COMMENT 'الحساب الفرعي',
  `line_narration` VARCHAR(255) DEFAULT NULL COMMENT 'شرح السطر',
  `debit_amount` DECIMAL(18, 4) NOT NULL DEFAULT 0.0000 COMMENT 'المبلغ المدين',
  `credit_amount` DECIMAL(18, 4) NOT NULL DEFAULT 0.0000 COMMENT 'المبلغ الدائن',
  CONSTRAINT `fk_line_entry` FOREIGN KEY (`entry_id`) REFERENCES `journal_entries` (`entry_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_line_acc` FOREIGN KEY (`account_code`) REFERENCES `chart_of_accounts` (`account_code`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='تفاصيل وسطور قيود اليومية';

-- ----------------------------------------------------------------------------
-- 7. الجداول التشغيلية (أذون المخازن والإنتاج والخزينة والتنبيهات)
-- بنيتها من القاعدة الحية erp_factory_db (مصدر الحقيقة)
-- ----------------------------------------------------------------------------

-- 7.1 سندات استلام المواد الخام (GRN)
CREATE TABLE IF NOT EXISTS `goods_receipt_notes` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `grn_code` VARCHAR(50) DEFAULT NULL,
  `supplier_account` VARCHAR(20) DEFAULT NULL,
  `raw_material_account` VARCHAR(20) DEFAULT NULL,
  `total_amount` DOUBLE DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `is_posted` TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7.2 سندات صرف المواد للإنتاج (Material Issue)
CREATE TABLE IF NOT EXISTS `material_issue_notes` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `issue_code` VARCHAR(50) DEFAULT NULL,
  `wip_account` VARCHAR(20) DEFAULT NULL,
  `raw_material_account` VARCHAR(20) DEFAULT NULL,
  `total_amount` DOUBLE DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `is_posted` TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7.3 سندات استلام المنتج التام (Finished Goods Receipt)
CREATE TABLE IF NOT EXISTS `finished_goods_notes` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `note_code` VARCHAR(50) DEFAULT NULL,
  `finished_goods_account` VARCHAR(20) DEFAULT NULL,
  `wip_account` VARCHAR(20) DEFAULT NULL,
  `total_amount` DOUBLE DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `is_posted` TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7.4 تحويلات سيارات التوزيع (Van Transfer)
CREATE TABLE IF NOT EXISTS `van_transfer_notes` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `transfer_code` VARCHAR(50) DEFAULT NULL,
  `van_account` VARCHAR(20) DEFAULT NULL,
  `finished_goods_account` VARCHAR(20) DEFAULT NULL,
  `total_amount` DOUBLE DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `quantity` DOUBLE DEFAULT NULL,
  `unit_cost` DOUBLE DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7.5 مرتجعات سيارات التوزيع (Van Return)
CREATE TABLE IF NOT EXISTS `van_return_notes` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `return_code` VARCHAR(50) DEFAULT NULL,
  `finished_goods_account` VARCHAR(20) DEFAULT NULL,
  `van_account` VARCHAR(20) DEFAULT NULL,
  `total_amount` DOUBLE DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `quantity` DOUBLE DEFAULT NULL,
  `unit_cost` DOUBLE DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7.6 مرتجعات المشتريات (Purchase Return Notes)
CREATE TABLE IF NOT EXISTS `purchase_return_notes` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `return_code` VARCHAR(50) DEFAULT NULL,
  `supplier_account` VARCHAR(20) DEFAULT NULL,
  `raw_material_account` VARCHAR(20) DEFAULT NULL,
  `total_amount` DOUBLE DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7.7 إقفال المصاريف غير المباشرة (Overhead Closing)
CREATE TABLE IF NOT EXISTS `overhead_closings` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `closing_code` VARCHAR(50) DEFAULT NULL,
  `actual_account` VARCHAR(20) DEFAULT NULL,
  `applied_account` VARCHAR(20) DEFAULT NULL,
  `cogs_account` VARCHAR(20) DEFAULT NULL,
  `actual_amount` DOUBLE DEFAULT NULL,
  `applied_amount` DOUBLE DEFAULT NULL,
  `month_year` VARCHAR(20) DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `is_posted` TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7.8 سندات الخزينة والبنك (Treasury Vouchers)
CREATE TABLE IF NOT EXISTS `treasury_vouchers` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `voucher_code` VARCHAR(50) DEFAULT NULL,
  `account_code` VARCHAR(20) DEFAULT NULL,
  `amount` DOUBLE DEFAULT NULL,
  `voucher_type` VARCHAR(20) DEFAULT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `voucher_number` VARCHAR(50) DEFAULT NULL,
  `voucher_date` DATE DEFAULT NULL,
  `reference_name` VARCHAR(255) DEFAULT NULL,
  `narration` TEXT DEFAULT NULL,
  UNIQUE KEY `uq_voucher_code` (`voucher_code`),
  UNIQUE KEY `uq_voucher_number` (`voucher_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7.9 التنبيهات المخزنية (Stock Alerts)
CREATE TABLE IF NOT EXISTS `stock_alerts` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `item_code` VARCHAR(50) DEFAULT NULL,
  `item_name` VARCHAR(100) DEFAULT NULL,
  `current_stock` DOUBLE DEFAULT NULL,
  `min_stock` DOUBLE DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uq_item_code` (`item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 8. جداول الموردين والعملاء والمفوضين والمرفقات
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `business_parties` (
  `code` VARCHAR(20) PRIMARY KEY,
  `ar_name` VARCHAR(255) NOT NULL,
  `en_name` VARCHAR(255) DEFAULT NULL,
  `party_type` ENUM('supplier','customer') NOT NULL,
  `status` ENUM('active','suspended') NOT NULL DEFAULT 'active',
  `owner_name` VARCHAR(255) DEFAULT NULL,
  `parent_account_code` VARCHAR(20) DEFAULT NULL,
  `sub_account_code` VARCHAR(20) DEFAULT NULL,
  `credit_limit` DECIMAL(18,2) DEFAULT 0,
  `credit_period_days` INT DEFAULT 0,
  `currency_code` VARCHAR(10) DEFAULT 'YER',
  `opening_balance` DECIMAL(18,4) DEFAULT 0,
  `balance_type` ENUM('debit','credit') DEFAULT 'debit',
  `vat_number` VARCHAR(20) UNIQUE,
  `cr_number` VARCHAR(50) UNIQUE,
  `cr_image_path` VARCHAR(500),
  `phone` VARCHAR(20),
  `mobile` VARCHAR(20),
  `email` VARCHAR(100),
  `address` TEXT,
  `contact_person` VARCHAR(255),
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `party_delegates` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `party_code` VARCHAR(20) NOT NULL,
  `delegate_name` VARCHAR(255) NOT NULL,
  `job_title` VARCHAR(255),
  `authorization_doc_path` VARCHAR(500),
  FOREIGN KEY (`party_code`) REFERENCES `business_parties` (`code`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `document_attachments` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `party_code` VARCHAR(20) NOT NULL,
  `doc_type` VARCHAR(50),
  `file_path` VARCHAR(500),
  `description` TEXT,
  `uploaded_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`party_code`) REFERENCES `business_parties` (`code`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 9. جداول التسلسل والرقن (Document Sequences)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `document_sequences` (
  `document_type` VARCHAR(50) PRIMARY KEY,
  `next_number` INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 10. جداول التطبيق البنكي (Bank Reconciliation)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bank_reconciliation` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `bank_account` VARCHAR(20),
  `entry_id` BIGINT,
  `journal_entry_number` VARCHAR(50),
  `reconciled` BOOLEAN DEFAULT FALSE,
  `reconciled_date` DATE,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uq_bank_entry` (`bank_account`, `journal_entry_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bank_reconciliation_memos` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `bank_account` VARCHAR(20),
  `as_of_date` DATE,
  `book_balance` DOUBLE,
  `bank_balance` DOUBLE,
  `diff` DOUBLE,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 11. تفعيل فحص المفاتيح الأجنبية وتعبئة البيانات الأساسية لشجرة الحسابات (Seed Data)
-- ----------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 1;

-- إدخال دليل الحسابات المعتمد مع ضمان الرقابة والمستويات (1-6)
INSERT INTO `chart_of_accounts` (`account_code`, `account_name`, `account_type`, `parent_code`, `account_level`, `is_sub_account`, `current_balance`) VALUES
-- الأصول (Assets)
('1', 'الأصول', 'ASSET', NULL, 1, 0, 0),
('12', 'الأصول المتداولة', 'ASSET', '1', 2, 0, 0),
('121', 'المخزون السلعي والمستودعات', 'ASSET', '12', 3, 0, 0),
('12103', 'مخزن المنتجات التامة الصنع', 'ASSET', '121', 4, 0, 0),
('1210301', 'مخزن المنتجات التامة الرئيسي - صنعاء', 'ASSET', '12103', 6, 1, 850000.0000),
('12101', 'مخزن المواد الخام ومواد التعبئة', 'ASSET', '121', 4, 0, 0),
('1210101', 'مخزن المواد الخام الرئيسي - صنعاء', 'ASSET', '12101', 6, 1, 0.0000),
('12102', 'مخزن الإنتاج تحت التشغيل', 'ASSET', '121', 4, 0, 0),
('1210201', 'أمر إنتاج قيد التشغيل الرئيسي - صنعاء', 'ASSET', '12102', 6, 1, 0.0000),
('123', 'المدينون والأرصدة المدينة', 'ASSET', '12', 3, 0, 0),
('12302', 'العملاء والذمم التجارية', 'ASSET', '123', 4, 0, 0),
('123020001', 'شركة الأمل للتوزيع والتجارة المحدودة', 'ASSET', '12302', 6, 1, 450000.0000),
('123020002', 'مؤسسة البركة للمنتجات الغذائية', 'ASSET', '12302', 6, 1, 280000.0000),
('123020003', 'شركة القدس للتسويق والاستيراد', 'ASSET', '12302', 6, 1, 150000.0000),

-- الخصوم والالتزامات (Liabilities)
('2', 'الخصوم والالتزامات', 'LIABILITY', NULL, 1, 0, 0),
('21', 'حسابات الموردين', 'LIABILITY', '2', 2, 0, 0),
('210101', 'مورد عام', 'LIABILITY', '21', 6, 1, 0),
('22', 'الالتزامات المتداولة', 'LIABILITY', '2', 2, 0, 0),
('2203', 'الأمانات والمستحقات الضريبية', 'LIABILITY', '22', 3, 0, 0),
('220301', 'أمانات ضريبة المبيعات والقيمة المضافة (15%)', 'LIABILITY', '2203', 6, 1, 65000.0000),

-- الإيرادات والمبيعات (Revenues)
('4', 'الإيرادات والمبيعات', 'REVENUE', NULL, 1, 0, 0),
('41', 'إيرادات النشاط الجاري والمصنعي', 'REVENUE', '4', 2, 0, 0),
('4101', 'إيرادات مبيعات المنتجات التامة', 'REVENUE', '41', 3, 0, 0),
('410101', 'مبيعات محليات وأغذية تامة الصنع', 'REVENUE', '4101', 6, 1, 1200000.0000),
('4102', 'مردودات ومسموحات المبيعات', 'REVENUE', '41', 3, 0, 0),
('410201', 'مردودات مبيعات المنتجات التامة', 'REVENUE', '4102', 6, 1, 35000.0000),

-- المصروفات والتكاليف (Expenses)
('5', 'المصروفات والتكاليف', 'EXPENSE', NULL, 1, 0, 0),
('51', 'تكاليف النشاط والتشغيل الصناعي', 'EXPENSE', '5', 2, 0, 0),
('5101', 'تكلفة البضاعة والمنتجات المباعة (COGS)', 'EXPENSE', '51', 3, 0, 0),
('510101', 'تكلفة مبيعات المنتجات التامة (COGS)', 'EXPENSE', '5101', 6, 1, 780000.0000),
('52', 'التكاليف الصناعية غير المباشرة', 'EXPENSE', '5', 2, 0, 0),
('5201', 'التكاليف الصناعية غير المباشرة', 'EXPENSE', '52', 4, 0, 0),
('520101', 'التكاليف الصناعية الفعلية', 'EXPENSE', '5201', 6, 1, 0.0000),
('520201', 'التكاليف الصناعية المحملة', 'EXPENSE', '5201', 6, 1, 0.0000),
('520901', 'انحرافات التكاليف الصناعية', 'EXPENSE', '5201', 6, 1, 0.0000)
ON DUPLICATE KEY UPDATE `account_name`=VALUES(`account_name`);

-- إدخال بيانات تجريبية للأصناف المصنعية
INSERT INTO `inventory_items` (`item_code`, `item_name`, `category`, `unit`, `unit_type`, `default_sale_price`, `unit_cost`, `current_stock`, `inventory_account`, `sales_revenue_account`, `cogs_account`) VALUES
('ITEM-101', 'عصير برتقال طبيعي 1 لتر - كرتون (12 عبوة)', 'عصائر ومنتجات تامة', 'كرتون', 'COUNT', 250.00, 180.00, 1500.00, '1210301', '410101', '510101'),
('ITEM-102', 'بسكويت ويفر محشو شوكولاتة (24 علبة)', 'بسكويت وحلويات', 'كرتون', 'COUNT', 180.00, 120.00, 3200.00, '1210301', '410101', '510101'),
('ITEM-103', 'مياه معدنية نقية 500 مل (24 قارورة)', 'مياه معبأة', 'كرتون', 'COUNT', 80.00, 50.00, 5000.00, '1210301', '410101', '510101')
ON DUPLICATE KEY UPDATE `item_name`=VALUES(`item_name`);

-- الحسابات التشغيلية الإضافية للمصانع الغذائية
INSERT INTO `chart_of_accounts` (`account_code`, `account_name`, `account_type`, `parent_code`, `account_level`, `is_sub_account`, `current_balance`) VALUES
('11', 'النقدية والبنوك', 'ASSET', '1', 2, 0, 0.0000),
('111', 'الصناديق والبنوك', 'ASSET', '11', 3, 0, 0.0000),
('1110101', 'الصندوق الرئيسي', 'ASSET', '111', 6, 1, 0.0000),
('12101', 'مخزون المواد الخام', 'ASSET', '121', 4, 0, 0.0000),
('1210101', 'مخزن المواد الخام الرئيسي', 'ASSET', '12101', 6, 1, 0.0000),
('12102', 'الإنتاج تحت التشغيل WIP', 'ASSET', '121', 4, 0, 0.0000),
('1210201', 'أمر إنتاج قيد التشغيل الرئيسي', 'ASSET', '12102', 6, 1, 0.0000),
('21', 'الموردون', 'LIABILITY', '2', 2, 0, 0.0000),
('210101', 'مورد عام', 'LIABILITY', '21', 6, 1, 0.0000),
('3', 'حقوق الملكية', 'EQUITY', NULL, 1, 0, 0.0000),
('31', 'رأس المال والأرباح المحتجزة', 'EQUITY', '3', 2, 0, 0.0000),
('310101', 'رأس المال', 'EQUITY', '31', 6, 1, 0.0000),
('53', 'الأجور المباشرة', 'EXPENSE', '5', 2, 0, 0.0000),
('530101', 'أجور الإنتاج المباشرة', 'EXPENSE', '53', 6, 1, 0.0000),
('54', 'المصروفات الإدارية والبيعية', 'EXPENSE', '5', 2, 0, 0.0000),
('540101', 'مصروفات إدارية عامة', 'EXPENSE', '54', 6, 1, 0.0000),
('540201', 'مصروفات بيع وتوزيع', 'EXPENSE', '54', 6, 1, 0.0000)
ON DUPLICATE KEY UPDATE `account_name`=VALUES(`account_name`), `parent_code`=VALUES(`parent_code`),
`account_level`=VALUES(`account_level`), `is_sub_account`=VALUES(`is_sub_account`);