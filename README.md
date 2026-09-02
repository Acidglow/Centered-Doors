# Acidglow's Centered Doors

Acidglow's Centered Doors is a NeoForge mod for Minecraft 26.2 that adds a Door Adjuster tool. The tool lets you move supported doors to the front, middle, or back of their block space while preserving their original appearance and item drops.

## Features

- Move supported doors between front, middle, and back positions.
- Mirror a door by moving the handle/hinge side from left to right or right to left.
- Open centered and back-positioned doors from the hinge side.
- Keep double doors opening together when they are a matching pair with opposite hinges.
- Allow fences, walls, glass panes, and similar blocks to connect to the side of centered doors only.
- Drop and clone the original vanilla door item from adjusted doors.
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
- Optional mod support: Macaw's Doors (`mcwdoors`)

The project currently builds against NeoForge `26.2.0.32-beta`.

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

The resulting jar is created in:

```text
build/libs/
```

## License

All Rights Reserved.
