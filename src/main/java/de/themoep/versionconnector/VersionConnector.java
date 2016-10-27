package de.themoep.versionconnector;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

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

public class VersionConnector extends Plugin implements Listener {

    FileConfiguration config;
    private boolean enabled = false;
    private boolean debug = false;

    private Map<Integer, List<ServerInfo>> versionMap;
    private Map<Integer, List<ServerInfo>> forgeMap;

    public void onEnable() {
        enabled = loadConfig();
        if(enabled) {
            getProxy().getPluginManager().registerListener(this, this);
        }
        getProxy().getPluginManager().registerCommand(this, new VersionConnectorCommand(this));
    }

    public boolean loadConfig() {
        try {
            config = new FileConfiguration(this, "config.yml");
            debug = getConfig().getBoolean("debug", true);
            versionMap = loadVersionMap(getConfig().getSection("versions"));
            forgeMap = loadVersionMap(getConfig().getSection("forge"));

            return true;
        } catch(IOException e) {
            getLogger().log(Level.SEVERE, "Can't load plugin config!");
            e.printStackTrace();
        }
        return false;
    }

    private Map<Integer, List<ServerInfo>> loadVersionMap(Configuration section) {
        Map<Integer, List<ServerInfo>> map = new HashMap<>();
        if(section == null) {
            return map;
        }
        for(String versionStr : section.getKeys()) {
            int rawVersion;
            try {
                rawVersion = ProtocolVersion.valueOf(versionStr).toInt();
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
        if(!enabled || e.getPlayer().getServer() != null) {
            return;
        }
        int rawVersion = e.getPlayer().getPendingConnection().getVersion();
        ProtocolVersion version = ProtocolVersion.getVersion(rawVersion);

        logDebug(e.getPlayer().getName() + "'s version: " + rawVersion + "/" + version + "/forge: " + e.getPlayer().isForgeUser());

        ServerInfo targetServer = getTargetServer(rawVersion, version, e.getPlayer().isForgeUser());
        if(targetServer != null) {
            e.setTarget(targetServer);
        }

    }

    private ServerInfo getTargetServer(int rawVersion, ProtocolVersion version, boolean forgeUser) {
        ServerInfo server = null;

        Map<Integer, List<ServerInfo>> map = forgeUser ? forgeMap : versionMap;
        List<ServerInfo> serverList = map.get(rawVersion);
        if(serverList == null || serverList.isEmpty()) {
            serverList = map.get(version.toInt());
        }
        if(serverList != null && !serverList.isEmpty()) {
            for(ServerInfo testServer : serverList) {
                if(server == null || testServer.getPlayers().size() < server.getPlayers().size()) {
                    server = testServer;
                }
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
