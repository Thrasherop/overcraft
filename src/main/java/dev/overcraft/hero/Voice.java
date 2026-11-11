package dev.overcraft.hero;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.BuiltInRegistries;
import dev.overcraft.Overcraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Heroes' voice lines for the game's moments ("I'm here!" on Swift Step, kill lines, callouts), heard around the hero,
 * by Overwatch's own rules for when a line may play (the owner, 2026-10-27: automatic lines were still landing right
 * next to each other). Each moment is a voice stimulus in the game data (.078) with a priority, a cooldown range in
 * seconds and an interrupt mode, read for every moment below (tools/reference/kiriko-voice.md):
 * <ul>
 * <li>a moment won't play again until its own cooldown (a random time in its range) has passed: an elimination line
 * at most every 15 s, healing every 14-17 s, multikill every 15-20 s;</li>
 * <li>a hero says one line at a time: while one plays, a new one is dropped, unless it is one Overwatch lets
 * interrupt (the communication wheel's lines and pings) and ranks at least as high; it then cuts the first one off.</li>
 * </ul>
 * Neither the data nor Blizzard's notes give a cooldown shared by all lines (searched: the voice categories hold no
 * times), so there is none here. The game's automatic chatter (kills, healing, abilities, On Fire, respawn) all sits
 * in one category that can't interrupt.
 */
public final class Voice {
	/**
	 * How far others hear a hero's lines: Overwatch's voice stimuli reach 25-35 m (the game data,
	 * tools/reference/kiriko-voice.md); 2 = 32 blocks.
	 */
	private static final float VOLUME = 2;
	/**
	 * How long a line holds the hero's voice (ticks, ours): the server doesn't know a line's length, and Overwatch's run
	 * about 0.5-2.5 s ("Nice" to "The Kitsune is ready to be unleashed").
	 */
	private static final int LINE_TICKS = 40;
	/** A second enemy kill within this many ticks of the last is a multikill (ours: 4 s; Overwatch gives no number). */
	private static final int KILL_CHAIN = 80;

	/**
	 * A moment's voice stimulus: its priority, its cooldown range (seconds; the stimulus' unnamed pair m_A73F2EBE /
	 * m_DD27AC95, read as min and max: M) and whether it may cut into another line (m_interrupt above 0).
	 */
	private record Stimulus(int priority, float cooldownMin, float cooldownMax, boolean interrupts) {
	}

	/** A moment the table doesn't list: plays when the hero is free, no cooldown. */
	private static final Stimulus OTHER = new Stimulus(50, 0, 0, false);

	/**
	 * Each moment's stimulus, from the game data (build 154088). The shared moments are every hero's (the same stimulus
	 * ids); a hero's own moments are keyed "<hero>.<moment>". Ids in the comments.
	 */
	private static final java.util.Map<String, Stimulus> STIMULI = java.util.Map.ofEntries(
		// the game's automatic chatter: category 4, interrupt 0
		java.util.Map.entry("elimination", new Stimulus(70, 15, 15, false)), // 0008
		java.util.Map.entry("multikill", new Stimulus(70, 15, 20, false)), // 00C9
		java.util.Map.entry("melee_kill", new Stimulus(65, 10, 14, false)), // 0032
		java.util.Map.entry("on_fire", new Stimulus(60, 2, 5, false)), // 0583
		java.util.Map.entry("respawn", new Stimulus(40, 15, 15, false)), // 002C
		java.util.Map.entry("melee", new Stimulus(35, 0.5f, 0.5f, false)), // 0023, the quick melee's shout
		java.util.Map.entry("select", new Stimulus(100, 1, 1, false)), // 002B
		java.util.Map.entry("resurrected", new Stimulus(75, 10, 10, false)), // 005A, a Mercy's Resurrect
		java.util.Map.entry("death", new Stimulus(90, 2, 5, false)), // 0001 (the Mercy pass: shared, every hero's)
		java.util.Map.entry("pain", new Stimulus(40, 1, 2.5f, false)), // 0003
		java.util.Map.entry("damage_boosted", new Stimulus(60, 30, 30, false)), // 00CC, a Mercy's damage boost
		// the communication wheel and pings: category 31, interrupt 1
		java.util.Map.entry("ult_ready", new Stimulus(70, 0, 0, true)), // 02E7
		java.util.Map.entry("call_ult_charging", new Stimulus(70, 0, 0, true)), // 0574
		java.util.Map.entry("call_ult_almost", new Stimulus(70, 0, 0, true)), // 05A7
		java.util.Map.entry("ping", new Stimulus(70, 0, 0, true)), // 11D9
		java.util.Map.entry("ping_warning", new Stimulus(70, 3, 3, true)), // 11DA
		java.util.Map.entry("ping_enemy", new Stimulus(70, 3, 3, true)), // 1117
		// Kiriko's own: category 4, interrupt 0
		java.util.Map.entry("kiriko.swift_step", new Stimulus(60, 10, 10, false)), // FC0
		java.util.Map.entry("kiriko.suzu", new Stimulus(60, 10, 10, false)), // FC5
		java.util.Map.entry("kiriko.heal", new Stimulus(60, 14, 17, false)), // FC4
		// Mercy's own (tools/soundmaps/mercy.txt): her beam lines from her staff's graph 006B, the rest by shared criteria
		java.util.Map.entry("mercy.beam_heal", new Stimulus(50, 10, 15, false)), // 00B9
		java.util.Map.entry("mercy.beam_boost", new Stimulus(50, 14, 17, false)), // 00BA
		java.util.Map.entry("mercy.beam_heal_ongoing", new Stimulus(50, 20, 25, false)), // 00BB (M)
		java.util.Map.entry("mercy.beam_boost_ongoing", new Stimulus(50, 20, 25, false)), // 00BC (M)
		java.util.Map.entry("mercy.guardian_angel", new Stimulus(50, 20, 25, false)), // 0099 (M)
		java.util.Map.entry("mercy.resurrect", new Stimulus(95, 0.1f, 0.1f, false)), // 009A (M)
		// Lúcio's own (research/lucio/voice.json): category 4, interrupt 0; raised by criteria events in his graphs
		java.util.Map.entry("lucio.crossfade_heal", new Stimulus(50, 15, 20, false)), // 03B0
		java.util.Map.entry("lucio.crossfade_speed", new Stimulus(50, 10, 20, false)), // 03B1
		java.util.Map.entry("lucio.amp", new Stimulus(50, 5, 5, false)), // 03B2
		java.util.Map.entry("lucio.soundwave", new Stimulus(50, 20, 25, false)), // 03B3
		java.util.Map.entry("lucio.wallride", new Stimulus(45, 10, 15, false)), // 03B4
		// Jetpack Cat's (jetpack_cat.txt's voice section; priorities ours, at Lucio's 50, where the map has none)
		java.util.Map.entry("jetpack_cat.boost", new Stimulus(50, 5, 5, false)), // 1993
		java.util.Map.entry("jetpack_cat.reload", new Stimulus(50, 20, 20, false)), // 19B9
		java.util.Map.entry("jetpack_cat.heal", new Stimulus(60, 20, 20, false)), // 19B8
		java.util.Map.entry("jetpack_cat.purr", new Stimulus(50, 0, 0, false)), // 19BB
		java.util.Map.entry("jetpack_cat.purr_end", new Stimulus(50, 0, 0, false)), // 19BA
		java.util.Map.entry("jetpack_cat.lifeline", new Stimulus(50, 0, 0, false)), // 1994
		java.util.Map.entry("jetpack_cat.tether_attached", new Stimulus(50, 0, 0, false)), // 19C4
		java.util.Map.entry("jetpack_cat.tether_end", new Stimulus(95, 0, 0, true)), // 19B6, priority 95
		// Lifeweaver's own (tools/soundmaps/lifeweaver.txt): from his graphs, the rest by shared criteria
		java.util.Map.entry("lifeweaver.heal", new Stimulus(60, 20, 25, false)), // 135A
		// 176B's own cooldown is 0, but 21C6 holds it behind a 15 s wait (#1401 / #1403)
		java.util.Map.entry("lifeweaver.reload", new Stimulus(60, 15, 15, false)), // 176B
		java.util.Map.entry("lifeweaver.platform", new Stimulus(60, 10, 10, false)), // 11F4
		java.util.Map.entry("lifeweaver.platform_used", new Stimulus(60, 10, 10, false)), // 13D5
		java.util.Map.entry("lifeweaver.dash", new Stimulus(60, 20, 25, false)), // 1200
		java.util.Map.entry("lifeweaver.grip", new Stimulus(60, 2, 2, false)), // 11FF
		java.util.Map.entry("lifeweaver.tree_end", new Stimulus(50, 20, 20, true)), // 135B
		// what every hero says as an Ana's Nano Boost lands on them: global, interrupting (the same in every voice set)
		java.util.Map.entry("nano_boosted", new Stimulus(95, 2, 2, true)), // 05E2
		// Ana's own (tools/soundmaps/ana.txt): from her graphs
		java.util.Map.entry("ana.heal_dart", new Stimulus(50, 10, 10, false)), // 05F1
		// 0E2C's own cooldown is 0, but 0B94 holds it behind a 15 s wait (#305 / #307), as Lifeweaver's 176B
		java.util.Map.entry("ana.reload", new Stimulus(60, 15, 15, false)), // 0E2C
		java.util.Map.entry("ana.sleep", new Stimulus(60, 2, 2, false)), // 05F5
		java.util.Map.entry("ana.grenade_heal", new Stimulus(70, 2, 2, false)), // 05F3
		java.util.Map.entry("ana.grenade_damage", new Stimulus(60, 2, 2, false)), // 05F4
		java.util.Map.entry("ana.ping_slept", new Stimulus(70, 3, 3, true)), // 11EA
		java.util.Map.entry("ana.ultimate", new Stimulus(95, 1, 1, false)), // 1836
		java.util.Map.entry("ana.nano_cast", new Stimulus(95, 1, 1, false)), // 0678
		// Sombra's (tools/soundmaps/sombra.txt, the voice section)
		java.util.Map.entry("sombra.nano_boosted", new Stimulus(95, 2, 2, true)), // 05E2
		java.util.Map.entry("sombra.hotfix", new Stimulus(60, 8, 12, false)), // 1A8F
		java.util.Map.entry("sombra.hack", new Stimulus(60, 4.5f, 4.5f, false)), // 068A
		java.util.Map.entry("sombra.cyberspace_hit", new Stimulus(60, 20, 20, false)), // 1591
		java.util.Map.entry("sombra.cyberspace_kill", new Stimulus(70, 15, 15, false)), // 1593
		java.util.Map.entry("sombra.translocate", new Stimulus(60, 15, 15, false)), // 0698
		java.util.Map.entry("sombra.uncloak", new Stimulus(80, 10, 10, false)), // 06E7
		java.util.Map.entry("sombra.ultimate", new Stimulus(95, 1, 1, false)), // 1860
		// Brigitte's (tools/soundmaps/brigitte.txt, the voice section)
		java.util.Map.entry("brigitte.nano_boosted", new Stimulus(95, 2, 2, true)), // 05E2
		java.util.Map.entry("brigitte.barrier_raise", new Stimulus(60, 30, 30, false)), // 0945
		java.util.Map.entry("brigitte.bash", new Stimulus(60, 10, 10, false)), // 094A
		java.util.Map.entry("brigitte.bash_hit", new Stimulus(50, 10, 10, false)), // 09C2
		java.util.Map.entry("brigitte.flail", new Stimulus(60, 5, 5, false)), // 094B
		java.util.Map.entry("brigitte.whip_hit", new Stimulus(60, 20, 20, false)), // 0A03
		java.util.Map.entry("brigitte.repair_pack", new Stimulus(60, 8, 12, false)), // 0948
		java.util.Map.entry("brigitte.repair_empty", new Stimulus(60, 5, 10, false)), // 15A6
		java.util.Map.entry("brigitte.inspire", new Stimulus(60, 20, 40, false)), // 09BE
		java.util.Map.entry("brigitte.barrier_damaged", new Stimulus(60, 40, 60, false)), // 09BF
		java.util.Map.entry("brigitte.barrier_low", new Stimulus(70, 20, 40, false)), // 09C0
		java.util.Map.entry("brigitte.barrier_broken", new Stimulus(60, 20, 20, false)), // 09C1
		java.util.Map.entry("brigitte.ultimate", new Stimulus(95, 1, 1, false)), // 183A
		// Juno's (tools/soundmaps/juno.txt, the voice section; her set 0278.05F)
		java.util.Map.entry("juno.nano_boosted", new Stimulus(95, 2, 2, true)), // 05E2
		java.util.Map.entry("juno.melee_kill", new Stimulus(65, 10, 14, false)), // 0032
		// her own reload stimulus (the shared 0E2C is not in her set): its cooldown is 0 (the map's reading)
		java.util.Map.entry("juno.reload", new Stimulus(60, 0, 0, false)), // 176F
		java.util.Map.entry("juno.heal", new Stimulus(60, 20, 20, false)), // 1727
		java.util.Map.entry("juno.torpedo_ally", new Stimulus(60, 20, 20, false)), // 16AF
		java.util.Map.entry("juno.torpedo", new Stimulus(60, 20, 20, false)), // 1761
		java.util.Map.entry("juno.ring", new Stimulus(60, 15, 15, false)), // 16B0
		java.util.Map.entry("juno.ping_ring", new Stimulus(70, 0, 0, true)), // 16DF
		java.util.Map.entry("juno.ultimate", new Stimulus(95, 1, 1, false)), // 184D
		// Sojourn's (tools/soundmaps/sojourn.txt, the voice section; her set 01C2.05F)
		java.util.Map.entry("sojourn.nano_boosted", new Stimulus(95, 2, 2, true)), // 05E2
		java.util.Map.entry("sojourn.melee_kill", new Stimulus(65, 10, 14, false)), // 0032
		java.util.Map.entry("sojourn.slide", new Stimulus(45, 1, 1, false)), // 0B90
		java.util.Map.entry("sojourn.slide_jump", new Stimulus(45, 1, 1, false)), // 0B91
		java.util.Map.entry("sojourn.disruptor", new Stimulus(60, 10, 10, false)), // 0B92
		java.util.Map.entry("sojourn.railgun_charged", new Stimulus(60, 15, 25, false)), // 0E7C
		java.util.Map.entry("sojourn.railgun_holding", new Stimulus(60, 20, 30, false)), // 0E7D
		java.util.Map.entry("sojourn.charged_kill", new Stimulus(60, 15, 15, false)), // 0B8F
		java.util.Map.entry("sojourn.charged_kill_air", new Stimulus(60, 15, 15, false)), // 0E7A
		java.util.Map.entry("sojourn.overclock_kill", new Stimulus(50, 2, 2, false)), // 0B93
		java.util.Map.entry("sojourn.overclock_kill_2", new Stimulus(60, 2, 2, false)), // 113C
		java.util.Map.entry("sojourn.overclock_kill_3", new Stimulus(60, 0, 0, false)), // 113D (kills 3 and 4)
		java.util.Map.entry("sojourn.overclock_kill_5", new Stimulus(60, 10, 10, false)), // 113E
		java.util.Map.entry("sojourn.overclock_end", new Stimulus(50, 20, 20, false)), // 0E87
		java.util.Map.entry("sojourn.ultimate", new Stimulus(95, 1, 1, false)), // 185C
		// Sigma's (tools/soundmaps/sigma.txt, the voice section; his set 01E5.05F; tools/reference/sigma-sounds.md 7)
		java.util.Map.entry("sigma.nano_boosted", new Stimulus(95, 2, 2, true)), // 05E2
		java.util.Map.entry("sigma.melee_kill", new Stimulus(65, 10, 14, false)), // 0032
		java.util.Map.entry("sigma.hyperspheres", new Stimulus(60, 5, 5, false)), // 0B99
		java.util.Map.entry("sigma.grasp", new Stimulus(60, 20, 20, false)), // 0B9A
		java.util.Map.entry("sigma.grasp_absorb", new Stimulus(60, 5, 5, false)), // 0B9B
		java.util.Map.entry("sigma.accretion", new Stimulus(60, 1, 1, false)), // 0B9C
		java.util.Map.entry("sigma.accretion_kill", new Stimulus(60, 10, 10, false)), // 0B9D
		java.util.Map.entry("sigma.barrier", new Stimulus(60, 40, 50, false)), // 0B9E
		java.util.Map.entry("sigma.barrier_recall", new Stimulus(60, 30, 45, false)), // 0B9F
		java.util.Map.entry("sigma.barrier_broken", new Stimulus(60, 15, 20, true)), // 0BA2
		java.util.Map.entry("sigma.barrier_damaged", new Stimulus(60, 40, 80, false)), // 0BA0
		java.util.Map.entry("sigma.barrier_low", new Stimulus(60, 5, 12, false)), // 0BA1
		java.util.Map.entry("sigma.flux_kill", new Stimulus(50, 5, 5, false)), // 0BA5
		java.util.Map.entry("sigma.ultimate", new Stimulus(95, 1, 1, false)), // 185B
		// D.Va's (tools/soundmaps/dva.txt, the voice section; her set 0057.05F)
		java.util.Map.entry("dva.nano_boosted", new Stimulus(95, 2, 2, true)), // 05E2
		java.util.Map.entry("dva.melee_kill", new Stimulus(65, 10, 14, false)), // 0032
		java.util.Map.entry("dva.boosters", new Stimulus(60, 2, 2, false)), // 03DA
		java.util.Map.entry("dva.matrix", new Stimulus(50, 2, 2, false)), // 03DB
		java.util.Map.entry("dva.matrix_absorb", new Stimulus(85, 10, 15, false)), // 06C1 (its pair 15 / 10, read as 10-15)
		java.util.Map.entry("dva.call_mech", new Stimulus(60, 2, 2, false)), // 0479
		java.util.Map.entry("dva.call_mech_kill", new Stimulus(50, 20, 20, false)), // 11CE
		java.util.Map.entry("dva.eject", new Stimulus(60, 2, 2, false)), // 0497
		java.util.Map.entry("dva.mech_back", new Stimulus(60, 2, 2, false)), // 049D
		java.util.Map.entry("dva.pilot_kill", new Stimulus(70, 15, 15, false)), // 10AF
		java.util.Map.entry("dva.ultimate", new Stimulus(95, 1, 1, false))); // 183C

	/** The communication wheel's lines ("call_*": Thanks, Hello ...): category 31, priority 70, no cooldown, interrupt 1. */
	private static final Stimulus CALL = new Stimulus(70, 0, 0, true);

	/**
	 * Overwatch's weights for the pools with conditional lines (the game data's: tools/reference/kiriko-voice.md): the
	 * generic pool's total, and each condition's. A condition that holds is said with its share of the sum (#13).
	 */
	private static final java.util.Map<String, Double> WEIGHTS = java.util.Map.ofEntries(
		java.util.Map.entry("kiriko.voice.swift_step", 7.0),
		java.util.Map.entry("kiriko.voice.swift_step.wrecking_ball", 0.1),
		java.util.Map.entry("kiriko.voice.swift_step.widowmaker", 0.1),
		java.util.Map.entry("kiriko.voice.swift_step.sigma", 0.1),
		java.util.Map.entry("kiriko.voice.swift_step.lucio", 0.1),
		java.util.Map.entry("kiriko.voice.swift_step.roadhog", 0.1),
		java.util.Map.entry("kiriko.voice.swift_step.reinhardt", 0.1),
		java.util.Map.entry("kiriko.voice.swift_step.junker_queen", 0.1),
		java.util.Map.entry("kiriko.voice.swift_step.ashe", 0.1),
		java.util.Map.entry("kiriko.voice.swift_step.cassidy", 0.1),
		java.util.Map.entry("kiriko.voice.swift_step.mercy", 0.1),
		java.util.Map.entry("kiriko.voice.swift_step.illari", 0.1),
		java.util.Map.entry("kiriko.voice.swift_step.genji", 1.3),
		java.util.Map.entry("kiriko.voice.swift_step.hanzo", 1.0),
		java.util.Map.entry("kiriko.voice.heal", 15.65),
		java.util.Map.entry("kiriko.voice.heal.reaper", 0.5),
		java.util.Map.entry("kiriko.voice.heal.genji", 0.5),
		java.util.Map.entry("kiriko.voice.heal.hanzo", 0.5),
		java.util.Map.entry("kiriko.voice.elimination", 15.4),
		java.util.Map.entry("kiriko.voice.elimination.headshot", 0.7),
		java.util.Map.entry("kiriko.voice.elimination.in_air", 1.8));

	private Voice() {
	}

	/** The stimulus for a hero's moment: their own, else the shared one, else the wheel's for a callout. */
	private static Stimulus stimulus(String hero, String moment) {
		Stimulus own = STIMULI.get(hero + "." + moment);
		if (own != null) {
			return own;
		}
		return STIMULI.getOrDefault(moment, moment.startsWith("call_") ? CALL : OTHER);
	}

	/**
	 * The hero's line for a moment of the game ("elimination", "ping", "call_thanks"...: sound events named
	 * "<hero>.voice.<moment>", the game's own lines through the local pack), heard around them, when Overwatch's rules
	 * let it play (see the class): its cooldown passed, and the hero free or the line one that may cut in. {@code
	 * chance}: where Overwatch's graph has a random gate (or ours, where it says so at the call); 1 where it has none.
	 */
	public static void say(ServerPlayer player, String moment, float chance, String... conditions) {
		HeroState state = HeroManager.state(player);
		if (state == null || state.hero == null) {
			return;
		}
		SoundEvent line = BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id(pick(state.hero.id + ".voice." + moment, conditions,
			player.getRandom())));
		if (line == null) {
			return;
		}
		long now = player.level().getGameTime();
		Stimulus stimulus = stimulus(state.hero.id, moment);
		if (now < state.voiceReadyAt.getOrDefault(moment, Long.MIN_VALUE)) {
			return;
		}
		boolean speaking = now < state.speakingUntil;
		if (speaking && !(stimulus.interrupts() && stimulus.priority() >= state.speakingPriority)) {
			return;
		}
		if (player.getRandom().nextFloat() >= chance) {
			return;
		}
		if (speaking && state.speakingLine != null) {
			// cut the line it interrupts off, for everyone who hears it
			var stop = new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(state.speakingLine,
				net.minecraft.sounds.SoundSource.PLAYERS);
			for (ServerPlayer other : player.level().players()) {
				other.connection.send(stop);
			}
		}
		float cooldown = stimulus.cooldownMin() + player.getRandom().nextFloat() * (stimulus.cooldownMax() - stimulus.cooldownMin());
		state.voiceReadyAt.put(moment, now + Math.round(cooldown * 20));
		state.speakingUntil = now + LINE_TICKS;
		state.speakingPriority = stimulus.priority();
		state.speakingLine = BuiltInRegistries.SOUND_EVENT.getKey(line);
		state.voiceAt = now;
		HeroSounds.fromHero(player, line, VOLUME);
	}

	/**
	 * The pool to say from: the moment's generic one, or a condition's that holds ("<moment>.<condition>": the ally's
	 * hero, a headshot), each by its share of the weights. Conditions with no lines of their own are left out.
	 */
	private static String pick(String moment, String[] conditions, net.minecraft.util.RandomSource random) {
		double total = WEIGHTS.getOrDefault(moment, 0.0);
		java.util.List<String> held = new java.util.ArrayList<>();
		for (String condition : conditions) {
			String id = moment + "." + condition;
			if (condition != null && WEIGHTS.containsKey(id)) {
				held.add(id);
				total += WEIGHTS.get(id);
			}
		}
		double roll = random.nextDouble() * total;
		for (String id : held) {
			roll -= WEIGHTS.get(id);
			if (roll < 0) {
				return id;
			}
		}
		return moment;
	}

	/** The id of the hero an entity is playing (a player in hero mode), as voice conditions name them, or null. */
	public static @org.jspecify.annotations.Nullable String heroOf(net.minecraft.world.entity.Entity entity) {
		if (entity instanceof ServerPlayer player && HeroManager.controls(player)) {
			HeroState state = HeroManager.state(player);
			return state == null || state.hero == null ? null : state.hero.id;
		}
		return null;
	}

	/**
	 * A hero player killed an enemy (a hostile mob or an enemy player: Overwatch's lines are for enemies, not a cow): a
	 * multikill line on the burst's second kill (within KILL_CHAIN, once per burst, not again for 15-20 s), else a melee
	 * remark or an elimination line, a headshot's or a kill in the air's among them. (Their own pain and death cries
	 * are their hurt and death sounds: PlayerMixin.) Called by KillFeed with the hero the kill is credited to and the blow
	 * that made it, so a fall after a boop counts as the hero's kill here too (its line, the multikill chain, Overclock's
	 * count). Multikill used to fire on any two kills of anything within 4 s, on
	 * every kill of a chain: "Now I'm having fun" seemed to come with every kill (#13).
	 */
	public static void onKill(LivingEntity victim, DamageSource source, net.minecraft.world.entity.@org.jspecify.annotations.Nullable Entity credited,
							  @org.jspecify.annotations.Nullable Blow blow) {
		if (credited instanceof ServerPlayer killer && killer != victim && Allies.wasEnemy(killer, victim)) {
			HeroState state = HeroManager.state(killer);
			if (state == null || state.hero == null) {
				return;
			}
			long now = killer.level().getGameTime();
			if (state.hero == Hero.SOJOURN) {
				// her own kill lines first (the Charged Shot's, Overclock's count): the shared ones below wait while she speaks
				dev.overcraft.sojourn.Sojourn.killed(killer, state);
			}
			if (state.hero == Hero.SIGMA) {
				// his own kill lines (the rock's "Collision!", the slam's "To the void!") first
				dev.overcraft.sigma.Sigma.killed(killer, source);
			}
			if (state.hero == Hero.DVA) {
				// her own: Call Mech's "Squished!", the pilot's "No mech? No problem!"
				dev.overcraft.dva.Dva.killed(killer, state, blow);
			}
			state.killChain = now - state.lastKillAt <= KILL_CHAIN ? state.killChain + 1 : 1;
			state.lastKillAt = now;
			if (state.killChain == 2 && now >= state.voiceReadyAt.getOrDefault("multikill", Long.MIN_VALUE)) {
				say(killer, "multikill", 1);
			} else if (blow != null ? blow.kind() == Blow.Kind.MELEE : source.getDirectEntity() == killer) {
				// ours: the data has no gate in front of it that could be read
				say(killer, "melee_kill", 0.6f);
			} else {
				boolean headshot = blow != null ? blow.crit() : state.critAt == now;
				boolean inAir = !victim.onGround() && !victim.isInWater() && !victim.onClimbable() && !victim.isPassenger();
				// the kill graph (33B4) tests something like random(0,1) < 0.66 on this path (a guess at its bytecode: L)
				say(killer, "elimination", 0.66f, headshot ? "headshot" : null, inAir ? "in_air" : null);
			}
		}
	}

	/** Going On Fire: the hero's On Fire line ("I'm on fire"), by the usual rules (a kill line said this tick wins). */
	public static void onFire(ServerPlayer player, HeroState state) {
		say(player, "on_fire", 1);
	}
}
