package io.github.alpharlee.swingshot.item;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ItemHandler {
	private static ItemHandler instance = new ItemHandler();
	private Map<ItemStack, Class<? extends SwingShotItem>> swingShotItems;

	public static ItemHandler getInstance() {
		return instance;
	}

	private ItemHandler() {
		swingShotItems = new HashMap<>(); // <ItemStack, Class<? extends SwingShotItem>>

		// TODO Move to an enum or file loader in the future
		{
			ItemStack ropeTemplateItem = new ItemStack(Material.SEA_PICKLE);
			ItemMetaBuilder.setItemStackMeta(ropeTemplateItem, ChatColor.YELLOW + " " + ChatColor.ITALIC + "SwingShot", new String[]{
					ChatColor.YELLOW + "Right-click on a block",
					ChatColor.YELLOW + "And swing around!"
			}, false);
			SwingingRopeItem.templateItem = ropeTemplateItem.clone();
			addSwingShotItem(ropeTemplateItem, SwingingRopeItem.class);
		}
	}

	void addSwingShotItem(ItemStack itemStack, Class<? extends SwingShotItem> clazz) {
		swingShotItems.put(itemStack, clazz);
	}

	public void onItemInteract(PlayerInteractEvent event) {
		boolean shouldCancel = false;

		SwingShotItem ssItem = getSwingShotItem(event.getPlayer().getInventory().getItemInMainHand());
		if (ssItem == null) {
			return;
		}

		switch (event.getAction()) {
			case LEFT_CLICK_AIR:
				shouldCancel = ssItem.onLeftClickAir(event);
				break;
			case LEFT_CLICK_BLOCK:
				shouldCancel = ssItem.onLeftClickBlock(event);
				break;
			case RIGHT_CLICK_AIR:
				shouldCancel = ssItem.onRightClickAir(event);
				break;
			case RIGHT_CLICK_BLOCK:
				shouldCancel = ssItem.onRightClickBlock(event);
				break;
			default:
				return;
		}

		if (shouldCancel) {
			event.setCancelled(true);
		}
	}

	/**
	 * Get the SwingShotItem associated to the given itemStack
	 * @param itemStack the in-game item representation of the SwingShotItem.
	 * @return the SwingShotItem or null if no corresponding item found.
	 */
	private SwingShotItem getSwingShotItem(ItemStack itemStack) {
		ItemStack freshCopy = itemStack.clone();
		freshCopy.setAmount(1);
		if (freshCopy instanceof Damageable) {
			((Damageable) freshCopy).setDamage(0);
		}

		Class<? extends SwingShotItem> ssClass = swingShotItems.get(freshCopy);
		if (ssClass == null) {
			return null;
		}

		try {
			return ssClass.getDeclaredConstructor(ItemStack.class).newInstance(itemStack);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		return null;
	}
}
