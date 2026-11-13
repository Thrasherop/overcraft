package dev.overcraft.hero;

import dev.overcraft.Ow;
import dev.overcraft.ana.AnaStats;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sleep (Ana's Sleep Dart: the status 026F -> 0BDE -> the shared crowd-control graph 224E, tools/reference/ana.md).
 * Asleep a creature does nothing until its time runs out. Damage wakes it, but only once it has slept 0.5 s (earlier
 * damage is ignored, not saved), and the waking takes 0.5 s. A mob is stopped by holding its AI off (aiHeld: never its
 * saved NoAI flag, #97);
 * a player can't move (their client: ClientSleep) or use their hero's keys (HeroManager.controls). Bosses sleep too, and
 * no Minecraft mob counts as a tank for the data's shorter tank sleep (the owner's open questions, ana-plan.md: ours).
 */
public final class Sleep {
	private Sleep() {
	}

	/** One sleeper: since and until when (game time), waking from a hit. */
	private record Sleeper(LivingEntity entity, long since, long until, boolean waking, boolean stun, boolean slides, double gravity) {
		Sleeper(LivingEntity entity, long since, long until, boolean waking) {
			this(entity, since, until, waking, false, false, 1);
		}

		Sleeper(LivingEntity entity, Holds.Hold hold) {
			this(entity, hold.since(), hold.until(), hold.waking(), hold.stun(), hold.slides(), hold.gravity());
		}

		Holds.Hold hold() {
			return new Holds.Hold(since, until, waking, stun, slides, gravity);
		}
	}

	private static final net.minecraft.resources.Identifier FLOAT = dev.overcraft.Overcraft.id("knockdown_float");
	/** Players on their way down from a float: no fall counts till they land. */
	private static final Map<UUID, ServerPlayer> FLOATED = new java.util.concurrent.ConcurrentHashMap<>();

	/** (Concurrent: Mob.isNoAi asks this from worldgen threads too.) */
	private static final Map<UUID, Sleeper> SLEEPERS = new java.util.concurrent.ConcurrentHashMap<>();

	public static boolean asleep(LivingEntity entity) {
		return SLEEPERS.containsKey(entity.getUUID());
	}

	/** Puts it to sleep for {@code seconds}, as Holds.stacked says (not on a knock-down: ours). */
	public static void put(LivingEntity entity, double seconds) {
		if (Combat.phaseStops(entity)) {
			// (224E #96 fails on a phased one, #575 lets only a knock-down by)
			return;
		}
		long now = entity.level().getGameTime();
		Sleeper old = SLEEPERS.get(entity.getUUID());
		Holds.Hold hold = Holds.stacked(old == null ? null : old.hold(), Holds.sleep(now, now + Ow.ticks(seconds)));
		if (hold == null) {
			return;
		}
		SLEEPERS.put(entity.getUUID(), new Sleeper(entity, hold));
		if (old == null || old.stun) {
			// the sleep's bed (1A1F, as long as the sleep): the sleeper's own, everyone else's at it (45 m)
			sound(entity, dev.overcraft.registry.OcSounds.ANA_SLEEP_TARGET, dev.overcraft.registry.OcSounds.ANA_SLEEP);
		}
		if (entity.level() instanceof net.minecraft.server.level.ServerLevel level) {
			Effects.play(level, "ana_asleep", entity.position(), Vec3.ZERO, entity, Ow.ticks(seconds), null);
		}
		if (entity instanceof Mob mob) {
			if (mob.getTarget() != null) {
				// (asleep it's still the one who was after them: Allies)
				Allies.remember(mob, mob.getTarget());
			}
			mob.setTarget(null);
		}
		if (entity instanceof ServerPlayer player) {
			player.setAttached(HeroManager.ASLEEP, true);
		}
	}

	/**
	 * A stun (Brigitte's Shield Bash in Rally, 224E with no type of its own: its public schema's default, 4EB6: 0.75 s): held as a sleeper is (a mob still, a player's
	 * movement and hero keys locked), but damage doesn't end it, and it has none of the sleep's look or sound.
	 */
	public static boolean stun(LivingEntity entity, double seconds) {
		return stun(entity, seconds, false, 1);
	}

	/**
	 * A knockdown (Sigma's Accretion: 224E as Rally's bash, 1.1 s): a stun in which the creature keeps the knockback it was
	 * given. A mob's AI is off, and with it Minecraft's own movement (a NoAI mob never travels: LivingEntity.aiStep), so the
	 * knockdown moves it as Minecraft would (gravity, drag, the ground's friction) until it ends.
	 */
	public static void knockdown(LivingEntity entity, double seconds) {
		stun(entity, seconds, true, 1);
	}

	/** The same with its gravity scaled (Hyper Strike's float: x0.01, 54A5 #404). */
	public static void knockdown(LivingEntity entity, double seconds, double gravity) {
		stun(entity, seconds, true, gravity);
	}

	/** Under a dart's sleep, not a stun or knock-down: Catnapper's tether can't pull it (Sleep.tick holds it still). */
	public static boolean sleeping(LivingEntity entity) {
		Sleeper s = SLEEPERS.get(entity.getUUID());
		return s != null && !s.stun;
	}

	/**
	 * Whether Overcraft holds this mob's AI off now (asleep, stunned, knocked down, lifted by Gravitic Flux): MobMixin answers
	 * Mob.isNoAi with it, so the mob does nothing, as a NoAI mob does, without its saved NoAI flag ever being touched (#97).
	 * That flag used to be set and put back from this map: a mob unloaded or saved mid-sleep (its chunk left behind, Save
	 * and Quit, a server stop) was written NoAI and stayed a statue for good. Held only for the very object put to sleep, so
	 * the same mob loaded again (a new object, same UUID) is awake, and the client's copy (same UUID, same JVM in single
	 * player) never is.
	 */
	public static boolean aiHeld(Mob mob) {
		if (SLEEPERS.isEmpty()) {
			return dev.overcraft.sigma.GraviticFlux.holds(mob);
		}
		Sleeper s = SLEEPERS.get(mob.getUUID());
		return s != null && s.entity == mob || dev.overcraft.sigma.GraviticFlux.holds(mob);
	}

	/**
	 * This thread is reading mobs' own NoAI flags, not Overcraft's hold (a save, a bucket, a conversion: MobMixin). Per thread:
	 * Mob.addAdditionalSaveData also runs on worldgen threads (a mob placed in a new chunk is saved into it: ProtoChunk.addEntity),
	 * and a shared counter could stick, after which no mob could be held until the game restarted (critique_systems S1).
	 */
	private static final ThreadLocal<Integer> OWN_VIEW = ThreadLocal.withInitial(() -> 0);

	/** Runs {@code read} seeing every mob's own NoAI flag (what gets saved or copied), not the hold. */
	public static <T> T ownAi(java.util.function.Supplier<T> read) {
		OWN_VIEW.set(OWN_VIEW.get() + 1);
		try {
			return read.get();
		} finally {
			OWN_VIEW.set(OWN_VIEW.get() - 1);
		}
	}

	/** Whether isNoAi should answer with the hold now (not while something saves or copies the mob). */
	public static boolean holdApplies(Mob mob) {
		return aiHeld(mob) && OWN_VIEW.get() == 0;
	}

	/** What Protection Suzu's cleanse does here (Combat.cleanse's table; Holds.cleansed has the rule). */
	public static boolean cleanse(LivingEntity entity) {
		Sleeper s = SLEEPERS.get(entity.getUUID());
		if (s == null) {
			return false;
		}
		boolean told = Holds.tells(s.hold());
		Holds.Hold after = Holds.cleansed(s.hold(), entity.level().getGameTime(), Ow.ticks(AnaStats.SLEEP_WAKING));
		if (after == null) {
			wake(s);
			if (entity.level() instanceof net.minecraft.server.level.ServerLevel level) {
				Effects.play(level, "brigitte_stun_end", entity.position(), Vec3.ZERO, entity, 0, null);
			}
		} else {
			SLEEPERS.put(entity.getUUID(), new Sleeper(s.entity, after));
		}
		return told;
	}

	/** Lets a player's client go, unless something else still holds them (a Flux's first 0.4 s, or a hold here). */
	public static void release(ServerPlayer player) {
		if (Holds.letsGo(SLEEPERS.containsKey(player.getUUID()), dev.overcraft.sigma.GraviticFlux.holding(player))) {
			player.removeAttached(HeroManager.ASLEEP);
		}
	}

	private static boolean stun(LivingEntity entity, double seconds, boolean slides, double gravity) {
		if (Combat.phaseStops(entity)) {
			// (a stun: 224E #96 / #575. A knock-down: the hit's own filter, mask 20 (M) as 5B7B's and 1EB9 #338's, and W; hits don't get here, Allies: this is the door for what isn't a hit)
			return false;
		}
		long now = entity.level().getGameTime();
		Sleeper old = SLEEPERS.get(entity.getUUID());
		Holds.Hold hold = Holds.stacked(old == null ? null : old.hold(), Holds.stun(now, now + Ow.ticks(seconds), slides, gravity));
		if (hold == null) {
			return false;
		}
		SLEEPERS.put(entity.getUUID(), new Sleeper(entity, hold));
		if (entity instanceof ServerPlayer player) {
			// held still by their client as a sleeper is (a stunned player walked on through it: found playing, two clients)
			player.setAttached(HeroManager.ASLEEP, true);
			if (hold.gravity() == 1 && old != null && old.gravity != 1) {
				// a plain knock-down ended the float (Holds.stacked, ours): its gravity back
				Combat.buff(player, net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY, FLOAT, 0, 0);
			}
			if (gravity != 1 && hold.gravity() != 1 && !Combat.devFixOff) {
				// a player moves on their own client: Hyper Strike's float (#109) goes there as Minecraft's gravity attribute, which
				// Minecraft mode falls by and hero mode reads (OvercraftClient's gravityScale); a mob's is in slide
				Combat.buff(player, net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY, FLOAT, gravity - 1, seconds);
				// Its landing doesn't hurt. Overwatch has no fall damage, and the float took a Brigitte struck aiming up 18 m: 13.92 -> 0 on the landing,
				// where the floatless throw cost 4 (runs/polish3/c3_hyper_up.txt). Ours, from that: not the #84 rule for pushes, which keep their falls
				FLOATED.put(player.getUUID(), player);
			}
		}
		return true;
	}

	/** {@code own} in the sleeper's head (a player), {@code others} at it for everyone else, out to 45 m (3866). */
	private static void sound(LivingEntity entity, net.minecraft.sounds.SoundEvent own, net.minecraft.sounds.SoundEvent others) {
		if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel level)) {
			return;
		}
		ServerPlayer player = entity instanceof ServerPlayer p ? p : null;
		if (player != null) {
			HeroSounds.toOwner(player, own);
		}
		HeroSounds.at(level, others, entity.getBoundingBox().getCenter(), 45, player);
	}

	/** Damage dealt inside {@link #quietly} doesn't wake anyone (Local Anesthetic's ticks). */
	private static int quiet;

	/** Runs damage that never wakes a sleeper (Ana's Local Anesthetic: D; W). */
	public static float quietly(java.util.function.Supplier<Float> damage) {
		quiet++;
		try {
			return damage.get();
		} finally {
			quiet--;
		}
	}

	/** It took damage (Overcraft's AFTER_DAMAGE): after 0.5 s asleep it wakes, 0.5 s later. */
	/** Damage that wakes no sleeper is running (Local Anesthetic's, tag 191F, 224E #203): it starts no healing reduction either (01C7 #882). */
	public static boolean quiet() {
		return quiet > 0;
	}

	public static void onDamaged(LivingEntity entity) {
		Sleeper s = SLEEPERS.get(entity.getUUID());
		if (s == null || s.waking || s.stun || quiet > 0) {
			return;
		}
		long now = entity.level().getGameTime();
		if (now - s.since >= Ow.ticks(AnaStats.SLEEP_WAKE_FROM) && s.until - now > Ow.ticks(AnaStats.SLEEP_WAKING)) {
			SLEEPERS.put(entity.getUUID(), new Sleeper(s.entity, s.since, now + Ow.ticks(AnaStats.SLEEP_WAKING), true));
		}
	}

	/** Each server tick (HeroManager): sleepers stay put, and wake when their time is up. */
	public static void tick(MinecraftServer server) {
		FLOATED.values().removeIf(player -> {
			player.resetFallDistance();
			// (not the tick it began, still on the ground)
			return !player.isAlive() || player.isRemoved() || player.onGround() && !SLEEPERS.containsKey(player.getUUID());
		});
		for (Sleeper s : new ArrayList<>(SLEEPERS.values())) {
			LivingEntity e = s.entity;
			if (e.isRemoved() || !e.isAlive() || e.level().getGameTime() >= s.until) {
				wake(s);
				continue;
			}
			holdFuse(e);
			if (dev.overcraft.sigma.GraviticFlux.lifted(e)) {
				// a Gravitic Flux moves it (its own tick)
				continue;
			}
			if (s.slides) {
				if (e instanceof Mob mob) {
					// (its AI held off, Minecraft doesn't move it: aiHeld)
					slide(mob, s.gravity);
				}
				continue;
			}
			Vec3 v = e.getDeltaMovement();
			e.setDeltaMovement(0, Math.min(v.y, 0), 0);
		}
	}

	/**
	 * A creeper's fuse runs in its own tick, not in its AI: held with its fuse lit it went off anyway (#101). It burns back down, as
	 * when a target walks off. One lit by flint and steel goes on (Minecraft's: nothing puts that out). For every hold: Gravitic Flux's too.
	 */
	public static void holdFuse(LivingEntity e) {
		if (e instanceof net.minecraft.world.entity.monster.Creeper creeper && !creeper.isIgnited()) {
			creeper.setSwellDir(-1);
		}
	}

	/** Stays up by itself: a knock-down doesn't drop it out of the sky (a ghast fell about 30 blocks; ours). */
	private static boolean flies(Mob mob) {
		return mob.isNoGravity() || mob.getMoveControl() instanceof net.minecraft.world.entity.ai.control.FlyingMoveControl
			|| mob instanceof net.minecraft.world.entity.monster.Ghast || mob instanceof net.minecraft.world.entity.monster.Phantom
			|| mob instanceof net.minecraft.world.entity.monster.Vex || mob instanceof net.minecraft.world.entity.monster.Blaze
			|| mob instanceof net.minecraft.world.entity.ambient.Bat;
	}

	/**
	 * One tick of a held mob carried by its speed, as LivingEntity.travel would (gravity, air drag, the ground's friction). A flyer keeps
	 * its height, and a boss isn't moved at all (the Ender Dragon has no physics: it sank through the island).
	 */
	public static void slide(Mob mob, double gravity) {
		if (Allies.isBoss(mob)) {
			mob.setDeltaMovement(Vec3.ZERO);
			return;
		}
		if (flies(mob)) {
			gravity = 0;
			// (and no hop: Catnapper's 0.1 up took it 2.2 m up, nothing bringing it down)
			mob.setDeltaMovement(mob.getDeltaMovement().multiply(1, 0, 1));
		}
		Vec3 v = mob.getDeltaMovement();
		mob.move(net.minecraft.world.entity.MoverType.SELF, v);
		Vec3 after = mob.getDeltaMovement();
		if (mob.isInWater()) {
			// water's drag, and it sinks slowly (LivingEntity.travelInWater's 0.8 and gravity / 16)
			mob.setDeltaMovement(after.x * 0.8, (after.y - mob.getGravity() * gravity / 16) * 0.8, after.z * 0.8);
			mob.needsSync = true;
			return;
		}
		float friction = mob.onGround()
			? mob.level().getBlockState(mob.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction() * 0.91f : 0.91f;
		mob.setDeltaMovement(after.x * friction, (after.y - mob.getGravity() * gravity) * 0.98, after.z * friction);
		mob.needsSync = true;
	}

	/** The world stopped: nothing held over (an entry would pin its mob and level until the next world's first tick). */
	public static void clear() {
		FLOATED.clear();
		SLEEPERS.clear();
	}

	private static void wake(Sleeper s) {
		SLEEPERS.remove(s.entity.getUUID());
		if (s.stun) {
			if (s.entity instanceof ServerPlayer player) {
				// (left set, a stunned player stayed frozen for good: found playing, two clients)
				release(player);
			}
			return;
		}
		if (s.entity.level() instanceof net.minecraft.server.level.ServerLevel level) {
			// 1A1F stops: its Stop events cut the bed and play the waking
			for (ServerPlayer listener : level.players()) {
				if (listener.distanceTo(s.entity) <= 53) {
					for (var bed : new net.minecraft.sounds.SoundEvent[] {dev.overcraft.registry.OcSounds.ANA_SLEEP,
						dev.overcraft.registry.OcSounds.ANA_SLEEP_TARGET}) {
						listener.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(bed.location(),
							net.minecraft.sounds.SoundSource.PLAYERS));
					}
				}
			}
			if (s.entity.isAlive()) {
				sound(s.entity, dev.overcraft.registry.OcSounds.ANA_WAKE_TARGET, dev.overcraft.registry.OcSounds.ANA_WAKE);
			}
			Effects.play(level, "ana_wake", s.entity.position(), Vec3.ZERO, s.entity, 0, null);
		}
		if (s.entity instanceof ServerPlayer player) {
			release(player);
		}
	}
}
