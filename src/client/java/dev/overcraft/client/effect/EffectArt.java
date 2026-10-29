package dev.overcraft.client.effect;

import com.mojang.blaze3d.platform.NativeImage;
import dev.overcraft.Overcraft;
import dev.overcraft.client.model.HeroTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The effects' textures: Overwatch's grey-scale masks from the local asset pack (textures/effect/, from
 * build_asset_pack.py effects), coloured here the way Overwatch colours them, through a gradient map from the mask's
 * brightness to a colour (tools/reference/kiriko-effects.md, "Colours": the ramps' linear values). Coloured for
 * additive drawing: the colour is the light added, so black adds nothing. Null without the pack: no effect is drawn.
 */
public final class EffectArt {
	/**
	 * Gradient maps: brightness stops and their linear colours, from the game's ramps. Overwatch renders in linear
	 * light; these become sRGB here. Values over 1 in the game glow (bloom); here the top of each map runs to a paler,
	 * whiter colour instead.
	 */
	public enum Palette {
		/**
		 * Kiriko's teal: most of her effects, as she and her allies see them. The game's ramp from 0.82 up; its faint end
		 * (0.25, 0.5: (0, .25, .22), (0, .5, .44)) moved toward blue: added over Minecraft's green grass it summed to
		 * mint (the fifth critique measured hues of 135-162 degrees against Kiriko's 176). Ours.
		 */
		TEAL(0, 0, 0, 0, .25f, 0, .15f, .28f, .5f, 0, .38f, .58f, .82f, 0, 1, .87f, 1, .55f, 1, .97f),
		/** The homing ofuda's gold. */
		GOLD(0, 0, 0, 0, .25f, .36f, .24f, 0, .5f, .71f, .49f, 0, .82f, 1, .82f, .3f, 1, 1, .97f, .75f),
		/**
		 * The kunai's and the Kitsune's cyan, white-hot at the top. Its faint end moved toward blue, as TEAL's (game:
		 * (0, .51, .45) at 0.3, (0, 1, .87) at 0.65).
		 */
		CYAN(0, 0, 0, 0, .3f, 0, .42f, .55f, .65f, 0, .95f, 1, 1, 1, 1, 1),
		/** Suzu's burst ring: (0.1, 2, 3) in the game. */
		SUZU_BLUE(0, 0, 0, 0, .5f, .017f, .33f, .5f, .85f, .033f, .67f, 1, 1, .5f, .9f, 1),
		/**
		 * Lúcio's Healing Boost, amber (colour strip 3F95, tools/reference/lucio-effects.md): #B37401 #FFCD01 #FBF701
		 * #FAFF62 as linear light.
		 */
		HEAL_AMBER(0, 0, 0, 0, .25f, .451f, .175f, 0, .5f, 1, .61f, 0, .82f, .965f, .93f, 0, 1, .956f, 1, .122f),
		/** Lúcio's Speed Boost, green to cyan (strip 3F94): #01A250 #01E767 #0BFF90 #52D8EA. */
		SPEED_GREEN(0, 0, 0, 0, .25f, 0, .361f, .08f, .5f, 0, .799f, .136f, .82f, .003f, 1, .279f, 1, .084f, .687f, .823f),
		/** Lúcio's team green (strips 35BC / 410E: his rounds, Soundwave, the barrier): #5FD900 #7BEB00 #A2FE00 #F2FE5D white. */
		LUCIO_GREEN(0, 0, 0, 0, .25f, .114f, .694f, 0, .45f, .198f, .831f, 0, .65f, .361f, .991f, 0, .85f, .888f, .991f, .109f,
			1, 1, 1, 1),
		/** Lúcio's round's glow (A3F's core): #9EB243 to #E2FF64, no white top (his team green's white stacked to a white ball). */
		LUCIO_ROUND(0, 0, 0, 0, .5f, .342f, .445f, .056f, 1, .761f, 1, .127f),
		// Jetpack Cat's (tools/reference/jetpack_cat-effects.md, from her LUT rows: research\jetpack_cat\work\fx_palettes.txt)
		/** Jetpack Cat's shot core glow (707A / 7079, LUT 800DA2FE r1): gold to white. */
		CAT_CORE(0, 0, 0, 0, .25f, 1, .707f, 0, .5f, 1, .887f, 0, .75f, 1, 1, .37f, 1, 1, 1, 1),
		/** Her paw shot's tube trail (80067279 r5, HDR 4.7): gold to white-hot. */
		CAT_TRACER(0, 0, 0, 0, .25f, 1, .716f, .088f, .5f, 1, 1, .387f, .75f, 1, 1, .902f, 1, 1, 1, 1),
		/** Her shots' outer glow (8006FF69 r1): blue to cyan. */
		CAT_HALO(0, 0, 0, 0, .25f, 0, .221f, 1, .5f, 0, .526f, 1, .75f, 0, .852f, 1, 1, 0, 1, 1),
		/** Her shots' sparks (800D7AF2 r1): cyan-blue to white to yellow. */
		CAT_SPARK(0, 0, 0, 0, .25f, 0, .788f, 1, .5f, .389f, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 0),
		/** Her shots' flat trail (8004C26D r8): blue to aqua. */
		CAT_RIBBON(0, 0, 0, 0, .25f, 0, .19f, 1, .5f, 0, .609f, .928f, .75f, 0, 1, .805f, 1, 0, 1, .55f),
		/** Centre-shot hit flash, gold (709F, 80031F33 r1, from black). */
		CAT_STAMP_GOLD(0, 0, 0, 0, .25f, .491f, .265f, 0, .5f, .983f, .53f, 0, .75f, 1, 1, .106f, 1, 1, 1, .352f),
		/** Centre-shot hit flash, blue (709F, 8000708B r1, from black). */
		CAT_STAMP_BLUE(0, 0, 0, 0, .25f, 0, .001f, .491f, .5f, 0, .001f, .983f, .75f, 0, .244f, 1, 1, 0, .492f, 1),
		/** The paw print (709F, 8005126D r4, HDR 3.5): gold to white. */
		CAT_PAW_PRINT(0, 0, 0, 0, .25f, 1, .735f, .109f, .5f, 1, 1, .385f, .75f, 1, 1, .82f, 1, 1, 1, 1),
		/** Enemy-hit flash (7077, 8001A030 r5, HDR 6.4): blue to cyan to white. */
		CAT_FLASH_BLUE(0, 0, 0, 0, .25f, 0, 1, 1, .5f, 0, 1, 1, .75f, 0, 1, 1, 1, 1, 1, 1),
		/** Heal sparks (7075 / Purr 741D, 800BB7A6 r4, HDR 5): gold to white. */
		CAT_HEAL_GOLD(0, 0, 0, 0, .25f, 1, 1, .371f, .5f, 1, 1, .709f, .75f, 1, 1, 1, 1, 1, 1, 1),
		/** Enemy view of her gold parts (800DA2FE r2): red to orange to white. */
		CAT_ENEMY_HOT(0, 0, 0, 0, .25f, 1, .09f, 0, .5f, 1, .208f, 0, .75f, 1, .585f, .37f, 1, 1, 1, 1),
		/** Enemy view of her blue parts (8006FF69 r2): red to orange. */
		CAT_ENEMY_ORANGE(0, 0, 0, 0, .25f, 1, .086f, 0, .5f, 1, .219f, 0, .75f, 1, .363f, 0, 1, 1, .517f, 0),
		/** Purr's 10 m sphere (70ED, 800E8940 r6, HDR 6.9): blue to cyan to white. */
		CAT_PURR_BLUE(0, 0, 0, 0, .25f, 0, 1, 1, .5f, 0, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 1),
		/** Purr's burst lines and nozzle glows (70ED, 800EB0AE r1, from black): gold to white. */
		CAT_PURR_GOLD(0, 0, 0, 0, .25f, .667f, .523f, 0, .5f, 1, .835f, .091f, .75f, 1, 1, .933f, 1, 1, 1, 1),
		/** Purr's 6.4 m pulse ring (70F3, 8001990A r8): blue to cyan. */
		CAT_PURR_RING(0, 0, 0, 0, .25f, 0, .528f, .974f, .5f, 0, .772f, 1, .75f, 0, 1, 1, 1, 0, 1, 1),
		// Mercy's (tools/reference/mercy-effects.md, the game's LUT rows)
		/** Mercy's heal beam 186F C14 (0.4 m ribbon; 8003A285 r3, HDR 10): gold to pale yellow. */
		MERCY_HEAL_BEAM(0, 0, 0, 0, .25f, 1, 1, .068f, .5f, 1, 1, .139f, .75f, 1, 1, 1, 1, 1, 1, 1),
		/** The same normalised by its peak: keeps the gold (use this for the beam). */
		MERCY_HEAL_BEAM_N(0, 0, 0, 0, .25f, .192f, .148f, .007f, .5f, .32f, .263f, .014f, .75f, .611f, .554f, .209f, 1, 1, .913f, .361f),
		/** Heal beam 186F C10/C12/C16 (0.2 / 0.1 / 0.2 m; 8006E3CC r3, HDR 5). */
		MERCY_HEAL_BEAM_SOFT(0, 0, 0, 0, .25f, .984f, .695f, .028f, .5f, 1, 1, .08f, .75f, 1, 1, .981f, 1, 1, 1, 1),
		MERCY_HEAL_BEAM_SOFT_N(0, 0, 0, 0, .25f, .197f, .139f, .006f, .5f, .358f, .295f, .016f, .75f, .576f, .523f, .196f, 1, 1, .913f, .361f),
		/** Boost beam 1870 C8/C12 (0.15 m; 8000D0E4 r3, HDR 10): blue to sky. */
		MERCY_BOOST_BEAM(0, 0, 0, 0, .25f, .266f, 1, 1, .5f, .476f, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 1),
		MERCY_BOOST_BEAM_N(0, 0, 0, 0, .25f, .027f, .16f, 1, .5f, .048f, .301f, 1, .75f, .192f, .445f, 1, 1, .342f, .59f, 1),
		/** Boost beam 1870 C10 (0.5 m glow; 8000F404 r3, HDR 15): deep blue to sky. */
		MERCY_BOOST_BEAM_GLOW(0, 0, 0, 0, .25f, .193f, 1, 1, .5f, .382f, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 1),
		MERCY_BOOST_BEAM_GLOW_N(0, 0, 0, 0, .25f, .013f, .081f, .301f, .5f, .025f, .161f, .535f, .75f, .181f, .373f, .766f, 1, .342f, .59f, 1),
		/** The staff head while boosting (0197 C18, 800FDFB8 r7, HDR 15): blue to pale cyan. */
		MERCY_STAFF_BLUE(0, 0, 0, 0, .25f, .125f, .752f, 1, .5f, .245f, 1, 1, .75f, .509f, 1, 1, 1, 1, 1, 1),
		MERCY_STAFF_BLUE_N(0, 0, 0, 0, .25f, .008f, .05f, .301f, .5f, .016f, .1f, .535f, .75f, .034f, .214f, .766f, 1, .342f, .59f, 1),
		/** The boosted ally's flash (0100 C8, 80080D4D r1, HDR 3): deep blue to sky. */
		MERCY_BOOSTED(0, 0, 0, 0, .25f, .037f, .222f, 1, .5f, .096f, .608f, 1, .75f, .517f, 1, 1, 1, 1, 1, 1),
		/** Her blaster round (01E1 C21, 80023619 r5): orange to pale yellow. */
		MERCY_SHOT(0, 0, 0, 0, .25f, 1, .695f, 0, .5f, 1, .866f, .032f, .75f, 1, .912f, .207f, 1, 1, .982f, .533f),
		/** Her round as enemies see it (01E1 C27, 800DCC79 r5): dark red to orange. */
		MERCY_SHOT_ENEMY(0, 0, 0, 0, .25f, .469f, 0, 0, .5f, .766f, 0, 0, .75f, 1, .055f, .012f, 1, 1, .258f, .054f),
		/** Guardian Angel's trail (colour strip 20D0 times the trail's tint (3.26, 3.26, 1.02), clamped). */
		MERCY_GA_TRAIL_T(0, .236f, .032f, .01f, .25f, .855f, .069f, .001f, .5f, 1, .009f, 0, .75f, 1, .868f, 0, 1, 1, 1, 1),
		/** Wing glows (GA 17E9 / Angelic Descent 019C, 800B92F6 r4): yellow to pale yellow. */
		MERCY_WING_GLOW(0, 0, 0, 0, .25f, 1, 1, .092f, .5f, 1, 1, .174f, .75f, 1, 1, .255f, 1, 1, 1, .337f),
		/** Wing motes at rest (0301, 80074C3E r5, HDR 3): black to gold to white. */
		MERCY_WING_GOLD(0, 0, 0, 0, .25f, .269f, .163f, .001f, .5f, .551f, .338f, .007f, .75f, 1, 1, .397f, 1, 1, 1, 1),
		MERCY_WING_GOLD_N(0, 0, 0, 0, .25f, .09f, .054f, 0, .5f, .184f, .113f, .002f, .75f, .517f, .417f, .132f, 1, 1, .913f, .392f),
		/** Valkyrie's feathers (40CD C19, 8009E3E3 r8, x1.6 in game): gold to pale yellow. */
		MERCY_VALK(0, 0, 0, 0, .25f, 1, .781f, .019f, .5f, 1, .911f, .038f, .75f, 1, 1, .143f, 1, 1, 1, .451f),
		/** Valkyrie's feathers as enemies see them (40CD C23, 800FA2A3 r8, x2.5): dark red to orange. */
		MERCY_VALK_ENEMY(0, 0, 0, 0, .25f, .405f, 0, 0, .5f, .552f, 0, 0, .75f, .794f, .124f, 0, 1, 1, .533f, .013f),
		/** Valkyrie's sparks (1F7B C26, 80060C6F r5): gold to pale yellow. */
		MERCY_VALK_SPARK(0, 0, 0, 0, .25f, 1, .724f, .015f, .5f, 1, .799f, .148f, .75f, 1, .873f, .279f, 1, 1, .947f, .412f),
		/** Valkyrie's sparks, enemy row (80060C6F r6): red to orange. */
		MERCY_VALK_SPARK_ENEMY(0, 0, 0, 0, .25f, 1, .076f, 0, .5f, 1, .152f, 0, .75f, 1, .227f, 0, 1, 1, .624f, .08f),
		/** Resurrect's burst sparks (632D C24, 800948DD r1, x5.4): orange to white. */
		MERCY_RES_SPARK(0, 0, 0, 0, .25f, 1, .495f, 0, .5f, 1, .72f, 0, .75f, 1, .829f, .106f, 1, 1, 1, 1),
		/** Resurrect's glow on the dead ally (2106 C29, 800B7236 r0, x4): black to yellow to white. */
		MERCY_RES_HALO(0, 0, 0, 0, .25f, .485f, .448f, 0, .5f, .971f, .895f, 0, .75f, 1, .959f, .403f, 1, 1, 1, .838f),
		/** The same as enemies see it (2106 C35, 80052D9B r0): black to red to peach. */
		MERCY_RES_HALO_ENEMY(0, 0, 0, 0, .25f, .485f, 0, 0, .5f, .971f, 0, 0, .75f, 1, .262f, .09f, 1, 1, .545f, .188f),
		/** The revived ally's rising crosses (019E C7, 800CBECB r2, HDR 5): orange to yellow to white. */
		MERCY_PLUS(0, 0, 0, 0, .25f, 1, .689f, .101f, .5f, 1, .767f, .136f, .75f, 1, .843f, .17f, 1, 1, 1, 1),
		/** Flash Heal's beam (75A7 C4, 8008E467 r4, HDR 30), normalised: brown-gold to pale yellow. */
		MERCY_FLASH_N(0, .021f, .011f, 0, .25f, .122f, .091f, .005f, .5f, .222f, .171f, .01f, .75f, .504f, .472f, .264f, 1, 1, .947f, .552f),
		/** Lifeline's rope core (72FA, 800367D1 r2, HDR 16): blue to cyan. */
		CAT_TETHER(0, 0, 0, 0, .25f, 0, 1, 1, .5f, 0, 1, 1, .75f, 0, 1, 1, 1, 0, 1, 1),
		/** Lifeline's rope as enemies see it (800367D1 r3): red to yellow. */
		CAT_TETHER_ENEMY(0, 0, 0, 0, .25f, 1, .131f, 0, .5f, 1, .263f, 0, .75f, 1, .629f, 0, 1, 1, 1, 0),
		/** Lifeline's hook glow (6EA2 / 71B1 cone, r3): #7FA8FF to white. */
		CAT_HOOK(0, 0, 0, 0, .25f, .25f, .33f, .5f, .5f, .5f, .66f, 1, .75f, .75f, .83f, 1, 1, 1, 1, 1),
		/** Lifeline's paw marker decal (7432, 800333C5 r3): blue to cyan. */
		CAT_MARKER(0, 0, 0, 0, .25f, .001f, .175f, .976f, .5f, 0, .404f, .975f, .75f, 0, .671f, .992f, 1, .072f, 1, .991f),
		/** Jetpack flame core (7064, 8004B7CE r7, HDR 11): blue to cyan to white. */
		CAT_FLAME(0, 0, 0, 0, .25f, .731f, 1, 1, .5f, 1, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 1),
		/** Jetpack outer flame (7064, 800DCB53 r7, from black): gold to pale yellow. */
		CAT_FLAME_GOLD(0, 0, 0, 0, .25f, 0, 0, 0, .5f, .433f, .256f, 0, .75f, .967f, .571f, 0, 1, .947f, .863f, .13f),
		/** Frenetic Flight's ribbons (7067, 80048E45 r3): cyan behind a white head (the head is the row's x = 0). */
		CAT_BOOST(0, 0, 0, 0, .25f, 0, .929f, 1, .5f, 0, .929f, 1, .75f, 0, .929f, 1, 1, 0, .929f, 1),
		/** Frenetic Flight's ribbons as enemies see them (80048E45 r4): orange. */
		CAT_BOOST_ENEMY(0, 0, 0, 0, .25f, 1, .347f, 0, .5f, 1, .347f, 0, .75f, 1, .347f, 0, 1, 1, .347f, 0),
		/** Catnapper's target rings (71B3, 80044C8A r4): cyan to blue. */
		CAT_NAP_MARKER(0, 0, 0, 0, .25f, 0, .393f, 1, .5f, 0, .067f, 1, .75f, 0, .067f, 1, 1, 0, .067f, 1),
		/** Catnapper's landing flash (71C2, 8006A53D r0, from black, HDR 7): violet-pink to sky to cyan. */
		CAT_NAP_IMPACT(0, 0, 0, 0, .25f, .307f, .109f, .744f, .5f, .237f, .218f, 1, .75f, .13f, .614f, 1, 1, .009f, 1, 1),
		// Lifeweaver's (tools/reference/lifeweaver-effects.md "Palettes", generated from his LUT rows)
		/** His hex sparkles 3EB7D (8005B0D22D52 r0, HDR 5): magenta-pink to white. */
		LW_PINK(0, 0, 0, 0, .25f, 1, .343f, 1, .5f, 1, .83f, 1, .75f, 1, 1, 1, 1, 1, 1, 1),
		LW_PINK_N(0, 0, 0, 0, .25f, .345f, .069f, .225f, .5f, .443f, .166f, .33f, .75f, .575f, .286f, .466f, 1, 1, 1, 1),
		/** The same, as enemies see them (r1): black to red to white. */
		LW_PINK_ENEMY_N(0, 0, 0, 0, .25f, .158f, .028f, .028f, .5f, .316f, .056f, .056f, .75f, .474f, .089f, .089f, 1, 1, .999f, .999f),
		/** Gold hex sparkles (800CF57A9184 r0, HDR 5): the blossom's side. */
		LW_GOLD(0, 0, 0, 0, .25f, 1, .951f, .125f, .5f, 1, 1, .321f, .75f, 1, 1, .567f, 1, 1, 1, 1),
		LW_GOLD_N(0, 0, 0, 0, .25f, .347f, .19f, .025f, .5f, .466f, .34f, .064f, .75f, .605f, .508f, .113f, 1, 1, 1, 1),
		/** The charging blossom's glows 0242 (30DE C33 / C37): pale gold, amber. */
		LW_BLOSSOM_GLOW(0, 0, 0, 0, .25f, 1, .755f, .242f, .5f, 1, .831f, .384f, .75f, 1, .906f, .523f, 1, 1, .982f, .665f),
		LW_BLOSSOM_AMBER(0, 0, 0, 0, .25f, .919f, .497f, .068f, .5f, .935f, .642f, .136f, .75f, .949f, .785f, .203f, 1, .964f, .93f, .27f),
		/** Petals 3FD63 (8007F2189D11 r3, HDR 4; the row falls: bright at mask 0). */
		LW_PETAL_N(0, 0, 0, 0, .25f, .831f, .588f, .253f, .5f, .661f, .469f, .202f, .75f, .537f, .38f, .164f, 1, .5f, .354f, .152f),
		/** The petals' over-life tint (800A664BB547 r0, 12BB): cream to pink to crimson; use by age, not by mask. */
		LW_PETAL_AGE_N(0, 0, 0, 0, .25f, .778f, .464f, .572f, .5f, .642f, .189f, .572f, .75f, .581f, .045f, .278f, 1, .572f, 0, .021f),
		/** Life Grip's vine 3110 (80072BB6FABB r3, HDR 20): lilac-pink to white. */
		LW_VINE_N(0, 0, 0, 0, .25f, .189f, .096f, .152f, .5f, .201f, .097f, .11f, .75f, .418f, .315f, .298f, 1, 1, 1, 1),
		/** The vine's coil 12262 (800B8E14B83B r3, HDR 5): magenta to white. */
		LW_VINE_COIL_N(0, 0, 0, 0, .25f, .347f, .067f, .33f, .5f, .545f, .134f, .511f, .75f, .787f, .271f, .721f, 1, 1, .999f, .999f),
		/** The blossom's flight trail 30D0 C25 (80098E9C7FFE r3, HDR 10): black to gold to white. */
		LW_BLOSSOM_TRAIL(0, 0, 0, 0, .25f, .898f, .585f, 0, .5f, 1, 1, .132f, .75f, 1, 1, .876f, 1, 1, 1, 1),
		LW_BLOSSOM_TRAIL_N(0, 0, 0, 0, .25f, .09f, .059f, 0, .5f, .168f, .126f, .013f, .75f, .337f, .327f, .088f, 1, 1, 1, 1),
		/** Gold ring flashes 2E8AF (800C725A38EE r0, HDR 2): heal, fire, tree heal, seed pickup. */
		LW_HEAL_RING_N(0, 0, 0, 0, .25f, .835f, .459f, .06f, .5f, .918f, .651f, .12f, .75f, 1, .84f, .184f, 1, 1, 1, 1),
		/** The tree's sprout / pulse sphere shells (800EF2EBAB76 r9, HDR 9.7): black to gold to pale yellow. */
		LW_TREE_WAVE_N(0, .003f, 0, .006f, .25f, .036f, .028f, .001f, .5f, .343f, .271f, .002f, .75f, .735f, .613f, .051f, 1, 1, .946f, .429f),
		LW_TREE_WAVE_ENEMY_N(0, .003f, 0, .006f, .25f, .036f, .002f, .001f, .5f, .343f, .023f, .002f, .75f, .735f, .139f, .051f, 1, 1, .728f, .429f),
		/** The pulse's ground ring (8004D4075AFE r3, HDR 2) and the flower-band wall (8001794386A1 r3, HDR 2). */
		LW_TREE_RING_N(0, 0, 0, 0, .25f, .452f, .398f, .079f, .5f, .5f, .459f, .217f, .75f, .56f, .537f, .401f, 1, 1, 1, 1),
		LW_TREE_WALL_N(0, 0, 0, 0, .25f, .167f, .159f, .046f, .5f, .297f, .285f, .092f, .75f, .464f, .447f, .149f, 1, 1, 1, 1),
		/** The thorn 122CA (8001A48833A6 r2, HDR 30): purple-magenta to white. */
		LW_THORN_N(0, 0, 0, 0, .25f, .115f, .01f, .122f, .5f, .185f, .058f, .192f, .75f, .469f, .264f, .478f, 1, 1, 1, 1),
		LW_THORN_ENEMY_N(0, 0, 0, 0, .25f, .1f, 0, 0, .5f, .128f, 0, 0, .75f, .394f, 0, 0, 1, 1, .999f, .999f),
		/** The thorn's tracer (8003062BE28B r3, HDR 12; falls: white at 0 to black at 1, reversed by age, M). */
		LW_THORN_TRAIL_N(0, 0, 0, 0, .25f, .749f, .158f, .643f, .5f, .418f, .056f, .368f, .75f, .162f, 0, .149f, 1, 0, 0, 0),
		/** The thorn's muzzle flash (80082222A07E r0) and hit burst (8009F205934A r0). */
		LW_THORN_FLASH(0, 0, 0, 0, .25f, 1, .058f, .588f, .5f, 1, .191f, .712f, .75f, .981f, .381f, .826f, 1, .939f, .61f, 1),
		LW_THORN_HIT(0, 0, 0, 0, .25f, .273f, .067f, .362f, .5f, .567f, .238f, .237f, .75f, .862f, .617f, .115f, 1, .991f, 1, 0),
		/** The platform's throw and rise trails (800EA70DA27E r2, HDR 5): pink to white; enemy r3. */
		LW_PLATFORM_TRAIL_N(0, 0, 0, 0, .25f, .391f, .086f, .347f, .5f, .398f, .087f, .353f, .75f, .469f, .103f, .416f, 1, 1, .687f, .956f),
		LW_PLATFORM_TRAIL_ENEMY_N(0, 0, 0, 0, .25f, .391f, .012f, .012f, .5f, .398f, .013f, .013f, .75f, .469f, .015f, .015f, 1, 1, .687f, .687f),
		/** The platform's bloom outline 122DA (8008CBB37314 r2, HDR 5): mauve to white. */
		LW_PLATFORM_FLOWER_N(0, 0, 0, 0, .25f, .067f, .032f, .055f, .5f, .16f, .076f, .133f, .75f, .387f, .183f, .322f, 1, 1, 1, 1),
		/** The raised platform's petal-tip glow (8009496E3E46 r0) and cup (8003C2A25A98 r5; enemy r6). */
		LW_PETAL_GLOW(0, 0, 0, 0, .25f, .828f, .127f, .676f, .5f, .824f, .221f, .707f, .75f, .823f, .318f, .741f, 1, .831f, .405f, .778f),
		LW_PETAL_CUP(0, 0, 0, 0, .25f, .684f, .167f, .588f, .5f, .77f, .334f, .709f, .75f, .856f, .498f, .828f, 1, 1, 1, 1),
		LW_PETAL_CUP_ENEMY(0, 0, 0, 0, .25f, .684f, .167f, .167f, .5f, .77f, .334f, .334f, .75f, .856f, .498f, .498f, 1, 1, 1, 1),
		/** The dash ribbons (800955CE7D8A r2, HDR 3, falls): white to cream gold (L which row). */
		LW_DASH_N(0, 0, 0, 0, .25f, .888f, .572f, .312f, .5f, .79f, .509f, .278f, .75f, .704f, .454f, .248f, 1, .667f, .43f, .234f),
		/** As the enemy sees her effects. */
		ENEMY(0, 0, 0, 0, .25f, .25f, .05f, .05f, .5f, .5f, .1f, .1f, .85f, 1, .21f, .21f, 1, 1, .6f, .6f),
		/** The mask as it is. */
		WHITE(0, 0, 0, 0, 1, 1, 1, 1),
		/** A colour texture used as it is (with its alpha): drawn blended, not added. */
		NATURAL;

		private final float[] stops;

		Palette(float... stops) {
			this.stops = stops;
		}

		/** The colour for a mask brightness, linear. */
		void colour(float v, float[] out) {
			int last = stops.length / 4 - 1;
			for (int i = 0; i < last; i++) {
				float t0 = stops[i * 4];
				float t1 = stops[(i + 1) * 4];
				if (v <= t1 || i == last - 1) {
					float k = t1 > t0 ? Math.clamp((v - t0) / (t1 - t0), 0, 1) : 1;
					for (int c = 0; c < 3; c++) {
						out[c] = stops[i * 4 + 1 + c] + (stops[(i + 1) * 4 + 1 + c] - stops[i * 4 + 1 + c]) * k;
					}
					return;
				}
			}
		}
	}

	private static final Map<String, Optional<Identifier>> CACHE = new HashMap<>();
	/** The talisman mask cut from the ofuda model's texture (see keepTalisman), or null without the pack. */
	private static @Nullable NativeImage talisman;
	/** Where the talisman's face sits in the ofuda model's texture atlas (fractions of its size; read off the atlas). */
	private static final float TALISMAN_X0 = 0.737f, TALISMAN_X1 = 0.862f, TALISMAN_Y0 = 0.05f, TALISMAN_Y1 = 0.452f;
	/**
	 * Masks whose dim parts the game cuts away (its ring meshes use only the bright edge; the soft interior is for
	 * other uses): brightness below this is black. Without it Suzu's 8 m ground ring was a lit disc.
	 */
	private static final Map<String, Float> BLACK_POINT = Map.ofEntries(Map.entry("ring_line", 0.45f),
		// the kunai's lens glint: its faint halo, gamma-encoded and added, read as a teal sphere round each hit (the
		// fourth critique); the streak itself is brighter than this
		Map.entry("glint", 0.12f),
		// Lifeweaver's: a compression floor of 1-15 (of 255) over a fifth to a third of each, gamma-encoded and added, lit
		// the whole quad: the platform's 5 m bloom burst and Superbloom's flash as grainy squares, the big sparkles as
		// soft squares (found playing)
		Map.entry("lw_burst", 0.06f), Map.entry("lw_sparkle", 0.06f), Map.entry("lw_star", 0.06f), Map.entry("lw_swirl", 0.04f),
		Map.entry("lw_hit_0", 0.05f), Map.entry("lw_hit_1", 0.05f), Map.entry("lw_hit_2", 0.05f), Map.entry("lw_hit_3", 0.05f));
	/**
	 * Blended ("soft") colours given directly in sRGB, brightness stop then red, green, blue (0-1), for palettes whose
	 * game ramp runs too pale to blend: soft gold took the ramp's top, near-white cream, and read as cream-khaki comets
	 * over grass (the fourth critique: Overwatch's on-screen gold is about #DCB900 to #A18700). Ours, from those.
	 */
	private static final Map<Palette, float[]> SOFT_RAMPS = Map.of(
		// more orange than the on-screen gold itself: blended at part alpha over green grass it shifts toward olive (the
		// sixth critique measured hues of 54-62 degrees against Overwatch's 45-50), and bright at the faint end too (a
		// dark one darkened leaves to khaki, the seventh)
		Palette.GOLD, new float[] {0, .9f, .5f, 0, .5f, .93f, .6f, 0, .9f, .95f, .73f, .08f, 1, .98f, .82f, .3f},
		// Kiriko's teal and the kunai's cyan, blended (the faint parts of her effects: added over green grass they
		// summed to mint, the fifth and sixth critiques), leaning blue so that over grass they land near 176 degrees
		// (its faint end bluer and brighter again: at part alpha the path tiles still read 158-164, the seventh)
		Palette.TEAL, new float[] {0, 0, .4f, .72f, .5f, 0, .62f, .86f, .9f, .12f, .86f, .92f, 1, .62f, .98f, .98f},
		Palette.CYAN, new float[] {0, 0, .5f, .75f, .5f, 0, .7f, .9f, .9f, .3f, .92f, 1, 1, .8f, 1, 1},
		// an enemy's effects, always blended (Fx): added, red over sky or grass washed out to pink-white. Ours
		// its dark end read brick brown over grass (the sixth critique): brighter throughout
		Palette.ENEMY, new float[] {0, .7f, .1f, .12f, .5f, .86f, .18f, .17f, .9f, .95f, .32f, .28f, 1, 1, .6f, .55f});

	private EffectArt() {
	}

	/** The mask called name (textures/effect/name.png) coloured with the palette, or null without the pack. */
	public static @Nullable Identifier get(String name, Palette palette) {
		return get(name, palette, false);
	}

	/**
	 * The same, or (soft) made for blending rather than adding: its brightness becomes alpha and its colour the
	 * palette's brighter half. Added light washes out over bright ground (gold over grass turned lime, the critique);
	 * blended, gold stays gold.
	 */
	public static @Nullable Identifier get(String name, Palette palette, boolean soft) {
		String key = name + "/" + palette.name().toLowerCase(Locale.ROOT) + (soft ? "_soft" : "");
		return CACHE.computeIfAbsent(key, k -> Optional.ofNullable(load(name, palette, soft, k))).orElse(null);
	}

	/**
	 * Masks drawn here rather than read from the pack (original shapes): "card", a rounded card with a bright rim and a
	 * faint face, standing in for the rush path's talisman slabs (the game's mesh 109BC is such a card).
	 */
	private static @Nullable NativeImage generated(String name) {
		if (name.equals("talisman")) {
			if (talisman == null) {
				return null;
			}
			NativeImage copy = new NativeImage(talisman.getWidth(), talisman.getHeight(), false);
			copy.copyFrom(talisman);
			return copy;
		}
		if (name.equals("sparkle_soft")) {
			return softCross(128);
		}
		if (!name.equals("card")) {
			return null;
		}
		int w = 128;
		int h = 64;
		NativeImage image = new NativeImage(w, h, false);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				// distance inside a rounded box, in pixels
				float qx = Math.abs(x + 0.5f - w / 2f) - (w / 2f - 14);
				float qy = Math.abs(y + 0.5f - h / 2f) - (h / 2f - 14);
				float outside = (float) Math.hypot(Math.max(qx, 0), Math.max(qy, 0)) + Math.min(Math.max(qx, qy), 0) - 10;
				float inside = -outside;
				float rim = inside < 0 ? 0 : (float) Math.exp(-Math.pow((inside - 3) / 2.5f, 2));
				float face = Math.clamp(inside / 4, 0, 1) * 0.35f;
				int v = Math.round(255 * Math.clamp(Math.max(rim, face), 0, 1));
				image.setPixel(x, y, 0xFF000000 | v << 16 | v << 8 | v);
			}
		}
		return image;
	}

	/**
	 * A glowing "+" (the rush's sparkles, 3DC7): two arms with a gaussian falloff across and along, and a bright core. The
	 * pack's own sparkle mask is a flat cross with an outline (the game blooms it in HDR); drawn as it is, it read as a
	 * Minecraft health icon (the fourth critique). Ours.
	 */
	private static NativeImage softCross(int size) {
		NativeImage image = new NativeImage(size, size, false);
		float c = size / 2f;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				float dx = (x + 0.5f - c) / c;
				float dy = (y + 0.5f - c) / c;
				// each arm: thin across (sigma 0.06), long along (sigma 0.45)
				float arms = (float) Math.max(Math.exp(-dx * dx / 0.0072f - dy * dy / 0.405f), Math.exp(-dy * dy / 0.0072f - dx * dx / 0.405f));
				float core = (float) Math.exp(-(dx * dx + dy * dy) / 0.02f);
				int v = Math.round(255 * Math.clamp(Math.max(arms, core), 0, 1));
				image.setPixel(x, y, 0xFF000000 | v << 16 | v << 8 | v);
			}
		}
		return image;
	}

	/** Whether no pixel is see-through: a grey mask, its shape in its grey (the masks), not an image with a cut-out. */
	private static boolean solidAlpha(NativeImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (image.getPixel(x, y) >>> 24 != 0xFF) {
					return false;
				}
			}
		}
		return true;
	}

	private static @Nullable Identifier load(String name, Palette palette, boolean soft, String key) {
		Identifier source = Overcraft.id("textures/effect/" + name + ".png");
		Minecraft mc = Minecraft.getInstance();
		NativeImage made = generated(name);
		if (made == null && mc.getResourceManager().getResource(source).isEmpty()) {
			return null;
		}
		try (InputStream in = made != null ? null : mc.getResourceManager().open(source);
			 NativeImage mask = made != null ? made : NativeImage.read(in)) {
			NativeImage out = new NativeImage(mask.getWidth(), mask.getHeight(), false);
			float[] c = new float[3];
			float black = BLACK_POINT.getOrDefault(name, 0f);
			boolean greyMask = palette == Palette.NATURAL && soft && solidAlpha(mask);
			for (int y = 0; y < mask.getHeight(); y++) {
				for (int x = 0; x < mask.getWidth(); x++) {
					int argb = mask.getPixel(x, y);
					if (palette == Palette.NATURAL) {
						// soft: its silhouette in white, to be tinted. A grey mask (solid alpha: the smoke) is its own silhouette,
						// its grey the alpha; drawn by its alpha alone it was a white square (Catnapper's landing, found playing)
						int silhouette = greyMask ? (argb >> 16) & 0xFF : argb >>> 24;
						out.setPixel(x, y, soft ? silhouette << 24 | 0xFFFFFF : argb);
						continue;
					}
					// the masks are grey (one channel copied to three); their alpha is solid
					float v = Math.clamp(((((argb >> 16) & 0xFF) / 255f) - black) / (1 - black), 0, 1);
					float[] ramp = soft ? SOFT_RAMPS.get(palette) : null;
					if (ramp != null) {
						ramp(ramp, v, c);
						out.setPixel(x, y, Math.round(255 * v) << 24 | byte255(c[0]) << 16 | byte255(c[1]) << 8 | byte255(c[2]));
					} else if (soft) {
						palette.colour(0.6f + 0.4f * v, c);
						out.setPixel(x, y, Math.round(255 * v) << 24 | srgb(c[0]) << 16 | srgb(c[1]) << 8 | srgb(c[2]));
					} else {
						palette.colour(v, c);
						out.setPixel(x, y, 0xFF000000 | srgb(c[0]) << 16 | srgb(c[1]) << 8 | srgb(c[2]));
					}
				}
			}
			Identifier id = Overcraft.id("effect/" + key);
			mc.getTextureManager().register(id, new HeroTexture(id, out, MipmapStrategy.MEAN));
			return id;
		} catch (IOException e) {
			Overcraft.LOG.warn("Effect texture {} didn't load: {}", name, e.toString());
			return null;
		}
	}

	/** A colour from stops of (brightness, r, g, b), as they are. */
	private static void ramp(float[] stops, float v, float[] out) {
		int last = stops.length / 4 - 1;
		for (int i = 0; i < last; i++) {
			float t0 = stops[i * 4];
			float t1 = stops[(i + 1) * 4];
			if (v <= t1 || i == last - 1) {
				float k = Math.clamp((v - t0) / (t1 - t0), 0, 1);
				for (int c = 0; c < 3; c++) {
					out[c] = stops[i * 4 + 1 + c] + (stops[(i + 1) * 4 + 1 + c] - stops[i * 4 + 1 + c]) * k;
				}
				return;
			}
		}
	}

	private static int byte255(float v) {
		return Math.round(255 * Math.clamp(v, 0, 1));
	}

	private static int srgb(float linear) {
		return Math.round(255 * (float) Math.pow(Math.clamp(linear, 0, 1), 1 / 2.2));
	}

	/**
	 * Keeps a mask of the ofuda's paper talisman (from its model's texture, as it loads: the local pack's) for effects:
	 * its ink and seal bright, the paper faint, so coloured and added it glows as glyphs on a card (the rush path's
	 * talisman tiles).
	 */
	public static void keepTalisman(NativeImage atlas) {
		int x0 = Math.round(atlas.getWidth() * TALISMAN_X0);
		int x1 = Math.round(atlas.getWidth() * TALISMAN_X1);
		int y0 = Math.round(atlas.getHeight() * TALISMAN_Y0);
		int y1 = Math.round(atlas.getHeight() * TALISMAN_Y1);
		int w = 96;
		int h = 288;
		NativeImage mask = new NativeImage(w, h, false);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int argb = atlas.getPixel(x0 + (x1 - x0) * x / w, y0 + (y1 - y0) * y / h);
				float r = ((argb >> 16) & 0xFF) / 255f;
				float g = ((argb >> 8) & 0xFF) / 255f;
				float b = (argb & 0xFF) / 255f;
				float lum = 0.3f * r + 0.59f * g + 0.11f * b;
				float ink = Math.clamp((0.85f - lum) / 0.65f, 0, 1);
				// a soft rim so the card's edge shows, fading to nothing at the very border
				float edge = Math.min(Math.min(x, w - 1 - x) / 6f, Math.min(y, h - 1 - y) / 6f);
				float rim = edge < 1 ? edge : Math.max(0, 1.6f - edge * 0.6f);
				float v = Math.clamp(Math.max(0.2f + 0.8f * ink, rim * 0.7f), 0, 1) * Math.clamp(edge, 0, 1);
				int c = Math.round(255 * v);
				mask.setPixel(x, y, 0xFF000000 | c << 16 | c << 8 | c);
			}
		}
		if (talisman != null) {
			talisman.close();
		}
		talisman = mask;
	}

	/** Resource packs changed: colour them again when next asked. */
	public static void clear() {
		CACHE.clear();
	}
}
