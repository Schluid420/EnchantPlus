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

/**
 * Shared logic for server and client:
 * which learned enchantments can the player apply to the item in the enchanting table,
 * and what does it cost?
 * Server and client must compute exactly the same order, because only a number
 * (the button id) is sent over the network.
 */
public final class EnchantOptions {
	private EnchantOptions() {
	}

	/** Button ids from this value upwards belong to this mod (vanilla buttons are 0 to 2). */
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

		// Sorted by id so server and client get the same order
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

	/** Required XP level (like the vanilla table, capped at 30). */
	public static int requiredLevel(Option option, int level) {
		int cost = option.holder().value().getMinCost(level);
		return Math.min(MAX_REQUIRED_LEVEL, Math.max(1, cost));
	}

	/** Price tier (1 to 3): the limits for tier 1 and tier 2 are set in the config, tier 3 is everything above. */
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

	/** Experience levels used up for a tier (1 to 3), set in the config. */
	public static int levelCost(int tier) {
		return ModConfig.levelCost[tier - 1];
	}

	/** Lapis lazuli used up for a tier (1 to 3), set in the config. */
	public static int lapisCost(int tier) {
		return ModConfig.lapisCost[tier - 1];
	}

	/** The player must have at least the required level, and enough to pay the cost. */
	public static int neededPlayerLevel(int requiredLevel, int tier) {
		return Math.max(requiredLevel, levelCost(tier));
	}

	/** Bookshelves needed for a given XP level cost: half of it, rounded up (30 levels = 15 shelves). */
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
