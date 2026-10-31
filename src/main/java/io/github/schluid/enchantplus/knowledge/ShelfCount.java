package io.github.schluid.enchantplus.knowledge;

/**
 * Implemented by the enchanting menu (through a mixin).
 * The server counts the bookshelves and syncs the number to the client, so the screen can show it.
 */
public interface ShelfCount {
	int enchantplus$getShelves();
}
