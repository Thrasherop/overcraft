# Closes only this worktree's client; removes the lock only if it is the one this session made.
$m = 'C:\Users\ultra\AppData\Local\Temp\claude\14452f9945c49089\6635df67-fe58-47ac-a0d2-4067e7d195cf\scratchpad\grip_lock_mine.txt'
$sel = { Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'fabric.dli.config' -and $_.CommandLine -match 'overcraft_grip' } }
foreach ($x in (& $sel)) { Stop-Process -Id $x.ProcessId -Force -ErrorAction SilentlyContinue }
for ($i = 0; $i -lt 20; $i++) { Start-Sleep 2; $left = & $sel; if (-not $left) { break } }
"my clients left: $($left.ProcessId)"
$l = Get-Item E:\claude\mods\.oc_client_lock -ErrorAction SilentlyContinue
if (-not $left -and $l -and (Test-Path $m) -and ((Get-Content $m).Trim() -eq "$($l.CreationTimeUtc.Ticks)")) { Remove-Item E:\claude\mods\.oc_client_lock; Remove-Item $m; "released mine" } else { "lock not removed: not mine, or none" }
