package io.github.schluid.enchantplus.client;

import com.google.gson.JsonObject;
import io.github.schluid.enchantplus.config.ModConfig;
import io.github.schluid.enchantplus.network.SettingsSync;
import io.github.schluid.enchantplus.network.VersionCheck;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Client side of server settings: a foreign server's settings are used in memory, the own ones return on leaving. */
public final class ServerSettingsClient {
	private ServerSettingsClient() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(SettingsSync.SettingsPayload.TYPE, (payload, context) ->
				use(context.client(), payload));
		ClientPlayNetworking.registerGlobalReceiver(VersionCheck.VersionPayload.TYPE, (payload, context) -> {
			String own = VersionCheck.version();
			if (!own.equals(payload.version()) && context.player() != null) {
				context.player().sendSystemMessage(Component.translatable("enchantplus.version.mismatch", own,
						payload.version()).withStyle(ChatFormatting.GOLD));
			}
			context.responseSender().sendPacket(new VersionCheck.VersionPayload(own));
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ModConfig::useOwnSettings));
	}

	private static void use(Minecraft client, SettingsSync.SettingsPayload payload) {
		// The host of a single player or LAN world keeps its own settings (they are the server's)
		if (client.getSingleplayerServer() != null) {
			return;
		}
		JsonObject settings = SettingsSync.unpack(payload.data());
		if (settings != null) {
			ModConfig.useServerSettings(settings, payload.canEdit());
		}
	}

	/** The settings in the menu are locked: they come from a server and the player is no operator. */
	public static boolean locked() {
		return ModConfig.serverMode() && !ModConfig.serverCanEdit();
	}

	/** An operator changed the settings of the server in the menu: send them, the server saves them. */
	public static void sendToServer() {
		if (ModConfig.serverMode() && ModConfig.serverCanEdit()
				&& ClientPlayNetworking.canSend(SettingsSync.EditPayload.TYPE)) {
			ClientPlayNetworking.send(new SettingsSync.EditPayload(SettingsSync.pack(ModConfig.settingsToJson())));
		}
	}
}
