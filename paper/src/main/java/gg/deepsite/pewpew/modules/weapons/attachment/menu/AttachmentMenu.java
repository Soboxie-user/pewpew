package gg.deepsite.pewpew.modules.weapons.attachment.menu;

import com.jazzkuh.inventorylib.objects.icon.Icon;
import gg.deepsite.pewpew.api.enums.AttachmentType;
import gg.deepsite.pewpew.api.enums.SoundEvent;
import gg.deepsite.pewpew.api.events.PewpewAttachmentEvent;
import gg.deepsite.pewpew.api.objects.PewPewItem;
import gg.deepsite.pewpew.api.objects.PewpewGunItem;
import gg.deepsite.pewpew.api.objects.attachment.PewpewAttachment;
import gg.deepsite.pewpew.modules.weapons.attachment.AttachmentUtil;
import gg.deepsite.pewpew.modules.weapons.lore.GunLoreRenderer;
import gg.deepsite.pewpew.utils.ChatUtils;
import gg.deepsite.pewpew.utils.Sounds;
import gg.deepsite.pewpew.utils.item.ItemBuilder;
import gg.deepsite.pewpew.utils.item.ItemFactory;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class AttachmentMenu extends AbstractGunMenu {

	private final AttachmentMenuConfig config;

	public AttachmentMenu(Player viewer, int gunSlot) {
		this(viewer, gunSlot, AttachmentMenuConfig.getInstance());
	}

	public AttachmentMenu(Player viewer, int gunSlot, AttachmentMenuConfig config) {
		// Plain MiniMessage rather than ChatUtils, so <font> tags in the title pass through untouched.
		super(MiniMessage.miniMessage().deserialize(config.getTitle()), config.getSize() / 9, viewer, gunSlot);
		this.config = config;
		build();
	}

	private void build() {
		clearItems();
		ItemStack gun = gunStack();
		if (gun == null) return;
		
		PewpewGunItem gunItem = (PewpewGunItem) itemsModule().fromItemStack(gun);
		List<AttachmentType> allowed = gunItem.getAllowedAttachmentSlots();

		// Mask tiles
		for (int slot : config.getMaskSlots()) {
			addItem(new Icon(slot, config.getMaskItem().clone()));
		}

		// Gun preview, with its live lore
		if (config.getGunSlot() != -1) {
			addItem(new Icon(config.getGunSlot(), generatePreviewGun(gun)));
		}

		// Slots from the pattern: fitted attachment, empty icon, or the unavailable icon where the gun has no such slot
		for (Map.Entry<Integer, AttachmentMenuConfig.SlotData> entry : config.getSlotMapping().entrySet()) {
			int slotIndex = entry.getKey();
			AttachmentType type = entry.getValue().type();
			
			if (allowed == null || !allowed.contains(type)) {
				addItem(new Icon(slotIndex, entry.getValue().unavailableIcon().clone()));
				continue;
			}

			PewpewAttachment fitted = AttachmentUtil.get(gun, type);
			if (fitted != null) {
				if (gunItem.isForcedSlot(type)) {
					addItem(new Icon(slotIndex, lockedAttachment(fitted)));
				} else {
					addItem(new Icon(slotIndex, ItemFactory.build(fitted), true, event -> detach(type)));
				}
			} else {
				addItem(new Icon(slotIndex, entry.getValue().emptyIcon().clone()));
			}
		}
	}

	private ItemStack generatePreviewGun(ItemStack gun) {
		ItemStack preview = gun.clone();
		return preview;
	}

	@Override
	public void onClick(InventoryClickEvent event) {
		if (!event.isShiftClick()) return;
		Inventory clicked = event.getClickedInventory();
		if (clicked == null || clicked.equals(getInventory())) return;

		ItemStack stack = event.getCurrentItem();
		if (stack == null) return;
		PewPewItem item = itemsModule().fromItemStack(stack);
		if (item instanceof PewpewAttachment attachment) fit(attachment);
	}

	private void fit(PewpewAttachment attachment) {
		ItemStack gun = gunStack();
		if (gun == null) {
			viewer.closeInventory();
			viewer.sendMessage(ChatUtils.prefix("<error>You must hold the gun to edit its attachments."));
			return;
		}

		PewpewGunItem gunItem = (PewpewGunItem) itemsModule().fromItemStack(gun);
		AttachmentType type = attachment.getSlot();
		if (gunItem.getAllowedAttachmentSlots() == null || !gunItem.getAllowedAttachmentSlots().contains(type)) {
			viewer.sendMessage(ChatUtils.prefix("<error>This gun has no " + label(type) + " slot."));
			return;
		}

		if (gunItem.isForcedSlot(type)) {
			viewer.sendMessage(ChatUtils.prefix("<error>The " + label(type) + " slot is locked and cannot be changed."));
			return;
		}

		if (!consumeOne(attachment.getId())) return;

		if (!new PewpewAttachmentEvent(viewer, gunOf(gun), gun, attachment, type, true).callEvent()) return;

		PewpewAttachment existing = AttachmentUtil.get(gun, type);
		if (existing != null) giveBack(existing);

		AttachmentUtil.set(gun, type, attachment.getId());
		GunLoreRenderer.refresh(gun);
		writeBack(gun);
		Sounds.at(viewer, SoundEvent.MENU_EQUIP);
		refresh();
	}

	private void detach(AttachmentType type) {
		ItemStack gun = gunStack();
		if (gun == null) {
			viewer.closeInventory();
			viewer.sendMessage(ChatUtils.prefix("<error>You must hold the gun to edit its attachments."));
			return;
		}

		PewpewGunItem gunItem = (PewpewGunItem) itemsModule().fromItemStack(gun);
		if (gunItem.isForcedSlot(type)) {
			viewer.sendMessage(ChatUtils.prefix("<error>This " + label(type) + " is locked and cannot be removed."));
			return;
		}

		PewpewAttachment fitted = AttachmentUtil.get(gun, type);
		if (fitted == null) return;
		if (!new PewpewAttachmentEvent(viewer, gunOf(gun), gun, fitted, type, false).callEvent()) return;

		AttachmentUtil.clear(gun, type);
		GunLoreRenderer.refresh(gun);
		writeBack(gun);
		giveBack(fitted);
		Sounds.at(viewer, SoundEvent.MENU_UNEQUIP);
		refresh();
	}

	private void refresh() {
		build();
		update();
	}

	private boolean consumeOne(String attachmentId) {
		ItemStack[] contents = viewer.getInventory().getStorageContents();
		for (int i = 0; i < contents.length; i++) {
			ItemStack stack = contents[i];
			if (stack == null) continue;
			PewPewItem item = itemsModule().fromItemStack(stack);
			if (item instanceof PewpewAttachment attachment && attachment.getId().equals(attachmentId)) {
				stack.setAmount(stack.getAmount() - 1);
				if (stack.getAmount() <= 0) contents[i] = null;
				viewer.getInventory().setStorageContents(contents);
				return true;
			}
		}
		return false;
	}

	private ItemStack lockedAttachment(PewpewAttachment attachment) {
		ItemStack stack = ItemFactory.build(attachment);
		return new ItemBuilder(stack)
				.addLoreLine("")
				.addLoreLine("<red>🔒 Locked <dark_gray>| built into this weapon")
				.toItemStack();
	}

	@Nullable
	private PewpewGunItem gunOf(ItemStack stack) {
		return itemsModule().fromItemStack(stack) instanceof PewpewGunItem gun ? gun : null;
	}
}
