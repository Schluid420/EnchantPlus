package io.github.schluid.enchantplus.knowledge;

import io.github.schluid.enchantplus.ModAttachments;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.player.Player;

/** Knowledge of a player. Write methods are server only. */
public final class EnchantKnowledge {
	private EnchantKnowledge() {
	}

	/** Known level, 0 = unknown. */
	public static int getLevel(Player player, String enchantmentId) {
		return all(player).getOrDefault(enchantmentId, 0);
	}

	/** Read only. */
	public static Map<String, Integer> all(Player player) {
		return player.getAttachedOrElse(ModAttachments.KNOWLEDGE, Map.of());
	}

	/** Learns up to the level, lower levels included. @return true if something new was learned */
	public static boolean learn(Player player, String enchantmentId, int level) {
		int known = getLevel(player, enchantmentId);
		if (level <= known) {
			return false;
		}
		// The stored map may be immutable
		Map<String, Integer> copy = new HashMap<>(all(player));
		copy.put(enchantmentId, level);
		player.setAttached(ModAttachments.KNOWLEDGE, copy);
		return true;
	}

	public static void reset(Player player) {
		player.setAttached(ModAttachments.KNOWLEDGE, Map.of());
	}
}
