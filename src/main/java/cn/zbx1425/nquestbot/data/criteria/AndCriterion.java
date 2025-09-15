package cn.zbx1425.nquestbot.data.criteria;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class AndCriterion implements Criterion {

    protected List<Criterion> criteria;

    public AndCriterion(List<Criterion> criteria) {
        this.criteria = criteria;
    }

    public AndCriterion(AndCriterion singleton) {
        this.criteria = singleton.criteria.stream().map(Criterion::createStatefulInstance).toList();
    }

    @Override
    public boolean isFulfilled(ServerPlayer player) {
        for (Criterion criterion : criteria) {
            if (!criterion.isFulfilled(player)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Component getDisplayRepr() {
        boolean first = true;
        MutableComponent objectives = Component.literal("").copy();
        for (Criterion criterion : criteria) {
            if (!first) {
                objectives.append(Component.literal(" and ").withStyle(ChatFormatting.GRAY));
            }
            objectives.append(criterion.getDisplayRepr());
            first = false;
        }
        return objectives;
    }

    @Override
    public AndCriterion createStatefulInstance() {
        return new AndCriterion(this);
    }

    @Override
    public void propagateManualTrigger(String triggerId) {
        for (Criterion criterion : criteria) {
            criterion.propagateManualTrigger(triggerId);
        }
    }
}
