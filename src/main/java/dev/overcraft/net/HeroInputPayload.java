package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server, hero controls (only while hotbar slot 1 is selected): which fire buttons are held, and the
 * ability keys pressed since the last one.
 */
public record HeroInputPayload(int held, int pressed, int extra, int wall, float yaw, float pitch, float partial) implements CustomPacketPayload {
	public static final int HELD_PRIMARY = 1;
	public static final int HELD_SECONDARY = 2;
	/** Riding a wall (Lúcio's Wall Ride: the client moves him; the server needs it for Soundwave Rider, and {@code wall} says how: OwMovement.wallCode). */
	public static final int HELD_WALL_RIDE = 4;
	/** Climbing a wall (Kiriko: the client moves her; the climb interrupts a reload, #42). */
	public static final int HELD_WALL_CLIMB = 8;

	public static final int PRESS_ABILITY_1 = 1;
	public static final int PRESS_ABILITY_2 = 2;
	public static final int PRESS_ULTIMATE = 4;
	public static final int PRESS_RELOAD = 8;
	public static final int PRESS_MELEE = 16;
	/**
	 * Sojourn's Charged Shot, pressed and already shown on her own client (ClientSojourn): her own copies aren't sent back, and
	 * {@code yaw} and {@code pitch} carry her aim of that moment, and {@code partial} how far between her last two positions the frame she saw was
	 * drawn (0..1), which the server turns into a place on her own path (Railgun.pressedShown). Only with this bit: the rest never send them.
	 */
	public static final int PRESS_CHARGED_SHOWN = 32;

	/** Extra presses: Mercy's weapon keys (Overwatch's 1 / 2, the scroll wheel), and Jump / Crouch (Guardian Angel's launches). */
	public static final int EXTRA_STAFF = 1;
	public static final int EXTRA_BLASTER = 2;
	public static final int EXTRA_SWAP = 4;
	public static final int EXTRA_JUMP = 8;
	public static final int EXTRA_CROUCH = 16;
	/** Jump with back held (Guardian Angel's backward launch). */
	public static final int EXTRA_BACK = 32;
	/** Sojourn's Power Slide ended against a wall (her client moves her: ClientSojourn). */
	public static final int EXTRA_SLIDE_END = 64;

	public HeroInputPayload(int held, int pressed) {
		this(held, pressed, 0, 0);
	}

	public HeroInputPayload(int held, int pressed, int extra) {
		this(held, pressed, extra, 0);
	}

	public HeroInputPayload(int held, int pressed, int extra, int wall) {
		this(held, pressed, extra, wall, 0, 0, 1);
	}

	public static final Type<HeroInputPayload> TYPE = new Type<>(Overcraft.id("hero_input"));
	public static final StreamCodec<FriendlyByteBuf, HeroInputPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeByte(p.held);
			buf.writeByte(p.pressed);
			buf.writeByte(p.extra);
			buf.writeVarInt(p.wall);
			if ((p.pressed & PRESS_CHARGED_SHOWN) != 0) {
				buf.writeFloat(p.yaw);
				buf.writeFloat(p.pitch);
				buf.writeFloat(p.partial);
			}
		}, buf -> {
			int held = buf.readByte(), pressed = buf.readByte(), extra = buf.readByte(), wall = buf.readVarInt();
			return (pressed & PRESS_CHARGED_SHOWN) != 0 ? new HeroInputPayload(held, pressed, extra, wall, buf.readFloat(), buf.readFloat(), buf.readFloat())
				: new HeroInputPayload(held, pressed, extra, wall);
		});

	@Override
	public Type<HeroInputPayload> type() {
		return TYPE;
	}
}
