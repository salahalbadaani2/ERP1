$p='E:\ERP1\BankReconciliationFrame.java'
$t=Get-Content -LiteralPath $p -Raw -Encoding UTF8
# Remove demo blocks - pattern without Arabic in replacement
$pattern='(?s)\}catch\(Exception ex\)\{\s*//.*?\r?\n\s*if\(model\.getRowCount\(\)==0\)\{\s*model\.addRow.*JE-DEMO-1.*?\r?\n\s*model\.addRow.*JE-DEMO-2.*?\r?\n\s*\}\s*\}\s*if\(model\.getRowCount\(\)==0\)\{\s*//.*?\r?\n\s*model\.addRow.*JE-DEMO-1.*?\r?\n\s*model\.addRow.*JE-DEMO-2.*?\r?\n\s*\}'
$replacement="}catch(Exception ex){`r`n            // table remains empty`r`n        }"
$t2=$t -replace $pattern, $replacement
Set-Content -LiteralPath $p -Value $t2 -Encoding UTF8
Write-Output 'DONE'
Select-String -Path $p -Pattern "JE-DEMO" | Measure-Object | ForEach-Object { Write-Output "REMAINING:$($_.Count)" }
