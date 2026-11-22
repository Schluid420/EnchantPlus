package io.github.schluid.enchantplus.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.schluid.enchantplus.EnchantPlus;
import io.github.schluid.enchantplus.config.ModConfig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/**
 * The server decides how the game plays: on join it sends all settings, the client uses them only in memory.
 * Operators may change them in the menu; the server saves and sends them to everyone. A single player or
 * LAN host uses its own settings. Transfer: gzipped JSON.
 */
public final class SettingsSync {
	private SettingsSync() {
	}

	/** Server to client: the settings of the server, and whether this player may change them. */
	public record SettingsPayload(byte[] data, boolean canEdit) implements CustomPacketPayload {
		public static final Type<SettingsPayload> TYPE = new Type<>(EnchantPlus.id("settings"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SettingsPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.BYTE_ARRAY, SettingsPayload::data, ByteBufCodecs.BOOL, SettingsPayload::canEdit,
				SettingsPayload::new);

		@Override
		public Type<SettingsPayload> type() {
			return TYPE;
		}
	}

	/** Client to server: an operator changed the settings of the server in the menu. */
	public record EditPayload(byte[] data) implements CustomPacketPayload {
		public static final Type<EditPayload> TYPE = new Type<>(EnchantPlus.id("settings_edit"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EditPayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.BYTE_ARRAY, EditPayload::data, EditPayload::new);

		@Override
		public Type<EditPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(SettingsPayload.TYPE, SettingsPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(EditPayload.TYPE, EditPayload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.getPlayer()));
		ServerPlayNetworking.registerGlobalReceiver(EditPayload.TYPE, (payload, context) -> edit(context.player(), payload));
	}

	/** Operators (permission level "gamemaster", like /gamemode) may change the settings of the server. */
	public static boolean canEdit(ServerPlayer player) {
		return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	/** Sends the current settings to one player, if the player has EnchantPlus. */
	public static void send(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, SettingsPayload.TYPE)) {
			ServerPlayNetworking.send(player, new SettingsPayload(pack(ModConfig.settingsToJson()), canEdit(player)));
		}
	}

	/** Sends the current settings to every player, e.g. after an operator changed them or after /reload. */
	public static void sendToAll(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			send(player);
		}
	}

	private static void edit(ServerPlayer player, EditPayload payload) {
		if (!canEdit(player)) {
			EnchantPlus.LOGGER.warn("{} tried to change the EnchantPlus settings without permission",
					player.getName().getString());
			return;
		}
		JsonObject settings = unpack(payload.data());
		if (settings == null) {
			return;
		}
		ModConfig.settingsFromJson(settings);
		ModConfig.save();
		EnchantPlus.LOGGER.info("{} changed the EnchantPlus settings", player.getName().getString());
		sendToAll(player.level().getServer());
		player.sendSystemMessage(Component.translatable("enchantplus.server.saved").withStyle(ChatFormatting.GREEN));
	}

	/** The settings as gzipped JSON. */
	public static byte[] pack(JsonObject settings) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream zip = new GZIPOutputStream(bytes)) {
			zip.write(settings.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException("Could not pack the settings", e);
		}
		return bytes.toByteArray();
	}

	/** Settings are a few hundred bytes; anything bigger is refused. */
	private static final int MAX_UNPACKED_BYTES = 64 * 1024;

	/** Reads gzipped JSON; null if it is broken or too big. */
	public static JsonObject unpack(byte[] data) {
		if (data.length > MAX_UNPACKED_BYTES) {
			EnchantPlus.LOGGER.error("The EnchantPlus settings that were sent are too big");
			return null;
		}
		try (GZIPInputStream zip = new GZIPInputStream(new ByteArrayInputStream(data))) {
			byte[] raw = zip.readNBytes(MAX_UNPACKED_BYTES + 1);
			if (raw.length > MAX_UNPACKED_BYTES) {
				EnchantPlus.LOGGER.error("The EnchantPlus settings that were sent are too big");
				return null;
			}
			return JsonParser.parseString(new String(raw, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException | RuntimeException e) {
			EnchantPlus.LOGGER.error("Could not read the EnchantPlus settings that were sent", e);
			return null;
		}
	}
}
