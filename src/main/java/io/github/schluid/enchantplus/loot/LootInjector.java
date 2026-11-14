package io.github.schluid.enchantplus.loot;

import io.github.schluid.enchantplus.EnchantPlus;
import io.github.schluid.enchantplus.config.ModConfig;
import java.util.Map;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;

/** Replaces the vanilla chest books with our tier tables (data/enchantplus/loot_table/books). */
public final class LootInjector {
	private LootInjector() {
	}

	private static final String COMMON = "enchantplus:books/common";
	private static final String MEDIUM = "enchantplus:books/medium";
	private static final String RARE = "enchantplus:books/rare";

	private static final Map<String, String> TARGETS = Map.ofEntries(
			// Easy: low levels
			Map.entry("minecraft:chests/simple_dungeon", COMMON),
			Map.entry("minecraft:chests/desert_pyramid", COMMON),
			Map.entry("minecraft:chests/jungle_temple", COMMON),
			Map.entry("minecraft:chests/abandoned_mineshaft", COMMON),
			Map.entry("minecraft:chests/shipwreck_treasure", COMMON),
			Map.entry("minecraft:chests/buried_treasure", COMMON),
			Map.entry("minecraft:chests/pillager_outpost", COMMON),
			Map.entry("minecraft:chests/underwater_ruin_big", COMMON),
			// Medium: up to level III and some special ones
			Map.entry("minecraft:chests/nether_bridge", MEDIUM),
			Map.entry("minecraft:chests/stronghold_corridor", MEDIUM),
			Map.entry("minecraft:chests/stronghold_crossing", MEDIUM),
			Map.entry("minecraft:chests/stronghold_library", MEDIUM),
			Map.entry("minecraft:chests/woodland_mansion", MEDIUM),
			Map.entry("minecraft:chests/trial_chambers/reward", MEDIUM),
			// Hard: high levels and special ones
			Map.entry("minecraft:chests/bastion_bridge", RARE),
			Map.entry("minecraft:chests/bastion_hoglin_stable", RARE),
			Map.entry("minecraft:chests/bastion_other", RARE),
			Map.entry("minecraft:chests/bastion_treasure", RARE),
			Map.entry("minecraft:chests/end_city_treasure", RARE),
			Map.entry("minecraft:chests/ancient_city", RARE),
			Map.entry("minecraft:chests/trial_chambers/reward_ominous", RARE));

	public static void register() {
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			String id = table.unwrapKey().map(key -> key.identifier().toString()).orElse("");
			if (!id.startsWith("minecraft:chests/")) {
				return;
			}

			drops.removeIf(stack -> stack.is(Items.ENCHANTED_BOOK));

			String tier = TARGETS.get(id);
			int percent = ModConfig.lootChancePercent;
			if (tier == null || percent <= 0) {
				return;
			}
			ResourceKey<LootTable> bookTableKey = ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(tier));
			var bookTable = context.getResolver().get(bookTableKey);
			if (bookTable.isEmpty()) {
				EnchantPlus.LOGGER.error("Book loot table {} not found", tier);
				return;
			}

			// e.g. 150% = 2 rolls at 75% each
			int rolls = (int) Math.ceil(percent / 100.0);
			double chance = percent / 100.0 / rolls;
			for (int i = 0; i < rolls; i++) {
				if (context.getRandom().nextDouble() < chance) {
					bookTable.get().value().getRandomItemsRaw(context, drops::add);
				}
			}
		});
	}
}
