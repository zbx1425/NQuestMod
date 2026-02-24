package cn.zbx1425.nquestmod.sgui;

import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.quest.PlayerProfile;
import cn.zbx1425.nquestmod.data.ranking.RankingApiClient;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.BaseSlotGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

public class ProfileScreen extends ParentedGui {

    private static final long STATS_CACHE_MAX_AGE_MS = 30_000;

    public ProfileScreen(ServerPlayer player, BaseSlotGui parent) {
        super(MenuType.GENERIC_9x3, player, parent);
        setTitle(Component.literal("My Profile"));
        init();
    }

    @Override
    public void init() {
        super.init();

        PlayerProfile profile = NQuestMod.INSTANCE.questDispatcher.playerProfiles.get(player.getGameProfile().getId());
        if (profile == null) return;

        renderProfile(profile);

        RankingApiClient api = NQuestMod.INSTANCE.rankingApi;
        if (api != null && api.isEnabled() && profile.isStatsCacheStale(STATS_CACHE_MAX_AGE_MS)) {
            api.getPlayerProfile(player.getGameProfile().getId()).whenComplete((stats, error) -> {
                if (error != null) {
                    NQuestMod.LOGGER.warn("Failed to refresh player stats for {}", player.getGameProfile().getName(), error);
                    return;
                }
                profile.qpBalance = stats.qpBalance;
                profile.totalQuestCompletions = stats.totalQuestCompletions;
                profile.lastStatsSyncTime = System.currentTimeMillis();
                player.getServer().execute(() -> {
                    if (isOpen()) renderProfile(profile);
                });
            });
        }
    }

    private void renderProfile(PlayerProfile profile) {
        setSlot(4, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(player.getGameProfile(), player.getServer())
                .setName(Component.literal(player.getGameProfile().getName()))
        );

        setSlot(11, new GuiElementBuilder(Items.EXPERIENCE_BOTTLE)
                .setName(Component.literal("Quest Points"))
                .addLoreLine(Component.literal(String.valueOf(profile.qpBalance)).withStyle(ChatFormatting.GREEN))
        );

        setSlot(13, new GuiElementBuilder(Items.EMERALD)
                .setName(Component.literal("Total Quests Completed"))
                .addLoreLine(Component.literal(String.valueOf(profile.totalQuestCompletions)).withStyle(ChatFormatting.GREEN))
        );

        setSlot(15, new GuiElementBuilder(Items.CLOCK)
                .setName(Component.literal("Quest History"))
                .addLoreLine(Component.literal("View your past quest completions.").withStyle(ChatFormatting.GRAY))
                .setCallback((index, type, action) -> {
                    new QuestHistoryScreen(player, this).open();
                })
        );
    }
}
