$p='E:\ERP1\BankReconciliationFrame.java'
$t=Get-Content -LiteralPath $p -Encoding UTF8
$new=@()
for($i=0; $i -lt $t.Count; $i++){
  $line=$t[$i]
  if($line -match 'JE-DEMO'){ continue }
  if($line -match 'عينات'){ continue }
  if($line -match 'if\(model\.getRowCount\(\)==0\)'){
    $nextIdx=$i+1
    while($nextIdx -lt $t.Count -and $t[$nextIdx].Trim() -eq ""){ $nextIdx++ }
    if($nextIdx -lt $t.Count -and $t[$nextIdx].Trim() -eq "}"){
      $hasDemo=$false
      # Check if this if block originally had demo data (we already removed those lines, so now it's empty)
      # If empty, skip both
      $i=$nextIdx
      continue
    }
  }
  $new+=$line
}
# Ensure catch block has comment if empty
$joined = $new -join "`r`n"
# Replace empty catch with comment
$joined = $joined -replace '\}catch\(Exception ex\)\{\s*\}', "}catch(Exception ex){`r`n            // لا تعرض بيانات وهمية - الجدول يبقى فارغاً`r`n        }"
Set-Content -LiteralPath $p -Value $joined -Encoding UTF8
Write-Output 'CLEANED'
Select-String -Path $p -Pattern "JE-DEMO" | Measure-Object | ForEach-Object { Write-Output "REMAINING:$($_.Count)" }
