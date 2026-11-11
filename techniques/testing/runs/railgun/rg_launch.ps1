# The lock is already taken in a command of its own. Overwatch and other clients checked here; writes which commit this client was built from.
$w = 'E:\claude\mods\overcraft_railgun'
$ow = Get-Process Overwatch, Battle.net -ErrorAction SilentlyContinue
$cl = Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'fabric.dli.config' }
$bl = Get-Process blender -ErrorAction SilentlyContinue
if ($ow -or $cl -or $bl) { "BUSY: $($ow.Name) $($cl.ProcessId) $($bl.Name)"; exit 3 }
New-Item -ItemType Directory -Force "$w\run" | Out-Null
$rev = git -c safe.directory=* -C $w rev-parse --short HEAD
$dirty = git -c safe.directory=* -C $w status --short -- src build.gradle gradle.properties
"$rev dirty_src_files=$(@($dirty).Where({$_}).Count) launched=$(Get-Date -Format s)" | Out-File -Encoding ascii "$w\run\rg_tree.txt"
Get-Content "$w\run\rg_tree.txt"
Remove-Item "$w\run\logs\latest.log" -ErrorAction SilentlyContinue
$env:OC_ASSET_PACK = 'E:/claude/mods/overcraft_ana/local-assets/pack'
$env:JAVA_TOOL_OPTIONS = '-Xmx3G'
Start-Process -FilePath "$w\gradlew.bat" -ArgumentList 'runClient' -WorkingDirectory $w -RedirectStandardOutput "$w\run\rg_client.out" -RedirectStandardError "$w\run\rg_client.err" -WindowStyle Hidden
"started"
