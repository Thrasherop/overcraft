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
	/** A shot hitting a creature (73A4: her copy, everyone else's at the hit), and the centre shot hitting a block (093B). */
	public static final SoundEvent JC_PAW_HIT = register("jetpack_cat.paw.hit");
	public static final SoundEvent JC_PAW_HIT_3P = register("jetpack_cat.paw.hit_3p");
	public static final SoundEvent JC_PAW_HIT_BLOCK = register("jetpack_cat.paw.hit_block");
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

	/**
	 * Lifeweaver's (tools/soundmaps/lifeweaver.txt, read from his effects' Wwise events): his own in his head, "_3p" what
	 * everyone else hears from him or the prop; the charge-scaled ones in three bins by the charge, [low, mid, full]
	 * (21C6 #954's: c <= 0.3, <= 0.9, above).
	 */
	public static final SoundEvent LW_CHARGE_START = register("lifeweaver.blossom.charge.start");
	public static final SoundEvent LW_CHARGE_START_3P = register("lifeweaver.blossom.charge.start_3p");
	public static final SoundEvent LW_CHARGE_LOOP = register("lifeweaver.blossom.charge.loop");
	public static final SoundEvent LW_CHARGE_LOOP_3P = register("lifeweaver.blossom.charge.loop_3p");
	public static final SoundEvent[] LW_CHARGE_END = {register("lifeweaver.blossom.charge.end_low"),
		register("lifeweaver.blossom.charge.end_mid"), register("lifeweaver.blossom.charge.end")};
	public static final SoundEvent[] LW_CHARGE_END_3P = {register("lifeweaver.blossom.charge.end_low_3p"),
		register("lifeweaver.blossom.charge.end_mid_3p"), register("lifeweaver.blossom.charge.end_3p")};
	public static final SoundEvent LW_BLOSSOM_FULL = register("lifeweaver.blossom.full");
	public static final SoundEvent[] LW_BLOSSOM_THROW = {register("lifeweaver.blossom.throw_low"),
		register("lifeweaver.blossom.throw_mid"), register("lifeweaver.blossom.throw")};
	public static final SoundEvent LW_BLOSSOM_THROW_3P = register("lifeweaver.blossom.throw_3p");
	public static final SoundEvent[] LW_BLOSSOM_HEAL = {register("lifeweaver.blossom.heal_low"),
		register("lifeweaver.blossom.heal_mid"), register("lifeweaver.blossom.heal")};
	public static final SoundEvent LW_BLOSSOM_RELOAD = register("lifeweaver.blossom.reload");
	public static final SoundEvent LW_BLOSSOM_RELOAD_3P = register("lifeweaver.blossom.reload_3p");
	public static final SoundEvent LW_THORN_FIRE = register("lifeweaver.thorn.fire");
	public static final SoundEvent LW_THORN_FIRE_3P = register("lifeweaver.thorn.fire_3p");
	public static final SoundEvent LW_THORN_FIRE_START = register("lifeweaver.thorn.fire_start");
	public static final SoundEvent LW_THORN_FIRE_START_3P = register("lifeweaver.thorn.fire_start_3p");
	public static final SoundEvent LW_THORN_SHOT = register("lifeweaver.thorn.shot");
	public static final SoundEvent LW_THORN_HIT = register("lifeweaver.thorn.hit");
	public static final SoundEvent LW_THORN_HIT_3P = register("lifeweaver.thorn.hit_3p");
	public static final SoundEvent LW_THORN_HIT_BLOCK = register("lifeweaver.thorn.hit_block");
	public static final SoundEvent LW_THORN_RELOAD = register("lifeweaver.thorn.reload");
	public static final SoundEvent LW_THORN_RELOAD_3P = register("lifeweaver.thorn.reload_3p");
	public static final SoundEvent LW_SWAP_THORNS = register("lifeweaver.swap.thorns");
	public static final SoundEvent LW_SWAP_BLOSSOM = register("lifeweaver.swap.blossom");
	public static final SoundEvent LW_MELEE = register("lifeweaver.melee");
	public static final SoundEvent LW_MELEE_3P = register("lifeweaver.melee_3p");
	public static final SoundEvent LW_PLATFORM_THROW = register("lifeweaver.platform.throw");
	public static final SoundEvent LW_PLATFORM_THROW_3P = register("lifeweaver.platform.throw_3p");
	public static final SoundEvent LW_PLATFORM_FLIGHT = register("lifeweaver.platform.flight");
	public static final SoundEvent LW_PLATFORM_DEPLOY = register("lifeweaver.platform.deploy");
	public static final SoundEvent LW_PLATFORM_DEPLOY_ENEMY = register("lifeweaver.platform.deploy_enemy");
	public static final SoundEvent LW_PLATFORM_ARMED = register("lifeweaver.platform.armed");
	public static final SoundEvent LW_PLATFORM_RISE = register("lifeweaver.platform.rise");
	public static final SoundEvent LW_PLATFORM_RISE_LOOP = register("lifeweaver.platform.rise.loop");
	public static final SoundEvent LW_PLATFORM_RISE_END = register("lifeweaver.platform.rise.end");
	public static final SoundEvent LW_PLATFORM_RAISED = register("lifeweaver.platform.raised");
	public static final SoundEvent LW_PLATFORM_RAISED_LOOP = register("lifeweaver.platform.raised.loop");
	public static final SoundEvent LW_PLATFORM_WARNING = register("lifeweaver.platform.warning");
	public static final SoundEvent LW_PLATFORM_DESCEND = register("lifeweaver.platform.descend");
	public static final SoundEvent LW_PLATFORM_LANDED = register("lifeweaver.platform.landed");
	public static final SoundEvent LW_PLATFORM_DESTROYED = register("lifeweaver.platform.destroyed");
	public static final SoundEvent LW_DASH = register("lifeweaver.dash");
	public static final SoundEvent LW_DASH_3P = register("lifeweaver.dash_3p");
	public static final SoundEvent LW_DASH_HEAL = register("lifeweaver.dash.heal");
	public static final SoundEvent LW_GRIP_CAST = register("lifeweaver.grip.cast");
	public static final SoundEvent LW_GRIP_CAST_3P = register("lifeweaver.grip.cast_3p");
	public static final SoundEvent LW_GRIP_CAST_ENEMY = register("lifeweaver.grip.cast_enemy");
	public static final SoundEvent LW_GRIP_VINE = register("lifeweaver.grip.vine");
	public static final SoundEvent LW_GRIP_LAND = register("lifeweaver.grip.land");
	public static final SoundEvent LW_GRIP_LAND_3P = register("lifeweaver.grip.land_3p");
	public static final SoundEvent LW_GRIP_LAND_ALLY = register("lifeweaver.grip.land_ally");
	public static final SoundEvent LW_GRIP_ALLY = register("lifeweaver.grip.ally");
	public static final SoundEvent LW_TREE_AIM = register("lifeweaver.tree.aim");
	public static final SoundEvent LW_TREE_PREVIEW = register("lifeweaver.tree.preview");
	public static final SoundEvent LW_TREE_CANCEL = register("lifeweaver.tree.cancel");
	public static final SoundEvent LW_TREE_SPAWN = register("lifeweaver.tree.spawn");
	public static final SoundEvent LW_TREE_SPAWN_3P = register("lifeweaver.tree.spawn_3p");
	public static final SoundEvent LW_TREE_LOOP = register("lifeweaver.tree.loop");
	public static final SoundEvent LW_TREE_LOOP_3P = register("lifeweaver.tree.loop_3p");
	public static final SoundEvent LW_TREE_LOOP_ENEMY = register("lifeweaver.tree.loop_enemy");
	public static final SoundEvent LW_TREE_PULSE = register("lifeweaver.tree.pulse");
	public static final SoundEvent LW_TREE_PULSE_7 = register("lifeweaver.tree.pulse_7");
	public static final SoundEvent LW_TREE_PULSE_8 = register("lifeweaver.tree.pulse_8");
	public static final SoundEvent LW_TREE_PULSE_ENEMY = register("lifeweaver.tree.pulse_enemy");
	public static final SoundEvent LW_TREE_HEAL = register("lifeweaver.tree.heal");
	public static final SoundEvent LW_TREE_HEAL_3P = register("lifeweaver.tree.heal_3p");
	public static final SoundEvent LW_TREE_WITHER = register("lifeweaver.tree.wither");
	public static final SoundEvent LW_TREE_DEATH = register("lifeweaver.tree.death");
	public static final SoundEvent LW_SUPERBLOOM = register("lifeweaver.superbloom");
	public static final SoundEvent LW_SUPERBLOOM_3P = register("lifeweaver.superbloom_3p");
	public static final SoundEvent LW_SEED_THROW = register("lifeweaver.seed.throw");
	public static final SoundEvent LW_SEED_THROW_3P = register("lifeweaver.seed.throw_3p");
	public static final SoundEvent LW_SEED_PICKUP = register("lifeweaver.seed.pickup");
	/** The Tree of Life's line as allies (and he) hear it, and as enemies do. */
	public static final SoundEvent LW_ULT_FRIENDLY = register("lifeweaver.ult.friendly");
	public static final SoundEvent LW_ULT_ENEMY = register("lifeweaver.ult.enemy");
	/**
	 * His lines by moment (Voice.say: "lifeweaver.voice.<moment>"), the game's own pools (the map's voice section). His
	 * wheel lines, "resurrected" and "damage_boosted" are every hero's, registered in init().
	 */
	public static final java.util.List<String> LIFEWEAVER_VOICE = java.util.List.of("elimination", "multikill",
		"melee_kill", "on_fire", "respawn", "melee", "select", "death", "pain", "ult_ready", "ping", "ping_warning",
		"ping_enemy", "heal", "reload", "platform", "platform_used", "dash", "grip", "tree_end");

	/**
	 * Ana's (tools/soundmaps/ana.txt, read from her effects' and clips' Wwise events): her own in her head, "_3p" what
	 * everyone else hears from her, the dart's target or the ally; "_target" / "_ally" what the one hit, slept or boosted
	 * hears; "_enemy" the boost's other team copy.
	 */
	public static final SoundEvent ANA_FIRE = register("ana.rifle.fire");
	public static final SoundEvent ANA_FIRE_3P = register("ana.rifle.fire_3p");
	public static final SoundEvent ANA_BOLT = register("ana.rifle.bolt");
	public static final SoundEvent ANA_BOLT_3P = register("ana.rifle.bolt_3p");
	public static final SoundEvent ANA_DART_SHOT = register("ana.rifle.dart.shot");
	public static final SoundEvent ANA_DART_FLIGHT = register("ana.rifle.dart.flight");
	public static final SoundEvent ANA_SCOPED_FIRE = register("ana.rifle.scoped.fire");
	public static final SoundEvent ANA_SCOPED_FIRE_3P = register("ana.rifle.scoped.fire_3p");
	public static final SoundEvent ANA_SCOPED_SHOT = register("ana.rifle.scoped.shot");
	public static final SoundEvent ANA_HIT = register("ana.rifle.hit");
	public static final SoundEvent ANA_HEAL = register("ana.rifle.heal");
	public static final SoundEvent ANA_HEAL_ALLY = register("ana.rifle.heal_ally");
	public static final SoundEvent ANA_HEAL_3P = register("ana.rifle.heal_3p");
	public static final SoundEvent ANA_DAMAGE = register("ana.rifle.damage");
	public static final SoundEvent ANA_DAMAGE_TARGET = register("ana.rifle.damage_target");
	public static final SoundEvent ANA_DAMAGE_3P = register("ana.rifle.damage_3p");
	public static final SoundEvent ANA_HEADSHOT = register("ana.rifle.headshot");
	public static final SoundEvent ANA_HIT_BLOCK = register("ana.rifle.hit_block");
	public static final SoundEvent ANA_SCOPE_IN = register("ana.scope.in");
	public static final SoundEvent ANA_SCOPE_IN_3P = register("ana.scope.in_3p");
	public static final SoundEvent ANA_SCOPE_LOOP = register("ana.scope.loop");
	public static final SoundEvent ANA_SCOPE_OUT = register("ana.scope.out");
	public static final SoundEvent ANA_SCOPE_OUT_3P = register("ana.scope.out_3p");
	public static final SoundEvent ANA_RELOAD = register("ana.reload");
	public static final SoundEvent ANA_RELOAD_3P = register("ana.reload_3p");
	public static final SoundEvent ANA_RELOAD_PARTIAL = register("ana.reload_partial");
	public static final SoundEvent ANA_RELOAD_PARTIAL_3P = register("ana.reload_partial_3p");
	public static final SoundEvent ANA_SLEEP_CAST = register("ana.sleep.cast");
	public static final SoundEvent ANA_SLEEP_CAST_3P = register("ana.sleep.cast_3p");
	public static final SoundEvent ANA_SLEEP_DART_SHOT = register("ana.sleep.dart.shot");
	public static final SoundEvent ANA_SLEEP_DART_HIT = register("ana.sleep.dart.hit");
	public static final SoundEvent ANA_SLEEP = register("ana.sleep");
	public static final SoundEvent ANA_SLEEP_TARGET = register("ana.sleep_target");
	public static final SoundEvent ANA_WAKE = register("ana.sleep.wake");
	public static final SoundEvent ANA_WAKE_TARGET = register("ana.sleep.wake_target");
	public static final SoundEvent ANA_GRENADE_THROW = register("ana.grenade.throw");
	public static final SoundEvent ANA_GRENADE_THROW_3P = register("ana.grenade.throw_3p");
	public static final SoundEvent ANA_GRENADE_BURST = register("ana.grenade.burst");
	public static final SoundEvent ANA_GRENADE_BURST_3P = register("ana.grenade.burst_3p");
	public static final SoundEvent ANA_GRENADE_IMPACT = register("ana.grenade.impact");
	public static final SoundEvent ANA_GRENADE_BOOST = register("ana.grenade.boost");
	public static final SoundEvent ANA_GRENADE_BOOST_3P = register("ana.grenade.boost_3p");
	public static final SoundEvent ANA_GRENADE_ANTIHEAL = register("ana.grenade.antiheal");
	public static final SoundEvent ANA_GRENADE_ANTIHEAL_3P = register("ana.grenade.antiheal_3p");
	public static final SoundEvent ANA_NANO_CAST = register("ana.nano.cast");
	public static final SoundEvent ANA_NANO_CAST_3P = register("ana.nano.cast_3p");
	public static final SoundEvent ANA_NANO_START = register("ana.nano.start");
	public static final SoundEvent ANA_NANO_START_3P = register("ana.nano.start_3p");
	public static final SoundEvent ANA_NANO_START_ENEMY = register("ana.nano.start_enemy");
	public static final SoundEvent ANA_NANO_LOOP = register("ana.nano.loop");
	public static final SoundEvent ANA_NANO_LOOP_3P = register("ana.nano.loop_3p");
	public static final SoundEvent ANA_NANO_LOOP_ENEMY = register("ana.nano.loop_enemy");
	public static final SoundEvent ANA_NANO_END = register("ana.nano.end");
	public static final SoundEvent ANA_NANO_END_3P = register("ana.nano.end_3p");
	public static final SoundEvent ANA_NANO_END_ENEMY = register("ana.nano.end_enemy");
	public static final SoundEvent ANA_MELEE = register("ana.melee");
	public static final SoundEvent ANA_MELEE_3P = register("ana.melee_3p");
	public static final SoundEvent ANA_ANESTHETIC = register("ana.perk.anesthetic");
	public static final SoundEvent ANA_ANESTHETIC_DOT = register("ana.perk.anesthetic.dot");
	public static final SoundEvent ANA_ANESTHETIC_DOT_3P = register("ana.perk.anesthetic.dot_3p");
	public static final SoundEvent ANA_ULT_FRIENDLY = register("ana.ult.friendly");
	public static final SoundEvent ANA_ULT_ENEMY = register("ana.ult.enemy");
	/**
	 * Her lines by moment (Voice.say: "ana.voice.<moment>"), the game's own pools (the map's voice section). Her wheel
	 * lines, "resurrected", "damage_boosted" and "nano_boosted" are every hero's, registered in init().
	 */
	public static final java.util.List<String> ANA_VOICE = java.util.List.of("elimination", "multikill", "melee_kill",
		"on_fire", "respawn", "melee", "select", "death", "pain", "ult_ready", "ping", "ping_warning", "ping_enemy",
		"heal_dart", "reload", "sleep", "grenade_heal", "grenade_damage", "ping_slept", "ultimate", "nano_cast");

	/**
	 * Sombra's (tools/soundmaps/sombra.txt): her sounds by name ("sombra." + name, played through
	 * dev.overcraft.sombra.SombraSounds) and her lines by moment (Voice.say: "sombra.voice.<moment>"; her wheel lines,
	 * "damage_boosted" and "nano_boosted" are every hero's, registered in init(); her set has no "resurrected").
	 */
	public static final java.util.List<String> SOMBRA_SOUNDS = java.util.List.of("pistol.fire", "pistol.fire_3p",
		"pistol.fire_end", "pistol.fire_end_3p", "pistol.shot", "pistol.hit", "pistol.hit_block", "pistol.draw",
		"pistol.draw_3p", "reload", "reload_3p", "hotfix.target.loop", "hotfix.target.end", "hotfix.cast", "hotfix.cast_3p",
		"hotfix.channel", "hotfix.channel_ally", "hotfix.channel_3p", "hack.channel", "hack.channel_3p", "hack.channel_enemy",
		"hotfix.end", "hotfix.end_3p", "hotfix.heal", "hotfix.heal_3p", "hotfix.heal.loop_3p", "hotfix.heal.end",
		"hotfix.recharge", "hotfix.data_packet", "hacked", "hacked_3p", "hacked.loop", "hacked.loop_3p", "hacked.hero_3p",
		"hacked.end_3p", "hack.object_3p", "hack.object.loop_3p", "hack.object.end_3p", "cyberspace.cast",
		"cyberspace.cast_3p", "cyberspace.shot", "cyberspace.flight", "cyberspace.hit_block", "cyberspace.zone",
		"cyberspace.zone_enemy", "cyberspace.zone_far", "cyberspace.zone.end", "cyberspace.weakened",
		"cyberspace.weakened.loop", "cyberspace.weakened_3p", "cyberspace.weakened.loop_3p", "translocator.throw",
		"translocator.throw_3p", "translocator.beacon", "translocator.beacon.loop", "translocator.beacon.loop_3p",
		"translocator.land", "translocator.depart", "translocator.depart_3p", "translocator.dest_3p", "translocator.arrive",
		"stealth.start", "stealth.start_3p", "stealth.loop", "stealth.end", "stealth.end_3p", "stealth.detected",
		"stealth.detected_3p", "emp.cast", "emp.pulse", "emp.pulse_3p", "emp.hit", "perk.life_hack", "perk.life_hack_3p",
		"ult.friendly", "ult.enemy");
	public static final java.util.List<String> SOMBRA_VOICE = java.util.List.of("elimination", "multikill", "melee_kill",
		"on_fire", "respawn", "melee", "select", "death", "pain", "ult_ready", "ping", "ping_warning", "ping_enemy", "hotfix",
		"hack", "cyberspace_hit", "cyberspace_kill", "translocate", "uncloak", "ultimate");

	/**
	 * Brigitte's (tools/soundmaps/brigitte.txt): her sounds by name ("brigitte." + name, played through
	 * dev.overcraft.brigitte.BrigitteSounds) and her lines by moment (Voice.say: "brigitte.voice.<moment>"; her wheel lines,
	 * "resurrected", "damage_boosted" and "nano_boosted" are every hero's, registered in init()).
	 */
	public static final java.util.List<String> BRIGITTE_SOUNDS = java.util.List.of(
		"barrier.break", "barrier.break_3p", "barrier.hold.loop", "barrier.hold.loop_3p", "barrier.low", "barrier.low_3p",
		"barrier.lower", "barrier.lower_3p", "barrier.raise", "barrier.raise_3p", "barrier.rally.loop",
		"barrier.rally.loop_3p", "barrier.rally_lower", "barrier.rally_lower_3p", "barrier.rally_raise",
		"barrier.rally_raise_3p", "bash.dash", "bash.dash_3p", "flail.backhand", "flail.backhand_3p", "flail.forehand",
		"flail.forehand_3p", "flail.hit", "flail.hit_3p", "flail.hit_block", "flail.impact", "flail.impact_3p",
		"flail.release", "flail.release_3p", "hit_barrier", "inspire.heal", "inspire.heal.loop", "inspire.heal_3p",
		"inspire.link.loop", "inspire.pulse", "inspire.pulse_3p", "perk.inspiring_strike", "perk.inspiring_strike_3p",
		"perk.whiplash", "perk.whiplash_3p", "rally.cast", "rally.cast_3p", "rally.end_3p", "rally.ending",
		"rally.ending_3p", "rally.loop", "rally.loop_3p", "rally.overhealth", "repair.cast", "repair.heal.end",
		"repair.heal.loop", "repair.heal.start", "repair.land", "repair.land_3p", "repair.recharge_1", "repair.recharge_2",
		"repair.recharge_3", "repair.throw_1", "repair.throw_2", "repair.throw_3", "stun", "stun.end", "stun.end_3p",
		"stun_3p", "ult.enemy", "ult.friendly", "whip.catch", "whip.hit", "whip.hit_3p", "whip.hit_block", "whip.return",
		"whip.return_3p", "whip.throw", "whip.throw_3p");
	public static final java.util.List<String> BRIGITTE_VOICE = java.util.List.of(
		"elimination", "multikill", "on_fire", "respawn", "melee", "select", "death", "pain", "ult_ready", "ping",
		"ping_warning", "ping_enemy", "barrier_raise", "bash", "bash_hit", "flail", "whip_hit", "repair_pack",
		"repair_empty", "inspire", "barrier_damaged", "barrier_low", "barrier_broken", "ultimate");

	/**
	 * Juno's (tools/soundmaps/juno.txt, read for wiring in tools/reference/juno-sounds.md): her sounds by name ("juno." + name,
	 * played through dev.overcraft.juno.JunoSounds on the server and dev.overcraft.client.JunoSounds for the loops) and her
	 * lines by moment (Voice.say: "juno.voice.<moment>"; her wheel lines, "resurrected", "damage_boosted" and "nano_boosted"
	 * are every hero's, registered in init()). The map's own pick for the ult line: ult.friendly / ult.enemy (16A9 / 16AA),
	 * not voice.ultimate (184D, the same line), which stays registered but unplayed.
	 */
	public static final java.util.List<String> JUNO_SOUNDS = java.util.List.of(
		"boots.double_jump", "boots.double_jump_3p", "boots.hover.loop", "boots.hover.loop_3p", "boots.hover_end",
		"boots.hover_end_3p", "boots.hover_start", "boots.hover_start_3p", "boots.idle.loop_3p", "glide.end", "glide.end_3p",
		"glide.ending", "glide.ending_3p", "glide.loop", "glide.loop_3p", "glide.start", "glide.start_3p", "jump", "jump_3p",
		"land", "mediblaster.hit", "mediblaster.hit_block", "mediblaster.hit_enemy", "mediblaster.hit_enemy_3p",
		"mediblaster.projectile", "mediblaster.reload", "mediblaster.reload_3p", "mediblaster.volley", "mediblaster.volley_3p",
		"mediblaster.volley_end", "mediblaster.volley_end_3p", "melee", "perk.locked_on", "perk.ring_refund", "ring.appear",
		"ring.cast", "ring.cast_3p", "ring.end", "ring.launch", "ring.loop", "ring.pass", "ring.pass_3p", "torpedo.fire",
		"torpedo.fire_3p", "torpedo.fire_alt", "torpedo.flight.loop", "torpedo.flight_enemy.loop", "torpedo.heal",
		"torpedo.heal_3p", "torpedo.heal_confirm", "torpedo.hit", "torpedo.hit_3p", "torpedo.hit_block", "torpedo.launch",
		"torpedo.launch_3p", "torpedo.lock.loop", "torpedo.lock.loop_3p", "torpedo.lock_end_3p", "torpedo.lock_start",
		"torpedo.lock_start_3p", "torpedo.lock_target", "torpedo.locking.loop", "ult.amp.end", "ult.amp.loop", "ult.amp.start",
		"ult.cast", "ult.cast_3p", "ult.enemy", "ult.friendly", "ult.ray.ending", "ult.ray.hum", "ult.ray.inside.loop");
	public static final java.util.List<String> JUNO_VOICE = java.util.List.of(
		"elimination", "multikill", "melee_kill", "on_fire", "respawn", "melee", "select", "death", "pain", "ult_ready", "ping",
		"ping_warning", "ping_enemy", "reload", "heal", "torpedo_ally", "torpedo", "ring", "ping_ring", "ultimate");

	/**
	 * Sojourn's (tools/soundmaps/sojourn.txt, read for wiring in tools/reference/sojourn-sounds.md): her sounds by name
	 * ("sojourn." + name, played through dev.overcraft.sojourn.SojournSounds on the server and dev.overcraft.client.SojournSounds
	 * for the loops) and her lines by moment (Voice.say: "sojourn.voice.<moment>"; her wheel lines, "resurrected",
	 * "damage_boosted" and "nano_boosted" are every hero's, registered in init()). Her set has no reload line (H). The ult line
	 * is ult.friendly / ult.enemy (0B19 / 0D2F); voice.ultimate (185C, her own copy, the enemy line's text) stays unplayed.
	 */
	public static final java.util.List<String> SOJOURN_SOUNDS = java.util.List.of(
		"charged.fire", "charged.fire_3p", "charged.fire_low", "charged.fire_low_3p", "charged.fire_mid", "charged.fire_mid_3p",
		"charged.hit", "charged.hit_block", "charged.hit_enemy", "charged.hit_enemy_3p", "charged.shot", "disruptor.burn.end",
		"disruptor.burn.loop", "disruptor.burn.start", "disruptor.cast", "disruptor.cast_3p", "disruptor.field.end",
		"disruptor.field.end_3p", "disruptor.field.loop", "disruptor.field.loop_3p", "disruptor.field.open",
		"disruptor.field.open_3p", "disruptor.flight.loop", "disruptor.flight.loop_3p", "disruptor.hit", "disruptor.hit_3p",
		"disruptor.hit_block", "energy.fill", "energy.full", "energy.gain", "energy.glow", "energy.spent", "melee", "melee_3p",
		"perk.thrusters.jump", "perk.thrusters.jump_3p", "perk.thrusters.recharge", "perk.thrusters.recharge_full",
		"perk.thrusters.start", "railgun.fire", "railgun.fire_3p", "railgun.fire_end", "railgun.fire_end_3p",
		"railgun.fire_first", "railgun.fire_first_3p", "railgun.hit", "railgun.hit_block", "railgun.hit_enemy",
		"railgun.hit_enemy_3p", "railgun.idle.loop", "railgun.idle.loop_3p", "railgun.projectile", "railgun.reload",
		"railgun.reload_3p", "slide.end", "slide.end_3p", "slide.jump", "slide.jump_3p", "slide.lift", "slide.lift_3p",
		"slide.loop", "slide.loop_3p", "slide.scrape.loop", "slide.scrape.loop_3p", "slide.start", "slide.start_3p",
		"slide.touch", "slide.touch_3p", "slide.wall", "slide.wall_3p", "ult.cast", "ult.cast_3p", "ult.end", "ult.end_3p",
		"ult.ending", "ult.ending_3p", "ult.enemy", "ult.friendly", "ult.loop", "ult.loop_3p");
	public static final java.util.List<String> SOJOURN_VOICE = java.util.List.of(
		"elimination", "multikill", "melee_kill", "on_fire", "respawn", "melee", "select", "death", "pain", "ult_ready", "ping",
		"ping_warning", "ping_enemy", "slide", "slide_jump", "disruptor", "railgun_charged", "railgun_holding", "charged_kill",
		"charged_kill_air", "overclock_kill", "overclock_kill_2", "overclock_kill_3", "overclock_kill_5", "overclock_end",
		"ultimate");

	/**
	 * Sigma's (tools/soundmaps/sigma.txt, read for wiring in tools/reference/sigma-sounds.md): his sounds by name ("sigma." +
	 * name, played through dev.overcraft.sigma.SigmaSounds on the server and dev.overcraft.client.SigmaSounds for the loops) and
	 * his lines by moment (Voice.say: "sigma.voice.<moment>"; his wheel lines, "resurrected", "damage_boosted" and
	 * "nano_boosted" are every hero's, registered in init()). His set has no reload line (H: no ammunition). The ult line is
	 * ult.friendly / ult.enemy (0BA4 / 0BA3); voice.ultimate (185B, his own copy, the enemy line's text) stays unplayed.
	 */
	public static final java.util.List<String> SIGMA_SOUNDS = java.util.List.of(
		"accretion.flight.loop", "accretion.flyby", "accretion.gather", "accretion.gather_3p", "accretion.hit_block",
		"accretion.hit_enemy", "accretion.hit_enemy_3p", "accretion.impact", "accretion.start", "accretion.throw",
		"accretion.throw_3p", "accretion.throw_crack", "ambient.loop", "ambient.loop_3p", "barrier.break", "barrier.break_3p",
		"barrier.deploy", "barrier.deploy_3p", "barrier.hold_3p", "barrier.hum.loop", "barrier.hum.loop_3p", "barrier.low",
		"barrier.move.loop", "barrier.move.loop_3p", "barrier.recall", "barrier.return", "barrier.return_3p", "barrier.send",
		"barrier.send_3p", "barrier.stop", "barrier.stop_3p", "flux.aim.loop", "flux.cast", "flux.cast_3p", "flux.end",
		"flux.end_3p", "flux.flight.loop", "flux.flight.loop_3p", "flux.layer", "flux.lift", "flux.lift_foley",
		"flux.lift_foley_3p", "flux.lifted", "flux.slam", "flux.slam_3p", "flux.slam_hit", "flux.slam_hit_3p", "flux.slam_land",
		"flux.start", "flux.start_3p", "grasp.absorb", "grasp.absorb_3p", "grasp.absorb_shooter", "grasp.layer", "grasp.loop",
		"grasp.loop_3p", "grasp.overhealth", "grasp.overhealth_3p", "grasp.start", "grasp.start_3p", "melee", "melee_3p",
		"move_3p", "perk.hyper_strike.hit", "perk.hyper_strike.hit_3p", "perk.hyper_strike.ready", "perk.kinetic_cycle",
		"perk.levitation.loop", "perk.levitation_3p", "sphere.burst", "sphere.burst2", "sphere.burst2_3p", "sphere.burst_3p",
		"sphere.fire", "sphere.fire_3p", "sphere.flight.loop", "sphere.hit", "sphere.hit_block", "sphere.hit_block2",
		"sphere.hit_block2_3p", "sphere.hit_block_3p", "sphere.hit_enemy", "sphere.hit_enemy_3p", "sphere.impact_3p",
		"sphere.reform", "sphere.reform_3p", "sphere.volley", "sphere.volley_3p", "ult.enemy", "ult.friendly");
	public static final java.util.List<String> SIGMA_VOICE = java.util.List.of(
		"elimination", "multikill", "melee_kill", "on_fire", "respawn", "melee", "select", "death", "pain", "ult_ready", "ping",
		"ping_warning", "ping_enemy", "hyperspheres", "grasp", "grasp_absorb", "accretion", "accretion_kill", "barrier",
		"barrier_recall", "barrier_broken", "barrier_damaged", "barrier_low", "flux_kill", "ultimate");

	/** Which of a charge-scaled sound's three bins a charge (0-1) takes (21C6 #954). */
	public static SoundEvent byCharge(SoundEvent[] bins, double charge) {
		return bins[charge <= 0.3 ? 0 : charge <= 0.9 ? 1 : 2];
	}

	static {
		LUCIO_VOICE.forEach(moment -> register("lucio.voice." + moment));
		JETPACK_CAT_VOICE.forEach(moment -> register("jetpack_cat.voice." + moment));
		MERCY_VOICE.forEach(moment -> register("mercy.voice." + moment));
		LIFEWEAVER_VOICE.forEach(moment -> register("lifeweaver.voice." + moment));
		ANA_VOICE.forEach(moment -> register("ana.voice." + moment));
		SOMBRA_SOUNDS.forEach(name -> register("sombra." + name));
		SOMBRA_VOICE.forEach(moment -> register("sombra.voice." + moment));
		BRIGITTE_SOUNDS.forEach(name -> register("brigitte." + name));
		BRIGITTE_VOICE.forEach(moment -> register("brigitte.voice." + moment));
		JUNO_SOUNDS.forEach(name -> register("juno." + name));
		JUNO_VOICE.forEach(moment -> register("juno.voice." + moment));
		SOJOURN_SOUNDS.forEach(name -> register("sojourn." + name));
		SOJOURN_VOICE.forEach(moment -> register("sojourn.voice." + moment));
		SIGMA_SOUNDS.forEach(name -> register("sigma." + name));
		SIGMA_VOICE.forEach(moment -> register("sigma.voice." + moment));
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
			// and when an Ana's Nano Boost lands on them, 1.4 s in (05E2: 0B9F #227 -> #226)
			register(hero.id + ".voice.nano_boosted");
		}
	}
}
