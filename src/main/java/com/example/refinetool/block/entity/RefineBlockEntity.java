package com.example.refinetool.block.entity;

import com.example.refinetool.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Repairable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Guarda o item a ser consertado e o material de reparo, e faz o conserto
 * gradualmente gastando pouquissimo material comparado a uma bigorna.
 */
public class RefineBlockEntity extends BlockEntity {

	/** Quanta durabilidade cada UNIDADE de material de reparo restaura. Bem mais barato que a bigorna. */
	public static final int DURABILITY_PER_MATERIAL = 50;
	/** A cada quantos ticks o bloco repara 1 ponto de durabilidade (10 ticks = 2x por segundo). */
	public static final int TICKS_PER_REPAIR_STEP = 10;
	/** Quantidade maxima de material que o bloco guarda de uma vez. */
	public static final int MAX_MATERIAL = 16;

	private ItemStack repairItem = ItemStack.EMPTY;
	private ItemStack materialItem = ItemStack.EMPTY;
	private int durabilitySinceLastMaterial = 0;
	private boolean refining = false;

	public RefineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.REFINE_BLOCK_ENTITY, pos, state);
	}

	// ---- usado pelo renderer para desenhar o item e escolher a animacao ----

	public ItemStack getRepairItem() {
		return this.repairItem;
	}

	public boolean isRefining() {
		return this.refining;
	}

	public boolean hasRepairItem() {
		return !this.repairItem.isEmpty();
	}

	// ---- chamado pelo RefineBlock ao interagir ----

	public boolean canAcceptAsRepairItem(ItemStack stack) {
		return this.repairItem.isEmpty() && !stack.isEmpty() && stack.isDamageableItem();
	}

	public boolean canAcceptAsMaterial(ItemStack stack) {
		if (this.repairItem.isEmpty() || stack.isEmpty()) {
			return false;
		}
		if (this.materialItem.getCount() >= MAX_MATERIAL) {
			return false;
		}
		if (!this.materialItem.isEmpty() && !ItemStack.isSameItemSameComponents(this.materialItem, stack)) {
			return false;
		}
		Repairable repairable = this.repairItem.get(DataComponents.REPAIRABLE);
		if (repairable == null) {
			return false;
		}
		return repairable.items().contains(stack.getItem().builtInRegistryHolder());
	}

	/** So chame depois de conferir canAcceptAsRepairItem(stack). Retira 1 unidade da stack recebida. */
	public void insertRepairItem(ItemStack stack) {
		this.repairItem = stack.split(1);
		setChanged();
		syncToClient();
	}

	/** So chame depois de conferir canAcceptAsMaterial(stack). */
	public void insertMaterial(ItemStack stack) {
		int space = MAX_MATERIAL - this.materialItem.getCount();
		int moved = Math.min(space, stack.getCount());
		if (this.materialItem.isEmpty()) {
			this.materialItem = stack.split(moved);
		} else {
			this.materialItem.grow(moved);
			stack.shrink(moved);
		}
		setChanged();
		syncToClient();
	}

	/** Retira o item guardado (consertado ou nao) e devolve para o jogador. */
	public ItemStack extractRepairItem() {
		ItemStack result = this.repairItem;
		this.repairItem = ItemStack.EMPTY;
		this.durabilitySinceLastMaterial = 0;
		this.refining = false;
		setChanged();
		syncToClient();
		return result;
	}

	/** Retira o material guardado (usado quando o bloco e quebrado, para nao perder o item). */
	public ItemStack extractMaterial() {
		ItemStack result = this.materialItem;
		this.materialItem = ItemStack.EMPTY;
		setChanged();
		return result;
	}

	// ---- tick do servidor ----

	public static void serverTick(Level level, BlockPos pos, BlockState state, RefineBlockEntity be) {
		boolean wasRefining = be.refining;
		boolean shouldRefine = !be.repairItem.isEmpty()
				&& be.repairItem.isDamaged()
				&& !be.materialItem.isEmpty();

		be.refining = shouldRefine;

		if (shouldRefine && level.getGameTime() % TICKS_PER_REPAIR_STEP == 0) {
			int newDamage = Math.max(0, be.repairItem.getDamageValue() - 1);
			be.repairItem.setDamageValue(newDamage);
			be.durabilitySinceLastMaterial++;

			if (be.durabilitySinceLastMaterial >= DURABILITY_PER_MATERIAL) {
				be.durabilitySinceLastMaterial = 0;
				be.materialItem.shrink(1);
			}
			be.setChanged();
			be.syncToClient();
		} else if (shouldRefine != wasRefining) {
			be.syncToClient();
		}
	}

	private void syncToClient() {
		if (this.level != null && !this.level.isClientSide) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	// ---- salvar / carregar ----

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (!this.repairItem.isEmpty()) {
			tag.put("RepairItem", this.repairItem.save(registries, new CompoundTag()));
		}
		if (!this.materialItem.isEmpty()) {
			tag.put("MaterialItem", this.materialItem.save(registries, new CompoundTag()));
		}
		tag.putInt("DurabilitySinceLastMaterial", this.durabilitySinceLastMaterial);
		tag.putBoolean("Refining", this.refining);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.repairItem = ItemStack.parseOptional(registries, tag.getCompoundOrEmpty("RepairItem"));
		this.materialItem = ItemStack.parseOptional(registries, tag.getCompoundOrEmpty("MaterialItem"));
		this.durabilitySinceLastMaterial = tag.getIntOr("DurabilitySinceLastMaterial", 0);
		this.refining = tag.getBooleanOr("Refining", false);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		this.saveAdditional(tag, registries);
		return tag;
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
