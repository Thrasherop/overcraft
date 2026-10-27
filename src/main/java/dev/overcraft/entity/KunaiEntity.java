package dev.overcraft.entity;

import dev.overcraft.hero.Allies;
import dev.overcraft.Ow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroSounds;
import dev.overcraft.kiriko.KirikoStats;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.registry.OcItems;
import dev.overcraft.registry.OcSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.phys.Vec3;

/** Kiriko's kunai: fast, no drop, no falloff, 2x on a headshot. Passes through allies. */
public class KunaiEntity extends OwProjectile {
	/** How far below the eyes a hit still counts as the head (blocks). */
	private static final double HEAD_DEPTH = 0.25;

	public KunaiEntity(EntityType<? extends KunaiEntity> type, Level level) {
		super(type, level);
	}

	public KunaiEntity(LivingEntity owner, Level level) {
		super(OcEntities.KUNAI, owner, level, new ItemStack(OcItems.KUNAI));
	}

	@Override
	protected Item getDefaultItem() {
		return OcItems.KUNAI;
	}

	@Override
	protected int lifetime() {
		return 40;
	}

	/** This kunai's hit was a headshot (set before its damage is dealt: the kill feed reads it). */
	private boolean headshot;

	public boolean headshot() {
		return headshot;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// the Ender Dragon is hit through its parts (its hitboxes are entities of their own, not living ones: the kunai
		// went through it, found playing #18)
		Entity target = entity instanceof EnderDragonPart part ? part.parentMob : entity;
		if (!super.canHitEntity(entity)) {
			return false;
		}
		// what isn't a creature (an end crystal, a boat, a painting) takes the hit as from an arrow (#34)
		return !(target instanceof LivingEntity) || getOwner() instanceof LivingEntity owner && Allies.canHurt(owner, target);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		EnderDragonPart part = hit.getEntity() instanceof EnderDragonPart p ? p : null;
		LivingEntity target = part != null ? part.parentMob : hit.getEntity() instanceof LivingEntity living ? living : null;
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (target == null) {
			// not a creature: hurt it as an arrow does (an end crystal explodes, a boat or minecart takes damage, a
			// painting breaks), then the kunai is spent
			Vec3 at = hit.getLocation();
			hit.getEntity().hurtServer(level, damageSources().thrown(this, getOwner()), (float) Ow.hp(KirikoStats.KUNAI_DAMAGE));
			Effects.play(level, "kunai_hit_entity", at, getOwner());
			level.playSound(null, at.x, at.y, at.z, OcSounds.KIRIKO_KUNAI_HIT, SoundSource.PLAYERS, 1, 1);
			discard();
			return;
		}
		Vec3 at = hit.getLocation();
		// the dragon's head is its head; anyone else's, the top of their box
		boolean crit = part != null ? part == part.parentMob.head : at.y >= target.getEyeY() - HEAD_DEPTH;
		// known before the damage: a killing blow's kill feed and voice line look at it while the damage is dealt
		headshot = crit;
		HeroState state = crit && getOwner() instanceof Player thrower ? HeroManager.state(thrower) : null;
		if (state != null) {
			state.critAt = level.getGameTime();
		}
		double damage = KirikoStats.KUNAI_DAMAGE * (crit ? KirikoStats.KUNAI_CRIT : 1);
		float dealt = part != null ? Combat.damagePart(level, part, damageSources().thrown(this, getOwner()), damage)
			: Combat.damage(target, damageSources().thrown(this, getOwner()), damage);
		Effects.play(level, "kunai_hit_entity", at, getOwner());
		level.playSound(null, at.x, at.y, at.z, OcSounds.KIRIKO_KUNAI_HIT, SoundSource.PLAYERS, 1, 1);
		if (getOwner() instanceof Player owner) {
			Combat.credit(owner, dealt);
			Combat.addFireForDamage(owner, dealt, Ow.toOw(target.getMaxHealth()));
			if (owner instanceof net.minecraft.server.level.ServerPlayer thrower && dealt > 0) {
				Effects.hitMarker(thrower, target, crit);
				dev.overcraft.kiriko.Kiriko.fortuneTeller(thrower);
			}
			if (crit) {
				// the critical hit's confirmation is the thrower's (it plays where they are)
				if (owner instanceof net.minecraft.server.level.ServerPlayer thrower) {
					HeroSounds.toOwner(thrower, OcSounds.KIRIKO_KUNAI_HEADSHOT);
				}
			}
		}
		discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel level) {
			Vec3 at = hit.getLocation();
			Effects.play(level, "kunai_hit_block", at, Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()), null, 0, getOwner());
			level.playSound(null, at.x, at.y, at.z, OcSounds.KIRIKO_KUNAI_HIT_BLOCK, SoundSource.PLAYERS, 1, 1);
			discard();
		}
	}
}
