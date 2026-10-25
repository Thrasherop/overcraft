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

		JsonObject skin = glb.get("skins", 0);
		JsonArray jointList = skin.getAsJsonArray("joints");
		int[] joints = new int[jointList.size()];
		for (int i = 0; i < joints.length; i++) {
			joints[i] = jointList.get(i).getAsInt();
		}
		float[] ibm = glb.floats(skin.get("inverseBindMatrices").getAsInt());
		Matrix4f[] inverseBind = new Matrix4f[joints.length];
		for (int i = 0; i < joints.length; i++) {
			inverseBind[i] = new Matrix4f().set(ibm, i * 16);
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
}
