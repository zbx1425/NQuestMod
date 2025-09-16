package cn.zbx1425.nquestbot;

import cn.zbx1425.nquestbot.data.QuestDispatcher;
import cn.zbx1425.nquestbot.data.IQuestCallbacks;
import cn.zbx1425.nquestbot.data.quest.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class QuestNotifications implements IQuestCallbacks {

    private final MinecraftServer server;

    public QuestNotifications(MinecraftServer server) {
        this.server = server;
    }

    public void onPlayerJoin(QuestDispatcher questEngine, UUID playerUuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player == null) return;
        updateBossBarForPlayer(questEngine, player);
    }

    @Override
    public void onQuestStarted(QuestDispatcher questEngine, UUID playerUuid, Quest quest) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player == null) return;
        player.sendSystemMessage(Component.literal("⭐ Quest Started! ⭐")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)), false);
        player.sendSystemMessage(Component.literal(quest.name).withStyle(ChatFormatting.YELLOW), false);

        if (quest.steps.size() > 0) {
            Step firstStep = quest.steps.get(0);
            player.sendSystemMessage(Component.literal("▶ First: ").withStyle(ChatFormatting.AQUA)
                    .append(firstStep.criteria.getDisplayRepr()), false);
        }
        updateBossBarForPlayer(questEngine, player);
    }

    @Override
    public void onStepCompleted(QuestDispatcher questEngine, UUID playerUuid, Quest quest, QuestProgress progress) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player == null) return;

        Step completedStep = quest.steps.get(progress.currentStepIndex);
        player.sendSystemMessage(Component.literal("✔ Step Complete: ").withStyle(ChatFormatting.GREEN)
                .append(completedStep.criteria.getDisplayRepr()), false);

        if (progress.currentStepIndex < quest.steps.size()) {
            Step nextStep = quest.steps.get(progress.currentStepIndex);
            MutableComponent nextStepMsg = Component.literal("▶ Next: ").withStyle(ChatFormatting.AQUA)
                    .append(nextStep.criteria.getDisplayRepr());
            player.sendSystemMessage(nextStepMsg, false);
        }
        updateBossBarForPlayer(questEngine, player);
    }

    @Override
    public void onQuestCompleted(QuestDispatcher questEngine, UUID playerUuid, Quest quest, QuestCompletionData data) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player == null) return;
        player.sendSystemMessage(Component.literal("⭐ Quest Complete! ⭐")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)), false);
        player.sendSystemMessage(Component.literal(quest.name).withStyle(ChatFormatting.YELLOW), false);
        player.sendSystemMessage(Component.literal("  Time taken: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(formatDuration(data.durationMillis)).withStyle(ChatFormatting.AQUA)), false);
        player.sendSystemMessage(Component.literal("  Quest Points: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal("+" + quest.questPoints + " QP").withStyle(ChatFormatting.GREEN)), false);
        updateBossBarForPlayer(questEngine, player);
    }

    @Override
    public void onQuestAborted(QuestDispatcher questEngine, UUID playerUuid, Quest quest) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player == null) return;
        player.sendSystemMessage(Component.literal("✘ Quest Aborted ✘")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)), false);
        player.sendSystemMessage(Component.literal(quest.name).withStyle(ChatFormatting.YELLOW), false);
        updateBossBarForPlayer(questEngine, player);
    }

    private void updateBossBarForPlayer(QuestDispatcher questEngine, ServerPlayer player) {
        Optional<Consumer<CustomBossEvent>> bossBarMessage = getBossBarMessage(questEngine, player.getGameProfile().getId());
        ResourceLocation playerId = NQuestBot.id(player.getGameProfile().getId().toString());
        CustomBossEvent event = player.getServer().getCustomBossEvents().get(playerId);
        if (bossBarMessage.isPresent()) {
            if (event == null) {
                event = player.getServer().getCustomBossEvents().create(playerId, Component.empty());
            }
            bossBarMessage.get().accept(event);
            event.setPlayers(List.of(player));
        } else {
            if (event != null) {
                player.getServer().getCustomBossEvents().remove(event);
            }
        }
    }

    public Optional<Consumer<CustomBossEvent>> getBossBarMessage(QuestDispatcher questEngine, UUID playerUuid) {
        PlayerProfile profile = questEngine.getPlayerProfile(playerUuid);
        if (profile == null || profile.activeQuests.isEmpty()) {
            return Optional.empty();
        }

        return profile.activeQuests.values().stream().findFirst().map(progress -> {
            Quest quest = questEngine.quests.get(progress.questId);
            if (quest == null || progress.currentStepIndex >= quest.steps.size()) {
                return null;
            }
            Step currentStep = quest.steps.get(progress.currentStepIndex);
            return (event) -> {
                 event.setName(currentStep.criteria.getDisplayRepr());
                 event.setMax(quest.steps.size());
                 event.setValue(progress.currentStepIndex);
                 event.setColor(BossEvent.BossBarColor.BLUE);
                 event.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
            };
        });
    }

    private String formatDuration(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
