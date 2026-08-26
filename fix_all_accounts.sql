USE `erp_factory_db`;

-- 1. تحويل ترميز جدول شجرة الحسابات وجدول القيود جذرياً إلى UTF-8
ALTER TABLE `chart_of_accounts` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `journal_entry_lines` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SET NAMES utf8mb4;

-- 2. تحديث كافة الأصول والحسابات الفرعية للعملاء والمخازن
UPDATE `chart_of_accounts` SET `account_name` = 'الأصول' WHERE `account_code` = '1';
UPDATE `chart_of_accounts` SET `account_name` = 'الأصول المتداولة' WHERE `account_code` = '12';
UPDATE `chart_of_accounts` SET `account_name` = 'المخزون السلعي' WHERE `account_code` = '121';
UPDATE `chart_of_accounts` SET `account_name` = 'مخزون المنتجات التامة' WHERE `account_code` = '12103';
UPDATE `chart_of_accounts` SET `account_name` = 'مخزن المنتجات التامة الرئيسي' WHERE `account_code` = '1210301';

UPDATE `chart_of_accounts` SET `account_name` = 'العملاء وحسابات القبض' WHERE `account_code` = '12302';
UPDATE `chart_of_accounts` SET `account_name` = 'شركة الأمل للتوزيع والتجارة' WHERE `account_code` = '123020001';
UPDATE `chart_of_accounts` SET `account_name` = 'شركة البحر المتوسط' WHERE `account_code` = '123020002';
UPDATE `chart_of_accounts` SET `account_name` = 'شركة العالمية للإنتاج' WHERE `account_code` = '123020003';

-- 3. تحديث الخصوم والالتزامات والضرائب
UPDATE `chart_of_accounts` SET `account_name` = 'الالتزامات والخصوم' WHERE `account_code` = '2';
UPDATE `chart_of_accounts` SET `account_name` = 'الالتزامات المتداولة' WHERE `account_code` = '22';
UPDATE `chart_of_accounts` SET `account_name` = 'أمانات ضريبة المبيعات والقيمة المضافة' WHERE `account_code` = '220301';

-- 4. تحديث الإيرادات والمبيعات
UPDATE `chart_of_accounts` SET `account_name` = 'الإيرادات والمبيعات' WHERE `account_code` = '4';
UPDATE `chart_of_accounts` SET `account_name` = 'مبيعات المنتجات التامة' WHERE `account_code` = '410101';
UPDATE `chart_of_accounts` SET `account_name` = 'مردودات مبيعات المنتجات التامة' WHERE `account_code` = '410201';

-- 5. تحديث المصروفات وتكلفة المبيعات
UPDATE `chart_of_accounts` SET `account_name` = 'المصروفات والتكاليف' WHERE `account_code` = '5';
UPDATE `chart_of_accounts` SET `account_name` = 'تكلفة البضاعة والمنتجات المباعة (COGS)' WHERE `account_code` = '51';
UPDATE `chart_of_accounts` SET `account_name` = 'تكلفة مبيعات المنتجات التامة (COGS)' WHERE `account_code` = '510101';