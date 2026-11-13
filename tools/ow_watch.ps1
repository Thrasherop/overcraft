# The owner's rule made automatic (CLAUDE.md, owner 2026-11-05): nothing of ours runs alongside Overwatch.
# The moment Overwatch or Battle.net is up, this closes every dev client and DataTool. It never touches Overwatch or
# Battle.net themselves. One copy runs at a time; a later start exits at once. What it did goes in the log below, for the
# agent whose client went away. A held client lock stays: its holder releases it.
#
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\ow_watch.ps1
# -DryRun logs what it would close and closes nothing. -Watch names other processes to watch for, for testing with a dummy.
param(
	[string[]]$Watch = @('Overwatch', 'Battle.net'),
	[switch]$DryRun,
	[double]$Every = 1,
	[string]$Log = 'E:\claude\mods\.oc_ow_watch.log',
	[double]$StopAfter = 0
)

# (a test's copy, with its own -Watch or -DryRun, runs beside the real one)
$mutex = New-Object System.Threading.Mutex($false, "Local\OvercraftOwWatch_$($Watch -join '_')$(if ($DryRun) { '_dry' })")
try {
	if (-not $mutex.WaitOne(0)) { exit 0 }
} catch [System.Threading.AbandonedMutexException] {
	# the last copy died without letting go (a crash): it's ours now
}

function Write-WatchLog([string]$text) {
	"$(Get-Date -Format s) $text" | Add-Content -Encoding ascii $Log
}

# A dev client is a java process with Fabric's dev launch config on its command line: never a player's own Minecraft
function Get-OurProcesses {
	Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe' OR Name='DataTool.exe'" | Where-Object {
		$_.Name -eq 'DataTool.exe' -or $_.CommandLine -match 'fabric\.dli\.config'
	}
}

Write-WatchLog "started pid $PID watching $($Watch -join ', ') every $Every s$(if ($DryRun) { ' (dry run)' })"
$began = Get-Date
$seen = $false
while ($StopAfter -le 0 -or ((Get-Date) - $began).TotalSeconds -lt $StopAfter) {
	$up = @(Get-Process -Name $Watch -ErrorAction SilentlyContinue)
	if ($up.Count -gt 0) {
		if (-not $seen) { Write-WatchLog "seen: $(($up | Select-Object -ExpandProperty Name -Unique) -join ', ')" }
		$seen = $true
		foreach ($p in @(Get-OurProcesses)) {
			if ($DryRun) {
				Write-WatchLog "would close $($p.Name) $($p.ProcessId)"
			} else {
				Write-WatchLog "closing $($p.Name) $($p.ProcessId)"
				Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
			}
		}
	} elseif ($seen) {
		Write-WatchLog 'clear: none running'
		$seen = $false
	}
	Start-Sleep -Milliseconds ([int]($Every * 1000))
}
Write-WatchLog "stopped pid $PID"
