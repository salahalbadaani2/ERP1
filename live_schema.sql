/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bank_reconciliation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `bank_account` varchar(20) DEFAULT NULL,
  `entry_id` bigint(20) DEFAULT NULL,
  `journal_entry_number` varchar(50) DEFAULT NULL,
  `reconciled` tinyint(1) DEFAULT 0,
  `reconciled_date` date DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_bank_entry` (`bank_account`,`journal_entry_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bank_reconciliation_memos` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `bank_account` varchar(20) DEFAULT NULL,
  `as_of_date` date DEFAULT NULL,
  `book_balance` double DEFAULT NULL,
  `bank_balance` double DEFAULT NULL,
  `diff` double DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `business_parties` (
  `code` varchar(20) NOT NULL,
  `ar_name` varchar(255) NOT NULL,
  `en_name` varchar(255) DEFAULT NULL,
  `party_type` enum('supplier','customer') NOT NULL,
  `status` enum('active','suspended') NOT NULL DEFAULT 'active',
  `owner_name` varchar(255) DEFAULT NULL,
  `parent_account_code` varchar(20) DEFAULT NULL,
  `sub_account_code` varchar(20) DEFAULT NULL,
  `credit_limit` decimal(18,2) DEFAULT 0.00,
  `credit_period_days` int(11) DEFAULT 0,
  `currency_code` varchar(10) DEFAULT 'YER',
  `opening_balance` decimal(18,4) DEFAULT 0.0000,
  `balance_type` enum('debit','credit') DEFAULT 'debit',
  `vat_number` varchar(20) DEFAULT NULL,
  `cr_number` varchar(50) DEFAULT NULL,
  `cr_image_path` varchar(500) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `mobile` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `address` text DEFAULT NULL,
  `contact_person` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`code`),
  UNIQUE KEY `vat_number` (`vat_number`),
  UNIQUE KEY `cr_number` (`cr_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `chart_of_accounts` (
  `account_code` varchar(20) NOT NULL COMMENT '├ء┬▒├آظأ├آظخ ├ء┬د├آظئ├ء┬ص├ء┬│├ء┬د├ء┬ذ (├آظخ├ء┬س├ء┬د├آظئ: 123020001)',
  `account_name` varchar(255) NOT NULL COMMENT '├ء┬د├ء┬│├آظخ ├ء┬د├آظئ├ء┬ص├ء┬│├ء┬د├ء┬ذ ├ء┬د├آظئ├آظخ├ء┬ص├ء┬د├ء┬│├ء┬ذ├آ┼ب ├ء┬ذ├ء┬د├آظئ├ء┬╣├ء┬▒├ء┬ذ├آ┼ب',
  `account_type` enum('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE') NOT NULL COMMENT '├ء┬د├آظئ├آظب├آ╦├ء┬╣ ├ء┬د├آظئ├ء┬▒├ء┬خ├آ┼ب├ء┬│├آ┼ب',
  `parent_code` varchar(20) DEFAULT NULL COMMENT '├ء┬▒├آظأ├آظخ ├ء┬د├آظئ├ء┬ص├ء┬│├ء┬د├ء┬ذ ├ء┬د├آظئ├ء┬ث├ء┬ذ',
  `account_level` int(11) NOT NULL DEFAULT 6 COMMENT '├آظخ├ء┬│├ء┬ز├آ╦├آظ░ ├ء┬د├آظئ├ء┬ص├ء┬│├ء┬د├ء┬ذ ├آ┬├آ┼ب ├ء┬د├آظئ├ء┬┤├ء┬ش├ء┬▒├ء┬ر (1 ├ء┬ح├آظئ├آظ░ 6)',
  `is_sub_account` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1 = ├آ┬├ء┬▒├ء┬╣├آ┼ب ├آ┼ب├آظأ├ء┬ذ├آظئ ├ء┬د├آظئ├ء┬ص├ء┬▒├آ╞ْ├ء┬ر├ء┼ْ 0 = ├ء┬▒├ء┬خ├آ┼ب├ء┬│├آ┼ب ├ء┬ز├ء┬ش├آظخ├آ┼ب├ء┬╣├آ┼ب',
  `current_balance` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '├ء┬د├آظئ├ء┬▒├ء┬╡├آ┼ب├ء┬» ├ء┬د├آظئ├ء┬ص├ء┬د├آظئ├آ┼ب',
  `currency` varchar(10) NOT NULL DEFAULT 'YER' COMMENT '├ء┬د├آظئ├ء┬╣├آظخ├آظئ├ء┬ر (YER, USD, SAR)',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`account_code`),
  KEY `idx_parent_code` (`parent_code`),
  KEY `idx_is_sub` (`is_sub_account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='├ء┬┤├ء┬ش├ء┬▒├ء┬ر ├آ╦├ء┬»├آظئ├آ┼ب├آظئ ├ء┬د├آظئ├ء┬ص├ء┬│├ء┬د├ء┬ذ├ء┬د├ء┬ز ├ء┬د├آظئ├آظخ├ء┬د├آظئ├آ┼ب├ء┬ر';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `document_attachments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `party_code` varchar(20) NOT NULL,
  `doc_type` varchar(50) DEFAULT NULL,
  `file_path` varchar(500) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `uploaded_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `party_code` (`party_code`),
  CONSTRAINT `document_attachments_ibfk_1` FOREIGN KEY (`party_code`) REFERENCES `business_parties` (`code`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `document_sequences` (
  `document_type` varchar(50) NOT NULL,
  `next_number` int(11) NOT NULL,
  PRIMARY KEY (`document_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `finished_goods_notes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `note_code` varchar(50) DEFAULT NULL,
  `finished_goods_account` varchar(20) DEFAULT NULL,
  `wip_account` varchar(20) DEFAULT NULL,
  `total_amount` double DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `is_posted` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `goods_receipt_notes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `grn_code` varchar(50) DEFAULT NULL,
  `supplier_account` varchar(20) DEFAULT NULL,
  `raw_material_account` varchar(20) DEFAULT NULL,
  `total_amount` double DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `is_posted` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `inventory_items` (
  `item_code` varchar(50) NOT NULL COMMENT '├آ╞ْ├آ╦├ء┬» ├ء┬د├آظئ├ء┬╡├آظب├آ┬ ├ء┬د├آظئ├آ┬├ء┬▒├آ┼ب├ء┬»',
  `item_name` varchar(255) NOT NULL COMMENT '├ء┬د├ء┬│├آظخ ├ء┬د├آظئ├ء┬╡├آظب├آ┬ ├ء┬د├آظئ├ء┬ز├ء┬ش├ء┬د├ء┬▒├آ┼ب/├ء┬د├آظئ├آظخ├ء┬╡├آظب├ء┬╣├آ┼ب',
  `category` varchar(100) DEFAULT '├آظخ├آظب├ء┬ز├ء┬ش├ء┬د├ء┬ز ├ء┬ز├ء┬د├آظخ├ء┬ر' COMMENT '├ء┬ز├ء┬╡├آظب├آ┼ب├آ┬ ├ء┬د├آظئ├ء┬╡├آظب├آ┬',
  `unit` varchar(50) NOT NULL DEFAULT '├آ╞ْ├ء┬▒├ء┬ز├آ╦├آظب' COMMENT '├آ╦├ء┬ص├ء┬»├ء┬ر ├ء┬د├آظئ├آظأ├آ┼ب├ء┬د├ء┬│',
  `unit_type` varchar(10) NOT NULL DEFAULT 'COUNT',
  `default_sale_price` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '├ء┬│├ء┬╣├ء┬▒ ├ء┬د├آظئ├ء┬ذ├آ┼ب├ء┬╣ ├ء┬د├آظئ├ء┬د├آ┬├ء┬ز├ء┬▒├ء┬د├ء┬╢├آ┼ب',
  `unit_cost` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '├ء┬ز├آ╞ْ├آظئ├آ┬├ء┬ر ├ء┬د├آظئ├آ╦├ء┬ص├ء┬»├ء┬ر ├ء┬د├آظئ├آظخ├ء┬╣├آ┼ب├ء┬د├ء┬▒├آ┼ب├ء┬ر',
  `current_stock` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '├ء┬د├آظئ├ء┬▒├ء┬╡├آ┼ب├ء┬» ├ء┬د├آظئ├آظخ├ء┬«├ء┬▓├آظب├آ┼ب ├ء┬د├آظئ├ء┬ص├ء┬د├آظئ├آ┼ب',
  `inventory_account` varchar(20) NOT NULL DEFAULT '1210301' COMMENT '├ء┬ص├ء┬│├ء┬د├ء┬ذ ├آظخ├ء┬«├ء┬▓├آظب ├ء┬د├آظئ├ء┬╡├آظب├آ┬ ├ء┬د├آظئ├آ┬├ء┬▒├ء┬╣├آ┼ب',
  `sales_revenue_account` varchar(20) NOT NULL DEFAULT '410101' COMMENT '├ء┬ص├ء┬│├ء┬د├ء┬ذ ├ء┬ح├آ┼ب├ء┬▒├ء┬د├ء┬» ├آظخ├ء┬ذ├آ┼ب├ء┬╣├ء┬د├ء┬ز ├ء┬د├آظئ├ء┬╡├آظب├آ┬',
  `cogs_account` varchar(20) NOT NULL DEFAULT '510101' COMMENT '├ء┬ص├ء┬│├ء┬د├ء┬ذ ├ء┬ز├آ╞ْ├آظئ├آ┬├ء┬ر ├آظخ├ء┬ذ├آ┼ب├ء┬╣├ء┬د├ء┬ز ├ء┬د├آظئ├ء┬╡├آظب├آ┬ (COGS)',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `conversion_factor` decimal(15,4) NOT NULL DEFAULT 1.0000,
  `min_stock_level` decimal(15,4) NOT NULL DEFAULT 0.0000,
  `expiry_date` date DEFAULT NULL,
  `batch_no` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`item_code`),
  KEY `fk_item_inv_acc` (`inventory_account`),
  CONSTRAINT `fk_item_inv_acc` FOREIGN KEY (`inventory_account`) REFERENCES `chart_of_accounts` (`account_code`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='├ء┬»├آظئ├آ┼ب├آظئ ├ء┬د├آظئ├ء┬ث├ء┬╡├آظب├ء┬د├آ┬ ├آ╦├ء┬د├آظئ├آظخ├ء┬«├ء┬▓├آ╦├آظب';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `inventory_movements` (
  `movement_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `document_number` varchar(50) DEFAULT NULL,
  `movement_type` varchar(20) DEFAULT NULL,
  `item_code` varchar(50) DEFAULT NULL,
  `item_name` varchar(255) DEFAULT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `unit_cost` decimal(15,2) DEFAULT NULL,
  `inventory_account` varchar(20) DEFAULT NULL,
  `counter_account` varchar(20) DEFAULT NULL,
  `receiver` varchar(255) DEFAULT NULL,
  `deliverer` varchar(255) DEFAULT NULL,
  `narration` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`movement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `journal_entries` (
  `entry_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `entry_number` varchar(50) NOT NULL COMMENT '├ء┬▒├آظأ├آظخ ├ء┬د├آظئ├آظأ├آ┼ب├ء┬» ├ء┬د├آظئ├آ┼ب├آ╦├آظخ├آ┼ب (├آظخ├ء┬س├ء┬د├آظئ: JV-2026-001)',
  `entry_date` date NOT NULL COMMENT '├ء┬ز├ء┬د├ء┬▒├آ┼ب├ء┬« ├ء┬د├آظئ├آظأ├آ┼ب├ء┬»',
  `reference_doc` varchar(50) DEFAULT NULL COMMENT '├ء┬د├آظئ├آظخ├ء┬▒├ء┬ش├ء┬╣ (├ء┬▒├آظأ├آظخ ├ء┬د├آظئ├آ┬├ء┬د├ء┬ز├آ╦├ء┬▒├ء┬ر ├ء┬ث├آ╦ ├ء┬د├آظئ├آظخ├ء┬▒├ء┬ز├ء┬ش├ء┬╣)',
  `source_module` enum('SALES','SALES_RETURN','TREASURY','MANUAL','INVENTORY') NOT NULL DEFAULT 'SALES_RETURN',
  `narration` text NOT NULL COMMENT '├ء┬د├آظئ├ء┬ذ├آ┼ب├ء┬د├آظب ├آ╦├ء┬د├آظئ├ء┬┤├ء┬▒├ء┬ص ├ء┬د├آظئ├آظخ├ء┬ص├ء┬د├ء┬│├ء┬ذ├آ┼ب ├آظئ├آظئ├آظأ├آ┼ب├ء┬»',
  `total_debit` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '├ء┬ح├ء┬ش├آظخ├ء┬د├آظئ├آ┼ب ├ء┬د├آظئ├آظخ├ء┬»├آ┼ب├آظب',
  `total_credit` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '├ء┬ح├ء┬ش├آظخ├ء┬د├آظئ├آ┼ب ├ء┬د├آظئ├ء┬»├ء┬د├ء┬خ├آظب',
  `is_balanced` tinyint(1) GENERATED ALWAYS AS (`total_debit` = `total_credit`) STORED COMMENT '├ء┬ص├ء┬د├آظئ├ء┬ر ├ء┬د├ء┬ز├ء┬▓├ء┬د├آظب ├ء┬د├آظئ├آظأ├آ┼ب├ء┬»',
  `posted_by` varchar(100) DEFAULT '├ء┬د├آظئ├آظب├ء┬╕├ء┬د├آظخ ├ء┬د├آظئ├ء┬ت├آظئ├آ┼ب',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`entry_id`),
  UNIQUE KEY `entry_number` (`entry_number`),
  KEY `idx_entry_date` (`entry_date`),
  KEY `idx_ref_doc` (`reference_doc`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='├ء┬▒├ء┬ث├ء┬│ ├ء┬│├آظب├ء┬» ├آظأ├آ┼ب├ء┬» ├ء┬د├آظئ├آ┼ب├آ╦├آظخ├آ┼ب├ء┬ر ├ء┬د├آظئ├ء┬╣├ء┬د├آظخ├ء┬ر';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `journal_entry_lines` (
  `line_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `entry_id` bigint(20) NOT NULL,
  `account_code` varchar(20) NOT NULL COMMENT '├ء┬د├آظئ├ء┬ص├ء┬│├ء┬د├ء┬ذ ├ء┬د├آظئ├آ┬├ء┬▒├ء┬╣├آ┼ب',
  `line_narration` varchar(255) DEFAULT NULL COMMENT '├ء┬┤├ء┬▒├ء┬ص ├ء┬د├آظئ├ء┬│├ء┬╖├ء┬▒',
  `debit_amount` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '├ء┬د├آظئ├آظخ├ء┬ذ├آظئ├ء┬║ ├ء┬د├آظئ├آظخ├ء┬»├آ┼ب├آظب',
  `credit_amount` decimal(18,4) NOT NULL DEFAULT 0.0000 COMMENT '├ء┬د├آظئ├آظخ├ء┬ذ├آظئ├ء┬║ ├ء┬د├آظئ├ء┬»├ء┬د├ء┬خ├آظب',
  PRIMARY KEY (`line_id`),
  KEY `fk_line_entry` (`entry_id`),
  KEY `fk_line_acc` (`account_code`),
  CONSTRAINT `fk_line_acc` FOREIGN KEY (`account_code`) REFERENCES `chart_of_accounts` (`account_code`) ON UPDATE CASCADE,
  CONSTRAINT `fk_line_entry` FOREIGN KEY (`entry_id`) REFERENCES `journal_entries` (`entry_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='├ء┬ز├آ┬├ء┬د├ء┬╡├آ┼ب├آظئ ├آ╦├ء┬│├ء┬╖├آ╦├ء┬▒ ├آظأ├آ┼ب├آ╦├ء┬» ├ء┬د├آظئ├آ┼ب├آ╦├آظخ├آ┼ب├ء┬ر';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `material_issue_notes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `issue_code` varchar(50) DEFAULT NULL,
  `wip_account` varchar(20) DEFAULT NULL,
  `raw_material_account` varchar(20) DEFAULT NULL,
  `total_amount` double DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `is_posted` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `overhead_closings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `closing_code` varchar(50) DEFAULT NULL,
  `actual_account` varchar(20) DEFAULT NULL,
  `applied_account` varchar(20) DEFAULT NULL,
  `cogs_account` varchar(20) DEFAULT NULL,
  `actual_amount` double DEFAULT NULL,
  `applied_amount` double DEFAULT NULL,
  `month_year` varchar(20) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `is_posted` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `party_delegates` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `party_code` varchar(20) NOT NULL,
  `delegate_name` varchar(255) NOT NULL,
  `job_title` varchar(255) DEFAULT NULL,
  `authorization_doc_path` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `party_code` (`party_code`),
  CONSTRAINT `party_delegates_ibfk_1` FOREIGN KEY (`party_code`) REFERENCES `business_parties` (`code`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `production_details` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `production_order` varchar(50) NOT NULL,
  `item_code` varchar(50) NOT NULL,
  `item_name` varchar(255) NOT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `unit_cost` decimal(15,2) NOT NULL,
  `total_cost` decimal(15,2) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_pd_order` (`production_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `purchase_invoice_details` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `invoice_code` varchar(50) NOT NULL,
  `item_code` varchar(50) NOT NULL,
  `item_name` varchar(255) NOT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `unit_cost` decimal(15,2) NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_pid_invoice` (`invoice_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `purchase_invoices` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `invoice_code` varchar(50) DEFAULT NULL,
  `inventory_account` varchar(20) DEFAULT NULL,
  `supplier_account` varchar(20) DEFAULT NULL,
  `input_tax_account` varchar(20) DEFAULT NULL,
  `amount` decimal(18,4) DEFAULT NULL,
  `tax_amount` decimal(18,4) DEFAULT NULL,
  `total_amount` decimal(18,4) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `invoice_code` (`invoice_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `purchase_return_invoices` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `return_code` varchar(50) DEFAULT NULL,
  `inventory_account` varchar(20) DEFAULT NULL,
  `supplier_account` varchar(20) DEFAULT NULL,
  `input_tax_account` varchar(20) DEFAULT NULL,
  `amount` decimal(18,4) DEFAULT NULL,
  `tax_amount` decimal(18,4) DEFAULT NULL,
  `total_amount` decimal(18,4) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `return_code` (`return_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `purchase_return_notes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `return_code` varchar(50) DEFAULT NULL,
  `supplier_account` varchar(20) DEFAULT NULL,
  `raw_material_account` varchar(20) DEFAULT NULL,
  `total_amount` double DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sales_invoice_details` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `invoice_code` varchar(50) NOT NULL,
  `item_code` varchar(50) NOT NULL,
  `item_name` varchar(255) NOT NULL,
  `quantity` decimal(12,3) NOT NULL,
  `unit_price` decimal(15,2) NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_sid_invoice` (`invoice_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sales_invoices` (
  `invoice_code` varchar(50) NOT NULL COMMENT '├ء┬▒├آظأ├آظخ ├آ┬├ء┬د├ء┬ز├آ╦├ء┬▒├ء┬ر ├ء┬د├آظئ├آظخ├ء┬ذ├آ┼ب├ء┬╣├ء┬د├ء┬ز (├آظخ├ء┬س├ء┬د├آظئ: INV-1001)',
  `invoice_date` date NOT NULL COMMENT '├ء┬ز├ء┬د├ء┬▒├آ┼ب├ء┬« ├ء┬د├آظئ├آ┬├ء┬د├ء┬ز├آ╦├ء┬▒├ء┬ر',
  `customer_account` varchar(20) NOT NULL COMMENT '├ء┬ص├ء┬│├ء┬د├ء┬ذ ├ء┬د├آظئ├ء┬╣├آظخ├آ┼ب├آظئ (123020001)',
  `sales_revenue_account` varchar(20) NOT NULL DEFAULT '410101' COMMENT '├ء┬ص├ء┬│├ء┬د├ء┬ذ ├ء┬د├آظئ├ء┬ح├آ┼ب├ء┬▒├ء┬د├ء┬»',
  `tax_account` varchar(20) NOT NULL DEFAULT '220301' COMMENT '├ء┬ص├ء┬│├ء┬د├ء┬ذ ├ء┬╢├ء┬▒├آ┼ب├ء┬ذ├ء┬ر ├ء┬د├آظئ├آظخ├ء┬ذ├آ┼ب├ء┬╣├ء┬د├ء┬ز',
  `finished_goods_account` varchar(20) NOT NULL DEFAULT '1210301' COMMENT '├ء┬ص├ء┬│├ء┬د├ء┬ذ ├آظخ├ء┬«├ء┬▓├آظب ├ء┬د├آظئ├آظخ├آظب├ء┬ز├ء┬ش├ء┬د├ء┬ز ├ء┬د├آظئ├ء┬ز├ء┬د├آظخ├ء┬ر',
  `cogs_account` varchar(20) NOT NULL DEFAULT '510101' COMMENT '├ء┬ص├ء┬│├ء┬د├ء┬ذ ├ء┬ز├آ╞ْ├آظئ├آ┬├ء┬ر ├ء┬د├آظئ├ء┬ذ├ء┬╢├ء┬د├ء┬╣├ء┬ر ├ء┬د├آظئ├آظخ├ء┬ذ├ء┬د├ء┬╣├ء┬ر',
  `subtotal_amount` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '├ء┬د├آظئ├آظخ├ء┬ذ├آظئ├ء┬║ ├ء┬د├آظئ├ء┬╡├ء┬د├آ┬├آ┼ب ├آظأ├ء┬ذ├آظئ ├ء┬د├آظئ├ء┬╢├ء┬▒├آ┼ب├ء┬ذ├ء┬ر',
  `tax_rate` decimal(5,4) NOT NULL DEFAULT 0.1500 COMMENT '├آظب├ء┬│├ء┬ذ├ء┬ر ├ء┬د├آظئ├ء┬╢├ء┬▒├آ┼ب├ء┬ذ├ء┬ر (├آظخ├ء┬س├ء┬د├آظئ: 0.15)',
  `tax_amount` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '├آظخ├ء┬ذ├آظئ├ء┬║ ├ء┬د├آظئ├ء┬╢├ء┬▒├آ┼ب├ء┬ذ├ء┬ر ├ء┬د├آظئ├آظخ├ء┬ص├ء┬ز├ء┬│├ء┬ذ',
  `total_invoice_amount` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '├ء┬ح├ء┬ش├آظخ├ء┬د├آظئ├آ┼ب ├ء┬د├آظئ├آ┬├ء┬د├ء┬ز├آ╦├ء┬▒├ء┬ر ├آظخ├ء┬╣ ├ء┬د├آظئ├ء┬╢├ء┬▒├آ┼ب├ء┬ذ├ء┬ر',
  `inventory_cost_amount` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '├ء┬ح├ء┬ش├آظخ├ء┬د├آظئ├آ┼ب ├ء┬ز├آ╞ْ├آظئ├آ┬├ء┬ر ├ء┬د├آظئ├آظخ├ء┬«├ء┬▓├آ╦├آظب (COGS)',
  `payment_terms` varchar(50) DEFAULT '├ء┬ت├ء┬ش├آظئ 30 ├آ┼ب├آ╦├آظخ' COMMENT '├ء┬┤├ء┬▒├آ╦├ء┬╖ ├ء┬د├آظئ├ء┬│├ء┬»├ء┬د├ء┬»',
  `notes` text DEFAULT NULL COMMENT '├آظخ├آظئ├ء┬د├ء┬ص├ء┬╕├ء┬د├ء┬ز ├ء┬ح├ء┬╢├ء┬د├آ┬├آ┼ب├ء┬ر',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`invoice_code`),
  UNIQUE KEY `uq_invoice_code` (`invoice_code`),
  KEY `idx_cust_inv` (`customer_account`),
  CONSTRAINT `fk_inv_cust` FOREIGN KEY (`customer_account`) REFERENCES `chart_of_accounts` (`account_code`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='├آ┬├آ╦├ء┬د├ء┬ز├آ┼ب├ء┬▒ ├ء┬د├آظئ├آظخ├ء┬ذ├آ┼ب├ء┬╣├ء┬د├ء┬ز ├ء┬د├آظئ├ء┬╡├ء┬د├ء┬»├ء┬▒├ء┬ر';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sales_return_invoices` (
  `return_code` varchar(50) NOT NULL COMMENT '├ء┬▒├آظأ├آظخ ├آ┬├ء┬د├ء┬ز├آ╦├ء┬▒├ء┬ر ├ء┬د├آظئ├آظخ├ء┬▒├ء┬ز├ء┬ش├ء┬╣ (├آظخ├ء┬س├ء┬د├آظئ: SRI-1001)',
  `original_invoice_code` varchar(50) DEFAULT '├آظخ├ء┬ذ├ء┬د├ء┬┤├ء┬▒ ├ء┬ذ├ء┬»├آ╦├آظب ├آ┬├ء┬د├ء┬ز├آ╦├ء┬▒├ء┬ر' COMMENT '├ء┬▒├آظأ├آظخ ├ء┬د├آظئ├آ┬├ء┬د├ء┬ز├آ╦├ء┬▒├ء┬ر ├ء┬د├آظئ├ء┬ث├ء┬╡├آظئ├آ┼ب├ء┬ر',
  `return_date` date NOT NULL COMMENT '├ء┬ز├ء┬د├ء┬▒├آ┼ب├ء┬« ├ء┬د├آظئ├آظخ├ء┬▒├ء┬ز├ء┬ش├ء┬╣',
  `customer_account` varchar(20) NOT NULL COMMENT '├ء┬ص├ء┬│├ء┬د├ء┬ذ ├ء┬د├آظئ├ء┬╣├آظخ├آ┼ب├آظئ ├ء┬د├آظئ├ء┬»├ء┬د├ء┬خ├آظب (123020001)',
  `sales_return_account` varchar(20) NOT NULL DEFAULT '410201' COMMENT '├ء┬ص├ء┬│├ء┬د├ء┬ذ ├آظخ├ء┬▒├ء┬»├آ╦├ء┬»├ء┬د├ء┬ز ├ء┬د├آظئ├آظخ├ء┬ذ├آ┼ب├ء┬╣├ء┬د├ء┬ز ├ء┬د├آظئ├آظخ├ء┬»├آ┼ب├آظب',
  `tax_account` varchar(20) NOT NULL DEFAULT '220301' COMMENT '├ء┬ص├ء┬│├ء┬د├ء┬ذ ├ء┬╢├ء┬▒├آ┼ب├ء┬ذ├ء┬ر ├ء┬د├آظئ├آظخ├ء┬ذ├آ┼ب├ء┬╣├ء┬د├ء┬ز ├ء┬د├آظئ├آظخ├ء┬»├آ┼ب├آظب ├ء┬د├آظئ├آظخ├ء┬│├ء┬ز├ء┬▒├ء┬»',
  `finished_goods_account` varchar(20) NOT NULL DEFAULT '1210301' COMMENT '├ء┬ص├ء┬│├ء┬د├ء┬ذ ├آظخ├ء┬«├ء┬▓├آظب ├ء┬د├آظئ├ء┬ز├ء┬د├آظخ ├ء┬د├آظئ├آظخ├ء┬│├ء┬ز├ء┬▒├ء┬» ├ء┬د├آظئ├آظخ├ء┬»├آ┼ب├آظب',
  `cogs_account` varchar(20) NOT NULL DEFAULT '510101' COMMENT '├ء┬ص├ء┬│├ء┬د├ء┬ذ ├ء┬ز├آ╞ْ├آظئ├آ┬├ء┬ر ├ء┬د├آظئ├آظخ├ء┬ذ├آ┼ب├ء┬╣├ء┬د├ء┬ز ├ء┬د├آظئ├آظخ├ء┬«├آ┬├ء┬╢ ├ء┬د├آظئ├ء┬»├ء┬د├ء┬خ├آظب',
  `return_amount` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '├آظأ├آ┼ب├آظخ├ء┬ر ├ء┬د├آظئ├آظخ├ء┬▒├ء┬ز├ء┬ش├ء┬╣ ├ء┬د├آظئ├ء┬ث├ء┬│├ء┬د├ء┬│├آ┼ب├ء┬ر',
  `is_tax_applied` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1 = ├ء┬«├ء┬د├ء┬╢├ء┬╣ ├آظئ├آظئ├ء┬╢├ء┬▒├آ┼ب├ء┬ذ├ء┬ر├ء┼ْ 0 = ├آظخ├ء┬╣├آ┬├آظ░',
  `tax_rate` decimal(5,4) NOT NULL DEFAULT 0.1500 COMMENT '├آظب├ء┬│├ء┬ذ├ء┬ر ├ء┬د├آظئ├ء┬╢├ء┬▒├آ┼ب├ء┬ذ├ء┬ر ├ء┬د├آظئ├آظخ├ء┬╖├ء┬ذ├آظأ├ء┬ر',
  `tax_amount` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '├آظخ├ء┬ذ├آظئ├ء┬║ ├ء┬د├آظئ├ء┬╢├ء┬▒├آ┼ب├ء┬ذ├ء┬ر ├ء┬د├آظئ├آظخ├ء┬│├ء┬ز├ء┬▒├ء┬»',
  `total_customer_credit` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '├ء┬ح├ء┬ش├آظخ├ء┬د├آظئ├آ┼ب ├ء┬د├آظئ├آظخ├ء┬│├ء┬ز├ء┬ص├آظأ ├آظئ├ء┬ص├ء┬│├ء┬د├ء┬ذ ├ء┬د├آظئ├ء┬╣├آظخ├آ┼ب├آظئ ├ء┬د├آظئ├ء┬»├ء┬د├ء┬خ├آظب',
  `inventory_cost` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '├ء┬د├آظئ├ء┬ز├آ╞ْ├آظئ├آ┬├ء┬ر ├ء┬د├آظئ├آظخ├ء┬«├ء┬▓├آظب├آ┼ب├ء┬ر ├ء┬د├آظئ├آظخ├ء┬│├ء┬ز├ء┬▒├ء┬»├ء┬ر ├آظئ├آظئ├ء┬ح├آظب├ء┬ز├ء┬د├ء┬ش ├ء┬د├آظئ├ء┬ز├ء┬د├آظخ',
  `return_reason` varchar(255) DEFAULT '├ء┬ز├آظئ├آ┬ ├ء┬ث├ء┬س├آظب├ء┬د├ء┬ة ├ء┬د├آظئ├آظب├آظأ├آظئ ├آ╦├ء┬د├آظئ├ء┬ز├ء┬«├ء┬▓├آ┼ب├آظب' COMMENT '├ء┬│├ء┬ذ├ء┬ذ ├ء┬د├آظئ├ء┬ح├ء┬▒├ء┬ش├ء┬د├ء┬╣ ├ء┬د├آظئ├آ┬├آظب├آ┼ب',
  `batch_no` varchar(50) DEFAULT '---' COMMENT '├ء┬▒├آظأ├آظخ ├ء┬د├آظئ├ء┬ز├ء┬┤├ء┬║├آ┼ب├آظئ├ء┬ر / ├ء┬د├آظئ├ء┬»├آ┬├ء┬╣├ء┬ر ├ء┬د├آظئ├آظخ├ء┬╡├آظب├ء┬╣├آ┼ب├ء┬ر',
  `status` enum('DRAFT','POSTED','CANCELLED') NOT NULL DEFAULT 'POSTED' COMMENT '├ء┬ص├ء┬د├آظئ├ء┬ر ├ء┬د├آظئ├ء┬ز├ء┬▒├ء┬ص├آ┼ب├آظئ ├ء┬د├آظئ├آظخ├ء┬ص├ء┬د├ء┬│├ء┬ذ├آ┼ب',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`return_code`),
  KEY `idx_ret_cust` (`customer_account`),
  KEY `idx_ret_date` (`return_date`),
  CONSTRAINT `fk_ret_cust` FOREIGN KEY (`customer_account`) REFERENCES `chart_of_accounts` (`account_code`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='├آ┬├آ╦├ء┬د├ء┬ز├آ┼ب├ء┬▒ ├آ╦├ء┬ث├ء┬░├آ╦├آظب ├آظخ├ء┬▒├ء┬»├آ╦├ء┬»├ء┬د├ء┬ز ├آ╦├آظخ├ء┬│├آظخ├آ╦├ء┬ص├ء┬د├ء┬ز ├ء┬د├آظئ├آظخ├ء┬ذ├آ┼ب├ء┬╣├ء┬د├ء┬ز';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sales_return_notes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `return_code` varchar(50) NOT NULL,
  `customer_account` varchar(20) NOT NULL,
  `sales_return_account` varchar(20) NOT NULL,
  `finished_goods_account` varchar(20) NOT NULL,
  `cogs_account` varchar(20) NOT NULL,
  `total_amount` decimal(15,2) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `return_code` (`return_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stock_alerts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `item_code` varchar(50) DEFAULT NULL,
  `item_name` varchar(100) DEFAULT NULL,
  `current_stock` double DEFAULT NULL,
  `min_stock` double DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_item_code` (`item_code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `treasury_vouchers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `voucher_code` varchar(50) DEFAULT NULL,
  `account_code` varchar(20) DEFAULT NULL,
  `amount` double DEFAULT NULL,
  `voucher_type` varchar(20) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `voucher_number` varchar(50) DEFAULT NULL,
  `voucher_date` date DEFAULT NULL,
  `reference_name` varchar(255) DEFAULT NULL,
  `narration` text DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_voucher_code` (`voucher_code`),
  UNIQUE KEY `uq_voucher_number` (`voucher_number`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `van_return_notes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `return_code` varchar(50) DEFAULT NULL,
  `finished_goods_account` varchar(20) DEFAULT NULL,
  `van_account` varchar(20) DEFAULT NULL,
  `total_amount` double DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `quantity` double DEFAULT NULL,
  `unit_cost` double DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `van_transfer_notes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `transfer_code` varchar(50) DEFAULT NULL,
  `van_account` varchar(20) DEFAULT NULL,
  `finished_goods_account` varchar(20) DEFAULT NULL,
  `total_amount` double DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `quantity` double DEFAULT NULL,
  `unit_cost` double DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
