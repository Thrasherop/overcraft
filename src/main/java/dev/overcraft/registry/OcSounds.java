package dev.overcraft.registry;

import dev.overcraft.Overcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/**
 * The heroes' sounds, one event per moment in the game. The jar's sounds.json points each at a vanilla sound so the mod
 * works on its own (voice lines at nothing); the player's local asset pack (tools/build_asset_pack.py sounds, mapped in
 * tools/soundmaps/) replaces them with Overwatch's own recordings.
 */
public final class OcSounds {
	/** Kitsune Rush's cast line as allies (and Kiriko) hear it: "Let the Kitsune guide you!" */
	public static final SoundEvent KIRIKO_ULT_FRIENDLY = register("kiriko.ult.friendly");
	/** ...and as enemies hear it: "(Japanese) O fox spirit, unleash your claws!" */
	public static final SoundEvent KIRIKO_ULT_ENEMY = register("kiriko.ult.enemy");

	// "_3P": what everyone but Kiriko's own player hears (HeroSounds.split)
	public static final SoundEvent KIRIKO_KUNAI_THROW = register("kiriko.kunai.throw");
	public static final SoundEvent KIRIKO_KUNAI_THROW_3P = register("kiriko.kunai.throw_3p");
	public static final SoundEvent KIRIKO_KUNAI_HIT = register("kiriko.kunai.hit");
	/** The thrower's confirmation of a critical hit. */
	public static final SoundEvent KIRIKO_KUNAI_HEADSHOT = register("kiriko.kunai.headshot");
	public static final SoundEvent KIRIKO_KUNAI_HIT_BLOCK = register("kiriko.kunai.hit_block");
	public static final SoundEvent KIRIKO_KUNAI_RELOAD = register("kiriko.kunai.reload");
	public static final SoundEvent KIRIKO_KUNAI_RELOAD_3P = register("kiriko.kunai.reload_3p");
	public static final SoundEvent KIRIKO_OFUDA_THROW = register("kiriko.ofuda.throw");
	public static final SoundEvent KIRIKO_OFUDA_THROW_3P = register("kiriko.ofuda.throw_3p");
	public static final SoundEvent KIRIKO_OFUDA_HEAL = register("kiriko.ofuda.heal");
	public static final SoundEvent KIRIKO_OFUDA_HIT_BLOCK = register("kiriko.ofuda.hit_block");
	public static final SoundEvent KIRIKO_SUZU_THROW = register("kiriko.suzu.throw");
	public static final SoundEvent KIRIKO_SUZU_THROW_3P = register("kiriko.suzu.throw_3p");
	public static final SoundEvent KIRIKO_SUZU_BURST = register("kiriko.suzu.burst");
	public static final SoundEvent KIRIKO_SWIFT_STEP_CAST = register("kiriko.swift_step.cast");
	public static final SoundEvent KIRIKO_SWIFT_STEP_DEPART = register("kiriko.swift_step.depart");
	public static final SoundEvent KIRIKO_SWIFT_STEP_ARRIVE = register("kiriko.swift_step.arrive");
	public static final SoundEvent KIRIKO_CLIMB_START = register("kiriko.climb.start");
	public static final SoundEvent KIRIKO_CLIMB_STEP = register("kiriko.climb.step");
	public static final SoundEvent KIRIKO_CLIMB_TOP = register("kiriko.climb.top");
	public static final SoundEvent KIRIKO_RUSH_SUMMON = register("kiriko.rush.summon");
	public static final SoundEvent KIRIKO_RUSH_CAST = register("kiriko.rush.cast");
	public static final SoundEvent KIRIKO_RUSH_CAST_3P = register("kiriko.rush.cast_3p");
	/** Each torii gate's chime, rising in pitch from the first to the sixth. */
	public static final SoundEvent[] KIRIKO_RUSH_GATES = {register("kiriko.rush.gate_1"), register("kiriko.rush.gate_2"),
		register("kiriko.rush.gate_3"), register("kiriko.rush.gate_4"), register("kiriko.rush.gate_5"), register("kiriko.rush.gate_6")};
	/** The click when an ability can't be used (Swift Step with no ally in view). */
	public static final SoundEvent KIRIKO_ABILITY_DENIED = register("kiriko.ability.denied");
	public static final SoundEvent MELEE_SWING = register("melee.swing");
	public static final SoundEvent MELEE_HIT = register("melee.hit");
	public static final SoundEvent KIRIKO_VOICE_SWIFT_STEP = register("kiriko.voice.swift_step");
	public static final SoundEvent KIRIKO_VOICE_SUZU = register("kiriko.voice.suzu");
	public static final SoundEvent KIRIKO_VOICE_HEAL = register("kiriko.voice.heal");
	/** Lines for the game's moments (Voice.say looks them up by hero: "<hero>.voice.<moment>"). */
	public static final SoundEvent KIRIKO_VOICE_ULT_READY = register("kiriko.voice.ult_ready");
	public static final SoundEvent KIRIKO_VOICE_ELIMINATION = register("kiriko.voice.elimination");
	public static final SoundEvent KIRIKO_VOICE_MELEE_KILL = register("kiriko.voice.melee_kill");
	public static final SoundEvent KIRIKO_VOICE_MULTIKILL = register("kiriko.voice.multikill");
	public static final SoundEvent KIRIKO_VOICE_RESPAWN = register("kiriko.voice.respawn");
	public static final SoundEvent KIRIKO_VOICE_SELECT = register("kiriko.voice.select");
	public static final SoundEvent KIRIKO_VOICE_DEATH = register("kiriko.voice.death");
	public static final SoundEvent KIRIKO_VOICE_PAIN = register("kiriko.voice.pain");
	public static final SoundEvent KIRIKO_VOICE_MELEE = register("kiriko.voice.melee");
	public static final SoundEvent KIRIKO_VOICE_ON_FIRE = register("kiriko.voice.on_fire");
	/** Pings (#40): Overwatch's hero calls out what they ping, heard by their team. */
	public static final SoundEvent KIRIKO_VOICE_PING = register("kiriko.voice.ping");
	public static final SoundEvent KIRIKO_VOICE_PING_WARNING = register("kiriko.voice.ping_warning");
	public static final SoundEvent KIRIKO_VOICE_PING_ENEMY = register("kiriko.voice.ping_enemy");
	/** Kitsune Rush's corridor, heard by each player in it (RushSound, #16): the opening hits, and the three looping beds. */
	public static final SoundEvent KIRIKO_RUSH_PATH_START = register("kiriko.rush.path_start");
	public static final SoundEvent KIRIKO_RUSH_PATH_1 = register("kiriko.rush.path_1");
	public static final SoundEvent KIRIKO_RUSH_PATH_2 = register("kiriko.rush.path_2");
	public static final SoundEvent KIRIKO_RUSH_PATH_3 = register("kiriko.rush.path_3");
	/**
	 * Kiriko's lines for one ally hero or one kind of kill, "kiriko.voice.<moment>.<condition>" (Voice picks them when
	 * the condition holds: tools/reference/kiriko-voice.md, #13).
	 */
	public static final java.util.List<String> KIRIKO_VOICE_CONDITIONS = java.util.List.of("swift_step.wrecking_ball", "swift_step.widowmaker", "swift_step.sigma", "swift_step.lucio", "swift_step.roadhog", "swift_step.reinhardt", "swift_step.junker_queen", "swift_step.ashe", "swift_step.cassidy", "swift_step.mercy", "swift_step.illari", "swift_step.genji", "swift_step.hanzo", "heal.reaper", "heal.genji", "heal.hanzo", "elimination.headshot", "elimination.in_air");

	static {
		KIRIKO_VOICE_CONDITIONS.forEach(condition -> register("kiriko.voice." + condition));
	}

	// Lúcio (tools/soundmaps/lucio.txt: the game's own, from his effects and their Wwise events). "_3P": what everyone but
	// his own player hears
	public static final SoundEvent LUCIO_AMP_FIRE = register("lucio.amp.fire");
	public static final SoundEvent LUCIO_AMP_FIRE_3P = register("lucio.amp.fire_3p");
	public static final SoundEvent LUCIO_AMP_HIT = register("lucio.amp.hit");
	public static final SoundEvent LUCIO_AMP_RELOAD = register("lucio.amp.reload");
	public static final SoundEvent LUCIO_AMP_RELOAD_3P = register("lucio.amp.reload_3p");
	public static final SoundEvent LUCIO_SOUNDWAVE = register("lucio.soundwave");
	public static final SoundEvent LUCIO_SOUNDWAVE_3P = register("lucio.soundwave_3p");
	/** The song swap's sting, his own (a hard cut to the new song). */
	public static final SoundEvent LUCIO_CROSSFADE_HEAL = register("lucio.crossfade.heal");
	public static final SoundEvent LUCIO_CROSSFADE_SPEED = register("lucio.crossfade.speed");
	/** His songs, a bar at a time (LucioMusic): his own in his head, the others' at him. */
	public static final SoundEvent LUCIO_SONG_HEAL = register("lucio.song.heal");
	public static final SoundEvent LUCIO_SONG_SPEED = register("lucio.song.speed");
	public static final SoundEvent LUCIO_SONG_HEAL_3P = register("lucio.song.heal_3p");
	public static final SoundEvent LUCIO_SONG_SPEED_3P = register("lucio.song.speed_3p");
	/** What others hear as his song changes, before the new one comes in. */
	public static final SoundEvent LUCIO_SONG_SCRATCH_3P = register("lucio.song.scratch_3p");
	public static final SoundEvent LUCIO_AMP_IT_UP = register("lucio.amp_it_up");
	public static final SoundEvent LUCIO_AMP_IT_UP_3P = register("lucio.amp_it_up_3p");
	public static final SoundEvent LUCIO_WALLRIDE = register("lucio.wallride");
	public static final SoundEvent LUCIO_WALLRIDE_3P = register("lucio.wallride_3p");
	public static final SoundEvent LUCIO_WALLRIDE_START = register("lucio.wallride.start");
	public static final SoundEvent LUCIO_WALLRIDE_END = register("lucio.wallride.end");
	public static final SoundEvent LUCIO_WALL_JUMP = register("lucio.wall_jump");
	public static final SoundEvent LUCIO_BARRIER_CAST = register("lucio.barrier.cast");
	public static final SoundEvent LUCIO_BARRIER_CAST_3P = register("lucio.barrier.cast_3p");
	public static final SoundEvent LUCIO_BARRIER_SLAM = register("lucio.barrier.slam");
	/** Sound Barrier's line as allies (and Lúcio) hear it, "Oh, let's break it down!", and as enemies do (Portuguese). */
	public static final SoundEvent LUCIO_ULT_FRIENDLY = register("lucio.ult.friendly");
	public static final SoundEvent LUCIO_ULT_ENEMY = register("lucio.ult.enemy");
	/** His lines by moment (Voice.say: "lucio.voice.<moment>"), the game's own pools (lucio.txt's voice section). */
	public static final java.util.List<String> LUCIO_VOICE = java.util.List.of("elimination", "multikill", "melee_kill",
		"on_fire", "respawn", "melee", "select", "death", "pain", "ult_ready", "ping", "ping_warning", "ping_enemy",
		"crossfade_heal", "crossfade_speed", "amp", "soundwave", "wallride");

	/**
	 * Jetpack Cat's (tools/soundmaps/jetpack_cat.txt, read from her effects' Wwise events): her own in her head, fire
	 * and reload also heard by everyone else ("_3p"); the loops are played and stopped by the client (JetpackCatSounds).
	 */
	/**
	 * Mercy's (tools/soundmaps/mercy.txt, read from her effects' Wwise events): her own in her head, "_3p" what everyone
	 * else hears at her; the loops (the beams, Angelic Descent, Valkyrie) are played and stopped by each client
	 * (MercySounds).
	 */
	public static final SoundEvent MERCY_BEAM_LOCK = register("mercy.beam.lock");
	public static final SoundEvent MERCY_BEAM_UNLOCK = register("mercy.beam.unlock");
	public static final SoundEvent MERCY_HEAL_START = register("mercy.beam.heal.start");
	public static final SoundEvent MERCY_HEAL_START_3P = register("mercy.beam.heal.start_3p");
	public static final SoundEvent MERCY_HEAL_LOOP = register("mercy.beam.heal.loop");
	public static final SoundEvent MERCY_HEAL_LOOP_3P = register("mercy.beam.heal.loop_3p");
	public static final SoundEvent MERCY_HEAL_END = register("mercy.beam.heal.end");
	public static final SoundEvent MERCY_HEAL_END_3P = register("mercy.beam.heal.end_3p");
	public static final SoundEvent MERCY_BOOST_START = register("mercy.beam.boost.start");
	public static final SoundEvent MERCY_BOOST_START_3P = register("mercy.beam.boost.start_3p");
	public static final SoundEvent MERCY_BOOST_LOOP = register("mercy.beam.boost.loop");
	public static final SoundEvent MERCY_BOOST_LOOP_3P = register("mercy.beam.boost.loop_3p");
	public static final SoundEvent MERCY_BOOST_END = register("mercy.beam.boost.end");
	public static final SoundEvent MERCY_BOOST_END_3P = register("mercy.beam.boost.end_3p");
	public static final SoundEvent MERCY_BLASTER_FIRE = register("mercy.blaster.fire");
	public static final SoundEvent MERCY_BLASTER_HIT = register("mercy.blaster.hit");
	public static final SoundEvent MERCY_BLASTER_HIT_3P = register("mercy.blaster.hit_3p");
	public static final SoundEvent MERCY_BLASTER_HIT_BLOCK = register("mercy.blaster.hit_block");
	public static final SoundEvent MERCY_RELOAD = register("mercy.blaster.reload");
	public static final SoundEvent MERCY_RELOAD_3P = register("mercy.blaster.reload_3p");
	public static final SoundEvent MERCY_SWAP_STAFF = register("mercy.swap.staff");
	public static final SoundEvent MERCY_SWAP_STAFF_3P = register("mercy.swap.staff_3p");
	public static final SoundEvent MERCY_SWAP_BLASTER = register("mercy.swap.blaster");
	public static final SoundEvent MERCY_SWAP_BLASTER_3P = register("mercy.swap.blaster_3p");
	public static final SoundEvent MERCY_MELEE_STAFF = register("mercy.melee.staff");
	public static final SoundEvent MERCY_MELEE_STAFF_3P = register("mercy.melee.staff_3p");
	public static final SoundEvent MERCY_MELEE_BLASTER = register("mercy.melee.blaster");
	public static final SoundEvent MERCY_MELEE_BLASTER_3P = register("mercy.melee.blaster_3p");
	public static final SoundEvent MERCY_GA_START = register("mercy.ga.start");
	public static final SoundEvent MERCY_GA_START_3P = register("mercy.ga.start_3p");
	public static final SoundEvent MERCY_GA_LAUNCH = register("mercy.ga.launch");
	public static final SoundEvent MERCY_GA_LAUNCH_3P = register("mercy.ga.launch_3p");
	public static final SoundEvent MERCY_DESCENT_START = register("mercy.descent.start");
	public static final SoundEvent MERCY_DESCENT_START_3P = register("mercy.descent.start_3p");
	public static final SoundEvent MERCY_DESCENT_LOOP = register("mercy.descent.loop");
	public static final SoundEvent MERCY_DESCENT_LOOP_3P = register("mercy.descent.loop_3p");
	public static final SoundEvent MERCY_DESCENT_END = register("mercy.descent.end");
	public static final SoundEvent MERCY_DESCENT_END_3P = register("mercy.descent.end_3p");
	public static final SoundEvent MERCY_RES_CAST = register("mercy.resurrect.cast");
	public static final SoundEvent MERCY_RES_CAST_3P = register("mercy.resurrect.cast_3p");
	public static final SoundEvent MERCY_RES_DONE = register("mercy.resurrect.done");
	public static final SoundEvent MERCY_RES_REVIVED = register("mercy.resurrect.revived");
	public static final SoundEvent MERCY_FLASH_CAST = register("mercy.flash_heal.cast");
	public static final SoundEvent MERCY_FLASH_CAST_3P = register("mercy.flash_heal.cast_3p");
	public static final SoundEvent MERCY_FLASH_HEAL = register("mercy.flash_heal.heal");
	public static final SoundEvent MERCY_VALKYRIE_CAST = register("mercy.valkyrie.cast");
	public static final SoundEvent MERCY_VALKYRIE_CAST_3P = register("mercy.valkyrie.cast_3p");
	public static final SoundEvent MERCY_VALKYRIE_LOOP = register("mercy.valkyrie.loop");
	public static final SoundEvent MERCY_VALKYRIE_LOOP_3P = register("mercy.valkyrie.loop_3p");
	public static final SoundEvent MERCY_VALKYRIE_ENDING = register("mercy.valkyrie.ending");
	public static final SoundEvent MERCY_VALKYRIE_ENDING_3P = register("mercy.valkyrie.ending_3p");
	public static final SoundEvent MERCY_VALKYRIE_END = register("mercy.valkyrie.end");
	public static final SoundEvent MERCY_VALKYRIE_END_3P = register("mercy.valkyrie.end_3p");
	/** Her lines by moment (Voice.say: "mercy.voice.<moment>"), the game's own pools (mercy.txt's voice section). */
	public static final java.util.List<String> MERCY_VOICE = java.util.List.of("elimination", "multikill", "melee_kill",
		"on_fire", "respawn", "melee", "select", "death", "pain", "ult_ready", "ping", "ping_warning", "ping_enemy",
		"beam_heal", "beam_boost", "beam_heal_ongoing", "beam_boost_ongoing", "guardian_angel", "resurrect",
		"resurrect_enemy");
	/** Valkyrie's line as allies (and Mercy) hear it, "Heroes never die!", and as enemies do. */
	public static final SoundEvent MERCY_ULT_FRIENDLY = register("mercy.ult.friendly");
	public static final SoundEvent MERCY_ULT_ENEMY = register("mercy.ult.enemy");
	public static final SoundEvent JC_PAW_FIRE = register("jetpack_cat.paw.fire");
	public static final SoundEvent JC_PAW_FIRE_3P = register("jetpack_cat.paw.fire_3p");
	public static final SoundEvent JC_PAW_HEAL = register("jetpack_cat.paw.heal");
	public static final SoundEvent JC_RELOAD = register("jetpack_cat.reload");
	public static final SoundEvent JC_RELOAD_3P = register("jetpack_cat.reload_3p");
	public static final SoundEvent JC_HUM = register("jetpack_cat.jetpack.hum");
	public static final SoundEvent JC_UP = register("jetpack_cat.jetpack.up");
	public static final SoundEvent JC_DOWN = register("jetpack_cat.jetpack.down");
	public static final SoundEvent JC_BOOST_START = register("jetpack_cat.boost.start");
	public static final SoundEvent JC_BOOST_LOOP = register("jetpack_cat.boost.loop");
	public static final SoundEvent JC_BOOST_END = register("jetpack_cat.boost.end");
	public static final SoundEvent JC_BOOST_REGEN = register("jetpack_cat.boost.regen");
	public static final SoundEvent JC_PURR_CAST = register("jetpack_cat.purr.cast");
	public static final SoundEvent JC_PURR_PULSE = register("jetpack_cat.purr.pulse");
	public static final SoundEvent JC_PURR_HEAL = register("jetpack_cat.purr.heal");
	public static final SoundEvent JC_LIFELINE_ATTACH = register("jetpack_cat.lifeline.attach");
	public static final SoundEvent JC_LIFELINE_LOOP = register("jetpack_cat.lifeline.loop");
	public static final SoundEvent JC_LIFELINE_DETACH = register("jetpack_cat.lifeline.detach");
	public static final SoundEvent JC_LIFELINE_ACTIVE = register("jetpack_cat.lifeline.active");
	public static final SoundEvent JC_LIFELINE_ON = register("jetpack_cat.lifeline.on");
	public static final SoundEvent JC_ULT_CAST = register("jetpack_cat.ult.cast");
	public static final SoundEvent JC_ULT_AIM_START = register("jetpack_cat.ult.aim_start");
	public static final SoundEvent JC_ULT_AIM = register("jetpack_cat.ult.aim");
	public static final SoundEvent JC_ULT_DIVE = register("jetpack_cat.ult.dive");
	public static final SoundEvent JC_ULT_DIVE_LOOP = register("jetpack_cat.ult.dive_loop");
	public static final SoundEvent JC_ULT_IMPACT = register("jetpack_cat.ult.impact");
	public static final SoundEvent JC_ULT_TETHER_START = register("jetpack_cat.ult.tether_start");
	public static final SoundEvent JC_ULT_TETHER = register("jetpack_cat.ult.tether");
	public static final SoundEvent JC_ULT_TETHER_END = register("jetpack_cat.ult.tether_end");
	public static final SoundEvent JC_ULT_FRIENDLY = register("jetpack_cat.ult.friendly");
	public static final SoundEvent JC_ULT_ENEMY = register("jetpack_cat.ult.enemy");
	/**
	 * Her lines by moment (Voice.say: "jetpack_cat.voice.<moment>"), the game's own pools (the map's voice section). Her
	 * wheel lines ("call_...") are every hero's, registered in init().
	 */
	public static final java.util.List<String> JETPACK_CAT_VOICE = java.util.List.of(
		"elimination", "multikill", "melee_kill", "on_fire", "respawn", "melee", "select", "death", "pain", "ult_ready",
		"ping", "ping_warning", "ping_enemy", "boost", "reload", "heal", "purr", "purr_end", "lifeline", "tether_end",
		"tether_attached");

	static {
		LUCIO_VOICE.forEach(moment -> register("lucio.voice." + moment));
		JETPACK_CAT_VOICE.forEach(moment -> register("jetpack_cat.voice." + moment));
		MERCY_VOICE.forEach(moment -> register("mercy.voice." + moment));
	}

	private OcSounds() {
	}

	/** Overwatch's end of a won match: the announcer's "Victory!" and its music (#18). From the local pack only. */
	public static final SoundEvent ANNOUNCER_VICTORY = register("announcer.victory");
	public static final SoundEvent MUSIC_VICTORY = register("music.victory");

	private static SoundEvent register(String name) {
		return Registry.register(BuiltInRegistries.SOUND_EVENT, Overcraft.id(name), SoundEvent.createVariableRangeEvent(Overcraft.id(name)));
	}

	/**
	 * Every hero's communication-wheel lines (dev.overcraft.comms): "<hero>.voice.call_<callout>", and the ult status's
	 * charging and almost-ready pools (its ready one is the hero's ult_ready). Silent until the hero's sounds are in the
	 * local pack.
	 */
	public static void init() {
		for (dev.overcraft.hero.Hero hero : dev.overcraft.hero.Hero.values()) {
			for (dev.overcraft.comms.Callout callout : dev.overcraft.comms.Callout.values()) {
				if (callout != dev.overcraft.comms.Callout.ULTIMATE_STATUS) {
					register(hero.id + ".voice.call_" + callout.id);
				}
			}
			register(hero.id + ".voice.call_ult_charging");
			register(hero.id + ".voice.call_ult_almost");
			// what every hero says when a Mercy resurrects or damage boosts them (stimuli 005A, 00CC)
			register(hero.id + ".voice.resurrected");
			register(hero.id + ".voice.damage_boosted");
		}
	}
}
