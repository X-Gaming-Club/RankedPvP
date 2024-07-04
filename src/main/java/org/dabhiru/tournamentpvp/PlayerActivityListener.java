package org.dabhiru.tournamentpvp;

//package com.example.mypvpplugin;

import net.luckperms.api.LuckPerms;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerActivityListener implements Listener {

    private final Tournamentpvp plugin;
    private final RankManager rankManager;
    //private final LuckPerms luckPerms;
    private Set<UUID> awaitingLinkMessage = new HashSet<>();
    public PlayerActivityListener(Tournamentpvp plugin) {
        this.plugin = plugin;
//        this.luckPerms = luckPerms;
        this.rankManager = new RankManager(plugin);


    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        if (!plugin.getDatabaseManager().isDiscordLinked(playerUUID)) {
            player.sendMessage("§6§l[XGaming] §r§7Link your Discord account to unlock more features!");
            player.sendMessage("§7Don't know how? Message an admin at §b§nhttps://xgaming.club/discord");

        }
        // Add player to awaiting set
        initializePlayerData(player);
    }



    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();

        if (killer != null) {
            UUID playerUUID = player.getUniqueId();
            UUID killerUUID = killer.getUniqueId();

            // Update killer's kill count
            updateKillCount(killerUUID);

            // Update player's death count
            updateDeathCount(playerUUID);

            // Update K/D ratio for killer
            updateKDRatio(killerUUID);

            // Killer gets points for killing a player
            adjustPlayerValues(killer, 0, getPointsForKillingPlayer(player));

            // Player loses points for dying
            adjustPlayerValues(player, 0, -getPenaltyForDying(player));
        }
    }
    private void updateKillCount(UUID playerUUID) {
        try (Connection connection = plugin.getDatabaseManager().getConnection()){
            PreparedStatement selectStatement = connection.prepareStatement(
                    "SELECT * FROM player_data WHERE uuid = ?");
            selectStatement.setString(1, playerUUID.toString());
            ResultSet resultSet = selectStatement.executeQuery();

            if (resultSet.next()) {
                // Player exists, update kill count
                int currentKillCount = resultSet.getInt("kill_count");
                PreparedStatement updateStatement = connection.prepareStatement(
                        "UPDATE player_data SET kill_count = ? WHERE uuid = ?");
                updateStatement.setInt(1, currentKillCount + 1);
                updateStatement.setString(2, playerUUID.toString());
                updateStatement.executeUpdate();
            } else {
                // Player does not exist, insert new record
                PreparedStatement insertStatement = connection.prepareStatement(
                        "INSERT INTO player_data (uuid, kill_count, death_count, kd_ratio) VALUES (?, 1, 0, 1.0)");
                insertStatement.setString(1, playerUUID.toString());
                insertStatement.executeUpdate();
            }

            resultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateDeathCount(UUID playerUUID) {
        try(Connection connection = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement selectStatement = connection.prepareStatement(
                    "SELECT * FROM player_data WHERE uuid = ?");
            selectStatement.setString(1, playerUUID.toString());
            ResultSet resultSet = selectStatement.executeQuery();

            if (resultSet.next()) {
                // Player exists, update death count
                int currentDeathCount = resultSet.getInt("death_count");
                PreparedStatement updateStatement = connection.prepareStatement(
                        "UPDATE player_data SET death_count = ? WHERE uuid = ?");
                updateStatement.setInt(1, currentDeathCount + 1);
                updateStatement.setString(2, playerUUID.toString());
                updateStatement.executeUpdate();
            } else {
                // Player does not exist, insert new record
                PreparedStatement insertStatement = connection.prepareStatement(
                        "INSERT INTO player_data (uuid, kill_count, death_count, kd_ratio) VALUES (?, 0, 1, 0.0)");
                insertStatement.setString(1, playerUUID.toString());
                insertStatement.executeUpdate();
            }

            resultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateKDRatio(UUID playerUUID) {
        try (Connection connection = plugin.getDatabaseManager().getConnection()){
            PreparedStatement selectStatement = connection.prepareStatement(
                    "SELECT kill_count, death_count FROM player_data WHERE uuid = ?");
            selectStatement.setString(1, playerUUID.toString());
            ResultSet resultSet = selectStatement.executeQuery();

            if (resultSet.next()) {
                int killCount = resultSet.getInt("kill_count");
                int deathCount = resultSet.getInt("death_count");

                double kdRatio = calculateKDRatio(killCount, deathCount);

                PreparedStatement updateStatement = connection.prepareStatement(
                        "UPDATE player_data SET kd_ratio = ? WHERE uuid = ?");
                updateStatement.setDouble(1, kdRatio);
                updateStatement.setString(2, playerUUID.toString());
                updateStatement.executeUpdate();
            }

            resultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private double calculateKDRatio(int killCount, int deathCount) {
        double kdRatio;

        if (deathCount == 0) {
            kdRatio = killCount; // Handle infinite K/D ratio when no deaths
        } else {
            // Adjust the formula here for a smoother increase
            kdRatio = (double) killCount / Math.max(1, deathCount); // Ensures division by at least 1 to avoid division by zero
            kdRatio = Math.log(kdRatio + 1) * 2; // Example logarithmic adjustment
        }

        return kdRatio;
    }



    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer != null) {
            int points = 0;

            // Points based on entity type
            if (entity.getType() == EntityType.PLAYER) {
                points = getPointsForKillingPlayer((Player) entity);
            }
//            else if (isBossMob(entity.getType())) {
//                points = 10 + (int) (Math.random() * 3);
//            } else if (isRegularMob(entity.getType())) {
//                points = 1 + (int) (Math.random() * 2); // 1-2 points
//            } else if (isBot(entity.getType())) {
//                points = 30 + (int) (Math.random() * 11); // 30-40 points
//            }

            // Adjust the killer's values
            adjustPlayerValues(killer, 0, points);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            int points = (int)(event.getFinalDamage() * 0.1); // 10% of damage dealt
            adjustPlayerValues(player, 0, points);
        }

        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            int points = (int)(event.getFinalDamage() * -0.1); // 10% of damage taken
            adjustPlayerValues(player, 0, points);
        }
    }


    private void adjustPlayerValues(Player player, int aDelta, int bDelta) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection connection = plugin.getDatabaseManager().getConnection()) {
                    // Retrieve current values
                    PreparedStatement statement = connection.prepareStatement(
                            "SELECT a, b FROM player_data WHERE uuid = ?");
                    statement.setString(1, player.getUniqueId().toString());
                    ResultSet resultSet = statement.executeQuery();
                    int a=0, b=0;
                    System.out.println(resultSet);
                    if (resultSet.next()) {
                        a = resultSet.getInt("a");
                        b = resultSet.getInt("b");
                    }

                    // Adjust values
                    a += aDelta;
                    b += bDelta;

                    // Ensure values of 'a' and 'b' are within bounds
                    while (b >= 250) {
                        b -= 250;
                        a += 1;
                    }

                    while (b < 0) {
                        b += 250;
                        a -= 1;
                    }

                    // Update the database
                    statement = connection.prepareStatement(
                            "UPDATE player_data SET a = ?, b = ? WHERE uuid = ?");
                    statement.setInt(1, a);
                    statement.setInt(2, b);
                    statement.setString(3, player.getUniqueId().toString());
                    statement.executeUpdate();
                    System.out.println(statement);
                    // Update the player's rank
                    rankManager.updateRank(player, a, b);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private int getPointsForKillingPlayer (Player player){
        String rank = plugin.getDatabaseManager().getPlayerRank(player.getUniqueId());
        switch (rank) {
            case "K-2":
            case "K-1":
                return 55;
            case "SH-2":
            case "SH-1":
                return 60;
            case "ER-2":
            case "ER-1":
                return 70;
            default:
                return 50; // Default value
        }
    }

    private int getPenaltyForDying (Player player){
        String rank = plugin.getDatabaseManager().getPlayerRank(player.getUniqueId());
        switch (rank) {
            case "K-1":
            case "K-2":
                return 55;
            case "SH-2":
            case "SH-1":
                return 60;
            case "ER-2":
            case "ER-1":
                return 70;
            default:
                return 50; // Default value
        }
    }

    private boolean isBossMob(EntityType type) {
        return type == EntityType.ENDER_DRAGON ||
                type == EntityType.WITHER ||
                type == EntityType.WARDEN ||
                type == EntityType.ELDER_GUARDIAN ||
                type == EntityType.GIANT ||
                type == EntityType.GUARDIAN ||
                type == EntityType.PHANTOM ||
                type == EntityType.RAVAGER ||
                type == EntityType.SHULKER ||
                type == EntityType.STRAY ||
                type == EntityType.WITHER_SKELETON;
    }

    private boolean isRegularMob(EntityType type) {
        return type == EntityType.ZOMBIE ||
                type == EntityType.SKELETON ||
                type == EntityType.SPIDER ||
                type == EntityType.CREEPER ||
                type == EntityType.ENDERMAN ||
                type == EntityType.HUSK ||
                type == EntityType.DROWNED ||
                type == EntityType.PILLAGER ||
                type == EntityType.VINDICATOR ||
                type == EntityType.EVOKER ||
                type == EntityType.CAVE_SPIDER ||
                type == EntityType.SILVERFISH ||
                type == EntityType.BLAZE ||
                type == EntityType.MAGMA_CUBE ||
                type == EntityType.SLIME ||
                type == EntityType.WITCH ||
                type == EntityType.ZOMBIFIED_PIGLIN;
    }


    private boolean isBot (EntityType type){
            // Placeholder for identifying bots (e.g., Citizens with 'Sentient' trait)
            Player player = null;
            return (type == EntityType.PLAYER && player.hasMetadata("NPC"));
        }

    private void initializePlayerData(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Connection connection = plugin.getDatabaseManager().getConnection();
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO player_data (uuid, minecraft_name, a, b) VALUES (?, ?, ?, ?) " +
                                    "ON DUPLICATE KEY UPDATE minecraft_name = VALUES(minecraft_name)");
                    statement.setString(1, player.getUniqueId().toString());
                    statement.setString(2, player.getName());
                    statement.setInt(3, 0);
                    statement.setInt(4, 0);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

}



