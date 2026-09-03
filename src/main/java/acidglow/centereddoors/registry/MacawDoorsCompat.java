package acidglow.centereddoors.registry;

import net.neoforged.neoforge.registries.DeferredRegister;

final class MacawDoorsCompat {
    static final String MOD_ID = "mcwdoors";

    private MacawDoorsCompat() {
    }

    static void register(DeferredRegister.Blocks blocks) {
        for (MacawDoorDefinitions.Definition definition : MacawDoorDefinitions.all()) {
            ModDoors.registerOptionalDoor(blocks, MOD_ID, definition.path(), definition.blockSetType());
        }
    }
}
