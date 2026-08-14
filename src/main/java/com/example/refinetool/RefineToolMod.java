package com.example.refinetool;

import com.example.refinetool.block.ModBlockEntities;
import com.example.refinetool.block.ModBlocks;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefineToolMod implements ModInitializer {

	public static final String MOD_ID = "refinetool";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.register();
		ModBlockEntities.register();
		LOGGER.info("[Refine Tool] Bloco de refino registrado.");
	}
}
