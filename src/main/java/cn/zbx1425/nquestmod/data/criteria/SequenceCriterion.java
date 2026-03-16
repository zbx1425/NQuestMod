package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.stream.Collectors;

public class SequenceCriterion implements Criterion {

    protected List<Criterion> criteria;

    public SequenceCriterion(List<Criterion> criteria) {
        this.criteria = criteria;
    }

    @Override
    public boolean evaluate(ServerPlayer player, CriterionContext ctx) {
        int currentStep = ctx.getInt("step", 0);
        if (currentStep >= criteria.size()) {
            return true;
        }
        if (criteria.get(currentStep).evaluate(player, ctx.child(currentStep))) {
            ctx.setInt("step", currentStep + 1);
            return currentStep + 1 >= criteria.size();
        }
        return false;
    }

    @Override
    public Component getDisplayRepr() {
        boolean first = true;
        MutableComponent objectives = Component.literal("").copy();
        for (Criterion criterion : criteria) {
            if (!first) {
                objectives.append(Component.literal(" then ").withStyle(ChatFormatting.GRAY));
            }
            objectives.append(criterion.getDisplayRepr());
            first = false;
        }
        return objectives;
    }

    @Override
    public void propagateManualTrigger(String triggerId, CriterionContext ctx) {
        for (int i = 0; i < criteria.size(); i++) {
            criteria.get(i).propagateManualTrigger(triggerId, ctx.child(i));
        }
    }

    @Override
    public Criterion expand() {
        return new SequenceCriterion(
            criteria.stream().map(Criterion::expand).collect(Collectors.toList()));
    }
}
