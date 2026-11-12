package dev.overcraft.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import dev.overcraft.Overcraft;
import dev.overcraft.client.HeroArt;
import dev.overcraft.hero.Hero;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The heroes' models, from {@code assets/overcraft/hero_models/<hero>.glb}: only in the player's local asset pack
 * (never shipped, see LocalAssetPack). A hero without one keeps the Minecraft player model.
 */
public final class HeroModels {
	/**
	 * A loaded model and its textures (by image index; null where the image didn't load), and which images are one flat
	 * colour (Overwatch's shader-driven overlays use them: HeroModelRenderer.isEyeShadow).
	 */
	public record Loaded(SkinnedModel model, @Nullable Identifier[] textures, boolean[] flat, Map<String, Glow> glow, Map<SkinnedModel.Piece, SkinnedModel.Piece> lit) {
	}

	/**
	 * A material's added light (tools/glow_maps.py from its .owmat: hero_models/<model>_glow.json, hero_models/glow/<id>.png), drawn
	 * lit by itself on the piece's own UVs: what Overwatch's shaders glow, and the pieces with no colour image at all.
	 */
	public record Glow(Identifier texture, float strength, @Nullable Identifier alt, boolean glass) {
	}

	private static final Map<Hero, Loaded> MODELS = new EnumMap<>(Hero.class);
	/**
	 * Models that aren't heroes (Kitsune Rush's fox), by file name: {@code hero_models/<name>.glb}. Effect meshes (the
	 * torii, Jetpack Cat's rings and cones: Fx.mesh) load only from this list.
	 */
	private static final List<String> EXTRAS = List.of("kitsune", "projectile_kunai", "projectile_ofuda", "projectile_suzu", "torii",
		"cat_torus", "cat_cone", "cat_sphere", "cat_purr_torus", "cat_purr_wall", "cat_half_ring", "cat_bubble",
		// the rest of #59: her flame tube, the cast's bell, the head slash's arc and dome
		"cat_flame", "cat_bell", "cat_slash_arc", "cat_dome",
		// Lifeweaver's: his Petal Platform, his Tree of Life, and his seed (model 126CC: thrown, and Sow the Seed's on the ground)
		"lifeweaver_platform", "lifeweaver_tree", "lifeweaver_seed", "projectile_lifeweaver_seed",
		// his effects' meshes (LifeweaverFx): the platform's bloom outline, Life Grip's bud and its point's flower
		"lw_flower_ring", "lw_bud", "lw_flower",
		// Sombra's Translocator beacon (model 10518)
		"sombra_beacon",
		// Brigitte's effects' meshes (BrigitteFx): the swing arcs, the shield's outline, the half dome, the band, Inspire's
		// ring, the shard, the whip head's ring, Rally's ground ring
		"brigitte_swing_arc", "brigitte_swing_arc_back", "brigitte_shield_plate", "brigitte_dome", "brigitte_band", "brigitte_ring",
		"brigitte_shard", "brigitte_whip_ring", "brigitte_rally_ring",
		// Juno's effects' meshes (JunoFx): the Hyper Ring (15AD6), the torpedo's nose shell (160EA), the disc flash
		// (5E5E); her rings are cat_torus (15A26)
		"juno_ring", "juno_half_shell", "juno_disc",
		// Sombra's stealth detection ring (129B: SombraFx)
		"sombra_cylinder",
		// Pharah's rocket (12F5B: her launcher's, Barrage's and Rocket Salvo's)
		"projectile_pharah_rocket",
		// Tracer's Pulse Bomb (160BB)
		"projectile_tracer_bomb");
	private static final Map<String, Loaded> EXTRA_MODELS = new HashMap<>();
	private static final List<Identifier> REGISTERED = new ArrayList<>();
	/** Bigger images are halved until they fit: a 4096 texture per material is a lot of video memory for a player. */
	private static final int MAX_TEXTURE = 2048;

	private HeroModels() {
	}

	public static @Nullable Loaded get(Hero hero) {
		return MODELS.get(hero);
	}

	/** The hero's first-person view model (hero_models/<hero>_1p.glb: Overwatch's first-person arms). */
	public static @Nullable Loaded firstPerson(Hero hero) {
		return EXTRA_MODELS.get(hero.id + "_1p");
	}

	/**
	 * The model this entity shows as this hero: D.Va's body (dev.overcraft.dva.Dva.pilotBody): the MEKA (dva.glb, FBB0) or
	 * the pilot (dva_pilot.glb, F2FF); the hero's own for everyone else.
	 */
	public static @Nullable Loaded of(Hero hero, net.minecraft.world.entity.@Nullable Entity entity) {
		if (hero == Hero.DVA && entity instanceof net.minecraft.world.entity.player.Player player && dev.overcraft.dva.Dva.pilotBody(player)) {
			return EXTRA_MODELS.get("dva_pilot");
		}
		return get(hero);
	}

	/** The first-person model for the body this player is in (D.Va's cockpit 10354, or the pilot's arms F32B). */
	public static @Nullable Loaded firstPerson(Hero hero, net.minecraft.world.entity.@Nullable Entity entity) {
		if (hero == Hero.DVA && entity instanceof net.minecraft.world.entity.player.Player player && dev.overcraft.dva.Dva.pilotBody(player)) {
			return EXTRA_MODELS.get("dva_pilot_1p");
		}
		return firstPerson(hero);
	}

	/** The hero's weapon when it's a model of its own (hero_models/<hero>_weapon.glb: Lúcio's Sonic Amplifier), or null. */
	public static @Nullable Loaded weapon(Hero hero) {
		return EXTRA_MODELS.get(hero.id + "_weapon");
	}

	/** A model that isn't a hero's (see {@link #EXTRAS}). */
	public static @Nullable Loaded get(String name) {
		return EXTRA_MODELS.get(name);
	}

	public static void init() {
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Overcraft.id("hero_models"),
			new SimpleReloadListener<Map<String, Prepared>>() {
				@Override
				protected Map<String, Prepared> prepare(SharedState state) {
					return load(state.resourceManager());
				}

				@Override
				protected void apply(Map<String, Prepared> prepared, SharedState state) {
					register(prepared);
				}
			});
	}

	private record Prepared(SkinnedModel model, List<@Nullable NativeImage> images, Map<String, PreparedGlow> glow, Map<SkinnedModel.Piece, SkinnedModel.Piece> lit) {
	}

	/** Darker than this (of 255, any channel) a glow's image adds nothing a player sees. */
	private static final int GLOW_DIM = 10;

	/**
	 * Each glowing piece's triangles whose part of the glow image has any light, as a piece of their own: an emission map lives on a hero's main material, so
	 * drawing whole pieces skinned and sent 49-100% of the hero's triangles twice (critique_parts C1). A triangle is sampled at a grid of points, so a thin lit strip in a large one isn't lost.
	 */
	private static Map<SkinnedModel.Piece, SkinnedModel.Piece> litPieces(SkinnedModel model, Map<String, PreparedGlow> glow) {
		Map<SkinnedModel.Piece, SkinnedModel.Piece> out = new java.util.IdentityHashMap<>();
		for (SkinnedModel.Piece piece : model.pieces) {
			PreparedGlow g = glow.get(piece.material());
			if (g != null) {
				out.put(piece, litPart(piece, g));
			}
		}
		return out;
	}

	private static SkinnedModel.Piece litPart(SkinnedModel.Piece piece, PreparedGlow g) {
		int[] idx = piece.indices();
		float[] uv = piece.uv();
		int[] map = new int[piece.vertexCount()];
		java.util.Arrays.fill(map, -1);
		int[] kept = new int[idx.length];
		int keptCount = 0, verts = 0;
		for (int t = 0; t + 2 < idx.length; t += 3) {
			if (!triangleLit(uv, idx[t], idx[t + 1], idx[t + 2], g)) {
				continue;
			}
			for (int k = 0; k < 3; k++) {
				int v = idx[t + k];
				if (map[v] < 0) {
					map[v] = verts++;
				}
				kept[keptCount++] = map[v];
			}
		}
		float[] pos = new float[verts * 3], normal = new float[verts * 3], u = new float[verts * 2], weight = new float[verts * 4];
		int[] joint = new int[verts * 4];
		for (int v = 0; v < map.length; v++) {
			int n = map[v];
			if (n >= 0) {
				System.arraycopy(piece.pos(), v * 3, pos, n * 3, 3);
				System.arraycopy(piece.normal(), v * 3, normal, n * 3, 3);
				System.arraycopy(uv, v * 2, u, n * 2, 2);
				System.arraycopy(piece.joint4(), v * 4, joint, n * 4, 4);
				System.arraycopy(piece.weight4(), v * 4, weight, n * 4, 4);
			}
		}
		return new SkinnedModel.Piece(pos, normal, u, joint, weight, java.util.Arrays.copyOf(kept, keptCount), piece.image(), piece.mesh(), piece.material());
	}

	private static boolean triangleLit(float[] uv, int a, int b, int c, PreparedGlow g) {
		int w = g.image().getWidth(), h = g.image().getHeight();
		float ax = uv[a * 2] * w, ay = uv[a * 2 + 1] * h, bx = uv[b * 2] * w, by = uv[b * 2 + 1] * h, cx = uv[c * 2] * w, cy = uv[c * 2 + 1] * h;
		// about one sample per 4 texels of its longest side, 2 to 12 a side
		float longest = Math.max(Math.max(Math.abs(ax - bx) + Math.abs(ay - by), Math.abs(bx - cx) + Math.abs(by - cy)), Math.abs(cx - ax) + Math.abs(cy - ay));
		int n = Math.clamp((int) (longest / 4), 2, 12);
		for (int i = 0; i <= n; i++) {
			for (int j = 0; j <= n - i; j++) {
				float s = i / (float) n, r = j / (float) n, q = 1 - s - r;
				int x = Math.floorMod((int) Math.floor(ax * s + bx * r + cx * q), w), y = Math.floorMod((int) Math.floor(ay * s + by * r + cy * q), h);
				if (lit(g.image(), x, y) || g.alt() != null && lit(g.alt(), x, y)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean lit(NativeImage image, int x, int y) {
		int p = image.getPixel(Math.min(x, image.getWidth() - 1), Math.min(y, image.getHeight() - 1));
		return (p & 0xFF) > GLOW_DIM || (p >> 8 & 0xFF) > GLOW_DIM || (p >> 16 & 0xFF) > GLOW_DIM;
	}

	private record PreparedGlow(String texture, float strength, NativeImage image, @Nullable NativeImage alt, boolean glass) {
	}

	/** The model's glows, if the pack has them; a bad row is skipped, not the model. */
	private static Map<String, PreparedGlow> readGlow(ResourceManager resources, String name, Map<String, NativeImage> images) {
		Map<String, PreparedGlow> out = new HashMap<>();
		Identifier table = Identifier.tryBuild(Overcraft.MOD_ID, "hero_models/" + name + "_glow.json");
		Optional<Resource> res = table == null ? Optional.empty() : resources.getResource(table);
		if (res.isEmpty()) {
			return out;
		}
		try (java.io.Reader reader = res.get().openAsReader()) {
			com.google.gson.JsonObject rows = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
			for (String material : rows.keySet()) {
				com.google.gson.JsonObject row = rows.getAsJsonObject(material);
				String texture = row.get("texture").getAsString();
				Identifier id = Overcraft.id("hero_models/glow/" + texture + ".png");
				Optional<Resource> png = resources.getResource(id);
				Optional<Resource> alt = row.has("alt") ? resources.getResource(Overcraft.id("hero_models/glow/" + row.get("alt").getAsString() + ".png"))
					: Optional.empty();
				if (png.isPresent()) {
					// one image for all the models that share it (the body, its view model, its props)
					NativeImage image = images.get(texture);
					if (image == null) {
						try (InputStream in = png.get().open()) {
							image = NativeImage.read(in);
						}
						images.put(texture, image);
					}
					NativeImage altImage = null;
					if (alt.isPresent()) {
						String altName = row.get("alt").getAsString();
						altImage = images.get(altName);
						if (altImage == null) {
							try (InputStream a = alt.get().open()) {
								altImage = NativeImage.read(a);
							}
							images.put(altName, altImage);
						}
					}
					out.put(material, new PreparedGlow(texture, row.get("strength").getAsFloat(), image, altImage, row.has("glass")));
				}
			}
		} catch (Exception e) {
			Overcraft.LOG.warn("The {} model's glows didn't load: {}", name, e.toString());
		}
		return out;
	}

	/** Every model the pack has, by file name (hero ids and {@link #EXTRAS}). */
	private static Map<String, Prepared> load(ResourceManager resources) {
		Map<String, Prepared> out = new HashMap<>();
		Map<String, NativeImage> glowImages = new HashMap<>();
		List<String> names = new ArrayList<>(EXTRAS);
		// the emotes' props, by the ids the emote data names (prop_<model id>)
		names.addAll(EmoteData.load(resources));
		// the weapons' attached parts (weapon_parts.json)
		names.addAll(WeaponParts.load(resources));
		for (Hero hero : Hero.values()) {
			names.add(hero.id);
			names.add(hero.id + "_1p");
			names.add(hero.id + "_weapon");
			// clips exported apart from the model (owm_survey.py --clips-only: the Hello wave), merged in below
			names.add(hero.id + "_clips");
			names.add(hero.id + "_1p_clips");
			// the hero's Play of the Game highlight intro (tools/clipmaps/<hero>_intro_clips.txt, techniques/potg/potg_look.md)
			names.add(hero.id + "_intro_clips");
			if (hero == Hero.MERCY) {
				names.add("mercy_staff");
				names.add("mercy_blaster");
			}
			if (hero == Hero.ANA) {
				// her Sleep Dart pistol, out only during the dart's cast (HeroWeapon.sidearm)
				names.add("ana_pistol");
			}
			if (hero == Hero.DVA) {
				// her second body: the pilot (F2FF) and her first-person arms (F32B); her Light Gun is dva_weapon (10246)
				names.add("dva_pilot");
				names.add("dva_pilot_1p");
			}
			if (hero == Hero.SIGMA) {
				// his Experimental Barrier (C015), its emitter orbs (FC88) and the Accretion rock (C5CA): drawn from steps 5 / 7
				names.add("sigma_barrier");
				names.add("sigma_barrier_orb");
				names.add("sigma_rock");
			}
			if (hero == Hero.BRIGITTE) {
				// her flail's head and her shield (HeroWeapon.extras; the barrier itself later)
				names.add("brigitte_flail_head");
				names.add("brigitte_shield");
			}
		}
		for (String name : names) {
			// a name that isn't a valid resource path is skipped, not the whole reload (an upper-case prop id threw out
			// every resource pack)
			Identifier id = Identifier.tryBuild(Overcraft.MOD_ID, "hero_models/" + name + ".glb");
			Optional<Resource> res = id == null ? Optional.empty() : resources.getResource(id);
			if (id == null) {
				Overcraft.LOG.error("Not a valid model name: {}", name);
			}
			if (res.isEmpty()) {
				continue;
			}
			try (InputStream in = res.get().open()) {
				SkinnedModel model = SkinnedModel.load(in.readAllBytes());
				List<NativeImage> images = new ArrayList<>();
				for (byte[] png : model.images) {
					images.add(readImage(png));
				}
				// the PNG bytes, read: kept they'd hold the pack's 2 GB of images on the heap twice over during a reload (F3+T ran out of memory at 3 GB, played)
				model.images.clear();
				// her cloth's drivers, from the game data (ClothSim)
				model.clothDrivers = ClothSim.readDrivers(resources, name);
				colourHair(resources, model, images);
				Map<String, PreparedGlow> glow = readGlow(resources, name, glowImages);
				out.put(name, new Prepared(model, images, glow, litPieces(model, glow)));
				Overcraft.LOG.info("Hero model {}: {} nodes, {} joints, {} pieces, {} clips {}", name, model.nodeNames.length,
					model.joints.length, model.pieces.size(), model.clips.size(), model.clips.keySet());
			} catch (Exception e) {
				Overcraft.LOG.error("Couldn't load the {} model", name, e);
			}
		}
		// a pack from before these parts degrades quietly in play (critique_parts F): say so where it can be seen
		for (String part : WeaponParts.names()) {
			if (!out.containsKey(part)) {
				Overcraft.LOG.warn("Asset pack incomplete: no model {} (tools/build_asset_pack.py weapons)", part);
			}
		}
		for (Hero hero : Hero.values()) {
			mergeClips(out, hero.id + "_clips", hero.id);
			mergeClips(out, hero.id + "_1p_clips", hero.id + "_1p");
			mergeClips(out, hero.id + "_intro_clips", hero.id);
		}
		return out;
	}

	/**
	 * A clips-only file's clips (the rig, no meshes) added to its hero's model, each channel moved to the model's bone of
	 * the same name: the files are exported from the same game model, so the names match (a channel with none is dropped).
	 */
	private static void mergeClips(Map<String, Prepared> out, String from, String into) {
		Prepared clips = out.remove(from);
		Prepared model = out.get(into);
		if (clips == null || model == null) {
			return;
		}
		int added = 0;
		for (SkinnedModel.Clip clip : clips.model().clips.values()) {
			List<SkinnedModel.Channel> channels = new ArrayList<>();
			for (SkinnedModel.Channel c : clip.channels()) {
				int node = model.model().node(clips.model().nodeNames[c.node()]);
				if (node >= 0) {
					channels.add(new SkinnedModel.Channel(node, c.path(), c.times(), c.values(), c.step()));
				}
			}
			model.model().clips.putIfAbsent(clip.name(), new SkinnedModel.Clip(clip.name(), clip.duration(), channels));
			added++;
		}
		Overcraft.LOG.info("Hero model {}: {} clips added from {} {}", into, added, from, clips.model().clips.keySet());
	}

	private static @Nullable NativeImage readImage(byte[] png) {
		try {
			NativeImage image = NativeImage.read(png);
			while (image.getWidth() > MAX_TEXTURE || image.getHeight() > MAX_TEXTURE) {
				NativeImage half = new NativeImage(image.getWidth() / 2, image.getHeight() / 2, true);
				image.resizeSubRectTo(0, 0, image.getWidth(), image.getHeight(), half);
				image.close();
				image = half;
			}
			return image;
		} catch (Exception e) {
			Overcraft.LOG.warn("A hero texture didn't load: {}", e.toString());
			return null;
		}
	}

	/** The white of the eyes, under their iris images (ours: a warm off-white, darkened by the lighting). */
	private static final int EYE_WHITE = 0xE6DED6;

	/** Lays the image over this colour: opaque throughout, its own pixels kept by their alpha. */
	private static void underlay(NativeImage image, int rgb) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getPixel(x, y);
				float a = (argb >>> 24) / 255f;
				int r = Math.round(((argb >> 16) & 0xFF) * a + ((rgb >> 16) & 0xFF) * (1 - a));
				int g = Math.round(((argb >> 8) & 0xFF) * a + ((rgb >> 8) & 0xFF) * (1 - a));
				int b = Math.round((argb & 0xFF) * a + (rgb & 0xFF) * (1 - a));
				image.setPixel(x, y, 0xFF000000 | r << 16 | g << 8 | b);
			}
		}
	}

	/**
	 * Where the held talismans' paper is in Kiriko's weapon atlas (fractions of its size; read off the atlas, and nothing
	 * else of hers samples it): the two fronts (seal and kanji) above, their blank backs below; and a patch of the
	 * fronts' plain white above the seal.
	 */
	private static final float PAPER_U0 = 0.745f, PAPER_U1 = 0.997f;
	private static final float FRONT_V0 = 0.03f, FRONT_V1 = 0.425f, BACK_V0 = 0.47f, BACK_V1 = 0.875f;
	private static final float WHITE_U0 = 0.76f, WHITE_U1 = 0.84f, WHITE_V0 = 0.045f, WHITE_V1 = 0.085f;

	/** The talisman paper: a piece nearly all of whose triangles sample the paper's part of the atlas. */
	private static boolean isPaper(SkinnedModel.Piece piece) {
		int in = 0;
		int tris = piece.indices().length / 3;
		for (int t = 0; t < tris; t++) {
			float u = 0;
			float v = 0;
			for (int k = 0; k < 3; k++) {
				int i = piece.indices()[t * 3 + k];
				u += piece.uv()[i * 2] / 3;
				v += piece.uv()[i * 2 + 1] / 3;
			}
			if (u >= 0.74f && u <= 1 && v >= FRONT_V0 && v <= BACK_V1) {
				in++;
			}
		}
		return tris > 0 && in > tris * 0.9f;
	}

	/**
	 * The talismans' paper white all over (the owner, 2026-10-27: "the back should be the same color as the white base
	 * of the ofuda texture"). Blizzard baked a shade into both sides, the white fading to #756664 toward where the
	 * fingers hold them, and under Minecraft's light the paper read as a grey slab (the seventh critique, #6). Each row's
	 * paper colour (its brightest share, the ink and the seal being the rest) is brought up to the plain white above the
	 * seal, channel by channel: the ink, the seal and the paper's grain stay, the shade goes.
	 */
	private static void whitenPaper(NativeImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		int[] white = paperColour(image, Math.round(w * WHITE_U0), Math.round(w * WHITE_U1), Math.round(h * WHITE_V0),
			Math.round(h * WHITE_V1), 0.5f);
		int x0 = Math.round(w * PAPER_U0);
		int x1 = Math.round(w * PAPER_U1);
		for (float[] band : new float[][] {{FRONT_V0, FRONT_V1}, {BACK_V0, BACK_V1}}) {
			for (int y = Math.round(h * band[0]); y < Math.round(h * band[1]); y++) {
				int[] paper = paperColour(image, x0, x1, y, y + 1, 0.85f);
				for (int x = x0; x < x1; x++) {
					int argb = image.getPixel(x, y);
					int out = argb & 0xFF000000;
					for (int c = 0; c < 3; c++) {
						int shift = 16 - c * 8;
						int v = (argb >> shift) & 0xFF;
						out |= Math.min(255, Math.round(v * (float) white[c] / Math.max(1, paper[c]))) << shift;
					}
					image.setPixel(x, y, out);
				}
			}
		}
	}

	/** Each channel's value at this share of the way up its sorted values, over a rectangle of the image. */
	private static int[] paperColour(NativeImage image, int x0, int x1, int y0, int y1, float share) {
		int n = Math.max(1, (x1 - x0) * (y1 - y0));
		int[][] values = new int[3][n];
		int i = 0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				int argb = image.getPixel(x, y);
				values[0][i] = (argb >> 16) & 0xFF;
				values[1][i] = (argb >> 8) & 0xFF;
				values[2][i] = argb & 0xFF;
				i++;
			}
		}
		int[] out = new int[3];
		for (int c = 0; c < 3; c++) {
			java.util.Arrays.sort(values[c], 0, i);
			out[c] = values[c][Math.min(i - 1, Math.max(0, Math.round((i - 1) * share)))];
		}
		return out;
	}

	/** The hair cards a pack is meant to hold colours for (the other black images are eye shadows and such, not drawn on purpose). */
	private static final java.util.Set<String> HAIR_CARDS = java.util.Set.of("000000008052");

	/**
	 * Hair cards exported black (Lúcio's dreads, material 8052: his hair shader colours them and glTF kept only the black image) take their
	 * real colours from hero_models/colour/<material>.png (tools/glow_maps.py, #105). Without it they stay black, and aren't drawn.
	 */
	private static void colourHair(ResourceManager resources, SkinnedModel model, List<@Nullable NativeImage> images) {
		for (SkinnedModel.Piece piece : model.pieces) {
			NativeImage image = piece.image() >= 0 && piece.image() < images.size() ? images.get(piece.image()) : null;
			if (image == null || piece.material().isEmpty() || !isBlack(image)) {
				continue;
			}
			Identifier id = Identifier.tryBuild(Overcraft.MOD_ID, "hero_models/colour/" + piece.material().toLowerCase(java.util.Locale.ROOT) + ".png");
			var found = id == null ? java.util.Optional.<net.minecraft.server.packs.resources.Resource>empty() : resources.getResource(id);
			if (found.isEmpty() && HAIR_CARDS.contains(piece.material())) {
				Overcraft.LOG.warn("The asset pack has no hair colours for {}: his dreads won't be drawn (py tools/build_asset_pack.py glows, then zip)", piece.material());
			}
			if (found.isPresent()) {
				try (InputStream in = found.get().open()) {
					NativeImage coloured = readImage(in.readAllBytes());
					if (coloured != null) {
						image.close();
						images.set(piece.image(), coloured);
					}
				} catch (Exception e) {
					Overcraft.LOG.error("Couldn't read hair colours {}", id, e);
				}
			}
		}
	}

	/** Every pixel black and opaque. */
	private static boolean isBlack(NativeImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (image.getPixel(x, y) != 0xFF000000) {
					return false;
				}
			}
		}
		return true;
	}

	/** A small image of one colour throughout. */
	private static boolean isFlat(NativeImage image) {
		if (image.getWidth() * image.getHeight() > 64 * 64) {
			return false;
		}
		int first = image.getPixel(0, 0);
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (image.getPixel(x, y) != first) {
					return false;
				}
			}
		}
		return true;
	}

	/** One texture per glow image, whichever models use it. */
	private static Identifier glowTexture(TextureManager textures, String name, NativeImage image) {
		Identifier id = Overcraft.id("hero_models/glow_image/" + name);
		if (!REGISTERED.contains(id)) {
			textures.register(id, new HeroTexture(id, image));
			REGISTERED.add(id);
		}
		return id;
	}

	private static void register(Map<String, Prepared> prepared) {
		HeroArt.clear();
		AimSheet.clear();
		dev.overcraft.client.assets.AssetPackVersion.clear();
		dev.overcraft.client.hud.HudShapes.clear();
		dev.overcraft.client.effect.EffectArt.clear();
		TextureManager textures = Minecraft.getInstance().getTextureManager();
		REGISTERED.forEach(textures::release);
		REGISTERED.clear();
		MODELS.clear();
		EXTRA_MODELS.clear();
		prepared.forEach((name, p) -> {
			Identifier[] ids = new Identifier[p.images().size()];
			boolean[] flat = new boolean[ids.length];
			// the eyeballs' images: only the iris is opaque in them, Overwatch's eye shader draws the white around it.
			// Drawn as they are, the eyes were see-through round the iris (a pale ring of sky, #19): the white goes in
			// under the iris here
			java.util.Set<Integer> paperDone = new java.util.HashSet<>();
			for (SkinnedModel.Piece piece : p.model().pieces) {
				if (piece.image() >= 0 && piece.image() < ids.length && p.images().get(piece.image()) != null
					&& p.model().mostlyOn(piece, "Eyeball")) {
					underlay(p.images().get(piece.image()), EYE_WHITE);
				}
				if (piece.image() >= 0 && piece.image() < ids.length && p.images().get(piece.image()) != null
					&& isPaper(piece) && paperDone.add(piece.image())) {
					whitenPaper(p.images().get(piece.image()));
				}
			}
			for (int i = 0; i < ids.length; i++) {
				NativeImage image = p.images().get(i);
				if (image == null || isBlack(image)) {
					// a pure black, opaque image is a layer Overwatch's shader colours: Lúcio's dreads' hair cards (material
					// 8052, a grey strand gradient its hair shader tints, exported black) stood off his head as a black
					// crown. Not drawn, as Kiriko's eye shadows aren't
					continue;
				}
				flat[i] = isFlat(image);
				if (name.equals("projectile_ofuda") && i == 0) {
					dev.overcraft.client.effect.EffectArt.keepTalisman(image);
				}
				Identifier id = Overcraft.id("hero_models/" + name + "/" + i);
				textures.register(id, new HeroTexture(id, image));
				REGISTERED.add(id);
				ids[i] = id;
			}
			Map<String, Glow> glow = new HashMap<>();
			p.glow().forEach((material, g) -> {
				Identifier id = glowTexture(textures, g.texture(), g.image());
				Identifier alt = g.alt() == null ? null : glowTexture(textures, g.texture() + "_alt", g.alt());
				glow.put(material, new Glow(id, g.strength(), alt, g.glass()));
			});
			Loaded loaded = new Loaded(p.model(), ids, flat, glow, p.lit());
			Hero hero = Hero.byId(name);
			if (hero != null) {
				MODELS.put(hero, loaded);
			} else {
				EXTRA_MODELS.put(name, loaded);
			}
		});
	}
}
