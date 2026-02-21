package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class Descriptor implements Criterion {

    protected Criterion base;
    public String description;

    public transient Component richDescription;

    public Descriptor(Criterion base, String description) {
        this.base = base;
        this.description = description;
        this.richDescription = Component.literal(description);
    }

    public Descriptor(Criterion base, Component richDescription) {
        this.base = base;
        this.description = richDescription.getString();
        this.richDescription = richDescription;
    }

    @Override
    public boolean evaluate(ServerPlayer player, CriterionContext ctx) {
        return base.evaluate(player, ctx.child("b"));
    }

    @Override
    public Component getDisplayRepr() {
        return richDescription != null ? richDescription : Component.literal(description);
    }

    @Override
    public void propagateManualTrigger(String triggerId, CriterionContext ctx) {
        base.propagateManualTrigger(triggerId, ctx.child("b"));
    }

    @Override
    public Criterion expand() {
        return new Descriptor(base.expand(), description);
    }
}
