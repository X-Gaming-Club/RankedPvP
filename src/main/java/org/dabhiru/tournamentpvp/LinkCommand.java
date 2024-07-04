package org.dabhiru.tournamentpvp;

//package com.example.mypvpplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LinkCommand implements CommandExecutor {

    private final Tournamentpvp plugin;

    public LinkCommand(Tournamentpvp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Check if the player is already linked
            if (plugin.getDatabaseManager().isDiscordLinked(player.getUniqueId())) {
                player.sendMessage("You have already linked your Discord account.");
                return true;
            }

            if (args.length != 1) {
                player.sendMessage("Usage: /link <discord_tag>");
                return false;
            }

            String discordId = args[0];

            // Link the Discord ID in the database
            plugin.getDatabaseManager().setDiscordTag(player.getUniqueId(), discordId);

            player.sendMessage("Your Discord ID has been linked successfully.");
        } else {
            sender.sendMessage("This command can only be used by players.");
        }
        return true;
    }
}
