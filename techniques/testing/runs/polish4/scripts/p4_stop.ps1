$p = Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'fabric.dli.config' -and $_.CommandLine -match 'polish4|p4base' }
foreach ($x in $p) { Stop-Process -Id $x.ProcessId -Force }
Start-Sleep 3
$left = Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'fabric.dli.config' }
"left: $($left.ProcessId)"
if (-not $left) { Remove-Item E:\claude\mods\.oc_client_lock; "released" }
