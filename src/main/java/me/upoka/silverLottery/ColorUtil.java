package me.upoka.silverLottery;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("(#|&#)([A-Fa-f0-9]{6})");

    public static String translate(String input) {
        if (input == null) return null;

        String translated = ChatColor.translateAlternateColorCodes('&', input);

        Matcher matcher = HEX_COLOR_PATTERN.matcher(translated);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(2);
            StringBuilder hexBuilder = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                hexBuilder.append('§').append(c);
            }
            matcher.appendReplacement(buffer, hexBuilder.toString());
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }
}
