package io.github.schluid.enchantplus.knowledge;

import io.github.schluid.enchantplus.config.ModConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Options and costs at the table. Server and client must compute the same order: only the button id is sent. */
public final class EnchantOptions {
	private EnchantOptions() {
	}

	public static final int BUTTON_BASE = 1000;
	public static final int LEVELS_PER_ENTRY = 16;
	public static final int MAX_REQUIRED_LEVEL = 30;

	public record Option(String id, Holder<Enchantment> holder, int maxLevel, int existingLevel) {
	}

	public static List<Option> forStack(Player player, ItemStack stack, RegistryAccess registries) {
		List<Option> result = new ArrayList<>();
		if (stack.isEmpty()) {
			return result;
		}
		Registry<Enchantment> registry = registries.lookupOrThrow(Registries.ENCHANTMENT);
		ItemEnchantments existing = stack.getEnchantments();
		boolean book = stack.is(Items.BOOK);
		if (book && !ModConfig.allowBookWriting) {
			return result;
		}

		// Sorted by id: same order on server and client
		TreeMap<String, Integer> known = new TreeMap<>(EnchantKnowledge.all(player));
		for (var entry : known.entrySet()) {
			Identifier identifier = Identifier.tryParse(entry.getKey());
			if (identifier == null) {
				continue;
			}
			Optional<Holder.Reference<Enchantment>> found = registry.get(identifier);
			if (found.isEmpty()) {
				continue;
			}
			Holder<Enchantment> holder = found.get();
			Enchantment enchantment = holder.value();

			// Maximum reached: only upgrades of existing ones
			if (ModConfig.maxEnchantments > 0 && !book && existing.size() >= ModConfig.maxEnchantments
					&& existing.getLevel(holder) == 0) {
				continue;
			}

			if (!book && !enchantment.isPrimaryItem(stack)) {
				continue;
			}

			boolean compatible = true;
			for (Holder<Enchantment> other : existing.keySet()) {
				if (!other.equals(holder) && !Enchantment.areCompatible(holder, other)) {
					compatible = false;
					break;
				}
			}
			if (!compatible) {
				continue;
			}

			int maxLevel = Math.min(Math.min(entry.getValue(), enchantment.getMaxLevel()), LEVELS_PER_ENTRY);
			int existingLevel = existing.getLevel(holder);
			if (maxLevel < 1 || existingLevel >= maxLevel) {
				continue;
			}
			result.add(new Option(entry.getKey(), holder, maxLevel, existingLevel));
		}
		return result;
	}

	/** Like vanilla, capped at 30. */
	public static int requiredLevel(Option option, int level) {
		int cost = option.holder().value().getMinCost(level);
		return Math.min(MAX_REQUIRED_LEVEL, Math.max(1, cost));
	}

	/** Tier 1-3, from the limits in the config. */
	public static int tier(int requiredLevel) {
		int first = ModConfig.tierLimit[0];
		int second = Math.max(first, ModConfig.tierLimit[1]);
		if (requiredLevel <= first) {
			return 1;
		}
		if (requiredLevel <= second) {
			return 2;
		}
		return 3;
	}

	public static int levelCost(int tier) {
		return ModConfig.levelCost[tier - 1];
	}

	public static int lapisCost(int tier) {
		return ModConfig.lapisCost[tier - 1];
	}

	/** With "upgrade difference": new price minus old price, at least 1. */
	public static int levelCostFor(Option option, int level) {
		int cost = levelCost(tier(requiredLevel(option, level)));
		if (ModConfig.upgradeDifference && option.existingLevel() > 0) {
			int old = levelCost(tier(requiredLevel(option, option.existingLevel())));
			return Math.max(1, cost - old);
		}
		return cost;
	}

	/** Same rule as {@link #levelCostFor}. */
	public static int lapisCostFor(Option option, int level) {
		int cost = lapisCost(tier(requiredLevel(option, level)));
		if (ModConfig.upgradeDifference && option.existingLevel() > 0) {
			int old = lapisCost(tier(requiredLevel(option, option.existingLevel())));
			return Math.max(0, cost - old);
		}
		return cost;
	}

	public static int neededPlayerLevel(int requiredLevel, int tier) {
		return Math.max(requiredLevel, levelCost(tier));
	}

	/** Half the level cost, rounded up (30 levels = 15 shelves). */
	public static int requiredShelves(int requiredLevel) {
		if (!ModConfig.requireShelves) {
			return 0;
		}
		return (requiredLevel + 1) / 2;
	}

	public static int buttonId(int index, int level) {
		return BUTTON_BASE + index * LEVELS_PER_ENTRY + (level - 1);
	}
}
