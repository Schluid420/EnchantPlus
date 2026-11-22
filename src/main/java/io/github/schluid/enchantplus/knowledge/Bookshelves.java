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

/** Vanilla shelf count; relaxed: gaps allowed, chiseled shelves count (3 books = 1). */
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
