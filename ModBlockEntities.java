package com.example.refinetool.block;

import com.example.refinetool.RefineToolMod;
import com.example.refinetool.block.entity.RefineBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {

	private ModBlockEntities() {
	}

	public static final BlockEntityType<RefineBlockEntity> REFINE_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(RefineToolMod.MOD_ID, "refine_block"),
			FabricBlockEntityTypeBuilder.create(RefineBlockEntity::new, ModBlocks.REFINE_BLOCK).build()
	);

	/** So dispara o carregamento estatico da classe (registro do campo acima). */
	public static void register() {
	}
}
