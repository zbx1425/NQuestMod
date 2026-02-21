package cn.zbx1425.nquestmod.data.quest;

import cn.zbx1425.nquestmod.data.criteria.Criterion;
import cn.zbx1425.nquestmod.data.criteria.CriterionContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class Step {

    public Criterion criteria;
    public Criterion failureCriteria;

    public Step(Criterion criteria, Criterion failureCriteria) {
        this.criteria = criteria;
        this.failureCriteria = failureCriteria;
    }

    public boolean evaluate(ServerPlayer player, CriterionContext ctx) {
        return criteria != null && criteria.evaluate(player, ctx);
    }

    public Optional<Component> evaluateFailure(
            ServerPlayer player, CriterionContext failCtx,
            Step defaultCriteria, CriterionContext defaultFailCtx) {
        if (failureCriteria != null) {
            if (failureCriteria.evaluate(player, failCtx)) {
                return Optional.of(failureCriteria.getDisplayRepr());
            }
            return Optional.empty();
        }
        if (defaultCriteria != null && defaultCriteria.failureCriteria != null) {
            if (defaultCriteria.failureCriteria.evaluate(player, defaultFailCtx)) {
                return Optional.of(defaultCriteria.failureCriteria.getDisplayRepr());
            }
        }
        return Optional.empty();
    }

    public Component getDisplayRepr() {
        return criteria != null ? criteria.getDisplayRepr() : Component.literal("Impossible Step");
    }

    public void propagateManualTrigger(String triggerId, CriterionContext criteriaCtx, CriterionContext failureCtx) {
        if (criteria != null) criteria.propagateManualTrigger(triggerId, criteriaCtx);
        if (failureCriteria != null) failureCriteria.propagateManualTrigger(triggerId, failureCtx);
    }

    public Step expand() {
        return new Step(
            criteria != null ? criteria.expand() : null,
            failureCriteria != null ? failureCriteria.expand() : null);
    }
}
