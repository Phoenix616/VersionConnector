# VersionConnector
Bungee plugin to connect different Minecraft client versions to different servers on join.

Development builds can be found on the [Minebench](https://www.minebench.de) Jenkins as usual: http://ci.minebench.de/job/VersionConnector/

## Versions directly supported:

It will fallback to the version with the closest protocol number below the actuall client's protocol. You can however set the protocol version directly if you want or submit additions to the [ProtocolVersion](https://github.com/Minebench/VersionConnector/blob/master/src/main/java/de/themoep/versionconnector/ProtocolVersion.java) enum.

- MINECRAFT_1_9 (Protcol version 107),
- MINECRAFT_1_8 (Protcol version 47),
- MINECRAFT_1_7_6 (Protcol version 5),
- MINECRAFT_1_7_2 (Protcol version 4)

## Config:

``` yaml
versions:
  '34': loby_prot_34 # Lobby for specific protocol version
  MINECRAFT_1_8: lobby_1_8 # Lobby for 1.8
  MINECRAFT_1_9: lobby_1_9 # Lobby for 1.9
  UNKNOWN: well_we_dont_know # Lobby for an Unknown version (not a fallback if no config for version was found!)
```