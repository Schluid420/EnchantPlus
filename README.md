# EnchantPlus

A Fabric mod for Minecraft Java 26.3 that removes the randomness of the enchanting table.

You find enchanted books in structure chests, read them once to learn the enchantment for good, and then pick exactly what you want at the enchanting table.

## How it works

- Enchanted books only come from structure chests (dungeons, temples, mineshafts, strongholds, bastions, end cities and more). The structures are sorted into three tiers: common, medium and rare.
- Right-click a book to learn its enchantments permanently. All lower levels are unlocked with it. The book is used up and gives experience: 1 level for a new book, 0.5 level for a book you already know (both adjustable).
- The enchanting table lists everything you have learned that fits the item in the table. Left click enchants, right click changes the level, the mouse wheel scrolls. The "Learned" button next to the window shows everything you have learned, with a hint which items it fits.
- Every enchantment costs experience levels and lapis lazuli. The price depends on three tiers and can be changed in the settings (default: 1 / 5 / 10 levels and 3 / 5 / 10 lapis).
- Every enchantment needs bookshelves around the table: half of its required level, rounded up (at least 1). Can be switched off in the settings.
- Moving enchantments with the anvil is blocked. Repairing and renaming still work.
- At the grindstone you can remove one chosen enchantment for lapis lazuli from your inventory (default 2). The normal grindstone still works.
- Optional switches remove enchanted books from villager trades, fishing and piglin bartering.

## Requirements

- Minecraft Java Edition 26.3
- Fabric Loader 0.19.2 or newer
- Fabric API
- Java 25
- [Mod Menu](https://modrinth.com/mod/modmenu) is optional and adds the settings screen

The mod must be installed on the client and on the server.

## Settings

Open the settings from Mod Menu, or edit `config/enchantplus.json`. On a server, the server's file decides.

| Setting | Meaning |
| --- | --- |
| Enchanting costs | For each of the three price tiers: the level limit, the level cost (1 to 100) and the lapis cost (0 to 64), typed into number fields. The same menu has the lapis cost for the grindstone (1 to 100) |
| Book chance in chests | How often books appear in structure chests (0 to 200 percent) |
| Bookshelves required | Every enchantment needs bookshelves around the table (half of the required level, at least 1) |
| Anvil lock | Blocks moving enchantments with the anvil |
| Writing books | Allows writing learned enchantments onto plain books |
| Remove villager / fishing / piglin books | Removes enchanted books from these sources |
| Learning effect | Sound and particles when a book is read |
| Descriptions | Short description of each enchantment in the tooltip |
| Book experience | Experience for a new book and for an already known book, in percent of one level (default 100 and 50) |
| Items stay in table | The item and the lapis stay in the enchanting table after closing it (default off) |
| Max enchantments | Maximum number of different enchantments per item at the table, off by default |
| Relaxed bookshelves | Torches, carpets etc. do not block a bookshelf, chiseled bookshelves count by books, 3 books = 1 bookshelf (default off) |
| Upgrade: difference | Upgrading an existing enchantment only costs the price difference (default off) |

If the config file cannot be read, the old file is kept as `enchantplus.json.broken` and defaults are used.

## Commands

For game masters:

- `/enchantplus` lists your knowledge
- `/enchantplus learn <enchantment> <level>`
- `/enchantplus reset`

## Customizing loot

Most changes can be made in the settings. If you want to change which books can appear, use your own datapack. You do not need to change the mod itself.

Put a file with the same path into your datapack, for example `data/enchantplus/loot_table/books/rare.json`. It replaces the mod's version of that loot table. The three tiers are `common`, `medium` and `rare`.

## Translations

Currently English and German. You can add another language with your own resource pack (`assets/enchantplus/lang/<code>.json`, based on `en_us.json`).

If you would like your translation to be part of the mod, open an issue or a pull request on GitHub, and I will add it.

## Modpacks

You may use EnchantPlus in modpacks. Please link to the official download (CurseForge or GitHub) instead of uploading the jar file somewhere else.

## Building

For personal use:

```
./gradlew build
```

The jar is placed in `build/libs`. Please do not publish modified versions.

## License

Copyright (c) 2026 Schluid. All rights reserved. See [LICENSE](LICENSE)
