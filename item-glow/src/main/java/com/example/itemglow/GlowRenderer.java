package com.example.itemglow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

/**
 * Adds the glow to held items.
 *
 * How it works: after Minecraft builds the render state for a held item, we ask the same item model
 * to build it again a few more times. Each extra copy is turned into a glow layer: its quads are
 * enlarged a little around the item's center, tinted with the animated glow color, made
 * fullbright and drawn translucent. The enlarged, tinted copies poke out around the item's edge,
 * which looks like a glowing outline.
 */
public final class GlowRenderer {
	/** Number of glow layers drawn around the item. */
	public static final int PASSES = 3;

	// Only touched from the render thread.
	private static boolean building = false;
	private static int currentPass = 0;
	private static float frameTime = 0f;

	private GlowRenderer() {
	}

	/** Called from the mixin after a block-model item finished updating its render state. */
	public static void handle(ItemModel model, List<BakedQuad> sourceQuads, ItemStackRenderState output,
							  ItemStack item, ItemModelResolver resolver, ItemDisplayContext context,
							  ClientLevel level, ItemOwner owner, int seed,
							  ItemStackRenderState.LayerRenderState layer) {
		if (building) {
			// We are inside our own extra update: this layer becomes a glow layer.
			applyGlowLayer(layer, sourceQuads, currentPass);
			return;
		}

		GlowConfig cfg = GlowConfig.get();
		if (!shouldGlow(cfg, context, item, owner)) {
			return;
		}

		building = true;
		try {
			frameTime = GlowAnimator.advance(cfg);
			for (int pass = 0; pass < PASSES; pass++) {
				currentPass = pass;
				model.update(output, item, resolver, context, level, owner, seed);
			}
		} finally {
			building = false;
		}
	}

	private static boolean shouldGlow(GlowConfig cfg, ItemDisplayContext context, ItemStack item, ItemOwner owner) {
		if (!cfg.enabled || item.isEmpty()) {
			return false;
		}
		if (context.firstPerson()) {
			return true;
		}
		if (cfg.thirdPerson
				&& (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
				|| context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND)) {
			return owner != null && owner == Minecraft.getInstance().player;
		}
		return false;
	}

	private static void applyGlowLayer(ItemStackRenderState.LayerRenderState layer, List<BakedQuad> sourceQuads, int pass) {
		GlowConfig cfg = GlowConfig.get();
		float scale = GlowAnimator.scale(cfg, pass, PASSES, frameTime);
		int color = GlowAnimator.color(cfg, pass, PASSES, frameTime);

		List<BakedQuad> out = layer.prepareQuadList();
		out.clear();
		boolean fromBlockAtlas = false;
		for (BakedQuad quad : sourceQuads) {
			out.add(glowQuad(quad, scale));
			if (TextureAtlas.LOCATION_BLOCKS.equals(quad.sprite().atlasLocation())) {
				fromBlockAtlas = true;
			}
		}

		layer.setRenderType(fromBlockAtlas ? Sheets.translucentBlockItemSheet() : Sheets.translucentItemSheet());
		layer.setFoilType(ItemStackRenderState.FoilType.NONE);
		// Every glow quad uses tint index 0, so this one color tints the whole layer.
		layer.prepareTintLayers(1)[0] = color;
	}

	/** Copy of a quad, scaled around the model center (0.5, 0.5, 0.5), tint index 0, fullbright, no shading. */
	private static BakedQuad glowQuad(BakedQuad q, float scale) {
		return new BakedQuad(
				scaled(q.position0(), scale),
				scaled(q.position1(), scale),
				scaled(q.position2(), scale),
				scaled(q.position3(), scale),
				q.packedUV0(),
				q.packedUV1(),
				q.packedUV2(),
				q.packedUV3(),
				0,
				q.direction(),
				q.sprite(),
				false,
				15
		);
	}

	private static Vector3f scaled(Vector3fc p, float scale) {
		return new Vector3f(
				0.5f + (p.x() - 0.5f) * scale,
				0.5f + (p.y() - 0.5f) * scale,
				0.5f + (p.z() - 0.5f) * scale
		);
	}
}
