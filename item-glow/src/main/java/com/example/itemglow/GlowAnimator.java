package com.example.itemglow;

/**
 * Pure math: turns the config plus the current time into a color/size for each glow layer.
 * Layer 0 is the inner layer, the last layer is the outermost.
 */
public final class GlowAnimator {
	private static double phase = 0;
	private static long lastNanos = System.nanoTime();

	private GlowAnimator() {
	}

	/**
	 * Advances the animation clock (scaled by the speed setting) and returns it.
	 * Call once per frame, then pass the result to {@link #color} and {@link #scale}.
	 */
	public static float advance(GlowConfig cfg) {
		long now = System.nanoTime();
		double dt = (now - lastNanos) / 1_000_000_000.0;
		lastNanos = now;
		if (dt > 0.25) {
			dt = 0.25; // e.g. after the game was paused or nothing was held for a while
		}
		phase = (phase + dt * cfg.speed) % 3600.0;
		return (float) phase;
	}

	/** Size multiplier for a layer (1.0 = exactly the item's size). */
	public static float scale(GlowConfig cfg, int layer, int layers, float t) {
		Look look = look(cfg, layer, layers, t);
		float reach = 0.05f + 0.25f * cfg.thickness;
		return 1.0f + reach * look.scale * (layer + 1) / layers;
	}

	/** ARGB tint for a layer. */
	public static int color(GlowConfig cfg, int layer, int layers, float t) {
		Look look = look(cfg, layer, layers, t);
		float hue = cfg.hue + look.hueShift;
		hue -= (float) Math.floor(hue);
		int rgb = hsvToRgb(hue, cfg.saturation, cfg.brightness);

		float falloff = 1.0f - 0.45f * layer / layers; // inner layers are stronger
		float alpha = clamp01(cfg.intensity * look.alpha * falloff);
		int a = Math.round(alpha * 255f);
		return (a << 24) | (rgb & 0xFFFFFF);
	}

	/** How the current mode changes opacity, size and hue over time. */
	private record Look(float alpha, float scale, float hueShift) {
	}

	private static Look look(GlowConfig cfg, int layer, int layers, float t) {
		float outward = layer / (float) layers;
		switch (cfg.mode) {
			case PULSE: {
				// Slow breathing: fades and swells together.
				float s = wave(t * 0.6f);
				return new Look(0.25f + 0.75f * s, 0.85f + 0.3f * s, 0f);
			}
			case RIPPLE: {
				// Brightness travels from the item outward.
				float s = wave(t * 0.8f - outward);
				return new Look(0.12f + 0.88f * s, 1f, 0f);
			}
			case FLICKER: {
				// Two fast waves of different speeds look like a random flicker.
				float n = 0.5f * (wave(t * 3.1f + layer * 0.37f) + wave(t * 7.7f + layer * 0.91f));
				return new Look(0.3f + 0.7f * n * n, 0.95f + 0.1f * n, 0f);
			}
			case RAINBOW:
				return new Look(1f, 1f, t * 0.12f + layer * 0.04f);
			case STROBE:
				return new Look(wave(t * 1.5f) > 0.5f ? 1f : 0.06f, 1f, 0f);
			case STEADY:
			default:
				return new Look(1f, 1f, 0f);
		}
	}

	/** Smooth 0..1 wave with a period of 1.0 in x. */
	private static float wave(float x) {
		return 0.5f + 0.5f * (float) Math.sin(x * 2.0 * Math.PI);
	}

	private static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}

	/** h, s, v all 0..1; returns 0xRRGGBB. */
	public static int hsvToRgb(float h, float s, float v) {
		h = h - (float) Math.floor(h);
		float r;
		float g;
		float b;
		float hh = h * 6f;
		int i = (int) Math.floor(hh);
		float f = hh - i;
		float p = v * (1f - s);
		float q = v * (1f - s * f);
		float u = v * (1f - s * (1f - f));
		switch (i % 6) {
			case 0: r = v; g = u; b = p; break;
			case 1: r = q; g = v; b = p; break;
			case 2: r = p; g = v; b = u; break;
			case 3: r = p; g = q; b = v; break;
			case 4: r = u; g = p; b = v; break;
			default: r = v; g = p; b = q; break;
		}
		return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
	}
}
