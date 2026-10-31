package io.github.schluid.enchantplus.knowledge;

import io.github.schluid.enchantplus.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Counts the bookshelves around an enchanting table.
 * Like vanilla, or with the option "relaxed shelves": the block between table and shelf
 * may be anything that is not a full block (torch, carpet, grass ...), and chiseled
 * bookshelves count by their books (3 books = 1 shelf).
 */
public final class Bookshelves {
	private Bookshelves() {
	}

	public static int count(Level level, BlockPos tablePos) {
		int shelves = 0;
		int chiseledBooks = 0;
		for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
			if (EnchantingTableBlock.isValidBookShelf(level, tablePos, offset)) {
				shelves++;
				continue;
			}
			if (!ModConfig.relaxedShelves) {
				continue;
			}
			BlockPos middle = tablePos.offset(offset.getX() / 2, offset.getY(), offset.getZ() / 2);
			BlockState middleState = level.getBlockState(middle);
			boolean clear = middleState.is(BlockTags.ENCHANTMENT_POWER_TRANSMITTER)
					|| !middleState.isCollisionShapeFullBlock(level, middle);
			if (!clear) {
				continue;
			}
			BlockState shelf = level.getBlockState(tablePos.offset(offset));
			if (shelf.is(BlockTags.ENCHANTMENT_POWER_PROVIDER)) {
				shelves++;
			} else if (shelf.is(Blocks.CHISELED_BOOKSHELF)) {
				for (BooleanProperty slot : ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES) {
					if (shelf.getValue(slot)) {
						chiseledBooks++;
					}
				}
			}
		}
		return shelves + chiseledBooks / 3;
	}
}
