#!/bin/bash

# ================================================
# Village Diplomacy - Multi-Loader Build Script
# ================================================

LOADER=${1:-help}
OPTION=${2:-}

show_help() {
    cat << 'EOF'

╔════════════════════════════════════════════════╗
║  Village Diplomacy - Multi-Loader Builder      ║
╚════════════════════════════════════════════════╝

Usage: ./build.sh [loader] [option]

Loaders:
  forge       - Build for Forge (default, fully working)
  fabric      - Build for Fabric (in development)
  neoforge    - Build for NeoForge (planned)
  quilt       - Build for Quilt (planned)
  all         - Build all loaders (Forge + ready loaders)

Options:
  clean       - Clean build directory before building
  install     - Copy JAR to Minecraft mods folder

Examples:
  ./build.sh forge          - Compile Forge
  ./build.sh forge install  - Compile Forge and copy to mods
  ./build.sh fabric clean   - Clean and rebuild Fabric

EOF
}

build_forge() {
    echo ""
    echo "Building for Forge..."
    chmod +x ./gradlew
    ./gradlew build
    
    if [ "$OPTION" = "install" ]; then
        echo ""
        echo "Installing Forge JAR to Minecraft mods folder..."
        MODS_DIR="$HOME/.minecraft/mods"
        mkdir -p "$MODS_DIR"
        cp build/libs/villagediplomacy-*.jar "$MODS_DIR/"
        echo "✅ Installation complete!"
    fi
}

build_fabric() {
    echo ""
    echo "Building for Fabric..."
    
    if [ "$OPTION" = "clean" ]; then
        echo "Cleaning Fabric build directory..."
        rm -rf fabric/build .gradle
    fi
    
    cd fabric
    chmod +x ../gradlew
    ../gradlew build
    cd ..
    
    if [ "$OPTION" = "install" ]; then
        echo ""
        echo "Installing Fabric JAR to Minecraft mods folder..."
        MODS_DIR="$HOME/.minecraft/mods"
        mkdir -p "$MODS_DIR"
        cp fabric/build/libs/villagediplomacy-fabric-*.jar "$MODS_DIR/"
        echo "✅ Installation complete!"
    fi
}

case "$LOADER" in
    forge)
        build_forge
        ;;
    fabric)
        build_fabric
        ;;
    all)
        echo ""
        echo "Building for all loaders..."
        build_forge
        echo ""
        echo "Fabric support coming soon..."
        ;;
    help|--help|-h|"")
        show_help
        ;;
    *)
        echo "Unknown loader: $LOADER"
        echo "Run './build.sh help' for more information."
        exit 1
        ;;
esac

echo ""
echo "Build completed!"
