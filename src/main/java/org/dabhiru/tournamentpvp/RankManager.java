package org.dabhiru.tournamentpvp;
//package com.example.mypvpplugin;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RankManager {

    private final Tournamentpvp plugin;
    //private final LuckPerms luckPerms;
    public RankManager(Tournamentpvp plugin) {
        this.plugin = plugin;

    }

    public void updateRank(Player player, int a, int b) {
        // Calculate the value of a and b
        while (b >= 350) {
            b -= 250;
            a += 1;
        }

        while (b < 0) {
            b += 250;
            a -= 1;
        }

        // Determine the rank based on the value of a
        String rank = determineRank(a);

        // Assign the rank to the player
        assignRank(player, rank);
    }

    private String determineRank(int a) {
        if (a >= 60) {
            return "ER-1";
        } else if (a >= 45) {
            return "ER-2";
        } else if (a >= 36) {
            return "SH-1";
        } else if (a >= 27) {
            return "SH-2";
        } else if (a >= 18) {
            return "K-1";
        } else if (a >= 10) {
            return "K-2";
        } else if (a >= 4) {
            return "D1+";
        } else {
            return "D1";
        }
    }

    private PrefixWeight determinePrefixAndWeight(String rank) {
        switch (rank) {
            case "ER-1":
                return new PrefixWeight("§6[ER-1 ꒯] ", 100);
            case "ER-2":
                return new PrefixWeight("§6[ER-2 ꒰] ", 90);
            case "SH-1":
                return new PrefixWeight("§c[SH-1 ꕊ] ", 80);
            case "SH-2":
                return new PrefixWeight("§c[SH-2 ꕋ] ", 70);
            case "K-1":
                return new PrefixWeight("§a[K-1 ꓭ] ", 60);
            case "K-2":
                return new PrefixWeight("§a[K-2 ꓮ] ", 50);
            case "D1+":
                return new PrefixWeight("§e[D1+ ꖈ] ", 40);
            case "D1":
                return new PrefixWeight("§e[D1 ꖉ] ", 30);
            default:
                return new PrefixWeight("", 0);
        }
    }

    private void assignRank(Player player, String rank) {
        LuckPerms luckPerms = plugin.getLuckPerms();

        // Determine prefix and weight for the rank
        PrefixWeight prefixWeight = determinePrefixAndWeight(rank);

        // Get or create the group
        Group group = luckPerms.getGroupManager().getGroup(rank);
        if (group == null) {
            group = luckPerms.getGroupManager().createAndLoadGroup(rank).join();
        }

        // Set the prefix and weight for the group
        group.data().add(Node.builder("prefix."+ prefixWeight.weight +"." + prefixWeight.prefix).build());
        group.data().add(Node.builder("weight." + prefixWeight.weight).build());

        // Save the group data
        luckPerms.getGroupManager().saveGroup(group);

        // Create an inheritance node
        InheritanceNode node = InheritanceNode.builder(group).build();

        // Remove all existing rank nodes before assigning the new rank
        luckPerms.getUserManager().loadUser(player.getUniqueId()).thenAccept(user -> {
            boolean hasRank = user.getNodes(NodeType.INHERITANCE).stream()
                    .anyMatch(existingNode -> existingNode.getGroupName().equalsIgnoreCase(rank));
            if (!hasRank) {
                // Remove all existing rank nodes before assigning the new rank
                Set<InheritanceNode> existingNodes = user.data().toCollection().stream()
                        .filter(NodeType.INHERITANCE::matches)
                        .map(NodeType.INHERITANCE::cast)
                        .collect(Collectors.toSet());

                existingNodes.forEach(user.data()::remove);
                user.data().add(node);

                // Save changes
                luckPerms.getUserManager().saveUser(user);
            }
        }).join();

        // Assign the Discord role if the player doesn't already have it
        plugin.getDiscordManager().assignDiscordRole(player, rank);

        // Update the player's rank in the database
        plugin.getDatabaseManager().updatePlayerRank(player.getUniqueId(), rank);
    }

    class PrefixWeight {
        String prefix;
        int weight;

        PrefixWeight(String prefix, int weight) {
            this.prefix = prefix;
            this.weight = weight;
        }
    }

}

