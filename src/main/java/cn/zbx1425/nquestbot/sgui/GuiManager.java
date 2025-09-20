package cn.zbx1425.nquestbot.sgui;

import cn.zbx1425.nquestbot.NQuestBot;
import cn.zbx1425.nquestbot.data.quest.PlayerProfile;
import net.minecraft.server.level.ServerPlayer;

public class GuiManager {

    private final NQuestBot nQuestBot;

    public GuiManager(NQuestBot nQuestBot) {
        this.nQuestBot = nQuestBot;
    }

    public void openEntry(ServerPlayer player) {
        PlayerProfile profile = nQuestBot.questDispatcher.playerProfiles.get(player.getGameProfile().getId());
        if (profile != null && !profile.activeQuests.isEmpty()) {
            new CurrentQuestScreen(player, null).open();
        } else {
            new MainMenuScreen(player).open();
        }
    }
}
