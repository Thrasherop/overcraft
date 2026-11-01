# Sombra data map, from Overwatch's game data (build 154088)

Read 2026-10-31 offline and read-only through TankLib/DataTool 2.25.0.0 (`Online=false`) on `build-154088` (2026-10-06), with
Overwatch and Battle.net checked closed (`Get-Process Overwatch, Battle.net` / `tasklist`) before every TankLib run
(`scripts\owinit.py` refuses otherwise). Nothing was decrypted beyond DataTool's own reading, nothing was run but TankLib,
nothing went into the repo. Confidence: **H** read in the data (a node, field, value, edge, string), **M** inferred (one link
guessed or read by analogy with Ana / Mercy), **L** guess. Node refs are `graph #uid`; vars `vNNN`; values by their `.130` id.
Research folder: `E:\Overwatch Archive\research\sombra\` (below, paths are relative to it).

Detail behind this map (each finding there has its node ids): `work_kit\kit_pistol_hotfix.md` (ids, buttons, hero stats and
settings, the values table, Machine Pistol, Hotfix, the hack status), `work_kit\kit_translocator_stealth.md` (Translocator,
beacon, stealth), `work_kit\kit_cyberspace_emp_perks.md` (Cyberspace, EMP, Tactician, Conspirator, perks, the old kit).
Dumps: `dump` (hero), `dump_ss` (graphs, entities: crawl from 012E.075 following 003 / 01B), `dump_lo` (loadouts, perk files,
the perk list 005B.177), `graphs\` (reflected graphs, `tl_nodes.py`), `resolved\statescript.json` (`analyze_ss.py`),
`work_kit\decomp_<g>.txt` + `edges_<g>.txt` (decompiled graphs), `work_kit\c_<g>.txt` (compacted with out-edges). Machine-
readable: `gamevalues.json`, `gvpair.json`, `loadout_stats.json`, `work\varvalues.json`, `work_kit\herosettings_sombra.json`.
Scripts: `scripts\` (copies of Ana's, retargeted: `owinit.py`, `crawl.py`, `hero0.py`, `gamevalues.py`, `gvpair.py`,
`varvalues.py` (values graph 3918), `tl_nodes.py`, `lostats.py`, `analyze_ss.py`, `digest.py`, `kit\decomp.py`,
`kit\decomp_out.py`, `kit\herosettings_sombra.py`, `kit\peek.py`, `kit\strings07c.py` ...; new: `kit\compact.py`,
`kit\lean.sh`, `kit\gvsummary.py`). TankLib scripts run with the kiriko-anim venv's interpreter (`-I`).

## Models, clip map, voice, sounds and effects
To be filled from the parallel extraction / clip-map pass (`extract\`, `work\`): not part of this kit pass. Ids the kit graphs
name, for that pass to join: Gameplay3P **121C.003**; pistol cosmetic **1221** on hp **000A** (its muzzle hp 0006); shot
1222 (no model); beacon **1575** (thrown from hp 0002); Cyberspace projectile **7763** (from hp 0002); anim aliases: reload
**0004**, throw **00F4** (Stadium 0957), teleport arrival **0007**, stealth in **0486** / out **0487**, Hotfix **047D**,
Cyberspace **0997** -> **0998**, EMP **0175**, hacked (on the target) **0070** (the shared hit / stun alias).

## Abilities (numbers from her values graph **3918** and the graphs that read them)
Graphs on Gameplay3P 121C (H): weapon manager 0015 (shared), **0B1B** Machine Pistol, **614C** Hotfix (-> **60FB** heal on the
ally, **0B1F** hack on anything else), **610E** Cyberspace (-> **6102** on projectile 7763), **0C19** Translocator (beacon 1575
runs **0C1A**, 261E), **420C** stealth, **0B31** EMP (-> **0B32** per target -> 0B1F), **3918** values (+ 3361), **51EC** perks
(+ 51A3), 3DCE ult callout, 0B1A shared helpers, 0043 quick melee, 004B shared ult, 01C7 role / regen, 0033, 08B6.
Buttons (H): PRIMARY **xD128396E** (buffer 0.15 s), SECONDARY **xB42D90B7** (Hotfix), ABILITY 1 **xCBF0F815** (Cyberspace),
ABILITY 2 **x2ADDA7B1** (Translocator), ULTIMATE **x2DFEDC52** (EMP), RELOAD x5D2A8B7F, QUICK MELEE xD19BBFFC; every ability
buffer 0.15 s. **Health 225** (0786; no armor, no shield), melee 40, 5.5 m/s, the hero-default mover (identical to Ana's),
**ult cost 3100** (0AFD "Ultimate Charge Max", 121C -> 004B), perk XP **2375 then 3335 more** (12AE / 12AF). Hero options
(file 0126.054, H): **Hotfix Sensitivity v9584 1.0** (0.1-1), **Toggle Hotfix v49204 off**, **Hide Stealth Detection Ring
v52253 off**, **Disable Translocator Camera Lock v52940 on**, vibrations incl. a Stealth one (v54091).
- **Machine Pistol** (0B1B; H unless said): PRIMARY held fires **20 shots/s** (v8421, x weapon speed) of **hitscan** (entity
  1222: no speed or motions, M), **7 damage**, **falloff 20 -> 35 m down to x0.3** (2.1; field reading M), crit x2 (M), size
  0.02 m (28A3 via 01C7, M), **60 rounds** (x lobby scalar), 1 per shot. **Spread 0 -> 2° over 10 shots**, recovering to 0 at
  4/s after **0.5 s** without firing. **0.15 s** before a re-press fires after a burst (#86). **Reload 1.2 s** (alias 0004),
  **clip full at 0.48 s** (0.4 x), fire locked to 1.2 s. Muzzle: the cosmetic pistol 1221's hp 0006.
- **Hotfix** (614C, 60FB, 0B1F; H unless said): SECONDARY, **hold** (release cancels; toggle option), **2 charges, one back
  every 8 s** (2A57 / 2ADA; the recharge is set at the end of a use when none is running). Target picked by a cone **10° x sensitivity** within
  **17 m**, sight through 002C; locked with a sight ray, a **0.5 s channel**, then **0.3 s** recovery (anim 047D).
  - **Ally hero**: 60FB on it: **65 at once, then 25 every second**, ending at **175 healed** or **12 s** (or its death); one
    charge spent. So a hurt ally gets all 175 in 4.4 s (H nodes, M per-second field).
  - **Anything else** (health pack, enemy deployable): the **hack** (0B1F), **no charge spent**, then a **6 s** lockout on
    non-ally hacks (2AD5); taking damage during such an attempt aborts it and starts the 6 s too. Message 2BB8 (Data Packet).
  - Whether enemy heroes pass Hotfix's filter: not decoded (L; the text says allies, health packs, deployables).
- **The hack** (0B1F on the target; H nodes, M meanings): **heroes: reveal 8 s and silence** (ability inputs 493C433E /
  4E596ED2 / 7FA25A61 / 292F571E / 5C571F43 = 0, running abilities cancelled, cooldowns held), both x (1 - CC reduction);
  silence **3 s from EMP**; from Hotfix it would read `Sombra.v61770`, which nothing sets (L; the panel says 1 s). **Health
  packs 30 s, deployables 10 s, B.O.B. 3 s.** Target anim 0070. The hacked-pack respawn rate 0EB3 0.25 is read by no node
  of hers (L).
- **Cyberspace** (610E, 6102; H unless said): ABILITY 1, a **0.45 s** cast, the projectile at **0.1 s** from hp 0002:
  **50 m/s**, no gravity (M), size **0.3 m** (0.225 + 0.075), bursts on impact or at **30 m** (M), lifetime 25 s. The zone:
  **5 m, 3 s**, sight-checked: **enemies 40 on opening + 25/s, weakened -50% damage and healing dealt** (floor -50%, M on the
  channels), linger **0.25 s**; **allies 40 + 30/s**, linger **0.75 s**. The projectile payload holds 50 damage (L: a direct
  hit?). **Cooldown 12 s from the cast's end.**
- **Not the live Translocator (found 2026-10-31):** the reading below (E again to teleport, 15 s, 5 HP, cooldown 1 s
  after landing) matches the **Community Crafted** mode's beacon (BZ 2026-06-30), not the live one. Live (BZ 2026-10-06
  12.25 m; W): it takes her by itself on its first terrain contact or 0.17 s out (12.25 m along its path), can't be
  damaged, the cooldown from the teleport, 0.4 s phased after. `SombraStats` follows BZ / W; the speed, gravity and the
  0.5 m lift are kept from here.
- **Translocator** (0C19, 0C1A; H unless said): ABILITY 2 throws the beacon in a **0.6 s** state: **72 m/s, gravity field 20**
  (M), **15 s** lifetime, 0.01 m, from hp 0002, sticks (M), **5 HP** (destroyable), usable after **0.17 s** (M). ABILITY 2
  again: a **0.4 s** protected move (invulnerable / no input, M) to **beacon + 0.5 m up**, not during EMP's cast. On arrival:
  stealth starts (241D), and **the 7.5 s cooldown starts 1 s later**; a lost beacon starts it at once.
- **Stealth** (420C; H unless said): **no ability of its own** (Stealth cooldown 80220 s read by nothing). Entered only by a
  teleport: **0.08 s, then 0.75 s fading out of sight, then 6.5 s stealthed**, then **0.3 s** back; **+60% move speed** while
  fading and stealthed (mover speed channel x786680F6, M). A teleport while stealthed refills 6.5 s. **Broken by** PRIMARY,
  ABILITY 1, ULTIMATE, QUICK MELEE, a shot; **not by damage** (damage gives 1.3 s of a shimmer flag and a 0.2 term on v23184,
  L). **Enemies within 4 m (x scale) with sight detect her** (ring UI 8 m wide, voice to the detector); the panel's 12 m is
  Lúcio's Crossfade value.
- **EMP** (0B31, 0B32; H unless said): ULTIMATE, charge full; **charge spent at activation**; a **0.65 s** cast (anim 0175, a
  1.25 lift L), **the pulse at 0.35 s**: **15 m** (x scale) around her, sight to several hit points with barriers in the
  ignore list (M): **barriers take 10000** (destroyed); enemies and their deployables take **20% of current health** (no crit)
  and are **hacked** (silence 3 s, reveal 8 s; their deployables too); teleport blocked during the cast. After it, her ult flag
  v842 for **5 s** (M).
- **Tactician** (01C7, tag **1172**, H nodes): store cap **0.25 x 3100 = 775**, gain x0.75 while full, refilled over 1 s once
  v842 rises (after EMP's cast, M). Regen 6 s / `5% + 10`/s = **21.25 HP/s** (M, shared nodes).
- **Conspirator** (FBB): **no graph of hers reads it** (H absent): the enemy-ult reveal is outside her graphs (L engine / UI).
- **Perks** (live list **005B.177**; H): minor **Life Hack 2AF0** (FC4): EMP's pulse puts **Hotfix (65 + 25/s to 175, 12 s)
  on every ally within 15 m** (0B31 #294 / #372). Minor **CTRL ALT ESC 2544** (BA5): a teleport **under 50% health** starts the
  passive regen **at once** (51EC #323 -> 2727 -> 01C7 #963, M the tick). Major **Data Packet 2AE9** (FBC): each Hotfix hack of
  a non-ally (health pack or deployable, M) **-4 s on Hotfix's recharge**, overflow carried into the next charge. Major
  **Cybersecurity 2AFF** (FCF): **x2 healing on allies under 50%** (51EC #543; category L).
- Ungated (H nodes, L meaning): 51EC cuts the **Translocator cooldown by 3 s** on message 27BB and on an elimination by
  Cyberspace's damage id BF46; and the old **Healthy Hacker** leftover (25 overhealth after a health-pack heal, up to 10 s).

## Nuances a port must not miss
1. One weapon, hitscan, 20/s, 7 with falloff to x0.3 between 20 and 35 m, spread grows over 10 shots and resets after 0.5 s (H/M).
2. Hotfix has 2 charges on an 8 s recharge, but hacking packs and deployables costs no charge, only a 6 s lockout (H).
3. Hotfix's heal: 65 at once + 25/s, capped by 175 healed or 12 s; Life Hack gives it to everyone in 15 m on EMP (H).
4. Cyberspace: 50 m/s, 0.3 m, 5 m zone for 3 s: 40 + 25/s and -50% output on enemies, 40 + 30/s on allies, short lingers (H/M).
5. Stealth is only the teleport's aftermath: 0.75 s in, 6.5 s, +60% speed; attacks break it, damage doesn't; 4 m detection (H/M).
6. The teleport takes 0.4 s, protected, lands 0.5 m above the beacon; the cooldown starts 1 s after landing (H/M).
7. EMP: 20% of current health in 15 m, barriers destroyed, 3 s silence + 8 s reveal; charge goes at the press (H).
8. Health 225, ult 3100 (not Ana's 250 / 2300) (H).
9. Panel rows reuse other heroes' values (Rifle 75, Crossfade 12 m / 0.6, Plasma Grenade): read numbers through the graphs (H).

## Disagreements with public sources
Not compared in this pass (no `sombra.md` public-notes file yet). Check Blizzard's patch notes, then the wiki, before a port:
the data's build is 2026-10-06.

## Not found
Hotfix's full target filter (enemy heroes or not) and its 1 s hero silence (v61770 unset); Conspirator's implementation; the
Cyberspace projectile's 50 payload and burst at 30 m; the exact weaken channels (v78156 / v78158); the hacked-pack respawn
rule (0EB3 read by nothing); EMP's 1.25 lift; the beacon's bounce / stick fields and any max throw range (`m_FEC435C6` 1000);
the stealth damage term on v23184 and what v690 / v6035 / v6773 are; who sends 27BB, 23EB, 240A; the per-second tick
pattern (`m_95F1FB0B`); the many Stadium branches (0790-07CB.134 ids, multi-level 06xx arrays); entity 66FA's kit in the
shared graphs (not hers). Models, clips, voice, effects: the parallel pass.
