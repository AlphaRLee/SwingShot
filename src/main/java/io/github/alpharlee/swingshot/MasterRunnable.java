package io.github.alpharlee.swingshot;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MasterRunnable extends BukkitRunnable {
	private static MasterRunnable ourInstance = new MasterRunnable();
	private Set<TaskCollection> taskCollections;

	public static MasterRunnable getInstance() {
		return ourInstance;
	}

	private MasterRunnable() {
		taskCollections = new HashSet<>();
	}

	@Override
	public void run() {
		for (TaskCollection taskCollection : taskCollections) {
			taskCollection.runTasks();
		}
	}

	public void addTaskCollection(TaskCollection taskCollection) {
		taskCollections.add(taskCollection);
	}
}
