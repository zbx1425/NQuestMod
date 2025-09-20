package cn.zbx1425.nquestmod.sgui;

import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.QuestException;
import cn.zbx1425.nquestmod.data.quest.PlayerProfile;
import cn.zbx1425.nquestmod.data.quest.Quest;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

public class MainMenuScreen extends SimpleGui {

    public MainMenuScreen(ServerPlayer player) {
        super(MenuType.GENERIC_9x3, player, false);
        setTitle(Component.literal("Nemo's Quest Mod v" +
            FabricLoader.getInstance().getModContainer("nquestmod")
                .get().getMetadata().getVersion().getFriendlyString()));

        PlayerProfile profile = NQuestMod.INSTANCE.questDispatcher.playerProfiles.get(player.getGameProfile().getId());
        if (profile == null) return;

        // Start a Quest
        if (!profile.activeQuests.isEmpty()) {
            // Current Quest
            setSlot(11, new GuiElementBuilder(Items.COMPASS)
                .setName(Component.literal("Current Quest"))
                .addLoreLine(Component.literal("View your active quest progress.").withStyle(ChatFormatting.GRAY))
                .setCallback((index, type, action) -> new CurrentQuestScreen(player, this).open())
            );
        } else {
            setSlot(11, new GuiElementBuilder(Items.WRITABLE_BOOK)
                .setName(Component.literal("Start a Quest"))
                .addLoreLine(Component.literal("View and start available quests.").withStyle(ChatFormatting.GRAY))
                .setCallback((index, type, action) -> {
                    new QuestListScreen(player, this, this::openStartQuestConfirmation).open();
                })
            );
        }

        // Leaderboards
        setSlot(13, new GuiElementBuilder(Items.SPYGLASS)
                .setName(Component.literal("Leaderboards"))
                .addLoreLine(Component.literal("Check out the top players.").withStyle(ChatFormatting.GRAY))
                .setCallback((index, type, action) -> {
                    new LeaderboardScreen(player, this).open();
                })
        );

        // My Profile
        setSlot(15, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setName(Component.literal("My Profile"))
                .addLoreLine(Component.literal("View your stats and quest history.").withStyle(ChatFormatting.GRAY))
                .setSkullOwner(player.getGameProfile(), player.getServer())
                .setCallback((index, type, action) -> {
                    new ProfileScreen(player, this).open();
                })
        );
    }

    
    private void openStartQuestConfirmation(Quest quest) {
        new DialogGui(player, this,
                Component.literal("Start Quest?"),
                new GuiElementBuilder(Items.BOOK)
                        .setName(Component.literal(quest.name)),
                (gui) -> {
                    try {
                        NQuestMod.INSTANCE.questDispatcher.startQuest(player.getGameProfile().getId(), quest.id);
                        gui.shouldJustClose = true;
                    } catch (QuestException e) {
                        player.sendSystemMessage(e.getDisplayRepr(), false);
                    }
                }
        ).open();
    }
}
