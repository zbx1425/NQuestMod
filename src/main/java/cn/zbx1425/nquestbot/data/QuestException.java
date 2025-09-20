package cn.zbx1425.nquestbot.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public class QuestException extends Exception {

    public final Type type;

    public QuestException(Type type) {
        super(type.readableName);
        this.type = type;
    }

    public CommandSyntaxException createMinecraftException() {
        return type.commandExceptionType.create();
    }

    public Component getDisplayRepr() {
//        return Component.translatable(type.translationKey);
        return Component.literal(type.readableName);
    }

    public enum Type {
        PLAYER_NOT_FOUND,
        QUEST_NOT_FOUND,
        QUEST_ALREADY_STARTED,
        QUEST_NOT_STARTED,
        QUEST_ONLY_ONE_AT_A_TIME;

        public final String translationKey = "nquestbot.quest_exception." + this.name().toLowerCase();
        public final String readableName = StringUtils.capitalize(this.name().toLowerCase(Locale.ROOT).replace('_', ' '));
        public final SimpleCommandExceptionType commandExceptionType = new SimpleCommandExceptionType(() -> readableName);
    }
}
