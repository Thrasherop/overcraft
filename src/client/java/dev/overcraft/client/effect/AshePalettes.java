package dev.overcraft.client.effect;

/**
 * Ashe's palettes' stops (EffectArt.Palette.ASHE_*), apart from HeroPalettes as JunkratPalettes are. Each is (mask value, r, g, b)
 * stops, linear, black at 0, read from her effects' colour LUTs (tools/reference/ashe-effects.md "Proposed palettes", from
 * fx_ashe_pal.py: HDR rows hue-clamped, a near-white top dropped so a solid mask ends on the colour, falling rows reversed). "XXXX rN" is
 * the LUT and its row. Rows the note gives "by age" are reversed here so the head's colour is the mask's bright end (Fx.trail colours its
 * mask, it has no colour by age). Where the note's rows are the same numbers under several roles, one array serves them (AsheFx says which).
 */
final class AshePalettes {
	private AshePalettes() {
	}

	/** 1262 r3 HDR 10 reversed. */
	static final float[] ASHE_LINE = {0, 0, 0, 0, .25f, 1, .115f, .014f, .5f, 1, .229f, .065f, .75f, 1, .336f, .112f, 1, 1, .434f, .156f};
	/** 0B5A r3 HDR 5 reversed. */
	static final float[] ASHE_LINE_ENEMY = {0, 0, 0, 0, .25f, 1, .08f, .047f, .5f, 1, .135f, .07f, .75f, 1, .19f, .094f, 1, 1, .246f, .117f};
	/** 2F36 r3 HDR 5 reversed. */
	static final float[] ASHE_LINE_SCOPED = {0, 0, 0, 0, .25f, 1, .159f, .035f, .5f, 1, .431f, .164f, .75f, 1, .685f, .285f, 1, 1, .921f, .397f};
	/** 933C r5 by age HDR 10; reversed, black at 0 (the head the bright end, as JunkratPalettes). */
	static final float[] ASHE_SMOKE_LINE = {0, 0, 0, 0, .25f, .033f, .015f, 0, .5f, .033f, .015f, 0, .75f, .033f, .015f, 0, 1, 1, .266f, .034f};
	/** E133 r6 by age; reversed, black at 0 (the head the bright end, as JunkratPalettes). */
	static final float[] ASHE_SMOKE_LINE_ENEMY = {0, 0, 0, 0, .25f, .033f, .015f, 0, .5f, .033f, .015f, 0, .75f, .033f, .015f, 0, 1, 1, .034f, .034f};
	/** 81EC r0 by age (also B.O.B.); reversed, black at 0 (the head the bright end, as JunkratPalettes). */
	static final float[] ASHE_SMOKE_LINE_SCOPED = {0, 0, 0, 0, .25f, .05f, .05f, .05f, .5f, .05f, .05f, .05f, .75f, .05f, .05f, .05f, 1, 1, .266f, .034f};
	/** F573 r4 HDR 10 reversed. */
	static final float[] ASHE_HIT = {0, 0, 0, 0, .25f, 1, .197f, 0, .5f, 1, .34f, 0, .75f, 1, .439f, 0, 1, 1, .439f, 0};
	/** 1BD2 r4 HDR 10 reversed. */
	static final float[] ASHE_HIT_ENEMY = {0, 0, 0, 0, .25f, 1, .032f, 0, .5f, 1, .063f, 0, .75f, 1, .084f, 0, 1, 1, .084f, 0};
	/** A680 r4 reversed. */
	static final float[] ASHE_MUZZLE_DOME_ENEMY = {0, 0, 0, 0, .25f, 1, .047f, 0, .5f, 1, .093f, 0, .75f, 1, .125f, 0, 1, 1, .125f, 0};
	/** 3773 r1 HDR 5. */
	static final float[] ASHE_MUZZLE_CONE = {0, 0, 0, 0, .25f, 1, .197f, .004f, .5f, 1, .218f, .005f, .75f, 1, .258f, .008f, 1, 1, .624f, .086f};
	/** EFAD r1 HDR 5. */
	static final float[] ASHE_MUZZLE_CONE_ENEMY = {0, 0, 0, 0, .25f, 1, .001f, 0, .5f, 1, .093f, .04f, .75f, 1, .325f, .187f, 1, 1, .597f, .381f};
	/** 6542 r1. */
	static final float[] ASHE_SCOPED_CONE_ENEMY = {0, 0, 0, 0, .25f, 1, 0, 0, .5f, 1, .05f, .028f, .75f, 1, .11f, .062f, 1, 1, .558f, .381f};
	/** 57C5 r0 HDR 2. */
	static final float[] ASHE_MUZZLE_FLASH = {0, 0, 0, 0, .25f, .693f, .44f, .062f, .5f, 1, .555f, .066f, .75f, 1, .52f, .056f, 1, 1, .5f, .05f};
	/** 420C r0. */
	static final float[] ASHE_MUZZLE_FLASH_B = {0, 0, 0, 0, .25f, .626f, .626f, .275f, .5f, .751f, .751f, .351f, .75f, .875f, .875f, .425f, 1, 1, 1, .5f};
	/** 178E r0 HDR 30 reversed. */
	static final float[] ASHE_MUZZLE_SPARK = {0, 0, 0, 0, .25f, 1, .004f, 0, .5f, 1, .009f, 0, .75f, 1, .016f, 0, 1, 1, .033f, 0};
	/** 8954 r1 HDR 1.37. */
	static final float[] ASHE_MUZZLE_SMOKE = {0, 0, 0, 0, .25f, .531f, .217f, .217f, .5f, .987f, .434f, .434f, .75f, 1, .5f, .5f, 1, 1, .605f, .605f};
	/** D025 r2 HDR 5 reversed. */
	static final float[] ASHE_HIT_DECAL = {0, 0, 0, 0, .25f, 1, .084f, 0, .5f, 1, .088f, .002f, .75f, 1, .252f, .064f, 1, 1, .397f, .12f};
	/** 1A50 r2 reversed. */
	static final float[] ASHE_HIT_DECAL_ENEMY = {0, 0, 0, 0, .25f, 1, 0, 0, .5f, 1, .001f, .001f, .75f, 1, .064f, .064f, 1, 1, .12f, .12f};
	/** D574 r1 HDR 5. */
	static final float[] ASHE_COACH_FLASH = {0, 0, 0, 0, .25f, 1, .154f, .006f, .5f, 1, .22f, .012f, .75f, 1, .405f, .159f, 1, 1, .738f, .473f};
	/** 291E r1. */
	static final float[] ASHE_COACH_FLASH_ENEMY = {0, 0, 0, 0, .25f, 1, .041f, .006f, .5f, 1, .079f, .012f, .75f, 1, .266f, .159f, 1, 1, .63f, .473f};
	/** A893 r1 HDR 5. */
	static final float[] ASHE_COACH_FLASH_BIG = {0, 0, 0, 0, .25f, 1, .301f, .053f, .5f, 1, .49f, .055f, .75f, 1, .612f, .04f, 1, 1, .838f, .381f};
	/** D285 r1. */
	static final float[] ASHE_COACH_FLASH_BIG_ENEMY = {0, 0, 0, 0, .25f, 1, .033f, .015f, .5f, 1, .066f, .031f, .75f, 1, .195f, .122f, 1, 1, .318f, .209f};
	/** 8335 r2 HDR 10. */
	static final float[] ASHE_COACH_SPARK = {0, 0, 0, 0, .25f, .52f, .008f, 0, .5f, 1, .149f, .009f, .75f, 1, .235f, .014f, 1, 1, .652f, 0};
	/** 5AA8 r2. */
	static final float[] ASHE_COACH_SPARK_ENEMY = {0, 0, 0, 0, .25f, .52f, .004f, 0, .5f, 1, .046f, .009f, .75f, 1, .07f, .014f, 1, 1, .122f, 0};
	/** 99A0 r0 HDR 8 reversed. */
	static final float[] ASHE_COACH_PELLET = {0, 0, 0, 0, .25f, 1, .099f, .105f, .5f, 1, .158f, .067f, .75f, 1, .207f, .042f, 1, 1, .251f, .024f};
	/** 7C18 r0 HDR 15. */
	static final float[] ASHE_COACH_FLAME = {0, 0, 0, 0, .25f, 1, .334f, .067f, .5f, 1, .501f, .05f, .75f, 1, .6f, .04f, 1, 1, .667f, .033f};
	/** EAD9 r1 HDR 1.37. */
	static final float[] ASHE_COACH_SMOKE = {0, 0, 0, 0, .25f, .531f, .294f, .294f, .5f, .987f, .124f, .124f, .75f, 1, .361f, .361f, 1, 1, .605f, .605f};
	/** 59D7 r1 HDR 5 (DFF9 r1 sparks, 8B98 r1 landed the same). */
	static final float[] ASHE_DYN_FUSE = {0, 0, 0, 0, .25f, 1, .352f, .067f, .5f, 1, .43f, .089f, .75f, 1, .617f, .144f, 1, 1, .73f, .178f};
	/** 6FF7 r1 (E9D9 r1, BDB8 r1 the same). */
	static final float[] ASHE_DYN_FUSE_ENEMY = {0, 0, 0, 0, .25f, 1, .125f, .067f, .5f, 1, .167f, .089f, .75f, 1, .27f, .144f, 1, 1, .332f, .178f};
	/** 7D63 r0 by age HDR 3; reversed, black at 0 (the head the bright end, as JunkratPalettes). */
	static final float[] ASHE_DYN_TRAIL = {0, 0, 0, 0, .25f, 0, 0, 0, .5f, 0, 0, 0, .75f, .92f, .344f, .037f, 1, 1, .445f, .034f};
	/** 448D r0 by age HDR 8; reversed, black at 0 (the head the bright end, as JunkratPalettes). */
	static final float[] ASHE_DYN_TRAIL_ENEMY = {0, 0, 0, 0, .25f, 0, 0, 0, .5f, 0, 0, 0, .75f, 1, .09f, .04f, 1, 1, .153f, .034f};
	/** 6A26 r3 by age; reversed, black at 0 (the head the bright end, as JunkratPalettes). */
	static final float[] ASHE_DYN_SMOKE = {0, 0, 0, 0, .25f, .037f, .037f, .037f, .5f, .042f, .042f, .042f, .75f, .145f, .145f, .145f, 1, 1, .887f, .366f};
	/** 77EC r8 by age; reversed, black at 0 (the head the bright end, as JunkratPalettes). */
	static final float[] ASHE_DYN_LIT_SWIRL = {0, 0, 0, 0, .25f, .013f, .013f, .013f, .5f, .021f, .021f, .021f, .75f, .161f, .027f, .027f, 1, 1, .347f, .061f};
	/** BC76 r2 HDR 5. */
	static final float[] ASHE_BLAST_SHELL = {0, 0, 0, 0, .25f, 1, .321f, .025f, .5f, 1, .38f, .025f, .75f, 1, .523f, .027f, 1, 1, .61f, .027f};
	/** BB62 r2. */
	static final float[] ASHE_BLAST_SHELL_ENEMY = {0, 0, 0, 0, .25f, 1, .069f, .025f, .5f, 1, .084f, .025f, .75f, 1, .121f, .027f, 1, 1, .144f, .027f};
	/** C390 r3 HDR 5. */
	static final float[] ASHE_BLAST_SPHERE = {0, 0, 0, 0, .25f, 1, .071f, .023f, .5f, 1, .086f, .022f, .75f, 1, .124f, .02f, 1, 1, .147f, .018f};
	/** 3CBA r4 HDR 10 (F21D r4, B0CB r4 the same). */
	static final float[] ASHE_BLAST_BALL = {0, 0, 0, 0, .25f, .103f, .008f, .001f, .5f, .927f, .184f, .012f, .75f, 1, .274f, .018f, 1, 1, .611f, .041f};
	/** 26EE r4. */
	static final float[] ASHE_BLAST_BALL_ENEMY = {0, 0, 0, 0, .25f, .103f, .004f, .001f, .5f, .927f, .071f, .012f, .75f, 1, .097f, .018f, 1, 1, .181f, .041f};
	/** 72C0 r4 (7506 r4 the same). */
	static final float[] ASHE_BLAST_FIRE = {0, 0, 0, 0, .25f, 1, .107f, .011f, .5f, 1, .207f, .014f, .75f, 1, .406f, .027f, 1, 1, .61f, .041f};
	/** FC12 r1 tint (10, 4.12, 0.34). */
	static final float[] ASHE_BLAST_SPARK = {0, 0, 0, 0, .25f, 1, .412f, .034f, .5f, 1, .412f, .034f, .75f, 1, .412f, .034f, 1, 1, .412f, .034f};
	/** 0454 r12 HDR 10. */
	static final float[] ASHE_BURN_FLAME = {0, 0, 0, 0, .25f, .504f, .135f, .008f, .5f, 1, .451f, .029f, .75f, 1, .519f, .034f, 1, 1, .611f, .041f};
	/** D2A3 r12. */
	static final float[] ASHE_BURN_FLAME_ENEMY = {0, 0, 0, 0, .25f, .502f, .015f, .001f, .5f, 1, .076f, .007f, .75f, 1, .098f, .009f, 1, 1, .12f, .011f};
	/** B8BD r6. */
	static final float[] ASHE_BURN_RING = {0, 0, 0, 0, .25f, .888f, .198f, 0, .5f, 1, .256f, 0, .75f, 1, 1, .206f, 1, 1, 1, .733f};
	/** A8A6 r0. */
	static final float[] ASHE_BURN_EMBER = {0, 0, 0, 0, .25f, 1, 1, .342f, .5f, 1, 1, .503f, .75f, 1, 1, .645f, 1, 1, 1, .789f};
	/** 7441 r3 HDR 10 reversed. */
	static final float[] ASHE_BOB_LINE = {0, 0, 0, 0, .25f, 1, .213f, .067f, .5f, 1, .242f, .067f, .75f, 1, .271f, .067f, 1, 1, .3f, .067f};
	/** 745C r3 reversed. */
	static final float[] ASHE_BOB_LINE_ENEMY = {0, 0, 0, 0, .25f, 1, .078f, .067f, .5f, 1, .089f, .067f, .75f, 1, .1f, .067f, 1, 1, .112f, .067f};
	/** FC0C r0 HDR 5 reversed. */
	static final float[] ASHE_BOB_MUZZLE_SPARK = {0, 0, 0, 0, .25f, 1, .055f, 0, .5f, 1, .112f, 0, .75f, 1, .172f, 0, 1, 1, .2f, 0};
	/** 47CC r5 HDR 5. */
	static final float[] ASHE_BOB_CHARGE = {0, 0, 0, 0, .25f, 1, .388f, .004f, .5f, 1, .453f, .005f, .75f, 1, .565f, .007f, 1, 1, .564f, .007f};
	/** 7B6E r5. */
	static final float[] ASHE_BOB_CHARGE_ENEMY = {0, 0, 0, 0, .25f, 1, .116f, .004f, .5f, 1, .15f, .005f, .75f, 1, .209f, .007f, 1, 1, .209f, .007f};
	/** 9C50 r6 HDR 5. */
	static final float[] ASHE_BOB_CHARGE_TRAIL = {0, 0, 0, 0, .25f, 1, .302f, .014f, .5f, 1, .361f, .018f, .75f, 1, .486f, .026f, 1, 1, .552f, .031f};
	/** 2C70 r6. */
	static final float[] ASHE_BOB_CHARGE_TRAIL_ENEMY = {0, 0, 0, 0, .25f, 1, .116f, .004f, .5f, 1, .15f, .005f, .75f, 1, .209f, .007f, 1, 1, .209f, .007f};
	/** 43EC r1. */
	static final float[] ASHE_BOB_KNOCK_ENEMY = {0, 0, 0, 0, .25f, 1, .036f, .004f, .5f, 1, .048f, .005f, .75f, 1, .071f, .008f, 1, 1, .275f, .086f};
	/** A321 r1 HDR 15 reversed. */
	static final float[] ASHE_BOB_KNOCK_STREAK = {0, 0, 0, 0, .25f, 1, .426f, .092f, .5f, 1, .548f, .119f, .75f, 1, .674f, .147f, 1, 1, .799f, .175f};
	/** C1B4 r1 reversed. */
	static final float[] ASHE_BOB_KNOCK_STREAK_ENEMY = {0, 0, 0, 0, .25f, 1, .149f, .092f, .5f, 1, .233f, .119f, .75f, 1, .317f, .147f, 1, 1, .402f, .175f};
	/** 2073 r3 HDR 2. */
	static final float[] ASHE_BOB_KNOCK_DOME = {0, 0, 0, 0, .25f, 1, .521f, .163f, .5f, 1, .591f, .209f, .75f, 1, .66f, .254f, 1, 1, .73f, .301f};
	/** 8E08 r3. */
	static final float[] ASHE_BOB_KNOCK_DOME_ENEMY = {0, 0, 0, 0, .25f, 1, .205f, .163f, .5f, 1, .293f, .209f, .75f, 1, .38f, .254f, 1, 1, .468f, .301f};
	/** 55E1 r1 HDR 1.23. */
	static final float[] ASHE_STING_SPARK = {0, 0, 0, 0, .25f, 1, .206f, .018f, .5f, 1, .376f, .018f, .75f, 1, .561f, .018f, 1, 1, .731f, .018f};
	/** 2BBE r11 HDR 10. */
	static final float[] ASHE_DETONATOR_FLAME = {0, 0, 0, 0, .25f, .382f, 0, 0, .5f, 1, .155f, .065f, .75f, 1, .362f, .151f, 1, 1, .597f, .597f};
}
