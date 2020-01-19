package io.github.alpharlee.swingshot;

import com.sun.javafx.tk.Toolkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class SwingTaskRegistry implements TaskCollection {
	private Map<Player, SwingPhysicsTask> physicsTasks;

	public SwingTaskRegistry() {
		physicsTasks = new HashMap<>();
	}

	public void runTasks() {
		for (SwingPhysicsTask task : physicsTasks.values()) {
			task.run();
		}
	}

	public boolean hasTask(Player player) {
		return physicsTasks.containsKey(player);
	}

	public void startPhysicsTask(Player player, Location pivot) {
		SwingPhysicsTask task = new SwingPhysicsTask(player, pivot);
		physicsTasks.put(player, task);
	}

	public void stopPhysicsTask(Player player) {
		physicsTasks.remove(player);
	}

	public SwingPhysicsTask getPhysicsTask(Player player) {
		return physicsTasks.get(player);
	}
}
