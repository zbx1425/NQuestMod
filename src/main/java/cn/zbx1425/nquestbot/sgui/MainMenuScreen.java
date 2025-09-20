package cn.zbx1425.nquestbot.sgui;

import cn.zbx1425.nquestbot.NQuestBot;
import cn.zbx1425.nquestbot.data.quest.PlayerProfile;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.util.List;

public class MainMenuScreen extends SimpleGui {

    public MainMenuScreen(ServerPlayer player) {
        super(MenuType.GENERIC_9x3, player, false);
        setTitle(Component.literal("NQuestBot Main Menu"));

        PlayerProfile profile = NQuestBot.INSTANCE.questDispatcher.playerProfiles.get(player.getGameProfile().getId());
        if (profile == null) return;

        // Start a Quest
        setSlot(11, new GuiElementBuilder(Items.WRITABLE_BOOK)
                .setName(Component.literal("Start a Quest"))
                .setLore(List.of(Component.literal("View and start available quests.")))
                .setCallback((index, type, action) -> {
                    new QuestListScreen(player, this, 0).open();
                })
        );

        // Leaderboards
        setSlot(13, new GuiElementBuilder(Items.SPYGLASS)
                .setName(Component.literal("Leaderboards"))
                .setLore(List.of(Component.literal("Check out the top players.")))
                .setCallback((index, type, action) -> {
                    new LeaderboardScreen(player, this).open();
                })
        );

        // My Profile
        setSlot(15, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setName(Component.literal("My Profile"))
                .setLore(List.of(Component.literal("View your stats and quest history.")))
                .setSkullOwner(player.getGameProfile(), player.getServer())
                .setCallback((index, type, action) -> {
                    new ProfileScreen(player, this).open();
                })
        );

        if (!profile.activeQuests.isEmpty()) {
            // Current Quest
            setSlot(4, new GuiElementBuilder(Items.COMPASS)
                    .setName(Component.literal("Current Quest"))
                    .setLore(List.of(Component.literal("View your active quest progress.")))
                    .setCallback((index, type, action) -> new CurrentQuestScreen(player, this).open())
            );
        }
    }
}
