package io.github.alpharlee.swingshot.item;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ItemMetaBuilder {
	/**
	 * Set the meta for an item stack (cosmetic). Item will default to unbreakable and always hide enchants and unbreakable flags.
	 * @param item ItemStack to set meta for
	 * @param displayName Display name
	 * @param lore Lore
	 * @param addEnchantShine Set to true to add unbreaking X to item (give a shiny luster)
	 * @return The modified ItemStack
	 */
	public static ItemStack setItemStackMeta(ItemStack item, String displayName, String[] lore, boolean addEnchantShine)
	{
		Map<Enchantment, Integer> enchants = new HashMap<Enchantment, Integer>();

		if (addEnchantShine)
		{
			//Random enchant. Not terribly important which one, since it will be hidden anyway
			enchants.put(Enchantment.DURABILITY, 10);
		}

		return setItemStackMeta(item, displayName, lore, enchants, true, true);
	}

	/**
	 * Set the meta for an item stack
	 * @param item ItemStack to set meta for
	 * @param displayName Display name
	 * @param lore Lore
	 * @param enchants Key represents desired enchant, values represents level. Will always aloow enchants beyond the maximum conventional level
	 * @param unbreakable Set to true for unbreakable
	 * @param hideFlags Set to true to hide enchants and unbreakable flag
	 * @return The modified ItemStack
	 */
	public static ItemStack setItemStackMeta(ItemStack item, String displayName, String[] lore, Map<Enchantment, Integer> enchants, boolean unbreakable, boolean hideFlags) {
		ItemMeta itemMeta = item.getItemMeta();

		itemMeta.setDisplayName(displayName);
		itemMeta.setLore(new ArrayList<String>(Arrays.asList(lore)));

		for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
			itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);
		}

		itemMeta.setUnbreakable(unbreakable);

		if (hideFlags) {
			itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			itemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
		}

		item.setItemMeta(itemMeta);
		return item;
	}
}
