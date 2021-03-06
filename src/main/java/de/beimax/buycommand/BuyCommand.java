/**
 * 
 */
package de.beimax.buycommand;

import java.util.Map;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import de.beimax.buycommand.listeners.BuyCommandPlayerListener;
import de.beimax.buycommand.utils.*;

/**
 * @author mkalus
 *
 */
public class BuyCommand extends JavaPlugin {
	public static final Logger log = Logger.getLogger("Minecraft");
	
	/**
	 * self reference to singleton
	 */
	private static BuyCommand buyCommand;
	
	/**
	 * get instance
	 * @return
	 */
	public static BuyCommand getPlugin() {
		return buyCommand;
	}

	/**
	 * reference to translator
	 */
	private static Translator lang;

	/**
	 * @param key of translation file
	 * @param replacers an even number of key/value pairs to replace key entries
	 * @return translated string
	 */
	public static String ll(String key, String... replacers) {
		return lang.ll(key, replacers);
	}

	/**
	 * Get translation section list
	 * @param key
	 * @return
	 */
	public static Map<String, String> lls(String section) {
		return lang.lls(section);
	}

	/**
	 * reference to Vault economy
	 */
	public static Economy economy = null;
	
	/**
	 * reference to Vault permissions
	 */
	public static Permission permission = null;
	
	/**
	 * reference to command processor singleton
	 */
	private static BuyCommandProcessor processor;

	/**
	 * get command processor
	 * @return
	 */
	public static BuyCommandProcessor getProcessor() {
		return processor;
	}

	/**
	 * checks permission, either Vault-based or using builtin system
	 * @param sender
	 * @return
	 */
	public static boolean checkPermission(CommandSender sender, String permission) {
		if (BuyCommand.permission != null) { // use Vault to check permissions
			return BuyCommand.permission.has(sender, permission);
		}
		// fallback to default Bukkit permission checking system
		return sender.hasPermission(permission) || sender.hasPermission("buycommand.*");
	}

	/**
	 * reference to player listener
	 */
	private BuyCommandPlayerListener playerListener;

	/* (non-Javadoc)
	 * @see org.bukkit.plugin.Plugin#onEnable()
	 */
	public void onEnable() {
		// initialize plugin
		log.info(this.toString() + " is loading.");	
		
		BuyCommand.buyCommand = this;

		// configure plugin (configuration stuff)
		configurePlugin();
		
		// check updates, if turned on
		checkForUpdate();

		// register vault stuff
		boolean economySetup = setupEconomy();
		if (economySetup == false) {
			getServer().getPluginManager().disablePlugin(this);
			return; // do not setup anything more
		}
		setupPermission();
		
		// add event listeners
		registerEvents();
	}

	/* (non-Javadoc)
	 * @see org.bukkit.plugin.Plugin#onDisable()
	 */
	public void onDisable() {
		log.info(this.toString() + " is shutting down.");

		// dereference to save resources
		BuyCommand.economy = null;
		BuyCommand.lang = null;

		//save config to disk
		this.saveConfig();
		
		BuyCommand.buyCommand = null;
	}

	/**
	 * Run some configuration stuff at the initialization of the plugin
	 */
	public void configurePlugin() {
		// define that default config should be copied
		this.getConfig().options().copyDefaults(true);

		// create config helper
		ConfigHelper configHelper = new ConfigHelper();
		// update sample config, if needed
		configHelper.updateSampleConfig();
		// update language files
		configHelper.updateLanguageFiles();

		// call loader for classes
		this.afterConfigLoad();
	}

	/**
	 * reload configuration and translation
	 */
	public void reloadBuyCommandConfiguration() {
		// reload the config file
		this.reloadConfig();
		
		// call reloader for classes
		this.afterConfigLoad();
	}

	/**
	 * loaded after config loading (used by configurePlugin and reloadBuyCommandConfiguration)
	 */
	protected void afterConfigLoad() {
		// (re-)initialize the translator
		BuyCommand.lang = new Translator(this, this.getConfig().getString("language", "en"));

		// (re-)configure command processor
		BuyCommand.processor = new BuyCommandProcessor();		
	}
	
	/**
	 * Check for updates
	 */
	protected void checkForUpdate() {
		// create update checker
		final UpdateChecker checker = new UpdateChecker();
		
		// possibly check for updates in the internet on startup
		if (this.getConfig().getBoolean("settings.updateNotificationOnStart", true)) {
			(new Thread() { // create a new anonymous thread that will check the version asyncronously
				@Override
				public void run() {
					try {
						String newVersion = checker.checkForUpdate(BuyCommand.buyCommand.getDescription().getVersion());
						if (newVersion != null)
							log.info("[BuyCommand] Update found for BuyCommand - please go to http://dev.bukkit.org/server-mods/buycommand/ to download version " + newVersion + "!");
					} catch (Exception e) {
						log.warning("[BuyCommand] Could not connect to remote server to check for update. Exception said: " + e.getMessage());
					}
				}
			}).start();
		}
		
		// also check for updates in the configuration files and update them, if needed
		checker.updateConfigurationVersion(this);
	}

	/**
	 * Configure event listeners
	 */
	protected void registerEvents() {
		// instanciate listeners
		playerListener = new BuyCommandPlayerListener();
		
		// Register events
		PluginManager pm = getServer().getPluginManager();
		
		// add listener for other plugins
		pm.registerEvents(playerListener, this);
	}

	/**
	 * set up economy - see http://dev.bukkit.org/server-mods/vault/
	 * @return
	 */
	private boolean setupEconomy() {
		if (this.getServer().getPluginManager().getPlugin("Vault") != null) {
			RegisteredServiceProvider<Economy> economyProvider = getServer()
					.getServicesManager().getRegistration(
							net.milkbowl.vault.economy.Economy.class);
			if (economyProvider != null) {
				economy = economyProvider.getProvider();
			}
	
			//BuyCommand.log.info("[BuyCommand] Vault hooked as economy plugin.");
			return (economy != null);
		}
		economy = null; // if the plugin is reloaded during play, possibly kill economy
		BuyCommand.log.info("[BuyCommand] Vault plugin not found - disabling BuyCommand.");
		return false;
	}
	
	/**
	 * set up permissions - see http://dev.bukkit.org/server-mods/vault/
	 * @return
	 */
	private boolean setupPermission() {
		if (this.getServer().getPluginManager().getPlugin("Vault") != null) {
			RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
	        if (permissionProvider != null) {
	            permission = permissionProvider.getProvider();
	        }
	
			//BuyCommand.log.info("[BuyCommand] Vault hooked as permission plugin.");
	        return (permission != null);
		}
		permission = null; // if the plugin is reloaded during play, possibly kill permissions
		BuyCommand.log.info("[BuyCommand] Vault plugin not found - defaulting to Bukkit permission system.");
		return false;
	}
}
