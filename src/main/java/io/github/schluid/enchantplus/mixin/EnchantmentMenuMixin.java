package io.github.schluid.enchantplus.mixin;

import io.github.schluid.enchantplus.ModAttachments;
import io.github.schluid.enchantplus.config.ModConfig;
import io.github.schluid.enchantplus.knowledge.Bookshelves;
import io.github.schluid.enchantplus.knowledge.EnchantOptions;
import io.github.schluid.enchantplus.knowledge.ShelfCount;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Button id = 1000 + entry * 16 + level - 1; vanilla ids 0-2 untouched. */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin implements ShelfCount {
	@Shadow
	@Final
	private Container enchantSlots;

	@Shadow
	@Final
	private ContainerLevelAccess access;

	@Shadow
	public abstract void slotsChanged(Container container);

	/** Shelf count, synced to the client. */
	@Unique
	private DataSlot enchantplus$shelves;

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			at = @At("RETURN"))
	private void enchantplus$initShelves(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		this.enchantplus$shelves = ((AbstractContainerMenuInvoker) (Object) this).enchantplus$addDataSlot(DataSlot.standalone());
		enchantplus$updateShelves();
		enchantplus$loadItems();
	}

	@Inject(method = "slotsChanged", at = @At("HEAD"))
	private void enchantplus$onSlotsChanged(Container container, CallbackInfo ci) {
		enchantplus$updateShelves();
	}

	/** Server only: on the client the access is empty. */
	@Unique
	private void enchantplus$updateShelves() {
		if (this.enchantplus$shelves == null) {
			return;
		}
		this.access.execute((world, pos) -> this.enchantplus$shelves.set(Bookshelves.count(world, pos)));
	}

	/** Server only: moves the stored items into the slots. */
	@Unique
	private void enchantplus$loadItems() {
		this.access.execute((world, pos) -> {
			if (!(world instanceof ServerLevel)) {
				return;
			}
			BlockEntity entity = world.getBlockEntity(pos);
			if (entity == null) {
				return;
			}
			List<ItemStack> saved = entity.getAttachedOrElse(ModAttachments.TABLE_ITEMS, List.of());
			if (saved.isEmpty()) {
				return;
			}
			entity.removeAttached(ModAttachments.TABLE_ITEMS);
			for (int i = 0; i < saved.size() && i < 2; i++) {
				this.enchantSlots.setItem(i, saved.get(i).copy());
			}
		});
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void enchantplus$keepItems(Player player, CallbackInfo ci) {
		if (!ModConfig.keepTableItems || !(player instanceof ServerPlayer)) {
			return;
		}
		this.access.execute((world, pos) -> {
			BlockEntity entity = world.getBlockEntity(pos);
			if (entity == null) {
				return;
			}
			List<ItemStack> keep = new ArrayList<>();
			for (int i = 0; i < 2; i++) {
				keep.add(this.enchantSlots.getItem(i).copy());
			}
			if (keep.get(0).isEmpty() && keep.get(1).isEmpty()) {
				return;
			}
			// Another player may have left items here meanwhile: drop them, never overwrite
			List<ItemStack> previous = entity.getAttachedOrElse(ModAttachments.TABLE_ITEMS, List.of());
			for (ItemStack old : previous) {
				if (!old.isEmpty() && world instanceof ServerLevel serverLevel) {
					Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, old.copy());
				}
			}
			entity.setAttached(ModAttachments.TABLE_ITEMS, keep);
			this.enchantSlots.setItem(0, ItemStack.EMPTY);
			this.enchantSlots.setItem(1, ItemStack.EMPTY);
		});
	}

	@Override
	public int enchantplus$getShelves() {
		return this.enchantplus$shelves == null ? 0 : this.enchantplus$shelves.get();
	}

	@Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
	private void enchantplus$click(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
		if (id < EnchantOptions.BUTTON_BASE) {
			return;
		}
		cir.setReturnValue(enchantplus$apply(player, id));
	}

	@Unique
	private boolean enchantplus$apply(Player player, int id) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		ItemStack stack = this.enchantSlots.getItem(0);
		ItemStack lapis = this.enchantSlots.getItem(1);
		if (stack.isEmpty()) {
			serverPlayer.sendSystemMessage(Component.translatable("enchantplus.message.no_item"));
			return false;
		}

		// Recomputed on the server, the client is not trusted
		List<EnchantOptions.Option> options =
				EnchantOptions.forStack(player, stack, player.level().registryAccess());

		int code = id - EnchantOptions.BUTTON_BASE;
		int index = code / EnchantOptions.LEVELS_PER_ENTRY;
		int level = code % EnchantOptions.LEVELS_PER_ENTRY + 1;
		if (index < 0 || index >= options.size()) {
			serverPlayer.sendSystemMessage(Component.translatable("enchantplus.message.unavailable"));
			return false;
		}
		EnchantOptions.Option option = options.get(index);
		if (level > option.maxLevel() || level <= option.existingLevel()) {
			serverPlayer.sendSystemMessage(Component.translatable(
					"enchantplus.message.bad_level", option.maxLevel(), option.existingLevel()));
			return false;
		}

		int required = EnchantOptions.requiredLevel(option, level);
		int tier = EnchantOptions.tier(required);
		int levelCost = EnchantOptions.levelCostFor(option, level);
		int lapisCost = EnchantOptions.lapisCostFor(option, level);

		boolean free = player.hasInfiniteMaterials();
		if (!free) {
			int shelves = this.access.evaluate(Bookshelves::count, 0);
			int neededShelves = EnchantOptions.requiredShelves(required);
			if (shelves < neededShelves) {
				serverPlayer.sendSystemMessage(Component.translatable(
						"enchantplus.message.need_shelves", neededShelves, shelves));
				return false;
			}
			if (lapisCost > 0 && (!lapis.is(Items.LAPIS_LAZULI) || lapis.getCount() < lapisCost)) {
				serverPlayer.sendSystemMessage(Component.translatable("enchantplus.message.need_lapis", lapisCost));
				return false;
			}
			int neededLevel = Math.max(required, levelCost);
			if (player.experienceLevel < neededLevel) {
				serverPlayer.sendSystemMessage(Component.translatable("enchantplus.message.need_level", neededLevel));
				return false;
			}
		}

		if (stack.is(Items.BOOK)) {
			stack = stack.transmuteCopy(Items.ENCHANTED_BOOK);
			this.enchantSlots.setItem(0, stack);
		}

		if (!free) {
			player.onEnchantmentPerformed(stack, levelCost);
		}
		stack.enchant(option.holder(), level);
		if (!free) {
			lapis.consume(lapisCost, player);
		}
		if (lapis.isEmpty()) {
			this.enchantSlots.setItem(1, ItemStack.EMPTY);
		}

		player.awardStat(Stats.ENCHANT_ITEM);
		CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, stack, levelCost);
		this.enchantSlots.setChanged();
		this.slotsChanged(this.enchantSlots);
		this.access.execute((world, pos) -> world.playSound(
				null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS,
				1.0F, world.getRandom().nextFloat() * 0.1F + 0.9F));
		return true;
	}
}
