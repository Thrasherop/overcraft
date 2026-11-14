# Run only after this session's own New-Item of the lock succeeded. One client from overcraft_grip.
$w = 'E:\claude\mods\overcraft_grip'
(Get-Item E:\claude\mods\.oc_client_lock).CreationTimeUtc.Ticks | Out-File -Encoding ascii C:\Users\ultra\AppData\Local\Temp\claude\14452f9945c49089\6635df67-fe58-47ac-a0d2-4067e7d195cf\scratchpad\grip_lock_mine.txt
$ow = Get-Process Overwatch, Battle.net -ErrorAction SilentlyContinue
$cl = Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'fabric.dli.config' }
if ($ow -or $cl) { "BUSY: $($ow.Name) $($cl.ProcessId)"; exit 3 }
$rev = git -c safe.directory=* -C $w rev-parse --short HEAD
$dirty = git -c safe.directory=* -C $w status --short -- src
"$rev dirty_src_files=$(@($dirty).Where({$_}).Count) launched=$(Get-Date -Format s)"
New-Item -ItemType Directory -Force "$w\run" | Out-Null
$env:OC_ASSET_PACK = 'E:\claude\mods\overcraft\local-assets\pack'
$env:JAVA_TOOL_OPTIONS = '-Xmx3G'
Start-Process -FilePath "$w\gradlew.bat" -ArgumentList 'runClient' -WorkingDirectory $w -RedirectStandardOutput "$w\run\grip_client.out" -RedirectStandardError "$w\run\grip_client.err" -WindowStyle Hidden
"started"
