package de.themoep.versionconnector;

import de.themoep.bungeeplugin.FileConfiguration;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;
import us.myles.ViaVersion.api.Via;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
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

public class VersionConnector extends Plugin implements Listener {

    private FileConfiguration config;
    private boolean enabled = false;
    private boolean debug = false;
    private int startBalancing = 0;

    private boolean isViaVersionAvailable = false;

    private Map<String, ConnectorInfo> joinConnectorMap;
    private Map<String, ConnectorInfo> connectorMap;

    private Map<UUID, Boolean> isForgeMap = new ConcurrentHashMap<>();

    public void onEnable() {
        enabled = loadConfig();
        if (enabled) {
            getProxy().getPluginManager().registerListener(this, this);
        }
        getProxy().getPluginManager().registerCommand(this, new VersionConnectorCommand(this));
        isViaVersionAvailable = getProxy().getPluginManager().getPlugin("ViaVersion") != null;
    }

    public boolean loadConfig() {
        try {
            config = new FileConfiguration(this, "config.yml");
            debug = getConfig().getBoolean("debug", true);
            startBalancing = getConfig().getInt("start-balancing", 0);
            joinConnectorMap = new HashMap<>();
            connectorMap = new HashMap<>();

            if (getConfig().contains("join")) {
                Configuration joinSection = getConfig().getSection("join");
                for (String key : joinSection.getKeys()) {
                    ConnectorInfo connectorInfo = loadConnectorInfo(
                            joinConnectorMap,
                            loadVersionMap(joinSection.getSection(key + ".versions")),
                            loadVersionMap(joinSection.getSection(key + ".forge")),
                            loadModsMap(joinSection.getSection(key + ".mods")));
                    if (getProxy().getServerInfo(key) != null) {
                        joinConnectorMap.put(key.toLowerCase(), connectorInfo);
                    }
                }
            }

            // Legacy config
            loadConnectorInfo(
                    connectorMap,
                    loadVersionMap(getConfig().getSection("versions")),
                    loadVersionMap(getConfig().getSection("forge")),
                    loadModsMap(getConfig().getSection("mods")));

            if (getConfig().contains("servers")) {
                Configuration serversSection = getConfig().getSection("servers");
                for (String key : serversSection.getKeys()) {
                    ConnectorInfo connectorInfo = loadConnectorInfo(
                            connectorMap,
                            loadVersionMap(serversSection.getSection(key + ".versions")),
                            loadVersionMap(serversSection.getSection(key + ".forge")),
                            loadModsMap(serversSection.getSection(key + ".mods"))
                    );
                    if (getProxy().getServerInfo(key) != null) {
                        connectorMap.put(key.toLowerCase(), connectorInfo);
                    }
                }
            }

            return true;
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Can't load plugin config!");
            e.printStackTrace();
        }
        return false;
    }

    private ConnectorInfo loadConnectorInfo(Map<String, ConnectorInfo> connectorMap, SortedMap<Integer, List<ServerInfo>> versions, SortedMap<Integer, List<ServerInfo>> forge, Map<String[], List<ServerInfo>> mods) {
        ConnectorInfo connectorInfo = new ConnectorInfo(versions, forge, mods);

        for (ServerInfo server : connectorInfo.getServers()) {
            connectorMap.putIfAbsent(server.getName().toLowerCase(), connectorInfo); // Legacy server selection
        }

        return connectorInfo;
    }

    private SortedMap<Integer, List<ServerInfo>> loadVersionMap(Configuration section) {
        SortedMap<Integer, List<ServerInfo>> map = new TreeMap<>();
        if (section == null) {
            return map;
        }
        for (String versionStr : section.getKeys()) {
            int rawVersion;
            try {
                rawVersion = Integer.parseInt(versionStr);
            } catch (NumberFormatException e2) {
                String getVersion = versionStr.toUpperCase().replace('.', '_');
                if (!getVersion.startsWith("MINECRAFT_")) {
                    getVersion = "MINECRAFT_" + getVersion;
                }
                try {
                    rawVersion = ProtocolVersion.valueOf(getVersion).toInt();
                } catch (IllegalArgumentException e1) {
                    try {
                        rawVersion = (int) ProtocolConstants.class.getField(getVersion).get(null);
                    } catch (IllegalAccessException | NoSuchFieldException e) {
                        getLogger().warning(versionStr + " is neither a valid Integer nor a string representation of a major protocol version?");
                        continue;
                    }
                }
            }
            String serverStr = section.getString(versionStr, null);
            if (serverStr != null && !serverStr.isEmpty()) {
                String[] serverNames = serverStr.split(",");
                List<ServerInfo> serverList = new ArrayList<>();
                for (String serverName : serverNames) {
                    ServerInfo server = getProxy().getServerInfo(serverName.trim());
                    if (server != null) {
                        serverList.add(server);
                    } else {
                        getLogger().warning(serverStr + " is defined for version " + rawVersion + "/" + versionStr + " but there is no server with that name?");
                    }
                }
                if (!serverList.isEmpty()) {
                    map.put(rawVersion, serverList);
                }
            }
        }
        return map;
    }

    private Map<String[], List<ServerInfo>> loadModsMap(Configuration section) {
        Map<String[], List<ServerInfo>> map = new LinkedHashMap<>();
        if (section == null) {
            return map;
        }
        for (String modsStr : section.getKeys()) {
            String[] mods = modsStr.split(",");
            String serverStr = section.getString(modsStr, null);
            if (serverStr != null && !serverStr.isEmpty()) {
                String[] serverNames = serverStr.split(",");
                List<ServerInfo> serverList = new ArrayList<>();
                for (String serverName : serverNames) {
                    ServerInfo server = getProxy().getServerInfo(serverName.trim());
                    if (server != null) {
                        serverList.add(server);
                    } else {
                        getLogger().warning(serverStr + " is defined for mods " + modsStr + " but there is no server with that name?");
                    }
                }
                if (!serverList.isEmpty()) {
                    map.put(mods, serverList);
                }
            }
        }
        return map;
    }

    @EventHandler
    public void onPlayerConnect(ServerConnectEvent e) {
        if (!isEnabled() || e.isCancelled()) {
            return;
        }
        int version = getVersion(e.getPlayer());
        boolean isForge = isForge(e.getPlayer());
        Map<String, String> modList = e.getPlayer().getModList();

        logDebug(e.getPlayer().getName() + "'s version: " + version + " (" + ProtocolVersion.getVersion(version) + ")/forge: " + isForge + "/mods: " + modList.size() + "/join: " + (e.getPlayer().getServer() == null));

        ConnectorInfo connectorInfo = null;
        if (e.getPlayer().getServer() == null) {
            connectorInfo = joinConnectorMap.get(e.getTarget().getName().toLowerCase());
        }

        if (connectorInfo == null) {
            connectorInfo = connectorMap.get(e.getTarget().getName().toLowerCase());
        }

        if (connectorInfo != null) {
            ServerInfo targetServer = getTargetServer(connectorInfo, e.getTarget(), version, isForge, modList);
            if (targetServer != null) {
                e.setTarget(targetServer);
            }
        } else {
            logDebug("Server " + e.getTarget().getName() + " does not have any special connection info set");
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        isForgeMap.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getSender() instanceof ProxiedPlayer
                && !((ProxiedPlayer) event.getSender()).isForgeUser()
                && event.getTag().equals("minecraft:brand")
                && event.getData().length > 0) {
            ByteBuf in = Unpooled.wrappedBuffer(event.getData());
            String brand = "";
            try {
                brand = DefinedPacket.readString(in);
            } catch (Exception e) {
                logDebug("Invalid brand data sent! (length: " + event.getData().length + ") " + e.getMessage());
            }
            in.release();
            if (brand.equals("forge")) {
                ProxiedPlayer p = (ProxiedPlayer) event.getSender();
                isForgeMap.put(p.getUniqueId(), true);
                if (p.getServer() != null) {
                    // Player already connected :S Try to move them to the more correct server if it exists
                    int version = getVersion(p);

                    logDebug(p.getName() + "'s version: " + version + " (" + ProtocolVersion.getVersion(version) + ")/forge: " + true);

                    ConnectorInfo connectorInfo = connectorMap.get(p.getServer().getInfo().getName().toLowerCase());
                    if (connectorInfo != null) {
                        ServerInfo targetServer = getTargetServer(connectorInfo, p.getServer().getInfo(), version, true, ((ProxiedPlayer) event.getSender()).getModList());
                        if (targetServer != null && targetServer != p.getServer().getInfo()) {
                            p.connect(targetServer);
                        }
                    }
                }
            }
        }
    }

    public int getVersion(ProxiedPlayer player) {
        if (isViaVersionAvailable) {
            return Via.getAPI().getPlayerVersion(player.getUniqueId());
        }

        return player.getPendingConnection().getVersion();
    }

    public boolean isForge(ProxiedPlayer player) {
        return player.isForgeUser() || isForgeMap.getOrDefault(player.getUniqueId(), false);
    }

    public Map<String, String> getMods(ProxiedPlayer player) {
        return player.getModList();
    }

    /**
     * Calculate the target server
     * @param connectorInfo The ConnectorInfo to get the target server from
     * @param targetServer  The server that is being connected to
     * @param version       The protocol version of the player'client
     * @param isForge       Whether or not the player is using a forge client
     * @param modList
     * @return The server that the player should be connecting to or <tt>null</tt> if it shouldn't be changed at all
     */
    private ServerInfo getTargetServer(ConnectorInfo connectorInfo, ServerInfo targetServer, int version, boolean isForge, Map<String, String> modList) {
        List<ServerInfo> serverList = connectorInfo.getServers(version, isForge, modList);
        if (serverList == null || serverList.isEmpty() // No servers configured for that version
                || startBalancing < 0 && serverList.contains(targetServer)) { // No need to balance and the target is already in the list
            logDebug("No servers found for " + targetServer.getName() + "/" + version + "/forge: " + isForge);
            return null;
        }

        ServerInfo server = null;
        for (ServerInfo testedServer : serverList) {
            if (server == null || startBalancing > -1 && server.getPlayers().size() >= startBalancing && testedServer.getPlayers().size() < server.getPlayers().size()) {
                server = testedServer;
            }
        }
        logDebug("Selected server " + (server != null ? server.getName() : "null") + " for " + targetServer.getName() + "/" + version + "/forge: " + isForge + "/mods: " + modList.size() + " from " + serverList.stream().map(ServerInfo::getName).collect(Collectors.joining(",")));
        return server;
    }

    public void logDebug(String msg) {
        if (debug)
            getLogger().log(Level.INFO, msg);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Configuration getConfig() {
        return config.getConfiguration();
    }

    public boolean isDebug() {
        return debug;
    }

    public Map<String, ConnectorInfo> getConnectorMap() {
        return connectorMap;
    }

    public int getStartBalancing() {
        return startBalancing;
    }
}
