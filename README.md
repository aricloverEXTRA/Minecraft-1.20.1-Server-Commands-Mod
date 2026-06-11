# Survival Commands — Fabric Mod for Minecraft 1.20.1

A clean, lightweight Fabric mod adding friendly survival commands.

## Commands

| Command | Description |
|---|---|
| `/spawn` | Teleport to world spawn |
| `/sethome` | Save your current location as home |
| `/home` | Teleport to your saved home |
| `/follow <player>` | Send a follow request to a player (tab-complete works!) |
| `/followaccept` | Accept an incoming follow request |
| `/followdeny` | Deny an incoming follow request |
| `/unfollow` | Stop following / stop being followed |
| `/menu` | Show clickable command list in chat |

## Building

### Requirements
- Java 17+
- Internet access (downloads Gradle, Fabric Loom, Minecraft mappings on first run)

### Steps

1. **Install Gradle 8.8+** from https://gradle.org/install/ (or use SDKMAN)
2. Clone / place this folder somewhere
3. Run:

```bash
cd survivalcommands
./gradlew build
```

4. The built `.jar` will be at:
   `build/libs/survivalcommands-1.0.0.jar`

5. Drop it in your server's `mods/` folder alongside `fabric-api-*.jar`

### First-time setup
Gradle will automatically download:
- Gradle itself (~150MB)
- Fabric Loom plugin
- Minecraft 1.20.1 (obfuscated jar + mappings)
- Fabric API

This may take 5–10 minutes on first run.

## Notes
- Homes are saved to `<world>/data/survivalcmds_homes.dat` and persist across restarts
- Follow sessions are cleared on server restart (in-memory only)
- Works server-side only — no client mod needed
