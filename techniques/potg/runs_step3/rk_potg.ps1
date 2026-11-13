# Real OS key and mouse events to the game during a POTG replay (after rk_mouse.ps1): refuses unless the Minecraft dev
# client's window is in front, and rechecks before every event; lets everything go if it isn't.
param([string]$Case = 'keys', [int]$Seconds = 5)
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
  if ($Case -eq 'hold') {
    # the left button (Kiriko's kunai) held for the fight, released at the end or if the game leaves the front
    # a first click takes the mouse into the window (Minecraft grabs it and doesn't attack with that click)
    Guard; [RkK]::mouse_event(0x2,0,0,0,[IntPtr]::Zero); Start-Sleep -Milliseconds 80; Guard; [RkK]::mouse_event(0x4,0,0,0,[IntPtr]::Zero); Start-Sleep -Milliseconds 400
    Guard; [RkK]::mouse_event(0x2,0,0,0,[IntPtr]::Zero)
    $end=(Get-Date).AddSeconds($Seconds); while ((Get-Date) -lt $end) { Start-Sleep -Milliseconds 250; Guard }
    [RkK]::mouse_event(0x4,0,0,0,[IntPtr]::Zero); "held the left button $Seconds s"
  } else {
    Down 0x57; Start-Sleep -Milliseconds 1200; Up 0x57      # W held
    Tap 0x20; Tap 0x45; Tap 0x51; Tap 0x47; Tap 0x31        # Space, E, Q, G, 1
    Tap 0x1B; Tap 0x0D; Tap 0x48                            # Escape, Enter, H (hero menu)
    Guard; [RkK]::mouse_event(0x2,0,0,0,[IntPtr]::Zero); Start-Sleep -Milliseconds 400; Guard; [RkK]::mouse_event(0x4,0,0,0,[IntPtr]::Zero)
    "sent W, Space, E, Q, G, 1, Escape, Enter, H, left button"
  }
} finally { Release }