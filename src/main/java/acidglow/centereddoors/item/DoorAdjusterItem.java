package acidglow.centereddoors.item;

import acidglow.centereddoors.block.AdjustedDoorBlock;
import acidglow.centereddoors.block.DoorDepth;
import acidglow.centereddoors.registry.ModDoors;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class DoorAdjusterItem extends Item {
    private static final int UPDATE_FLAGS = Block.UPDATE_ALL;

    public DoorAdjusterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        Optional<DoorTarget> target = DoorTarget.find(level, context.getClickedPos());
        if (target.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        adjustDoor(level, player, target.get(), context.isSecondaryUseActive());

        return InteractionResult.SUCCESS_SERVER;
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getItemStack().getItem() instanceof DoorAdjusterItem)) {
            return;
        }

        Optional<DoorTarget> target = DoorTarget.find(event.getLevel(), event.getPos());
        if (target.isEmpty()) {
            return;
        }

        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);
        event.setCancellationResult(event.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);

        if (!event.getLevel().isClientSide()) {
            adjustDoor(event.getLevel(), event.getEntity(), target.get(), event.getEntity().isSecondaryUseActive());
        }
    }

    private static void adjustDoor(Level level, Player player, DoorTarget target, boolean mirror) {
        if (mirror) {
            mirrorDoor(level, target);
            player.sendSystemMessage(Component.translatable("message.acidglowscentereddoors.mirrored"));
        } else {
            DoorDepth nextDepth = moveDoor(level, target);
            player.sendSystemMessage(Component.translatable("message.acidglowscentereddoors.position." + nextDepth.modelSuffix()));
        }
    }

    private static DoorDepth moveDoor(Level level, DoorTarget target) {
        DoorDepth nextDepth = target.currentDepth().next();
        setAdjustedDoor(level, target, target.lowerState(), target.upperState(), nextDepth);
        return nextDepth;
    }

    private static void mirrorDoor(Level level, DoorTarget target) {
        DoorHingeSide hinge = target.lowerState().getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT
                ? DoorHingeSide.RIGHT
                : DoorHingeSide.LEFT;

        BlockState lowerState = target.lowerState().setValue(DoorBlock.HINGE, hinge);
        BlockState upperState = target.upperState().setValue(DoorBlock.HINGE, hinge);
        level.setBlock(target.lowerPos(), lowerState, UPDATE_FLAGS);
        level.setBlock(target.lowerPos().above(), upperState, UPDATE_FLAGS);
    }

    private static void setAdjustedDoor(Level level, DoorTarget target, BlockState lowerSource, BlockState upperSource, DoorDepth depth) {
        AdjustedDoorBlock adjustedDoor = target.adjustedDoor();
        BlockState lowerState = adjustedDoor.withPropertiesOf(lowerSource)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(AdjustedDoorBlock.DEPTH, depth);
        BlockState upperState = adjustedDoor.withPropertiesOf(upperSource)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)
                .setValue(AdjustedDoorBlock.DEPTH, depth);

        level.setBlock(target.lowerPos(), lowerState, UPDATE_FLAGS);
        level.setBlock(target.lowerPos().above(), upperState, UPDATE_FLAGS);
    }

    private record DoorTarget(BlockPos lowerPos, BlockState lowerState, BlockState upperState, AdjustedDoorBlock adjustedDoor) {
        private static Optional<DoorTarget> find(Level level, BlockPos clickedPos) {
            BlockState clickedState = level.getBlockState(clickedPos);
            if (!(clickedState.getBlock() instanceof DoorBlock) || !ModDoors.canAdjust(clickedState.getBlock())) {
                return Optional.empty();
            }

            BlockPos lowerPos = clickedState.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? clickedPos : clickedPos.below();
            BlockState lowerState = level.getBlockState(lowerPos);
            BlockState upperState = level.getBlockState(lowerPos.above());
            if (!(lowerState.getBlock() instanceof DoorBlock)
                    || !(upperState.getBlock() instanceof DoorBlock)
                    || lowerState.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER
                    || upperState.getValue(DoorBlock.HALF) != DoubleBlockHalf.UPPER) {
                return Optional.empty();
            }

            Optional<AdjustedDoorBlock> adjustedDoor = ModDoors.adjustedDoorFor(lowerState.getBlock());
            return adjustedDoor.map(doorBlock -> new DoorTarget(lowerPos, lowerState, upperState, doorBlock));
        }

        private DoorDepth currentDepth() {
            return this.lowerState.hasProperty(AdjustedDoorBlock.DEPTH)
                    ? this.lowerState.getValue(AdjustedDoorBlock.DEPTH)
                    : DoorDepth.FRONT;
        }
    }
}
