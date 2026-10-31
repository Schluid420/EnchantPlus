package io.github.schluid.enchantplus.knowledge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Shared by server and client: the enchantments of the item in the grindstone,
 * in a fixed order (sorted by id), so a plain number can identify one of them.
 */
public final class GrindOptions {
	private GrindOptions() {
	}

	/** First button id of this mod at the grindstone (vanilla has no buttons there). */
	public static final int BUTTON_BASE = 2000;

	public static List<Holder<Enchantment>> forStack(ItemStack stack) {
		List<Holder<Enchantment>> result = new ArrayList<>();
		if (stack.isEmpty()) {
			return result;
		}
		result.addAll(EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet());
		result.sort(Comparator.comparing(Holder::getRegisteredName));
		return result;
	}
}
