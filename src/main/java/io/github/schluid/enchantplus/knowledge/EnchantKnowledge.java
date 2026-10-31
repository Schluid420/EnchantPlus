package io.github.schluid.enchantplus.knowledge;

import io.github.schluid.enchantplus.ModAttachments;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.player.Player;

/**
 * Access to the enchantment knowledge of a player.
 * Writing methods must only be called on the server.
 */
public final class EnchantKnowledge {
	private EnchantKnowledge() {
	}

	/** Returns up to which level the player knows the enchantment (0 = unknown). */
	public static int getLevel(Player player, String enchantmentId) {
		return all(player).getOrDefault(enchantmentId, 0);
	}

	/** All learned enchantments (read only, do not modify the returned map). */
	public static Map<String, Integer> all(Player player) {
		return player.getAttachedOrElse(ModAttachments.KNOWLEDGE, Map.of());
	}

	/**
	 * Learns an enchantment up to the given level. All lower levels are unlocked with it.
	 *
	 * @return true if the player improved, false if they already knew this level
	 */
	public static boolean learn(Player player, String enchantmentId, int level) {
		int known = getLevel(player, enchantmentId);
		if (level <= known) {
			return false;
		}
		// Copy first, the stored map may be immutable
		Map<String, Integer> copy = new HashMap<>(all(player));
		copy.put(enchantmentId, level);
		player.setAttached(ModAttachments.KNOWLEDGE, copy);
		return true;
	}

	/** Clears all knowledge. */
	public static void reset(Player player) {
		player.setAttached(ModAttachments.KNOWLEDGE, Map.of());
	}
}
