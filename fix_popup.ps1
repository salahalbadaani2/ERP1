$p='E:\ERP1\AccountTreeDialog.java'
$t=Get-Content -LiteralPath $p -Raw -Encoding UTF8
$t2=$t -replace 'if \(managementMode && e\.getClickCount\(\) == 1 && !e\.isPopupTrigger\(\)\) \{\s*showActions\(e\);\s*\}', ''
if($t2 -ne $t){
  $utf8NoBom=New-Object System.Text.UTF8Encoding $false
  [System.IO.File]::WriteAllText($p, $t2, $utf8NoBom)
  Write-Output 'FIXED_POPUP'
} else {
  Write-Output 'NOT_FOUND'
}
