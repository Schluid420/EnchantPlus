package io.github.schluid.enchantplus.knowledge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;

/** Counts the bookshelves around an enchanting table, exactly like vanilla (0 to 15). */
public final class Bookshelves {
	private Bookshelves() {
	}

	public static int count(Level level, BlockPos tablePos) {
		int shelves = 0;
		for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
			if (EnchantingTableBlock.isValidBookShelf(level, tablePos, offset)) {
				shelves++;
			}
		}
		return shelves;
	}
}
