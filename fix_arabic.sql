USE `erp_factory_db`;
SET NAMES utf8mb4;

UPDATE `chart_of_accounts` SET `account_name` = 'مردودات مبيعات المنتجات التامة' WHERE `account_code` = '410201';
UPDATE `chart_of_accounts` SET `account_name` = 'أمانات ضريبة المبيعات والقيمة المضافة' WHERE `account_code` = '220301';
UPDATE `chart_of_accounts` SET `account_name` = 'شركة الأمل للتوزيع والتجارة' WHERE `account_code` = '123020001';
UPDATE `chart_of_accounts` SET `account_name` = 'مخزن المنتجات التامة الرئيسي' WHERE `account_code` = '1210301';
UPDATE `chart_of_accounts` SET `account_name` = 'تكلفة مبيعات المنتجات التامة (COGS)' WHERE `account_code` = '510101';