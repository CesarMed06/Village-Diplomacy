@echo off
REM ================================================
REM Village Diplomacy - Multi-Loader Build Script
REM ================================================

setlocal enabledelayedexpansion

if "%1"=="" (
    echo.
    echo ╔════════════════════════════════════════════════╗
    echo ║  Village Diplomacy - Multi-Loader Builder      ║
    echo ╚════════════════════════════════════════════════╝
    echo.
    echo Usage: build.bat [loader] [option]
    echo.
    echo Loaders:
    echo   forge       - Build for Forge (default, fully working)
    echo   fabric      - Build for Fabric (in development)
    echo   neoforge    - Build for NeoForge (planned)
    echo   quilt       - Build for Quilt (planned)
    echo   all         - Build all loaders (Forge + ready loaders)
    echo.
    echo Options:
    echo   clean       - Clean build directory before building
    echo   install     - Copy JAR to Minecraft mods folder
    echo.
    echo Examples:
    echo   build.bat forge          - Compile Forge
    echo   build.bat forge install  - Compile Forge and copy to mods
    echo   build.bat fabric clean   - Clean and rebuild Fabric
    echo.
    goto :eof
)

set LOADER=%1
set OPTION=%2

if "%LOADER%"=="forge" (
    echo.
    echo Building for Forge...
    call gradlew.bat build
    if "%OPTION%"=="install" (
        echo.
        echo Installing Forge JAR to Minecraft mods folder...
        copy "build\libs\villagediplomacy-*.jar" "%APPDATA%\.minecraft\mods\" /Y
        echo ✅ Installation complete!
    )
) else if "%LOADER%"=="fabric" (
    echo.
    echo Building for Fabric...
    if "%OPTION%"=="clean" (
        echo Cleaning Fabric build directory...
        rmdir /s /q fabric\build 2>nul
        rmdir /s /q .gradle 2>nul
    )
    cd fabric
    call ..\gradlew.bat build
    cd ..
    if "%OPTION%"=="install" (
        echo.
        echo Installing Fabric JAR to Minecraft mods folder...
        copy "fabric\build\libs\villagediplomacy-fabric-*.jar" "%APPDATA%\.minecraft\mods\" /Y
        echo ✅ Installation complete!
    )
) else if "%LOADER%"=="all" (
    echo.
    echo Building for all loaders...
    call build.bat forge
    echo.
    echo Fabric support coming soon...
) else (
    echo Unknown loader: %LOADER%
    echo Run 'build.bat' without arguments for help.
    exit /b 1
)

echo.
echo Build completed!
