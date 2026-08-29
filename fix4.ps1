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
      $i=$nextIdx
      continue
    }
  }
  $new+=$line
}
$joined = $new -join "`r`n"
$joined = $joined -replace '\}catch\(Exception ex\)\{\s*\}', "}catch(Exception ex){`r`n            // table remains empty`r`n        }"
Set-Content -LiteralPath $p -Value $joined -Encoding UTF8
Write-Output 'CLEANED'
Select-String -Path $p -Pattern "JE-DEMO" | Measure-Object | ForEach-Object { Write-Output "REMAINING:$($_.Count)" }
