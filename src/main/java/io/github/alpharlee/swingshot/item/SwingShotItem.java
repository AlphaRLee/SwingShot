package io.github.alpharlee.swingshot.item;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public abstract class SwingShotItem {
	private ItemStack itemStack;

	public SwingShotItem(ItemStack itemStack) {
		this.itemStack = itemStack;
	}

	/**
	 * Get the item stack associated with this item
	 * @return
	 *
	 * @author R Lee
	 */
	public ItemStack getItemStack() {
		return this.itemStack;
	}

	public boolean onLeftClickAir(PlayerInteractEvent event) { return false; }
	public boolean onLeftClickBlock(PlayerInteractEvent event) { return false; }
	public boolean onRightClickAir(PlayerInteractEvent event) { return false; }
	public boolean onRightClickBlock(PlayerInteractEvent event) { return false; }
}
