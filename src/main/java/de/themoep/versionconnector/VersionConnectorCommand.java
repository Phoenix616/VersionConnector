package de.themoep.versionconnector;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Licensed under the Nietzsche Public License v0.6
 *
 * Copyright 2016 Max Lee (https://github.com/Phoenix616/)
 *
 * Copyright, like God, is dead.  Let its corpse serve only to guard against its
 * resurrection.  You may do anything with this work that copyright law would
 * normally restrict so long as you retain the above notice(s), this license, and
 * the following misquote and disclaimer of warranty with all redistributed
 * copies, modified or verbatim.  You may also replace this license with the Open
 * Works License, available at the http://owl.apotheon.org website.
 *
 *    Copyright is dead.  Copyright remains dead, and we have killed it.  How
 *    shall we comfort ourselves, the murderers of all murderers?  What was
 *    holiest and mightiest of all that the world of censorship has yet owned has
 *    bled to death under our knives: who will wipe this blood off us?  What
 *    water is there for us to clean ourselves?  What festivals of atonement,
 *    what sacred games shall we have to invent?  Is not the greatness of this
 *    deed too great for us?  Must we ourselves not become authors simply to
 *    appear worthy of it?
 *                                     - apologies to Friedrich Wilhelm Nietzsche
 *
 * No warranty is implied by distribution under the terms of this license.
 */

public class VersionConnectorCommand extends Command {
    private final VersionConnector plugin;

    public VersionConnectorCommand(VersionConnector plugin) {
        super(plugin.getDescription().getName(), plugin.getDescription().getName().toLowerCase() + ".command", "vc", "vercon");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length > 0) {
            if("check".equalsIgnoreCase(args[0])) {
                if(args.length == 1) {
                    sender.sendMessage(ChatColor.YELLOW + "Player versions:");
                    if(plugin.getProxy().getOnlineCount() == 0) {
                        sender.sendMessage(ChatColor.RED + "No player online");
                    } else {
                        Map<ProtocolVersion, Integer> versionMap = new HashMap<ProtocolVersion, Integer>();
                        for(ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                            ProtocolVersion version = ProtocolVersion.getVersion(player.getPendingConnection().getVersion());
                            Integer count = versionMap.get(version);
                            if(count == null) {
                                count = 0;
                            }
                            versionMap.put(version, count + 1);
                        }

                        for(ProtocolVersion version : ProtocolVersion.values()) {
                            if(versionMap.containsKey(version)) {
                                sender.sendMessage(ChatColor.AQUA + version.toString() + ": " + ChatColor.YELLOW + versionMap.get(version));
                            }
                        }
                    }
                } else {
                    List<ProxiedPlayer> players = new ArrayList<ProxiedPlayer>();
                    if("-all".equalsIgnoreCase(args[1])) {
                        players.addAll(plugin.getProxy().getPlayers());
                    } else {
                        for(int i = 1; i < args.length; i++) {
                            ProxiedPlayer player = plugin.getProxy().getPlayer(args[i]);
                            if(player != null) {
                                players.add(player);
                            } else {
                                sender.sendMessage(ChatColor.YELLOW + args[i] + ChatColor.RED + " is not online!");
                            }
                        }
                    }

                    Collections.sort(players, Collections.reverseOrder(new Comparator<ProxiedPlayer>() {
                        public int compare(ProxiedPlayer p1, ProxiedPlayer p2) {
                            return Integer.valueOf(p1.getPendingConnection().getVersion()).compareTo(p2.getPendingConnection().getVersion());
                        }
                    }));

                    for(ProxiedPlayer player : players) {
                        ProtocolVersion version = ProtocolVersion.getVersion(player.getPendingConnection().getVersion());
                        String versionStr = version.toString().replace("MINECRAFT_", "").replace("_", ".");
                        sender.sendMessage(ChatColor.AQUA + player.getName() + ChatColor.YELLOW + ": " + versionStr + "/" + version.toInt());
                    }
                }
            } else if("config".equalsIgnoreCase(args[0])) {
                sender.sendMessage(ChatColor.YELLOW + "Current configuration:");
                Configuration versions = plugin.getConfig().getSection("versions");
                if(versions.getKeys().size() == 0) {
                    sender.sendMessage(ChatColor.AQUA + "Nothing configured.");
                } else {
                    for(String key : versions.getKeys()) {
                        sender.sendMessage(ChatColor.AQUA + key + ": " + ChatColor.YELLOW + versions.getString(key));
                    }
                }
            } else if("reload".equalsIgnoreCase(args[0])) {
                if(plugin.loadConfig()) {
                    sender.sendMessage(ChatColor.GREEN + "Config reloaded!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Error occured while reloading the config! Take a look at the console log.");
                }
            }
        } else {
            sender.sendMessage(ChatColor.AQUA + plugin.getDescription().getName() + ChatColor.YELLOW + " version " + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.AQUA + "Usage: " + ChatColor.YELLOW + "/vc [check [<player>]|config|reload]");
        }
    }
}
