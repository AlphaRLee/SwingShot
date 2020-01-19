package io.github.alpharlee.swingshot.item;

import io.github.alpharlee.swingshot.Main;
import io.github.alpharlee.swingshot.SwingPhysicsTask;
import io.github.alpharlee.swingshot.SwingTaskRegistry;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class SwingingRopeItem extends SwingShotItem {
	public static ItemStack templateItem;

	public SwingingRopeItem(ItemStack itemStack) {
		super(itemStack);
	}

	@Override
	public boolean onLeftClickBlock(PlayerInteractEvent event) { return onLeftClickAir(event); }

	@Override
	public boolean onLeftClickAir(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		SwingTaskRegistry taskRegistry = Main.getInstance().getSwingTaskRegistry();
		SwingPhysicsTask physicsTask = taskRegistry.getPhysicsTask(player);
		if (physicsTask == null) {
			launchRope(taskRegistry, player);
		} else {
			Location pivot = physicsTask.getPivot();
			Vector boostToPivot = pivot.subtract(physicsTask.getPlayerSwingLocation()).toVector().normalize();
			player.setVelocity(player.getVelocity().add(boostToPivot));

			taskRegistry.stopPhysicsTask(player);
		}

		return true;
	}

	@Override
	public boolean onRightClickBlock(PlayerInteractEvent event) {
		return onRightClickAir(event);
	}

	@Override
	public boolean onRightClickAir(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		SwingTaskRegistry taskRegistry = Main.getInstance().getSwingTaskRegistry();
		if (taskRegistry.hasTask(player)) {
			taskRegistry.stopPhysicsTask(player);

			if (player.isSprinting()) {
				Vector endBoost = player.getVelocity().multiply(1.3); // TODO replace magic
				player.setVelocity(endBoost);

//				player.sendMessage(String.format("!!! Player left sprinting. Boost: %.3f, %.3f, %.3f", endBoost.getX(), endBoost.getY(), endBoost.getZ())); // FIXME delete
			}

		} else {
			launchRope(taskRegistry, player);
		}

		return true;
	}

	private void launchRope(SwingTaskRegistry taskRegistry, Player player) {
		Location pivot = getPlayerTargetLocation(player);
		if (pivot != null) {
			taskRegistry.startPhysicsTask(player, pivot);
		}
	}

	private Location getPlayerTargetLocation(Player player) {
		int maxDistance = 30; // TODO set as configurable

		Set<Material> clearMaterials = new HashSet<>();
		clearMaterials.add(Material.AIR);
		clearMaterials.add(Material.WATER);

		Vector playerDirection = player.getEyeLocation().getDirection();

		Block targetBlock = player.getTargetBlock(clearMaterials, maxDistance);
		if (targetBlock == null) {
			return null;
		}

		RayTraceResult rayTraceResult = targetBlock.rayTrace(player.getEyeLocation(), playerDirection, maxDistance, FluidCollisionMode.NEVER);
		if (rayTraceResult == null
				|| rayTraceResult.getHitPosition() == null) { // TODO find which line is throwing a nullpointerexception
			return null;
		}

		return rayTraceResult.getHitPosition().toLocation(player.getWorld());
	}
}
