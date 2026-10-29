package dev.overcraft.client.effect;

import dev.overcraft.client.ClientHero;
import dev.overcraft.client.ClientLifeweaver;
import dev.overcraft.hero.Hero;
import dev.overcraft.lifeweaver.Lifeweaver;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Lifeweaver's markers, until his effects are read from his data (lifeweaver-plan.md "Later"): where Tree of Life will
 * go while he aims it (his data's preview 48D8 and its effect 4785), and the ally his charging blossom is aimed at (the
 * tracker's preview). Minecraft's dust in his pink: placeholders, ours.
 */
public final class LifeweaverFx {
	private static final DustParticleOptions PINK = new DustParticleOptions(0xF3A7C8, 1.2f);

	private LifeweaverFx() {
	}

	/** Each client tick: the local Lifeweaver's markers. */
	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null || ClientHero.hero() != Hero.LIFEWEAVER || !ClientHero.controls()) {
			return;
		}
		ClientLifeweaver.State s = ClientLifeweaver.state(mc.player);
		if (s == null) {
			return;
		}
		long t = mc.level.getGameTime();
		if (s.has(Lifeweaver.FLAG_TREE_AIM)) {
			Vec3 at = Lifeweaver.treeSpot(mc.player);
			if (at != null) {
				// a ring the trunk's size where it will stand
				for (int i = 0; i < 12; i++) {
					double a = Math.PI * 2 * i / 12 + t * 0.05;
					mc.level.addParticle(PINK, at.x + Math.cos(a), at.y + 0.1, at.z + Math.sin(a), 0, 0, 0);
				}
			}
		}
		if (s.has(Lifeweaver.FLAG_CHARGING) && s.bloomTarget() >= 0 && t % 2 == 0) {
			Entity ally = mc.level.getEntity(s.bloomTarget());
			if (ally != null) {
				Vec3 top = ally.position().add(0, ally.getBbHeight() + 0.35, 0);
				for (int i = 0; i < 6; i++) {
					double a = Math.PI * 2 * i / 6 + t * 0.2;
					mc.level.addParticle(PINK, top.x + Math.cos(a) * 0.35, top.y, top.z + Math.sin(a) * 0.35, 0, 0, 0);
				}
			}
		}
	}
}
