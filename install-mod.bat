@echo off
setlocal enabledelayedexpansion

set MODS_DIR=C:\Users\cmedg\AppData\Roaming\.minecraft\mods

if "%1"=="" (
    echo =====================================
    echo  VillageDiplomacy Multi-Loader Mgr
    echo =====================================
    echo.
    echo Usage: install-mod.bat [loader]
    echo.
    echo Loaders:
    echo   forge      - Forge version
    echo   fabric     - Fabric version
    echo   quilt      - Quilt version
    echo   clean      - Remove all
    echo   build-all  - Build all loaders
    echo.
    goto :end
)

if "%1"=="forge" (
    del /F /Q "%MODS_DIR%\villagediplomacy-*.jar" 2>nul
    copy /Y "build\libs\villagediplomacy-1.3.0.jar" "%MODS_DIR%" >nul 2>&1
    echo [OK] Forge installed. Launch with Forge 47.4.10
    goto :end
)

if "%1"=="fabric" (
    del /F /Q "%MODS_DIR%\villagediplomacy-*.jar" 2>nul
    copy /Y "fabric\build\libs\villagediplomacy-fabric-1.3.0.jar" "%MODS_DIR%" >nul 2>&1
    echo [OK] Fabric installed. Launch with Fabric Loader 0.15.11
    goto :end
)

if "%1"=="quilt" (
    del /F /Q "%MODS_DIR%\villagediplomacy-*.jar" 2>nul
    copy /Y "quilt\build\libs\villagediplomacy-quilt-1.3.0.jar" "%MODS_DIR%" >nul 2>&1
    echo [OK] Quilt installed. Launch with Quilt Loader 0.24.0
    goto :end
)

if "%1"=="clean" (
    del /F /Q "%MODS_DIR%\villagediplomacy-*.jar" 2>nul
    echo [OK] All mods removed
    goto :end
)

if "%1"=="build-all" (
    echo Building all loaders...
    call gradlew.bat build --no-daemon -x test
    cd fabric && ..\gradlew.bat build --no-daemon -x test && cd ..
    cd quilt && ..\gradlew.bat build --no-daemon -x test && cd ..
    echo [OK] All built. Use: install-mod.bat [forge/fabric/quilt]
    goto :end
)

echo Unknown option: %1
goto :end

:end
endlocal
