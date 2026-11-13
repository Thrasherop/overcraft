/*
 * Copyright (c) 2025 senseiwells
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package dev.overcraft.vendor.arcade.replay.mixins.player;

import com.mojang.authlib.GameProfile;
import dev.overcraft.vendor.arcade.replay.ducks.ReplayViewable;
import dev.overcraft.vendor.arcade.replay.recorder.player.ReplayPlayerRecorders;
import dev.overcraft.vendor.arcade.replay.viewer.ReplayViewer;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Overcraft: the recording tap moved to Connection.send (dev.overcraft.mixin.PotgSendMixin, ReplayTap): Fabric's
// PacketSender writes there past this listener, so attachment syncs never reached the recording (the spike).
@Mixin(value = ServerCommonPacketListenerImpl.class, priority = 5000)
public abstract class ServerCommonPacketListenerImplMixin {
    @Shadow
    protected abstract GameProfile playerProfile();

    @Shadow
    @Final
    protected MinecraftServer server;

    @Inject(
        method = "onDisconnect",
        at = @At("TAIL")
    )
    private void onDisconnect(DisconnectionDetails details, CallbackInfo ci) {
        // Overcraft: its segment is saved only if a highlight is in it, then the folder is tidied (PotgRecorder)
        dev.overcraft.potg.PotgRecorder.disconnected(this.server, this.playerProfile().id());

        if (this instanceof ReplayViewable viewable) {
            ReplayViewer viewer = viewable.arcade_getViewingReplay();
            if (viewer != null) {
                viewer.close();
            }
        }
    }
}
