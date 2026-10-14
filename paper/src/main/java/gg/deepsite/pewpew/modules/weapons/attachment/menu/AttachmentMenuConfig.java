package gg.deepsite.pewpew.modules.weapons.attachment.menu;

import gg.deepsite.pewpew.PewpewPlugin;
import gg.deepsite.pewpew.api.enums.AttachmentType;
import gg.deepsite.pewpew.utils.ChatUtils;
import gg.deepsite.pewpew.utils.PewpewLog;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The attachment menu layout, read from {@code attachment-menu.yml}. The {@code pattern} is a grid of
 * characters, nine per row: {@code -} is a mask tile, the gun-preview character is the gun, and every
 * other character must be defined under {@code slots}.
 *
 * <p>Mistakes in the file are reported as warnings and the offending part is skipped.
 */
public class AttachmentMenuConfig {

	private static final String FILE_NAME = "attachment-menu.yml";
	private static final String DEFAULT_TITLE = "<dark_gray>Attachments";
	private static final String DEFAULT_UNAVAILABLE_NAME = "<red>Not available on this gun";
	private static final int COLUMNS = 9;
	private static final int MAX_ROWS = 6;
	private static final char MASK_CHAR = '-';

	private static AttachmentMenuConfig instance;

	private final int size;
	private final String title;
	private final ItemStack maskItem;
	private final int gunSlot;
	private final Map<Integer, SlotData> slotMapping = new LinkedHashMap<>();
	private final List<Integer> maskSlots = new ArrayList<>();

	public AttachmentMenuConfig(ConfigurationSection node) {
		this.title = node.getString("title", DEFAULT_TITLE);

		List<String> pattern = node.getStringList("pattern");
		int rows = node.getInt("rows", Math.max(1, pattern.size()));
		if (rows < 1 || rows > MAX_ROWS) {
			int clamped = Math.max(1, Math.min(MAX_ROWS, rows));
			warn("'rows' must be between 1 and " + MAX_ROWS + ", using " + clamped);
			rows = clamped;
		}
		this.size = rows * COLUMNS;
		this.maskItem = icon(node.getConfigurationSection("mask"), Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");

		char gunChar = 'G';
		boolean previewEnabled = true;
		ConfigurationSection preview = node.getConfigurationSection("gun-preview");
		if (preview != null) {
			String raw = preview.getString("character", "G");
			if (raw.length() == 1) gunChar = raw.charAt(0);
			else warn("gun-preview.character must be a single character, using 'G'");
			previewEnabled = preview.getBoolean("enabled", true);
		}

		Map<Character, SlotData> symbols = readSlots(node.getConfigurationSection("slots"), gunChar,
				node.getConfigurationSection("unavailable-icon"));
		if (symbols.isEmpty()) warn("no slots are defined under 'slots', so the menu will have nothing to fit");

		int gun = -1;
		Set<AttachmentType> placed = new HashSet<>();
		for (int row = 0; row < pattern.size(); row++) {
			if (row >= rows) {
				warn("pattern line " + (row + 1) + " is beyond 'rows' (" + rows + ") and was ignored");
				break;
			}
			String line = pattern.get(row);
			if (line.length() != COLUMNS) {
				warn("pattern line " + (row + 1) + " has " + line.length() + " characters, expected " + COLUMNS);
			}
			for (int col = 0; col < Math.min(line.length(), COLUMNS); col++) {
				char c = line.charAt(col);
				int index = row * COLUMNS + col;
				if (c == ' ') continue;
				if (c == MASK_CHAR) {
					maskSlots.add(index);
				} else if (c == gunChar) {
					if (gun == -1) gun = index;
					else warn("the gun preview character '" + c + "' appears more than once; extra ones ignored");
				} else {
					SlotData data = symbols.get(c);
					if (data == null) {
						warn("pattern uses '" + c + "' but it has no entry under 'slots'");
					} else if (!placed.add(data.type())) {
						warn("slot type " + data.type().name() + " appears more than once in the pattern; "
								+ "a gun holds one attachment per type, so extra tiles were ignored");
					} else {
						slotMapping.put(index, data);
					}
				}
			}
		}
		this.gunSlot = previewEnabled ? gun : -1;
	}

	private static Map<Character, SlotData> readSlots(@Nullable ConfigurationSection slots, char gunChar,
																	  @Nullable ConfigurationSection globalUnavailable) {
		Map<Character, SlotData> result = new HashMap<>();
		if (slots == null) return result;

		for (String key : slots.getKeys(false)) {
			if (key.length() != 1) {
				warn("slot key '" + key + "' must be a single character (quote it if it is a YAML keyword)");
				continue;
			}
			char symbol = key.charAt(0);
			if (symbol == MASK_CHAR || symbol == gunChar) {
				warn("slot key '" + key + "' is reserved for the mask or the gun preview");
				continue;
			}
			ConfigurationSection section = slots.getConfigurationSection(key);
			if (section == null) continue;

			String typeName = section.getString("type");
			if (typeName == null) {
				warn("slot '" + key + "' is missing 'type'");
				continue;
			}
			AttachmentType type;
			try {
				type = AttachmentType.register(typeName);
			} catch (IllegalArgumentException e) {
				warn("slot '" + key + "': " + e.getMessage());
				continue;
			}
			ItemStack emptyIcon = icon(section.getConfigurationSection("empty-icon"), Material.LIME_STAINED_GLASS_PANE,
					section.getString("name", " "));
			ConfigurationSection own = section.getConfigurationSection("unavailable-icon");
			ItemStack unavailableIcon = icon(own != null ? own : globalUnavailable, Material.BARRIER,
					DEFAULT_UNAVAILABLE_NAME);
			result.put(symbol, new SlotData(type, emptyIcon, unavailableIcon));
		}
		return result;
	}

	@SuppressWarnings("deprecation")
	private static ItemStack icon(@Nullable ConfigurationSection section, Material fallback, String defaultName) {
		Material material = fallback;
		int modelData = 0;
		String name = defaultName;
		List<String> lore = List.of();
		if (section != null) {
			String raw = section.getString("material");
			if (raw != null) {
				Material matched = Material.matchMaterial(raw);
				if (matched == null) warn("unknown material '" + raw + "', using " + fallback.name());
				else material = matched;
			}
			modelData = section.getInt("custom-model-data", 0);
			name = section.getString("name", defaultName);
			lore = section.getStringList("lore");
		}
		ItemStack stack = new ItemStack(material);
		ItemMeta meta = stack.getItemMeta();
		if (meta != null) {
			if (modelData > 0) meta.setCustomModelData(modelData);
			meta.displayName(ChatUtils.format(name).decoration(TextDecoration.ITALIC, false));
			if (!lore.isEmpty()) {
				meta.lore(lore.stream()
						.map(line -> ChatUtils.format(line).decoration(TextDecoration.ITALIC, false))
						.toList());
			}
			stack.setItemMeta(meta);
		}
		return stack;
	}

	private static void warn(String message) {
		PewpewLog.get().warning("[AttachmentMenu] " + FILE_NAME + ": " + message);
	}

	private static AttachmentMenuConfig loadOrSaveDefault() {
		File file = new File(PewpewPlugin.getInstance().getDataFolder(), FILE_NAME);
		if (!file.exists()) PewpewPlugin.getInstance().saveResource(FILE_NAME, false);
		return new AttachmentMenuConfig(YamlConfiguration.loadConfiguration(file));
	}

	/**
	 * Reads {@code attachment-menu.yml} again and registers any new slot types. This has to run before the
	 * item files are loaded, because guns and attachments refer to those types by name.
	 */
	public static void reloadConfig() {
		instance = loadOrSaveDefault();
	}

	public static AttachmentMenuConfig getInstance() {
		if (instance == null) reloadConfig();
		return instance;
	}

	public int getSize() { return size; }

	public String getTitle() { return title; }

	public ItemStack getMaskItem() { return maskItem; }

	/** Slot index of the gun preview, or -1 when it is disabled or not in the pattern. */
	public int getGunSlot() { return gunSlot; }

	public Map<Integer, SlotData> getSlotMapping() { return slotMapping; }

	public List<Integer> getMaskSlots() { return maskSlots; }

	/** {@code emptyIcon}: the gun has this slot and nothing is fitted. {@code unavailableIcon}: the gun has no such slot. */
	public record SlotData(AttachmentType type, ItemStack emptyIcon, ItemStack unavailableIcon) {}
}
