package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.ui.Component;

/**
 * Archs — clean parallax background with robust, gapless horizontal tiling.
 *
 * Compatibility notes for Noosa:
 * - Image.width / Image.height are public fields (no height()/width() methods).
 * - Component doesn't expose requestLayout(); we call layout() directly after param changes.
 * - No extra effects: only horizontal parallax and integer-aligned tiling to avoid seams.
 */
public class Archs extends Component {

	private static final int LAYER_COUNT = 10;
	// Enough tiles to cover ultra-wide screens at small scales.
	private static final int TILE_COUNT  = 13; // 6 left, 1 center, 6 right

	// Parallax scroll speeds (pixels/sec). Layer 0 is slowest, 9 is fastest.
	private static final float BASE_SPEED = 10f;
	private static final float SPEED_STEP = 3f;

	// Persisted offsets so TitleScene re-creations keep motion continuity.
	private static float[] savedOffsets = new float[LAYER_COUNT];
	private static boolean offsetsInit  = false;

	// Sprites arranged per layer.
	private Image[][] tiles; // [layer][0..TILE_COUNT-1]

	// Controls
	private boolean parallax = true;
	public  boolean reversed = false;

	// Sizing
	// By default, sprites are scaled to fit screen height; you can shrink/enlarge with spriteScale.
	private float spriteScale = 0.50f;    // relative to "fit-to-height"
	private float outputHeightPx = -1f;  // if > 0, force output height in px (overrides spriteScale)

	@Override
	protected void layout() {
		float h = (outputHeightPx > 0f) ? outputHeightPx : Game.height;
		float scaleToFit = h / Math.max(1f, tiles != null && tiles[0][TILE_COUNT/2] != null ? tiles[0][TILE_COUNT/2].height : h);
		float s = scaleToFit * spriteScale;

		for (int i = 0; i < LAYER_COUNT; i++) {
			for (int t = 0; t < TILE_COUNT; t++) {
				Image img = tiles[i][t];
				if (img == null) continue;
				img.scale.set(s);
				img.y = 0f;
			}
			positionLayer(i);
		}
	}

	public void setParallaxEnabled(boolean enabled) { this.parallax = enabled; }
	public void setReversed(boolean rev)            { this.reversed = rev;     }
	public void setSpriteScale(float scale)         { this.spriteScale = Math.max(0.05f, scale); layout(); }
	public void setOutputHeightPx(float hPx)        { this.outputHeightPx = hPx; layout(); }

	public static void resetOffsets() {
		for (int i = 0; i < LAYER_COUNT; i++) savedOffsets[i] = 0f;
		offsetsInit = true;
	}

	@Override
	protected void createChildren() {
		// Start title music when the background (Archs) is constructed.
		try { Music.INSTANCE.play(Assets.Music.TITLE, true); }
		catch (Throwable ignore) {}

		if (!offsetsInit) resetOffsets();
		if (tiles == null) tiles = new Image[LAYER_COUNT][TILE_COUNT];

		for (int i = 0; i < LAYER_COUNT; i++) {
			String tex = getTitleAsset(i);
			for (int t = 0; t < TILE_COUNT; t++) {
				Image img;
				try {
					img = new Image(tex);
				} catch (Throwable ignore) {
					img = new Image(); // transparent fallback
					img.alpha(0f);
				}
				tiles[i][t] = img;
				add(img);
			}
		}
		layout();
	}

	@Override
	public void update() {
		super.update();
		if (!parallax) return;

		// Time step
		float dt = Game.elapsed;

		for (int i = 0; i < LAYER_COUNT; i++) {
			float speed = BASE_SPEED + i * SPEED_STEP;
			float dir   = reversed ? -1f : 1f;
			savedOffsets[i] += dir * speed * dt;
			positionLayer(i);
		}
	}

	/**
	 * Re-positions all tiles of a given layer based on its saved offset.
	 * Gapless tiling:
	 *  - Scale each tile uniformly.
	 *  - Compute integer-rounded scaled width; use that to step tiles.
	 *  - Wrap offset into [0, width).
	 *  - Snap base x to an integer pixel.
	 *  - Lay tiles at base + k*width for k in [-6..+6] (TILE_COUNT = 13).
	 */
	private void positionLayer(int i) {
		Image center = tiles[i][TILE_COUNT / 2];
		if (center == null) return;

		// Scaled width, integer-rounded to prevent sub-pixel seams between neighbors.
		float w = Math.max(1f, Math.round(center.width * center.scale.x));

		// Wrap offset into [0, w)
		float off = (w > 0f) ? (savedOffsets[i] % w) : 0f;
		if (off < 0) off += w;

		// Base x so that the center tile begins at -off, snapped to integer pixels.
		float base = -off;
		base = (float) Math.round(base);

		// Place tiles left/right of center so the screen is fully covered.
		int mid = TILE_COUNT / 2;
		for (int t = 0; t < TILE_COUNT; t++) {
			Image img = tiles[i][t];
			if (img == null) continue;
			int k = t - mid;
			float x = base + k * w;
			img.x = x;
			img.y = 0f;
		}
	}

	private String getTitleAsset(int idx) {
		switch (idx) {
			case 0: return Assets.Interfaces.TITLE_0;
			case 1: return Assets.Interfaces.TITLE_1;
			case 2: return Assets.Interfaces.TITLE_2;
			case 3: return Assets.Interfaces.TITLE_3;
			case 4: return Assets.Interfaces.TITLE_4;
			case 5: return Assets.Interfaces.TITLE_5;
			case 6: return Assets.Interfaces.TITLE_6;
			case 7: return Assets.Interfaces.TITLE_7;
			case 8: return Assets.Interfaces.TITLE_8;
			case 9: return Assets.Interfaces.TITLE_9;
		}
		return Assets.Interfaces.TITLE_0;
	}
}
