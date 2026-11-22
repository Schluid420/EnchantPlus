package io.github.schluid.enchantplus.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Frame of the EnchantPlus windows: double gold line, gold corner fittings (like the book textures), title bar with two diamonds. */
final class PanelFrame {
	private PanelFrame() {
	}

	/** Content starts this far from the left, right and bottom edge. */
	static final int IN = 6;
	/** Content starts this far below the top edge (below the title bar). */
	static final int TOP = 20;
	/** Title text position from the top left corner. */
	static final int TITLE_X = 8;
	static final int TITLE_Y = 8;

	// Colours of the mod icon, with less saturation
	private static final int OUTER = 0xFF0E0B19;
	private static final int BODY = 0xFF1C162B;
	private static final int BAR = 0xFF362C56;
	private static final int GOLD = 0xFFBE9A4F;
	private static final int GOLD_DARK = 0xFF79612C;
	private static final int CREAM = 0xFFF4F0E2;

	/** Full window with title bar. */
	static void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		int right = x + width;
		int bottom = y + height;
		graphics.fill(x, y, right, bottom, OUTER);
		graphics.fill(x + 1, y + 1, right - 1, bottom - 1, BODY);
		// Double line: gold outside, dark gold inside
		graphics.outline(x + 2, y + 2, width - 4, height - 4, GOLD);
		graphics.outline(x + 4, y + 4, width - 8, height - 8, GOLD_DARK);
		// Title bar with a gold line under it
		graphics.fill(x + 5, y + 5, right - 5, y + TOP - 2, BAR);
		graphics.fill(x + 5, y + TOP - 2, right - 5, y + TOP - 1, GOLD);
		diamond(graphics, right - 17, y + 11);
		diamond(graphics, right - 11, y + 11);
		corners(graphics, x, y, right, bottom);
	}

	/** Thin version for small spaces (the list in the enchanting table): 2 pixels wide, with the corner fittings. */
	static void drawSlim(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		int right = x + width;
		int bottom = y + height;
		graphics.fill(x, y, right, bottom, OUTER);
		graphics.outline(x + 1, y + 1, width - 2, height - 2, GOLD_DARK);
		graphics.fill(x + 2, y + 2, right - 2, bottom - 2, BODY);
		corners(graphics, x, y, right, bottom);
	}

	/** L-shaped gold fittings in the four corners, 7 pixels long, with a stud. */
	private static void corners(GuiGraphicsExtractor graphics, int x, int y, int right, int bottom) {
		int len = 7;
		// Top left
		graphics.fill(x, y, x + len, y + 1, GOLD);
		graphics.fill(x, y, x + 1, y + len, GOLD);
		graphics.fill(x + 1, y + 1, x + len, y + 2, GOLD_DARK);
		graphics.fill(x + 1, y + 1, x + 2, y + len, GOLD_DARK);
		// Top right
		graphics.fill(right - len, y, right, y + 1, GOLD);
		graphics.fill(right - 1, y, right, y + len, GOLD);
		graphics.fill(right - len, y + 1, right - 1, y + 2, GOLD_DARK);
		graphics.fill(right - 2, y + 1, right - 1, y + len, GOLD_DARK);
		// Bottom left
		graphics.fill(x, bottom - 1, x + len, bottom, GOLD);
		graphics.fill(x, bottom - len, x + 1, bottom, GOLD);
		graphics.fill(x + 1, bottom - 2, x + len, bottom - 1, GOLD_DARK);
		graphics.fill(x + 1, bottom - len, x + 2, bottom - 1, GOLD_DARK);
		// Bottom right
		graphics.fill(right - len, bottom - 1, right, bottom, GOLD);
		graphics.fill(right - 1, bottom - len, right, bottom, GOLD);
		graphics.fill(right - len, bottom - 2, right - 1, bottom - 1, GOLD_DARK);
		graphics.fill(right - 2, bottom - len, right - 1, bottom - 1, GOLD_DARK);
		// Studs
		graphics.fill(x + 3, y + 3, x + 4, y + 4, CREAM);
		graphics.fill(right - 4, y + 3, right - 3, y + 4, CREAM);
		graphics.fill(x + 3, bottom - 4, x + 4, bottom - 3, CREAM);
		graphics.fill(right - 4, bottom - 4, right - 3, bottom - 3, CREAM);
	}

	/** Tooltip of the window, drawn by us after the window (the vanilla tooltip would end up behind it). */
	private static List<Component> tip = null;

	static void tip(List<Component> lines) {
		tip = lines;
	}

	static void clearTip() {
		tip = null;
	}

	/** Draws the tooltip set during this frame next to the mouse, inside the screen. */
	static void drawTip(GuiGraphicsExtractor graphics, Font font, Screen screen, int mouseX, int mouseY) {
		if (tip == null || tip.isEmpty()) {
			return;
		}
		List<FormattedCharSequence> lines = new ArrayList<>();
		for (Component line : tip) {
			lines.addAll(font.split(line, 200));
		}
		tip = null;
		int width = 0;
		for (FormattedCharSequence line : lines) {
			width = Math.max(width, font.width(line));
		}
		int w = width + 10;
		int h = lines.size() * 10 + 8;
		int x = mouseX + 12;
		int y = mouseY - 12;
		if (x + w > screen.width - 2) {
			x = mouseX - 12 - w;
		}
		x = Math.max(2, x);
		y = Math.max(2, Math.min(y, screen.height - h - 2));
		graphics.fill(x, y, x + w, y + h, OUTER);
		graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, BODY);
		graphics.outline(x + 1, y + 1, w - 2, h - 2, GOLD_DARK);
		int ty = y + 5;
		for (FormattedCharSequence line : lines) {
			graphics.text(font, line, x + 5, ty, 0xFFFFFFFF, true);
			ty += 10;
		}
	}

	/** Small diamond (5 x 5) around the given centre, cream in the middle. */
	private static void diamond(GuiGraphicsExtractor graphics, int cx, int cy) {
		graphics.fill(cx, cy - 2, cx + 1, cy - 1, GOLD);
		graphics.fill(cx - 1, cy - 1, cx + 2, cy, GOLD);
		graphics.fill(cx - 2, cy, cx + 3, cy + 1, GOLD);
		graphics.fill(cx - 1, cy + 1, cx + 2, cy + 2, GOLD);
		graphics.fill(cx, cy + 2, cx + 1, cy + 3, GOLD);
		graphics.fill(cx, cy, cx + 1, cy + 1, CREAM);
	}
}
