package io.github.schluid.enchantplus.command;

import io.github.schluid.enchantplus.knowledge.EnchantKnowledge;
import io.github.schluid.enchantplus.network.VersionCheck;
import net.minecraft.ChatFormatting;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /enchantplus [learn <enchantment> <level> | reset | versions], game master only. */
public final class KnowledgeCommand {
	private KnowledgeCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("enchantplus")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ctx -> list(ctx.getSource()))
						.then(Commands.literal("reset")
								.executes(ctx -> reset(ctx.getSource())))
						.then(Commands.literal("versions")
								.executes(ctx -> versions(ctx.getSource())))
						.then(Commands.literal("learn")
								.then(Commands.argument("id", StringArgumentType.word())
										.then(Commands.argument("level", IntegerArgumentType.integer(1, 10))
												.executes(ctx -> learn(
														ctx.getSource(),
														StringArgumentType.getString(ctx, "id"),
														IntegerArgumentType.getInteger(ctx, "level"))))))));
	}

	private static int list(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		Map<String, Integer> all = EnchantKnowledge.all(player);
		if (all.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("enchantplus.command.empty"), false);
			return 0;
		}
		all.forEach((id, level) ->
				source.sendSuccess(() -> Component.translatable("enchantplus.command.entry", id, level), false));
		return all.size();
	}

	private static int learn(CommandSourceStack source, String id, int level) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		String lower = id.toLowerCase(Locale.ROOT);
		String fullId = lower.contains(":") ? lower : "minecraft:" + lower;
		boolean changed = EnchantKnowledge.learn(player, fullId, level);
		source.sendSuccess(() -> changed
				? Component.translatable("enchantplus.command.learned", fullId, level)
				: Component.translatable("enchantplus.command.unchanged"), false);
		return changed ? 1 : 0;
	}

	private static int versions(CommandSourceStack source) {
		source.sendSuccess(() -> Component.translatable("enchantplus.command.versions", VersionCheck.version()), false);
		for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
			String version = VersionCheck.of(player);
			ChatFormatting color = version == null ? ChatFormatting.GRAY
					: version.equals(VersionCheck.version()) ? ChatFormatting.GREEN : ChatFormatting.GOLD;
			source.sendSuccess(() -> Component.literal(player.getName().getString() + ": "
					+ (version == null ? "-" : version)).withStyle(color), false);
		}
		return 1;
	}

	private static int reset(CommandSourceStack source) throws CommandSyntaxException {
		EnchantKnowledge.reset(source.getPlayerOrException());
		source.sendSuccess(() -> Component.translatable("enchantplus.command.reset"), false);
		return 1;
	}
}
