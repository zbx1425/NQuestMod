package cn.zbx1425.nquestmod.data.ranking;

import cn.zbx1425.nquestmod.ServerConfig;
import cn.zbx1425.nquestmod.data.NQuestGson;
import cn.zbx1425.nquestmod.data.quest.QuestCompletionData;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RankingApiClient {

    private final ServerConfig config;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final Gson gson = NQuestGson.INSTANCE;

    public RankingApiClient(ServerConfig config) {
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
        return config.webSyncEnabled.value
            && config.webBackendUrl.value != null && !config.webBackendUrl.value.isEmpty();
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
        body.add("stepDetails", gson.toJsonTree(data.stepDetails));

        return postJson("/completions", body).thenApply(json -> {
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
        String url = "/leaderboards/qp?period=" + period + "&limit=" + limit + "&offset=" + offset;
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
        String url = "/leaderboards/completions?period=" + period + "&limit=" + limit + "&offset=" + offset;
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
        String url = "/leaderboards/speedrun/" + questId
                + "?period=" + period + "&mode=" + mode + "&limit=" + limit + "&offset=" + offset;
        return getJson(url).thenApply(json -> {
            LeaderboardPage<QuestCompletionData> page = new LeaderboardPage<>();
            page.total = json.has("total") ? json.get("total").getAsInt() : 0;
            JsonObject questObj = json.getAsJsonObject("quest");
            page.entries = new ArrayList<>();
            for (JsonElement el : json.getAsJsonArray("entries")) {
                page.entries.add(parseCompletionEntry(el.getAsJsonObject(), questObj, null));
            }
            return page;
        });
    }

    // --- Player ---

    public CompletableFuture<PlayerStatsResponse> getPlayerProfile(UUID playerUuid) {
        return getJson("/players/" + playerUuid + "/profile").thenApply(json -> {
            PlayerStatsResponse resp = new PlayerStatsResponse();
            resp.playerUuid = playerUuid;
            resp.qpBalance = json.has("qpBalance") ? json.get("qpBalance").getAsInt() : 0;
            resp.totalQuestCompletions = json.has("totalQuestCompletions") ? json.get("totalQuestCompletions").getAsInt() : 0;
            return resp;
        }).exceptionally(e -> {
            if (isErrorCode(e, "PLAYER_NOT_FOUND")) {
                PlayerStatsResponse empty = new PlayerStatsResponse();
                empty.playerUuid = playerUuid;
                return empty;
            }
            throw rethrow(e);
        });
    }

    public CompletableFuture<LeaderboardPage<QuestCompletionData>> getPlayerHistory(UUID playerUuid, int limit, int offset) {
        String url = "/players/" + playerUuid + "/history?limit=" + limit + "&offset=" + offset;
        return getJson(url).thenApply(json -> {
            LeaderboardPage<QuestCompletionData> page = new LeaderboardPage<>();
            page.total = json.has("total") ? json.get("total").getAsInt() : 0;
            JsonObject playerObj = json.getAsJsonObject("player");
            page.entries = new ArrayList<>();
            for (JsonElement el : json.getAsJsonArray("entries")) {
                page.entries.add(parseCompletionEntry(el.getAsJsonObject(), null, playerObj));
            }
            return page;
        }).exceptionally(e -> {
            if (isErrorCode(e, "PLAYER_NOT_FOUND")) {
                return new LeaderboardPage<>();
            }
            throw rethrow(e);
        });
    }

    // --- HTTP helpers ---

    private CompletableFuture<JsonObject> getJson(String path) {
        Exception callSite = new Exception("Async origin: GET " + path);
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.webBackendUrl.value + path))
                        .header("X-API-Key", config.webApiKey.value)
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new ApiException(response.statusCode(), "GET", path, response.body());
                }
                return gson.fromJson(response.body(), JsonObject.class);
            } catch (ApiException e) {
                e.addSuppressed(callSite);
                throw e;
            } catch (Exception e) {
                RuntimeException wrapped = new RuntimeException(e);
                wrapped.addSuppressed(callSite);
                throw wrapped;
            }
        }, executor);
    }

    private CompletableFuture<JsonObject> postJson(String path, JsonObject body) {
        Exception callSite = new Exception("Async origin: POST " + path);
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.webBackendUrl.value + path))
                        .header("X-API-Key", config.webApiKey.value)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200 && response.statusCode() != 201) {
                    throw new ApiException(response.statusCode(), "POST", path, response.body());
                }
                return gson.fromJson(response.body(), JsonObject.class);
            } catch (ApiException e) {
                e.addSuppressed(callSite);
                throw e;
            } catch (Exception e) {
                RuntimeException wrapped = new RuntimeException(e);
                wrapped.addSuppressed(callSite);
                throw wrapped;
            }
        }, executor);
    }

    // --- Exception helpers ---

    public static ApiException unwrapApiException(Throwable t) {
        while (t != null) {
            if (t instanceof ApiException api) return api;
            t = t.getCause();
        }
        return null;
    }

    private static boolean isErrorCode(Throwable t, String errorCode) {
        ApiException api = unwrapApiException(t);
        return api != null && errorCode.equals(api.errorCode);
    }

    private static CompletionException rethrow(Throwable t) {
        return t instanceof CompletionException ce ? ce : new CompletionException(t);
    }

    // --- JSON helpers ---

    // hOrLObj: history or leaderboard entry, note that leaderboard entry does not have questId/questName
    // lQuestObj: quest object from leaderboard entry
    private QuestCompletionData parseCompletionEntry(JsonObject hOrLObj, JsonObject lQuestObj, JsonObject hPlayerObj) {
        QuestCompletionData data = new QuestCompletionData();
        if (hPlayerObj != null) {
            data.playerUuid = UUID.fromString(hPlayerObj.get("playerUuid").getAsString());
            data.playerName = hPlayerObj.has("playerName") ? hPlayerObj.get("playerName").getAsString() : null;
        } else {
            data.playerUuid = UUID.fromString(hOrLObj.get("playerUuid").getAsString());
            data.playerName = hOrLObj.has("playerName") ? hOrLObj.get("playerName").getAsString() : null;
        }
        if (lQuestObj != null) {
            data.questId = lQuestObj.has("id") ? lQuestObj.get("id").getAsString() : null;
            data.questName = lQuestObj.has("name") ? lQuestObj.get("name").getAsString() : null;
        } else {
            data.questId = hOrLObj.has("questId") ? hOrLObj.get("questId").getAsString() : null;
            data.questName = hOrLObj.has("questName") ? hOrLObj.get("questName").getAsString() : null;
        }
        data.completionTime = hOrLObj.has("completionTime") ? hOrLObj.get("completionTime").getAsLong() : 0;
        data.durationMillis = hOrLObj.has("durationMillis") ? hOrLObj.get("durationMillis").getAsLong() : 0;
        if (lQuestObj != null) {
            data.questPoints = lQuestObj.has("questPoints") ? lQuestObj.get("questPoints").getAsInt() : 0;
        } else {
            data.questPoints = hOrLObj.has("questPoints") ? hOrLObj.get("questPoints").getAsInt() : 0;
        }
        if (hOrLObj.has("stepDetails") && !hOrLObj.get("stepDetails").isJsonNull()) {
            data.stepDetails = gson.fromJson(hOrLObj.get("stepDetails"),
                    new TypeToken<Map<Integer, QuestCompletionData.StepDetail>>() {}.getType());
        } else if (hOrLObj.has("stepDurations") && !hOrLObj.get("stepDurations").isJsonNull()) {
            Map<Integer, Long> oldDurations = gson.fromJson(hOrLObj.get("stepDurations"),
                    new TypeToken<Map<Integer, Long>>() {}.getType());
            data.stepDetails = new HashMap<>();
            for (Map.Entry<Integer, Long> entry : oldDurations.entrySet()) {
                data.stepDetails.put(entry.getKey(),
                        new QuestCompletionData.StepDetail(entry.getValue(), null, null));
            }
        } else {
            data.stepDetails = new HashMap<>();
        }
        return data;
    }

    // --- Exception & Response DTOs ---

    public static class ApiException extends RuntimeException {
        public final int statusCode;
        public final String errorCode;
        public final String errorMessage;

        public ApiException(int statusCode, String method, String path, String responseBody) {
            super(formatMessage(statusCode, method, path, responseBody));
            this.statusCode = statusCode;
            String parsedCode = null;
            String parsedMessage = null;
            if (responseBody != null && !responseBody.isBlank()) {
                try {
                    JsonObject obj = NQuestGson.INSTANCE.fromJson(responseBody, JsonObject.class);
                    if (obj.has("error")) parsedCode = obj.get("error").getAsString();
                    if (obj.has("message")) parsedMessage = obj.get("message").getAsString();
                } catch (Exception ignored) {
                }
            }
            this.errorCode = parsedCode;
            this.errorMessage = parsedMessage;
        }

        private static String formatMessage(int statusCode, String method, String path, String responseBody) {
            StringBuilder sb = new StringBuilder("HTTP ").append(statusCode)
                    .append(" from ").append(method).append(" ").append(path);
            if (responseBody != null && responseBody.length() <= 200) {
                sb.append(": ").append(responseBody);
            }
            return sb.toString();
        }
    }

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
