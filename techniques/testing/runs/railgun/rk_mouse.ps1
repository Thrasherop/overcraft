# Real OS mouse events to the game, for what the computer-use tool can't do: both buttons at once, a flick.
# Refuses unless the Minecraft dev client's window is in front, and rechecks before every step.
param([string]$Case)
Add-Type @'
using System; using System.Runtime.InteropServices; using System.Text;
public class RkM {
  [DllImport("user32.dll")] public static extern void mouse_event(uint f, int dx, int dy, uint d, IntPtr e);
  [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
}
'@
$MOVE=0x1; $LD=0x2; $LU=0x4; $RD=0x8; $RU=0x10
function Guard { $b=New-Object Text.StringBuilder 256; [RkM]::GetWindowText([RkM]::GetForegroundWindow(),$b,256)|Out-Null; if($b.ToString() -notlike 'Minecraft*26.3*'){ [RkM]::mouse_event($LU,0,0,0,[IntPtr]::Zero); [RkM]::mouse_event($RU,0,0,0,[IntPtr]::Zero); throw "game not in front: $b" } }
function Ev($f,$x=0,$y=0){ Guard; [RkM]::mouse_event($f,$x,$y,0,[IntPtr]::Zero) }
$g=Get-Process java | Where-Object { $_.MainWindowHandle -ne 0 } | Select-Object -First 1
(New-Object -ComObject WScript.Shell).AppActivate([int]$g.Id) | Out-Null; Start-Sleep -Milliseconds 400
switch ($Case) {
  'lefthold' { Ev $LD; Start-Sleep -Milliseconds 700; Ev $RD; Start-Sleep -Milliseconds 60; Ev $RU; Start-Sleep -Milliseconds 1500; Ev $LU }
  'bothheld' { Ev $LD; Start-Sleep -Milliseconds 500; Ev $RD; Start-Sleep -Milliseconds 1500; Ev $RU; Start-Sleep -Milliseconds 300; Ev $LU }
  'flick'    { 1..4 | ForEach-Object { Ev $MOVE (-50) 0 }; Ev $RD; Start-Sleep -Milliseconds 60; Ev $RU }
  'flickhuman' { 1..8 | ForEach-Object { Ev $MOVE (-25) 0; Start-Sleep -Milliseconds 12 }; Start-Sleep -Milliseconds 15; Ev $RD; Start-Sleep -Milliseconds 60; Ev $RU }
  'flickwait'  { 1..4 | ForEach-Object { Ev $MOVE (-50) 0 }; Start-Sleep -Milliseconds 30; Ev $RD; Start-Sleep -Milliseconds 60; Ev $RU }
  'tap'      { Ev $RD; Start-Sleep -Milliseconds 60; Ev $RU }
  default { throw "unknown case $Case" }
}
"sent $Case"
