package io.github.schluid.enchantplus.mixin;

import io.github.schluid.enchantplus.config.ModConfig;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional: new librarian book offers become experience bottle offers. Existing trades stay. */
@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {
	@Inject(method = "addOffersFromTradeSet", at = @At("TAIL"))
	private void enchantplus$replaceBooks(ServerLevel level, MerchantOffers offers, ResourceKey<TradeSet> key,
			CallbackInfo ci) {
		if (!ModConfig.blockVillagerBooks) {
			return;
		}
		for (int i = 0; i < offers.size(); i++) {
			MerchantOffer offer = offers.get(i);
			if (!offer.getResult().is(Items.ENCHANTED_BOOK)) {
				continue;
			}
			int emeralds = Math.max(1, offer.getItemCostA().count());
			int bottles = Math.max(1, Math.min(64, emeralds / Math.max(1, ModConfig.emeraldsPerBottle)));
			offers.set(i, new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), Optional.empty(),
					new ItemStack(Items.EXPERIENCE_BOTTLE, bottles), offer.getMaxUses(), offer.getXp(),
					offer.getPriceMultiplier()));
		}
	}
}
