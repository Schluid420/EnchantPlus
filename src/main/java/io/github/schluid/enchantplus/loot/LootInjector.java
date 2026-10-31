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

/**
 * Makes structure chests the only place with a fixed set of enchanted books:
 *
 * 1. Every vanilla enchanted book is removed from chest loot (they were random and ignored the tiers).
 * 2. For the structures listed below, our own book is rolled instead, using the tier tables in
 *    src/main/resources/data/enchantplus/loot_table/books/ (common, medium, rare).
 *
 * The chance can be changed in the config and takes effect immediately.
 */
public final class LootInjector {
	private LootInjector() {
	}

	private static final String COMMON = "enchantplus:books/common";
	private static final String MEDIUM = "enchantplus:books/medium";
	private static final String RARE = "enchantplus:books/rare";

	private static final Map<String, String> TARGETS = Map.ofEntries(
			// Easy structures: low levels
			Map.entry("minecraft:chests/simple_dungeon", COMMON),
			Map.entry("minecraft:chests/desert_pyramid", COMMON),
			Map.entry("minecraft:chests/jungle_temple", COMMON),
			Map.entry("minecraft:chests/abandoned_mineshaft", COMMON),
			Map.entry("minecraft:chests/shipwreck_treasure", COMMON),
			Map.entry("minecraft:chests/buried_treasure", COMMON),
			Map.entry("minecraft:chests/pillager_outpost", COMMON),
			Map.entry("minecraft:chests/underwater_ruin_big", COMMON),
			// Medium structures: levels up to III and some special enchantments
			Map.entry("minecraft:chests/nether_bridge", MEDIUM),
			Map.entry("minecraft:chests/stronghold_corridor", MEDIUM),
			Map.entry("minecraft:chests/stronghold_crossing", MEDIUM),
			Map.entry("minecraft:chests/stronghold_library", MEDIUM),
			Map.entry("minecraft:chests/woodland_mansion", MEDIUM),
			Map.entry("minecraft:chests/trial_chambers/reward", MEDIUM),
			// Hard structures: high levels and special enchantments
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

			// 1. Remove the random vanilla books
			drops.removeIf(stack -> stack.is(Items.ENCHANTED_BOOK));

			// 2. Roll our own book for the structures in the list
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

			// Example: 150% = 2 rolls with a 75% chance each; the tier table applies its own chance on top
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
