package io.github.lightman314.lightmanscurrency.common.blocks.traderblocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import io.github.lightman314.lightmanscurrency.common.blockentity.trader.ItemTraderBlockEntity;
import io.github.lightman314.lightmanscurrency.common.blocks.templates.interfaces.IRotatableBlock;
import io.github.lightman314.lightmanscurrency.common.blocks.traderblocks.interfaces.IItemTraderBlock;
import io.github.lightman314.lightmanscurrency.common.blocks.traderblocks.templates.TraderBlockRotatable;
import io.github.lightman314.lightmanscurrency.common.blocks.util.LazyShapes;
import io.github.lightman314.lightmanscurrency.common.core.ModBlockEntities;
import io.github.lightman314.lightmanscurrency.common.items.tooltips.LCTooltips;
import io.github.lightman314.lightmanscurrency.util.MathUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ShelfBlock extends TraderBlockRotatable implements IItemTraderBlock {

    public static final int TRADECOUNT = 1;

    private static final VoxelShape SHAPE_NORTH = createCuboidShape(0d, 0d, 0d, 16d, 16d, 5d);
    private static final VoxelShape SHAPE_SOUTH = createCuboidShape(0d,0d,11d,16d,16d,16d);
    private static final VoxelShape SHAPE_EAST = createCuboidShape(11d,0d,0d,16d,16d,16d);
    private static final VoxelShape SHAPE_WEST = createCuboidShape(0d,0d,0d,5d,16d,16d);

    public ShelfBlock(Settings properties) { super(properties, LazyShapes.lazyDirectionalShape(SHAPE_NORTH, SHAPE_EAST, SHAPE_SOUTH, SHAPE_WEST)); }

    @Override
    public BlockEntity makeTrader(BlockPos pos, BlockState state) { return new ItemTraderBlockEntity(pos, state, TRADECOUNT); }

    @Override
    public BlockEntityType<?> traderType() { return ModBlockEntities.ITEM_TRADER; }

    @Override
    @Environment(EnvType.CLIENT)
    public List<Vector3f> GetStackRenderPos(int tradeSlot, BlockState state, boolean isDoubleTrade) {
        List<Vector3f> posList = new ArrayList<Vector3f>(1);
        if(tradeSlot == 0)
        {
            Direction facing = this.getFacing(state);
            //Define directions for easy positional handling
            Vector3f forward = IRotatableBlock.getForwardVect(facing);
            Vector3f right = IRotatableBlock.getRightVect(facing);
            Vector3f up = MathUtil.YP();
            Vector3f offset = IRotatableBlock.getOffsetVect(facing);
            //Only 1 position for shelves
            posList.add(MathUtil.VectorAdd(offset, MathUtil.VectorMult(right, 0.5f), MathUtil.VectorMult(forward, 14.5f/16f), MathUtil.VectorMult(up, 9f/16f)));
        }

        return posList;
    }


    @Override
    @Environment(EnvType.CLIENT)
    public List<Quaternionf> GetStackRenderRot(int tradeSlot, BlockState state)
    {
        //Return null for automatic rotation
        List<Quaternionf> rotation = new ArrayList<>();
        int facing = this.getFacing(state).getHorizontal();
        rotation.add(MathUtil.getRotationDegrees(facing * -90f));
        return rotation;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public float GetStackRenderScale(int tradeSlot, BlockState state){ return 14f/16f; }

    @Override
    @Environment(EnvType.CLIENT)
    public int maxRenderIndex() { return TRADECOUNT; }

    @Override
    protected Supplier<List<Text>> getItemTooltips() { return LCTooltips.ITEM_TRADER; }

}