package io.github.schluid.enchantplus.client.compat;

import io.github.schluid.enchantplus.client.EnchantPanel;
import io.github.schluid.enchantplus.client.GrindPanel;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

/** JEI (only loaded if installed): keeps its item lists away from our windows, so they are not covered and clicks reach them. */
public class EnchantPlusJeiPlugin implements IModPlugin {
	@Override
	public Identifier getPluginUid() {
		return Identifier.fromNamespaceAndPath("enchantplus", "jei");
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addGuiContainerHandler(EnchantmentScreen.class, new IGuiContainerHandler<EnchantmentScreen>() {
			@Override
			public List<Rect2i> getGuiExtraAreas(EnchantmentScreen screen) {
				return EnchantPanel.areas(screen);
			}
		});
		registration.addGuiContainerHandler(GrindstoneScreen.class, new IGuiContainerHandler<GrindstoneScreen>() {
			@Override
			public List<Rect2i> getGuiExtraAreas(GrindstoneScreen screen) {
				return GrindPanel.areas(screen);
			}
		});
	}
}
