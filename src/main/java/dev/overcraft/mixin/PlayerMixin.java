package dev.overcraft.mixin;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.HeroManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A hero sounds like the hero, not Steve: their own pain grunt when hurt (Minecraft's plain hurt sound; burning,
 * drowning and the like keep theirs) and their own cry when they die. On every side (the hero attachment is synced),
 * as Minecraft plays these from the entity itself.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	/**
	 * A hero's own body box (dev.overcraft.hero.HeroBody) on or off as the hero, the mode and the room around change, before
	 * Minecraft's tick sets the pose from it.
	 */
	@Inject(method = "tick", at = @At("HEAD"))
	private void overcraft$heroBody(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		Player self = (Player) (Object) this;
		// also when the box Minecraft holds isn't the one wanted: the client's own player kept Minecraft's box on rejoin while
		// the server's was his (found playing: it changed only when a crouch refreshed it)
		if (self instanceof net.minecraft.server.level.ServerPlayer server) {
			// where his box doesn't fit (a doorway, beside the bed): moved to room, or back to Minecraft mode
			dev.overcraft.hero.HeroBody.makeRoom(server);
		}
		boolean changed = dev.overcraft.hero.HeroBody.update(self);
		net.minecraft.world.entity.EntityDimensions want = self.getDimensions(self.getPose());
		if (changed || want.width() != self.getBbWidth() || want.height() != self.getBbHeight()) {
			self.refreshDimensions();
		}
	}

	/**
	 * Sojourn's Power Slide keeps her low (sojourn-plan.md: a crouch-height box; Minecraft's crouching pose, 1.5 m, is the
	 * nearest to her mover's 1.25 m): her camera drops and she slides under 1.5 m gaps. On her client from the slide it moves,
	 * on the server from her kit (dev.overcraft.sojourn.Sojourn.slidingPose).
	 */
	@Inject(method = "getDesiredPose", at = @At("HEAD"), cancellable = true)
	private void overcraft$slidePose(CallbackInfoReturnable<net.minecraft.world.entity.Pose> cir) {
		if (dev.overcraft.sojourn.Sojourn.slidingPose((Player) (Object) this)) {
			cir.setReturnValue(net.minecraft.world.entity.Pose.CROUCHING);
		}
	}

	/**
	 * A hero's pain grunt is their voice's pain stimulus (0003), not a sound on every hit: the server says it by Voice's rules
	 * (its 1-2.5 s cooldown, one line at a time), and Minecraft's own hurt sound (the server's playHurtSound, every client's
	 * handleDamageEvent) is silenced for it. It played on every hit: 36 grunts in about 1 s from D.Va's Micro Missiles (#129).
	 */
	@Inject(method = "getHurtSound", at = @At("RETURN"), cancellable = true)
	private void overcraft$heroPain(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
		if (cir.getReturnValue() == SoundEvents.PLAYER_HURT) {
			SoundEvent pain = heroLine("pain");
			if (pain != null) {
				if (dev.overcraft.hero.Voice.devPainEveryHit) {
					cir.setReturnValue(pain);
					return;
				}
				// the server's call is playHurtSound's, once per hit that hurts (LivingEntity.hurtServer)
				if ((Object) this instanceof net.minecraft.server.level.ServerPlayer server) {
					dev.overcraft.hero.Voice.say(server, "pain", 1);
				}
				cir.setReturnValue(null);
			}
		}
	}

	@Inject(method = "getDeathSound", at = @At("RETURN"), cancellable = true)
	private void overcraft$heroDeath(CallbackInfoReturnable<SoundEvent> cir) {
		SoundEvent death = heroLine("death");
		if (death != null) {
			cir.setReturnValue(death);
		}
	}

	private SoundEvent heroLine(String moment) {
		String hero = ((Player) (Object) this).getAttached(HeroManager.HERO);
		return hero == null ? null : BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id(hero + ".voice." + moment));
	}
}
