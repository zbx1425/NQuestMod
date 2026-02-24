package cn.zbx1425.nquestmod.data.ranking;

import cn.zbx1425.nquestmod.data.NQuestGson;
import cn.zbx1425.nquestmod.data.SyncConfig;
import cn.zbx1425.nquestmod.data.quest.QuestCompletionData;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RankingApiClient {

    private final SyncConfig config;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final Gson gson = NQuestGson.INSTANCE;

    public RankingApiClient(SyncConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "NQuest-Ranking");
            t.setDaemon(true);
            return t;
        });
    }

    public boolean isEnabled() {
        return config.isValid();
    }

    public void shutdown() {
        executor.shutdown();
    }

    // --- Write ---

    public CompletableFuture<CompletionResponse> submitCompletion(QuestCompletionData data) {
        JsonObject body = new JsonObject();
        body.addProperty("playerUuid", data.playerUuid.toString());
        body.addProperty("playerName", data.playerName);
        body.addProperty("questId", data.questId);
        body.addProperty("questName", data.questName);
        body.addProperty("completionTime", data.completionTime);
        body.addProperty("durationMillis", data.durationMillis);
        body.addProperty("questPoints", data.questPoints);
        body.add("stepDurations", gson.toJsonTree(data.stepDurations));

        return postJson("/api/completions", body).thenApply(json -> {
            CompletionResponse resp = new CompletionResponse();
            resp.completionId = json.get("completionId").getAsLong();
            resp.isPersonalBest = json.has("isPersonalBest") && json.get("isPersonalBest").getAsBoolean();
            resp.isWorldRecord = json.has("isWorldRecord") && json.get("isWorldRecord").getAsBoolean();
            resp.rank = json.has("rank") ? json.get("rank").getAsInt() : -1;
            if (json.has("updatedStats")) {
                JsonObject stats = json.getAsJsonObject("updatedStats");
                resp.qpBalance = stats.get("qpBalance").getAsInt();
                resp.totalQuestCompletions = stats.get("totalQuestCompletions").getAsInt();
            }
            return resp;
        });
    }

    // --- Leaderboards ---

    public CompletableFuture<LeaderboardPage<PlayerQPEntry>> getQPLeaderboard(String period, int limit, int offset) {
        String url = "/api/leaderboards/qp?period=" + period + "&limit=" + limit + "&offset=" + offset;
        return getJson(url).thenApply(json -> {
            LeaderboardPage<PlayerQPEntry> page = new LeaderboardPage<>();
            page.total = json.has("total") ? json.get("total").getAsInt() : 0;
            page.entries = new ArrayList<>();
            for (JsonElement el : json.getAsJsonArray("entries")) {
                JsonObject obj = el.getAsJsonObject();
                page.entries.add(new PlayerQPEntry(
                        UUID.fromString(obj.get("playerUuid").getAsString()),
                        obj.get("value").getAsInt()
                ));
            }
            return page;
        });
    }

    public CompletableFuture<LeaderboardPage<PlayerCompletionsEntry>> getCompletionsLeaderboard(String period, int limit, int offset) {
        String url = "/api/leaderboards/completions?period=" + period + "&limit=" + limit + "&offset=" + offset;
        return getJson(url).thenApply(json -> {
            LeaderboardPage<PlayerCompletionsEntry> page = new LeaderboardPage<>();
            page.total = json.has("total") ? json.get("total").getAsInt() : 0;
            page.entries = new ArrayList<>();
            for (JsonElement el : json.getAsJsonArray("entries")) {
                JsonObject obj = el.getAsJsonObject();
                page.entries.add(new PlayerCompletionsEntry(
                        UUID.fromString(obj.get("playerUuid").getAsString()),
                        obj.get("value").getAsInt()
                ));
            }
            return page;
        });
    }

    public CompletableFuture<LeaderboardPage<QuestCompletionData>> getSpeedrunLeaderboard(
            String questId, String period, String mode, int limit, int offset) {
        String url = "/api/leaderboards/speedrun/" + questId
                + "?period=" + period + "&mode=" + mode + "&limit=" + limit + "&offset=" + offset;
        return getJson(url).thenApply(json -> {
            LeaderboardPage<QuestCompletionData> page = new LeaderboardPage<>();
            page.total = json.has("total") ? json.get("total").getAsInt() : 0;
            page.entries = new ArrayList<>();
            for (JsonElement el : json.getAsJsonArray("entries")) {
                page.entries.add(parseCompletionEntry(el.getAsJsonObject()));
            }
            return page;
        });
    }

    // --- Player ---

    public CompletableFuture<PlayerStatsResponse> getPlayerProfile(UUID playerUuid) {
        return getJson("/api/players/" + playerUuid + "/profile").thenApply(json -> {
            PlayerStatsResponse resp = new PlayerStatsResponse();
            resp.playerUuid = playerUuid;
            resp.qpBalance = json.has("qpBalance") ? json.get("qpBalance").getAsInt() : 0;
            resp.totalQuestCompletions = json.has("totalQuestCompletions") ? json.get("totalQuestCompletions").getAsInt() : 0;
            return resp;
        });
    }

    public CompletableFuture<LeaderboardPage<QuestCompletionData>> getPlayerHistory(UUID playerUuid, int limit, int offset) {
        String url = "/api/players/" + playerUuid + "/history?limit=" + limit + "&offset=" + offset;
        return getJson(url).thenApply(json -> {
            LeaderboardPage<QuestCompletionData> page = new LeaderboardPage<>();
            page.total = json.has("total") ? json.get("total").getAsInt() : 0;
            page.entries = new ArrayList<>();
            for (JsonElement el : json.getAsJsonArray("entries")) {
                page.entries.add(parseCompletionEntry(el.getAsJsonObject()));
            }
            return page;
        });
    }

    // --- Helpers ---

    private QuestCompletionData parseCompletionEntry(JsonObject obj) {
        QuestCompletionData data = new QuestCompletionData();
        data.playerUuid = UUID.fromString(obj.get("playerUuid").getAsString());
        data.playerName = obj.has("playerName") ? obj.get("playerName").getAsString() : null;
        data.questId = obj.has("questId") ? obj.get("questId").getAsString() : null;
        data.questName = obj.has("questName") ? obj.get("questName").getAsString() : null;
        data.completionTime = obj.has("completionTime") ? obj.get("completionTime").getAsLong() : 0;
        data.durationMillis = obj.has("durationMillis") ? obj.get("durationMillis").getAsLong() : 0;
        data.questPoints = obj.has("questPoints") ? obj.get("questPoints").getAsInt() : 0;
        if (obj.has("stepDurations") && !obj.get("stepDurations").isJsonNull()) {
            data.stepDurations = gson.fromJson(obj.get("stepDurations"), new TypeToken<Map<Integer, Long>>() {}.getType());
        } else {
            data.stepDurations = new HashMap<>();
        }
        return data;
    }

    private CompletableFuture<JsonObject> getJson(String path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.backendUrl + path))
                        .header("X-API-Key", config.apiKey)
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IOException("HTTP " + response.statusCode() + " from GET " + path);
                }
                return gson.fromJson(response.body(), JsonObject.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    private CompletableFuture<JsonObject> postJson(String path, JsonObject body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.backendUrl + path))
                        .header("X-API-Key", config.apiKey)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200 && response.statusCode() != 201) {
                    throw new IOException("HTTP " + response.statusCode() + " from POST " + path);
                }
                return gson.fromJson(response.body(), JsonObject.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    // --- Response DTOs ---

    public static class CompletionResponse {
        public long completionId;
        public boolean isPersonalBest;
        public boolean isWorldRecord;
        public int rank;
        public int qpBalance;
        public int totalQuestCompletions;
    }

    public static class LeaderboardPage<T> {
        public List<T> entries = new ArrayList<>();
        public int total;
    }

    public static class PlayerStatsResponse {
        public UUID playerUuid;
        public int qpBalance;
        public int totalQuestCompletions;
    }
}
