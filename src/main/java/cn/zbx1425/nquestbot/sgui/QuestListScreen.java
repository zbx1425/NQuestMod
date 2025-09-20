package cn.zbx1425.nquestbot.sgui;

import cn.zbx1425.nquestbot.NQuestBot;
import cn.zbx1425.nquestbot.data.QuestException;
import cn.zbx1425.nquestbot.data.quest.Quest;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.BaseSlotGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuestListScreen extends SimpleGui {

    private BaseSlotGui parent;

    private final int page;
    private static final int PAGE_SIZE = 2 * 9;
    private final List<Quest> quests;

    public QuestListScreen(ServerPlayer player, BaseSlotGui parent, int page) {
        super(MenuType.GENERIC_9x3, player, false);
        this.parent = parent;

        this.page = page;
        this.quests = new ArrayList<>(NQuestBot.INSTANCE.questDispatcher.quests.values());
        setTitle(Component.literal("Start a Quest (Page " + (page + 1) + ")"));
        draw();
    }

    private void draw() {
        int startIndex = page * PAGE_SIZE;
        boolean hasNextPage = (startIndex + PAGE_SIZE) < quests.size();

        for (int i = 0; i < PAGE_SIZE; i++) {
            int questIndex = startIndex + i;
            if (questIndex < quests.size()) {
                Quest quest = quests.get(questIndex);
                setSlot(i, new GuiElementBuilder(Items.BOOK)
                        .setName(Component.literal(quest.name))
                        .setLore(formatDescription(quest.description))
                        .setCallback((index, type, action) -> openConfirmation(quest))
                );
            } else {
                clearSlot(i);
            }
        }

        setSlot(9 * 2, new GuiElementBuilder(Items.ARROW)
                .setName(Component.literal("Back to Main Menu"))
                .setCallback((index, type, action) -> close())
        );

        if (page > 0) {
            setSlot(9 * 2 + 5, new GuiElementBuilder(Items.PAPER)
                    .setName(Component.literal("<<<<"))
                    .setCallback((index, type, action) -> new QuestListScreen(player, parent, page - 1).open())
            );
        }

        if (hasNextPage) {
            setSlot(9 * 2 + 7, new GuiElementBuilder(Items.PAPER)
                    .setName(Component.literal(">>>>"))
                    .setCallback((index, type, action) -> new QuestListScreen(player, parent, page + 1).open())
            );
        }
    }

    @Override
    public void onClose() {
        if (parent != null) parent.open();
    }

    private void openConfirmation(Quest quest) {
        SimpleGui confirmationGui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
        confirmationGui.setTitle(Component.literal("Start Quest?"));
        confirmationGui.setSlot(9 + 1, new GuiElementBuilder(Items.BOOK)
                .setName(Component.literal(quest.name))
                .setLore(formatDescription(quest.description))
        );
        confirmationGui.setSlot(9 * 2 + 5, new GuiElementBuilder(Items.RED_CONCRETE)
                .setName(Component.literal("Cancel"))
                .setCallback((index, type, action) -> this.open())
        );
        confirmationGui.setSlot(9 * 2 + 7, new GuiElementBuilder(Items.GREEN_CONCRETE)
                .setName(Component.literal("Confirm"))
                .setCallback((index, type, action) -> {
                    try {
                        NQuestBot.INSTANCE.questDispatcher.startQuest(player.getGameProfile().getId(), quest.id);
                        confirmationGui.close();
                    } catch (QuestException e) {
                        player.sendSystemMessage(e.getDisplayRepr(), false);
                    }
                })
        );
        confirmationGui.open();
    }

    private List<Component> formatDescription(String description) {
        return List.of(description.split("\n")).stream()
                .map(line -> Component.literal(line).withStyle(ChatFormatting.GRAY))
                .collect(Collectors.toList());
    }
}
