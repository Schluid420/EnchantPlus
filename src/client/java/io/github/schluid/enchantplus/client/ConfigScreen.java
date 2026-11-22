package io.github.schluid.enchantplus.client;

import io.github.schluid.enchantplus.config.ModConfig;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

/** Mod Menu settings screen in GearBalance style. Saved on close. */
public class ConfigScreen extends Screen {
	private static final int TAB_Y = 22;
	private static final int HEADER_Y = 50;
	private static final int TABLE_TOP = 62;
	private static final int ROW_HEIGHT = 22;
	private static final int BOTTOM_SPACE = 50;
	private static final int NAME_WIDTH = 150;
	private static final int COLUMN_WIDTH = 70;
	private static final int BOX_WIDTH = 50;
	private static final int MAX_COLUMNS = 3;
	private static final int RESET_WIDTH = 20;
	private static final int TOOLTIP_WIDTH = 200;
	private static final int TABLE_WIDTH = NAME_WIDTH + MAX_COLUMNS * COLUMN_WIDTH + RESET_WIDTH + 4;

	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREY = 0xFFA0A0A0;
	private static final int RED = 0xFFFF5555;
	private static final int YELLOW = 0xFFFFE066;
	private static final int GREEN = 0xFF66E066;
	private static final int ROW_DARK = 0x30000000;
	private static final int ROW_LIGHT = 0x18FFFFFF;

	private enum Tab {
		COSTS,
		BOOKS,
		TABLE;

		String key() {
			return "enchantplus.config.tab." + name().toLowerCase(Locale.ROOT);
		}
	}

	/** On/off is stored as 1/0, so all cells work the same. */
	private record Cell(String id, IntSupplier current, IntConsumer setter, int def, int min, int max,
			boolean toggle, String suffix) {
	}

	/** A null cell leaves the column empty; key + ".tip" is the tooltip. */
	private record Row(String key, Component name, List<Cell> cells) {
	}

	// Kept while the game runs
	private static Tab tab = Tab.COSTS;
	private static final Map<Tab, Integer> SCROLL = new EnumMap<>(Tab.class);

	/** Values from the config file when the screen was opened. */
	private final Map<String, Integer> saved = new HashMap<>();
	private final Screen parent;
	/** The settings when the screen opened, to see whether an operator changed those of the server. */
	private final String settingsAtOpen = ModConfig.settingsToJson().toString();
	private final List<Row> rows = new ArrayList<>();
	private List<String> headers = List.of();
	private int visibleRows;
	/** Updated only when a value changes. */
	private int changes;
	private final List<int[]> resetButtons = new ArrayList<>();
	/** Drawn by hand, the arrow of the font is too small. */
	private static final String[] RESET_ICON = {
			"..###.#",
			".#...##",
			"#...###",
			"#......",
			"#.....#",
			".#...#.",
			"..###.."};
	private static final int RESET_PIXEL = 2;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("enchantplus.config.title"));
		this.parent = parent;
		for (Tab t : Tab.values()) {
			for (Row row : buildRows(t)) {
				for (Cell cell : row.cells()) {
					if (cell != null) {
						saved.putIfAbsent(cell.id(), cell.current().getAsInt());
					}
				}
			}
		}
	}

	private static List<String> headersFor(Tab t) {
		if (t == Tab.COSTS) {
			return List.of("enchantplus.config.col_limit", "enchantplus.config.col_levels", "enchantplus.config.col_lapis");
		}
		return List.of("enchantplus.config.col_value");
	}

	private static List<Row> buildRows(Tab t) {
		List<Row> list = new ArrayList<>();
		switch (t) {
			case COSTS -> {
				for (int tier = 0; tier < 3; tier++) {
					int i = tier;
					List<Cell> cells = new ArrayList<>();
					// Tier 3 has no limit
					cells.add(i < 2 ? number("limit" + i, () -> ModConfig.tierLimit[i], v -> ModConfig.tierLimit[i] = v,
							ModConfig.DEFAULT_TIER_LIMIT[i], 1, 100, "") : null);
					cells.add(number("levels" + i, () -> ModConfig.levelCost[i], v -> ModConfig.levelCost[i] = v,
							ModConfig.DEFAULT_LEVEL_COST[i], 1, 100, ""));
					cells.add(number("lapis" + i, () -> ModConfig.lapisCost[i], v -> ModConfig.lapisCost[i] = v,
							ModConfig.DEFAULT_LAPIS_COST[i], 0, 64, ""));
					list.add(new Row("enchantplus.config.tier_row",
							Component.translatable("enchantplus.config.tier_row", i + 1), cells));
				}
				List<Cell> grind = new ArrayList<>();
				grind.add(null);
				grind.add(null);
				grind.add(number("grind", () -> ModConfig.grindLapisCost, v -> ModConfig.grindLapisCost = v, 2, 1, 100, ""));
				list.add(row("enchantplus.config.grind_row", grind));
			}
			case BOOKS -> {
				list.add(row("enchantplus.config.loot", List.of(number("loot", () -> ModConfig.lootChancePercent,
						v -> ModConfig.lootChancePercent = v, 100, 0, 500, "%"))));
				list.add(row("enchantplus.config.book_xp_new", List.of(number("xpNew", () -> ModConfig.bookXpNewPercent,
						v -> ModConfig.bookXpNewPercent = v, 100, 0, 500, "%"))));
				list.add(row("enchantplus.config.book_xp_dup", List.of(number("xpDup", () -> ModConfig.bookXpDuplicatePercent,
						v -> ModConfig.bookXpDuplicatePercent = v, 50, 0, 500, "%"))));
				list.add(row("enchantplus.config.effect", List.of(toggle("effect", () -> ModConfig.learnEffect,
						v -> ModConfig.learnEffect = v, true))));
				list.add(row("enchantplus.config.villager_books", List.of(toggle("villager", () -> ModConfig.blockVillagerBooks,
						v -> ModConfig.blockVillagerBooks = v, false))));
				list.add(row("enchantplus.config.emeralds_per_bottle", List.of(number("bottle", () -> ModConfig.emeraldsPerBottle,
						v -> ModConfig.emeraldsPerBottle = v, 3, 1, 64, ""))));
				list.add(row("enchantplus.config.fishing_books", List.of(toggle("fishing", () -> ModConfig.blockFishingBooks,
						v -> ModConfig.blockFishingBooks = v, false))));
				list.add(row("enchantplus.config.piglin_books", List.of(toggle("piglin", () -> ModConfig.blockPiglinBooks,
						v -> ModConfig.blockPiglinBooks = v, false))));
			}
			case TABLE -> {
				list.add(row("enchantplus.config.shelves", List.of(toggle("shelves", () -> ModConfig.requireShelves,
						v -> ModConfig.requireShelves = v, true))));
				list.add(row("enchantplus.config.relaxed_shelves", List.of(toggle("relaxed", () -> ModConfig.relaxedShelves,
						v -> ModConfig.relaxedShelves = v, false))));
				list.add(row("enchantplus.config.anvil", List.of(toggle("anvil", () -> ModConfig.anvilLock,
						v -> ModConfig.anvilLock = v, true))));
				list.add(row("enchantplus.config.books", List.of(toggle("writing", () -> ModConfig.allowBookWriting,
						v -> ModConfig.allowBookWriting = v, true))));
				list.add(row("enchantplus.config.descriptions", List.of(toggle("descriptions", () -> ModConfig.showDescriptions,
						v -> ModConfig.showDescriptions = v, true))));
				list.add(row("enchantplus.config.keep_items", List.of(toggle("keep", () -> ModConfig.keepTableItems,
						v -> ModConfig.keepTableItems = v, false))));
				list.add(row("enchantplus.config.max_ench", List.of(number("maxEnch", () -> ModConfig.maxEnchantments,
						v -> ModConfig.maxEnchantments = v, 0, 0, 50, ""))));
				list.add(row("enchantplus.config.upgrade_diff", List.of(toggle("upgrade", () -> ModConfig.upgradeDifference,
						v -> ModConfig.upgradeDifference = v, false))));
			}
		}
		return list;
	}

	private static Row row(String key, List<Cell> cells) {
		return new Row(key, Component.translatable(key), cells);
	}

	private static Cell number(String id, IntSupplier current, IntConsumer setter, int def, int min, int max, String suffix) {
		return new Cell(id, current, setter, def, min, max, false, suffix);
	}

	private interface BoolSetter {
		void set(boolean value);
	}

	private interface BoolGetter {
		boolean get();
	}

	private static Cell toggle(String id, BoolGetter current, BoolSetter setter, boolean def) {
		return new Cell(id, () -> current.get() ? 1 : 0, v -> setter.set(v != 0), def ? 1 : 0, 0, 1, true, "");
	}

	/** Locked: the value comes from a server and the player is no operator. Own view options stay open. */
	private static boolean locked(Cell cell) {
		return ServerSettingsClient.locked() && !"descriptions".equals(cell.id());
	}

	private static boolean rowLocked(Row row) {
		return row.cells().stream().filter(c -> c != null).allMatch(ConfigScreen::locked);
	}

	/** White = default, green = saved, yellow = not saved yet. */
	private int color(Cell cell, int value) {
		if (value == cell.def()) {
			return WHITE;
		}
		Integer inFile = saved.get(cell.id());
		return inFile != null && inFile == value ? GREEN : YELLOW;
	}

	private static ChatFormatting format(int color) {
		return color == GREEN ? ChatFormatting.GREEN : color == YELLOW ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
	}

	private static int changeCount() {
		int count = 0;
		for (Tab t : Tab.values()) {
			for (Row row : buildRows(t)) {
				for (Cell cell : row.cells()) {
					if (cell != null && cell.current().getAsInt() != cell.def()) {
						count++;
					}
				}
			}
		}
		return count;
	}

	private static void resetAll() {
		for (Tab t : Tab.values()) {
			for (Row row : buildRows(t)) {
				resetRow(row);
			}
		}
	}

	private static void resetRow(Row row) {
		for (Cell cell : row.cells()) {
			if (cell != null) {
				cell.setter().accept(cell.def());
			}
		}
	}

	private int tableLeft() {
		return this.width / 2 - TABLE_WIDTH / 2;
	}

	/** Single-column tabs sit next to the reset buttons. */
	private int columnX(int column) {
		int offset = MAX_COLUMNS - headers.size();
		return tableLeft() + NAME_WIDTH + (column + offset) * COLUMN_WIDTH;
	}

	private int resetX() {
		return tableLeft() + NAME_WIDTH + MAX_COLUMNS * COLUMN_WIDTH + 2;
	}

	private int scroll() {
		return SCROLL.getOrDefault(tab, 0);
	}

	private int maxScroll() {
		return Math.max(0, rows.size() - visibleRows);
	}

	@Override
	protected void init() {
		rows.clear();
		rows.addAll(buildRows(tab));
		headers = headersFor(tab);
		changes = changeCount();
		visibleRows = Math.max(3, (this.height - TABLE_TOP - BOTTOM_SPACE) / ROW_HEIGHT);
		SCROLL.put(tab, Math.max(0, Math.min(scroll(), maxScroll())));

		Tab[] tabs = Tab.values();
		int tabWidth = Math.min(90, (this.width - 20) / tabs.length - 4);
		int tabsLeft = this.width / 2 - (tabs.length * (tabWidth + 4) - 4) / 2;
		for (int i = 0; i < tabs.length; i++) {
			Tab t = tabs[i];
			Button button = Button.builder(Component.translatable(t.key()), b -> {
				tab = t;
				this.rebuildWidgets();
			}).bounds(tabsLeft + i * (tabWidth + 4), TAB_Y, tabWidth, 20)
					.tooltip(Tooltip.create(Component.translatable(t.key() + ".tip"))).build();
			button.active = t != tab;
			this.addRenderableWidget(button);
		}

		resetButtons.clear();
		for (int r = 0; r < visibleRows; r++) {
			int index = scroll() + r;
			if (index >= rows.size()) {
				break;
			}
			Row row = rows.get(index);
			int y = TABLE_TOP + r * ROW_HEIGHT;

			for (int c = 0; c < row.cells().size(); c++) {
				Cell cell = row.cells().get(c);
				if (cell == null) {
					continue;
				}
				if (cell.toggle()) {
					this.addRenderableWidget(toggleButton(cell, row.name(), columnX(c), y + 1));
				} else {
					this.addRenderableWidget(numberBox(cell, row.name(), columnX(c), y + 2));
				}
			}

			// The icon is drawn in extractRenderState
			resetButtons.add(new int[] {resetX(), y + 1});
			Button resetButton = Button.builder(Component.empty(), b -> {
				resetRow(row);
				this.rebuildWidgets();
			}).bounds(resetX(), y + 1, RESET_WIDTH, ROW_HEIGHT - 2)
					.tooltip(Tooltip.create(Component.translatable("enchantplus.config.reset_row"))).build();
			resetButton.active = !rowLocked(row);
			this.addRenderableWidget(resetButton);
		}

		int buttonWidth = 120;
		int bottomLeft = this.width / 2 - (2 * (buttonWidth + 4) - 4) / 2;
		int by = this.height - 28;
		Button allDefault = Button.builder(Component.translatable("enchantplus.config.all_default"), b -> {
			BooleanConsumer answer = yes -> {
				if (yes) {
					resetAll();
				}
				this.minecraft.setScreenAndShow(this);
			};
			this.minecraft.setScreenAndShow(new ConfirmScreen(answer,
					Component.translatable("enchantplus.config.reset_title"),
					Component.translatable("enchantplus.config.reset_message"),
					Component.translatable("gui.yes"),
					Component.translatable("gui.no")));
		}).bounds(bottomLeft, by, buttonWidth, 20)
				.tooltip(Tooltip.create(Component.translatable("enchantplus.config.all_default.tip"))).build();
		allDefault.active = !ServerSettingsClient.locked();
		this.addRenderableWidget(allDefault);
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
				.bounds(bottomLeft + buttonWidth + 4, by, buttonWidth, 20).build());
	}

	private EditBox numberBox(Cell cell, Component name, int x, int y) {
		EditBox box = new EditBox(this.font, x, y, BOX_WIDTH, ROW_HEIGHT - 4, name);
		box.setMaxLength(3);
		box.setValue(String.valueOf(cell.current().getAsInt()));
		box.setTextColor(locked(cell) ? GREY : color(cell, cell.current().getAsInt()));
		box.setEditable(!locked(cell));
		box.setTooltip(Tooltip.create(valueTip(cell)));
		box.setResponder(text -> {
			Integer parsed = parse(text);
			if (parsed == null || parsed < cell.min() || parsed > cell.max()) {
				box.setTextColor(RED);
				return;
			}
			cell.setter().accept(parsed);
			box.setTextColor(color(cell, parsed));
			changes = changeCount();
		});
		return box;
	}

	private CycleButton<Boolean> toggleButton(Cell cell, Component name, int x, int y) {
		CycleButton<Boolean> toggle = CycleButton.<Boolean>builder(value -> Component.translatable(value ? "options.on" : "options.off")
						.withStyle(locked(cell) ? ChatFormatting.GRAY : format(color(cell, value ? 1 : 0))), cell.current().getAsInt() != 0)
				.withValues(List.of(true, false))
				.displayOnlyValue()
				.withTooltip(value -> Tooltip.create(valueTip(cell)))
				.create(x, y, BOX_WIDTH, ROW_HEIGHT - 2, name, (button, value) -> {
					cell.setter().accept(value ? 1 : 0);
					// Rebuild so the text colour matches
					this.rebuildWidgets();
				});
		toggle.active = !locked(cell);
		return toggle;
	}

	private static Component valueTip(Cell cell) {
		MutableComponent tip = Component.translatable("enchantplus.config.default", cell.toggle()
				? Component.translatable(cell.def() != 0 ? "options.on" : "options.off")
				: Component.literal(cell.def() + cell.suffix()));
		if (!cell.toggle()) {
			tip.append("\n").append(Component.translatable("enchantplus.config.range", cell.min(), cell.max())
					.withStyle(ChatFormatting.GRAY));
		}
		if (locked(cell)) {
			tip.append("\n").append(Component.translatable("enchantplus.server.locked.tip").withStyle(ChatFormatting.GOLD));
		}
		return tip;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0 && maxScroll() > 0) {
			int next = Math.max(0, Math.min(maxScroll(), scroll() + (scrollY > 0 ? -1 : 1)));
			if (next != scroll()) {
				SCROLL.put(tab, next);
				this.rebuildWidgets();
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int left = tableLeft();
		int right = left + TABLE_WIDTH;

		for (int r = 0; r < visibleRows && scroll() + r < rows.size(); r++) {
			int y = TABLE_TOP + r * ROW_HEIGHT;
			graphics.fill(left, y, right, y + ROW_HEIGHT, r % 2 == 0 ? ROW_DARK : ROW_LIGHT);
		}

		super.extractRenderState(graphics, mouseX, mouseY, delta);

		MutableComponent title = this.title.copy().append(Component.translatable("enchantplus.config.changes",
				changes).withStyle(ChatFormatting.GRAY));
		if (ModConfig.serverMode()) {
			title.append(Component.translatable(ModConfig.serverCanEdit() ? "enchantplus.server.editing"
					: "enchantplus.server.locked").withStyle(ChatFormatting.GOLD));
		}
		graphics.centeredText(this.font, title, this.width / 2, 8, WHITE);

		graphics.text(this.font, Component.translatable("enchantplus.config.col_setting"), left + 4, HEADER_Y, GREY);
		for (int c = 0; c < headers.size(); c++) {
			graphics.text(this.font, Component.translatable(headers.get(c)), columnX(c), HEADER_Y, GREY);
		}
		graphics.horizontalLine(left, right - 1, TABLE_TOP - 2, 0xFF606060);

		for (int r = 0; r < visibleRows && scroll() + r < rows.size(); r++) {
			Row row = rows.get(scroll() + r);
			int y = TABLE_TOP + r * ROW_HEIGHT;
			int textY = y + (ROW_HEIGHT - 8) / 2;
			String name = this.font.plainSubstrByWidth(row.name().getString(), NAME_WIDTH - 8);
			graphics.text(this.font, name, left + 4, textY, WHITE);
			for (int c = 0; c < row.cells().size(); c++) {
				Cell cell = row.cells().get(c);
				if (cell != null && !cell.suffix().isEmpty()) {
					graphics.text(this.font, cell.suffix(), columnX(c) + BOX_WIDTH + 3, textY, GREY);
				}
			}
			if (tab == Tab.COSTS && r + scroll() == 2) {
				graphics.text(this.font, Component.translatable("enchantplus.config.above"), columnX(0) + 4, textY, GREY);
			}
			if (mouseX >= left && mouseX < left + NAME_WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT) {
				List<FormattedCharSequence> lines = new ArrayList<>(this.font.split(row.name(), TOOLTIP_WIDTH));
				lines.addAll(this.font.split(Component.translatable(row.key() + ".tip").withStyle(ChatFormatting.GRAY),
						TOOLTIP_WIDTH));
				graphics.setTooltipForNextFrame(this.font, lines, mouseX, mouseY);
			}
		}

		int iconSize = RESET_ICON.length * RESET_PIXEL;
		for (int[] pos : resetButtons) {
			int left0 = pos[0] + (RESET_WIDTH - iconSize) / 2;
			int top0 = pos[1] + (ROW_HEIGHT - 2 - iconSize) / 2;
			drawIcon(graphics, left0 + 1, top0 + 1, 0xFF3F3F3F);
			drawIcon(graphics, left0, top0, WHITE);
		}

		if (maxScroll() > 0) {
			int trackHeight = visibleRows * ROW_HEIGHT;
			int barHeight = Math.max(10, trackHeight * visibleRows / rows.size());
			int barY = TABLE_TOP + (trackHeight - barHeight) * scroll() / maxScroll();
			graphics.fill(right + 2, TABLE_TOP, right + 5, TABLE_TOP + trackHeight, 0x40FFFFFF);
			graphics.fill(right + 2, barY, right + 5, barY + barHeight, 0xFFC0C0C0);
		}

		String note = tab == Tab.COSTS ? "enchantplus.config.tiers_note" : "enchantplus.config.note";
		graphics.centeredText(this.font, Component.translatable(note), this.width / 2, this.height - 42, GREY);
	}

	private static void drawIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
		for (int row = 0; row < RESET_ICON.length; row++) {
			String line = RESET_ICON[row];
			for (int col = 0; col < line.length(); col++) {
				if (line.charAt(col) == '#') {
					int px = x + col * RESET_PIXEL;
					int py = y + row * RESET_PIXEL;
					graphics.fill(px, py, px + RESET_PIXEL, py + RESET_PIXEL, color);
				}
			}
		}
	}

	@Override
	public void onClose() {
		// Tier 2 must not end below tier 1
		if (ModConfig.tierLimit[1] < ModConfig.tierLimit[0]) {
			ModConfig.tierLimit[1] = ModConfig.tierLimit[0];
		}
		ModConfig.save();
		// Server settings: an operator sends the changes, the server saves them
		if (ModConfig.serverMode() && !ModConfig.settingsToJson().toString().equals(settingsAtOpen)) {
			ServerSettingsClient.sendToServer();
		}
		this.minecraft.setScreenAndShow(this.parent);
	}

	private static Integer parse(String text) {
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
