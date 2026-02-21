package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.stream.Collectors;

public class OrCriterion implements Criterion {

    protected List<Criterion> criteria;

    public OrCriterion(List<Criterion> criteria) {
        this.criteria = criteria;
    }

    @Override
    public boolean evaluate(ServerPlayer player, CriterionContext ctx) {
        for (int i = 0; i < criteria.size(); i++) {
            if (criteria.get(i).evaluate(player, ctx.child(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Component getDisplayRepr() {
        boolean first = true;
        MutableComponent objectives = Component.literal("").copy();
        for (Criterion criterion : criteria) {
            if (!first) {
                objectives.append(Component.literal(" or ").withStyle(ChatFormatting.GRAY));
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
        return new OrCriterion(
            criteria.stream().map(Criterion::expand).collect(Collectors.toList()));
    }
}
