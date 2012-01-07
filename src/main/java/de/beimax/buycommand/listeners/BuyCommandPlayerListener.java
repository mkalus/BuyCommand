/**
 * 
 */
package de.beimax.buycommand.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;

import de.beimax.buycommand.BuyCommand;
import de.beimax.buycommand.utils.UpdateChecker;

/**
 * @author mkalus
 * Listens to player events
 */
public class BuyCommandPlayerListener extends PlayerListener {
	/* (non-Javadoc)
	 * @see org.bukkit.event.player.PlayerListener#onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent)
	 */
	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		// update checker activated
		if (BuyCommand.getPlugin().getConfig().getBoolean("settings.updateNotificationOnLogin", true)) {
			Player player = event.getPlayer();
			// Check for updates whenever an operator or user with the right buycommand.updatenotify joins the game
			if (player.isOp() || BuyCommand.checkPermission(player, "buycommand.updatenotify")) {
				UpdateChecker checker = new UpdateChecker();
				try {
					// compare versions
					String oldVersion = BuyCommand.getPlugin().getDescription().getVersion();
					String newVersion = checker.checkForUpdate(oldVersion);
					if (newVersion != null) // do we have a version update? => notify player
						player.sendMessage(BuyCommand.ll("feedback.update", "[OLDVERSION]", oldVersion, "[NEWVERSION]", newVersion));
				} catch (Exception e) {
					player.sendMessage("BuyCommand could not get version update - see log for details.");
					BuyCommand.log.warning("[BuyCommand] Could not connect to remote server to check for update. Exception said: " + e.getMessage());
				}
			}
		}
	}


	/* (non-Javadoc)
	 * @see org.bukkit.event.player.PlayerListener#onPlayerCommandPreprocess(org.bukkit.event.player.PlayerCommandPreprocessEvent)
	 */
	@Override
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		// let the processor do all the work
		BuyCommand.getProcessor().processCommand(event);
	}
}