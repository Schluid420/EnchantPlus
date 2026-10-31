package io.github.schluid.enchantplus.mixin;

import io.github.schluid.enchantplus.config.ModConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks transferring or combining enchantments at the anvil, because that would bypass the
 * learning system. Repairing with materials and renaming still work.
 *
 * Rule: if the right slot holds an enchanted item or an enchanted book, there is no result.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
	@Shadow
	@Final
	private DataSlot cost;

	@Inject(method = "createResult", at = @At("TAIL"))
	private void enchantplus$blockEnchantTransfer(CallbackInfo ci) {
		if (!ModConfig.anvilLock) {
			return;
		}
		AnvilMenu menu = (AnvilMenu) (Object) this;
		ItemStack right = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
		if (right.isEmpty()) {
			return;
		}

		ItemEnchantments stored = right.get(DataComponents.STORED_ENCHANTMENTS);
		boolean hasEnchantments = !right.getEnchantments().isEmpty()
				|| (stored != null && !stored.isEmpty());
		if (hasEnchantments) {
			menu.getSlot(AnvilMenu.RESULT_SLOT).set(ItemStack.EMPTY);
			this.cost.set(0);
		}
	}
}
