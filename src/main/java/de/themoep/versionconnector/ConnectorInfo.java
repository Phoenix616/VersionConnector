package de.themoep.versionconnector;

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

import net.md_5.bungee.api.config.ServerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConnectorInfo {

    private final Map<Integer, List<ServerInfo>> vanillaMap;
    private final Map<Integer, List<ServerInfo>> forgeMap;

    private final List<ServerInfo> serverList;

    public ConnectorInfo(Map<Integer, List<ServerInfo>> vanillaMap, Map<Integer, List<ServerInfo>> forgeMap) {
        this.vanillaMap = vanillaMap;
        this.forgeMap = forgeMap;

        serverList = new ArrayList<>();

        vanillaMap.values().forEach(serverList::addAll);
        forgeMap.values().forEach(serverList::addAll);
    }

    public Map<Integer, List<ServerInfo>> getVanillaMap() {
        return vanillaMap;
    }

    public Map<Integer, List<ServerInfo>> getForgeMap() {
        return forgeMap;
    }

    public List<ServerInfo> getServers() {
        return serverList;
    }

    /**
     * Calculate the server to connect to when connecting to this connector based on the version and player count
     * @param rawVersion        The raw version of the player'client
     * @param version           The protocol version of the player's client
     * @param isForge           Whether or not the player is using a forge client
     * @param startBalancing    The emount of players on the server before we try to balance onto another one (if configured)
     * @return                  The server that the player should be connecting to or <tt>null</tt> if he shouldn't connect to any of them
     */
    public ServerInfo getTargetServer(int rawVersion, ProtocolVersion version, boolean isForge, int startBalancing) {
        ServerInfo server = null;

        Map<Integer, List<ServerInfo>> map = isForge ? forgeMap : vanillaMap;
        List<ServerInfo> serverList = map.get(rawVersion);
        if(serverList == null || serverList.isEmpty()) {
            serverList = map.get(version.toInt());
        }

        if(serverList != null && !serverList.isEmpty()) {
            for(ServerInfo testServer : serverList) {
                if(server == null || server.getPlayers().size() >= startBalancing && testServer.getPlayers().size() < server.getPlayers().size()) {
                    server = testServer;
                }
            }
        }
        return server;
    }
}
