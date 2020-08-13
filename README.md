# VersionConnector
Bungee plugin to connect different Minecraft client versions to different servers on join or server switch. Includes simple load balancing and Forge switch. (Forge can only be detected with 1.8-1.13 clients!)

Development builds can be found on the [Minebench](https://www.minebench.de) Jenkins as usual: http://ci.minebench.de/job/VersionConnector/

## Versions directly supported:

It will fallback to the version with the closest protocol number below the actual client's protocol. You can however set the protocol version directly if you want or submit additions to the [ProtocolVersion](https://github.com/Minebench/VersionConnector/blob/master/src/main/java/de/themoep/versionconnector/ProtocolVersion.java) enum.

- 1.16.2 (Protocol version 751)
- 1.16.1 (Protocol version 736)
- 1.16 (Protocol version 735)
- 1.15.2 (Protocol version 578)
- 1.15.1 (Protocol version 575)
- 1.15 (Protocol version 573)
- 1.14.4 (Protocol version 498)
- 1.14.3 (Protocol version 490)
- 1.14.2 (Protocol version 485)
- 1.14.1 (Protocol version 480)
- 1.14 (Protocol version 477)
- 1.13.2 (Protocol version 404)
- 1.13.1 (Protocol version 401)
- 1.13 (Protocol version 393)
- 1.12.2 (Protocol version 340)
- 1.12.1 (Protocol version 338)
- 1.12 (Protocol version 335)
- 1.11.2 (Protocol version 316)
- 1.11.1 (Protocol version 316)
- 1.11 (Protocol version 315)
- 1.10 (Protocol version 210)
- 1.9.4 (Protocol version 110)
- 1.9 (Protocol version 107)
- 1.8 (Protocol version 47)
- 1.7.6 (Protocol version 5)
- 1.7.2 (Protocol version 4)

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
      '1.8': lobby_1_8_a, lobby_1_8_b # Lobbies for 1.8
      '1.9': lobby_1_9 # Lobby for 1.9
      UNKNOWN: well_we_dont_know # Lobby for an Unknown version (not a fallback if no config for version was found!)
    forge:
      '1.9': forge_lobby_1_9
      '1.8': forge_lobby_1_8_a, forge_lobby_1_8_b
  survival:
    versions:
      '1.8': survival_1_8
      '1.10': survival_1_10
      UNKNOWN: survival_wat
    forge:
      '1.9': forge_suvival_1_9
      '1.8': forge_suvival_1_8_a, forge_suvival_1_8_b
```
