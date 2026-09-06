package acidglow.centereddoors.test;

import acidglow.centereddoors.AcidglowsCenteredDoors;
import acidglow.centereddoors.block.AdjustedDoorBlock;
import acidglow.centereddoors.block.DoorDepth;
import acidglow.centereddoors.registry.ModDoors;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.fml.ModList;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.Holder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public final class CenteredDoorsGameTests {
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");
    private static final String MACAW_ADJUSTED_PREFIX = "adjusted_mcwdoors_";
    private static final int EXPECTED_MACAW_DOORS = 210;
    private static final int EXPLOSION_TRIALS = 1024;

    private CenteredDoorsGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        if (FMLEnvironment.getDist() != Dist.DEDICATED_SERVER) {
            return;
        }
        var environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(AcidglowsCenteredDoors.MODID, "default"),
                new net.minecraft.gametest.framework.TestEnvironmentDefinition.AllOf()
        );
        register(event, environment, "cycle", CenteredDoorsGameTests::cycle, 40);
        register(event, environment, "movement_direction", CenteredDoorsGameTests::movementDirection, 40);
        register(event, environment, "upper_half", CenteredDoorsGameTests::upperHalfConversion, 20);
        register(event, environment, "hinge", CenteredDoorsGameTests::hingeMirroring, 20);
        register(event, environment, "redstone_and_double", CenteredDoorsGameTests::redstoneAndDoubleDoor, 20);
        register(event, environment, "shapes", CenteredDoorsGameTests::shapesAndSupport, 20);
        register(event, environment, "drops", CenteredDoorsGameTests::dropsAndPickBlock, 20);
        register(event, environment, "macaw_absent", CenteredDoorsGameTests::macawAbsent, 20);
        register(event, environment, "macaw_all_supported", CenteredDoorsGameTests::macawPresent, 20);
    }

    private static void register(
            RegisterGameTestsEvent event,
            Holder<net.minecraft.gametest.framework.TestEnvironmentDefinition<?>> environment,
            String name,
            Consumer<GameTestHelper> function,
            int maxTicks
    ) {
        event.registerTest(
                Identifier.fromNamespaceAndPath(AcidglowsCenteredDoors.MODID, name),
                new DirectGameTest(new TestData<>(environment, EMPTY_STRUCTURE, maxTicks, 0, true), function)
        );
    }

    private static void cycle(GameTestHelper helper) {
        BlockPos lowerPos = new BlockPos(1, 1, 1);
        placeDoor(helper, lowerPos, Blocks.OAK_DOOR.defaultBlockState(), DoorHingeSide.LEFT);
        Player player = adjusterPlayer(helper);

        useAdjuster(helper, lowerPos, player);
        assertState(helper, lowerPos, isAdjustedAt(helper.getBlockState(lowerPos), DoorDepth.MIDDLE_TO_BACK), "first adjustment did not move the door to the middle");
        assertDoorOrientation(helper, lowerPos, Direction.NORTH, DoorHingeSide.LEFT);
        useAdjuster(helper, lowerPos.above(), player);
        assertState(helper, lowerPos, isAdjustedAt(helper.getBlockState(lowerPos), DoorDepth.BACK), "second adjustment did not move the door to the back");
        assertDoorOrientation(helper, lowerPos, Direction.SOUTH, DoorHingeSide.RIGHT);
        useAdjuster(helper, lowerPos, player);
        assertState(helper, lowerPos, isAdjustedAt(helper.getBlockState(lowerPos), DoorDepth.MIDDLE_TO_FRONT), "third adjustment did not move the door to the middle");
        assertDoorOrientation(helper, lowerPos, Direction.SOUTH, DoorHingeSide.RIGHT);
        useAdjuster(helper, lowerPos.above(), player);
        assertState(helper, lowerPos, isAdjustedAt(helper.getBlockState(lowerPos), DoorDepth.FRONT), "fourth adjustment did not return the door to the front");
        assertDoorOrientation(helper, lowerPos, Direction.NORTH, DoorHingeSide.LEFT);
        assertState(helper, lowerPos.above(), helper.getBlockState(lowerPos.above()).getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER, "upper half was not preserved");
        helper.succeed();
    }

    private static void upperHalfConversion(GameTestHelper helper) {
        BlockPos lowerPos = new BlockPos(1, 1, 1);
        placeDoor(helper, lowerPos, Blocks.BIRCH_DOOR.defaultBlockState(), DoorHingeSide.RIGHT);
        Player player = adjusterPlayer(helper);

        useAdjuster(helper, lowerPos.above(), player);
        assertState(helper, lowerPos, helper.getBlockState(lowerPos).getBlock() instanceof AdjustedDoorBlock, "lower half was not converted");
        assertState(helper, lowerPos.above(), helper.getBlockState(lowerPos.above()).getBlock() instanceof AdjustedDoorBlock, "upper half was not converted");
        helper.succeed();
    }

    private static void movementDirection(GameTestHelper helper) {
        Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        DoorHingeSide[] hinges = {DoorHingeSide.LEFT, DoorHingeSide.RIGHT};
        Player player = adjusterPlayer(helper);

        for (int index = 0; index < facings.length * hinges.length; index++) {
            BlockPos lowerPos = new BlockPos(1 + index * 2, 1, 1);
            Direction facing = facings[index / hinges.length];
            DoorHingeSide hinge = hinges[index % hinges.length];
            placeDoor(helper, lowerPos, Blocks.OAK_DOOR.defaultBlockState(), hinge, facing);

            useAdjuster(helper, lowerPos, player);
            assertDoorOrientation(helper, lowerPos, facing, hinge);
            useAdjuster(helper, lowerPos, player);
            assertDoorOrientation(helper, lowerPos, facing.getOpposite(), oppositeHinge(hinge));
            useAdjuster(helper, lowerPos, player);
            assertDoorOrientation(helper, lowerPos, facing.getOpposite(), oppositeHinge(hinge));
            useAdjuster(helper, lowerPos, player);
            assertDoorOrientation(helper, lowerPos, facing, hinge);
        }
        helper.succeed();
    }

    private static void hingeMirroring(GameTestHelper helper) {
        BlockPos lowerPos = new BlockPos(1, 1, 1);
        placeAdjustedDoor(helper, lowerPos, DoorDepth.FRONT, DoorHingeSide.LEFT, false);
        Player player = adjusterPlayer(helper);
        player.setShiftKeyDown(true);

        useAdjuster(helper, lowerPos.above(), player);
        helper.assertBlockProperty(lowerPos, DoorBlock.HINGE, DoorHingeSide.RIGHT);
        helper.assertBlockProperty(lowerPos.above(), DoorBlock.HINGE, DoorHingeSide.RIGHT);
        useAdjuster(helper, lowerPos, player);
        helper.assertBlockProperty(lowerPos, DoorBlock.HINGE, DoorHingeSide.LEFT);
        helper.assertBlockProperty(lowerPos.above(), DoorBlock.HINGE, DoorHingeSide.LEFT);
        helper.succeed();
    }

    private static void redstoneAndDoubleDoor(GameTestHelper helper) {
        BlockPos first = new BlockPos(1, 1, 1);
        BlockPos second = first.east();
        placeAdjustedDoor(helper, first, DoorDepth.FRONT, DoorHingeSide.LEFT, false);
        placeAdjustedDoor(helper, second, DoorDepth.FRONT, DoorHingeSide.RIGHT, false);

        helper.useBlock(first, helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertBlockProperty(first, DoorBlock.OPEN, true);
        helper.assertBlockProperty(second, DoorBlock.OPEN, true);

        helper.setBlock(first.north(), Blocks.REDSTONE_BLOCK);
        helper.assertBlockProperty(first, DoorBlock.OPEN, true);
        helper.setBlock(first.north(), Blocks.AIR);
        helper.assertBlockProperty(first, DoorBlock.OPEN, false);
        helper.assertBlockProperty(second, DoorBlock.OPEN, false);
        helper.succeed();
    }

    private static void shapesAndSupport(GameTestHelper helper) {
        AdjustedDoorBlock door = ModDoors.adjustedDoorFor(Blocks.OAK_DOOR).orElseThrow();
        BlockPos pos = new BlockPos(1, 1, 1);

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (DoorDepth depth : DoorDepth.values()) {
                BlockState closedState = door.defaultBlockState()
                        .setValue(DoorBlock.FACING, facing)
                        .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                        .setValue(AdjustedDoorBlock.DEPTH, depth);
                assertShapeBounds(
                        helper,
                        closedState.getShape(helper.getLevel(), helper.absolutePos(pos), CollisionContext.empty()),
                        expectedClosedBounds(facing, depth),
                        "closed " + facing + " " + depth
                );

                for (DoorHingeSide hinge : DoorHingeSide.values()) {
                    BlockState openState = closedState
                            .setValue(DoorBlock.OPEN, true)
                            .setValue(DoorBlock.HINGE, hinge);
                    assertShapeBounds(
                            helper,
                            openState.getShape(helper.getLevel(), helper.absolutePos(pos), CollisionContext.empty()),
                            expectedOpenBounds(facing, hinge, depth),
                            "open " + facing + " " + hinge + " " + depth
                    );
                }

                if (depth.isMiddle()) {
                    if (closedState.getBlockSupportShape(helper.getLevel(), helper.absolutePos(pos)).isEmpty()) {
                        helper.fail("middle adjusted door has no support shape");
                    }
                }
            }
        }
        helper.succeed();
    }

    private static void dropsAndPickBlock(GameTestHelper helper) {
        BlockPos lowerPos = new BlockPos(1, 1, 1);
        placeAdjustedDoor(helper, lowerPos, DoorDepth.BACK, DoorHingeSide.LEFT, false);
        BlockState adjustedLower = helper.getBlockState(lowerPos);
        BlockState adjustedUpper = helper.getBlockState(lowerPos.above());
        assertDoorLootAndClone(helper, lowerPos, Blocks.OAK_DOOR, adjustedLower, adjustedUpper);
        assertExplosionBehavior(helper, lowerPos, Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), adjustedLower);

        helper.getLevel().destroyBlock(helper.absolutePos(lowerPos), true, null);
        helper.assertItemEntityCountIs(Blocks.OAK_DOOR.asItem(), lowerPos, 2.0, 1);
        helper.succeed();
    }

    private static void macawAbsent(GameTestHelper helper) {
        if (ModList.get().isLoaded("mcwdoors")) {
            helper.succeed();
            return;
        }

        boolean compatibilityBlockRegistered = BuiltInRegistries.BLOCK.keySet().stream()
                .anyMatch(key -> key.getNamespace().equals("acidglowscentereddoors")
                        && key.getPath().startsWith("adjusted_mcwdoors_"));
        assertState(helper, new BlockPos(1, 1, 1), !compatibilityBlockRegistered,
                "Macaw compatibility blocks were registered while Macaw's Doors was absent");
        helper.succeed();
    }

    private static void macawPresent(GameTestHelper helper) {
        if (!ModList.get().isLoaded("mcwdoors")) {
            if (Boolean.parseBoolean(System.getenv("CENTERED_DOORS_REQUIRE_MACAW"))) {
                helper.fail("Macaw's Doors is required for this test run but was not loaded");
            }
            helper.succeed();
            return;
        }

        List<Identifier> sourceIds = BuiltInRegistries.BLOCK.keySet().stream()
                .filter(key -> key.getNamespace().equals(AcidglowsCenteredDoors.MODID))
                .map(Identifier::getPath)
                .filter(path -> path.startsWith(MACAW_ADJUSTED_PREFIX))
                .map(path -> Identifier.fromNamespaceAndPath("mcwdoors", path.substring(MACAW_ADJUSTED_PREFIX.length())))
                .sorted(Comparator.comparing(Identifier::toString))
                .toList();
        if (sourceIds.size() != EXPECTED_MACAW_DOORS) {
            helper.fail("expected " + EXPECTED_MACAW_DOORS + " Macaw compatibility registrations, found " + sourceIds.size());
        }

        BlockPos lowerPos = new BlockPos(1, 1, 1);
        Player player = adjusterPlayer(helper);
        for (Identifier sourceId : sourceIds) {
            Block sourceDoor = BuiltInRegistries.BLOCK.getOptional(sourceId).orElseThrow(
                    () -> new IllegalStateException("Macaw's Doors is missing supported door " + sourceId)
            );
            if (!(sourceDoor instanceof DoorBlock)) {
                helper.fail(sourceId + " is registered as supported but is not a door block");
            }
            AdjustedDoorBlock adjustedDoor = ModDoors.adjustedDoorFor(sourceDoor).orElseThrow(
                    () -> new IllegalStateException(sourceId + " has no adjusted registration")
            );

            assertMacawConversion(helper, lowerPos, player, sourceId, sourceDoor, adjustedDoor, lowerPos);
            assertMacawConversion(helper, lowerPos, player, sourceId, sourceDoor, adjustedDoor, lowerPos.above());
            assertDoorLootAndClone(
                    helper,
                    lowerPos,
                    sourceDoor,
                    helper.getBlockState(lowerPos),
                    helper.getBlockState(lowerPos.above())
            );
        }
        helper.succeed();
    }

    private static void assertDoorLootAndClone(
            GameTestHelper helper,
            BlockPos lowerPos,
            Block sourceDoor,
            BlockState adjustedLower,
            BlockState adjustedUpper
    ) {
        ItemStack clone = adjustedLower.getCloneItemStack(helper.getLevel(), helper.absolutePos(lowerPos), false);
        if (!clone.is(sourceDoor.asItem())) {
            helper.fail("pick-block returned the wrong item for " + BuiltInRegistries.BLOCK.getKey(sourceDoor));
        }

        List<ItemStack> lowerDrops = getDrops(helper, lowerPos, adjustedLower, null);
        if (lowerDrops.size() != 1 || !lowerDrops.getFirst().is(sourceDoor.asItem())) {
            helper.fail("lower half returned the wrong drops for " + BuiltInRegistries.BLOCK.getKey(sourceDoor));
        }
        if (!getDrops(helper, lowerPos.above(), adjustedUpper, null).isEmpty()) {
            helper.fail("upper half dropped an item for " + BuiltInRegistries.BLOCK.getKey(sourceDoor));
        }
    }

    private static void assertExplosionBehavior(
            GameTestHelper helper,
            BlockPos pos,
            BlockState sourceState,
            BlockState adjustedState
    ) {
        int sourceSurvivals = 0;
        int adjustedSurvivals = 0;
        for (int trial = 0; trial < EXPLOSION_TRIALS; trial++) {
            sourceSurvivals += getDrops(helper, pos, sourceState, 2.0F).isEmpty() ? 0 : 1;
            adjustedSurvivals += getDrops(helper, pos, adjustedState, 2.0F).isEmpty() ? 0 : 1;
        }

        int minimumExpected = EXPLOSION_TRIALS * 2 / 5;
        int maximumExpected = EXPLOSION_TRIALS * 3 / 5;
        if (sourceSurvivals < minimumExpected || sourceSurvivals > maximumExpected) {
            helper.fail("source door did not follow its expected explosion survival rate: " + sourceSurvivals);
        }
        if (adjustedSurvivals < minimumExpected || adjustedSurvivals > maximumExpected) {
            helper.fail("adjusted door did not preserve the source explosion condition: " + adjustedSurvivals);
        }
    }

    private static List<ItemStack> getDrops(
            GameTestHelper helper,
            BlockPos pos,
            BlockState state,
            Float explosionRadius
    ) {
        LootParams.Builder params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(pos)))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY);
        if (explosionRadius != null) {
            params.withParameter(LootContextParams.EXPLOSION_RADIUS, explosionRadius);
        }
        return state.getDrops(params);
    }

    private static void assertMacawConversion(
            GameTestHelper helper,
            BlockPos lowerPos,
            Player player,
            Identifier sourceId,
            Block sourceDoor,
            AdjustedDoorBlock adjustedDoor,
            BlockPos clickedPos
    ) {
        placeDoor(helper, lowerPos, sourceDoor.defaultBlockState(), DoorHingeSide.LEFT);
        useAdjuster(helper, clickedPos, player);
        assertState(helper, lowerPos, helper.getBlockState(lowerPos).getBlock() == adjustedDoor,
                sourceId + " lower half did not convert when clicking " + clickedPos.subtract(lowerPos));
        assertState(helper, lowerPos.above(), helper.getBlockState(lowerPos.above()).getBlock() == adjustedDoor,
                sourceId + " upper half did not convert when clicking " + clickedPos.subtract(lowerPos));
    }

    private static void placeDoor(GameTestHelper helper, BlockPos lowerPos, BlockState sourceState, DoorHingeSide hinge) {
        placeDoor(helper, lowerPos, sourceState, hinge, Direction.NORTH);
    }

    private static void placeDoor(
            GameTestHelper helper,
            BlockPos lowerPos,
            BlockState sourceState,
            DoorHingeSide hinge,
            Direction facing
    ) {
        helper.setBlock(lowerPos, sourceState
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(DoorBlock.HINGE, hinge)
                .setValue(DoorBlock.FACING, facing));
        helper.setBlock(lowerPos.above(), sourceState
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)
                .setValue(DoorBlock.HINGE, hinge)
                .setValue(DoorBlock.FACING, facing));
    }

    private static void placeAdjustedDoor(GameTestHelper helper, BlockPos lowerPos, DoorDepth depth, DoorHingeSide hinge, boolean open) {
        AdjustedDoorBlock door = AcidglowsCenteredDoors.BLOCKS.getEntries().stream()
                .map(holder -> holder.get())
                .filter(AdjustedDoorBlock.class::isInstance)
                .map(AdjustedDoorBlock.class::cast)
                .findFirst()
                .orElseThrow();
        BlockState state = door.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HINGE, hinge)
                .setValue(DoorBlock.OPEN, open);
        helper.setBlock(lowerPos, state.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER).setValue(AdjustedDoorBlock.DEPTH, depth));
        helper.setBlock(lowerPos.above(), state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER).setValue(AdjustedDoorBlock.DEPTH, depth));
    }

    private static Player adjusterPlayer(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, AcidglowsCenteredDoors.DOOR_ADJUSTER.get().getDefaultInstance());
        return player;
    }

    private static void useAdjuster(GameTestHelper helper, BlockPos pos, Player player) {
        BlockPos absolutePos = helper.absolutePos(pos);
        player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).useOn(new UseOnContext(
                player,
                net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(
                        net.minecraft.world.phys.Vec3.atCenterOf(absolutePos),
                        Direction.NORTH,
                        absolutePos,
                        true
                )
        ));
    }

    private static boolean isAdjustedAt(BlockState state, DoorDepth depth) {
        return state.getBlock() instanceof AdjustedDoorBlock && state.getValue(AdjustedDoorBlock.DEPTH) == depth;
    }

    private static void assertDoorOrientation(GameTestHelper helper, BlockPos lowerPos, Direction facing, DoorHingeSide hinge) {
        helper.assertBlockProperty(lowerPos, DoorBlock.FACING, facing);
        helper.assertBlockProperty(lowerPos.above(), DoorBlock.FACING, facing);
        helper.assertBlockProperty(lowerPos, DoorBlock.HINGE, hinge);
        helper.assertBlockProperty(lowerPos.above(), DoorBlock.HINGE, hinge);
    }

    private static DoorHingeSide oppositeHinge(DoorHingeSide hinge) {
        return hinge == DoorHingeSide.LEFT ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
    }

    private static AABB expectedClosedBounds(Direction facing, DoorDepth depth) {
        if (depth == DoorDepth.BACK) {
            depth = DoorDepth.FRONT;
        }
        double min = depth.minZ() / 16.0;
        double max = depth.maxZ() / 16.0;
        return switch (facing) {
            case NORTH -> new AABB(0.0, 0.0, min, 1.0, 1.0, max);
            case SOUTH -> new AABB(0.0, 0.0, 1.0 - max, 1.0, 1.0, 1.0 - min);
            case WEST -> new AABB(min, 0.0, 0.0, max, 1.0, 1.0);
            case EAST -> new AABB(1.0 - max, 0.0, 0.0, 1.0 - min, 1.0, 1.0);
            default -> throw new IllegalStateException("Door facing must be horizontal");
        };
    }

    private static AABB expectedOpenBounds(Direction facing, DoorHingeSide hinge, DoorDepth depth) {
        AABB closed = expectedClosedBounds(facing, depth);
        double closedMin = facing.getAxis() == Direction.Axis.X ? closed.minX : closed.minZ;
        double closedMax = facing.getAxis() == Direction.Axis.X ? closed.maxX : closed.maxZ;
        double lengthMin = switch (facing) {
            case EAST, SOUTH -> closedMin;
            case WEST, NORTH -> closedMax - 1.0;
            default -> throw new IllegalStateException("Door facing must be horizontal");
        };
        double lengthMax = lengthMin + 1.0;

        Direction openDirection = hinge == DoorHingeSide.RIGHT
                ? facing.getCounterClockWise()
                : facing.getClockWise();
        AABB front = expectedClosedBounds(openDirection, DoorDepth.FRONT);
        double thicknessMin = facing.getAxis() == Direction.Axis.X ? front.minZ : front.minX;
        double thicknessMax = facing.getAxis() == Direction.Axis.X ? front.maxZ : front.maxX;

        return facing.getAxis() == Direction.Axis.X
                ? new AABB(lengthMin, 0.0, thicknessMin, lengthMax, 1.0, thicknessMax)
                : new AABB(thicknessMin, 0.0, lengthMin, thicknessMax, 1.0, lengthMax);
    }

    private static void assertShapeBounds(
            GameTestHelper helper,
            VoxelShape shape,
            AABB expected,
            String description
    ) {
        if (shape.isEmpty()) {
            helper.fail(description + " adjusted door has an empty collision shape");
        }

        AABB actual = shape.bounds();
        double epsilon = 1.0E-9;
        if (Math.abs(actual.minX - expected.minX) > epsilon
                || Math.abs(actual.minY - expected.minY) > epsilon
                || Math.abs(actual.minZ - expected.minZ) > epsilon
                || Math.abs(actual.maxX - expected.maxX) > epsilon
                || Math.abs(actual.maxY - expected.maxY) > epsilon
                || Math.abs(actual.maxZ - expected.maxZ) > epsilon) {
            helper.fail(description + " collision bounds were " + actual + ", expected " + expected);
        }
    }

    private static void assertState(GameTestHelper helper, BlockPos pos, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(pos, message);
        }
    }

    private static final class DirectGameTest extends GameTestInstance {
        private final Consumer<GameTestHelper> function;

        private DirectGameTest(
                TestData<Holder<net.minecraft.gametest.framework.TestEnvironmentDefinition<?>>> info,
                Consumer<GameTestHelper> function
        ) {
            super(info);
            this.function = function;
        }

        @Override
        public void run(GameTestHelper helper) {
            function.accept(helper);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return null;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("direct");
        }

    }
}
