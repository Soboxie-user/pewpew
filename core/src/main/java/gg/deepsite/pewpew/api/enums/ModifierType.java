package gg.deepsite.pewpew.api.enums;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The gun stats an attachment can change through its {@code modifiers} block.
 *
 * <p>Every stat accepts a multiplier ({@code damage: 1.1} or {@code damage_multiplier: 1.1}) and a flat
 * addition ({@code damage_bonus: 2}). Additions are applied first, then multipliers:
 * {@code (base + additions) * multipliers}. A stat listed as {@code additiveByDefault} treats a bare key
 * as an addition instead of a multiplier.
 */
public enum ModifierType {

	// Output and ballistics
	DAMAGE("Damage", true, false),
	HEADSHOT("Headshot", true, false),
	RANGE("Range", true, false),
	PROJECTILE_SPEED("Projectile Speed", true, false),
	/** Above 1.0 the gun fires faster, below 1.0 slower. */
	FIRE_RATE("Fire Rate", true, false),

	// Handling
	RECOIL("Recoil", false, false),
	SPREAD("Spread", false, false),
	AIM_RECOIL("ADS Recoil", false, false),
	AIM_SPREAD("ADS Spread", false, false),

	// Ammo and reloading
	MAX_AMMO("Ammo", true, true),
	RELOAD_TIME("Reload Time", false, false);

	private final String label;
	private final boolean higherIsBetter;
	private final boolean additiveByDefault;

	ModifierType(String label, boolean higherIsBetter, boolean additiveByDefault) {
		this.label = label;
		this.higherIsBetter = higherIsBetter;
		this.additiveByDefault = additiveByDefault;
	}

	public String label() {
		return label;
	}

	public boolean higherIsBetter() {
		return higherIsBetter;
	}

	public boolean additiveByDefault() {
		return additiveByDefault;
	}

	/** Looks a stat up by its config name, ignoring case and treating {@code -} like {@code _}. */
	@Nullable
	public static ModifierType fromKey(String key) {
		String normalized = key.trim().toUpperCase(Locale.ROOT).replace('-', '_');
		for (ModifierType type : values()) {
			if (type.name().equals(normalized)) return type;
		}
		return null;
	}
}
