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
	public static final EntityType<dev.overcraft.entity.BlasterShotEntity> BLASTER_SHOT = register("blaster_shot",
		EntityType.Builder.<dev.overcraft.entity.BlasterShotEntity>of(dev.overcraft.entity.BlasterShotEntity::new, MobCategory.MISC)
			.noLootTable().noSave()
			.sized((float) dev.overcraft.mercy.MercyStats.BLASTER_RADIUS * 2, (float) dev.overcraft.mercy.MercyStats.BLASTER_RADIUS * 2)
			.clientTrackingRange(8).updateInterval(1));
	// Lifeweaver's: Healing Blossom and Thorn Volley; the thrown platform and seed; the platform, the seed on the ground and
	// the tree (none saved: they're his while he's here)
	public static final EntityType<dev.overcraft.entity.BlossomEntity> BLOSSOM = register("blossom",
		EntityType.Builder.<dev.overcraft.entity.BlossomEntity>of(dev.overcraft.entity.BlossomEntity::new, MobCategory.MISC)
			.noLootTable().noSave()
			.sized((float) dev.overcraft.lifeweaver.LifeweaverStats.BLOSSOM_RADIUS * 2, (float) dev.overcraft.lifeweaver.LifeweaverStats.BLOSSOM_RADIUS * 2)
			.clientTrackingRange(8).updateInterval(1));
	public static final EntityType<dev.overcraft.entity.ThornEntity> THORN = register("thorn",
		EntityType.Builder.<dev.overcraft.entity.ThornEntity>of(dev.overcraft.entity.ThornEntity::new, MobCategory.MISC)
			.noLootTable().noSave()
			.sized((float) dev.overcraft.lifeweaver.LifeweaverStats.THORN_RADIUS * 2, (float) dev.overcraft.lifeweaver.LifeweaverStats.THORN_RADIUS * 2)
			.clientTrackingRange(8).updateInterval(1));
	public static final EntityType<dev.overcraft.entity.PetalThrowEntity> PETAL_THROW = register("petal_throw",
		EntityType.Builder.<dev.overcraft.entity.PetalThrowEntity>of(dev.overcraft.entity.PetalThrowEntity::new, MobCategory.MISC)
			.noLootTable().noSave()
			.sized((float) dev.overcraft.lifeweaver.LifeweaverStats.PLATFORM_THROW_RADIUS * 2, (float) dev.overcraft.lifeweaver.LifeweaverStats.PLATFORM_THROW_RADIUS * 2)
			.clientTrackingRange(8).updateInterval(1));
	public static final EntityType<dev.overcraft.entity.PetalPlatformEntity> PETAL_PLATFORM = register("petal_platform",
		EntityType.Builder.<dev.overcraft.entity.PetalPlatformEntity>of(dev.overcraft.entity.PetalPlatformEntity::new, MobCategory.MISC)
			.noLootTable().noSave()
			.sized((float) dev.overcraft.lifeweaver.LifeweaverStats.PLATFORM_RADIUS * 2, (float) dev.overcraft.lifeweaver.LifeweaverStats.PLATFORM_THICKNESS)
			.clientTrackingRange(10).updateInterval(1));
	public static final EntityType<dev.overcraft.entity.SeedEntity> SEED = register("seed",
		EntityType.Builder.<dev.overcraft.entity.SeedEntity>of(dev.overcraft.entity.SeedEntity::new, MobCategory.MISC)
			.noLootTable().noSave().sized(0.3f, 0.3f).clientTrackingRange(8).updateInterval(2));
	public static final EntityType<dev.overcraft.entity.TreeOfLifeEntity> TREE_OF_LIFE = register("tree_of_life",
		EntityType.Builder.<dev.overcraft.entity.TreeOfLifeEntity>of(dev.overcraft.entity.TreeOfLifeEntity::new, MobCategory.MISC)
			.noLootTable().noSave()
			.sized((float) dev.overcraft.lifeweaver.LifeweaverStats.TRUNK_RADIUS * 2, (float) dev.overcraft.lifeweaver.LifeweaverStats.TRUNK_HEIGHT)
			.clientTrackingRange(10).updateInterval(2));
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
