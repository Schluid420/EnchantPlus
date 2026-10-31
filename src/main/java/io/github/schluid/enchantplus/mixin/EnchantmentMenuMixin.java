package io.github.schluid.enchantplus.mixin;

import io.github.schluid.enchantplus.knowledge.Bookshelves;
import io.github.schluid.enchantplus.knowledge.EnchantOptions;
import io.github.schluid.enchantplus.knowledge.ShelfCount;
import java.util.List;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
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

/**
 * Hooks into the button clicks of the enchanting table menu.
 * Button ids from 1000 upwards belong to this mod: id = 1000 + entry * 16 + level - 1.
 * The vanilla buttons (ids 0 to 2) are left untouched.
 */
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

	/** Number of bookshelves, counted on the server and synced to the client like vanilla data. */
	@Unique
	private DataSlot enchantplus$shelves;

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			at = @At("RETURN"))
	private void enchantplus$initShelves(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		this.enchantplus$shelves = ((AbstractContainerMenuInvoker) (Object) this).enchantplus$addDataSlot(DataSlot.standalone());
		enchantplus$updateShelves();
	}

	@Inject(method = "slotsChanged", at = @At("HEAD"))
	private void enchantplus$onSlotsChanged(Container container, CallbackInfo ci) {
		enchantplus$updateShelves();
	}

	/** Only does something on the server: on the client the access is empty and the synced value stays. */
	@Unique
	private void enchantplus$updateShelves() {
		if (this.enchantplus$shelves == null) {
			return;
		}
		this.access.execute((world, pos) -> this.enchantplus$shelves.set(Bookshelves.count(world, pos)));
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

		// The server recomputes the same list as the client and never trusts the client
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
		int levelCost = EnchantOptions.levelCost(tier);
		int lapisCost = EnchantOptions.lapisCost(tier);

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
			int neededLevel = EnchantOptions.neededPlayerLevel(required, tier);
			if (player.experienceLevel < neededLevel) {
				serverPlayer.sendSystemMessage(Component.translatable("enchantplus.message.need_level", neededLevel));
				return false;
			}
		}

		// A plain book becomes an enchanted book
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
