package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class OrCriterion implements Criterion {

    protected List<Criterion> criteria;

    public OrCriterion(List<Criterion> criteria) {
        this.criteria = criteria;
    }

    public OrCriterion(OrCriterion singleton) {
        this.criteria = singleton.criteria.stream().map(Criterion::createStatefulInstance).toList();
    }

    @Override
    public boolean isFulfilled(ServerPlayer player) {
        for (Criterion criterion : criteria) {
            if (criterion.isFulfilled(player)) {
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
    public OrCriterion createStatefulInstance() {
        return new OrCriterion(this);
    }

    @Override
    public void propagateManualTrigger(String triggerId) {
        for (Criterion criterion : criteria) {
            criterion.propagateManualTrigger(triggerId);
        }
    }
}
