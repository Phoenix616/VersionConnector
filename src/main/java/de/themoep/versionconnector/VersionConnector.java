package de.themoep.versionconnector;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.ViaAPI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

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

    public void onEnable() {
        enabled = loadConfig();
        if(enabled) {
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
                loadConnectorInfo(
                        loadVersionMap(serversSection.getSection(key + ".versions")),
                        loadVersionMap(serversSection.getSection(key + ".forge"))
                );
            }

            return true;
        } catch(IOException e) {
            getLogger().log(Level.SEVERE, "Can't load plugin config!");
            e.printStackTrace();
        }
        return false;
    }

    private void loadConnectorInfo(Map<Integer, List<ServerInfo>> versions, Map<Integer, List<ServerInfo>> forge) {
        ConnectorInfo connectorInfo = new ConnectorInfo(versions, forge);

        for (ServerInfo server : connectorInfo.getServers()) {
            connectorMap.put(server.getName().toLowerCase(), connectorInfo);
        }
    }

    private Map<Integer, List<ServerInfo>> loadVersionMap(Configuration section) {
        Map<Integer, List<ServerInfo>> map = new HashMap<>();
        if(section == null) {
            return map;
        }
        for(String versionStr : section.getKeys()) {
            int rawVersion;
            try {
                rawVersion = ProtocolVersion.valueOf(versionStr.toUpperCase()).toInt();
            } catch(IllegalArgumentException  e1) {
                try {
                    rawVersion = Integer.parseInt(versionStr);
                } catch(NumberFormatException e2) {
                    getLogger().warning(versionStr + " is neither a valid Integer nor a string representation of a major protocol version?");
                    continue;
                }
            }
            String serverStr = section.getString(versionStr, null);
            if(serverStr != null && !serverStr.isEmpty()) {
                String[] serverNames = serverStr.split(",");
                List<ServerInfo> serverList = new ArrayList<ServerInfo>();
                for(String serverName : serverNames) {
                    ServerInfo server = getProxy().getServerInfo(serverName.trim());
                    if(server != null) {
                        serverList.add(server);
                    } else {
                        getLogger().warning(serverStr + " is defined for version " + rawVersion + "/" + versionStr + " but there is no server with that name?");
                    }
                }
                if(!serverList.isEmpty()) {
                    map.put(rawVersion, serverList);
                }
            }
        }
        return map;
    }

    @EventHandler
    public void onPlayerConnect(ServerConnectEvent e) {
        if(!isEnabled() || e.isCancelled()) {
            return;
        }
        int rawVersion = getVersion(e.getPlayer());
        ProtocolVersion version = ProtocolVersion.getVersion(rawVersion);

        logDebug(e.getPlayer().getName() + "'s version: " + rawVersion + "/" + version + "/forge: " + e.getPlayer().isForgeUser());

        ServerInfo targetServer = getTargetServer(e.getTarget(), rawVersion, version, e.getPlayer().isForgeUser());
        if(targetServer != null) {
            e.setTarget(targetServer);
        }
    }

    private int getVersion(ProxiedPlayer player) {
        if (isViaVersionAvailable) {
            return Via.getAPI().getPlayerVersion(player.getUniqueId());
        }

        return player.getPendingConnection().getVersion();
    }

    /**
     * Calculate the target server
     * @param targetServer  The server that is being connected to
     * @param rawVersion    The raw version of the player'client
     * @param version       The protocol version of the player's client
     * @param isForge       Whether or not the player is using a forge client
     * @return              The server that the player should be connecting to or <tt>null</tt> if it shouldn't be changed at all
     */
    private ServerInfo getTargetServer(ServerInfo targetServer, int rawVersion, ProtocolVersion version, boolean isForge) {
        ConnectorInfo connectorInfo = connectorMap.get(targetServer.getName().toLowerCase());
        if (connectorInfo == null) {
            return null;
        }

        List<ServerInfo> serverList = connectorInfo.getServers(rawVersion, version, isForge);
        if (serverList == null || serverList.isEmpty() // No servers configured for that version
                || startBalancing < 0 && serverList.contains(targetServer)) { // No need to balance and the target is already in the list
            return null;
        }

        ServerInfo server = null;
        for(ServerInfo testedServer : serverList) {
            if(server == null || startBalancing > -1 && server.getPlayers().size() >= startBalancing && testedServer.getPlayers().size() < server.getPlayers().size()) {
                server = testedServer;
            }
        }
        return server;
    }

    public void logDebug(String msg) {
        if(debug)
            getLogger().log(Level.INFO, msg);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Configuration getConfig() {
        return config.getConfiguration();
    }
}
