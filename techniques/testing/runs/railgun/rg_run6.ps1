$sp='C:\Users\ultra\AppData\Local\Temp\claude\E--claude-mods-overcraft\6635df67-fe58-47ac-a0d2-4067e7d195cf\scratchpad'
if (Get-Process Overwatch, Battle.net -ErrorAction SilentlyContinue) { "OVERWATCH RUNNING"; exit 3 }
& "$sp\rg_up.ps1"
$env:RG_PAY='partial'
py -I "$sp\rg_oc.py" "$sp\rg4.py" "$sp\rg4_final.txt" bank peek garbage turn qrep mash flickreal 2>&1 | Out-Null
Remove-Item Env:RG_PAY
py -I "$sp\rg_oc.py" "$sp\rg3.py" "$sp\rg4_final_mash3.txt" mash 2>&1 | Out-Null
"mash done"
& "$sp\rg_run5b.ps1"
