$sp='C:\Users\ultra\AppData\Local\Temp\claude\E--claude-mods-overcraft\6635df67-fe58-47ac-a0d2-4067e7d195cf\scratchpad'
$rr='E:\claude\mods\overcraft_railgun\techniques\testing\runs\railgun'
$env:RG_TAG='final'
if (Get-Process Overwatch, Battle.net -ErrorAction SilentlyContinue) { "OVERWATCH RUNNING"; exit 3 }
py -I "$sp\rg_oc.py" "$sp\rg_extra.py" "$sp\rg2_final_x.txt" damage reloadend hardstop more 2>&1 | Out-Null
"extra done"
py -I "$sp\rg_oc.py" "$sp\rg2_8.py" "$sp\rg2_final.txt" 2>&1 | Out-Null
"8 done"
py -I "$sp\rg_oc.py" "$sp\rg2.py" "$sp\rg2_final.txt" clip shots rig soundpoll hold holdammo buffer 2>&1 | Out-Null
"rg2 a done"
py -I "$sp\rg_oc.py" "$sp\rg2.py" "$sp\rg2_final.txt" lag lag4 lag5 lag5c 2>&1 | Out-Null
"rg2 lag done"
py -I "$sp\rg_oc.py" "$sp\rg2_5b.py" "$sp\rg2_final.txt" 2>&1 | Out-Null
"5b done"
if (Get-Process Overwatch, Battle.net -ErrorAction SilentlyContinue) { "OVERWATCH RUNNING"; exit 3 }
py -I "$sp\rg_oc.py" "$rr\rk_rig.py" "$sp\rg2_final_flick.txt" rig 50 2>&1 | Out-Null
py -I "$sp\rg_oc.py" "$rr\rk_flick.py" "$sp\rg2_final_flick.txt" "$rr\rk_mouse.ps1" flick flick flickhuman flickwait 2>&1 | Out-Null
"flick done"
"ALL DONE"
