# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.0] - 2026-11-22

### Added

- Multiplayer: the server decides the settings. On join the server sends all game settings to the client, which uses them only in memory; the own settings come back when leaving. On a server the settings screen is locked for normal players (greyed out, with a hint) and open for operators, whose changes are saved by the server and sent to every player. "Descriptions" stays the player's own choice. Single player and LAN hosts use their own settings.
- Version check: a chat warning if the client and server versions differ, and `/enchantplus versions` lists the versions of all players.
- `/reload` reads `config/enchantplus.json` again on a server and sends the settings to everyone.

### Changed

- All 128 enchanted book textures redrawn in 128 x 128: thinner books, clean edges, smooth shading and more details per level (stitched leather, iron corners with rivets, gold double frame, cut gem, glowing gem with sparkles).
- Requires Fabric Loader 0.20.1 or newer (before: 0.19.2).
- Loot chance and book experience can now be set up to 500 percent in the settings screen (before: 200; the config file already allowed 500).
- In creative mode (infinite materials) a book is no longer used up when you read it.
- The grindstone panel scrolls when an item has more than 8 enchantments.
- Removing an enchantment at the grindstone must be confirmed: the first click turns the row red ("Really remove?"), the second click removes it. A click anywhere else, or 6 seconds without a click, cancels.
- New window frame in the colours of the mod icon (purple with gold studs and a title bar) for the table and grindstone windows. The title text of the "Learned" window is removed.
- JEI support: JEI keeps its item list away from the "Learned" window and the grindstone window, so it no longer covers them or takes their clicks.

### Fixed

- "Items stay in table": when two players used the same table at the same time, items left by one player could be overwritten by the other. Old items are now dropped instead.
- The settings that a client sends to the server are limited to 64 KB (protection against oversized data).
- Price tier 2 can no longer end below tier 1 when the config file is read.

## [1.2.0] - 2026-11-14

### Added

- Every vanilla enchanted book has its own texture: a closed book lying on its side, with a cover colour per item group, an ornament and a bookmark colour per enchantment. The fittings show the level: leather, iron, gold, gold with a gem, gold with a glowing gem. Enchantments with only one level always look like the highest level. Books with several enchantments and books of other mods keep the vanilla look.

### Changed

- New settings screen in the style of GearBalance: tabs (Costs, Books, Enchanting Table), a table with a field per value, a reset button per row and a "Reset defaults" button. Colours: white = default, green = saved, yellow = not saved yet, red = invalid. Hover over a name for an explanation.
- The separate "Enchanting costs" screen is now the "Costs" tab.
- "Remove villager books" is now "Replace villager books": librarians sell experience bottles instead of enchanted books, for the same emeralds. How many emeralds one bottle costs can be set ("Emeralds per bottle", default 3).

### Fixed

- The new default prices from 1.1.0 (1 / 5 / 10 levels, 3 / 5 / 10 lapis lazuli) were not used for new config files.

### Updating from 1.1.0

- Learned enchantments and all settings are kept.
- Config files created by 1.1.0 still contain the old prices (5 / 10 / 20 levels, 2 / 4 / 8 lapis lazuli). To use the new defaults, click "Reset defaults" in the settings or delete `config/enchantplus.json`.
- Librarians that already have their trades keep them. Only newly employed librarians sell experience bottles.

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
