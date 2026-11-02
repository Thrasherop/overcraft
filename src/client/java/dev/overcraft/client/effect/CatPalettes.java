package dev.overcraft.client.effect;

/**
 * Jetpack Cat's palettes' stops from the rest of #59 (EffectArt.Palette.CAT_*: the enum's own initializer outgrew the
 * class file's 64 KB, as SojournPalettes says). From her LUT rows (research\jetpack_cat\workx_digest.txt,
 * assets2\workx_digest.txt): each row's six samples at .2 to 1 of the mask, black at 0, linear.
 */
final class CatPalettes {
	private CatPalettes() {
	}

	/** The jetpack's outer flame (7064 C24, 80063381 r7): deep blue to teal. */
	static final float[] CAT_FLAME_TEAL = {0, 0, 0, 0, .2f, 0, .337f, .796f, .36f, .129f, .475f, .804f, .52f, .216f, .62f, .82f, .68f, .306f, .796f, .839f, .84f, .376f, .933f, .863f, 1, .396f, .976f, .871f};
	/** The Catnapper cast's bell (7373 C37, 8007DA82 r7, from black): gold to pale yellow. */
	static final float[] CAT_BELL_GOLD = {0, 0, 0, 0, .2f, 0, 0, 0, .36f, 0, 0, 0, .52f, 0, 0, 0, .68f, .682f, .537f, 0, .84f, .984f, .78f, 0, 1, 1, .98f, .69f};
	/** The cast's and the dive's exhaust rings (7373 C18 / 7163 C73, 80038B2E r4, HDR 4.65): blue to cyan. */
	static final float[] CAT_RING_CYAN = {0, 0, 0, 0, .2f, 0, .063f, .749f, .36f, 0, .478f, .878f, .52f, 0, .745f, 1, .68f, 0, 1, 1, .84f, 0, 1, 1, 1, 0, 1, 1};
	/** The same rings' gold pair (7373 C21 / 7163 C84, 800BD8BA r4, HDR 3.4): yellow to white. */
	static final float[] CAT_RING_GOLD = {0, 0, 0, 0, .2f, 1, .929f, 0, .36f, 1, .937f, 0, .52f, 1, .984f, 0, .68f, 1, 1, 0, .84f, 1, 1, 0, 1, 1, 1, 1};
	/** The head slash's arcs (71B8 C8, 8005B765 r8, HDR 17): cyan to white. */
	static final float[] CAT_SLASH = {0, 0, 0, 0, .2f, 0, 1, 1, .36f, 0, 1, 1, .52f, 0, 1, 1, .68f, 0, 1, 1, .84f, 0, 1, 1, 1, 1, 1, 1};
	/** The same, the enemy row (8005B765 r9): red to white. */
	static final float[] CAT_SLASH_ENEMY = {0, 0, 0, 0, .2f, 1, 0, 0, .36f, 1, 0, 0, .52f, 1, 1, 1, .68f, 1, 1, 1, .84f, 1, 1, 1, 1, 1, 1, 1};
	/** The head slash's domes (71B8 C10, 8007E2E6 r7, HDR 6.2): sky to cyan to white. */
	static final float[] CAT_DOME = {0, 0, 0, 0, .2f, 0, .627f, 1, .36f, 0, .847f, 1, .52f, 0, 1, 1, .68f, 0, 1, 1, .84f, 0, 1, 1, 1, .925f, 1, 1};
	/** The inner dome (71B8 C12, 8000B7F8 r6, HDR 3): deep blue to cyan. */
	static final float[] CAT_DOME_DEEP = {0, 0, 0, 0, .2f, 0, .016f, 1, .36f, 0, .369f, 1, .52f, 0, .588f, 1, .68f, 0, 1, 1, .84f, 0, 1, 1, 1, .914f, 1, 1};
	/** Both domes' enemy rows (8007E2E6 r8 / 8000B7F8 r5): red to white. */
	static final float[] CAT_DOME_ENEMY = {0, 0, 0, 0, .2f, 1, 0, 0, .36f, 1, .047f, 0, .52f, 1, .102f, 0, .68f, 1, .184f, 0, .84f, 1, .282f, 0, 1, 1, 1, 1};
	/** The landing's ground flash (71C2 C19, 800C2FC5 r3): navy to deep blue. */
	static final float[] CAT_NAP_GROUND = {0, 0, 0, 0, .2f, .004f, .012f, .322f, .36f, .004f, .012f, .455f, .52f, .004f, .012f, .592f, .68f, .004f, .016f, .761f, .84f, 0, .016f, .89f, 1, 0, .016f, 1};
	/** The same, the enemy row (800C2FC5 r4): maroon to red. */
	static final float[] CAT_NAP_GROUND_ENEMY = {0, 0, 0, 0, .2f, .322f, .004f, .004f, .36f, .455f, .004f, .004f, .52f, .592f, .004f, .004f, .68f, .761f, .004f, .004f, .84f, .89f, 0, 0, 1, 1, 0, 0};
	/** Transport Shielding's hexes (5513 C3, 800D0BB1 r3): teal to cyan. */
	static final float[] CAT_HEX = {0, 0, 0, 0, .2f, 0, .478f, .584f, .36f, 0, .525f, .643f, .52f, 0, .584f, .718f, .68f, 0, .675f, .827f, .84f, 0, .749f, .918f, 1, 0, .816f, 1};
	/** Claws Out's marks (74D0 C13, 8006D576 r4, from black): teal to pale cyan. */
	static final float[] CAT_CLAWS = {0, 0, 0, 0, .2f, 0, 0, 0, .36f, .188f, .384f, .38f, .52f, .302f, .592f, .58f, .68f, .424f, .812f, .8f, .84f, .514f, .973f, .957f, 1, .737f, 1, 1};
	/** The same, the enemy row (8006D576 r3): dark red to pale pink. */
	static final float[] CAT_CLAWS_ENEMY = {0, 0, 0, 0, .2f, 0, 0, 0, .36f, .384f, .188f, .188f, .52f, .592f, .302f, .302f, .68f, .812f, .424f, .424f, .84f, .973f, .514f, .514f, 1, 1, .737f, .737f};
	/** Claws Out's sparks (74D0 C15, 800D0510 r1, from black): navy to cyan. */
	static final float[] CAT_CLAW_SPARK = {0, 0, 0, 0, .2f, 0, 0, 0, .36f, 0, .184f, .471f, .52f, 0, .298f, .722f, .68f, 0, .416f, .984f, .84f, 0, .745f, 1, 1, 0, 1, 1};
	/** The same, the enemy row (800D0510 r0): red to yellow. */
	static final float[] CAT_CLAW_SPARK_ENEMY = {0, 0, 0, 0, .2f, 1, 0, 0, .36f, 1, .471f, 0, .52f, 1, .722f, 0, .68f, 1, .984f, 0, .84f, .827f, 1, .471f, 1, 1, 1, 0};
	/** The landing hit's sphere (7256 C13, 800C7EF8 r5, HDR 4.8): blue to cyan. */
	static final float[] CAT_SHOCK = {0, 0, 0, 0, .2f, 0, .012f, .671f, .36f, 0, .451f, .816f, .52f, 0, .686f, .984f, .68f, 0, .937f, 1, .84f, 0, 1, 1, 1, 0, 1, 1};
	/** The landing hit's flame burst (7256 C17, 800CFAE7 r3, HDR 22): blue to sky to cyan. */
	static final float[] CAT_SHOCK_FLAME = {0, 0, 0, 0, .2f, .027f, .255f, .659f, .36f, .075f, .439f, 1, .52f, .118f, .608f, 1, .68f, .169f, .8f, 1, .84f, .18f, 1, 1, 1, 0, 1, 1};
	/** The same, the enemy row (800CFAE7 r4): dark red to orange. */
	static final float[] CAT_SHOCK_FLAME_ENEMY = {0, 0, 0, 0, .2f, .659f, .122f, .027f, .36f, 1, .227f, .075f, .52f, 1, .322f, .118f, .68f, 1, .431f, .169f, .84f, 1, .569f, .18f, 1, 1, .816f, 0};
	/** The landing hit's sparks (7256 C21, 8008B3B1 r1, from black): violet to blue to white. */
	static final float[] CAT_SHOCK_SPARK = {0, 0, 0, 0, .2f, 0, 0, 0, .36f, .137f, 0, .529f, .52f, .227f, 0, .804f, .68f, .247f, .298f, 1, .84f, .004f, .553f, .992f, 1, .886f, .925f, 1};
	/** The dive's thin tube trail (7163 C57, 8009F6ED r3, HDR 3.6): white head, cyan. */
	static final float[] CAT_DIVE_CYAN = {0, 0, 0, 0, .2f, 1, 1, 1, .36f, .235f, 1, 1, .52f, 0, 1, 1, .68f, 0, 1, 1, .84f, 0, 1, 1, 1, 0, 1, 1};
	/** The dive's middle tube trail (7163 C61, 800C202B r3, HDR 2.7): cyan to blue. */
	static final float[] CAT_DIVE_BLUE = {0, 0, 0, 0, .2f, 0, 1, 1, .36f, .051f, .141f, 1, .52f, .051f, 0, 1, .68f, .051f, 0, 1, .84f, .051f, 0, 1, 1, .051f, 0, 1};
	/** The dive's wide tube trail (7163 C65, 8000C8DF r3, HDR 2.7): white head, gold. */
	static final float[] CAT_DIVE_GOLD = {0, 0, 0, 0, .2f, 1, 1, 1, .36f, 1, .827f, 0, .52f, 1, .827f, 0, .68f, 1, .827f, 0, .84f, 1, .827f, 0, 1, 1, .827f, 0};
	/** The dive trails' enemy rows (r4): white head, orange. */
	static final float[] CAT_DIVE_ENEMY = {0, 0, 0, 0, .2f, 1, 1, 1, .36f, 1, .62f, 0, .52f, 1, .624f, 0, .68f, 1, .624f, 0, .84f, 1, .624f, 0, 1, 1, .624f, 0};
	/** The dive's strip trails (7163 C103, 8004F990 r3, HDR 2.7): blue to cyan. */
	static final float[] CAT_DIVE_STRIP = {0, 0, 0, 0, .2f, 0, .416f, 1, .36f, 0, .965f, 1, .52f, 0, .969f, 1, .68f, 0, .969f, 1, .84f, 0, .969f, 1, 1, 0, .969f, 1};
	/** The jetpack's and the dive's gold sparks (7064 C44 / 7163 C92, r1): gold to pale yellow. */
	static final float[] CAT_SPARK_GOLD = {0, 0, 0, 0, .2f, 1, .792f, 0, .36f, 1, .808f, .09f, .52f, 1, .831f, .157f, .68f, 1, .871f, .227f, .84f, 1, .906f, .282f, 1, 1, .941f, .325f};
	/** The jetpack's pale sparks (7064 C47 / 7163 C113, 800A522D r1): slate to pale blue. */
	static final float[] CAT_SPARK_PALE = {0, 0, 0, 0, .2f, .369f, .502f, .714f, .36f, .408f, .545f, .741f, .52f, .463f, .604f, .78f, .68f, .541f, .69f, .839f, .84f, .604f, .765f, .894f, 1, .663f, .827f, .945f};
	/** The jetpack sparks' enemy row (800D5B12 r2): red to pale pink. */
	static final float[] CAT_SPARK_ENEMY = {0, 0, 0, 0, .2f, 1, 0, 0, .36f, 1, .231f, .212f, .52f, 1, .369f, .337f, .68f, 1, .561f, .522f, .84f, 1, .725f, .671f, 1, 1, .847f, .788f};
	/** The paw print's second set (709F C56, 800751FB r4, HDR 2.4): deep blue to white. */
	static final float[] CAT_PAW_PRINT_BLUE = {0, 0, 0, 0, .2f, 0, .067f, .804f, .36f, .173f, .306f, .812f, .52f, .294f, .49f, .855f, .68f, .475f, .761f, .969f, .84f, .733f, 1, 1, 1, .937f, 1, 1};
}
