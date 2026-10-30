"""What to track for each hero in a movement recorder log (tools/workshop/movement_recorder.txt): what their keys are,
and named columns made from the log's generic channels (Shift / E / Q in use, the buttons, the resource meters, charges,
cooldowns, walls, forms). parse_log.py --csv adds these columns for the hero each tick is on.

Each hero's entry: 'keys' (what Shift, E, Q and the fire buttons are, for reading the log) and 'columns' (name: the
tick field it comes from). The kits are as of 2026-10; a mapping marked '?' in its key text is a guess at which
generic channel the game uses for it (which button's resource meter holds a hero's fuel): check it against the first
log of that hero and fix it here.
"""

HEROES = {
    'Kiriko': {
        'keys': 'LMB Healing Ofuda | RMB Kunai | Shift Swift Step | E Protection Suzu | Q Kitsune Rush | wall climb',
        'columns': {'climbing': 'wall', 'swift_step': 'ab1', 'suzu': 'ab2', 'rush': 'ult', 'ofuda_held': 'fire1',
                    'kunai_held': 'fire2'},
    },
    'Lúcio': {
        'keys': 'LMB Sonic Amplifier | RMB Soundwave | Shift Crossfade | E Amp It Up | Q Sound Barrier | wall ride',
        # the Workshop can't read his song: Crossfade presses are counted (xf); the song is the one he started on when
        # the count is even (say which in the notes)
        'columns': {'riding': 'wall', 'crossfades': 'xf', 'amped': 'ab2', 'barrier': 'ult', 'soundwave_held': 'fire2'},
    },
    'Jetpack Cat': {
        'keys': 'LMB fire | RMB Frenetic Flight (boost) | Shift Lifeline | Q Catnapper',
        'columns': {'boost_held': 'fire2', 'flight_energy': 'res_fire2', 'lifeline': 'ab1', 'catnapper': 'ult'},
    },
    'Mercy': {
        'keys': 'LMB heal beam (staff) / blaster | RMB damage boost (staff) | Shift Guardian Angel | E Resurrect | '
                'Q Valkyrie | Jump falling: Angelic Descent',
        'columns': {'guardian_angel': 'ab1', 'resurrect': 'ab2', 'valkyrie': 'ult', 'heal_held': 'fire1',
                    'boost_held': 'fire2', 'jump_held': 'jump', 'crouch_held': 'crouch'},
    },
    'Lifeweaver': {
        'keys': 'LMB Healing Blossom (hold) | RMB Thorn Volley | Shift Petal Platform | E Life Grip | Jump in the air: '
                'Rejuvenating Dash | Q Tree of Life',
        'columns': {'platform': 'ab1', 'grip': 'ab2', 'tree': 'ult', 'blossom_held': 'fire1', 'thorns_held': 'fire2',
                    'dash_cooldown?': 'cd_ab1'},
    },
    'Ana': {
        'keys': 'LMB Biotic Rifle | RMB Take Aim (scope) | Shift Sleep Dart | E Biotic Grenade | Q Nano Boost',
        'columns': {'scoped_held': 'fire2', 'sleep_dart': 'ab1', 'grenade': 'ab2', 'nano': 'ult',
                    'sleep_cooldown': 'cd_ab1', 'grenade_cooldown': 'cd_ab2'},
    },
    'Sombra': {
        'keys': 'LMB Machine Pistol | RMB Hack | Shift Translocator? | E Virus? | Q EMP | Stealth?',
        'columns': {'hack_held': 'fire2', 'shift': 'ab1', 'e': 'ab2', 'emp': 'ult', 'hacking': 'firing2'},
    },
    'Brigitte': {
        'keys': 'LMB Rocket Flail | RMB Barrier Shield | LMB with shield: Shield Bash | Shift Whip Shot | E Repair Pack | '
                'Q Rally',
        'columns': {'shield_held': 'fire2', 'whip_shot': 'ab1', 'repair_pack': 'ab2', 'rally': 'ult',
                    'shield_meter?': 'res_fire2'},
    },
    'Juno': {
        'keys': 'LMB Mediblaster | RMB Pulsar Torpedoes | Shift Glide Boost | E Hyper Ring | Q Orbital Ray | '
                'Jump in the air: hover',
        'columns': {'glide_boost': 'ab1', 'hyper_ring': 'ab2', 'orbital_ray': 'ult', 'hover_meter?': 'res_jump',
                    'jump_held': 'jump'},
    },
    'Sojourn': {
        'keys': 'LMB Railgun | RMB railgun shot (charge) | Shift Power Slide | E Disruptor Shot | Q Overclock',
        'columns': {'power_slide': 'ab1', 'disruptor': 'ab2', 'overclock': 'ult', 'rail_charge?': 'res_fire2'},
    },
    'Sigma': {
        'keys': 'LMB Hyperspheres | RMB Experimental Barrier | Shift Kinetic Grasp | E Accretion | Q Gravitic Flux',
        'columns': {'barrier_held': 'fire2', 'grasp': 'ab1', 'accretion': 'ab2', 'flux': 'ult',
                    'barrier_meter?': 'res_fire2'},
    },
    'D.Va': {
        'keys': 'LMB Fusion Cannons | RMB Defense Matrix | Shift Boosters | E Micro Missiles | Q Self-Destruct / '
                'Call Mech | out of the mech: alternate form',
        'columns': {'pilot': 'alt_form', 'boosters': 'ab1', 'missiles': 'ab2', 'matrix_held': 'fire2',
                    'matrix_meter?': 'res_fire2', 'self_destruct': 'ult'},
    },
    'Pharah': {
        'keys': 'LMB Rocket Launcher | Shift Jump Jet? | E Concussive Blast | Jump held: Hover Jets | Q Barrage',
        'columns': {'jump_jet': 'ab1', 'concussive': 'ab2', 'barrage': 'ult', 'fuel?': 'res_jump', 'jump_held': 'jump'},
    },
    'Tracer': {
        'keys': 'LMB Pulse Pistols | Shift Blink (3 charges) | E Recall | Q Pulse Bomb',
        'columns': {'blink': 'ab1', 'blinks_left': 'charge_ab1', 'blink_cooldown': 'cd_ab1', 'recall': 'ab2',
                    'pulse_bomb': 'ult'},
    },
    'Cassidy': {
        'keys': 'LMB Peacekeeper | RMB Fan the Hammer | Shift Combat Roll | E grenade | Q Deadeye',
        'columns': {'combat_roll': 'ab1', 'grenade': 'ab2', 'deadeye': 'ult'},
    },
    'Junkrat': {
        'keys': 'LMB Frag Launcher | Shift Concussion Mine (2 charges) | E Steel Trap | Q RIP-Tire',
        'columns': {'mine': 'ab1', 'mines_left': 'charge_ab1', 'trap': 'ab2', 'tire': 'ult'},
    },
}


def columns():
    """Every named column across the heroes, in a fixed order (the CSV's hero-specific part)."""
    seen = []
    for hero in HEROES.values():
        for name in hero['columns']:
            if name not in seen:
                seen.append(name)
    return seen


def named(tick):
    """The tick's hero-specific columns: the ones its hero has, from its generic fields; the rest left empty."""
    hero = HEROES.get(tick.get('hero', ''), {})
    out = {}
    for name, field in hero.get('columns', {}).items():
        out[name] = tick.get(field, '')
    return out
