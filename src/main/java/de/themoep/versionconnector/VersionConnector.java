package de.themoep.versionconnector;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
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

    public void onEnable() {
        try {
            config = new FileConfiguration(this, new File(getDataFolder(), "config.yml"));
            enabled = true;
            getProxy().getPluginManager().registerListener(this, this);
        } catch(IOException e) {
            getLogger().log(Level.SEVERE, "Can't load plugin config!");
        }
    }

    @EventHandler
    public void onPlayerConnect(ServerConnectEvent e) {
        if(!enabled || e.getPlayer().getServer() != null) {
            return;
        }
        int rawVersion = e.getPlayer().getPendingConnection().getVersion();
        ProtocolVersion version = ProtocolVersion.getVersion(rawVersion);

        getLogger().log(Level.INFO, e.getPlayer().getName() + "'s version: " + rawVersion + "/" + version);

        String serverName = getConfig().getString("versions." + rawVersion, null);
        if(serverName == null) {
            serverName = getConfig().getString("versions." + version, null);
        }
        if(serverName != null) {
            ServerInfo server = getProxy().getServerInfo(serverName);
            if(server != null) {
                e.setTarget(server);
            } else {
                getLogger().warning(serverName + " is defined for version " + rawVersion + "/" + version + " but there is no server with that name?");
            }
        }

    }

    public boolean isEnabled() {
        return enabled;
    }

    public Configuration getConfig() {
        return config.getConfiguration();
    }
}
