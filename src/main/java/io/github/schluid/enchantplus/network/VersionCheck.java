package io.github.schluid.enchantplus.network;

import io.github.schluid.enchantplus.EnchantPlus;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** The server sends its version on join; the client warns in chat if it differs and replies (see "/enchantplus versions"). */
public final class VersionCheck {
	private VersionCheck() {
	}

	/** Both ways: the EnchantPlus version of the sender. */
	public record VersionPayload(String version) implements CustomPacketPayload {
		public static final Type<VersionPayload> TYPE = new Type<>(EnchantPlus.id("version"));
		public static final StreamCodec<RegistryFriendlyByteBuf, VersionPayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.STRING_UTF8, VersionPayload::version, VersionPayload::new);

		@Override
		public Type<VersionPayload> type() {
			return TYPE;
		}
	}

	/** Version of each player online that has EnchantPlus. */
	private static final Map<UUID, String> VERSIONS = new ConcurrentHashMap<>();

	public static String version() {
		return FabricLoader.getInstance().getModContainer(EnchantPlus.MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("?");
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(VersionPayload.TYPE, VersionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(VersionPayload.TYPE, VersionPayload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (ServerPlayNetworking.canSend(player, VersionPayload.TYPE)) {
				ServerPlayNetworking.send(player, new VersionPayload(version()));
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> VERSIONS.remove(handler.getPlayer().getUUID()));
		ServerPlayNetworking.registerGlobalReceiver(VersionPayload.TYPE, (payload, context) -> {
			String theirs = payload.version().length() > 64 ? payload.version().substring(0, 64) : payload.version();
			VERSIONS.put(context.player().getUUID(), theirs);
			if (!theirs.equals(version())) {
				EnchantPlus.LOGGER.warn("{} uses EnchantPlus {}, the server {}", context.player().getName().getString(),
						theirs, version());
			}
		});
	}

	/** Version of the player, null if the player has no EnchantPlus (or did not answer yet). */
	public static String of(ServerPlayer player) {
		return VERSIONS.get(player.getUUID());
	}
}
