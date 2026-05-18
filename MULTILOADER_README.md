# Village Diplomacy - Multi-Loader Build Instructions

This mod is available for multiple Minecraft mod loaders. Each loader can be compiled independently.

## Current Status

### ✅ Forge (Primary - MC 1.20.1)
- **Status**: Working and Tested
- **How to Compile**: 
  ```bash
  ./gradlew build
  cp build/libs/villagediplomacy-*.jar "$MINECRAFT_MODS_DIR"
  ```
- **Output**: `villagediplomacy-1.1.0.jar`

### 🚀 Fabric (In Development - MC 1.20.1)
- **Status**: Build system configured, awaiting API adjustments
- **Structure**: Separate from Forge, uses Fabric Loader + Fabric API
- **Event Mapping**: Converted Forge events to Fabric event listeners
- **How to Compile** (when ready):
  ```bash
  cd fabric
  ../gradlew -p fabric build
  cp build/libs/villagediplomacy-fabric-*.jar "$MINECRAFT_MODS_DIR"
  ```

### 🔮 NeoForge (Planned - MC 1.20.1+)
- **Status**: Directory structure created
- **Note**: NeoForge is easier to port from Forge (similar API)
- **Timeline**: After Fabric is stable

### 🎨 Quilt (Planned - MC 1.20.1+)
- **Status**: Directory structure created
- **Note**: Quilt is based on Fabric, will share Fabric API adaptations
- **Timeline**: After Fabric is stable

## Folder Structure

```
VILLAGEDIPLOMACY/
├── src/                          # Forge mod (original)
│   └── main/java/...
├── fabric/                        # Fabric port
│   ├── src/main/java/...
│   ├── src/main/resources/
│   │   ├── fabric.mod.json       # Fabric mod metadata
│   │   └── assets/...            # Shared resources
│   └── build.gradle              # Fabric-specific config
├── neoforge/                      # NeoForge port (planned)
├── quilt/                         # Quilt port (planned)
├── build.gradle                   # Main Forge build config
├── gradle.properties              # Shared version info
└── gradlew                        # Gradle wrapper
```

## Version Information

- **Minecraft**: 1.20.1
- **Forge**: 47.4.10
- **Fabric Loader**: 0.15.11
- **Fabric API**: 0.91.1+1.20.1
- **Java**: 17+

## Development Notes

### Forge → Fabric Event Mapping
The event handling differs between loaders:

| Forge Event | Fabric Equivalent |
|---|---|
| `PlayerInteractEvent.LeftClickBlock` | `PlayerBlockBreakEvents.BEFORE` |
| `PlayerInteractEvent.RightClickBlock` | `UseBlockCallback.EVENT` |
| `PlayerInteractEvent.EntityInteract` | `UseEntityCallback.EVENT` |
| `TickEvent.ServerTickEvent` | `ServerTickEvents.END_SERVER_TICK` |
| `PlayerEvent.PlayerLoggedInEvent` | `ServerPlayConnectionEvents.JOIN` |

### Code Sharing Strategy
- **Common Code**: Personality, Reputation, Data, Commands, Utils (shared)
- **Loader-Specific**: Event handlers, Entry points, Mod metadata

### Testing on Fabric (when ready)
```bash
# Compile Fabric JAR
./gradlew -p fabric build

# Copy to Fabric-enabled Minecraft installation
cp fabric/build/libs/villagediplomacy-fabric-*.jar "$FABRIC_MODS_DIR"

# Run Fabric server/client
# (requires Fabric Loader and Fabric API to be installed)
```

## Troubleshooting

### Build Fails on Fabric
- Clean cache: `rm -rf .gradle fabric/build`
- Verify Fabric Loader version compatibility
- Check network connectivity for Maven repos

### JAR Not Loading
- Ensure correct loader version in `fabric.mod.json`
- Verify dependencies are installed
- Check mod ID matches

## Next Steps

1. ✅ Complete Forge compilation and testing
2. 🔄 Refine Fabric event handlers
3. 🔄 Test Fabric JAR in Fabric environment
4. 📋 Implement NeoForge (similar to Forge)
5. 📋 Implement Quilt (based on Fabric)
6. 📦 Release multi-loader JARs

## Contact & Support

For issues or questions about multi-loader support, refer to the main mod repository.
