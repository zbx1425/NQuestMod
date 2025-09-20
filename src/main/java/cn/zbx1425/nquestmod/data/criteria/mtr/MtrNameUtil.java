package cn.zbx1425.nquestmod.data.criteria.mtr;

import cn.zbx1425.nquestmod.interop.TscStatus;
import org.mtr.core.tool.Utilities;

import java.util.Locale;
import java.util.regex.Pattern;

public class MtrNameUtil {

    public static boolean matches(String criteria, TscStatus.NameIdData target) {
        if (target.name().equalsIgnoreCase(Utilities.numberToPaddedHexString(target.id()))) return true;
        String targetNameEnglishOnly = processLocalizedName(target.name());
        return targetNameEnglishOnly.toLowerCase(Locale.ROOT).startsWith(criteria.toLowerCase(Locale.ROOT));
    }

    private static final Pattern CJK_PATTERN = Pattern.compile("[\\u4E00-\\u9FFF\\u3040-\\u30FF\\uAC00-\\uD7AF]");

    private static String processLocalizedName(String input) {
        String[] parts = input.split("\\|\\|", 2);
        String mainPart = parts[0];
        String[] segments = mainPart.split("\\|");
        StringBuilder cleaned = new StringBuilder();
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (!trimmed.isEmpty() && !CJK_PATTERN.matcher(trimmed).find()) {
                if (!cleaned.isEmpty()) {
                    cleaned.append(" ");
                }
                cleaned.append(trimmed);
            }
        }
        return cleaned.toString();
    }
}
