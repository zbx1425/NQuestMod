package cn.zbx1425.nquestmod.sgui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.BaseSlotGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class DialogGui extends ParentedGui {

    public boolean shouldJustClose = false;

    public DialogGui(ServerPlayer player, BaseSlotGui parent,
                     Component title,
                     @Nullable GuiElementBuilder centerDisplay,
                     Consumer<DialogGui> onConfirm) {
        super(MenuType.GENERIC_9x3, player, parent);
        setTitle(title);

        if (centerDisplay != null) {
            setSlot(9 + 1, centerDisplay);
        }

        setSlot(9 * 2 + 5, new GuiElementBuilder(Items.RED_CONCRETE)
                .setName(Component.literal("Cancel"))
                .setCallback((index, type, action) -> close())
        );
        setSlot(9 * 2 + 7, new GuiElementBuilder(Items.LIME_CONCRETE)
                .setName(Component.literal("Confirm"))
                .setCallback((index, type, action) -> {
                    onConfirm.accept(this);
                    close();
                })
        );
    }

    @Override
    public void onClose() {
        if (shouldJustClose) return;
        super.onClose();
    }
}
