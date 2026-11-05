# Statuses and the cleanse: how to read what ends what

How to find, in the game data, what a cleanse removes and what a phase prevents, and what was found. From [#119](https://github.com/Thrasherop/overcraft/issues/119), 2026-11-05, build 154088, offline with Overwatch closed. Up: [Techniques](../README.md) · reading graphs: [reading_game_data](reading_game_data.md) · the play: [polish4](../testing/polish4.md) · [heroes/kiriko](../heroes/kiriko.md)

## The lesson: "no node found" meant it had been looked for in the wrong place
`lifeweaver-map.md` and the #117 comment said the data has no cleanse node. **There is no node on the caster's side because the statuses listen.** The status sets a flag and sends itself one message; each debuff's own graph reads them. So search for the readers, across every hero's `work_kit`, not the ability's graph.

Finding it took: the Suzu's volley payload `m_277A4ECA` -> status **031A.053** -> its `m_5A4051BC` -> graph **180C**. Not one of the 101 graphs dumped for Kiriko: **a .053 status's graph needs crawling by hand**.

## The mechanism (H nodes; M that v8440 means "phased")
- **180C (Suzu's status):** #60 StateStack **v8440 = 1** -> #92 sends itself message **1BC5**. #111 v530 = 1 under the same #64 (`m_type` x45AB6711), for #63's wait v10353 = v29286.v69871 x v59314: not re-read, the mod keeps 0.5 s (BZ, W; played: 8-10 ticks read).
- **21EE (Life Grip):** #606 v8440 = 1, #605 sends 1BC5. The same mechanism. Corrects `kit_grip_tree.md` 2f (which had 1BC5 as an L candidate).
- **224E (the shared crowd-control graph)** answers 1BC5 by its type, `v27561 = v26550.v27606`, override **6BD6**:

| Type | Used by | Entry on 1BC5 | Result |
|---|---|---|---|
| 676B (and A860) | Sleep (0BDE #337) | #401 -> #552 -> #418 sends 1C7D -> #204 -> #304 | wakes, by damage's own path but past its 0.5 s gate (#332): 0.5 s to get up |
| 4EB6, 0E34 | 4EB6: Hack (0B1F, explicitly) and **224E's public schema default** (`m_publicSchema`: 6BD6 = 4EB6, H), so Rally's stun (0EE3 #887 / #888 set no type). This resolves `kit_shield_bash.md`'s "L". 0E34: no caller read | #402 -> #551 -> #416 sends 1C7D -> #297 | ended |
| 6BBE | Knock-down: Accretion (1BBB), Catnapper (5B7B), bash clash (0EE3 #942) | #400: **no edge leaves it** | stays |

- **Prevention in 224E:** #96 tests self against a filter (`m_84065FD1` 64, `m_4EA686DB` 20); a phased hero fails it, and #575 lets it on only if `{v530 and v8440 and type 6BBE}`. So a sleep or stun doesn't start on the phased; **a knock-down would, were it not for the hit's own filter** (5B7B's trackers and the rock's 1EB9 #338: mask 20, M; `c_1BBB` has no filter of its own, it's the rock's). Gravitic Flux's lift has mask 16 (1E41): no bit 4, so the phased are lifted, as W says.
- **Other readers of v8440, each ending itself:** 0B1F (Hack, #453 -> #610 -> #611 sends 1C7D -> #609), 0C72 (anti-heal: its stop, then `v8440` -> 1C7D), 01C7 #1186 (healing reduction), 51F0, 4F81, 51A3, 3692, 5F18, 5F34, 60DA, 4EFE, 56D3 (not in the mod's kits, or not read further).
- **Other setters of v8440:** 00CB, 0EE4, 463A, 52D7, 00CA, 1065, 1A2A, 017B, 0C19: not read. A future hero with a self-cleanse starts here.
- **1C7D** is the status's "I was cleansed". 180C #93 hears it; within its first 0.65 s (#129 -> #130 v7310, #131) it sends **1955** to Kiriko and 2467. 30D8 #88 hears 1955 -> 0.5 s -> voice **FC5** (#93, #89). Built (#121, [polish4](../testing/polish4.md)): the mod's senders are Sleep 224E #418, Rally's stun 224E #416, Hack 0B1F #611, the grenade 0C72 #275, Claws Out 5BAF #91; `Combat.cleanse` returns whether one was cleansed. 01C7 #1186 ends with no 1C7D, and 6123 (Local Anesthetic) holds neither v8440 nor 1C7D. Those gates are also on Entry in 0C72 and 0B1F: a status that starts on a phased one would tell at once, which is what the 0.65 s is for. Whether they start there wasn't found.
- **The game's own words** (dump-strings): 294C1 "Removes Cleansable status effects and Stuns. Ignored by Knockdown and Displace."; 294E5 a Sleep "can be removed by Greater Cleanse"; 294E6 a Stun "is removed by Greater Cleanse"; 29E21 Knockdown "A Stun that cannot be Cleansed". Which ability wears which word wasn't found: raw STU GUIDs are obfuscated, so a byte search for a .07C found nothing (W: Suzu is "greater cleanse").

## Filter mask m_4EA686DB
20 (bits 16, 4) is on 224E #96, Cyberspace's enemy tracker (6102), the Disruptor field's (1443), Catnapper's (5B7B), EMP's (0B31) and the rock's (1EB9 #338); the bash's is 148 (128 + 20); Flux's lift 16. Read as "not the phased or invulnerable": **M**, by these uses. Hence an area's effect drops a phased hero and takes them back after.

## Where the mod holds it
`Combat.cleanse`'s table is the list, every negative state and what happens to it (`Combat.phaseStops` says what the other phases, Resurrect, Translocator, Swift Step, do with it). **A hit that passes through the phased must skip them before its marker and sound, not after:** the critique found Catnapper's tether, Soundwave's lockout, EMP's stealth end, Hyper Strike's look all landing on a target the state itself refused. A stop-sound packet names a sound, not one playing: send it where it stops nothing else. A new negative state must get a row: a door in (`Combat.phaseStops`) and a way out (a `cleanse` of its own, called from there). A held look needs an end in `Effects.ENDS` or a late viewer is shown a state that's gone ([visual_review](../testing/visual_review.md) for checking looks).

## List ops, for reading any graph
On field `m_26E3B366`: **4C3509FB** add `m_value`, **DA54F8F7** remove (`m_value`, `m_index` or `m_key`), **1C2CC6D5** clear. M, by every use read. It's how 0EE3 #890 keeps the bashed target out of Rally's shockwave ([heroes/brigitte](../heroes/brigitte.md)), and 1259 #1001 the same on the whip's area.
