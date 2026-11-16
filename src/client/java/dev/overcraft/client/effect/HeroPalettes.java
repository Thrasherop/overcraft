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
	static final float[] TR_SHOT = {0, 0, 0, 0, .3f, .2f, .29f, .45f, .7f, .445f, .645f, 1, 1, .85f, .95f, 1};
	static final float[] TR_SHOT_ENEMY = {0, 0, 0, 0, .3f, .45f, .09f, .05f, .7f, 1, .194f, .103f, 1, 1, .75f, .6f};
}
