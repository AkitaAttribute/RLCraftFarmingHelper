# RLCraft Farming Helper

A lightweight NeoForge mod for Minecraft **1.21.1** that lets players right-click mature crops to harvest them and automatically replant them when a matching planting item is available.

This branch is the Minecraft 1.21.1 NeoForge port. The `main` branch remains the Forge 1.12.2 version.

## Behavior

When a player right-clicks a supported mature crop with the main hand:

1. The mod verifies that the crop is fully grown.
2. The server harvests it through normal player block-breaking logic, preserving standard drops and relevant break hooks.
3. The mod searches the player's main inventory first and offhand second for a block item that places the same crop block.
4. When a matching item is found, one is consumed and the crop is replanted at age 0.
5. When no matching item is available, the mature crop is still harvested and replanting is skipped.

Creative-mode players do not consume the planting item.

## Supported crops

- Crops implemented with Minecraft's `CropBlock` base class, including vanilla wheat, carrots, potatoes, and beetroot.
- Nether Wart, which uses a separate age-based block implementation.
- Compatible modded crops that extend `CropBlock` and use a `BlockItem` planting item associated with the same crop block.

## Compatibility

- Minecraft: `1.21.1`
- Mod loader: NeoForge `21.1.x`
- Java: `21`

The mod is required on the server. Installing it on clients is recommended so handled right-click interactions are immediately mirrored client-side.

## Build

A Gradle wrapper JAR is not committed, so use Gradle 8.10.2 or allow the GitHub Actions workflow to build the branch.

```bash
gradle clean build --no-daemon
```

The output JAR is produced in:

```text
build/libs/rlcraftfarminghelper-2.0.0.jar
```

## Branches

- `main`: Forge 1.12.2
- `1.21.1-NeoForge`: NeoForge 1.21.1

The GitHub Actions workflow detects the project type and selects Java 8 with Gradle 4.10.3 for the legacy Forge branch or Java 21 with Gradle 8.10.2 for this NeoForge branch.
