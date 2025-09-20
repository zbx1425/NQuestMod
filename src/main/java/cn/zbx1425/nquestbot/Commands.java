package cn.zbx1425.nquestbot;

import cn.zbx1425.nquestbot.data.QuestException;
import cn.zbx1425.nquestbot.data.QuestPersistence;
import cn.zbx1425.nquestbot.data.quest.Quest;
import cn.zbx1425.nquestbot.data.quest.QuestCategory;
import cn.zbx1425.nquestbot.sgui.GuiStarter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Commands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                Function<String, LiteralArgumentBuilder<CommandSourceStack>> literal,
                                BiFunction<String, ArgumentType<?>, RequiredArgumentBuilder<CommandSourceStack, ?>> argument) {
        dispatcher.register(literal.apply("nquest")
            .executes(ctx -> {
                GuiStarter.openEntry(ctx.getSource().getPlayerOrException());
                return 1;
            })
            .then(literal.apply("start")
                .requires(source -> source.hasPermission(2))
                .then(argument.apply("participant", EntityArgument.player())
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
                    .then(argument.apply("sign", StringArgumentType.word())
                    .then(argument.apply("json", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            validateTimestampSignature(ctx);
                            Quest quest = setQuestDefinition(StringArgumentType.getString(ctx, "json"));
                            Component message = Component.literal("Quest definition accepted with ID: ")
                                .append(Component.literal(quest.id).withStyle(net.minecraft.ChatFormatting.AQUA));
                            ctx.getSource().sendSuccess(() -> message, false);
                            return 1;
                        })
                    ))
                )
                .then(literal.apply("get")
                    .requires(source -> source.hasPermission(3))
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
                .then(literal.apply("remove")
                    .requires(source -> source.hasPermission(3))
                    .then(argument.apply("quest_id", StringArgumentType.string())
                        .executes(ctx -> {
                            String questId = StringArgumentType.getString(ctx, "quest_id");
                            removeQuestDefinition(questId);
                            Component message = Component.literal("Quest definition removed: ")
                                .append(Component.literal(questId).withStyle(net.minecraft.ChatFormatting.AQUA));
                            ctx.getSource().sendSuccess(() -> message, false);
                            return 1;
                        })
                    )
                )
            )
            .then(literal.apply("categories")
                .requires(source -> source.hasPermission(2))
                .then(literal.apply("set")
                    .then(argument.apply("sign", StringArgumentType.word())
                    .then(argument.apply("json", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            validateTimestampSignature(ctx);
                            try {
                                Map<String, QuestCategory> categories = QuestPersistence.deserializeCategories(StringArgumentType.getString(ctx, "json"));
                                NQuestBot.INSTANCE.questCategories.clear();
                                NQuestBot.INSTANCE.questCategories.putAll(categories);
                                NQuestBot.INSTANCE.questStorage.saveQuestCategories(NQuestBot.INSTANCE.questCategories);
                            } catch (Exception ex) {
                                NQuestBot.LOGGER.error("Failed to parse or save categories", ex);
                                throw new SimpleCommandExceptionType(Component.literal("Failed to parse or save categories: " + ex)).create();
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("Quest categories updated."), false);
                            return 1;
                        })
                    ))
                )
                .then(literal.apply("get")
                    .requires(source -> source.hasPermission(3))
                    .executes(ctx -> {
                        String categoriesJson = QuestPersistence.serializeCategories(NQuestBot.INSTANCE.questCategories);
                        Component message = Component.literal("Quest Categories JSON Data: ")
                            .append(Component.literal("[Click to copy]").withStyle(style ->
                                style.withUnderlined(true)
                                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                        net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, categoriesJson))
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
            .then(literal.apply("sign")
                .requires(source -> source.hasPermission(3))
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    if (NQuestBot.INSTANCE.commandSigner == null) {
                        throw new SimpleCommandExceptionType(Component.literal("Command signing is not set up.")).create();
                    }
                    String signedTimestamp = NQuestBot.INSTANCE.commandSigner.signTimestamp();
                    Component message = Component.literal("Timestamp signature (valid in 5 mins): ")
                        .append(Component.literal(signedTimestamp).withStyle(style ->
                            style.withUnderlined(true)
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                    net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, signedTimestamp))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                    net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Click to copy to clipboard")))
                                .withColor(net.minecraft.ChatFormatting.AQUA)
                        ));
                    source.sendSuccess(() -> message, false);
                    return 1;
                })
            )
        );
    }

    private static void validateTimestampSignature(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (NQuestBot.INSTANCE.commandSigner == null) {
            throw new SimpleCommandExceptionType(Component.literal("Command signing is not set up.")).create();
        }
        String providedSignature = StringArgumentType.getString(ctx, "sign");
        if (!NQuestBot.INSTANCE.commandSigner.verifySignedTimestamp(providedSignature, 5 * 60 * 1000)) {
            throw new SimpleCommandExceptionType(Component.literal("Invalid or expired signature.")).create();
        }
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

    private static void removeQuestDefinition(String questId) throws CommandSyntaxException {
        if (NQuestBot.INSTANCE.questDispatcher.quests.remove(questId) == null) {
            throw new QuestException(QuestException.Type.QUEST_NOT_FOUND).createMinecraftException();
        }
        try {
            NQuestBot.INSTANCE.questStorage.removeQuestDefinition(questId);
        } catch (Exception ex) {
            throw new SimpleCommandExceptionType(Component.literal("Failed to delete quest definition: " + ex)).create();
        }
    }
}
