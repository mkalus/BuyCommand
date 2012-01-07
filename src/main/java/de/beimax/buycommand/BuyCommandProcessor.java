/**
 * 
 */
package de.beimax.buycommand;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * @author mkalus
 *
 */
public class BuyCommandProcessor {
	/**
	 * core method - called by listener to process event
	 * @param event
	 */
	public void processCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		
		// sanity check
		if(player == null) return;

		// player is in group which is not charged for commands?
		if (BuyCommand.checkPermission(player, "buycommand.free")) return;
		
		// now cycle through commands an check whether it belongs to one the can be charged for
		for(String commandKey : BuyCommand.getPlugin().getConfig().getConfigurationSection("commands").getKeys(false)) {
			// get pattern to match
			String pattern = BuyCommand.getPlugin().getConfig().getString("commands." + commandKey + ".command");
			if (pattern == null) {
				BuyCommand.log.warning("[BuyCommand] Command with key " + commandKey + " is missing a command pattern. Please correct your config!");
				continue;
			}
			
			// compile and match pattern
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(event.getMessage());
			if(m.find()) { // found a matching pattern!
				// get cost
				double cost = BuyCommand.getPlugin().getConfig().getDouble("commands." + commandKey + ".cost", Double.MAX_VALUE);
				if (cost == Double.MAX_VALUE) {
					BuyCommand.log.warning("[BuyCommand] Cost of command with key " + commandKey + " is not a correct number. Please correct your config!");
					continue;
				}
				// if command does not cost anything, continue
				if (cost == 0) {
					BuyCommand.log.info("[BuyCommand] Cost of command with key " + commandKey + " is 0. Skipping it!");
					continue;
				}
				
				// costs ok, now check if there is an exception or only group
				if (checkException(player, commandKey)) continue;
				if (BuyCommand.getPlugin().getConfig().contains("commands." + commandKey + ".only") && !checkOnly(player, commandKey)) continue;
				
				// this command costs something!
				if (BuyCommand.economy.hasAccount(player.getName())) {
					if (cost > 0) { // deducting costs
						String amount = BuyCommand.economy.format(cost);
						double balance = BuyCommand.economy.getBalance(player.getName());
						if (cost > balance) { // insufficient funds
							player.sendMessage(ChatColor.DARK_RED + BuyCommand.ll("errors.insufficientFunds", "[AMOUNT]", amount, "[BANK]", BuyCommand.economy.format(balance)));
							event.setCancelled(true);
						} else { // deduct from bank account
							BuyCommand.economy.withdrawPlayer(player.getName(), cost); // withdraw money
							player.sendMessage(ChatColor.RED + BuyCommand.ll("feedback.withdraw", "[AMOUNT]", amount));
						}
					} else { // paying commands
						String amount = BuyCommand.economy.format(-cost);
						BuyCommand.economy.depositPlayer(player.getName(), -cost); // pay money
						player.sendMessage(ChatColor.GREEN + BuyCommand.ll("feedback.pay", "[AMOUNT]", amount));
					}
				} else { // no account
					player.sendMessage(ChatColor.DARK_RED + BuyCommand.ll("errors.noAccount"));
					event.setCancelled(true);
				}
			}
		}
	}

	/**
	 * check for exception groups of a specific key
	 * @param player
	 * @param commandKey
	 * @return
	 */
	private boolean checkException(Player player, String commandKey) {
		return checkPath(player, "commands." + commandKey + ".except");
	}

	/**
	 * check for only groups of a specific key
	 * @param player
	 * @param commandKey
	 * @return
	 */
	private boolean checkOnly(Player player, String commandKey) {
		return checkPath(player, "commands." + commandKey + ".only");
	}
	
	/**
	 * helper method for checkException and checkOnly
	 * @param player
	 * @param path
	 * @return
	 */
	private boolean checkPath(Player player, String path) {
		// get list
		if (BuyCommand.getPlugin().getConfig().isList(path)) { // list of permissions
			// go through all of them
			for (Object permission : BuyCommand.getPlugin().getConfig().getStringList(path))
			if (permission == null || !(permission instanceof String)) { // error?
				BuyCommand.log.warning("[BuyCommand] Invalid permission key in " + path + ". Please correct your config!");
				continue;
			} else // check single permission
				if (BuyCommand.checkPermission(player, (String) permission)) return true;
		} else if (BuyCommand.getPlugin().getConfig().isString(path)) {
			// check single string permission
			return BuyCommand.checkPermission(player, BuyCommand.getPlugin().getConfig().getString(path));
		}
		return false;
	}
}
