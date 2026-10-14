package com.example.itemglow;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemGlowClient implements ClientModInitializer {
	public static final String MOD_ID = "itemglow";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Shows up in Options > Controls > Key Binds under "Item Glow", where it can be rebound. */
	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	public static KeyMapping openGuiKey;

	@Override
	public void onInitializeClient() {
		GlowConfig.load();

		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.itemglow.open",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_G,
				CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openGuiKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new GlowScreen());
				}
			}
		});

		LOGGER.info("Item Glow loaded. Press G (rebindable in Controls) to open the glow menu.");
	}
}
