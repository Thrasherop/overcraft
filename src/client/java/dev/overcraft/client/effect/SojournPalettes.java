package dev.overcraft.client.effect;

/**
 * Sojourn's palettes' stops (EffectArt.Palette.SOJ_*: the enum's own initializer outgrew the class file's 64 KB, so her
 * arrays live here). Written by her palette pass from her ramps (tools/reference/sojourn-effects.md "Palettes"): each is
 * (mask value, r, g, b) x 5, linear, black at 0; the _N rows are the same LUT row divided by its peak.
 */
final class SojournPalettes {
	private SojournPalettes() {
	}

	/** the Railgun's muzzle flare 210FD (2837 C46 / C48), 0.38-0.51 m and 0.55-2.5 m, 0.13 s: navy, cyan, cream (800F8C16CE25 r0, peak 1.00; game hex #004C88 #00ABD8 #00E5E1 #93FED9 #FFFAB6). */
	static final float[] SOJ_MUZZLE = {0, 0, 0, 0, .25f, 0, .408f, .688f, .5f, 0, .783f, .752f, .75f, .293f, .987f, .691f, 1, 1, .956f, .468f};
	/** the muzzle flare's enemy twin (2837 C54): plum, red, gold (800E592E452A r0, peak 2.09; game hex #B30088 #C92C70 #E2414F #FF7723 #FFFF00). */
	static final float[] SOJ_MUZZLE_ENEMY = {0, 0, 0, 0, .25f, .585f, .025f, .163f, .5f, .757f, .053f, .078f, .75f, 1, .183f, .017f, 1, 1, 1, 0};
	static final float[] SOJ_MUZZLE_ENEMY_N = {0, 0, 0, 0, .25f, .279f, .012f, .078f, .5f, .362f, .025f, .037f, .75f, .555f, .087f, .008f, 1, 1, .485f, 0};
	/** the muzzle's sparks 183A7 (2837 C74), 10-15 at 2-7.6 m/s: teal to cream, HDR 7 (800D949751B6 r1, peak 1.00; game hex #00E1D9 #00E1D9 #00E1D9 #AFECD0 #FFF9C4). */
	static final float[] SOJ_MUZZLE_SPARK = {0, 0, 0, 0, .25f, 0, .753f, .694f, .5f, 0, .753f, .694f, .75f, .426f, .835f, .633f, 1, 1, .947f, .552f};
	/** the muzzle sparks' enemy row (2837 C66): red to pale yellow (8002CA7C1279 r1, peak 1.00; game hex #E10000 #E10000 #E10B02 #F77430 #FFF177). */
	static final float[] SOJ_MUZZLE_SPARK_ENEMY = {0, 0, 0, 0, .25f, .753f, 0, 0, .5f, .756f, .003f, .001f, .75f, .934f, .175f, .03f, 1, 1, .879f, .184f};
	/** the round's ribbon 1FDF6 (280D C6), 0.096 m wide, 0.015-0.07 s: navy, cyan, cream, HDR 1.8 (8007CD484B35 r4, peak 1.83; game hex #004C88 #00ADDB #00F7F0 #C1FFF7 #FFFFEE). */
	static final float[] SOJ_ROUND = {0, 0, 0, 0, .25f, 0, .419f, .706f, .5f, 0, .934f, .875f, .75f, .534f, 1, .933f, 1, 1, 1, .856f};
	static final float[] SOJ_ROUND_N = {0, 0, 0, 0, .25f, 0, .229f, .386f, .5f, 0, .511f, .478f, .75f, .292f, .755f, .51f, 1, 1, .956f, .468f};
	/** the round's enemy ribbon (280D C4): plum, red, orange, pale yellow, HDR 4.9 (8004F1F9D1E7 r2, peak 4.86; game hex #C5009E #EE3092 #FF5757 #FF8700 #FFFFE2). */
	static final float[] SOJ_ROUND_ENEMY = {0, 0, 0, 0, .25f, .856f, .03f, .288f, .5f, 1, .096f, .096f, .75f, 1, .243f, 0, 1, 1, 1, .757f};
	static final float[] SOJ_ROUND_ENEMY_N = {0, 0, 0, 0, .25f, .176f, .006f, .059f, .5f, .504f, .02f, .02f, .75f, 1, .05f, 0, 1, 1, .831f, .156f};
	/** a round's hit flare 210FD (31E5 C23), 0.57-2.3 m, 0.07-0.16 s: navy, teal, pale yellow (8003B3BE55A6 r0, peak 1.27; game hex #005989 #0098A4 #00BFBD #00E2CE #FFFF85). */
	static final float[] SOJ_HIT = {0, 0, 0, 0, .25f, 0, .315f, .372f, .5f, 0, .521f, .512f, .75f, 0, .76f, .619f, 1, 1, 1, .234f};
	static final float[] SOJ_HIT_N = {0, 0, 0, 0, .25f, 0, .248f, .293f, .5f, 0, .41f, .403f, .75f, 0, .598f, .487f, 1, 1, .879f, .184f};
	/** the hit flare's enemy row (31E5 C29): plum, red, orange, pale yellow (800C3112949E r0, peak 1.27; game hex #440043 #9F001B #C73900 #E36200 #FFFF85). */
	static final float[] SOJ_HIT_ENEMY = {0, 0, 0, 0, .25f, .348f, 0, .011f, .5f, .569f, .041f, 0, .75f, .771f, .123f, 0, 1, 1, 1, .234f};
	static final float[] SOJ_HIT_ENEMY_N = {0, 0, 0, 0, .25f, .274f, 0, .009f, .5f, .448f, .032f, 0, .75f, .607f, .096f, 0, 1, 1, .879f, .184f};
	/** a round's hit sparks 183A7 (31E5 C27), 4 at 2.4-2.7 m/s: blue, cyan, pale yellow (800A4B0C4BDD r0, peak 1.00; game hex #007DFF #007DFF #00BCE4 #7CDCB8 #FFF177). */
	static final float[] SOJ_HIT_SPARK = {0, 0, 0, 0, .25f, 0, .205f, 1, .5f, 0, .505f, .775f, .75f, .203f, .718f, .479f, 1, 1, .879f, .184f};
	/** the hit sparks' enemy row (31E5 C33) (800DF9B6EC1D r0, peak 1.27; game hex #440043 #8D003A #B8002E #E15600 #FFFF85). */
	static final float[] SOJ_HIT_SPARK_ENEMY = {0, 0, 0, 0, .25f, .268f, 0, .042f, .5f, .478f, 0, .028f, .75f, .756f, .094f, 0, 1, 1, 1, .234f};
	static final float[] SOJ_HIT_SPARK_ENEMY_N = {0, 0, 0, 0, .25f, .211f, 0, .033f, .5f, .376f, 0, .022f, .75f, .595f, .074f, 0, 1, 1, .879f, .184f};
	/** the gun's energy glow 210FD on hp 007C (31E6 C47), 0.15 / 0.33 / 0.49 m by energy: teal to cyan, HDR 2.5 (800E50D3FD92 r0, peak 2.51; game hex #00DDFF #00E1FF #00F2FF #00FFFF #00FFFF). */
	static final float[] SOJ_GUN_GLOW = {0, 0, 0, 0, .25f, 0, .751f, 1, .5f, 0, .887f, 1, .75f, 0, 1, 1, 1, 0, 1, 1};
	static final float[] SOJ_GUN_GLOW_N = {0, 0, 0, 0, .25f, 0, .299f, .414f, .5f, 0, .353f, .47f, .75f, 0, .549f, .598f, 1, 0, 1, .921f};
	/** the energy glow's enemy row (31E6 C63): dark red to orange (800656F5F311 r1, peak 2.51; game hex #FF0000 #FF0000 #FF2A00 #FF7A00 #FFCA00). */
	static final float[] SOJ_GUN_GLOW_ENEMY = {0, 0, 0, 0, .25f, 1, 0, 0, .5f, 1, .023f, 0, .75f, 1, .194f, 0, 1, 1, .589f, 0};
	static final float[] SOJ_GUN_GLOW_ENEMY_N = {0, 0, 0, 0, .25f, .414f, 0, 0, .5f, .473f, .009f, 0, .75f, .614f, .077f, 0, 1, 1, .235f, 0};
	/** the gun's big glow 210FD on hp 007C (31E6 C61), 0.55-0.97 m: blue to cyan (800E7000846D r0, peak 1.19; game hex #0027BD #0086CD #00CEED #00E5FF #00EEFF). */
	static final float[] SOJ_GUN_CORE = {0, 0, 0, 0, .25f, 0, .238f, .614f, .5f, 0, .615f, .851f, .75f, 0, .784f, 1, 1, 0, .858f, 1};
	static final float[] SOJ_GUN_CORE_N = {0, 0, 0, 0, .25f, 0, .201f, .517f, .5f, 0, .518f, .716f, .75f, 0, .66f, .914f, 1, 0, .723f, 1};
	/** its enemy row: red to orange (800E7000846D r1, peak 1.19; game hex #BD0000 #CD6200 #ED9C00 #FFAE00 #FFB600). */
	static final float[] SOJ_GUN_CORE_ENEMY = {0, 0, 0, 0, .25f, .614f, .123f, 0, .5f, .851f, .333f, 0, .75f, 1, .425f, 0, 1, 1, .465f, 0};
	static final float[] SOJ_GUN_CORE_ENEMY_N = {0, 0, 0, 0, .25f, .517f, .103f, 0, .5f, .716f, .28f, 0, .75f, .914f, .358f, 0, 1, 1, .392f, 0};
	/** the 1P reload's flash 210FD on hp 007C (30A6 C11): teal to mint, HDR 4.2 (80020564EDB5 r0, peak 4.20; game hex #00B1E4 #00DCD2 #00FFC5 #00FFF9 #00FFFF). */
	static final float[] SOJ_RELOAD = {0, 0, 0, 0, .25f, 0, .712f, .641f, .5f, 0, 1, .558f, .75f, 0, 1, .947f, 1, 0, 1, 1};
	static final float[] SOJ_RELOAD_N = {0, 0, 0, 0, .25f, 0, .17f, .153f, .5f, 0, .29f, .133f, .75f, 0, .506f, .226f, 1, 0, 1, .392f};
	/** the Charged Shot's muzzle flare 210FD (2834 C37), 1-2.5 m: dark teal, blue, cyan (8003099EB5DB r0, peak 1.00; game hex #00251C #009ED8 #00C3F5 #00D3E1 #00E1CA). */
	static final float[] SOJ_CHARGED_MUZZLE = {0, 0, 0, 0, .25f, 0, .341f, .685f, .5f, 0, .546f, .913f, .75f, 0, .649f, .753f, 1, 0, .753f, .59f};
	/** the charged muzzle's enemy row (2834 C39): black, plum, red, gold, HDR 3.8 (8008C988E551 r0, peak 3.79; game hex #000000 #BD147D #FF3577 #FF4E66 #FFFF94). */
	static final float[] SOJ_CHARGED_MUZZLE_ENEMY = {0, 0, 0, 0, .25f, .509f, .007f, .206f, .5f, 1, .035f, .186f, .75f, 1, .076f, .133f, 1, 1, 1, .296f};
	static final float[] SOJ_CHARGED_MUZZLE_ENEMY_N = {0, 0, 0, 0, .25f, .134f, .002f, .054f, .5f, .343f, .009f, .049f, .75f, .63f, .02f, .035f, 1, 1, .665f, .078f};
	/** the Charged Shot's beam 2A5D0 (2835 C26 / C34, 1.3 s): navy, teal, green, white, HDR 2.3 (80085AB06897 r3, peak 2.26; game hex #0051B5 #007FA4 #00B096 #00F57E #FFFFFF). */
	static final float[] SOJ_CHARGED_BEAM = {0, 0, 0, 0, .25f, 0, .212f, .369f, .5f, 0, .436f, .307f, .75f, 0, .909f, .207f, 1, 1, 1, 1};
	static final float[] SOJ_CHARGED_BEAM_N = {0, 0, 0, 0, .25f, 0, .094f, .163f, .5f, 0, .193f, .135f, .75f, 0, .402f, .092f, 1, 1, 1, 1};
	/** the beam's enemy row (2835 C21): plum, red, gold, HDR 3.8 (800F11791498 r9, peak 3.79; game hex #7E0068 #9B1663 #DA3159 #FF494D #FFFF94). */
	static final float[] SOJ_CHARGED_BEAM_ENEMY = {0, 0, 0, 0, .25f, .329f, .008f, .124f, .5f, .7f, .031f, .099f, .75f, 1, .067f, .074f, 1, 1, 1, .296f};
	static final float[] SOJ_CHARGED_BEAM_ENEMY_N = {0, 0, 0, 0, .25f, .087f, .002f, .033f, .5f, .185f, .008f, .026f, .75f, .341f, .018f, .02f, 1, 1, .665f, .078f};
	/** the tracer 3216's glows (2BF0 C25-C31), 0.3-0.4 m at 256-500/s along it, 0.28-0.75 s: cream, ice blue, blue, HDR 8.2 (8005DDCA22CC r4, peak 8.19, falls; game hex #FFFFFF #FFFFFF #FFFFFF #00FFFF #00FFFF). */
	static final float[] SOJ_CHARGED_TRACER = {0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, 1, 1, .75f, 0, 1, 1, 1, 0, 1, 1};
	static final float[] SOJ_CHARGED_TRACER_N = {0, 0, 0, 0, .25f, 1, 1, .637f, .5f, .395f, .607f, .857f, .75f, 0, .351f, 1, 1, 0, .351f, 1};
	/** the tracer's enemy row (2835 C23): plum, red, gold, HDR 3.8 (800ED038B259 r2, peak 3.79; game hex #7E0068 #C4207B #FF4477 #FF6168 #FFFF94). */
	static final float[] SOJ_CHARGED_TRACER_ENEMY = {0, 0, 0, 0, .25f, .55f, .014f, .198f, .5f, 1, .059f, .183f, .75f, 1, .121f, .139f, 1, 1, 1, .296f};
	static final float[] SOJ_CHARGED_TRACER_ENEMY_N = {0, 0, 0, 0, .25f, .145f, .004f, .052f, .5f, .347f, .015f, .048f, .75f, .62f, .032f, .037f, 1, 1, .665f, .078f};
	/** the tracer's sparks 1F35 (2BF0 C27), 4.4-9 m/s, 0.17-0.24 m: white to steel blue, HDR 15.6 (800808C16B1D r1, peak 15.59, falls; game hex #FFFFFF #FFFFFF #FFFFFF #FFFFFF #FFFFFF). */
	static final float[] SOJ_CHARGED_SPARK = {0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 1};
	static final float[] SOJ_CHARGED_SPARK_N = {0, 0, 0, 0, .25f, .74f, .779f, .864f, .5f, .481f, .559f, .728f, .75f, .316f, .404f, .594f, 1, .15f, .247f, .459f};
	/** the charged hit's sparks 183A7 (22BF C27), 27-29 at 2.6-6 m/s: blue to white (8004081A686E r1, peak 1.00; game hex #0004FF #696AFF #AFAFFF #DCDCFF #FFFFFF). */
	static final float[] SOJ_CHARGED_HIT_SPARK = {0, 0, 0, 0, .25f, .143f, .144f, 1, .5f, .43f, .43f, 1, .75f, .712f, .713f, 1, 1, 1, 1, 1};
	/** its enemy row (22BF C29): red, orange, peach (800FE1C55DCE r1, peak 1.55; game hex #FF0000 #FF3A00 #FF6500 #FF9600 #FFFEA3). */
	static final float[] SOJ_CHARGED_HIT_SPARK_ENEMY = {0, 0, 0, 0, .25f, 1, .042f, 0, .5f, 1, .13f, 0, .75f, 1, .307f, 0, 1, 1, .99f, .364f};
	static final float[] SOJ_CHARGED_HIT_SPARK_ENEMY_N = {0, 0, 0, 0, .25f, .648f, .027f, 0, .5f, .665f, .084f, 0, .75f, .731f, .197f, 0, 1, 1, .637f, .234f};
	/** the charged hit's smoke 1DFCA/B (22BF C40), 0.9-2.6 m, 0.6-1 s: grey to ice blue (8004D5D699CD r1, peak 1.79; game hex #555555 #8C999D #B0C3C9 #C2ECFB #B6FFFF). */
	static final float[] SOJ_CHARGED_SMOKE = {0, 0, 0, 0, .25f, .263f, .318f, .337f, .5f, .436f, .545f, .583f, .75f, .542f, .842f, .962f, 1, .469f, 1, 1};
	static final float[] SOJ_CHARGED_SMOKE_N = {0, 0, 0, 0, .25f, .147f, .178f, .188f, .5f, .244f, .304f, .325f, .75f, .303f, .471f, .537f, 1, .262f, .776f, 1};
	/** its enemy row (22BF C42): grey to peach (800569E244A1 r1, peak 1.50; game hex #555555 #9D938C #C9BBB0 #F4DDCD #FFFFE8). */
	static final float[] SOJ_CHARGED_SMOKE_ENEMY = {0, 0, 0, 0, .25f, .337f, .294f, .263f, .5f, .583f, .497f, .436f, .75f, .903f, .721f, .607f, 1, 1, 1, .807f};
	static final float[] SOJ_CHARGED_SMOKE_ENEMY_N = {0, 0, 0, 0, .25f, .224f, .196f, .175f, .5f, .388f, .331f, .29f, .75f, .601f, .48f, .405f, 1, 1, .696f, .538f};
	/** the slide's thruster glows 242 on hp 002C / 002E / 0030 / 0031 (2905 C23), 0.69 m, 13/s each: blue, cyan, mint (800DBCBFA263 r0, peak 1.00; game hex #002ED1 #00B1E8 #00EDFC #70FAE6 #A2FFC0). */
	static final float[] SOJ_SLIDE_JET = {0, 0, 0, 0, .25f, 0, .437f, .803f, .5f, 0, .847f, .969f, .75f, .161f, .956f, .789f, 1, .361f, 1, .527f};
	/** their enemy row (2905 C25): plum to red, HDR 3.8 (800499AD68D4 r0, peak 3.79; game hex #7E0068 #BD0063 #FF0056 #FF0042 #FF0000). */
	static final float[] SOJ_SLIDE_JET_ENEMY = {0, 0, 0, 0, .25f, .508f, 0, .124f, .5f, 1, 0, .093f, .75f, 1, 0, .055f, 1, 1, 0, 0};
	static final float[] SOJ_SLIDE_JET_ENEMY_N = {0, 0, 0, 0, .25f, .134f, 0, .033f, .5f, .274f, 0, .024f, .75f, .44f, 0, .014f, 1, 1, 0, 0};
	/** the slide's dust 1DFCA/B at the pelvis / hp 0017 (2905 C35), 1.06-1.2 m, 0.17-0.66 s: grey (80008989E0C7 r1, peak 0.86; game hex #393532 #88847E #B4AEA7 #D4CDC5 #EFE8DF). */
	static final float[] SOJ_SLIDE_SMOKE = {0, 0, 0, 0, .25f, .247f, .229f, .209f, .5f, .454f, .423f, .386f, .75f, .657f, .613f, .56f, 1, .863f, .807f, .738f};
	/** the slide's sparks 183A7 from her feet hp 001B / 001C (2905 C38), 2.4-4.7 m/s: gold to plum, HDR 4.8 (800D405CC99B r0, peak 4.80, falls; game hex #FFFF5D #FFFF92 #FFFFAF #FFDAC1 #FF00D1). */
	static final float[] SOJ_SLIDE_SPARK = {0, 0, 0, 0, .25f, 1, 1, .288f, .5f, 1, 1, .428f, .75f, 1, .701f, .531f, 1, 1, 0, .636f};
	static final float[] SOJ_SLIDE_SPARK_N = {0, 0, 0, 0, .25f, .824f, .436f, .06f, .5f, .647f, .29f, .089f, .75f, .474f, .146f, .111f, 1, .297f, 0, .133f};
	/** the slide's ground dust 14F9 (2905 C42), 1-1.45 m: warm grey (8001D4A0A8D5 r1, peak 0.62; game hex #928D87 #A4A19A #B3B1AA #C1C0B8 #CECEC5). */
	static final float[] SOJ_SLIDE_DUST = {0, 0, 0, 0, .25f, .37f, .354f, .322f, .5f, .453f, .442f, .401f, .75f, .534f, .529f, .479f, 1, .617f, .617f, .558f};
	/** the thrusters' streaks 1F79A (2905 C30), 0.19-0.3 m: violet to blue, HDR 5.7 (8003D2CF06D1 r4, peak 5.72; game hex #6E00FF #6E00FF #6E00FF #6E00FF #6EFFFF). */
	static final float[] SOJ_SLIDE_STREAK = {0, 0, 0, 0, .25f, .154f, 0, 1, .5f, .154f, 0, 1, .75f, .154f, 0, 1, 1, .154f, 1, 1};
	static final float[] SOJ_SLIDE_STREAK_N = {0, 0, 0, 0, .25f, .027f, 0, .403f, .5f, .027f, 0, .537f, .75f, .027f, 0, .802f, 1, .027f, .371f, 1};
	/** the slide jump's plume 1DFCA/B at hp 002D / 002F (330E C10), 103/s at 10.6-14.4 m/s: grey-teal (800D5EAB3710 r4, peak 0.51; game hex #628E8E #629C9C #62A8A8 #62B3B3 #62BDBD). */
	static final float[] SOJ_JUMP_SMOKE = {0, 0, 0, 0, .25f, .122f, .33f, .33f, .5f, .122f, .39f, .39f, .75f, .122f, .449f, .449f, 1, .122f, .509f, .509f};
	/** the slide jump's glow 210FD (330E C16), 1.36-1.45 m: blue to mint (800CC1E28E36 r0, peak 1.00; game hex #001EFF #008CF2 #00BDE3 #00E1D3 #00FFC2). */
	static final float[] SOJ_JUMP_GLOW = {0, 0, 0, 0, .25f, 0, .261f, .884f, .5f, 0, .508f, .769f, .75f, 0, .752f, .655f, 1, 0, 1, .539f};
	/** the slide jump's sparks 23F (330E C13): teal to ice, HDR 5.9 (80024CEF4843 r4, peak 5.88; game hex #00FFD2 #00FFDE #00FFFF #00FFFF #FFFFFF). */
	static final float[] SOJ_JUMP_SPARK = {0, 0, 0, 0, .25f, 0, 1, .73f, .5f, 0, 1, 1, .75f, 0, 1, 1, 1, 1, 1, 1};
	static final float[] SOJ_JUMP_SPARK_N = {0, 0, 0, 0, .25f, 0, .392f, .124f, .5f, 0, .522f, .245f, .75f, 0, .781f, .744f, 1, .361f, .973f, 1};
	/** the Disruptor in flight, line glows C061 (2C8D C17), 0.6-1 m: green, teal, ice, HDR 3.9 (800FAC707B08 r4, peak 3.90; game hex #00FFC6 #63FFFC #A3FFFF #D8FFFF #FFFFFF). */
	static final float[] SOJ_DISRUPTOR_CORE = {0, 0, 0, 0, .25f, .124f, 1, .976f, .5f, .365f, 1, 1, .75f, .689f, 1, 1, 1, 1, 1, 1};
	static final float[] SOJ_DISRUPTOR_CORE_N = {0, 0, 0, 0, .25f, .032f, .628f, .25f, .5f, .093f, .732f, .434f, .75f, .176f, .88f, .683f, 1, .275f, 1, .964f};
	/** its enemy row: red to peach, HDR 3.9 (800FAC707B08 r5, peak 3.90; game hex #FF0000 #FF7F63 #FFD0A3 #FFFFD8 #FFFFFF). */
	static final float[] SOJ_DISRUPTOR_CORE_ENEMY = {0, 0, 0, 0, .25f, 1, .213f, .124f, .5f, 1, .629f, .365f, .75f, 1, 1, .689f, 1, 1, 1, 1};
	static final float[] SOJ_DISRUPTOR_CORE_ENEMY_N = {0, 0, 0, 0, .25f, .628f, .055f, .032f, .5f, .732f, .161f, .093f, .75f, .88f, .304f, .176f, 1, 1, .473f, .275f};
	/** its soft glow A17F (2C8D C21), 0.7 m: dark teal to white, HDR 5 (8001849425C2 r1, peak 5.00; game hex #005C4C #00A375 #00E7A1 #FFFF9E #FFFFFF). */
	static final float[] SOJ_DISRUPTOR_GLOW = {0, 0, 0, 0, .25f, 0, .367f, .177f, .5f, 0, .799f, .358f, .75f, 1, 1, .34f, 1, 1, 1, 1};
	static final float[] SOJ_DISRUPTOR_GLOW_N = {0, 0, 0, 0, .25f, 0, .073f, .035f, .5f, 0, .16f, .072f, .75f, .304f, .321f, .068f, 1, 1, .999f, .999f};
	/** its enemy row (2C8D C19): dark red to pale yellow, HDR 5 (8005468757CE r1, peak 5.00; game hex #FF2020 #FF7041 #FFA960 #FFF277 #FFFFFF). */
	static final float[] SOJ_DISRUPTOR_GLOW_ENEMY = {0, 0, 0, 0, .25f, 1, .162f, .053f, .5f, 1, .394f, .116f, .75f, 1, .885f, .184f, 1, 1, 1, 1};
	static final float[] SOJ_DISRUPTOR_GLOW_ENEMY_N = {0, 0, 0, 0, .25f, .203f, .032f, .011f, .5f, .256f, .079f, .023f, .75f, .399f, .177f, .037f, 1, 1, .947f, .376f};
	/** its motes 4A58 drawn in (2C8D C23), 0.13-0.17 m, 50/s: green to ice, HDR 4.2 (800D845BCEB0 r0, peak 4.20; game hex #00FF8E #41FFAF #74FFEA #D0FFFF #FFFFFF). */
	static final float[] SOJ_DISRUPTOR_MOTE = {0, 0, 0, 0, .25f, .052f, 1, .429f, .5f, .175f, 1, .825f, .75f, .628f, 1, 1, 1, 1, 1, 1};
	static final float[] SOJ_DISRUPTOR_MOTE_N = {0, 0, 0, 0, .25f, .013f, .261f, .102f, .5f, .042f, .336f, .196f, .75f, .15f, .643f, .552f, 1, .275f, 1, .965f};
	/** their enemy row: dark red to peach (800D845BCEB0 r1, peak 4.20; game hex #FF0000 #FF5A41 #FF9E74 #FFF9B8 #FFFFFF). */
	static final float[] SOJ_DISRUPTOR_MOTE_ENEMY = {0, 0, 0, 0, .25f, 1, .103f, .052f, .5f, 1, .344f, .175f, .75f, 1, .945f, .481f, 1, 1, 1, 1};
	static final float[] SOJ_DISRUPTOR_MOTE_ENEMY_N = {0, 0, 0, 0, .25f, .261f, .025f, .013f, .5f, .336f, .082f, .042f, .75f, .542f, .225f, .115f, 1, 1, .54f, .275f};
	/** the launch flash A17F (2C92 C9 / C11), 0.4 m and 6 m, 0.15 s: slate teal to mint, HDR 5 (8000BBD2D7CD r0, peak 5.00; game hex #54C1FC #62D9FF #9FFFFF #C8FFFF #4CFFFF). */
	static final float[] SOJ_DISRUPTOR_LAUNCH = {0, 0, 0, 0, .25f, .123f, .694f, 1, .5f, .348f, 1, 1, .75f, .576f, 1, 1, 1, .072f, 1, 1};
	static final float[] SOJ_DISRUPTOR_LAUNCH_N = {0, 0, 0, 0, .25f, .025f, .139f, .205f, .5f, .07f, .222f, .263f, .75f, .115f, .395f, .372f, 1, .014f, 1, .694f};
	/** its enemy row: brown to yellow, HDR 5 (80085554FDB5 r1, peak 5.00; game hex #FC5454 #FF6D62 #FFCA9F #FFFFC0 #FFFF4C). */
	static final float[] SOJ_DISRUPTOR_LAUNCH_ENEMY = {0, 0, 0, 0, .25f, 1, .152f, .123f, .5f, 1, .591f, .348f, .75f, 1, 1, .526f, 1, 1, 1, .072f};
	static final float[] SOJ_DISRUPTOR_LAUNCH_ENEMY_N = {0, 0, 0, 0, .25f, .205f, .03f, .025f, .5f, .263f, .118f, .07f, .75f, .423f, .325f, .105f, 1, 1, .93f, .014f};
	/** the field's ground decal 299E5 x 3DB8 (2228 C21), 6 m: blue, teal, mint, HDR 3.6 (800BA56BCC99 r5, peak 3.63; game hex #00B2FF #A1FFFF #DBFFFF #FFFFFF #FFFFFF). */
	static final float[] SOJ_FIELD_DECAL = {0, 0, 0, 0, .25f, .356f, 1, 1, .5f, .711f, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 1};
	static final float[] SOJ_FIELD_DECAL_N = {0, 0, 0, 0, .25f, .098f, .343f, .351f, .5f, .196f, .564f, .398f, .75f, .292f, .781f, .443f, 1, .391f, 1, .479f};
	/** the field's line glows C061 (2228 C23), 0.96-1.4 m, 16.6/s: green to ice, HDR 3.9 (80043C79F5F7 r4, peak 3.90; game hex #00FFC6 #63FFFC #A3FFFF #D8FFFF #FFFFFF). */
	static final float[] SOJ_FIELD_SWIRL = {0, 0, 0, 0, .25f, .124f, 1, .976f, .5f, .365f, 1, 1, .75f, .689f, 1, 1, 1, 1, 1, 1};
	static final float[] SOJ_FIELD_SWIRL_N = {0, 0, 0, 0, .25f, .032f, .628f, .25f, .5f, .093f, .732f, .434f, .75f, .176f, .88f, .683f, 1, .275f, 1, .964f};
	/** their enemy row: magenta, red, orange, cream, HDR 5.9 (80043C79F5F7 r5, peak 5.86; game hex #FF00FF #FF00ED #FF007A #FFB100 #FFFFFF). */
	static final float[] SOJ_FIELD_SWIRL_ENEMY = {0, 0, 0, 0, .25f, 1, 0, .844f, .5f, 1, 0, .195f, .75f, 1, .44f, 0, 1, 1, 1, 1};
	static final float[] SOJ_FIELD_SWIRL_ENEMY_N = {0, 0, 0, 0, .25f, .58f, 0, .144f, .5f, .668f, 0, .033f, .75f, .809f, .075f, 0, 1, 1, .815f, .216f};
	/** the field's sphere 1B761 (2228 C42), its 4 m: dark teal to mint, HDR 3 (800D7E44BBB5 r1, peak 3.00; game hex #00617A #00C3F6 #57F1FF #92FFFF #EAFFFF). */
	static final float[] SOJ_FIELD_DOME = {0, 0, 0, 0, .25f, 0, .549f, .918f, .5f, .095f, .883f, 1, .75f, .286f, 1, 1, 1, .824f, 1, 1};
	static final float[] SOJ_FIELD_DOME_N = {0, 0, 0, 0, .25f, 0, .183f, .306f, .5f, .032f, .295f, .368f, .75f, .095f, .465f, .404f, 1, .275f, 1, .591f};
	/** its enemy row: black, dark red, peach, HDR 5 (800D7E44BBB5 r2, peak 5.00; game hex #000000 #BA2929 #FF3F3F #FF9D9D #FFFFFF). */
	static final float[] SOJ_FIELD_DOME_ENEMY = {0, 0, 0, 0, .25f, .49f, .022f, .022f, .5f, 1, .05f, .05f, .75f, 1, .339f, .339f, 1, 1, 1, 1};
	static final float[] SOJ_FIELD_DOME_ENEMY_N = {0, 0, 0, 0, .25f, .098f, .004f, .004f, .5f, .222f, .01f, .01f, .75f, .354f, .068f, .068f, 1, 1, .673f, .503f};
	/** the field's flame ring E22E (29132 x 29149, 2228 C33), 11.8 m mesh: navy, green, ice, HDR 2.7 (8007A20C522D r6, peak 2.71; game hex #0034A3 #00AF7D #00FFAC #5BFFFF #E0FFFF). */
	static final float[] SOJ_FIELD_GROUND = {0, 0, 0, 0, .25f, 0, .43f, .206f, .5f, 0, 1, .413f, .75f, .105f, 1, 1, 1, .743f, 1, 1};
	static final float[] SOJ_FIELD_GROUND_N = {0, 0, 0, 0, .25f, 0, .159f, .076f, .5f, 0, .561f, .153f, .75f, .039f, .843f, .406f, 1, .275f, 1, .965f};
	/** its enemy row: plum, red, orange, cream (8007A20C522D r7, peak 2.71; game hex #5B006D #EF0000 #FF923C #FFFB78 #FFFFE0). */
	static final float[] SOJ_FIELD_GROUND_ENEMY = {0, 0, 0, 0, .25f, .861f, 0, 0, .5f, 1, .289f, .046f, .75f, 1, .964f, .189f, 1, 1, 1, .743f};
	static final float[] SOJ_FIELD_GROUND_ENEMY_N = {0, 0, 0, 0, .25f, .318f, 0, 0, .5f, .662f, .107f, .017f, .75f, .835f, .357f, .07f, 1, 1, .775f, .275f};
	/** the field's opening flash 4A58 (2228 C31), 4.33 m, 0.35 s: green to ice, HDR 4.2 (80072E35BD9F r0, peak 4.20; game hex #00FF8E #48FFBD #86FFFF #C8FFFF #FFFFFF). */
	static final float[] SOJ_FIELD_FLASH = {0, 0, 0, 0, .25f, .065f, 1, .508f, .5f, .237f, 1, 1, .75f, .575f, 1, 1, 1, 1, 1, 1};
	static final float[] SOJ_FIELD_FLASH_N = {0, 0, 0, 0, .25f, .016f, .303f, .121f, .5f, .056f, .417f, .255f, .75f, .137f, .67f, .527f, 1, .275f, 1, .965f};
	/** its enemy row: dark red to pale yellow (80072E35BD9F r1, peak 5.00; game hex #FF0000 #F33D00 #FD6100 #FFFF60 #FFFFB6). */
	static final float[] SOJ_FIELD_FLASH_ENEMY = {0, 0, 0, 0, .25f, .898f, .046f, 0, .5f, .983f, .119f, 0, .75f, 1, 1, .117f, 1, 1, 1, .465f};
	static final float[] SOJ_FIELD_FLASH_ENEMY_N = {0, 0, 0, 0, .25f, .18f, .009f, 0, .5f, .197f, .024f, 0, .75f, .406f, .215f, .023f, 1, 1, .76f, .093f};
	/** the field's core sphere DBF (2228 C25), 0.5-0.6 m: teal to pale yellow, HDR 1.6 (800326663928 r3, peak 1.60; game hex #00FFEF #8EFFE5 #D6FFDE #FFFFD1 #FFFFB0). */
	static final float[] SOJ_FIELD_CORE = {0, 0, 0, 0, .25f, .272f, 1, .782f, .5f, .674f, 1, .731f, .75f, 1, 1, .635f, 1, 1, 1, .434f};
	static final float[] SOJ_FIELD_CORE_N = {0, 0, 0, 0, .25f, .17f, .674f, .487f, .5f, .42f, .791f, .456f, .75f, .708f, .898f, .396f, 1, 1, .93f, .271f};
	/** its enemy row: dark red to peach, HDR 3.3 (800326663928 r2, peak 3.29; game hex #FF0000 #EF5939 #F6905F #FFF9A8 #FFFFF4). */
	static final float[] SOJ_FIELD_CORE_ENEMY = {0, 0, 0, 0, .25f, .862f, .1f, .041f, .5f, .926f, .278f, .115f, .75f, 1, .948f, .391f, 1, 1, 1, .903f};
	static final float[] SOJ_FIELD_CORE_ENEMY_N = {0, 0, 0, 0, .25f, .262f, .03f, .013f, .5f, .282f, .085f, .035f, .75f, .576f, .288f, .119f, 1, 1, .665f, .275f};
	/** the field's motes 4A58 drawn in (2228 C27), 75/s: green to ice, HDR 4.2 (800D1E8D75EF r0, peak 4.20; game hex #00FF8E #41FFAF #74FFEA #D0FFFF #FFFFFF). */
	static final float[] SOJ_FIELD_MOTE = {0, 0, 0, 0, .25f, .052f, 1, .429f, .5f, .175f, 1, .825f, .75f, .628f, 1, 1, 1, 1, 1, 1};
	static final float[] SOJ_FIELD_MOTE_N = {0, 0, 0, 0, .25f, .013f, .261f, .102f, .5f, .042f, .336f, .196f, .75f, .15f, .643f, .552f, 1, .275f, 1, .965f};
	/** their enemy row: rust to pale yellow (800D1E8D75EF r1, peak 4.20; game hex #FF4900 #FF7531 #FFB25A #FFFB86 #FFFFFF). */
	static final float[] SOJ_FIELD_MOTE_ENEMY = {0, 0, 0, 0, .25f, 1, .178f, .03f, .5f, 1, .444f, .101f, .75f, 1, .968f, .24f, 1, 1, 1, 1};
	static final float[] SOJ_FIELD_MOTE_ENEMY_N = {0, 0, 0, 0, .25f, .261f, .042f, .007f, .5f, .336f, .106f, .024f, .75f, .496f, .23f, .057f, 1, 1, 1, .275f};
	/** Deceleration Field's sphere 1385 (6C02 C5), 8 m: black, navy, steel, ice, HDR 5 (80053ECD2398 r8, peak 5.00; game hex #000000 #0F0079 #1B00AF #CCFFFF #FFFFFF). */
	static final float[] SOJ_DECEL = {0, 0, 0, 0, .25f, .005f, 0, .19f, .5f, .011f, 0, .43f, .75f, .601f, 1, 1, 1, 1, 1, 1};
	static final float[] SOJ_DECEL_N = {0, 0, 0, 0, .25f, .001f, 0, .038f, .5f, .002f, 0, .086f, .75f, .12f, .215f, .323f, 1, .731f, 1, .965f};
	/** its enemy row: dark red to pink, HDR 2.7 (80053ECD2398 r9, peak 2.71; game hex #7A0000 #F50000 #FF5555 #FF8888 #FFE0E0). */
	static final float[] SOJ_DECEL_ENEMY = {0, 0, 0, 0, .25f, .915f, 0, 0, .5f, 1, .091f, .091f, .75f, 1, .248f, .248f, 1, 1, .743f, .743f};
	static final float[] SOJ_DECEL_ENEMY_N = {0, 0, 0, 0, .25f, .338f, 0, 0, .5f, .44f, .034f, .034f, .75f, .524f, .092f, .092f, 1, 1, .275f, .275f};
	/** Overclock's glows 210FD on the gun hp 007C (2880 C33 / C60), 0.5-1.4 m, 20/s: blue, teal, green (800AD66DBD97 r0, peak 1.34; game hex #0052FF #00A2E9 #00D1CF #00FDA1 #00FF54). */
	static final float[] SOJ_OC_GLOW = {0, 0, 0, 0, .25f, 0, .361f, .811f, .5f, 0, .637f, .622f, .75f, 0, .986f, .358f, 1, 0, 1, .089f};
	static final float[] SOJ_OC_GLOW_N = {0, 0, 0, 0, .25f, 0, .269f, .605f, .5f, 0, .475f, .464f, .75f, 0, .736f, .267f, 1, 0, 1, .067f};
	/** their enemy row (2880 C45): plum, red, orange, yellow (800E9644391F r0, peak 1.34; game hex #9800A9 #E81E6F #FF6000 #FFB100 #FFFF00). */
	static final float[] SOJ_OC_GLOW_ENEMY = {0, 0, 0, 0, .25f, .81f, .013f, .159f, .5f, 1, .118f, 0, .75f, 1, .44f, 0, 1, 1, 1, 0};
	static final float[] SOJ_OC_GLOW_ENEMY_N = {0, 0, 0, 0, .25f, .604f, .01f, .118f, .5f, .873f, .088f, 0, .75f, .936f, .328f, 0, 1, 1, .991f, 0};
	/** Overclock's core sphere DBF on the gun (2880 C37), 0.59 m: black, slate, blue, aqua, HDR 2.4 (8008BFCF1E87 r1, peak 2.42; game hex #000000 #002342 #8398BD #00B3FF #00FFFF). */
	static final float[] SOJ_OC_CORE = {0, 0, 0, 0, .25f, 0, .017f, .055f, .5f, .225f, .313f, .512f, .75f, 0, .45f, 1, 1, 0, 1, 1};
	static final float[] SOJ_OC_CORE_N = {0, 0, 0, 0, .25f, 0, .007f, .023f, .5f, .093f, .129f, .211f, .75f, 0, .186f, .464f, 1, 0, 1, .597f};
	/** its enemy row (2880 C49): black, violet, red, yellow (800A11F4443D r1, peak 2.42; game hex #000000 #330042 #B863B4 #FF006A #FFFF00). */
	static final float[] SOJ_OC_CORE_ENEMY = {0, 0, 0, 0, .25f, .033f, 0, .055f, .5f, .478f, .125f, .456f, .75f, 1, 0, .144f, 1, 1, 1, 0};
	static final float[] SOJ_OC_CORE_ENEMY_N = {0, 0, 0, 0, .25f, .014f, 0, .023f, .5f, .197f, .051f, .188f, .75f, .46f, 0, .06f, 1, 1, .776f, 0};
	/** Overclock's vent streaks 2A3EB x 2A3E2 on mesh E613 (2880 C42 / C57): blue, teal, mint, HDR 4.6 (800DF109EACE r2, peak 4.60; game hex #00D7FF #00FFFF #00FFFF #6FFFFF #C9FFFF). */
	static final float[] SOJ_OC_VENT = {0, 0, 0, 0, .25f, 0, 1, 1, .5f, 0, 1, 1, .75f, .16f, 1, 1, 1, .586f, 1, 1};
	static final float[] SOJ_OC_VENT_N = {0, 0, 0, 0, .25f, 0, .299f, .331f, .5f, 0, .6f, .278f, .75f, .035f, .905f, .278f, 1, .127f, 1, .468f};
	/** their enemy row (2880 C54): plum, red, orange, HDR 3.8 (8008BC3FD9C7 r2, peak 3.79; game hex #7E0068 #A00085 #FF7383 #FFB96E #FFF200). */
	static final float[] SOJ_OC_VENT_ENEMY = {0, 0, 0, 0, .25f, .352f, 0, .234f, .5f, 1, .172f, .228f, .75f, 1, .483f, .155f, 1, 1, .888f, 0};
	static final float[] SOJ_OC_VENT_ENEMY_N = {0, 0, 0, 0, .25f, .093f, 0, .062f, .5f, .285f, .046f, .06f, .75f, .606f, .128f, .041f, 1, 1, .235f, 0};
	/** Overclock's material effect on her (333B C0, hp 0004): cyan falling to black, HDR 6.6 (80035FEA47FC r1, peak 6.59, falls; game hex #00FFFF #009E98 #005350 #001A19 #000000). */
	static final float[] SOJ_OC_BODY = {0, 0, 0, 0, .25f, 0, .343f, .316f, .5f, 0, .087f, .08f, .75f, 0, .01f, .009f, 1, 0, 0, 0};
	static final float[] SOJ_OC_BODY_N = {0, 0, 0, 0, .25f, 0, .052f, .048f, .5f, 0, .013f, .012f, .75f, 0, .002f, .001f, 1, 0, 0, 0};
}
