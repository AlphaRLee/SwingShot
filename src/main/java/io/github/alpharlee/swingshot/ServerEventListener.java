package io.github.alpharlee.swingshot;

import io.github.alpharlee.swingshot.item.ItemHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class ServerEventListener implements Listener {
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		ItemHandler.getInstance().onItemInteract(event);
	}
}
