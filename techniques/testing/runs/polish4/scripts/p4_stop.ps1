# Closes only my clients (this worktree's, and my old-commit one). Removes the lock only if it's the one my launch made: its creation time is what the launch wrote.
$m = 'C:\Users\ultra\AppData\Local\Temp\claude\E--claude-mods-overcraft\6635df67-fe58-47ac-a0d2-4067e7d195cf\scratchpad\p4_lock_mine.txt'
$p = Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'fabric.dli.config' -and $_.CommandLine -match 'overcraft_polish4|overcraft_p4old' }
foreach ($x in $p) { Stop-Process -Id $x.ProcessId -Force -ErrorAction SilentlyContinue }
for ($i = 0; $i -lt 20; $i++) {
  Start-Sleep 2
  $left = Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'fabric.dli.config' -and $_.CommandLine -match 'overcraft_polish4|overcraft_p4old' }
  if (-not $left) { break }
}
"my clients left: $($left.ProcessId)"
$l = Get-Item E:\claude\mods\.oc_client_lock -ErrorAction SilentlyContinue
if (-not $left -and $l -and (Test-Path $m) -and ((Get-Content $m).Trim() -eq "$($l.CreationTimeUtc.Ticks)")) { Remove-Item E:\claude\mods\.oc_client_lock; Remove-Item $m; "released mine" } else { "lock not removed: not mine, or none" }
