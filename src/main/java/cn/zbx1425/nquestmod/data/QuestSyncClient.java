package cn.zbx1425.nquestmod.data;

import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.quest.Quest;
import cn.zbx1425.nquestmod.data.quest.QuestCategory;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class QuestSyncClient {

    private final SyncConfig config;
    private final QuestPersistence questStorage;
    private final MinecraftServer server;
    private final HttpClient httpClient;
    private final ExecutorService syncExecutor;

    private long localLastModified = 0;
    private long lastPollTick = 0;
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    public QuestSyncClient(SyncConfig config, QuestPersistence questStorage, MinecraftServer server) {
        this.config = config;
        this.questStorage = questStorage;
        this.server = server;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.syncExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "NQuest-Sync");
            t.setDaemon(true);
            return t;
        });
        loadSyncState();
    }

    public boolean isEnabled() {
        return config.isValid();
    }

    public void tick(long tickCount) {
        if (!isEnabled()) return;
        long pollIntervalTicks = config.pollIntervalSeconds * 20L;
        if (lastPollTick != 0 && tickCount - lastPollTick < pollIntervalTicks) return;
        lastPollTick = tickCount;
        pollAsync();
    }

    public void triggerImmediateSync() {
        if (!isEnabled()) return;
        lastPollTick = 0;
    }

    public void shutdown() {
        syncExecutor.shutdown();
    }

    private void pollAsync() {
        if (!syncInProgress.compareAndSet(false, true)) return;

        CompletableFuture.runAsync(() -> {
            try {
                long remoteLastModified = fetchSyncStatus();
                if (remoteLastModified <= localLastModified) {
                    return;
                }

                NQuestMod.LOGGER.info("Remote data updated (remote={}, local={}), fetching bundle...",
                        remoteLastModified, localLastModified);

                SyncBundle bundle = fetchSyncBundle();
                if (bundle == null) return;

                writeBundleToCache(bundle);

                server.execute(() -> {
                    try {
                        applyBundle(bundle);
                        NQuestMod.LOGGER.info("Sync complete: {} quests, {} categories (lastModified={})",
                                bundle.quests.size(), bundle.categories.size(), bundle.lastModified);
                    } catch (Exception e) {
                        NQuestMod.LOGGER.error("Failed to apply sync bundle on main thread", e);
                    }
                });
            } catch (Exception e) {
                NQuestMod.LOGGER.warn("Sync poll failed, using local cache", e);
            } finally {
                syncInProgress.set(false);
            }
        }, syncExecutor);
    }

    private void applyBundle(SyncBundle bundle) {
        NQuestMod.INSTANCE.questDispatcher.reloadQuests(bundle.quests);
        NQuestMod.INSTANCE.questCategories.clear();
        NQuestMod.INSTANCE.questCategories.putAll(bundle.categories);
        localLastModified = bundle.lastModified;
        saveSyncState();
    }

    private long fetchSyncStatus() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.backendUrl + "/sync/status"))
                .header("X-API-Key", config.apiKey)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Sync status returned HTTP " + response.statusCode());
        }

        JsonObject json = NQuestGson.INSTANCE.fromJson(response.body(), JsonObject.class);
        return json.get("lastModified").getAsLong();
    }

    private SyncBundle fetchSyncBundle() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.backendUrl + "/sync/bundle"))
                .header("X-API-Key", config.apiKey)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Sync bundle returned HTTP " + response.statusCode());
        }

        JsonObject json = NQuestGson.INSTANCE.fromJson(response.body(), JsonObject.class);

        SyncBundle bundle = new SyncBundle();
        bundle.lastModified = json.get("lastModified").getAsLong();

        Type questMapType = new TypeToken<Map<String, Quest>>() {}.getType();
        bundle.quests = NQuestGson.INSTANCE.fromJson(json.get("quests"), questMapType);
        if (bundle.quests == null) bundle.quests = new HashMap<>();

        Type categoryMapType = new TypeToken<Map<String, QuestCategory>>() {}.getType();
        bundle.categories = NQuestGson.INSTANCE.fromJson(json.get("categories"), categoryMapType);
        if (bundle.categories == null) bundle.categories = new HashMap<>();

        return bundle;
    }

    private void writeBundleToCache(SyncBundle bundle) throws IOException {
        Map<String, Quest> oldQuests = NQuestMod.INSTANCE.questDispatcher.quests;
        int written = 0;
        for (Map.Entry<String, Quest> entry : bundle.quests.entrySet()) {
            Quest oldQuest = oldQuests.get(entry.getKey());
            if (oldQuest != null) {
                String oldJson = NQuestGson.INSTANCE.toJson(oldQuest);
                String newJson = NQuestGson.INSTANCE.toJson(entry.getValue());
                if (oldJson.equals(newJson)) continue;
            }
            questStorage.saveQuestDefinition(entry.getValue());
            written++;
        }
        if (written > 0 || oldQuests.size() != bundle.quests.size()) {
            NQuestMod.LOGGER.info("Sync cache: wrote {} changed quests, {} total",
                    written, bundle.quests.size());
        }
        questStorage.removeStaleQuests(bundle.quests.keySet());
        questStorage.saveQuestCategories(bundle.categories);
    }

    private void loadSyncState() {
        Path stateFile = questStorage.basePath.resolve("sync_state.json");
        if (Files.exists(stateFile)) {
            try (Reader reader = Files.newBufferedReader(stateFile)) {
                JsonObject json = NQuestGson.INSTANCE.fromJson(reader, JsonObject.class);
                if (json != null && json.has("lastModified")) {
                    localLastModified = json.get("lastModified").getAsLong();
                }
            } catch (Exception e) {
                NQuestMod.LOGGER.warn("Failed to load sync state", e);
            }
        }
    }

    private void saveSyncState() {
        Path stateFile = questStorage.basePath.resolve("sync_state.json");
        try (Writer writer = Files.newBufferedWriter(stateFile)) {
            JsonObject json = new JsonObject();
            json.addProperty("lastModified", localLastModified);
            NQuestGson.PRETTY.toJson(json, writer);
        } catch (IOException e) {
            NQuestMod.LOGGER.warn("Failed to save sync state", e);
        }
    }

    private static class SyncBundle {
        long lastModified;
        Map<String, Quest> quests;
        Map<String, QuestCategory> categories;
    }
}
