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
		// Ana's (tools/reference/ana-effects.md "Palettes", generated from her colour rows)
		/** Her rifle's muzzle bulb 963D (800F41F7 r5, HDR 2): gold to white. */
		ANA_MUZZLE(0, 0, 0, 0, .25f, 1, .658f, 0, .5f, 1, 1, 0, .75f, 1, 1, .156f, 1, 1, 1, 1),
		ANA_MUZZLE_N(0, 0, 0, 0, .25f, .579f, .329f, 0, .5f, .751f, .519f, 0, .75f, .874f, .715f, .078f, 1, 1, .89f, .659f),
		/** The unscoped shot's trail: strip 9414 (sampled) x the gold tint (5, 3.99, 1.66), normalised. */
		ANA_SHOT_TRAIL(0, 0, 0, 0, .25f, .956f, .487f, .077f, .5f, .973f, .631f, .098f, .75f, .956f, .695f, .115f, 1, .973f, .735f, .223f),
		/** The scoped tracer (80008917 r4, HDR 2): gold to cream. */
		ANA_TRACER(0, 0, 0, 0, .25f, 1, .767f, .037f, .5f, 1, 1, .217f, .75f, 1, 1, .643f, 1, 1, 1, 1),
		ANA_TRACER_N(0, 0, 0, 0, .25f, .576f, .383f, .018f, .5f, .75f, .535f, .108f, .75f, .924f, .74f, .322f, 1, 1, .896f, .571f),
		/** The ally hit's burst 4B64D (800AB80B r1, HDR 2.8), glow 4C09 and sparks (800342A1 r1): pale gold, amber. */
		ANA_HEAL_BURST_N(0, 0, 0, 0, .25f, .549f, .405f, .15f, .5f, .694f, .567f, .214f, .75f, .846f, .739f, .283f, 1, 1, .913f, .352f),
		ANA_HEAL_GLOW(0, 0, 0, 0, .25f, 1, .635f, .123f, .5f, 1, .736f, .198f, .75f, 1, .837f, .272f, 1, 1, .939f, .347f),
		/** The enemy hit's lens discs (8009CD1E r4, HDR 10) and bubbles (800025BA r3): purple, violet. */
		ANA_HARM_N(0, 0, 0, 0, .25f, .059f, .004f, .254f, .5f, .114f, .008f, .466f, .75f, .301f, .021f, .732f, 1, .497f, .036f, 1),
		ANA_HARM_BUBBLE(0, 0, 0, 0, .25f, .211f, 0, .64f, .5f, .285f, 0, .825f, .75f, .363f, .004f, 1, 1, .552f, .117f, 1),
		/** The shot's impact sparkle 64B91 (800C00E6 r4, HDR 7, falls) and the world decals (800E8A1D / 8007CA88 r2). */
		ANA_IMPACT_N(0, 0, 0, 0, .25f, .296f, .048f, .446f, .5f, .094f, .001f, .428f, .75f, .027f, .006f, .079f, 1, .027f, .006f, .079f),
		ANA_DECAL_GOLD_N(0, 0, 0, 0, .25f, 1, .65f, .233f, .5f, .985f, .644f, .23f, .75f, .562f, .368f, .137f, 1, .132f, .087f, .042f),
		/** The grenade's trail (8006AB57 r4): black to violet to gold. */
		ANA_GRENADE_TRAIL(0, 0, 0, 0, .25f, .363f, 0, .628f, .5f, .586f, .047f, .911f, .75f, .607f, .152f, .71f, 1, .913f, .723f, .059f),
		/** The burst's gold dome (80079014 r5), violet-yellow shell (8000C715 r2), splashes (8009749B / 80029CEC r4). */
		ANA_BURST_DOME(0, 0, 0, 0, .25f, .504f, .294f, 0, .5f, .759f, .504f, .091f, .75f, .878f, .666f, .227f, 1, 1, .831f, .366f),
		ANA_BURST_SHELL(0, 0, 0, 0, .25f, .114f, .009f, .199f, .5f, .228f, .018f, .398f, .75f, .34f, .026f, .594f, 1, .982f, .783f, .019f),
		ANA_SPLASH_N(0, 0, 0, 0, .25f, .268f, 0, .517f, .5f, .269f, 0, .519f, .75f, .269f, 0, .521f, 1, .702f, .275f, .984f),
		ANA_SPLASH_GOLD_N(0, 0, 0, 0, .25f, .333f, .17f, 0, .5f, .333f, .17f, 0, .75f, .333f, .17f, 0, 1, .999f, .745f, .242f),
		/** The burst's droplets (80085D4D r1, HDR 10 / 800280E0 r1), splat decal (80024EDE r1, HDR 10), glow (800F4D2E r0). */
		ANA_DROP_GOLD_N(0, 0, 0, 0, .25f, .425f, .26f, 0, .5f, .708f, .457f, 0, .75f, .852f, .611f, 0, 1, 1, .768f, 0),
		ANA_DROP_PURPLE(0, 0, 0, 0, .25f, .346f, 0, .614f, .5f, .417f, 0, .743f, .75f, .487f, 0, .871f, 1, .558f, 0, 1),
		ANA_BURST_DECAL(0, 0, 0, 0, .25f, .609f, .053f, .906f, .5f, .793f, .328f, .465f, .75f, .956f, .584f, .091f, 1, 1, 1, 1),
		ANA_BURST_GLOW(0, 0, 0, 0, .25f, .577f, 0, 1, .5f, .693f, .4f, .708f, .75f, .845f, .786f, .34f, 1, 1, .847f, .006f),
		/** The heal boost's plus (80091108 r4) and the anti-heal's broken plus (800533DC r1, falls). */
		ANA_BOOST_PLUS(0, 0, 0, 0, .25f, 1, .845f, .341f, .5f, 1, .887f, .396f, .75f, 1, .928f, .45f, 1, 1, 1, 1),
		ANA_ANTIHEAL(0, 0, 0, 0, .25f, .434f, .035f, .821f, .5f, .423f, .071f, .737f, .75f, .413f, .106f, .655f, 1, .402f, .141f, .571f),
		/** The sleep dart's glow (80034914 r1, HDR 10), the Zs over life (80053871 r3, 12BB, HDR 1.3), the sleep sparks over life (800277D9 r0, HDR 5.7, falls). */
		ANA_SLEEP_GLOW_N(0, 0, 0, 0, .25f, .006f, .045f, .202f, .5f, .01f, .068f, .304f, .75f, .02f, .139f, .621f, 1, .032f, .223f, 1),
		ANA_SLEEP_Z(0, 0, 0, 0, .25f, .325f, .233f, .32f, .5f, .651f, .466f, .64f, .75f, .971f, .695f, .955f, 1, 1, .928f, 1),
		ANA_SLEEP_SPARK(0, 0, 0, 0, .25f, 1, 1, .38f, .5f, 1, .783f, .293f, .75f, 1, .447f, .148f, 1, 1, 0, 0),
		/** Local Anesthetic's dome (8008512C r0), gas (800861DA r7), drops (8005EE9F r3), the numbed wisps (800747DE r1, HDR 2). */
		ANA_LA_DOME(0, 0, 0, 0, .25f, .051f, .024f, .552f, .5f, .172f, .359f, .856f, .75f, .174f, .693f, .994f, 1, .019f, .982f, .982f),
		ANA_LA_GAS(0, 0, 0, 0, .25f, .068f, .182f, .643f, .5f, .13f, .371f, .879f, .75f, .36f, .677f, 1, 1, .637f, .921f, 1),
		ANA_LA_DROP(0, 0, 0, 0, .25f, .03f, .167f, .347f, .5f, .094f, .437f, .763f, .75f, .172f, .672f, .934f, 1, .242f, .863f, 1),
		ANA_LA_WISP_N(0, 0, 0, 0, .25f, .038f, .46f, .703f, .5f, .001f, .118f, .929f, .75f, .002f, .076f, .555f, 1, .042f, .063f, .135f),
		/** Nano Boost: the ally's shells (8000CCD7 r4, Mercy's boost blue), flames (80007283 r5, HDR 6), sparks (800DAD00 r1), the beam (80049B54 r2, HDR 3), the launch puffs over life (800AE47A r0). */
		ANA_NANO_BLUE(0, 0, 0, 0, .25f, .026f, .156f, 1, .5f, .046f, .293f, 1, .75f, .187f, .44f, 1, 1, .342f, .59f, 1),
		ANA_NANO_FLAME_N(0, 0, 0, 0, .25f, .005f, .031f, .191f, .5f, .013f, .085f, .281f, .75f, .087f, .2f, .447f, 1, .342f, .591f, 1),
		ANA_NANO_BEAM(0, 0, 0, 0, .25f, .116f, .186f, 1, .5f, .266f, .427f, 1, .75f, .513f, .821f, 1, 1, 1, 1, 1),
		ANA_NANO_BEAM_N(0, 0, 0, 0, .25f, .039f, .062f, .348f, .5f, .089f, .142f, .399f, .75f, .171f, .274f, .515f, 1, .443f, .71f, 1),
		ANA_NANO_MUZZLE(0, 0, 0, 0, .25f, .537f, .181f, 0, .5f, .733f, .362f, 0, .75f, .893f, .601f, 0, 1, 1, .941f, 0),
		// ---- Sombra (tools/reference/sombra-effects.md; research\sombra\workx_palettes.txt): her team's rows, the enemies' _ENEMY twins ----
		/** Cyberspace's trail ribbon (502E C25): violet to pink, HDR 5 (800B665E3BD4 r3, peak 5.00; game hex #B800FF #FF51FF #FF71FF #FF96FF #FFB7FF). */
		SOMBRA_CYBER_TRAIL(0, 0, 0, 0, .25f, 1, .082f, 1, .5f, 1, .165f, 1, .75f, 1, .306f, 1, 1, 1, .476f, 1),
		SOMBRA_CYBER_TRAIL_N(0, 0, 0, 0, .25f, .236f, .016f, .42f, .5f, .377f, .033f, .559f, .75f, .606f, .061f, .763f, 1, .879f, .095f, 1),
		/** the trail's twin row: red-orange to pale yellow, HDR 5 (800B665E3BD4 r4, peak 5.00; game hex #FF1200 #FF8051 #FFAE71 #FFE596 #FFFFB7). */
		SOMBRA_CYBER_TRAIL_ENEMY(0, 0, 0, 0, .25f, 1, .215f, .082f, .5f, 1, .423f, .165f, .75f, 1, .781f, .306f, 1, 1, 1, .476f),
		SOMBRA_CYBER_TRAIL_ENEMY_N(0, 0, 0, 0, .25f, .42f, .043f, .016f, .5f, .559f, .085f, .033f, .75f, .763f, .156f, .061f, 1, 1, .242f, .095f),
		/** the flying data cube 13EE4 (502E C28): violet to pale pink (8008C0FCC358 r1, peak 1.00; game hex #1E061E #B30CD4 #E475FA #F1B8FD #FEE5FF). */
		SOMBRA_CYBER_CUBE(0, .013f, .002f, .013f, .25f, .45f, .004f, .658f, .5f, .773f, .178f, .96f, .75f, .881f, .478f, .98f, 1, .991f, .783f, 1),
		/** the cube's twin row: rust to white (8008C0FCC358 r2, peak 1.00; game hex #1E0606 #A84513 #DA694B #FA9E90 #FAFAFF). */
		SOMBRA_CYBER_CUBE_ENEMY(0, .013f, .002f, .002f, .25f, .39f, .06f, .007f, .5f, .703f, .142f, .07f, .75f, .956f, .344f, .281f, 1, .956f, .956f, 1),
		/** the zone's 11.8 m sphere 1385 (7C7E C72): purple, peak 0.76 (800462B4FD6C r3, peak 0.76; game hex #9D0CA6 #AE0FB8 #BD11C7 #CA13D5 #D615E2). */
		SOMBRA_CYBER_SPHERE(0, 0, 0, 0, .25f, .422f, .005f, .477f, .5f, .506f, .005f, .572f, .75f, .589f, .006f, .666f, 1, .674f, .007f, .762f),
		/** the sphere's twin row: dim brown-red, peak 0.16 (800462B4FD6C r4, peak 0.16; game hex #511A15 #5A1E18 #63211C #6A251E #712721). */
		SOMBRA_CYBER_SPHERE_ENEMY(0, 0, 0, 0, .25f, .103f, .013f, .009f, .5f, .123f, .015f, .011f, .75f, .144f, .018f, .013f, 1, .165f, .021f, .015f),
		/** the zone's code streaks 9BDF (7C7E C38): brown to gold, HDR 5 (800F2B087BEC r7, peak 5.03; game hex #DC6000 #FFC622 #FFFF45 #FFFF5B #FFFFFF). */
		SOMBRA_CYBER_GOLD(0, 0, 0, 0, .25f, 1, .565f, .016f, .5f, 1, 1, .06f, .75f, 1, 1, .104f, 1, 1, 1, 1),
		SOMBRA_CYBER_GOLD_N(0, 0, 0, 0, .25f, .275f, .112f, .003f, .5f, .574f, .35f, .012f, .75f, .868f, .583f, .021f, 1, .994f, .818f, .273f),
		/** the streaks' twin row: brown to orange, HDR 5 (800F2B087BEC r8, peak 5.03; game hex #DC1500 #FF6F22 #FFC845 #FFFE5B #FFFFFF). */
		SOMBRA_CYBER_ORANGE(0, 0, 0, 0, .25f, 1, .159f, .016f, .5f, 1, .58f, .06f, .75f, 1, .993f, .104f, 1, 1, 1, 1),
		SOMBRA_CYBER_ORANGE_N(0, 0, 0, 0, .25f, .275f, .032f, .003f, .5f, .574f, .115f, .012f, .75f, .868f, .197f, .021f, 1, .994f, .549f, .273f),
		/** the zone's circuit sphere D72C (7C7E C58): dark gold to cream, HDR 2.7 (80050EECEE60 r9, peak 2.71; game hex #412600 #D29100 #FFC325 #FFED7D #FFFFE0). */
		SOMBRA_CYBER_CIRCUIT(0, 0, 0, 0, .25f, .646f, .282f, 0, .5f, 1, .545f, .018f, .75f, 1, .85f, .204f, 1, 1, 1, .743f),
		SOMBRA_CYBER_CIRCUIT_N(0, .02f, .007f, 0, .25f, .239f, .104f, 0, .5f, .441f, .201f, .007f, .75f, .524f, .314f, .075f, 1, 1, .745f, .275f),
		/** the circuit sphere's twin row: plum to orange, HDR 3.8 (80050EECEE60 r10, peak 3.79; game hex #7E0068 #A00085 #FF7383 #FFB96E #FFF200). */
		SOMBRA_CYBER_CIRCUIT_ENEMY(0, 0, 0, 0, .25f, .352f, 0, .234f, .5f, 1, .172f, .228f, .75f, 1, .483f, .155f, 1, 1, .888f, 0),
		SOMBRA_CYBER_CIRCUIT_ENEMY_N(0, 0, 0, 0, .25f, .093f, 0, .062f, .5f, .285f, .046f, .06f, .75f, .606f, .128f, .041f, 1, 1, .235f, 0),
		/** the zone's rising plus 3DC7 (7C7E C74): gold to pale yellow, HDR 3 (8001FA6F94F5 r2, peak 3.00; game hex #DE9A00 #FFDB21 #FFFF93 #FFFFB5 #FFFFFF). */
		SOMBRA_CYBER_PLUS(0, 0, 0, 0, .25f, 1, .71f, .015f, .5f, 1, 1, .29f, .75f, 1, 1, .464f, 1, 1, 1, 1),
		SOMBRA_CYBER_PLUS_N(0, 0, 0, 0, .25f, .351f, .237f, .005f, .5f, .408f, .337f, .097f, .75f, .527f, .458f, .155f, 1, 1, .913f, .351f),
		/** the zone's ground decal 7B847 (7C7E C65): violet to cream, HDR 3 (800032683E86 r4, peak 3.00; game hex #340048 #D28965 #FFFFD8 #FFFFFF #FFFFFF). */
		SOMBRA_CYBER_DECAL(0, 0, 0, 0, .25f, .644f, .249f, .13f, .5f, 1, 1, .685f, .75f, 1, 1, 1, 1, 1, 1, 1),
		SOMBRA_CYBER_DECAL_N(0, .011f, 0, .022f, .25f, .215f, .083f, .043f, .5f, .457f, .383f, .229f, .75f, .973f, .909f, .619f, 1, 1, .964f, .679f),
		/** the burst's flare A76F (7CDB C17): violet to pink, HDR 3 (8008CA52628C r1, peak 3.00; game hex #50007D #D500FF #FF5AFF #FFB1FF #FFE5FF). */
		SOMBRA_CYBER_BURST(0, 0, 0, 0, .25f, .668f, 0, 1, .5f, 1, .103f, 1, .75f, 1, .438f, 1, 1, 1, .787f, 1),
		SOMBRA_CYBER_BURST_N(0, 0, 0, 0, .25f, .223f, 0, .353f, .5f, .543f, .034f, .754f, .75f, .818f, .146f, .939f, 1, 1, .262f, .973f),
		/** the burst flare's twin row: rust to salmon, HDR 4.4 (8008CA52628C r2, peak 4.43; game hex #000000 #DE6B46 #FF9462 #FFC986 #FFFFD7). */
		SOMBRA_CYBER_BURST_ENEMY(0, 0, 0, 0, .25f, .732f, .148f, .06f, .5f, 1, .298f, .121f, .75f, 1, .586f, .239f, 1, 1, 1, .677f),
		SOMBRA_CYBER_BURST_ENEMY_N(0, 0, 0, 0, .25f, .165f, .033f, .014f, .5f, .332f, .067f, .027f, .75f, .654f, .132f, .054f, 1, 1, .227f, .153f),
		/** the weakened enemy's arrows 66E4 (7AE1 C14): violet to pink, HDR 3 (80051C68041B r2, peak 3.00; game hex #8E00E2 #A933D0 #CF55A9 #FF777E #FFFDFF). */
		SOMBRA_WEAKENED(0, 0, 0, 0, .25f, .399f, .033f, .628f, .5f, .623f, .092f, .397f, .75f, 1, .184f, .21f, 1, 1, .983f, 1),
		SOMBRA_WEAKENED_N(0, 0, 0, 0, .25f, .133f, .011f, .209f, .5f, .208f, .031f, .132f, .75f, .347f, .061f, .07f, 1, 1, .328f, .366f),
		/** the arrows' other row: dark red to salmon, HDR 3 (80051C68041B r4, peak 3.00; game hex #8E0200 #AB0900 #F75035 #FF8B61 #FFB17D). */
		SOMBRA_WEAKENED_ALT(0, 0, 0, 0, .25f, .406f, .003f, 0, .5f, .926f, .081f, .036f, .75f, 1, .26f, .12f, 1, 1, .441f, .205f),
		SOMBRA_WEAKENED_ALT_N(0, 0, 0, 0, .25f, .136f, .001f, 0, .5f, .309f, .027f, .012f, .75f, .652f, .087f, .04f, 1, 1, .147f, .068f),
		/** the weakened enemy's falling chevrons DFDC (7AE1 C24): black to pink (80017FEBBB59 r3, peak 1.00; game hex #000000 #AF334F #DB667F #F7839D #FF92AD). */
		SOMBRA_WEAKENED_CHEVRON(0, 0, 0, 0, .25f, .426f, .033f, .078f, .5f, .706f, .134f, .214f, .75f, .93f, .227f, .339f, 1, 1, .286f, .415f),
		/** Hotfix's strands to an ally (7C6E C9): brown-gold to cream, HDR 3.9 (8006FB0C3C19 r3, peak 3.89; game hex #DE9A19 #E59F1A #FFD256 #FFFFA1 #FFFFFF). */
		SOMBRA_HOTFIX_STRAND(0, 0, 0, 0, .25f, .785f, .347f, .011f, .5f, 1, .648f, .094f, .75f, 1, 1, .356f, 1, 1, 1, 1),
		SOMBRA_HOTFIX_STRAND_N(0, 0, 0, 0, .25f, .202f, .089f, .003f, .5f, .269f, .166f, .024f, .75f, .448f, .396f, .092f, 1, 1, .965f, .564f),
		/** the strands' twin (7C6E C13): brown-red to peach, HDR 3.9 (8005C557A361 r2, peak 3.89; game hex #DE4119 #E5431A #FF7C56 #FFD09E #FFFFFF). */
		SOMBRA_HOTFIX_STRAND_ENEMY(0, 0, 0, 0, .25f, .785f, .057f, .011f, .5f, 1, .203f, .094f, .75f, 1, .631f, .342f, 1, 1, 1, 1),
		SOMBRA_HOTFIX_STRAND_ENEMY_N(0, 0, 0, 0, .25f, .202f, .015f, .003f, .5f, .269f, .052f, .024f, .75f, .438f, .162f, .088f, 1, 1, .723f, .564f),
		/** Hotfix's strands to a hacked object (1B32 C21): indigo to lilac, HDR 3.9 (800C55D7C70A r3, peak 3.89; game hex #5719DE #5A1AE5 #9656FF #F7A1FF #FFFFFF). */
		SOMBRA_HACK_STRAND(0, 0, 0, 0, .25f, .102f, .011f, .785f, .5f, .305f, .094f, 1, .75f, .933f, .356f, 1, 1, 1, 1, 1),
		SOMBRA_HACK_STRAND_N(0, 0, 0, 0, .25f, .026f, .003f, .202f, .5f, .078f, .024f, .269f, .75f, .24f, .092f, .448f, 1, .855f, .564f, 1),
		/** the hack strands' twin (1B32 C23): dark red to pink, HDR 3.9 (800B4A29B50C r3, peak 3.89; game hex #DE1919 #E51A1A #FF5D56 #FFA99D #FFFFFF). */
		SOMBRA_HACK_STRAND_ENEMY(0, 0, 0, 0, .25f, .785f, .011f, .011f, .5f, 1, .109f, .094f, .75f, 1, .396f, .336f, 1, 1, 1, 1),
		SOMBRA_HACK_STRAND_ENEMY_N(0, 0, 0, 0, .25f, .202f, .003f, .003f, .5f, .269f, .028f, .024f, .75f, .434f, .102f, .086f, 1, 1, .521f, .462f),
		/** the healed ally's code wall A512 (6570 C29): brown to gold, HDR 5 (8003744D4E14 r7, peak 5.03; game hex #DC6000 #FFC622 #FFFF45 #FFFF5B #FFFFFF). */
		SOMBRA_HOTFIX_HEAL(0, 0, 0, 0, .25f, 1, .565f, .016f, .5f, 1, 1, .06f, .75f, 1, 1, .104f, 1, 1, 1, 1),
		SOMBRA_HOTFIX_HEAL_N(0, 0, 0, 0, .25f, .275f, .112f, .003f, .5f, .574f, .35f, .012f, .75f, .868f, .583f, .021f, 1, .994f, .818f, .273f),
		/** the code wall's twin row: brown to orange, HDR 5 (8003744D4E14 r8, peak 5.03; game hex #DC1500 #FF6F22 #FFC845 #FFFE5B #FFFFFF). */
		SOMBRA_HOTFIX_HEAL_ENEMY(0, 0, 0, 0, .25f, 1, .159f, .016f, .5f, 1, .58f, .06f, .75f, 1, .993f, .104f, 1, 1, 1, 1),
		SOMBRA_HOTFIX_HEAL_ENEMY_N(0, 0, 0, 0, .25f, .275f, .032f, .003f, .5f, .574f, .115f, .012f, .75f, .868f, .197f, .021f, 1, .994f, .549f, .273f),
		/** the healed ally's body glow A7B6 (6570 C37): amber to gold, HDR 4 (80087210BF0E r1, peak 4.00; game hex #000000 #F69419 #FFD70C #FFFF5F #FFFFA6). */
		SOMBRA_HOTFIX_GLOW(0, 0, 0, 0, .25f, .923f, .296f, .01f, .5f, 1, .68f, .004f, .75f, 1, 1, .114f, 1, 1, 1, .381f),
		SOMBRA_HOTFIX_GLOW_N(0, 0, 0, 0, .25f, .231f, .074f, .002f, .5f, .409f, .17f, .001f, .75f, .659f, .328f, .028f, 1, 1, .565f, .095f),
		/** the beacon's ribbons (1B0F C8): indigo to violet to white, HDR 2.45 (800265C1ADAE r3, peak 2.45; game hex #100033 #FF0BFF #FFBEFF #FFFFFF #FFFFFF). */
		SOMBRA_BEACON_TRAIL(0, 0, 0, 0, .25f, .996f, .003f, 1, .5f, 1, .518f, 1, .75f, 1, 1, 1, 1, 1, 1, 1),
		SOMBRA_BEACON_TRAIL_N(0, .002f, 0, .014f, .25f, .406f, .001f, .764f, .5f, .633f, .211f, 1, .75f, .775f, .512f, 1, 1, .904f, .921f, 1),
		/** the beacon ribbons' twin row: brown to orange-red to white (800048EE6A61 r4, peak 2.45; game hex #331000 #FF480B #FFCFBE #FFFFFF #FFFFFF). */
		SOMBRA_BEACON_TRAIL_ENEMY(0, 0, 0, 0, .25f, 1, .065f, .003f, .5f, 1, .623f, .518f, .75f, 1, 1, 1, 1, 1, 1, 1),
		SOMBRA_BEACON_TRAIL_ENEMY_N(0, .014f, .002f, 0, .25f, .764f, .027f, .001f, .5f, 1, .254f, .211f, .75f, 1, .569f, .512f, 1, .904f, .921f, 1),
		/** the teleport's flare 29F7 (1A76 / 5FE0 C17): indigo to violet to white (800FCD048FF0 r1, peak 1.00; game hex #310063 #DB2AF8 #E89EFF #EFD8FF #FFFFFF). */
		SOMBRA_TELEPORT(0, 0, 0, 0, .25f, .707f, .023f, .935f, .5f, .808f, .344f, 1, .75f, .859f, .685f, 1, 1, 1, 1, 1),
		/** the teleport flare's twin row: indigo to coral to white (800FCD048FF0 r2, peak 1.00; game hex #310063 #F75033 #FFA79E #FFDBD8 #FFFFFF). */
		SOMBRA_TELEPORT_ENEMY(0, 0, 0, 0, .25f, .928f, .08f, .033f, .5f, 1, .387f, .344f, .75f, 1, .708f, .685f, 1, 1, 1, 1),
		/** her body flash at the teleport (1B1E / 3683 material): lilac to purple, HDR 12.6, falls (800C31BA6BBA r1, peak 12.63, falls; game hex #FFFFFF #FFFFFF #FFE1FF #FF9CFF #FF00FF). */
		SOMBRA_TELEPORT_BODY(0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, .751f, 1, .75f, 1, .331f, 1, 1, 1, 0, 1),
		SOMBRA_TELEPORT_BODY_N(0, 0, 0, 0, .25f, .594f, .116f, .86f, .5f, .487f, .059f, .719f, .75f, .354f, .026f, .536f, 1, .208f, 0, .317f),
		/** the body flash's twin row: coral to brown, HDR 12.5, falls (800C31BA6BBA r2, peak 12.54, falls; game hex #FFFFFA #FFFFA3 #FFF573 #FFDA49 #FFBF01). */
		SOMBRA_TELEPORT_BODY_ENEMY(0, 0, 0, 0, .25f, 1, 1, .366f, .5f, 1, .912f, .172f, .75f, 1, .699f, .067f, 1, 1, .522f, 0),
		SOMBRA_TELEPORT_BODY_ENEMY_N(0, 0, 0, 0, .25f, .855f, .097f, .029f, .5f, .712f, .073f, .014f, .75f, .533f, .056f, .005f, 1, .335f, .042f, 0),
		/** the stealth detection ring 129B (4DD9 C3): black to violet to white, HDR 20 (800561F03EEF r3, peak 19.98; game hex #1F0335 #E509FF #FF0DFF #FF05FF #FFFFFF). */
		SOMBRA_DETECT_RING(0, 0, 0, 0, .25f, .782f, .003f, 1, .5f, 1, .004f, 1, .75f, 1, .002f, 1, 1, 1, 1, 1),
		SOMBRA_DETECT_RING_N(0, .001f, 0, .002f, .25f, .039f, 0, .138f, .5f, .08f, 0, .286f, .75f, .185f, 0, .661f, 1, 1, 1, 1),
		/** 256F's glow streaks 242 (C23): violet to white (800C48520539 r1, peak 1.00; game hex #C300FF #913EFF #9C8DFF #D5D0FF #FFFFFF). */
		SOMBRA_BEACON_LAND(0, 0, 0, 0, .25f, .282f, .049f, 1, .5f, .332f, .266f, 1, .75f, .663f, .63f, 1, 1, 1, 1, 1),
		/** Her team's strip A06C: the effects reading's report (tools/reference/sombra-effects.md), sRGB to linear. */
		SOMBRA_PURPLE(0, 0, 0, 0, .1f, .171f, 0, .22f, .325f, .191f, .012f, .462f, .55f, .227f, .058f, .863f, .775f, .296f, .168f, .973f, 1, 1, 1, 1),
		/** Her team's strip A06B: the effects reading's report (tools/reference/sombra-effects.md), sRGB to linear. */
		SOMBRA_PURPLE_DARK(0, 0, 0, 0, .1f, 0, 0, 0, .325f, .162f, 0, .15f, .55f, .309f, .016f, .638f, .775f, .209f, .231f, 1, 1, .991f, .991f, 1),
		/** Their twin, strip ADD0: the effects reading's report (tools/reference/sombra-effects.md), sRGB to linear. */
		SOMBRA_ENEMY_RED(0, 0, 0, 0, .1f, .319f, 0, 0, .325f, .651f, 0, 0, .55f, .956f, .074f, 0, .775f, 1, .402f, 0, 1, 1, 1, 1),
		/** The hacked skull's strip A720 (#651B84 > #BDAAFF; the middle ours): the effects reading's report (tools/reference/sombra-effects.md), sRGB to linear. */
		SOMBRA_SKULL(0, 0, 0, 0, .1f, .13f, .011f, .231f, .55f, .27f, .125f, .539f, 1, .509f, .402f, 1),
		/** The pistol's muzzle strip A260, magenta to gold: the effects reading's report (tools/reference/sombra-effects.md), sRGB to linear. */
		SOMBRA_MUZZLE(0, 0, 0, 0, .1f, .558f, 0, .73f, .325f, .839f, .117f, .156f, .55f, 1, .584f, .025f, .775f, 1, 1, .109f, 1, 1, 1, 1),
		/** The pistol's tracer 1998, warm orange #FF6236 (normalised; the ends ours): the effects reading's report (tools/reference/sombra-effects.md), sRGB to linear. */
		SOMBRA_TRACER(0, 0, 0, 0, .1f, 0, 0, 0, .55f, 1, .122f, .037f, 1, 1, .644f, .479f),
		/** Her heals' gold LUT row (Hotfix's healed ally, Cyberspace's allies): the effects reading's report (tools/reference/sombra-effects.md), sRGB to linear. */
		SOMBRA_HEAL_GOLD(0, 0, 0, 0, .1f, .141f, .023f, 0, .325f, .27f, .109f, .003f, .55f, .571f, .347f, .012f, .775f, .871f, .584f, .02f, 1, .991f, .815f, .275f),
		// Brigitte's (tools/reference/brigitte-effects.md, researchrigitte\workx_palettes.txt)
		/** the flail swing arc 5F3F (3P clip 4E9E effect 26EB C17): black, amber, tan, sky blue, HDR 2 (800A3EC1BF96 r6, peak 2.00; game hex #000000 #FDBC0E #FFE7AE #8DD5FF #CAFFFF). */
		BRIGITTE_SWING(0, 0, 0, 0, .25f, .983f, .5f, .004f, .5f, 1, .802f, .425f, .75f, .266f, .663f, 1, 1, .592f, 1, 1),
		BRIGITTE_SWING_N(0, 0, 0, 0, .25f, .491f, .25f, .002f, .5f, .664f, .401f, .213f, .75f, .133f, .331f, .876f, 1, .296f, .708f, 1),
		/** the swing arc's twin (26EB C15): black, red, orange, pale gold, HDR 2 (80061D37D65D r6, peak 2.00; game hex #000000 #FD210E #FF6327 #FFB146 #FFFFCA). */
		BRIGITTE_SWING_ENEMY(0, 0, 0, 0, .25f, .983f, .015f, .004f, .5f, 1, .126f, .02f, .75f, 1, .441f, .061f, 1, 1, 1, .592f),
		BRIGITTE_SWING_ENEMY_N(0, 0, 0, 0, .25f, .491f, .008f, .002f, .5f, .866f, .063f, .01f, .75f, .979f, .221f, .031f, 1, 1, .679f, .296f),
		/** the 1P swing arc 5D48 (clip 4F53 effect 2624 C9): the same gold-to-blue row (800D213EBF90 r7, peak 2.00; game hex #000000 #FDBC0E #FFE7AE #8DD5FF #CAFFFF). */
		BRIGITTE_SWING_1P(0, 0, 0, 0, .25f, .983f, .5f, .004f, .5f, 1, .802f, .425f, .75f, .266f, .663f, 1, 1, .592f, 1, 1),
		BRIGITTE_SWING_1P_N(0, 0, 0, 0, .25f, .491f, .25f, .002f, .5f, .664f, .401f, .213f, .75f, .133f, .331f, .876f, 1, .296f, .708f, 1),
		/** the 3P chain ribbon 13DB9 (216C / 22CF C2): olive to pale yellow, HDR 10 (8007A4D04894 r1, peak 9.99; game hex #814300 #BD9C00 #FFFF06 #FFFFFF #FFFFFF). */
		BRIGITTE_CHAIN(0, 0, 0, 0, .25f, .507f, .334f, 0, .5f, 1, 1, .002f, .75f, 1, 1, 1, 1, 1, 1, 1),
		BRIGITTE_CHAIN_N(0, 0, 0, 0, .25f, .051f, .033f, 0, .5f, .172f, .137f, 0, .75f, .624f, .573f, .186f, 1, .983f, 1, .738f),
		/** the chain ribbon's twin row: dark red to pink-white, HDR 10 (8007A4D04894 r0, peak 9.99; game hex #810000 #E30000 #FF5948 #FFFFDD #FFFFFF). */
		BRIGITTE_CHAIN_ENEMY(0, 0, 0, 0, .25f, .768f, 0, 0, .5f, 1, .1f, .064f, .75f, 1, 1, .72f, 1, 1, 1, 1),
		BRIGITTE_CHAIN_ENEMY_N(0, 0, 0, 0, .25f, .077f, 0, 0, .5f, .135f, .01f, .006f, .75f, .343f, .112f, .072f, 1, 1, .738f, .738f),
		/** the energy chain 33C1 / 33C2 and 1P swing ribbon (29D41 x 19167): navy to cyan, HDR 3.5 (800134B6C381 r4, peak 3.50; game hex #0E4585 #8023F5 #25C9FF #3CFFFF #FFFFFF). */
		BRIGITTE_ENERGY_BLUE(0, 0, 0, 0, .25f, .216f, .017f, .914f, .5f, .019f, .581f, 1, .75f, .046f, 1, 1, 1, 1, 1, 1),
		BRIGITTE_ENERGY_BLUE_N(0, 0, 0, 0, .25f, .062f, .005f, .261f, .5f, .005f, .166f, .41f, .75f, .013f, .48f, .628f, 1, .361f, .965f, 1),
		/** the energy chain's other row: brown to gold to cream, HDR 3.5 (800134B6C381 r6, peak 3.50; game hex #7A150C #F6951F #FF8E25 #FFC539 #FFFFFF). */
		BRIGITTE_ENERGY_GOLD(0, 0, 0, 0, .25f, .919f, .302f, .013f, .5f, 1, .271f, .019f, .75f, 1, .56f, .041f, 1, 1, 1, 1),
		BRIGITTE_ENERGY_GOLD_N(0, 0, 0, 0, .25f, .263f, .086f, .004f, .5f, .41f, .077f, .005f, .75f, .59f, .16f, .012f, 1, 1, .815f, .361f),
		/** the swing's ground hit 220B dust 14D1C (C9 / C11 tint row): red to gold, HDR 1.5 (80046EF0C935 r0, peak 1.46; game hex #FF0000 #FF8300 #FFB300 #FFD600 #FFF400). */
		BRIGITTE_GROUND_DUST(0, 0, 0, 0, .25f, 1, .227f, 0, .5f, 1, .453f, 0, .75f, 1, .676f, 0, 1, 1, .902f, 0),
		BRIGITTE_GROUND_DUST_N(0, 0, 0, 0, .25f, .763f, .155f, 0, .5f, .843f, .31f, 0, .75f, .92f, .463f, 0, 1, 1, .618f, 0),
		/** the whip head's sparks 4C09 (clip 5276 effect 2770 C19): red-orange to cream (800FD0A96386 r7, peak 1.00; game hex #F72C00 #FB9A20 #FFCE30 #FFE47B #FFF7A4). */
		BRIGITTE_WHIP_SPARK(0, 0, 0, 0, .25f, .964f, .322f, .015f, .5f, .999f, .62f, .029f, .75f, 1, .774f, .198f, 1, 1, .93f, .371f),
		/** the whip sparks' twin (2770 C23): red-orange to peach (800BEB9E3BB8 r7, peak 1.00; game hex #F72C00 #FB5E20 #FF7B30 #FFB07B #FFD7A4). */
		BRIGITTE_WHIP_SPARK_ALT(0, 0, 0, 0, .25f, .964f, .111f, .015f, .5f, .999f, .197f, .029f, .75f, 1, .435f, .198f, 1, 1, .679f, .371f),
		/** the whip head's smoke DFCB (2770 C38): grey-brown to cream, HDR 1.3 (800112076AB5 r4, peak 1.31; game hex #6C635D #A4988F #D8CDC1 #FEF5E5 #FFFFFF). */
		BRIGITTE_WHIP_SMOKE(0, 0, 0, 0, .25f, .372f, .312f, .276f, .5f, .685f, .614f, .531f, .75f, .993f, .911f, .782f, 1, 1, 1, 1),
		BRIGITTE_WHIP_SMOKE_N(0, 0, 0, 0, .25f, .285f, .239f, .212f, .5f, .525f, .47f, .407f, .75f, .761f, .697f, .599f, 1, 1, .928f, .794f),
		/** the barrier field material 5B2C (look 13917) LUT row 0: (dim red at 0) navy, blue, cyan; alpha 0.08 -> 1 (800E79730886 r0, peak 0.93; game hex #AD1E2F #852549 #353C75 #1785BF #17E0F7). */
		BRIGITTE_BARRIER(0, 0, 0, 0, .25f, .233f, .018f, .066f, .5f, .036f, .045f, .178f, .75f, .008f, .233f, .519f, 1, .008f, .745f, .93f),
		/** the field's row 1: dark red, red, orange, white (800E79730886 r1, peak 1.00; game hex #4E1F1E #B42E09 #F33900 #FFA200 #FFFEFF). */
		BRIGITTE_BARRIER_ENEMY(0, 0, 0, 0, .25f, .459f, .027f, .003f, .5f, .898f, .041f, 0, .75f, 1, .362f, 0, 1, 1, .991f, 1),
		/** the break's shield silhouette 5DF1 (268C C45): cream to blue, HDR 4, falls (800D2E31E3D1 r2, peak 4.00, falls; game hex #FFFFFF #FFFFFF #C4FFFF #88F7FF #59DFFF). */
		BRIGITTE_BREAK_SHIELD(0, 0, 0, 0, .25f, 1, 1, 1, .5f, .551f, 1, 1, .75f, .245f, .934f, 1, 1, .101f, .738f, 1),
		BRIGITTE_BREAK_SHIELD_N(0, 0, 0, 0, .25f, .274f, .469f, .977f, .5f, .138f, .337f, 1, .75f, .061f, .233f, 1, 1, .025f, .184f, 1),
		/** the silhouette's twin (C51): cream to red, HDR 4, falls (80094DD0A453 r2, peak 4.00, falls; game hex #FFFFFF #FFFFFF #FFCCC4 #FF8C88 #FF5959). */
		BRIGITTE_BREAK_SHIELD_ENEMY(0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, .605f, .551f, .75f, 1, .262f, .245f, 1, 1, .101f, .101f),
		BRIGITTE_BREAK_SHIELD_ENEMY_N(0, 0, 0, 0, .25f, 1, .283f, .251f, .5f, 1, .151f, .138f, .75f, 1, .066f, .061f, 1, 1, .025f, .025f),
		/** the break's glow 17C0 on 5DF1 (C47): periwinkle to cyan, HDR 5 (8005E0AB817E r0, peak 5.00, falls; game hex #FFFFFF #BFF5FF #4CFFFF #83FFFF #94FFFF). */
		BRIGITTE_BREAK_GLOW(0, 0, 0, 0, .25f, .524f, .912f, 1, .5f, .072f, 1, 1, .75f, .226f, 1, 1, 1, .297f, 1, 1),
		BRIGITTE_BREAK_GLOW_N(0, 0, 0, 0, .25f, .105f, .182f, .729f, .5f, .014f, .241f, .62f, .75f, .045f, .525f, .695f, 1, .059f, .658f, .73f),
		/** the glow's twin (C53): salmon to orange, HDR 5 (8001358106BB r0, peak 5.00, falls; game hex #FFFFFF #FFBFBF #FFA34C #FFFF83 #FFFF94). */
		BRIGITTE_BREAK_GLOW_ENEMY(0, 0, 0, 0, .25f, 1, .524f, .524f, .5f, 1, .365f, .072f, .75f, 1, 1, .226f, 1, 1, 1, .297f),
		BRIGITTE_BREAK_GLOW_ENEMY_N(0, 0, 0, 0, .25f, .729f, .105f, .105f, .5f, .62f, .073f, .014f, .75f, .695f, .228f, .045f, 1, .73f, .3f, .059f),
		/** the break's flying hexes 2B0E / 2B10 (C43): black, blue, black, HDR 2.8 (800EFBD692D3 r0, peak 2.83; game hex #000000 #00D2FF #00A4FF #0059AB #000000). */
		BRIGITTE_BREAK_HEX(0, 0, 0, 0, .25f, 0, .643f, 1, .5f, 0, .369f, 1, .75f, 0, .1f, .405f, 1, 0, 0, 0),
		BRIGITTE_BREAK_HEX_N(0, 0, 0, 0, .25f, 0, .227f, .921f, .5f, 0, .13f, .529f, .75f, 0, .035f, .143f, 1, 0, 0, 0),
		/** the hexes' twin (C55): black, red, black, HDR 3.5 (8001D049C744 r0, peak 3.45; game hex #000000 #FF0000 #FF0000 #BA0000 #000000). */
		BRIGITTE_BREAK_HEX_ENEMY(0, 0, 0, 0, .25f, 1, 0, 0, .5f, 1, 0, 0, .75f, .493f, 0, 0, 1, 0, 0, 0),
		BRIGITTE_BREAK_HEX_ENEMY_N(0, 0, 0, 0, .25f, .921f, 0, 0, .5f, .529f, 0, 0, .75f, .143f, 0, 0, 1, 0, 0, 0),
		/** the bash's flashes 4C09 at hp 0075 / 0077 (clip 5073 effect 26A8 C19): black, gold, white (800A250B7916 r2, peak 1.00; game hex #000000 #D8A92C #FFD144 #FFE596 #FFFFFF). */
		BRIGITTE_BASH_FLASH(0, 0, 0, 0, .25f, .687f, .397f, .025f, .5f, 1, .64f, .058f, .75f, 1, .785f, .307f, 1, 1, 1, 1),
		/** the bash's forward sparks (26A8 C21): red-orange to pale yellow (80017A3FA6B7 r7, peak 1.00; game hex #F72C00 #FA861A #FDB328 #FFD54F #FFF7A4). */
		BRIGITTE_BASH_SPARK(0, 0, 0, 0, .25f, .955f, .237f, .011f, .5f, .98f, .45f, .021f, .75f, 1, .667f, .078f, 1, 1, .93f, .371f),
		/** the dash's speed lines E99F (2207 C24): sky blue to grey, HDR 5, falls (800A2B3AA445 r3, peak 5.00, falls; game hex #48FFFF #40FFFF #E6FFFF #FFFFFF #FFFFFF). */
		BRIGITTE_BASH_SPEED(0, 0, 0, 0, .25f, .052f, 1, 1, .5f, .792f, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 1),
		BRIGITTE_BASH_SPEED_N(0, 0, 0, 0, .25f, .01f, .325f, .799f, .5f, .159f, .335f, .6f, .75f, .278f, .367f, .5f, 1, .4f, .4f, .4f),
		/** the speed lines' twin (2207 C32): red to grey, HDR 5, falls (80080E2A8D89 r3, peak 5.00; game hex #FF9A9A #FF8B8B #FFF4F4 #FFFFFF #FFFFFF). */
		BRIGITTE_BASH_SPEED_ENEMY(0, 0, 0, 0, .25f, 1, .259f, .259f, .5f, 1, .908f, .908f, .75f, 1, 1, 1, 1, 1, 1, 1),
		BRIGITTE_BASH_SPEED_ENEMY_N(0, 0, 0, 0, .25f, .799f, .052f, .052f, .5f, .6f, .182f, .182f, .75f, .5f, .29f, .29f, 1, .4f, .4f, .4f),
		/** the dash ribbon 18F81 (2207 C21): black, magenta, violet, blue (L: a colour row) (800B53313A9C r1, peak 1.00; game hex #000000 #E10063 #A00099 #5600D4 #0000FF). */
		BRIGITTE_BASH_RIBBON(0, 0, 0, 0, .25f, .756f, 0, .126f, .5f, .352f, 0, .319f, .75f, .094f, 0, .657f, 1, 0, 0, 1),
		/** the bash hit's shield imprint 5DF1 (2206 C27): blue, HDR 12 (8003EBE35744 r6, peak 12.00, falls; game hex #FFFFFF #FFFFFF #FFFFFF #FFFFFF #FFFFFF). */
		BRIGITTE_BASH_SHIELD(0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 1),
		BRIGITTE_BASH_SHIELD_N(0, 0, 0, 0, .25f, .143f, .398f, .916f, .5f, .13f, .361f, .832f, .75f, .117f, .326f, .75f, 1, .104f, .289f, .667f),
		/** the imprint's twin (2206 C35): red, HDR 12 (800EB2F3E6F3 r6, peak 12.00, falls; game hex #FFFFFF #FFFFFF #FFFFFF #FFFFFF #FFFFFF). */
		BRIGITTE_BASH_SHIELD_ENEMY(0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 1),
		BRIGITTE_BASH_SHIELD_ENEMY_N(0, 0, 0, 0, .25f, .916f, .143f, .143f, .5f, .832f, .13f, .13f, .75f, .75f, .117f, .117f, 1, .667f, .104f, .104f),
		/** the imprint's edge 15111 (2206 C29): pale blue to navy, HDR 16, falls (800C02263F45 r3, peak 16.00, falls; game hex #FFFFFF #FFFFFF #FFFFFF #FFFFFF #3EDDFF). */
		BRIGITTE_BASH_EDGE(0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, 1, 1, .75f, 1, 1, 1, 1, .048f, .727f, 1),
		BRIGITTE_BASH_EDGE_N(0, 0, 0, 0, .25f, .531f, .685f, .9f, .5f, .353f, .501f, .8f, .75f, .178f, .275f, .502f, 1, .003f, .045f, .188f),
		/** the edge's twin (2206 C37): pale red to dark red, HDR 16, falls (8003B4B67361 r3, peak 16.00, falls; game hex #FFFFFF #FFFFFF #FFFFFF #FFFFFF #FF3E3E). */
		BRIGITTE_BASH_EDGE_ENEMY(0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, 1, 1, .75f, 1, 1, 1, 1, 1, .048f, .048f),
		BRIGITTE_BASH_EDGE_ENEMY_N(0, 0, 0, 0, .25f, .9f, .531f, .531f, .5f, .8f, .353f, .353f, .75f, .502f, .178f, .178f, 1, .188f, .003f, .003f),
		/** the bash hit's streaks 14F9 on B18 (2206 C25): orange to white (80069396B3CB r3, peak 1.00; game hex #FF7D00 #FFCB3A #FFF962 #FFFCC5 #FFFFFF). */
		BRIGITTE_BASH_STREAK(0, 0, 0, 0, .25f, 1, .595f, .042f, .5f, 1, .95f, .123f, .75f, 1, .975f, .558f, 1, 1, 1, 1),
		/** the stun halo 247C on DE0 (220D C12 / C14): dim gold, peak 0.21 (800650C8E74A r0, peak 0.21; game hex #6B5300 #6B7252 #6B7F63 #6B7251 #6B5300). */
		BRIGITTE_STUN_HALO(0, 0, 0, 0, .25f, .147f, .17f, .084f, .5f, .147f, .211f, .125f, .75f, .147f, .169f, .082f, 1, .147f, .087f, 0),
		/** Inspire's ground ring 296B (2182 C4): yellow to cream (800F84A796FC r6, peak 1.00; game hex #FFF45D #FFF271 #FFF394 #FFF7B6 #FFFAD1). */
		BRIGITTE_INSPIRE(0, 0, 0, 0, .25f, 1, .886f, .166f, .5f, 1, .897f, .296f, .75f, 1, .927f, .469f, 1, 1, .956f, .637f),
		/** the ring's other row: cream, olive to dark olive, HDR 20, falls (800F84A796FC r7, peak 20.00, falls; game hex #FFFFFF #FFFFFF #FFFFFF #FFFFDB #FFF5B6). */
		BRIGITTE_INSPIRE_RISE(0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, 1, 1, .75f, 1, 1, .709f, 1, 1, .913f, .468f),
		BRIGITTE_INSPIRE_RISE_N(0, 0, 0, 0, .25f, .176f, .161f, .082f, .5f, .114f, .104f, .053f, .75f, .076f, .069f, .035f, 1, .05f, .046f, .023f),
		/** the pack's trail ribbon 18B18 x 44F7 (218D C13): olive to gold to pale yellow, HDR 6 (80023C1B3EE2 r3, peak 6.00; game hex #827400 #F0B526 #FFFA2B #FFFF27 #FFFFFF). */
		BRIGITTE_PACK_RIBBON(0, 0, 0, 0, .25f, .875f, .462f, .02f, .5f, 1, .954f, .024f, .75f, 1, 1, .02f, 1, 1, 1, 1),
		BRIGITTE_PACK_RIBBON_N(0, 0, 0, 0, .25f, .146f, .077f, .003f, .5f, .264f, .159f, .004f, .75f, .542f, .378f, .003f, 1, 1, .872f, .178f),
		/** the trail's other row: brown-red to orange to cream, HDR 10 (80023C1B3EE2 r4, peak 9.99; game hex #CA351A #FF6310 #FF8600 #FFFF00 #FFFFFF). */
		BRIGITTE_PACK_RIBBON_HOT(0, 0, 0, 0, .25f, 1, .124f, .005f, .5f, 1, .237f, 0, .75f, 1, 1, 0, 1, 1, 1, 1),
		BRIGITTE_PACK_RIBBON_HOT_N(0, 0, 0, 0, .25f, .189f, .012f, .001f, .5f, .316f, .024f, 0, .75f, .636f, .187f, 0, 1, 1, .808f, .655f),
		/** the pack's wide ribbon 18B18 x 18E48 (218D C17): gold to pale yellow, HDR 3 (800E37F7CCCB r2, peak 3.00; game hex #FFFF00 #FFFF00 #FFFF00 #FFFF00 #FFFFC1). */
		BRIGITTE_PACK_TRAIL(0, 0, 0, 0, .25f, 1, 1, 0, .5f, 1, 1, 0, .75f, 1, 1, 0, 1, 1, 1, .533f),
		BRIGITTE_PACK_TRAIL_N(0, 0, 0, 0, .25f, .827f, .611f, 0, .5f, .894f, .676f, 0, .75f, .92f, .707f, 0, 1, 1, .872f, .178f),
		/** the pack's core glow A7B6 (218D C12): olive to cream, HDR 6 (8005CCE53193 r0, peak 6.00; game hex #C5B70D #E1D212 #FFEF17 #FFFF79 #FFFFFF). */
		BRIGITTE_PACK_CORE(0, 0, 0, 0, .25f, .756f, .645f, .006f, .5f, 1, .867f, .009f, .75f, 1, 1, .192f, 1, 1, 1, 1),
		BRIGITTE_PACK_CORE_N(0, 0, 0, 0, .25f, .126f, .108f, .001f, .5f, .169f, .145f, .001f, .75f, .284f, .24f, .032f, 1, 1, .939f, .611f),
		/** the pack's lens streak 23C0 (218D C21): black, gold, cream (8005822073E7 r2, peak 1.00; game hex #000000 #FFC21E #FFE23B #FFF39D #FFFDE5). */
		BRIGITTE_PACK_LENS(0, 0, 0, 0, .25f, 1, .542f, .013f, .5f, 1, .76f, .043f, .75f, 1, .894f, .338f, 1, 1, .982f, .783f),
		/** the arrival's lens 23C0 (2F2E C8): gold to cream (800D212EA4B1 r2, peak 1.00; game hex #BC8E0D #FFC21E #FFE23B #FFF39D #FFFDE5). */
		BRIGITTE_PACK_ARRIVE(0, 0, 0, 0, .25f, 1, .542f, .013f, .5f, 1, .76f, .043f, .75f, 1, .894f, .338f, 1, 1, .982f, .783f),
		/** the arrival's dome flash 5F08 (2F2E C10): cream, yellow, orange, HDR 5 (8009A5973C9E r6, peak 5.00, falls; game hex #FFFFFF #FFFF05 #FFFF19 #FFFF24 #FFFF2D). */
		BRIGITTE_PACK_DOME(0, 0, 0, 0, .25f, 1, 1, .002f, .5f, 1, 1, .01f, .75f, 1, 1, .018f, 1, 1, 1, .026f),
		BRIGITTE_PACK_DOME_N(0, 0, 0, 0, .25f, .871f, .644f, 0, .5f, .914f, .496f, .002f, .75f, .956f, .352f, .004f, 1, 1, .205f, .005f),
		/** the arrival's glow 174D8 (2F2E C12): olive to cream, HDR 6 (80078AB6C344 r1, peak 6.00; game hex #C5B70D #E1D212 #FFEF17 #FFFF79 #FFFFFF). */
		BRIGITTE_PACK_HEAL_GLOW(0, 0, 0, 0, .25f, .756f, .645f, .006f, .5f, 1, .867f, .009f, .75f, 1, 1, .192f, 1, 1, 1, 1),
		BRIGITTE_PACK_HEAL_GLOW_N(0, 0, 0, 0, .25f, .126f, .108f, .001f, .5f, .169f, .145f, .001f, .75f, .284f, .24f, .032f, 1, 1, .939f, .611f),
		/** Rally's dome flash 5F08 (219D C100 / C102): sky blue to gold, HDR 2 (8004E14B20FF r5, peak 2.00, falls; game hex #98F8FF #EEE3FF #FFCC1E #FFCC19 #FFCC19). */
		BRIGITTE_RALLY_DOME(0, 0, 0, 0, .25f, .856f, .769f, 1, .5f, 1, .602f, .013f, .75f, 1, .601f, .01f, 1, 1, .601f, .01f),
		BRIGITTE_RALLY_DOME_N(0, 0, 0, 0, .25f, .428f, .384f, .503f, .5f, .7f, .301f, .006f, .75f, .701f, .301f, .005f, 1, .701f, .301f, .005f),
		/** the dome's twin (219D C98): peach to red, HDR 2 (800F92902535 r5, peak 2.00, falls; game hex #FFFFD0 #FFA366 #FF8456 #FF5941 #FF3232). */
		BRIGITTE_RALLY_DOME_ENEMY(0, 0, 0, 0, .25f, 1, .365f, .132f, .5f, 1, .232f, .092f, .75f, 1, .1f, .052f, 1, 1, .032f, .032f),
		BRIGITTE_RALLY_DOME_ENEMY_N(0, 0, 0, 0, .25f, .997f, .183f, .066f, .5f, .872f, .116f, .046f, .75f, .75f, .05f, .026f, 1, .686f, .016f, .016f),
		/** Rally's dome layers 4DB (219D C120 / C126, 50B0 C84): yellow to dark gold, HDR 5, falls (800C27AA37F7 r6, peak 5.00, falls; game hex #FFFF00 #FFFF00 #FFFF00 #FFFF00 #FFE400). */
		BRIGITTE_RALLY_GOLD(0, 0, 0, 0, .25f, 1, 1, 0, .5f, 1, 1, 0, .75f, 1, 1, 0, 1, 1, .779f, 0),
		BRIGITTE_RALLY_GOLD_N(0, 0, 0, 0, .25f, .874f, .343f, 0, .5f, .713f, .28f, 0, .75f, .553f, .219f, 0, 1, .391f, .156f, 0),
		/** the layers' twin (50B0 C89): orange to dark red, HDR 5, falls (8009EC364E69 r6, peak 5.00, falls; game hex #FFF000 #FF3000 #FF2600 #FF1900 #FF0000). */
		BRIGITTE_RALLY_GOLD_ENEMY(0, 0, 0, 0, .25f, 1, .03f, 0, .5f, 1, .02f, 0, .75f, 1, .01f, 0, 1, 1, 0, 0),
		BRIGITTE_RALLY_GOLD_ENEMY_N(0, 0, 0, 0, .25f, .874f, .006f, 0, .5f, .713f, .004f, 0, .75f, .553f, .002f, 0, 1, .391f, 0, 0),
		/** Rally's armour pattern 144E5 x 14486 on the dome (219D C104): black, gold, black, peak 0.5 (800102BC6092 r0, peak 0.50; game hex #000000 #BB9400 #9D7D00 #5F4900 #010000). */
		BRIGITTE_RALLY_ARMOR(0, 0, 0, 0, .25f, .494f, .297f, 0, .5f, .338f, .205f, 0, .75f, .113f, .068f, 0, 1, 0, 0, 0),
		/** the pattern's twin row: red, HDR 3.5 (800102BC6092 r4, peak 3.50; game hex #FF4646 #FF4646 #FF4646 #FF4646 #FF5252). */
		BRIGITTE_RALLY_ARMOR_ENEMY(0, 0, 0, 0, .25f, 1, .06f, .06f, .5f, 1, .06f, .06f, .75f, 1, .06f, .06f, 1, 1, .085f, .085f),
		BRIGITTE_RALLY_ARMOR_ENEMY_N(0, 0, 0, 0, .25f, .714f, .017f, .017f, .5f, .714f, .017f, .017f, .75f, .714f, .017f, .017f, 1, 1, .024f, .024f),
		/** Rally's bokeh 187EB x 1DC68 (219D / 50B0 C78): orange to amber (800238C77258 r3, peak 1.00; game hex #D28500 #D28500 #D28500 #D58700 #FFA200). */
		BRIGITTE_RALLY_BUBBLES(0, 0, 0, 0, .25f, .644f, .234f, 0, .5f, .644f, .234f, 0, .75f, .663f, .241f, 0, 1, 1, .361f, 0),
		/** the bokeh's twin (50B0 C82): red, HDR 3.5 (80090572CD42 r3, peak 3.50; game hex #FF4646 #FF4646 #FF4646 #FF4646 #FF5252). */
		BRIGITTE_RALLY_BUBBLES_ENEMY(0, 0, 0, 0, .25f, 1, .06f, .06f, .5f, 1, .06f, .06f, .75f, 1, .06f, .06f, 1, 1, .085f, .085f),
		BRIGITTE_RALLY_BUBBLES_ENEMY_N(0, 0, 0, 0, .25f, .714f, .017f, .017f, .5f, .714f, .017f, .017f, .75f, .714f, .017f, .017f, 1, 1, .024f, .024f),
		/** the flail head's flare 2F4F0 (50B0 C66 / C68): brown-gold to white, HDR 2 (800B19CA4E19 r1, peak 2.00; game hex #9A6826 #C88A32 #F3AA3E #FFD14B #FFFFFF). */
		BRIGITTE_RALLY_FLARE(0, 0, 0, 0, .25f, .579f, .256f, .032f, .5f, .894f, .401f, .048f, .75f, 1, .637f, .071f, 1, 1, 1, 1),
		BRIGITTE_RALLY_FLARE_N(0, 0, 0, 0, .25f, .289f, .128f, .016f, .5f, .447f, .201f, .024f, .75f, .624f, .319f, .036f, 1, 1, 1, 1),
		/** the flare's twin (50B0 C70): dark red to pink-white, HDR 3 (800D0C985867 r1, peak 3.00; game hex #9A2626 #CB4B33 #FF6A42 #FF995A #FFFFFF). */
		BRIGITTE_RALLY_FLARE_ENEMY(0, 0, 0, 0, .25f, .595f, .07f, .033f, .5f, 1, .144f, .054f, .75f, 1, .32f, .102f, 1, 1, 1, 1),
		BRIGITTE_RALLY_FLARE_ENEMY_N(0, 0, 0, 0, .25f, .198f, .023f, .011f, .5f, .336f, .048f, .018f, .75f, .58f, .107f, .034f, 1, 1, .351f, .351f),
		/** the flail head's lens streak 23C0 (50B0 C60 / C62): brown-gold to white (800BCA09D3B4 r1, peak 1.00; game hex #9A6826 #C08530 #DE9B38 #F6B140 #FFFFFF). */
		BRIGITTE_RALLY_LENS(0, 0, 0, 0, .25f, .528f, .233f, .029f, .5f, .733f, .328f, .04f, .75f, .923f, .441f, .051f, 1, 1, 1, 1),
		/** the lens's twin (50B0 C64): dark red to pink (80097E0E434B r1, peak 1.00; game hex #9A2626 #C04130 #DE5338 #F66340 #FFA0A0). */
		BRIGITTE_RALLY_LENS_ENEMY(0, 0, 0, 0, .25f, .528f, .053f, .029f, .5f, .733f, .086f, .04f, .75f, .923f, .124f, .051f, 1, 1, .351f, .351f),
		/** the ally's body glow (21DE material C4 / C5): black, brown, amber, gold, HDR 2 (8001131C7DEA r1, peak 2.00; game hex #000000 #A65B00 #FE8D00 #FFDD38 #FFFF86). */
		BRIGITTE_RALLY_ALLY(0, 0, 0, 0, .25f, .383f, .103f, 0, .5f, .99f, .268f, 0, .75f, 1, .723f, .04f, 1, 1, 1, .239f),
		BRIGITTE_RALLY_ALLY_N(0, 0, 0, 0, .25f, .191f, .052f, 0, .5f, .495f, .134f, 0, .75f, .803f, .361f, .02f, 1, 1, .814f, .119f),
		/** the body glow's other row: black, dark red, red, orange, HDR 2 (8001131C7DEA r2, peak 2.00; game hex #000000 #960000 #FF0000 #FF5200 #FF7D00). */
		BRIGITTE_RALLY_ALLY_ENEMY(0, 0, 0, 0, .25f, .304f, 0, 0, .5f, 1, 0, 0, .75f, 1, .085f, 0, 1, 1, .204f, 0),
		BRIGITTE_RALLY_ALLY_ENEMY_N(0, 0, 0, 0, .25f, .152f, 0, 0, .5f, .504f, 0, 0, .75f, .848f, .042f, 0, 1, 1, .102f, 0),
		/** Rally's ground decals 14D09 / 14C71 (220E C1 / C2): gold to amber, HDR 1.25 (800D56B43898 r5, peak 1.25, falls; game hex #FFCD0B #FFC006 #FFB300 #FFAF00 #FFAB00). */
		BRIGITTE_RALLY_DECAL(0, 0, 0, 0, .25f, 1, .53f, .002f, .5f, 1, .451f, 0, .75f, 1, .428f, 0, 1, 1, .406f, 0),
		BRIGITTE_RALLY_DECAL_N(0, 0, 0, 0, .25f, .901f, .425f, .001f, .5f, .999f, .361f, 0, .75f, .952f, .343f, 0, 1, .905f, .325f, 0),
		/** the decal's twin (220E C0): orange to red, HDR 1.5 (80055B2E2BA0 r5, peak 1.50; game hex #E18108 #EB5307 #F55E0B #FF4B0B #FF300F). */
		BRIGITTE_RALLY_DECAL_ENEMY(0, 0, 0, 0, .25f, .834f, .086f, .002f, .5f, .917f, .111f, .003f, .75f, 1, .071f, .003f, 1, 1, .029f, .005f),
		BRIGITTE_RALLY_DECAL_ENEMY_N(0, 0, 0, 0, .25f, .556f, .057f, .001f, .5f, .612f, .074f, .002f, .75f, .667f, .047f, .002f, 1, 1, .019f, .003f),
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
