# PROJECT_MAP — ERP المصنع (E:\ERP1)

خريطة تشغيلية للجلسات القادمة. القاعدة الحية = مصدر الحقيقة الوحيد المعتمد. لا تُنفَّذ أي معاملة اتصال قبل قراءة هذا الملف وفحص `git log --oneline -5` وأي ملف جديد باسم M*.

## [TECH_STACK]
- Java Swing على ويندوز (javac/java على PATH، لا build system — `run.bat` يترجم ويشغّل).
- اتصال: `mysql-connector-j-8.3.0.jar` في جذر المشروع.
- قاعدة: MariaDB 10.4.32 (XAMPP) على localhost:3306، مستخدم `root` بكلمة فارغة، قاعدة `erp_factory_db`. يُدار الاتصال عبر `DatabaseManager.getConnection()`.
- الاختبار الآلي: editing ملف في `%TEMP%\opencode\m4test\` ثم javac مع ملفات E:\ERP1.

## [SYSTEM_FLOW]
`run.bat` → `MainWindow` → `initializeDatabase() + initializeParties()` (اتصال فقط — DDL مُعطَّل داخل `if (false)`) ثم `DatabaseAutoMigration.run()` (تحديث حسابات القاعدة فقط) → شاشات الوحدات (SalesModuleFrame وغيره).

مبيعات (المسار الفعلي الوحيد المربوط بواجهة المستخدم):
`SalesInvoiceForm` → `SalesPostingService.postSale(...)` → ترحيل موحد بمعاملة واحدة عبر `PostingEngine.postJournalEntry(conn, entry, tx)`:
- رأس `journal_entries(entry_number UNIQUE, entry_date, reference_doc, source_module, narration, total_debit, total_credit, posted_by='النظام الآلي')` + `getGeneratedKeys()` → ثم `journal_entry_lines(entry_id, account_code, line_narration, debit_amount, credit_amount)` + تحديث `chart_of_accounts.current_balance` (أصول/مصاريف 1x/5x تزيد بالمدين).
- حركة مخزون عبر `InventoryPostingService`: إدراج `inventory_movements(document_number, movement_type[RECEIPT/ISSUE/SALE], ...)` + تعديل `inventory_items.current_stock` (حارس رصيد غير سالب).
- مردودات: `SalesReturnInvoiceForm` → `SalesPostingService/SalesReturnInvoice` → نفس المسار + `sales_return_notes`.
- منع الإدراج المكرر: `SELECT COUNT(*) FROM journal_entries WHERE entry_number = ?`.

## [ARCHITECTURE]
- `DatabaseManager.java`: مركز الاتصال + CRUD + تحقق الحسابات (`validateSubAccount`, `isSubAccount`) + `insertSalesInvoice`/`printSalesReport` (مُحاذَيان للأعمدة الحية لكن غير مُستدعَيَن). كود DDL القديم داخل `if (false)` (استرجاعه من git tag عند الحاجة).
- `DatabaseAutoMigration.java`: upsert حسابات الغلاصم القياسية عند كل إقلاع + إعادة بناء index على inventory_movements (تعديل جسيم لكنه متناسق).
- `schema.sql`: المرجع الوحيد للبنية — Idempotent، "CREATE ... IF NOT EXISTS" بلا "DROP TABLE"، يتضمن 15 جدولاً تشغيلياً + seed data. يُشغَّل يدوياً للقواعد الفارغة فقط.
- `PostingEngine.java`: الترحيل القياسي الوحيد (متزن + قيد مفيض عن الخطأ). حساب nontrivial: is_balanced/entry_id عبر مفاتيح مولّدة.
- `AccountValidator`: يحجب الترحيل إلى حسابات أب (غير طرفية).
- `DatabaseManager` العتاد للواجهات Legacy الفردية — لا يُستخدم من الشاشات الحالية.

## [ORPHANS & PENDING]
1. **فواتير المبيعات لا تُحفظ في `sales_invoices`** — حتى `postSale` الحقيقي يكتب قيداً + حركة مخزون فقط، فلا تظهر الفواتير في تقارير المبيعات (`insertSalesInvoice` مُحاذى لكنه dead). PENDING: إضافة حفظ رأس + أسطر `sales_invoices/sales_invoice_details` داخل معاملة `postSale`.
2. `SalesInvoice.java` (شاشة قديمة). تم إصلاح مساره اليدوي (journal بتوقيع جديد) لكنه لم يكتب `sales_invoices` أيضاً وغير مربوط بأزرار الواجهة.
3. `duplicate key` المحتمل: `entry_number` فريد؛ لو أُعيد الاعتماد لنفس المستند يُرفض الإدراج عمداً (حماية) — غيّر رقم المستند/احذف القيد السابق.
4. `DatabaseAutoMigration.run()` يعدّل جسدياً بعض الجداول كل إقلاع (إعادة بناء index) — متناسق مع الأعمدة الحية، لكن يمكن لاحقاً تقييده بإصدار.
5. الكود داخل `if (false)` في `DatabaseManager` قابل للاسترجاع من `git tag backup-before-schema-fix-20260905-011306` ولا يؤثر على التشغيل.
6. الخادم MariaDB 10.4 وموصل MySQL 8 — متوافقان عملياً، لا تغيير مطلوب.
7. `DatabaseAdminDialog` يشير لجداول غير موجودة (items/exchange_rates) → رسالة تحذير تجميلية فقط.
8. فواتير المبيعات القديمة التشاركية المرتبطة برقم المستند الواحد تتطلب معالجة على مستوى app (لم تُلمس).

> أي تعديل مستقبلي للـ schema: اثبِت توافقه مع `live_schema.sql` (التقط منه واحداً جديداً عند الحاجة) وتجنّب `DROP` نهائياً في التعليمات البرمجية.