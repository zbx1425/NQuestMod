package cn.zbx1425.nquestmod.sgui;

import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.ranking.PlayerCompletionsEntry;
import cn.zbx1425.nquestmod.data.ranking.PlayerQPEntry;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.BaseSlotGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public class LeaderboardScreen extends TabbedItemListGui<Object, LeaderboardScreen.LeaderboardType, LeaderboardScreen.TimeRange> {

    public enum LeaderboardType { QP, COMPLETIONS, SPEEDRUN }
    public enum TimeRange { ALL_TIME, MONTHLY }
    public static final List<Pair<LeaderboardType, Supplier<GuiElementBuilder>>> PRIMARY_TABS = List.of(
        Pair.of(LeaderboardType.QP, () -> new GuiElementBuilder(Items.DIAMOND).setName(Component.literal("Total QP"))),
        Pair.of(LeaderboardType.COMPLETIONS, () -> new GuiElementBuilder(Items.EMERALD).setName(Component.literal("Total Completions"))),
        Pair.of(LeaderboardType.SPEEDRUN, () -> new GuiElementBuilder(Items.CLOCK).setName(Component.literal("Quest Speedruns")))
    );
    public static final List<Pair<TimeRange, Supplier<GuiElementBuilder>>> SECONDARY_TABS = List.of(
        Pair.of(TimeRange.ALL_TIME, () -> new GuiElementBuilder(Items.GOLD_BLOCK).setName(Component.literal("All-Time"))),
        Pair.of(TimeRange.MONTHLY, () -> new GuiElementBuilder(Items.IRON_BLOCK).setName(Component.literal("Monthly")))
    );

    public LeaderboardScreen(ServerPlayer player, BaseSlotGui parent) {
        super(MenuType.GENERIC_9x4, player, parent,
                LeaderboardType.QP, PRIMARY_TABS,
                TimeRange.ALL_TIME, SECONDARY_TABS
        );
        setTitle(Component.literal("Leaderboards"));
        init();
    }

    @Override
    public void init() {
        if (selectedPrimaryTab == LeaderboardType.SPEEDRUN) {
            selectedPrimaryTab = LeaderboardType.QP;
            new QuestListScreen(player, parent, (quest) -> {
                new QuestSpeedrunScreen(player, this, quest, selectedSecondaryTab == TimeRange.MONTHLY).open();
            }).open();
            return;
        }
        super.init();
        fillHeaderFooter();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected CompletableFuture<Pair<List<Object>, Integer>> supplyItems(int offset, int limit) {
        boolean isMonthly = selectedSecondaryTab == TimeRange.MONTHLY;
        return CompletableFuture.supplyAsync(() -> {
            try {
                List result;
                switch (selectedPrimaryTab) {
                    case QP:
                        result = NQuestMod.INSTANCE.userDatabase.getOverallQPLeaderboard(limit, isMonthly);
                        break;
                    case COMPLETIONS:
                        result = NQuestMod.INSTANCE.userDatabase.getQuestCompletionsLeaderboard(limit, isMonthly);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + selectedPrimaryTab);
                    // SPEEDRUN case will be handled separately
                }
                return Pair.of(result, result.size());
            } catch (SQLException e) {
                NQuestMod.LOGGER.error("Failed to load leaderboard data", e);
            }
            return Pair.of(List.of(), 0);
        });
    }

    @Override
    protected GuiElementBuilder createElementForItem(Object entry, int index) {
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
            final int rank = page * ((rowContentEnds - rowContentStarts + 1) * 9) + index + 1;
            final String finalValue = value;
            return buildPlayerEntry(player.getServer(), playerUuid, p -> Component.literal("#" + rank + " " + p))
                .addLoreLine(Component.literal(finalValue).withStyle(ChatFormatting.GOLD));
        }
        return new GuiElementBuilder(Items.BARRIER).setName(Component.literal("Error"));
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
