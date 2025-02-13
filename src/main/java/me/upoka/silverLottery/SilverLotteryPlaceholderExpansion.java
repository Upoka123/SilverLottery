package me.upoka.silverLottery;

import org.bukkit.entity.Player;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class SilverLotteryPlaceholderExpansion extends PlaceholderExpansion {

    private SilverLottery plugin;

    public SilverLotteryPlaceholderExpansion(SilverLottery plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "lottery";
    }

    @Override
    public String getAuthor() {
        return "Upoka";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        SilverLotteryManager lm = plugin.getLotteryManager();
        if (lm == null || identifier == null) {
            return "";
        }
        switch (identifier.toLowerCase()) {
            case "last_winner":
                // Ha nincs előző nyertes, visszatérhetünk például "N/A"-val
                String lastWinner = lm.getLastWinner();
                return (lastWinner != null && !lastWinner.isEmpty()) ? lastWinner : "N/A";
            case "current_prize":
                return String.valueOf(lm.getCurrentPrize());
            case "next_drawing":
                return lm.getNextDrawing();
            case "your_tickets":
                return (player != null) ? String.valueOf(lm.getTicketsOfPlayer(player.getUniqueId())) : "0";
            case "total_tickets":
                return String.valueOf(lm.getTotalTickets());
            default:
                return "";
        }
    }
}
