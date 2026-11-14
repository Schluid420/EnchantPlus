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

/** Entrypoint: players learn enchantments from books and pick them at the table. */
public class EnchantPlus implements ModInitializer {
	public static final String MOD_ID = "enchantplus";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Config first, everything else reads it
		ModConfig.load();
		ModAttachments.init();
		KnowledgeCommand.register();
		BookLearning.register();
		LootInjector.register();
		BookSourceFilter.register();

		LOGGER.info("EnchantPlus loaded.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
