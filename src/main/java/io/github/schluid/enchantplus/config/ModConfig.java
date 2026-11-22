package io.github.schluid.enchantplus.config;

import io.github.schluid.enchantplus.EnchantPlus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Settings in config/enchantplus.json. On a server, the server's settings decide (see SettingsSync). */
public final class ModConfig {
	private ModConfig() {
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static final int[] DEFAULT_TIER_LIMIT = {10, 20};
	public static final int[] DEFAULT_LEVEL_COST = {1, 5, 10};
	public static final int[] DEFAULT_LAPIS_COST = {3, 5, 10};

	/** Highest required level of tier 1 and tier 2; tier 3 is everything above. */
	public static int[] tierLimit = DEFAULT_TIER_LIMIT.clone();
	public static int lootChancePercent = 100;
	/** Per tier 1-3. */
	public static int[] levelCost = DEFAULT_LEVEL_COST.clone();
	public static int[] lapisCost = DEFAULT_LAPIS_COST.clone();
	public static boolean requireShelves = true;
	public static boolean anvilLock = true;
	public static boolean allowBookWriting = true;
	public static boolean learnEffect = true;
	/** Client only: a server never sets it. */
	public static boolean showDescriptions = true;
	/** Percent of one level. */
	public static int bookXpNewPercent = 100;
	public static int bookXpDuplicatePercent = 50;
	public static int grindLapisCost = 2;
	public static boolean keepTableItems = false;
	public static int maxEnchantments = 0;
	public static boolean relaxedShelves = false;
	public static boolean upgradeDifference = false;
	public static boolean blockVillagerBooks = false;
	public static int emeraldsPerBottle = 3;
	public static boolean blockFishingBooks = false;
	public static boolean blockPiglinBooks = false;

	private static final class Data {
		int[] tierLimit = DEFAULT_TIER_LIMIT.clone();
		int lootChancePercent = 100;
		int[] levelCost = DEFAULT_LEVEL_COST.clone();
		int[] lapisCost = DEFAULT_LAPIS_COST.clone();
		boolean requireShelves = true;
		boolean anvilLock = true;
		boolean allowBookWriting = true;
		boolean learnEffect = true;
		boolean showDescriptions = true;
		int bookXpNewPercent = 100;
		int bookXpDuplicatePercent = 50;
		int grindLapisCost = 2;
		boolean keepTableItems = false;
		int maxEnchantments = 0;
		boolean relaxedShelves = false;
		boolean upgradeDifference = false;
		boolean blockVillagerBooks = false;
		int emeraldsPerBottle = 3;
		boolean blockFishingBooks = false;
		boolean blockPiglinBooks = false;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("enchantplus.json");
	}

	public static void load() {
		Path path = file();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				Data data = GSON.fromJson(reader, Data.class);
				if (data != null) {
					apply(data);
				}
			} catch (IOException | RuntimeException e) {
				EnchantPlus.LOGGER.error("Could not read the config file, using defaults", e);
				backupBrokenFile(path);
			}
		}
		// Creates the file on first start
		save();
	}

	/** Takes over all values, limited to their allowed range. */
	private static void apply(Data data) {
		tierLimit = readValues(data.tierLimit, tierLimit, 2, 1, 100);
		lootChancePercent = Math.max(0, Math.min(500, data.lootChancePercent));
		levelCost = readValues(data.levelCost, levelCost, 3, 1, 100);
		lapisCost = readValues(data.lapisCost, lapisCost, 3, 0, 64);
		requireShelves = data.requireShelves;
		anvilLock = data.anvilLock;
		allowBookWriting = data.allowBookWriting;
		learnEffect = data.learnEffect;
		showDescriptions = data.showDescriptions;
		bookXpNewPercent = Math.max(0, Math.min(500, data.bookXpNewPercent));
		bookXpDuplicatePercent = Math.max(0, Math.min(500, data.bookXpDuplicatePercent));
		grindLapisCost = Math.max(1, Math.min(100, data.grindLapisCost));
		keepTableItems = data.keepTableItems;
		maxEnchantments = Math.max(0, Math.min(50, data.maxEnchantments));
		relaxedShelves = data.relaxedShelves;
		upgradeDifference = data.upgradeDifference;
		blockVillagerBooks = data.blockVillagerBooks;
		emeraldsPerBottle = Math.max(1, Math.min(64, data.emeraldsPerBottle));
		blockFishingBooks = data.blockFishingBooks;
		blockPiglinBooks = data.blockPiglinBooks;
		// Tier 2 must not end below tier 1
		if (tierLimit[1] < tierLimit[0]) {
			tierLimit[1] = tierLimit[0];
		}
	}

	private static Data toData() {
		Data data = new Data();
		data.tierLimit = tierLimit.clone();
		data.lootChancePercent = lootChancePercent;
		data.levelCost = levelCost.clone();
		data.lapisCost = lapisCost.clone();
		data.requireShelves = requireShelves;
		data.anvilLock = anvilLock;
		data.allowBookWriting = allowBookWriting;
		data.learnEffect = learnEffect;
		data.showDescriptions = showDescriptions;
		data.bookXpNewPercent = bookXpNewPercent;
		data.bookXpDuplicatePercent = bookXpDuplicatePercent;
		data.grindLapisCost = grindLapisCost;
		data.keepTableItems = keepTableItems;
		data.maxEnchantments = maxEnchantments;
		data.relaxedShelves = relaxedShelves;
		data.upgradeDifference = upgradeDifference;
		data.blockVillagerBooks = blockVillagerBooks;
		data.emeraldsPerBottle = emeraldsPerBottle;
		data.blockFishingBooks = blockFishingBooks;
		data.blockPiglinBooks = blockPiglinBooks;
		return data;
	}

	private static int[] readValues(int[] fromFile, int[] fallback, int count, int min, int max) {
		if (fromFile == null || fromFile.length != count) {
			return fallback;
		}
		int[] result = new int[count];
		for (int i = 0; i < count; i++) {
			result[i] = Math.max(min, Math.min(max, fromFile[i]));
		}
		return result;
	}

	/** Saves an unreadable file as enchantplus.json.broken. */
	private static void backupBrokenFile(Path path) {
		try {
			Files.copy(path, path.resolveSibling("enchantplus.json.broken"),
					java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			EnchantPlus.LOGGER.warn("Your old config was saved as enchantplus.json.broken");
		} catch (IOException e) {
			EnchantPlus.LOGGER.error("Could not back up the broken config file", e);
		}
	}

	// Settings of a server: a client uses them in memory only; its own settings stay in the file
	// and come back when the player leaves.

	/** The own settings while those of a server are used, null otherwise. */
	private static JsonObject ownSettings;
	private static boolean serverCanEdit;

	/** The settings of a server are used right now. */
	public static boolean serverMode() {
		return ownSettings != null;
	}

	/** The player may change the settings of the server (operator). */
	public static boolean serverCanEdit() {
		return serverCanEdit;
	}

	/** All settings that decide how the game plays; the client-only ones are left out. */
	public static JsonObject settingsToJson() {
		JsonObject root = GSON.toJsonTree(toData()).getAsJsonObject();
		root.remove("showDescriptions");
		return root;
	}

	/** Replaces all game settings; missing entries get their default. Client-only options stay. */
	public static void settingsFromJson(JsonObject root) {
		boolean descriptions = showDescriptions;
		try {
			Data data = GSON.fromJson(root, Data.class);
			if (data != null) {
				apply(data);
			}
		} catch (RuntimeException e) {
			EnchantPlus.LOGGER.error("Could not read the settings that were sent", e);
		}
		showDescriptions = descriptions;
	}

	/** Uses the settings a server sent; the own ones are kept. */
	public static void useServerSettings(JsonObject settings, boolean canEdit) {
		if (ownSettings == null) {
			ownSettings = settingsToJson();
		}
		settingsFromJson(settings);
		serverCanEdit = canEdit;
	}

	/** Back to the own settings after leaving a server. */
	public static void useOwnSettings() {
		if (ownSettings != null) {
			JsonObject own = ownSettings;
			ownSettings = null;
			serverCanEdit = false;
			settingsFromJson(own);
		}
	}

	public static void save() {
		if (ownSettings != null) {
			// On a server only client-only options change in the file; the own settings stay
			JsonObject server = settingsToJson();
			settingsFromJson(ownSettings);
			try {
				write();
			} finally {
				settingsFromJson(server);
			}
			return;
		}
		write();
	}

	private static void write() {
		try (Writer writer = Files.newBufferedWriter(file())) {
			GSON.toJson(toData(), writer);
		} catch (IOException e) {
			EnchantPlus.LOGGER.error("Could not save the config file", e);
		}
	}
}
