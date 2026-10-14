package gg.deepsite.pewpew.modules.weapons.lore;

import gg.deepsite.pewpew.api.enums.ModifierType;
import gg.deepsite.pewpew.api.objects.PewpewEffect;
import gg.deepsite.pewpew.api.objects.attachment.*;
import gg.deepsite.pewpew.utils.ChatUtils;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class AttachmentLoreRenderer {

	public static void apply(ItemStack stack, PewpewAttachment attachment) {
		ItemMeta meta = stack.getItemMeta();
		if (meta == null) return;

		List<Component> lore = new ArrayList<>();
		if (attachment.getLore() != null) {
			for (String flavor : attachment.getLore()) lore.add(line(flavor));
		}

		List<Component> stats = stats(attachment);
		if (!stats.isEmpty()) {
			lore.add(Component.empty());
			lore.add(line("<color>Attachment <dark_gray>(" + label(attachment) + ")"));
			lore.addAll(stats);
		}

		meta.lore(lore);
		stack.setItemMeta(meta);
	}

	private static List<Component> stats(PewpewAttachment attachment) {
		List<Component> lines = new ArrayList<>();
		if (attachment instanceof PewpewBarrelAttachment barrel) {
			modifier(lines, "Damage", barrel.getDamageModifier(), true);
			modifier(lines, "Range", barrel.getRangeModifier(), true);
		} else if (attachment instanceof PewpewGripAttachment grip) {
			modifier(lines, "Recoil", grip.getRecoilModifier(), false);
		} else if (attachment instanceof PewpewScopeAttachment scope) {
			lines.add(stat("Zoom", "x" + trim(scope.getZoom())));
			if (scope.getAimSpreadMultiplier() != 1.0)
				modifier(lines, "ADS Spread", scope.getAimSpreadMultiplier(), false);
			if (scope.getAimRecoilMultiplier() != 1.0)
				modifier(lines, "ADS Recoil", scope.getAimRecoilMultiplier(), false);
		} else if (attachment instanceof PewpewMagazineAttachment magazine) {
			if (magazine.getAmmoBonus() != 0) {
				String sign = magazine.getAmmoBonus() > 0 ? "<success>+" : "<error>";
				lines.add(stat("Ammo", sign + magazine.getAmmoBonus()));
			}
			if (magazine.getReloadModifier() != 1.0) modifier(lines, "Reload", magazine.getReloadModifier(), false);
		}
		if (attachment.getModifiers() != null) modifierLines(lines, attachment.getModifiers());
		if (attachment.getEffects() != null) effectLines(lines, attachment.getEffects());
		return lines;
	}

	private static void effectLines(List<Component> lines, AttachmentEffects effects) {
		if (effects.chance() < 1.0) lines.add(stat("Effect chance", "<color>" + trim(effects.chance() * 100) + "%"));
		if (effects.cooldown() > 0) lines.add(stat("Effect cooldown", "<color>" + trim(effects.cooldown() / 20.0) + "s"));
		if (effects.fireTicks() > 0) lines.add(stat("Ignites", "<color>" + trim(effects.fireTicks() / 20.0) + "s"));
		if (effects.freezeTicks() > 0) lines.add(stat("Freezes", "<color>" + trim(effects.freezeTicks() / 20.0) + "s"));
		for (PewpewEffect effect : effects.victimEffects()) lines.add(stat("Inflicts", "<color>" + describe(effect)));
		for (PewpewEffect effect : effects.shooterEffects()) lines.add(stat("On hit, you gain", "<color>" + describe(effect)));
		if (effects.explosive() != null) {
			lines.add(stat("Explosion", "<color>" + trim(effects.explosive().blastRadius()) + " blocks"));
			if (effects.explosionCooldown() > 0) {
				lines.add(stat("Explosion cooldown", "<color>" + trim(effects.explosionCooldown() / 20.0) + "s"));
			}
		}
	}

	private static String describe(PewpewEffect effect) {
		return effect.type().value().replace('_', ' ') + " " + (effect.amplifier() + 1)
				+ " (" + trim(effect.duration() / 20.0) + "s)";
	}

	private static void modifierLines(List<Component> lines, AttachmentModifiers modifiers) {
		for (ModifierType type : ModifierType.values()) {
			modifier(lines, type.label(), modifiers.multiplier(type), type.higherIsBetter());
			double addition = modifiers.addition(type);
			if (addition == 0.0) continue;
			boolean good = type.higherIsBetter() == (addition > 0);
			lines.add(stat(type.label(), (good ? "<success>" : "<error>") + (addition > 0 ? "+" : "") + trim(addition)));
		}
	}

	private static void modifier(List<Component> lines, String label, double multiplier, boolean higherIsBetter) {
		if (multiplier == 1.0) return;
		int percent = (int) Math.round((multiplier - 1.0) * 100);
		boolean good = higherIsBetter == (percent > 0);
		String color = good ? "<success>" : "<error>";
		String sign = percent > 0 ? "+" : "";
		lines.add(stat(label, color + sign + percent + "%"));
	}

	private static Component stat(String label, String value) {
		return line("<gray>" + label + " <dark_gray>┃ " + value);
	}

	private static String label(PewpewAttachment attachment) {
		String name = attachment.getSlot().name();
		return name.charAt(0) + name.substring(1).toLowerCase();
	}

	private static String trim(double value) {
		return value == Math.floor(value) ? String.valueOf((int) value) : String.valueOf(value);
	}

	private static Component line(String miniMessage) {
		return ChatUtils.format(miniMessage).decoration(TextDecoration.ITALIC, false);
	}
}
