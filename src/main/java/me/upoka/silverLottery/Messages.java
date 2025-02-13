package me.upoka.silverLottery;

import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Messages {

    private static FileConfiguration messagesConfig;
    private static File messagesFile;

    public static void setup(JavaPlugin plugin) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        reload();
    }

    public static void reload() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public static FileConfiguration getConfig() {
        return messagesConfig;
    }

    public static String getMessage(String key) {
        if (messagesConfig == null)
            return "§c[Hiba: nincs messages konfiguráció]";
        String msg = messagesConfig.getString(key, "§c[Hiányzó üzenet: " + key + "]");
        String prefix = SilverLottery.getInstance().getConfig().getString("globalPrefix", "&6[Lottery]&r");
        msg = msg.replace("%prefix%", prefix);
        return ColorUtil.translate(msg);
    }

    public static String getMessage(String key, String... placeholders) {
        String msg = getMessage(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        return msg;
    }
}
