# Stops only this worktree's client, by PID. The lock is removed in a command of its own, after this says "left: ".
$p = Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'fabric.dli.config' -and $_.CommandLine -match 'overcraft_railgun' }
foreach ($x in $p) { Stop-Process -Id $x.ProcessId -Force }
for ($i = 0; $i -lt 15; $i++) {
  Start-Sleep 2
  $left = Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'fabric.dli.config' }
  if (-not $left) { break }
}
"left: $($left.ProcessId)"
