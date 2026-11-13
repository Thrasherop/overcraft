$sp='C:\Users\ultra\AppData\Local\Temp\claude\E--claude-mods-overcraft\6635df67-fe58-47ac-a0d2-4067e7d195cf\scratchpad'
function Clear-OW { if (Get-Process Overwatch, Battle.net -ErrorAction SilentlyContinue) { "OVERWATCH RUNNING: stopped"; & "$sp\rg_stop.ps1"; exit 3 } }
Clear-OW
& "$sp\rg_up.ps1"
py -I "$sp\rg_oc.py" "$sp\rg4_pick.py" "$sp\rg4_after_pick.txt" 2>&1 | Out-Null
"pick done"
& "$sp\rg_stop.ps1"
Clear-OW
& "$sp\rg_up.ps1"
py -I "$sp\rg_oc.py" "$sp\rg3_by.py" "$sp\rg4_after_by.txt" 2>&1 | Out-Null
"by done"
& "$sp\rg_stop.ps1"
Clear-OW
& "$sp\rg_up.ps1"
py -I "$sp\rg_oc.py" "$sp\rg2.py" "$sp\rg4_after_first.txt" first 2>&1 | Out-Null
$env:RG_PAY='partial'
py -I "$sp\rg_oc.py" "$sp\rg4.py" "$sp\rg4_after.txt" bank peek garbage turn qrep 2>&1 | Out-Null
"abuse done"
Clear-OW
py -I "$sp\rg_oc.py" "$sp\rg4.py" "$sp\rg4_after.txt" mash flickreal 2>&1 | Out-Null
& "$sp\rg_stop.ps1"
"ALL DONE"
