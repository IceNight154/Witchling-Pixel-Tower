package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.ui.Archs;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings;
import com.shatteredpixel.shatteredpixeldungeon.effects.BannerSprites;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.services.news.News;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.Updates;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.AvailableUpdateData;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.Badges;

import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.Image;
import com.watabou.noosa.ColorBlock;
import com.watabou.input.PointerEvent;

import java.util.Date;

/**
 * TitleScene (filled v11)
 * - Adds a top→down navy gradient overlay for a “night sky” feel
 * - Replaces feel of bottom→up black gradient with a downward flow
 * - Keeps all prior behaviors (menu, return, idle timeout, etc.)
 */
public class TitleScene extends PixelScene {

	private static boolean sShowMenuOnStart = false;

	private static final float BTN_W   = 120f;
	private static final float BTN_H   = 18f;
	private static final float BTN_GAP = 4f;
	private static final float PADDING = 6f;
	private static final float SLIDE_OFFSET = 24f;

	private static final float IDLE_TIMEOUT = 15f; // seconds

	// Night-sky navy color (ARGB without alpha; alpha set per strip)
	private static final int NIGHT_NAVY = 0x0B1B3A; // deep navy

	private PointerArea inputCatcher;
	private Group       gateLayer;
	private Group       menuLayer;
	private Group       gradientLayer; // new: top→down gradient overlay

	private boolean firstTapDone = false;
	private float   revealT      = 0f;

	private Image titleImage;
	private Image glowImage;

	private float scrollOffset = 0f;
	private float minScroll = 0f;
	private float maxScroll = 0f;
	private float lastDragY = 0f;
	private boolean dragging = false;

	// Idle return state
	private float idleT = 0f;
	private boolean returningToTitle = false;
	private float returnT = 0f;

	private StyledButton[] menuButtons;

	public void create() {

		super.create();
		uiCamera.visible = false;

		Archs archs = new Archs();
		archs.setSize(Camera.main.width, Camera.main.height);
		add(archs);

		// --- Top→down navy gradient overlay (above background, below logo/menu) ---
		gradientLayer = new Group();
		add(gradientLayer);
		buildTopDownGradient();

		gateLayer = new Group();
		add(gateLayer);

		titleImage = new Image(BannerSprites.get( landscape() ? BannerSprites.Type.TITLE_LAND : BannerSprites.Type.TITLE_PORT ));
		gateLayer.add(titleImage);

		glowImage = new Image(BannerSprites.get( landscape() ? BannerSprites.Type.TITLE_GLOW_LAND : BannerSprites.Type.TITLE_GLOW_PORT )){
			private float time = 0f;
			
			public void update(){
				super.update();
				time += Game.elapsed;
				float a = 0.8f + (float)Math.sin(time * 2.0f) * 0.2f;
				alpha(a * (firstTapDone ? Math.max(0f, 1f - revealT) : 1f));
				x = titleImage.x;
				y = titleImage.y;
			}
		};
		gateLayer.add(glowImage);

		centerTitle();

		inputCatcher = new PointerArea(0, 0, Camera.main.width, Camera.main.height) {

			protected void onTouchDown(PointerEvent e){
				lastDragY = e.current.y;
				dragging = firstTapDone;
				idleT = 0f;
			}

			
			protected void onDrag(PointerEvent e) {
				if (firstTapDone && dragging) {
					float dy = e.current.y - lastDragY;
					lastDragY = e.current.y;
					scrollOffset += dy;
					clampScroll();
				}
				idleT = 0f;
			}

			protected void onTouchUp(PointerEvent e){
				dragging = false;
				idleT = 0f;
			}

			
			protected void onClick(PointerEvent event) {
				if (!firstTapDone) {
					firstTapDone = true;
					revealT = 0f;
				}
				idleT = 0f;
			}
		};
		add(inputCatcher);

		menuLayer = new Group();
		menuLayer.visible = false;
		add(menuLayer);

		buildMenu();

		Badges.loadGlobal();

		if (sShowMenuOnStart){
			jumpToMenuState();
		}

		fadeIn();
	}

	/**
	 * Build a simple vertical gradient by stacking semi-transparent color strips.
	 * Top is strongest alpha; fades out toward bottom.
	 */
	private void buildTopDownGradient(){
		gradientLayer.clear();

		float w = Camera.main.width;
		float h = Camera.main.height;

		final int STEPS = 36;          // more steps → smoother gradient
		float stripH = (float)Math.ceil(h / (float)STEPS);
		float baseAlpha = 0.55f;       // overall intensity at the very top

		for (int i = 0; i < STEPS; i++){
			ColorBlock strip = new ColorBlock(w, stripH, NIGHT_NAVY);
			// alpha decreases from top(i=0) to bottom(i=STEPS-1)
			float t = (float)i / (float)(STEPS - 1);
			float a = baseAlpha * (1f - t);
			strip.alpha(a);
			strip.x = 0;
			strip.y = i * stripH;
			gradientLayer.add(strip);
		}
	}

	private void centerTitle(){
		float w = Camera.main.width;
		float h = Camera.main.height;
		titleImage.x = (w - titleImage.width())/2f;
		titleImage.y = (h - titleImage.height())/2f;
		glowImage.x  = titleImage.x;
		glowImage.y  = titleImage.y;
		align(titleImage);
		align(glowImage);
	}

	private void jumpToMenuState(){
		firstTapDone = true;
		revealT = 1f;
		menuLayer.visible = true;
		titleImage.visible = false;
		glowImage.visible = false;
		updateScrollBounds();
		float contentHeight = menuButtons.length * BTN_H + (menuButtons.length - 1) * BTN_GAP;
		float centerX = (Camera.main.width - BTN_W) / 2f;
		float baseTop = (Camera.main.height - contentHeight)/2f;
		for (int i = 0; i < menuButtons.length; i++) {
			float targetY = baseTop + scrollOffset + i*(BTN_H + BTN_GAP);
			StyledButton b = menuButtons[i];
			b.setRect(centerX, targetY, BTN_W, BTN_H);
			align(b);
			b.alpha(1f);
		}
		idleT = 0f;
		returningToTitle = false;
		returnT = 0f;
	}

	private void buildMenu() {
		final Chrome.Type BTN_STYLE = Chrome.Type.GREY_BUTTON_TR;

		StyledButton btnPlay = new StyledButton(BTN_STYLE, Messages.get(this, "enter", "Play")) {
			
			protected void onClick() {
				sShowMenuOnStart = true;
				if (GamesInProgress.checkAll().size() == 0){
					GamesInProgress.selectedClass = null;
					GamesInProgress.curSlot = 1;
					ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
				} else {
					ShatteredPixelDungeon.switchNoFade( StartScene.class );
				}
			}
		};
		btnPlay.icon(Icons.get(Icons.ENTER));
		menuLayer.add(btnPlay);

		StyledButton btnSupport = new StyledButton(BTN_STYLE, Messages.get(this, "support", "Support")) {
			
			protected void onClick() {
				sShowMenuOnStart = true;
				ShatteredPixelDungeon.switchNoFade(SupporterScene.class);
			}
		};
		btnSupport.icon(Icons.get(Icons.GOLD));
		menuLayer.add(btnSupport);

		StyledButton btnRankings = new StyledButton(BTN_STYLE, Messages.get(this, "rankings", "Rankings")) {
			
			protected void onClick() {
				sShowMenuOnStart = true;
				ShatteredPixelDungeon.switchNoFade( RankingsScene.class );
			}
		};
		btnRankings.icon(Icons.get(Icons.RANKINGS));
		menuLayer.add(btnRankings);

		StyledButton btnJournal = new StyledButton(BTN_STYLE, Messages.get(this, "journal", "Journal")) {
			
			protected void onClick() {
				sShowMenuOnStart = true;
				ShatteredPixelDungeon.switchNoFade( JournalScene.class );
			}
		};
		btnJournal.icon(Icons.get(Icons.JOURNAL));
		menuLayer.add(btnJournal);

		StyledButton btnNews = new NewsButton(BTN_STYLE, Messages.get(this, "news", "News"));
		btnNews.icon(Icons.get(Icons.NEWS));
		menuLayer.add(btnNews);

		StyledButton btnChanges = new ChangesButton(BTN_STYLE, Messages.get(this, "changes", "Changes"));
		btnChanges.icon(Icons.get(Icons.CHANGES));
		menuLayer.add(btnChanges);

		StyledButton btnSettings = new StyledButton(BTN_STYLE, Messages.get(this, "settings", "Settings")) {
			
			protected void onClick() {
				if (Messages.lang().status() == Languages.Status.X_UNFINISH){
					WndSettings.last_index = 5;
				}
				ShatteredPixelDungeon.scene().add(new WndSettings());
			}
		};
		btnSettings.icon(Icons.get(Icons.PREFS));
		menuLayer.add(btnSettings);

		StyledButton btnAbout = new StyledButton(BTN_STYLE, Messages.get(this, "about", "About")) {
			
			protected void onClick() {
				sShowMenuOnStart = true;
				ShatteredPixelDungeon.switchScene( AboutScene.class );
			}
		};
		btnAbout.icon(Icons.get(Icons.SHPX));
		menuLayer.add(btnAbout);

		menuButtons = new StyledButton[]{ btnPlay, btnSupport, btnRankings, btnJournal, btnNews, btnChanges, btnSettings, btnAbout };

		for (StyledButton b : menuButtons){
			b.alpha(0f);
		}

		updateScrollBounds();
	}

	private void updateScrollBounds(){
		float contentHeight = menuButtons.length * BTN_H + (menuButtons.length - 1) * BTN_GAP;
		float viewport = Camera.main.height - (PADDING*2);
		float baseTop = (Camera.main.height - contentHeight)/2f;

		if (contentHeight <= viewport){
			minScroll = maxScroll = 0f;
			scrollOffset = 0f;
		} else {
			float topLimitOffset = PADDING - baseTop;
			float bottomLimitOffset = (Camera.main.height - PADDING) - contentHeight - baseTop;
			minScroll = bottomLimitOffset;
			maxScroll = topLimitOffset;
			clampScroll();
		}
	}

	private void clampScroll(){
		if (scrollOffset < minScroll) scrollOffset = minScroll;
		if (scrollOffset > maxScroll) scrollOffset = maxScroll;
	}

	
	public void update() {
		super.update();

		if (firstTapDone && !menuLayer.visible) {
			menuLayer.visible = true;
			updateScrollBounds();
		}
		if (firstTapDone && revealT < 1f && !returningToTitle) {
			revealT = Math.min(1f, revealT + Game.elapsed * 2.0f);
		}

		if (firstTapDone && !returningToTitle){
			idleT += Game.elapsed;
			if (idleT >= IDLE_TIMEOUT){
				returningToTitle = true;
				returnT = 0f;
			}
		}
		if (returningToTitle){
			returnT = Math.min(1f, returnT + Game.elapsed * 2.0f);
			if (returnT >= 1f){
				firstTapDone = false;
				revealT = 0f;
				returningToTitle = false;
				menuLayer.visible = false;
				titleImage.visible = true;
				glowImage.visible = true;
				titleImage.alpha(1f);
				glowImage.alpha(1f);
				centerTitle();
				idleT = 0f;
			}
		}

		if (menuLayer.visible){
			float contentHeight = menuButtons.length * BTN_H + (menuButtons.length - 1) * BTN_GAP;
			float centerX = (Camera.main.width - BTN_W) / 2f;
			float baseTop = (Camera.main.height - contentHeight)/2f;

			for (int i = 0; i < menuButtons.length; i++) {
				float delay = i * 0.08f;
				float local = Math.max(0f, revealT - delay);
				float a = Math.min(1f, local / 0.6f);
				if (returningToTitle){
					a *= (1f - returnT);
				}

				float targetY = baseTop + scrollOffset + i*(BTN_H + BTN_GAP);
				float slide = SLIDE_OFFSET * (1f - a);
				if (returningToTitle){
					slide += SLIDE_OFFSET * returnT * 0.5f;
				}

				StyledButton b = menuButtons[i];
				b.setRect(centerX, targetY + slide, BTN_W, BTN_H);
				align(b);
				b.alpha(a);
			}
		}

		if (titleImage != null){
			float baseY = (Camera.main.height - titleImage.height())/2f;
			float liftOut  = -10f * revealT * (firstTapDone && !returningToTitle ? 1f : 0f);
			float liftIn   = -10f * (1f - returnT) * (returningToTitle ? 1f : 0f);
			titleImage.y = baseY + liftOut + liftIn;

			float titleA = 1f;
			if (firstTapDone && !returningToTitle){
				titleA = Math.max(0f, 1f - revealT);
			} else if (returningToTitle){
				titleA = returnT;
			}
			titleImage.alpha(titleA);
			glowImage.alpha(titleA);
		}
	}

	
	protected void onBackPressed() {
		ShatteredPixelDungeon.instance.finish();
	}

	// --- Inner classes matching origin behaviors ---

	private static class NewsButton extends StyledButton {

		public NewsButton(Chrome.Type type, String label){
			super(type, label);
			if (SPDSettings.news()) News.checkForNews();
		}

		int unreadCount = -1;

		
		public void update(){
			super.update();

			if (unreadCount == -1 && News.articlesAvailable()){
				long lastRead = SPDSettings.newsLastRead();
				if (lastRead == 0){
					if (News.articles().get(0) != null){
						SPDSettings.newsLastRead(News.articles().get(0).date.getTime());
					}
				} else {
					unreadCount = News.unreadArticles(new Date(SPDSettings.newsLastRead()));
					if (unreadCount > 0){
						unreadCount = Math.min(unreadCount, 9);
						text(text() + "(" + unreadCount + ")");
					}
				}
			}
		}

		
		protected void onClick(){
			super.onClick();
			sShowMenuOnStart = true;
			ShatteredPixelDungeon.switchNoFade( NewsScene.class );
		}
	}

	private static class ChangesButton extends StyledButton {

		public ChangesButton(Chrome.Type type, String label){
			super(type, label);
			if (SPDSettings.updates()) Updates.checkForUpdate();
		}

		
		protected void onClick(){
			if (Updates.updateAvailable()){
				final AvailableUpdateData update = Updates.updateData();
				ShatteredPixelDungeon.scene().addToFront(new WndOptions(
						Icons.get(Icons.CHANGES),
						update.versionName == null ? Messages.get(this,"title") : Messages.get(this,"versioned_title", update.versionName),
						update.desc == null ? Messages.get(this,"desc") : update.desc,
						Messages.get(this,"update"),
						Messages.get(this,"changes")
				){
					
					protected void onSelect(int index){
						if (index == 0){
							Updates.launchUpdate(update);
						} else if (index == 1){
							ChangesScene.changesSelected = 0;
							sShowMenuOnStart = true;
							ShatteredPixelDungeon.switchNoFade( ChangesScene.class );
						}
					}
				});
			} else {
				ChangesScene.changesSelected = 0;
				sShowMenuOnStart = true;
				ShatteredPixelDungeon.switchNoFade( ChangesScene.class );
			}
		}
	}
}
