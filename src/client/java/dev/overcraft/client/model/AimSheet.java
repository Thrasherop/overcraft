package dev.overcraft.client.model;

import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * An aim pose sheet (the blend trees' AnimationPose2d on the aim drivers: its X on df 0xB6, its Y on df 0x14, modes 6 / 5):
 * an additive clip whose frames are not a motion but a table of poses, so the frame is picked by where the hero aims. Its node
 * holds the clip and the drivers and no layout (m_668D2844 is 3 on all 218 sheets read), and the layouts differ per sheet, so
 * the pitch line is measured from the clip: the frames where the spine-to-head chain goes from bent forward (looking down),
 * through a neutral frame, to bent back (techniques/animation/aim_sheets.md).
 * <ul>
 * <li>a line of consecutive frames: Kiriko's 63 = 3 x 21 (21-41, 31 level), Ana's 84 = 21 + 3 x 21 (42-62), Sojourn's 66 =
 * 3 x 11 + 3 x 11 (11-21), Sombra's run sheets of 7, Lúcio's 74 (53-59);</li>
 * <li>rows of seven (one per pitch, seven yaws each), so the pitch line steps by 7: Brigitte's 21 (3, 10, 17) and her shield's
 * 28, whose rows are in another order and four of them.</li>
 * </ul>
 */
public final class AimSheet {
	/** A sheet's own frames, by the keys of its longest channel. */
	private static final String[] CHAIN = {"Spine_1", "Spine_2", "Spine_3", "Neck", "Head"};
	/** Aim pitch (degrees) at the sheet's ends: Minecraft's whole range (ours; each end's gun was measured against it). */
	public static final float AIM_PITCH = 90;
	/** The pitch line's frames in key index, down end first, and which of them is level. */
	public record Layout(int[] line, int neutral) {
	}

	private static final Map<SkinnedModel.Clip, AimSheet> SHEETS = new java.util.IdentityHashMap<>();

	/** A resource reload: the old models' clips go with them. */
	static void clear() {
		SHEETS.clear();
	}
	private static final AimSheet NONE = new AimSheet(null, null, null, new float[0]);

	private final SkinnedModel.@Nullable Clip clip;
	private final @Nullable Layout layout;
	private final float @Nullable [] times;
	/** The chain's summed pitch at each line entry (degrees, + down): what the sheet has already turned the head by. */
	private final float[] pitch;

	private AimSheet(SkinnedModel.@Nullable Clip clip, @Nullable Layout layout, float @Nullable [] times, float[] pitch) {
		this.clip = clip;
		this.layout = layout;
		this.times = times;
		this.pitch = pitch;
	}

	/** The clip's sheet, measured once, or null when the clip doesn't measure as one (Sojourn's add_crouch is a breath). */
	public static @Nullable AimSheet of(SkinnedModel model, SkinnedModel.@Nullable Clip clip) {
		if (clip == null) {
			return null;
		}
		AimSheet sheet = SHEETS.computeIfAbsent(clip, c -> measure(model, c));
		return sheet == NONE ? null : sheet;
	}

	private static AimSheet measure(SkinnedModel model, SkinnedModel.Clip clip) {
		float[] times = null;
		for (SkinnedModel.Channel ch : clip.channels()) {
			if (ch.path() == 'R' && (times == null || ch.times().length > times.length)) {
				times = ch.times();
			}
		}
		if (times == null || times.length < 3) {
			return NONE;
		}
		// the export repeats the last frame: the sheet's table ends one key before
		int n = times.length - 1;
		float[] x = new float[n], y = new float[n], total = new float[n];
		Skeleton.Pose at = new Skeleton.Pose(model.nodeNames.length);
		int found = 0;
		for (int k = 0; k < n; k++) {
			for (Quaternionf r : at.r) {
				r.identity();
			}
			Skeleton.sample(clip, times[k], at);
			found = 0;
			for (String name : CHAIN) {
				int node = model.node(name);
				if (node < 0) {
					continue;
				}
				found++;
				Quaternionf q = at.r[node];
				float s = q.w < 0 ? -1 : 1;
				float w = Math.min(1, Math.abs(q.w));
				float angle = 2 * (float) Math.toDegrees(Math.acos(w));
				float sin = (float) Math.sqrt(Math.max(0, 1 - w * w));
				if (sin > 1.0E-5f) {
					float rx = s * q.x / sin * angle, ry = s * q.y / sin * angle, rz = s * q.z / sin * angle;
					x[k] += rx;
					y[k] += ry;
					total[k] += Math.abs(rx) + Math.abs(ry) + Math.abs(rz);
				}
			}
		}
		Layout layout = found == 0 ? null : layout(x, y, total);
		if (layout == null) {
			return NONE;
		}
		float[] pitch = new float[layout.line().length];
		for (int i = 0; i < pitch.length; i++) {
			pitch[i] = x[layout.line()[i]];
		}
		return new AimSheet(clip, layout, times, pitch);
	}

	/** How far from zero (degrees, the whole chain's axes summed) a frame may be and still be the sheet's level frame. */
	private static final float NEUTRAL = 25;
	private static final float NEAR_NEUTRAL = 1.5f;
	/** A line's whole pitch span must be at least this (degrees): a sheet's narrower axis, or a breath, is not an aim. */
	private static final float MIN_SPAN = 30;
	/** Frames this far apart in pitch or from level in yaw are another row of the table. */
	private static final float MAX_STEP = 45, MAX_YAW = 55;
	/**
	 * Each side of level must turn at least this (degrees): Sojourn's add_crouch_walk passed a span test alone as frames 17, 3, 31 with its chain at +39, 0, -3,
	 * and aiming up left her gun level (critique_parts A1).
	 */
	private static final float MIN_SIDE = 10;
	/** Rows of a yaw-and-pitch table hold this many yaws in every sheet read (Brigitte, Sigma, Sombra's stealth). */
	private static final int ROW = 7;
	private static final float ROW_YAW = 20, ROW_STEP = 8;

	/**
	 * The pitch line from each frame's chain pitch x (+ down), yaw y and total turn, or null: the longest run of consecutive
	 * frames through a level frame that bends monotonically, else a level frame's column across rows of seven.
	 */
	static @Nullable Layout layout(float[] x, float[] y, float[] total) {
		float min = Float.MAX_VALUE;
		for (float t : total) {
			min = Math.min(min, t);
		}
		if (min > NEUTRAL) {
			return null;
		}
		List<Integer> level = new ArrayList<>();
		for (int f = 0; f < total.length; f++) {
			if (total[f] <= min + NEAR_NEUTRAL) {
				level.add(f);
			}
		}
		int bestA = -1, bestB = -1, bestC = -1;
		float bestSpan = MIN_SPAN - 1.0E-3f;
		for (int c : level) {
			int a = c;
			while (a > 0 && x[a - 1] > x[a] && x[a - 1] - x[a] < MAX_STEP && Math.abs(y[a - 1] - y[c]) < MAX_YAW) {
				a--;
			}
			int b = c;
			while (b < x.length - 1 && x[b + 1] < x[b] && x[b] - x[b + 1] < MAX_STEP && Math.abs(y[b + 1] - y[c]) < MAX_YAW) {
				b++;
			}
			float span = x[a] - x[b];
			if (span > bestSpan && a < c && c < b && x[a] - x[c] >= MIN_SIDE && x[c] - x[b] >= MIN_SIDE) {
				bestSpan = span;
				bestA = a;
				bestB = b;
				bestC = c;
			}
		}
		if (bestC >= 0) {
			int[] line = new int[bestB - bestA + 1];
			for (int i = 0; i < line.length; i++) {
				line[i] = bestA + i;
			}
			return new Layout(line, bestC - bestA);
		}
		for (int c : level) {
			List<Integer> column = new ArrayList<>();
			for (int f = c % ROW; f < x.length; f += ROW) {
				// (a row at about level's pitch is no step of the line: Sojourn's crouch-walk frame 31 at -3 between 3 at 0 and 10 at -39)
				if (Math.abs(y[f] - y[c]) < ROW_YAW && (f == c || Math.abs(x[f] - x[c]) >= ROW_STEP)) {
					column.add(f);
				}
			}
			column.sort(Comparator.comparing(f -> -x[f]));
			int n = column.size();
			int at = column.indexOf(c);
			if (n >= 3 && 0 < at && at < n - 1 && x[column.get(0)] - x[column.get(n - 1)] >= MIN_SPAN
				&& x[column.get(0)] - x[c] >= MIN_SIDE && x[c] - x[column.get(n - 1)] >= MIN_SIDE) {
				return new Layout(column.stream().mapToInt(Integer::intValue).toArray(), at);
			}
		}
		return null;
	}

	/** The share along the line (entries, fractional) for an aim pitch (degrees, + down). */
	private float position(float aimPitch) {
		float d = Math.clamp(aimPitch / AIM_PITCH, -1, 1);
		int c = layout.neutral();
		return d >= 0 ? c * (1 - d) : c + -d * (layout.line().length - 1 - c);
	}

	/** Lays the sheet's pose for this aim over the pose. The head's own look takes the rest of the pitch: {@link #pitchAt}. */
	public void apply(Skeleton.Pose pose, float aimPitch, float weight, boolean @Nullable [] mask) {
		float at = position(aimPitch);
		int a = (int) Math.floor(at);
		int b = Math.min(a + 1, layout.line().length - 1);
		float f = at - a;
		int fa = layout.line()[a], fb = layout.line()[b];
		if (fb == fa + 1) {
			// neighbouring frames: the clip's own interpolation
			Skeleton.addAdditive(clip, times[fa] + (times[fb] - times[fa]) * f, pose, weight, mask);
		} else {
			if (a != layout.neutral()) {
				Skeleton.addAdditive(clip, times[fa], pose, weight * (1 - f), mask);
			}
			if (b != layout.neutral() && f > 0) {
				Skeleton.addAdditive(clip, times[fb], pose, weight * f, mask);
			}
		}
	}

	/** What the sheet's spine, neck and head have already turned for this aim (degrees, + down). */
	public float pitchAt(float aimPitch) {
		float at = position(aimPitch);
		int a = (int) Math.floor(at);
		int b = Math.min(a + 1, pitch.length - 1);
		return pitch[a] + (pitch[b] - pitch[a]) * (at - a);
	}

	/** Dev: the line's frames (DevServer "aimsheets"). */
	public String describe() {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < layout.line().length; i++) {
			out.append(i == layout.neutral() ? "[" : "").append(layout.line()[i]).append(i == layout.neutral() ? "]" : "").append(' ');
		}
		return out + String.format(java.util.Locale.ROOT, "(%.0f to %.0f deg)", pitch[0], pitch[pitch.length - 1]);
	}
}
