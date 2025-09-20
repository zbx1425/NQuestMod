package cn.zbx1425.nquestbot.sgui;

import cn.zbx1425.nquestbot.NQuestBot;
import cn.zbx1425.nquestbot.data.QuestException;
import cn.zbx1425.nquestbot.data.quest.PlayerProfile;
import cn.zbx1425.nquestbot.data.quest.Quest;
import cn.zbx1425.nquestbot.data.quest.QuestProgress;
import cn.zbx1425.nquestbot.data.quest.Step;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.BaseSlotGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.stream.Collectors;

public class CurrentQuestScreen extends SimpleGui {

    private BaseSlotGui parent;

    private Quest quest;
    private QuestProgress progress;

    int stepPage = 0;

    public CurrentQuestScreen(ServerPlayer player, BaseSlotGui parent) {
        super(MenuType.GENERIC_9x3, player, false);
        this.parent = parent;

        PlayerProfile profile = NQuestBot.INSTANCE.questDispatcher.playerProfiles.get(player.getGameProfile().getId());
        if (profile == null) return;
        progress = profile.activeQuests.values().stream().findFirst().orElse(null);
        if (progress == null) return;
        quest = NQuestBot.INSTANCE.questDispatcher.quests.get(progress.questId);
        if (quest == null) return;
        setTitle(Component.literal(quest.name));

        init();
    }

    void init() {
        for (int i = stepPage * 9; i < Math.min(stepPage * 9 + 9, quest.steps.size()); i++) {
            Step step = quest.steps.get(i);
            if (step == null) continue;
            Item icon;
            if (progress.currentStepIndex < i) {
                icon = Items.GRAY_CONCRETE;
            } else if (progress.currentStepIndex == i) {
                icon = Items.YELLOW_CONCRETE;
            } else {
                icon = Items.GREEN_TERRACOTTA;
            }
            setSlot(i % 9, new GuiElementBuilder(icon)
                .setName(step.criteria.getDisplayRepr())
                .setCount(i + 1)
            );
        }

        if (stepPage > 0) {
            setSlot(9 * 2 + 5, new GuiElementBuilder(Items.PAPER)
                .setName(Component.literal("<<<<"))
                .setCallback((slot, clickType, mcClickType) -> {
                    stepPage--;
                    init();
                })
            );
        }
        if (stepPage * 9 + 9 < quest.steps.size()) {
            setSlot(9 * 2 + 7, new GuiElementBuilder(Items.PAPER)
                .setName(Component.literal(">>>>"))
                .setCallback((slot, clickType, mcClickType) -> {
                    stepPage++;
                    init();
                })
            );
        }

        setSlot(9 * 2, new GuiElementBuilder(Items.ARROW)
            .setName(Component.literal("Back"))
            .setCallback((slot, clickType, mcClickType) -> close())
        );

        setSlot(9 * 2 + 2, new GuiElementBuilder(Items.BARRIER)
            .setName(Component.literal("Abort Quest"))
            .setCallback((slot, clickType, mcClickType) -> {
                SimpleGui confirmationGui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
                confirmationGui.setTitle(Component.literal("Abort Quest?"));
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
                            NQuestBot.INSTANCE.questDispatcher.stopQuests(player.getGameProfile().getId());
                            confirmationGui.close();
                        } catch (QuestException e) {
                            player.sendSystemMessage(e.getDisplayRepr(), false);
                        }
                    })
                );
                confirmationGui.open();
            })
        );
    }

    @Override
    public boolean onClick(int index, ClickType type, net.minecraft.world.inventory.ClickType action, GuiElementInterface element) {
        return super.onClick(index, type, action, element);
    }

    @Override
    public void onClose() {
        if (parent != null) parent.open();
    }

    private List<Component> formatDescription(String description) {
        return List.of(description.split("\n")).stream()
            .map(line -> Component.literal(line).withStyle(ChatFormatting.GRAY))
            .collect(Collectors.toList());
    }
}
