package com.example.refinetool.client;

import com.example.refinetool.block.ModBlockEntities;
import com.example.refinetool.client.model.RefineBlockModel;
import com.example.refinetool.client.render.RefineBlockEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;

public class RefineToolClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(RefineBlockModel.LAYER_LOCATION, RefineBlockModel::createBodyLayer);
		BlockEntityRendererRegistry.register(ModBlockEntities.REFINE_BLOCK_ENTITY, RefineBlockEntityRenderer::new);
	}
}
