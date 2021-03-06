package me.armar.plugins.autorank.commands;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.commands.manager.AutorankCommand;
import me.armar.plugins.autorank.language.Lang;
import me.armar.plugins.autorank.playtimes.Playtimes.dataType;

public class LeaderboardCommand extends AutorankCommand {

	private final Autorank plugin;

	public LeaderboardCommand(final Autorank instance) {
		this.setUsage("/ar leaderboard <type>");
		this.setDesc("Show the leaderboard.");
		this.setPermission("autorank.leaderboard");

		plugin = instance;
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {

		if (!plugin.getCommandsManager().hasPermission("autorank.leaderboard", sender)) {
			return true;
		}

		// Whether to broadcast
		boolean broadcast = false;
		boolean force = false;

		for (final String arg : args) {
			if (arg.equalsIgnoreCase("force")) {

				// Check for permission
				if (!sender.hasPermission("autorank.leaderboard.force")) {
					sender.sendMessage(Lang.NO_PERMISSION.getConfigValue("autorank.leaderboard.force"));
					return true;
				}

				force = true;
			} else if (arg.equalsIgnoreCase("broadcast")) {

				// Check for permission
				if (!sender.hasPermission("autorank.leaderboard.broadcast")) {
					sender.sendMessage(Lang.NO_PERMISSION.getConfigValue("autorank.leaderboard.broadcast"));
					return true;
				}

				broadcast = true;
			}
		}

		String leaderboardType = "total";
		dataType type = null;

		if (args.length > 1 && !args[1].equalsIgnoreCase("force") && !args[1].equalsIgnoreCase("broadcast")) {
			leaderboardType = args[1].toLowerCase();
		}

		if (leaderboardType.equalsIgnoreCase("total")) {
			type = dataType.TOTAL_TIME;
		} else if (leaderboardType.equalsIgnoreCase("daily") || leaderboardType.contains("day")) {
			type = dataType.DAILY_TIME;
		} else if (leaderboardType.contains("week")) {
			type = dataType.WEEKLY_TIME;
		} else if (leaderboardType.contains("month")) {
			type = dataType.MONTHLY_TIME;
		}

		if (type == null) {
			sender.sendMessage(Lang.INVALID_LEADERBOARD_TYPE.getConfigValue());
			return true;
		}

		if (force) {
			// Forcely update leaderboard first.
			plugin.getLeaderboard().updateLeaderboard(type);
		}

		if (!broadcast) {
			plugin.getLeaderboard().sendLeaderboard(sender, type);
		} else {
			plugin.getLeaderboard().broadcastLeaderboard(type);
		}

		return true;
	}

	/* (non-Javadoc)
	 * @see me.armar.plugins.autorank.commands.manager.AutorankCommand#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
	 */
	@Override
	public List<String> onTabComplete(final CommandSender sender, final Command cmd, final String commandLabel,
			final String[] args) {
		// TODO Auto-generated method stub
		return null;
	}

}
