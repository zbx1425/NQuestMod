package cn.zbx1425.nquestmod.data.ranking;

import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.NQuestGson;
import cn.zbx1425.nquestmod.data.quest.QuestCompletionData;
import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PendingCompletions {

    private final Path walFile;
    private final Gson gson = NQuestGson.INSTANCE;
    private final AtomicBoolean replayInProgress = new AtomicBoolean(false);

    public PendingCompletions(Path basePath) {
        this.walFile = basePath.resolve("pending_completions.jsonl");
    }

    public synchronized void enqueue(QuestCompletionData data) {
        try {
            String line = gson.toJson(data) + "\n";
            Files.writeString(walFile, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            NQuestMod.LOGGER.error("Failed to write pending completion to WAL", e);
        }
    }

    public boolean hasPending() {
        return Files.exists(walFile);
    }

    public synchronized List<QuestCompletionData> drainAll() {
        List<QuestCompletionData> results = new ArrayList<>();
        if (!Files.exists(walFile)) return results;
        try {
            List<String> lines = Files.readAllLines(walFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                try {
                    results.add(gson.fromJson(trimmed, QuestCompletionData.class));
                } catch (Exception e) {
                    NQuestMod.LOGGER.warn("Skipping malformed WAL entry: {}", trimmed, e);
                }
            }
            Files.deleteIfExists(walFile);
        } catch (IOException e) {
            NQuestMod.LOGGER.error("Failed to read pending completions from WAL", e);
        }
        return results;
    }

    public void replayAll(RankingApiClient rankingApi) {
        if (!replayInProgress.compareAndSet(false, true)) return;
        try {
            List<QuestCompletionData> pending = drainAll();
            if (pending.isEmpty()) return;
            NQuestMod.LOGGER.info("Replaying {} pending completions from WAL", pending.size());
            for (QuestCompletionData data : pending) {
                rankingApi.submitCompletion(data).whenComplete((response, error) -> {
                    if (error != null) {
                        NQuestMod.LOGGER.error("Failed to replay pending completion for quest {}, re-enqueuing", data.questId, error);
                        enqueue(data);
                    }
                });
            }
        } finally {
            replayInProgress.set(false);
        }
    }

    public void replayIfNeeded(RankingApiClient rankingApi) {
        if (!hasPending()) return;
        replayAll(rankingApi);
    }
}
