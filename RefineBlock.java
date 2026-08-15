package com.example.refinetool.block;

import com.example.refinetool.block.entity.RefineBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * NOTA sobre a 1.21.11: varias APIs usadas aqui mudaram nessa versao
 * especifica (a mais recente que existe, lancada bem depois do meu
 * treinamento):
 *  - Level.isClientSide agora e um METODO (isClientSide()), nao mais um campo.
 *  - BlockStateProperties.HORIZONTAL_FACING agora tem o tipo generico
 *    Property<Direction> (a classe "DirectionProperty" dedicada nao existe
 *    mais nesse pacote).
 *  - Block#useItemOn agora retorna InteractionResult (nao mais
 *    ItemInteractionResult, que parece ter sido unificado com InteractionResult).
 *  - Block#onRemove nao existe mais; para "salvar o item antes do bloco
 *    sumir" uso playerWillDestroy (chamado quando um JOGADOR quebra o
 *    bloco - explosoes etc. nao disparam isso, limitacao aceitavel aqui).
 */
public class RefineBlock extends BaseEntityBlock {

	public static final MapCodec<RefineBlock> CODEC = simpleCodec(RefineBlock::new);

	public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

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
	protected MapCodec<RefineBlock> codec() {
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
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	// Sem override de getRenderShape: o modelo de bloco (models/block/refine_block.json)
	// nao tem elementos, entao o sistema de modelo vanilla nao desenha nada e toda a
	// geometria vem do RefineBlockEntityRenderer.

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RefineBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return null;
		}
		return createTickerHelper(type, ModBlockEntities.REFINE_BLOCK_ENTITY, RefineBlockEntity::serverTick);
	}

	// ---- interacao: mao vazia retira o item; mao com item insere item/material ----

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!(level.getBlockEntity(pos) instanceof RefineBlockEntity blockEntity)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		if (blockEntity.canAcceptAsRepairItem(stack)) {
			if (!level.isClientSide()) {
				blockEntity.insertRepairItem(stack);
			}
			return InteractionResult.SUCCESS;
		}

		if (blockEntity.canAcceptAsMaterial(stack)) {
			if (!level.isClientSide()) {
				blockEntity.insertMaterial(stack);
			}
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!(level.getBlockEntity(pos) instanceof RefineBlockEntity blockEntity) || !blockEntity.hasRepairItem()) {
			return InteractionResult.PASS;
		}

		if (!level.isClientSide()) {
			ItemStack result = blockEntity.extractRepairItem();
			if (!player.getInventory().add(result)) {
				player.drop(result, false);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Chamado quando um JOGADOR quebra o bloco - devolve o item guardado em vez de perde-lo. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RefineBlockEntity blockEntity) {
			ItemStack repairItem = blockEntity.extractRepairItem();
			if (!repairItem.isEmpty()) {
				Block.popResource(level, pos, repairItem);
			}
			ItemStack material = blockEntity.extractMaterial();
			if (!material.isEmpty()) {
				Block.popResource(level, pos, material);
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}
}
