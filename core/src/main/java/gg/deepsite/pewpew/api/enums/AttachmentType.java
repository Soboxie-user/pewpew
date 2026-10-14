package gg.deepsite.pewpew.api.enums;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A kind of attachment slot. This behaves like an enum, but new constants can be registered at runtime
 * from {@code attachment-menu.yml}. Instances are interned by name, so {@code ==} comparisons keep working.
 */
public final class AttachmentType {

	/** Names end up in a NamespacedKey, so they are limited to the characters that key allows. */
	private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_.-]{1,32}");

	private static final Map<String, AttachmentType> REGISTRY = new LinkedHashMap<>();

	// The original slots, always present so existing guns and items keep working.
	public static final AttachmentType SCOPE = register("SCOPE");
	public static final AttachmentType BARREL = register("BARREL");
	public static final AttachmentType GRIP = register("GRIP");
	public static final AttachmentType MAGAZINE = register("MAGAZINE");

	private final String name;

	private AttachmentType(String name) {
		this.name = name;
	}

	/**
	 * Registers a slot type, or returns the existing one with that name.
	 *
	 * @throws IllegalArgumentException if the name contains characters other than letters, digits, {@code _},
	 *                                  {@code .} or {@code -}, or is longer than 32 characters
	 */
	public static synchronized AttachmentType register(String name) {
		if (name == null || !VALID_NAME.matcher(name).matches()) {
			throw new IllegalArgumentException("Invalid attachment type '" + name
					+ "': use 1-32 letters, digits, '_', '.' or '-'");
		}
		return REGISTRY.computeIfAbsent(name.toUpperCase(Locale.ROOT), AttachmentType::new);
	}

	/** Mirrors {@code Enum.valueOf}. */
	public static synchronized AttachmentType valueOf(String name) {
		AttachmentType type = REGISTRY.get(name.toUpperCase(Locale.ROOT));
		if (type == null) {
			throw new IllegalArgumentException("No attachment type named " + name);
		}
		return type;
	}

	/** Mirrors {@code Enum.values}: a snapshot of every registered type, in registration order. */
	public static synchronized List<AttachmentType> values() {
		return List.copyOf(REGISTRY.values());
	}

	public String name() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}
