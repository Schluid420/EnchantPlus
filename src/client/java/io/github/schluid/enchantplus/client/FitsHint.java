package io.github.schluid.enchantplus.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

/** Builds the "Fits: sword, axe ..." hint for an enchantment. */
public final class FitsHint {
	private FitsHint() {
	}

	public static Component of(Enchantment enchantment) {
		HolderSet<Item> items = enchantment.getSupportedItems();
		var tag = items.unwrapKey();
		if (tag.isPresent()) {
			String key = "enchantplus.fits." + tag.get().location().getPath().replace('/', '.');
			if (Language.getInstance().has(key)) {
				return Component.translatable("enchantplus.gui.fits", Component.translatable(key));
			}
		}
		// Unknown tag (e.g. from another mod): list a few item names
		List<String> names = new ArrayList<>();
		for (Holder<Item> holder : (Iterable<Holder<Item>>) items.stream()::iterator) {
			if (names.size() >= 3) {
				names.add("...");
				break;
			}
			names.add(holder.value().getDefaultInstance().getHoverName().getString());
		}
		if (names.isEmpty()) {
			return Component.translatable("enchantplus.gui.fits_any");
		}
		return Component.translatable("enchantplus.gui.fits", String.join(", ", names));
	}
}
