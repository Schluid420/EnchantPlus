package io.github.schluid.enchantplus.mixin;

import io.github.schluid.enchantplus.config.ModConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional: removes enchanted book offers from villagers when the new trades are created.
 * Villagers that already have their trades keep them.
 */
@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {
	@Inject(method = "addOffersFromTradeSet", at = @At("TAIL"))
	private void enchantplus$removeBooks(ServerLevel level, MerchantOffers offers, ResourceKey<TradeSet> key,
			CallbackInfo ci) {
		if (ModConfig.blockVillagerBooks) {
			offers.removeIf(offer -> offer.getResult().is(Items.ENCHANTED_BOOK));
		}
	}
}
