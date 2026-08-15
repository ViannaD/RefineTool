package com.example.refinetool.block;

import com.example.refinetool.RefineToolMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {

	private ModBlocks() {
	}

	public static final Identifier REFINE_BLOCK_ID = Identifier.fromNamespaceAndPath(RefineToolMod.MOD_ID, "refine_block");

	public static final RefineBlock REFINE_BLOCK = Registry.register(
			BuiltInRegistries.BLOCK,
			REFINE_BLOCK_ID,
			new RefineBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.WOOD)
					.strength(2.5F, 6.0F)
					.noOcclusion())
	);

	public static final BlockItem REFINE_BLOCK_ITEM = Registry.register(
			BuiltInRegistries.ITEM,
			REFINE_BLOCK_ID,
			new BlockItem(REFINE_BLOCK, new Item.Properties())
	);

	/** So dispara o carregamento estatico da classe (registro dos campos acima). */
	public static void register() {
	}
}
