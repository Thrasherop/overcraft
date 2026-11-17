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
		TEAL(PaletteStops0.TEAL),
		/** The homing ofuda's gold. */
		GOLD(PaletteStops0.GOLD),
		/**
		 * The kunai's and the Kitsune's cyan, white-hot at the top. Its faint end moved toward blue, as TEAL's (game:
		 * (0, .51, .45) at 0.3, (0, 1, .87) at 0.65).
		 */
		CYAN(PaletteStops0.CYAN),
		/** Suzu's burst ring: (0.1, 2, 3) in the game. */
		SUZU_BLUE(PaletteStops0.SUZU_BLUE),
		/**
		 * Lúcio's Healing Boost, amber (colour strip 3F95, tools/reference/lucio-effects.md): #B37401 #FFCD01 #FBF701
		 * #FAFF62 as linear light.
		 */
		HEAL_AMBER(PaletteStops0.HEAL_AMBER),
		/** Lúcio's Speed Boost, green to cyan (strip 3F94): #01A250 #01E767 #0BFF90 #52D8EA. */
		SPEED_GREEN(PaletteStops0.SPEED_GREEN),
		/** Lúcio's team green (strips 35BC / 410E: his rounds, Soundwave, the barrier): #5FD900 #7BEB00 #A2FE00 #F2FE5D white. */
		LUCIO_GREEN(PaletteStops0.LUCIO_GREEN),
		/** Lúcio's round's glow (A3F's core): #9EB243 to #E2FF64, no white top (his team green's white stacked to a white ball). */
		LUCIO_ROUND(PaletteStops0.LUCIO_ROUND),
		// Jetpack Cat's (tools/reference/jetpack_cat-effects.md, from her LUT rows: research\jetpack_cat\work\fx_palettes.txt)
		/** Jetpack Cat's shot core glow (707A / 7079, LUT 800DA2FE r1): gold to white. */
		CAT_CORE(PaletteStops0.CAT_CORE),
		/** Her paw shot's tube trail (80067279 r5, HDR 4.7): gold to white-hot. */
		CAT_TRACER(PaletteStops0.CAT_TRACER),
		/** Her shots' outer glow (8006FF69 r1): blue to cyan. */
		CAT_HALO(PaletteStops0.CAT_HALO),
		/** Her shots' sparks (800D7AF2 r1): cyan-blue to white to yellow. */
		CAT_SPARK(PaletteStops0.CAT_SPARK),
		/** Her shots' flat trail (8004C26D r8): blue to aqua. */
		CAT_RIBBON(PaletteStops0.CAT_RIBBON),
		/** Centre-shot hit flash, gold (709F, 80031F33 r1, from black). */
		CAT_STAMP_GOLD(PaletteStops0.CAT_STAMP_GOLD),
		/** Centre-shot hit flash, blue (709F, 8000708B r1, from black). */
		CAT_STAMP_BLUE(PaletteStops0.CAT_STAMP_BLUE),
		/** The paw print (709F, 8005126D r4, HDR 3.5): gold to white. */
		CAT_PAW_PRINT(PaletteStops0.CAT_PAW_PRINT),
		/** Enemy-hit flash (7077, 8001A030 r5, HDR 6.4): blue to cyan to white. */
		CAT_FLASH_BLUE(PaletteStops0.CAT_FLASH_BLUE),
		/** Heal sparks (7075 / Purr 741D, 800BB7A6 r4, HDR 5): gold to white. */
		CAT_HEAL_GOLD(PaletteStops0.CAT_HEAL_GOLD),
		/** Enemy view of her gold parts (800DA2FE r2): red to orange to white. */
		CAT_ENEMY_HOT(PaletteStops0.CAT_ENEMY_HOT),
		/** Enemy view of her blue parts (8006FF69 r2): red to orange. */
		CAT_ENEMY_ORANGE(PaletteStops0.CAT_ENEMY_ORANGE),
		/** Purr's 10 m sphere (70ED, 800E8940 r6, HDR 6.9): blue to cyan to white. */
		CAT_PURR_BLUE(PaletteStops0.CAT_PURR_BLUE),
		/** Purr's burst lines and nozzle glows (70ED, 800EB0AE r1, from black): gold to white. */
		CAT_PURR_GOLD(PaletteStops0.CAT_PURR_GOLD),
		/** Purr's 6.4 m pulse ring (70F3, 8001990A r8): blue to cyan. */
		CAT_PURR_RING(PaletteStops0.CAT_PURR_RING),
		// Mercy's (tools/reference/mercy-effects.md, the game's LUT rows)
		/** Mercy's heal beam 186F C14 (0.4 m ribbon; 8003A285 r3, HDR 10): gold to pale yellow. */
		MERCY_HEAL_BEAM(PaletteStops0.MERCY_HEAL_BEAM),
		/** The same normalised by its peak: keeps the gold (use this for the beam). */
		MERCY_HEAL_BEAM_N(PaletteStops0.MERCY_HEAL_BEAM_N),
		/** Heal beam 186F C10/C12/C16 (0.2 / 0.1 / 0.2 m; 8006E3CC r3, HDR 5). */
		MERCY_HEAL_BEAM_SOFT(PaletteStops0.MERCY_HEAL_BEAM_SOFT),
		MERCY_HEAL_BEAM_SOFT_N(PaletteStops0.MERCY_HEAL_BEAM_SOFT_N),
		/** Boost beam 1870 C8/C12 (0.15 m; 8000D0E4 r3, HDR 10): blue to sky. */
		MERCY_BOOST_BEAM(PaletteStops0.MERCY_BOOST_BEAM),
		MERCY_BOOST_BEAM_N(PaletteStops0.MERCY_BOOST_BEAM_N),
		/** Boost beam 1870 C10 (0.5 m glow; 8000F404 r3, HDR 15): deep blue to sky. */
		MERCY_BOOST_BEAM_GLOW(PaletteStops0.MERCY_BOOST_BEAM_GLOW),
		MERCY_BOOST_BEAM_GLOW_N(PaletteStops0.MERCY_BOOST_BEAM_GLOW_N),
		/** The staff head while boosting (0197 C18, 800FDFB8 r7, HDR 15): blue to pale cyan. */
		MERCY_STAFF_BLUE(PaletteStops0.MERCY_STAFF_BLUE),
		MERCY_STAFF_BLUE_N(PaletteStops0.MERCY_STAFF_BLUE_N),
		/** The boosted ally's flash (0100 C8, 80080D4D r1, HDR 3): deep blue to sky. */
		MERCY_BOOSTED(PaletteStops0.MERCY_BOOSTED),
		/** Her blaster round (01E1 C21, 80023619 r5): orange to pale yellow. */
		MERCY_SHOT(PaletteStops0.MERCY_SHOT),
		/** Her round as enemies see it (01E1 C27, 800DCC79 r5): dark red to orange. */
		MERCY_SHOT_ENEMY(PaletteStops0.MERCY_SHOT_ENEMY),
		/** Guardian Angel's trail (colour strip 20D0 times the trail's tint (3.26, 3.26, 1.02), clamped). */
		MERCY_GA_TRAIL_T(PaletteStops0.MERCY_GA_TRAIL_T),
		/** Wing glows (GA 17E9 / Angelic Descent 019C, 800B92F6 r4): yellow to pale yellow. */
		MERCY_WING_GLOW(PaletteStops0.MERCY_WING_GLOW),
		/** Wing motes at rest (0301, 80074C3E r5, HDR 3): black to gold to white. */
		MERCY_WING_GOLD(PaletteStops0.MERCY_WING_GOLD),
		MERCY_WING_GOLD_N(PaletteStops0.MERCY_WING_GOLD_N),
		/** Valkyrie's feathers (40CD C19, 8009E3E3 r8, x1.6 in game): gold to pale yellow. */
		MERCY_VALK(PaletteStops0.MERCY_VALK),
		/** Valkyrie's feathers as enemies see them (40CD C23, 800FA2A3 r8, x2.5): dark red to orange. */
		MERCY_VALK_ENEMY(PaletteStops0.MERCY_VALK_ENEMY),
		/** Valkyrie's sparks (1F7B C26, 80060C6F r5): gold to pale yellow. */
		MERCY_VALK_SPARK(PaletteStops0.MERCY_VALK_SPARK),
		/** Valkyrie's sparks, enemy row (80060C6F r6): red to orange. */
		MERCY_VALK_SPARK_ENEMY(PaletteStops0.MERCY_VALK_SPARK_ENEMY),
		/** Resurrect's burst sparks (632D C24, 800948DD r1, x5.4): orange to white. */
		MERCY_RES_SPARK(PaletteStops0.MERCY_RES_SPARK),
		/** Resurrect's glow on the dead ally (2106 C29, 800B7236 r0, x4): black to yellow to white. */
		MERCY_RES_HALO(PaletteStops0.MERCY_RES_HALO),
		/** The same as enemies see it (2106 C35, 80052D9B r0): black to red to peach. */
		MERCY_RES_HALO_ENEMY(PaletteStops0.MERCY_RES_HALO_ENEMY),
		/** The revived ally's rising crosses (019E C7, 800CBECB r2, HDR 5): orange to yellow to white. */
		MERCY_PLUS(PaletteStops0.MERCY_PLUS),
		/** Flash Heal's beam (75A7 C4, 8008E467 r4, HDR 30), normalised: brown-gold to pale yellow. */
		MERCY_FLASH_N(PaletteStops0.MERCY_FLASH_N),
		/** Lifeline's rope core (72FA, 800367D1 r2, HDR 16): blue to cyan. */
		CAT_TETHER(PaletteStops0.CAT_TETHER),
		/** Lifeline's rope as enemies see it (800367D1 r3): red to yellow. */
		CAT_TETHER_ENEMY(PaletteStops0.CAT_TETHER_ENEMY),
		/** Lifeline's hook glow (6EA2 / 71B1 cone, r3): #7FA8FF to white. */
		CAT_HOOK(PaletteStops0.CAT_HOOK),
		/** The hook's bubbles (71B1 C9, 80047234 r3, HDR 3.6), normalised: deep blue to cyan. */
		CAT_BUBBLE(PaletteStops0.CAT_BUBBLE),
		/** The towed ally's arc flash (7454 C6, 800EFD5D r6 #42FFFF), its brightness the mask's: black to cyan. */
		CAT_ARC_FLASH(PaletteStops0.CAT_ARC_FLASH),
		/** The towed ally's arc lines (7454 C8, 8007D524 r8): slate blue to white . */
		CAT_ARC_LINES(PaletteStops0.CAT_ARC_LINES),
		/** Lifeline's paw marker decal (7432, 800333C5 r3): blue to cyan. */
		CAT_MARKER(PaletteStops0.CAT_MARKER),
		/** Jetpack flame core (7064, 8004B7CE r7, HDR 11): blue to cyan to white. */
		CAT_FLAME(PaletteStops0.CAT_FLAME),
		/** Jetpack outer flame (7064, 800DCB53 r7, from black): gold to pale yellow. */
		CAT_FLAME_GOLD(PaletteStops0.CAT_FLAME_GOLD),
		/** Frenetic Flight's ribbons (7067, 80048E45 r3): cyan behind a white head (the head is the row's x = 0). */
		CAT_BOOST(PaletteStops0.CAT_BOOST),
		/** Frenetic Flight's ribbons as enemies see them (80048E45 r4): orange. */
		CAT_BOOST_ENEMY(PaletteStops0.CAT_BOOST_ENEMY),
		/** Catnapper's target rings (71B3, 80044C8A r4): cyan to blue. */
		CAT_NAP_MARKER(PaletteStops0.CAT_NAP_MARKER),
		/** Catnapper's landing flash (71C2, 8006A53D r0, from black, HDR 7): violet-pink to sky to cyan. */
		CAT_NAP_IMPACT(PaletteStops0.CAT_NAP_IMPACT),
		// the rest of #59 (CatPalettes: the enum's initializer is at the class file's limit)
		/** The jetpack's outer flame (7064 C24, 80063381 r7): deep blue to teal. */
		CAT_FLAME_TEAL(CatPalettes.CAT_FLAME_TEAL),
		/** The Catnapper cast's bell (7373 C37, 8007DA82 r7, from black): gold to pale yellow. */
		CAT_BELL_GOLD(CatPalettes.CAT_BELL_GOLD),
		/** The cast's and the dive's exhaust rings (7373 C18 / 7163 C73, 80038B2E r4, HDR 4.65): blue to cyan. */
		CAT_RING_CYAN(CatPalettes.CAT_RING_CYAN),
		/** The same rings' gold pair (7373 C21 / 7163 C84, 800BD8BA r4, HDR 3.4): yellow to white. */
		CAT_RING_GOLD(CatPalettes.CAT_RING_GOLD),
		/** The head slash's arcs (71B8 C8, 8005B765 r8, HDR 17): cyan to white. */
		CAT_SLASH(CatPalettes.CAT_SLASH),
		/** The same, the enemy row (8005B765 r9): red to white. */
		CAT_SLASH_ENEMY(CatPalettes.CAT_SLASH_ENEMY),
		/** The head slash's domes (71B8 C10, 8007E2E6 r7, HDR 6.2): sky to cyan to white. */
		CAT_DOME(CatPalettes.CAT_DOME),
		/** The inner dome (71B8 C12, 8000B7F8 r6, HDR 3): deep blue to cyan. */
		CAT_DOME_DEEP(CatPalettes.CAT_DOME_DEEP),
		/** Both domes' enemy rows (8007E2E6 r8 / 8000B7F8 r5): red to white. */
		CAT_DOME_ENEMY(CatPalettes.CAT_DOME_ENEMY),
		/** The landing's ground flash (71C2 C19, 800C2FC5 r3): navy to deep blue. */
		CAT_NAP_GROUND(CatPalettes.CAT_NAP_GROUND),
		/** The same, the enemy row (800C2FC5 r4): maroon to red. */
		CAT_NAP_GROUND_ENEMY(CatPalettes.CAT_NAP_GROUND_ENEMY),
		/** Transport Shielding's hexes (5513 C3, 800D0BB1 r3): teal to cyan. */
		CAT_HEX(CatPalettes.CAT_HEX),
		/** Claws Out's marks (74D0 C13, 8006D576 r4, from black): teal to pale cyan. */
		CAT_CLAWS(CatPalettes.CAT_CLAWS),
		/** The same, the enemy row (8006D576 r3): dark red to pale pink. */
		CAT_CLAWS_ENEMY(CatPalettes.CAT_CLAWS_ENEMY),
		/** Claws Out's sparks (74D0 C15, 800D0510 r1, from black): navy to cyan. */
		CAT_CLAW_SPARK(CatPalettes.CAT_CLAW_SPARK),
		/** The same, the enemy row (800D0510 r0): red to yellow. */
		CAT_CLAW_SPARK_ENEMY(CatPalettes.CAT_CLAW_SPARK_ENEMY),
		/** The landing hit's sphere (7256 C13, 800C7EF8 r5, HDR 4.8): blue to cyan. */
		CAT_SHOCK(CatPalettes.CAT_SHOCK),
		/** The landing hit's flame burst (7256 C17, 800CFAE7 r3, HDR 22): blue to sky to cyan. */
		CAT_SHOCK_FLAME(CatPalettes.CAT_SHOCK_FLAME),
		/** The same, the enemy row (800CFAE7 r4): dark red to orange. */
		CAT_SHOCK_FLAME_ENEMY(CatPalettes.CAT_SHOCK_FLAME_ENEMY),
		/** The landing hit's sparks (7256 C21, 8008B3B1 r1, from black): violet to blue to white. */
		CAT_SHOCK_SPARK(CatPalettes.CAT_SHOCK_SPARK),
		/** The dive's thin tube trail (7163 C57, 8009F6ED r3, HDR 3.6): white head, cyan. */
		CAT_DIVE_CYAN(CatPalettes.CAT_DIVE_CYAN),
		/** The dive's middle tube trail (7163 C61, 800C202B r3, HDR 2.7): cyan to blue. */
		CAT_DIVE_BLUE(CatPalettes.CAT_DIVE_BLUE),
		/** The dive's wide tube trail (7163 C65, 8000C8DF r3, HDR 2.7): white head, gold. */
		CAT_DIVE_GOLD(CatPalettes.CAT_DIVE_GOLD),
		/** The dive trails' enemy rows (r4): white head, orange. */
		CAT_DIVE_ENEMY(CatPalettes.CAT_DIVE_ENEMY),
		/** The dive's strip trails (7163 C103, 8004F990 r3, HDR 2.7): blue to cyan. */
		CAT_DIVE_STRIP(CatPalettes.CAT_DIVE_STRIP),
		/** The jetpack's and the dive's gold sparks (7064 C44 / 7163 C92, r1): gold to pale yellow. */
		CAT_SPARK_GOLD(CatPalettes.CAT_SPARK_GOLD),
		/** The jetpack's pale sparks (7064 C47 / 7163 C113, 800A522D r1): slate to pale blue. */
		CAT_SPARK_PALE(CatPalettes.CAT_SPARK_PALE),
		/** The jetpack sparks' enemy row (800D5B12 r2): red to pale pink. */
		CAT_SPARK_ENEMY(CatPalettes.CAT_SPARK_ENEMY),
		/** The paw print's second set (709F C56, 800751FB r4, HDR 2.4): deep blue to white. */
		CAT_PAW_PRINT_BLUE(CatPalettes.CAT_PAW_PRINT_BLUE),
		// Lifeweaver's (tools/reference/lifeweaver-effects.md "Palettes", generated from his LUT rows)
		/** His hex sparkles 3EB7D (8005B0D22D52 r0, HDR 5): magenta-pink to white. */
		LW_PINK(PaletteStops0.LW_PINK),
		LW_PINK_N(PaletteStops0.LW_PINK_N),
		/** The same, as enemies see them (r1): black to red to white. */
		LW_PINK_ENEMY_N(PaletteStops0.LW_PINK_ENEMY_N),
		/** Gold hex sparkles (800CF57A9184 r0, HDR 5): the blossom's side. */
		LW_GOLD(PaletteStops0.LW_GOLD),
		LW_GOLD_N(PaletteStops0.LW_GOLD_N),
		/** The charging blossom's glows 0242 (30DE C33 / C37): pale gold, amber. */
		LW_BLOSSOM_GLOW(PaletteStops0.LW_BLOSSOM_GLOW),
		LW_BLOSSOM_AMBER(PaletteStops0.LW_BLOSSOM_AMBER),
		/** Petals 3FD63 (8007F2189D11 r3, HDR 4; the row falls: bright at mask 0). */
		LW_PETAL_N(PaletteStops0.LW_PETAL_N),
		/** The petals' over-life tint (800A664BB547 r0, 12BB): cream to pink to crimson; use by age, not by mask. */
		LW_PETAL_AGE_N(PaletteStops0.LW_PETAL_AGE_N),
		/** Life Grip's vine 3110 (80072BB6FABB r3, HDR 20): lilac-pink to white. */
		LW_VINE_N(PaletteStops0.LW_VINE_N),
		/** The vine's coil 12262 (800B8E14B83B r3, HDR 5): magenta to white. */
		LW_VINE_COIL_N(PaletteStops0.LW_VINE_COIL_N),
		/** The blossom's flight trail 30D0 C25 (80098E9C7FFE r3, HDR 10): black to gold to white. */
		LW_BLOSSOM_TRAIL(PaletteStops0.LW_BLOSSOM_TRAIL),
		LW_BLOSSOM_TRAIL_N(PaletteStops0.LW_BLOSSOM_TRAIL_N),
		/** Gold ring flashes 2E8AF (800C725A38EE r0, HDR 2): heal, fire, tree heal, seed pickup. */
		LW_HEAL_RING_N(PaletteStops0.LW_HEAL_RING_N),
		/** The tree's sprout / pulse sphere shells (800EF2EBAB76 r9, HDR 9.7): black to gold to pale yellow. */
		LW_TREE_WAVE_N(PaletteStops0.LW_TREE_WAVE_N),
		LW_TREE_WAVE_ENEMY_N(PaletteStops0.LW_TREE_WAVE_ENEMY_N),
		/** The pulse's ground ring (8004D4075AFE r3, HDR 2) and the flower-band wall (8001794386A1 r3, HDR 2). */
		LW_TREE_RING_N(PaletteStops0.LW_TREE_RING_N),
		LW_TREE_WALL_N(PaletteStops0.LW_TREE_WALL_N),
		/** The thorn 122CA (8001A48833A6 r2, HDR 30): purple-magenta to white. */
		LW_THORN_N(PaletteStops0.LW_THORN_N),
		LW_THORN_ENEMY_N(PaletteStops0.LW_THORN_ENEMY_N),
		/** The thorn's tracer (8003062BE28B r3, HDR 12; falls: white at 0 to black at 1, reversed by age, M). */
		LW_THORN_TRAIL_N(PaletteStops0.LW_THORN_TRAIL_N),
		/** The thorn's muzzle flash (80082222A07E r0) and hit burst (8009F205934A r0). */
		LW_THORN_FLASH(PaletteStops0.LW_THORN_FLASH),
		LW_THORN_HIT(PaletteStops0.LW_THORN_HIT),
		/** The platform's throw and rise trails (800EA70DA27E r2, HDR 5): pink to white; enemy r3. */
		LW_PLATFORM_TRAIL_N(PaletteStops0.LW_PLATFORM_TRAIL_N),
		LW_PLATFORM_TRAIL_ENEMY_N(PaletteStops0.LW_PLATFORM_TRAIL_ENEMY_N),
		/** The platform's bloom outline 122DA (8008CBB37314 r2, HDR 5): mauve to white. */
		LW_PLATFORM_FLOWER_N(PaletteStops0.LW_PLATFORM_FLOWER_N),
		/** The raised platform's petal-tip glow (8009496E3E46 r0) and cup (8003C2A25A98 r5; enemy r6). */
		LW_PETAL_GLOW(PaletteStops0.LW_PETAL_GLOW),
		LW_PETAL_CUP(PaletteStops0.LW_PETAL_CUP),
		LW_PETAL_CUP_ENEMY(PaletteStops0.LW_PETAL_CUP_ENEMY),
		/** The dash ribbons (800955CE7D8A r2, HDR 3, falls): white to cream gold (L which row). */
		LW_DASH_N(PaletteStops0.LW_DASH_N),
		/** As the enemy sees her effects. */
		ENEMY(PaletteStops0.ENEMY),
		/** The mask as it is. */
		WHITE(PaletteStops0.WHITE),
		// Ana's (tools/reference/ana-effects.md "Palettes", generated from her colour rows)
		/** Her rifle's muzzle bulb 963D (800F41F7 r5, HDR 2): gold to white. */
		ANA_MUZZLE(PaletteStops0.ANA_MUZZLE),
		ANA_MUZZLE_N(PaletteStops0.ANA_MUZZLE_N),
		/** The unscoped shot's trail: strip 9414 (sampled) x the gold tint (5, 3.99, 1.66), normalised. */
		ANA_SHOT_TRAIL(PaletteStops0.ANA_SHOT_TRAIL),
		/** The scoped tracer (80008917 r4, HDR 2): gold to cream. */
		ANA_TRACER(PaletteStops0.ANA_TRACER),
		ANA_TRACER_N(PaletteStops0.ANA_TRACER_N),
		/** The ally hit's burst 4B64D (800AB80B r1, HDR 2.8), glow 4C09 and sparks (800342A1 r1): pale gold, amber. */
		ANA_HEAL_BURST_N(PaletteStops0.ANA_HEAL_BURST_N),
		ANA_HEAL_GLOW(PaletteStops0.ANA_HEAL_GLOW),
		/** The enemy hit's lens discs (8009CD1E r4, HDR 10) and bubbles (800025BA r3): purple, violet. */
		ANA_HARM_N(PaletteStops0.ANA_HARM_N),
		ANA_HARM_BUBBLE(PaletteStops0.ANA_HARM_BUBBLE),
		/** The shot's impact sparkle 64B91 (800C00E6 r4, HDR 7, falls) and the world decals (800E8A1D / 8007CA88 r2). */
		ANA_IMPACT_N(PaletteStops0.ANA_IMPACT_N),
		ANA_DECAL_GOLD_N(PaletteStops0.ANA_DECAL_GOLD_N),
		/** The grenade's trail (8006AB57 r4): black to violet to gold. */
		ANA_GRENADE_TRAIL(PaletteStops0.ANA_GRENADE_TRAIL),
		/** The burst's gold dome (80079014 r5), violet-yellow shell (8000C715 r2), splashes (8009749B / 80029CEC r4). */
		ANA_BURST_DOME(PaletteStops0.ANA_BURST_DOME),
		ANA_BURST_SHELL(PaletteStops0.ANA_BURST_SHELL),
		ANA_SPLASH_N(PaletteStops0.ANA_SPLASH_N),
		ANA_SPLASH_GOLD_N(PaletteStops0.ANA_SPLASH_GOLD_N),
		/** The burst's droplets (80085D4D r1, HDR 10 / 800280E0 r1), splat decal (80024EDE r1, HDR 10), glow (800F4D2E r0). */
		ANA_DROP_GOLD_N(PaletteStops0.ANA_DROP_GOLD_N),
		ANA_DROP_PURPLE(PaletteStops0.ANA_DROP_PURPLE),
		ANA_BURST_DECAL(PaletteStops0.ANA_BURST_DECAL),
		ANA_BURST_GLOW(PaletteStops0.ANA_BURST_GLOW),
		/** The heal boost's plus (80091108 r4) and the anti-heal's broken plus (800533DC r1, falls). */
		ANA_BOOST_PLUS(PaletteStops0.ANA_BOOST_PLUS),
		ANA_ANTIHEAL(PaletteStops0.ANA_ANTIHEAL),
		/** The sleep dart's glow (80034914 r1, HDR 10), the Zs over life (80053871 r3, 12BB, HDR 1.3), the sleep sparks over life (800277D9 r0, HDR 5.7, falls). */
		ANA_SLEEP_GLOW_N(PaletteStops0.ANA_SLEEP_GLOW_N),
		ANA_SLEEP_Z(PaletteStops0.ANA_SLEEP_Z),
		ANA_SLEEP_SPARK(PaletteStops0.ANA_SLEEP_SPARK),
		/** Local Anesthetic's dome (8008512C r0), gas (800861DA r7), drops (8005EE9F r3), the numbed wisps (800747DE r1, HDR 2). */
		ANA_LA_DOME(PaletteStops0.ANA_LA_DOME),
		ANA_LA_GAS(PaletteStops0.ANA_LA_GAS),
		ANA_LA_DROP(PaletteStops0.ANA_LA_DROP),
		ANA_LA_WISP_N(PaletteStops0.ANA_LA_WISP_N),
		/** Nano Boost: the ally's shells (8000CCD7 r4, Mercy's boost blue), flames (80007283 r5, HDR 6), sparks (800DAD00 r1), the beam (80049B54 r2, HDR 3), the launch puffs over life (800AE47A r0). */
		ANA_NANO_BLUE(PaletteStops0.ANA_NANO_BLUE),
		ANA_NANO_FLAME_N(PaletteStops0.ANA_NANO_FLAME_N),
		ANA_NANO_BEAM(PaletteStops0.ANA_NANO_BEAM),
		ANA_NANO_BEAM_N(PaletteStops0.ANA_NANO_BEAM_N),
		ANA_NANO_MUZZLE(PaletteStops0.ANA_NANO_MUZZLE),
		// ---- Sombra (tools/reference/sombra-effects.md; research\sombra\workx_palettes.txt): her team's rows, the enemies' _ENEMY twins ----
		/** Cyberspace's trail ribbon (502E C25): violet to pink, HDR 5 (800B665E3BD4 r3, peak 5.00; game hex #B800FF #FF51FF #FF71FF #FF96FF #FFB7FF). */
		SOMBRA_CYBER_TRAIL(PaletteStops0.SOMBRA_CYBER_TRAIL),
		SOMBRA_CYBER_TRAIL_N(PaletteStops0.SOMBRA_CYBER_TRAIL_N),
		/** the trail's twin row: red-orange to pale yellow, HDR 5 (800B665E3BD4 r4, peak 5.00; game hex #FF1200 #FF8051 #FFAE71 #FFE596 #FFFFB7). */
		SOMBRA_CYBER_TRAIL_ENEMY(PaletteStops0.SOMBRA_CYBER_TRAIL_ENEMY),
		SOMBRA_CYBER_TRAIL_ENEMY_N(PaletteStops0.SOMBRA_CYBER_TRAIL_ENEMY_N),
		/** the flying data cube 13EE4 (502E C28): violet to pale pink (8008C0FCC358 r1, peak 1.00; game hex #1E061E #B30CD4 #E475FA #F1B8FD #FEE5FF). */
		SOMBRA_CYBER_CUBE(PaletteStops0.SOMBRA_CYBER_CUBE),
		/** the cube's twin row: rust to white (8008C0FCC358 r2, peak 1.00; game hex #1E0606 #A84513 #DA694B #FA9E90 #FAFAFF). */
		SOMBRA_CYBER_CUBE_ENEMY(PaletteStops0.SOMBRA_CYBER_CUBE_ENEMY),
		/** the zone's 11.8 m sphere 1385 (7C7E C72): purple, peak 0.76 (800462B4FD6C r3, peak 0.76; game hex #9D0CA6 #AE0FB8 #BD11C7 #CA13D5 #D615E2). */
		SOMBRA_CYBER_SPHERE(PaletteStops0.SOMBRA_CYBER_SPHERE),
		/** the sphere's twin row: dim brown-red, peak 0.16 (800462B4FD6C r4, peak 0.16; game hex #511A15 #5A1E18 #63211C #6A251E #712721). */
		SOMBRA_CYBER_SPHERE_ENEMY(PaletteStops0.SOMBRA_CYBER_SPHERE_ENEMY),
		/** the zone's code streaks 9BDF (7C7E C38): brown to gold, HDR 5 (800F2B087BEC r7, peak 5.03; game hex #DC6000 #FFC622 #FFFF45 #FFFF5B #FFFFFF). */
		SOMBRA_CYBER_GOLD(PaletteStops0.SOMBRA_CYBER_GOLD),
		SOMBRA_CYBER_GOLD_N(PaletteStops0.SOMBRA_CYBER_GOLD_N),
		/** the streaks' twin row: brown to orange, HDR 5 (800F2B087BEC r8, peak 5.03; game hex #DC1500 #FF6F22 #FFC845 #FFFE5B #FFFFFF). */
		SOMBRA_CYBER_ORANGE(PaletteStops0.SOMBRA_CYBER_ORANGE),
		SOMBRA_CYBER_ORANGE_N(PaletteStops0.SOMBRA_CYBER_ORANGE_N),
		/** the zone's circuit sphere D72C (7C7E C58): dark gold to cream, HDR 2.7 (80050EECEE60 r9, peak 2.71; game hex #412600 #D29100 #FFC325 #FFED7D #FFFFE0). */
		SOMBRA_CYBER_CIRCUIT(PaletteStops0.SOMBRA_CYBER_CIRCUIT),
		SOMBRA_CYBER_CIRCUIT_N(PaletteStops0.SOMBRA_CYBER_CIRCUIT_N),
		/** the circuit sphere's twin row: plum to orange, HDR 3.8 (80050EECEE60 r10, peak 3.79; game hex #7E0068 #A00085 #FF7383 #FFB96E #FFF200). */
		SOMBRA_CYBER_CIRCUIT_ENEMY(PaletteStops0.SOMBRA_CYBER_CIRCUIT_ENEMY),
		SOMBRA_CYBER_CIRCUIT_ENEMY_N(PaletteStops0.SOMBRA_CYBER_CIRCUIT_ENEMY_N),
		/** the zone's rising plus 3DC7 (7C7E C74): gold to pale yellow, HDR 3 (8001FA6F94F5 r2, peak 3.00; game hex #DE9A00 #FFDB21 #FFFF93 #FFFFB5 #FFFFFF). */
		SOMBRA_CYBER_PLUS(PaletteStops0.SOMBRA_CYBER_PLUS),
		SOMBRA_CYBER_PLUS_N(PaletteStops0.SOMBRA_CYBER_PLUS_N),
		/** the zone's ground decal 7B847 (7C7E C65): violet to cream, HDR 3 (800032683E86 r4, peak 3.00; game hex #340048 #D28965 #FFFFD8 #FFFFFF #FFFFFF). */
		SOMBRA_CYBER_DECAL(PaletteStops0.SOMBRA_CYBER_DECAL),
		SOMBRA_CYBER_DECAL_N(PaletteStops0.SOMBRA_CYBER_DECAL_N),
		/** the burst's flare A76F (7CDB C17): violet to pink, HDR 3 (8008CA52628C r1, peak 3.00; game hex #50007D #D500FF #FF5AFF #FFB1FF #FFE5FF). */
		SOMBRA_CYBER_BURST(PaletteStops0.SOMBRA_CYBER_BURST),
		SOMBRA_CYBER_BURST_N(PaletteStops1.SOMBRA_CYBER_BURST_N),
		/** the burst flare's twin row: rust to salmon, HDR 4.4 (8008CA52628C r2, peak 4.43; game hex #000000 #DE6B46 #FF9462 #FFC986 #FFFFD7). */
		SOMBRA_CYBER_BURST_ENEMY(PaletteStops1.SOMBRA_CYBER_BURST_ENEMY),
		SOMBRA_CYBER_BURST_ENEMY_N(PaletteStops1.SOMBRA_CYBER_BURST_ENEMY_N),
		/** the weakened enemy's arrows 66E4 (7AE1 C14): violet to pink, HDR 3 (80051C68041B r2, peak 3.00; game hex #8E00E2 #A933D0 #CF55A9 #FF777E #FFFDFF). */
		SOMBRA_WEAKENED(PaletteStops1.SOMBRA_WEAKENED),
		SOMBRA_WEAKENED_N(PaletteStops1.SOMBRA_WEAKENED_N),
		/** the arrows' other row: dark red to salmon, HDR 3 (80051C68041B r4, peak 3.00; game hex #8E0200 #AB0900 #F75035 #FF8B61 #FFB17D). */
		SOMBRA_WEAKENED_ALT(PaletteStops1.SOMBRA_WEAKENED_ALT),
		SOMBRA_WEAKENED_ALT_N(PaletteStops1.SOMBRA_WEAKENED_ALT_N),
		/** the weakened enemy's falling chevrons DFDC (7AE1 C24): black to pink (80017FEBBB59 r3, peak 1.00; game hex #000000 #AF334F #DB667F #F7839D #FF92AD). */
		SOMBRA_WEAKENED_CHEVRON(PaletteStops1.SOMBRA_WEAKENED_CHEVRON),
		/** Hotfix's strands to an ally (7C6E C9): brown-gold to cream, HDR 3.9 (8006FB0C3C19 r3, peak 3.89; game hex #DE9A19 #E59F1A #FFD256 #FFFFA1 #FFFFFF). */
		SOMBRA_HOTFIX_STRAND(PaletteStops1.SOMBRA_HOTFIX_STRAND),
		SOMBRA_HOTFIX_STRAND_N(PaletteStops1.SOMBRA_HOTFIX_STRAND_N),
		/** the strands' twin (7C6E C13): brown-red to peach, HDR 3.9 (8005C557A361 r2, peak 3.89; game hex #DE4119 #E5431A #FF7C56 #FFD09E #FFFFFF). */
		SOMBRA_HOTFIX_STRAND_ENEMY(PaletteStops1.SOMBRA_HOTFIX_STRAND_ENEMY),
		SOMBRA_HOTFIX_STRAND_ENEMY_N(PaletteStops1.SOMBRA_HOTFIX_STRAND_ENEMY_N),
		/** Hotfix's strands to a hacked object (1B32 C21): indigo to lilac, HDR 3.9 (800C55D7C70A r3, peak 3.89; game hex #5719DE #5A1AE5 #9656FF #F7A1FF #FFFFFF). */
		SOMBRA_HACK_STRAND(PaletteStops1.SOMBRA_HACK_STRAND),
		SOMBRA_HACK_STRAND_N(PaletteStops1.SOMBRA_HACK_STRAND_N),
		/** the hack strands' twin (1B32 C23): dark red to pink, HDR 3.9 (800B4A29B50C r3, peak 3.89; game hex #DE1919 #E51A1A #FF5D56 #FFA99D #FFFFFF). */
		SOMBRA_HACK_STRAND_ENEMY(PaletteStops1.SOMBRA_HACK_STRAND_ENEMY),
		SOMBRA_HACK_STRAND_ENEMY_N(PaletteStops1.SOMBRA_HACK_STRAND_ENEMY_N),
		/** the healed ally's code wall A512 (6570 C29): brown to gold, HDR 5 (8003744D4E14 r7, peak 5.03; game hex #DC6000 #FFC622 #FFFF45 #FFFF5B #FFFFFF). */
		SOMBRA_HOTFIX_HEAL(PaletteStops1.SOMBRA_HOTFIX_HEAL),
		SOMBRA_HOTFIX_HEAL_N(PaletteStops1.SOMBRA_HOTFIX_HEAL_N),
		/** the code wall's twin row: brown to orange, HDR 5 (8003744D4E14 r8, peak 5.03; game hex #DC1500 #FF6F22 #FFC845 #FFFE5B #FFFFFF). */
		SOMBRA_HOTFIX_HEAL_ENEMY(PaletteStops1.SOMBRA_HOTFIX_HEAL_ENEMY),
		SOMBRA_HOTFIX_HEAL_ENEMY_N(PaletteStops1.SOMBRA_HOTFIX_HEAL_ENEMY_N),
		/** the healed ally's body glow A7B6 (6570 C37): amber to gold, HDR 4 (80087210BF0E r1, peak 4.00; game hex #000000 #F69419 #FFD70C #FFFF5F #FFFFA6). */
		SOMBRA_HOTFIX_GLOW(PaletteStops1.SOMBRA_HOTFIX_GLOW),
		SOMBRA_HOTFIX_GLOW_N(PaletteStops1.SOMBRA_HOTFIX_GLOW_N),
		/** the beacon's ribbons (1B0F C8): indigo to violet to white, HDR 2.45 (800265C1ADAE r3, peak 2.45; game hex #100033 #FF0BFF #FFBEFF #FFFFFF #FFFFFF). */
		SOMBRA_BEACON_TRAIL(PaletteStops1.SOMBRA_BEACON_TRAIL),
		SOMBRA_BEACON_TRAIL_N(PaletteStops1.SOMBRA_BEACON_TRAIL_N),
		/** the beacon ribbons' twin row: brown to orange-red to white (800048EE6A61 r4, peak 2.45; game hex #331000 #FF480B #FFCFBE #FFFFFF #FFFFFF). */
		SOMBRA_BEACON_TRAIL_ENEMY(PaletteStops1.SOMBRA_BEACON_TRAIL_ENEMY),
		SOMBRA_BEACON_TRAIL_ENEMY_N(PaletteStops1.SOMBRA_BEACON_TRAIL_ENEMY_N),
		/** the teleport's flare 29F7 (1A76 / 5FE0 C17): indigo to violet to white (800FCD048FF0 r1, peak 1.00; game hex #310063 #DB2AF8 #E89EFF #EFD8FF #FFFFFF). */
		SOMBRA_TELEPORT(PaletteStops1.SOMBRA_TELEPORT),
		/** the teleport flare's twin row: indigo to coral to white (800FCD048FF0 r2, peak 1.00; game hex #310063 #F75033 #FFA79E #FFDBD8 #FFFFFF). */
		SOMBRA_TELEPORT_ENEMY(PaletteStops1.SOMBRA_TELEPORT_ENEMY),
		/** her body flash at the teleport (1B1E / 3683 material): lilac to purple, HDR 12.6, falls (800C31BA6BBA r1, peak 12.63, falls; game hex #FFFFFF #FFFFFF #FFE1FF #FF9CFF #FF00FF). */
		SOMBRA_TELEPORT_BODY(PaletteStops1.SOMBRA_TELEPORT_BODY),
		SOMBRA_TELEPORT_BODY_N(PaletteStops1.SOMBRA_TELEPORT_BODY_N),
		/** the body flash's twin row: coral to brown, HDR 12.5, falls (800C31BA6BBA r2, peak 12.54, falls; game hex #FFFFFA #FFFFA3 #FFF573 #FFDA49 #FFBF01). */
		SOMBRA_TELEPORT_BODY_ENEMY(PaletteStops1.SOMBRA_TELEPORT_BODY_ENEMY),
		SOMBRA_TELEPORT_BODY_ENEMY_N(PaletteStops1.SOMBRA_TELEPORT_BODY_ENEMY_N),
		/** the stealth detection ring 129B (4DD9 C3): black to violet to white, HDR 20 (800561F03EEF r3, peak 19.98; game hex #1F0335 #E509FF #FF0DFF #FF05FF #FFFFFF). */
		SOMBRA_DETECT_RING(PaletteStops1.SOMBRA_DETECT_RING),
		SOMBRA_DETECT_RING_N(PaletteStops1.SOMBRA_DETECT_RING_N),
		/** 256F's glow streaks 242 (C23): violet to white (800C48520539 r1, peak 1.00; game hex #C300FF #913EFF #9C8DFF #D5D0FF #FFFFFF). */
		SOMBRA_BEACON_LAND(PaletteStops1.SOMBRA_BEACON_LAND),
		/** Her team's strip A06C: the effects reading's report (tools/reference/sombra-effects.md), sRGB to linear. */
		SOMBRA_PURPLE(PaletteStops1.SOMBRA_PURPLE),
		/** Her team's strip A06B: the effects reading's report (tools/reference/sombra-effects.md), sRGB to linear. */
		SOMBRA_PURPLE_DARK(PaletteStops1.SOMBRA_PURPLE_DARK),
		/** Their twin, strip ADD0: the effects reading's report (tools/reference/sombra-effects.md), sRGB to linear. */
		SOMBRA_ENEMY_RED(PaletteStops1.SOMBRA_ENEMY_RED),
		/** The hacked skull's strip A720 (#651B84 > #BDAAFF; the middle ours): the effects reading's report (tools/reference/sombra-effects.md), sRGB to linear. */
		SOMBRA_SKULL(PaletteStops1.SOMBRA_SKULL),
		/** The pistol's muzzle strip A260, magenta to gold: the effects reading's report (tools/reference/sombra-effects.md), sRGB to linear. */
		SOMBRA_MUZZLE(PaletteStops1.SOMBRA_MUZZLE),
		/** D.Va's matrix tube 2D61: tint (0.05, 1, 3) both teams (dva-effects.md, H), scaled to its brightest; the white top ours. */
		DVA_MATRIX_TUBE(HeroPalettes.DVA_MATRIX_TUBE),
		/** Its ring frames and pulses: team tint (0.14, 2.5, 1.3), scaled (H). */
		DVA_MATRIX(HeroPalettes.DVA_MATRIX),
		/** The same for an enemy D.Va: (4, 0.38, 0.085), scaled (H). */
		DVA_MATRIX_ENEMY(HeroPalettes.DVA_MATRIX_ENEMY),
		/** The absorb beam and flash 1506 / 1507: team A35B row 3, #70FFB5 (H). */
		DVA_ABSORB(HeroPalettes.DVA_ABSORB),
		/** The same for an enemy: F56B row 3, #FFAC70 (H). */
		DVA_ABSORB_ENEMY(HeroPalettes.DVA_ABSORB_ENEMY),
		/** D.Va's Self-Destruct, team and enemy (HeroPalettes). */
		DVA_SD(HeroPalettes.DVA_SD),
		DVA_SD_ENEMY(HeroPalettes.DVA_SD_ENEMY),
		/** Illari's (HeroPalettes). */
		IL_SUN(HeroPalettes.IL_SUN),
		IL_FIRE(HeroPalettes.IL_FIRE),
		IL_SHOT(HeroPalettes.IL_SHOT),
		IL_MARK(HeroPalettes.IL_MARK),
		IL_SCORCH(HeroPalettes.IL_SCORCH),
		/** Tracer's muzzle flash (HeroPalettes). */
		TR_MUZZLE(HeroPalettes.TR_MUZZLE),
		TR_MUZZLE_ENEMY(HeroPalettes.TR_MUZZLE_ENEMY),
		/** D.Va's Micro Missiles: flame, smoke, hit (HeroPalettes). */
		DVA_MISSILE(HeroPalettes.DVA_MISSILE),
		/** D.Va's Fusion Cannons and Boosters (HeroPalettes). */
		DVA_FUSION(HeroPalettes.DVA_FUSION),
		DVA_FUSION_ENEMY(HeroPalettes.DVA_FUSION_ENEMY),
		DVA_BOOST(HeroPalettes.DVA_BOOST),
		DVA_BOOST_ENEMY(HeroPalettes.DVA_BOOST_ENEMY),
		DVA_MISSILE_ENEMY(HeroPalettes.DVA_MISSILE_ENEMY),
		DVA_SMOKE(HeroPalettes.DVA_SMOKE),
		DVA_MISSILE_HIT(HeroPalettes.DVA_MISSILE_HIT),
		DVA_MISSILE_HIT_ENEMY(HeroPalettes.DVA_MISSILE_HIT_ENEMY),
		/**
		 * Tracer's shot line 002C (tools/reference/tracer-effects.md): her team row, LUT 0967 row 4, a flat HDR (1.78, 2.58, 4.0), here
		 * scaled to its brightest channel; the white top stands in for the HDR's bloom (ours).
		 */
		TR_SHOT(HeroPalettes.TR_SHOT),
		/** Tracer's Blink / Recall and her Pulse Bomb, team and enemy (HeroPalettes). */
		TR_BLINK(HeroPalettes.TR_BLINK),
		TR_BLINK_ENEMY(HeroPalettes.TR_BLINK_ENEMY),
		TR_BOMB(HeroPalettes.TR_BOMB),
		TR_BOMB_ENEMY(HeroPalettes.TR_BOMB_ENEMY),
		/** The same for an enemy Tracer: LUT CDAD row 4, a flat HDR (3.2, 0.62, 0.33), scaled the same way. */
		TR_SHOT_ENEMY(HeroPalettes.TR_SHOT_ENEMY),
		/** The pistol's tracer 1998, warm orange #FF6236 (normalised; the ends ours): the effects reading's report (tools/reference/sombra-effects.md), sRGB to linear. */
		SOMBRA_TRACER(PaletteStops1.SOMBRA_TRACER),
		/** Her heals' gold LUT row (Hotfix's healed ally, Cyberspace's allies): the effects reading's report (tools/reference/sombra-effects.md), sRGB to linear. */
		SOMBRA_HEAL_GOLD(PaletteStops1.SOMBRA_HEAL_GOLD),
		// Brigitte's (tools/reference/brigitte-effects.md, researchrigitte\workx_palettes.txt)
		/** the flail swing arc 5F3F (3P clip 4E9E effect 26EB C17): black, amber, tan, sky blue, HDR 2 (800A3EC1BF96 r6, peak 2.00; game hex #000000 #FDBC0E #FFE7AE #8DD5FF #CAFFFF). */
		BRIGITTE_SWING(PaletteStops1.BRIGITTE_SWING),
		BRIGITTE_SWING_N(PaletteStops1.BRIGITTE_SWING_N),
		/** the swing arc's twin (26EB C15): black, red, orange, pale gold, HDR 2 (80061D37D65D r6, peak 2.00; game hex #000000 #FD210E #FF6327 #FFB146 #FFFFCA). */
		BRIGITTE_SWING_ENEMY(PaletteStops1.BRIGITTE_SWING_ENEMY),
		BRIGITTE_SWING_ENEMY_N(PaletteStops1.BRIGITTE_SWING_ENEMY_N),
		/** the 1P swing arc 5D48 (clip 4F53 effect 2624 C9): the same gold-to-blue row (800D213EBF90 r7, peak 2.00; game hex #000000 #FDBC0E #FFE7AE #8DD5FF #CAFFFF). */
		BRIGITTE_SWING_1P(PaletteStops1.BRIGITTE_SWING_1P),
		BRIGITTE_SWING_1P_N(PaletteStops1.BRIGITTE_SWING_1P_N),
		/** the 3P chain ribbon 13DB9 (216C / 22CF C2): olive to pale yellow, HDR 10 (8007A4D04894 r1, peak 9.99; game hex #814300 #BD9C00 #FFFF06 #FFFFFF #FFFFFF). */
		BRIGITTE_CHAIN(PaletteStops1.BRIGITTE_CHAIN),
		BRIGITTE_CHAIN_N(PaletteStops1.BRIGITTE_CHAIN_N),
		/** the chain ribbon's twin row: dark red to pink-white, HDR 10 (8007A4D04894 r0, peak 9.99; game hex #810000 #E30000 #FF5948 #FFFFDD #FFFFFF). */
		BRIGITTE_CHAIN_ENEMY(PaletteStops1.BRIGITTE_CHAIN_ENEMY),
		BRIGITTE_CHAIN_ENEMY_N(PaletteStops1.BRIGITTE_CHAIN_ENEMY_N),
		/** the energy chain 33C1 / 33C2 and 1P swing ribbon (29D41 x 19167): navy to cyan, HDR 3.5 (800134B6C381 r4, peak 3.50; game hex #0E4585 #8023F5 #25C9FF #3CFFFF #FFFFFF). */
		BRIGITTE_ENERGY_BLUE(PaletteStops1.BRIGITTE_ENERGY_BLUE),
		BRIGITTE_ENERGY_BLUE_N(PaletteStops1.BRIGITTE_ENERGY_BLUE_N),
		/** the energy chain's other row: brown to gold to cream, HDR 3.5 (800134B6C381 r6, peak 3.50; game hex #7A150C #F6951F #FF8E25 #FFC539 #FFFFFF). */
		BRIGITTE_ENERGY_GOLD(PaletteStops1.BRIGITTE_ENERGY_GOLD),
		BRIGITTE_ENERGY_GOLD_N(PaletteStops1.BRIGITTE_ENERGY_GOLD_N),
		/** the swing's ground hit 220B dust 14D1C (C9 / C11 tint row): red to gold, HDR 1.5 (80046EF0C935 r0, peak 1.46; game hex #FF0000 #FF8300 #FFB300 #FFD600 #FFF400). */
		BRIGITTE_GROUND_DUST(PaletteStops1.BRIGITTE_GROUND_DUST),
		BRIGITTE_GROUND_DUST_N(PaletteStops1.BRIGITTE_GROUND_DUST_N),
		/** the whip head's sparks 4C09 (clip 5276 effect 2770 C19): red-orange to cream (800FD0A96386 r7, peak 1.00; game hex #F72C00 #FB9A20 #FFCE30 #FFE47B #FFF7A4). */
		BRIGITTE_WHIP_SPARK(PaletteStops1.BRIGITTE_WHIP_SPARK),
		/** the whip sparks' twin (2770 C23): red-orange to peach (800BEB9E3BB8 r7, peak 1.00; game hex #F72C00 #FB5E20 #FF7B30 #FFB07B #FFD7A4). */
		BRIGITTE_WHIP_SPARK_ALT(PaletteStops1.BRIGITTE_WHIP_SPARK_ALT),
		/** the whip head's smoke DFCB (2770 C38): grey-brown to cream, HDR 1.3 (800112076AB5 r4, peak 1.31; game hex #6C635D #A4988F #D8CDC1 #FEF5E5 #FFFFFF). */
		BRIGITTE_WHIP_SMOKE(PaletteStops1.BRIGITTE_WHIP_SMOKE),
		BRIGITTE_WHIP_SMOKE_N(PaletteStops1.BRIGITTE_WHIP_SMOKE_N),
		/** the barrier field material 5B2C (look 13917) LUT row 0: (dim red at 0) navy, blue, cyan; alpha 0.08 -> 1 (800E79730886 r0, peak 0.93; game hex #AD1E2F #852549 #353C75 #1785BF #17E0F7). */
		BRIGITTE_BARRIER(PaletteStops1.BRIGITTE_BARRIER),
		/** the field's row 1: dark red, red, orange, white (800E79730886 r1, peak 1.00; game hex #4E1F1E #B42E09 #F33900 #FFA200 #FFFEFF). */
		BRIGITTE_BARRIER_ENEMY(PaletteStops1.BRIGITTE_BARRIER_ENEMY),
		/** the break's shield silhouette 5DF1 (268C C45): cream to blue, HDR 4, falls (800D2E31E3D1 r2, peak 4.00, falls; game hex #FFFFFF #FFFFFF #C4FFFF #88F7FF #59DFFF). */
		BRIGITTE_BREAK_SHIELD(PaletteStops1.BRIGITTE_BREAK_SHIELD),
		BRIGITTE_BREAK_SHIELD_N(PaletteStops1.BRIGITTE_BREAK_SHIELD_N),
		/** the silhouette's twin (C51): cream to red, HDR 4, falls (80094DD0A453 r2, peak 4.00, falls; game hex #FFFFFF #FFFFFF #FFCCC4 #FF8C88 #FF5959). */
		BRIGITTE_BREAK_SHIELD_ENEMY(PaletteStops1.BRIGITTE_BREAK_SHIELD_ENEMY),
		BRIGITTE_BREAK_SHIELD_ENEMY_N(PaletteStops1.BRIGITTE_BREAK_SHIELD_ENEMY_N),
		/** the break's glow 17C0 on 5DF1 (C47): periwinkle to cyan, HDR 5 (8005E0AB817E r0, peak 5.00, falls; game hex #FFFFFF #BFF5FF #4CFFFF #83FFFF #94FFFF). */
		BRIGITTE_BREAK_GLOW(PaletteStops1.BRIGITTE_BREAK_GLOW),
		BRIGITTE_BREAK_GLOW_N(PaletteStops1.BRIGITTE_BREAK_GLOW_N),
		/** the glow's twin (C53): salmon to orange, HDR 5 (8001358106BB r0, peak 5.00, falls; game hex #FFFFFF #FFBFBF #FFA34C #FFFF83 #FFFF94). */
		BRIGITTE_BREAK_GLOW_ENEMY(PaletteStops1.BRIGITTE_BREAK_GLOW_ENEMY),
		BRIGITTE_BREAK_GLOW_ENEMY_N(PaletteStops1.BRIGITTE_BREAK_GLOW_ENEMY_N),
		/** the break's flying hexes 2B0E / 2B10 (C43): black, blue, black, HDR 2.8 (800EFBD692D3 r0, peak 2.83; game hex #000000 #00D2FF #00A4FF #0059AB #000000). */
		BRIGITTE_BREAK_HEX(PaletteStops1.BRIGITTE_BREAK_HEX),
		BRIGITTE_BREAK_HEX_N(PaletteStops1.BRIGITTE_BREAK_HEX_N),
		/** the hexes' twin (C55): black, red, black, HDR 3.5 (8001D049C744 r0, peak 3.45; game hex #000000 #FF0000 #FF0000 #BA0000 #000000). */
		BRIGITTE_BREAK_HEX_ENEMY(PaletteStops1.BRIGITTE_BREAK_HEX_ENEMY),
		BRIGITTE_BREAK_HEX_ENEMY_N(PaletteStops1.BRIGITTE_BREAK_HEX_ENEMY_N),
		/** the bash's flashes 4C09 at hp 0075 / 0077 (clip 5073 effect 26A8 C19): black, gold, white (800A250B7916 r2, peak 1.00; game hex #000000 #D8A92C #FFD144 #FFE596 #FFFFFF). */
		BRIGITTE_BASH_FLASH(PaletteStops1.BRIGITTE_BASH_FLASH),
		/** the bash's forward sparks (26A8 C21): red-orange to pale yellow (80017A3FA6B7 r7, peak 1.00; game hex #F72C00 #FA861A #FDB328 #FFD54F #FFF7A4). */
		BRIGITTE_BASH_SPARK(PaletteStops1.BRIGITTE_BASH_SPARK),
		/** the dash's speed lines E99F (2207 C24): sky blue to grey, HDR 5, falls (800A2B3AA445 r3, peak 5.00, falls; game hex #48FFFF #40FFFF #E6FFFF #FFFFFF #FFFFFF). */
		BRIGITTE_BASH_SPEED(PaletteStops1.BRIGITTE_BASH_SPEED),
		BRIGITTE_BASH_SPEED_N(PaletteStops1.BRIGITTE_BASH_SPEED_N),
		/** the speed lines' twin (2207 C32): red to grey, HDR 5, falls (80080E2A8D89 r3, peak 5.00; game hex #FF9A9A #FF8B8B #FFF4F4 #FFFFFF #FFFFFF). */
		BRIGITTE_BASH_SPEED_ENEMY(PaletteStops1.BRIGITTE_BASH_SPEED_ENEMY),
		BRIGITTE_BASH_SPEED_ENEMY_N(PaletteStops1.BRIGITTE_BASH_SPEED_ENEMY_N),
		/** the dash ribbon 18F81 (2207 C21): black, magenta, violet, blue (L: a colour row) (800B53313A9C r1, peak 1.00; game hex #000000 #E10063 #A00099 #5600D4 #0000FF). */
		BRIGITTE_BASH_RIBBON(PaletteStops1.BRIGITTE_BASH_RIBBON),
		/** the bash hit's shield imprint 5DF1 (2206 C27): blue, HDR 12 (8003EBE35744 r6, peak 12.00, falls; game hex #FFFFFF #FFFFFF #FFFFFF #FFFFFF #FFFFFF). */
		BRIGITTE_BASH_SHIELD(PaletteStops1.BRIGITTE_BASH_SHIELD),
		BRIGITTE_BASH_SHIELD_N(PaletteStops1.BRIGITTE_BASH_SHIELD_N),
		/** the imprint's twin (2206 C35): red, HDR 12 (800EB2F3E6F3 r6, peak 12.00, falls; game hex #FFFFFF #FFFFFF #FFFFFF #FFFFFF #FFFFFF). */
		BRIGITTE_BASH_SHIELD_ENEMY(PaletteStops1.BRIGITTE_BASH_SHIELD_ENEMY),
		BRIGITTE_BASH_SHIELD_ENEMY_N(PaletteStops1.BRIGITTE_BASH_SHIELD_ENEMY_N),
		/** the imprint's edge 15111 (2206 C29): pale blue to navy, HDR 16, falls (800C02263F45 r3, peak 16.00, falls; game hex #FFFFFF #FFFFFF #FFFFFF #FFFFFF #3EDDFF). */
		BRIGITTE_BASH_EDGE(PaletteStops1.BRIGITTE_BASH_EDGE),
		BRIGITTE_BASH_EDGE_N(PaletteStops1.BRIGITTE_BASH_EDGE_N),
		/** the edge's twin (2206 C37): pale red to dark red, HDR 16, falls (8003B4B67361 r3, peak 16.00, falls; game hex #FFFFFF #FFFFFF #FFFFFF #FFFFFF #FF3E3E). */
		BRIGITTE_BASH_EDGE_ENEMY(PaletteStops1.BRIGITTE_BASH_EDGE_ENEMY),
		BRIGITTE_BASH_EDGE_ENEMY_N(PaletteStops1.BRIGITTE_BASH_EDGE_ENEMY_N),
		/** the bash hit's streaks 14F9 on B18 (2206 C25): orange to white (80069396B3CB r3, peak 1.00; game hex #FF7D00 #FFCB3A #FFF962 #FFFCC5 #FFFFFF). */
		BRIGITTE_BASH_STREAK(PaletteStops1.BRIGITTE_BASH_STREAK),
		/** the stun halo 247C on DE0 (220D C12 / C14): dim gold, peak 0.21 (800650C8E74A r0, peak 0.21; game hex #6B5300 #6B7252 #6B7F63 #6B7251 #6B5300). */
		BRIGITTE_STUN_HALO(PaletteStops1.BRIGITTE_STUN_HALO),
		/** Inspire's ground ring 296B (2182 C4): yellow to cream (800F84A796FC r6, peak 1.00; game hex #FFF45D #FFF271 #FFF394 #FFF7B6 #FFFAD1). */
		BRIGITTE_INSPIRE(PaletteStops1.BRIGITTE_INSPIRE),
		/** the ring's other row: cream, olive to dark olive, HDR 20, falls (800F84A796FC r7, peak 20.00, falls; game hex #FFFFFF #FFFFFF #FFFFFF #FFFFDB #FFF5B6). */
		BRIGITTE_INSPIRE_RISE(PaletteStops1.BRIGITTE_INSPIRE_RISE),
		BRIGITTE_INSPIRE_RISE_N(PaletteStops1.BRIGITTE_INSPIRE_RISE_N),
		/** the pack's trail ribbon 18B18 x 44F7 (218D C13): olive to gold to pale yellow, HDR 6 (80023C1B3EE2 r3, peak 6.00; game hex #827400 #F0B526 #FFFA2B #FFFF27 #FFFFFF). */
		BRIGITTE_PACK_RIBBON(PaletteStops1.BRIGITTE_PACK_RIBBON),
		BRIGITTE_PACK_RIBBON_N(PaletteStops1.BRIGITTE_PACK_RIBBON_N),
		/** the trail's other row: brown-red to orange to cream, HDR 10 (80023C1B3EE2 r4, peak 9.99; game hex #CA351A #FF6310 #FF8600 #FFFF00 #FFFFFF). */
		BRIGITTE_PACK_RIBBON_HOT(PaletteStops1.BRIGITTE_PACK_RIBBON_HOT),
		BRIGITTE_PACK_RIBBON_HOT_N(PaletteStops1.BRIGITTE_PACK_RIBBON_HOT_N),
		/** the pack's wide ribbon 18B18 x 18E48 (218D C17): gold to pale yellow, HDR 3 (800E37F7CCCB r2, peak 3.00; game hex #FFFF00 #FFFF00 #FFFF00 #FFFF00 #FFFFC1). */
		BRIGITTE_PACK_TRAIL(PaletteStops1.BRIGITTE_PACK_TRAIL),
		BRIGITTE_PACK_TRAIL_N(PaletteStops1.BRIGITTE_PACK_TRAIL_N),
		/** the pack's core glow A7B6 (218D C12): olive to cream, HDR 6 (8005CCE53193 r0, peak 6.00; game hex #C5B70D #E1D212 #FFEF17 #FFFF79 #FFFFFF). */
		BRIGITTE_PACK_CORE(PaletteStops1.BRIGITTE_PACK_CORE),
		BRIGITTE_PACK_CORE_N(PaletteStops1.BRIGITTE_PACK_CORE_N),
		/** the pack's lens streak 23C0 (218D C21): black, gold, cream (8005822073E7 r2, peak 1.00; game hex #000000 #FFC21E #FFE23B #FFF39D #FFFDE5). */
		BRIGITTE_PACK_LENS(PaletteStops1.BRIGITTE_PACK_LENS),
		/** the arrival's lens 23C0 (2F2E C8): gold to cream (800D212EA4B1 r2, peak 1.00; game hex #BC8E0D #FFC21E #FFE23B #FFF39D #FFFDE5). */
		BRIGITTE_PACK_ARRIVE(PaletteStops1.BRIGITTE_PACK_ARRIVE),
		/** the arrival's dome flash 5F08 (2F2E C10): cream, yellow, orange, HDR 5 (8009A5973C9E r6, peak 5.00, falls; game hex #FFFFFF #FFFF05 #FFFF19 #FFFF24 #FFFF2D). */
		BRIGITTE_PACK_DOME(PaletteStops1.BRIGITTE_PACK_DOME),
		BRIGITTE_PACK_DOME_N(PaletteStops1.BRIGITTE_PACK_DOME_N),
		/** the arrival's glow 174D8 (2F2E C12): olive to cream, HDR 6 (80078AB6C344 r1, peak 6.00; game hex #C5B70D #E1D212 #FFEF17 #FFFF79 #FFFFFF). */
		BRIGITTE_PACK_HEAL_GLOW(PaletteStops1.BRIGITTE_PACK_HEAL_GLOW),
		BRIGITTE_PACK_HEAL_GLOW_N(PaletteStops1.BRIGITTE_PACK_HEAL_GLOW_N),
		/** Rally's dome flash 5F08 (219D C100 / C102): sky blue to gold, HDR 2 (8004E14B20FF r5, peak 2.00, falls; game hex #98F8FF #EEE3FF #FFCC1E #FFCC19 #FFCC19). */
		BRIGITTE_RALLY_DOME(PaletteStops1.BRIGITTE_RALLY_DOME),
		BRIGITTE_RALLY_DOME_N(PaletteStops1.BRIGITTE_RALLY_DOME_N),
		/** the dome's twin (219D C98): peach to red, HDR 2 (800F92902535 r5, peak 2.00, falls; game hex #FFFFD0 #FFA366 #FF8456 #FF5941 #FF3232). */
		BRIGITTE_RALLY_DOME_ENEMY(PaletteStops1.BRIGITTE_RALLY_DOME_ENEMY),
		BRIGITTE_RALLY_DOME_ENEMY_N(PaletteStops1.BRIGITTE_RALLY_DOME_ENEMY_N),
		/** Rally's dome layers 4DB (219D C120 / C126, 50B0 C84): yellow to dark gold, HDR 5, falls (800C27AA37F7 r6, peak 5.00, falls; game hex #FFFF00 #FFFF00 #FFFF00 #FFFF00 #FFE400). */
		BRIGITTE_RALLY_GOLD(PaletteStops1.BRIGITTE_RALLY_GOLD),
		BRIGITTE_RALLY_GOLD_N(PaletteStops1.BRIGITTE_RALLY_GOLD_N),
		/** the layers' twin (50B0 C89): orange to dark red, HDR 5, falls (8009EC364E69 r6, peak 5.00, falls; game hex #FFF000 #FF3000 #FF2600 #FF1900 #FF0000). */
		BRIGITTE_RALLY_GOLD_ENEMY(PaletteStops1.BRIGITTE_RALLY_GOLD_ENEMY),
		BRIGITTE_RALLY_GOLD_ENEMY_N(PaletteStops1.BRIGITTE_RALLY_GOLD_ENEMY_N),
		/** Rally's armour pattern 144E5 x 14486 on the dome (219D C104): black, gold, black, peak 0.5 (800102BC6092 r0, peak 0.50; game hex #000000 #BB9400 #9D7D00 #5F4900 #010000). */
		BRIGITTE_RALLY_ARMOR(PaletteStops1.BRIGITTE_RALLY_ARMOR),
		/** the pattern's twin row: red, HDR 3.5 (800102BC6092 r4, peak 3.50; game hex #FF4646 #FF4646 #FF4646 #FF4646 #FF5252). */
		BRIGITTE_RALLY_ARMOR_ENEMY(PaletteStops1.BRIGITTE_RALLY_ARMOR_ENEMY),
		BRIGITTE_RALLY_ARMOR_ENEMY_N(PaletteStops1.BRIGITTE_RALLY_ARMOR_ENEMY_N),
		/** Rally's bokeh 187EB x 1DC68 (219D / 50B0 C78): orange to amber (800238C77258 r3, peak 1.00; game hex #D28500 #D28500 #D28500 #D58700 #FFA200). */
		BRIGITTE_RALLY_BUBBLES(PaletteStops1.BRIGITTE_RALLY_BUBBLES),
		/** the bokeh's twin (50B0 C82): red, HDR 3.5 (80090572CD42 r3, peak 3.50; game hex #FF4646 #FF4646 #FF4646 #FF4646 #FF5252). */
		BRIGITTE_RALLY_BUBBLES_ENEMY(PaletteStops1.BRIGITTE_RALLY_BUBBLES_ENEMY),
		BRIGITTE_RALLY_BUBBLES_ENEMY_N(PaletteStops1.BRIGITTE_RALLY_BUBBLES_ENEMY_N),
		/** the flail head's flare 2F4F0 (50B0 C66 / C68): brown-gold to white, HDR 2 (800B19CA4E19 r1, peak 2.00; game hex #9A6826 #C88A32 #F3AA3E #FFD14B #FFFFFF). */
		BRIGITTE_RALLY_FLARE(PaletteStops1.BRIGITTE_RALLY_FLARE),
		BRIGITTE_RALLY_FLARE_N(PaletteStops1.BRIGITTE_RALLY_FLARE_N),
		/** the flare's twin (50B0 C70): dark red to pink-white, HDR 3 (800D0C985867 r1, peak 3.00; game hex #9A2626 #CB4B33 #FF6A42 #FF995A #FFFFFF). */
		BRIGITTE_RALLY_FLARE_ENEMY(PaletteStops1.BRIGITTE_RALLY_FLARE_ENEMY),
		BRIGITTE_RALLY_FLARE_ENEMY_N(PaletteStops1.BRIGITTE_RALLY_FLARE_ENEMY_N),
		/** the flail head's lens streak 23C0 (50B0 C60 / C62): brown-gold to white (800BCA09D3B4 r1, peak 1.00; game hex #9A6826 #C08530 #DE9B38 #F6B140 #FFFFFF). */
		BRIGITTE_RALLY_LENS(PaletteStops1.BRIGITTE_RALLY_LENS),
		/** the lens's twin (50B0 C64): dark red to pink (80097E0E434B r1, peak 1.00; game hex #9A2626 #C04130 #DE5338 #F66340 #FFA0A0). */
		BRIGITTE_RALLY_LENS_ENEMY(PaletteStops1.BRIGITTE_RALLY_LENS_ENEMY),
		/** the ally's body glow (21DE material C4 / C5): black, brown, amber, gold, HDR 2 (8001131C7DEA r1, peak 2.00; game hex #000000 #A65B00 #FE8D00 #FFDD38 #FFFF86). */
		BRIGITTE_RALLY_ALLY(PaletteStops1.BRIGITTE_RALLY_ALLY),
		BRIGITTE_RALLY_ALLY_N(PaletteStops1.BRIGITTE_RALLY_ALLY_N),
		/** the body glow's other row: black, dark red, red, orange, HDR 2 (8001131C7DEA r2, peak 2.00; game hex #000000 #960000 #FF0000 #FF5200 #FF7D00). */
		BRIGITTE_RALLY_ALLY_ENEMY(PaletteStops1.BRIGITTE_RALLY_ALLY_ENEMY),
		BRIGITTE_RALLY_ALLY_ENEMY_N(PaletteStops1.BRIGITTE_RALLY_ALLY_ENEMY_N),
		/** Rally's ground decals 14D09 / 14C71 (220E C1 / C2): gold to amber, HDR 1.25 (800D56B43898 r5, peak 1.25, falls; game hex #FFCD0B #FFC006 #FFB300 #FFAF00 #FFAB00). */
		BRIGITTE_RALLY_DECAL(PaletteStops1.BRIGITTE_RALLY_DECAL),
		BRIGITTE_RALLY_DECAL_N(PaletteStops1.BRIGITTE_RALLY_DECAL_N),
		/** the decal's twin (220E C0): orange to red, HDR 1.5 (80055B2E2BA0 r5, peak 1.50; game hex #E18108 #EB5307 #F55E0B #FF4B0B #FF300F). */
		BRIGITTE_RALLY_DECAL_ENEMY(PaletteStops1.BRIGITTE_RALLY_DECAL_ENEMY),
		BRIGITTE_RALLY_DECAL_ENEMY_N(PaletteStops1.BRIGITTE_RALLY_DECAL_ENEMY_N),
		// Juno's (tools/reference/juno-effects.md, research/juno/work/fx_palettes.txt; the _N rows normalised by their peak)
		/** Mediblaster muzzle torus 15A26 (51DA C29): sky blue then yellow, peak 1 (800C4E2EE0F8 r3, peak 1.00, falls; game hex #9BD6FF #FFE746 #FFE746 #FFE746 #FFE746). */
		JUNO_MEDI_MUZZLE(PaletteStops1.JUNO_MEDI_MUZZLE),
		/** the muzzle torus's twin row: peach to red, HDR 5 (800C4E2EE0F8 r4, peak 5.00, falls; game hex #FFFFFF #FFFFFF #FFD0D0 #FF9999 #FF0000). */
		JUNO_MEDI_MUZZLE_ENEMY(PaletteStops1.JUNO_MEDI_MUZZLE_ENEMY),
		JUNO_MEDI_MUZZLE_ENEMY_N(PaletteStops1.JUNO_MEDI_MUZZLE_ENEMY_N),
		/** muzzle flare 37A79 (51DA C33 / C35): blue then yellow, HDR 2 (8003770F99FD r0, peak 2.00; game hex #0088FF #0088FF #0088FF #FFFFB6 #FFFFB6). */
		JUNO_MEDI_FLASH(PaletteStops1.JUNO_MEDI_FLASH),
		JUNO_MEDI_FLASH_N(PaletteStops1.JUNO_MEDI_FLASH_N),
		/** the flare's twin: red then peach, HDR 2 (8003770F99FD r1, peak 2.00; game hex #FF0000 #FF0000 #FF0000 #FFE5B6 #FFE5B6). */
		JUNO_MEDI_FLASH_ENEMY(PaletteStops1.JUNO_MEDI_FLASH_ENEMY),
		JUNO_MEDI_FLASH_ENEMY_N(PaletteStops1.JUNO_MEDI_FLASH_ENEMY_N),
		/** the round in flight (53B6 C10, strips 3A512 x 47DD9): navy, olive, gold, HDR 5 (80096596474B r3, peak 5.00; game hex #000307 #0057B9 #FFF736 #FFFF43 #FFFF69). */
		JUNO_MEDI_TRACER(PaletteStops1.JUNO_MEDI_TRACER),
		JUNO_MEDI_TRACER_N(PaletteStops1.JUNO_MEDI_TRACER_N),
		/** the round's twin row: dark red, red, orange, HDR 1.7 (80096596474B r4, peak 1.69; game hex #000307 #B30103 #F22E0A #FF7F2B #FFAA3D). */
		JUNO_MEDI_TRACER_ENEMY(PaletteStops1.JUNO_MEDI_TRACER_ENEMY),
		JUNO_MEDI_TRACER_ENEMY_N(PaletteStops1.JUNO_MEDI_TRACER_ENEMY_N),
		/** the round's core (53B6 C8): black, olive, gold, HDR 20 (800D4E226EAA r3, peak 19.98; game hex #000307 #FFFF44 #FFFF53 #FFFF94 #FFFFC7). */
		JUNO_MEDI_TRACER_CORE(PaletteStops1.JUNO_MEDI_TRACER_CORE),
		JUNO_MEDI_TRACER_CORE_N(PaletteStops2.JUNO_MEDI_TRACER_CORE_N),
		/** hit on an ally (58B4 C6, 1CB5E x 4DB): black to cyan, HDR 2 (800A50ADFEBC r0, peak 2.00; game hex #000000 #00BCBC #06FFFF #42FFFF #5DFFFF). */
		JUNO_MEDI_ALLY_HIT(PaletteStops2.JUNO_MEDI_ALLY_HIT),
		JUNO_MEDI_ALLY_HIT_N(PaletteStops2.JUNO_MEDI_ALLY_HIT_N),
		/** the ally hit's glow 266F (58B4 C8): pale yellow to olive, HDR 3.1, falls (800B693E275E r1, peak 3.14, falls; game hex #FFFFE8 #FFFFD2 #FBFD8B #FEF08B #FFEB8B). */
		JUNO_MEDI_ALLY_HIT_GLOW(PaletteStops2.JUNO_MEDI_ALLY_HIT_GLOW),
		JUNO_MEDI_ALLY_HIT_GLOW_N(PaletteStops2.JUNO_MEDI_ALLY_HIT_GLOW_N),
		/** hit on the world: decal (53B7 C1, 1CB5E x 266F): black to pale yellow, HDR 2 (800ADA9F68B9 r4, peak 2.00; game hex #000000 #FFFFC1 #FFFFD8 #FFFFEC #FFFFF8). */
		JUNO_MEDI_WORLD(PaletteStops2.JUNO_MEDI_WORLD),
		JUNO_MEDI_WORLD_N(PaletteStops2.JUNO_MEDI_WORLD_N),
		/** the decal's twin: black to pink, HDR 2 (800ADA9F68B9 r5, peak 2.00; game hex #000000 #FFC1C1 #FFD8D8 #FFECEC #FFF8F8). */
		JUNO_MEDI_WORLD_ENEMY(PaletteStops2.JUNO_MEDI_WORLD_ENEMY),
		JUNO_MEDI_WORLD_ENEMY_N(PaletteStops2.JUNO_MEDI_WORLD_ENEMY_N),
		/** reload sparks torus 15A26 (60BA C9): black, steel blue, cyan, HDR 10 (800CFFC2B92C r3, peak 9.99; game hex #000000 #DBFFFF #FFFFFF #FFFFFF #FFFFFF). */
		JUNO_RELOAD_SPARK(PaletteStops2.JUNO_RELOAD_SPARK),
		JUNO_RELOAD_SPARK_N(PaletteStops2.JUNO_RELOAD_SPARK_N),
		/** reload ribbon (60BA C23, 299E3 x 4C003): black, violet, blue, HDR 5 (800218C59433 r4, peak 5.00; game hex #000000 #7A46C0 #847BF4 #A1C1FF #F0FFFF). */
		JUNO_RELOAD_RIBBON(PaletteStops2.JUNO_RELOAD_RIBBON),
		JUNO_RELOAD_RIBBON_N(PaletteStops2.JUNO_RELOAD_RIBBON_N),
		/** the reload ribbon's twin: black, dark red, white, HDR 5 (800218C59433 r5, peak 5.00; game hex #000000 #AD4D4D #FF7878 #FFEEC1 #FFFFFF). */
		JUNO_RELOAD_RIBBON_ENEMY(PaletteStops2.JUNO_RELOAD_RIBBON_ENEMY),
		JUNO_RELOAD_RIBBON_ENEMY_N(PaletteStops2.JUNO_RELOAD_RIBBON_ENEMY_N),
		/** pods charging (5254 C25, 37C36): black, slate, olive, white, HDR 6 (80089955364C r1, peak 6.00; game hex #000000 #B2D5F2 #FFFF6E #FFFFFF #FFFFFF). */
		JUNO_POD_GLOW(PaletteStops2.JUNO_POD_GLOW),
		JUNO_POD_GLOW_N(PaletteStops2.JUNO_POD_GLOW_N),
		/** the pod glow's twin: black, rust, orange, cream, HDR 4.9 (80089955364C r2, peak 4.88; game hex #000000 #FF411E #FFAA4B #FFFFEC #FFFFFF). */
		JUNO_POD_GLOW_ENEMY(PaletteStops2.JUNO_POD_GLOW_ENEMY),
		JUNO_POD_GLOW_ENEMY_N(PaletteStops2.JUNO_POD_GLOW_ENEMY_N),
		/** pod ribbons on hp 048D / 048E (5254 C59 / C61): black, blue, yellow, HDR 3 (8000EFCEE857 r2, peak 3.00; game hex #000000 #82C0FF #A4F3FF #FFFFA1 #FFFFDA). */
		JUNO_POD_RIBBON(PaletteStops2.JUNO_POD_RIBBON),
		JUNO_POD_RIBBON_N(PaletteStops2.JUNO_POD_RIBBON_N),
		/** the pod ribbons' twin: black, dark red, white, HDR 5 (8000EFCEE857 r3, peak 5.00; game hex #000000 #AD4D4D #FF7878 #FFEEC1 #FFFFFF). */
		JUNO_POD_RIBBON_ENEMY(PaletteStops2.JUNO_POD_RIBBON_ENEMY),
		JUNO_POD_RIBBON_ENEMY_N(PaletteStops2.JUNO_POD_RIBBON_ENEMY_N),
		/** the pod's star burst 12F7D (5254 C57): black, slate, olive, white, HDR 5 (8003AE56E82B r4, peak 5.00; game hex #000000 #90A9FA #EDEFC5 #FFFFAE #FFFFFF). */
		JUNO_POD_BURST(PaletteStops2.JUNO_POD_BURST),
		JUNO_POD_BURST_N(PaletteStops2.JUNO_POD_BURST_N),
		/** the burst's twin: black, brown, white, HDR 5 (8003AE56E82B r5, peak 5.00; game hex #000000 #F2A81B #FFECB5 #FFFFFF #FFFFFF). */
		JUNO_POD_BURST_ENEMY(PaletteStops2.JUNO_POD_BURST_ENEMY),
		JUNO_POD_BURST_ENEMY_N(PaletteStops2.JUNO_POD_BURST_ENEMY_N),
		/** launch flare 37A79 (5AF3 C29): blue then yellow, HDR 2 (80056BA6F14B r0, peak 2.00; game hex #0088FF #0088FF #0088FF #FFFFB6 #FFFFB6). */
		JUNO_TORP_LAUNCH(PaletteStops2.JUNO_TORP_LAUNCH),
		JUNO_TORP_LAUNCH_N(PaletteStops2.JUNO_TORP_LAUNCH_N),
		/** the launch flare's twin: red then peach, HDR 2 (80056BA6F14B r1, peak 2.00; game hex #FF0000 #FF0000 #FF0000 #FFFFB6 #FFFFB6). */
		JUNO_TORP_LAUNCH_ENEMY(PaletteStops2.JUNO_TORP_LAUNCH_ENEMY),
		JUNO_TORP_LAUNCH_ENEMY_N(PaletteStops2.JUNO_TORP_LAUNCH_ENEMY_N),
		/** ally torpedo trail (51E8 C39 / C47, 2FCA7): black, gold, pale yellow, HDR 3 (8008CE76213B r3, peak 3.00; game hex #000000 #FFDE8E #FFFF8F #FFFFE5 #FFFFFF). */
		JUNO_TORP_HEAL_RIBBON(PaletteStops2.JUNO_TORP_HEAL_RIBBON),
		JUNO_TORP_HEAL_RIBBON_N(PaletteStops2.JUNO_TORP_HEAL_RIBBON_N),
		/** the ally trail's twin: black, red, white, HDR 5 (8008CE76213B r4, peak 5.00; game hex #000000 #FF9B94 #FFEBBF #FFFFFF #FFFFFF). */
		JUNO_TORP_HEAL_RIBBON_ENEMY(PaletteStops2.JUNO_TORP_HEAL_RIBBON_ENEMY),
		JUNO_TORP_HEAL_RIBBON_ENEMY_N(PaletteStops2.JUNO_TORP_HEAL_RIBBON_ENEMY_N),
		/** ally torpedo core sphere DBF (51E8 C36 / C46): black, slate, cream, HDR 5 (8003CB620243 r1, peak 5.00; game hex #000000 #A3C0F8 #F2F2F2 #FFFFFF #FFFFFF). */
		JUNO_TORP_HEAL_CORE(PaletteStops2.JUNO_TORP_HEAL_CORE),
		JUNO_TORP_HEAL_CORE_N(PaletteStops2.JUNO_TORP_HEAL_CORE_N),
		/** ally torpedo half-shell 160EA (51E8 C42): brown to pale yellow, HDR 5 (800BCD06F60D r2, peak 5.00; game hex #6D4000 #CCA770 #FFE9A4 #FFFFD2 #FFFFFF). */
		JUNO_TORP_HEAL_SHELL(PaletteStops2.JUNO_TORP_HEAL_SHELL),
		JUNO_TORP_HEAL_SHELL_N(PaletteStops2.JUNO_TORP_HEAL_SHELL_N),
		/** the shell's twin: dark red to white, HDR 5 (800BCD06F60D r3, peak 5.00; game hex #FF0000 #FF0000 #FFA783 #FFFFEA #FFFFFF). */
		JUNO_TORP_HEAL_SHELL_ENEMY(PaletteStops2.JUNO_TORP_HEAL_SHELL_ENEMY),
		JUNO_TORP_HEAL_SHELL_ENEMY_N(PaletteStops2.JUNO_TORP_HEAL_SHELL_ENEMY_N),
		/** enemy torpedo trail (5940 C15, 299E3 x 4C003): navy to pale blue, HDR 5 (800BC34C1380 r4, peak 5.00; game hex #2A00A6 #3D3BBA #5F70EB #9EC4FF #FFFFFF). */
		JUNO_TORP_DMG_RIBBON(PaletteStops2.JUNO_TORP_DMG_RIBBON),
		JUNO_TORP_DMG_RIBBON_N(PaletteStops2.JUNO_TORP_DMG_RIBBON_N),
		/** the damage trail's twin: black, red, orange-cream, HDR 5 (800BC34C1380 r5, peak 5.00; game hex #000000 #AD4D4D #FF7878 #FFF5C8 #FFFFFF). */
		JUNO_TORP_DMG_RIBBON_ENEMY(PaletteStops2.JUNO_TORP_DMG_RIBBON_ENEMY),
		JUNO_TORP_DMG_RIBBON_ENEMY_N(PaletteStops2.JUNO_TORP_DMG_RIBBON_ENEMY_N),
		/** enemy torpedo half-shell 160EA (5940 C19): violet to light blue, HDR 5 (8008DC40B527 r2, peak 5.00; game hex #44006D #BC85E2 #E5BBFF #F0FFFF #FFFFFF). */
		JUNO_TORP_DMG_SHELL(PaletteStops2.JUNO_TORP_DMG_SHELL),
		JUNO_TORP_DMG_SHELL_N(PaletteStops2.JUNO_TORP_DMG_SHELL_N),
		/** the damage shell's twin: dark red to peach, HDR 5 (8008DC40B527 r3, peak 5.00; game hex #FF0000 #FF0000 #FFA27E #FFFFDE #FFFFFF). */
		JUNO_TORP_DMG_SHELL_ENEMY(PaletteStops2.JUNO_TORP_DMG_SHELL_ENEMY),
		JUNO_TORP_DMG_SHELL_ENEMY_N(PaletteStops2.JUNO_TORP_DMG_SHELL_ENEMY_N),
		/** torpedo hit on an enemy, ring 724 (5B0D C15): grey-teal to cyan, HDR 5 (8008397E5051 r1, peak 5.00; game hex #CAFFF6 #CAFFF6 #F5FFFF #FFFFFF #FFFFFF). */
		JUNO_TORP_DMG_HIT(PaletteStops2.JUNO_TORP_DMG_HIT),
		JUNO_TORP_DMG_HIT_N(PaletteStops2.JUNO_TORP_DMG_HIT_N),
		/** the damage hit's twin: grey-brown to pink-red, HDR 5 (8008397E5051 r2, peak 5.00; game hex #FFD2CA #FFD2CA #FFF5F5 #FFFFFF #FFFFFF). */
		JUNO_TORP_DMG_HIT_ENEMY(PaletteStops2.JUNO_TORP_DMG_HIT_ENEMY),
		JUNO_TORP_DMG_HIT_ENEMY_N(PaletteStops2.JUNO_TORP_DMG_HIT_ENEMY_N),
		/** torpedo hit on an ally (5B0C C14 / 5F46 C12, DA5B): white to yellow, falls (80030597EE51 r0, peak 1.00, falls; game hex #FFFFFF #FEFFDE #FCFFB5 #FBFF8B #FBFF8B). */
		JUNO_TORP_HEAL_HIT(PaletteStops2.JUNO_TORP_HEAL_HIT),
		/** the ally hit's half-shell 160EA (5B0C C25): brown to white, HDR 5 (800C88FF95CC r2, peak 5.00; game hex #6D4000 #E2CF85 #FFFFC6 #FFFFFF #FFFFFF). */
		JUNO_TORP_HEAL_HIT_SHELL(PaletteStops2.JUNO_TORP_HEAL_HIT_SHELL),
		JUNO_TORP_HEAL_HIT_SHELL_N(PaletteStops2.JUNO_TORP_HEAL_HIT_SHELL_N),
		/** ally torpedo hits a wall (5F45 C13, 568C): black, olive, gold, white, HDR 5 (800B671DC421 r1, peak 5.00; game hex #000000 #FAD173 #FFFF9E #FFFFC2 #FFFFFF). */
		JUNO_TORP_WALL(PaletteStops2.JUNO_TORP_WALL),
		JUNO_TORP_WALL_N(PaletteStops2.JUNO_TORP_WALL_N),
		/** lock legs add 8A7D C5 (29F7 sparks): navy to cyan, HDR 3 (800BDFFAB667 r0, peak 3.00; game hex #1C00A4 #3B47B5 #617EDD #64CAFF #00FFFF). */
		JUNO_TORP_LOCK_GLINT(PaletteStops2.JUNO_TORP_LOCK_GLINT),
		JUNO_TORP_LOCK_GLINT_N(PaletteStops2.JUNO_TORP_LOCK_GLINT_N),
		/** the ring itself: look 1BABB material E786 LUT row 1: black, navy, blue, HDR 1.78 (80073F0A0946 r1, peak 1.78; game hex #26032B #374593 #4164CA #2F90FF #00AEFF). */
		JUNO_RING(PaletteStops2.JUNO_RING),
		JUNO_RING_N(PaletteStops2.JUNO_RING_N),
		/** the ring's enemy look 1BC0D material E8C3 row 1: black, dark red, red, HDR 1.78 (800D3BE4E436 r1, peak 1.78; game hex #26032B #91063D #C80849 #FF0435 #FF0000). */
		JUNO_RING_ENEMY(PaletteStops2.JUNO_RING_ENEMY),
		JUNO_RING_ENEMY_N(PaletteStops2.JUNO_RING_ENEMY_N),
		/** the ring's ring sprites 15B77 (541C C24, 16E9D x 19167): black, navy, sky blue, HDR 5 (800CABBF218C r5, peak 5.00; game hex #000000 #2B49B6 #2486D9 #1BCAFF #FFFFFF). */
		JUNO_RING_SPARK(PaletteStops2.JUNO_RING_SPARK),
		JUNO_RING_SPARK_N(PaletteStops2.JUNO_RING_SPARK_N),
		/** the ring sprites' twin: black, brown, orange, HDR 2.2 (800CABBF218C r6, peak 2.16; game hex #000000 #BE5100 #F26A00 #FF8700 #FFA300). */
		JUNO_RING_SPARK_ENEMY(PaletteStops2.JUNO_RING_SPARK_ENEMY),
		JUNO_RING_SPARK_ENEMY_N(PaletteStops2.JUNO_RING_SPARK_ENEMY_N),
		/** the ring's disc flash 5E5E size 16 (541C C49, 190EC x 299DF): navy, teal, white, HDR 3 (80060448B358 r3, peak 3.00; game hex #241AD3 #2A87E8 #36FFFF #3FFFFF #FFFFFF). */
		JUNO_RING_DISC(PaletteStops2.JUNO_RING_DISC),
		JUNO_RING_DISC_N(PaletteStops2.JUNO_RING_DISC_N),
		/** the disc's twin: dark red, red, peach, HDR 3 (80060448B358 r4, peak 3.00; game hex #D32F1A #F73A21 #FF5031 #FF613C #FFFFFF). */
		JUNO_RING_DISC_ENEMY(PaletteStops2.JUNO_RING_DISC_ENEMY),
		JUNO_RING_DISC_ENEMY_N(PaletteStops2.JUNO_RING_DISC_ENEMY_N),
		/** the ring's torus glow 15A26 (541C C30): black, teal, mint, HDR 10 (800215C126D1 r3, peak 10.00; game hex #000000 #D8FFFF #FFFFFF #FFFFFF #FFFFFF). */
		JUNO_RING_GLOW(PaletteStops2.JUNO_RING_GLOW),
		JUNO_RING_GLOW_N(PaletteStops2.JUNO_RING_GLOW_N),
		/** the torus glow's twin: black, brown, white, HDR 4 (800215C126D1 r4, peak 4.00; game hex #000000 #CA8E64 #FFC785 #FFFFFF #FFFFFF). */
		JUNO_RING_GLOW_ENEMY(PaletteStops2.JUNO_RING_GLOW_ENEMY),
		JUNO_RING_GLOW_ENEMY_N(PaletteStops2.JUNO_RING_GLOW_ENEMY_N),
		/** the throw flash 37A79 / 568C at her hand (5AE7 C21-C27): black to mint, HDR 3 (800105E80D56 r0, peak 3.00; game hex #000000 #6FA47E #BDFFEE #F7FFFF #FFFFFF). */
		JUNO_RING_LAUNCH(PaletteStops2.JUNO_RING_LAUNCH),
		JUNO_RING_LAUNCH_N(PaletteStops2.JUNO_RING_LAUNCH_N),
		/** the three flying orbs' trails (5B22 C11, 299E3): white, blue-grey, green, HDR 5, falls (800D9C7EE5BA r3, peak 5.00, falls; game hex #FFFFFF #FFFFFF #FDFFFF #00FF3E #00C72C). */
		JUNO_RING_ORB_RIBBON(PaletteStops2.JUNO_RING_ORB_RIBBON),
		JUNO_RING_ORB_RIBBON_N(PaletteStops2.JUNO_RING_ORB_RIBBON_N),
		/** the orbs' trail twin: white, tan, red, HDR 5, falls (800D9C7EE5BA r5, peak 5.00, falls; game hex #FFFFFF #FFFFFF #FFFDFD #FF0000 #C70000). */
		JUNO_RING_ORB_RIBBON_ENEMY(PaletteStops2.JUNO_RING_ORB_RIBBON_ENEMY),
		JUNO_RING_ORB_RIBBON_ENEMY_N(PaletteStops2.JUNO_RING_ORB_RIBBON_ENEMY_N),
		/** the ring's end streaks 49944 (5D44 C36): navy to white, HDR 3 (800FD4BBE94C r3, peak 3.00; game hex #241AD3 #2D21FD #FFFFFF #FFFFFF #FFFFFF). */
		JUNO_RING_END(PaletteStops2.JUNO_RING_END),
		JUNO_RING_END_N(PaletteStops2.JUNO_RING_END_N),
		/** the end streaks' twin: dark red to white, HDR 3 (800FD4BBE94C r4, peak 3.00; game hex #D3291A #FD3321 #FFFFFF #FFFFFF #FFFFFF). */
		JUNO_RING_END_ENEMY(PaletteStops2.JUNO_RING_END_ENEMY),
		JUNO_RING_END_ENEMY_N(PaletteStops2.JUNO_RING_END_ENEMY_N),
		/** the speed lines on a boosted ally E99F (56DE C6): yellow to green, HDR 25, falls (800F2AAB0A81 r0, peak 24.98, falls; game hex #FFFF59 #FFFF7C #FFFF95 #FFFFAA #FFFFBB). */
		JUNO_RING_BOOST(PaletteStops2.JUNO_RING_BOOST),
		JUNO_RING_BOOST_N(PaletteStops2.JUNO_RING_BOOST_N),
		/** the ground circle decal 4DBCC x 16E7A, 8.5 m (54F9 C32): black, olive-gold, pale blue-white, HDR 2 (800B875A72EE r3, peak 2.00; game hex #000000 #B69F0E #F8D918 #B6D3FF #FFFFFF). */
		JUNO_RAY_DECAL(PaletteStops2.JUNO_RAY_DECAL),
		JUNO_RAY_DECAL_N(PaletteStops2.JUNO_RAY_DECAL_N),
		/** the decal's twin: black, red, peach (800B875A72EE r4, peak 1.00; game hex #000000 #D45C4B #DB7A68 #E2907E #FFD4BD). */
		JUNO_RAY_DECAL_ENEMY(PaletteStops2.JUNO_RAY_DECAL_ENEMY),
		/** the landing ring decal 139C growing 0 -> 7 m (54F9 C41): black, slate, white, HDR 3 (800533AEEE94 r1, peak 3.00; game hex #000000 #7C82B6 #AAB3F8 #DCE2FF #FFFFFF). */
		JUNO_RAY_SHOCK(PaletteStops2.JUNO_RAY_SHOCK),
		JUNO_RAY_SHOCK_N(PaletteStops2.JUNO_RAY_SHOCK_N),
		/** the landing ring's twin: black, brown, cream, HDR 3 (800533AEEE94 r2, peak 3.00; game hex #000000 #B67C7C #F8AAAA #FFD5D0 #FFFFFF). */
		JUNO_RAY_SHOCK_ENEMY(PaletteStops2.JUNO_RAY_SHOCK_ENEMY),
		JUNO_RAY_SHOCK_ENEMY_N(PaletteStops2.JUNO_RAY_SHOCK_ENEMY_N),
		/** the beam column 15A21 (54F9 C38, 18E48 x 31951): pale blue, yellow, white, HDR 4 (80051C2A6670 r2, peak 4.09; game hex #F3FFFF #F3FFFF #FFFFFF #FFFFFF #FFFFFF). */
		JUNO_RAY_COLUMN(PaletteStops2.JUNO_RAY_COLUMN),
		JUNO_RAY_COLUMN_N(PaletteStops2.JUNO_RAY_COLUMN_N),
		/** the beam's outer column 15A21 (54F9 C40, 4D4 x 4DBCD): navy, teal, cyan, HDR 3.6 (800CE2546379 r2, peak 3.56; game hex #000087 #00CAFF #00FFFF #C2FFFF #00FFFF). */
		JUNO_RAY_COLUMN_EDGE(PaletteStops2.JUNO_RAY_COLUMN_EDGE),
		JUNO_RAY_COLUMN_EDGE_N(PaletteStops2.JUNO_RAY_COLUMN_EDGE_N),
		/** the column's twin row: black, brown, peach, HDR 3 (80051C2A6670 r3, peak 3.00; game hex #000000 #B2782E #F3A442 #FFD47F #FFFFF8). */
		JUNO_RAY_COLUMN_ENEMY(PaletteStops2.JUNO_RAY_COLUMN_ENEMY),
		JUNO_RAY_COLUMN_ENEMY_N(PaletteStops2.JUNO_RAY_COLUMN_ENEMY_N),
		/** the descending rings 15A26 size 8 (54F9 C36): black, slate, cream, HDR 50 (8004AEC38771 r3, peak 49.97; game hex #000000 #FFFFFF #FFFFFF #FFFFFF #FFFFFF). */
		JUNO_RAY_RINGS(PaletteStops2.JUNO_RAY_RINGS),
		JUNO_RAY_RINGS_N(PaletteStops2.JUNO_RAY_RINGS_N),
		/** the rings' twin: black, brown, white, HDR 4 (8004AEC38771 r4, peak 4.00; game hex #000000 #CA8E64 #FFC785 #FFFFFF #FFFFFF). */
		JUNO_RAY_RINGS_ENEMY(PaletteStops2.JUNO_RAY_RINGS_ENEMY),
		JUNO_RAY_RINGS_ENEMY_N(PaletteStops2.JUNO_RAY_RINGS_ENEMY_N),
		/** the ground glow 801 size 3 (54F9 C26, 4DB x 4C003 x C495): navy to white, HDR 5 (80044605BC64 r5, peak 5.00; game hex #0014AD #6173FF #EAFFFF #FFFFFF #FFFFFF). */
		JUNO_RAY_GROUND(PaletteStops2.JUNO_RAY_GROUND),
		JUNO_RAY_GROUND_N(PaletteStops2.JUNO_RAY_GROUND_N),
		/** the ground glow's twin: black to red-pink, HDR 4 (80044605BC64 r7, peak 4.00; game hex #000000 #B25454 #FFA2A2 #FFD4D4 #FFE6E6). */
		JUNO_RAY_GROUND_ENEMY(PaletteStops2.JUNO_RAY_GROUND_ENEMY),
		JUNO_RAY_GROUND_ENEMY_N(PaletteStops2.JUNO_RAY_GROUND_ENEMY_N),
		/** falling streaks 49944 at 40 m/s (54F9 C52): blue to pale blue-white, HDR 1.8 (80050158D799 r3, peak 1.80; game hex #241AD3 #291EE9 #B9BFFF #FAFFFF #FFFFFF). */
		JUNO_RAY_RAIN(PaletteStops2.JUNO_RAY_RAIN),
		JUNO_RAY_RAIN_N(PaletteStops2.JUNO_RAY_RAIN_N),
		/** the falling streaks' twin: black, red, pink, HDR 8 (80050158D799 r4, peak 8.00; game hex #000000 #FF0000 #FF6565 #FFF8F8 #FFFFFF). */
		JUNO_RAY_RAIN_ENEMY(PaletteStops2.JUNO_RAY_RAIN_ENEMY),
		JUNO_RAY_RAIN_ENEMY_N(PaletteStops2.JUNO_RAY_RAIN_ENEMY_N),
		/** the low wall 1F85 at the circle (54F9 C28, 38E1D x 31951): navy to light blue, HDR 7 (80063BF353FF r3, peak 7.00; game hex #0014AD #96B5FB #E2FFFF #FFFFFF #FFFFFF). */
		JUNO_RAY_WALL(PaletteStops2.JUNO_RAY_WALL),
		JUNO_RAY_WALL_N(PaletteStops2.JUNO_RAY_WALL_N),
		/** the wall's twin: peach to gold, falls (80063BF353FF r7, peak 1.00, falls; game hex #FFD9B5 #FFC3B0 #FFAAA9 #F0A17C #DF9700). */
		JUNO_RAY_WALL_ENEMY(PaletteStops2.JUNO_RAY_WALL_ENEMY),
		/** an ally in the ray: material effect on the body (3169 C11, 19167): blue, HDR 5 (8000F7697B4A r2, peak 5.00; game hex #5266FF #5468FF #7789FF #A5B4FF #FFFFFF). */
		JUNO_RAY_ALLY(PaletteStops2.JUNO_RAY_ALLY),
		JUNO_RAY_ALLY_N(PaletteStops2.JUNO_RAY_ALLY_N),
		/** the body effect's other row: yellow to brown, falls (8000F7697B4A r1, peak 0.90, falls; game hex #F3DA00 #D67400 #B35900 #8F5900 #7C5900). */
		JUNO_RAY_ALLY_GOLD(PaletteStops2.JUNO_RAY_ALLY_GOLD),
		/** the ally's rising chevrons DFDC (3169 C17): black to light blue (800FC76640A5 r3, peak 1.00; game hex #000000 #336FAF #6699DA #83B5F7 #92C2FF). */
		JUNO_RAY_ALLY_RISE(PaletteStops2.JUNO_RAY_ALLY_RISE),
		/** the cast's cone EEB on her hand (5CCA C72): black, blue, cyan, HDR 10 (800FBE8BE003 r3, peak 9.99; game hex #002B57 #00CDFF #00FFFF #AEFFFF #FFFFFF). */
		JUNO_RAY_CAST(PaletteStops2.JUNO_RAY_CAST),
		JUNO_RAY_CAST_N(PaletteStops2.JUNO_RAY_CAST_N),
		/** the cast cone's twin: black, red, orange, HDR 10 (800FBE8BE003 r4, peak 9.99; game hex #570F00 #FF6B00 #FF9400 #FFFFAE #FFFFFF). */
		JUNO_RAY_CAST_ENEMY(PaletteStops2.JUNO_RAY_CAST_ENEMY),
		JUNO_RAY_CAST_ENEMY_N(PaletteStops2.JUNO_RAY_CAST_ENEMY_N),
		/** the 3P cast ribbon (clip effect 61AD C8): violet, blue, cyan-white, HDR 5 (800CE2C38A79 r6, peak 5.00; game hex #8038FF #5A7AFF #62FFFF #DDFFFF #FFFFFF). */
		JUNO_RAY_CAST_RIBBON(PaletteStops2.JUNO_RAY_CAST_RIBBON),
		JUNO_RAY_CAST_RIBBON_N(PaletteStops2.JUNO_RAY_CAST_RIBBON_N),
		/** the cast ribbon's twin: red to cream, HDR 5 (800CE2C38A79 r7, peak 5.00; game hex #FF4F38 #FF7740 #FFB962 #FFFFDD #FFFFFF). */
		JUNO_RAY_CAST_RIBBON_ENEMY(PaletteStops2.JUNO_RAY_CAST_RIBBON_ENEMY),
		JUNO_RAY_CAST_RIBBON_ENEMY_N(PaletteStops2.JUNO_RAY_CAST_RIBBON_ENEMY_N),
		/** boot and glide jet glow 1603 (5D10 C9, 5A40 C11, 5C36 C7): navy, teal, white, HDR 8 (800265141E85 r0, peak 8.00; game hex #2115FF #2EA9FF #50FFFF #FFFFFF #FFFFFF). */
		JUNO_BOOT_JET(PaletteStops2.JUNO_BOOT_JET),
		JUNO_BOOT_JET_N(PaletteStops2.JUNO_BOOT_JET_N),
		/** jet flame cone E21 x 2D278 (5A40 C15, 5C36 C9): blue to white, HDR 5 (800EC4671407 r0, peak 5.00; game hex #2D32FF #31C2FF #34FFFF #39FFFF #FFFFFF). */
		JUNO_BOOT_FLAME(PaletteStops2.JUNO_BOOT_FLAME),
		JUNO_BOOT_FLAME_N(PaletteStops2.JUNO_BOOT_FLAME_N),
		/** the inner flame cone (5D10 C13, 5A40 C17, 5C36 C11): violet, teal, white, HDR 8 (80095C8F5DA2 r0, peak 8.00; game hex #C715FF #BB84FE #AFE8FF #86FFFF #FFFFFF). */
		JUNO_BOOT_FLAME_VIOLET(PaletteStops2.JUNO_BOOT_FLAME_VIOLET),
		JUNO_BOOT_FLAME_VIOLET_N(PaletteStops2.JUNO_BOOT_FLAME_VIOLET_N),
		/** jet sparks 1F35 (5D10 C15, 5A40 C19): cyan, pale cyan (8000939E8147 r2, peak 1.00; game hex #00D4FF #5BE0FF #7EEBFF #98F5FF #ADFFFF). */
		JUNO_BOOT_SPARK(PaletteStops2.JUNO_BOOT_SPARK),
		/** the boots' ribbon (5985 C7, 2FCA7): pale blue to navy, HDR 5, falls (8009B556202A r4, peak 5.00, falls; game hex #FFFFFF #BEEEFF #6A95FF #6A95FF #6A95FF). */
		JUNO_BOOT_RIBBON(PaletteStops2.JUNO_BOOT_RIBBON),
		JUNO_BOOT_RIBBON_N(PaletteStops2.JUNO_BOOT_RIBBON_N),
		/** hover torus 15A26 under her (5D10 C11): white to orange, HDR 3, falls (800BD5F0566C r3, peak 3.00, falls; game hex #FFFFFF #FFFFFF #FFFFBA #FFEB00 #FFE600). */
		JUNO_HOVER_RING(PaletteStops2.JUNO_HOVER_RING),
		JUNO_HOVER_RING_N(PaletteStops2.JUNO_HOVER_RING_N),
		/** the hover torus's twin: peach to red, HDR 5, falls (800BD5F0566C r4, peak 5.00, falls; game hex #FFFFFF #FFFFFF #FFD0D0 #FF9999 #FF0000). */
		JUNO_HOVER_RING_ENEMY(PaletteStops2.JUNO_HOVER_RING_ENEMY),
		JUNO_HOVER_RING_ENEMY_N(PaletteStops2.JUNO_HOVER_RING_ENEMY_N),
		/** glide torus 15A26 (5A40 C13): white to sky blue, HDR 3, falls (8007A6420EC4 r3, peak 3.00, falls; game hex #FFFFFF #EFFFFF #EBFFFF #E6FFFF #E1FFFF). */
		JUNO_GLIDE_RING(PaletteStops2.JUNO_GLIDE_RING),
		JUNO_GLIDE_RING_N(PaletteStops2.JUNO_GLIDE_RING_N),
		/** the glide torus's twin: peach to red, HDR 5, falls (8007A6420EC4 r4, peak 5.00, falls; game hex #FFFFFF #FFFFFF #FFD0D0 #FF9999 #FF0000). */
		JUNO_GLIDE_RING_ENEMY(PaletteStops2.JUNO_GLIDE_RING_ENEMY),
		JUNO_GLIDE_RING_ENEMY_N(PaletteStops2.JUNO_GLIDE_RING_ENEMY_N),
		/** the 1P glide screen hex burst 5708 (5963 C9, ECIM(40)): blue to white (80035A6BEECD r7, peak 1.00; game hex #6E98FF #9ACAFF #BAEFFF #DCFFFF #FFFFFF). */
		JUNO_GLIDE_SCREEN(PaletteStops2.JUNO_GLIDE_SCREEN),
		// Sojourn's (tools/reference/sojourn-effects.md; research\sojourn\fx, ramps per ability): her team's cyan / teal / blue
		// rows and her enemies' red ones, each LUT's row as the renderer names it
		/** the Railgun's muzzle flare 210FD (2837 C46 / C48), 0.38-0.51 m and 0.55-2.5 m, 0.13 s: navy, cyan, cream (800F8C16CE25 r0, peak 1.00; game hex #004C88 #00ABD8 #00E5E1 #93FED9 #FFFAB6). */
		SOJ_MUZZLE(SojournPalettes.SOJ_MUZZLE),
		/** the muzzle flare's enemy twin (2837 C54): plum, red, gold (800E592E452A r0, peak 2.09; game hex #B30088 #C92C70 #E2414F #FF7723 #FFFF00). */
		SOJ_MUZZLE_ENEMY(SojournPalettes.SOJ_MUZZLE_ENEMY),
		SOJ_MUZZLE_ENEMY_N(SojournPalettes.SOJ_MUZZLE_ENEMY_N),
		/** the muzzle's sparks 183A7 (2837 C74), 10-15 at 2-7.6 m/s: teal to cream, HDR 7 (800D949751B6 r1, peak 1.00; game hex #00E1D9 #00E1D9 #00E1D9 #AFECD0 #FFF9C4). */
		SOJ_MUZZLE_SPARK(SojournPalettes.SOJ_MUZZLE_SPARK),
		/** the muzzle sparks' enemy row (2837 C66): red to pale yellow (8002CA7C1279 r1, peak 1.00; game hex #E10000 #E10000 #E10B02 #F77430 #FFF177). */
		SOJ_MUZZLE_SPARK_ENEMY(SojournPalettes.SOJ_MUZZLE_SPARK_ENEMY),
		/** the round's ribbon 1FDF6 (280D C6), 0.096 m wide, 0.015-0.07 s: navy, cyan, cream, HDR 1.8 (8007CD484B35 r4, peak 1.83; game hex #004C88 #00ADDB #00F7F0 #C1FFF7 #FFFFEE). */
		SOJ_ROUND(SojournPalettes.SOJ_ROUND),
		SOJ_ROUND_N(SojournPalettes.SOJ_ROUND_N),
		/** the round's enemy ribbon (280D C4): plum, red, orange, pale yellow, HDR 4.9 (8004F1F9D1E7 r2, peak 4.86; game hex #C5009E #EE3092 #FF5757 #FF8700 #FFFFE2). */
		SOJ_ROUND_ENEMY(SojournPalettes.SOJ_ROUND_ENEMY),
		SOJ_ROUND_ENEMY_N(SojournPalettes.SOJ_ROUND_ENEMY_N),
		/** a round's hit flare 210FD (31E5 C23), 0.57-2.3 m, 0.07-0.16 s: navy, teal, pale yellow (8003B3BE55A6 r0, peak 1.27; game hex #005989 #0098A4 #00BFBD #00E2CE #FFFF85). */
		SOJ_HIT(SojournPalettes.SOJ_HIT),
		SOJ_HIT_N(SojournPalettes.SOJ_HIT_N),
		/** the hit flare's enemy row (31E5 C29): plum, red, orange, pale yellow (800C3112949E r0, peak 1.27; game hex #440043 #9F001B #C73900 #E36200 #FFFF85). */
		SOJ_HIT_ENEMY(SojournPalettes.SOJ_HIT_ENEMY),
		SOJ_HIT_ENEMY_N(SojournPalettes.SOJ_HIT_ENEMY_N),
		/** a round's hit sparks 183A7 (31E5 C27), 4 at 2.4-2.7 m/s: blue, cyan, pale yellow (800A4B0C4BDD r0, peak 1.00; game hex #007DFF #007DFF #00BCE4 #7CDCB8 #FFF177). */
		SOJ_HIT_SPARK(SojournPalettes.SOJ_HIT_SPARK),
		/** the hit sparks' enemy row (31E5 C33) (800DF9B6EC1D r0, peak 1.27; game hex #440043 #8D003A #B8002E #E15600 #FFFF85). */
		SOJ_HIT_SPARK_ENEMY(SojournPalettes.SOJ_HIT_SPARK_ENEMY),
		SOJ_HIT_SPARK_ENEMY_N(SojournPalettes.SOJ_HIT_SPARK_ENEMY_N),
		/** the gun's energy glow 210FD on hp 007C (31E6 C47), 0.15 / 0.33 / 0.49 m by energy: teal to cyan, HDR 2.5 (800E50D3FD92 r0, peak 2.51; game hex #00DDFF #00E1FF #00F2FF #00FFFF #00FFFF). */
		SOJ_GUN_GLOW(SojournPalettes.SOJ_GUN_GLOW),
		SOJ_GUN_GLOW_N(SojournPalettes.SOJ_GUN_GLOW_N),
		/** the energy glow's enemy row (31E6 C63): dark red to orange (800656F5F311 r1, peak 2.51; game hex #FF0000 #FF0000 #FF2A00 #FF7A00 #FFCA00). */
		SOJ_GUN_GLOW_ENEMY(SojournPalettes.SOJ_GUN_GLOW_ENEMY),
		SOJ_GUN_GLOW_ENEMY_N(SojournPalettes.SOJ_GUN_GLOW_ENEMY_N),
		/** the gun's big glow 210FD on hp 007C (31E6 C61), 0.55-0.97 m: blue to cyan (800E7000846D r0, peak 1.19; game hex #0027BD #0086CD #00CEED #00E5FF #00EEFF). */
		SOJ_GUN_CORE(SojournPalettes.SOJ_GUN_CORE),
		SOJ_GUN_CORE_N(SojournPalettes.SOJ_GUN_CORE_N),
		/** its enemy row: red to orange (800E7000846D r1, peak 1.19; game hex #BD0000 #CD6200 #ED9C00 #FFAE00 #FFB600). */
		SOJ_GUN_CORE_ENEMY(SojournPalettes.SOJ_GUN_CORE_ENEMY),
		SOJ_GUN_CORE_ENEMY_N(SojournPalettes.SOJ_GUN_CORE_ENEMY_N),
		/** the 1P reload's flash 210FD on hp 007C (30A6 C11): teal to mint, HDR 4.2 (80020564EDB5 r0, peak 4.20; game hex #00B1E4 #00DCD2 #00FFC5 #00FFF9 #00FFFF). */
		SOJ_RELOAD(SojournPalettes.SOJ_RELOAD),
		SOJ_RELOAD_N(SojournPalettes.SOJ_RELOAD_N),
		/** the Charged Shot's muzzle flare 210FD (2834 C37), 1-2.5 m: dark teal, blue, cyan (8003099EB5DB r0, peak 1.00; game hex #00251C #009ED8 #00C3F5 #00D3E1 #00E1CA). */
		SOJ_CHARGED_MUZZLE(SojournPalettes.SOJ_CHARGED_MUZZLE),
		/** the charged muzzle's enemy row (2834 C39): black, plum, red, gold, HDR 3.8 (8008C988E551 r0, peak 3.79; game hex #000000 #BD147D #FF3577 #FF4E66 #FFFF94). */
		SOJ_CHARGED_MUZZLE_ENEMY(SojournPalettes.SOJ_CHARGED_MUZZLE_ENEMY),
		SOJ_CHARGED_MUZZLE_ENEMY_N(SojournPalettes.SOJ_CHARGED_MUZZLE_ENEMY_N),
		/** the Charged Shot's beam 2A5D0 (2835 C26 / C34, 1.3 s): navy, teal, green, white, HDR 2.3 (80085AB06897 r3, peak 2.26; game hex #0051B5 #007FA4 #00B096 #00F57E #FFFFFF). */
		SOJ_CHARGED_BEAM(SojournPalettes.SOJ_CHARGED_BEAM),
		SOJ_CHARGED_BEAM_N(SojournPalettes.SOJ_CHARGED_BEAM_N),
		/** the beam's enemy row (2835 C21): plum, red, gold, HDR 3.8 (800F11791498 r9, peak 3.79; game hex #7E0068 #9B1663 #DA3159 #FF494D #FFFF94). */
		SOJ_CHARGED_BEAM_ENEMY(SojournPalettes.SOJ_CHARGED_BEAM_ENEMY),
		SOJ_CHARGED_BEAM_ENEMY_N(SojournPalettes.SOJ_CHARGED_BEAM_ENEMY_N),
		/** the tracer 3216's glows (2BF0 C25-C31), 0.3-0.4 m at 256-500/s along it, 0.28-0.75 s: cream, ice blue, blue, HDR 8.2 (8005DDCA22CC r4, peak 8.19, falls; game hex #FFFFFF #FFFFFF #FFFFFF #00FFFF #00FFFF). */
		SOJ_CHARGED_TRACER(SojournPalettes.SOJ_CHARGED_TRACER),
		SOJ_CHARGED_TRACER_N(SojournPalettes.SOJ_CHARGED_TRACER_N),
		/** the tracer's enemy row (2835 C23): plum, red, gold, HDR 3.8 (800ED038B259 r2, peak 3.79; game hex #7E0068 #C4207B #FF4477 #FF6168 #FFFF94). */
		SOJ_CHARGED_TRACER_ENEMY(SojournPalettes.SOJ_CHARGED_TRACER_ENEMY),
		SOJ_CHARGED_TRACER_ENEMY_N(SojournPalettes.SOJ_CHARGED_TRACER_ENEMY_N),
		/** the tracer's sparks 1F35 (2BF0 C27), 4.4-9 m/s, 0.17-0.24 m: white to steel blue, HDR 15.6 (800808C16B1D r1, peak 15.59, falls; game hex #FFFFFF #FFFFFF #FFFFFF #FFFFFF #FFFFFF). */
		SOJ_CHARGED_SPARK(SojournPalettes.SOJ_CHARGED_SPARK),
		SOJ_CHARGED_SPARK_N(SojournPalettes.SOJ_CHARGED_SPARK_N),
		/** the charged hit's sparks 183A7 (22BF C27), 27-29 at 2.6-6 m/s: blue to white (8004081A686E r1, peak 1.00; game hex #0004FF #696AFF #AFAFFF #DCDCFF #FFFFFF). */
		SOJ_CHARGED_HIT_SPARK(SojournPalettes.SOJ_CHARGED_HIT_SPARK),
		/** its enemy row (22BF C29): red, orange, peach (800FE1C55DCE r1, peak 1.55; game hex #FF0000 #FF3A00 #FF6500 #FF9600 #FFFEA3). */
		SOJ_CHARGED_HIT_SPARK_ENEMY(SojournPalettes.SOJ_CHARGED_HIT_SPARK_ENEMY),
		SOJ_CHARGED_HIT_SPARK_ENEMY_N(SojournPalettes.SOJ_CHARGED_HIT_SPARK_ENEMY_N),
		/** the charged hit's smoke 1DFCA/B (22BF C40), 0.9-2.6 m, 0.6-1 s: grey to ice blue (8004D5D699CD r1, peak 1.79; game hex #555555 #8C999D #B0C3C9 #C2ECFB #B6FFFF). */
		SOJ_CHARGED_SMOKE(SojournPalettes.SOJ_CHARGED_SMOKE),
		SOJ_CHARGED_SMOKE_N(SojournPalettes.SOJ_CHARGED_SMOKE_N),
		/** its enemy row (22BF C42): grey to peach (800569E244A1 r1, peak 1.50; game hex #555555 #9D938C #C9BBB0 #F4DDCD #FFFFE8). */
		SOJ_CHARGED_SMOKE_ENEMY(SojournPalettes.SOJ_CHARGED_SMOKE_ENEMY),
		SOJ_CHARGED_SMOKE_ENEMY_N(SojournPalettes.SOJ_CHARGED_SMOKE_ENEMY_N),
		/** the slide's thruster glows 242 on hp 002C / 002E / 0030 / 0031 (2905 C23), 0.69 m, 13/s each: blue, cyan, mint (800DBCBFA263 r0, peak 1.00; game hex #002ED1 #00B1E8 #00EDFC #70FAE6 #A2FFC0). */
		SOJ_SLIDE_JET(SojournPalettes.SOJ_SLIDE_JET),
		/** their enemy row (2905 C25): plum to red, HDR 3.8 (800499AD68D4 r0, peak 3.79; game hex #7E0068 #BD0063 #FF0056 #FF0042 #FF0000). */
		SOJ_SLIDE_JET_ENEMY(SojournPalettes.SOJ_SLIDE_JET_ENEMY),
		SOJ_SLIDE_JET_ENEMY_N(SojournPalettes.SOJ_SLIDE_JET_ENEMY_N),
		/** the slide's dust 1DFCA/B at the pelvis / hp 0017 (2905 C35), 1.06-1.2 m, 0.17-0.66 s: grey (80008989E0C7 r1, peak 0.86; game hex #393532 #88847E #B4AEA7 #D4CDC5 #EFE8DF). */
		SOJ_SLIDE_SMOKE(SojournPalettes.SOJ_SLIDE_SMOKE),
		/** the slide's sparks 183A7 from her feet hp 001B / 001C (2905 C38), 2.4-4.7 m/s: gold to plum, HDR 4.8 (800D405CC99B r0, peak 4.80, falls; game hex #FFFF5D #FFFF92 #FFFFAF #FFDAC1 #FF00D1). */
		SOJ_SLIDE_SPARK(SojournPalettes.SOJ_SLIDE_SPARK),
		SOJ_SLIDE_SPARK_N(SojournPalettes.SOJ_SLIDE_SPARK_N),
		/** the slide's ground dust 14F9 (2905 C42), 1-1.45 m: warm grey (8001D4A0A8D5 r1, peak 0.62; game hex #928D87 #A4A19A #B3B1AA #C1C0B8 #CECEC5). */
		SOJ_SLIDE_DUST(SojournPalettes.SOJ_SLIDE_DUST),
		/** the thrusters' streaks 1F79A (2905 C30), 0.19-0.3 m: violet to blue, HDR 5.7 (8003D2CF06D1 r4, peak 5.72; game hex #6E00FF #6E00FF #6E00FF #6E00FF #6EFFFF). */
		SOJ_SLIDE_STREAK(SojournPalettes.SOJ_SLIDE_STREAK),
		SOJ_SLIDE_STREAK_N(SojournPalettes.SOJ_SLIDE_STREAK_N),
		/** the slide jump's plume 1DFCA/B at hp 002D / 002F (330E C10), 103/s at 10.6-14.4 m/s: grey-teal (800D5EAB3710 r4, peak 0.51; game hex #628E8E #629C9C #62A8A8 #62B3B3 #62BDBD). */
		SOJ_JUMP_SMOKE(SojournPalettes.SOJ_JUMP_SMOKE),
		/** the slide jump's glow 210FD (330E C16), 1.36-1.45 m: blue to mint (800CC1E28E36 r0, peak 1.00; game hex #001EFF #008CF2 #00BDE3 #00E1D3 #00FFC2). */
		SOJ_JUMP_GLOW(SojournPalettes.SOJ_JUMP_GLOW),
		/** the slide jump's sparks 23F (330E C13): teal to ice, HDR 5.9 (80024CEF4843 r4, peak 5.88; game hex #00FFD2 #00FFDE #00FFFF #00FFFF #FFFFFF). */
		SOJ_JUMP_SPARK(SojournPalettes.SOJ_JUMP_SPARK),
		SOJ_JUMP_SPARK_N(SojournPalettes.SOJ_JUMP_SPARK_N),
		/** the Disruptor in flight, line glows C061 (2C8D C17), 0.6-1 m: green, teal, ice, HDR 3.9 (800FAC707B08 r4, peak 3.90; game hex #00FFC6 #63FFFC #A3FFFF #D8FFFF #FFFFFF). */
		SOJ_DISRUPTOR_CORE(SojournPalettes.SOJ_DISRUPTOR_CORE),
		SOJ_DISRUPTOR_CORE_N(SojournPalettes.SOJ_DISRUPTOR_CORE_N),
		/** its enemy row: red to peach, HDR 3.9 (800FAC707B08 r5, peak 3.90; game hex #FF0000 #FF7F63 #FFD0A3 #FFFFD8 #FFFFFF). */
		SOJ_DISRUPTOR_CORE_ENEMY(SojournPalettes.SOJ_DISRUPTOR_CORE_ENEMY),
		SOJ_DISRUPTOR_CORE_ENEMY_N(SojournPalettes.SOJ_DISRUPTOR_CORE_ENEMY_N),
		/** its soft glow A17F (2C8D C21), 0.7 m: dark teal to white, HDR 5 (8001849425C2 r1, peak 5.00; game hex #005C4C #00A375 #00E7A1 #FFFF9E #FFFFFF). */
		SOJ_DISRUPTOR_GLOW(SojournPalettes.SOJ_DISRUPTOR_GLOW),
		SOJ_DISRUPTOR_GLOW_N(SojournPalettes.SOJ_DISRUPTOR_GLOW_N),
		/** its enemy row (2C8D C19): dark red to pale yellow, HDR 5 (8005468757CE r1, peak 5.00; game hex #FF2020 #FF7041 #FFA960 #FFF277 #FFFFFF). */
		SOJ_DISRUPTOR_GLOW_ENEMY(SojournPalettes.SOJ_DISRUPTOR_GLOW_ENEMY),
		SOJ_DISRUPTOR_GLOW_ENEMY_N(SojournPalettes.SOJ_DISRUPTOR_GLOW_ENEMY_N),
		/** its motes 4A58 drawn in (2C8D C23), 0.13-0.17 m, 50/s: green to ice, HDR 4.2 (800D845BCEB0 r0, peak 4.20; game hex #00FF8E #41FFAF #74FFEA #D0FFFF #FFFFFF). */
		SOJ_DISRUPTOR_MOTE(SojournPalettes.SOJ_DISRUPTOR_MOTE),
		SOJ_DISRUPTOR_MOTE_N(SojournPalettes.SOJ_DISRUPTOR_MOTE_N),
		/** their enemy row: dark red to peach (800D845BCEB0 r1, peak 4.20; game hex #FF0000 #FF5A41 #FF9E74 #FFF9B8 #FFFFFF). */
		SOJ_DISRUPTOR_MOTE_ENEMY(SojournPalettes.SOJ_DISRUPTOR_MOTE_ENEMY),
		SOJ_DISRUPTOR_MOTE_ENEMY_N(SojournPalettes.SOJ_DISRUPTOR_MOTE_ENEMY_N),
		/** the launch flash A17F (2C92 C9 / C11), 0.4 m and 6 m, 0.15 s: slate teal to mint, HDR 5 (8000BBD2D7CD r0, peak 5.00; game hex #54C1FC #62D9FF #9FFFFF #C8FFFF #4CFFFF). */
		SOJ_DISRUPTOR_LAUNCH(SojournPalettes.SOJ_DISRUPTOR_LAUNCH),
		SOJ_DISRUPTOR_LAUNCH_N(SojournPalettes.SOJ_DISRUPTOR_LAUNCH_N),
		/** its enemy row: brown to yellow, HDR 5 (80085554FDB5 r1, peak 5.00; game hex #FC5454 #FF6D62 #FFCA9F #FFFFC0 #FFFF4C). */
		SOJ_DISRUPTOR_LAUNCH_ENEMY(SojournPalettes.SOJ_DISRUPTOR_LAUNCH_ENEMY),
		SOJ_DISRUPTOR_LAUNCH_ENEMY_N(SojournPalettes.SOJ_DISRUPTOR_LAUNCH_ENEMY_N),
		/** the field's ground decal 299E5 x 3DB8 (2228 C21), 6 m: blue, teal, mint, HDR 3.6 (800BA56BCC99 r5, peak 3.63; game hex #00B2FF #A1FFFF #DBFFFF #FFFFFF #FFFFFF). */
		SOJ_FIELD_DECAL(SojournPalettes.SOJ_FIELD_DECAL),
		SOJ_FIELD_DECAL_N(SojournPalettes.SOJ_FIELD_DECAL_N),
		/** the field's line glows C061 (2228 C23), 0.96-1.4 m, 16.6/s: green to ice, HDR 3.9 (80043C79F5F7 r4, peak 3.90; game hex #00FFC6 #63FFFC #A3FFFF #D8FFFF #FFFFFF). */
		SOJ_FIELD_SWIRL(SojournPalettes.SOJ_FIELD_SWIRL),
		SOJ_FIELD_SWIRL_N(SojournPalettes.SOJ_FIELD_SWIRL_N),
		/** their enemy row: magenta, red, orange, cream, HDR 5.9 (80043C79F5F7 r5, peak 5.86; game hex #FF00FF #FF00ED #FF007A #FFB100 #FFFFFF). */
		SOJ_FIELD_SWIRL_ENEMY(SojournPalettes.SOJ_FIELD_SWIRL_ENEMY),
		SOJ_FIELD_SWIRL_ENEMY_N(SojournPalettes.SOJ_FIELD_SWIRL_ENEMY_N),
		/** the field's sphere 1B761 (2228 C42), its 4 m: dark teal to mint, HDR 3 (800D7E44BBB5 r1, peak 3.00; game hex #00617A #00C3F6 #57F1FF #92FFFF #EAFFFF). */
		SOJ_FIELD_DOME(SojournPalettes.SOJ_FIELD_DOME),
		SOJ_FIELD_DOME_N(SojournPalettes.SOJ_FIELD_DOME_N),
		/** its enemy row: black, dark red, peach, HDR 5 (800D7E44BBB5 r2, peak 5.00; game hex #000000 #BA2929 #FF3F3F #FF9D9D #FFFFFF). */
		SOJ_FIELD_DOME_ENEMY(SojournPalettes.SOJ_FIELD_DOME_ENEMY),
		SOJ_FIELD_DOME_ENEMY_N(SojournPalettes.SOJ_FIELD_DOME_ENEMY_N),
		/** the field's flame ring E22E (29132 x 29149, 2228 C33), 11.8 m mesh: navy, green, ice, HDR 2.7 (8007A20C522D r6, peak 2.71; game hex #0034A3 #00AF7D #00FFAC #5BFFFF #E0FFFF). */
		SOJ_FIELD_GROUND(SojournPalettes.SOJ_FIELD_GROUND),
		SOJ_FIELD_GROUND_N(SojournPalettes.SOJ_FIELD_GROUND_N),
		/** its enemy row: plum, red, orange, cream (8007A20C522D r7, peak 2.71; game hex #5B006D #EF0000 #FF923C #FFFB78 #FFFFE0). */
		SOJ_FIELD_GROUND_ENEMY(SojournPalettes.SOJ_FIELD_GROUND_ENEMY),
		SOJ_FIELD_GROUND_ENEMY_N(SojournPalettes.SOJ_FIELD_GROUND_ENEMY_N),
		/** the field's opening flash 4A58 (2228 C31), 4.33 m, 0.35 s: green to ice, HDR 4.2 (80072E35BD9F r0, peak 4.20; game hex #00FF8E #48FFBD #86FFFF #C8FFFF #FFFFFF). */
		SOJ_FIELD_FLASH(SojournPalettes.SOJ_FIELD_FLASH),
		SOJ_FIELD_FLASH_N(SojournPalettes.SOJ_FIELD_FLASH_N),
		/** its enemy row: dark red to pale yellow (80072E35BD9F r1, peak 5.00; game hex #FF0000 #F33D00 #FD6100 #FFFF60 #FFFFB6). */
		SOJ_FIELD_FLASH_ENEMY(SojournPalettes.SOJ_FIELD_FLASH_ENEMY),
		SOJ_FIELD_FLASH_ENEMY_N(SojournPalettes.SOJ_FIELD_FLASH_ENEMY_N),
		/** the field's core sphere DBF (2228 C25), 0.5-0.6 m: teal to pale yellow, HDR 1.6 (800326663928 r3, peak 1.60; game hex #00FFEF #8EFFE5 #D6FFDE #FFFFD1 #FFFFB0). */
		SOJ_FIELD_CORE(SojournPalettes.SOJ_FIELD_CORE),
		SOJ_FIELD_CORE_N(SojournPalettes.SOJ_FIELD_CORE_N),
		/** its enemy row: dark red to peach, HDR 3.3 (800326663928 r2, peak 3.29; game hex #FF0000 #EF5939 #F6905F #FFF9A8 #FFFFF4). */
		SOJ_FIELD_CORE_ENEMY(SojournPalettes.SOJ_FIELD_CORE_ENEMY),
		SOJ_FIELD_CORE_ENEMY_N(SojournPalettes.SOJ_FIELD_CORE_ENEMY_N),
		/** the field's motes 4A58 drawn in (2228 C27), 75/s: green to ice, HDR 4.2 (800D1E8D75EF r0, peak 4.20; game hex #00FF8E #41FFAF #74FFEA #D0FFFF #FFFFFF). */
		SOJ_FIELD_MOTE(SojournPalettes.SOJ_FIELD_MOTE),
		SOJ_FIELD_MOTE_N(SojournPalettes.SOJ_FIELD_MOTE_N),
		/** their enemy row: rust to pale yellow (800D1E8D75EF r1, peak 4.20; game hex #FF4900 #FF7531 #FFB25A #FFFB86 #FFFFFF). */
		SOJ_FIELD_MOTE_ENEMY(SojournPalettes.SOJ_FIELD_MOTE_ENEMY),
		SOJ_FIELD_MOTE_ENEMY_N(SojournPalettes.SOJ_FIELD_MOTE_ENEMY_N),
		/** Deceleration Field's sphere 1385 (6C02 C5), 8 m: black, navy, steel, ice, HDR 5 (80053ECD2398 r8, peak 5.00; game hex #000000 #0F0079 #1B00AF #CCFFFF #FFFFFF). */
		SOJ_DECEL(SojournPalettes.SOJ_DECEL),
		SOJ_DECEL_N(SojournPalettes.SOJ_DECEL_N),
		/** its enemy row: dark red to pink, HDR 2.7 (80053ECD2398 r9, peak 2.71; game hex #7A0000 #F50000 #FF5555 #FF8888 #FFE0E0). */
		SOJ_DECEL_ENEMY(SojournPalettes.SOJ_DECEL_ENEMY),
		SOJ_DECEL_ENEMY_N(SojournPalettes.SOJ_DECEL_ENEMY_N),
		/** Overclock's glows 210FD on the gun hp 007C (2880 C33 / C60), 0.5-1.4 m, 20/s: blue, teal, green (800AD66DBD97 r0, peak 1.34; game hex #0052FF #00A2E9 #00D1CF #00FDA1 #00FF54). */
		SOJ_OC_GLOW(SojournPalettes.SOJ_OC_GLOW),
		SOJ_OC_GLOW_N(SojournPalettes.SOJ_OC_GLOW_N),
		/** their enemy row (2880 C45): plum, red, orange, yellow (800E9644391F r0, peak 1.34; game hex #9800A9 #E81E6F #FF6000 #FFB100 #FFFF00). */
		SOJ_OC_GLOW_ENEMY(SojournPalettes.SOJ_OC_GLOW_ENEMY),
		SOJ_OC_GLOW_ENEMY_N(SojournPalettes.SOJ_OC_GLOW_ENEMY_N),
		/** Overclock's core sphere DBF on the gun (2880 C37), 0.59 m: black, slate, blue, aqua, HDR 2.4 (8008BFCF1E87 r1, peak 2.42; game hex #000000 #002342 #8398BD #00B3FF #00FFFF). */
		SOJ_OC_CORE(SojournPalettes.SOJ_OC_CORE),
		SOJ_OC_CORE_N(SojournPalettes.SOJ_OC_CORE_N),
		/** its enemy row (2880 C49): black, violet, red, yellow (800A11F4443D r1, peak 2.42; game hex #000000 #330042 #B863B4 #FF006A #FFFF00). */
		SOJ_OC_CORE_ENEMY(SojournPalettes.SOJ_OC_CORE_ENEMY),
		SOJ_OC_CORE_ENEMY_N(SojournPalettes.SOJ_OC_CORE_ENEMY_N),
		/** Overclock's vent streaks 2A3EB x 2A3E2 on mesh E613 (2880 C42 / C57): blue, teal, mint, HDR 4.6 (800DF109EACE r2, peak 4.60; game hex #00D7FF #00FFFF #00FFFF #6FFFFF #C9FFFF). */
		SOJ_OC_VENT(SojournPalettes.SOJ_OC_VENT),
		SOJ_OC_VENT_N(SojournPalettes.SOJ_OC_VENT_N),
		/** their enemy row (2880 C54): plum, red, orange, HDR 3.8 (8008BC3FD9C7 r2, peak 3.79; game hex #7E0068 #A00085 #FF7383 #FFB96E #FFF200). */
		SOJ_OC_VENT_ENEMY(SojournPalettes.SOJ_OC_VENT_ENEMY),
		SOJ_OC_VENT_ENEMY_N(SojournPalettes.SOJ_OC_VENT_ENEMY_N),
		/** Overclock's material effect on her (333B C0, hp 0004): cyan falling to black, HDR 6.6 (80035FEA47FC r1, peak 6.59, falls; game hex #00FFFF #009E98 #005350 #001A19 #000000). */
		SOJ_OC_BODY(SojournPalettes.SOJ_OC_BODY),
		SOJ_OC_BODY_N(SojournPalettes.SOJ_OC_BODY_N),
		// Sigma's (tools/reference/sigma-effects.md; their stops in SigmaPalettes, which holds every row his pass read: only those
		// drawn are named here, the enum's initializer being near the class file's 64 KB)
		SIG_SPHERE_LAUNCH(SigmaPalettes.SIG_SPHERE_LAUNCH),
		SIG_SPHERE_LAUNCH_RING(SigmaPalettes.SIG_SPHERE_LAUNCH_RING),
		SIG_SPHERE_LAUNCH_RING_ENEMY(SigmaPalettes.SIG_SPHERE_LAUNCH_RING_ENEMY),
		SIG_SPHERE_CORE(SigmaPalettes.SIG_SPHERE_CORE),
		SIG_SPHERE_CORE_ENEMY(SigmaPalettes.SIG_SPHERE_CORE_ENEMY),
		SIG_SPHERE_TRAIL(SigmaPalettes.SIG_SPHERE_TRAIL),
		SIG_SPHERE_TRAIL_ENEMY(SigmaPalettes.SIG_SPHERE_TRAIL_ENEMY),
		SIG_SPHERE_TRAIL_THIN(SigmaPalettes.SIG_SPHERE_TRAIL_THIN),
		SIG_SPHERE_TRAIL_THIN_ENEMY(SigmaPalettes.SIG_SPHERE_TRAIL_THIN_ENEMY),
		SIG_SPHERE_HALO(SigmaPalettes.SIG_SPHERE_HALO),
		SIG_SPHERE_HALO_ENEMY(SigmaPalettes.SIG_SPHERE_HALO_ENEMY),
		SIG_SPHERE_MOTE(SigmaPalettes.SIG_SPHERE_MOTE),
		SIG_BURST_RING(SigmaPalettes.SIG_BURST_RING),
		SIG_BURST_RING_ENEMY(SigmaPalettes.SIG_BURST_RING_ENEMY),
		SIG_BURST_STREAK(SigmaPalettes.SIG_BURST_STREAK),
		SIG_BURST_STREAK_ENEMY(SigmaPalettes.SIG_BURST_STREAK_ENEMY),
		SIG_BURST_SHELL(SigmaPalettes.SIG_BURST_SHELL),
		SIG_BURST_SHELL_ENEMY(SigmaPalettes.SIG_BURST_SHELL_ENEMY),
		SIG_BURST_FLASH(SigmaPalettes.SIG_BURST_FLASH),
		SIG_BURST_FLASH_ENEMY(SigmaPalettes.SIG_BURST_FLASH_ENEMY),
		SIG_HAND_CORE(SigmaPalettes.SIG_HAND_CORE),
		SIG_HAND_SPARK(SigmaPalettes.SIG_HAND_SPARK),
		SIG_HAND_SPARK_ENEMY(SigmaPalettes.SIG_HAND_SPARK_ENEMY),
		SIG_HAND_GLOW(SigmaPalettes.SIG_HAND_GLOW),
		SIG_HAND_MATERIAL(SigmaPalettes.SIG_HAND_MATERIAL),
		SIG_HAND_EMISSION(SigmaPalettes.SIG_HAND_EMISSION),
		SIG_BARRIER(SigmaPalettes.SIG_BARRIER),
		SIG_BARRIER_ENEMY(SigmaPalettes.SIG_BARRIER_ENEMY),
		SIG_BARRIER_MOTE(SigmaPalettes.SIG_BARRIER_MOTE),
		SIG_BARRIER_MOTE_ENEMY(SigmaPalettes.SIG_BARRIER_MOTE_ENEMY),
		SIG_BARRIER_SEND(SigmaPalettes.SIG_BARRIER_SEND),
		SIG_BARRIER_HAND(SigmaPalettes.SIG_BARRIER_HAND),
		SIG_BARRIER_FLYBACK(SigmaPalettes.SIG_BARRIER_FLYBACK),
		SIG_BARRIER_FLYBACK_ENEMY(SigmaPalettes.SIG_BARRIER_FLYBACK_ENEMY),
		SIG_BARRIER_RECALL_FLASH(SigmaPalettes.SIG_BARRIER_RECALL_FLASH),
		SIG_BARRIER_RECALL_FLASH_ENEMY(SigmaPalettes.SIG_BARRIER_RECALL_FLASH_ENEMY),
		SIG_BARRIER_RECALL_GLOW(SigmaPalettes.SIG_BARRIER_RECALL_GLOW),
		SIG_BARRIER_RECALL_GLOW_ENEMY(SigmaPalettes.SIG_BARRIER_RECALL_GLOW_ENEMY),
		SIG_BARRIER_BREAK(SigmaPalettes.SIG_BARRIER_BREAK),
		SIG_BARRIER_BREAK_ENEMY(SigmaPalettes.SIG_BARRIER_BREAK_ENEMY),
		SIG_BARRIER_BREAK_SPARK(SigmaPalettes.SIG_BARRIER_BREAK_SPARK),
		SIG_BARRIER_BREAK_SPARK_ENEMY(SigmaPalettes.SIG_BARRIER_BREAK_SPARK_ENEMY),
		SIG_BARRIER_BREAK_STREAK(SigmaPalettes.SIG_BARRIER_BREAK_STREAK),
		SIG_BARRIER_SHATTER(SigmaPalettes.SIG_BARRIER_SHATTER),
		SIG_BARRIER_HEAL(SigmaPalettes.SIG_BARRIER_HEAL),
		SIG_BARRIER_HEAL_ENEMY(SigmaPalettes.SIG_BARRIER_HEAL_ENEMY),
		SIG_GRASP_FIELD(SigmaPalettes.SIG_GRASP_FIELD),
		SIG_GRASP_SHELL(SigmaPalettes.SIG_GRASP_SHELL),
		SIG_GRASP_SHELL_ENEMY(SigmaPalettes.SIG_GRASP_SHELL_ENEMY),
		SIG_GRASP_GLOW(SigmaPalettes.SIG_GRASP_GLOW),
		SIG_GRASP_GLOW_ENEMY(SigmaPalettes.SIG_GRASP_GLOW_ENEMY),
		SIG_GRASP_ORB(SigmaPalettes.SIG_GRASP_ORB),
		SIG_GRASP_ORB_ENEMY(SigmaPalettes.SIG_GRASP_ORB_ENEMY),
		SIG_GRASP_HAZE(SigmaPalettes.SIG_GRASP_HAZE),
		SIG_GRASP_ABSORB(SigmaPalettes.SIG_GRASP_ABSORB),
		SIG_GRASP_ABSORB_ENEMY(SigmaPalettes.SIG_GRASP_ABSORB_ENEMY),
		SIG_GRASP_ABSORB_RING(SigmaPalettes.SIG_GRASP_ABSORB_RING),
		SIG_GRASP_ABSORB_RING_ENEMY(SigmaPalettes.SIG_GRASP_ABSORB_RING_ENEMY),
		SIG_OVERHEALTH(SigmaPalettes.SIG_OVERHEALTH),
		SIG_OVERHEALTH_ENEMY(SigmaPalettes.SIG_OVERHEALTH_ENEMY),
		SIG_OVERHEALTH_STREAK(SigmaPalettes.SIG_OVERHEALTH_STREAK),
		SIG_OVERHEALTH_SHELL(SigmaPalettes.SIG_OVERHEALTH_SHELL),
		SIG_OVERHEALTH_SHELL_ENEMY(SigmaPalettes.SIG_OVERHEALTH_SHELL_ENEMY),
		SIG_ROCK_GATHER(SigmaPalettes.SIG_ROCK_GATHER),
		SIG_ROCK_GATHER_ENEMY(SigmaPalettes.SIG_ROCK_GATHER_ENEMY),
		SIG_ROCK_GATHER_DISC(SigmaPalettes.SIG_ROCK_GATHER_DISC),
		SIG_ROCK_GATHER_FLASH(SigmaPalettes.SIG_ROCK_GATHER_FLASH),
		SIG_ROCK_TRAIL(SigmaPalettes.SIG_ROCK_TRAIL),
		SIG_ROCK_TRAIL_ENEMY(SigmaPalettes.SIG_ROCK_TRAIL_ENEMY),
		SIG_ROCK_IMPACT_FLASH(SigmaPalettes.SIG_ROCK_IMPACT_FLASH),
		SIG_ROCK_IMPACT_FLASH_ENEMY(SigmaPalettes.SIG_ROCK_IMPACT_FLASH_ENEMY),
		SIG_ROCK_IMPACT_GLOW(SigmaPalettes.SIG_ROCK_IMPACT_GLOW),
		SIG_ROCK_IMPACT_GLOW_ENEMY(SigmaPalettes.SIG_ROCK_IMPACT_GLOW_ENEMY),
		SIG_ROCK_IMPACT_DUST(SigmaPalettes.SIG_ROCK_IMPACT_DUST),
		SIG_ROCK_IMPACT_DUST_ENEMY(SigmaPalettes.SIG_ROCK_IMPACT_DUST_ENEMY),
		SIG_ROCK_SCORCH(SigmaPalettes.SIG_ROCK_SCORCH),
		SIG_ROCK_SCORCH_ENEMY(SigmaPalettes.SIG_ROCK_SCORCH_ENEMY),
		SIG_ROCK_SMOKE(SigmaPalettes.SIG_ROCK_SMOKE),
		SIG_ROCK_CORE(SigmaPalettes.SIG_ROCK_CORE),
		SIG_FLUX_RETICLE(SigmaPalettes.SIG_FLUX_RETICLE),
		SIG_FLUX_RETICLE_DECAL(SigmaPalettes.SIG_FLUX_RETICLE_DECAL),
		SIG_FLUX_BODY(SigmaPalettes.SIG_FLUX_BODY),
		SIG_FLUX_BODY_ENEMY(SigmaPalettes.SIG_FLUX_BODY_ENEMY),
		SIG_FLUX_BODY_HALO(SigmaPalettes.SIG_FLUX_BODY_HALO),
		SIG_FLUX_BODY_HALO_ENEMY(SigmaPalettes.SIG_FLUX_BODY_HALO_ENEMY),
		SIG_FLUX_COLUMN(SigmaPalettes.SIG_FLUX_COLUMN),
		SIG_FLUX_COLUMN_ENEMY(SigmaPalettes.SIG_FLUX_COLUMN_ENEMY),
		SIG_FLUX_COLUMN_OUTER(SigmaPalettes.SIG_FLUX_COLUMN_OUTER),
		SIG_FLUX_COLUMN_OUTER_ENEMY(SigmaPalettes.SIG_FLUX_COLUMN_OUTER_ENEMY),
		SIG_FLUX_DECAL(SigmaPalettes.SIG_FLUX_DECAL),
		SIG_FLUX_DECAL_ENEMY(SigmaPalettes.SIG_FLUX_DECAL_ENEMY),
		SIG_FLUX_SPARK(SigmaPalettes.SIG_FLUX_SPARK),
		SIG_FLUX_PULL(SigmaPalettes.SIG_FLUX_PULL),
		SIG_FLUX_PULL_ENEMY(SigmaPalettes.SIG_FLUX_PULL_ENEMY),
		SIG_FLUX_PULL_SPARK(SigmaPalettes.SIG_FLUX_PULL_SPARK),
		SIG_FLUX_PULL_SPARK_ENEMY(SigmaPalettes.SIG_FLUX_PULL_SPARK_ENEMY),
		SIG_LIFT_RING(SigmaPalettes.SIG_LIFT_RING),
		SIG_LIFT_RING_ENEMY(SigmaPalettes.SIG_LIFT_RING_ENEMY),
		SIG_LIFT_TRAIL(SigmaPalettes.SIG_LIFT_TRAIL),
		SIG_LIFT_TRAIL_ENEMY(SigmaPalettes.SIG_LIFT_TRAIL_ENEMY),
		SIG_LIFT_TRAIL_DARK(SigmaPalettes.SIG_LIFT_TRAIL_DARK),
		SIG_LIFT_TRAIL_DARK_ENEMY(SigmaPalettes.SIG_LIFT_TRAIL_DARK_ENEMY),
		SIG_LIFTED_GLOW(SigmaPalettes.SIG_LIFTED_GLOW),
		SIG_LIFTED_GLOW_ENEMY(SigmaPalettes.SIG_LIFTED_GLOW_ENEMY),
		SIG_LIFTED_RING(SigmaPalettes.SIG_LIFTED_RING),
		SIG_LIFTED_RING_ENEMY(SigmaPalettes.SIG_LIFTED_RING_ENEMY),
		SIG_LIFTOFF_DUST(SigmaPalettes.SIG_LIFTOFF_DUST),
		SIG_LEVITATE(SigmaPalettes.SIG_LEVITATE),
		SIG_LEVITATE_ENEMY(SigmaPalettes.SIG_LEVITATE_ENEMY),
		SIG_HYPER_STRIKE(SigmaPalettes.SIG_HYPER_STRIKE),
		SIG_HYPER_STRIKE_ENEMY(SigmaPalettes.SIG_HYPER_STRIKE_ENEMY),
		SIG_HYPER_STRIKE_SPARK(SigmaPalettes.SIG_HYPER_STRIKE_SPARK),
		SIG_HYPER_STRIKE_SPARK_ENEMY(SigmaPalettes.SIG_HYPER_STRIKE_SPARK_ENEMY),
		// Torbjorn's (tools/reference/torbjorn-effects.md; their stops and sources in HeroPalettes)
		TB_OVERLOAD(HeroPalettes.TB_OVERLOAD),
		TB_OVERLOAD_ENEMY(HeroPalettes.TB_OVERLOAD_ENEMY),
		TB_OVERLOAD_B(HeroPalettes.TB_OVERLOAD_B),
		TB_OVERLOAD_B_ENEMY(HeroPalettes.TB_OVERLOAD_B_ENEMY),
		TB_OVERLOAD_SMOKE(HeroPalettes.TB_OVERLOAD_SMOKE),
		TB_OVERLOAD_SPARK(HeroPalettes.TB_OVERLOAD_SPARK),
		TB_MOLTEN_GLOW(HeroPalettes.TB_MOLTEN_GLOW),
		TB_MOLTEN_GLOW_ENEMY(HeroPalettes.TB_MOLTEN_GLOW_ENEMY),
		TB_MOLTEN_FLAME(HeroPalettes.TB_MOLTEN_FLAME),
		TB_MOLTEN_FLAME_ENEMY(HeroPalettes.TB_MOLTEN_FLAME_ENEMY),
		TB_MOLTEN_SPARK(HeroPalettes.TB_MOLTEN_SPARK),
		TB_MOLTEN_SPARK_ENEMY(HeroPalettes.TB_MOLTEN_SPARK_ENEMY),
		TB_SLAG_LAUNCH(HeroPalettes.TB_SLAG_LAUNCH),
		TB_SLAG_LAUNCH_ENEMY(HeroPalettes.TB_SLAG_LAUNCH_ENEMY),
		TB_SLAG_GLOW(HeroPalettes.TB_SLAG_GLOW),
		TB_SLAG_GLOW_ENEMY(HeroPalettes.TB_SLAG_GLOW_ENEMY),
		TB_SLAG_TRAIL(HeroPalettes.TB_SLAG_TRAIL),
		TB_SLAG_TRAIL_ENEMY(HeroPalettes.TB_SLAG_TRAIL_ENEMY),
		TB_SLAG_DRIP(HeroPalettes.TB_SLAG_DRIP),
		TB_POOL_FIRE(HeroPalettes.TB_POOL_FIRE),
		TB_POOL_FIRE_ENEMY(HeroPalettes.TB_POOL_FIRE_ENEMY),
		TB_POOL_FLASH(HeroPalettes.TB_POOL_FLASH),
		TB_POOL_FLASH_ENEMY(HeroPalettes.TB_POOL_FLASH_ENEMY),
		TB_POOL_LAVA(HeroPalettes.TB_POOL_LAVA),
		TB_POOL_LAVA_ENEMY(HeroPalettes.TB_POOL_LAVA_ENEMY),
		TB_POOL_EMBER(HeroPalettes.TB_POOL_EMBER),
		TB_BURN(HeroPalettes.TB_BURN),
		TB_BURN_B(HeroPalettes.TB_BURN_B),
		TB_BURN_RING(HeroPalettes.TB_BURN_RING),
		TB_RIVET(HeroPalettes.TB_RIVET),
		TB_RIVET_TRAIL(HeroPalettes.TB_RIVET_TRAIL),
		TB_MUZZLE(HeroPalettes.TB_MUZZLE),
		TB_MUZZLE_ENEMY(HeroPalettes.TB_MUZZLE_ENEMY),
		TB_MUZZLE_B(HeroPalettes.TB_MUZZLE_B),
		TB_MUZZLE_B_ENEMY(HeroPalettes.TB_MUZZLE_B_ENEMY),
		TB_MUZZLE_SMOKE(HeroPalettes.TB_MUZZLE_SMOKE),
		TB_RIVET_SPARK(HeroPalettes.TB_RIVET_SPARK),
		TB_RIVET_HIT(HeroPalettes.TB_RIVET_HIT),
		TB_PELLET_TRAIL(HeroPalettes.TB_PELLET_TRAIL),
		TB_PELLET_TRAIL_ENEMY(HeroPalettes.TB_PELLET_TRAIL_ENEMY),
		TB_PELLET_MUZZLE_ENEMY(HeroPalettes.TB_PELLET_MUZZLE_ENEMY),
		TB_PELLET_HIT(HeroPalettes.TB_PELLET_HIT),
		TB_RELOAD_GLOW(HeroPalettes.TB_RELOAD_GLOW),
		TB_RELOAD_GLOW_B(HeroPalettes.TB_RELOAD_GLOW_B),
		TB_RELOAD_3P(HeroPalettes.TB_RELOAD_3P),
		TB_TURRET_FLASH(HeroPalettes.TB_TURRET_FLASH),
		TB_TURRET_SMOKE(HeroPalettes.TB_TURRET_SMOKE),
		TB_THROWN(HeroPalettes.TB_THROWN),
		TB_TURRET_LINE(HeroPalettes.TB_TURRET_LINE),
		// Cassidy's (cassidy-effects.md; their rows on HeroPalettes' arrays)
		CS_LINE(HeroPalettes.CS_LINE),
		CS_LINE_ENEMY(HeroPalettes.CS_LINE_ENEMY),
		CS_DEADEYE_LINE(HeroPalettes.CS_DEADEYE_LINE),
		CS_SMOKE_LINE(HeroPalettes.CS_SMOKE_LINE),
		CS_SILVER_LINE(HeroPalettes.CS_SILVER_LINE),
		CS_DEADEYE_TUBE(HeroPalettes.CS_DEADEYE_TUBE),
		CS_MUZZLE_FLASH(HeroPalettes.CS_MUZZLE_FLASH),
		CS_MUZZLE_FLASH_C(HeroPalettes.CS_MUZZLE_FLASH_C),
		CS_FAN_FLASH(HeroPalettes.CS_FAN_FLASH),
		CS_MUZZLE_SPARK(HeroPalettes.CS_MUZZLE_SPARK),
		CS_MUZZLE_RING(HeroPalettes.CS_MUZZLE_RING),
		CS_MUZZLE_SMOKE(HeroPalettes.CS_MUZZLE_SMOKE),
		CS_HIT(HeroPalettes.CS_HIT),
		CS_HIT_ENEMY(HeroPalettes.CS_HIT_ENEMY),
		CS_HIT_SHARD(HeroPalettes.CS_HIT_SHARD),
		CS_HIT_SHARD_ENEMY(HeroPalettes.CS_HIT_SHARD_ENEMY),
		CS_HIT_DECAL(HeroPalettes.CS_HIT_DECAL),
		CS_HIT_DECAL_ENEMY(HeroPalettes.CS_HIT_DECAL_ENEMY),
		CS_RELOAD_SMOKE(HeroPalettes.CS_RELOAD_SMOKE),
		CS_SILVER_ARMED(HeroPalettes.CS_SILVER_ARMED),
		CS_SILVER_ARMED_ENEMY(HeroPalettes.CS_SILVER_ARMED_ENEMY),
		CS_BLEED(HeroPalettes.CS_BLEED),
		CS_BLEED_SPARK(HeroPalettes.CS_BLEED_SPARK),
		CS_ROLL_DUST(HeroPalettes.CS_ROLL_DUST),
		CS_FLASH(HeroPalettes.CS_FLASH),
		CS_FLASH_ENEMY(HeroPalettes.CS_FLASH_ENEMY),
		CS_FLASH_DOME(HeroPalettes.CS_FLASH_DOME),
		CS_FLASH_DOME_ENEMY(HeroPalettes.CS_FLASH_DOME_ENEMY),
		CS_FLASH_RAYS(HeroPalettes.CS_FLASH_RAYS),
		CS_FLASH_STREAK(HeroPalettes.CS_FLASH_STREAK),
		CS_FLASH_STREAK_ENEMY(HeroPalettes.CS_FLASH_STREAK_ENEMY),
		CS_FLASH_TRAIL(HeroPalettes.CS_FLASH_TRAIL),
		CS_FLASH_TRAIL_B(HeroPalettes.CS_FLASH_TRAIL_B),
		CS_DEADEYE_EYE(HeroPalettes.CS_DEADEYE_EYE),
		CS_DEADEYE_STAR(HeroPalettes.CS_DEADEYE_STAR),
		CS_DEADEYE_GLOW(HeroPalettes.CS_DEADEYE_GLOW),
		CS_DEADEYE_GLOW_ENEMY(HeroPalettes.CS_DEADEYE_GLOW_ENEMY),
		CS_DEADEYE_DECAL(HeroPalettes.CS_DEADEYE_DECAL),
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
		dev.overcraft.hero.PressTrace.mark("art_miss:" + key);
		long t0 = System.nanoTime();
		NativeImage out = coloured(name, palette, soft);
		if (out == null) {
			return null;
		}
		Identifier id = Overcraft.id("effect/" + key);
		Minecraft.getInstance().getTextureManager().register(id, new HeroTexture(id, out, MipmapStrategy.MEAN));
		missNanos += System.nanoTime() - t0;
		missKeys.add(name + "," + palette + (soft ? ",soft" : ""));
		return id;
	}

	/** Art made on the client's thread, in the frame that wanted it (#128): logged by {@link #frame}, with that frame's time. */
	private static long missNanos;
	private static final java.util.List<String> missKeys = new java.util.ArrayList<>();
	private static long lastFrame;

	/** The mask coloured with the palette, not yet a texture: read from the pack and worked out pixel by pixel. */
	private static @Nullable NativeImage coloured(String name, Palette palette, boolean soft) {
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
			return out;
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
		QUEUED.clear();
		generation++;
	}

	// ---- ahead of its first use (#126) ----

	/** One piece of a hero's art: a mask ("name", or "name#frames" as Fx.particle has it), its palette, blended or added. */
	public record Art(String mask, Palette palette, boolean soft) {
	}

	private record Made(String key, NativeImage image, int generation) {
	}

	/** Coloured on the worker, waiting for the client's thread to make them textures. */
	private static final java.util.Queue<Made> MADE = new java.util.concurrent.ConcurrentLinkedQueue<>();
	private static final java.util.concurrent.atomic.AtomicInteger OUTSTANDING = new java.util.concurrent.atomic.AtomicInteger();
	/** Sent to the worker: CACHE only knows what's finished, and she and another Sojourn's list share art. */
	private static final java.util.Set<String> QUEUED = new java.util.HashSet<>();
	private static long madeBytes;
	/** What a frame may spend turning them into textures: ours, under a frame at 144 Hz by far. */
	private static final long FRAME_BUDGET_NANOS = 1_500_000;
	private static volatile int generation;
	private static java.util.concurrent.@Nullable ExecutorService maker;

	public static int generation() {
		return generation;
	}

	/** Texture memory of what was made ahead, mips included (the harness). */
	public static long madeBytes() {
		return madeBytes;
	}

	/** How many are still on their way (the harness). */
	public static int outstanding() {
		return OUTSTANDING.get();
	}

	/**
	 * A hero's art made before it's wanted. Its first use read every mask from the pack and coloured it pixel by pixel on the client's
	 * thread, in the frame of the shot: 930 ms of two frames on Sojourn's first Charged Shot, 252 of 290 samples of the thread in
	 * {@link #load} (#126). Here a low-priority worker does that part, and each frame makes a few textures of what's ready. Asked for
	 * before it's ready, it loads as it did, and the worker's copy is dropped.
	 */
	public static void prepare(java.util.List<Art> arts) {
		if (maker == null) {
			maker = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
				Thread t = new Thread(r, "Overcraft effect art");
				t.setDaemon(true);
				t.setPriority(Thread.MIN_PRIORITY);
				return t;
			});
		}
		int now = generation;
		for (Art art : arts) {
			int hash = art.mask().indexOf('#');
			int frames = hash >= 0 ? Integer.parseInt(art.mask().substring(hash + 1)) : 1;
			for (int i = 0; i < frames; i++) {
				String name = hash >= 0 ? art.mask().substring(0, hash) + "_" + i : art.mask();
				String key = name + "/" + art.palette().name().toLowerCase(Locale.ROOT) + (art.soft() ? "_soft" : "");
				if (!CACHE.containsKey(key) && QUEUED.add(key)) {
					OUTSTANDING.incrementAndGet();
					maker.execute(() -> {
						try {
							NativeImage image = coloured(name, art.palette(), art.soft());
							if (image != null) {
								MADE.add(new Made(key, image, now));
								return;
							}
						} catch (RuntimeException e) {
							Overcraft.LOG.warn("Effect art {} wasn't made ahead: {}", key, e.toString());
						}
						OUTSTANDING.decrementAndGet();
					});
				}
			}
		}
	}

	/** Each frame, on the client's thread: textures from what the worker has made, within the budget. */
	public static void frame() {
		long now = System.nanoTime();
		if (!missKeys.isEmpty()) {
			// the frame that paid for it, from the last one's end: a hitch the player sees, and what to make ahead (#128)
			Overcraft.LOG.info("Effect art made on first use, hero {}: {} in {} ms, frame {} ms: {}", dev.overcraft.client.ClientHero.hero(),
				missKeys.size(), missNanos / 1_000_000, lastFrame == 0 ? -1 : (now - lastFrame) / 1_000_000, String.join(" ", missKeys));
			missKeys.clear();
			missNanos = 0;
		}
		lastFrame = now;
		Made made = MADE.peek();
		if (made == null) {
			return;
		}
		long end = System.nanoTime() + FRAME_BUDGET_NANOS;
		while ((made = MADE.poll()) != null) {
			OUTSTANDING.decrementAndGet();
			if (made.generation() == generation && !CACHE.containsKey(made.key())) {
				Identifier id = Overcraft.id("effect/" + made.key());
				madeBytes += made.image().getWidth() * (long) made.image().getHeight() * 4 * 4 / 3;
				Minecraft.getInstance().getTextureManager().register(id, new HeroTexture(id, made.image(), MipmapStrategy.MEAN));
				CACHE.put(made.key(), Optional.of(id));
			} else {
				made.image().close();
			}
			if (System.nanoTime() >= end) {
				break;
			}
		}
		long spent = System.nanoTime() - now;
		if (spent > 10_000_000) {
			// a texture well over the budget: worth knowing (#128)
			Overcraft.LOG.info("Effect art made ahead: {} ms of a frame on textures, {} left", spent / 1_000_000, OUTSTANDING.get());
		}
	}
}
