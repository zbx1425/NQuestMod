package cn.zbx1425.nquestbot;

import cn.zbx1425.nquestbot.data.QuestDispatcher;
import cn.zbx1425.nquestbot.data.QuestPersistence;
import cn.zbx1425.nquestbot.data.ranking.RankingManager;
import cn.zbx1425.nquestbot.gui.GuiManager;
import cn.zbx1425.nquestbot.interop.TscStatus;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import cn.zbx1425.nquestbot.data.quest.PlayerProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NQuestBot implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("NQuestBot");
    public static NQuestBot INSTANCE;

    public QuestPersistence questStorage;
    public QuestDispatcher questDispatcher;
    public QuestNotifications questNotifications;
    public RankingManager rankingManager;
    public GuiManager guiManager;

    @Override
    public void onInitialize() {
        INSTANCE = this;

        ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
            try {
                Path basePath = server.getWorldPath(LevelResource.ROOT).resolve("quest_bot");
                Files.createDirectories(basePath);
                // Well this isn't clean but we only have one server instance anyway
                questStorage = new QuestPersistence(basePath);
                questNotifications = new QuestNotifications(server);
                rankingManager = new RankingManager(basePath.resolve("ranking.json"));
                questDispatcher = new QuestDispatcher(questNotifications, rankingManager);
                questDispatcher.quests = questStorage.loadQuestDefinitions();
                guiManager = new GuiManager(this);
            } catch (IOException ex) {
                LOGGER.error("Failed to initialize quest storage", ex);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((packetListener, packetSender, server) -> {
            ServerPlayer player = packetListener.getPlayer();
            try {
                questDispatcher.playerProfiles.put(player.getGameProfile().getId(),
                        questStorage.loadPlayerProfile(player.getGameProfile().getId()));
                questNotifications.onPlayerJoin(questDispatcher, player.getGameProfile().getId());
            } catch (IOException ex) {
                LOGGER.error("Failed to load player profile for {}", player.getGameProfile().getName(), ex);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((packetListener, server) -> {
            ServerPlayer player = packetListener.getPlayer();
            PlayerProfile profile = questDispatcher.playerProfiles.remove(player.getGameProfile().getId());
            if (profile != null) {
                try {
                    questStorage.savePlayerProfile(profile);
                } catch (IOException ex) {
                    LOGGER.error("Failed to save player profile for {}", player.getGameProfile().getName(), ex);
                }
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            assert questDispatcher != null;
            if (server.getTickCount() % 20 == 5) {
                TscStatus.requestUpdate(server);
            }
            if (server.getTickCount() % 20 != 15) return; // Once 1 second
            TscStatus.isAnyQuestGoingOn = questDispatcher.updatePlayers(server.getPlayerList()::getPlayer);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, ctx, selection) ->
            Commands.register(dispatcher, net.minecraft.commands.Commands::literal, net.minecraft.commands.Commands::argument)
        );
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation("nquestbot", path);
    }
}
