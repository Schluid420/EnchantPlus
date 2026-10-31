# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-10-31

### Added

- Links to the GitHub repository and the issue tracker in the mod info.
- "Learned" button at the enchanting table: opens a scrollable list of all learned enchantments (grey if the enchantment does not fit the item in the table).
- Hint "Fits: sword, axe ..." in the tooltips of the enchantment list.
- Books give experience: a new book gives 1 level, an already known book gives 0.5 level. Both values are adjustable in the Mod Menu settings.
- Grindstone: a panel next to the grindstone lists the enchantments of the item; a click removes only that one for lapis lazuli taken from the inventory (default 2, adjustable in the Mod Menu). The normal grindstone behaviour is unchanged.
- Option "Items stay in table" (default off): the item and the lapis lazuli stay in the enchanting table after closing it, and drop when the table is destroyed.
- Option "Max enchantments" (default off): limits how many different enchantments an item can get at the table; existing ones can still be upgraded. The enchanting table shows a message when the item has reached the maximum.
- Option "Relaxed bookshelves" (default off): torches, carpets, grass etc. between table and bookshelf no longer block it, and chiseled bookshelves count by their books (3 books = 1 bookshelf).
- Option "Upgrade: difference" (default off): upgrading an enchantment the item already has only costs the price difference (levels and lapis lazuli).

### Changed

- New default prices: 1 / 5 / 10 levels and 3 / 5 / 10 lapis lazuli per tier (before: 5 / 10 / 20 and 2 / 4 / 8). Existing config files keep their values.
- An already known enchanted book is now used up and gives experience (before it was kept).

## [1.0.0] - 2026-10-30

### Added

- Learn enchantments permanently by right-clicking enchanted books.
- Enchanting table lists learned enchantments with selectable levels.
- Enchanted books only come from structure chests, in three tiers (common, medium, rare).
- Enchanting costs experience levels and lapis lazuli, set per price tier (default 5 / 10 / 20 levels and 2 / 4 / 8 lapis lazuli).
- Bookshelves are required for higher levels.
- Anvil lock: enchantments cannot be moved or combined at the anvil.
- Settings screen via Mod Menu, with optional switches for villager, fishing and piglin books.
- Learning effect and tooltip descriptions for enchantments.
- Commands `/enchantplus`, `/enchantplus learn` and `/enchantplus reset`.
- English and German translations.
