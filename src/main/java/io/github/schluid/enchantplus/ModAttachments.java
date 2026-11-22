package io.github.schluid.enchantplus;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** Learned enchantments per player (id -> level), synced to the owner. */
public final class ModAttachments {
	private ModAttachments() {
	}

	public static final Codec<Map<String, Integer>> KNOWLEDGE_CODEC =
			Codec.unboundedMap(Codec.STRING, Codec.INT);

	public static final StreamCodec<ByteBuf, Map<String, Integer>> KNOWLEDGE_STREAM_CODEC =
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT);

	public static final AttachmentType<Map<String, Integer>> KNOWLEDGE =
			AttachmentRegistry.<Map<String, Integer>>builder()
					.persistent(KNOWLEDGE_CODEC)
					.copyOnDeath()
					.syncWith(KNOWLEDGE_STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
					.buildAndRegister(EnchantPlus.id("knowledge"));

	/** Items kept in an enchanting table. */
	public static final AttachmentType<List<ItemStack>> TABLE_ITEMS =
			AttachmentRegistry.<List<ItemStack>>builder()
					.persistent(ItemStack.OPTIONAL_CODEC.listOf())
					.buildAndRegister(EnchantPlus.id("table_items"));

	public static void init() {
		EnchantPlus.LOGGER.info("Knowledge attachment registered: {}", KNOWLEDGE);
	}
}
