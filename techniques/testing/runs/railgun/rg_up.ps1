# Launch this worktree's client and wait for its harness. Overwatch checked inside rg_launch. Optional: -Pack <zip or folder> for this run.
param([string]$Pack)
$sp='C:\Users\ultra\AppData\Local\Temp\claude\E--claude-mods-overcraft\6635df67-fe58-47ac-a0d2-4067e7d195cf\scratchpad'
$s=[IO.File]::ReadAllText("$sp\rg_launch.ps1")
if ($Pack) { $s=$s.Replace("E:/claude/mods/overcraft_ana/local-assets/pack",$Pack) }
& ([scriptblock]::Create($s))
$t=Get-Date; $ok=$false
while (((Get-Date)-$t).TotalSeconds -lt 280) { Start-Sleep 4; if ((Test-Path E:\claude\mods\overcraft_railgun\run\logs\latest.log) -and (Select-String -Path E:\claude\mods\overcraft_railgun\run\logs\latest.log -Pattern 'DevServer on' -Quiet)) { $ok=$true; break } }
"devserver: $ok"
Select-String -Path E:\claude\mods\overcraft_railgun\run\logs\latest.log -Pattern 'BindException|asset pack' | Select-Object -First 3 | ForEach-Object { $_.Line.Substring(0,[Math]::Min(180,$_.Line.Length)) }
Start-Sleep 25
$g=Get-Process java | Where-Object { $_.MainWindowHandle -ne 0 } | Select-Object -First 1
if ($g) { (New-Object -ComObject WScript.Shell).AppActivate([int]$g.Id) | Out-Null }
