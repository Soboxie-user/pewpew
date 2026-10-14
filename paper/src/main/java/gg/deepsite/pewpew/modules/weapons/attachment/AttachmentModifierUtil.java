package gg.deepsite.pewpew.modules.weapons.attachment;

import gg.deepsite.pewpew.PewpewPlugin;
import gg.deepsite.pewpew.api.enums.AttachmentType;
import gg.deepsite.pewpew.api.enums.ModifierType;
import gg.deepsite.pewpew.api.objects.PewPewItem;
import gg.deepsite.pewpew.api.objects.attachment.AttachmentModifiers;
import gg.deepsite.pewpew.api.objects.attachment.PewpewAttachment;
import gg.deepsite.pewpew.modules.items.ItemsModule;
import lombok.experimental.UtilityClass;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Combines the {@code modifiers} of every attachment fitted to a gun. Additions are applied before
 * multipliers: {@code (base + additions) * multipliers}.
 */
@UtilityClass
public class AttachmentModifierUtil {

	/** Every attachment currently fitted to the gun, in slot registration order. */
	@NotNull
	public static List<PewpewAttachment> fitted(@NotNull ItemStack gun) {
		ItemMeta meta = gun.getItemMeta();
		if (meta == null) return List.of();
		PersistentDataContainer data = meta.getPersistentDataContainer();
		ItemsModule items = PewpewPlugin.getModuleManager().get(ItemsModule.class);

		List<PewpewAttachment> result = null;
		for (AttachmentType slot : AttachmentType.values()) {
			String id = data.get(AttachmentUtil.key(slot), PersistentDataType.STRING);
			if (id == null) continue;
			PewPewItem item = items.get(id);
			if (item instanceof PewpewAttachment attachment && attachment.getSlot() == slot) {
				if (result == null) result = new ArrayList<>(4);
				result.add(attachment);
			}
		}
		return result == null ? List.of() : result;
	}

	/** Applies the fitted attachments' modifiers for {@code type} to {@code base}. */
	public static double apply(@NotNull ItemStack gun, @NotNull ModifierType type, double base) {
		return apply(fitted(gun), type, base);
	}

	/** Same, for attachments already read from the gun, so one shot does not read it again. */
	public static double apply(@NotNull List<PewpewAttachment> fitted, @NotNull ModifierType type, double base) {
		double additions = 0.0;
		double multiplier = 1.0;
		for (PewpewAttachment attachment : fitted) {
			AttachmentModifiers modifiers = attachment.getModifiers();
			if (modifiers == null) continue;
			additions += modifiers.addition(type);
			multiplier *= modifiers.multiplier(type);
		}
		return (base + additions) * multiplier;
	}

	/** The ids of these attachments, comma separated, for carrying them on a projectile. Empty when there are none. */
	@NotNull
	public static String ids(@NotNull List<PewpewAttachment> fitted) {
		StringBuilder joined = new StringBuilder();
		for (PewpewAttachment attachment : fitted) {
			if (joined.length() > 0) joined.append(',');
			joined.append(attachment.getId());
		}
		return joined.toString();
	}

	/** Reverse of {@link #ids}; ids that no longer exist (after a reload) are skipped. */
	@NotNull
	public static List<PewpewAttachment> fromIds(@Nullable String ids) {
		if (ids == null || ids.isEmpty()) return List.of();
		ItemsModule items = PewpewPlugin.getModuleManager().get(ItemsModule.class);
		List<PewpewAttachment> result = new ArrayList<>(4);
		for (String id : ids.split(",")) {
			if (items.get(id) instanceof PewpewAttachment attachment) result.add(attachment);
		}
		return result;
	}

	/** For stats that are a multiplier on something else (recoil, spread): the combined multiplier, never negative. */
	public static double multiplier(@NotNull ItemStack gun, @NotNull ModifierType type) {
		return Math.max(0.0, apply(gun, type, 1.0));
	}
}
