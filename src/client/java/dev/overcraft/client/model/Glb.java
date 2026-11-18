package dev.overcraft.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** A binary glTF (.glb) file: its JSON and binary chunk, and reading its accessors. Only what hero models use. */
public final class Glb {
	/** The JSON but its two largest arrays, accessors and buffer views (read into the tables below). */
	public final JsonObject json;
	private final ByteBuffer bin;
	/**
	 * Accessors and buffer views as plain numbers. A hero's file holds ~77,000 of each (a buffer view per clip channel); as a JSON
	 * tree they took several hundred MB for the moment it was read, and at the launcher's default 2 GB a reload ran out of memory
	 * parsing one (#166, played).
	 */
	private final Table accessors, views;

	private Glb(JsonObject json, ByteBuffer bin, Table accessors, Table views) {
		this.json = json;
		this.bin = bin;
		this.accessors = accessors;
		this.views = views;
	}

	/** Rows of int columns, grown as read; -1 where a field is absent. */
	private static final class Table {
		final String[] columns;
		int[][] cols;
		int size;

		Table(String... columns) {
			this.columns = columns;
			cols = new int[columns.length][64];
		}

		int add() {
			if (size == cols[0].length) {
				for (int c = 0; c < cols.length; c++) {
					cols[c] = java.util.Arrays.copyOf(cols[c], size * 2);
				}
			}
			for (int[] col : cols) {
				col[size] = -1;
			}
			return size++;
		}

		int get(int row, int column) {
			if (row < 0 || row >= size) {
				throw new IndexOutOfBoundsException("glTF index " + row);
			}
			return cols[column][row];
		}

		void read(JsonReader in, java.util.function.BiFunction<String, JsonReader, Integer> special) throws IOException {
			in.beginArray();
			while (in.hasNext()) {
				int row = add();
				in.beginObject();
				while (in.hasNext()) {
					String name = in.nextName();
					int column = java.util.Arrays.asList(columns).indexOf(name);
					if (column < 0) {
						in.skipValue();
						continue;
					}
					Integer value = special.apply(name, in);
					cols[column][row] = value != null ? value : in.nextInt();
				}
				in.endObject();
			}
			in.endArray();
		}
	}

	private static final int ACC_VIEW = 0, ACC_OFFSET = 1, ACC_COUNT = 2, ACC_TYPE = 3, ACC_COMPONENT = 4, ACC_NORMALIZED = 5;
	private static final int VIEW_OFFSET = 0, VIEW_LENGTH = 1, VIEW_STRIDE = 2;

	public static Glb read(byte[] data) throws IOException {
		ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		if (buf.getInt() != 0x46546C67) {
			throw new IOException("not a .glb file");
		}
		buf.getInt(); // version
		int length = buf.getInt();
		JsonObject json = null;
		ByteBuffer bin = null;
		Table accessors = new Table("bufferView", "byteOffset", "count", "type", "componentType", "normalized");
		Table views = new Table("byteOffset", "byteLength", "byteStride");
		while (buf.position() < length) {
			int chunkLength = buf.getInt();
			int chunkType = buf.getInt();
			if (chunkType == 0x4E4F534A) {
				json = new JsonObject();
				// streamed straight from the bytes (no copy as a String), the two big arrays into tables, the rest as a tree
				try (JsonReader in = new JsonReader(new java.io.InputStreamReader(
					new java.io.ByteArrayInputStream(data, buf.position(), chunkLength), StandardCharsets.UTF_8))) {
					in.beginObject();
					while (in.hasNext()) {
						String name = in.nextName();
						switch (name) {
							case "accessors" -> accessors.read(in, (field, r) -> {
								try {
									return switch (field) {
										case "type" -> components(r.nextString());
										case "normalized" -> r.nextBoolean() ? 1 : 0;
										default -> null;
									};
								} catch (IOException e) {
									throw new java.io.UncheckedIOException(e);
								}
							});
							case "bufferViews" -> views.read(in, (field, r) -> null);
							default -> json.add(name, JsonParser.parseReader(in));
						}
					}
					in.endObject();
				} catch (java.io.UncheckedIOException e) {
					throw e.getCause();
				}
			} else if (chunkType == 0x004E4942) {
				bin = ByteBuffer.wrap(data, buf.position(), chunkLength).slice().order(ByteOrder.LITTLE_ENDIAN);
			}
			buf.position(buf.position() + chunkLength);
		}
		if (json == null || bin == null) {
			throw new IOException(".glb without JSON or binary chunk");
		}
		return new Glb(json, bin, accessors, views);
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
			case "MAT2" -> 4;
			case "MAT3" -> 9;
			case "MAT4" -> 16;
			default -> throw new IllegalArgumentException("accessor type " + type);
		};
	}

	/** An accessor as floats (normalized integers become 0..1). */
	public float[] floats(int accessorIndex) {
		int count = accessors.get(accessorIndex, ACC_COUNT);
		int comps = accessors.get(accessorIndex, ACC_TYPE);
		int type = accessors.get(accessorIndex, ACC_COMPONENT);
		boolean normalized = accessors.get(accessorIndex, ACC_NORMALIZED) == 1;
		float[] out = new float[count * comps];
		Reader r = reader(accessorIndex, comps, type);
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
		int count = accessors.get(accessorIndex, ACC_COUNT);
		int comps = accessors.get(accessorIndex, ACC_TYPE);
		int type = accessors.get(accessorIndex, ACC_COMPONENT);
		int[] out = new int[count * comps];
		Reader r = reader(accessorIndex, comps, type);
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
		int offset = Math.max(0, views.get(index, VIEW_OFFSET));
		byte[] out = new byte[views.get(index, VIEW_LENGTH)];
		bin.get(offset, out);
		return out;
	}

	private Reader reader(int accessor, int comps, int type) {
		int view = accessors.get(accessor, ACC_VIEW);
		int base = Math.max(0, views.get(view, VIEW_OFFSET)) + Math.max(0, accessors.get(accessor, ACC_OFFSET));
		int size = switch (type) {
			case 5120, 5121 -> 1;
			case 5122, 5123 -> 2;
			default -> 4;
		};
		int stride = views.get(view, VIEW_STRIDE) > 0 ? views.get(view, VIEW_STRIDE) : size * comps;
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
