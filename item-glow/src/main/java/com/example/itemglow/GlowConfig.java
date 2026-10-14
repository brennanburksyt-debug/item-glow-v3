package com.example.itemglow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** All user-adjustable glow settings. Saved as config/itemglow.json. */
public class GlowConfig {
	public enum Mode {
		STEADY("Steady"),
		PULSE("Pulse"),
		RIPPLE("Ripple"),
		FLICKER("Flicker"),
		RAINBOW("Rainbow"),
		STROBE("Strobe");

		public final String label;

		Mode(String label) {
			this.label = label;
		}

		public Mode next() {
			Mode[] all = values();
			return all[(ordinal() + 1) % all.length];
		}
	}

	public boolean enabled = true;
	/** Color wheel angle, 0..1. */
	public float hue = 0.78f;
	/** Color wheel distance from the center, 0..1. */
	public float saturation = 1.0f;
	/** Brightness slider, 0..1. */
	public float brightness = 1.0f;
	public Mode mode = Mode.PULSE;
	/** Animation speed multiplier, 0.1..4. */
	public float speed = 1.0f;
	/** Glow strength (opacity), 0..1. */
	public float intensity = 0.8f;
	/** How far the glow reaches out, 0..1. */
	public float thickness = 0.5f;
	/** Also show the glow on the item in your hand in third person. */
	public boolean thirdPerson = false;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static GlowConfig instance = new GlowConfig();

	public static GlowConfig get() {
		return instance;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("itemglow.json");
	}

	public static void load() {
		Path path = file();
		if (Files.exists(path)) {
			try {
				GlowConfig loaded = GSON.fromJson(Files.readString(path), GlowConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (Exception e) {
				ItemGlowClient.LOGGER.warn("Could not read {}, using defaults", path, e);
			}
		}
		instance.sanitize();
	}

	public static void save() {
		instance.sanitize();
		try {
			Files.writeString(file(), GSON.toJson(instance));
		} catch (IOException e) {
			ItemGlowClient.LOGGER.warn("Could not save {}", file(), e);
		}
	}

	public static void resetToDefaults() {
		instance = new GlowConfig();
	}

	private void sanitize() {
		if (mode == null) {
			mode = Mode.PULSE;
		}
		hue = clamp(hue, 0f, 1f);
		saturation = clamp(saturation, 0f, 1f);
		brightness = clamp(brightness, 0f, 1f);
		speed = clamp(speed, 0.1f, 4f);
		intensity = clamp(intensity, 0f, 1f);
		thickness = clamp(thickness, 0f, 1f);
	}

	private static float clamp(float v, float lo, float hi) {
		return Math.max(lo, Math.min(hi, v));
	}
}
