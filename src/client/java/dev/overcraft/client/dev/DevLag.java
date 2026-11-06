package dev.overcraft.client.dev;

import dev.overcraft.net.HeroInputPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Dev only, off by default (the harness's {@code presstrace lag <ms>}, #126): a stand-in for a ping. Her input payloads wait this
 * long on the way out, and the hero payloads the server sends this client (sync, actions, effects, own sounds) this long on the way
 * in, in order. Not real latency: nothing else waits (moves, positional sounds, chat), and the server is still in this process.
 * With no lag set, both calls go straight through and no thread is made.
 */
public final class DevLag {
	private static volatile long millis;
	private static ScheduledExecutorService pool;

	private DevLag() {
	}

	public static synchronized void set(long ms) {
		millis = ms;
		if (ms > 0 && pool == null) {
			pool = Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = new Thread(r, "Overcraft DevLag");
				t.setDaemon(true);
				return t;
			});
		}
	}

	/** A payload from the server: runs now, or after the lag on the client's thread. */
	public static void in(Runnable run) {
		long ms = millis;
		if (ms <= 0) {
			run.run();
		} else {
			pool.schedule(() -> Minecraft.getInstance().execute(run), ms, TimeUnit.MILLISECONDS);
		}
	}

	/** Her input for the server: sent now, or after the lag. */
	public static void out(HeroInputPayload payload) {
		long ms = millis;
		if (ms <= 0) {
			ClientPlayNetworking.send(payload);
		} else {
			pool.schedule(() -> Minecraft.getInstance().execute(() -> ClientPlayNetworking.send(payload)), ms, TimeUnit.MILLISECONDS);
		}
	}

	// ---- the first press: what the client's thread is doing (dev only, presstrace sample) ----

	/** {@code ms} of samples of the client thread's stack, every 2 ms from now: which of our calls, and what's on top. */
	public static String sample(Thread main, int ms) throws InterruptedException {
		Map<String, Integer> ours = new TreeMap<>(), top = new TreeMap<>();
		long end = System.nanoTime() + ms * 1_000_000L;
		int n = 0;
		while (System.nanoTime() < end) {
			StackTraceElement[] stack = main.getStackTrace();
			n++;
			String mine = "(none of ours)";
			for (StackTraceElement e : stack) {
				if (e.getClassName().startsWith("dev.overcraft") && !e.getClassName().contains(".mixin.")) {
					mine = e.getClassName().replace("dev.overcraft.client.", "") + "." + e.getMethodName();
					break;
				}
			}
			ours.merge(mine, 1, Integer::sum);
			if (stack.length > 0) {
				top.merge(stack[0].getClassName() + "." + stack[0].getMethodName(), 1, Integer::sum);
			}
			Thread.sleep(2);
		}
		StringBuilder out = new StringBuilder("samples " + n + "\nours:\n");
		ours.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(25).forEach(e -> out.append(e.getValue()).append(' ').append(e.getKey()).append('\n'));
		out.append("top:\n");
		top.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(25).forEach(e -> out.append(e.getValue()).append(' ').append(e.getKey()).append('\n'));
		return out.toString();
	}
}
