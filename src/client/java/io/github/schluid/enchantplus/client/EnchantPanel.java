package io.github.schluid.enchantplus.client;

import io.github.schluid.enchantplus.config.ModConfig;
import io.github.schluid.enchantplus.knowledge.EnchantOptions;
import io.github.schluid.enchantplus.knowledge.ShelfCount;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import io.github.schluid.enchantplus.knowledge.EnchantKnowledge;
import java.util.TreeMap;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Replaces the three vanilla buttons INSIDE the enchanting table window with the list
 * of learned enchantments.
 * Left click = enchant, right click = change level, mouse wheel = scroll.
 */
public final class EnchantPanel {
	private EnchantPanel() {
	}

	// Size of the vanilla enchanting table window
	private static final int GUI_WIDTH = 176;
	private static final int GUI_HEIGHT = 166;

	// Area of the three vanilla buttons (relative to the top left corner of the window)
	private static final int REGION_X = 60;
	private static final int REGION_Y = 14;
	private static final int ROW_WIDTH = 108;
	private static final int ROW_HEIGHT = 19;
	private static final int ROWS = 3;

	// Our own dark panel, so it stays readable with every theme (vanilla light, dark UI mods)
	private static final int COVER = 0xFF1E1E1E;
	private static final int FRAME = 0xFF6E6E6E;
	private static final int ROW_OK = 0xFF7A6A48;
	private static final int ROW_BAD = 0xFF4A4232;
	private static final int ROW_HOVER = 0xFF9A8A62;
	private static final int ROW_BORDER = 0xFF2B2B2B;
	private static final int TEXT_OK = 0xFFFFFFFF;
	private static final int TEXT_BAD = 0xFFB0B0B0;
	private static final int COST_OK = 0xFF7CFC7C;
	private static final int COST_BAD = 0xFFFF6B6B;
	private static final int HINT = 0xFFD8D8D8;

	/** Selected level per enchantment (display only, the server validates everything again). */
	private static final Map<String, Integer> SELECTED = new HashMap<>();
	private static int scroll = 0;

	// "Learned" button and popup list (to the right of the window)
	private static final int BTN_W = 56;
	private static final int BTN_H = 16;
	private static final int POP_W = 128;
	private static final int POP_ROW = 12;
	private static final int POP_ROWS = 9;
	private static boolean popupOpen = false;
	private static int popupScroll = 0;

	// Dragging the "Learned" button / popup title with the mouse
	private static boolean pressed = false;
	private static boolean dragging = false;
	private static boolean pressedOnButton = false;
	private static double moved = 0;

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof EnchantmentScreen enchantScreen)) {
				return;
			}
			scroll = 0;
			popupScroll = 0;
			popupOpen = false;
			pressed = false;
			dragging = false;
			PanelPos.ENCHANT.load();
			ScreenEvents.afterBackground(screen).register((s, graphics, mouseX, mouseY, delta) ->
					render(client, enchantScreen, graphics, mouseX, mouseY));
			ScreenMouseEvents.allowMouseClick(screen).register((s, event) ->
					!click(client, enchantScreen, event));
			ScreenMouseEvents.allowMouseDrag(screen).register((s, event, dragX, dragY) ->
					!drag(enchantScreen, dragX, dragY));
			ScreenMouseEvents.allowMouseRelease(screen).register((s, event) ->
					!release(event));
			ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, scrollX, scrollY) ->
					!scroll(client, enchantScreen, mouseX, mouseY, scrollY));
		});
	}

	private static int guiLeft(Screen screen) {
		return (screen.width - GUI_WIDTH) / 2;
	}

	private static int guiTop(Screen screen) {
		return (screen.height - GUI_HEIGHT) / 2;
	}

	private static List<EnchantOptions.Option> options(Minecraft client, EnchantmentMenu menu) {
		if (client.player == null || client.level == null) {
			return List.of();
		}
		ItemStack stack = menu.getSlot(0).getItem();
		return EnchantOptions.forStack(client.player, stack, client.level.registryAccess());
	}

	private static int selectedLevel(EnchantOptions.Option option) {
		int level = SELECTED.getOrDefault(option.id(), option.maxLevel());
		if (level > option.maxLevel() || level <= option.existingLevel()) {
			level = option.maxLevel();
		}
		return level;
	}

	private static boolean inRegion(Screen screen, double x, double y) {
		double lx = x - guiLeft(screen);
		double ly = y - guiTop(screen);
		return lx >= REGION_X - 2 && lx < REGION_X + ROW_WIDTH + 2
				&& ly >= REGION_Y - 2 && ly < REGION_Y + ROWS * ROW_HEIGHT + 2;
	}

	private static void render(Minecraft client, EnchantmentScreen screen, GuiGraphicsExtractor graphics,
			int mouseX, int mouseY) {
		renderList(client, screen, graphics, mouseX, mouseY);
		renderButtonAndPopup(client, screen, graphics, mouseX, mouseY);
	}

	private static void renderList(Minecraft client, EnchantmentScreen screen, GuiGraphicsExtractor graphics,
			int mouseX, int mouseY) {
		EnchantmentMenu menu = screen.getMenu();
		Font font = client.font;
		int left = guiLeft(screen);
		int top = guiTop(screen);

		// Cover the vanilla buttons and draw our own frame
		int x0 = left + REGION_X - 2;
		int y0 = top + REGION_Y - 2;
		int w = ROW_WIDTH + 4;
		int h = ROWS * ROW_HEIGHT + 4;
		graphics.fill(x0, y0, x0 + w, y0 + h, COVER);
		graphics.outline(x0, y0, w, h, FRAME);

		ItemStack item = menu.getSlot(0).getItem();
		List<EnchantOptions.Option> options = options(client, menu);

		if (item.isEmpty()) {
			graphics.text(font, Component.translatable("enchantplus.gui.empty_1"), left + REGION_X + 4, top + REGION_Y + 14, HINT, false);
			graphics.text(font, Component.translatable("enchantplus.gui.empty_2"), left + REGION_X + 4, top + REGION_Y + 26, HINT, false);
			return;
		}
		if (options.isEmpty()) {
			boolean full = ModConfig.maxEnchantments > 0 && !item.is(Items.BOOK)
					&& item.getEnchantments().size() >= ModConfig.maxEnchantments;
			if (full) {
				graphics.text(font, Component.translatable("enchantplus.gui.max_1"), left + REGION_X + 4, top + REGION_Y + 8, HINT, false);
				graphics.text(font, Component.translatable("enchantplus.gui.max_2"), left + REGION_X + 4, top + REGION_Y + 20, HINT, false);
				graphics.text(font, Component.translatable("enchantplus.gui.max_3", ModConfig.maxEnchantments), left + REGION_X + 4, top + REGION_Y + 32, HINT, false);
				return;
			}
			graphics.text(font, Component.translatable("enchantplus.gui.none_1"), left + REGION_X + 4, top + REGION_Y + 8, HINT, false);
			graphics.text(font, Component.translatable("enchantplus.gui.none_2"), left + REGION_X + 4, top + REGION_Y + 20, HINT, false);
			graphics.text(font, Component.translatable("enchantplus.gui.none_3"), left + REGION_X + 4, top + REGION_Y + 32, HINT, false);
			return;
		}

		int maxScroll = Math.max(0, options.size() - ROWS);
		scroll = Math.max(0, Math.min(scroll, maxScroll));

		ItemStack lapisStack = menu.getSlot(1).getItem();
		int lapis = lapisStack.is(Items.LAPIS_LAZULI) ? lapisStack.getCount() : 0;
		boolean free = client.player != null && client.player.hasInfiniteMaterials();
		int playerLevel = client.player == null ? 0 : client.player.experienceLevel;
		int shelves = menu instanceof ShelfCount counter ? counter.enchantplus$getShelves() : 0;

		for (int row = 0; row < ROWS; row++) {
			int index = scroll + row;
			if (index >= options.size()) {
				break;
			}
			EnchantOptions.Option option = options.get(index);
			int level = selectedLevel(option);
			int required = EnchantOptions.requiredLevel(option, level);
			int tier = EnchantOptions.tier(required);
			int neededShelves = EnchantOptions.requiredShelves(required);
			boolean shelvesOk = free || shelves >= neededShelves;
			int levelCost = EnchantOptions.levelCostFor(option, level);
			int lapisCost = EnchantOptions.lapisCostFor(option, level);
			int neededLevel = Math.max(required, levelCost);
			boolean affordable = shelvesOk && (free || (playerLevel >= neededLevel && lapis >= lapisCost));

			int rx = left + REGION_X;
			int ry = top + REGION_Y + row * ROW_HEIGHT;
			boolean hovered = mouseX >= rx && mouseX < rx + ROW_WIDTH && mouseY >= ry && mouseY < ry + ROW_HEIGHT;

			graphics.fill(rx, ry, rx + ROW_WIDTH, ry + ROW_HEIGHT - 1, hovered ? ROW_HOVER : (affordable ? ROW_OK : ROW_BAD));
			graphics.outline(rx, ry, ROW_WIDTH, ROW_HEIGHT - 1, ROW_BORDER);

			String cost = String.valueOf(levelCost);
			int costWidth = font.width(cost);
			String name = Enchantment.getFullname(option.holder(), level).getString();
			String shown = font.plainSubstrByWidth(name, ROW_WIDTH - costWidth - 14);
			graphics.text(font, shown, rx + 4, ry + 5, affordable ? TEXT_OK : TEXT_BAD, true);
			graphics.text(font, cost, rx + ROW_WIDTH - costWidth - 4, ry + 5, affordable ? COST_OK : COST_BAD, true);

			if (hovered) {
				List<Component> tip = new ArrayList<>();
				tip.add(Enchantment.getFullname(option.holder(), level));
				String descKey = "enchantplus.desc." + option.id().replace(':', '.');
				if (ModConfig.showDescriptions && Language.getInstance().has(descKey)) {
					tip.add(Component.translatable(descKey).withStyle(ChatFormatting.DARK_AQUA));
				}
				tip.add(Component.translatable("enchantplus.gui.requires_level", neededLevel));
				tip.add(Component.translatable("enchantplus.gui.requires_shelves", neededShelves, shelves)
						.withStyle(shelvesOk ? ChatFormatting.GRAY : ChatFormatting.RED));
				tip.add(Component.translatable("enchantplus.gui.cost", levelCost, lapisCost));
				tip.add(Component.translatable("enchantplus.gui.left_click"));
				if (option.maxLevel() - option.existingLevel() > 1) {
					tip.add(Component.translatable("enchantplus.gui.right_click"));
				}
				graphics.setTooltipForNextFrame(font, tip, Optional.empty(), mouseX, mouseY);
			}
		}

		// Small scrollbar if there are more entries than rows
		if (maxScroll > 0) {
			int trackX = left + REGION_X + ROW_WIDTH;
			int trackH = ROWS * ROW_HEIGHT;
			int barH = Math.max(6, trackH * ROWS / options.size());
			int barY = top + REGION_Y + (trackH - barH) * scroll / maxScroll;
			graphics.fill(trackX, barY, trackX + 2, barY + barH, 0xFF6A5ACD);
		}
	}


	// ---- "Learned" button + popup ----

	private static int baseX(Screen screen) {
		return guiLeft(screen) + GUI_WIDTH + 4;
	}

	private static int baseY(Screen screen) {
		return guiTop(screen) + 4;
	}

	/** Keeps the moved button + popup inside the game window. */
	private static void clampPos(Screen screen) {
		PanelPos pos = PanelPos.ENCHANT;
		pos.dx = Math.max(-baseX(screen), Math.min(screen.width - POP_W - baseX(screen), pos.dx));
		pos.dy = Math.max(-baseY(screen), Math.min(screen.height - (BTN_H + 2 + popH()) - baseY(screen), pos.dy));
	}

	private static int btnX(Screen screen) {
		clampPos(screen);
		return baseX(screen) + (int) Math.round(PanelPos.ENCHANT.dx);
	}

	private static int btnY(Screen screen) {
		clampPos(screen);
		return baseY(screen) + (int) Math.round(PanelPos.ENCHANT.dy);
	}

	private static int popY(Screen screen) {
		return btnY(screen) + BTN_H + 2;
	}

	private static int popH() {
		return 14 + POP_ROWS * POP_ROW + 4;
	}

	private static boolean inButton(Screen screen, double x, double y) {
		return x >= btnX(screen) && x < btnX(screen) + BTN_W && y >= btnY(screen) && y < btnY(screen) + BTN_H;
	}

	private static boolean inPopup(Screen screen, double x, double y) {
		return popupOpen && x >= btnX(screen) && x < btnX(screen) + POP_W && y >= popY(screen) && y < popY(screen) + popH();
	}

	/** Learned enchantments, sorted by id (id -> level). */
	private static TreeMap<String, Integer> learned(Minecraft client) {
		if (client.player == null) {
			return new TreeMap<>();
		}
		return new TreeMap<>(EnchantKnowledge.all(client.player));
	}

	private static void renderButtonAndPopup(Minecraft client, EnchantmentScreen screen, GuiGraphicsExtractor graphics,
			int mouseX, int mouseY) {
		Font font = client.font;
		int bx = btnX(screen);
		int by = btnY(screen);
		boolean hover = inButton(screen, mouseX, mouseY);
		graphics.fill(bx, by, bx + BTN_W, by + BTN_H, hover || popupOpen ? ROW_HOVER : ROW_OK);
		graphics.outline(bx, by, BTN_W, BTN_H, ROW_BORDER);
		Component label = Component.translatable("enchantplus.gui.learned_button");
		graphics.text(font, label, bx + (BTN_W - font.width(label)) / 2, by + 4, TEXT_OK, true);
		if (hover && !dragging) {
			graphics.setTooltipForNextFrame(font, List.of(Component.translatable("enchantplus.gui.move_hint"),
					Component.translatable("enchantplus.gui.move_reset")), Optional.empty(), mouseX, mouseY);
		}

		if (!popupOpen) {
			return;
		}
		int px = bx;
		int py = popY(screen);
		int ph = popH();
		graphics.fill(px, py, px + POP_W, py + ph, COVER);
		graphics.outline(px, py, POP_W, ph, FRAME);
		graphics.text(font, Component.translatable("enchantplus.gui.learned_title"), px + 4, py + 4, HINT, false);

		TreeMap<String, Integer> all = learned(client);
		if (all.isEmpty()) {
			graphics.text(font, Component.translatable("enchantplus.gui.learned_none"), px + 4, py + 20, HINT, false);
			return;
		}
		int maxScroll = Math.max(0, all.size() - POP_ROWS);
		popupScroll = Math.max(0, Math.min(popupScroll, maxScroll));
		ItemStack item = screen.getMenu().getSlot(0).getItem();
		Registry<Enchantment> registry = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

		int index = 0;
		int row = 0;
		for (var entry : all.entrySet()) {
			if (index++ < popupScroll) {
				continue;
			}
			if (row >= POP_ROWS) {
				break;
			}
			Identifier identifier = Identifier.tryParse(entry.getKey());
			var found = identifier == null ? java.util.Optional.<Holder.Reference<Enchantment>>empty() : registry.get(identifier);
			int ry = py + 14 + row * POP_ROW;
			row++;
			if (found.isEmpty()) {
				continue;
			}
			Holder<Enchantment> holder = found.get();
			int level = Math.min(entry.getValue(), holder.value().getMaxLevel());
			boolean fits = item.isEmpty() || holder.value().isPrimaryItem(item) || item.is(Items.BOOK);
			boolean rowHover = mouseX >= px + 2 && mouseX < px + POP_W - 2 && mouseY >= ry && mouseY < ry + POP_ROW;
			if (rowHover) {
				graphics.fill(px + 2, ry, px + POP_W - 2, ry + POP_ROW, 0xFF3A3A3A);
			}
			String name = Enchantment.getFullname(holder, level).getString();
			graphics.text(font, font.plainSubstrByWidth(name, POP_W - 12), px + 5, ry + 2, fits ? TEXT_OK : TEXT_BAD, false);

			if (rowHover) {
				List<Component> tip = new ArrayList<>();
				tip.add(Enchantment.getFullname(holder, level));
				String descKey = "enchantplus.desc." + entry.getKey().replace(':', '.');
				if (ModConfig.showDescriptions && Language.getInstance().has(descKey)) {
					tip.add(Component.translatable(descKey).withStyle(ChatFormatting.DARK_AQUA));
				}
				tip.add(FitsHint.of(holder.value()).copy().withStyle(ChatFormatting.GRAY));
				if (!fits) {
					tip.add(Component.translatable("enchantplus.gui.not_fitting").withStyle(ChatFormatting.RED));
				}
				graphics.setTooltipForNextFrame(font, tip, Optional.empty(), mouseX, mouseY);
			}
		}

		if (maxScroll > 0) {
			int trackH = POP_ROWS * POP_ROW;
			int barH = Math.max(6, trackH * POP_ROWS / all.size());
			int barY = py + 14 + (trackH - barH) * popupScroll / maxScroll;
			graphics.fill(px + POP_W - 4, barY, px + POP_W - 2, barY + barH, 0xFF6A5ACD);
		}
	}

	/** @return true if the click was consumed by our area */
	private static boolean click(Minecraft client, EnchantmentScreen screen, MouseButtonEvent event) {
		if (inButton(screen, event.x(), event.y())) {
			if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
				pressed = true;
				dragging = false;
				pressedOnButton = true;
				moved = 0;
			} else if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
				PanelPos.ENCHANT.reset();
			}
			return true;
		}
		if (inPopup(screen, event.x(), event.y())) {
			if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && event.y() < popY(screen) + 14) {
				pressed = true;
				dragging = false;
				pressedOnButton = false;
				moved = 0;
			}
			return true;
		}
		if (!inRegion(screen, event.x(), event.y())) {
			return false;
		}

		int ly = (int) (event.y() - guiTop(screen)) - REGION_Y;
		int lx = (int) (event.x() - guiLeft(screen)) - REGION_X;
		if (ly < 0 || ly >= ROWS * ROW_HEIGHT || lx < 0 || lx >= ROW_WIDTH) {
			return true;
		}

		EnchantmentMenu menu = screen.getMenu();
		List<EnchantOptions.Option> options = options(client, menu);
		int index = scroll + ly / ROW_HEIGHT;
		if (index >= options.size()) {
			return true;
		}
		EnchantOptions.Option option = options.get(index);
		int level = selectedLevel(option);

		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			if (client.gameMode != null) {
				client.gameMode.handleInventoryButtonClick(menu.containerId, EnchantOptions.buttonId(index, level));
			}
		} else if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			int next = level - 1;
			if (next <= option.existingLevel()) {
				next = option.maxLevel();
			}
			SELECTED.put(option.id(), next);
		}
		return true;
	}

	/** @return true if the drag belongs to us (vanilla must ignore it) */
	private static boolean drag(Screen screen, double dragX, double dragY) {
		if (!pressed) {
			return false;
		}
		moved += Math.abs(dragX) + Math.abs(dragY);
		if (moved > 3) {
			dragging = true;
		}
		if (dragging) {
			PanelPos.ENCHANT.dx += dragX;
			PanelPos.ENCHANT.dy += dragY;
			clampPos(screen);
		}
		return true;
	}

	/** @return true if the release belongs to us */
	private static boolean release(MouseButtonEvent event) {
		if (!pressed || event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return false;
		}
		pressed = false;
		if (dragging) {
			dragging = false;
			PanelPos.ENCHANT.save();
		} else if (pressedOnButton) {
			popupOpen = !popupOpen;
		}
		return true;
	}

	private static boolean scroll(Minecraft client, EnchantmentScreen screen, double mouseX, double mouseY,
			double scrollY) {
		if (inPopup(screen, mouseX, mouseY)) {
			int maxPopup = Math.max(0, learned(client).size() - POP_ROWS);
			if (scrollY > 0) {
				popupScroll = Math.max(0, popupScroll - 1);
			} else if (scrollY < 0) {
				popupScroll = Math.min(maxPopup, popupScroll + 1);
			}
			return true;
		}
		if (!inRegion(screen, mouseX, mouseY)) {
			return false;
		}
		int maxScroll = Math.max(0, options(client, screen.getMenu()).size() - ROWS);
		if (scrollY > 0) {
			scroll = Math.max(0, scroll - 1);
		} else if (scrollY < 0) {
			scroll = Math.min(maxScroll, scroll + 1);
		}
		return true;
	}
}
