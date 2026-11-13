# Tracer: the coordinator's real-input pass (060766e, 2026-11-12)

Up: [Tracer](../../../heroes/tracer.md) · beside it: the critic's report `critic_d0c7e1f.md`

Fresh `gradlew runClient`, new survival world `rk_tracer`. Tracer picked by clicking her T on the select screen (card:
Damage, Flanker, 175 health) and Ready. Setup (day, no monsters, targets, the staircase, `/overcraft ult`) through the
harness; every action with the real mouse and keys.

| Step | Expected | Seen |
|---|---|---|
| HUD | 175 health, Blink LShift, Recall E, 40 rounds | as expected |
| Shift x5, 3.6 s apart (the critic's critical bug) | 7.5 m each | z 7.5 -> 45.0 |
| Shift x4, 0.25 s apart | 3 Blinks (22.5 m), the 4th empty, HUD recharge timer | z 45 -> 67.5; timer shown |
| E | Recall, 12 s cooldown | cooldown 11.05 after 1.5 s (position not checkable mid-batch: see below) |
| Hold left mouse 1 s on a 500-health husk at 5 m | 40 rounds gone in 1 s; up to 230 OW | 0 rounds, auto-reload; husk -17.3 (216 OW, armor and spread) |
| Q at the husk, ult full | sticks, 350 at the centre, Tracer outside 4 m unhurt | stuck on it; husk -27.9 (349 OW); Tracer 14.0 (full); ult spent |
| Shift at the foot of an 8-step oak staircase | climbs (the old 1.5 m cap is gone) | y +6.5 over 7.5 m |
| G, Shift, G | Minecraft mode: sneak only; back: her HUD | no Blink in Minecraft mode; HUD back, hero tracer |

Also seen: a daylight NoAI zombie burned and died before it could be measured (my setup, not a bug).

Not tried with real input: Recall's landing spot (the harness runs of the builder and fixer cover it), Recall across
dimensions, wall slide, perks, death and respawn, hero switch with a bomb out, the Nether, a second player.
