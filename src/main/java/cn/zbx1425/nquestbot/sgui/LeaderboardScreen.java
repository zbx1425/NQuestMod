package cn.zbx1425.nquestbot.sgui;

import cn.zbx1425.nquestbot.NQuestBot;
import cn.zbx1425.nquestbot.data.ranking.PlayerCompletionsEntry;
import cn.zbx1425.nquestbot.data.ranking.PlayerQPEntry;
import cn.zbx1425.nquestbot.data.ranking.QuestTimeEntry;
import com.mojang.authlib.GameProfile;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.BaseSlotGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class LeaderboardScreen extends SimpleGui {

    private BaseSlotGui parent;

    private enum LeaderboardType { QP, COMPLETIONS, SPEEDRUN }
    private LeaderboardType currentType = LeaderboardType.QP;
    private boolean monthly = false;

    public LeaderboardScreen(ServerPlayer player, BaseSlotGui parent) {
        super(MenuType.GENERIC_9x4, player, false);
        this.parent = parent;

        setTitle(Component.literal("Leaderboards"));
        draw();
    }

    private void draw() {
        // Clear previous content
        for (int i = 0; i < getSize(); i++) {
            clearSlot(i);
        }

        // Tabs
        setSlot(1, glowIf(new GuiElementBuilder(Items.DIAMOND), currentType == LeaderboardType.QP)
                .setName(Component.literal("Total QP"))
                .setCallback((i, t, a) -> { currentType = LeaderboardType.QP; draw(); }));
        setSlot(2, glowIf(new GuiElementBuilder(Items.EMERALD), currentType == LeaderboardType.COMPLETIONS)
                .setName(Component.literal("Total Completions"))
                .setCallback((i, t, a) -> { currentType = LeaderboardType.COMPLETIONS; draw(); }));
        setSlot(3, glowIf(new GuiElementBuilder(Items.CLOCK), currentType == LeaderboardType.SPEEDRUN)
                .setName(Component.literal("Quest Speedruns"))
                .setCallback((i, t, a) -> {
                    // TODO: Open quest selection for speedrun
                }));

        // Time Range
        setSlot(7, glowIf(new GuiElementBuilder(Items.GOLD_BLOCK), !monthly)
                .setName(Component.literal("All-Time"))
                .setCallback((i, t, a) -> { monthly = false; draw(); }));
        setSlot(8, glowIf(new GuiElementBuilder(Items.IRON_BLOCK), monthly)
                .setName(Component.literal("Monthly"))
                .setCallback((i, t, a) -> { monthly = true; draw(); }));

        // Back button
        setSlot(9 * 3, new GuiElementBuilder(Items.ARROW)
                .setName(Component.literal("Back"))
                .setCallback((index, type, action) -> close())
        );

        loadAndDisplayData();
    }

    @Override
    public void onClose() {
        if (parent != null) parent.open();
    }

    private void loadAndDisplayData() {
        setSlot(9 * 2 + 4, new GuiElementBuilder(Items.LIGHT_GRAY_CONCRETE).setName(Component.literal("Loading...")));

        CompletableFuture.supplyAsync(() -> {
            try {
                switch (currentType) {
                    case QP:
                        return NQuestBot.INSTANCE.userDatabase.getOverallQPLeaderboard(20, monthly);
                    case COMPLETIONS:
                        return NQuestBot.INSTANCE.userDatabase.getQuestCompletionsLeaderboard(20, monthly);
                    // SPEEDRUN case will be handled separately
                }
            } catch (SQLException e) {
                NQuestBot.LOGGER.error("Failed to load leaderboard data", e);
            }
            return List.of();
        }).thenAcceptAsync(this::displayData, player.getServer());
    }

    private void displayData(List<?> data) {
        // Clear loading indicator and previous data
        for (int i = 9; i < 9 + 9 * 2; i++) clearSlot(i);

        if (data == null || data.isEmpty()) {
            setSlot(9 * 2 + 4, new GuiElementBuilder(Items.GLASS_BOTTLE).setName(Component.literal("No data available.")));
            return;
        }

        for (int i = 0; i < data.size(); i++) {
            int slot = 9 + i;
            if (slot >= 9 + 9 * 2) break;

            Object entry = data.get(i);
            UUID playerUuid = null;
            String value = "";

            if (entry instanceof PlayerQPEntry qpEntry) {
                playerUuid = qpEntry.playerUuid;
                value = qpEntry.totalQP + " QP";
            } else if (entry instanceof PlayerCompletionsEntry compEntry) {
                playerUuid = compEntry.playerUuid;
                value = compEntry.totalCompletions + " completions";
            }

            if (playerUuid != null) {
                final int rank = i + 1;
                final String finalValue = value;
                setSlot(slot, buildPlayerEntry(player.getServer(), playerUuid, p ->
                        Component.literal("#" + rank + " " + p + " - " + finalValue)));
            }
        }
    }

    private GuiElementBuilder glowIf(GuiElementBuilder builder, boolean glow) {
        if (glow) builder.setCount(64).glow();
        return builder;
    }

    private GuiElementBuilder buildPlayerEntry(MinecraftServer server, UUID uuid, Function<String, Component> nameBuilder) {
        var builder = new GuiElementBuilder(Items.PLAYER_HEAD);
        server.getProfileCache().get(uuid).ifPresentOrElse(profile -> {
            builder.setSkullOwner(profile, server);
            builder.setName(nameBuilder.apply(profile.getName()));
        }, () -> {
            builder.setName(nameBuilder.apply(uuid.toString()));
        });
        return builder;
    }
}
