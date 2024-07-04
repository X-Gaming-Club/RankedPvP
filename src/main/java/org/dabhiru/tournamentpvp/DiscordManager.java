package org.dabhiru.tournamentpvp;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
//import org.bukkit.Color;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.entity.Player;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import javax.security.auth.login.LoginException;

public class DiscordManager extends ListenerAdapter{

    private final Tournamentpvp plugin;
    private JDA jda;
    private Guild guild;
    String rankk;

    public DiscordManager(Tournamentpvp plugin) {
        this.plugin = plugin;
        initializeJDA();

    }

    private void initializeJDA() {
        String token = plugin.getConfig().getString("discord.token");
        String serverId = plugin.getConfig().getString("discord.serverId");

        try {
            jda = JDABuilder.createDefault(token)
                    .addEventListeners(this)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .enableCache(CacheFlag.VOICE_STATE)
                    .build()
                    .awaitReady();
            guild = jda.getGuildById(serverId);
            if (guild == null) {
                plugin.getLogger().severe("Could not find Discord server with ID: " + serverId);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return; // Ignore bot messages

        String message = event.getMessage().getContentRaw();
        String[] args = message.split("\\s+");
        if (args[0].equalsIgnoreCase("/id")) {
            sendUserId(event);
        }

        //tring message = event.getMessage().getContentRaw();


        if (args[0].equalsIgnoreCase("/kd")) {
            sendKD(event);
        } else if (args[0].equalsIgnoreCase("/top") && args.length == 2) {
            try {
                int topNumber = Integer.parseInt(args[1]);
                sendTopLeaderboard(event, topNumber);
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("Invalid command usage. Correct usage: `/top {number}`").queue();
            }
        }
    }

    private void sendKD(MessageReceivedEvent event) {
        Member member = event.getMember();
        if (member == null) return;

        // Get Discord user ID
        String discordId = member.getId();

        // Check if Discord ID is linked to Minecraft player
        UUID uuid = plugin.getDatabaseManager().getMinecraftUUID(discordId);
        if (uuid == null) {
            event.getChannel().sendMessage("Your Discord is not linked to a Minecraft player.").queue();
            return;
        }

        // Fetch KD ratio from database
        double kdRatio = plugin.getDatabaseManager().getPlayerKDRatio(uuid);

        // Send KD ratio in channel and DM
        event.getChannel().sendMessage(member.getAsMention() + " Your KD Ratio: " + kdRatio).queue();
        member.getUser().openPrivateChannel().queue(privateChannel ->
                privateChannel.sendMessage("Your KD Ratio: " + kdRatio).queue());
    }
    private void sendTopLeaderboard(MessageReceivedEvent event, int topNumber) {
        List<String> leaderboard = plugin.getDatabaseManager().getTopKDLeaderboard(topNumber);

        StringBuilder leaderboardMessage = new StringBuilder("Top " + topNumber + " KD Ratios:\n");
        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboardMessage.append((i + 1)).append(". ").append(leaderboard.get(i)).append("\n");
        }

        event.getChannel().sendMessage(leaderboardMessage.toString()).queue();
    }

    private void sendUserId(MessageReceivedEvent event) {
        String userId = event.getAuthor().getId();
        event.getAuthor().openPrivateChannel().queue(channel -> {
            channel.sendMessage("Your Discord ID: " + userId).queue();
            channel.sendMessage("Run below command in minecraft storymode server to show rank in discord.").queue();
            channel.sendMessage("/link " + userId).queue();
        });
    }

    private void startDailyMessageTask() {
        Timer timer = new Timer();
        timer.schedule(new DailyMessageTask(jda, plugin), 0, 24 * 60 * 60 * 1000); // Schedule to run once a day
    }

    private static class DailyMessageTask extends TimerTask {

        private final JDA jda;
        private final Tournamentpvp plugin;

        public DailyMessageTask(JDA jda, Tournamentpvp plugin) {
            this.jda = jda;
            this.plugin = plugin;
        }

        @Override
        public void run() {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                UUID playerUUID = player.getUniqueId();
                double kdRatio = plugin.getDatabaseManager().getPlayerKDRatio(playerUUID);
                String rank = plugin.getDatabaseManager().getPlayerRank(playerUUID);
                String message = String.format("You are in current rank %s. Improve your PvP skill! Your K/D ratio is %.2f.", rank, kdRatio);

                String discordId = plugin.getDatabaseManager().getDiscordTag(playerUUID);
                if (discordId != null) {
                    jda.retrieveUserById(discordId).queue(user -> {
                        user.openPrivateChannel().queue(channel -> {
                            channel.sendMessage(message).queue();
                        });
                    });
                }

            }
        }
    }


    public void assignDiscordRole(Player player, String rank) {
        String discordId = plugin.getDatabaseManager().getDiscordTag(player.getUniqueId());
        if (discordId == null) {
            plugin.getLogger().warning("Discord ID not found for player: " + player.getName());
            return;
        }

        jda.retrieveUserById(discordId).queue(user -> {
            plugin.getLogger().info("username " + user.getName());
            guild.retrieveMember(user).queue(member -> {
                plugin.getLogger().info("Member " + member.getEffectiveName());
                if (member == null) {
                    plugin.getLogger().warning("Member not found in guild for discord ID: " + discordId);
                    return;
                }
                //String roleName = rank + " " + getRankEmoji(rank);
                Role role = guild.getRolesByName(rank, true).stream().findFirst().orElse(null);
                Color roleColor = determineRoleColor(rank);
                rankk = rank;

                if (role == null) {
                    plugin.getLogger().info("Creating new role: " + rank);
                    guild.createRole()
                            .setName(rank)
                            .setColor(roleColor)
                            .setHoisted(true)
                            .queue(
                                    createdRole -> {
                                        plugin.getLogger().info("Role " + createdRole.getName() + " created successfully");
                                        assignRoleToMember(member, createdRole);
                                    },
                                    error -> plugin.getLogger().severe("Error creating role: " + error.getMessage())
                            );
                } else {
                    if (member.getRoles().contains(role)) {

                        role.getManager().setColor(roleColor).setHoisted(true).queue(
                                success -> plugin.getLogger().info("Member already has the role: " + rank),
                                error -> plugin.getLogger().severe("Error setting role color: " + error.getMessage())
                        );

                    } else {
                        role.getManager().setColor(roleColor).setHoisted(true).queue(
                                success -> assignRoleToMember(member, role),
                                error -> plugin.getLogger().severe("Error setting role color: " + error.getMessage())
                        );
                    }
                }
            });
        }, error -> plugin.getLogger().severe("Failed to retrieve user by ID: " + error.getMessage()));
    }

    private void assignRoleToMember(Member member, Role role) {
        try {
            guild.addRoleToMember(member, role).queue(
                    success -> plugin.getLogger().info("Role " + role.getName() + " assigned successfully to " + member.getEffectiveName()),
                    error -> plugin.getLogger().severe("Failed to assign role: " + error.getMessage())
            );

            // Retrieve emoji ID and assign icon to member's nickname

        } catch (HierarchyException e) {
            plugin.getLogger().warning("Cannot modify member with higher or equal highest role: " + member.getEffectiveName());
        }
    }


    private Color determineRoleColor(String rank) {
        switch (rank) {
            case "D1":
            case "D1+":
                return Color.RED; // Redstone color
            case "K-2":
            case "K-1":
                return Color.YELLOW; // Gold color
            case "SH-2":
            case "SH-1":
                return Color.GREEN; // Emerald color
            case "ER-2":
            case "ER-1":
                return Color.CYAN; // Diamond color
            default:
                return Color.WHITE; // Default color
        }

    }



}
