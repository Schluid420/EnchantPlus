package io.github.schluid.enchantplus.mixin;

import io.github.schluid.enchantplus.config.ModConfig;
import io.github.schluid.enchantplus.knowledge.GrindOptions;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grindstone: removes ONE chosen enchantment for lapis lazuli from the player's inventory.
 * Button id = 2000 + index in the sorted enchantment list of the item in the input slot.
 * The normal grindstone behaviour is not touched.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class GrindstoneButtonMixin {
	@Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
	private void enchantplus$grind(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
		if (id < GrindOptions.BUTTON_BASE || !(((Object) this) instanceof GrindstoneMenu)) {
			return;
		}
		cir.setReturnValue(enchantplus$remove(player, id - GrindOptions.BUTTON_BASE));
	}

	@Unique
	private boolean enchantplus$remove(Player player, int index) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
		Slot input = menu.getSlot(GrindstoneMenu.INPUT_SLOT);
		ItemStack stack = input.getItem();
		if (stack.isEmpty() || !menu.getSlot(GrindstoneMenu.ADDITIONAL_SLOT).getItem().isEmpty()) {
			return false;
		}
		List<Holder<Enchantment>> list = GrindOptions.forStack(stack);
		if (index < 0 || index >= list.size()) {
			return false;
		}
		Holder<Enchantment> target = list.get(index);

		int cost = ModConfig.grindLapisCost;
		boolean free = player.hasInfiniteMaterials();
		Inventory inventory = player.getInventory();
		if (!free && cost > 0) {
			int have = 0;
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				ItemStack s = inventory.getItem(i);
				if (s.is(Items.LAPIS_LAZULI)) {
					have += s.getCount();
				}
			}
			if (have < cost) {
				serverPlayer.sendSystemMessage(Component.translatable("enchantplus.message.need_lapis", cost));
				return false;
			}
			int left = cost;
			for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
				ItemStack s = inventory.getItem(i);
				if (s.is(Items.LAPIS_LAZULI)) {
					int take = Math.min(left, s.getCount());
					s.shrink(take);
					left -= take;
				}
			}
			inventory.setChanged();
		}

		EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.removeIf(holder -> holder.equals(target)));
		// A book without any enchantment left becomes a plain book again
		if (stack.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty()) {
			ItemStack plain = stack.transmuteCopy(Items.BOOK);
			plain.remove(DataComponents.STORED_ENCHANTMENTS);
			stack = plain;
		}
		input.set(stack);
		menu.broadcastChanges();
		player.level().playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE,
				SoundSource.BLOCKS, 1.0F, player.level().getRandom().nextFloat() * 0.1F + 0.9F);
		return true;
	}
}
