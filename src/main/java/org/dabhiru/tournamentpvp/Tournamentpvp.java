package org.dabhiru.tournamentpvp;


//package com.example.mypvpplugin;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Tournamentpvp extends JavaPlugin {

    public LuckPerms luckPerms;
    private DatabaseManager databaseManager;
    private DiscordManager discordManager;
    private RankManager rankManager;
    @Override

    public void onEnable() {



        saveDefaultConfig();
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                luckPerms = provider.getProvider();

            }
            try {
                luckPerms = LuckPermsProvider.get();
            } catch (IllegalStateException e) {
                getLogger().severe("LuckPerms is not loaded properly. Disabling plugin.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } else {
            getLogger().severe("LuckPerms plugin is not enabled. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        //this.luckPerms = LuckPermsProvider.get();
        this.databaseManager = new DatabaseManager(this);
        this.discordManager = new DiscordManager(this);
        this.rankManager = new RankManager(this);
        discordManager = new DiscordManager(this);
        getServer().getPluginManager().registerEvents(new PlayerActivityListener(this), this);
        getCommand("link").setExecutor(new LinkCommand(this));


    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }
}
