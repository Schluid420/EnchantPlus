package io.github.schluid.enchantplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.schluid.enchantplus.config.ModConfig;
import io.github.schluid.enchantplus.knowledge.GrindOptions;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Panel to the right of the grindstone: lists the enchantments of the item in the input slot.
 * A click removes the chosen one (price in lapis lazuli from the inventory, see Mod Menu).
 */
public final class GrindPanel {
	private GrindPanel() {
	}

	private static final int GUI_WIDTH = 176;
	private static final int GUI_HEIGHT = 166;
	private static final int PANEL_W = 128;
	private static final int ROW_H = 14;
	private static final int MAX_ROWS = 8;

	private static final int COVER = 0xFF1E1E1E;
	private static final int FRAME = 0xFF6E6E6E;
	private static final int ROW_OK = 0xFF7A6A48;
	private static final int ROW_BAD = 0xFF4A4232;
	private static final int ROW_HOVER = 0xFF9A8A62;
	private static final int ROW_BORDER = 0xFF2B2B2B;
	private static final int TEXT_OK = 0xFFFFFFFF;
	private static final int TEXT_BAD = 0xFFB0B0B0;
	private static final int HINT = 0xFFD8D8D8;

	private static boolean pressed = false;
	private static boolean dragging = false;

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof GrindstoneScreen grindScreen)) {
				return;
			}
			pressed = false;
			dragging = false;
			PanelPos.GRIND.load();
			ScreenEvents.afterBackground(screen).register((s, graphics, mouseX, mouseY, delta) ->
					render(client, grindScreen, graphics, mouseX, mouseY));
			ScreenMouseEvents.allowMouseDrag(screen).register((s, event, dragX, dragY) -> {
				if (!pressed) {
					return true;
				}
				dragging = true;
				PanelPos.GRIND.dx += dragX;
				PanelPos.GRIND.dy += dragY;
				clampPos(s);
				return false;
			});
			ScreenMouseEvents.allowMouseRelease(screen).register((s, event) -> {
				if (!pressed) {
					return true;
				}
				pressed = false;
				if (dragging) {
					dragging = false;
					PanelPos.GRIND.save();
				}
				return false;
			});
			ScreenMouseEvents.allowMouseClick(screen).register((s, event) ->
					!click(client, grindScreen, event));
		});
	}

	private static int baseX(Screen screen) {
		return (screen.width - GUI_WIDTH) / 2 + GUI_WIDTH + 4;
	}

	private static int baseY(Screen screen) {
		return (screen.height - GUI_HEIGHT) / 2 + 4;
	}

	/** Keeps the moved panel inside the game window. */
	private static void clampPos(Screen screen) {
		PanelPos pos = PanelPos.GRIND;
		pos.dx = Math.max(-baseX(screen), Math.min(screen.width - PANEL_W - baseX(screen), pos.dx));
		pos.dy = Math.max(-baseY(screen), Math.min(screen.height - (16 + MAX_ROWS * ROW_H + 4) - baseY(screen), pos.dy));
	}

	private static int px(Screen screen) {
		clampPos(screen);
		return baseX(screen) + (int) Math.round(PanelPos.GRIND.dx);
	}

	private static int py(Screen screen) {
		clampPos(screen);
		return baseY(screen) + (int) Math.round(PanelPos.GRIND.dy);
	}

	private static List<Holder<Enchantment>> list(GrindstoneScreen screen) {
		GrindstoneMenu menu = screen.getMenu();
		if (!menu.getSlot(GrindstoneMenu.ADDITIONAL_SLOT).getItem().isEmpty()) {
			return List.of();
		}
		return GrindOptions.forStack(menu.getSlot(GrindstoneMenu.INPUT_SLOT).getItem());
	}

	private static int lapisInInventory(Minecraft client) {
		if (client.player == null) {
			return 0;
		}
		int have = 0;
		var inv = client.player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.is(Items.LAPIS_LAZULI)) {
				have += s.getCount();
			}
		}
		return have;
	}

	private static boolean affordable(Minecraft client) {
		boolean free = client.player != null && client.player.hasInfiniteMaterials();
		return free || ModConfig.grindLapisCost <= 0 || lapisInInventory(client) >= ModConfig.grindLapisCost;
	}

	private static void render(Minecraft client, GrindstoneScreen screen, GuiGraphicsExtractor graphics,
			int mouseX, int mouseY) {
		List<Holder<Enchantment>> list = list(screen);
		if (list.isEmpty()) {
			return;
		}
		Font font = client.font;
		int x = px(screen);
		int y = py(screen);
		int rows = Math.min(list.size(), MAX_ROWS);
		int h = 16 + rows * ROW_H + 4;
		graphics.fill(x, y, x + PANEL_W, y + h, COVER);
		graphics.outline(x, y, PANEL_W, h, FRAME);
		graphics.text(font, Component.translatable("enchantplus.gui.grind_title"), x + 4, y + 4, HINT, false);
		if (mouseX >= x && mouseX < x + PANEL_W && mouseY >= y && mouseY < y + 16 && !dragging) {
			graphics.setTooltipForNextFrame(font, List.of(Component.translatable("enchantplus.gui.move_hint_title"),
					Component.translatable("enchantplus.gui.move_reset")), Optional.empty(), mouseX, mouseY);
		}

		boolean ok = affordable(client);
		for (int i = 0; i < rows; i++) {
			Holder<Enchantment> holder = list.get(i);
			int ry = y + 16 + i * ROW_H;
			boolean hover = mouseX >= x + 2 && mouseX < x + PANEL_W - 2 && mouseY >= ry && mouseY < ry + ROW_H;
			graphics.fill(x + 2, ry, x + PANEL_W - 2, ry + ROW_H - 1, hover ? ROW_HOVER : (ok ? ROW_OK : ROW_BAD));
			graphics.outline(x + 2, ry, PANEL_W - 4, ROW_H - 1, ROW_BORDER);
			int level = net.minecraft.world.item.enchantment.EnchantmentHelper
					.getEnchantmentsForCrafting(screen.getMenu().getSlot(GrindstoneMenu.INPUT_SLOT).getItem())
					.getLevel(holder);
			String name = Enchantment.getFullname(holder, level).getString();
			graphics.text(font, font.plainSubstrByWidth(name, PANEL_W - 12), x + 5, ry + 3, ok ? TEXT_OK : TEXT_BAD, true);

			if (hover) {
				String descKey = "enchantplus.desc." + holder.getRegisteredName().replace(':', '.');
				java.util.ArrayList<Component> tip = new java.util.ArrayList<>();
				tip.add(Enchantment.getFullname(holder, level));
				if (ModConfig.showDescriptions && Language.getInstance().has(descKey)) {
					tip.add(Component.translatable(descKey).withStyle(ChatFormatting.DARK_AQUA));
				}
				tip.add(Component.translatable("enchantplus.gui.grind_cost", ModConfig.grindLapisCost)
						.withStyle(ok ? ChatFormatting.GRAY : ChatFormatting.RED));
				tip.add(Component.translatable("enchantplus.gui.grind_click"));
				graphics.setTooltipForNextFrame(font, tip, Optional.empty(), mouseX, mouseY);
			}
		}
	}

	/** @return true if the click was consumed by our panel */
	private static boolean click(Minecraft client, GrindstoneScreen screen, MouseButtonEvent event) {
		List<Holder<Enchantment>> list = list(screen);
		if (list.isEmpty()) {
			return false;
		}
		int x = px(screen);
		int y = py(screen);
		int rows = Math.min(list.size(), MAX_ROWS);
		double mx = event.x();
		double my = event.y();
		if (mx < x || mx >= x + PANEL_W || my < y || my >= y + 16 + rows * ROW_H + 4) {
			return false;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && my < y + 16) {
			pressed = true;
			dragging = false;
			return true;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && my < y + 16) {
			PanelPos.GRIND.reset();
			return true;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			int index = (int) ((my - y - 16) / ROW_H);
			if (my >= y + 16 && index >= 0 && index < rows && client.gameMode != null) {
				client.gameMode.handleInventoryButtonClick(screen.getMenu().containerId, GrindOptions.BUTTON_BASE + index);
			}
		}
		return true;
	}
}
