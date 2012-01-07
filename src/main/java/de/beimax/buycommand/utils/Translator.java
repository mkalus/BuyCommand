/**
 * 
 */
package de.beimax.buycommand.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.file.YamlConfiguration;

import de.beimax.buycommand.BuyCommand;

/**
 * @author mkalus
 * Tranlation Helper
 */
public class Translator {
	/**
	 * language
	 */
	private final String language;

	/**
	 * language configuration
	 */
	private final YamlConfiguration lang;

	/**
	 * Constructor
	 * @param plugin reference to plugin
	 */
	public Translator(BuyCommand plugin, String language) {
		this.language = language;
		// load yaml file - test language existance first
		File languageFile = new File(plugin.getDataFolder(), "lang_" + language + ".yml");
		if (!languageFile.exists()) {
			BuyCommand.log.warning("[BuyCommand] Language file lang_" + language + ".yml does not exist - falling back to English default file.");
			languageFile = new File(plugin.getDataFolder(), "lang_en.yml");
		}
		lang = YamlConfiguration.loadConfiguration(languageFile);
	}
	
	/**
	 * @return current language
	 */
	public String getLanguage() {
		return this.language;
	}
	
	/**
	 * @param key of translation file
	 * @param replacers an even number of key/value pairs to replace key entries
	 * @return translated string
	 */
	public String ll(String key, String... replacers) {
		String translated = lang.getString(key); // get key from config
		// error?
		if (translated == null) return "ERROR: Key " + key + " does not exist in language file lang_" + getLanguage() + ".yml).";
		// check replacements
		if (replacers.length % 2 != 0) return "ERROR: Usage of ll with " + key + " has to contain an even number of needles/replacers (" + translated + ").";
		// cycle through replacements
		for (int i = 0; i < replacers.length; i+=2) {
			translated = translated.replace(replacers[i], replacers[i+1]);
		}
		return translated;
	}

	/**
	 * Get translation section list
	 * @param section
	 * @return
	 */
	public Map<String, String> lls(String section) {
		// sanity check
		if (!lang.isConfigurationSection(section)) return null;
		// get keys
		Set<String> keys = lang.getConfigurationSection(section).getKeys(false);
		Map<String, String> map = new HashMap<String, String>();
		for (String key : keys) {
			map.put(key, lang.getString(section + "." + key));
		}

		return map;
	}
}
