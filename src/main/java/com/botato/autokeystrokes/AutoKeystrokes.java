package com.botato.autokeystrokes;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoKeystrokes implements ModInitializer {
	public static final String MOD_ID = "autokeystrokes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("AutoKeystrokes Initialized!");
	}
}