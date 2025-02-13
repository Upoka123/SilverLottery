package me.upoka.silverLottery;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;

public class SilverLottery extends JavaPlugin {

    private static SilverLottery instance;
    private Economy econ;
    private SilverLotteryManager lotteryManager;
    private DatabaseManager dbManager;

    public static SilverLottery getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadMessages();

        if (!setupEconomy()) {
            getLogger().severe("Vault economy nem található! A plugin leáll.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        dbManager = new DatabaseManager(this);
        dbManager.setupDatabase();

        lotteryManager = new SilverLotteryManager(this, dbManager, econ);

        SilverLotteryCommand lotteryCommand = new SilverLotteryCommand(lotteryManager);
        getCommand("lottery").setExecutor(lotteryCommand);
        getCommand("lottery").setTabCompleter(new SilverLotteryTabCompleter());

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SilverLotteryPlaceholderExpansion(this).register();
        }

        long announcementIntervalSec = getConfig().getLong("announcementIntervalSeconds", 300L);
        long announcementIntervalTicks = announcementIntervalSec * 20;
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            String msg = Messages.getMessage("lottery.announcement");
            msg = msg.replace("%current_prize%", String.valueOf(lotteryManager.getCurrentPrize()));
            msg = msg.replace("%next_drawing%", lotteryManager.getNextDrawing());
            Bukkit.broadcastMessage(msg);
        }, announcementIntervalTicks, announcementIntervalTicks);

        getLogger().info("SilverLottery plugin successfully started!");
    }

    @Override
    public void onDisable() {
        dbManager.close();
        getLogger().info("SilverLottery plugin kikapcsolva!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null)
            return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null)
            return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        Messages.setup(this);
    }

    public FileConfiguration getMessagesConfig() {
        return Messages.getConfig();
    }

    public SilverLotteryManager getLotteryManager() {
        return lotteryManager;
    }
}
