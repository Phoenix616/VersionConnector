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

/*
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
                if(!sender.hasPermission(getPermission() + ".check")) {
                    sender.sendMessage(ChatColor.RED + "You don't have the permission " + getPermission() + ".check");
                    return;
                }
                if(args.length == 1) {
                    if(plugin.getProxy().getOnlineCount() == 0) {
                        sender.sendMessage(ChatColor.RED + "No player online");
                    } else {
                        Map<ProtocolVersion, Integer> versionMap = new HashMap<ProtocolVersion, Integer>();
                        Map<ProtocolVersion, Integer> forgeMap = new HashMap<ProtocolVersion, Integer>();
                        for(ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                            ProtocolVersion version = ProtocolVersion.getVersion(player.getPendingConnection().getVersion());
                            if(player.isForgeUser()) {
                                forgeMap.put(version, forgeMap.containsKey(version) ? forgeMap.get(version) + 1 : 1);
                            } else {
                                versionMap.put(version, versionMap.containsKey(version) ? versionMap.get(version) + 1 : 1);
                            }
                        }

                        sender.sendMessage(ChatColor.YELLOW + "Player versions:");
                        for(ProtocolVersion version : ProtocolVersion.values()) {
                            if(versionMap.containsKey(version)) {
                                sender.sendMessage(ChatColor.AQUA + version.toString() + ": " + ChatColor.YELLOW + versionMap.get(version));
                            }
                        }
                        if(forgeMap.size() > 0) {
                            sender.sendMessage(ChatColor.YELLOW + "Forge versions:");
                            for(ProtocolVersion version : ProtocolVersion.values()) {
                                if(forgeMap.containsKey(version)) {
                                    sender.sendMessage(ChatColor.AQUA + version.toString() + ": " + ChatColor.YELLOW + forgeMap.get(version));
                                }
                            }
                        }
                    }
                } else {
                    List<ProxiedPlayer> players = new ArrayList<ProxiedPlayer>();
                    if("-all".equalsIgnoreCase(args[1])) {
                        if(!sender.hasPermission(getPermission() + ".check.all")) {
                            sender.sendMessage(ChatColor.RED + "You don't have the permission " + getPermission() + ".check.all");
                            return;
                        }
                        players.addAll(plugin.getProxy().getPlayers());
                    } else {
                        if(!sender.hasPermission(getPermission() + ".check.other")) {
                            sender.sendMessage(ChatColor.RED + "You don't have the permission " + getPermission() + ".check.other");
                            return;
                        }
                        for(int i = 1; i < args.length; i++) {
                            ProxiedPlayer player = plugin.getProxy().getPlayer(args[i]);
                            if(player != null) {
                                players.add(player);
                            } else {
                                sender.sendMessage(ChatColor.YELLOW + args[i] + ChatColor.RED + " is not online!");
                            }
                        }
                    }

                    Collections.sort(players, Collections.reverseOrder(
                            (p1, p2) -> Integer.valueOf(p1.getPendingConnection().getVersion()).compareTo(p2.getPendingConnection().getVersion())
                    ));

                    for(ProxiedPlayer player : players) {
                        ProtocolVersion version = ProtocolVersion.getVersion(player.getPendingConnection().getVersion());
                        String versionStr = version.toString().replace("MINECRAFT_", "").replace("_", ".");
                        sender.sendMessage(ChatColor.AQUA + player.getName() + ChatColor.YELLOW + ": " + versionStr + "/" + version.toInt() + "/forge: " + player.isForgeUser());
                    }
                }
            } else if("config".equalsIgnoreCase(args[0])) {
                if(!sender.hasPermission(getPermission() + ".config")) {
                    sender.sendMessage(ChatColor.RED + "You don't have the permission " + getPermission() + ".config");
                    return;
                }
                sender.sendMessage(ChatColor.AQUA+ "Debug: " + ChatColor.YELLOW + plugin.getConfig().getBoolean("debug", true));

                Configuration versions = plugin.getConfig().getSection("versions");
                if (!versions.getKeys().isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "Legacy versions configuration:");
                    for(String key : versions.getKeys()) {
                        sender.sendMessage(ChatColor.AQUA + "  " + key + ": " + ChatColor.YELLOW + versions.getString(key));
                    }
                }
                Configuration forge = plugin.getConfig().getSection("forge");
                if (!forge.getKeys().isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "Legacy Forge configuration:");
                    for(String key : forge.getKeys()) {
                        sender.sendMessage(ChatColor.AQUA + "  " + key + ": " + ChatColor.YELLOW + forge.getString(key));
                    }
                }

                Configuration servers = plugin.getConfig().getSection("servers");
                if (servers.getKeys().size() == 0) {
                    sender.sendMessage(ChatColor.AQUA + "No servers config.");
                } else {
                    for (String key : servers.getKeys()) {

                        Configuration server = servers.getSection(key);
                        sender.sendMessage(ChatColor.YELLOW + key + " configuration:");
                        Configuration serverVersions = server.getSection("forge");
                        if (serverVersions.getKeys().isEmpty()) {
                            sender.sendMessage(ChatColor.AQUA + "  No versions config.");
                        } else {
                            sender.sendMessage(ChatColor.YELLOW + "  Versions:");
                            for (String serverName : serverVersions.getKeys()) {
                                sender.sendMessage(ChatColor.AQUA + "    " + serverName + ": " + ChatColor.YELLOW + serverVersions.getString(serverName));
                            }
                        }

                        Configuration serverForge = server.getSection("forge");
                        if (serverForge.getKeys().isEmpty()) {
                            sender.sendMessage(ChatColor.AQUA + "  No forge config.");
                        } else {
                            sender.sendMessage(ChatColor.YELLOW + "  Forge:");
                            for (String serverName : serverForge.getKeys()) {
                                sender.sendMessage(ChatColor.AQUA + "    " + serverName + ": " + ChatColor.YELLOW + serverForge.getString(serverName));
                            }
                        }
                    }
                }
            } else if("reload".equalsIgnoreCase(args[0])) {
                if(!sender.hasPermission(getPermission() + ".reload")) {
                    sender.sendMessage(ChatColor.RED + "You don't have the permission " + getPermission() + ".reload");
                    return;
                }
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
