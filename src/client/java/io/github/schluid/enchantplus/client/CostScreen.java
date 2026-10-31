package io.github.schluid.enchantplus.client;

import io.github.schluid.enchantplus.config.ModConfig;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Sub screen of the settings: what an enchantment costs in experience levels and lapis lazuli.
 * There are three price tiers. The tier of an enchantment depends on its required level:
 * tier 1 up to the first limit, tier 2 up to the second limit, tier 3 above.
 * All values are typed into number fields.
 */
public class CostScreen extends Screen {
	private static final int FIELD_WIDTH = 55;
	private static final int FIELD_HEIGHT = 20;
	private static final int COLUMN_SPACING = 65;
	private static final int ROW_SPACING = 28;
	private static final int LABEL_WIDTH = 60;

	private static final int LEVEL_MIN = 1;
	private static final int LEVEL_MAX = 100;
	private static final int LAPIS_MIN = 0;
	private static final int LAPIS_MAX = 64;
	private static final int GRIND_MIN = 1;
	private static final int GRIND_MAX = 100;

	private final Screen parent;

	private int startX;
	private int firstRowY;

	public CostScreen(Screen parent) {
		super(Component.translatable("enchantplus.config.costs_title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.startX = this.width / 2 - (LABEL_WIDTH + 3 * COLUMN_SPACING) / 2;
		this.firstRowY = this.height / 6 + 20;

		int limitX = this.startX + LABEL_WIDTH;
		int levelX = limitX + COLUMN_SPACING;
		int lapisX = levelX + COLUMN_SPACING;

		for (int tier = 0; tier < 3; tier++) {
			int index = tier;
			int y = this.firstRowY + tier * ROW_SPACING;
			// Tier 3 has no limit, it is everything above the limit of tier 2
			if (tier < 2) {
				this.addRenderableWidget(numberField(limitX, y, ModConfig.tierLimit[index], LEVEL_MIN, LEVEL_MAX,
						value -> ModConfig.tierLimit[index] = value));
			}
			this.addRenderableWidget(numberField(levelX, y, ModConfig.levelCost[index], LEVEL_MIN, LEVEL_MAX,
					value -> ModConfig.levelCost[index] = value));
			this.addRenderableWidget(numberField(lapisX, y, ModConfig.lapisCost[index], LAPIS_MIN, LAPIS_MAX,
					value -> ModConfig.lapisCost[index] = value));
		}

		// Grindstone: lapis for removing one enchantment, same column as the other lapis prices
		this.addRenderableWidget(numberField(lapisX, this.firstRowY + 3 * ROW_SPACING + 8, ModConfig.grindLapisCost,
				GRIND_MIN, GRIND_MAX, value -> ModConfig.grindLapisCost = value));

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(this.width / 2 - 100, this.height - 32, 200, 20)
				.build());
	}

	/** A text box that only accepts digits and keeps the value inside min and max. */
	private EditBox numberField(int x, int y, int current, int min, int max, IntConsumer setter) {
		EditBox box = new EditBox(this.font, x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.empty());
		box.setMaxLength(3);
		box.setValue(String.valueOf(current));
		box.setResponder(text -> {
			String digits = text.replaceAll("[^0-9]", "");
			if (!digits.equals(text)) {
				// Removes everything that is not a digit and runs this check again
				box.setValue(digits);
				return;
			}
			if (digits.isEmpty()) {
				return;
			}
			setter.accept(Math.max(min, Math.min(max, Integer.parseInt(digits))));
		});
		return box;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);

		int limitX = this.startX + LABEL_WIDTH;
		int levelX = limitX + COLUMN_SPACING;
		int lapisX = levelX + COLUMN_SPACING;

		int headerY = this.firstRowY - 14;
		graphics.text(this.font, Component.translatable("enchantplus.config.col_limit"), limitX, headerY, 0xFFA0A0A0, true);
		graphics.text(this.font, Component.translatable("enchantplus.config.col_levels"), levelX, headerY, 0xFFA0A0A0, true);
		graphics.text(this.font, Component.translatable("enchantplus.config.col_lapis"), lapisX, headerY, 0xFFA0A0A0, true);

		for (int tier = 0; tier < 3; tier++) {
			int y = this.firstRowY + tier * ROW_SPACING + 6;
			graphics.text(this.font, Component.translatable("enchantplus.config.tier_row", tier + 1),
					this.startX, y, 0xFFFFFFFF, true);
			if (tier == 2) {
				graphics.text(this.font, Component.translatable("enchantplus.config.above"),
						limitX + 4, y, 0xFFA0A0A0, true);
			}
		}

		graphics.text(this.font, Component.translatable("enchantplus.config.grind_row"),
				this.startX, this.firstRowY + 3 * ROW_SPACING + 14, 0xFFFFFFFF, true);

		graphics.centeredText(this.font, Component.translatable("enchantplus.config.tiers_note"),
				this.width / 2, this.height - 50, 0xFFA0A0A0);
	}

	@Override
	public void onClose() {
		// Tier 2 can never end below tier 1
		if (ModConfig.tierLimit[1] < ModConfig.tierLimit[0]) {
			ModConfig.tierLimit[1] = ModConfig.tierLimit[0];
		}
		ModConfig.save();
		this.minecraft.setScreenAndShow(this.parent);
	}
}
