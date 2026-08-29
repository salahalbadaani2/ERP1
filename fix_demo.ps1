$p='E:\ERP1\BankReconciliationFrame.java'
$t=Get-Content -LiteralPath $p -Raw -Encoding UTF8
$pattern='(?s)\}catch\(Exception ex\)\{\s*//[^\r\n]*\r?\n\s*if\(model\.getRowCount\(\)==0\)\{\s*model\.addRow.*JE-DEMO-1.*?\r?\n\s*model\.addRow.*JE-DEMO-2.*?\r?\n\s*\}\s*\}\s*if\(model\.getRowCount\(\)==0\)\{\s*//[^\r\n]*\r?\n\s*model\.addRow.*JE-DEMO-1.*?\r?\n\s*model\.addRow.*JE-DEMO-2.*?\r?\n\s*\}'
$replacement="}catch(Exception ex){`r`n            // لا تعرض بيانات وهمية - الجدول يبقى فارغاً`r`n        }"
$t2=$t -replace $pattern, $replacement
if($t2 -ne $t){ Set-Content -LiteralPath $p -Value $t2 -Encoding UTF8; Write-Output 'REPLACED' } else { Write-Output 'NOT_FOUND' }
