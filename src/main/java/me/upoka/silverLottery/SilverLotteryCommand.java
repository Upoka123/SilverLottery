package me.upoka.silverLottery;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SilverLotteryCommand implements CommandExecutor {

    private SilverLotteryManager lotteryManager;

    public SilverLotteryCommand(SilverLotteryManager lotteryManager) {
        this.lotteryManager = lotteryManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "buy":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Messages.getMessage("lottery.error.player_only"));
                    return true;
                }
                Player player = (Player) sender;
                if (args.length < 2) {
                    player.sendMessage(Messages.getMessage("lottery.buy.usage"));
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[1]);
                    lotteryManager.buyTickets(player, amount);
                } catch (NumberFormatException e) {
                    player.sendMessage(Messages.getMessage("lottery.error.invalid_number"));
                }
                break;

            case "help":
                sendHelp(sender);
                break;

            case "reload":
                if (!sender.hasPermission("lottery.reload")) {
                    sender.sendMessage(Messages.getMessage("lottery.error.no_permission"));
                    return true;
                }
                SilverLottery.getInstance().reloadConfig();
                Messages.reload();
                sender.sendMessage(Messages.getMessage("lottery.reload.success"));
                break;

            case "status":
                sendStatus(sender);
                break;

            case "forcestart":
                if (!sender.hasPermission("lottery.forcestart")) {
                    sender.sendMessage(Messages.getMessage("lottery.error.no_permission"));
                    return true;
                }
                lotteryManager.forceStart();
                sender.sendMessage(Messages.getMessage("lottery.forcestart.success"));
                break;

            case "reset":
                if (!sender.hasPermission("lottery.reset")) {
                    sender.sendMessage(Messages.getMessage("lottery.error.no_permission"));
                    return true;
                }
                lotteryManager.resetLottery();
                sender.sendMessage(Messages.getMessage("lottery.reset.success"));
                break;

            case "remove":
                if (!sender.hasPermission("lottery.modify")) {
                    sender.sendMessage(Messages.getMessage("lottery.error.no_permission"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Messages.getMessage("lottery.remove.usage"));
                    return true;
                }
                String removePlayer = args[1];
                try {
                    int removeAmount = Integer.parseInt(args[2]);
                    lotteryManager.removeTickets(removePlayer, removeAmount);
                    sender.sendMessage(Messages.getMessage("lottery.remove.success", "%player%", removePlayer, "%amount%", String.valueOf(removeAmount)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Messages.getMessage("lottery.error.invalid_number"));
                }
                break;

            case "add":
                if (!sender.hasPermission("lottery.modify")) {
                    sender.sendMessage(Messages.getMessage("lottery.error.no_permission"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Messages.getMessage("lottery.add.usage"));
                    return true;
                }
                String addPlayer = args[1];
                try {
                    int addAmount = Integer.parseInt(args[2]);
                    lotteryManager.addTickets(addPlayer, addAmount);
                    sender.sendMessage(Messages.getMessage("lottery.add.success", "%player%", addPlayer, "%amount%", String.valueOf(addAmount)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Messages.getMessage("lottery.error.invalid_number"));
                }
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate(Messages.getMessage("lottery.help")));
    }

    private void sendStatus(CommandSender sender) {
        String prize = String.valueOf(lotteryManager.getCurrentPrize());
        String total = String.valueOf(lotteryManager.getTotalTickets());
        String next = lotteryManager.getNextDrawing();
        String winner = lotteryManager.getLastWinner();
        // Az adott játékos által a jelenlegi körre vásárolt szelvények száma (%ptickets%)
        String ptickets = "0";
        if (sender instanceof Player) {
            ptickets = String.valueOf(lotteryManager.getTicketsOfPlayer(((Player) sender).getUniqueId()));
        }
        sender.sendMessage(ColorUtil.translate(
                Messages.getMessage("lottery.status",
                        "%prize%", prize,
                        "%total%", total,
                        "%next%", next,
                        "%winner%", winner,
                        "%ptickets%", ptickets
                )
        ));
    }
}
