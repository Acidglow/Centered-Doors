package acidglow.centereddoors.block;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class AdjustedDoorBlock extends DoorBlock {
    public static final EnumProperty<DoorDepth> DEPTH = EnumProperty.create("position", DoorDepth.class);

    private static final Map<DoorDepth, Map<Direction, VoxelShape>> SHAPES_BY_DEPTH = createShapes();
    private static final Map<Direction, VoxelShape> CENTER_SIDE_SUPPORT_SHAPES = Shapes.rotateHorizontal(
            Shapes.or(
                    Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0),
                    Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0)
            )
    );
    private final Block vanillaDoor;
    private final Identifier vanillaDoorId;

    public AdjustedDoorBlock(BlockSetType type, Block vanillaDoor, BlockBehaviour.Properties properties) {
        this(type, vanillaDoor, BuiltInRegistries.BLOCK.getKey(vanillaDoor), properties);
    }

    public AdjustedDoorBlock(BlockSetType type, Identifier vanillaDoorId, BlockBehaviour.Properties properties) {
        this(type, null, vanillaDoorId, properties);
    }

    private AdjustedDoorBlock(BlockSetType type, @Nullable Block vanillaDoor, Identifier vanillaDoorId, BlockBehaviour.Properties properties) {
        super(type, properties);
        this.vanillaDoor = vanillaDoor;
        this.vanillaDoorId = vanillaDoorId;
        this.registerDefaultState(this.defaultBlockState().setValue(DEPTH, DoorDepth.FRONT));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(OPEN)) {
            return getOpenShape(state);
        }

        return getClosedShape(state);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (!state.getValue(DEPTH).isMiddle()) {
            return Shapes.empty();
        }

        return CENTER_SIDE_SUPPORT_SHAPES.get(state.getValue(FACING));
    }

    @Override
    @SuppressWarnings("deprecation")
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this.vanillaDoor());
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return List.of();
        }

        Block sourceDoor = this.vanillaDoor();
        if (sourceDoor == Blocks.AIR) {
            return List.of();
        }

        return List.of(new ItemStack(sourceDoor));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        InteractionResult result = super.useWithoutItem(state, level, pos, player, hitResult);
        if (result.consumesAction()) {
            syncDoubleDoorOpen(level, pos);
        }

        return result;
    }

    @Override
    public void setOpen(@Nullable Entity sourceEntity, Level level, BlockState state, BlockPos pos, boolean shouldOpen) {
        super.setOpen(sourceEntity, level, state, pos, shouldOpen);
        syncDoubleDoorOpen(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        syncDoubleDoorOpen(level, pos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DEPTH);
    }

    private static Map<DoorDepth, Map<Direction, VoxelShape>> createShapes() {
        Map<DoorDepth, Map<Direction, VoxelShape>> shapes = new EnumMap<>(DoorDepth.class);
        for (DoorDepth depth : DoorDepth.values()) {
            shapes.put(depth, Shapes.rotateHorizontal(Block.boxZ(16.0, depth.minZ(), depth.maxZ())));
        }
        return shapes;
    }

    protected static VoxelShape getClosedShape(BlockState state) {
        return SHAPES_BY_DEPTH.get(state.getValue(DEPTH)).get(state.getValue(FACING));
    }

    private static Direction getDoorDirection(BlockState state) {
        Direction direction = state.getValue(FACING);
        return state.getValue(OPEN)
                ? (state.getValue(HINGE) == DoorHingeSide.RIGHT ? direction.getCounterClockWise() : direction.getClockWise())
                : direction;
    }

    private static VoxelShape getOpenShape(BlockState state) {
        DoorDepth depth = state.getValue(DEPTH);
        Direction facing = state.getValue(FACING);
        Direction openDirection = getDoorDirection(state);
        AxisInterval length = getOpenLengthInterval(facing, depth);
        AxisInterval thickness = getFrontInterval(openDirection);

        return switch (facing.getAxis()) {
            case X -> Block.box(length.min(), 0.0, thickness.min(), length.max(), 16.0, thickness.max());
            case Z -> Block.box(thickness.min(), 0.0, length.min(), thickness.max(), 16.0, length.max());
            case Y -> Shapes.empty();
        };
    }

    private static AxisInterval getOpenLengthInterval(Direction facing, DoorDepth depth) {
        AxisInterval closedDepth = getDepthInterval(facing, depth);
        return switch (facing) {
            case EAST, SOUTH -> new AxisInterval(closedDepth.min(), closedDepth.min() + 16.0);
            case WEST, NORTH -> new AxisInterval(closedDepth.max() - 16.0, closedDepth.max());
            default -> throw new IllegalStateException("Door facing must be horizontal");
        };
    }

    private static AxisInterval getFrontInterval(Direction direction) {
        return getDepthInterval(direction, DoorDepth.FRONT);
    }

    private static AxisInterval getDepthInterval(Direction direction, DoorDepth depth) {
        double min = depth.minZ();
        double max = depth.maxZ();
        return switch (direction) {
            case NORTH, WEST -> new AxisInterval(min, max);
            case SOUTH, EAST -> new AxisInterval(16.0 - max, 16.0 - min);
            default -> throw new IllegalStateException("Door direction must be horizontal");
        };
    }

    private void syncDoubleDoorOpen(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) {
            return;
        }

        findDoubleDoorPartner(level, getLowerPos(pos, state), state).ifPresent(partnerPos -> {
            BlockState partnerState = level.getBlockState(partnerPos);
            boolean open = state.getValue(OPEN);
            if (partnerState.getValue(OPEN) != open) {
                level.setBlock(partnerPos, partnerState.setValue(OPEN, open), 10);
            }
        });
    }

    private java.util.Optional<BlockPos> findDoubleDoorPartner(Level level, BlockPos lowerPos, BlockState state) {
        Direction facing = state.getValue(FACING);
        for (Direction side : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
            BlockPos candidatePos = lowerPos.relative(side);
            BlockState candidateState = level.getBlockState(candidatePos);
            if (isMatchingDoubleDoorPartner(level, candidatePos, candidateState, state)) {
                return java.util.Optional.of(candidatePos);
            }
        }

        return java.util.Optional.empty();
    }

    private boolean isMatchingDoubleDoorPartner(Level level, BlockPos candidatePos, BlockState candidateState, BlockState state) {
        if (!candidateState.is(this)
                || candidateState.getValue(HALF) != DoubleBlockHalf.LOWER
                || candidateState.getValue(FACING) != state.getValue(FACING)
                || candidateState.getValue(HINGE) == state.getValue(HINGE)) {
            return false;
        }

        BlockState candidateUpperState = level.getBlockState(candidatePos.above());
        return candidateUpperState.is(this) && candidateUpperState.getValue(HALF) == DoubleBlockHalf.UPPER;
    }

    private static BlockPos getLowerPos(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    private Block vanillaDoor() {
        if (this.vanillaDoor != null) {
            return this.vanillaDoor;
        }

        return BuiltInRegistries.BLOCK.getOptional(this.vanillaDoorId).orElse(Blocks.AIR);
    }

    private record AxisInterval(double min, double max) {
    }
}
