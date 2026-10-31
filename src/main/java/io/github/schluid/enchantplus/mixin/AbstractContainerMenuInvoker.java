package io.github.schluid.enchantplus.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Gives our code access to the protected addDataSlot method of every menu. */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuInvoker {
	@Invoker("addDataSlot")
	DataSlot enchantplus$addDataSlot(DataSlot slot);
}
