package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client, every tick while the player has a hero: what the HUD shows. Cooldowns are seconds left;
 * reloads are 0..1 progress (-1 when not reloading). canChangeHero: near your bed (or no hero yet), H opens the
 * select screen. onFire: the hero is On Fire (Combat.tickFire). Perks (#36): the perk level (1-3), perkProgress 0..1
 * toward the next level (1 at level 3), perkReady the tier waiting to be picked (0 none, 1 minor, 2 major), and the picked
 * minor and major perks (Perk ordinals, -1 for none). For heroes with more: ability3Cooldown (Lúcio's Soundwave, s),
 * mode (Lúcio's song: 0 healing, 1 speed), activeLeft (an ability running, s left: Amp It Up) and meter (0..1: Soundwave
 * Rider's charge). shield: the hero's shields left (Overwatch points: Lifeweaver's, dev.overcraft.hero.Shields).
 */
public record HeroSyncPayload(String heroId, float ultCharge, int ultCost, boolean ultActive,
							  int primaryAmmo, float primaryReload, int secondaryAmmo, float secondaryReload,
							  float ability1Cooldown, float ability2Cooldown, boolean rushed,
							  boolean canChangeHero, boolean onFire,
							  int perkLevel, float perkProgress, int perkReady, int minorPerk, int majorPerk,
							  float ability3Cooldown, int mode, float activeLeft, float meter, float shield,
							  Widgets widgets) implements CustomPacketPayload {
	public static final HeroSyncPayload NONE = new HeroSyncPayload("", 0, 1, false, 0, -1, 0, -1, 0, 0, false, true, false,
		1, 0, 0, -1, -1, 0, 0, 0, 0);

	/**
	 * What a hero's widgets at the reticle show that the fields above don't (#85, client/hud/ReticleHud): each kit says what its
	 * own mean (HeroKit.widgets). target and target2: entities' ids, -1 none; preview: the ally the kit's button would take now, the
	 * server's own pick so the marker can't disagree with the button, -1 none; flag: a state the widget shows; count: a number to
	 * show, 0 none; left and total: a timer's seconds left and full length, 0 none; left2: a second timer's seconds left; name and
	 * face: who, when the client has no entity to ask (a soul): their name and entity type, or "hero:id", "" none.
	 */
	public record Widgets(int target, int target2, int preview, boolean flag, int count, float left, float total, float left2, String name, String face) {
		public static final Widgets NONE = new Widgets(-1, -1, -1, false, 0, 0, 0, 0, "", "");
	}

	/** Without shields (the kits' own sync; HeroManager adds them: withShield). */
	public HeroSyncPayload(String heroId, float ultCharge, int ultCost, boolean ultActive, int primaryAmmo, float primaryReload,
						   int secondaryAmmo, float secondaryReload, float ability1Cooldown, float ability2Cooldown, boolean rushed,
						   boolean canChangeHero, boolean onFire, int perkLevel, float perkProgress, int perkReady, int minorPerk,
						   int majorPerk, float ability3Cooldown, int mode, float activeLeft, float meter) {
		this(heroId, ultCharge, ultCost, ultActive, primaryAmmo, primaryReload, secondaryAmmo, secondaryReload, ability1Cooldown,
			ability2Cooldown, rushed, canChangeHero, onFire, perkLevel, perkProgress, perkReady, minorPerk, majorPerk,
			ability3Cooldown, mode, activeLeft, meter, 0, Widgets.NONE);
	}

	public HeroSyncPayload withShield(float shield) {
		return new HeroSyncPayload(heroId, ultCharge, ultCost, ultActive, primaryAmmo, primaryReload, secondaryAmmo, secondaryReload,
			ability1Cooldown, ability2Cooldown, rushed, canChangeHero, onFire, perkLevel, perkProgress, perkReady, minorPerk, majorPerk,
			ability3Cooldown, mode, activeLeft, meter, shield, widgets);
	}

	public HeroSyncPayload withWidgets(Widgets widgets) {
		return new HeroSyncPayload(heroId, ultCharge, ultCost, ultActive, primaryAmmo, primaryReload, secondaryAmmo, secondaryReload,
			ability1Cooldown, ability2Cooldown, rushed, canChangeHero, onFire, perkLevel, perkProgress, perkReady, minorPerk, majorPerk,
			ability3Cooldown, mode, activeLeft, meter, shield, widgets);
	}

	public static final Type<HeroSyncPayload> TYPE = new Type<>(Overcraft.id("hero_sync"));
	public static final StreamCodec<FriendlyByteBuf, HeroSyncPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeUtf(p.heroId, 32);
			buf.writeFloat(p.ultCharge);
			buf.writeVarInt(p.ultCost);
			buf.writeBoolean(p.ultActive);
			buf.writeVarInt(p.primaryAmmo);
			buf.writeFloat(p.primaryReload);
			buf.writeVarInt(p.secondaryAmmo);
			buf.writeFloat(p.secondaryReload);
			buf.writeFloat(p.ability1Cooldown);
			buf.writeFloat(p.ability2Cooldown);
			buf.writeBoolean(p.rushed);
			buf.writeBoolean(p.canChangeHero);
			buf.writeBoolean(p.onFire);
			buf.writeByte(p.perkLevel);
			buf.writeFloat(p.perkProgress);
			buf.writeByte(p.perkReady);
			buf.writeByte(p.minorPerk);
			buf.writeByte(p.majorPerk);
			buf.writeFloat(p.ability3Cooldown);
			buf.writeVarInt(p.mode);
			buf.writeFloat(p.activeLeft);
			buf.writeFloat(p.meter);
			buf.writeFloat(p.shield);
			buf.writeVarInt(p.widgets.target());
			buf.writeVarInt(p.widgets.target2());
			buf.writeVarInt(p.widgets.preview());
			buf.writeBoolean(p.widgets.flag());
			buf.writeVarInt(p.widgets.count());
			buf.writeFloat(p.widgets.left());
			buf.writeFloat(p.widgets.total());
			buf.writeFloat(p.widgets.left2());
			buf.writeUtf(net.minecraft.util.StringUtil.truncateStringIfNecessary(p.widgets.name(), 64, false), 64);
			buf.writeUtf(net.minecraft.util.StringUtil.truncateStringIfNecessary(p.widgets.face(), 64, false), 64);
		}, buf -> new HeroSyncPayload(buf.readUtf(32), buf.readFloat(), buf.readVarInt(), buf.readBoolean(),
			buf.readVarInt(), buf.readFloat(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
			buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
			buf.readByte(), buf.readFloat(), buf.readByte(), buf.readByte(), buf.readByte(),
			buf.readFloat(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
			new Widgets(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readUtf(64), buf.readUtf(64))));

	@Override
	public Type<HeroSyncPayload> type() {
		return TYPE;
	}
}
