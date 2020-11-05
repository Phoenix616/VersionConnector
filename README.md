# VersionConnector
Bungee plugin to connect different Minecraft client versions to different servers on join or server switch. Includes simple load balancing and Forge switch. (Forge can only be detected with 1.8-1.13 clients!)

Development builds can be found on the [Minebench](https://www.minebench.de) Jenkins as usual: http://ci.minebench.de/job/VersionConnector/

## Versions directly supported:

The plugin contains some constants which can be used to specify a version string instead of the protocol version. See [this class](https://github.com/Minebench/VersionConnector/blob/master/src/main/java/de/themoep/versionconnector/ProtocolVersion.java) for a full list of them.

It will fallback to the version with the closest protocol number below the actual client's protocol. You can however set the protocol version directly if you want or submit additions to the [ProtocolVersion](https://github.com/Minebench/VersionConnector/blob/master/src/main/java/de/themoep/versionconnector/ProtocolVersion.java) enum.

## Config:

``` yaml
debug: false
# Minimum amount of players that need to be online on one server to start balancing
# new players to the other server (e.g. between lobby_1_8_a & lobby_1_8_b)
start-balancing: 0
servers:
  lobby:
    versions:
      '34': lobby_prot_34 # Lobby for specific protocol version
      '1_8': lobby_1_8_a, lobby_1_8_b # Lobbies for 1.8
      '1_9': lobby_1_9 # Lobby for 1.9
      UNKNOWN: well_we_dont_know # Lobby for an Unknown version (not a fallback if no config for version was found!)
    forge:
      '1_9': forge_lobby_1_9
      '1_8': forge_lobby_1_8_a, forge_lobby_1_8_b
  survival:
    versions:
      '1_8': survival_1_8
      '1_10': survival_1_10
      UNKNOWN: survival_wat
    forge:
      '1_9': forge_suvival_1_9
      '1_8': forge_suvival_1_8_a, forge_suvival_1_8_b
```
