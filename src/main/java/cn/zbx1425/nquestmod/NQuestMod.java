package cn.zbx1425.nquestmod;

import cn.zbx1425.nquestmod.data.QuestDispatcher;
import cn.zbx1425.nquestmod.data.QuestPersistence;
import cn.zbx1425.nquestmod.data.QuestSyncClient;
import cn.zbx1425.nquestmod.data.quest.QuestCategory;
import cn.zbx1425.nquestmod.data.quest.PlayerProfile;
import cn.zbx1425.nquestmod.data.ranking.LocalProfileStorage;
import cn.zbx1425.nquestmod.data.ranking.PendingCompletions;
import cn.zbx1425.nquestmod.data.ranking.RankingApiClient;
import cn.zbx1425.nquestmod.interop.GenerationStatus;
import cn.zbx1425.nquestmod.interop.TscStatus;
import com.google.gson.JsonPrimitive;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NQuestMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("NQuestMod");
    public static NQuestMod INSTANCE;

    public static ServerConfig SERVER_CONFIG = new ServerConfig();

    public QuestPersistence questStorage;
    public RankingApiClient rankingApi;
    public LocalProfileStorage profileStorage;
    public PendingCompletions pendingCompletions;
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
                SERVER_CONFIG.load(server.getServerDirectory().toPath().resolve("config").resolve("nquest.json"));

                Path basePath = server.getWorldPath(LevelResource.ROOT).resolve("nquest");
                Files.createDirectories(basePath);

                questStorage = new QuestPersistence(basePath);
                questCategories = questStorage.loadQuestCategories();

                rankingApi = new RankingApiClient(SERVER_CONFIG);
                profileStorage = new LocalProfileStorage(basePath);
                pendingCompletions = new PendingCompletions(basePath);

                questNotifications = new QuestNotifications(server);
                questDispatcher = new QuestDispatcher(questNotifications, rankingApi);
                questDispatcher.quests = questStorage.loadQuestDefinitions();

                commandSigner = new CommandSigner();
                if (SERVER_CONFIG.commandSigningKey.value != null) {
                    commandSigner.signingKey = SERVER_CONFIG.commandSigningKey.value;
                } else {
                    SERVER_CONFIG.commandSigningKey = SERVER_CONFIG.commandSigningKey.withNewValueToPersist(
                        commandSigner.signingKey, new JsonPrimitive(commandSigner.signingKey.toString()));
                }

                questSyncClient = new QuestSyncClient(SERVER_CONFIG, questStorage, server);
                if (questSyncClient.isEnabled()) {
                    LOGGER.info("Quest remote sync enabled: {}", SERVER_CONFIG.webBackendUrl.value);
                }

                if (rankingApi.isEnabled()) {
                    pendingCompletions.replayAll(rankingApi);
                }
            } catch (IOException ex) {
                LOGGER.error("Failed to initialize NQuest", ex);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (questSyncClient != null) {
                questSyncClient.shutdown();
            }
            if (rankingApi != null) {
                rankingApi.shutdown();
            }
            if (questStorage != null && questCategories != null) {
                try {
                    questStorage.saveQuestCategories(questCategories);
                } catch (IOException e) {
                    LOGGER.error("Failed to save quest categories", e);
                }
            }
            try {
                SERVER_CONFIG.save();
            } catch (IOException e) {
                LOGGER.error("Failed to save server config", e);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((packetListener, packetSender, server) -> {
            server.execute(() -> {
                ServerPlayer player = packetListener.getPlayer();
                UUID playerUuid = player.getGameProfile().getId();

                PlayerProfile profile = new PlayerProfile();
                profile.playerUuid = playerUuid;
                profile.activeQuests = profileStorage.load(playerUuid);

                questDispatcher.playerProfiles.put(playerUuid, profile);

                if (rankingApi != null && rankingApi.isEnabled()) {
                    rankingApi.getPlayerProfile(playerUuid).whenComplete((stats, error) -> {
                        if (error != null) {
                            LOGGER.warn("Failed to fetch player stats for {}", player.getGameProfile().getName(), error);
                            return;
                        }
                        profile.qpBalance = stats.qpBalance;
                        profile.totalQuestCompletions = stats.totalQuestCompletions;
                        profile.lastStatsSyncTime = System.currentTimeMillis();
                    });
                }

                questNotifications.onPlayerJoin(questDispatcher, playerUuid);
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
                    profileStorage.save(playerUuid, profile.activeQuests);
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
            if (server.getTickCount() % 20 != 15) return;
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
