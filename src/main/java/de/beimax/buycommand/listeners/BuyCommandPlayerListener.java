/**
 * 
 */
package de.beimax.buycommand.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import de.beimax.buycommand.BuyCommand;
import de.beimax.buycommand.utils.UpdateChecker;

/**
 * @author mkalus
 * Listens to player events
 */
public class BuyCommandPlayerListener implements Listener {
	/**
	 * Check updates
	 * @param event
	 */
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		// update checker activated
		if (BuyCommand.getPlugin().getConfig().getBoolean("settings.updateNotificationOnLogin", true)) {
			final Player player = event.getPlayer();
			// Check for updates whenever an operator or user with the right buycommand.updatenotify joins the game
			if (player.isOp() || BuyCommand.checkPermission(player, "buycommand.updatenotify")) {
				(new Thread() { // create a new anonymous thread that will check the version asyncronously
					@Override
					public void run() {
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
				}).start();
			}
		}
	}

	/**
	 * preprocess commands
	 * @param event
	 */
	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		// let the processor do all the work
		BuyCommand.getProcessor().processCommand(event);
	}
}
