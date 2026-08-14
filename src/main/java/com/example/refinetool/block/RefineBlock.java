package com.example.refinetool.block;

import com.example.refinetool.block.entity.RefineBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RefineBlock extends BaseEntityBlock {

	public static final MapCodec<RefineBlock> CODEC = simpleCodec(RefineBlock::new);

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/**
	 * Caixa de colisao aproximada (o modelo real e irregular, com pernas
	 * finas e inclinadas - isto e so uma caixa envolvente para colisao e
	 * mira do jogador).
	 */
	private static final VoxelShape SHAPE = Shapes.box(0.125D, 0.0D, 0.0D, 0.875D, 1.0D, 0.9375D);

	public RefineBlock(Properties properties) {
		super(properties);
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		// Geometria toda desenhada pelo RefineBlockEntityRenderer, o modelo de
		// bloco em si fica vazio (ver models/block/refine_block.json).
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RefineBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide) {
			return null;
		}
		return createTickerHelper(type, ModBlockEntities.REFINE_BLOCK_ENTITY, RefineBlockEntity::serverTick);
	}

	// ---- interacao: mao vazia retira o item; mao com item insere item/material ----

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!(level.getBlockEntity(pos) instanceof RefineBlockEntity blockEntity)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}

		if (blockEntity.canAcceptAsRepairItem(stack)) {
			if (!level.isClientSide) {
				blockEntity.insertRepairItem(stack);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}

		if (blockEntity.canAcceptAsMaterial(stack)) {
			if (!level.isClientSide) {
				blockEntity.insertMaterial(stack);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}

		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!(level.getBlockEntity(pos) instanceof RefineBlockEntity blockEntity) || !blockEntity.hasRepairItem()) {
			return InteractionResult.PASS;
		}

		if (!level.isClientSide) {
			ItemStack result = blockEntity.extractRepairItem();
			if (!player.getInventory().add(result)) {
				player.drop(result, false);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock())) {
			if (level.getBlockEntity(pos) instanceof RefineBlockEntity blockEntity) {
				ItemStack repairItem = blockEntity.extractRepairItem();
				if (!repairItem.isEmpty()) {
					Block.popResource(level, pos, repairItem);
				}
				ItemStack material = blockEntity.extractMaterial();
				if (!material.isEmpty()) {
					Block.popResource(level, pos, material);
				}
			}
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}
