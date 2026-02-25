package cn.zbx1425.nquestmod.sgui;

import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.quest.Quest;
import cn.zbx1425.nquestmod.data.quest.QuestCompletionData;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.BaseSlotGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class QuestCompletionDetailScreen extends ItemListGui<Map.Entry<Integer, QuestCompletionData.StepDetail>> {

    private final QuestCompletionData completionData;
    private final Quest quest;

    public QuestCompletionDetailScreen(ServerPlayer player, BaseSlotGui parent, QuestCompletionData completionData) {
        super(MenuType.GENERIC_9x4, player, parent);
        this.completionData = completionData;
        this.quest = NQuestMod.INSTANCE.questDispatcher.quests.get(completionData.questId);
        setTitle(Component.literal("Step Details"));
        init();
        rowContentStarts = 1;
    }

    @Override
    public void init() {
        super.init();

        setSlot(0, new GuiElementBuilder(Items.BOOK)
                .setName(Component.literal(quest != null ? quest.name : completionData.questId))
                .setLore(quest != null ? quest.formatDescription() : List.of())
        );
        setSlot(1, buildPlayerEntry(player.getServer(), completionData.playerUuid,
                name -> Component.literal("Completed by: " + name))
        );
        setSlot(2, new GuiElementBuilder(Items.EMERALD)
                .setName(Component.literal("QPs Awarded: " + completionData.questPoints))
        );

        setSlot(8, new GuiElementBuilder(Items.CLOCK)
                .setName(Component.literal("Total Time: " + String.format("%.2f s", completionData.durationMillis / 1000.0)))
        );

        fillHeaderFooter();
    }

    @Override
    protected CompletableFuture<Pair<List<Map.Entry<Integer, QuestCompletionData.StepDetail>>, Integer>> supplyItems(int offset, int limit) {
        if (completionData.stepDetails == null || completionData.stepDetails.isEmpty()) {
            return CompletableFuture.completedFuture(Pair.of(List.of(), 0));
        }
        List<Map.Entry<Integer, QuestCompletionData.StepDetail>> entries =
                new ArrayList<>(completionData.stepDetails.entrySet());
        entries.sort(Map.Entry.comparingByKey());

        return CompletableFuture.completedFuture(Pair.of(
                entries.stream().skip(offset).limit(limit).toList(),
                entries.size()
        ));
    }

    @Override
    protected GuiElementBuilder createElementForItem(Map.Entry<Integer, QuestCompletionData.StepDetail> item, int index) {
        int stepIndex = item.getKey();
        QuestCompletionData.StepDetail detail = item.getValue();

        Component stepName = detail.description != null
                ? Component.literal(detail.description)
                : Component.literal("Step " + (stepIndex + 1));
        String timeStr = String.format("%.2f s", detail.durationMillis / 1000.0);

        GuiElementBuilder builder = new GuiElementBuilder(Items.CLOCK)
                .setName(stepName)
                .addLoreLine(Component.literal("Duration: " + timeStr).withStyle(ChatFormatting.GOLD))
                .setCount(stepIndex + 1);

        if (detail.linesRidden != null && !detail.linesRidden.isEmpty()) {
            builder.addLoreLine(Component.literal("Lines: " + String.join(", ", detail.linesRidden))
                    .withStyle(ChatFormatting.AQUA));
        }

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
