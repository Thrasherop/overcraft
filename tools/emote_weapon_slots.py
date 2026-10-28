"""Puts a hero's weapon stow windows, per weapon, into her emote data (emotes/<hero>.json), from the EntityControls
(chunk ECEC) of each emote clip's animation effect (08F) as read raw from the game data.

Why raw: TankLib 2.25's EntityControl structure predates a field. The chunk is 0x40 bytes now: 0x00 the clip (006),
0x08 a second GUID (0 so far), 0x10 the identifier (01C: which child-entity slot of the hero it controls), 0x20 the
action (low 16 bits; 1 = put away for the window, 4 + a clip = stays out and plays that clip). TankLib's "Action" byte is
the identifier's low byte, which is why tools/emote_data.py's STOW = 204 matched Lúcio's slot 12CC and nothing of Mercy's.
The reader is E:\\Overwatch Archive\\research\\mercy\\scripts\\emote_weapons\\ecec.py (offline, Overwatch closed); its
output (ecec_emotes.json, every control of every clip of her emotes) is what this reads. Research:
E:\\Overwatch Archive\\research\\mercy\\emote_weapons.md (2026-10-28), checked against contacts: inside the action-1
windows a held staff would pass through her head and arms, at every window edge her hand is back on it.

Mercy's slots (her entity 00E4's STUChildEntityDefinition): 12A5 the Caduceus Staff (185F), 206C the Caduceus Blaster
(025E). "stow" gets the staff's windows, "stowBlaster" the blaster's; with no control on a slot that weapon stays out.
The staff's own clips in Caduceus, Parasol and Flurry (action 4: 1627, 4DC8, 852E) aren't played yet: the staff is held.

    py -I tools/emote_weapon_slots.py mercy "E:\\Overwatch Archive\\research\\mercy\\emote_weapons\\ecec_emotes.json"
"""
import json
import sys
from pathlib import Path

SLOTS = {"mercy": {"stow": "12A5.01C", "stowBlaster": "206C.01C"}}
PUT_AWAY = 1


def windows(clip, slot):
    out = []
    for comp in clip.get("components", []):
        for child in comp["children"]:
            if child["tag"] == "ECEC" and child["identifier"] == slot and child["action"] == PUT_AWAY and not child["anim"]:
                out.append([comp["start"], round(comp["start"] + comp["duration"], 4)])
    return sorted(out)


def main():
    hero, controls = sys.argv[1], json.loads(Path(sys.argv[2]).read_text(encoding="utf-8"))
    path = Path(__file__).resolve().parent.parent / "src/main/resources/assets/overcraft/emotes" / (hero + ".json")
    data = json.loads(path.read_text(encoding="utf-8"))
    for emote, entry in data["emotes"].items():
        for name, clip in entry["clips"].items():
            read = controls.get(emote, {}).get(clip["id"])
            if read is None:
                print("no controls read for", emote, name, clip["id"])
                continue
            for key, slot in SLOTS[hero].items():
                clip[key] = windows(read, slot)
            print(emote, name, clip["id"], {k: clip[k] for k in SLOTS[hero]})
    path.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
