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
import net.md_5.bungee.protocol.OverflowPacketException;
import us.myles.ViaVersion.api.Via;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
            connectorMap = new HashMap<>();

            // Legacy config
            loadConnectorInfo(
                    loadVersionMap(getConfig().getSection("versions")),
                    loadVersionMap(getConfig().getSection("forge"))
            );

            Configuration serversSection = getConfig().getSection("servers");
            for (String key : serversSection.getKeys()) {
                ConnectorInfo connectorInfo = loadConnectorInfo(
                        loadVersionMap(serversSection.getSection(key + ".versions")),
                        loadVersionMap(serversSection.getSection(key + ".forge"))
                );
                if (getProxy().getServerInfo(key) != null) {
                    connectorMap.put(key.toLowerCase(), connectorInfo);
                }
            }

            return true;
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Can't load plugin config!");
            e.printStackTrace();
        }
        return false;
    }

    private ConnectorInfo loadConnectorInfo(SortedMap<Integer, List<ServerInfo>> versions, SortedMap<Integer, List<ServerInfo>> forge) {
        ConnectorInfo connectorInfo = new ConnectorInfo(versions, forge);

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
                try {
                    String getVersion = versionStr.toUpperCase().replace('.', '_');
                    if (!getVersion.startsWith("MINECRAFT_")) {
                        getVersion = "MINECRAFT_" + getVersion;
                    }
                    rawVersion = ProtocolVersion.valueOf(getVersion).toInt();
                } catch (IllegalArgumentException e1) {
                    getLogger().warning(versionStr + " is neither a valid Integer nor a string representation of a major protocol version?");
                    continue;
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

    @EventHandler
    public void onPlayerConnect(ServerConnectEvent e) {
        if (!isEnabled() || e.isCancelled()) {
            return;
        }
        int version = getVersion(e.getPlayer());
        boolean isForge = isForge(e.getPlayer());

        logDebug(e.getPlayer().getName() + "'s version: " + version + " (" + ProtocolVersion.getVersion(version) + ")/forge: " + isForge);

        ServerInfo targetServer = getTargetServer(e.getTarget(), version, isForge);
        if (targetServer != null) {
            e.setTarget(targetServer);
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
            } catch (IndexOutOfBoundsException | OverflowPacketException e) {
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

                    ServerInfo targetServer = getTargetServer(p.getServer().getInfo(), version, true);
                    if (targetServer != null && targetServer != p.getServer().getInfo()) {
                        p.connect(targetServer);
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

    /**
     * Calculate the target server
     * @param targetServer The server that is being connected to
     * @param version      The protocol version of the player'client
     * @param isForge      Whether or not the player is using a forge client
     * @return The server that the player should be connecting to or <tt>null</tt> if it shouldn't be changed at all
     */
    private ServerInfo getTargetServer(ServerInfo targetServer, int version, boolean isForge) {
        ConnectorInfo connectorInfo = connectorMap.get(targetServer.getName().toLowerCase());
        if (connectorInfo == null) {
            logDebug("Server " + targetServer.getName() + " does not have any special connection info set");
            return null;
        }

        List<ServerInfo> serverList = connectorInfo.getServers(version, isForge);
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
        logDebug("Selected server " + (server != null ? server.getName() : "null") + " for " + targetServer.getName() + "/" + version + "/forge: " + isForge + " from " + serverList.stream().map(ServerInfo::getName).collect(Collectors.joining(",")));
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
