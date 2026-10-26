package dev.overcraft.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A hero's model from a .glb: the skeleton (nodes with their rest pose), the skin, the mesh pieces (one per material)
 * and the animation clips. glTF space: Y up, metres; the hero faces +Z.
 */
public final class SkinnedModel {
	public final String[] nodeNames;
	public final int[] parent;
	public final Vector3f[] restT;
	public final Quaternionf[] restR;
	public final Vector3f[] restS;
	/** Nodes ordered parents first. */
	public final int[] order;
	public final int[] joints;
	public final Matrix4f[] inverseBind;
	public final List<Piece> pieces = new ArrayList<>();
	public final List<byte[]> images = new ArrayList<>();
	public final Map<String, Clip> clips = new HashMap<>();

	/** One mesh primitive: its vertices, skinning, triangles and image. */
	public record Piece(float[] pos, float[] normal, float[] uv, int[] joint4, float[] weight4, int[] indices, int image) {
		public int vertexCount() {
			return pos.length / 3;
		}
	}

	/** One animation track: a node's translation, rotation or scale over time. */
	public record Channel(int node, char path, float[] times, float[] values, boolean step) {
	}

	public record Clip(String name, float duration, List<Channel> channels) {
	}

	private SkinnedModel(int nodeCount) {
		nodeNames = new String[nodeCount];
		parent = new int[nodeCount];
		restT = new Vector3f[nodeCount];
		restR = new Quaternionf[nodeCount];
		restS = new Vector3f[nodeCount];
		order = new int[nodeCount];
		joints = new int[0];
		inverseBind = new Matrix4f[0];
	}

	private SkinnedModel(SkinnedModel nodes, int[] joints, Matrix4f[] inverseBind) {
		nodeNames = nodes.nodeNames;
		parent = nodes.parent;
		restT = nodes.restT;
		restR = nodes.restR;
		restS = nodes.restS;
		order = nodes.order;
		this.joints = joints;
		this.inverseBind = inverseBind;
	}

	public int node(String name) {
		for (int i = 0; i < nodeNames.length; i++) {
			if (name.equals(nodeNames[i])) {
				return i;
			}
		}
		return -1;
	}

	public static SkinnedModel load(byte[] data) throws IOException {
		Glb glb = Glb.read(data);
		JsonArray nodes = glb.array("nodes");
		SkinnedModel tree = new SkinnedModel(nodes.size());
		java.util.Arrays.fill(tree.parent, -1);
		for (int i = 0; i < nodes.size(); i++) {
			JsonObject n = nodes.get(i).getAsJsonObject();
			tree.nodeNames[i] = n.has("name") ? n.get("name").getAsString() : "node" + i;
			float[] t = Glb.floatArray(n.get("translation"), 0, 0, 0);
			float[] r = Glb.floatArray(n.get("rotation"), 0, 0, 0, 1);
			float[] s = Glb.floatArray(n.get("scale"), 1, 1, 1);
			if (n.has("matrix")) {
				Matrix4f m = new Matrix4f().set(Glb.floatArray(n.get("matrix")));
				Vector3f mt = m.getTranslation(new Vector3f());
				Quaternionf mr = m.getUnnormalizedRotation(new Quaternionf()).normalize();
				Vector3f ms = m.getScale(new Vector3f());
				t = new float[] {mt.x, mt.y, mt.z};
				r = new float[] {mr.x, mr.y, mr.z, mr.w};
				s = new float[] {ms.x, ms.y, ms.z};
			}
			tree.restT[i] = new Vector3f(t[0], t[1], t[2]);
			tree.restR[i] = new Quaternionf(r[0], r[1], r[2], r[3]);
			tree.restS[i] = new Vector3f(s[0], s[1], s[2]);
			if (n.has("children")) {
				for (JsonElement c : n.getAsJsonArray("children")) {
					tree.parent[c.getAsInt()] = i;
				}
			}
		}
		// parents before children
		int k = 0;
		boolean[] done = new boolean[nodes.size()];
		while (k < nodes.size()) {
			for (int i = 0; i < nodes.size(); i++) {
				if (!done[i] && (tree.parent[i] < 0 || done[tree.parent[i]])) {
					tree.order[k++] = i;
					done[i] = true;
				}
			}
		}

		int[] joints;
		Matrix4f[] inverseBind;
		boolean skinned = glb.array("skins").size() > 0;
		if (skinned) {
			JsonObject skin = glb.get("skins", 0);
			JsonArray jointList = skin.getAsJsonArray("joints");
			joints = new int[jointList.size()];
			for (int i = 0; i < joints.length; i++) {
				joints[i] = jointList.get(i).getAsInt();
			}
			float[] ibm = glb.floats(skin.get("inverseBindMatrices").getAsInt());
			inverseBind = new Matrix4f[joints.length];
			for (int i = 0; i < joints.length; i++) {
				inverseBind[i] = new Matrix4f().set(ibm, i * 16);
			}
		} else {
			// a static mesh (an effect's, like the torii gate): one joint, the node holding the mesh
			int meshNode = 0;
			for (int i = 0; i < nodes.size(); i++) {
				if (nodes.get(i).getAsJsonObject().has("mesh")) {
					meshNode = i;
					break;
				}
			}
			joints = new int[] {meshNode};
			inverseBind = new Matrix4f[] {new Matrix4f()};
		}
		SkinnedModel model = new SkinnedModel(tree, joints, inverseBind);

		// images (embedded PNGs) and which image each material uses as its base colour
		for (JsonElement img : glb.array("images")) {
			model.images.add(glb.bufferView(img.getAsJsonObject().get("bufferView").getAsInt()));
		}
		int[] materialImage = new int[glb.array("materials").size()];
		for (int i = 0; i < materialImage.length; i++) {
			JsonObject mat = glb.get("materials", i);
			materialImage[i] = -1;
			if (mat.has("pbrMetallicRoughness") && mat.getAsJsonObject("pbrMetallicRoughness").has("baseColorTexture")) {
				int tex = mat.getAsJsonObject("pbrMetallicRoughness").getAsJsonObject("baseColorTexture").get("index").getAsInt();
				materialImage[i] = glb.get("textures", tex).get("source").getAsInt();
			}
		}

		for (JsonElement m : glb.array("meshes")) {
			for (JsonElement p : m.getAsJsonObject().getAsJsonArray("primitives")) {
				JsonObject prim = p.getAsJsonObject();
				JsonObject attrs = prim.getAsJsonObject("attributes");
				float[] pos = glb.floats(attrs.get("POSITION").getAsInt());
				int count = pos.length / 3;
				float[] normal = attrs.has("NORMAL") ? glb.floats(attrs.get("NORMAL").getAsInt()) : new float[count * 3];
				float[] uv = attrs.has("TEXCOORD_0") ? glb.floats(attrs.get("TEXCOORD_0").getAsInt()) : new float[count * 2];
				int[] joint4 = attrs.has("JOINTS_0") ? glb.ints(attrs.get("JOINTS_0").getAsInt()) : new int[count * 4];
				float[] weight4 = attrs.has("WEIGHTS_0") ? glb.floats(attrs.get("WEIGHTS_0").getAsInt()) : new float[count * 4];
				if (!skinned) {
					for (int v = 0; v < count; v++) {
						weight4[v * 4] = 1;
					}
				}
				int[] indices = prim.has("indices") ? glb.ints(prim.get("indices").getAsInt()) : sequence(count);
				int material = prim.has("material") ? prim.get("material").getAsInt() : -1;
				model.pieces.add(new Piece(pos, normal, uv, joint4, weight4, indices, material >= 0 ? materialImage[material] : -1));
			}
		}

		for (JsonElement a : glb.array("animations")) {
			JsonObject anim = a.getAsJsonObject();
			String name = anim.has("name") ? anim.get("name").getAsString() : "clip" + model.clips.size();
			JsonArray samplers = anim.getAsJsonArray("samplers");
			List<Channel> channels = new ArrayList<>();
			float duration = 0;
			for (JsonElement c : anim.getAsJsonArray("channels")) {
				JsonObject ch = c.getAsJsonObject();
				JsonObject target = ch.getAsJsonObject("target");
				if (!target.has("node")) {
					continue;
				}
				String path = target.get("path").getAsString();
				char kind = switch (path) {
					case "translation" -> 'T';
					case "rotation" -> 'R';
					case "scale" -> 'S';
					default -> 0;
				};
				if (kind == 0) {
					continue;
				}
				JsonObject sampler = samplers.get(ch.get("sampler").getAsInt()).getAsJsonObject();
				float[] times = glb.floats(sampler.get("input").getAsInt());
				float[] values = glb.floats(sampler.get("output").getAsInt());
				boolean step = sampler.has("interpolation") && "STEP".equals(sampler.get("interpolation").getAsString());
				if (kind == 'R') {
					dropRotationSpikes(values);
				}
				channels.add(new Channel(target.get("node").getAsInt(), kind, times, values, step));
				duration = Math.max(duration, times[times.length - 1]);
			}
			model.clips.put(name, new Clip(name, duration, channels));
		}
		return model;
	}

	private static int[] sequence(int n) {
		int[] out = new int[n];
		for (int i = 0; i < n; i++) {
			out[i] = i;
		}
		return out;
	}

	/**
	 * Mends single broken keys in a rotation track (x, y, z, w per key): a key turned more than SPIKE_ANGLE from both
	 * neighbours while they agree with each other takes their average. Overwatch's standing breath (AFDB, as exported)
	 * has two such keys on the right thigh, 180 degrees out for one frame, which kicked her leg out sideways every
	 * 3 seconds at idle (seen under casts, reloads and the Suzu throw). Real motion never turns that far in one frame.
	 */
	static int dropRotationSpikes(float[] q) {
		int keys = q.length / 4;
		int mended = 0;
		for (int k = 1; k < keys - 1; k++) {
			float toPrev = angle(q, k, k - 1);
			float toNext = angle(q, k, k + 1);
			if (toPrev > SPIKE_ANGLE && toNext > SPIKE_ANGLE && angle(q, k - 1, k + 1) < SPIKE_ANGLE / 4) {
				float sign = dot(q, k - 1, k + 1) < 0 ? -1 : 1;
				float len = 0;
				for (int c = 0; c < 4; c++) {
					q[k * 4 + c] = (q[(k - 1) * 4 + c] + sign * q[(k + 1) * 4 + c]) / 2;
					len += q[k * 4 + c] * q[k * 4 + c];
				}
				len = (float) Math.sqrt(len);
				for (int c = 0; c < 4; c++) {
					q[k * 4 + c] /= len;
				}
				mended++;
			}
		}
		return mended;
	}

	/** Above this (radians) between neighbouring keys a rotation key is taken as broken. */
	private static final float SPIKE_ANGLE = (float) Math.toRadians(90);

	private static float dot(float[] q, int a, int b) {
		return q[a * 4] * q[b * 4] + q[a * 4 + 1] * q[b * 4 + 1] + q[a * 4 + 2] * q[b * 4 + 2] + q[a * 4 + 3] * q[b * 4 + 3];
	}

	/** The angle between two keys' rotations (either sign of a quaternion is the same rotation). */
	private static float angle(float[] q, int a, int b) {
		float d = Math.min(1, Math.abs(dot(q, a, b)));
		return 2 * (float) Math.acos(d);
	}
}
