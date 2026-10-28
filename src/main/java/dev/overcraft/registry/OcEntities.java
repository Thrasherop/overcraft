package dev.overcraft.registry;

import dev.overcraft.Overcraft;
import dev.overcraft.entity.KitsuneEntity;
import dev.overcraft.entity.KunaiEntity;
import dev.overcraft.entity.OfudaEntity;
import dev.overcraft.entity.SonicEntity;
import dev.overcraft.entity.SuzuEntity;
import dev.overcraft.kiriko.KirikoStats;
import dev.overcraft.lucio.LucioStats;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class OcEntities {
	public static final EntityType<KunaiEntity> KUNAI = register("kunai", EntityType.Builder.<KunaiEntity>of(KunaiEntity::new, MobCategory.MISC)
		.noLootTable().sized((float) KirikoStats.KUNAI_RADIUS * 2, (float) KirikoStats.KUNAI_RADIUS * 2).clientTrackingRange(8).updateInterval(1));
	public static final EntityType<OfudaEntity> OFUDA = register("ofuda", EntityType.Builder.<OfudaEntity>of(OfudaEntity::new, MobCategory.MISC)
		.noLootTable().sized((float) KirikoStats.OFUDA_RADIUS * 2, (float) KirikoStats.OFUDA_RADIUS * 2).clientTrackingRange(8).updateInterval(1));
	public static final EntityType<SuzuEntity> SUZU = register("suzu", EntityType.Builder.<SuzuEntity>of(SuzuEntity::new, MobCategory.MISC)
		.noLootTable().sized((float) KirikoStats.SUZU_PROJECTILE_RADIUS * 2, (float) KirikoStats.SUZU_PROJECTILE_RADIUS * 2)
		.clientTrackingRange(8).updateInterval(1));
	public static final EntityType<SonicEntity> SONIC = register("sonic", EntityType.Builder.<SonicEntity>of(SonicEntity::new, MobCategory.MISC)
		.noLootTable().sized((float) LucioStats.AMP_RADIUS * 2, (float) LucioStats.AMP_RADIUS * 2).clientTrackingRange(8).updateInterval(1));
	public static final EntityType<dev.overcraft.entity.PawEntity> PAW = register("paw", EntityType.Builder.<dev.overcraft.entity.PawEntity>of(
		dev.overcraft.entity.PawEntity::new, MobCategory.MISC).noLootTable().noSave()
		.sized((float) dev.overcraft.jetpackcat.JetpackCatStats.CENTRE_RADIUS * 2, (float) dev.overcraft.jetpackcat.JetpackCatStats.CENTRE_RADIUS * 2)
		.clientTrackingRange(8).updateInterval(1));
	public static final EntityType<KitsuneEntity> KITSUNE = register("kitsune", EntityType.Builder.<KitsuneEntity>of(KitsuneEntity::new, MobCategory.MISC)
		.noLootTable().noSave().sized(0.8f, 0.8f).clientTrackingRange(8).updateInterval(1));

	private OcEntities() {
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Overcraft.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
	}
}
