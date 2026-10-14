package gg.deepsite.pewpew.api.objects.attachment;

import gg.deepsite.pewpew.api.enums.ModifierType;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The parsed {@code modifiers} block of an attachment: flat additions and multipliers per stat.
 *
 * <pre>
 * modifiers:
 *   damage: 0.9              # multiplier
 *   range_multiplier: 1.1    # multiplier, long form
 *   recoil_bonus: -0.1       # flat addition
 *   max_ammo: 5              # MAX_AMMO is additive when written bare
 * </pre>
 */
public record AttachmentModifiers(Map<ModifierType, Double> additions, Map<ModifierType, Double> multipliers) {

	public static final AttachmentModifiers NONE = new AttachmentModifiers(Map.of(), Map.of());

	private static final String[] ADDITIVE_SUFFIXES = {"_bonus", "_add"};
	private static final String[] MULTIPLIER_SUFFIXES = {"_multiplier", "_modifier", "_mult"};

	public AttachmentModifiers {
		additions = immutable(additions);
		multipliers = immutable(multipliers);
	}

	public double addition(ModifierType type) {
		return additions.getOrDefault(type, 0.0);
	}

	public double multiplier(ModifierType type) {
		return multipliers.getOrDefault(type, 1.0);
	}

	public boolean isEmpty() {
		return additions.isEmpty() && multipliers.isEmpty();
	}

	/**
	 * Parses a {@code modifiers} node. Problems are reported through {@code warn} and the offending entry is
	 * skipped, so one typo never takes the whole attachment down.
	 */
	public static AttachmentModifiers parse(ConfigurationNode node, Consumer<String> warn) {
		if (node == null || node.virtual()) return NONE;
		if (!node.isMap()) {
			warn.accept("'modifiers' must be a map of stat names to numbers");
			return NONE;
		}

		Map<ModifierType, Double> additions = new EnumMap<>(ModifierType.class);
		Map<ModifierType, Double> multipliers = new EnumMap<>(ModifierType.class);

		for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
			String raw = String.valueOf(entry.getKey());
			String key = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');

			Boolean additive = null;
			for (String suffix : ADDITIVE_SUFFIXES) {
				if (key.endsWith(suffix)) {
					additive = true;
					key = key.substring(0, key.length() - suffix.length());
					break;
				}
			}
			if (additive == null) {
				for (String suffix : MULTIPLIER_SUFFIXES) {
					if (key.endsWith(suffix)) {
						additive = false;
						key = key.substring(0, key.length() - suffix.length());
						break;
					}
				}
			}

			ModifierType type = ModifierType.fromKey(key);
			if (type == null) {
				warn.accept("unknown modifier '" + raw + "'");
				continue;
			}
			if (additive == null) additive = type.additiveByDefault();

			double value = entry.getValue().getDouble(Double.NaN);
			if (Double.isNaN(value) || Double.isInfinite(value)) {
				warn.accept("modifier '" + raw + "' must be a number");
				continue;
			}
			if (!additive && value < 0) {
				warn.accept("multiplier '" + raw + "' cannot be negative");
				continue;
			}

			Map<ModifierType, Double> target = additive ? additions : multipliers;
			if (target.containsKey(type)) {
				warn.accept("modifier '" + raw + "' is set more than once, using the last value");
			}
			target.put(type, value);
		}

		return additions.isEmpty() && multipliers.isEmpty() ? NONE : new AttachmentModifiers(additions, multipliers);
	}

	private static Map<ModifierType, Double> immutable(Map<ModifierType, Double> source) {
		return source.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(source));
	}
}
