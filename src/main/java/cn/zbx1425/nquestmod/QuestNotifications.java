package cn.zbx1425.nquestmod;

import cn.zbx1425.nquestmod.data.QuestDispatcher;
import cn.zbx1425.nquestmod.data.IQuestCallbacks;
import cn.zbx1425.nquestmod.data.quest.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

        if (!quest.steps.isEmpty()) {
            Step firstStep = quest.steps.get(0);
            player.sendSystemMessage(Component.literal("▶ First: ").withStyle(ChatFormatting.AQUA)
                    .append(firstStep.criteria.getDisplayRepr()), false);
        }
        sendSoundEffect(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0f, 1.0f);
        updateBossBarForPlayer(questEngine, player);
    }

    @Override
    public void onStepCompleted(QuestDispatcher questEngine, UUID playerUuid, Quest quest, QuestProgress progress) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player == null) return;

        if (progress.currentStepIndex > 0) {
            Step completedStep = quest.steps.get(progress.currentStepIndex - 1);
            player.sendSystemMessage(Component.literal("✔ Step Complete: ").withStyle(ChatFormatting.GREEN)
                .append(completedStep.criteria.getDisplayRepr()), false);
        }

        if (progress.currentStepIndex < quest.steps.size()) {
            Step nextStep = quest.steps.get(progress.currentStepIndex);
            MutableComponent nextStepMsg = Component.literal("▶ Next: ").withStyle(ChatFormatting.AQUA)
                    .append(nextStep.criteria.getDisplayRepr());
            player.sendSystemMessage(nextStepMsg, false);
        }
        sendSoundEffect(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0f, 1.0f);
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
        sendSoundEffect(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        updateBossBarForPlayer(questEngine, player);
    }

    @Override
    public void onQuestAborted(QuestDispatcher questEngine, UUID playerUuid, Quest quest) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player == null) return;
        player.sendSystemMessage(Component.literal("✘ Quest Aborted ✘")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)), false);
        player.sendSystemMessage(Component.literal(quest.name).withStyle(ChatFormatting.YELLOW), false);
        sendSoundEffect(player, SoundEvents.ANVIL_LAND, 0.5f, 1.0f);
        updateBossBarForPlayer(questEngine, player);
    }

    @Override
    public void onQuestFailed(QuestDispatcher questEngine, UUID playerUuid, Quest quest, Component reason) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player == null) return;
        player.sendSystemMessage(Component.literal("✘ Quest Failed ✘")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)), false);
        player.sendSystemMessage(Component.literal(quest.name).withStyle(ChatFormatting.YELLOW), false);
        player.sendSystemMessage(Component.literal("  Reason: ").withStyle(ChatFormatting.WHITE)
            .append(reason.copy().withStyle(ChatFormatting.RED)), false);
        sendSoundEffect(player, SoundEvents.ANVIL_LAND, 0.5f, 1.0f);
        updateBossBarForPlayer(questEngine, player);
    }

    private void updateBossBarForPlayer(QuestDispatcher questEngine, ServerPlayer player) {
        Optional<Consumer<CustomBossEvent>> bossBarMessage = getBossBarMessage(questEngine, player.getGameProfile().getId());
        ResourceLocation playerId = NQuestMod.id(player.getGameProfile().getId().toString());
        CustomBossEvent event = player.getServer().getCustomBossEvents().get(playerId);
        if (bossBarMessage.isPresent()) {
            if (event == null) {
                event = player.getServer().getCustomBossEvents().create(playerId, Component.empty());
                event.setPlayers(List.of(player));
                event.setColor(BossEvent.BossBarColor.BLUE);
                event.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
            }
            bossBarMessage.get().accept(event);
        } else {
            if (event != null) {
                event.removeAllPlayers();
                player.getServer().getCustomBossEvents().remove(event);
            }
        }
    }

    private Optional<Consumer<CustomBossEvent>> getBossBarMessage(QuestDispatcher questEngine, UUID playerUuid) {
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
            };
        });
    }

    private String formatDuration(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void sendSoundEffect(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
            SoundSource.MASTER,
            player.getX(), player.getY(), player.getZ(),
            volume, pitch, 0
        ));
    }
}
