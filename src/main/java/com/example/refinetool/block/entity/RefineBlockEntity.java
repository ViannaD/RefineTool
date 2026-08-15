package com.example.refinetool.block.entity;

import com.example.refinetool.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Guarda o item a ser consertado (slot 0) e o material de reparo (slot 1), e
 * faz o conserto gradualmente gastando pouquissimo material comparado a uma
 * bigorna.
 *
 * NOTA: a partir da 1.21.11 a Mojang trocou o sistema de salvar/carregar
 * BlockEntity de CompoundTag+HolderLookup.Provider para ValueOutput/
 * ValueInput (uma abstracao nova). Tambem simplifiquei a validacao do
 * material: em vez de checar o componente "Repairable" do item (API ainda
 * muito instavel nessa versao), qualquer item diferente da ferramenta ja
 * inserida serve como material. Se quiser restringir a materiais especificos
 * (ferro para picareta de ferro, membrana de phantom para elytra etc.),
 * ajuste canAcceptAsMaterial() abaixo.
 */
public class RefineBlockEntity extends BlockEntity {

	/** Quanta durabilidade cada UNIDADE de material de reparo restaura. Bem mais barato que a bigorna. */
	public static final int DURABILITY_PER_MATERIAL = 50;
	/** A cada quantos ticks o bloco repara 1 ponto de durabilidade (10 ticks = 2x por segundo). */
	public static final int TICKS_PER_REPAIR_STEP = 10;
	/** Quantidade maxima de material que o bloco guarda de uma vez. */
	public static final int MAX_MATERIAL = 16;

	private static final int SLOT_REPAIR = 0;
	private static final int SLOT_MATERIAL = 1;

	private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
	private int durabilitySinceLastMaterial = 0;
	private boolean refining = false;

	public RefineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.REFINE_BLOCK_ENTITY, pos, state);
	}

	// ---- usado pelo renderer para desenhar o item e escolher a animacao ----

	public ItemStack getRepairItem() {
		return this.items.get(SLOT_REPAIR);
	}

	public boolean isRefining() {
		return this.refining;
	}

	public boolean hasRepairItem() {
		return !getRepairItem().isEmpty();
	}

	// ---- chamado pelo RefineBlock ao interagir ----

	public boolean canAcceptAsRepairItem(ItemStack stack) {
		return getRepairItem().isEmpty() && !stack.isEmpty() && stack.isDamageableItem();
	}

	public boolean canAcceptAsMaterial(ItemStack stack) {
		ItemStack repairItem = getRepairItem();
		ItemStack materialItem = this.items.get(SLOT_MATERIAL);
		if (repairItem.isEmpty() || stack.isEmpty()) {
			return false;
		}
		if (materialItem.getCount() >= MAX_MATERIAL) {
			return false;
		}
		if (!materialItem.isEmpty() && !ItemStack.isSameItemSameComponents(materialItem, stack)) {
			return false;
		}
		// Simplificacao: qualquer item diferente da propria ferramenta serve
		// de material (ver nota na documentacao da classe).
		return !ItemStack.isSameItem(stack, repairItem);
	}

	/** So chame depois de conferir canAcceptAsRepairItem(stack). Retira 1 unidade da stack recebida. */
	public void insertRepairItem(ItemStack stack) {
		this.items.set(SLOT_REPAIR, stack.split(1));
		setChanged();
		syncToClient();
	}

	/** So chame depois de conferir canAcceptAsMaterial(stack). */
	public void insertMaterial(ItemStack stack) {
		ItemStack materialItem = this.items.get(SLOT_MATERIAL);
		int space = MAX_MATERIAL - materialItem.getCount();
		int moved = Math.min(space, stack.getCount());
		if (materialItem.isEmpty()) {
			this.items.set(SLOT_MATERIAL, stack.split(moved));
		} else {
			materialItem.grow(moved);
			stack.shrink(moved);
		}
		setChanged();
		syncToClient();
	}

	/** Retira o item guardado (consertado ou nao) e devolve para o jogador. */
	public ItemStack extractRepairItem() {
		ItemStack result = this.items.get(SLOT_REPAIR);
		this.items.set(SLOT_REPAIR, ItemStack.EMPTY);
		this.durabilitySinceLastMaterial = 0;
		this.refining = false;
		setChanged();
		syncToClient();
		return result;
	}

	/** Retira o material guardado (usado quando o bloco e quebrado, para nao perder o item). */
	public ItemStack extractMaterial() {
		ItemStack result = this.items.get(SLOT_MATERIAL);
		this.items.set(SLOT_MATERIAL, ItemStack.EMPTY);
		setChanged();
		return result;
	}

	// ---- tick do servidor ----

	public static void serverTick(Level level, BlockPos pos, BlockState state, RefineBlockEntity be) {
		ItemStack repairItem = be.items.get(SLOT_REPAIR);
		ItemStack materialItem = be.items.get(SLOT_MATERIAL);

		boolean wasRefining = be.refining;
		boolean shouldRefine = !repairItem.isEmpty() && repairItem.isDamaged() && !materialItem.isEmpty();

		be.refining = shouldRefine;

		if (shouldRefine && level.getGameTime() % TICKS_PER_REPAIR_STEP == 0) {
			int newDamage = Math.max(0, repairItem.getDamageValue() - 1);
			repairItem.setDamageValue(newDamage);
			be.durabilitySinceLastMaterial++;

			if (be.durabilitySinceLastMaterial >= DURABILITY_PER_MATERIAL) {
				be.durabilitySinceLastMaterial = 0;
				materialItem.shrink(1);
			}
			be.setChanged();
			be.syncToClient();
		} else if (shouldRefine != wasRefining) {
			be.syncToClient();
		}
	}

	private void syncToClient() {
		if (this.level != null && !this.level.isClientSide()) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	// ---- salvar / carregar (API nova: ValueOutput / ValueInput, sem HolderLookup.Provider) ----

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, this.items);
		output.putInt("DurabilitySinceLastMaterial", this.durabilitySinceLastMaterial);
		output.putBoolean("Refining", this.refining);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.items.clear();
		ContainerHelper.loadAllItems(input, this.items);
		while (this.items.size() < 2) {
			this.items.add(ItemStack.EMPTY);
		}
		this.durabilitySinceLastMaterial = input.getIntOr("DurabilitySinceLastMaterial", 0);
		this.refining = input.getBooleanOr("Refining", false);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveWithoutMetadata(registries);
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
