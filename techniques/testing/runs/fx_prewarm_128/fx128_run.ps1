# #128: for each hero, a fresh client launch, fx128.py, the client stopped. The lock is taken in a command of its own first.
param([string]$tag, [string[]]$heroes)
$w = 'E:\claude\mods\overcraft_fxwarm'
$sp = 'C:\Users\ultra\AppData\Local\Temp\claude\14452f9945c49089\6635df67-fe58-47ac-a0d2-4067e7d195cf\scratchpad'
$res = "$sp\fx128_$tag.txt"
foreach ($h in $heroes) {
  if (Get-Process Overwatch, Battle.net -ErrorAction SilentlyContinue) { "OVERWATCH RUNNING: stopped"; exit 3 }
  $cl = Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'fabric.dli.config' }
  if ($cl) { "ANOTHER CLIENT: $($cl.ProcessId)"; exit 3 }
  Remove-Item "$w\run\logs\latest.log" -ErrorAction SilentlyContinue
  $env:OC_ASSET_PACK = 'E:\claude\mods\overcraft\local-assets\pack'
  $p = Start-Process -FilePath "$w\gradlew.bat" -ArgumentList 'runClient' -WorkingDirectory $w -RedirectStandardOutput "$sp\fx128_client.out" -RedirectStandardError "$sp\fx128_client.err" -WindowStyle Hidden -PassThru
  py -I "$sp\fx128.py" $h $res 2>&1 | Out-Host
  Copy-Item "$w\run\logs\latest.log" "$sp\fx128_${tag}_$h.log" -ErrorAction SilentlyContinue
  $j = Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'fabric.dli.config' -and $_.CommandLine -match 'overcraft_fxwarm' }
  foreach ($x in $j) { Stop-Process -Id $x.ProcessId -Force }
  for ($i = 0; $i -lt 15; $i++) {
    Start-Sleep 2
    $left = Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'fabric.dli.config' }
    if (-not $left) { break }
  }
  "done $h, left: $($left.ProcessId)"
}
Get-Content $res
