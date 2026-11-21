package dev.overcraft.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coarser copies of a skinned model's pieces for heroes seen from afar (owner, 2026-11-21: "having a handful of enemy heros is
 * pretty laggy client side"). Every frame each hero's whole Overwatch mesh (58-68k triangles) is skinned on the CPU and sent
 * (HeroModelRenderer.emit): eight enemy heroes in view took the game from 30 fps to 13, emit 26.8 ms a frame against 1.4 alone
 * (DevServer "timing", a night with eight /summon'ed enemies).
 * <p>
 * A copy welds the vertices that share a cell of a grid (and a UV cell, so the texture's seams stay apart): each cell keeps its
 * first vertex, whole (position, normal, UV, joints and weights: no averaging, so the skinning stays the original's), and the
 * triangles whose corners met drop out. Measured on the meshes (oc_scratch/lodtest.py): 1.5 cm keeps 33-46 % of the triangles,
 * 2.5 cm 18-29 %, 4 cm 9-17 %, 6 cm 5-10 %. The cell is chosen about one screen pixel wide at the hero's distance (the camera's
 * field of view and the window's height: a scope's zoom keeps the detail), so the coarser shape isn't seen. Built off the render
 * thread, once a piece; the full piece draws until its copy is ready. Ours.
 */
public final class HeroLod {
	private HeroLod() {
	}

	/** The cells, metres (model space). */
	private static final float[] CELLS = {0.015f, 0.025f, 0.04f, 0.06f};
	/** Dev ("timing lod off"): every piece whole, to compare. */
	public static boolean enabled = true;
	/** The copies by piece and level (a missing entry: not asked for yet; the piece itself: building or nothing to gain). */
	private static final Map<SkinnedModel.Piece, SkinnedModel.Piece[]> COPIES = new ConcurrentHashMap<>();
	/** Metres a screen pixel spans per metre of distance, this frame. */
	private static float pixelPerMetre = 0.0013f;
	private static long frameStamp = -1;

	/**
	 * The piece to draw for this pose: the coarsest copy whose cell is under a pixel where it's drawn. Only for poses at the
	 * world's scale (a menu's preview is drawn at dozens of GUI units a metre: always whole).
	 */
	public static SkinnedModel.Piece pick(SkinnedModel.Piece piece, Matrix4f pose) {
		if (!enabled || piece.indices().length < 300) {
			return piece;
		}
		float scale = (float) Math.sqrt(pose.m00() * pose.m00() + pose.m01() * pose.m01() + pose.m02() * pose.m02());
		if (scale <= 0 || scale > 3) {
			return piece;
		}
		refresh();
		float distance = (float) Math.sqrt(pose.m30() * pose.m30() + pose.m31() * pose.m31() + pose.m32() * pose.m32());
		// the cell in the model's own units: its scale (D.Va's MEKA, a scaled-down hero) turns metres into them
		float cell = distance * pixelPerMetre / scale;
		int level = -1;
		for (int i = 0; i < CELLS.length && CELLS[i] <= cell; i++) {
			level = i;
		}
		if (level < 0) {
			return piece;
		}
		SkinnedModel.Piece[] copies = COPIES.computeIfAbsent(piece, p -> new SkinnedModel.Piece[CELLS.length]);
		SkinnedModel.Piece copy = copies[level];
		if (copy == null) {
			copies[level] = piece;
			int at = level;
			Util.backgroundExecutor().execute(() -> copies[at] = weld(piece, CELLS[at]));
			return piece;
		}
		return copy;
	}

	/** Once a frame: a pixel's span from the camera's field of view and the window's height. */
	private static void refresh() {
		Minecraft mc = Minecraft.getInstance();
		long stamp = mc.getFrameTimeNs();
		if (stamp == frameStamp) {
			return;
		}
		frameStamp = stamp;
		float fov = mc.gameRenderer.mainCamera().getFov();
		int height = Math.max(1, mc.getWindow().getHeight());
		pixelPerMetre = (float) (2 * Math.tan(Math.toRadians(fov) / 2) / height);
	}

	/** The piece with its vertices welded on a grid of this cell (see the class). */
	static SkinnedModel.Piece weld(SkinnedModel.Piece piece, float cell) {
		int n = piece.vertexCount();
		float[] pos = piece.pos();
		float[] uv = piece.uv();
		Map<Long, Integer> cells = new HashMap<>();
		int[] keep = new int[n];
		int[] newIndex = new int[n];
		int kept = 0;
		for (int v = 0; v < n; v++) {
			long x = (long) Math.floor(pos[v * 3] / cell);
			long y = (long) Math.floor(pos[v * 3 + 1] / cell);
			long z = (long) Math.floor(pos[v * 3 + 2] / cell);
			long u = uv.length >= (v + 1) * 2 ? (long) Math.floor(uv[v * 2] * 8) : 0;
			long w = uv.length >= (v + 1) * 2 ? (long) Math.floor(uv[v * 2 + 1] * 8) : 0;
			long key = x * 73856093L ^ y * 19349663L ^ z * 83492791L ^ u * 2654435761L ^ w * 40503L;
			Integer first = cells.get(key);
			if (first == null) {
				cells.put(key, kept);
				keep[kept] = v;
				newIndex[v] = kept++;
			} else {
				newIndex[v] = first;
			}
		}
		int[] idx = piece.indices();
		int[] out = new int[idx.length];
		int count = 0;
		for (int t = 0; t + 2 < idx.length; t += 3) {
			int a = newIndex[idx[t]], b = newIndex[idx[t + 1]], c = newIndex[idx[t + 2]];
			if (a != b && b != c && a != c) {
				out[count++] = a;
				out[count++] = b;
				out[count++] = c;
			}
		}
		if (count > idx.length * 0.8) {
			// little to gain: the piece itself
			return piece;
		}
		float[] p = new float[kept * 3];
		float[] nm = new float[kept * 3];
		float[] tex = new float[kept * 2];
		int[] joints = new int[kept * 4];
		float[] weights = new float[kept * 4];
		for (int k = 0; k < kept; k++) {
			int v = keep[k];
			System.arraycopy(pos, v * 3, p, k * 3, 3);
			System.arraycopy(piece.normal(), v * 3, nm, k * 3, 3);
			if (uv.length >= (v + 1) * 2) {
				System.arraycopy(uv, v * 2, tex, k * 2, 2);
			}
			System.arraycopy(piece.joint4(), v * 4, joints, k * 4, 4);
			System.arraycopy(piece.weight4(), v * 4, weights, k * 4, 4);
		}
		return new SkinnedModel.Piece(p, nm, tex, joints, weights, java.util.Arrays.copyOf(out, count), piece.image(), piece.mesh(),
			piece.material());
	}
}
