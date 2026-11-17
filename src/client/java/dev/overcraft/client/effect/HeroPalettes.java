package dev.overcraft.client.effect;

/**
 * Tracer's and D.Va's palettes' stops (EffectArt.Palette.TR_* and DVA_*: the enum's own initializer outgrew the class file's
 * 64 KB, as SojournPalettes says). Each is (mask value, r, g, b) stops, linear, black at 0; their sources are on the enum's
 * entries (tools/reference/tracer-effects.md, dva-effects.md). D.Va's matrix rows end on their own colour: drawn through a solid
 * mask (1298, all white) they take the top stop, and a whitened top drew her rings white (found playing).
 */
final class HeroPalettes {
	private HeroPalettes() {
	}

	static final float[] DVA_MATRIX_TUBE = {0, 0, 0, 0, .5f, .009f, .17f, .5f, 1, .017f, .333f, 1};
	static final float[] DVA_MATRIX = {0, 0, 0, 0, .5f, .028f, .5f, .26f, 1, .056f, 1, .52f};
	static final float[] DVA_MATRIX_ENEMY = {0, 0, 0, 0, .5f, .5f, .048f, .01f, 1, 1, .095f, .021f};
	static final float[] DVA_ABSORB = {0, 0, 0, 0, .5f, .22f, .5f, .35f, .85f, .44f, 1, .71f, 1, .8f, 1, .9f};
	static final float[] DVA_ABSORB_ENEMY = {0, 0, 0, 0, .5f, .5f, .33f, .22f, .85f, 1, .67f, .44f, 1, 1, .9f, .8f};
	/** Self-Destruct's field and blast: team 638F r0 #00F2BC (H), its top its own colour. */
	static final float[] DVA_SD = {0, 0, 0, 0, .5f, 0, .47f, .37f, .85f, 0, .95f, .74f, 1, .6f, 1, .9f};
	/** The same for an enemy: AA5A r0 #F25E00 (H). */
	static final float[] DVA_SD_ENEMY = {0, 0, 0, 0, .5f, .47f, .18f, 0, .85f, .95f, .37f, 0, 1, 1, .8f, .5f};
	/** Micro Missiles' flame 11C86: team 593A r0, white to cyan (H; which pair is team, M). */
	static final float[] DVA_MISSILE = {0, 0, 0, 0, .4f, .03f, .5f, .5f, .75f, .1f, 1, 1, 1, 1, 1, 1};
	/** The enemy's: 4450 r0, white to orange (H). */
	static final float[] DVA_MISSILE_ENEMY = {0, 0, 0, 0, .4f, .5f, .22f, .05f, .75f, 1, .45f, .1f, 1, 1, 1, 1};
	/** Their smoke trail 11BFC: EE6C r6, grey #717171 (H), drawn soft. */
	static final float[] DVA_SMOKE = {0, 0, 0, 0, 1, .44f, .44f, .44f};
	/** Their hit 1FA5: team B908 r3, green to yellow (H). */
	static final float[] DVA_MISSILE_HIT = {0, 0, 0, 0, .5f, .06f, .45f, .25f, .85f, .16f, .83f, .1f, 1, .93f, .83f, 0};
	/** The enemy's: B908 r4, orange (H). */
	static final float[] DVA_MISSILE_HIT_ENEMY = {0, 0, 0, 0, .5f, .45f, .2f, 0, .85f, .83f, .37f, 0, 1, .93f, .72f, 0};
	/** Tracer's Blink and Recall: team 73D0 r0 #007ABC > #FFFFFF (H). */
	static final float[] TR_BLINK = {0, 0, 0, 0, .5f, 0, .29f, .44f, .85f, 0, .48f, .74f, 1, 1, 1, 1};
	/** The enemy's: r1 #BC0000 > #FF9500 (H). */
	static final float[] TR_BLINK_ENEMY = {0, 0, 0, 0, .5f, .44f, 0, 0, .85f, .74f, 0, 0, 1, 1, .58f, 0};
	/** Her Pulse Bomb: team 7179 r5 #0041FE > #FFFFFF (H). */
	static final float[] TR_BOMB = {0, 0, 0, 0, .4f, 0, .13f, .5f, .75f, 0, .25f, 1, 1, 1, 1, 1};
	/** The enemy's: r6 #FE0000 > #FFFFFF (H). */
	static final float[] TR_BOMB_ENEMY = {0, 0, 0, 0, .4f, .5f, 0, 0, .75f, 1, 0, 0, 1, 1, 1, 1};
	/** Her Fusion Cannons' tracer 10BB: team 07FE r4 #00FFBC (H). */
	static final float[] DVA_FUSION = {0, 0, 0, 0, .5f, 0, .5f, .37f, .85f, 0, 1, .74f, 1, .8f, 1, .95f};
	/** The enemy's: C2B9 r4 #FF4300 (H). */
	static final float[] DVA_FUSION_ENEMY = {0, 0, 0, 0, .5f, .5f, .13f, 0, .85f, 1, .26f, 0, 1, 1, .8f, .6f};
	/** Her Boosters' flames 1492: 3AEC r2 / 9190 r2 green (0.1, 3.7, 1) scaled (H). */
	static final float[] DVA_BOOST = {0, 0, 0, 0, .5f, .015f, .5f, .14f, .85f, .03f, 1, .27f, 1, .8f, 1, .7f};
	/** The same for an enemy: her flames' orange 54EA (3, 0.6, 0) scaled (H; which colour each side sees is M). */
	static final float[] DVA_BOOST_ENEMY = {0, 0, 0, 0, .5f, .5f, .1f, 0, .85f, 1, .2f, 0, 1, 1, .7f, .4f};
	static final float[] TR_SHOT = {0, 0, 0, 0, .3f, .2f, .29f, .45f, .7f, .445f, .645f, 1, 1, .85f, .95f, 1};
	/** Her muzzle 0076's cards: the channel-shifted LUTs print #FF2447 > #00B5F5 (tracer-effects.md: her side ends cyan, M). */
	static final float[] TR_MUZZLE = {0, 0, 0, 0, .35f, 0, .07f, .74f, 1, 0, .71f, .96f};
	/** The enemy's: ending #FF2447 (M). */
	static final float[] TR_MUZZLE_ENEMY = {0, 0, 0, 0, .35f, .5f, .04f, .1f, 1, 1, .14f, .28f};
	/** Illari's gold (D1CA r0 / 74C6 r0), ending on its cream #FFEEB7 so solid masks keep the colour (tools/reference/illari-effects.md). */
	static final float[] IL_SUN = {0, 0, 0, 0, .25f, .61f, .38f, .25f, .5f, .89f, .59f, .31f, .75f, .98f, .82f, .57f, 1, 1, .93f, .72f};
	/** Her sparks and flames (74C6 r1). */
	static final float[] IL_FIRE = {0, 0, 0, 0, .25f, .44f, .16f, .10f, .5f, .85f, .35f, .23f, .75f, .99f, .45f, .18f, 1, 1, .49f, 0};
	/** Her rifle's shot (2B74 r2). */
	static final float[] IL_SHOT = {0, 0, 0, 0, .25f, .50f, .24f, .10f, .5f, .67f, .33f, .15f, .75f, .89f, .46f, .23f, 1, 1, .94f, .78f};
	/** Sunstruck's mark (171E r9). */
	static final float[] IL_MARK = {0, 0, 0, 0, .25f, .65f, .41f, .11f, .5f, .75f, .59f, .31f, .75f, .87f, .77f, .44f, 1, 1, .93f, .56f};
	/** The burst's scorch (5F07 r2: dark), drawn as a dim brown mark (M). */
	static final float[] IL_SCORCH = {0, 0, 0, 0, 1, .12f, .07f, .03f};
	static final float[] TR_SHOT_ENEMY = {0, 0, 0, 0, .3f, .45f, .09f, .05f, .7f, 1, .194f, .103f, 1, 1, .75f, .6f};
}
