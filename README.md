# RLCraftFarmingHelper

A Minecraft Forge mod for Minecraft **1.12.2** that lets players right-click mature crops to harvest and automatically replant them.

## Behavior

When a player right-clicks a mature `BlockCrops` block:

1. The mod validates that the crop is fully grown.
2. The crop is harvested through normal player harvest logic (`tryHarvestBlock`) so break hooks/events still run.
3. The mod primarily validates replant items via Forge's IPlantable contract (same planted crop block), with a stage-0-drop fallback for broader compatibility.
4. If the player has at least one matching replant item in main inventory or offhand, one is consumed (main inventory preferred, offhand fallback).
5. The crop is replanted at age 0.

If no matching seed-equivalent item is found in inventory, nothing is harvested.

## Compatibility

- Minecraft: `1.12.2`
- Forge target for compilation: `14.23.5.2847`
- Forge `14.23.5.2860` compatibility is intended, but Forge no longer publishes userdev artifacts for that exact patch through current public Maven endpoints.

## Build

This project uses ForgeGradle 2.3 and requires Java 8 for full build/reobf.

```bash
./gradlew clean build
```

Output jar is produced in:

- `build/libs/rlcraftfarminghelper-1.0.1.jar`
- optional local mirror: `successful-builds/rlcraftfarminghelper-1.0.1+forge-14.23.5.2847.jar` (ignored by git)
