package dev.overcraft.client.effect;

/**
 * Widowmaker's palettes' stops (EffectArt.Palette.WIDOW_*), apart from HeroPalettes as AshePalettes are. Each is (mask value, r, g, b)
 * stops, linear, black at 0, read from her effects' colour LUTs (tools/reference/widowmaker-effects.md "Proposed palettes", from
 * fx_widow_pal.py: HDR rows hue-clamped, a near-white top dropped so a solid mask ends on the colour, falling rows reversed). "XXXX rN" is
 * the LUT and its row. Rows the note gives the same numbers under several roles are one array here (WidowmakerFx says which): the full
 * charge's smoke bands (8B64 r5 / D44D r5) are WIDOW_SMOKE / WIDOW_SMOKE_ENEMY's numbers, its core (818E r4) WIDOW_CORE's within 0.01.
 */
final class WidowmakerPalettes {
	private WidowmakerPalettes() {
	}

	/** 3B92 r4 HDR 6.98: hip line 510 x 50C, 0.035 m, 0.15 s (0066 C7). */
	static final float[] WIDOW_LINE = {0, 0, 0, 0, .25f, 1, .163f, .051f, .5f, 1, .154f, .048f, .75f, 1, .147f, .046f, 1, 1, .14f, .044f};
	/** 7ED6 r4 HDR 6.98: its twin (0066 C9). */
	static final float[] WIDOW_LINE_ENEMY = {0, 0, 0, 0, .25f, 1, .056f, .051f, .5f, 1, .054f, .048f, .75f, 1, .051f, .046f, 1, 1, .049f, .044f};
	/** 35AC r4 HDR 1.09: scoped < 50 % core 510 x 50C 0.158 m, 1 s (0118 C14). */
	static final float[] WIDOW_CORE = {0, 0, 0, 0, .25f, 1, .282f, 0, .5f, 1, .282f, 0, .75f, 1, .282f, 0, 1, 1, .282f, 0};
	/** 7C8A r2 HDR 5.49: the glow band 965, 0.1 / 0.5 m, 0.3 s (0067 C23 / C27, 0117 C25). */
	static final float[] WIDOW_GLOW = {0, 0, 0, 0, .25f, 1, .308f, .094f, .5f, 1, .308f, .094f, .75f, 1, .308f, .094f, 1, 1, .308f, .094f};
	/** 33BA r6 HDR 5, white top dropped: smoke band 4D5 x B35 x 965, 0.733 m, 3 s (0118 C16; C12 BFED r6 0.5 m, 0067 C19 6BA2 r6 the same). */
	static final float[] WIDOW_SMOKE = {0, 0, 0, 0, .33f, 1, 1, 1, .67f, 1, 1, 1, 1, 1, 1, 1};
	/** 609B r6 HDR 5: its twin (0118 C18; 0067 C25 0219 r6 the same). */
	static final float[] WIDOW_SMOKE_ENEMY = {0, 0, 0, 0, .25f, 1, .058f, 0, .5f, 1, .058f, 0, .75f, 1, .058f, 0, 1, 1, .058f, 0};
	/** 9BF5 r5 HDR 6: hip muzzle flash 299E1 x 3269F (4) (18F7 C15). */
	static final float[] WIDOW_MUZZLE = {0, 0, 0, 0, .25f, 1, .417f, 0, .5f, 1, .488f, .002f, .75f, 1, .674f, .136f, 1, 1, .783f, .216f};
	/** 1E64 r6 HDR 1.43: the red row of the same mask (18F7 C27). */
	static final float[] WIDOW_MUZZLE_ENEMY = {0, 0, 0, 0, .25f, 1, .043f, .024f, .5f, 1, .143f, .09f, .75f, 1, .242f, .156f, 1, 1, .577f, .485f};
	/** DFEE r0 HDR 1.43: hip muzzle glow 2007A (18F7 C25). */
	static final float[] WIDOW_MUZZLE_GLOW = {0, 0, 0, 0, .25f, 1, .518f, .024f, .5f, 1, .609f, .089f, .75f, 1, .699f, .154f, 1, .254f, .658f, 1};
	/** 86EF r0: scoped muzzle flare DA5B (18FB / 1FD6 / 18F9 C18). */
	static final float[] WIDOW_FLARE = {0, 0, 0, 0, .25f, 1, 1, 0, .5f, 1, 1, 0, .75f, 1, 1, .518f, 1, 1, 1, .518f};
	/** 6AA6 r2 HDR 30.25, white top dropped: hit sparks 1BE6 (4) (010F C17, 0110 C11). */
	static final float[] WIDOW_HIT_SPARK = {0, 0, 0, 0, .33f, .992f, 1, .992f, .67f, .992f, 1, .992f, 1, .992f, 1, .992f};
	/** 76F6 r2 HDR 30: its twin (010F C12, 0110 C15). */
	static final float[] WIDOW_HIT_SPARK_ENEMY = {0, 0, 0, 0, .25f, 1, .051f, .033f, .5f, 1, .051f, .033f, .75f, 1, .051f, .033f, 1, 1, .051f, .033f};
	/** 0304 r2 HDR 30: hit star 1BEC (2) on mesh EF7 (010F C19, 0110 C13). */
	static final float[] WIDOW_HIT_STAR = {0, 0, 0, 0, .25f, 1, .867f, .267f, .5f, 1, .867f, .267f, .75f, 1, .867f, .267f, 1, 1, .867f, .267f};
	/** 2B70 r2 HDR 30: its twin (010F C14, 0110 C17). */
	static final float[] WIDOW_HIT_STAR_ENEMY = {0, 0, 0, 0, .25f, 1, .15f, .042f, .5f, 1, .15f, .042f, .75f, 1, .15f, .042f, 1, 1, .15f, .042f};
	/** 35DA r2 HDR 4, reversed: the hip hit's decal 220A, 0.667 s (010F C10 / C15). */
	static final float[] WIDOW_HIT_DECAL = {0, 0, 0, 0, .25f, 1, .056f, 0, .5f, 1, .12f, 0, .75f, 1, .185f, 0, 1, 1, .25f, 0};
	/** C636 r4: the cast puff 1DC68 x 1DC1F (0291 C5, 0285 C5). */
	static final float[] WIDOW_CAST = {0, 0, 0, 0, .25f, .174f, 0, .39f, .5f, .347f, 0, .78f, .75f, .557f, .187f, 1, 1, .823f, .631f, 1};
	/** A089 r0 HDR 7, reversed: the cast sparks 1F35 (0291 C7, 0285 C7). */
	static final float[] WIDOW_CAST_SPARK = {0, 0, 0, 0, .25f, 1, .501f, .034f, .5f, 1, .624f, .022f, .75f, 1, .682f, .027f, 1, 1, .714f, .032f};
	/** texture 1DD6: the rope's own colour texture 1DD6 (0277 C3). */
	static final float[] WIDOW_ROPE = {0, 0, 0, 0, .25f, .319f, .319f, .319f, .5f, .332f, .328f, .332f, .75f, .402f, .402f, .402f, 1, .491f, .491f, .491f};
	/** 8EE5 r2 HDR 3: the mine's flash 242 3.7 m (00FE C6; 6DBF C6 the same). */
	static final float[] WIDOW_MINE_FLASH_RED = {0, 0, 0, 0, .25f, 1, .006f, 0, .5f, 1, .006f, 0, .75f, 1, .006f, 0, 1, 1, .006f, 0};
	/** B4DA r2 HDR 3: the mine's flash 242 2.07 m (00FE C8; 6DBF C8). */
	static final float[] WIDOW_MINE_FLASH_BLUE = {0, 0, 0, 0, .25f, 0, .006f, 1, .5f, 0, .006f, 1, .75f, 0, .006f, 1, 1, 0, .006f, 1};
	/** FD44 r1 HDR 4.88: the thrown mine's ribbon AC4 0.5 m, 0.5 s (18F8 C6). */
	static final float[] WIDOW_MINE_TRAIL = {0, 0, 0, 0, .25f, .184f, 0, 1, .5f, .16f, 0, 1, .75f, .141f, 0, 1, 1, .125f, 0, 1};
	/** 0665 r7: burst half dome E531 5.4-5.9 m, 0.67 s (00EC C23). */
	static final float[] WIDOW_BURST_SHELL = {0, 0, 0, 0, .25f, .2f, 0, .324f, .5f, .399f, 0, .648f, .75f, .508f, 0, .83f, 1, .61f, 0, 1};
	/** A504 r7: its twin (00EC C19). */
	static final float[] WIDOW_BURST_SHELL_ENEMY = {0, 0, 0, 0, .25f, .324f, 0, 0, .5f, .648f, 0, 0, .75f, .83f, 0, 0, 1, 1, 0, 0};
	/** 5F6E r7: burst hemisphere 963D 2.4 +- 0.3, 0.92 s (00EC C25). */
	static final float[] WIDOW_BURST_HEMI = {0, 0, 0, 0, .25f, .102f, 0, .132f, .5f, .204f, 0, .264f, .75f, .305f, 0, .395f, 1, .407f, 0, .527f};
	/** B62E r7: its twin (00EC C21). */
	static final float[] WIDOW_BURST_HEMI_ENEMY = {0, 0, 0, 0, .25f, .227f, 0, 0, .5f, .454f, 0, 0, .75f, .677f, 0, 0, 1, .904f, 0, 0};
	/** B7E9 r4: burst sphere 85B 3 m, 0.17 s (00EC C29). */
	static final float[] WIDOW_BURST_SPHERE = {0, 0, 0, 0, .25f, .118f, .033f, .118f, .5f, .236f, .066f, .236f, .75f, .315f, .036f, .429f, 1, .49f, .145f, .702f};
	/** CD26 r4: its twin (00EC C27). */
	static final float[] WIDOW_BURST_SPHERE_ENEMY = {0, 0, 0, 0, .25f, .118f, .033f, .033f, .5f, .235f, .066f, .066f, .75f, .428f, .036f, .036f, 1, .701f, .144f, .144f};
	/** 429E r5: burst flames 1CAFA on mesh E58C (00EC C35). */
	static final float[] WIDOW_BURST_FLAME = {0, 0, 0, 0, .25f, .075f, 0, .089f, .5f, .151f, 0, .178f, .75f, .226f, 0, .266f, 1, .418f, 0, .491f};
	/** 017E r5: its twin (00EC C32). */
	static final float[] WIDOW_BURST_FLAME_ENEMY = {0, 0, 0, 0, .25f, .155f, .007f, 0, .5f, .217f, .014f, 0, .75f, .279f, .021f, 0, 1, .491f, 0, 0};
	/** A7B8 r3: poison clouds 4F0 (4), 8 a second (3FCA C13). */
	static final float[] WIDOW_POISON_CLOUD = {0, 0, 0, 0, .25f, .232f, .07f, .505f, .5f, .308f, .101f, .669f, .75f, .382f, .131f, .831f, 1, .455f, .161f, .988f};
	/** 6B93 r2: poison ring 52E8 on mesh 310C 2 m (3FCA C15). */
	static final float[] WIDOW_POISON_RING = {0, 0, 0, 0, .25f, .113f, .011f, 1, .5f, .113f, .011f, 1, .75f, .113f, .011f, 1, 1, .113f, .011f, 1};
	/** 5185 r10: poison domes DAF (3FCA C19). */
	static final float[] WIDOW_POISON_DOME = {0, 0, 0, 0, .25f, .084f, 0, .122f, .5f, .084f, 0, .122f, .75f, .084f, 0, .122f, 1, .084f, 0, .122f};
	/** 30D7 r9 HDR 3.02: its twin (3FCA C17). */
	static final float[] WIDOW_POISON_DOME_ENEMY = {0, 0, 0, 0, .25f, 1, .087f, .087f, .5f, 1, .191f, .191f, .75f, 1, .293f, .293f, 1, 1, .347f, .347f};
	/** F0AC r1 HDR 1.29: poison bits 9487 (4) (3FCA C21). */
	static final float[] WIDOW_POISON_BITS = {0, 0, 0, 0, .25f, .033f, 0, .544f, .5f, .047f, 0, .799f, .75f, .059f, 0, 1, 1, .058f, 0, 1};
	/** F081 r2 HDR 2.45, white top dropped: the dart's ribbon 187EB 0.155 m, 0.2 s (6DC0 C6). */
	static final float[] WIDOW_DART_TRAIL = {0, 0, 0, 0, .33f, .537f, .227f, 1, .67f, .677f, .454f, 1, 1, .815f, .678f, 1};
	/** F081 r3 HDR 2.45, white top dropped: the same LUT's red row (6DC0 C6). */
	static final float[] WIDOW_DART_TRAIL_ENEMY = {0, 0, 0, 0, .33f, 1, .233f, .227f, .67f, 1, .458f, .454f, 1, 1, .679f, .678f};
}
