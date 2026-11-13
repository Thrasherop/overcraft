# Real OS left button held on client 1's window (Kiriko's kunai) for a dragon kill; guarded: refuses and lets go unless
# that window is in front, checked every 100 ms. Usage: potg_mouse.ps1 <seconds>
param([double]$Seconds)
Add-Type @'
using System; using System.Runtime.InteropServices;
public class PotgM {
  [DllImport("user32.dll")] public static extern void mouse_event(uint f, int dx, int dy, uint d, IntPtr e);
  [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
  [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
}
'@
$LD=0x2; $LU=0x4
$p = Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'overcraft_potgb' -and $_.CommandLine -notmatch 'devPort' -and $_.CommandLine -match 'dli' } | Select-Object -First 1
if (-not $p) { throw "client 1 not found" }
(New-Object -ComObject WScript.Shell).AppActivate([int]$p.ProcessId) | Out-Null; Start-Sleep -Milliseconds 500
function Front { $fp=0; [PotgM]::GetWindowThreadProcessId([PotgM]::GetForegroundWindow(), [ref]$fp) | Out-Null; return $fp -eq $p.ProcessId }
if (-not (Front)) { throw "client 1 not in front" }
[PotgM]::mouse_event($LD,0,0,0,[IntPtr]::Zero)
$t0 = Get-Date
try {
  while (((Get-Date) - $t0).TotalSeconds -lt $Seconds) {
    if (-not (Front)) { throw "client 1 left the front" }
    Start-Sleep -Milliseconds 100
  }
} finally { [PotgM]::mouse_event($LU,0,0,0,[IntPtr]::Zero) }
"held left $Seconds s on pid $($p.ProcessId)"
