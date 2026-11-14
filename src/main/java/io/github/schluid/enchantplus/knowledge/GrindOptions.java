package io.github.schluid.enchantplus.knowledge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Enchantments of the grindstone item, sorted by id so a number can identify one. */
public final class GrindOptions {
	private GrindOptions() {
	}

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
