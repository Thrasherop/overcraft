package dev.overcraft.mixin;

import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.UUID;

/** Minecraft's own zombie villager cure (the golden apple's), for Protection Suzu's cleanse (SuzuEntity). */
@Mixin(ZombieVillager.class)
public interface ZombieVillagerInvoker {
	@Invoker("startConverting")
	void overcraft$startConverting(@Nullable UUID player, int time);
}
