package com.example.refinetool.client;

import com.example.refinetool.block.ModBlockEntities;
import com.example.refinetool.client.model.RefineBlockModel;
import com.example.refinetool.client.render.RefineBlockEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class RefineToolClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(RefineBlockModel.LAYER_LOCATION, RefineBlockModel::createBodyLayer);
		// Assinatura confirmada no javadoc oficial da 1.21.11:
		// BlockEntityRenderers.register(BlockEntityType<? extends T>, BlockEntityRendererProvider<T,S>)
		BlockEntityRenderers.register(ModBlockEntities.REFINE_BLOCK_ENTITY, RefineBlockEntityRenderer::new);
	}
}
