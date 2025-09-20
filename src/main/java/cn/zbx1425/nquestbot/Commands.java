package cn.zbx1425.nquestbot;

import cn.zbx1425.nquestbot.data.QuestException;
import cn.zbx1425.nquestbot.data.QuestPersistence;
import cn.zbx1425.nquestbot.data.quest.Quest;
import cn.zbx1425.nquestbot.sgui.CurrentQuestScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiFunction;
import java.util.function.Function;

public class Commands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                Function<String, LiteralArgumentBuilder<CommandSourceStack>> literal,
                                BiFunction<String, ArgumentType<?>, RequiredArgumentBuilder<CommandSourceStack, ?>> argument) {
        dispatcher.register(literal.apply("nquest")
            .executes(ctx -> {
                NQuestBot.INSTANCE.guiManager.openEntry(ctx.getSource().getPlayerOrException());
                return 1;
            })
            .then(literal.apply("start")
                .then(argument.apply("participant", EntityArgument.player())
                    .requires(source -> source.hasPermission(2))
                    .then(argument.apply("quest_id", StringArgumentType.string())
                        .executes(ctx -> {
                            startQuest(EntityArgument.getPlayer(ctx, "participant"), StringArgumentType.getString(ctx, "quest_id"));
                            return 1;
                        })
                    )
                )
            )
            .then(literal.apply("stop")
                .then(argument.apply("participant", EntityArgument.player())
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        stopQuests(EntityArgument.getPlayer(ctx, "participant"));
                        return 1;
                    })
                )
                .executes(ctx -> {
                    stopQuests(ctx.getSource().getPlayerOrException());
                    return 1;
                })
            )
            .then(literal.apply("trigger")
                .requires(source -> source.hasPermission(2))
                .then(argument.apply("participant", EntityArgument.player())
                    .requires(source -> source.hasPermission(2))
                    .then(argument.apply("trigger_id", StringArgumentType.string())
                        .executes(ctx -> {
                            triggerManualCriterion(EntityArgument.getPlayer(ctx, "participant"), StringArgumentType.getString(ctx, "trigger_id"));
                            return 1;
                        })
                    )
                )
            )
            .then(literal.apply("quests")
                .requires(source -> source.hasPermission(2))
                .then(literal.apply("set")
                    .then(argument.apply("json", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            Quest quest = setQuestDefinition(StringArgumentType.getString(ctx, "json"));
                            Component message = Component.literal("Quest definition accepted with ID: ")
                                .append(Component.literal(quest.id).withStyle(net.minecraft.ChatFormatting.AQUA));
                            ctx.getSource().sendSuccess(() -> message, false);
                            return 1;
                        })
                    )
                )
                .then(literal.apply("get")
                    .then(argument.apply("quest_id", StringArgumentType.string())
                        .executes(ctx -> {
                            String questJson = getQuestDefinition(StringArgumentType.getString(ctx, "quest_id"));
                            Component message = Component.literal("Quest JSON Data: ")
                                .append(Component.literal("[Click to copy]").withStyle(style ->
                                    style.withUnderlined(true)
                                        .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                            net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, questJson))
                                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                            net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                            Component.literal("Click to copy to clipboard")))
                                        .withColor(net.minecraft.ChatFormatting.AQUA)
                                ));
                            ctx.getSource().sendSuccess(() -> message, false);
                            return 1;
                        })
                    )
                )
            )
        );
    }

    private static void startQuest(ServerPlayer participant, String questId) throws CommandSyntaxException {
        try {
            NQuestBot.INSTANCE.questDispatcher.startQuest(participant.getGameProfile().getId(), questId);
        } catch (QuestException ex) {
            throw ex.createMinecraftException();
        }
    }

    private static void stopQuests(ServerPlayer participant) throws CommandSyntaxException {
        try {
            NQuestBot.INSTANCE.questDispatcher.stopQuests(participant.getGameProfile().getId());
        } catch (QuestException ex) {
            throw ex.createMinecraftException();
        }
    }

    private static void triggerManualCriterion(ServerPlayer participant, String triggerId) throws CommandSyntaxException {
        try {
            NQuestBot.INSTANCE.questDispatcher.triggerManualCriterion(participant.getGameProfile().getId(), triggerId, participant);
        } catch (QuestException ex) {
            throw ex.createMinecraftException();
        }
    }

    private static String getQuestDefinition(String questId) throws CommandSyntaxException {
        Quest quest = NQuestBot.INSTANCE.questDispatcher.quests.get(questId);
        if (quest == null) throw new QuestException(QuestException.Type.QUEST_NOT_FOUND).createMinecraftException();
        return QuestPersistence.serializeQuest(quest);
    }

    private static Quest setQuestDefinition(String questJson) throws CommandSyntaxException {
        try {
            Quest quest = QuestPersistence.deserializeQuest(questJson);
            NQuestBot.INSTANCE.questDispatcher.quests.put(quest.id, quest);
            NQuestBot.INSTANCE.questStorage.saveQuestDefinition(quest);
            return quest;
        } catch (Exception ex) {
            throw new SimpleCommandExceptionType(Component.literal("Failed to parse or save quest definition: " + ex)).create();
        }
    }
}
