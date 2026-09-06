#!/usr/bin/env python3
"""Generate Macaw's Doors registration and validate its generated assets."""

import argparse
import json
from pathlib import Path
import zipfile


ROOT = Path(__file__).resolve().parents[1]
DEFINITIONS = ROOT / "tools" / "compatibility_doors.json"
GENERATED_JAVA = ROOT / "src/main/java/acidglow/centereddoors/registry/MacawDoorDefinitions.java"
RESOURCES = ROOT / "src/main/resources"
ASSETS = RESOURCES / "assets" / "acidglowscentereddoors"
BLOCKSTATES = ASSETS / "blockstates"
ALL_MODELS = ASSETS / "models"
MODELS = ALL_MODELS / "block"
TEXTURES = ASSETS / "textures"
LOCAL_NAMESPACE = "acidglowscentereddoors"


class AssetSource:
    def __init__(self, path):
        self.path = path
        self.archive = zipfile.ZipFile(path) if zipfile.is_zipfile(path) else None

    def contains(self, relative_path):
        if self.archive is not None:
            return relative_path in self.archive.namelist()
        return (self.path / relative_path).is_file()

    def read_json(self, relative_path):
        if self.archive is not None:
            return json.loads(self.archive.read(relative_path))
        return json.loads((self.path / relative_path).read_text(encoding="utf-8-sig"))

    def close(self):
        if self.archive is not None:
            self.archive.close()


def load_definitions():
    data = json.loads(DEFINITIONS.read_text())
    return [
        (variant["path"] if isinstance(variant, dict) else variant, family["type"], variant)
        for family in data["families"]
        for variant in family["variants"]
    ]


def generate_java(definitions):
    lines = [
        "package acidglow.centereddoors.registry;",
        "",
        "import java.util.List;",
        "import net.minecraft.world.level.block.state.properties.BlockSetType;",
        "",
        "final class MacawDoorDefinitions {",
        "    private MacawDoorDefinitions() {",
        "    }",
        "",
        "    static List<Definition> all() {",
        "        return List.of(",
    ]
    entries = [
        f'                new Definition("{variant}", BlockSetType.{block_set_type})'
        for variant, block_set_type, _ in definitions
    ]
    lines.append(",\n".join(entries))
    lines.extend([
        "        );",
        "    }",
        "",
        "    record Definition(String path, BlockSetType blockSetType) {",
        "    }",
        "}",
        "",
    ])
    GENERATED_JAVA.write_text("\n".join(lines))


def asset_paths(definitions):
    return {variant for variant, _, _ in definitions}


def read_json(path):
    try:
        return json.loads(path.read_text(encoding="utf-8-sig"))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise SystemExit(f"{path}: invalid JSON: {error}") from error


def local_reference_path(reference, root, suffix):
    if not isinstance(reference, str) or reference.startswith("#"):
        return None
    namespace, separator, path = reference.partition(":")
    if not separator or namespace != LOCAL_NAMESPACE:
        return None
    return root / f"{path}{suffix}"


def iter_named_strings(value, key_name):
    if isinstance(value, dict):
        for key, child in value.items():
            if key == key_name and isinstance(child, str):
                yield child
            yield from iter_named_strings(child, key_name)
    elif isinstance(value, list):
        for child in value:
            yield from iter_named_strings(child, key_name)


def validate_json_and_local_references():
    json_files = sorted(RESOURCES.rglob("*.json"))
    parsed = {path: read_json(path) for path in json_files}
    checked_models = set()
    checked_textures = set()

    for path, data in parsed.items():
        for reference in iter_named_strings(data, "model"):
            target = local_reference_path(reference, ALL_MODELS, ".json")
            if target is not None:
                if not target.is_file():
                    raise SystemExit(f"{path}: missing local model {target}")
                checked_models.add(target)

        if path.is_relative_to(ALL_MODELS):
            parent = data.get("parent") if isinstance(data, dict) else None
            target = local_reference_path(parent, ALL_MODELS, ".json")
            if target is not None:
                if not target.is_file():
                    raise SystemExit(f"{path}: missing local parent model {target}")
                checked_models.add(target)

            textures = data.get("textures", {}) if isinstance(data, dict) else {}
            if not isinstance(textures, dict):
                raise SystemExit(f"{path}: textures must be an object")
            for reference in textures.values():
                target = local_reference_path(reference, TEXTURES, ".png")
                if target is not None:
                    if not target.is_file():
                        raise SystemExit(f"{path}: missing local texture {target}")
                    checked_textures.add(target)

    return len(json_files), len(checked_models), len(checked_textures)


def generate_assets(definitions, output_root):
    output_blockstates = output_root / "blockstates"
    output_models = output_root / "models" / "block" / "mcwdoors"
    output_blockstates.mkdir(parents=True, exist_ok=True)
    output_models.mkdir(parents=True, exist_ok=True)

    suffixes = [
        f"{half}_{hinge}_{position}"
        for half in ("bottom", "top")
        for hinge in ("left", "right")
        for position in ("middle",)
    ] + [
        f"{half}_{hinge}_open_{position}_{facing}"
        for half in ("bottom", "top")
        for hinge in ("left", "right")
        for position in ("middle",)
        for facing in ("east", "north", "south", "west")
    ]
    for variant, _, definition in definitions:
        blockstate = {"variants": {}}
        for position in ("front", "middle_back", "back", "middle_front"):
            for facing in ("east", "north", "south", "west"):
                for half in ("lower", "upper"):
                    for hinge in ("left", "right"):
                        for open_state in ("false", "true"):
                            key = f"position={position},facing={facing},half={half},hinge={hinge},open={open_state}"
                            if position in ("front", "back"):
                                value = definition["front_models"][f"{facing},{half},{hinge},{open_state}"]
                            else:
                                model_position = "middle"
                                model_half = "bottom" if half == "lower" else "top"
                                render_facing = facing
                                render_hinge = hinge
                                if position == "middle_front" and open_state == "false":
                                    render_facing = {
                                        "east": "west",
                                        "north": "south",
                                        "south": "north",
                                        "west": "east",
                                    }[facing]
                                    render_hinge = "right" if hinge == "left" else "left"
                                suffix = f"{model_half}_{render_hinge}"
                                suffix += (
                                    f"_open_{model_position}_{facing}"
                                    if open_state == "true"
                                    else f"_{model_position}"
                                )
                                value = {"model": f"acidglowscentereddoors:block/mcwdoors/{variant}_{suffix}"}
                                if open_state == "false":
                                    rotation = {"north": 270, "south": 90, "west": 180}.get(render_facing)
                                    if rotation is not None:
                                        value["y"] = rotation
                            blockstate["variants"][key] = value
        (output_blockstates / f"adjusted_mcwdoors_{variant}.json").write_text(
            json.dumps(blockstate, indent=2, sort_keys=True) + "\n"
        )
        for suffix in suffixes:
            model = {
                "parent": f"acidglowscentereddoors:block/door_{suffix}",
                "textures": definition["textures"],
            }
            (output_models / f"{variant}_{suffix}.json").write_text(
                json.dumps(model, indent=2, sort_keys=True) + "\n"
            )


def prune_assets(definitions):
    variants = asset_paths(definitions)
    for path in BLOCKSTATES.glob("adjusted_mcwdoors_*.json"):
        variant = path.stem.removeprefix("adjusted_mcwdoors_")
        if variant not in variants:
            path.unlink()
    referenced_models = set()
    for path in BLOCKSTATES.glob("adjusted_mcwdoors_*.json"):
        for value in read_json(path).get("variants", {}).values():
            model = value.get("model", "")
            prefix = f"{LOCAL_NAMESPACE}:block/mcwdoors/"
            if model.startswith(prefix):
                referenced_models.add(model.removeprefix(prefix) + ".json")
    for path in (MODELS / "mcwdoors").glob("*.json"):
        if path.name not in referenced_models:
            path.unlink()


def asset_reference_path(namespace, kind, path):
    return f"assets/{namespace}/{kind}/{path}"


def validate_external_model(namespace, model_path, sources, origin, visited):
    source = sources.get(namespace)
    if source is None:
        raise SystemExit(f"{origin}: no asset source supplied for model {namespace}:{model_path}")

    key = (namespace, model_path)
    if key in visited:
        return
    visited.add(key)

    relative_path = asset_reference_path(namespace, "models", model_path + ".json")
    if not source.contains(relative_path):
        raise SystemExit(f"{origin}: missing model {namespace}:{model_path}")

    model = source.read_json(relative_path)
    if not isinstance(model, dict):
        raise SystemExit(f"{origin}: model {namespace}:{model_path} must be an object")

    parent = model.get("parent")
    if parent and not isinstance(parent, str):
        raise SystemExit(f"{origin}: model {namespace}:{model_path} has an invalid parent")
    if isinstance(parent, str) and not parent.startswith("#"):
        parent_namespace, separator, parent_path = parent.partition(":")
        if not separator:
            parent_namespace, parent_path = namespace, parent
        validate_external_model(parent_namespace, parent_path, sources, origin, visited)

    textures = model.get("textures", {})
    if not isinstance(textures, dict):
        raise SystemExit(f"{origin}: model {namespace}:{model_path} textures must be an object")
    for texture in textures.values():
        if not isinstance(texture, str) or texture.startswith("#"):
            continue
        texture_namespace, separator, texture_path = texture.partition(":")
        if not separator:
            texture_namespace, texture_path = namespace, texture
        texture_source = sources.get(texture_namespace)
        if texture_source is None:
            raise SystemExit(f"{origin}: no asset source supplied for texture {texture}")
        texture_file = asset_reference_path(texture_namespace, "textures", texture_path + ".png")
        if not texture_source.contains(texture_file):
            raise SystemExit(f"{origin}: missing texture {texture}")


def validate_assets(definitions, sources):
    expected_blockstates = {
        f"adjusted_mcwdoors_{variant}.json" for variant, _, _ in definitions
    }
    actual_blockstates = {
        path.name for path in BLOCKSTATES.glob("adjusted_mcwdoors_*.json")
    }
    if actual_blockstates != expected_blockstates:
        missing = sorted(expected_blockstates - actual_blockstates)
        stale = sorted(actual_blockstates - expected_blockstates)
        raise SystemExit(f"blockstate mismatch; missing={missing}, stale={stale}")

    referenced_models = set()
    for blockstate in sorted(BLOCKSTATES.glob("adjusted_mcwdoors_*.json")):
        data = read_json(blockstate)
        for variant in data.get("variants", {}).values():
            model = variant.get("model")
            if not model:
                raise SystemExit(f"{blockstate}: variant has no model")
            if ":" in model:
                namespace, model_path = model.split(":", 1)
                validate_external_model(namespace, model_path, sources, str(blockstate), set())
            if model.startswith("acidglowscentereddoors:"):
                model_name = model.removeprefix("acidglowscentereddoors:").removeprefix("block/")
                referenced_models.add(model_name + ".json")
                model_path = MODELS / (model_name + ".json")
                if not model_path.exists():
                    raise SystemExit(f"{blockstate}: missing model {model_path}")

    actual_models = {
        path.relative_to(MODELS).as_posix()
        for path in (MODELS / "mcwdoors").rglob("*.json")
    }
    stale_models = sorted(actual_models - referenced_models)
    if stale_models:
        raise SystemExit(f"unreferenced Macaw models: {stale_models[:10]}")

    for model_name in sorted(referenced_models):
        model_path = MODELS / model_name
        model = read_json(model_path)
        if "parent" not in model or "textures" not in model:
            raise SystemExit(f"{model_path}: missing parent or textures")

    for _, _, definition in definitions:
        for reference in iter_named_strings(definition.get("front_models", {}), "model"):
            namespace, separator, model_path = reference.partition(":")
            if not separator:
                raise SystemExit(f"compatibility_doors.json: invalid model reference {reference}")
            validate_external_model(namespace, model_path, sources, "compatibility_doors.json", set())

        for reference in definition.get("textures", {}).values():
            namespace, separator, texture_path = reference.partition(":")
            if not separator:
                raise SystemExit(f"compatibility_doors.json: invalid texture reference {reference}")
            source = sources.get(namespace)
            if source is None:
                raise SystemExit(f"compatibility_doors.json: no asset source supplied for texture {reference}")
            texture_file = asset_reference_path(namespace, "textures", texture_path + ".png")
            if not source.contains(texture_file):
                raise SystemExit(f"compatibility_doors.json: missing texture {reference}")

    json_count, local_model_count, local_texture_count = validate_json_and_local_references()
    print(
        f"Validated {len(definitions)} definitions, {len(expected_blockstates)} blockstates, "
        f"{len(referenced_models)} adjusted models, {json_count} JSON resources, "
        f"{local_model_count} local model references, and {local_texture_count} local texture references "
        f"plus external model and texture targets."
    )


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--generate-java", action="store_true")
    parser.add_argument("--generate-assets", type=Path)
    parser.add_argument("--prune", action="store_true")
    parser.add_argument("--check-assets", action="store_true")
    parser.add_argument("--vanilla-assets", type=Path)
    parser.add_argument("--mcwdoors-assets", type=Path)
    args = parser.parse_args()
    definitions = load_definitions()
    if args.generate_java:
        generate_java(definitions)
    if args.generate_assets:
        generate_assets(definitions, args.generate_assets)
    if args.prune:
        prune_assets(definitions)
    if args.check_assets:
        if args.vanilla_assets is None or args.mcwdoors_assets is None:
            parser.error("--check-assets requires --vanilla-assets and --mcwdoors-assets")
        sources = {
            LOCAL_NAMESPACE: AssetSource(RESOURCES),
            "minecraft": AssetSource(args.vanilla_assets),
            "mcwdoors": AssetSource(args.mcwdoors_assets),
        }
        try:
            validate_assets(definitions, sources)
        finally:
            for source in sources.values():
                source.close()
    if not args.generate_java and not args.generate_assets and not args.prune and not args.check_assets:
        parser.error("select --generate-java, --generate-assets, --prune, and/or --check-assets")


if __name__ == "__main__":
    main()
