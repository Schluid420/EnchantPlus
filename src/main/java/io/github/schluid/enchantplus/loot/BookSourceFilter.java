package io.github.schluid.enchantplus.loot;

import io.github.schluid.enchantplus.config.ModConfig;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.item.Items;

/** Optional: no enchanted books from fishing or piglin bartering. */
public final class BookSourceFilter {
	private BookSourceFilter() {
	}

	public static void register() {
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			String path = table.unwrapKey().map(key -> key.identifier().getPath()).orElse("");
			boolean fishing = ModConfig.blockFishingBooks && path.startsWith("gameplay/fishing");
			boolean bartering = ModConfig.blockPiglinBooks && path.equals("gameplay/piglin_bartering");
			if (fishing || bartering) {
				drops.removeIf(stack -> stack.is(Items.ENCHANTED_BOOK));
			}
		});
	}
}
