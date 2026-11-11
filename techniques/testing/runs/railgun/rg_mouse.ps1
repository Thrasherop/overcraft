# Real OS mouse events, as rk_mouse.ps1, refusing unless the game's window is in front. For what must be timed from the harness or go on a while:
#  mash <cps> <upMs> <secs>: right button clicks at that rate
#  armed <file> <counts>: ready first, then on <file> appearing: <counts> left in one burst and the click in that frame
param([string]$Case, [string]$A1, [string]$A2, [string]$A3)
Add-Type @'
using System; using System.Runtime.InteropServices; using System.Text;
public class RgM {
  [DllImport("user32.dll")] public static extern void mouse_event(uint f, int dx, int dy, uint d, IntPtr e);
  [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
  [DllImport("winmm.dll")] public static extern uint timeBeginPeriod(uint p);
}
'@
[RgM]::timeBeginPeriod(1) | Out-Null
$MOVE=0x1; $RD=0x8; $RU=0x10
function Guard { $b=New-Object Text.StringBuilder 256; [RgM]::GetWindowText([RgM]::GetForegroundWindow(),$b,256)|Out-Null; if($b.ToString() -notlike 'Minecraft*26.3*'){ [RgM]::mouse_event($RU,0,0,0,[IntPtr]::Zero); throw "game not in front: $b" } }
function Ev($f,$x=0,$y=0){ Guard; [RgM]::mouse_event($f,$x,$y,0,[IntPtr]::Zero) }
$g=Get-Process java | Where-Object { $_.MainWindowHandle -ne 0 } | Select-Object -First 1
(New-Object -ComObject WScript.Shell).AppActivate([int]$g.Id) | Out-Null; Start-Sleep -Milliseconds 300
switch ($Case) {
  'mash' {
    $cps=[double]$A1; $up=[int]$A2; $secs=[double]$A3; $period=1000.0/$cps; $down=[int]($period-$up); $sw=[Diagnostics.Stopwatch]::StartNew(); $n=0
    while ($sw.Elapsed.TotalSeconds -lt $secs) { Ev $RD; Start-Sleep -Milliseconds $down; Ev $RU; $n++; while ($sw.Elapsed.TotalMilliseconds -lt $n*$period) { } }
    "clicks $n"
  }
  'armed' {
    while (-not (Test-Path $A1)) { }
    $c=[int]$A2; for($i=0;$i -lt $c/50;$i++){ Ev $MOVE (-50) 0 }; Ev $RD; Start-Sleep -Milliseconds 40; Ev $RU
    "fired"
  }
  default { throw "unknown case $Case" }
}
