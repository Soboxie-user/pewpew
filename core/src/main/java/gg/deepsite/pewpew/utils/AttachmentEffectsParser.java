package gg.deepsite.pewpew.utils;

import gg.deepsite.pewpew.api.objects.ExplosiveConfig;
import gg.deepsite.pewpew.api.objects.PewpewEffect;
import gg.deepsite.pewpew.api.objects.attachment.AttachmentEffects;
import net.kyori.adventure.key.Key;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Reads the {@code effects} block of an attachment. */
public final class AttachmentEffectsParser {

	private static final Set<String> KEYS = Set.of(
			"victimEffects", "shooterEffects", "fireTicks", "freezeTicks", "impactParticle", "explosive",
			"chance", "cooldown");

	private AttachmentEffectsParser() {
	}

	public static AttachmentEffects parse(String fileName, String id, ConfigurationNode node, Consumer<String> warn) {
		if (node == null || node.virtual()) return AttachmentEffects.NONE;
		if (!node.isMap()) {
			warn.accept("'effects' must be a map");
			return AttachmentEffects.NONE;
		}
		for (Object key : node.childrenMap().keySet()) {
			if (!KEYS.contains(String.valueOf(key))) warn.accept("unknown effect '" + key + "'");
		}

		List<PewpewEffect> victim = WeaponDeserializer.parseEffects(fileName, id, node.node("victimEffects"));
		List<PewpewEffect> shooter = WeaponDeserializer.parseEffects(fileName, id, node.node("shooterEffects"));
		int fireTicks = Math.max(0, node.node("fireTicks").getInt(0));
		int freezeTicks = Math.max(0, node.node("freezeTicks").getInt(0));
		Key particle = WeaponDeserializer.parseKey(fileName, id, "effects.impactParticle",
				node.node("impactParticle").getString(), null);

		ExplosiveConfig explosive = null;
		ConfigurationNode explosiveNode = node.node("explosive");
		if (!explosiveNode.virtual()) {
			ConfigurationNode rebuild = explosiveNode.node("rebuild");
			explosive = new ExplosiveConfig(
					Math.max(0.0, explosiveNode.node("blastRadius").getDouble(4.0)),
					Math.max(0.0, explosiveNode.node("explosionDamage").getDouble(12.0)),
					Math.max(0.0, explosiveNode.node("explosionKnockback").getDouble(1.2)),
					explosiveNode.node("damageBlocks").getBoolean(false),
					rebuild.node("enabled").getBoolean(false),
					Math.max(0, rebuild.node("delay").getInt(100)),
					Math.max(1, rebuild.node("blocksPerTick").getInt(2)));
		}

		double chance = node.node("chance").getDouble(1.0);
		if (Double.isNaN(chance) || chance < 0.0 || chance > 1.0) {
			warn.accept("'chance' must be between 0 and 1, using 1");
			chance = 1.0;
		}
		int cooldown = Math.max(0, node.node("cooldown").getInt(0));
		int explosionCooldown = Math.max(0, explosiveNode.node("cooldown").getInt(0));

		AttachmentEffects effects = new AttachmentEffects(victim, shooter, fireTicks, freezeTicks, particle, explosive,
				chance, cooldown, explosionCooldown);
		return effects.isEmpty() ? AttachmentEffects.NONE : effects;
	}
}
