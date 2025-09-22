package cn.zbx1425.nquestmod.mixin;

import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.QuestException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.ScoreboardCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(ScoreboardCommand.class)
public class ScoreboardCommandMixin {

    @Inject(method = "setScore", at = @At("HEAD"), cancellable = true)
    private static void setScore(CommandSourceStack commandSourceStack, Collection<ScoreHolder> collection, Objective objective, int i, CallbackInfoReturnable<Integer> cir) {
        if (objective.getName().equals("mtrq_quest_complete")) {
            for (ScoreHolder scoreHolder : collection) {
                ServerPlayer playerOrNull = commandSourceStack.getServer().getPlayerList().getPlayerByName(scoreHolder.getScoreboardName());
                if (playerOrNull == null) continue;
                try {
                    NQuestMod.INSTANCE.questDispatcher.triggerManualCriterion(playerOrNull.getGameProfile().getId(),
                        "legacy_trigger_" + i, playerOrNull);
                } catch (QuestException qe) {
                    playerOrNull.sendSystemMessage(qe.getDisplayRepr(), false);
                }
            }
            cir.setReturnValue(1);
        }
    }
}
