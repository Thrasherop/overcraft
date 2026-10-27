package dev.overcraft.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** A binary glTF (.glb) file: its JSON and binary chunk, and reading its accessors. Only what hero models use. */
public final class Glb {
	public final JsonObject json;
	private final ByteBuffer bin;

	private Glb(JsonObject json, ByteBuffer bin) {
		this.json = json;
		this.bin = bin;
	}

	public static Glb read(byte[] data) throws IOException {
		ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		if (buf.getInt() != 0x46546C67) {
			throw new IOException("not a .glb file");
		}
		buf.getInt(); // version
		int length = buf.getInt();
		JsonObject json = null;
		ByteBuffer bin = null;
		while (buf.position() < length) {
			int chunkLength = buf.getInt();
			int chunkType = buf.getInt();
			if (chunkType == 0x4E4F534A) {
				json = JsonParser.parseString(new String(data, buf.position(), chunkLength, StandardCharsets.UTF_8)).getAsJsonObject();
			} else if (chunkType == 0x004E4942) {
				bin = ByteBuffer.wrap(data, buf.position(), chunkLength).slice().order(ByteOrder.LITTLE_ENDIAN);
			}
			buf.position(buf.position() + chunkLength);
		}
		if (json == null || bin == null) {
			throw new IOException(".glb without JSON or binary chunk");
		}
		return new Glb(json, bin);
	}

	public JsonArray array(String name) {
		return json.has(name) ? json.getAsJsonArray(name) : new JsonArray();
	}

	public JsonObject get(String name, int index) {
		return array(name).get(index).getAsJsonObject();
	}

	private static int components(String type) {
		return switch (type) {
			case "SCALAR" -> 1;
			case "VEC2" -> 2;
			case "VEC3" -> 3;
			case "VEC4" -> 4;
			case "MAT4" -> 16;
			default -> throw new IllegalArgumentException("accessor type " + type);
		};
	}

	/** An accessor as floats (normalized integers become 0..1). */
	public float[] floats(int accessorIndex) {
		JsonObject acc = get("accessors", accessorIndex);
		int count = acc.get("count").getAsInt();
		int comps = components(acc.get("type").getAsString());
		int type = acc.get("componentType").getAsInt();
		boolean normalized = acc.has("normalized") && acc.get("normalized").getAsBoolean();
		float[] out = new float[count * comps];
		Reader r = reader(acc, comps, type);
		for (int i = 0; i < count; i++) {
			for (int c = 0; c < comps; c++) {
				out[i * comps + c] = switch (type) {
					case 5126 -> r.buf.getFloat(r.at(i, c));
					case 5121 -> (r.buf.get(r.at(i, c)) & 0xFF) / (normalized ? 255f : 1f);
					case 5123 -> (r.buf.getShort(r.at(i, c)) & 0xFFFF) / (normalized ? 65535f : 1f);
					case 5125 -> (float) Integer.toUnsignedLong(r.buf.getInt(r.at(i, c)));
					default -> throw new IllegalArgumentException("component type " + type);
				};
			}
		}
		return out;
	}

	/** An integer accessor (indices, joint numbers). */
	public int[] ints(int accessorIndex) {
		JsonObject acc = get("accessors", accessorIndex);
		int count = acc.get("count").getAsInt();
		int comps = components(acc.get("type").getAsString());
		int type = acc.get("componentType").getAsInt();
		int[] out = new int[count * comps];
		Reader r = reader(acc, comps, type);
		for (int i = 0; i < count; i++) {
			for (int c = 0; c < comps; c++) {
				out[i * comps + c] = switch (type) {
					case 5121 -> r.buf.get(r.at(i, c)) & 0xFF;
					case 5123 -> r.buf.getShort(r.at(i, c)) & 0xFFFF;
					case 5125 -> r.buf.getInt(r.at(i, c));
					default -> throw new IllegalArgumentException("integer component type " + type);
				};
			}
		}
		return out;
	}

	/** The bytes of a buffer view (embedded images). */
	public byte[] bufferView(int index) {
		JsonObject view = get("bufferViews", index);
		int offset = view.has("byteOffset") ? view.get("byteOffset").getAsInt() : 0;
		byte[] out = new byte[view.get("byteLength").getAsInt()];
		bin.get(offset, out);
		return out;
	}

	private Reader reader(JsonObject acc, int comps, int type) {
		JsonObject view = get("bufferViews", acc.get("bufferView").getAsInt());
		int base = (view.has("byteOffset") ? view.get("byteOffset").getAsInt() : 0)
			+ (acc.has("byteOffset") ? acc.get("byteOffset").getAsInt() : 0);
		int size = switch (type) {
			case 5120, 5121 -> 1;
			case 5122, 5123 -> 2;
			default -> 4;
		};
		int stride = view.has("byteStride") ? view.get("byteStride").getAsInt() : size * comps;
		return new Reader(bin, base, stride, size);
	}

	private record Reader(ByteBuffer buf, int base, int stride, int size) {
		int at(int i, int c) {
			return base + i * stride + c * size;
		}
	}

	public static float[] floatArray(JsonElement element, float... fallback) {
		if (element == null) {
			return fallback;
		}
		JsonArray a = element.getAsJsonArray();
		float[] out = new float[a.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = a.get(i).getAsFloat();
		}
		return out;
	}
}
