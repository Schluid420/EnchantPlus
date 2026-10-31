package io.github.schluid.enchantplus.mixin;

import io.github.schluid.enchantplus.ModAttachments;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Items left in an enchanting table drop when the table is destroyed. */
@Mixin(BlockEntity.class)
public abstract class TableDropMixin {
	@Inject(method = "preRemoveSideEffects", at = @At("HEAD"))
	private void enchantplus$dropItems(BlockPos pos, BlockState state, CallbackInfo ci) {
		BlockEntity self = (BlockEntity) (Object) this;
		if (!(self instanceof EnchantingTableBlockEntity) || !(self.getLevel() instanceof ServerLevel level)) {
			return;
		}
		List<ItemStack> saved = self.getAttachedOrElse(ModAttachments.TABLE_ITEMS, List.of());
		for (ItemStack stack : saved) {
			if (!stack.isEmpty()) {
				Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
			}
		}
		self.removeAttached(ModAttachments.TABLE_ITEMS);
	}
}
