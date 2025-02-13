package me.upoka.silverLottery;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseManager {

    private JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setupDatabase() {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");
        try {
            if (dbType.equalsIgnoreCase("mysql")) {
                String host = plugin.getConfig().getString("database.mysql.host", "localhost");
                int port = plugin.getConfig().getInt("database.mysql.port", 3306);
                String database = plugin.getConfig().getString("database.mysql.database", "lottery");
                String username = plugin.getConfig().getString("database.mysql.username", "root");
                String password = plugin.getConfig().getString("database.mysql.password", "");
                connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, username, password);
            } else if (dbType.equalsIgnoreCase("h2")) {
                String database = plugin.getConfig().getString("database.h2.database", "lottery");
                connection = DriverManager.getConnection("jdbc:h2:./" + database);
            } else {
                connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/lottery.db");
            }

            // Állítsuk be a CREATE TABLE parancsokat a használt adatbázis típusától függően
            String createLotteryLogQuery;
            String createLotteryStateQuery;
            String createLotteryMetaQuery;

            if (dbType.equalsIgnoreCase("mysql") || dbType.equalsIgnoreCase("h2")) {
                createLotteryLogQuery = "CREATE TABLE IF NOT EXISTS lottery_log (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "player_uuid VARCHAR(36), " +
                        "tickets INT, " +
                        "timestamp BIGINT" +
                        ")";
                createLotteryStateQuery = "CREATE TABLE IF NOT EXISTS lottery_state (" +
                        "player_uuid VARCHAR(36) PRIMARY KEY, " +
                        "tickets INT" +
                        ")";
                createLotteryMetaQuery = "CREATE TABLE IF NOT EXISTS lottery_meta (" +
                        "id INT PRIMARY KEY, " +
                        "current_prize DOUBLE, " +
                        "last_winner VARCHAR(255)" +
                        ")";
            } else {
                // SQLite szintaxis
                createLotteryLogQuery = "CREATE TABLE IF NOT EXISTS lottery_log (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "player_uuid TEXT, " +
                        "tickets INTEGER, " +
                        "timestamp BIGINT" +
                        ")";
                createLotteryStateQuery = "CREATE TABLE IF NOT EXISTS lottery_state (" +
                        "player_uuid TEXT PRIMARY KEY, " +
                        "tickets INTEGER" +
                        ")";
                createLotteryMetaQuery = "CREATE TABLE IF NOT EXISTS lottery_meta (" +
                        "id INTEGER PRIMARY KEY, " +
                        "current_prize DOUBLE, " +
                        "last_winner TEXT" +
                        ")";
            }

            try (PreparedStatement ps = connection.prepareStatement(createLotteryLogQuery)) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(createLotteryStateQuery)) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(createLotteryMetaQuery)) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void logPurchase(UUID playerUUID, int tickets) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO lottery_log (player_uuid, tickets, timestamp) VALUES (?, ?, ?)")) {
                ps.setString(1, playerUUID.toString());
                ps.setInt(2, tickets);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // Beolvassa a lottery_state táblából az aktuális jegyállapotot
    public Map<UUID, Integer> loadLotteryState() {
        Map<UUID, Integer> state = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT player_uuid, tickets FROM lottery_state")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                int ticketCount = rs.getInt("tickets");
                state.put(uuid, ticketCount);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return state;
    }

    public static class LotteryMeta {
        public double currentPrize;
        public String lastWinner;

        public LotteryMeta(double currentPrize, String lastWinner) {
            this.currentPrize = currentPrize;
            this.lastWinner = lastWinner;
        }
    }

    // Beolvassa a lottery_meta táblából az aktuális metaadatokat.
    // Ha még nincs adat, beszúr egy alapértelmezett értékkel.
    public LotteryMeta loadLotteryMeta(double basePrize) {
        LotteryMeta meta = null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT current_prize, last_winner FROM lottery_meta WHERE id = 1")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double currentPrize = rs.getDouble("current_prize");
                String lastWinner = rs.getString("last_winner");
                meta = new LotteryMeta(currentPrize, lastWinner);
            } else {
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO lottery_meta (id, current_prize, last_winner) VALUES (1, ?, ?)")) {
                    insert.setDouble(1, basePrize);
                    insert.setString(2, "");
                    insert.executeUpdate();
                }
                meta = new LotteryMeta(basePrize, "");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return meta;
    }

    // Frissíti (vagy beszúrja) a játékos jegyeit a lottery_state táblában
    public void updatePlayerTickets(UUID uuid, int tickets) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE lottery_state SET tickets = ? WHERE player_uuid = ?")) {
            ps.setInt(1, tickets);
            ps.setString(2, uuid.toString());
            int affected = ps.executeUpdate();
            if (affected == 0) {
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO lottery_state (player_uuid, tickets) VALUES (?, ?)")) {
                    insert.setString(1, uuid.toString());
                    insert.setInt(2, tickets);
                    insert.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Frissíti a lottery_meta táblában az aktuális nyereményt és utolsó nyertest
    public void updateLotteryMeta(double currentPrize, String lastWinner) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE lottery_meta SET current_prize = ?, last_winner = ? WHERE id = 1")) {
            ps.setDouble(1, currentPrize);
            ps.setString(2, lastWinner);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void resetLotteryData(double basePrize) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM lottery_state")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Csak a current_prize értékét állítjuk vissza, a last_winner értéke megmarad
        try (PreparedStatement ps = connection.prepareStatement("UPDATE lottery_meta SET current_prize = ? WHERE id = 1")) {
            ps.setDouble(1, basePrize);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void close() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
