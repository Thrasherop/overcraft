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
		// Lúcio's own (research/lucio/voice.json): category 4, interrupt 0; raised by criteria events in his graphs
		java.util.Map.entry("lucio.crossfade_heal", new Stimulus(50, 15, 20, false)), // 03B0
		java.util.Map.entry("lucio.crossfade_speed", new Stimulus(50, 10, 20, false)), // 03B1
		java.util.Map.entry("lucio.amp", new Stimulus(50, 5, 5, false)), // 03B2
		java.util.Map.entry("lucio.soundwave", new Stimulus(50, 20, 25, false)), // 03B3
		java.util.Map.entry("lucio.wallride", new Stimulus(45, 10, 15, false))); // 03B4

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
	 * are their hurt and death sounds: PlayerMixin.) Multikill used to fire on any two kills of anything within 4 s, on
	 * every kill of a chain: "Now I'm having fun" seemed to come with every kill (#13).
	 */
	public static void onDeath(LivingEntity victim, DamageSource source) {
		if (source.getEntity() instanceof ServerPlayer killer && killer != victim
			&& (victim instanceof net.minecraft.world.entity.monster.Enemy || victim instanceof net.minecraft.world.entity.player.Player)
			&& !Allies.wasAlly(killer, victim)) {
			HeroState state = HeroManager.state(killer);
			if (state == null || state.hero == null) {
				return;
			}
			long now = killer.level().getGameTime();
			state.killChain = now - state.lastKillAt <= KILL_CHAIN ? state.killChain + 1 : 1;
			state.lastKillAt = now;
			if (state.killChain == 2 && now >= state.voiceReadyAt.getOrDefault("multikill", Long.MIN_VALUE)) {
				say(killer, "multikill", 1);
			} else if (source.getDirectEntity() == killer) {
				// ours: the data has no gate in front of it that could be read
				say(killer, "melee_kill", 0.6f);
			} else {
				boolean headshot = state.critAt == now;
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
