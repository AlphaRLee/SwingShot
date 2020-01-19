package io.github.alpharlee.swingshot;

import io.github.alpharlee.swingshot.item.ItemHandler;
import io.github.alpharlee.swingshot.item.SwingingRopeItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	private static Main instance;

	private ServerEventListener eventListener;
	private SwingTaskRegistry swingTaskRegistry;

	@Override
	public void onEnable() {
		instance = this;

		eventListener = new ServerEventListener();
		getServer().getPluginManager().registerEvents(eventListener, this);

		ItemHandler.getInstance(); // Force loadup of static

		swingTaskRegistry = new SwingTaskRegistry();
		MasterRunnable.getInstance().addTaskCollection(swingTaskRegistry);
		MasterRunnable.getInstance().runTaskTimer(this, 1l, 1l);
	}

	@Override
	public void onDisable() {

	}

	public static Main getInstance() {
		return instance;
	}

	public SwingTaskRegistry getSwingTaskRegistry() {
		return swingTaskRegistry;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().toLowerCase().equals("swingshot")) {
			//TODO Implement commands here
			if (!(sender instanceof Player)) {
				sender.sendMessage("Coming soon! SwingShot for server consoles like you!");
				return true;
			}

			Player player = (Player) sender;

			if (args.length < 1) {
				sender.sendMessage(ChatColor.RED + "Use /swingshot help");
				return true;
			}

			switch (args[0].toLowerCase()) {
				case "rope":
					giveRopeCommand(player);
					break;
				case "help": case "?":
					player.sendMessage("TODO - Add a help guide for SwingShot"); // TODO Add a help guide for SwingShot
					break;
				case "debug":
					debugCommand(sender, args);
					break;
			}
		}

		return true;
	}

	// TODO Move elsewhere
	public void giveRopeCommand(Player player) {
		player.getInventory().addItem(new ItemStack(SwingingRopeItem.templateItem));
		player.sendMessage(ChatColor.LIGHT_PURPLE + "!!! Here's a new rope!");
	}

	// FIXME delete this function
	public void debugCommand(CommandSender sender, String[] args) {
		if (args.length < 2) {
			sender.sendMessage("!!! No debug arg given. Reminder to delete later");
		}

		switch (args[1].toLowerCase()) {
			case "lvm": case "linearvelocitymulitplier":
				if (args.length > 2) {
					SwingPhysicsTask.linearVelocityMultiplier = Double.parseDouble(args[2]);
					sender.sendMessage("!!! linear velocity multiplier of " + args[2] + " applied");
				} else {
					SwingPhysicsTask.linearVelocityMultiplier = 0.5;
					sender.sendMessage("!!! Default linear velocity multiplier of 0.5 applied");
				}

				break;

			case "spd": case "spdebug" : case "swingphysicsdebug":
				SwingPhysicsTask.printDebug = !SwingPhysicsTask.printDebug;
				sender.sendMessage("!!! SwingPhysicsTask.printDebug toggled to: " + SwingPhysicsTask.printDebug);
				break;

			case "steps":
				if (args.length > 2) {
					SwingPhysicsTask.stepsPerTick = Integer.parseInt(args[2]);
					sender.sendMessage("!!! steps per tick of " + args[2] + " applied");
				} else {
					SwingPhysicsTask.stepsPerTick = 5;
					sender.sendMessage("!!! Default steps per tick of 5 applied");
				}
		}
	}
}
