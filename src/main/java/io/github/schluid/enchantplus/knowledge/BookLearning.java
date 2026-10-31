package io.github.schluid.enchantplus.knowledge;

import io.github.schluid.enchantplus.config.ModConfig;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Right-clicking with an enchanted book in hand teaches the player its enchantments.
 * A new book gives XP (default 1 level), an already known book is consumed too and gives less (default 0.5 level).
 */
public final class BookLearning {
	private BookLearning() {
	}

	public static void register() {
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!stack.is(Items.ENCHANTED_BOOK)) {
				return InteractionResult.PASS;
			}
			// Learning only happens on the server
			if (!(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}

			ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
			if (stored == null || stored.isEmpty()) {
				return InteractionResult.PASS;
			}

			MutableComponent learned = Component.empty();
			boolean anythingNew = false;

			for (var entry : stored.entrySet()) {
				Holder<Enchantment> holder = entry.getKey();
				int bookLevel = entry.getIntValue();

				if (EnchantKnowledge.learn(serverPlayer, holder.getRegisteredName(), bookLevel)) {
					if (anythingNew) {
						learned.append(", ");
					}
					anythingNew = true;
					learned.append(Enchantment.getFullname(holder, bookLevel));
				}
			}

			if (anythingNew) {
				stack.shrink(1);
				giveXp(serverPlayer, ModConfig.bookXpNewPercent);
				serverPlayer.sendSystemMessage(Component.translatable("enchantplus.message.learned", learned));
				if (ModConfig.learnEffect) {
					playLearnEffect(serverPlayer);
				}
			} else if (ModConfig.bookXpDuplicatePercent > 0) {
				// Known book: it is consumed and gives (by default) half a level
				stack.shrink(1);
				giveXp(serverPlayer, ModConfig.bookXpDuplicatePercent);
				serverPlayer.sendSystemMessage(Component.translatable("enchantplus.message.already_known_xp"));
			} else {
				serverPlayer.sendSystemMessage(Component.translatable("enchantplus.message.already_known"));
			}
			return InteractionResult.SUCCESS;
		});
	}

	/** Gives a part of ONE level (percent) as experience points, based on the player's current level. */
	private static void giveXp(ServerPlayer player, int percent) {
		int points = Math.round(player.getXpNeededForNextLevel() * percent / 100.0F);
		if (points > 0) {
			player.giveExperiencePoints(points);
		}
	}

	/** Sound plus a swirl of enchanting particles around the player. */
	private static void playLearnEffect(ServerPlayer player) {
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
				SoundSource.PLAYERS, 0.6F, 1.2F);
		player.level().sendParticles(ParticleTypes.ENCHANT,
				player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.5, 0.8, 0.5, 0.5);
	}
}
