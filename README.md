# Acidglow's Centered Doors

Acidglow's Centered Doors is a NeoForge mod for Minecraft 26.2 that adds a Door Adjuster tool. The tool lets you move vanilla doors to the front, middle, or back of their block space without replacing the door visually with a different item.

## Features

- Move supported doors between front, middle, and back positions.
- Mirror a door by moving the handle/hinge side from left to right or right to left.
- Open centered and back-positioned doors from the hinge side.
- Keep double doors opening together when they are a matching pair with opposite hinges.
- Allow fences, walls, glass panes, and similar blocks to connect to the side of centered doors only.
- Drop and clone the original vanilla door item from adjusted doors.

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

The project currently builds against NeoForge `26.2.0.32-beta`.

## Building

Build the mod jar with:

```powershell
.\gradlew.bat build
```

The jar is created in:

```text
build/libs/
```

## License

All Rights Reserved.
