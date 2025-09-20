package cn.zbx1425.nquestmod.sgui;

import cn.zbx1425.nquestmod.NQuestMod;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.BaseSlotGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ItemListGui<TItem> extends ParentedGui {

    protected int rowContentStarts;
    protected int rowContentEnds;

    protected int page = 0;

    private final AtomicBoolean isLoading = new AtomicBoolean(false);
    // I haven't pinpointed the real reason behind the syncing issue, so here's a simple cooldown timer
    private CompletableFuture<Void> pageCooldown = CompletableFuture.completedFuture(null);

    public ItemListGui(MenuType<?> type, ServerPlayer player, BaseSlotGui parent) {
        super(type, player, parent);
        rowContentStarts = 0;
        rowContentEnds = height - 2;
    }

    @Override
    public void init() {
        super.init();

        isLoading.set(true);
        int pageSize = (rowContentEnds - rowContentStarts + 1) * 9;
        int offset = page * pageSize;
        CompletableFuture<Pair<List<TItem>, Integer>> pageItemsFuture = supplyItems(offset, pageSize);
        if (!pageItemsFuture.isDone()) {
            for (int slot = rowContentStarts * 9; slot < (rowContentEnds + 1) * 9; slot++) {
                clearSlot(slot);
            }
            setSlot((int)Math.ceil((rowContentStarts + rowContentEnds) / 2.0) * 9 + 4, new GuiElementBuilder(Items.SNOWBALL)
                .setName(Component.literal("Loading...")));
        }
        pageItemsFuture.thenAccept(pageItemsAndSize -> getPlayer().getServer().execute(() -> {
            clearSlot((int)Math.ceil((rowContentStarts + rowContentEnds) / 2.0) * 9 + 4);
            List<TItem> pageItems = pageItemsAndSize.getLeft();
            int totalSize = pageItemsAndSize.getRight();
            for (int slot = rowContentStarts * 9; slot < (rowContentEnds + 1) * 9; slot++) {
                int index = slot - rowContentStarts * 9 + offset;
                if (index < pageItems.size()) {
                    TItem item = pageItems.get(index);
                    setSlot(slot, createElementForItem(item, index));
                } else {
                    clearSlot(slot);
                }
            }
            if (page > 0) {
                setSlot(9 * (rowContentEnds + 1) + 5, new GuiElementBuilder(Items.PAPER)
                    .setName(Component.literal("<<<<"))
                    .setCount(page)
                    .setCallback((index, type, action) -> {
                        if (!pageCooldown.isDone()) return;
                        if (!isLoading.compareAndSet(false, true)) return;
                        pageCooldown = new CompletableFuture<Void>().completeOnTimeout(null, 500, TimeUnit.MILLISECONDS);
                        page--;
                        init();
                    })
                );
            } else {
                setSlot(9 * (rowContentEnds + 1) + 5, FOOTER_FILLER);
            }
            if ((page + 1) * pageSize < totalSize) {
                setSlot(9 * (rowContentEnds + 1) + 7, new GuiElementBuilder(Items.PAPER)
                    .setName(Component.literal(">>>>"))
                    .setCount(page + 2)
                    .setCallback((index, type, action) -> {
                        if (!pageCooldown.isDone()) return;
                        if (!isLoading.compareAndSet(false, true)) return;
                        pageCooldown = new CompletableFuture<Void>().completeOnTimeout(null, 500, TimeUnit.MILLISECONDS);
                        page++;
                        init();
                    })
                );
            } else {
                setSlot(9 * (rowContentEnds + 1) + 7, FOOTER_FILLER);
            }
            if (totalSize > pageSize) {
                Component pageInfo = totalSize == 99999
                    ? Component.literal("Page " + (page + 1))
                    : Component.literal("Page " + (page + 1) + " of " + ((totalSize + pageSize - 1) / pageSize));
                setSlot(9 * (rowContentEnds + 1) + 6, new GuiElementBuilder(Items.CHAIN)
                    .setCount(page + 1)
                    .setName(pageInfo)
                );
            } else {
                setSlot(9 * (rowContentEnds + 1) + 6, FOOTER_FILLER);
            }
            isLoading.set(false);
        })).exceptionally(ex -> {
            NQuestMod.LOGGER.error("Error loading items for ItemListGui", ex);
            for (int slot = rowContentStarts * 9; slot < (rowContentEnds + 1) * 9; slot++) {
                clearSlot(slot);
            }
            setSlot((int)Math.ceil((rowContentStarts + rowContentEnds) / 2.0) * 9 + 4, new GuiElementBuilder(Items.BARRIER)
                .setName(Component.literal("Error loading items"))
                .addLoreLine(Component.literal(ex.getMessage()))
            );
            isLoading.set(false);
            return null;
        });
    }

    protected static final GuiElement HEADER_FILLER = new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).hideFlags().setName(Component.empty()).build();
    protected static final GuiElement FOOTER_FILLER = new GuiElementBuilder(Items.BLACK_STAINED_GLASS_PANE).hideFlags().setName(Component.empty()).build();

    protected void fillHeaderFooter() {
        for (int slot = 0; slot < rowContentStarts * 9; slot++) {
            if (getSlot(slot) == null) setSlot(slot, HEADER_FILLER);
        }
        for (int slot = (rowContentEnds + 1) * 9; slot < height * 9; slot++) {
            if (getSlot(slot) == null) setSlot(slot, FOOTER_FILLER);
        }
    }

    protected abstract CompletableFuture<Pair<List<TItem>, Integer>> supplyItems(int offset, int limit);

    protected abstract GuiElementBuilder createElementForItem(TItem item, int index);
}
