package dev.overcraft.client.effect;

/**
 * Junkrat's palettes' stops (EffectArt.Palette.JR_*), apart from HeroPalettes as SojournPalettes / SigmaPalettes are: the enum's own
 * initializer is at the class file's 64 KB, and his 47 arrays beside another hero's in one class would crowd it too. Each is (mask
 * value, r, g, b) stops, linear, black at 0, read from his effects' colour LUTs (tools/reference/junkrat-effects.md "Proposed
 * palettes", from fx_junkrat_pal.py: HDR rows hue-clamped, a near-white top dropped so a solid mask ends on the colour, falling rows
 * reversed). "XXXX rN" is the LUT and its row. Rows the note gives "by age" (a ribbon's colour over its life, head first) are reversed
 * here so the head's colour is the mask's bright end (ours: Fx.trail colours its mask, it has no colour by age).
 */
final class JunkratPalettes {
	private JunkratPalettes() {
	}

	// ---- Frag Launcher ----
	/** the launch's streaks and burst mesh (1468): 4286 r0. */
	static final float[] JR_FRAG_MUZZLE = {0, 0, 0, 0, .25f, 1, .51f, 0, .5f, 1, .566f, 0, .75f, 1, .621f, 0, 1, 1, .775f, 0};
	/** the launch's sparks (1468): ramp texture 3127. */
	static final float[] JR_FRAG_MUZZLE_SPARK = {0, 0, 0, 0, .25f, .117f, .078f, 0, .5f, .991f, .644f, 0, .75f, .991f, .672f, .006f, 1, 1, .863f, .44f};
	/** the launch's smoke puff (1468): 23E7 r1 greys. */
	static final float[] JR_FRAG_MUZZLE_SMOKE = {0, 0, 0, 0, .25f, .163f, .135f, .135f, .5f, .328f, .27f, .27f, .75f, .49f, .402f, .402f, 1, .655f, .537f, .537f};
	/** the frag's glow in flight (051B): 1FA3 r1, HDR 2.85 (his side). */
	static final float[] JR_FRAG_GLOW = {0, 0, 0, 0, .25f, 1, .098f, 0, .5f, 1, .215f, 0, .75f, 1, .293f, 0, 1, 1, .392f, 0};
	/** its redder twin, 1FA3 r2 (enemies, M). */
	static final float[] JR_FRAG_GLOW_ENEMY = {0, 0, 0, 0, .25f, .998f, .075f, 0, .5f, .996f, .149f, 0, .75f, .994f, .223f, 0, 1, .992f, .298f, 0};
	/** the frag's soft glow (051B): 4967 r1, HDR 2.85. */
	static final float[] JR_FRAG_GLOW_B = {0, 0, 0, 0, .25f, 1, .252f, 0, .5f, 1, .545f, 0, .75f, 1, .754f, 0, 1, .991f, 1, 0};
	/** the frag's sparks (051B): 9CE5 r1, HDR 5.39. */
	static final float[] JR_FRAG_SPARK = {0, 0, 0, 0, .25f, 1, .265f, 0, .5f, 1, .333f, 0, .75f, 1, .444f, 0, 1, 1, .497f, 0};
	/** the frag's ribbon (051B): A8BE r8 by age, HDR 4.61, reversed (the orange-red head on top). */
	static final float[] JR_FRAG_TRAIL = {0, 0, 0, 0, .25f, .125f, .042f, .004f, .5f, .237f, .037f, .002f, .75f, .423f, .035f, 0, 1, 1, .125f, 0};
	/** the burst's fireball 47C4 (13C6): 5737 r2, HDR 12, reversed. */
	static final float[] JR_FRAG_FIRE = {0, 0, 0, 0, .25f, 1, .04f, .036f, .5f, 1, .102f, .055f, .75f, 1, .309f, .121f, 1, 1, .439f, .162f};
	/** the burst's flash 2613A (13C6): 70B9 r1, HDR 5, top dropped. */
	static final float[] JR_FRAG_FLASH = {0, 0, 0, 0, .33f, .657f, .17f, 0, .67f, 1, .266f, 0, 1, 1, .55f, 0};
	/** the burst's smoke 1DFCB (13C6): 8B8E r2 rose-grey. */
	static final float[] JR_FRAG_SMOKE = {0, 0, 0, 0, .25f, .312f, .144f, .144f, .5f, .429f, .243f, .243f, .75f, .495f, .349f, .349f, 1, .473f, .473f, .473f};
	/** the blasts' sparks 2C009 (rows DB55 / E127 are parameters: 7975 r1, HDR 5.39, M). */
	static final float[] JR_SPARK = {0, 0, 0, 0, .25f, 1, .244f, 0, .5f, 1, .36f, 0, .75f, 1, .465f, 0, 1, 1, .647f, .008f};
	/** the scorch decal 31E8 (13C6 / 151A / 15D2): C7EA's tint (0.61, 0.66, 1.11) on a dark decal, drawn blended (ours: its darkness). */
	static final float[] JR_SCORCH = {0, 0, 0, 0, 1, .045f, .04f, .035f};
	/** the reload's sparks (08AD / 16AA, the 1.2 s burst): 195E r0, HDR 3.26, reversed. */
	static final float[] JR_RELOAD_SPARK = {0, 0, 0, 0, .25f, 1, .267f, .03f, .5f, 1, .478f, .041f, .75f, 1, .654f, .05f, 1, 1, .8f, .058f};
	/** the reload's second sparks (the 0.83 s burst): 33D8 r0, HDR 4.91, reversed. */
	static final float[] JR_RELOAD_SPARK_B = {0, 0, 0, 0, .25f, 1, .268f, .006f, .5f, 1, .462f, .009f, .75f, 1, .613f, .011f, 1, 1, .734f, .012f};

	// ---- Concussion Mine ----
	/** the blast's fireball (151A): 3301 r0, HDR 2.45. */
	static final float[] JR_MINE_FIRE = {0, 0, 0, 0, .25f, 1, .014f, 0, .5f, 1, .025f, 0, .75f, 1, .033f, 0, 1, 1, .04f, 0};
	/** the blast's flame tongues 14F9 (151A): 803B r1, HDR 9.56, top dropped (his side). */
	static final float[] JR_MINE_FLAME = {0, 0, 0, 0, .33f, 1, .254f, 0, .67f, 1, .444f, 0, 1, 1, .947f, 0};
	/** their redder twin, 803B r2 (enemies, M). */
	static final float[] JR_MINE_FLAME_ENEMY = {0, 0, 0, 0, .25f, 1, .157f, 0, .5f, 1, .231f, 0, .75f, 1, .553f, 0, 1, 1, .948f, 0};
	/** the blast's glow 4C09 (151A): 2855 r1, HDR 3. */
	static final float[] JR_MINE_GLOW = {0, 0, 0, 0, .25f, 1, .216f, 0, .5f, 1, .354f, 0, .75f, 1, .446f, 0, 1, 1, .775f, 0};
	/** the blast's lit smoke (151A): D480 r0, HDR 2.68. */
	static final float[] JR_MINE_SMOKE = {0, 0, 0, 0, .25f, 1, .258f, 0, .5f, 1, .27f, 0, .75f, 1, .282f, 0, 1, 1, .286f, 0};
	/** the mine's red blink 2D6CC (156A): CC40 r1, HDR 4.88. */
	static final float[] JR_MINE_LIGHT = {0, 0, 0, 0, .25f, 1, 0, 0, .5f, 1, .367f, 0, .75f, 1, .749f, .009f, 1, 1, .905f, .014f};
	/** the mine's gold blink (156A): 1976 r0, HDR 2.33. */
	static final float[] JR_MINE_BLINK = {0, 0, 0, 0, .25f, .547f, .518f, 0, .5f, 1, .947f, 0, .75f, 1, .963f, .287f, 1, 1, .982f, .611f};
	/** the mine's ribbons (156A): AFB3 r2, HDR 4.26 (his side, M). */
	static final float[] JR_MINE_TRAIL = {0, 0, 0, 0, .25f, 1, .906f, .32f, .5f, 1, .915f, .364f, .75f, 1, .936f, .473f, 1, 1, .948f, .533f};
	/** their twin, 63C1 r2, HDR 10.4 (enemies, M). */
	static final float[] JR_MINE_TRAIL_ENEMY = {0, 0, 0, 0, .25f, .845f, 0, 0, .5f, 1, 0, 0, .75f, 1, 0, 0, 1, 1, .947f, .533f};
	/** the detonator lit (625C): 5688 r1 shifted (red to white), top dropped. */
	static final float[] JR_DETONATOR = {0, 0, 0, 0, .33f, .702f, .008f, .002f, .67f, .717f, .015f, .003f, 1, .86f, .181f, .174f};

	// ---- Steel Trap ----
	/** the thrown trap's ribbon (6705): D3BB r3 reversed, top dropped (his side, M). */
	static final float[] JR_TRAP_THROWN = {0, 0, 0, 0, .33f, 1, .668f, .043f, .67f, 1, .685f, .086f, 1, 1, .702f, .129f};
	/** its twin, D3BB r4 (enemies, M). */
	static final float[] JR_TRAP_THROWN_ENEMY = {0, 0, 0, 0, .33f, 1, .552f, .552f, .67f, 1, .7f, .7f, 1, 1, .85f, .85f};
	/** the chain (16D5): F309 r1. */
	static final float[] JR_TRAP_CHAIN = {0, 0, 0, 0, .25f, .708f, .6f, 0, .5f, .806f, .666f, 0, .75f, .902f, .732f, 0, 1, 1, .799f, 0};
	/** the snap's dust (16E0): B980 r1 greys. */
	static final float[] JR_TRAP_SMOKE = {0, 0, 0, 0, .25f, .154f, .154f, .154f, .5f, .193f, .193f, .193f, .75f, .231f, .231f, .231f, 1, .27f, .27f, .27f};
	/** the caught's plus marks (19F4): 33DC r0 (rises then falls: its samples sorted by brightness, ours). */
	static final float[] JR_TRAPPED = {0, 0, 0, 0, .5f, .382f, .052f, 0, 1, 1, .172f, 0};
	/** the caught's 2 m ring (19F4): ramp texture 8FF5, violet-blue. */
	static final float[] JR_TRAPPED_RING = {0, 0, 0, 0, .25f, .091f, 0, .434f, .5f, .188f, .02f, .708f, .75f, .165f, .107f, .93f, 1, .153f, .162f, 1};
	/** the caught's wisps (19F4): 5443 r3, violet. */
	static final float[] JR_TRAPPED_B = {0, 0, 0, 0, .25f, .309f, .098f, .412f, .5f, .554f, .273f, .691f, .75f, .764f, .438f, .92f, 1, .858f, .541f, 1};

	// ---- RIP-Tire ----
	/** the tire's ribbons (156F): 9804 r6 by age, HDR 7, reversed (the orange head on top; the smoke's blacks add nothing). */
	static final float[] JR_TIRE_TRAIL = {0, 0, 0, 0, .5f, .008f, .008f, .008f, .75f, .071f, .022f, .005f, 1, 1, .122f, 0};
	/** their redder twin, 3422 r8 (enemies, M). */
	static final float[] JR_TIRE_TRAIL_ENEMY = {0, 0, 0, 0, .5f, .008f, .008f, .008f, .75f, .071f, .005f, .005f, 1, 1, .017f, 0};
	/** the fuse's glows (156F): DA5F r0, HDR 2.85 (his side). */
	static final float[] JR_TIRE_FUSE = {0, 0, 0, 0, .25f, 1, .252f, 0, .5f, 1, .545f, 0, .75f, 1, .754f, 0, 1, .991f, 1, 0};
	/** their twin, DA5F r1 (enemies, M). */
	static final float[] JR_TIRE_FUSE_ENEMY = {0, 0, 0, 0, .25f, 1, .022f, 0, .5f, 1, .05f, 0, .75f, 1, .065f, 0, 1, 1, .086f, 0};
	/** the tire's sparks 183A7 (156F): D4A4 r1, HDR 10, reversed. */
	static final float[] JR_TIRE_SPARK = {0, 0, 0, 0, .25f, 1, .388f, .185f, .5f, 1, .466f, .186f, .75f, 1, .545f, .187f, 1, 1, .624f, .188f};
	/** the tire's glow ball (156F): B8EA r5, grey > orange > grey. */
	static final float[] JR_TIRE_GLOW = {0, 0, 0, 0, .25f, .676f, .283f, .161f, .5f, .944f, .28f, .074f, .75f, .619f, .283f, .18f, 1, .287f, .287f, .287f};
	/** the blast's fireball (15D2): FC57 r1, HDR 12, reversed. */
	static final float[] JR_TIRE_FIRE = {0, 0, 0, 0, .25f, 1, .381f, .153f, .5f, 1, .468f, .232f, .75f, 1, .634f, .382f, 1, 1, .799f, .533f};
	/** the blast's big flash 2613A (15D2): ABBB r0 shifted, HDR 5. */
	static final float[] JR_TIRE_FLASH_BIG = {0, 0, 0, 0, .25f, .657f, .17f, 0, .5f, 1, .266f, 0, .75f, 1, .544f, 0, 1, 1, .921f, .209f};
	/** the blast's 4 m sphere (15D2): 3E05 r0, HDR 5. */
	static final float[] JR_TIRE_SHELL = {0, 0, 0, 0, .25f, 1, .188f, .023f, .5f, 1, .219f, .022f, .75f, 1, .295f, .02f, 1, 1, .342f, .018f};
	/** the blast's ground ring (15D2): ramp texture 31C0 x B894 r0 tint (5, 0.5, 0). */
	static final float[] JR_TIRE_RING = {0, 0, 0, 0, .25f, 1, .049f, 0, .5f, 1, .078f, 0, .75f, 1, .1f, 0, 1, 1, .1f, 0};
	/** the blast's flame tongues (15D2): E413 r1, HDR 9.56, top dropped; Nitro Boost's flare (6740) beside them (ours). */
	static final float[] JR_TIRE_FLAMES = {0, 0, 0, 0, .33f, 1, .254f, 0, .67f, 1, .444f, 0, 1, 1, .947f, 0};

	// ---- Total Mayhem ----
	/** a bomb's ribbon (1792): 77EF r0. */
	static final float[] JR_BOMB_TRAIL = {0, 0, 0, 0, .25f, .468f, .044f, .001f, .5f, .615f, .087f, .001f, .75f, .76f, .13f, .002f, 1, .907f, .174f, .002f};
	/** a bomb's glow 29F7 (1792): 023F r0. */
	static final float[] JR_BOMB_GLOW = {0, 0, 0, 0, .25f, .805f, .023f, 0, .5f, .917f, .049f, 0, .75f, .983f, .075f, 0, 1, 1, .1f, 0};
	/** a bomb's burst flash (14CC): 0B6C r0, HDR 8. */
	static final float[] JR_BOMB_FLASH = {0, 0, 0, 0, .25f, 1, 0, 0, .5f, 1, .046f, 0, .75f, 1, .188f, .004f, 1, 1, .485f, .184f};
	/** a bomb's burst sparks (14CC): FE9B r1, HDR 5.39. */
	static final float[] JR_BOMB_SPARK = {0, 0, 0, 0, .25f, 1, 0, 0, .5f, 1, .114f, 0, .75f, 1, .324f, .013f, 1, 1, .617f, .136f};
}
