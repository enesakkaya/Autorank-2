package me.armar.plugins.autorank.playerdatahandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.api.events.RequirementCompleteEvent;
import me.armar.plugins.autorank.playerchecker.result.Result;
import me.armar.plugins.autorank.rankbuilder.ChangeGroup;
import me.armar.plugins.autorank.rankbuilder.holders.RequirementsHolder;

/**
 * PlayerDataHandler will keep track of the latest known group and progress a
 * player made (via /ar complete)
 * When the last known group is not equal to the current group of a player, all
 * progress should be reset as a player is not longer in the same group.
 * 
 * PlayerDataHandler uses a file (/playerdata/playerdata.yml) which keeps
 * tracks of these things.
 * 
 * @author Staartvin
 * 
 */
public class PlayerDataHandler {

	private FileConfiguration config;
	private File configFile;
	private boolean convertingData = false;

	private final Autorank plugin;

	public PlayerDataHandler(final Autorank instance) {
		this.plugin = instance;

		// Start requirement saver task
		// Run save task every 2 minutes
		plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				saveConfig();
			}
		}, 1200, 2400);
	}

	public void addCompletedRanks(final UUID uuid, final String rank) {
		final List<String> completed = getCompletedRanks(uuid);

		completed.add(rank);

		setCompletedRanks(uuid, completed);
	}

	public void addPlayerProgress(final UUID uuid, final int reqID) {
		final List<Integer> progress = getProgress(uuid);

		if (hasCompletedRequirement(reqID, uuid))
			return;

		progress.add(reqID);

		setPlayerProgress(uuid, progress);
	}

	public void convertNamesToUUIDs() {

		if (convertingData)
			return;

		convertingData = true;

		plugin.getLogger().info("Starting to convert playerdata.yml");

		// Run async to prevent problems.
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

			@Override
			public void run() {
				// Backup beforehand
				plugin.getBackupManager().backupFile("/playerdata/playerdata.yml", null);

				for (final String name : getConfig().getKeys(false)) {

					// Probably UUID because names don't have dashes.
					if (name.contains("-"))
						continue;

					final UUID uuid = plugin.getUUIDStorage().getStoredUUID(name);

					if (uuid == null)
						continue;

					final List<Integer> progress = config.getIntegerList(name + ".progress");
					final String lastKnownGroup = config.getString(name + ".last group");

					// Remove name
					config.set(name, null);

					// Replace name with UUID
					config.set(uuid.toString() + ".progress", progress);
					config.set(uuid.toString() + ".last group", lastKnownGroup);
				}

				plugin.getLogger().info("Converted playerdata.yml to UUID format");
			}
		});
	}

	public void createNewFile() {
		reloadConfig();
		saveConfig();
		loadConfig();

		// Convert old format to new UUID storage format
		//convertNamesToUUIDs();

		plugin.getLogger().info("Loaded playerdata.");
	}

	private List<String> getCompletedRanks(final UUID uuid) {
		final List<String> completed = config.getStringList(uuid.toString() + ".completed ranks");

		return completed;
	}

	public FileConfiguration getConfig() {
		if (config == null) {
			this.reloadConfig();
		}
		return config;
	}

	public String getLastKnownGroup(final UUID uuid) {
		//Validate.notNull(uuid, "UUID of a player is null!");

		//UUID uuid = UUIDManager.getUUIDFromPlayer(playerName);
		return config.getString(uuid.toString() + ".last group");
	}

	@SuppressWarnings("unchecked")
	public List<Integer> getProgress(final UUID uuid) {
		//UUID uuid = UUIDManager.getUUIDFromPlayer(playerName);
		//Validate.notNull(uuid, "UUID of a player is null!");

		return (List<Integer>) config.getList(uuid.toString() + ".progress", new ArrayList<Integer>());
	}

	public boolean hasCompletedRank(final UUID uuid, final String rank) {
		// If player can rank up forever on the same rank, we will always return false.
		// Fixed issue #134
		if (plugin.getConfigHandler().allowInfiniteRanking()) {
			return false;
		}

		return getCompletedRanks(uuid).contains(rank);
	}

	public boolean hasCompletedRequirement(final int reqID, final UUID uuid) {
		final List<Integer> progress = getProgress(uuid);

		return progress.contains(reqID);
	}

	public void loadConfig() {

		config.options().header("This file contains all the data Autorank needs of players");

		config.options().copyDefaults(true);
		saveConfig();
	}

	@SuppressWarnings("deprecation")
	public void reloadConfig() {
		if (configFile == null) {
			configFile = new File(plugin.getDataFolder() + "/playerdata", "playerdata.yml");
		}
		config = YamlConfiguration.loadConfiguration(configFile);

		// Look for defaults in the jar
		final InputStream defConfigStream = plugin.getResource("playerdata.yml");
		if (defConfigStream != null) {
			final YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			config.setDefaults(defConfig);
		}
	}

	public void runResults(final RequirementsHolder holder, final Player player) {

		// Fire event so it can be cancelled
		// Create the event here/
		// TODO Implement logic for events with RequirementHolder
		final RequirementCompleteEvent event = new RequirementCompleteEvent(player, holder);
		// Call the event
		Bukkit.getServer().getPluginManager().callEvent(event);

		// Check if event is cancelled.
		if (event.isCancelled())
			return;

		// Run results
		final List<Result> results = holder.getResults();

		// Apply result
		for (final Result realResult : results) {
			realResult.applyResult(player);
		}
	}

	public void saveConfig() {
		if (config == null || configFile == null) {
			return;
		}
		try {
			getConfig().save(configFile);
		} catch (final IOException ex) {
			plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
		}
	}

	public void setCompletedRanks(final UUID uuid, final List<String> completedRanks) {
		config.set(uuid.toString() + ".completed ranks", completedRanks);
	}

	public void setLastKnownGroup(final UUID uuid, final String group) {
		//UUID uuid = UUIDManager.getUUIDFromPlayer(playerName);
		config.set(uuid.toString() + ".last group", group);
	}

	public void setPlayerProgress(final UUID uuid, final List<Integer> progress) {
		//UUID uuid = UUIDManager.getUUIDFromPlayer(playerName);

		config.set(uuid.toString() + ".progress", progress);
	}

	public boolean hasLeaderboardExemption(final UUID uuid) {
		//Validate.notNull(uuid, "UUID of a player is null!");
		return config.getBoolean(uuid.toString() + ".exempt leaderboard", false);
	}

	public void hasLeaderboardExemption(final UUID uuid, final boolean value) {
		config.set(uuid.toString() + ".exempt leaderboard", value);
	}

	public void setChosenPath(final UUID uuid, final String path) {
		config.set(uuid.toString() + ".chosen path", path);
	}

	public String getChosenPath(final UUID uuid) {
		return config.getString(uuid.toString() + ".chosen path", "unknown");
	}

	public boolean checkValidChosenPath(final Player player) {

		final String groupName = plugin.getPermPlugHandler().getPrimaryGroup(player);
		final String chosenPath = this.getChosenPath(player.getUniqueId());

		final List<ChangeGroup> changeGroups = plugin.getPlayerChecker().getChangeGroupManager()
				.getChangeGroups(groupName);

		boolean validChosenPath = false;

		// Check whether the chosen path equals one of the change groups
		for (final ChangeGroup group : changeGroups) {
			if (group.getInternalGroup().equals(chosenPath)) {
				validChosenPath = true;
			}
		}

		if (!validChosenPath) {
			// Somehow there wrong chosen path was still left over. Remove it.
			plugin.getPlayerDataHandler().setChosenPath(player.getUniqueId(), null);
		}

		return validChosenPath;

	}
}
