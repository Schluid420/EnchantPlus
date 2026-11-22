package io.github.schluid.enchantplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.schluid.enchantplus.config.ModConfig;
import io.github.schluid.enchantplus.knowledge.GrindOptions;
import java.util.List;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Holder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/** Grindstone side panel: click an enchantment to remove it for lapis. */
public final class GrindPanel {
	private GrindPanel() {
	}

	private static final int GUI_WIDTH = 176;
	private static final int GUI_HEIGHT = 166;
	private static final int PANEL_W = 136;
	private static final int TOP = PanelFrame.TOP;
	private static final int IN = PanelFrame.IN;
	private static final int ROW_H = 14;
	private static final int MAX_ROWS = 8;

	private static final int ROW_OK = 0xFF483C68;
	private static final int ROW_BAD = 0xFF2B233E;
	private static final int ROW_HOVER = 0xFF6F5F9A;
	private static final int ROW_BORDER = 0xFF0E0B19;
	private static final int TEXT_OK = 0xFFFFFFFF;
	private static final int TEXT_BAD = 0xFF9E96B4;
	private static final int HINT = 0xFFF4F0E2;

	private static boolean pressed = false;
	private static boolean dragging = false;
	private static int scroll = 0;

	private static final int ROW_CONFIRM = 0xFF7A2E2E;
	private static final int ROW_CONFIRM_HOVER = 0xFF9A3C3C;
	private static final long CONFIRM_MS = 6000;
	/** Row (index in the list) waiting for the second click, -1 = none. */
	private static int pending = -1;
	private static String pendingId = "";
	private static long pendingEnd = 0;

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof GrindstoneScreen grindScreen)) {
				return;
			}
			pressed = false;
			dragging = false;
			scroll = 0;
			pending = -1;
			PanelPos.GRIND.load();
			// Last, in its own layer: in front of the slots and items; JEI keeps away (EnchantPlusJeiPlugin)
			ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
				graphics.nextStratum();
				PanelFrame.clearTip();
				render(client, grindScreen, graphics, mouseX, mouseY);
				PanelFrame.drawTip(graphics, client.font, s, mouseX, mouseY);
			});
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
			ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, scrollX, scrollY) ->
					!scroll(grindScreen, mouseX, mouseY, scrollY));
		});
	}

	private static int baseX(Screen screen) {
		return (screen.width - GUI_WIDTH) / 2 + GUI_WIDTH + 4;
	}

	private static int baseY(Screen screen) {
		return (screen.height - GUI_HEIGHT) / 2 + 4;
	}

	private static void clampPos(Screen screen) {
		PanelPos pos = PanelPos.GRIND;
		pos.dx = Math.max(-baseX(screen), Math.min(screen.width - PANEL_W - baseX(screen), pos.dx));
		pos.dy = Math.max(-baseY(screen), Math.min(screen.height - (TOP + MAX_ROWS * ROW_H + IN) - baseY(screen), pos.dy));
	}

	private static int px(Screen screen) {
		clampPos(screen);
		return baseX(screen) + (int) Math.round(PanelPos.GRIND.dx);
	}

	private static int py(Screen screen) {
		clampPos(screen);
		return baseY(screen) + (int) Math.round(PanelPos.GRIND.dy);
	}

	/** The window while it is shown, for JEI. */
	public static List<Rect2i> areas(GrindstoneScreen screen) {
		List<Holder<Enchantment>> list = list(screen);
		if (list.isEmpty()) {
			return List.of();
		}
		int rows = Math.min(list.size(), MAX_ROWS);
		return List.of(new Rect2i(px(screen), py(screen), PANEL_W, TOP + rows * ROW_H + IN));
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

	/** The row that waits for confirmation; forgets it when it timed out or the list changed. */
	private static int pendingRow(List<Holder<Enchantment>> list) {
		if (pending >= 0 && (System.currentTimeMillis() > pendingEnd || pending >= list.size()
				|| !list.get(pending).getRegisteredName().equals(pendingId))) {
			pending = -1;
		}
		return pending;
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
		scroll = Math.max(0, Math.min(scroll, list.size() - MAX_ROWS));
		int rows = Math.min(list.size(), MAX_ROWS);
		int h = TOP + rows * ROW_H + IN;
		PanelFrame.draw(graphics, x, y, PANEL_W, h);
		graphics.text(font, Component.translatable("enchantplus.gui.grind_title"), x + PanelFrame.TITLE_X, y + PanelFrame.TITLE_Y, HINT, false);
		if (mouseX >= x && mouseX < x + PANEL_W && mouseY >= y && mouseY < y + TOP && !dragging) {
			PanelFrame.tip(List.of(Component.translatable("enchantplus.gui.move_hint_title"),
					Component.translatable("enchantplus.gui.move_reset")));
		}

		boolean ok = affordable(client);
		int confirmRow = pendingRow(list);
		for (int i = 0; i < rows; i++) {
			Holder<Enchantment> holder = list.get(scroll + i);
			int ry = y + TOP + i * ROW_H;
			boolean hover = mouseX >= x + IN && mouseX < x + PANEL_W - IN && mouseY >= ry && mouseY < ry + ROW_H;
			boolean confirm = scroll + i == confirmRow;
			int fillColor = confirm ? (hover ? ROW_CONFIRM_HOVER : ROW_CONFIRM)
					: (hover ? ROW_HOVER : (ok ? ROW_OK : ROW_BAD));
			graphics.fill(x + IN, ry, x + PANEL_W - IN, ry + ROW_H - 1, fillColor);
			graphics.outline(x + IN, ry, PANEL_W - 2 * IN, ROW_H - 1, ROW_BORDER);
			int level = net.minecraft.world.item.enchantment.EnchantmentHelper
					.getEnchantmentsForCrafting(screen.getMenu().getSlot(GrindstoneMenu.INPUT_SLOT).getItem())
					.getLevel(holder);
			String name = confirm ? Component.translatable("enchantplus.gui.grind_confirm").getString()
					: Enchantment.getFullname(holder, level).getString();
			graphics.text(font, font.plainSubstrByWidth(name, PANEL_W - 2 * IN - 10), x + IN + 3, ry + 3,
					confirm || ok ? TEXT_OK : TEXT_BAD, true);

			if (hover) {
				String descKey = "enchantplus.desc." + holder.getRegisteredName().replace(':', '.');
				java.util.ArrayList<Component> tip = new java.util.ArrayList<>();
				tip.add(Enchantment.getFullname(holder, level));
				if (ModConfig.showDescriptions && Language.getInstance().has(descKey)) {
					tip.add(Component.translatable(descKey).withStyle(ChatFormatting.DARK_AQUA));
				}
				tip.add(Component.translatable("enchantplus.gui.grind_cost", ModConfig.grindLapisCost)
						.withStyle(ok ? ChatFormatting.GRAY : ChatFormatting.RED));
				tip.add(Component.translatable(confirm ? "enchantplus.gui.grind_confirm_tip" : "enchantplus.gui.grind_click")
						.withStyle(confirm ? ChatFormatting.RED : ChatFormatting.WHITE));
				PanelFrame.tip(tip);
			}
		}

		if (list.size() > MAX_ROWS) {
			int trackH = rows * ROW_H;
			int barH = Math.max(6, trackH * MAX_ROWS / list.size());
			int barY = y + TOP + (trackH - barH) * scroll / (list.size() - MAX_ROWS);
			graphics.fill(x + PANEL_W - IN - 3, barY, x + PANEL_W - IN - 1, barY + barH, 0xFFBE9A4F);
		}
	}

	/** @return true if the wheel was used on our panel */
	private static boolean scroll(GrindstoneScreen screen, double mouseX, double mouseY, double scrollY) {
		List<Holder<Enchantment>> list = list(screen);
		if (list.size() <= MAX_ROWS) {
			return false;
		}
		int x = px(screen);
		int y = py(screen);
		int rows = Math.min(list.size(), MAX_ROWS);
		if (mouseX < x || mouseX >= x + PANEL_W || mouseY < y || mouseY >= y + TOP + rows * ROW_H + IN) {
			return false;
		}
		int max = list.size() - MAX_ROWS;
		if (scrollY > 0) {
			scroll = Math.max(0, scroll - 1);
		} else if (scrollY < 0) {
			scroll = Math.min(max, scroll + 1);
		}
		return true;
	}

	/** @return true if the click was ours */
	private static boolean click(Minecraft client, GrindstoneScreen screen, MouseButtonEvent event) {
		List<Holder<Enchantment>> list = list(screen);
		if (list.isEmpty()) {
			pending = -1;
			return false;
		}
		int x = px(screen);
		int y = py(screen);
		int rows = Math.min(list.size(), MAX_ROWS);
		double mx = event.x();
		double my = event.y();
		if (mx < x || mx >= x + PANEL_W || my < y || my >= y + TOP + rows * ROW_H + IN) {
			// A click anywhere else cancels
			pending = -1;
			return false;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && my < y + TOP) {
			pressed = true;
			dragging = false;
			return true;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && my < y + TOP) {
			PanelPos.GRIND.reset();
			return true;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			int index = (int) ((my - y - TOP) / ROW_H);
			if (my >= y + TOP && index >= 0 && index < rows && client.gameMode != null) {
				int absolute = scroll + index;
				if (pendingRow(list) == absolute) {
					// Second click on the same row: remove it
					pending = -1;
					client.gameMode.handleInventoryButtonClick(screen.getMenu().containerId,
							GrindOptions.BUTTON_BASE + absolute);
				} else {
					pending = absolute;
					pendingId = list.get(absolute).getRegisteredName();
					pendingEnd = System.currentTimeMillis() + CONFIRM_MS;
				}
				return true;
			}
		}
		pending = -1;
		return true;
	}
}
