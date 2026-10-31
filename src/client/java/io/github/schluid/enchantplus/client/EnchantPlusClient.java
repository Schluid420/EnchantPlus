package io.github.schluid.enchantplus.client;

import net.fabricmc.api.ClientModInitializer;

public class EnchantPlusClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Selection list inside the enchanting table
		EnchantPanel.register();
		GrindPanel.register();
	}
}
