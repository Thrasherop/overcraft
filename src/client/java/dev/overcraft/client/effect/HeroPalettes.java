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
	// Torbjorn's (tools/reference/torbjorn-effects.md; research	orbjorn\workx_palettes.txt): rows whose top fell dark
	// through a bright mask are ended on their own colour (ours, noted on each)
	/** Overload's vent flames 1CAFA x B08 x 1CB5D (26F1 C20): orange to cream (8009ECD37341 r4, peak 1.00; game hex #FF4000 #FF6300 #FF7B00 #FF8E00 #FFFCBC). */
	static final float[] TB_OVERLOAD = {0, 0, 0, 0, .25f, 1, .125f, 0, .5f, 1, .198f, 0, .75f, 1, .27f, 0, 1, 1, .973f, .502f};
	/** the vent flames' twin (26F1 C23): red to gold (8007AC38C882 r4, peak 1.00; game hex #FF0000 #FF2000 #FF2F00 #FF3A00 #FFCA00). */
	static final float[] TB_OVERLOAD_ENEMY = {0, 0, 0, 0, .25f, 1, .014f, 0, .5f, 1, .029f, 0, .75f, 1, .043f, 0, 1, 1, .59f, 0};
	/** the second flame layer 299E1 x 1CB5D (26F1 C26): dark red to yellow, HDR 2 (8009493D485E r6, peak 2.00; game hex #AF0000 #DE2D00 #FF4200 #FF9500 #FFFF00). */
	static final float[] TB_OVERLOAD_B = {0, 0, 0, 0, .25f, .734f, .027f, 0, .5f, 1, .055f, 0, .75f, 1, .301f, 0, 1, 1, 1, 0};
	/** its twin (26F1 C29): red to amber, HDR 2 (80066563313B r6, peak 2.00; game hex #FF0000 #FF2000 #FF3100 #FF4300 #FFF600). */
	static final float[] TB_OVERLOAD_B_ENEMY = {0, 0, 0, 0, .25f, 1, .014f, 0, .5f, 1, .03f, 0, .75f, 1, .056f, 0, 1, 1, .925f, 0};
	/** the heat smoke 377D5 x B08 (26F1 C37): cream to dark grey, HDR 3, falls (800F31122C42 r3, peak 3.00, falls; game hex #FFFFDA #7A7A7A #676767 #505050 #2B2B2B). */
	static final float[] TB_OVERLOAD_SMOKE = {0, 0, 0, 0, .25f, .193f, .193f, .193f, .5f, .137f, .137f, .137f, .75f, .081f, .081f, .081f, 1, .024f, .024f, .024f};
	/** the sparks 1F35 (26F1 C45): amber to orange, HDR 10 (8005F99F64C2 r1, peak 10.00, falls; game hex #FFFF75 #FFFF66 #FFFF54 #FFFF3B #FFFF00). */
	static final float[] TB_OVERLOAD_SPARK = {0, 0, 0, 0, .25f, 1, 1, .132f, .5f, 1, 1, .088f, .75f, 1, 1, .044f, 1, 1, 1, 0};
	/** the forge glow 174D8 at hp 0075 (26E5 C57): orange to gold (800858A0A206 r1, peak 1.00; game hex #FF8500 #FF9A00 #FFAC00 #FFBC00 #FFCA00). */
	static final float[] TB_MOLTEN_GLOW = {0, 0, 0, 0, .25f, 1, .324f, 0, .5f, 1, .413f, 0, .75f, 1, .501f, 0, 1, 1, .59f, 0};
	/** its twin (26E5 C59): red to orange (800C112B11EC r1, peak 1.00; game hex #FF0000 #FF4000 #FF5A00 #FF6C00 #FF7C00). */
	static final float[] TB_MOLTEN_GLOW_ENEMY = {0, 0, 0, 0, .25f, 1, .051f, 0, .5f, 1, .101f, 0, .75f, 1, .151f, 0, 1, 1, .202f, 0};
	/** the held flames 1CAFA x 1CB5D x B08 (26E5 C63): rust to yellow, HDR 3 (8004DC8838C6 r4, peak 3.01; game hex #FF4000 #FF9000 #FFBD00 #FFDF00 #FFFF00). */
	static final float[] TB_MOLTEN_FLAME = {0, 0, 0, 0, .25f, 1, .281f, 0, .5f, 1, .51f, 0, .75f, 1, .736f, 0, 1, 1, 1, 0};
	/** their twin (26E5 C65): red to gold (800E4808491F r4, peak 1.00; game hex #FF0000 #FF4400 #FF6000 #FF7400 #FFC900). */
	static final float[] TB_MOLTEN_FLAME_ENEMY = {0, 0, 0, 0, .25f, 1, .058f, 0, .5f, 1, .117f, 0, .75f, 1, .174f, 0, 1, 1, .584f, 0};
	/** the held sparks 1F35 (26E5 C49): brown to pale yellow, HDR 3 (80076CFB570C r1, peak 3.00; game hex #A52300 #ED6616 #FFAE1D #FFFF28 #FFFFFF). */
	static final float[] TB_MOLTEN_SPARK = {0, 0, 0, 0, .25f, .848f, .132f, .008f, .5f, 1, .425f, .012f, .75f, 1, 1, .021f, 1, 1, 1, 1};
	/** their twin (26E5 C53): dark red to orange, HDR 10 (80082F79D0EB r1, peak 9.99; game hex #FF3600 #FF5300 #FF9E00 #FFFF00 #FFFF00). */
	static final float[] TB_MOLTEN_SPARK_ENEMY = {0, 0, 0, 0, .25f, 1, .087f, 0, .5f, 1, .343f, 0, .75f, 1, 1, 0, 1, 1, 1, 0};
	/** the slag's launch flare (26CC C12): rust to cream, HDR 2 (80008515059C r5, peak 2.00; game hex #FF4000 #FF7800 #FF9F00 #FFCE00 #FFFFAA). */
	static final float[] TB_SLAG_LAUNCH = {0, 0, 0, 0, .25f, 1, .187f, 0, .5f, 1, .348f, 0, .75f, 1, .616f, 0, 1, 1, 1, .403f};
	/** its twin (26CC C14): red to cream, HDR 2 (8001EE310154 r5, peak 2.00; game hex #FF0000 #FF4700 #FF6800 #FF8F00 #FFFFFF). */
	static final float[] TB_SLAG_LAUNCH_ENEMY = {0, 0, 0, 0, .25f, 1, .064f, 0, .5f, 1, .137f, 0, .75f, 1, .275f, 0, 1, 1, 1, 1};
	/** the slag's glow A7B6 x 1CB5D (2509 C54): orange to cream, HDR 1.3 (80034BCCFC52 r4, peak 1.32; game hex #FF4000 #FF7700 #FF9900 #FFBB00 #FFFFD5). */
	static final float[] TB_SLAG_GLOW = {0, 0, 0, 0, .25f, 1, .184f, 0, .5f, 1, .318f, 0, .75f, 1, .5f, 0, 1, 1, 1, .663f};
	/** its twin (2509 C66): red to cream, HDR 3 (800BD80D96AA r4, peak 3.00; game hex #FF0C00 #FF5400 #FF8200 #FF9E00 #FFFFFF). */
	static final float[] TB_SLAG_GLOW_ENEMY = {0, 0, 0, 0, .25f, 1, .089f, 0, .5f, 1, .224f, 0, .75f, 1, .341f, 0, 1, 1, 1, 1};
	/** the slag's ribbons E71 (2509 C55 / C57 / C59): dark red to cream, HDR 10 (80087635D35B r6, peak 10.00; game hex #FF0000 #FF9200 #FFC800 #FFFF00 #FFFFFF). */
	static final float[] TB_SLAG_TRAIL = {0, 0, 0, 0, .25f, 1, .286f, 0, .5f, 1, .577f, 0, .75f, 1, 1, 0, 1, 1, 1, 1};
	/** the fourth ribbon (2509 C63): red to cream, HDR 3 (8008B27F09F8 r6, peak 3.00; game hex #FF0C00 #FF4F00 #FF7100 #FF8700 #FFFFFF). */
	static final float[] TB_SLAG_TRAIL_ENEMY = {0, 0, 0, 0, .25f, 1, .079f, 0, .5f, 1, .167f, 0, .75f, 1, .243f, 0, 1, 1, 1, 1};
	/** the slag's drips 1F35 (2509 C50 / C62, 120-160 a second): orange, dark at both ends (80024FB351C6 r0, peak 0.88; game hex #000000 #F04D00 #EE4700 #E83E00 #230000). Its dark top replaced by its orange (ours: drawn through the spark mask's bright core, it was the dark end). */
	static final float[] TB_SLAG_DRIP = {0, 0, 0, 0, .25f, .873f, .074f, 0, .5f, .859f, .063f, 0, .75f, .81f, .048f, 0, 1, .873f, .074f, 0};
	/** the pool forming: fire 1B709 x 4DB (26FB C31): black, orange, yellow (8004A610FA25 r1, peak 1.00; game hex #000000 #F23400 #FF5800 #FF7100 #FFE150). */
	static final float[] TB_POOL_FIRE = {0, 0, 0, 0, .25f, .884f, .034f, 0, .5f, 1, .097f, 0, .75f, 1, .164f, 0, 1, 1, .753f, .08f};
	/** its twin (26FB C43): black, red, orange (800968EF9EDF r1, peak 1.00; game hex #320000 #F21200 #FF2F00 #FF4100 #FF6B00). */
	static final float[] TB_POOL_FIRE_ENEMY = {0, 0, 0, 0, .25f, .888f, .006f, 0, .5f, 1, .029f, 0, .75f, 1, .053f, 0, 1, 1, .147f, 0};
	/** the forming flash 1B97A x 1B97B (26FB C33): black, orange, gold (8006E2998FFB r3, peak 1.00; game hex #000000 #FF4800 #FF6D00 #FFB800 #FFE150). */
	static final float[] TB_POOL_FLASH = {0, 0, 0, 0, .25f, 1, .065f, 0, .5f, 1, .152f, 0, .75f, 1, .477f, 0, 1, 1, .753f, .08f};
	/** its twin (26FB C45): dark red to orange (800D627220B7 r3, peak 1.00; game hex #3E0000 #FF2B00 #FF3800 #FF4C00 #FF7400). */
	static final float[] TB_POOL_FLASH_ENEMY = {0, 0, 0, 0, .25f, 1, .024f, 0, .5f, 1, .039f, 0, .75f, 1, .072f, 0, 1, 1, .175f, 0};
	/** the pool's lava blobs 83F7 / 885C (2754 C17 / C20): pale yellow, ochre, brown, falls (800AEA60FE77 r3, peak 4.00, falls; game hex #FFFFFF #EBB400 #A9650D #763507 #600505). */
	static final float[] TB_POOL_LAVA = {0, 0, 0, 0, .25f, .833f, .459f, 0, .5f, .397f, .129f, .004f, .75f, .18f, .036f, .002f, 1, .117f, .002f, .001f};
	/** the enemy pool's blobs (2754 C25): pale yellow to dark brown, falls (80013036E741 r3, peak 4.00, falls; game hex #FFFFFF #EBB400 #A97903 #8C5A06 #895009). */
	static final float[] TB_POOL_LAVA_ENEMY = {0, 0, 0, 0, .25f, .833f, .459f, 0, .5f, .397f, .191f, .001f, .75f, .264f, .103f, .002f, 1, .25f, .081f, .003f};
	/** the pool's embers 9489 x 4F3 (2754 C18): dark red, red, orange, gold (800BC3DF492B r0, peak 1.00; game hex #740000 #B40D00 #E01700 #FF3C00 #FFB100). */
	static final float[] TB_POOL_EMBER = {0, 0, 0, 0, .25f, .459f, .004f, 0, .5f, .744f, .009f, 0, .75f, 1, .045f, 0, 1, 1, .44f, 0};
	/** a burning enemy's flames 4A7E x 4C09 x 4DB (254F C53 / C55): dark red to orange, HDR 10 (800D8CF3D2A3 r12, peak 9.99; game hex #BC0000 #BC2105 #FF5617 #FF8A2A #FFFF5D). */
	static final float[] TB_BURN = {0, 0, 0, 0, .25f, .502f, .015f, .001f, .5f, 1, .092f, .009f, .75f, 1, .256f, .024f, 1, 1, 1, .11f};
	/** the second flame layer (254F C57 / C59): brown to gold, HDR 10 (800CC35D0454 r12, peak 9.99; game hex #BC4D0C #BC6716 #FFDE3E #FFFF5E #FFFFAB). */
	static final float[] TB_BURN_B = {0, 0, 0, 0, .25f, .504f, .135f, .008f, .5f, 1, .731f, .048f, .75f, 1, 1, .113f, 1, 1, 1, .409f};
	/** the burn's ring 86CF x D9B (254F C61): orange to cream (800FD35FB8BD r6, peak 1.00; game hex #E46900 #F27B00 #FF8B00 #FFFF7D #FFFFDE). */
	static final float[] TB_BURN_RING = {0, 0, 0, 0, .25f, .888f, .198f, 0, .5f, 1, .256f, 0, .75f, 1, 1, .206f, 1, 1, 1, .733f};
	/** the rivet in flight, mesh 83D6 x 299E1 (2653 C17 / C23): amber to yellow (800C80AA89BD r2, peak 1.00; game hex #FE9501 #FEB701 #FED301 #FFEA00 #FFFF00). */
	static final float[] TB_RIVET = {0, 0, 0, 0, .25f, .993f, .476f, 0, .5f, .995f, .651f, 0, .75f, .998f, .824f, 0, 1, 1, 1, 0};
	/** the rivet's ribbon 797, 2A480 x 2A063 (2653 C20): black, rust, orange, gold, HDR 11 (800ECFD9D950 r4, peak 10.99; game hex #000000 #FF8600 #FFB700 #FFFF00 #FFFF00). */
	static final float[] TB_RIVET_TRAIL = {0, 0, 0, 0, .25f, 1, .237f, 0, .5f, 1, .474f, 0, .75f, 1, 1, 0, 1, 1, 1, 0};
	/** the muzzle flash 1924B x 18F00 (2651 C33): black, brown, ochre, cream, HDR 3 (8008D9900576 r1, peak 3.00; game hex #000000 #C36B00 #FF9400 #FFEB57 #FFFFFF). */
	static final float[] TB_MUZZLE = {0, 0, 0, 0, .25f, .545f, .147f, 0, .5f, 1, .295f, 0, .75f, 1, .827f, .096f, 1, 1, 1, 1};
	/** its twin (2651 C35): dark red to peach, HDR 3 (8009C30DDC98 r1, peak 3.00; game hex #4D0000 #C93C00 #FF5400 #FF8647 #FFFFE5). */
	static final float[] TB_MUZZLE_ENEMY = {0, 0, 0, 0, .25f, .583f, .045f, 0, .5f, 1, .09f, 0, .75f, 1, .237f, .063f, 1, 1, 1, .787f};
	/** the muzzle's star 191DD x 4DB (2651 C25 / C27): cream to brown, HDR 4, falls (8005582BF3DF r2, peak 4.00, falls; game hex #FFFFFF #FFFFFF #FFFFFF #FFFEA2 #FFC540). */
	static final float[] TB_MUZZLE_B = {0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, 1, 1, .75f, 1, .987f, .359f, 1, 1, .558f, .052f};
	/** its twin (2651 C29): peach to red-brown, HDR 4, falls (800AD467C2F1 r2, peak 4.00, falls; game hex #FFFFFF #FFFFFF #FFF8DD #FF9B85 #FF3700). */
	static final float[] TB_MUZZLE_B_ENEMY = {0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, .938f, .726f, .75f, 1, .327f, .233f, 1, 1, .039f, 0};
	/** the muzzle smoke 19271 x B08 (2651 C41 / C43): greys (8001A0D9260E r4, peak 0.47; game hex #000000 #807B72 #716D65 #55524D #212121). Its near-black top replaced by its grey (ours). */
	static final float[] TB_MUZZLE_SMOKE = {0, 0, 0, 0, .25f, .216f, .198f, .169f, .5f, .166f, .152f, .131f, .75f, .091f, .084f, .073f, 1, .091f, .084f, .073f};
	/** the hit sparks 1F35 (2650 C11, 265B, 2509 C19): brown to pale cream, HDR 10 (8008B9759935 r1, peak 9.99; game hex #A52300 #ED6616 #FFAE1D #FFFF39 #FFFFFF). */
	static final float[] TB_RIVET_SPARK = {0, 0, 0, 0, .25f, .848f, .132f, .008f, .5f, 1, .425f, .012f, .75f, 1, 1, .042f, 1, 1, 1, 1};
	/** the rivet's hit flash 4C09 (2650 C13): black, brown, orange, white, HDR 6 (800BF5C56B5C r1, peak 6.18; game hex #000000 #A05A00 #FF9300 #FFFC4F #FFFFFF). */
	static final float[] TB_RIVET_HIT = {0, 0, 0, 0, .25f, .35f, .102f, 0, .5f, 1, .293f, 0, .75f, 1, .972f, .077f, 1, 1, 1, 1};
	/** a pellet's ribbon 7F5 x 192B0 (1865 C12): cream to dark brown, HDR 4, falls (800CFBFF66D1 r3, peak 4.00, falls; game hex #FFFFFF #FFA81C #FF4E04 #CB2E09 #481D11). Its dark top replaced by its orange (ours: the ribbon's bright middle took the dark end). */
	static final float[] TB_PELLET_TRAIL = {0, 0, 0, 0, .25f, 1, .391f, .011f, .5f, 1, .077f, .001f, .75f, .596f, .027f, .003f, 1, 1, .391f, .011f};
	/** its twin (1865 C16): red to dark brown, HDR 4, falls (8004A5808E1F r3, peak 4.00, falls; game hex #FF6D00 #F53706 #C72E0A #9A260E #481D11). Its dark top replaced by its red (ours). */
	static final float[] TB_PELLET_TRAIL_ENEMY = {0, 0, 0, 0, .25f, .912f, .038f, .002f, .5f, .573f, .027f, .003f, .75f, .321f, .02f, .004f, 1, .912f, .038f, .002f};
	/** its twin (2662 C23): black, red, peach, HDR 3 (800D05FD21B8 r1, peak 3.00; game hex #000000 #C30000 #FF0000 #FF9457 #FFFFFF). */
	static final float[] TB_PELLET_MUZZLE_ENEMY = {0, 0, 0, 0, .25f, .545f, 0, 0, .5f, 1, 0, 0, .75f, 1, .296f, .096f, 1, 1, 1, 1};
	/** a pellet's hit on a creature 44A8F x 1CB5D x 299E3 (05AF C7): brown, orange, peach, HDR 8 (80006777DF03 r7, peak 8.00; game hex #6E1800 #FF8300 #FFE303 #FFFFCC #FFFFFF). */
	static final float[] TB_PELLET_HIT = {0, 0, 0, 0, .25f, 1, .228f, 0, .5f, 1, .767f, .001f, .75f, 1, 1, .603f, 1, 1, 1, 1};
	/** the 1P reload's molten glow 1B6D6 x 5DF at hp 000A (00E2 C15): rust to yellow, HDR 3 (800A866948C3 r4, peak 3.00; game hex #FF5A00 #FFAF00 #FFFF00 #FFFF00 #FFFF00). */
	static final float[] TB_RELOAD_GLOW = {0, 0, 0, 0, .25f, 1, .427f, 0, .5f, 1, 1, 0, .75f, 1, 1, 0, 1, 1, 1, 0};
	/** the reload's soft glow A7B6 (00E2 C19): rust to gold, HDR 3 (8004370AA23B r1, peak 3.00; game hex #FF8500 #FFAF00 #FFF300 #FFFF00 #FFFF00). */
	static final float[] TB_RELOAD_GLOW_B = {0, 0, 0, 0, .25f, 1, .426f, 0, .5f, 1, .894f, 0, .75f, 1, 1, 0, 1, 1, 1, 0};
	/** the 3P reload's burst 5DF x 18B18 at hp 000B (029D C14): dark red, ochre, pale yellow, HDR 3 (800BBB36B981 r4, peak 3.00; game hex #FF0000 #FF6800 #FFE100 #FFFF82 #FFFFD2). */
	static final float[] TB_RELOAD_3P = {0, 0, 0, 0, .25f, 1, .138f, 0, .5f, 1, .752f, 0, .75f, 1, 1, .223f, 1, 1, 1, .647f};
	/** the turret's shot flash 4C09 (2654 C16): black, brown, ochre, cream, HDR 3 (8006309741B1 r0, peak 3.00; game hex #000000 #C76D00 #FF9700 #FFE0B2 #FFFFFF). */
	static final float[] TB_TURRET_FLASH = {0, 0, 0, 0, .25f, .569f, .154f, 0, .5f, 1, .308f, 0, .75f, 1, .745f, .445f, 1, 1, 1, 1};
	/** the turret's shot smoke 377D5 x B08 (2654 C26): greys (8005336A3133 r4, peak 0.48; game hex #000000 #514D48 #746F67 #54504A #000000). Its black top replaced by its grey (ours: the smoke's core drew black). */
	static final float[] TB_TURRET_SMOKE = {0, 0, 0, 0, .25f, .082f, .075f, .064f, .5f, .175f, .159f, .137f, .75f, .088f, .08f, .069f, 1, .088f, .08f, .069f};
	/** the thrown turret's ribbon 99F x 1298 (272D C2): cream to gold, HDR 2, falls (800B844FD37C r1, peak 2.00, falls; game hex #FFFFFF #FFFFFF #FFFFCA #FFF67D #FFE400). */
	static final float[] TB_THROWN = {0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, 1, .589f, .75f, 1, .922f, .205f, 1, 1, .775f, 0};
	/** the turret's shot line 801 x 510 x 50C (2654): tint D763 r4 (10, 5.4, 0), orange HDR, scaled to its brightest (H); the ramp ours. */
	static final float[] TB_TURRET_LINE = {0, 0, 0, 0, .3f, .3f, .16f, 0, .7f, 1, .54f, 0, 1, 1, .7f, .25f};

	// ---- Cassidy's (tools/reference/cassidy-effects.md "Proposed palettes": raw stops from fx_cassidy_palettes.py, each ending on
	// its colour; "/peak" normalised by the row's HDR peak, "top = 75 %" the white core dropped: ours) ----
	/** his shot's line 18D7 (mask 510 x 50C), his side: 0FEF r4 /peak, cyan to blue (H row, M role). */
	static final float[] CS_LINE = {0, 0, 0, 0, .25f, 0, .7f, .971f, .5f, 0, .568f, .938f, .75f, 0, .438f, .906f, 1, 0, .306f, .878f};
	/** the same to his enemies: C54D r4 /peak, orange to red. */
	static final float[] CS_LINE_ENEMY = {0, 0, 0, 0, .25f, .957f, .147f, 0, .5f, .913f, .059f, 0, .75f, .896f, .021f, 0, 1, .888f, .001f, 0};
	/** Deadeye's second line 510 x 50C (060D): 59BA r4 /peak, amber to red. */
	static final float[] CS_DEADEYE_LINE = {0, 0, 0, 0, .25f, .972f, .271f, 0, .5f, .944f, .18f, 0, .75f, .916f, .091f, 0, 1, .888f, .001f, 0};
	/** the smoke along the line (18D7's 4DB x B35 x 965): B2E7 r7 /peak, grey. */
	static final float[] CS_SMOKE_LINE = {0, 0, 0, 0, .25f, .362f, .361f, .358f, .5f, .36f, .36f, .36f, .75f, .36f, .36f, .36f, 1, .36f, .36f, .36f};
	/** Silver Bullet's blue-headed smoke (6DB8): 734D r7 /peak, blue to grey. */
	static final float[] CS_SILVER_LINE = {0, 0, 0, 0, .25f, .119f, .342f, .74f, .5f, .155f, .272f, .48f, .75f, .152f, .211f, .316f, 1, .15f, .15f, .15f};
	/** Deadeye's shot tube 8C0 (060D): 06FF r6, orange. */
	static final float[] CS_DEADEYE_TUBE = {0, 0, 0, 0, .25f, 1, .285f, .081f, .5f, 1, .285f, .081f, .75f, 1, .285f, .081f, 1, 1, .285f, .081f};
	/** the muzzle's flashes 5C04 (057A): 3352 r1, brown to cream, top = 75 %. */
	static final float[] CS_MUZZLE_FLASH = {0, 0, 0, 0, .25f, 1, .372f, 0, .5f, 1, .702f, .023f, 1, 1, 1, .106f};
	/** the muzzle's back flashes 5C04 (057A): B2A1 r1, dark red to cream, top = 75 %. */
	static final float[] CS_MUZZLE_FLASH_C = {0, 0, 0, 0, .25f, 1, .127f, .035f, .5f, 1, .285f, .077f, 1, 1, .756f, .437f};
	/** Fan the Hammer's muzzle flash (356B): BD91 r1, gold to cream. */
	static final float[] CS_FAN_FLASH = {0, 0, 0, 0, .25f, 1, .476f, .007f, .5f, 1, .496f, .015f, .75f, 1, .561f, .068f, 1, 1, .879f, .52f};
	/** the muzzle's sparks 1BCFB (057A): 8370 r2, orange to cream. */
	static final float[] CS_MUZZLE_SPARK = {0, 0, 0, 0, .25f, 1, .259f, 0, .5f, 1, .393f, 0, .75f, 1, .525f, 0, 1, 1, .888f, .584f};
	/** the muzzle's ring E700 (057A): 7147 r6, orange to cream, top = 75 %. */
	static final float[] CS_MUZZLE_RING = {0, 0, 0, 0, .25f, 1, .379f, 0, .5f, 1, .61f, 0, 1, 1, .817f, .194f};
	/** the muzzle's smoke B08 x 1C77 x 5429 (057A): B999 r7, cream to tan. */
	static final float[] CS_MUZZLE_SMOKE = {0, 0, 0, 0, .25f, 1, 1, .381f, .5f, 1, 1, .605f, .75f, 1, .908f, .551f, 1, .862f, .669f, .407f};
	/** the hit's flashes 5B71 (05AE), his side: 3949 r1, blue to cream, top = 75 %. */
	static final float[] CS_HIT = {0, 0, 0, 0, .25f, .006f, .471f, 1, .5f, .032f, .494f, .993f, 1, .512f, .71f, .83f};
	/** the same to his enemies: 606A r1, red to yellow, top = 75 %. */
	static final float[] CS_HIT_ENEMY = {0, 0, 0, 0, .25f, .917f, .01f, .003f, .5f, .997f, .021f, .005f, 1, 1, .239f, .019f};
	/** the hit's shards EF7 x 5B70 (05AE): 275F r1, gold to cream. */
	static final float[] CS_HIT_SHARD = {0, 0, 0, 0, .25f, 1, .476f, .007f, .5f, 1, .495f, .015f, .75f, 1, .678f, .255f, 1, 1, .879f, .52f};
	/** the same to his enemies: 2559 r1, red to yellow. */
	static final float[] CS_HIT_SHARD_ENEMY = {0, 0, 0, 0, .25f, .917f, .01f, .003f, .5f, .997f, .021f, .005f, .75f, 1, .239f, .019f, 1, .982f, 1, .048f};
	/** the hit's decal 29F7 (05AE), his side: 5C90 r2, cyan to blue. */
	static final float[] CS_HIT_DECAL = {0, 0, 0, 0, .25f, 0, 1, 1, .5f, 0, 1, 1, .75f, 0, .559f, 1, 1, 0, 0, 1};
	/** the same to his enemies: 6F1B r2, yellow to red. */
	static final float[] CS_HIT_DECAL_ENEMY = {0, 0, 0, 0, .25f, 1, 1, 0, .5f, 1, 1, 0, .75f, 1, .559f, 0, 1, 1, 0, 0};
	/** the reload's barrel smoke 9EF x B0A (0402 / 05C0): 4287 r0, peach. */
	static final float[] CS_RELOAD_SMOKE = {0, 0, 0, 0, .25f, 1, .284f, .214f, .5f, 1, .352f, .248f, .75f, 1, .419f, .283f, 1, 1, .486f, .317f};
	/** Silver Bullet armed at his wrist (6E6B), his side: CB3F r4, navy to pale cyan, top = 75 %. */
	static final float[] CS_SILVER_ARMED = {0, 0, 0, 0, .25f, .002f, .014f, .18f, .5f, .01f, .387f, .599f, 1, .118f, 1, 1};
	/** the same to his enemies: CB3F r5, dark red to cream, top = 75 %. */
	static final float[] CS_SILVER_ARMED_ENEMY = {0, 0, 0, 0, .25f, .076f, .001f, .001f, .5f, .325f, .013f, .005f, 1, 1, .227f, .13f};
	/** the bleed's glows (3488): F7C6 r0, pink to magenta. */
	static final float[] CS_BLEED = {0, 0, 0, 0, .25f, 1, .282f, .869f, .5f, 1, .231f, .933f, .75f, 1, .167f, .973f, 1, 1, .081f, .972f};
	/** the bleed's sparks 1F35 (3488): 8FF6 r1, dark red to pink, top = 75 %. */
	static final float[] CS_BLEED_SPARK = {0, 0, 0, 0, .25f, .484f, 0, 0, .5f, .968f, 0, 0, 1, 1, .132f, .132f};
	/** Combat Roll's dust 5429 x B08 x 1FC65 (05E4): 706F r2, black to cream, top = 75 %. */
	static final float[] CS_ROLL_DUST = {0, 0, 0, 0, .25f, .33f, .293f, .252f, .5f, .661f, .586f, .504f, 1, .986f, .875f, .753f};
	/** the Flashbang's flash 2D6C9 (5C8C), his side: FF88 r0, black to gold to pale yellow, top = 75 %. */
	static final float[] CS_FLASH = {0, 0, 0, 0, .25f, .419f, .245f, .005f, .5f, 1, .748f, .016f, 1, 1, 1, .064f};
	/** the same to his enemies: FF88 r1, rust to peach, top = 50 %. */
	static final float[] CS_FLASH_ENEMY = {0, 0, 0, 0, .25f, 1, .052f, .013f, 1, 1, .404f, .07f};
	/** the bang's dome 963D (5C8C), his side: E5D5 r1, gold, top = 75 %. */
	static final float[] CS_FLASH_DOME = {0, 0, 0, 0, .25f, .403f, .161f, 0, .5f, .682f, .302f, 0, 1, .879f, .485f, 0};
	/** the same to his enemies: E5D5 r2, red, top = 75 %. */
	static final float[] CS_FLASH_DOME_ENEMY = {0, 0, 0, 0, .25f, 1, .158f, .071f, .5f, 1, .256f, .123f, 1, 1, .411f, .202f};
	/** the bang's second flash 16E9D x 29887 (5C8C): A55C r4, blue, top = 75 %. */
	static final float[] CS_FLASH_RAYS = {0, 0, 0, 0, .25f, 0, .158f, .887f, .5f, .044f, .284f, 1, 1, .124f, .5f, 1};
	/** the bang's streaks 1FBCE x 19167 (5C8C), his side: CC06 r5, navy to pale blue, top = 75 %. */
	static final float[] CS_FLASH_STREAK = {0, 0, 0, 0, .25f, .052f, .125f, .42f, .5f, .033f, .243f, .733f, 1, .011f, .475f, 1};
	/** the same to his enemies: CC06 r7, plum to yellow, top = 75 %. */
	static final float[] CS_FLASH_STREAK_ENEMY = {0, 0, 0, 0, .25f, .898f, .21f, .025f, .5f, 1, .403f, .061f, 1, 1, 1, .3f};
	/** the Flashbang's ribbon in flight AC4 (0600): ECA2 r2 /peak, orange. */
	static final float[] CS_FLASH_TRAIL = {0, 0, 0, 0, .25f, 1, .139f, .006f, .5f, 1, .139f, .006f, .75f, 1, .139f, .006f, 1, 1, .139f, .006f};
	/** its second ribbon (0600): DB42 r3 /peak, warm yellow. */
	static final float[] CS_FLASH_TRAIL_B = {0, 0, 0, 0, .25f, 1, .728f, .269f, .5f, 1, .728f, .269f, .75f, 1, .728f, .269f, 1, 1, .728f, .269f};
	/** Deadeye's eye glint 2F4F0 x 242 (0610): EE96 r0, its closing black dropped (magenta at .5: L). */
	static final float[] CS_DEADEYE_EYE = {0, 0, 0, 0, .25f, 1, 0, 0, .5f, 1, 0, 1, 1, .542f, 0, 0};
	/** Deadeye's star F7A7 at his eye (0610): 7C32 r1, tan to white, top = 50 %. */
	static final float[] CS_DEADEYE_STAR = {0, 0, 0, 0, .25f, 1, .708f, .255f, 1, 1, .904f, .354f};
	/** Deadeye's glows 139C on him (0610), his side: 4EEC r1, ochre to white, top = 50 %. */
	static final float[] CS_DEADEYE_GLOW = {0, 0, 0, 0, .25f, 1, .68f, 0, 1, 1, 1, .264f};
	/** the same to his enemies: DFD2 r3, dark red to gold, top = 75 %. */
	static final float[] CS_DEADEYE_GLOW_ENEMY = {0, 0, 0, 0, .25f, .557f, .002f, 0, .5f, 1, .121f, 0, 1, 1, .435f, .003f};
	/** Deadeye's hit 010F (decal 220A, puffs 1BE6): 35DA r2, yellow to red. */
	static final float[] CS_DEADEYE_DECAL = {0, 0, 0, 0, .25f, 1, .74f, 0, .5f, 1, .48f, 0, .75f, 1, .225f, 0, 1, 1, 0, 0};
}
