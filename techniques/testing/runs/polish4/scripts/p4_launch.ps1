# One client, the lock first, Overwatch and others checked in the same command. Writes which commit this client was built from.
$w = 'E:\claude\mods\overcraft_polish4'
New-Item -ItemType Directory -Path E:\claude\mods\.oc_client_lock -ErrorAction Stop | Out-Null
(Get-Item E:\claude\mods\.oc_client_lock).CreationTimeUtc.Ticks | Out-File -Encoding ascii C:\Users\ultra\AppData\Local\Temp\claude\E--claude-mods-overcraft\6635df67-fe58-47ac-a0d2-4067e7d195cf\scratchpad\p4_lock_mine.txt
$ow = Get-Process Overwatch, Battle.net -ErrorAction SilentlyContinue
$cl = Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'fabric.dli.config' }
$bl = Get-Process blender -ErrorAction SilentlyContinue
if ($ow -or $cl -or $bl) { "BUSY: $($ow.Name) $($cl.ProcessId) $($bl.Name)"; Remove-Item E:\claude\mods\.oc_client_lock; exit 3 }
$rev = git -c safe.directory=* -C $w rev-parse --short HEAD
$dirty = git -c safe.directory=* -C $w status --short -- src build.gradle gradle.properties
"$rev dirty_src_files=$(@($dirty).Where({$_}).Count) launched=$(Get-Date -Format s)" | Out-File -Encoding ascii "$w\run\p4_launch_tree.txt"
Get-Content "$w\run\p4_launch_tree.txt"
Remove-Item "$w\run\logs\latest.log" -ErrorAction SilentlyContinue
$env:OC_ASSET_PACK = 'E:/claude/mods/overcraft_ana/local-assets/pack'
$env:JAVA_TOOL_OPTIONS = '-Xmx3G'
Start-Process -FilePath "$w\gradlew.bat" -ArgumentList 'runClient' -WorkingDirectory $w -RedirectStandardOutput "$w\run\p4_client.out" -RedirectStandardError "$w\run\p4_client.err" -WindowStyle Hidden
"started"
