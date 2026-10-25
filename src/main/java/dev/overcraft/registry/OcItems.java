package dev.overcraft.registry;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroWeaponItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

public final class OcItems {
	public static final Item KIRIKO_WEAPON = register("kiriko_weapon",
		p -> new HeroWeaponItem(Hero.KIRIKO, p), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

	// What Kiriko's projectiles look like in flight (not obtainable)
	public static final Item KUNAI = register("kunai", Item::new, new Item.Properties());
	public static final Item OFUDA = register("ofuda", Item::new, new Item.Properties());
	public static final Item SUZU = register("suzu", Item::new, new Item.Properties());

	private OcItems() {
	}

	public static Item weapon(Hero hero) {
		return switch (hero) {
			case KIRIKO -> KIRIKO_WEAPON;
			default -> KIRIKO_WEAPON;
		};
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Overcraft.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void init() {
	}
}
