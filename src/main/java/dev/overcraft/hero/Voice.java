package dev.overcraft.hero;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.BuiltInRegistries;
import dev.overcraft.Overcraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Heroes' voice lines on their abilities ("I'm here!" on Swift Step): heard around the hero, not every time (chance),
 * and never two close together (the game spaces them too; the gap here is ours).
 */
public final class Voice {
	/**
	 * How far others hear a hero's lines: Overwatch's voice stimuli reach 25-35 m (the game data,
	 * tools/reference/kiriko-voice.md); 2 = 32 blocks.
	 */
	private static final float VOLUME = 2;
	/** Ticks between two lines from one hero (ours). */
	private static final int GAP_TICKS = 60;
	/** A second enemy kill within this many ticks of the last is a multikill (ours: 4 s; Overwatch gives no number). */
	private static final int KILL_CHAIN = 80;
	/** Ticks before another multikill line can play: the game data's 15-20 s (tools/reference/kiriko-voice.md, M). */
	private static final int MULTIKILL_COOLDOWN_MIN = 300;
	private static final int MULTIKILL_COOLDOWN_MAX = 400;

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

	/**
	 * The hero's line for a moment of the game ("ult_ready", "elimination", "death"...: sound events named
	 * "<hero>.voice.<moment>", the game's own lines through the local pack), heard around them. {@code spaced}: kept
	 * out of the gap after another line (chatter); unspaced lines (death, the ult) always play.
	 */
	public static void say(ServerPlayer player, String moment, float chance, boolean spaced, String... conditions) {
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
		if (spaced && now - state.voiceAt < GAP_TICKS || player.getRandom().nextFloat() >= chance) {
			return;
		}
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
	 * A line said at most once per {@code minTicks} (on top of the usual spacing), by chance when it may: for moments that
	 * come many times a second (healing: ofuda land about 14 times a second).
	 */
	public static void sayNowAndThen(ServerPlayer player, String moment, float chance, int minTicks, String... conditions) {
		HeroState state = HeroManager.state(player);
		if (state == null) {
			return;
		}
		long now = player.level().getGameTime();
		Long last = state.saidAt.get(moment);
		if (last != null && now - last < minTicks) {
			return;
		}
		long before = state.voiceAt;
		say(player, moment, chance, true, conditions);
		if (state.voiceAt != before) {
			state.saidAt.put(moment, now);
		}
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
			if (state.killChain == 2 && now >= state.multikillReadyAt) {
				state.multikillReadyAt = now + MULTIKILL_COOLDOWN_MIN + killer.getRandom().nextInt(MULTIKILL_COOLDOWN_MAX - MULTIKILL_COOLDOWN_MIN + 1);
				say(killer, "multikill", 1, false);
			} else if (source.getDirectEntity() == killer) {
				say(killer, "melee_kill", 0.6f, true);
			} else {
				boolean headshot = state.critAt == now;
				boolean inAir = !victim.onGround() && !victim.isInWater() && !victim.onClimbable() && !victim.isPassenger();
				say(killer, "elimination", 0.35f, true, headshot ? "headshot" : null, inAir ? "in_air" : null);
			}
		}
	}

	/**
	 * Going On Fire: Kiriko's On Fire line ("I'm on fire"), unless a line was said this very tick (Overwatch ranks
	 * kill lines above it: 70 against 60, the game data).
	 */
	public static void onFire(ServerPlayer player, HeroState state) {
		if (state.voiceAt != player.level().getGameTime()) {
			say(player, "on_fire", 1, true);
		}
	}

	public static void maybe(ServerPlayer hero, HeroState state, SoundEvent line, float chance) {
		long now = hero.level().getGameTime();
		if (now - state.voiceAt < GAP_TICKS || hero.getRandom().nextFloat() >= chance) {
			return;
		}
		state.voiceAt = now;
		HeroSounds.fromHero(hero, line, VOLUME);
	}
}
