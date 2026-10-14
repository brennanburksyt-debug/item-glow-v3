package com.example.itemglow;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/** The settings menu: color wheel, brightness slider and animation options. */
public class GlowScreen extends Screen {
	private static final int PANEL_W = 320;
	private static final int PANEL_H = 226;

	private static final int WHEEL_R = 60;
	private static final int CELL = 2;

	// Precomputed wheel cells: hue and saturation for each, so drawing is cheap.
	private static final int CELLS = (WHEEL_R * 2) / CELL;
	private static final float[] CELL_HUE = new float[CELLS * CELLS];
	private static final float[] CELL_SAT = new float[CELLS * CELLS];
	private static final boolean[] CELL_IN = new boolean[CELLS * CELLS];

	static {
		for (int cy = 0; cy < CELLS; cy++) {
			for (int cx = 0; cx < CELLS; cx++) {
				float dx = (cx + 0.5f) * CELL - WHEEL_R;
				float dy = (cy + 0.5f) * CELL - WHEEL_R;
				float dist = (float) Math.sqrt(dx * dx + dy * dy);
				int i = cy * CELLS + cx;
				CELL_IN[i] = dist <= WHEEL_R;
				CELL_SAT[i] = Math.min(dist / WHEEL_R, 1f);
				float hue = (float) (Math.atan2(dy, dx) / (2.0 * Math.PI));
				CELL_HUE[i] = hue - (float) Math.floor(hue);
			}
		}
	}

	private int panelX;
	private int panelY;
	private int wheelCx;
	private int wheelCy;
	private boolean draggingWheel = false;
	private boolean wasMouseDown = false;

	public GlowScreen() {
		super(Component.literal("Item Glow"));
	}

	@Override
	protected void init() {
		GlowConfig cfg = GlowConfig.get();
		panelX = (this.width - PANEL_W) / 2;
		panelY = (this.height - PANEL_H) / 2;
		wheelCx = panelX + 20 + WHEEL_R;
		wheelCy = panelY + 32 + WHEEL_R;

		// Brightness slider sits under the wheel.
		addRenderableWidget(new ValueSlider(panelX + 20, panelY + 158, WHEEL_R * 2, 20,
				"Brightness", cfg.brightness,
				v -> cfg.brightness = (float) v,
				v -> Math.round(v * 100) + "%"));

		int rx = panelX + 160;
		int rw = 150;
		int y = panelY + 30;

		addRenderableWidget(Button.builder(toggleLabel("Glow", cfg.enabled), b -> {
			cfg.enabled = !cfg.enabled;
			b.setMessage(toggleLabel("Glow", cfg.enabled));
		}).pos(rx, y).size(rw, 20).build());
		y += 24;

		addRenderableWidget(Button.builder(modeLabel(cfg), b -> {
			cfg.mode = cfg.mode.next();
			b.setMessage(modeLabel(cfg));
		}).pos(rx, y).size(rw, 20).build());
		y += 24;

		addRenderableWidget(new ValueSlider(rx, y, rw, 20,
				"Speed", (cfg.speed - 0.1) / 3.9,
				v -> cfg.speed = (float) (0.1 + v * 3.9),
				v -> String.format("%.1fx", 0.1 + v * 3.9)));
		y += 24;

		addRenderableWidget(new ValueSlider(rx, y, rw, 20,
				"Intensity", cfg.intensity,
				v -> cfg.intensity = (float) v,
				v -> Math.round(v * 100) + "%"));
		y += 24;

		addRenderableWidget(new ValueSlider(rx, y, rw, 20,
				"Thickness", cfg.thickness,
				v -> cfg.thickness = (float) v,
				v -> Math.round(v * 100) + "%"));
		y += 24;

		addRenderableWidget(Button.builder(toggleLabel("Third person", cfg.thirdPerson), b -> {
			cfg.thirdPerson = !cfg.thirdPerson;
			b.setMessage(toggleLabel("Third person", cfg.thirdPerson));
		}).pos(rx, y).size(rw, 20).build());

		int by = panelY + PANEL_H - 28;
		addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
			GlowConfig.resetToDefaults();
			this.rebuildWidgets();
		}).pos(rx, by).size(72, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
				.pos(rx + 78, by).size(72, 20).build());
	}

	private static Component toggleLabel(String name, boolean on) {
		return Component.literal(name + ": " + (on ? "ON" : "OFF"));
	}

	private static Component modeLabel(GlowConfig cfg) {
		return Component.literal("Animation: " + cfg.mode.label);
	}

	@Override
	public boolean isPauseScreen() {
		// Keep the world running so the held item animates behind the menu.
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// Intentionally empty: no blur or darkening, so you can see the glow on your held item.
	}

	@Override
	public void removed() {
		GlowConfig.save();
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		updateWheelDrag(mouseX, mouseY);

		GlowConfig cfg = GlowConfig.get();

		// Panel
		g.fill(panelX - 1, panelY - 1, panelX + PANEL_W + 1, panelY + PANEL_H + 1, 0xFF000000);
		g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xE0181820);
		g.drawString(this.font, "Item Glow", panelX + 10, panelY + 10, 0xFFFFFFFF, true);
		g.drawString(this.font, "Hold an item to preview", panelX + 160 + 6, panelY + 10, 0xFF9090A0, false);

		drawWheel(g, cfg);
		drawPreviewStrip(g, cfg);

		super.render(g, mouseX, mouseY, delta);
	}

	private void drawWheel(GuiGraphics g, GlowConfig cfg) {
		float shown = 0.2f + 0.8f * cfg.brightness; // never fully black so the wheel stays usable
		int x0 = wheelCx - WHEEL_R;
		int y0 = wheelCy - WHEEL_R;

		g.fill(x0 - 2, y0 - 2, x0 + WHEEL_R * 2 + 2, y0 + WHEEL_R * 2 + 2, 0xFF0A0A10);

		for (int cy = 0; cy < CELLS; cy++) {
			for (int cx = 0; cx < CELLS; cx++) {
				int i = cy * CELLS + cx;
				if (!CELL_IN[i]) {
					continue;
				}
				int rgb = GlowAnimator.hsvToRgb(CELL_HUE[i], CELL_SAT[i], shown);
				int px = x0 + cx * CELL;
				int py = y0 + cy * CELL;
				g.fill(px, py, px + CELL, py + CELL, 0xFF000000 | rgb);
			}
		}

		// Marker at the chosen color
		double angle = cfg.hue * 2.0 * Math.PI;
		int mx = wheelCx + (int) Math.round(Math.cos(angle) * cfg.saturation * WHEEL_R);
		int my = wheelCy + (int) Math.round(Math.sin(angle) * cfg.saturation * WHEEL_R);
		g.fill(mx - 4, my - 4, mx + 5, my + 5, 0xFF000000);
		g.fill(mx - 3, my - 3, mx + 4, my + 4, 0xFFFFFFFF);
		int chosen = GlowAnimator.hsvToRgb(cfg.hue, cfg.saturation, cfg.brightness);
		g.fill(mx - 2, my - 2, mx + 3, my + 3, 0xFF000000 | chosen);
	}

	/** A small animated bar that shows the current color and animation. */
	private void drawPreviewStrip(GuiGraphics g, GlowConfig cfg) {
		int x = panelX + 20;
		int y = panelY + 186;
		int w = WHEEL_R * 2;
		int h = 22;
		g.fill(x, y, x + w, y + h, 0xFF0A0A10);

		if (!cfg.enabled) {
			return;
		}
		// Stack every glow layer's color on top of each other, like the real glow does.
		float t = GlowAnimator.advance(cfg);
		int layers = GlowRenderer.PASSES;
		for (int layer = layers - 1; layer >= 0; layer--) {
			g.fill(x + 2, y + 2, x + w - 2, y + h - 2, GlowAnimator.color(cfg, layer, layers, t));
		}
	}

	private void updateWheelDrag(int mouseX, int mouseY) {
		boolean down = GLFW.glfwGetMouseButton(this.minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		if (down) {
			float dx = mouseX - wheelCx;
			float dy = mouseY - wheelCy;
			boolean inside = dx * dx + dy * dy <= (WHEEL_R + 2) * (WHEEL_R + 2);
			if (!wasMouseDown && inside) {
				draggingWheel = true;
			}
			if (draggingWheel) {
				GlowConfig cfg = GlowConfig.get();
				float dist = (float) Math.sqrt(dx * dx + dy * dy);
				cfg.saturation = Math.min(dist / WHEEL_R, 1f);
				float hue = (float) (Math.atan2(dy, dx) / (2.0 * Math.PI));
				cfg.hue = hue - (float) Math.floor(hue);
			}
		} else {
			draggingWheel = false;
		}
		wasMouseDown = down;
	}

	/** A vanilla-style slider that reports its 0..1 value through a callback. */
	private static class ValueSlider extends AbstractSliderButton {
		private final String name;
		private final DoubleConsumer onChange;
		private final DoubleFunction<String> format;

		ValueSlider(int x, int y, int w, int h, String name, double value01,
					DoubleConsumer onChange, DoubleFunction<String> format) {
			super(x, y, w, h, Component.empty(), Math.max(0, Math.min(1, value01)));
			this.name = name;
			this.onChange = onChange;
			this.format = format;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal(name + ": " + format.apply(this.value)));
		}

		@Override
		protected void applyValue() {
			onChange.accept(this.value);
		}
	}
}
