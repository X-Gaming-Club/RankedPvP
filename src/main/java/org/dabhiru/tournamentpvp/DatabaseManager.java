package org.dabhiru.tournamentpvp;

//package com.example.mypvpplugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private final Tournamentpvp plugin;
    private Connection connection;

    public DatabaseManager(Tournamentpvp plugin) {
        this.plugin = plugin;
        connect();
    }

    private void connect() {
        String host = plugin.getConfig().getString("database.host");
        int port = plugin.getConfig().getInt("database.port");
        String database = plugin.getConfig().getString("database.name");
        String username = plugin.getConfig().getString("database.username");
        String password = plugin.getConfig().getString("database.password");
        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false", username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect(); // Reconnect if connection is null or closed
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check connection status: " + e.getMessage());
            e.printStackTrace();
        }
        return connection;
    }
    public boolean isDiscordLinked(UUID uuid) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT discord_id FROM player_data WHERE uuid = ?");
            statement.setString(1, uuid.toString());
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next() && resultSet.getString("discord_id") != null;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public String getPlayerRank(UUID uuid) {
        try {
            Connection connection = plugin.getDatabaseManager().getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT rank FROM player_data WHERE uuid = ?");
            statement.setString(1, uuid.toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("rank");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "ChallengerTier3"; // Default rank if not found
    }
    public void updatePlayerRank(UUID uuid, String newRank) {
        try {
            Connection connection = plugin.getDatabaseManager().getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE player_data SET rank = ? WHERE uuid = ?;");
            statement.setString(1, newRank);
            statement.setString(2, uuid.toString());
            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Successfully updated rank for player with UUID: " + uuid);
            } else {
                System.out.println("No player found with UUID: " + uuid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public UUID getMinecraftUUID(String discordId) {
        try (Connection connection = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT uuid FROM player_data WHERE discord_id = ?");
            statement.setString(1, discordId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return UUID.fromString(resultSet.getString("uuid"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String getDiscordTag(UUID uuid) {
        try (Connection connection = plugin.getDatabaseManager().getConnection()){
        PreparedStatement statement = connection.prepareStatement("SELECT discord_id FROM player_data WHERE uuid = ?");
            statement.setString(1, uuid.toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("discord_id");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Database error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;// Return null if not found
    }
    public List<String> getTopKDLeaderboard(int topNumber) {
        List<String> leaderboard = new ArrayList<>();
        try (Connection connection = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT minecraft_name, kd_ratio FROM player_data ORDER BY kd_ratio DESC LIMIT ?");
            statement.setInt(1, topNumber);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String username = resultSet.getString("minecraft_name");
                double kdRatio = resultSet.getDouble("kd_ratio");
                leaderboard.add(username + ": " + kdRatio);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return leaderboard;
    }
    public double getPlayerKDRatio(UUID uuid) {
        double kdRatio = 0.0;

        try (Connection connection = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT kd_ratio FROM player_data WHERE uuid = ?");
            statement.setString(1, uuid.toString());
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                kdRatio = resultSet.getDouble("kd_ratio");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Database error: " + e.getMessage());
            e.printStackTrace();
        }

        return kdRatio; // Return 0.0 if not found
    }

    public void setDiscordTag(UUID uuid, String discordId) {

        try (Connection connection = plugin.getDatabaseManager().getConnection()){
        PreparedStatement statement = connection.prepareStatement("INSERT INTO player_data (uuid, discord_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE discord_id = VALUES(discord_id)");
            statement.setString(1, uuid.toString());
            statement.setString(2, discordId);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public String getMinecraftName(UUID uuid) {
        Connection connection = plugin.getDatabaseManager().getConnection();
        try (PreparedStatement statement = connection.prepareStatement("SELECT minecraft_name FROM player_data WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("minecraft_name");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Database error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void updateMinecraftName(UUID uuid, String minecraftName) {
        Connection connection = plugin.getDatabaseManager().getConnection();
        try (PreparedStatement statement = connection.prepareStatement("UPDATE player_data SET minecraft_name = ? WHERE uuid = ?")) {
            statement.setString(1, minecraftName);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
