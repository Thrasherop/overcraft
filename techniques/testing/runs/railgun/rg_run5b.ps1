$sp='C:\Users\ultra\AppData\Local\Temp\claude\E--claude-mods-overcraft\6635df67-fe58-47ac-a0d2-4067e7d195cf\scratchpad'
$rr='E:\claude\mods\overcraft_railgun\techniques\testing\runs\railgun'
function Clear-OW { if (Get-Process Overwatch, Battle.net -ErrorAction SilentlyContinue) { "OVERWATCH RUNNING: stopped"; & "$sp\rg_stop.ps1"; exit 3 } }
Clear-OW
py -I "$sp\rg_oc.py" "$sp\rg3.py" "$sp\rg4_after_r3.txt" mash repeat stuck hack speed many 2>&1 | Out-Null
"rg3 done"
py -I "$sp\rg_oc.py" "$sp\rg2.py" "$sp\rg4_after_lag.txt" lag lag4 lag5 lag5c 2>&1 | Out-Null
py -I "$sp\rg_oc.py" "$sp\rg2_5b.py" "$sp\rg4_after_lag.txt" 2>&1 | Out-Null
"lag done"
py -I "$sp\rg_oc.py" "$sp\rg2.py" "$sp\rg4_after_buf.txt" buffer 2>&1 | Out-Null
Clear-OW
py -I "$sp\rg_oc.py" "$rr\rk_rig.py" "$sp\rg4_after_flick.txt" rig 50 2>&1 | Out-Null
py -I "$sp\rg_oc.py" "$sp\rg3_flick.py" "$sp\rg4_after_flick.txt" "$rr\rk_mouse.ps1" flick flickhuman flickwait 2>&1 | Out-Null
"flick done"
py -I "$sp\rg_oc.py" "$sp\rg_table.py" "$sp\rg4_after_table.txt" idle reload hold between melee empty emote oc 2>&1 | Out-Null
py -I "$sp\rg_oc.py" "$sp\rg_extra.py" "$sp\rg4_after_x.txt" reloadend hardstop 2>&1 | Out-Null
py -I "$sp\rg_oc.py" "$sp\rg2_stun.py" "$sp\rg4_after_x.txt" 2>&1 | Out-Null
"table done"
& "$sp\rg_stop.ps1"
"ALL DONE"
