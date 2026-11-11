$sp='C:\Users\ultra\AppData\Local\Temp\claude\E--claude-mods-overcraft\6635df67-fe58-47ac-a0d2-4067e7d195cf\scratchpad'
if (Get-Process Overwatch, Battle.net -ErrorAction SilentlyContinue) { "OVERWATCH RUNNING"; exit 3 }
& "$sp\rg_up.ps1"
$env:RG_ART='1'
py -I "$sp\rg_oc.py" "$sp\rg4_pick.py" "$sp\rg5_pick.txt" 2>&1 | Out-Null
Remove-Item Env:RG_ART
$env:RG_PAY='partial'
py -I "$sp\rg_oc.py" "$sp\rg4.py" "$sp\rg5.txt" bank peek mash qrep 2>&1 | Out-Null
Remove-Item Env:RG_PAY
& "$sp\rg_stop.ps1"
"ALL DONE"
