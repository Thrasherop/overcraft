package dev.overcraft.client.effect;

/**
 * Sigma's palettes' stops (EffectArt.Palette.SIG_*: kept out of the enum's initializer, which has a 64 KB limit, as
 * SojournPalettes). Written by his palette pass from his ramps (tools/reference/sigma-effects.md "Palettes";
 * research\sigma\scriptsxx_sigma_palettes.py): each is (mask value, r, g, b) x 5, linear, black at 0; the _N rows are the
 * same LUT row divided by its peak. Each doc line gives the effect and renderer, the LUT id and row, and the game's colours.
 */
final class SigmaPalettes {
	private SigmaPalettes() {
	}

	/** a sphere's launch streak 20126 (2BE1 C12 / C14), 0.18-0.49 m stretched x11-13, 0.18 s: black, slate, white (800B4455C70C r1, peak 2.19; game hex #000000 #4B517B #6971AA #7F88CB #FFFFFF). */
	static final float[] SIG_SPHERE_LAUNCH = {0, 0, 0, 0, .25f, .071f, .082f, .2f, .5f, .141f, .165f, .4f, .75f, .211f, .246f, .597f, 1, 1, 1, 1};
	static final float[] SIG_SPHERE_LAUNCH_N = {0, 0, 0, 0, .25f, .032f, .038f, .091f, .5f, .065f, .075f, .183f, .75f, .096f, .112f, .273f, 1, 1, 1, 1};
	/** the launch's cone CA08 (2BE1 C17, 200E4 x 1C7A), 0.33-0.82 m, 0.3 s: navy, slate, white (800E7FFB73C2 r3, peak 2.00; game hex #000061 #414184 #6B6BA1 #B0B0CA #FFFFFF). */
	static final float[] SIG_SPHERE_LAUNCH_RING = {0, 0, 0, 0, .25f, .052f, .052f, .232f, .5f, .148f, .148f, .357f, .75f, .433f, .433f, .589f, 1, 1, 1, 1};
	static final float[] SIG_SPHERE_LAUNCH_RING_N = {0, 0, 0, 0, .25f, .026f, .026f, .116f, .5f, .074f, .074f, .179f, .75f, .217f, .217f, .295f, 1, 1, 1, 1};
	/** the launch cone's enemy twin (2BE1 C21): dark red, rose, white (8000F1B92B9A r3, peak 2.00; game hex #FF0000 #FF2C2C #FF4040 #FF6D6D #FFFFFF). */
	static final float[] SIG_SPHERE_LAUNCH_RING_ENEMY = {0, 0, 0, 0, .25f, 1, .026f, .026f, .5f, 1, .051f, .051f, .75f, 1, .153f, .153f, 1, 1, 1, 1};
	static final float[] SIG_SPHERE_LAUNCH_RING_ENEMY_N = {0, 0, 0, 0, .25f, .5f, .013f, .013f, .5f, .5f, .026f, .026f, .75f, .5f, .077f, .077f, 1, 1, .665f, .665f};
	/** the sphere in flight, sphere 85B (2BDE C21, 5DF x 18E48), 0.22 m, 10/s, 0.15 s: black, navy, white, HDR 7 (800609C43E60 r3, peak 7.00; game hex #000000 #00002B #00003F #2F2FFF #FFFFFF). */
	static final float[] SIG_SPHERE_CORE = {0, 0, 0, 0, .25f, 0, 0, .025f, .5f, 0, 0, .05f, .75f, .029f, .029f, 1, 1, 1, 1, 1};
	static final float[] SIG_SPHERE_CORE_N = {0, 0, 0, 0, .25f, 0, 0, .004f, .5f, 0, 0, .007f, .75f, .004f, .004f, .143f, 1, 1, 1, 1};
	/** the sphere's enemy twin (2BDE C23): black, dark red, white, HDR 7 (80055D3FEF24 r3, peak 7.00; game hex #000000 #2B0000 #3F0000 #FF2F2F #FFFFFF). */
	static final float[] SIG_SPHERE_CORE_ENEMY = {0, 0, 0, 0, .25f, .025f, 0, 0, .5f, .05f, 0, 0, .75f, 1, .029f, .029f, 1, 1, 1, 1};
	static final float[] SIG_SPHERE_CORE_ENEMY_N = {0, 0, 0, 0, .25f, .004f, 0, 0, .5f, .007f, 0, 0, .75f, .143f, .004f, .004f, 1, 1, 1, 1};
	/** the sphere's wide ribbon 1298 (2BDE C25), 1.0 m, 60/s, 0.25 s: lavender, blue, navy, falls (8009DF3F642D r3, peak 1.00, falls; game hex #7D7EFF #3132FF #0002DE #0002AA #00014A). */
	static final float[] SIG_SPHERE_TRAIL = {0, 0, 0, 0, .25f, .031f, .032f, 1, .5f, 0, .001f, .727f, .75f, 0, .001f, .4f, 1, 0, 0, .068f};
	/** the wide ribbon's enemy twin (2BDE C34): orange, red, dark red, falls (800F5F1BA99C r3, peak 1.00, falls; game hex #FF9D32 #FF400F #DE0000 #AA0000 #4A0000). */
	static final float[] SIG_SPHERE_TRAIL_ENEMY = {0, 0, 0, 0, .25f, 1, .051f, .005f, .5f, .727f, 0, 0, .75f, .4f, 0, 0, 1, .068f, 0, 0};
	/** the sphere's thin ribbon 1298 (2BDE C27), 0.25 m, 60/s, 0.32 s: periwinkle to indigo, HDR 4, falls (80004D860CF1 r4, peak 4.00, falls; game hex #F3FDFF #D4E0FF #ADB9FF #7682FF #4A59FF). */
	static final float[] SIG_SPHERE_TRAIL_THIN = {0, 0, 0, 0, .25f, .659f, .747f, 1, .5f, .418f, .484f, 1, .75f, .182f, .225f, 1, 1, .068f, .1f, 1};
	static final float[] SIG_SPHERE_TRAIL_THIN_N = {0, 0, 0, 0, .25f, .165f, .187f, .88f, .5f, .105f, .121f, .627f, .75f, .045f, .056f, .378f, 1, .017f, .025f, .25f};
	/** the thin ribbon's enemy twin (2BDE C36): peach to dark red, HDR 7, falls (800BB7D6B9BE r4, peak 7.00, falls; game hex #FFFFFF #FFFABE #FF6363 #FF4E4E #FF2E2E). */
	static final float[] SIG_SPHERE_TRAIL_THIN_ENEMY = {0, 0, 0, 0, .25f, 1, .96f, .515f, .5f, 1, .126f, .126f, .75f, 1, .077f, .077f, 1, 1, .027f, .027f};
	static final float[] SIG_SPHERE_TRAIL_THIN_ENEMY_N = {0, 0, 0, 0, .25f, .785f, .137f, .074f, .5f, .569f, .018f, .018f, .75f, .358f, .011f, .011f, 1, .143f, .004f, .004f};
	/** the sphere's halo 4C09 (2BDE C30), 0.32 m, 5/s + 3, 0.34 s: black, blue, lavender, white, HDR 2 (800CC5F6C405 r1, peak 2.00; game hex #000000 #4154E6 #9AA1FF #FFFFFF #FFFFFF). */
	static final float[] SIG_SPHERE_HALO = {0, 0, 0, 0, .25f, .053f, .089f, .794f, .5f, .323f, .356f, 1, .75f, 1, 1, 1, 1, 1, 1, 1};
	static final float[] SIG_SPHERE_HALO_N = {0, 0, 0, 0, .25f, .026f, .044f, .397f, .5f, .162f, .178f, .502f, .75f, .515f, .528f, .783f, 1, 1, 1, 1};
	/** the halo's enemy twin (2BDE C33): black, maroon, pink, HDR 5 (800C18AB9F2D r1, peak 5.00; game hex #000000 #E64141 #FF6767 #FFDBDB #FFFFFF). */
	static final float[] SIG_SPHERE_HALO_ENEMY = {0, 0, 0, 0, .25f, .794f, .053f, .053f, .5f, 1, .135f, .135f, .75f, 1, .71f, .71f, 1, 1, 1, 1};
	static final float[] SIG_SPHERE_HALO_ENEMY_N = {0, 0, 0, 0, .25f, .159f, .011f, .011f, .5f, .204f, .027f, .027f, .75f, .653f, .142f, .142f, 1, 1, .305f, .305f};
	/** the sphere's dark motes 1F35 (2BDE C39), 0.16 m, 10-16/s at 3 m/s, 0.33 s: black to dark indigo, peak 0.23 (8004AFDAC522 r1, peak 0.23; game hex #000000 #181545 #242160 #2D2A74 #353185). */
	static final float[] SIG_SPHERE_MOTE = {0, 0, 0, 0, .25f, .009f, .008f, .059f, .5f, .018f, .015f, .118f, .75f, .027f, .023f, .176f, 1, .036f, .031f, .234f};
	/** the burst's disc 11C1 (2EAB C33, 1FDB2), 4.4-5.4 m, 0.3-0.39 s: black, slate, white, HDR 10 (8005B267580A r3, peak 9.99; game hex #000000 #726FAB #9D99E9 #DBD9FF #FFFFFF). */
	static final float[] SIG_BURST_RING = {0, 0, 0, 0, .25f, .168f, .16f, .407f, .5f, .336f, .319f, .815f, .75f, .707f, .696f, 1, 1, 1, 1, 1};
	static final float[] SIG_BURST_RING_N = {0, 0, 0, 0, .25f, .017f, .016f, .041f, .5f, .034f, .032f, .082f, .75f, .071f, .07f, .1f, 1, 1, 1, 1};
	/** the burst disc's enemy twin (2EAB C27): black, brown-red, white, HDR 10 (800B4003D5F1 r3, peak 9.99; game hex #000000 #AB6F6F #E99999 #FFD9D9 #FFFFFF). */
	static final float[] SIG_BURST_RING_ENEMY = {0, 0, 0, 0, .25f, .407f, .16f, .16f, .5f, .815f, .319f, .319f, .75f, 1, .696f, .696f, 1, 1, 1, 1};
	static final float[] SIG_BURST_RING_ENEMY_N = {0, 0, 0, 0, .25f, .041f, .016f, .016f, .5f, .082f, .032f, .032f, .75f, .1f, .07f, .07f, 1, 1, 1, 1};
	/** the burst's dark streaks 19167 x 1FBCE (2EAB C39 / C41), 0.27-0.57 m stretched x20: black to dark navy, peak 0.12 (8003C87617EB r3, peak 0.12; game hex #000000 #000631 #000C46 #001155 #001562). */
	static final float[] SIG_BURST_STREAK = {0, 0, 0, 0, .25f, 0, .002f, .031f, .5f, 0, .004f, .061f, .75f, 0, .006f, .091f, 1, 0, .007f, .122f};
	/** the dark streaks' enemy twin (2EAB C29 / C31): black to dark red, peak 0.12 (8004212E3DDA r3, peak 0.12; game hex #000000 #310000 #460000 #550000 #620000). */
	static final float[] SIG_BURST_STREAK_ENEMY = {0, 0, 0, 0, .25f, .031f, 0, 0, .5f, .061f, 0, 0, .75f, .091f, 0, 0, 1, .122f, 0, 0};
	/** the burst's sphere DAB5 (2EAB C35, 200E4 x 1C7A), 4.5 m, 0.32-0.37 s: black, navy, blue, white (800752AE6CA5 r3, peak 1.00; game hex #000000 #001E5A #05236E #133AAD #FFFFFF). */
	static final float[] SIG_BURST_SHELL = {0, 0, 0, 0, .25f, 0, .013f, .103f, .5f, .002f, .017f, .155f, .75f, .006f, .042f, .415f, 1, 1, 1, 1};
	/** the burst sphere's enemy twin (2EAB C47): black, dark red, red, white (80061EAEC2BD r3, peak 1.00; game hex #000000 #5A0000 #6E0505 #AD1313 #FFFFFF). */
	static final float[] SIG_BURST_SHELL_ENEMY = {0, 0, 0, 0, .25f, .103f, 0, 0, .5f, .155f, .002f, .002f, .75f, .415f, .006f, .006f, 1, 1, 1, 1};
	/** the burst's flash 208CC x 7AB (2EAB C44), 8-9 m, 0.25-0.3 s: black, navy, slate, white, HDR 3 (800009BFE391 r2, peak 3.00; game hex #000000 #2948A5 #3C65E1 #88A1FF #FFFFFF). */
	static final float[] SIG_BURST_FLASH = {0, 0, 0, 0, .25f, .022f, .066f, .376f, .5f, .045f, .131f, .752f, .75f, .244f, .359f, 1, 1, 1, 1, 1};
	static final float[] SIG_BURST_FLASH_N = {0, 0, 0, 0, .25f, .007f, .022f, .125f, .5f, .015f, .044f, .251f, .75f, .082f, .12f, .333f, 1, .999f, .999f, 1};
	/** the burst flash's enemy twin (2EAB C37): black, maroon, rose, white, HDR 3 (800BA3DAE900 r2, peak 3.00; game hex #000000 #A52929 #E13C3C #FF8888 #FFFFFF). */
	static final float[] SIG_BURST_FLASH_ENEMY = {0, 0, 0, 0, .25f, .376f, .022f, .022f, .5f, .752f, .045f, .045f, .75f, 1, .244f, .244f, 1, 1, 1, 1};
	static final float[] SIG_BURST_FLASH_ENEMY_N = {0, 0, 0, 0, .25f, .125f, .007f, .007f, .5f, .251f, .015f, .015f, .75f, .333f, .082f, .082f, 1, 1, .999f, .999f};
	/** the burst's refraction sphere 85B (2EAB C25, BC5 normal map 18F81), 1 m, 0.2 s: navy, blue (L: a distortion, drawn as a faint dark sphere) (80079163D5BF r1, peak 0.78; game hex #00000F #0000A8 #00009F #00004F #000001). */
	static final float[] SIG_BURST_WARP = {0, 0, 0, 0, .25f, 0, 0, .393f, .5f, 0, 0, .345f, .75f, 0, 0, .078f, 1, 0, 0, 0};
	/** a hand sphere re-forming, sphere DBF (2D5B C21 at 0.6 s), 0.085 m, 0.35 s: black, navy, slate, white, HDR 5 (800FAAE9ECFD r3, peak 5.00; game hex #000000 #588AFF #AABCFF #DAE1FF #FFFFFF). */
	static final float[] SIG_HAND_CORE = {0, 0, 0, 0, .25f, .098f, .256f, 1, .5f, .4f, .505f, 1, .75f, .698f, .75f, 1, 1, 1, 1, 1};
	static final float[] SIG_HAND_CORE_N = {0, 0, 0, 0, .25f, .02f, .051f, .2f, .5f, .08f, .101f, .2f, .75f, .14f, .15f, .2f, 1, .999f, .999f, 1};
	/** the hand sphere's sparkles B08 x 1FC54 (2D5B C23 / C25 from 0.9 s), 0.24-0.37 m, 7/s: black, lavender, white, HDR 1.5 (80056B573858 r1, peak 1.50; game hex #000000 #7E7EFF #B7B7FF #DFDFFF #FFFFFF). */
	static final float[] SIG_HAND_SPARK = {0, 0, 0, 0, .25f, .208f, .208f, 1, .5f, .473f, .473f, 1, .75f, .734f, .734f, 1, 1, 1, 1, 1};
	static final float[] SIG_HAND_SPARK_N = {0, 0, 0, 0, .25f, .139f, .139f, .667f, .5f, .316f, .316f, .667f, .75f, .49f, .49f, .667f, 1, .999f, .999f, 1};
	/** the sparkles' enemy twin (2D5B C27): black, rose, pink, white, HDR 1.5 (800259334CD2 r1, peak 1.50; game hex #000000 #FF5F5F #FFACAC #FFDADA #FFFFFF). */
	static final float[] SIG_HAND_SPARK_ENEMY = {0, 0, 0, 0, .25f, 1, .114f, .114f, .5f, 1, .411f, .411f, .75f, 1, .703f, .703f, 1, 1, 1, 1};
	static final float[] SIG_HAND_SPARK_ENEMY_N = {0, 0, 0, 0, .25f, .667f, .076f, .076f, .5f, .667f, .274f, .274f, .75f, .667f, .469f, .469f, 1, 1, .999f, .999f};
	/** the hand sphere's glow 174D8 (2D5B C33 at 0.6 s), 0.68-0.83 m, 0.68 s: black, navy, blue, lavender, white, HDR 3 (80002B22BAB7 r1, peak 3.00; game hex #000000 #0000B3 #0000F4 #FFFFFF #FFFFFF). */
	static final float[] SIG_HAND_GLOW = {0, 0, 0, 0, .25f, 0, 0, .451f, .5f, 0, 0, .901f, .75f, 1, 1, 1, 1, 1, 1, 1};
	static final float[] SIG_HAND_GLOW_N = {0, 0, 0, 0, .25f, 0, 0, .15f, .5f, 0, 0, .3f, .75f, .348f, .348f, .655f, 1, 1, 1, 1};
	/** the hand sphere's twinkles 1FBCE (2D5B C31), 0.11 m, 7/s + 5: black, indigo, lavender, white, HDR 3 (80040009B405 r1, peak 3.00; game hex #000000 #3737BB #4E4EFE #FFFFFF #FFFFFF). */
	static final float[] SIG_HAND_TWINKLE = {0, 0, 0, 0, .25f, .038f, .038f, .497f, .5f, .076f, .076f, .994f, .75f, 1, 1, 1, 1, 1, 1, 1};
	static final float[] SIG_HAND_TWINKLE_N = {0, 0, 0, 0, .25f, .013f, .013f, .166f, .5f, .025f, .025f, .331f, .75f, .377f, .377f, .704f, 1, 1, 1, 1};
	/** the hand spheres' own material 7C98 (shader B87768E79B35, masks 19167 / 18F00 / 5DF): its LUT row 1 (alpha up to 3.4 = its emission, M): black, plum, violet, blue (8000DFB0EAFE r1, peak 0.75; game hex #000000 #330006 #54000F #55006A #005BB9). */
	/**
	 * the same row with its alpha as the emission it is (RGB x A, M: A rises 0 -> 1.4 -> 3.4 -> 1.35 along it), over its peak
	 * 1.56: black, plum, violet, vivid blue-violet, blue (ours: the multiply; drawn as is, the row's middle read pink on the cores,
	 * found playing).
	 */
	static final float[] SIG_HAND_EMISSION = {0, 0, 0, 0, .25f, .03f, 0, .002f, .5f, .14f, 0, .22f, .75f, .064f, .03f, 1, 1, 0, .09f, .42f};
	static final float[] SIG_HAND_MATERIAL = {0, 0, 0, 0, .25f, .033f, 0, .002f, .5f, .09f, 0, .005f, .75f, .092f, 0, .143f, 1, 0, .105f, .487f};
	/** the barrier's own material 7C75 (shader EB04637DEAF0; hex cells 18F9B, seams 18F9A, rim 1FE0A, cracks 548D1): LUT row 2: blue, sky blue, white, HDR 6 (800567241D0F r2, peak 6.00; game hex #4D9DFF #5CBEFF #71FFFF #95FFFF #FFFFFF). */
	static final float[] SIG_BARRIER = {0, 0, 0, 0, .25f, .106f, .515f, 1, .5f, .165f, 1, 1, .75f, .299f, 1, 1, 1, 1, 1, 1};
	static final float[] SIG_BARRIER_N = {0, 0, 0, 0, .25f, .018f, .086f, .291f, .5f, .028f, .173f, .395f, .75f, .05f, .356f, .56f, 1, 1, 1, 1};
	/** the barrier material's other row (M: the enemy look): dark red, red, orange, white, HDR 8 (800567241D0F r1, peak 8.00; game hex #CD5D5B #FF8028 #FF9B00 #FFFF00 #FFFFFF). */
	static final float[] SIG_BARRIER_ENEMY = {0, 0, 0, 0, .25f, 1, .217f, .021f, .5f, 1, .326f, 0, .75f, 1, 1, 0, 1, 1, 1, 1};
	static final float[] SIG_BARRIER_ENEMY_N = {0, 0, 0, 0, .25f, .459f, .027f, .003f, .5f, .898f, .041f, 0, .75f, 1, .363f, 0, 1, 1, .991f, 1};
	/** motes at the barrier's four corners 0029 / 0032 / 002A / 0033 (2C8E C7, 4DB x 1FC54), 0.65-0.75 m, 2/s each, 0.66-1.3 s: black, blue, sky blue (800AFC7EF12D r3, peak 1.00; game hex #000000 #0055B2 #0076F2 #3D93FF #5DACFF). */
	static final float[] SIG_BARRIER_MOTE = {0, 0, 0, 0, .25f, 0, .091f, .443f, .5f, 0, .182f, .886f, .75f, .046f, .292f, 1, 1, .109f, .412f, 1};
	/** the corner motes' other renderer (2C8E C14, 2-4/s each; M: the enemy twin): black, red, peach, white, HDR 1.5 (800ADC53D831 r3, peak 1.50; game hex #000000 #DE0000 #FF8B69 #FFEEB6 #FFFFFF). */
	static final float[] SIG_BARRIER_MOTE_ENEMY = {0, 0, 0, 0, .25f, .733f, 0, 0, .5f, 1, .256f, .14f, .75f, 1, .855f, .467f, 1, 1, 1, 1};
	static final float[] SIG_BARRIER_MOTE_ENEMY_N = {0, 0, 0, 0, .25f, .489f, 0, 0, .5f, .667f, .171f, .093f, .75f, .866f, .57f, .312f, 1, 1, .999f, .999f};
	/** streaks off his left hand while it's sent (2DD2 C7, 16E9D x 4C09), 0.49 m stretched x2, 6/s at 4.9 m/s, 0.67-1.3 s: black, indigo, lavender, white, HDR 2 (8001383F3C84 r3, peak 2.00; game hex #000000 #6163FF #ACADFF #DADBFF #FFFFFF). */
	static final float[] SIG_BARRIER_SEND = {0, 0, 0, 0, .25f, .118f, .125f, 1, .5f, .414f, .418f, 1, .75f, .705f, .707f, 1, 1, 1, 1, 1};
	static final float[] SIG_BARRIER_SEND_N = {0, 0, 0, 0, .25f, .059f, .063f, .5f, .5f, .207f, .209f, .5f, .75f, .352f, .353f, .5f, 1, 1, 1, 1};
	/** glints on his hand while it's sent (2DD2 C9, 1FC54 x 19167), 0.12-0.37 m, 0.17 s: black, slate, white, HDR 3 (8002F79407AD r1, peak 3.00; game hex #000000 #6666B9 #8D8DFB #CFCFFF #FFFFFF). */
	static final float[] SIG_BARRIER_SEND_SPARK = {0, 0, 0, 0, .25f, .133f, .133f, .483f, .5f, .265f, .265f, .966f, .75f, .621f, .621f, 1, 1, 1, 1, 1};
	static final float[] SIG_BARRIER_SEND_SPARK_N = {0, 0, 0, 0, .25f, .044f, .044f, .161f, .5f, .088f, .088f, .322f, .75f, .207f, .207f, .334f, 1, 1, 1, 1};
	/** the glow at his hand on recall (2DD7 C5, 0.84 m, 0.35 s) and while held (2DF5 C5, 0.33 m): black, indigo, lavender, white, HDR 2 (800C5815818A r1, peak 2.00; game hex #000000 #6163FF #ACADFF #DADBFF #FFFFFF). */
	static final float[] SIG_BARRIER_HAND = {0, 0, 0, 0, .25f, .118f, .125f, 1, .5f, .414f, .418f, 1, .75f, .705f, .707f, 1, 1, 1, 1, 1};
	static final float[] SIG_BARRIER_HAND_N = {0, 0, 0, 0, .25f, .059f, .063f, .5f, .5f, .207f, .209f, .5f, .75f, .352f, .353f, .5f, 1, 1, 1, 1};
	/** recall: streaks from its four corners toward him (2E68 C7, 1FBCE), 1.0 m, 2-3 at 20-30 m/s, 0.19-0.33 s: white, periwinkle, navy, HDR 7, falls (800E2A441B4A r2, peak 7.00, falls; game hex #FFFFFF #FFFFFF #E5FFFF #A8D3FF #0000FF). */
	static final float[] SIG_BARRIER_FLYBACK = {0, 0, 0, 0, .25f, 1, 1, 1, .5f, .781f, 1, 1, .75f, .394f, .65f, 1, 1, 0, 0, 1};
	static final float[] SIG_BARRIER_FLYBACK_N = {0, 0, 0, 0, .25f, .241f, .398f, .785f, .5f, .112f, .184f, .569f, .75f, .056f, .093f, .358f, 1, 0, 0, .143f};
	/** the flyback streaks' enemy twin (2E68 C9 / 2C90 C21): white, salmon, dark red, HDR 7, falls (8003B879145D r2, peak 7.00, falls; game hex #FFFFFF #FFFFFF #FFFFE5 #FFC6A8 #FF0000). */
	static final float[] SIG_BARRIER_FLYBACK_ENEMY = {0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, 1, .781f, .75f, 1, .565f, .394f, 1, 1, 0, 0};
	static final float[] SIG_BARRIER_FLYBACK_ENEMY_N = {0, 0, 0, 0, .25f, .785f, .346f, .241f, .5f, .569f, .16f, .112f, .75f, .358f, .081f, .056f, 1, .143f, 0, 0};
	/** recall: the flash at the barrier (2C90 C23, 29F7), 2.0 m, 0.1 s: black, indigo, lavender (800ECC3C9FF2 r0, peak 1.00; game hex #000000 #4A4A89 #6868BC #7D7DE0 #8F8FFF). */
	static final float[] SIG_BARRIER_RECALL_FLASH = {0, 0, 0, 0, .25f, .069f, .069f, .251f, .5f, .138f, .138f, .502f, .75f, .206f, .206f, .749f, 1, .275f, .275f, 1};
	/** the recall flash's enemy twin (2C90 C33): black, maroon, red (800265166ADA r0, peak 1.00; game hex #000000 #891D1D #BC2B2B #E03535 #FF3E3E). */
	static final float[] SIG_BARRIER_RECALL_FLASH_ENEMY = {0, 0, 0, 0, .25f, .251f, .012f, .012f, .5f, .502f, .024f, .024f, .75f, .749f, .036f, .036f, 1, 1, .048f, .048f};
	/** recall: glows at the barrier (2C90 C27, 174D8), 2.0 m, 0.18-0.36 s: black, indigo, lavender, HDR 3 (800F9D5A0FC7 r1, peak 3.00; game hex #000000 #4B4B8A #6868BD #7E7EE2 #EAEAFF). */
	static final float[] SIG_BARRIER_RECALL_GLOW = {0, 0, 0, 0, .25f, .07f, .07f, .254f, .5f, .139f, .139f, .508f, .75f, .208f, .208f, .758f, 1, .824f, .824f, 1};
	static final float[] SIG_BARRIER_RECALL_GLOW_N = {0, 0, 0, 0, .25f, .023f, .023f, .085f, .5f, .046f, .046f, .169f, .75f, .069f, .069f, .253f, 1, .275f, .275f, 1};
	/** the recall glow's enemy twin (2C90 C29, 3.0 m): black, maroon, red, HDR 3 (800ACA9CA760 r1, peak 3.00; game hex #000000 #8A1D1D #BD2B2B #E23636 #FF6A6A). */
	static final float[] SIG_BARRIER_RECALL_GLOW_ENEMY = {0, 0, 0, 0, .25f, .254f, .012f, .012f, .5f, .508f, .025f, .025f, .75f, .758f, .036f, .036f, 1, 1, .144f, .144f};
	static final float[] SIG_BARRIER_RECALL_GLOW_ENEMY_N = {0, 0, 0, 0, .25f, .085f, .004f, .004f, .5f, .169f, .008f, .008f, .75f, .253f, .012f, .012f, 1, 1, .048f, .048f};
	/** break: puffs 14F9 (4E26 / 6919 C16), 16 x 0.68-1.28 m in a 65 deg cone, 0.2-0.35 s: navy, blue, sky blue, white (8001BED36202 r3, peak 1.00; game hex #021148 #0016BE #388AEE #A8CCFF #FFFFFF). */
	static final float[] SIG_BARRIER_BREAK = {0, 0, 0, 0, .25f, 0, .008f, .513f, .5f, .04f, .254f, .858f, .75f, .392f, .603f, 1, 1, 1, 1, 1};
	/** the break puffs' enemy row (same LUT row 4): dark red, red, pink, white (8001BED36202 r4, peak 1.00; game hex #480202 #BE0000 #EE3838 #FFA8A8 #FFFFFF). */
	static final float[] SIG_BARRIER_BREAK_ENEMY = {0, 0, 0, 0, .25f, .513f, 0, 0, .5f, .858f, .04f, .04f, .75f, 1, .392f, .392f, 1, 1, 1, 1};
	/** break: 50 sparks 1F35 (4E26 C22), 0.08-0.1 m at 4.4-9.4 m/s, 0.25-1 s: cream, periwinkle, blue, HDR 4, falls (80028C6D7A81 r2, peak 4.00, falls; game hex #FFFFFF #FFFFFF #C4FFFF #88F7FF #59DFFF). */
	static final float[] SIG_BARRIER_BREAK_SPARK = {0, 0, 0, 0, .25f, 1, 1, 1, .5f, .551f, 1, 1, .75f, .245f, .934f, 1, 1, .101f, .738f, 1};
	static final float[] SIG_BARRIER_BREAK_SPARK_N = {0, 0, 0, 0, .25f, .274f, .469f, .977f, .5f, .138f, .337f, 1, .75f, .061f, .233f, 1, 1, .025f, .184f, 1};
	/** the break sparks' enemy row: pink-white, salmon, red, HDR 4, falls (80028C6D7A81 r3, peak 4.00, falls; game hex #FFFFFF #FFFFFF #FFC4C4 #FF8888 #FF5959). */
	static final float[] SIG_BARRIER_BREAK_SPARK_ENEMY = {0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, .551f, .551f, .75f, 1, .245f, .245f, 1, 1, .101f, .101f};
	static final float[] SIG_BARRIER_BREAK_SPARK_ENEMY_N = {0, 0, 0, 0, .25f, 1, .251f, .251f, .5f, 1, .138f, .138f, .75f, 1, .061f, .061f, 1, 1, .025f, .025f};
	/** break: 12 shards 2B0F / 2B0E / 2B10 on mesh EE4 (4E26 C26), 0.8-1.3 m at 3-5 m/s, 0.2-0.27 s: black, blue, black (a bell), HDR 2.8 (800EFBD692D3 r0, peak 2.83; game hex #000000 #00D2FF #00A4FF #0059AB #000000). */
	static final float[] SIG_BARRIER_BREAK_STREAK = {0, 0, 0, 0, .25f, 0, .643f, 1, .5f, 0, .369f, 1, .75f, 0, .1f, .405f, 1, 0, 0, 0};
	static final float[] SIG_BARRIER_BREAK_STREAK_N = {0, 0, 0, 0, .25f, 0, .227f, .921f, .5f, 0, .13f, .529f, .75f, 0, .035f, .143f, 1, 0, 0, 0};
	/** break while moving: the barrier-shaped flash, mesh A3C 6.79 x 4.25 m (2E66 C41, 17C0), size 0.8, 0.18 s: periwinkle, blue, teal, cyan, HDR 5 (8005E1821FDD r0, peak 5.00, falls; game hex #FFFFFF #BFF5FF #4CFFFF #83FFFF #94FFFF). */
	static final float[] SIG_BARRIER_SHATTER = {0, 0, 0, 0, .25f, .524f, .912f, 1, .5f, .072f, 1, 1, .75f, .226f, 1, 1, 1, .297f, 1, 1};
	static final float[] SIG_BARRIER_SHATTER_N = {0, 0, 0, 0, .25f, .105f, .182f, .729f, .5f, .014f, .241f, .62f, .75f, .045f, .525f, .695f, 1, .059f, .658f, .73f};
	/** Hyper Regeneration's heal on the barrier (6E81 C4, 2B10 x 2B0E on mesh EE4), 0.95 m stretched, 16/s for 0.5 s at hp 01DA: navy, teal, cyan, mint, HDR 3 (80054C5B2976 r3, peak 3.00; game hex #00064C #0088CF #00BAFF #00FCFF #FFFFFF). */
	static final float[] SIG_BARRIER_HEAL = {0, 0, 0, 0, .25f, 0, .246f, .622f, .5f, 0, .491f, 1, .75f, 0, .978f, 1, 1, 1, 1, 1};
	static final float[] SIG_BARRIER_HEAL_N = {0, 0, 0, 0, .25f, 0, .082f, .208f, .5f, 0, .164f, .391f, .75f, 0, .326f, .534f, 1, .397f, 1, .948f};
	/** the heal's enemy row: brown, rust, orange, cream, HDR 3 (80054C5B2976 r4, peak 3.00; game hex #4C0000 #CF3000 #FF4500 #FFBD00 #FFFFFF). */
	static final float[] SIG_BARRIER_HEAL_ENEMY = {0, 0, 0, 0, .25f, .622f, .03f, 0, .5f, 1, .06f, 0, .75f, 1, .506f, 0, 1, 1, 1, 1};
	static final float[] SIG_BARRIER_HEAL_ENEMY_N = {0, 0, 0, 0, .25f, .208f, .01f, 0, .5f, .391f, .02f, 0, .75f, .531f, .169f, 0, 1, 1, .738f, .397f};
	/** the field's disc C770 2.0 m flat (2BE4 C35, 18A1 x 200E4) 2.8 m ahead of hp 01B6, x4.1-4.5 = 8-9 m across, 8/s, 0.37-0.49 s: black, navy, blue, HDR 3 (800C75B864B9 r3, peak 3.00; game hex #000000 #003C89 #0054BC #0066E0 #00C1FF). */
	static final float[] SIG_GRASP_FIELD = {0, 0, 0, 0, .25f, 0, .045f, .251f, .5f, 0, .089f, .502f, .75f, 0, .133f, .749f, 1, 0, .533f, 1};
	static final float[] SIG_GRASP_FIELD_N = {0, 0, 0, 0, .25f, 0, .015f, .084f, .5f, 0, .03f, .167f, .75f, 0, .044f, .25f, 1, 0, .178f, 1};
	/** the field's dish C943 (2BE4 C33) at hp 01B6, 0.52-0.83 m, 6/s, 0.6-0.9 s: black, navy, blue, sky blue (800070B4C44A r3, peak 0.97; game hex #000000 #052367 #0A348F #6790CE #8FC1FC). */
	static final float[] SIG_GRASP_DISH = {0, 0, 0, 0, .25f, .002f, .017f, .137f, .5f, .003f, .034f, .273f, .75f, .137f, .28f, .619f, 1, .275f, .533f, .973f};
	/** the field's shell C62D 1.5 x 1.5 x 1.14 m (2BE4 C44), 1.0, 2/s, 1 s: navy, blue, periwinkle, HDR 3 (8002B2F88AAF r5, peak 3.00; game hex #0072FF #0037A8 #3053C8 #5076FC #FFFFFF). */
	static final float[] SIG_GRASP_SHELL = {0, 0, 0, 0, .25f, 0, .038f, .393f, .5f, .03f, .087f, .579f, .75f, .081f, .181f, .973f, 1, 1, 1, 1};
	static final float[] SIG_GRASP_SHELL_N = {0, 0, 0, 0, .25f, 0, .013f, .131f, .5f, .01f, .029f, .193f, .75f, .027f, .06f, .324f, 1, .351f, .462f, 1};
	/** the shell's enemy twin (2BE4 C49): red, maroon, orange-peach, HDR 3.5 (800ABC745F7C r5, peak 3.51; game hex #FF0000 #BD1F1F #D33028 #FF4F1B #FFFFCE). */
	static final float[] SIG_GRASP_SHELL_ENEMY = {0, 0, 0, 0, .25f, .507f, .013f, .013f, .5f, .654f, .03f, .021f, .75f, 1, .079f, .011f, 1, 1, 1, .615f};
	static final float[] SIG_GRASP_SHELL_ENEMY_N = {0, 0, 0, 0, .25f, .144f, .004f, .004f, .5f, .186f, .009f, .006f, .75f, .305f, .023f, .003f, 1, .853f, .361f, .175f};
	/** the glow at hp 0313 (2BE4 C39, 208CC), 3.0 m, 10-22/s by dataflow 64F, 0.1-0.15 s: black, indigo, lavender, white (80036603DF97 r1, peak 1.00; game hex #010028 #5A57D5 #9998FF #D4D3FF #FFFFFF). */
	static final float[] SIG_GRASP_GLOW = {0, 0, 0, 0, .25f, .101f, .095f, .668f, .5f, .32f, .313f, 1, .75f, .657f, .654f, 1, 1, 1, 1, 1};
	/** the glow's enemy twin (2BE4 C41, 2.65 m): dark red, rose, pink, white (800A4AC7707A r1, peak 1.00; game hex #280000 #BD4C4C #FF6A6A #FFC6C6 #FFFFFF). */
	static final float[] SIG_GRASP_GLOW_ENEMY = {0, 0, 0, 0, .25f, .51f, .072f, .072f, .5f, .999f, .144f, .144f, .75f, 1, .568f, .568f, 1, 1, 1, 1};
	/** the orb at hp 0313 (2BE4 C27, sphere DBF), 0.3 m, held: black, navy, indigo, white, HDR 5 (80063092E833 r1, peak 5.00; game hex #131338 #4646D0 #7070FF #DFDFFF #FFFFFF). */
	static final float[] SIG_GRASP_ORB = {0, 0, 0, 0, .25f, .061f, .061f, .631f, .5f, .162f, .162f, 1, .75f, .736f, .736f, 1, 1, 1, 1, 1};
	static final float[] SIG_GRASP_ORB_N = {0, 0, 0, 0, .25f, .012f, .012f, .126f, .5f, .032f, .032f, .236f, .75f, .147f, .147f, .254f, 1, 1, 1, 1};
	/** the orb's enemy twin (2BE4 C29): black, maroon, red, white, HDR 5 (8008EBD74818 r1, peak 5.00; game hex #381313 #B31C1C #EF2323 #FF3B3B #FFFFFF). */
	static final float[] SIG_GRASP_ORB_ENEMY = {0, 0, 0, 0, .25f, .453f, .012f, .012f, .5f, .867f, .017f, .017f, .75f, 1, .044f, .044f, 1, 1, 1, 1};
	static final float[] SIG_GRASP_ORB_ENEMY_N = {0, 0, 0, 0, .25f, .091f, .002f, .002f, .5f, .173f, .003f, .003f, .75f, .254f, .009f, .009f, 1, 1, 1, 1};
	/** the haze (2BE4 C47, 4C09), 3.0 m, 2/s + 3, 2 s: black, navy, blue, HDR 2 (8007B8CFBA2F r1, peak 2.00; game hex #000000 #0054BC #0075FF #008DFF #00A1FF). */
	static final float[] SIG_GRASP_HAZE = {0, 0, 0, 0, .25f, 0, .089f, .502f, .5f, 0, .178f, 1, .75f, 0, .266f, 1, 1, 0, .356f, 1};
	static final float[] SIG_GRASP_HAZE_N = {0, 0, 0, 0, .25f, 0, .045f, .251f, .5f, 0, .089f, .502f, .75f, 0, .133f, .749f, 1, 0, .178f, 1};
	/** an absorbed shot's flare 208CC (2C84 C11, ~1.5 m, 0.14 s, M): navy, blue, periwinkle, white (800AA319228B r1, peak 1.00; game hex #00175C #0037B1 #004AE3 #8293FF #FFFFFF). */
	static final float[] SIG_GRASP_ABSORB = {0, 0, 0, 0, .25f, 0, .038f, .438f, .5f, 0, .068f, .769f, .75f, .222f, .291f, 1, 1, 1, 1, 1};
	/** the absorb flare's enemy twin (2C84 C17): red, orange-red, white (8009DF124B7E r1, peak 1.00; game hex #BA0000 #D13000 #E54500 #F75300 #FFFFFF). */
	static final float[] SIG_GRASP_ABSORB_ENEMY = {0, 0, 0, 0, .25f, .637f, .029f, 0, .5f, .783f, .059f, 0, .75f, .927f, .087f, 0, 1, 1, 1, 1};
	/** an absorbed shot's 4-frame ring 1CB5E on mesh 310C (2C84 C13): black, slate blue, sky blue, white, HDR 3 (800DED866EC6 r1, peak 3.00; game hex #000000 #347297 #4A9DCF #5ABDF7 #FFFFFF). */
	static final float[] SIG_GRASP_ABSORB_RING = {0, 0, 0, 0, .25f, .034f, .169f, .31f, .5f, .068f, .339f, .621f, .75f, .101f, .506f, .927f, 1, 1, 1, 1};
	static final float[] SIG_GRASP_ABSORB_RING_N = {0, 0, 0, 0, .25f, .011f, .056f, .103f, .5f, .023f, .113f, .207f, .75f, .034f, .169f, .309f, 1, .999f, .999f, 1};
	/** the absorb ring's enemy twin (2C84 C15): black, maroon, red, white, HDR 3 (800C9E9E72F0 r1, peak 3.00; game hex #000000 #971E1E #CF2C2C #F73737 #FFFFFF). */
	static final float[] SIG_GRASP_ABSORB_RING_ENEMY = {0, 0, 0, 0, .25f, .31f, .013f, .013f, .5f, .621f, .025f, .025f, .75f, .927f, .038f, .038f, 1, 1, 1, 1};
	static final float[] SIG_GRASP_ABSORB_RING_ENEMY_N = {0, 0, 0, 0, .25f, .103f, .004f, .004f, .5f, .207f, .008f, .008f, .75f, .309f, .013f, .013f, 1, 1, .999f, .999f};
	/** the overhealth given: a material effect on his body (2D3E C8, 2.17 s; 1P 3182): plum, violet, blue, sky blue, HDR 1.9 (800E8CF18011 r1, peak 1.93; game hex #7C0095 #6B38E1 #564FFF #3760FF #49F0FF). */
	static final float[] SIG_OVERHEALTH = {0, 0, 0, 0, .25f, .148f, .039f, .75f, .5f, .092f, .078f, 1, .75f, .038f, .116f, 1, 1, .066f, .868f, 1};
	static final float[] SIG_OVERHEALTH_N = {0, 0, 0, 0, .25f, .076f, .02f, .388f, .5f, .048f, .04f, .62f, .75f, .02f, .06f, .849f, 1, .034f, .449f, .997f};
	/** the overhealth material's other row (M: enemy): rust, orange, gold, HDR 2 (800E8CF18011 r2, peak 2.00; game hex #F91C00 #FF6000 #FF8603 #FF9D0D #FFFF6F). */
	static final float[] SIG_OVERHEALTH_ENEMY = {0, 0, 0, 0, .25f, 1, .116f, 0, .5f, 1, .237f, .001f, .75f, 1, .337f, .004f, 1, 1, 1, .16f};
	static final float[] SIG_OVERHEALTH_ENEMY_N = {0, 0, 0, 0, .25f, .531f, .058f, 0, .5f, .664f, .119f, .001f, .75f, .827f, .169f, .002f, 1, 1, .708f, .08f};
	/** the overhealth's rising streaks (2D3E C11, 2B0E / 2B0F / 2B10), 0.1-0.2 m, 300/s for 0.27 s, 0.5 s, 2 m above hp 0004: black, sky blue, black (a bell) (8001D91E5A78 r0, peak 0.91; game hex #000000 #57ACE2 #51A1D3 #39759B #000000). */
	static final float[] SIG_OVERHEALTH_STREAK = {0, 0, 0, 0, .25f, .095f, .412f, .757f, .5f, .082f, .355f, .652f, .75f, .041f, .179f, .329f, 1, 0, 0, 0};
	/** the overhealth's sphere DBF (2D3E C13 at 0.73 s), 2.0 m, 0.4-0.52 s: a constant tint (0.13, 0.8, 2.32) (80076E520195 r3, peak 2.32; game hex #66E7FF #66E7FF #66E7FF #66E7FF #66E7FF). */
	static final float[] SIG_OVERHEALTH_SHELL = {0, 0, 0, 0, .25f, .133f, .799f, 1, .5f, .133f, .799f, 1, .75f, .133f, .799f, 1, 1, .133f, .799f, 1};
	static final float[] SIG_OVERHEALTH_SHELL_N = {0, 0, 0, 0, .25f, .057f, .344f, 1, .5f, .057f, .344f, 1, .75f, .057f, .344f, 1, 1, .057f, .344f, 1};
	/** the overhealth sphere's enemy twin (2D3E C16): a constant tint (2.32, 0.8, 0) (800648D389EC r3, peak 2.32; game hex #FFE700 #FFE700 #FFE700 #FFE700 #FFE700). */
	static final float[] SIG_OVERHEALTH_SHELL_ENEMY = {0, 0, 0, 0, .25f, 1, .799f, 0, .5f, 1, .799f, 0, .75f, 1, .799f, 0, 1, 1, .799f, 0};
	static final float[] SIG_OVERHEALTH_SHELL_ENEMY_N = {0, 0, 0, 0, .25f, 1, .344f, 0, .5f, 1, .344f, 0, .75f, 1, .344f, 0, 1, 1, .344f, 0};
	/** the gather's sphere DBF at hp 01B6 (2C64 C26 at 0.37 s), 1.58 m, 0.45 s: navy, black, slate, white, HDR 3 (80061097C253 r3, peak 3.00; game hex #263C6E #0B1938 #0F2045 #4E67B0 #FFFFFF). */
	static final float[] SIG_ROCK_GATHER = {0, 0, 0, 0, .25f, .004f, .01f, .039f, .5f, .005f, .014f, .06f, .75f, .076f, .135f, .434f, 1, 1, 1, 1};
	static final float[] SIG_ROCK_GATHER_N = {0, 0, 0, 0, .25f, .001f, .003f, .013f, .5f, .002f, .005f, .02f, .75f, .025f, .045f, .145f, 1, 1, 1, 1};
	/** the gather sphere's twin (2C64 C28, 3.16 m; M: enemy): navy, maroon, red, white, HDR 3 (800E8BAB84E3 r3, peak 3.00; game hex #263C6E #30101E #441015 #CD2525 #FFFFFF). */
	static final float[] SIG_ROCK_GATHER_ENEMY = {0, 0, 0, 0, .25f, .03f, .005f, .013f, .5f, .057f, .005f, .007f, .75f, .613f, .019f, .019f, 1, 1, 1, 1};
	static final float[] SIG_ROCK_GATHER_ENEMY_N = {0, 0, 0, 0, .25f, .01f, .002f, .004f, .5f, .019f, .002f, .002f, .75f, .205f, .006f, .006f, 1, 1, 1, 1};
	/** the gather's dish C943 (2C64 C36 at 0.5 s), 1.65 m, 0.37 s: black, navy, blue, peak 0.44 (8003E1C392CF r4, peak 0.44; game hex #000000 #030B1F #091531 #293865 #5069B2). */
	static final float[] SIG_ROCK_GATHER_DISC = {0, 0, 0, 0, .25f, .001f, .003f, .014f, .5f, .003f, .008f, .031f, .75f, .022f, .04f, .13f, 1, .081f, .14f, .444f};
	/** the gather's flat flash 1BAC (2C64 C39 at 0.9 s), 3.5 m, 0.15 s: black, navy, blue (8007EB29D5B9 r4, peak 1.00; game hex #000000 #314589 #4760BC #5674E0 #6385FF). */
	static final float[] SIG_ROCK_GATHER_FLASH = {0, 0, 0, 0, .25f, .031f, .059f, .251f, .5f, .063f, .118f, .502f, .75f, .093f, .176f, .749f, 1, .125f, .234f, 1};
	/** the rock's ribbon 1298 (2D99 C10), up to 1.0 m wide, 30/s, 0.5 s: black, blue, black (a bell) (800A354999D6 r3, peak 0.98; game hex #000000 #002CF1 #0023C9 #001793 #000000). */
	static final float[] SIG_ROCK_TRAIL = {0, 0, 0, 0, .25f, 0, .025f, .875f, .5f, 0, .017f, .582f, .75f, 0, .008f, .293f, 1, 0, 0, 0};
	/** the rock ribbon's enemy twin (2D99 C6): black, red, black (800E39471D4E r3, peak 0.98; game hex #000000 #F11B00 #C91400 #930C00 #000000). */
	static final float[] SIG_ROCK_TRAIL_ENEMY = {0, 0, 0, 0, .25f, .875f, .011f, 0, .5f, .582f, .007f, 0, .75f, .293f, .004f, 0, 1, 0, 0, 0};
	/** the impact flash 4DB x 1FC54 (2C07 C66), 0.25-0.35 s: black, navy, blue, sky blue, HDR 5 (800757E56C57 r1, peak 5.00; game hex #000000 #014186 #006FCD #008CFC #FFFFFF). */
	static final float[] SIG_ROCK_IMPACT_FLASH = {0, 0, 0, 0, .25f, 0, .053f, .238f, .5f, 0, .159f, .609f, .75f, 0, .263f, .975f, 1, 1, 1, 1};
	static final float[] SIG_ROCK_IMPACT_FLASH_N = {0, 0, 0, 0, .25f, 0, .011f, .048f, .5f, 0, .032f, .122f, .75f, 0, .053f, .195f, 1, .382f, .687f, 1};
	/** the impact flash's enemy twin (2C07 C44): black, maroon, red, pink, HDR 5 (800C7365557A r1, peak 5.00; game hex #000000 #860000 #CD0000 #FC0000 #FFFFFF). */
	static final float[] SIG_ROCK_IMPACT_FLASH_ENEMY = {0, 0, 0, 0, .25f, .238f, 0, 0, .5f, .609f, 0, 0, .75f, .975f, 0, 0, 1, 1, 1, 1};
	static final float[] SIG_ROCK_IMPACT_FLASH_ENEMY_N = {0, 0, 0, 0, .25f, .048f, 0, 0, .5f, .122f, 0, 0, .75f, .195f, 0, 0, 1, 1, .382f, .382f};
	/** the impact glows 208CC (2C07 C52), 2-3, 0.2-0.32 s: black, navy, blue, white, HDR 3 (800A13B9D7C9 r0, peak 3.00; game hex #000000 #005AAF #007CEE #A8C2FF #FFFFFF). */
	static final float[] SIG_ROCK_IMPACT_GLOW = {0, 0, 0, 0, .25f, 0, .102f, .426f, .5f, 0, .203f, .853f, .75f, .394f, .541f, 1, 1, 1, 1, 1};
	static final float[] SIG_ROCK_IMPACT_GLOW_N = {0, 0, 0, 0, .25f, 0, .034f, .142f, .5f, 0, .068f, .284f, .75f, .131f, .18f, .337f, 1, 1, 1, 1};
	/** the impact glows' enemy twin (2C07 C56): black, red, pink, white (8006794CA30F r0, peak 1.00; game hex #000000 #AF0000 #EE0000 #FFA8A8 #FFFFFF). */
	static final float[] SIG_ROCK_IMPACT_GLOW_ENEMY = {0, 0, 0, 0, .25f, .426f, 0, 0, .5f, .853f, 0, 0, .75f, 1, .39f, .39f, 1, 1, 1, 1};
	/** the impact's quick puffs 14F9 (2C07 C48 / C50), 2-5 at 3.3-5.5 m/s, 0.08-0.15 s: black, navy, blue-grey, white, HDR 3 (800B1D47DEB4 r1, peak 3.00; game hex #000000 #0040B9 #0059FB #BEC7FF #FFFFFF). */
	static final float[] SIG_ROCK_IMPACT_DUST = {0, 0, 0, 0, .25f, 0, .051f, .483f, .5f, 0, .101f, .966f, .75f, .517f, .573f, 1, 1, 1, 1, 1};
	static final float[] SIG_ROCK_IMPACT_DUST_N = {0, 0, 0, 0, .25f, 0, .017f, .161f, .5f, 0, .034f, .322f, .75f, .172f, .191f, .351f, 1, 1, 1, 1};
	/** the puffs' enemy twin (2C07 C54 / C58): black, maroon, red-grey, white, HDR 3 (80055F351B80 r1, peak 3.00; game hex #000000 #B30000 #F30000 #FFB6B6 #FFFFFF). */
	static final float[] SIG_ROCK_IMPACT_DUST_ENEMY = {0, 0, 0, 0, .25f, .449f, 0, 0, .5f, .897f, 0, 0, .75f, 1, .469f, .469f, 1, 1, 1, 1};
	static final float[] SIG_ROCK_IMPACT_DUST_ENEMY_N = {0, 0, 0, 0, .25f, .15f, 0, 0, .5f, .299f, 0, 0, .75f, .351f, .157f, .157f, 1, 1, 1, 1};
	/** the impact's 10-puff bursts 14F9 (2C07 C46 / C60): black, rust, brown, white, HDR 3 (8001645796B4 r1, peak 3.00; game hex #000000 #B94B00 #FB6900 #FFCBBE #FFFFFF). */
	static final float[] SIG_ROCK_IMPACT_EMBER = {0, 0, 0, 0, .25f, .483f, .071f, 0, .5f, .966f, .142f, 0, .75f, 1, .595f, .517f, 1, 1, 1, 1};
	static final float[] SIG_ROCK_IMPACT_EMBER_N = {0, 0, 0, 0, .25f, .161f, .024f, 0, .5f, .322f, .047f, 0, .75f, .351f, .199f, .172f, 1, 1, 1, 1};
	/** the impact's ground decal 2048A (2C07 C67), 3.9 s: black, slate, steel blue, periwinkle (80088F28F543 r1, peak 1.00; game hex #000000 #1B1D28 #292C3A #343D6F #91A4EC). */
	static final float[] SIG_ROCK_SCORCH = {0, 0, 0, 0, .25f, .011f, .013f, .021f, .5f, .022f, .025f, .042f, .75f, .034f, .047f, .158f, 1, .283f, .371f, .838f};
	/** the decal's enemy twin (2C07 C72): black, maroon, red, pink (800C74C530F1 r1, peak 1.00; game hex #000000 #620D0D #881717 #A31D1D #FFADAD). */
	static final float[] SIG_ROCK_SCORCH_ENEMY = {0, 0, 0, 0, .25f, .122f, .004f, .004f, .5f, .244f, .008f, .008f, .75f, .364f, .012f, .012f, 1, 1, .418f, .418f};
	/** the impact's dust 245A / 5429 (2C07 C69), 5 x 0.7-1.7 m stretched x2, 2 s: taupe to dark brown, peak 0.25, falls (80012E849335 r1, peak 0.25, falls; game hex #8A786E #7E6C63 #6F5E55 #5F4E45 #49372E). */
	static final float[] SIG_ROCK_SMOKE = {0, 0, 0, 0, .25f, .207f, .15f, .124f, .5f, .16f, .113f, .091f, .75f, .114f, .076f, .06f, 1, .067f, .038f, .027f};
	/** the impact's core sphere DBF (2C07 C62 / C64, 16E9D), 0.26 s: black, purple, violet, HDR 17 (800680E830A2 r0, peak 17.45; game hex #000702 #FF07FF #FF07FF #FF07FF #FF07FF). */
	static final float[] SIG_ROCK_CORE = {0, 0, 0, 0, .25f, 1, .002f, 1, .5f, 1, .002f, 1, .75f, 1, .002f, 1, 1, 1, .002f, 1};
	static final float[] SIG_ROCK_CORE_N = {0, 0, 0, 0, .25f, .139f, 0, .26f, .5f, .169f, 0, .519f, .75f, .18f, 0, .775f, 1, .184f, 0, 1};
	/** the aim reticle's cylinder DE0 (2C9E C4, 18A1), held 5 s: black, slate, periwinkle (800F7C5921B9 r1, peak 1.00; game hex #000000 #545889 #757BBC #8C93E0 #A0A8FF). */
	static final float[] SIG_FLUX_RETICLE = {0, 0, 0, 0, .25f, .088f, .098f, .251f, .5f, .176f, .197f, .502f, .75f, .263f, .293f, .749f, 1, .351f, .391f, 1};
	/** the aim reticle's ground decal 202A6 x 9489 x 202B3 (2C9E C2): black, indigo, periwinkle, HDR 10 (800ED9F97F22 r5, peak 9.99; game hex #000000 #B0B0FF #FFFFFF #FFFFFF #FFFFFF). */
	static final float[] SIG_FLUX_RETICLE_DECAL = {0, 0, 0, 0, .25f, .433f, .433f, 1, .5f, 1, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 1};
	static final float[] SIG_FLUX_RETICLE_DECAL_N = {0, 0, 0, 0, .25f, .043f, .043f, .138f, .5f, .117f, .117f, .372f, .75f, .215f, .215f, .684f, 1, .314f, .314f, 1};
	/** his glow through the ult on hp 0076 (2C68 C17, 4C09 x 4DB), 0.66-1.33 m, 5/s, 0.65-1 s: black, indigo, slate, white, HDR 3 (8005E6165461 r2, peak 3.00; game hex #000000 #9696FF #C3C3FF #FFFFFF #FFFFFF). */
	static final float[] SIG_FLUX_BODY = {0, 0, 0, 0, .25f, .304f, .304f, 1, .5f, .543f, .543f, 1, .75f, 1, 1, 1, 1, 1, 1, 1};
	static final float[] SIG_FLUX_BODY_N = {0, 0, 0, 0, .25f, .101f, .101f, .334f, .5f, .181f, .181f, .336f, .75f, .499f, .499f, .642f, 1, 1, 1, 1};
	/** his ult glow's enemy twin (2C68 C24): black, maroon, rose, white, HDR 3 (800A2FB218C2 r2, peak 3.00; game hex #000000 #9E1414 #D92020 #FF7272 #FFFFFF). */
	static final float[] SIG_FLUX_BODY_ENEMY = {0, 0, 0, 0, .25f, .343f, .007f, .007f, .5f, .693f, .014f, .014f, .75f, 1, .168f, .168f, 1, 1, 1, 1};
	static final float[] SIG_FLUX_BODY_ENEMY_N = {0, 0, 0, 0, .25f, .114f, .002f, .002f, .5f, .231f, .005f, .005f, .75f, .642f, .056f, .056f, 1, 1, 1, 1};
	/** his ult halo (2C68 C19, 2006C), 2.0 m, 5/s, 1 s: black, indigo, slate, white, HDR 7 (8008C74E1092 r3, peak 7.00; game hex #000000 #9696FF #C2C2FF #FFFFFF #FFFFFF). */
	static final float[] SIG_FLUX_BODY_HALO = {0, 0, 0, 0, .25f, .304f, .304f, 1, .5f, .537f, .537f, 1, .75f, 1, 1, 1, 1, 1, 1, 1};
	static final float[] SIG_FLUX_BODY_HALO_N = {0, 0, 0, 0, .25f, .043f, .043f, .143f, .5f, .077f, .077f, .143f, .75f, .171f, .171f, .212f, 1, 1, 1, 1};
	/** the halo's enemy twin (2C68 C26): black, maroon, white, HDR 7 (800EEE9B4DC6 r3, peak 7.00; game hex #000000 #9E2323 #D73333 #FF6161 #FFFFFF). */
	static final float[] SIG_FLUX_BODY_HALO_ENEMY = {0, 0, 0, 0, .25f, .341f, .016f, .016f, .5f, .682f, .033f, .033f, .75f, 1, .119f, .119f, 1, 1, 1, 1};
	static final float[] SIG_FLUX_BODY_HALO_ENEMY_N = {0, 0, 0, 0, .25f, .049f, .002f, .002f, .5f, .097f, .005f, .005f, .75f, .182f, .017f, .017f, 1, 1, 1, 1};
	/** the cast's column, cylinder DE0 (2C6B C16, 1C7A x 200E4) at the aimed point, 3 s: black, navy, blue, lavender, HDR 2 (80005725A714 r2, peak 2.00; game hex #000000 #0000CF #5D5DFF #9292FF #FFFFFF). */
	static final float[] SIG_FLUX_COLUMN = {0, 0, 0, 0, .25f, 0, 0, .622f, .5f, .109f, .109f, 1, .75f, .286f, .286f, 1, 1, 1, 1, 1};
	static final float[] SIG_FLUX_COLUMN_N = {0, 0, 0, 0, .25f, 0, 0, .311f, .5f, .054f, .054f, .641f, .75f, .143f, .143f, .899f, 1, .515f, .515f, 1};
	/** the column's enemy twin (2C6B C25): black, maroon, red, orange, HDR 2 (800C7484E7B7 r2, peak 2.00; game hex #000000 #CC0000 #FF3232 #FF6240 #FF9F00). */
	static final float[] SIG_FLUX_COLUMN_ENEMY = {0, 0, 0, 0, .25f, .602f, 0, 0, .5f, 1, .032f, .032f, .75f, 1, .121f, .052f, 1, 1, .349f, 0};
	static final float[] SIG_FLUX_COLUMN_ENEMY_N = {0, 0, 0, 0, .25f, .301f, 0, 0, .5f, .639f, .016f, .016f, .75f, .899f, .061f, .026f, 1, 1, .175f, 0};
	/** the cast's outer cylinder DE0 (2C6B C20, 200E4 x 18A1), 6 s: black, navy, slate blue, white, HDR 2 (800DA6F46FA0 r1, peak 2.00; game hex #000000 #0000B1 #5757DB #9C9CFF #FFFFFF). */
	static final float[] SIG_FLUX_COLUMN_OUTER = {0, 0, 0, 0, .25f, 0, 0, .437f, .5f, .095f, .095f, .705f, .75f, .331f, .331f, 1, 1, 1, 1, 1};
	static final float[] SIG_FLUX_COLUMN_OUTER_N = {0, 0, 0, 0, .25f, 0, 0, .219f, .5f, .047f, .047f, .353f, .75f, .165f, .165f, .547f, 1, .73f, .73f, 1};
	/** the outer cylinder's enemy twin (2C6B C27): black, maroon, red, cream, HDR 2 (80015804DA3A r1, peak 2.00; game hex #000000 #B10000 #DB3120 #FF5B3F #FFFFFF). */
	static final float[] SIG_FLUX_COLUMN_OUTER_ENEMY = {0, 0, 0, 0, .25f, .437f, 0, 0, .5f, .705f, .031f, .014f, .75f, 1, .106f, .049f, 1, 1, 1, 1};
	static final float[] SIG_FLUX_COLUMN_OUTER_ENEMY_N = {0, 0, 0, 0, .25f, .219f, 0, 0, .5f, .353f, .015f, .007f, .75f, .543f, .053f, .025f, 1, 1, .847f, .73f};
	/** the cast's ground decal 202A6 x 202B3 x 9489 (2C6B C18): black, indigo, periwinkle, HDR 3 (800BFFEB4E9A r5, peak 3.00; game hex #000000 #6F6FBC #A6A6FF #D4D4FF #F8F8FF). */
	static final float[] SIG_FLUX_DECAL = {0, 0, 0, 0, .25f, .158f, .158f, .502f, .5f, .382f, .382f, 1, .75f, .66f, .66f, 1, 1, .942f, .942f, 1};
	static final float[] SIG_FLUX_DECAL_N = {0, 0, 0, 0, .25f, .053f, .053f, .167f, .5f, .128f, .128f, .406f, .75f, .22f, .22f, .701f, 1, .314f, .314f, 1};
	/** the decal's enemy twin (2C6B C23): black, maroon, red, HDR 3 (8004F28C37F3 r5, peak 3.00; game hex #000000 #BC2E2E #FF4949 #FF5F5F #FF7070). */
	static final float[] SIG_FLUX_DECAL_ENEMY = {0, 0, 0, 0, .25f, .502f, .027f, .027f, .5f, 1, .066f, .066f, .75f, 1, .115f, .115f, 1, 1, .163f, .163f};
	static final float[] SIG_FLUX_DECAL_ENEMY_N = {0, 0, 0, 0, .25f, .167f, .009f, .009f, .5f, .406f, .022f, .022f, .75f, .701f, .038f, .038f, 1, 1, .054f, .054f};
	/** the cast's rising sparks 1F35 (2C6B C22), 0.09-0.25 m, 64/s + 32 at 10-12 m/s up, 0.17-0.43 s: navy, slate, periwinkle (800BB9FAFB46 r1, peak 1.00; game hex #000C3F #636C92 #8A94C0 #A5B1E2 #BCC9FF). */
	static final float[] SIG_FLUX_SPARK = {0, 0, 0, 0, .25f, .126f, .149f, .288f, .5f, .252f, .295f, .526f, .75f, .377f, .438f, .761f, 1, .502f, .584f, 1};
	/** the lift's rising cylinders DE0 (2C6A C30), 20 m/s up, 0.56 s: black, navy, slate, white, HDR 3 (800AB39788A6 r1, peak 3.00; game hex #000000 #1A1ABA #6767CE #D3D3FF #FFFFFF). */
	static final float[] SIG_FLUX_PULL = {0, 0, 0, 0, .25f, .01f, .01f, .49f, .5f, .136f, .136f, .618f, .75f, .65f, .65f, 1, 1, 1, 1, 1};
	static final float[] SIG_FLUX_PULL_N = {0, 0, 0, 0, .25f, .003f, .003f, .164f, .5f, .045f, .045f, .206f, .75f, .217f, .217f, .379f, 1, 1, 1, 1};
	/** the rising cylinders' enemy twin (2C6A C43, 10 m/s): black, maroon, red, cream, HDR 1.2 (800B5B8E8A78 r1, peak 1.20; game hex #000000 #AC0000 #CC2D1D #E54E35 #FFFFF1). */
	static final float[] SIG_FLUX_PULL_ENEMY = {0, 0, 0, 0, .25f, .414f, 0, 0, .5f, .601f, .026f, .012f, .75f, .78f, .076f, .035f, 1, 1, 1, .876f};
	static final float[] SIG_FLUX_PULL_ENEMY_N = {0, 0, 0, 0, .25f, .345f, 0, 0, .5f, .501f, .022f, .01f, .75f, .65f, .063f, .03f, 1, 1, .847f, .731f};
	/** the lift's fast sparks 1F35 (2C6A C33 / C35), 0.1-0.25 m at 40 m/s up, 32/s + 9: lavender, periwinkle, navy, HDR 7, falls (80060A33F6DF r2, peak 7.00, falls; game hex #FFFFFF #FFFFFF #D6D6FF #9D9DFF #0000FF). */
	static final float[] SIG_FLUX_PULL_SPARK = {0, 0, 0, 0, .25f, 1, 1, 1, .5f, .676f, .676f, 1, .75f, .335f, .335f, 1, 1, 0, 0, 1};
	static final float[] SIG_FLUX_PULL_SPARK_N = {0, 0, 0, 0, .25f, .219f, .219f, .785f, .5f, .097f, .097f, .569f, .75f, .048f, .048f, .358f, 1, 0, 0, .143f};
	/** the fast sparks' enemy twin (2C6A C47): salmon, red, dark red, HDR 7, falls (800B29BCFAB8 r2, peak 7.00, falls; game hex #FFFFFF #FFE3E3 #FF9D9D #FF7272 #FF0000). */
	static final float[] SIG_FLUX_PULL_SPARK_ENEMY = {0, 0, 0, 0, .25f, 1, .768f, .768f, .5f, 1, .339f, .339f, .75f, 1, .168f, .168f, 1, 1, 0, 0};
	static final float[] SIG_FLUX_PULL_SPARK_ENEMY_N = {0, 0, 0, 0, .25f, .785f, .11f, .11f, .5f, .569f, .048f, .048f, .75f, .358f, .024f, .024f, 1, .143f, 0, 0};
	/** a target's lift start, disc 11C1 (2D88 C13, 1FDB2) on hp 0001, 3 x 3.0 m, 0.22-0.27 s: white, slate, navy, HDR 5, falls (8009504C5EE5 r4, peak 5.00, falls; game hex #FFFFFF #E8F0FF #A9B1FF #7781FF #2A42FF). */
	static final float[] SIG_LIFT_RING = {0, 0, 0, 0, .25f, .806f, .868f, 1, .5f, .398f, .442f, 1, .75f, .185f, .221f, 1, 1, .023f, .054f, 1};
	static final float[] SIG_LIFT_RING_N = {0, 0, 0, 0, .25f, .161f, .174f, .327f, .5f, .08f, .088f, .245f, .75f, .037f, .044f, .212f, 1, .005f, .011f, .2f};
	/** the lift-start disc's enemy twin (2D88 C17): white, rose, maroon, HDR 5, falls (800DB7D23312 r4, peak 5.00, falls; game hex #FFFFFF #FFAEAE #FF8080 #FF5656 #FF0000). */
	static final float[] SIG_LIFT_RING_ENEMY = {0, 0, 0, 0, .25f, 1, .421f, .421f, .5f, 1, .216f, .216f, .75f, 1, .094f, .094f, 1, 1, 0, 0};
	static final float[] SIG_LIFT_RING_ENEMY_N = {0, 0, 0, 0, .25f, .315f, .084f, .084f, .5f, .245f, .043f, .043f, .75f, .212f, .019f, .019f, 1, .2f, 0, 0};
	/** a lifted target's ribbons 1298 on hp 0001 (2BEB C10 / 2BE7 C26), 0.34 m, 20/s, 0.23 s: white, periwinkle, HDR 5, falls (800A423F26CA r2, peak 5.00, falls; game hex #FFFFFF #FFFFFF #FFFFFF #FFFFFF #FFFFFF). */
	static final float[] SIG_LIFT_TRAIL = {0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, 1, 1, .75f, 1, 1, 1, 1, 1, 1, 1};
	static final float[] SIG_LIFT_TRAIL_N = {0, 0, 0, 0, .25f, .809f, .859f, 1, .5f, .618f, .718f, 1, .75f, .429f, .58f, 1, 1, .238f, .439f, 1};
	/** the ribbons' enemy twin (2BEB C12): white, pink, red, HDR 5, falls (800F0A9DF1BD r2, peak 5.00, falls; game hex #FFFFFF #FFFFFF #FFFFFF #FFFFFF #FF0000). */
	static final float[] SIG_LIFT_TRAIL_ENEMY = {0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, 1, 1, .75f, 1, 1, 1, 1, 1, 0, 0};
	static final float[] SIG_LIFT_TRAIL_ENEMY_N = {0, 0, 0, 0, .25f, 1, .749f, .749f, .5f, 1, .498f, .498f, .75f, 1, .251f, .251f, 1, 1, 0, 0};
	/** a lifted target's wide ribbons (2BEB C14 / 2BE7 C24), 0.49 m, 20/s, 0.29 s: periwinkle to navy-black, HDR 5, falls (800BBEC44BA8 r2, peak 5.00, falls; game hex #FFFFFF #E4FFFF #A1D6FF #759FEC #001B4A). */
	static final float[] SIG_LIFT_TRAIL_DARK = {0, 0, 0, 0, .25f, .773f, 1, 1, .5f, .356f, .673f, 1, .75f, .179f, .345f, .838f, 1, 0, .011f, .068f};
	static final float[] SIG_LIFT_TRAIL_DARK_N = {0, 0, 0, 0, .25f, .155f, .287f, .659f, .5f, .071f, .135f, .319f, .75f, .036f, .069f, .168f, 1, 0, .002f, .014f};
	/** the wide ribbons' enemy twin (2BEB C16): red to dark red, HDR 2, falls (80088EDADC9B r2, peak 2.00, falls; game hex #FF5656 #FF4848 #FF3636 #FF1F1F #FF0000). */
	static final float[] SIG_LIFT_TRAIL_DARK_ENEMY = {0, 0, 0, 0, .25f, 1, .065f, .065f, .5f, 1, .037f, .037f, .75f, 1, .013f, .013f, 1, 1, 0, 0};
	static final float[] SIG_LIFT_TRAIL_DARK_ENEMY_N = {0, 0, 0, 0, .25f, .924f, .032f, .032f, .5f, .749f, .019f, .019f, .75f, .576f, .007f, .007f, 1, .5f, 0, 0};
	/** a lifted target's glow 174D8 (2BE7 C23), 2-3.9 m, 4/s, 0.5 s: periwinkle to blue (8009BD54F2C4 r1, peak 1.00, falls; game hex #94A7FF #8797FF #7785FF #656EFF #4D50FF). */
	static final float[] SIG_LIFTED_GLOW = {0, 0, 0, 0, .25f, .24f, .31f, 1, .5f, .185f, .233f, 1, .75f, .13f, .157f, 1, 1, .074f, .08f, 1};
	/** the lifted glow's enemy twin (2BE7 C29): salmon to red (800FA3744390 r1, peak 1.00, falls; game hex #FF5C5C #FF5050 #FF4141 #FF2E2E #FF0000). */
	static final float[] SIG_LIFTED_GLOW_ENEMY = {0, 0, 0, 0, .25f, 1, .08f, .08f, .5f, 1, .053f, .053f, .75f, 1, .027f, .027f, 1, 1, 0, 0};
	/** a lifted target's disc 11C1 (2BE7 C21, 1FDB2), 2.0 m, 1.5-2/s + 3, 1-1.5 s: white, lavender, periwinkle, falls (800234B988C2 r4, peak 1.00, falls; game hex #FFFFFF #E9EBFF #D0D4FF #B2BAFF #8C99FF). */
	static final float[] SIG_LIFTED_RING = {0, 0, 0, 0, .25f, .815f, .829f, 1, .5f, .629f, .658f, 1, .75f, .447f, .489f, 1, 1, .262f, .318f, 1};
	/** the lifted disc's enemy twin (2BE7 C35, 2.5 m): white, pink, red, falls (80063250F3F9 r4, peak 1.00, falls; game hex #FFFFFF #FFE0E0 #FFBBBB #FF8989 #FF0000). */
	static final float[] SIG_LIFTED_RING_ENEMY = {0, 0, 0, 0, .25f, 1, .749f, .749f, .5f, 1, .498f, .498f, .75f, 1, .251f, .251f, 1, 1, 0, 0};
	/** a grounded target lifted (2BEA C21, A7B6), 0.49 m, 0.17 s: blue, azure, sky blue, ice (8003361A9BF3 r0, peak 1.00; game hex #2447DE #0F79DA #529FDF #81BEE9 #B5F5FF). */
	static final float[] SIG_LIFTOFF_GLOW = {0, 0, 0, 0, .25f, .005f, .192f, .699f, .5f, .084f, .346f, .736f, .75f, .219f, .514f, .816f, 1, .462f, .913f, 1};
	/** the lift-off glow's enemy twin (2BEA C23): red, salmon, pink (8006DF9C8967 r0, peak 1.00; game hex #DE2424 #DA0F0F #DF5252 #E98181 #FFB5B5). */
	static final float[] SIG_LIFTOFF_GLOW_ENEMY = {0, 0, 0, 0, .25f, .699f, .005f, .005f, .5f, .736f, .084f, .084f, .75f, .816f, .219f, .219f, 1, 1, .462f, .462f};
	/** the lift-off's debris 245A / 5429 (2BEA C29), 2-2.5 m stretched x6 at 15 m/s, 1.25-1.75 s: violet, periwinkle, azure (80050C80D9CA r1, peak 1.00; game hex #9A17FF #8766FF #528BFF #26A6FF #00BDFF). */
	static final float[] SIG_LIFTOFF_DEBRIS = {0, 0, 0, 0, .25f, .242f, .134f, 1, .5f, .084f, .259f, 1, .75f, .02f, .383f, 1, 1, 0, .508f, 1};
	/** the lift-off's dust 14F9 (2BEA C27), 5 then 3 x 1.95-2.15 m at 2-5 m/s, 0.39-0.54 s: white, light grey, grey, dark brown, falls (80007EA281F7 r5, peak 2.00, falls; game hex #FFFFFF #FFFFFF #FFFFFF #C5C1BD #4D3B1E). */
	static final float[] SIG_LIFTOFF_DUST = {0, 0, 0, 0, .25f, 1, 1, 1, .5f, 1, 1, 1, .75f, .557f, .535f, .511f, 1, .074f, .044f, .013f};
	static final float[] SIG_LIFTOFF_DUST_N = {0, 0, 0, 0, .25f, .758f, .754f, .75f, .5f, .517f, .509f, .501f, .75f, .279f, .267f, .256f, 1, .037f, .022f, .006f};
	/** Levitation on hp 0001 (65BF C5, mesh 963D, 299E1 x 299E8 x A7B6), 0.66-0.78 m stretched x8-10, 3/s, 1.1 s: black, purple, violet, lilac (800293615773 r5, peak 1.00; game hex #000000 #7809BA #A511FD #B88EFF #E9DEFF). */
	static final float[] SIG_LEVITATE = {0, 0, 0, 0, .25f, .187f, .003f, .491f, .5f, .374f, .005f, .982f, .75f, .48f, .271f, 1, 1, .815f, .73f, 1};
	/** Levitation's enemy row (65BF C5 row 6): black, red, pink (800293615773 r6, peak 1.00; game hex #000000 #BA0909 #FD1111 #FF8E8E #FFDEDE). */
	static final float[] SIG_LEVITATE_ENEMY = {0, 0, 0, 0, .25f, .491f, .003f, .003f, .5f, .982f, .005f, .005f, .75f, 1, .271f, .271f, 1, 1, .73f, .73f};
	/** Hyper Strike charged, on hp 0002 (65C7 C15, 4C09), 0.5-0.94 m, 20/s, 0.28-0.41 s: black, navy, slate, ice, HDR 4.2 (800FE64AF7C5 r0, peak 4.15; game hex #000000 #14264E #3E5391 #87ACDF #FFFFFF). */
	static final float[] SIG_HYPER_STRIKE = {0, 0, 0, 0, .25f, .007f, .019f, .077f, .5f, .049f, .087f, .283f, .75f, .244f, .411f, .738f, 1, 1, 1, 1};
	static final float[] SIG_HYPER_STRIKE_N = {0, 0, 0, 0, .25f, .002f, .005f, .018f, .5f, .012f, .021f, .068f, .75f, .059f, .099f, .178f, 1, .523f, .871f, 1};
	/** Hyper Strike's enemy row (65C7 C15 row 1): red to salmon (800FE64AF7C5 r1, peak 1.00; game hex #F40000 #F74343 #FA5E5E #FC7272 #FF8282). */
	static final float[] SIG_HYPER_STRIKE_ENEMY = {0, 0, 0, 0, .25f, .928f, .056f, .056f, .5f, .952f, .112f, .112f, .75f, .976f, .167f, .167f, 1, 1, .223f, .223f};
	/** Hyper Strike's dish sparks C943 (65C7 C11 / C13), 0.13 m, 7/s, 0.75-1 s: black, navy, slate, white, HDR 5 (8002BCF06E60 r4, peak 5.00; game hex #000000 #14264E #3E5391 #B5BFE8 #FFFFFF). */
	static final float[] SIG_HYPER_STRIKE_SPARK = {0, 0, 0, 0, .25f, .007f, .019f, .077f, .5f, .049f, .087f, .283f, .75f, .462f, .518f, .803f, 1, 1, 1, 1};
	static final float[] SIG_HYPER_STRIKE_SPARK_N = {0, 0, 0, 0, .25f, .001f, .004f, .015f, .5f, .01f, .017f, .057f, .75f, .093f, .104f, .161f, 1, 1, 1, 1};
	/** the dish sparks' enemy row (row 5): black, brown, white, HDR 5 (8002BCF06E60 r5, peak 5.00; game hex #2E0000 #4E1A14 #91483E #E8B9B5 #FFFFFF). */
	static final float[] SIG_HYPER_STRIKE_SPARK_ENEMY = {0, 0, 0, 0, .25f, .077f, .01f, .007f, .5f, .283f, .064f, .049f, .75f, .803f, .486f, .462f, 1, 1, 1, 1};
	static final float[] SIG_HYPER_STRIKE_SPARK_ENEMY_N = {0, 0, 0, 0, .25f, .015f, .002f, .001f, .5f, .057f, .013f, .01f, .75f, .161f, .097f, .093f, 1, 1, 1, 1};
}
