package io.github.schluid.enchantplus.client;

import net.fabricmc.api.ClientModInitializer;

public class EnchantPlusClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EnchantPanel.register();
		GrindPanel.register();
	}
}
