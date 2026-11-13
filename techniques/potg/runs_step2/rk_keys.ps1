# Real OS key and mouse events to the game during a POTG replay (after rk_mouse.ps1): refuses unless the Minecraft dev
# client's window is in front, and rechecks before every event; lets everything go if it isn't.
Add-Type @'
using System; using System.Runtime.InteropServices; using System.Text;
public class RkK {
  [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint f, IntPtr e);
  [DllImport("user32.dll")] public static extern uint MapVirtualKey(uint code, uint type);
  [DllImport("user32.dll")] public static extern void mouse_event(uint f, int dx, int dy, uint d, IntPtr e);
  [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
}
'@
$KEYUP=0x2
$held=@()
function Release { foreach ($k in $script:held) { [RkK]::keybd_event($k, [RkK]::MapVirtualKey($k,0), $KEYUP, [IntPtr]::Zero) }; [RkK]::mouse_event(0x4,0,0,0,[IntPtr]::Zero) }
function Guard { $b=New-Object Text.StringBuilder 256; [RkK]::GetWindowText([RkK]::GetForegroundWindow(),$b,256)|Out-Null; if($b.ToString() -notlike 'Minecraft*26.3*'){ Release; throw "game not in front: $b" } }
function Down($vk) { Guard; [RkK]::keybd_event($vk, [RkK]::MapVirtualKey($vk,0), 0, [IntPtr]::Zero); $script:held += $vk }
function Up($vk) { Guard; [RkK]::keybd_event($vk, [RkK]::MapVirtualKey($vk,0), $KEYUP, [IntPtr]::Zero) }
function Tap($vk) { Down $vk; Start-Sleep -Milliseconds 120; Up $vk; Start-Sleep -Milliseconds 300 }
$g=Get-Process java | Where-Object { $_.MainWindowHandle -ne 0 } | Select-Object -First 1
(New-Object -ComObject WScript.Shell).AppActivate([int]$g.Id) | Out-Null; Start-Sleep -Milliseconds 500
try {
  Down 0x57; Start-Sleep -Milliseconds 1200; Up 0x57      # W held
  Tap 0x20                                                 # Space
  Tap 0x45                                                 # E: Protection Suzu
  Tap 0x51                                                 # Q: Kitsune Rush
  Tap 0x47                                                 # G: hero mode
  Tap 0x31                                                 # 1: hotbar
  Guard; [RkK]::mouse_event(0x2,0,0,0,[IntPtr]::Zero); Start-Sleep -Milliseconds 400; Guard; [RkK]::mouse_event(0x4,0,0,0,[IntPtr]::Zero)  # left button
  "sent W, Space, E, Q, G, 1, left button"
} finally { Release }
