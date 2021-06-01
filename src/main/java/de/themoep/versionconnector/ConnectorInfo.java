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
import java.util.SortedMap;

public class ConnectorInfo {

    private final SortedMap<Integer, List<ServerInfo>> vanillaMap;
    private final SortedMap<Integer, List<ServerInfo>> forgeMap;
    private final Map<String[], List<ServerInfo>> modMap;

    private final List<ServerInfo> serverList;

    public ConnectorInfo(SortedMap<Integer, List<ServerInfo>> vanillaMap, SortedMap<Integer, List<ServerInfo>> forgeMap, Map<String[], List<ServerInfo>> modMap) {
        this.vanillaMap = vanillaMap;
        this.forgeMap = forgeMap;
        this.modMap = modMap;

        serverList = new ArrayList<>();

        vanillaMap.values().forEach(serverList::addAll);
        forgeMap.values().forEach(serverList::addAll);
    }

    public SortedMap<Integer, List<ServerInfo>> getVanillaMap() {
        return vanillaMap;
    }

    public SortedMap<Integer, List<ServerInfo>> getForgeMap() {
        return forgeMap;
    }

    public Map<String[], List<ServerInfo>> getModMap() {
        return modMap;
    }

    /**
     * Get the list of servers that matches the inputted parameters
     * @param version       The protocol version of the player's client
     * @param isForge       Whether or not the player is using a forge client
     * @param modList
     * @return The list of servers that matches the inputted parameters
     */
    public List<ServerInfo> getServers(int version, boolean isForge, Map<String, String> modList) {
        if (!modList.isEmpty()) {
            List<ServerInfo> bestMatch = null;
            int matchedMods = 0;
            for (Map.Entry<String[], List<ServerInfo>> entry : modMap.entrySet()) {
                int matched = 0;
                for (String modName : entry.getKey()) {
                    if (modList.containsKey(modName)) {
                        matched++;
                    }
                }
                if (matched > matchedMods) {
                    bestMatch = entry.getValue();
                    matchedMods = matched;
                }
            }
            if (bestMatch != null) {
                return bestMatch;
            }
        }

        SortedMap<Integer, List<ServerInfo>> map = isForge ? forgeMap : vanillaMap;
        List<ServerInfo> serverList = map.get(version);
        if (serverList == null) {
            SortedMap<Integer, List<ServerInfo>> smallerVersions = map.headMap(version);
            if (!smallerVersions.isEmpty()) {
                serverList = map.get(smallerVersions.lastKey());
            }
        }
        return serverList;
    }

    public List<ServerInfo> getServers() {
        return serverList;
    }

}
