$p = Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'fabric.dli.config' -and $_.CommandLine -match 'polish4|p4base|p4old' }
foreach ($x in $p) { Stop-Process -Id $x.ProcessId -Force }
for ($i = 0; $i -lt 15; $i++) {
  Start-Sleep 2
  $left = Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'fabric.dli.config' }
  if (-not $left) { break }
}
"left: $($left.ProcessId)"
if (-not $left) { Remove-Item E:\claude\mods\.oc_client_lock; "released" }
