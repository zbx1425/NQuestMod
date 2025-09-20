package cn.zbx1425.nquestmod.sgui;

import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.quest.PlayerProfile;
import net.minecraft.server.level.ServerPlayer;

public class GuiStarter {

    public static void openEntry(ServerPlayer player) {
        PlayerProfile profile = NQuestMod.INSTANCE.questDispatcher.playerProfiles.get(player.getGameProfile().getId());
        if (profile != null && !profile.activeQuests.isEmpty()) {
            new CurrentQuestScreen(player, new MainMenuScreen(player)).open();
        } else {
            new MainMenuScreen(player).open();
        }
    }
}
