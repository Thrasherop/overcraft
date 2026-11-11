package dev.overcraft.client.mixin;

import dev.overcraft.client.dev.PotgReplayClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Play of the Game's ghost view (PotgReplayClient, dev only): while a replay draws a frame or plays its step, the level's
 * entities are the ghosts. getEntities() is what getEntity, entitiesForRendering and the box searches go through (design:
 * "The choke point"); adding and removing go to the ghosts' map, and nothing pushes the real player.
 */
@Mixin(ClientLevel.class)
public abstract class PotgClientLevelMixin {
	@Inject(method = "getEntities", at = @At("HEAD"), cancellable = true)
	private void overcraft$ghostLookups(CallbackInfoReturnable<LevelEntityGetter<Entity>> cir) {
		if (PotgReplayClient.ghostView()) {
			cir.setReturnValue(PotgReplayClient.ghosts());
		}
	}

	@Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
	private void overcraft$ghostAdd(Entity entity, CallbackInfo ci) {
		if (PotgReplayClient.ghostView()) {
			PotgReplayClient.addGhost(entity);
			ci.cancel();
		}
	}

	@Inject(method = "removeEntity", at = @At("HEAD"), cancellable = true)
	private void overcraft$ghostRemove(int id, Entity.RemovalReason reason, CallbackInfo ci) {
		if (PotgReplayClient.ghostView()) {
			PotgReplayClient.removeGhost(id, reason);
			ci.cancel();
		}
	}

	@Inject(method = "getPushableEntities", at = @At("HEAD"), cancellable = true)
	private void overcraft$ghostsPushNobody(Entity pusher, AABB box, CallbackInfoReturnable<List<Entity>> cir) {
		if (PotgReplayClient.ghostView()) {
			cir.setReturnValue(List.of());
		}
	}
}