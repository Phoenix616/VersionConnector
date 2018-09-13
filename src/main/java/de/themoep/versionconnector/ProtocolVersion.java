package de.themoep.versionconnector;

import java.util.LinkedHashMap;
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

public enum ProtocolVersion {
    MINECRAFT_1_13_1(401),
    MINECRAFT_1_13(393),
    MINECRAFT_1_12_2(340),
    MINECRAFT_1_12_1(338),
    MINECRAFT_1_12(335),
    MINECRAFT_1_11_2(316),
    MINECRAFT_1_11_1(316),
    MINECRAFT_1_11(315),
    MINECRAFT_1_10(210),
    MINECRAFT_1_9_4(110),
    MINECRAFT_1_9_2(109),
    MINECRAFT_1_9_1(108),
    MINECRAFT_1_9(107),
    MINECRAFT_1_8(47),
    MINECRAFT_1_7_6(5),
    MINECRAFT_1_7_2(4),
    UNKNOWN(0);

    private final int number;
    private static Map<Integer, ProtocolVersion> numbers;

    static {
        numbers = new LinkedHashMap<>();
        for(ProtocolVersion version : values()) {
            numbers.put(version.number, version);
        }
    }

    ProtocolVersion(int versionNumber) {
        this.number = versionNumber;
    }

    public static ProtocolVersion getVersion(int versionNumber) {
        ProtocolVersion protocolVersion = numbers.get(versionNumber);
        if (protocolVersion != null) {
            return protocolVersion;
        }
        for(ProtocolVersion version : values()) {
            if(version.toInt() <= versionNumber) {
                return version;
            }
        }
        return UNKNOWN;
    }

    public int toInt() {
        return number;
    }
}
