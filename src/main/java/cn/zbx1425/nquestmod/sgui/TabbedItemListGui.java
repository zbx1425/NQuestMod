package cn.zbx1425.nquestmod.sgui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.BaseSlotGui;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public abstract class TabbedItemListGui<TItem, TPrimaryTab, TSecondaryTab> extends ItemListGui<TItem> {

    protected TPrimaryTab selectedPrimaryTab;
    protected final List<Pair<TPrimaryTab, Supplier<GuiElementBuilder>>> primaryTabs;

    @Nullable
    protected TSecondaryTab selectedSecondaryTab;
    @Nullable
    protected final List<Pair<TSecondaryTab, Supplier<GuiElementBuilder>>> secondaryTabs;

    public TabbedItemListGui(MenuType<?> type, ServerPlayer player, BaseSlotGui parent,
                             TPrimaryTab defaultPrimaryTab, List<Pair<TPrimaryTab, Supplier<GuiElementBuilder>>> primaryTabs,
                             @Nullable TSecondaryTab defaultSecondaryTab, @Nullable List<Pair<TSecondaryTab, Supplier<GuiElementBuilder>>> secondaryTabs) {
        super(type, player, parent);
        this.selectedPrimaryTab = defaultPrimaryTab;
        this.primaryTabs = primaryTabs;
        this.selectedSecondaryTab = defaultSecondaryTab;
        this.secondaryTabs = secondaryTabs;
        this.rowContentStarts = 1;
    }

    @Override
    public void init() {
        super.init();

        // Primary Tabs (from left)
        for (int i = 0; i < primaryTabs.size(); i++) {
            Pair<TPrimaryTab, Supplier<GuiElementBuilder>> tab = primaryTabs.get(i);
            TPrimaryTab tabKey = tab.getKey();
            GuiElementBuilder element = tab.getValue().get();
            if (tabKey.equals(selectedPrimaryTab)) {
                element.glow();
                element.setCount(64);
            }
            element.setCallback((index, type, action) -> {
                if (!tabKey.equals(selectedPrimaryTab)) {
                    selectedPrimaryTab = tabKey;
                    this.page = 0;
                    init();
                }
            });
            setSlot(i, element);
        }

        // Secondary Tabs (from right)
        if (secondaryTabs != null) {
            for (int i = 0; i < secondaryTabs.size(); i++) {
                Pair<TSecondaryTab, Supplier<GuiElementBuilder>> tab = secondaryTabs.get(i);
                TSecondaryTab tabKey = tab.getKey();
                GuiElementBuilder element = tab.getValue().get();
                if (tabKey.equals(selectedSecondaryTab)) {
                    element.glow();
                    element.setCount(64);
                }
                element.setCallback((index, type, action) -> {
                    if (!tabKey.equals(selectedSecondaryTab)) {
                        selectedSecondaryTab = tabKey;
                        this.page = 0;
                        init();
                    }
                });
                setSlot(8 - i, element);
            }
        }
    }
}
