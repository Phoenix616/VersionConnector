package de.themoep.versionconnector;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                        Map<Integer, Integer> versionMap = new LinkedHashMap<>();
                        Map<Integer, Integer> forgeMap = new LinkedHashMap<>();
                        Map<String, Integer> modsMap = new LinkedHashMap<>();
                        for(ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                            int version = plugin.getVersion(player);
                            if (plugin.isForge(player)) {
                                forgeMap.put(version, forgeMap.getOrDefault(version, 0) + 1);
                            } else {
                                versionMap.put(version, versionMap.getOrDefault(version, 0) + 1);
                            }
                            if (player.getModList().size() > 0) {
                                String mods = player.getModList().keySet().stream().sorted(String::compareToIgnoreCase).collect(Collectors.joining(","));
                                modsMap.put(mods, modsMap.getOrDefault(mods, 0) + 1);
                            }
                        }

                        sender.sendMessage(ChatColor.YELLOW + "Player versions:");
                        versionMap.entrySet().stream().sorted(Collections.reverseOrder(Comparator.comparingInt(Map.Entry::getKey))).forEach(e -> {
                            ProtocolVersion version = ProtocolVersion.getVersion(e.getKey());
                            if (version != ProtocolVersion.UNKNOWN) {
                                sender.sendMessage(ChatColor.AQUA + version.toString() + ": " + ChatColor.YELLOW + e.getValue());
                            } else {
                                sender.sendMessage(ChatColor.AQUA + String.valueOf(e.getKey()) + ": " + ChatColor.YELLOW + e.getValue());
                            }
                        });
                        if(forgeMap.size() > 0) {
                            sender.sendMessage(ChatColor.YELLOW + "Forge versions:");
                            forgeMap.entrySet().stream().sorted(Collections.reverseOrder(Comparator.comparingInt(Map.Entry::getKey))).forEach(e -> {
                                ProtocolVersion version = ProtocolVersion.getVersion(e.getKey());
                                if (version != ProtocolVersion.UNKNOWN) {
                                    sender.sendMessage(ChatColor.AQUA + version.toString() + ": " + ChatColor.YELLOW + e.getValue());
                                } else {
                                    sender.sendMessage(ChatColor.AQUA + String.valueOf(e.getKey()) + ": " + ChatColor.YELLOW + e.getValue());
                                }
                            });
                        }
                        if (modsMap.size() > 0) {
                            sender.sendMessage(ChatColor.YELLOW + "Mods:");
                            modsMap.entrySet().stream().sorted(Collections.reverseOrder(Comparator.comparingInt(Map.Entry::getValue)))
                                    .forEach(e -> sender.sendMessage(ChatColor.AQUA + e.getKey() + ": " + ChatColor.YELLOW + e.getValue()));
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

                    players.sort(Collections.reverseOrder(Comparator.comparingInt(plugin::getVersion)));

                    for(ProxiedPlayer player : players) {
                        int rawVersion = plugin.getVersion(player);
                        sender.sendMessage(ChatColor.AQUA + player.getName() + ChatColor.YELLOW + ": " + ProtocolVersion.getVersion(rawVersion) + "/" + rawVersion + "/forge: " + plugin.isForge(player) + "/mods: " + player.getModList().entrySet().stream().map(e -> e.getKey() + "(" + e.getValue() + ")").collect(Collectors.joining(", ")));
                    }
                }
            } else if("config".equalsIgnoreCase(args[0])) {
                if(!sender.hasPermission(getPermission() + ".config")) {
                    sender.sendMessage(ChatColor.RED + "You don't have the permission " + getPermission() + ".config");
                    return;
                }
                sender.sendMessage(ChatColor.AQUA+ "Debug: " + ChatColor.YELLOW + plugin.isDebug());
                sender.sendMessage(ChatColor.AQUA+ "Start balancing: " + ChatColor.YELLOW + plugin.getStartBalancing());

                for (Map.Entry<String, ConnectorInfo> entry : plugin.getConnectorMap().entrySet()) {
                    sender.sendMessage(ChatColor.YELLOW + entry.getKey() + " configuration:");

                    if (entry.getValue().getVanillaMap().isEmpty()) {
                        sender.sendMessage(ChatColor.AQUA + "  No versions config.");
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "  Versions:");
                        for (Map.Entry<Integer, List<ServerInfo>> versionEntry : entry.getValue().getVanillaMap().entrySet()) {
                            ProtocolVersion protocolVersion = ProtocolVersion.getVersion(versionEntry.getKey());
                            sender.sendMessage(ChatColor.AQUA + "    " + (protocolVersion != ProtocolVersion.UNKNOWN ? protocolVersion : versionEntry.getKey()) + ": "
                                    + ChatColor.YELLOW + versionEntry.getValue().stream().map(ServerInfo::getName).collect(Collectors.joining(", ")));
                        }
                    }

                    if (entry.getValue().getForgeMap().isEmpty()) {
                        sender.sendMessage(ChatColor.AQUA + "  No forge config.");
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "  Forge:");
                        for (Map.Entry<Integer, List<ServerInfo>> versionEntry : entry.getValue().getForgeMap().entrySet()) {
                            ProtocolVersion protocolVersion = ProtocolVersion.getVersion(versionEntry.getKey());
                            sender.sendMessage(ChatColor.AQUA + "    " + (protocolVersion != ProtocolVersion.UNKNOWN ? protocolVersion : versionEntry.getKey()) + ": "
                                    + ChatColor.YELLOW + versionEntry.getValue().stream().map(ServerInfo::getName).collect(Collectors.joining(", ")));
                        }
                    }

                    if (entry.getValue().getModMap().isEmpty()) {
                        sender.sendMessage(ChatColor.AQUA + "  No mods config.");
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "  Mods:");
                        for (Map.Entry<String[], List<ServerInfo>> modEntry : entry.getValue().getModMap().entrySet()) {
                            sender.sendMessage(ChatColor.AQUA + "    " + String.join(",", modEntry.getKey()) + ": "
                                    + ChatColor.YELLOW + modEntry.getValue().stream().map(ServerInfo::getName).collect(Collectors.joining(", ")));
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
