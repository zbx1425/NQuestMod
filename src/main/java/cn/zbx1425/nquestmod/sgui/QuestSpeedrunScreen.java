package cn.zbx1425.nquestmod.sgui;

import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.quest.Quest;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.BaseSlotGui;
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
import net.minecraft.ChatFormatting;
import cn.zbx1425.nquestmod.data.quest.QuestCompletionData;

public class QuestSpeedrunScreen extends TabbedItemListGui<QuestCompletionData, LeaderboardScreen.LeaderboardType, Void> {

    private final Quest quest;
    private final boolean isMonthly;

    public QuestSpeedrunScreen(ServerPlayer player, BaseSlotGui parent, Quest quest, boolean isMonthly) {
        super(MenuType.GENERIC_9x4, player, parent,
                LeaderboardScreen.LeaderboardType.SPEEDRUN, LeaderboardScreen.PRIMARY_TABS,
                null, List.of()
        );
        this.quest = quest;
        this.isMonthly = isMonthly;
        setTitle(Component.literal("Leaderboards - Speedruns"));
        init();
    }

    @Override
    public void init() {
        super.init();

        if (selectedPrimaryTab != LeaderboardScreen.LeaderboardType.SPEEDRUN) {
            if (parent instanceof LeaderboardScreen leaderboardScreen) {
                leaderboardScreen.selectedPrimaryTab = selectedPrimaryTab;
                leaderboardScreen.init();
            }
            goBack();
            return;
        }

        setSlot(7, new GuiElementBuilder(Items.BOOK)
                .setName(Component.literal(quest.name))
                .setLore(quest.formatDescription())
        );
        setSlot(8, LeaderboardScreen.SECONDARY_TABS.get(isMonthly ? 1 : 0).getRight().get());

        fillHeaderFooter();
    }

    @Override
    protected CompletableFuture<Pair<List<QuestCompletionData>, Integer>> supplyItems(int offset, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<QuestCompletionData> entries = NQuestMod.INSTANCE.userDatabase.getQuestTimeLeaderboard(quest.id, limit, isMonthly);
                return Pair.of(entries, entries.size());
            } catch (SQLException e) {
                NQuestMod.LOGGER.error("Failed to load quest time leaderboard", e);
                return Pair.of(List.of(), 0);
            }
        });
    }

    @Override
    protected GuiElementBuilder createElementForItem(QuestCompletionData item, int index) {
        final int rank = page * ((rowContentEnds - rowContentStarts + 1) * 9) + index + 1;
        long seconds = item.durationMillis / 1000;
        final String timeStr = String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);

        return buildPlayerEntry(player.getServer(), item.playerUuid, p ->
            Component.literal("#" + rank + " " + p))
                .addLoreLine(Component.literal(timeStr).withStyle(ChatFormatting.GOLD))
                .addLoreLine(Component.literal("Click for details").withStyle(ChatFormatting.GRAY))
                .setCallback((i, t, a) -> {
                    new QuestCompletionDetailScreen(player, this, item).open();
                });
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
