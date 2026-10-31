# EnchantPlus

A Fabric mod for Minecraft Java 26.3 that removes the randomness of the enchanting table.

You find enchanted books in structure chests, read them once to learn the enchantment for good, and then pick exactly what you want at the enchanting table.

## How it works

- Enchanted books only come from structure chests (dungeons, temples, mineshafts, strongholds, bastions, end cities and more). The structures are sorted into three tiers: common, medium and rare.
- Right-click a book to learn its enchantments permanently. All lower levels are unlocked with it. The book is used up.
- The enchanting table lists everything you have learned that fits the item in the table. Left click enchants, right click changes the level, the mouse wheel scrolls.
- Every enchantment costs experience levels and lapis lazuli. The price depends on three tiers and can be changed in the settings (default: 5 / 10 / 20 levels and 2 / 4 / 8 lapis).
- Higher levels need bookshelves around the table.
- Moving enchantments with the anvil is blocked. Repairing and renaming still work.
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
| Enchanting costs | For each of the three price tiers: the level limit, the level cost (1 to 100) and the lapis cost (0 to 64), typed into number fields |
| Book chance in chests | How often books appear in structure chests (0 to 200 percent) |
| Bookshelves required | Higher levels need bookshelves around the table |
| Anvil lock | Blocks moving enchantments with the anvil |
| Writing books | Allows writing learned enchantments onto plain books |
| Remove villager / fishing / piglin books | Removes enchanted books from these sources |
| Learning effect | Sound and particles when a book is read |
| Descriptions | Short description of each enchantment in the tooltip |

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

You may use EnchantPlus in modpacks. Please link to the official download (Modrinth or GitHub) instead of uploading the jar file somewhere else.

## Building

For personal use:

```
./gradlew build
```

The jar is placed in `build/libs`. Please do not publish modified versions.

## License

Copyright (c) 2026 Schluid. All rights reserved. See [LICENSE](LICENSE)
