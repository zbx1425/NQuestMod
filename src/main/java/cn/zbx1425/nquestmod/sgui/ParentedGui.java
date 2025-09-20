package cn.zbx1425.nquestmod.sgui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.BaseSlotGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

public abstract class ParentedGui extends SimpleGui {

    protected final BaseSlotGui parent;

    public ParentedGui(MenuType<?> type, ServerPlayer player, BaseSlotGui parent) {
        super(type, player, false);
        this.parent = parent;
    }

    public void init() {
        // Back button
        setSlot((height - 1) * 9, new GuiElementBuilder(Items.ARROW)
            .setName(Component.literal("Back"))
            .setCallback((index, type, action) -> close())
        );
    }

    @Override
    public void onClose() {
        if (parent != null) {
            parent.open();
        } else {
            super.onClose();
        }
    }
}
