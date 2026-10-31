package io.github.schluid.enchantplus.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Remembers how far the player moved one of our panels away from its default spot.
 * Stored in config/enchantplus_panels.properties (survives game restarts).
 */
final class PanelPos {
	static final PanelPos ENCHANT = new PanelPos("enchant");
	static final PanelPos GRIND = new PanelPos("grind");

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("enchantplus_panels.properties");
	}

	private final String key;
	double dx;
	double dy;

	private PanelPos(String key) {
		this.key = key;
	}

	void load() {
		dx = 0;
		dy = 0;
		Path path = file();
		if (!Files.exists(path)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(path)) {
			Properties props = new Properties();
			props.load(reader);
			dx = Double.parseDouble(props.getProperty(key + ".dx", "0"));
			dy = Double.parseDouble(props.getProperty(key + ".dy", "0"));
		} catch (IOException | NumberFormatException e) {
			dx = 0;
			dy = 0;
		}
	}

	void save() {
		Path path = file();
		Properties props = new Properties();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				props.load(reader);
			} catch (IOException e) {
				// start fresh
			}
		}
		props.setProperty(key + ".dx", String.valueOf(Math.round(dx)));
		props.setProperty(key + ".dy", String.valueOf(Math.round(dy)));
		try (Writer writer = Files.newBufferedWriter(path)) {
			props.store(writer, "EnchantPlus panel positions");
		} catch (IOException e) {
			// not critical
		}
	}

	void reset() {
		dx = 0;
		dy = 0;
		save();
	}
}
