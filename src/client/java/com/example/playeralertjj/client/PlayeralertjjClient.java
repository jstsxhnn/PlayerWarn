package com.example.playeralertjj.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class PlayeralertjjClient implements ClientModInitializer {
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("playeralertjj.json");
    private static final Set<UUID> PLAYERS_IN_RANGE = new HashSet<>();
    private static PlayerAlertConfig config = new PlayerAlertConfig();

    @Override
    public void onInitializeClient() {
        config = PlayerAlertConfig.load(CONFIG_PATH);
        config.save(CONFIG_PATH);
        registerCommands();
        ClientTickEvents.END_CLIENT_TICK.register(PlayeralertjjClient::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            PLAYERS_IN_RANGE.clear();
            return;
        }

        int viewDistanceChunks = client.options.getViewDistance().getValue();
        double maxDistance = viewDistanceChunks * 16.0;
        double maxDistanceSq = maxDistance * maxDistance;

        Set<UUID> nowInRange = new HashSet<>();
        for (PlayerEntity other : client.world.getPlayers()) {
            if (other == client.player) {
                continue;
            }
            String otherName = other.getName().getString();
            if (config.isIgnored(otherName)) {
                continue;
            }
            if (client.player.squaredDistanceTo(other) <= maxDistanceSq) {
                UUID uuid = other.getUuid();
                nowInRange.add(uuid);
                if (!PLAYERS_IN_RANGE.contains(uuid)) {
                    sendWarning(client, otherName);
                }
            }
        }

        PLAYERS_IN_RANGE.clear();
        PLAYERS_IN_RANGE.addAll(nowInRange);
    }

    private static void sendWarning(MinecraftClient client, String otherName) {
        client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
        client.player.sendMessage(
                Text.literal("Warnung: Spieler " + otherName + " in Render-Distance."),
                false
        );
    }

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("playeralert")
                        .then(ClientCommandManager.literal("ignore")
                                .then(ClientCommandManager.argument("player", StringArgumentType.word())
                                        .executes(PlayeralertjjClient::addIgnored))
                                .then(ClientCommandManager.literal("add")
                                        .then(ClientCommandManager.argument("player", StringArgumentType.word())
                                                .executes(PlayeralertjjClient::addIgnored)))
                                .then(ClientCommandManager.literal("remove")
                                        .then(ClientCommandManager.argument("player", StringArgumentType.word())
                                                .executes(PlayeralertjjClient::removeIgnored)))
                                .then(ClientCommandManager.literal("list")
                                        .executes(PlayeralertjjClient::listIgnored))
                                .then(ClientCommandManager.literal("clear")
                                        .executes(PlayeralertjjClient::clearIgnored)))
                        .then(ClientCommandManager.literal("unignore")
                                .then(ClientCommandManager.argument("player", StringArgumentType.word())
                                        .executes(PlayeralertjjClient::removeIgnored)))
        ));
    }

    private static int addIgnored(CommandContext<FabricClientCommandSource> context) {
        String name = StringArgumentType.getString(context, "player");
        MinecraftClient client = context.getSource().getClient();
        if (name.isBlank()) {
            sendClientMessage(client, "Bitte einen Spielernamen angeben.");
            return 0;
        }

        boolean added = config.addIgnored(name);
        config.save(CONFIG_PATH);
        if (added) {
            sendClientMessage(client, "Ignoriere Spieler: " + name);
        } else {
            sendClientMessage(client, "Spieler ist bereits ignoriert: " + name);
        }
        return 1;
    }

    private static int removeIgnored(CommandContext<FabricClientCommandSource> context) {
        String name = StringArgumentType.getString(context, "player");
        MinecraftClient client = context.getSource().getClient();
        if (name.isBlank()) {
            sendClientMessage(client, "Bitte einen Spielernamen angeben.");
            return 0;
        }

        boolean removed = config.removeIgnored(name);
        config.save(CONFIG_PATH);
        if (removed) {
            sendClientMessage(client, "Nicht mehr ignoriert: " + name);
        } else {
            sendClientMessage(client, "Spieler war nicht in der Ignorierliste: " + name);
        }
        return 1;
    }

    private static int listIgnored(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();
        List<String> ignored = new ArrayList<>(config.getIgnoredPlayers());
        if (ignored.isEmpty()) {
            sendClientMessage(client, "Ignorierliste ist leer.");
            return 1;
        }

        Collections.sort(ignored);
        sendClientMessage(client, "Ignoriert: " + String.join(", ", ignored));
        return 1;
    }

    private static int clearIgnored(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();
        config.clearIgnored();
        config.save(CONFIG_PATH);
        sendClientMessage(client, "Ignorierliste geleert.");
        return 1;
    }

    private static void sendClientMessage(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), false);
            return;
        }
        client.inGameHud.getChatHud().addMessage(Text.literal(message));
    }
}
