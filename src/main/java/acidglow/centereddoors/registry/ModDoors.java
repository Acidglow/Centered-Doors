package acidglow.centereddoors.registry;

import acidglow.centereddoors.block.AdjustedDoorBlock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDoors {
    private static final Map<Block, DeferredBlock<AdjustedDoorBlock>> VANILLA_TO_ADJUSTED = new LinkedHashMap<>();
    private static final Map<Identifier, DeferredBlock<? extends AdjustedDoorBlock>> SOURCE_ID_TO_ADJUSTED = new LinkedHashMap<>();

    private static boolean registered;

    private ModDoors() {
    }

    public static void register(DeferredRegister.Blocks blocks) {
        if (registered) {
            return;
        }

        registerDoor(blocks, "oak", Blocks.OAK_DOOR, BlockSetType.OAK);
        registerDoor(blocks, "spruce", Blocks.SPRUCE_DOOR, BlockSetType.SPRUCE);
        registerDoor(blocks, "birch", Blocks.BIRCH_DOOR, BlockSetType.BIRCH);
        registerDoor(blocks, "jungle", Blocks.JUNGLE_DOOR, BlockSetType.JUNGLE);
        registerDoor(blocks, "acacia", Blocks.ACACIA_DOOR, BlockSetType.ACACIA);
        registerDoor(blocks, "cherry", Blocks.CHERRY_DOOR, BlockSetType.CHERRY);
        registerDoor(blocks, "dark_oak", Blocks.DARK_OAK_DOOR, BlockSetType.DARK_OAK);
        registerDoor(blocks, "pale_oak", Blocks.PALE_OAK_DOOR, BlockSetType.PALE_OAK);
        registerDoor(blocks, "mangrove", Blocks.MANGROVE_DOOR, BlockSetType.MANGROVE);
        registerDoor(blocks, "bamboo", Blocks.BAMBOO_DOOR, BlockSetType.BAMBOO);
        registerDoor(blocks, "crimson", Blocks.CRIMSON_DOOR, BlockSetType.CRIMSON);
        registerDoor(blocks, "warped", Blocks.WARPED_DOOR, BlockSetType.WARPED);
        registerDoor(blocks, "iron", Blocks.IRON_DOOR, BlockSetType.IRON);
        MacawDoorsCompat.register(blocks);

        registered = true;
    }

    public static Optional<AdjustedDoorBlock> adjustedDoorFor(Block block) {
        if (block instanceof AdjustedDoorBlock adjustedDoorBlock) {
            return Optional.of(adjustedDoorBlock);
        }

        DeferredBlock<AdjustedDoorBlock> adjustedDoor = VANILLA_TO_ADJUSTED.get(block);
        if (adjustedDoor != null) {
            return Optional.of(adjustedDoor.get());
        }

        DeferredBlock<? extends AdjustedDoorBlock> optionalAdjustedDoor = SOURCE_ID_TO_ADJUSTED.get(BuiltInRegistries.BLOCK.getKey(block));
        return optionalAdjustedDoor == null ? Optional.empty() : Optional.of(optionalAdjustedDoor.get());
    }

    public static boolean canAdjust(Block block) {
        return block instanceof AdjustedDoorBlock
                || VANILLA_TO_ADJUSTED.containsKey(block)
                || SOURCE_ID_TO_ADJUSTED.containsKey(BuiltInRegistries.BLOCK.getKey(block));
    }

    private static void registerDoor(DeferredRegister.Blocks blocks, String name, Block vanillaDoor, BlockSetType type) {
        DeferredBlock<AdjustedDoorBlock> adjustedDoor = blocks.registerBlock(
                "adjusted_" + name + "_door",
                properties -> new AdjustedDoorBlock(type, vanillaDoor, properties),
                () -> BlockBehaviour.Properties.ofFullCopy(vanillaDoor)
        );
        VANILLA_TO_ADJUSTED.put(vanillaDoor, adjustedDoor);
        SOURCE_ID_TO_ADJUSTED.put(BuiltInRegistries.BLOCK.getKey(vanillaDoor), adjustedDoor);
    }

    static void registerOptionalDoor(DeferredRegister.Blocks blocks, String namespace, String path, BlockSetType type) {
        Identifier sourceId = Identifier.fromNamespaceAndPath(namespace, path);
        DeferredBlock<? extends AdjustedDoorBlock> adjustedDoor = blocks.registerBlock(
                "adjusted_" + namespace + "_" + path,
                properties -> new AdjustedDoorBlock(type, sourceId, properties),
                () -> BlockBehaviour.Properties.of()
                        .noOcclusion()
                        .strength(2.0F, 3.0F)
        );
        SOURCE_ID_TO_ADJUSTED.put(sourceId, adjustedDoor);
    }
}
