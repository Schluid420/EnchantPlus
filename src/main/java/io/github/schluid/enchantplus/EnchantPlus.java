package io.github.schluid.enchantplus;

import io.github.schluid.enchantplus.command.KnowledgeCommand;
import io.github.schluid.enchantplus.config.ModConfig;
import io.github.schluid.enchantplus.knowledge.BookLearning;
import io.github.schluid.enchantplus.loot.BookSourceFilter;
import io.github.schluid.enchantplus.loot.LootInjector;
import io.github.schluid.enchantplus.network.SettingsSync;
import io.github.schluid.enchantplus.network.VersionCheck;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entrypoint. */
public class EnchantPlus implements ModInitializer {
	public static final String MOD_ID = "enchantplus";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Config first
		ModConfig.load();
		ModAttachments.init();
		KnowledgeCommand.register();
		BookLearning.register();
		LootInjector.register();
		BookSourceFilter.register();
		SettingsSync.register();
		VersionCheck.register();
		// /reload reads the file again and sends the settings to everyone
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			ModConfig.load();
			SettingsSync.sendToAll(server);
		});

		LOGGER.info("EnchantPlus loaded.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
