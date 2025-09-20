package cn.zbx1425.nquestmod.data.ranking;

import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.quest.PlayerProfile;
import cn.zbx1425.nquestmod.data.quest.Quest;
import cn.zbx1425.nquestmod.data.quest.QuestCompletionData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.sqlite.SQLiteConfig;

import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class QuestUserDatabase {

    private Connection connection;
    private final Gson GSON = new GsonBuilder().create();

    public QuestUserDatabase(Path dbPath) throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toString(), config.toProperties());
        initSchema();
    }

    private void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_profiles (" +
                    "player_uuid TEXT PRIMARY KEY NOT NULL," +
                    "total_quest_points INTEGER NOT NULL DEFAULT 0," +
                    "total_quest_completions INTEGER NOT NULL DEFAULT 0," +
                    "json TEXT NOT NULL" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS quest_completions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_uuid TEXT NOT NULL," +
                    "quest_id TEXT NOT NULL," +
                    "completion_time INTEGER NOT NULL," +
                    "duration_millis INTEGER NOT NULL," +
                    "quest_points INTEGER NOT NULL," +
                    "json TEXT" +
                    ")");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_profiles_qp ON player_profiles (total_quest_points DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_profiles_completions ON player_profiles (total_quest_completions DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_completions_player_time ON quest_completions (player_uuid, completion_time DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_completions_quest_duration ON quest_completions (quest_id, duration_millis)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_completions_time ON quest_completions (completion_time)");
        }
    }

    public PlayerProfile loadPlayerProfile(UUID playerUuid) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT total_quest_points, total_quest_completions, json FROM player_profiles WHERE player_uuid = ?")) {
            stmt.setString(1, playerUuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                PlayerProfile profile = GSON.fromJson(rs.getString("json"), PlayerProfile.class);
                profile.playerUuid = playerUuid;
                // These two fields are authoritative from the DB.
                profile.totalQuestPoints = rs.getInt("total_quest_points");
                profile.totalQuestCompletions = rs.getInt("total_quest_completions");
                return profile;
            } else {
                PlayerProfile newProfile = new PlayerProfile();
                newProfile.playerUuid = playerUuid;
                return newProfile;
            }
        }
    }

    public void savePlayerProfile(PlayerProfile profile) throws SQLException {
        String json = GSON.toJson(profile);
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO player_profiles(player_uuid, total_quest_points, total_quest_completions, json) VALUES(?, ?, ?, ?) " +
                        "ON CONFLICT(player_uuid) DO UPDATE SET " +
                        "total_quest_points = excluded.total_quest_points, " +
                        "total_quest_completions = excluded.total_quest_completions, " +
                        "json = excluded.json")) {
            stmt.setString(1, profile.playerUuid.toString());
            stmt.setInt(2, profile.totalQuestPoints);
            stmt.setInt(3, profile.totalQuestCompletions);
            stmt.setString(4, json);
            stmt.executeUpdate();
        }
    }

    public void addQuestCompletion(UUID playerUuid, Quest quest, QuestCompletionData completionData) throws SQLException {
        connection.setAutoCommit(false);
        try {
            JsonObject miscFields = new JsonObject();
            miscFields.add("stepDurations", GSON.toJsonTree(completionData.stepDurations));
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO quest_completions(player_uuid, quest_id, completion_time, duration_millis, quest_points, json) " +
                            "VALUES(?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, quest.id);
                stmt.setLong(3, completionData.completionTime);
                stmt.setLong(4, completionData.durationMillis);
                stmt.setInt(5, quest.questPoints);
                stmt.setString(6, GSON.toJson(miscFields));
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE player_profiles SET " +
                            "total_quest_points = total_quest_points + ?, " +
                            "total_quest_completions = total_quest_completions + 1, " +
                            "json = '{}' " + // We only doing one quest at a time, so just clear activeQuests
                            "WHERE player_uuid = ?")) {
                stmt.setInt(1, quest.questPoints);
                stmt.setString(2, playerUuid.toString());
                int updatedRows = stmt.executeUpdate();
                if (updatedRows == 0) {
                    // This case happens if a player completes their very first quest
                    // and doesn't have a profile entry yet.
                    PlayerProfile newProfile = new PlayerProfile();
                    newProfile.playerUuid = playerUuid;
                    newProfile.totalQuestPoints = quest.questPoints;
                    newProfile.totalQuestCompletions = 1;
                    savePlayerProfile(newProfile); // This will call INSERT
                }
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public List<PlayerQPEntry> getOverallQPLeaderboard(int limit, boolean monthly) throws SQLException {
        List<PlayerQPEntry> result = new ArrayList<>();
        String sql;
        if (monthly) {
            sql = "SELECT player_uuid, SUM(quest_points) AS monthly_qp " +
                    "FROM quest_completions WHERE completion_time >= ? " +
                    "GROUP BY player_uuid ORDER BY monthly_qp DESC LIMIT ?";
        } else {
            sql = "SELECT player_uuid, total_quest_points FROM player_profiles " +
                "WHERE total_quest_points > 0 ORDER BY total_quest_points DESC LIMIT ?";
        }
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (monthly) {
                stmt.setLong(1, getMonthlyTimestampCutoff());
                stmt.setInt(2, limit);
            } else {
                stmt.setInt(1, limit);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(new PlayerQPEntry(UUID.fromString(rs.getString("player_uuid")), rs.getInt(2)));
            }
        }
        return result;
    }

    public List<PlayerCompletionsEntry> getQuestCompletionsLeaderboard(int limit, boolean monthly) throws SQLException {
        List<PlayerCompletionsEntry> result = new ArrayList<>();
        String sql;
        if (monthly) {
            sql = "SELECT player_uuid, COUNT(id) AS monthly_completions " +
                    "FROM quest_completions WHERE completion_time >= ? " +
                    "GROUP BY player_uuid ORDER BY monthly_completions DESC LIMIT ?";
        } else {
            sql = "SELECT player_uuid, total_quest_completions FROM player_profiles " +
                    "WHERE total_quest_completions > 0 ORDER BY total_quest_completions DESC LIMIT ?";
        }
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (monthly) {
                stmt.setLong(1, getMonthlyTimestampCutoff());
                stmt.setInt(2, limit);
            } else {
                stmt.setInt(1, limit);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(new PlayerCompletionsEntry(UUID.fromString(rs.getString("player_uuid")), rs.getInt(2)));
            }
        }
        return result;
    }

    public List<QuestCompletionData> getQuestTimeLeaderboard(String questId, int limit, boolean monthly) throws SQLException {
        List<QuestCompletionData> result = new ArrayList<>();
        String sql = "SELECT * FROM quest_completions " +
                "WHERE quest_id = ? " +
                (monthly ? "AND completion_time >= ? " : "") +
                "ORDER BY duration_millis ASC LIMIT ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int paramIndex = 1;
            stmt.setString(paramIndex++, questId);
            if (monthly) {
                stmt.setLong(paramIndex++, getMonthlyTimestampCutoff());
            }
            stmt.setInt(paramIndex, limit);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(questCompletionDataFromResultSet(rs));
            }
        }
        return result;
    }

    public List<QuestCompletionData> getPlayerQuestHistory(UUID playerUuid, int limit, int offset) throws SQLException {
        List<QuestCompletionData> result = new ArrayList<>();
        String sql = "SELECT * FROM quest_completions WHERE player_uuid = ? ORDER BY completion_time DESC LIMIT ? OFFSET ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(questCompletionDataFromResultSet(rs));
            }
        }
        return result;
    }

    private QuestCompletionData questCompletionDataFromResultSet(ResultSet rs) throws SQLException {
        QuestCompletionData data = new QuestCompletionData();
        data.playerUuid = UUID.fromString(rs.getString("player_uuid"));
        data.questId = rs.getString("quest_id");
        data.completionTime = rs.getLong("completion_time");
        data.durationMillis = rs.getLong("duration_millis");
        data.questPoints = rs.getInt("quest_points");
        String json = rs.getString("json");
        if (json != null) {
            JsonObject miscFields = GSON.fromJson(json, JsonObject.class);
            data.stepDurations = GSON.fromJson(miscFields.get("stepDurations"), new TypeToken<Map<Integer, Long>>() {}.getType());
        } else {
            data.stepDurations = new HashMap<>();
        }
        return data;
    }

    private long getMonthlyTimestampCutoff() {
        return Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli();
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                NQuestMod.LOGGER.error("Failed to close database connection", e);
            }
        }
    }
}
