$sp='C:\Users\ultra\AppData\Local\Temp\claude\E--claude-mods-overcraft\6635df67-fe58-47ac-a0d2-4067e7d195cf\scratchpad'
function Clear-OW { if (Get-Process Overwatch, Battle.net -ErrorAction SilentlyContinue) { "OVERWATCH RUNNING: stopped"; & "$sp\rg_stop.ps1"; exit 3 } }
Clear-OW
& "$sp\rg_up.ps1"
py -I "$sp\rg_oc.py" "$sp\rg3_by.py" "$sp\rg3_after_by.txt" 2>&1 | Out-Null
"by done"
& "$sp\rg_stop.ps1"
Clear-OW
& "$sp\rg_up.ps1" -Pack 'E:/claude/mods/overcraft_ana/local-assets/overcraft-assets.zip'
py -I "$sp\rg_oc.py" "$sp\rg2.py" "$sp\rg3_after_zip.txt" first 2>&1 | Out-Null
Select-String -Path E:\claude\mods\overcraft_railgun\run\logs\latest.log -Pattern 'asset pack|Effect art|Effect texture|zip file closed|Exception' | Select-Object -First 12 | ForEach-Object { $_.Line.Substring(0,[Math]::Min(170,$_.Line.Length)) } | Out-File -Encoding utf8 "$sp\rg3_after_zip_log.txt"
"zip done"
& "$sp\rg_stop.ps1"
"ALL DONE"
