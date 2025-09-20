package cn.zbx1425.nquestmod.sgui;

import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.quest.Quest;
import cn.zbx1425.nquestmod.data.quest.QuestCompletionData;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.BaseSlotGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class QuestHistoryScreen extends ItemListGui<QuestCompletionData> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public QuestHistoryScreen(ServerPlayer player, BaseSlotGui parent) {
        super(MenuType.GENERIC_9x4, player, parent);
        setTitle(Component.literal("My Quest History"));
        init();
    }

    @Override
    public void init() {
        super.init();
        fillHeaderFooter();
    }

    @Override
    protected CompletableFuture<Pair<List<QuestCompletionData>, Integer>> supplyItems(int offset, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<QuestCompletionData> history = NQuestMod.INSTANCE.userDatabase
                        .getPlayerQuestHistory(player.getGameProfile().getId(), limit, offset);
                return Pair.of(history, history.size() < limit ? history.size() : 99999); // Assume there's always more for history
            } catch (SQLException e) {
                NQuestMod.LOGGER.error("Failed to load player quest history", e);
                return Pair.of(List.of(), 0);
            }
        });
    }

    @Override
    protected GuiElementBuilder createElementForItem(QuestCompletionData item, int index) {
        Quest quest = NQuestMod.INSTANCE.questDispatcher.quests.get(item.questId);
        String questName = (quest != null) ? quest.name : item.questId;
        long seconds = item.durationMillis / 1000;

        return new GuiElementBuilder(Items.BOOK)
                .setName(Component.literal(questName))
                .addLoreLine(Component.literal("Completed on: ").append(
                    Component.literal(dateFormat.format(new Date(item.completionTime))).withStyle(ChatFormatting.GOLD)))
                .addLoreLine(Component.literal("Duration: ").append(
                    Component.literal(String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)).withStyle(ChatFormatting.AQUA)))
                .addLoreLine(Component.literal("QP Awarded: ").append(
                    Component.literal(String.valueOf(item.questPoints)).withStyle(ChatFormatting.GREEN)))
                .addLoreLine(Component.literal("Click for details").withStyle(ChatFormatting.GRAY))
                .setCallback((i, t, a) -> {
                    new QuestCompletionDetailScreen(player, this, item).open();
                });
    }
}
