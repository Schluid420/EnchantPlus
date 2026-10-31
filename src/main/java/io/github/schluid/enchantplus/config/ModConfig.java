package io.github.schluid.enchantplus.config;

import io.github.schluid.enchantplus.EnchantPlus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Settings of the mod, stored in config/enchantplus.json.
 * The values are static so every part of the mod (server and client side) reads the same ones.
 * In multiplayer the server's file decides for everything that happens on the server.
 */
public final class ModConfig {
	private ModConfig() {
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Highest required level of price tier 1 and of price tier 2. Tier 3 is everything above (1 to 100). */
	public static int[] tierLimit = {10, 20};
	/** Multiplier for the chance of finding books in structure chests, in percent (0 to 200). */
	public static int lootChancePercent = 100;
	/** Experience levels that are used up per enchanting, for the three tiers (cheap, medium, expensive). */
	public static int[] levelCost = {5, 10, 20};
	/** Lapis lazuli that is used up per enchanting, for the three tiers. */
	public static int[] lapisCost = {2, 4, 8};
	/** Higher enchantment levels need bookshelves around the table. */
	public static boolean requireShelves = true;
	/** Blocks applying enchanted books and combining enchantments at the anvil. */
	public static boolean anvilLock = true;
	/** Allows writing learned enchantments onto plain books at the enchanting table. */
	public static boolean allowBookWriting = true;
	/** Sound and particles when a book is read. */
	public static boolean learnEffect = true;
	/** Shows a short description of the enchantment in the table tooltip. */
	public static boolean showDescriptions = true;
	/** Removes enchanted books from villager trades (vanilla when off). */
	public static boolean blockVillagerBooks = false;
	/** Removes enchanted books from fishing (vanilla when off). */
	public static boolean blockFishingBooks = false;
	/** Removes enchanted books from piglin bartering (vanilla when off). */
	public static boolean blockPiglinBooks = false;

	/** What is written to the JSON file. */
	private static final class Data {
		int[] tierLimit = {10, 20};
		int lootChancePercent = 100;
		int[] levelCost = {5, 10, 20};
		int[] lapisCost = {2, 4, 8};
		boolean requireShelves = true;
		boolean anvilLock = true;
		boolean allowBookWriting = true;
		boolean learnEffect = true;
		boolean showDescriptions = true;
		boolean blockVillagerBooks = false;
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
					tierLimit = readValues(data.tierLimit, tierLimit, 2, 1, 100);
					lootChancePercent = Math.max(0, Math.min(500, data.lootChancePercent));
					levelCost = readValues(data.levelCost, levelCost, 3, 1, 100);
					lapisCost = readValues(data.lapisCost, lapisCost, 3, 0, 64);
					requireShelves = data.requireShelves;
					anvilLock = data.anvilLock;
					allowBookWriting = data.allowBookWriting;
					learnEffect = data.learnEffect;
					showDescriptions = data.showDescriptions;
					blockVillagerBooks = data.blockVillagerBooks;
					blockFishingBooks = data.blockFishingBooks;
					blockPiglinBooks = data.blockPiglinBooks;
				}
			} catch (IOException | RuntimeException e) {
				EnchantPlus.LOGGER.error("Could not read the config file, using defaults", e);
				backupBrokenFile(path);
			}
		}
		// Writing the file right away creates it with defaults on the first start
		save();
	}

	/** Reads a fixed number of values from the file and keeps them inside the allowed range. */
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

	/** Keeps a copy of an unreadable file (enchantplus.json.broken) so hand-made settings are not lost. */
	private static void backupBrokenFile(Path path) {
		try {
			Files.copy(path, path.resolveSibling("enchantplus.json.broken"),
					java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			EnchantPlus.LOGGER.warn("Your old config was saved as enchantplus.json.broken");
		} catch (IOException e) {
			EnchantPlus.LOGGER.error("Could not back up the broken config file", e);
		}
	}

	public static void save() {
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
		data.blockVillagerBooks = blockVillagerBooks;
		data.blockFishingBooks = blockFishingBooks;
		data.blockPiglinBooks = blockPiglinBooks;
		try (Writer writer = Files.newBufferedWriter(file())) {
			GSON.toJson(data, writer);
		} catch (IOException e) {
			EnchantPlus.LOGGER.error("Could not save the config file", e);
		}
	}
}
