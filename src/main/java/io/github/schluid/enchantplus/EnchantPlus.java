package io.github.schluid.enchantplus;

import io.github.schluid.enchantplus.command.KnowledgeCommand;
import io.github.schluid.enchantplus.config.ModConfig;
import io.github.schluid.enchantplus.knowledge.BookLearning;
import io.github.schluid.enchantplus.loot.BookSourceFilter;
import io.github.schluid.enchantplus.loot.LootInjector;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entrypoint (runs on both the client and the server).
 *
 * Idea of the mod: enchantments are no longer random. Players learn them once from enchanted books,
 * the knowledge is stored on the player, and the enchanting table lets them pick what to apply.
 */
public class EnchantPlus implements ModInitializer {
	public static final String MOD_ID = "enchantplus";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Settings first, everything else reads them
		ModConfig.load();
		// Per-player storage of learned enchantments
		ModAttachments.init();
		// Admin/testing command: /enchantplus
		KnowledgeCommand.register();
		// Right-click an enchanted book to learn it
		BookLearning.register();
		// Adds enchanted books to structure chests
		LootInjector.register();
		// Optional: no books from fishing and bartering
		BookSourceFilter.register();

		LOGGER.info("EnchantPlus loaded.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
