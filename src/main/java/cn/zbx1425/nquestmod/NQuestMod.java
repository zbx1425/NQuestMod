package cn.zbx1425.nquestmod;

import cn.zbx1425.nquestmod.data.QuestDispatcher;
import cn.zbx1425.nquestmod.data.QuestPersistence;
import cn.zbx1425.nquestmod.data.QuestSyncClient;
import cn.zbx1425.nquestmod.data.SyncConfig;
import cn.zbx1425.nquestmod.data.quest.QuestCategory;
import cn.zbx1425.nquestmod.data.ranking.QuestUserDatabase;
import cn.zbx1425.nquestmod.interop.GenerationStatus;
import cn.zbx1425.nquestmod.interop.TscStatus;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import cn.zbx1425.nquestmod.data.quest.PlayerProfile;
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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NQuestMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("NQuestMod");
    public static NQuestMod INSTANCE;

    public QuestPersistence questStorage;
    public QuestUserDatabase userDatabase;
    public QuestDispatcher questDispatcher;
    public QuestNotifications questNotifications;
    public Map<String, QuestCategory> questCategories = new HashMap<>();

    public CommandSigner commandSigner;
    public QuestSyncClient questSyncClient;

    @Override
    public void onInitialize() {
        INSTANCE = this;

        ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
            try {
                Path basePath = server.getWorldPath(LevelResource.ROOT).resolve("nquest");
                Files.createDirectories(basePath);

                questStorage = new QuestPersistence(basePath);
                questCategories = questStorage.loadQuestCategories();

                userDatabase = new QuestUserDatabase(basePath.resolve("user.db"));
                questNotifications = new QuestNotifications(server);
                questDispatcher = new QuestDispatcher(questNotifications, userDatabase);
                questDispatcher.quests = questStorage.loadQuestDefinitions();

                commandSigner = questStorage.getOrCreateCommandSigner();

                SyncConfig syncConfig = questStorage.loadSyncConfig();
                questSyncClient = new QuestSyncClient(syncConfig, questStorage, server);
                if (questSyncClient.isEnabled()) {
                    LOGGER.info("Quest remote sync enabled: {}", syncConfig.backendUrl);
                }
            } catch (IOException | SQLException ex) {
                LOGGER.error("Failed to initialize NQuest", ex);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (questSyncClient != null) {
                questSyncClient.shutdown();
            }
            if (userDatabase != null) {
                userDatabase.close();
            }
            if (questStorage != null && questCategories != null) {
                try {
                    questStorage.saveQuestCategories(questCategories);
                } catch (IOException e) {
                    LOGGER.error("Failed to save quest categories", e);
                }
            }
        });

        ServerPlayConnectionEvents.JOIN.register((packetListener, packetSender, server) -> {
            server.execute(() -> {
                ServerPlayer player = packetListener.getPlayer();
                try {
                    questDispatcher.playerProfiles.put(player.getGameProfile().getId(),
                        userDatabase.loadPlayerProfile(player.getGameProfile().getId()));
                    questNotifications.onPlayerJoin(questDispatcher, player.getGameProfile().getId());
                } catch (SQLException ex) {
                    LOGGER.error("Failed to load player profile for {}", player.getGameProfile().getName(), ex);
                }
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((packetListener, server) -> {
            server.execute(() -> {
                ServerPlayer player = packetListener.getPlayer();
                UUID playerUuid = player.getGameProfile().getId();
                if (questDispatcher.isDebugMode(playerUuid)) {
                    questDispatcher.toggleDebugMode(playerUuid);
                }
                PlayerProfile profile = questDispatcher.playerProfiles.remove(playerUuid);
                if (profile != null) {
                    try {
                        userDatabase.savePlayerProfile(profile);
                    } catch (SQLException ex) {
                        LOGGER.error("Failed to save player profile for {}", player.getGameProfile().getName(), ex);
                    }
                }
            });
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            assert questDispatcher != null;
            if (server.getTickCount() % 20 == 5) {
                TscStatus.requestUpdate(server);
            }
            if (server.getTickCount() % 20 == 10 && questSyncClient != null) {
                questSyncClient.tick(server.getTickCount());
            }
            if (server.getTickCount() % 20 != 15) return; // Once 1 second
            TscStatus.isAnyQuestGoingOn = questDispatcher.updatePlayers(server.getPlayerList()::getPlayer);
            GenerationStatus.nextGeneration();
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, ctx, selection) ->
            Commands.register(dispatcher, net.minecraft.commands.Commands::literal, net.minecraft.commands.Commands::argument)
        );
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation("nquestmod", path);
    }
}
