# #129: a pain grunt on every hit (2026-11-13)

Up: [Sounds](../../../sound/sounds.md) · [Techniques](../../../README.md). Branch `fix/pain-lines` from `next` 5ca5da1 (0.3.42).

**The rule, from the data** (`E:\Overwatch Archive\research\kiriko-voice\stim078_all.json`, build 154088; read offline,
Overwatch and Battle.net checked closed): pain is voice stimulus **0003** (light: cooldown 1-2.5 s) and **0004** (heavy:
0.5-1 s), both priority 40, interrupt 0, no chance field; each gated by an engine-set scripted event (0608 / 0609) whose
damage threshold isn't in the data, and no statescript graph raises them. Nothing else limits them. So: Voice's usual
rules (its own cooldown, one line at a time), with 0003's 1-2.5 s, since the mod's one `pain` event holds both
stimuli's lines. Ours: a grunt holds the voice 1 s (`PAIN_TICKS`; the 308 pain files run 0.23-1.45 s, median 0.61 s).

**How:** one dev client (`gradlew runClient` in this worktree, `OC_ASSET_PACK` the main checkout's pack), fresh flat
survival world `p129`, teams red (me) / blue (DevBot `kb`, Kiriko, 10 m ahead). Input: the harness's key mappings
(`key ability_2 tap`, `key secondary_fire down`), not the desktop's mouse. Count: the client's `heard` polled every 0.1 s,
lines matching `kiriko.voice.pain`. Old behaviour with `bot fix 129 off` (Minecraft's hurt sound, as before), in the same
build. Raw output: [raw.txt](raw.txt). Screenshots (run/screenshots): `p129_setup.png`, `p129_tr_fire2.png`,
`p129_self.png`, `p129_mob.png`.

| Step | Fix off | Fix on |
|---|---|---|
| D.Va Micro Missiles (E) at the bot, 225 -> 9.2 health both times | **36 grunts in 1.0 s** (the issue's number) | **1** |
| Tracer, pistols held 1.2 s | 19 | 1 |
| Junkrat, frags held 2.5 s | 4 (bot nearly dead) | 1 |
| Junkrat, one frag every ~4 s, x3 | - | **3** (one per hit: spaced hits still grunt) |
| A zombie hits me (Junkrat), 8 s | 4 grunts on 5 hits | 4 grunts, 1.8-3 s apart |

Also seen: the death line still plays (Junkrat burned to death: `junkrat.voice.death`); fire damage keeps Minecraft's
burn sound (no grunt, as before); a husk hit by Tracer plays Minecraft's "Husk hurts" (mobs are untouched: the fix is in
`PlayerMixin`). Fix-on missile damage equalled fix-off's: only the sound changed.

**Not tried:** two clients (what the other player hears: the line now comes from `HeroSounds.fromHero`, the path every
voice line uses); heavy pain's shorter cooldown (no data rule to tell a heavy hit); a pain grunt delaying a kill line
(priority 40 vs 70: the kill line is dropped for up to 1 s); the real desktop mouse; mobs' own hurt sounds per hit
(Minecraft's, not a voice line; not counted).
