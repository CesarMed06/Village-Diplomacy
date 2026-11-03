<div align="center">

<img src="branding/logo-512.png" alt="Village Diplomacy logo" width="180">

# 🏘️ Village Diplomacy

**A comprehensive Minecraft mod that transforms your relationship with villages.** Dynamic reputation, diplomacy systems, and unique villager personalities — for Forge, NeoForge, Fabric & Quilt.

[![Modrinth](https://img.shields.io/modrinth/dt/nL13t6rJ?logo=modrinth&label=Modrinth&color=00AF5C)](https://modrinth.com/project/nL13t6rJ)
[![CurseForge](https://img.shields.io/badge/CurseForge-Download-F16436?logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/village-diplomacy)
[![License](https://img.shields.io/github/license/CesarMed06/Village-Diplomacy)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.5.0-blue)](https://github.com/CesarMed06/Village-Diplomacy/releases)

</div>

---

## 📥 Download

| Platform | Link |
|----------|------|
| 🟢 Modrinth | [modrinth.com/mod/village-diplomacy](https://modrinth.com/mod/village-diplomacy) |
| 🟠 CurseForge | [curseforge.com/minecraft/mc-mods/village-diplomacy](https://www.curseforge.com/minecraft/mc-mods/village-diplomacy) |

---

## ✨ Features

- **Dynamic Reputation System** — Your actions towards villagers affect how the entire village perceives you. Steal, trade, or protect them and watch the consequences unfold.
- **Diplomacy Mechanics** — Negotiate with village leaders, form alliances or trigger conflicts between villages.
- **Unique Villager Personalities** — Every villager has a distinct personality type that influences how they react to you and other events.
- **Multi-Loader Support** — Fully compatible with Forge, NeoForge, Fabric, and Quilt.

---

## 🛠️ Installation

### From Modrinth / CurseForge (Recommended)
Download the mod from the links above and place the `.jar` in your `mods/` folder.

### Manual Install
Download the jar that matches your loader and Minecraft version and drop it into your `mods/` folder.

> ⚠️ **Only one jar of Village Diplomacy at a time.** The four loader jars share the same `mod_id`, so having two of them in `mods/` will make the game fail to load. Keep only the one for the loader you play.

---

## 📚 Documentation

| File | What it is |
|------|------------|
| [`changelog.txt`](changelog.txt) | Full version history. |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Contribution guidelines. |

---

## 🧩 Dependencies

| Mod | Required? | For |
|-----|-----------|-----|
| [Fabric API](https://modrinth.com/mod/fabric-api) | **Required** | Fabric and Quilt jars |
| [Guard Villagers](https://www.curseforge.com/minecraft/mc-mods/guard-villagers) | Optional | Guards react to your reputation and act as witnesses |
| [MCA Reborn](https://www.curseforge.com/minecraft/mc-mods/minecraft-comes-alive-reborn) | Optional | MCA guards react to your reputation (Forge / NeoForge only) |

---

## 🏗️ Building

Every module is a standalone Gradle project. Pick the folder for the version and loader you want:

| Minecraft | Forge | NeoForge | Fabric | Quilt |
|-----------|-------|----------|--------|-------|
| 1.20.1 | `/` (root) | `neoforge/` | `fabric/` | `quilt/` |
| 1.21 | — | `neoforge121/` | — | — |
| 1.21.1 | `forge1211/` | `neoforge1211/` | `fabric1211/` | `quilt1211/` |

```
cd fabric1211
../gradlew build
```

The jar ends up in `build/libs/`. `forge1211/` ships its own wrapper (Gradle 9.3.1, ForgeGradle 7),
so build it with `./gradlew build` from inside that folder.
