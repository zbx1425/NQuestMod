package cn.zbx1425.nquestbot.sgui;

import cn.zbx1425.nquestbot.NQuestBot;
import cn.zbx1425.nquestbot.data.quest.Quest;
import cn.zbx1425.nquestbot.data.quest.QuestCompletionData;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.BaseSlotGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class QuestHistoryScreen extends SimpleGui {

    private BaseSlotGui parent;

    private final int page;
    private static final int PAGE_SIZE = 2 * 9;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public QuestHistoryScreen(ServerPlayer player, BaseSlotGui parent, int page) {
        super(MenuType.GENERIC_9x3, player, false);
        this.parent = parent;

        this.page = page;
        setTitle(Component.literal("My Quest History (Page " + (page + 1) + ")"));
        loadHistory();
    }

    private void loadHistory() {
        CompletableFuture.supplyAsync(() -> {
            try {
                int offset = page * PAGE_SIZE;
                return NQuestBot.INSTANCE.userDatabase
                        .getPlayerQuestHistory(player.getGameProfile().getId(), PAGE_SIZE + 1, offset);
            } catch (SQLException e) {
                NQuestBot.LOGGER.error("Failed to load player quest history", e);
                return List.<QuestCompletionData>of();
            }
        }).thenAcceptAsync(this::updateDisplay, player.getServer());
    }

    private void updateDisplay(List<QuestCompletionData> history) {
        boolean hasNextPage = history.size() > PAGE_SIZE;

        for (int i = 0; i < PAGE_SIZE; i++) {
            if (i < history.size() && i < PAGE_SIZE) {
                QuestCompletionData entry = history.get(i);
                Quest quest = NQuestBot.INSTANCE.questDispatcher.quests.get(entry.questId);
                String questName = (quest != null) ? quest.name : entry.questId;
                long seconds = entry.durationMillis / 1000;

                setSlot(i, new GuiElementBuilder(Items.BOOK)
                        .setName(Component.literal(questName))
                        .setLore(List.of(
                                Component.literal("Completed on: " + dateFormat.format(new Date(entry.completionTime))),
                                Component.literal(String.format("Duration: %d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)),
                                Component.literal("QP Awarded: " + entry.questPoints)
                        ))
                );
            } else {
                clearSlot(i);
            }
        }

        // Navigation buttons
        setSlot(9 * 3, new GuiElementBuilder(Items.ARROW)
                .setName(Component.literal("Back"))
                .setCallback((index, type, action) -> close())
        );

        if (page > 0) {
            setSlot(9 * 3 + 5, new GuiElementBuilder(Items.PAPER)
                    .setName(Component.literal("<<<<"))
                    .setCallback((index, type, action) -> new QuestHistoryScreen(player, parent, page - 1).open())
            );
        }

        if (hasNextPage) {
            setSlot(9 * 3 + 7, new GuiElementBuilder(Items.PAPER)
                    .setName(Component.literal(">>>>"))
                    .setCallback((index, type, action) -> new QuestHistoryScreen(player, parent, page + 1).open())
            );
        }
    }

    @Override
    public void onClose() {
        if (parent != null) parent.open();
    }
}
