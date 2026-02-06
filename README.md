# Village Diplomacy

A reputation and relationship system for Minecraft 1.20.1 that tracks player interactions with villagers and villages. Each village maintains independent reputation data, and villagers respond dynamically based on your actions and their individual personalities.

## Overview

This mod implements a comprehensive reputation system where every action near villagers has consequences. Trading increases reputation, while stealing or destructive behavior decreases it. The system tracks 7 reputation ranks from HERO to VILLAIN, with each rank affecting how villagers and iron golems interact with the player.

Villages are detected automatically within a 200-block radius. Each village maintains separate reputation scores, allowing players to be heroes in one village while being villains in another.

## Core Features

- **Village-specific reputation tracking** - Each village maintains independent reputation data
- **Villager personalities** - 15 distinct temperament types (brave, shy, greedy, paranoid, wise, etc.)
- **Iron Golem memory system** - Golems track player aggression and can become permanently hostile
- **Dynamic messaging** - 250+ contextual messages based on reputation level and villager personality
- **Village naming system** - Assign custom names to villages for easier identification
- **Block interaction events** - Villagers react to placement of beds, chests, furnaces, and other blocks
- **Greeting system** - High-reputation players (HERO/ALLY) receive personalized greetings from villagers

## Reputation Mechanics

**Positive Actions:**
- Complete trade with villager: +5
- Enter village houses (when reputation is positive): +5

**Negative Actions:**
- Open village chests/barrels: -10
- Steal items from containers: -15
- Break villager beds: -15
- Destroy crops: -15
- Break village structures: -10
- Attack villagers: -10
- Kill villagers: -50

## Reputation Ranks

- **HERO** (500+): Villages love you, golems forgive everything, villagers greet you
- **ALLY** (200-499): Trusted friend, villagers are friendly
- **FRIENDLY** (100-199): Welcome in the village
- **NEUTRAL** (0-99): Normal interactions
- **UNWELCOME** (-1 to -100): Villagers don't trust you
- **ENEMY** (-101 to -200): Hostile reactions
- **VILLAIN** (-500 or lower): Everyone hates you

## Commands

**Player Commands:**
```
/pardon me              - Costs 100 gold ingots, resets iron golem hostility
/diplomacy name <name>  - Assigns custom name to nearest village
/diplomacy info         - Displays current village and reputation status
```

**Administrator Commands:**
- `/diplomacy reputation get/set/add` - Manage player reputation values
- `/diplomacy villages list` - View all discovered villages
- Additional admin commands available for village management and testing

## Installation

1. Install Minecraft Forge 47.4.10 for version 1.20.1
2. Place the mod JAR file in your `.minecraft/mods` folder
3. Launch Minecraft with the Forge profile

## Usage Tips

- Reputation is village-specific - you can maintain different standings with different villages
- Iron golems require payment (100 gold ingots) via `/pardon me` to reset hostility
- Consider establishing bases outside village boundaries if maintaining low reputation
- Trading is the most reliable method to increase reputation
- Players with HERO or ALLY status receive personalized greetings from villagers

## Known Limitations

- Block tracking has technical limitations - breaking player-placed blocks may still affect reputation
- Villager line-of-sight detection may occasionally trigger through walls

## Technical Information

**Author:** cesoti2006  
**Version:** 1.0.0  
**Minecraft Version:** 1.20.1  
**Forge Version:** 47.4.10+

---

*Reputation system with persistent data storage and village-specific tracking.*
