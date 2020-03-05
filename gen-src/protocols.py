import json
import requests
import sys

def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)

PROTOCOL_VERSIONS_URL = "https://raw.githubusercontent.com/PrismarineJS/minecraft-data/master/data/pc/common/protocolVersions.json"

protocol_versions = requests.get(PROTOCOL_VERSIONS_URL)
parsed = json.loads(protocol_versions.text)

generated = """package de.themoep.versionconnector;

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
    UNKNOWN(0),
"""

# The JSON file might have more than 1 entry of the same version.
# The easiest way to avoid it is to just use a set of versions.
versions = set()

for version in parsed:
    if not version["usesNetty"]:
        continue
    proto = version["version"]
    mc = version["minecraftVersion"].upper().replace('.', '_').replace('-', '_')
    if mc in versions:
        eprint(f"Found {mc} again; this might be an error looking into!")
        continue
    versions.add(mc)
    generated += f"""    MINECRAFT_{mc}({proto}),\n"""

generated += """    ;


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
        return UNKNOWN;
    }

    public static ProtocolVersion matchVersion(int versionNumber) {
        ProtocolVersion protocolVersion = getVersion(versionNumber);
        if (protocolVersion == UNKNOWN) {
            for (ProtocolVersion version : values()) {
                if (version.toInt() <= versionNumber) {
                    return version;
                }
            }
        }
        return protocolVersion;
    }

    public int toInt() {
        return number;
    }

    public String toString() {
        return name().replace("MINECRAFT_", "").replace("_", ".");
    }
}"""

print(generated)
