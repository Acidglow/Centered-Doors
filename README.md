# Acidglow's Centered Doors

Acidglow's Centered Doors is a NeoForge mod for Minecraft 26.2 that adds a Door Adjuster tool. The tool lets you move supported doors to the front, middle, or back of their block space while preserving their original appearance and item drops.

## Features

- Move supported doors between front, middle, and back positions.
- Mirror a door by moving the handle/hinge side from left to right or right to left.
- Open centered and back-positioned doors from the hinge side.
- Keep double doors opening together when they are a matching pair with opposite hinges.
- Allow fences, walls, glass panes, and similar blocks to connect to the side of centered doors only.
- Drop and clone the original source-door item from adjusted doors.
- Support regular doors from Macaw's Doors when that mod is installed.

## Door Adjuster

Hold the Door Adjuster in your main hand and use it on a door.

- Right-click: cycle the door position.
- Sneak + right-click: mirror the hinge side.

The position cycle is:

```text
Front -> Middle -> Back -> Middle -> Front
```

When the tool is held in the main hand, right-clicking a door adjusts it instead of opening it.

## Recipe

```text
_ I _
I S _
_ _ S
```

Where:

- `I` = Iron Ingot
- `S` = Stick
- `_` = Empty slot

## Supported Doors

- Oak
- Spruce
- Birch
- Jungle
- Acacia
- Cherry
- Dark Oak
- Pale Oak
- Mangrove
- Bamboo
- Crimson
- Warped
- Iron

## Compatibility

- Minecraft: 26.2
- NeoForge: 26.2.0.0 and newer
- Java: 25
- Optional mod support: Macaw's Doors 1.1.5 and newer (`mcwdoors`)

The project currently builds against NeoForge `26.2.0.32-beta` and is tested with Macaw's Doors `1.1.5` for Minecraft 26.2.

## Building

Install a Java 25 JDK, then build the mod with the included Gradle wrapper.

Windows:

```powershell
.\gradlew.bat build
```

Linux and macOS:

```bash
./gradlew build
```

Macaw's Doors compatibility definitions are kept in
`tools/compatibility_doors.json`. Regenerate the registration source and validate
all blockstates and models with the Minecraft client and Macaw's Doors JARs:

```bash
python3 tools/generate_compatibility.py \
  --generate-java \
  --check-assets \
  --vanilla-assets .gradle/caches/minecraft/versions/26.2/client.jar \
  --mcwdoors-assets run/client/mods/mcw-doors-1.1.5-mc26.2neoforge.jar
```

To regenerate the checked-in compatibility assets and remove resources for
deleted variants, use:

```bash
python3 tools/generate_compatibility.py \
  --generate-java \
  --generate-assets src/main/resources/assets/acidglowscentereddoors \
  --prune \
  --check-assets \
  --vanilla-assets .gradle/caches/minecraft/versions/26.2/client.jar \
  --mcwdoors-assets run/client/mods/mcw-doors-1.1.5-mc26.2neoforge.jar
```

The asset checker verifies that every local model is reachable, then resolves
generated model parents and texture references recursively across the project
resources, the vanilla client JAR, and Macaw's Doors JAR.

To verify every supported Macaw door at runtime, place the tested Macaw's Doors
jar in `run/gameTestServer/mods`, then run:

```bash
CENTERED_DOORS_REQUIRE_MACAW=true ./gradlew runGameTestServer
```

The environment flag makes the test fail instead of skip if Macaw's Doors was
not loaded. The compatibility test checks all 210 supported source IDs and
converts each door by clicking both its lower and upper half.

The resulting jar is created in:

```text
build/libs/
```

## License

MIT. See [LICENSE](LICENSE).
