package cn.zbx1425.nquestmod.sgui;

import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.quest.Quest;
import cn.zbx1425.nquestmod.data.quest.QuestCategory;
import cn.zbx1425.nquestmod.data.quest.QuestTier;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.BaseSlotGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class QuestListScreen extends TabbedItemListGui<Quest, Pair<String, QuestCategory>, Void> {

    private final BiConsumer<Quest, QuestListScreen> callback;

    public QuestListScreen(ServerPlayer player, BaseSlotGui parent, BiConsumer<Quest, QuestListScreen> callback) {
        super(MenuType.GENERIC_9x4, player, parent,
                NQuestMod.INSTANCE.questCategories.entrySet().stream()
                    .min(Comparator.comparingInt(c -> c.getValue().order))
                    .map(Pair::of)
                    .orElse(null),
                NQuestMod.INSTANCE.questCategories.entrySet().stream()
                    .sorted(Comparator.comparingInt(c -> c.getValue().order))
                    .map(entry -> Pair.<Pair<String, QuestCategory>, Supplier<GuiElementBuilder>>of(
                        Pair.of(entry),
                        () -> new GuiElementBuilder(
                            BuiltInRegistries.ITEM.getOptional(new ResourceLocation(entry.getValue().icon)).orElse(Items.STONE))
                            .setName(Component.literal(entry.getValue().name))
                            .setLore(entry.getValue().formatDescription())
                    ))
                    .toList(),
                null, null
        );
        this.callback = callback;
        setTitle(Component.literal("Select a Quest"));
        init();
    }

    @Override
    public void init() {
        super.init();
        fillHeaderFooter();
    }

    @Override
    protected CompletableFuture<Pair<List<Quest>, Integer>> supplyItems(int offset, int limit) {
        if (selectedPrimaryTab == null) {
            return CompletableFuture.completedFuture(Pair.of(List.of(), 0));
        }
        List<Quest> filteredQuests = NQuestMod.INSTANCE.questDispatcher.quests.values().stream()
                .filter(q -> selectedPrimaryTab.getKey().equals(q.category))
                .sorted(Comparator.<Quest>comparingInt(q -> {
                    QuestCategory cat = NQuestMod.INSTANCE.questCategories.get(q.category);
                    if (cat == null || cat.tiers == null) return Integer.MAX_VALUE;
                    QuestTier tier = cat.tiers.get(q.tier);
                    return tier != null ? tier.order : Integer.MAX_VALUE;
                }).thenComparing(q -> q.id))
                .toList();

        return CompletableFuture.completedFuture(Pair.of(
                filteredQuests.stream().skip(offset).limit(limit).collect(Collectors.toList()),
                filteredQuests.size()
        ));
    }

    @Override
    protected GuiElementBuilder createElementForItem(Quest item, int index) {
        QuestCategory cat = NQuestMod.INSTANCE.questCategories.get(item.category);
        QuestTier tier = null;
        if (cat != null && cat.tiers != null) {
            tier = cat.tiers.get(item.tier);
        }

        GuiElementBuilder builder = new GuiElementBuilder(Items.BOOK);
        if (tier != null) {
            builder.setItem(BuiltInRegistries.ITEM.getOptional(new ResourceLocation(tier.icon)).orElse(Items.STONE));
            builder.addLoreLine(Component.literal("Tier: " + tier.name).withStyle(ChatFormatting.YELLOW));
        }
        for (Component loreLine : item.formatDescription()) {
            builder.addLoreLine(loreLine);
        }
        return builder.setName(Component.literal(item.name))
                .setCallback((i, t, a) -> this.callback.accept(item, this));
    }
}
