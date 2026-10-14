package com.example.itemglow.mixin;

import com.example.itemglow.GlowRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Hooks the end of BlockModelWrapper.update (the same method Fabric API's own renderer mixin hooks,
 * with the same signature) so the glow layers are added right after the item's normal layer.
 */
@Mixin(BlockModelWrapper.class)
abstract class BlockModelWrapperMixin {
	@Shadow
	@Final
	private List<BakedQuad> quads;

	@Inject(method = "update", at = @At("RETURN"))
	private void itemglow$addGlowLayers(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
										ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed,
										CallbackInfo ci, @Local ItemStackRenderState.LayerRenderState layer) {
		GlowRenderer.handle((ItemModel) (Object) this, this.quads, output, item, resolver, displayContext,
				level, owner, seed, layer);
	}
}
