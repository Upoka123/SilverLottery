package me.upoka.silverLottery;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import net.milkbowl.vault.economy.Economy;

public class SilverLotteryManager {

    private SilverLottery plugin;
    private DatabaseManager dbManager;
    private Economy econ;
    private Map<UUID, Integer> tickets = new HashMap<>();
    private int totalTickets = 0;
    private double basePrize;
    private double currentPrize;
    private String lastWinner = "";
    private long nextDrawingTime = -1;

    public SilverLotteryManager(SilverLottery plugin, DatabaseManager dbManager, Economy econ) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.econ = econ;
        basePrize = plugin.getConfig().getDouble("lottery.prize", 1000.0);

        // Betöltjük az adatbázisból a jegyállapotot és a metaadatokat
        tickets = dbManager.loadLotteryState();
        totalTickets = tickets.values().stream().mapToInt(Integer::intValue).sum();
        DatabaseManager.LotteryMeta meta = dbManager.loadLotteryMeta(basePrize);
        currentPrize = meta.currentPrize;
        lastWinner = meta.lastWinner;

        // Az első sorsolás ütemezése a configban megadott késleltetéssel
        scheduleNextDrawing();
    }

    public void buyTickets(Player player, int amount) {
        if (amount <= 0) {
            player.sendMessage(Messages.getMessage("lottery.buy.positive_amount"));
            return;
        }
        double ticketPrice = plugin.getConfig().getDouble("lottery.ticketPrice", 10.0);
        double cost = ticketPrice * amount;
        if (!econ.has(player, cost)) {
            player.sendMessage(Messages.getMessage("lottery.buy.not_enough_money"));
            return;
        }
        int maxTickets = plugin.getConfig().getInt("lottery.maxTicketsPerPlayer", 100);
        if (player.hasPermission("lottery.max.200")) {
            maxTickets = 200;
        }
        int currentTickets = tickets.getOrDefault(player.getUniqueId(), 0);
        if (currentTickets + amount > maxTickets) {
            player.sendMessage(Messages.getMessage("lottery.buy.max_reached", "%max%", String.valueOf(maxTickets)));
            return;
        }
        econ.withdrawPlayer(player, cost);
        int newTicketCount = currentTickets + amount;
        tickets.put(player.getUniqueId(), newTicketCount);
        totalTickets += amount;
        currentPrize += cost;
        player.sendMessage(Messages.getMessage("lottery.buy.success", "%amount%", String.valueOf(amount)));

        dbManager.logPurchase(player.getUniqueId(), amount);
        dbManager.updatePlayerTickets(player.getUniqueId(), newTicketCount);
        dbManager.updateLotteryMeta(currentPrize, lastWinner);
        // Nincs sorsolás ütemezése itt, mert az függetlenül, a scheduleNextDrawing() metódus szerint fut.
    }


    public void addTickets(String playerName, int amount) {
        OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = offPlayer.getUniqueId();
        int currentTickets = tickets.getOrDefault(uuid, 0);
        int newTicketCount = currentTickets + amount;
        tickets.put(uuid, newTicketCount);
        totalTickets += amount;
        dbManager.updatePlayerTickets(uuid, newTicketCount);
    }

    public void removeTickets(String playerName, int amount) {
        OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = offPlayer.getUniqueId();
        int currentTickets = tickets.getOrDefault(uuid, 0);
        int removal = Math.min(currentTickets, amount);
        int newTicketCount = currentTickets - removal;
        tickets.put(uuid, newTicketCount);
        totalTickets -= removal;
        dbManager.updatePlayerTickets(uuid, newTicketCount);
    }

    public void forceStart() {
        drawLottery();
    }

    public void resetLottery() {
        tickets.clear();
        totalTickets = 0;
        currentPrize = basePrize;
        nextDrawingTime = -1;
        dbManager.resetLotteryData(basePrize);
    }

    private void scheduleNextDrawing() {
        long drawingDelaySec = plugin.getConfig().getLong("lottery.drawingDelaySeconds", 10L);
        nextDrawingTime = System.currentTimeMillis() + drawingDelaySec * 1000;
        Bukkit.getScheduler().runTaskLater(plugin, this::drawLottery, drawingDelaySec * 20);
    }

    private void drawLottery() {
        if (totalTickets == 0) {
            Bukkit.broadcastMessage(Messages.getMessage("lottery.draw.no_tickets"));
        } else {
            int winningTicket = (int) (Math.random() * totalTickets) + 1;
            int count = 0;
            UUID winnerUUID = null;
            for (Map.Entry<UUID, Integer> entry : tickets.entrySet()) {
                count += entry.getValue();
                if (winningTicket <= count) {
                    winnerUUID = entry.getKey();
                    break;
                }
            }
            if (winnerUUID != null) {
                OfflinePlayer winner = Bukkit.getOfflinePlayer(winnerUUID);
                econ.depositPlayer(winner, currentPrize);
                lastWinner = (winner.getName() != null) ? winner.getName() : Messages.getMessage("lottery.unknown_player");
                Bukkit.broadcastMessage(Messages.getMessage("lottery.draw.winner", "%winner%", lastWinner, "%prize%", String.valueOf(currentPrize)));
                dbManager.updateLotteryMeta(currentPrize, lastWinner);
            } else {
                Bukkit.broadcastMessage(Messages.getMessage("lottery.draw.error"));
            }
        }
        // Reseteljük a lottó állapotát, de a lastWinner értéke megmarad
        resetLottery();
        // A sorsolás befejezése után azonnal ütemezzük a következő sorsolást
        scheduleNextDrawing();
    }

    public String getNextDrawing() {
        if (nextDrawingTime == -1) {
            return "00:00:00";
        }
        long remainingMillis = nextDrawingTime - System.currentTimeMillis();
        if (remainingMillis < 0) {
            remainingMillis = 0;
        }
        long totalSeconds = remainingMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public String getLastWinner() {
        return lastWinner;
    }

    public double getCurrentPrize() {
        return currentPrize;
    }

    public int getTicketsOfPlayer(UUID uuid) {
        return tickets.getOrDefault(uuid, 0);
    }

    public int getTotalTickets() {
        return totalTickets;
    }
}
