package cn.zbx1425.nquestmod.sgui;

import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.QuestException;
import cn.zbx1425.nquestmod.data.quest.PlayerProfile;
import cn.zbx1425.nquestmod.data.quest.Quest;
import cn.zbx1425.nquestmod.data.quest.QuestProgress;
import cn.zbx1425.nquestmod.data.quest.Step;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.BaseSlotGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CurrentQuestScreen extends ItemListGui<Step> {

    private final Quest quest;
    private final QuestProgress progress;

    public CurrentQuestScreen(ServerPlayer player, BaseSlotGui parent) {
        super(MenuType.GENERIC_9x3, player, parent);

        PlayerProfile profile = NQuestMod.INSTANCE.questDispatcher.playerProfiles.get(player.getGameProfile().getId());
        progress = profile != null ? profile.activeQuests.values().stream().findFirst().orElse(null) : null;
        quest = progress != null ? NQuestMod.INSTANCE.questDispatcher.quests.get(progress.questId) : null;

        if (quest != null) {
            setTitle(Component.literal(quest.name));
            init();
        }
    }

    @Override
    public void init() {
        super.init();

        if (quest == null) return;
        setSlot(9 * 2 + 2, new GuiElementBuilder(Items.BARRIER)
                .setName(Component.literal("Abort Quest"))
                .setCallback((slot, clickType, mcClickType) -> {
                    new DialogGui(player, this,
                            Component.literal("Abort Quest?"),
                            new GuiElementBuilder(Items.BOOK)
                                    .setName(Component.literal(quest.name))
                                    .setLore(formatDescription(quest.description)),
                            (gui) -> {
                                try {
                                    NQuestMod.INSTANCE.questDispatcher.stopQuests(player.getGameProfile().getId());
                                    gui.shouldJustClose = true;
                                } catch (QuestException e) {
                                    player.sendSystemMessage(e.getDisplayRepr(), false);
                                }
                            }
                    ).open();
                })
        );

        fillHeaderFooter();
    }

    @Override
    protected CompletableFuture<Pair<List<Step>, Integer>> supplyItems(int offset, int limit) {
        if (quest == null) {
            return CompletableFuture.completedFuture(Pair.of(List.of(), 0));
        }
        List<Step> steps = quest.steps;
        return CompletableFuture.completedFuture(Pair.of(
                steps.stream().skip(offset).limit(limit).collect(Collectors.toList()),
                steps.size()
        ));
    }

    @Override
    protected GuiElementBuilder createElementForItem(Step item, int index) {
        Item icon;
        if (progress.currentStepIndex > index) {
            icon = Items.GREEN_TERRACOTTA;
        } else if (progress.currentStepIndex == index) {
            icon = Items.YELLOW_CONCRETE;
        } else {
            icon = Items.GRAY_CONCRETE;
        }
        return new GuiElementBuilder(icon)
                .setName(item.criteria.getDisplayRepr())
                .setCount(index + 1);
    }

    private List<Component> formatDescription(String description) {
        return List.of(description.split("\n")).stream()
                .map(line -> Component.literal(line).withStyle(ChatFormatting.GRAY))
                .collect(Collectors.toList());
    }
}
