package io.github.schluid.enchantplus.client;

import io.github.schluid.enchantplus.config.ModConfig;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Settings screen, opened from Mod Menu ("Configure" button).
 * Uses only vanilla buttons, so no extra library is needed.
 */
public class ConfigScreen extends Screen {
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int SPACING = 22;

	private final Screen parent;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("enchantplus.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = this.width / 2 - BUTTON_WIDTH - 5;
		int right = this.width / 2 + 5;
		int y = this.height / 9;

		this.addRenderableWidget(Button.builder(Component.translatable("enchantplus.config.costs_menu"),
						button -> this.minecraft.setScreenAndShow(new CostScreen(this)))
				.bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		this.addRenderableWidget(toggleButton(right, y, "enchantplus.config.shelves",
				ModConfig.requireShelves, value -> ModConfig.requireShelves = value));
		y += SPACING;
		this.addRenderableWidget(percentButton(left, y, "enchantplus.config.loot",
				List.of(0, 25, 50, 100, 150, 200), ModConfig.lootChancePercent, value -> ModConfig.lootChancePercent = value));
		this.addRenderableWidget(toggleButton(right, y, "enchantplus.config.anvil",
				ModConfig.anvilLock, value -> ModConfig.anvilLock = value));
		y += SPACING;
		this.addRenderableWidget(toggleButton(left, y, "enchantplus.config.books",
				ModConfig.allowBookWriting, value -> ModConfig.allowBookWriting = value));
		this.addRenderableWidget(toggleButton(right, y, "enchantplus.config.villager_books",
				ModConfig.blockVillagerBooks, value -> ModConfig.blockVillagerBooks = value));
		y += SPACING;
		this.addRenderableWidget(toggleButton(left, y, "enchantplus.config.fishing_books",
				ModConfig.blockFishingBooks, value -> ModConfig.blockFishingBooks = value));
		this.addRenderableWidget(toggleButton(right, y, "enchantplus.config.piglin_books",
				ModConfig.blockPiglinBooks, value -> ModConfig.blockPiglinBooks = value));
		y += SPACING;
		this.addRenderableWidget(toggleButton(left, y, "enchantplus.config.effect",
				ModConfig.learnEffect, value -> ModConfig.learnEffect = value));
		this.addRenderableWidget(toggleButton(right, y, "enchantplus.config.descriptions",
				ModConfig.showDescriptions, value -> ModConfig.showDescriptions = value));
		y += SPACING;
		this.addRenderableWidget(percentButton(left, y, "enchantplus.config.book_xp_new",
				List.of(0, 25, 50, 100, 150, 200), ModConfig.bookXpNewPercent, value -> ModConfig.bookXpNewPercent = value));
		this.addRenderableWidget(percentButton(right, y, "enchantplus.config.book_xp_dup",
				List.of(0, 25, 50, 100, 150, 200), ModConfig.bookXpDuplicatePercent, value -> ModConfig.bookXpDuplicatePercent = value));
		y += SPACING;
		this.addRenderableWidget(toggleButton(left, y, "enchantplus.config.keep_items",
				ModConfig.keepTableItems, value -> ModConfig.keepTableItems = value));
		this.addRenderableWidget(CycleButton.<Integer>builder(
						value -> value == 0 ? Component.translatable("options.off") : Component.literal(String.valueOf(value)),
						ModConfig.maxEnchantments)
				.withValues(List.of(0, 1, 2, 3, 4, 5, 6, 8, 10))
				.create(right, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("enchantplus.config.max_ench"),
						(button, value) -> ModConfig.maxEnchantments = value));
		y += SPACING;
		this.addRenderableWidget(toggleButton(left, y, "enchantplus.config.relaxed_shelves",
				ModConfig.relaxedShelves, value -> ModConfig.relaxedShelves = value));
		this.addRenderableWidget(toggleButton(right, y, "enchantplus.config.upgrade_diff",
				ModConfig.upgradeDifference, value -> ModConfig.upgradeDifference = value));

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(this.width / 2 - 100, this.height - 26, 200, BUTTON_HEIGHT)
				.build());
	}

	private static CycleButton<Integer> percentButton(int x, int y, String key, List<Integer> values, int current,
			java.util.function.IntConsumer setter) {
		return CycleButton.<Integer>builder(value -> Component.literal(value + "%"), current)
				.withValues(values)
				.create(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable(key),
						(button, value) -> setter.accept(value));
	}

	private static CycleButton<Boolean> toggleButton(int x, int y, String key, boolean current,
			java.util.function.Consumer<Boolean> setter) {
		return CycleButton.onOffBuilder(current)
				.create(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable(key),
						(button, value) -> setter.accept(value));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.translatable("enchantplus.config.note"),
				this.width / 2, this.height - 36, 0xFFA0A0A0);
	}

	@Override
	public void onClose() {
		ModConfig.save();
		this.minecraft.setScreenAndShow(this.parent);
	}
}
