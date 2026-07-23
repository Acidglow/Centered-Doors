package acidglow.centereddoors.block;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AdjustedDoorBlock extends DoorBlock {
    public static final EnumProperty<DoorDepth> DEPTH = EnumProperty.create("position", DoorDepth.class);

    private static final Map<DoorDepth, Map<Direction, VoxelShape>> SHAPES_BY_DEPTH = createShapes();
    private final Block vanillaDoor;

    public AdjustedDoorBlock(BlockSetType type, Block vanillaDoor, BlockBehaviour.Properties properties) {
        super(type, properties);
        this.vanillaDoor = vanillaDoor;
        this.registerDefaultState(this.defaultBlockState().setValue(DEPTH, DoorDepth.FRONT));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        Direction doorDirection = state.getValue(OPEN)
                ? (state.getValue(HINGE) == DoorHingeSide.RIGHT ? direction.getCounterClockWise() : direction.getClockWise())
                : direction;
        return SHAPES_BY_DEPTH.get(state.getValue(DEPTH)).get(doorDirection);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this.vanillaDoor);
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
}
