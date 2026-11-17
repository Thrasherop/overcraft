package dev.overcraft.client;

import dev.overcraft.net.EndPhasePayload;
import dev.overcraft.potg.EndPhase;
import dev.overcraft.registry.OcSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * The dragon fight's end-phase music (#45; the server's half is EndPhase): Overwatch's overtime cue, its intro once and
 * then its loop for as long as the fight lasts (STUSound 69B7's playlist, music-round9 and tools/soundmaps/kiriko.txt, H),
 * quiet at a third of the dragon's health and up to full as it falls, faded out by the killing blow's VICTORY (whose own
 * track then plays: PotgMusic). Heard in the head, as the interface's (like PotgMusic's).
 *
 * <p>Wwise starts the loop at the intro's exit cue while the intro's tail rings on; so do we, by the clock (each tick).
 * The loop then repeats seamlessly by itself (Minecraft loops the file; its tail is laid over its start in the pack).
 */
public final class EndMusic {
	private EndMusic() {
	}

	/** Full volume: forUI's default, the level PotgMusic plays VICTORY's track at, so the hand-over is level (ours). */
	private static final float FULL = 0.25f;
	/** At a third of the dragon's health, this share of FULL (ours: the owner's "starting quiet"). */
	private static final float QUIET = 0.25f;
	/** The intro's exit cue, 13.714 s into its file (69B7's intro segment: exit 13.929 s less entry 0.214 s, H). */
	private static final long INTRO_EXIT_MS = 13714;
	/** The fade at the end: 0.5 s (music-round9: the overtime cue's fade out, H), in ticks. */
	private static final int FADE_TICKS = 10;
	/** How fast the volume follows the dragon's health (ours: a 2% step eased over about half a second). */
	private static final float EASE = 0.15f;
	/** A section stopped from outside (a level change, /stopsound) comes back after this long (ours). */
	private static final long RESTART_MS = 1000;

	private static @Nullable Section intro;
	private static @Nullable Section loop;
	/** The fight being heard (EndPhasePayload.fight), and whether there is one. */
	private static int fight;
	private static boolean inFight;
	/** Whether the music should be heard (false once it has been told to stop, or is fading). */
	private static boolean on;
	private static float target;
	private static float level;
	/** Ticks left of the fade out, or -1. */
	private static int fading = -1;
	/** Played time of the intro (ms, the game's pauses left out), for its exit cue. */
	private static long introPlayed;
	private static long lastMs;
	private static long lastStart;

	/** The server's news (EndPhasePayload). */
	public static void receive(EndPhasePayload payload) {
		// a replayed fight carries its own news: not during a showing (as VICTORY: OvercraftClient)
		if (ClientPotg.active()) {
			return;
		}
		if (payload.share() < 0) {
			fadeOut();
			return;
		}
		target = volumeAt(payload.share());
		if (!inFight || payload.fight() != fight) {
			stopNow();
			inFight = true;
			fight = payload.fight();
			level = target;
			on = true;
			start(true);
		} else if (!on || fading >= 0) {
			// the same fight back (its dragon loaded again): on the loop, not its intro again
			stopNow();
			level = target;
			on = true;
			start(false);
		}
	}

	/** The share of FULL at a dragon health share: QUIET at a third, rising evenly to full at none (ours). */
	static float volumeAt(float share) {
		float t = Mth.clamp(1 - share / EndPhase.START_SHARE, 0, 1);
		return QUIET + (1 - QUIET) * t;
	}

	/** The killing blow (VictoryHud.show): the fade out, and this dragon's music is over. */
	public static void victory() {
		fadeOut();
		inFight = false;
	}

	/** The music should be heard: MinecraftSoundKeepMixin keeps it through a level change while it is. */
	public static boolean playing() {
		return on && fading < 0 && (active(intro) || active(loop));
	}

	/** Each client tick: the volume, the fade, the loop on the intro's exit cue, a section stopped from outside. */
	public static void tick(Minecraft mc) {
		if (intro == null && loop == null && !on) {
			return;
		}
		long now = Util.getMillis();
		long dt = Math.min(Math.max(0, now - lastMs), 250);
		lastMs = now;
		if (mc.isPaused()) {
			// the sounds are paused with the game: so is the intro's clock
			return;
		}
		level += (target - level) * EASE;
		if (fading >= 0) {
			if (--fading < 0) {
				stopNow();
			}
			return;
		}
		if (!on) {
			return;
		}
		if (intro != null && loop == null) {
			introPlayed += dt;
			// the tick nearest the cue (ticks are 50 ms apart)
			if (introPlayed >= INTRO_EXIT_MS - 25) {
				startLoop(mc);
			}
		}
		if (!active(intro) && !active(loop) && now - lastStart > RESTART_MS) {
			// stopped from outside (a level change, /stopsound) or the intro cut short: on with the loop
			stopNow();
			on = true;
			start(false);
		}
	}

	/** Leaving the world: nothing carries over. */
	public static void clear() {
		stopNow();
		inFight = false;
		fight = 0;
	}

	private static void fadeOut() {
		if (on && (intro != null || loop != null)) {
			fading = FADE_TICKS;
		} else {
			stopNow();
		}
		on = false;
	}

	private static void start(boolean withIntro) {
		Minecraft mc = Minecraft.getInstance();
		lastMs = Util.getMillis();
		lastStart = lastMs;
		introPlayed = 0;
		if (withIntro && has(mc, OcSounds.MUSIC_OVERTIME_INTRO)) {
			intro = new Section(OcSounds.MUSIC_OVERTIME_INTRO, false);
			mc.getSoundManager().play(intro);
		} else {
			startLoop(mc);
		}
	}

	private static void startLoop(Minecraft mc) {
		lastStart = Util.getMillis();
		if (!has(mc, OcSounds.MUSIC_OVERTIME_LOOP)) {
			// no pack, no music (as PotgMusic: the sounds come from the local pack only)
			on = false;
			return;
		}
		loop = new Section(OcSounds.MUSIC_OVERTIME_LOOP, true);
		mc.getSoundManager().play(loop);
	}

	private static boolean has(Minecraft mc, SoundEvent event) {
		return mc.getSoundManager().getSoundEvent(event.location()) != null;
	}

	private static void stopNow() {
		var sounds = Minecraft.getInstance().getSoundManager();
		if (intro != null) {
			sounds.stop(intro);
			intro = null;
		}
		if (loop != null) {
			sounds.stop(loop);
			loop = null;
		}
		fading = -1;
		on = false;
	}

	private static boolean active(@Nullable SoundInstance section) {
		return section != null && Minecraft.getInstance().getSoundManager().isActive(section);
	}

	/** The gain right now: the dragon's share, through the fade. */
	private static float gain() {
		float fade = fading >= 0 ? (float) fading / FADE_TICKS : 1;
		return FULL * level * fade;
	}

	/** One section of the cue (the intro, or the loop, looping), its volume following the dragon. */
	private static final class Section extends AbstractTickableSoundInstance {
		Section(SoundEvent event, boolean looping) {
			super(event, SoundSource.UI, SoundInstance.createUnseededRandom());
			this.looping = looping;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
			this.volume = gain();
		}

		@Override
		public void tick() {
			if (this != intro && this != loop) {
				stop();
				return;
			}
			volume = gain();
		}

		@Override
		public boolean canStartSilent() {
			// the Interface slider at 0, or the quietest start: it still keeps its time
			return true;
		}
	}
}
