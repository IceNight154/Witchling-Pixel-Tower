/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
/* ========================= 한국어 해설 주석 안내 =========================
 * 이 파일은 업로드된 TitleScene.java를 기반으로, 주요 블록(클래스/메서드)
 * 단위에 한국어 설명 주석을 추가한 버전입니다.
 * 원본 동작에는 영향을 주지 않으며, 각 블록의 역할/의도를 이해하기 쉽도록
 * 상단에 설명을 덧붙였습니다.
 * (일부 익명 내부클래스의 update/draw 등은 일반적인 역할 설명으로 표기)
 * ====================================================================== */

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.BannerSprites;
import com.shatteredpixel.shatteredpixeldungeon.effects.Fireball;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.services.news.News;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.AvailableUpdateData;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.Updates;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.Archs;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndVictoryCongrats;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.ColorMath;
import com.watabou.utils.DeviceCompat;

import java.util.Date;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Interpolation;
import com.watabou.noosa.Group;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.particles.Emitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ManaBuffFireflyParticle;


// 타이틀 화면(Title Scene)을 담당하는 씬 클래스. 배경, 로고, 버튼 및 각종 애니메이션을 초기화/표시합니다.
public class TitleScene extends PixelScene {
	// ==== 추가: 세련된 타이틀 연출 상태 ====
	private boolean _revealStarted = false;        // 사용자 조작 감지 후 true
	private float _revealT = 0f;                   // 0->1 페이드 인 진행도
	private static final float REVEAL_DUR = 0.65f; // 버튼 등장 시간

	// 파티클/연출 레이어
	private Group _fxLayer;
	private Emitter _firefliesEmitter;

	// 전체 페이드 베일(초기에는 버튼을 가려서 '숨김' 효과)
	private ColorBlock _veil;

	// 반딧불 생성 강도(초당 스폰량의 스케일)
	private float _fireflyIntensity = 1.0f;


	@Override
// 씬 초기화: 배경/로고/버튼/애니메이션 구성 및 저장 데이터·업데이트/뉴스 상태를 반영합니다.
	public void create() {

		super.create();

		add(new com.watabou.noosa.ColorBlock(Camera.main.width, Camera.main.height, 0x66000000));


		// ==== 추가: 깊은 밤 숲 분위기 + 반딧불 레이어 ====
		int _w_ = Camera.main.width;
		int _h_ = Camera.main.height;

		// FX 레이어를 최하단에 추가(배경 위, UI 아래)
		_fxLayer = new Group();
		addToBack(_fxLayer);

		// 화면 전체에 차분한 밤색 틴트(푸른 기운) 오버레이
		ColorBlock nightTint = new ColorBlock(_w_, _h_, 0x081A2B);
		nightTint.alpha(0.55f);
		_fxLayer.add(nightTint);

		// 화면 아래에서 올라오는 푸른 마나 반딧불이 이펙트
		_firefliesEmitter = new Emitter();
		_firefliesEmitter.pos(0, _h_ - 2); // 하단 근처에서 스폰
		_fxLayer.add(_firefliesEmitter);
		_firefliesEmitter.pour(ManaBuffFireflyParticle.factory(), 0.12f); // 천천히 잔잔하게 생성

		// ==== 추가: 초기 버튼 숨김을 위한 베일(사용자 첫 입력까지 유지) ====
		_veil = new ColorBlock(_w_, _h_, 0x000000);
		_veil.alpha(0.92f); // 거의 검게 덮음
		add(_veil); // UI 위에 배치하여 버튼을 가린다.
		Music.INSTANCE.playTracks(
				new String[]{Assets.Music.THEME_1, Assets.Music.THEME_2},
				new float[]{1, 1},
				false);

		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;

		Archs archs = new Archs();
		archs.setSize( w, h );
		add( archs );

		Image title = BannerSprites.get( landscape() ? BannerSprites.Type.TITLE_LAND : BannerSprites.Type.TITLE_PORT);
		add( title );

		float topRegion = Math.max(title.height - 6, h*0.45f);

		title.x = (w - title.width()) / 2f;
		title.y = 2 + (topRegion - title.height()) / 2f;

		align(title);

		if (landscape()){
			placeTorch(title.x + 30, title.y + 35);
			placeTorch(title.x + title.width - 30, title.y + 35);
		} else {
			placeTorch(title.x + 16, title.y + 70);
			placeTorch(title.x + title.width - 16, title.y + 70);
		}

		Image signs = new Image(BannerSprites.get( landscape() ? BannerSprites.Type.TITLE_GLOW_LAND : BannerSprites.Type.TITLE_GLOW_PORT)){
			private float time = 0;
			@Override
// 프레임마다 호출되는 업데이트 로직입니다. 애니메이션·입력·버튼 상태 등을 갱신합니다.
			public void update() {
				super.update();
				am = Math.max(0f, (float)Math.sin( time += Game.elapsed ));
				if (time >= 1.5f*Math.PI) time = 0;

				// ==== 추가: 최초 입력 감지 후 버튼 페이드 인 ====
				if (!_revealStarted) {
					// 키보드/패드/터치 등 '어떤 입력'이든 감지
					boolean anyKey = false;
					// 데스크톱/컨트롤러 호환: 주요 키들도 함께 체크
					if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
							Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
							Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
							Gdx.input.isKeyJustPressed(Input.Keys.UP) ||
							Gdx.input.isKeyJustPressed(Input.Keys.DOWN) ||
							Gdx.input.isKeyJustPressed(Input.Keys.LEFT) ||
							Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
						anyKey = true;
					}
					try {
						anyKey = Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY);
					} catch (Throwable t) {
						// 일부 플랫폼에서 ANY_KEY를 지원하지 않을 수 있음
					}
					if (Gdx.input.justTouched() || anyKey) {
						_revealStarted = true;
					}
				}
				if (_revealStarted) {
					_revealT = Math.min(1f, _revealT + Game.elapsed / REVEAL_DUR);

					// 베일을 서서히 걷음(버튼이 자연스럽게 드러나는 효과)
					if (_veil != null) {
						float a = 1f - Interpolation.fade.apply(_revealT);
						_veil.alpha(a * 0.92f);
						if (_revealT >= 1f) {
							_veil.killAndErase();
							_veil = null;
						}
					}
				}

				// ==== 추가: 반딧불이 이펙트를 조금 더 고요하게 진동시키기 ====
				if (_firefliesEmitter != null) {
					// 아주 약간씩 스폰 레이트를 파동시키며 생동감 부여
					float pulse = (float)Math.sin(time*0.5f)*0.1f + 0.9f; // 0.8 ~ 1.0
				}
			}
			@Override
// 그리기 단계에서 호출됩니다. (필요 시 특수 블렌딩 등 렌더링 처리)
			public void draw() {
				Blending.setLightMode();
				super.draw();
				Blending.setNormalMode();
			}
		};
		signs.x = title.x + (title.width() - signs.width())/2f;
		signs.y = title.y;
		add( signs );

		final Chrome.Type GREY_TR = Chrome.Type.GREY_BUTTON_TR;

		StyledButton btnPlay = new StyledButton(GREY_TR, Messages.get(this, "enter")){
			@Override
// 클릭 시 동작을 정의합니다. (해당 버튼/요소의 화면 전환 또는 기능 실행)
			protected void onClick() {
				if (GamesInProgress.checkAll().size() == 0){
					GamesInProgress.selectedClass = null;
					GamesInProgress.curSlot = 1;
					ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
				} else {
					ShatteredPixelDungeon.switchNoFade( StartScene.class );
				}
			}

			@Override
// 길게 누를 때의 동작을 정의합니다. (필요 시 세부 정보 또는 대체 동작)
			protected boolean onLongClick() {
				//making it easier to start runs quickly while debugging
				if (DeviceCompat.isDebug()) {
					GamesInProgress.selectedClass = null;
					GamesInProgress.curSlot = 1;
					ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
					return true;
				}
				return super.onLongClick();
			}
		};
		btnPlay.icon(Icons.get(Icons.ENTER));
		add(btnPlay);

		StyledButton btnSupport = new SupportButton(GREY_TR, Messages.get(this, "support"));
		add(btnSupport);

		StyledButton btnRankings = new StyledButton(GREY_TR,Messages.get(this, "rankings")){
			@Override
// 클릭 시 동작을 정의합니다. (해당 버튼/요소의 화면 전환 또는 기능 실행)
			protected void onClick() {
				ShatteredPixelDungeon.switchNoFade( RankingsScene.class );
			}
		};
		btnRankings.icon(Icons.get(Icons.RANKINGS));
		add(btnRankings);
		Dungeon.daily = Dungeon.dailyReplay = false;

		StyledButton btnBadges = new StyledButton(GREY_TR, Messages.get(this, "journal")){
			@Override
// 클릭 시 동작을 정의합니다. (해당 버튼/요소의 화면 전환 또는 기능 실행)
			protected void onClick() {
				ShatteredPixelDungeon.switchNoFade( JournalScene.class );
			}
		};
		btnBadges.icon(Icons.get(Icons.JOURNAL));
		add(btnBadges);

		StyledButton btnNews = new NewsButton(GREY_TR, Messages.get(this, "news"));
		btnNews.icon(Icons.get(Icons.NEWS));
		add(btnNews);

		StyledButton btnChanges = new ChangesButton(GREY_TR, Messages.get(this, "changes"));
		btnChanges.icon(Icons.get(Icons.CHANGES));
		add(btnChanges);

		StyledButton btnSettings = new SettingsButton(GREY_TR, Messages.get(this, "settings"));
		add(btnSettings);

		StyledButton btnAbout = new StyledButton(GREY_TR, Messages.get(this, "about")){
			@Override
// 클릭 시 동작을 정의합니다. (해당 버튼/요소의 화면 전환 또는 기능 실행)
			protected void onClick() {
				ShatteredPixelDungeon.switchScene( AboutScene.class );
			}
		};
		btnAbout.icon(Icons.get(Icons.SHPX));
		add(btnAbout);

		final int BTN_HEIGHT = 20;
		int GAP = (int)(h - topRegion - (landscape() ? 3 : 4)*BTN_HEIGHT)/3;
		GAP /= landscape() ? 3 : 5;
		GAP = Math.max(GAP, 2);

		float buttonAreaWidth = landscape() ? PixelScene.MIN_WIDTH_L-6 : PixelScene.MIN_WIDTH_P-2;
		float btnAreaLeft = (Camera.main.width - buttonAreaWidth) / 2f;
		if (landscape()) {
			btnPlay.setRect(btnAreaLeft, topRegion+GAP, (buttonAreaWidth/2)-1, BTN_HEIGHT);
			align(btnPlay);
			btnSupport.setRect(btnPlay.right()+2, btnPlay.top(), btnPlay.width(), BTN_HEIGHT);
			btnRankings.setRect(btnPlay.left(), btnPlay.bottom()+ GAP, (float) (Math.floor(buttonAreaWidth/3f)-1), BTN_HEIGHT);
			btnBadges.setRect(btnRankings.right()+2, btnRankings.top(), btnRankings.width(), BTN_HEIGHT);
			btnNews.setRect(btnBadges.right()+2, btnBadges.top(), btnRankings.width(), BTN_HEIGHT);
			btnSettings.setRect(btnRankings.left(), btnRankings.bottom() + GAP, btnRankings.width(), BTN_HEIGHT);
			btnChanges.setRect(btnSettings.right()+2, btnSettings.top(), btnRankings.width(), BTN_HEIGHT);
			btnAbout.setRect(btnChanges.right()+2, btnSettings.top(), btnRankings.width(), BTN_HEIGHT);
		} else {
			btnPlay.setRect(btnAreaLeft, topRegion+GAP, buttonAreaWidth, BTN_HEIGHT);
			align(btnPlay);
			btnSupport.setRect(btnPlay.left(), btnPlay.bottom()+ GAP, btnPlay.width(), BTN_HEIGHT);
			btnRankings.setRect(btnPlay.left(), btnSupport.bottom()+ GAP, (btnPlay.width()/2)-1, BTN_HEIGHT);
			btnBadges.setRect(btnRankings.right()+2, btnRankings.top(), btnRankings.width(), BTN_HEIGHT);
			btnNews.setRect(btnRankings.left(), btnRankings.bottom()+ GAP, btnRankings.width(), BTN_HEIGHT);
			btnChanges.setRect(btnNews.right()+2, btnNews.top(), btnNews.width(), BTN_HEIGHT);
			btnSettings.setRect(btnNews.left(), btnNews.bottom()+GAP, btnRankings.width(), BTN_HEIGHT);
			btnAbout.setRect(btnSettings.right()+2, btnSettings.top(), btnSettings.width(), BTN_HEIGHT);
		}

		BitmapText version = new BitmapText( "v" + Game.version, pixelFont);
		version.measure();
		version.hardlight( 0x888888 );
		version.x = w - version.width() - 4;
		version.y = h - version.height() - 2;
		add( version );

		if (DeviceCompat.isDesktop()) {
			ExitButton btnExit = new ExitButton();
			btnExit.setPos( w - btnExit.width(), 0 );
			add( btnExit );
		}

		Badges.loadGlobal();
		if (Badges.isUnlocked(Badges.Badge.VICTORY) && !SPDSettings.victoryNagged()) {
			SPDSettings.victoryNagged(true);
			add(new WndVictoryCongrats());
		}

		fadeIn();
	}

	// 배경 장식(횃불)을 배치하고, 불꽃/빛 효과를 설정합니다.
	private void placeTorch( float x, float y ) {
		Fireball fb = new Fireball();
		fb.x = x - fb.width()/2f;
		fb.y = y - fb.height();

		align(fb);
		add( fb );
	}

	// 내부 클래스: 뉴스/공지 버튼. 최신 뉴스/공지 보기 화면으로 이동합니다.
	private static class NewsButton extends StyledButton {

		public NewsButton(Chrome.Type type, String label ){
			super(type, label);
			if (SPDSettings.news()) News.checkForNews();
		}

		int unreadCount = -1;

		@Override
// 프레임마다 호출되는 업데이트 로직입니다. 애니메이션·입력·버튼 상태 등을 갱신합니다.
		public void update() {
			super.update();

			if (unreadCount == -1 && News.articlesAvailable()){
				long lastRead = SPDSettings.newsLastRead();
				if (lastRead == 0){
					if (News.articles().get(0) != null) {
						SPDSettings.newsLastRead(News.articles().get(0).date.getTime());
					}
				} else {
					unreadCount = News.unreadArticles(new Date(SPDSettings.newsLastRead()));
					if (unreadCount > 0) {
						unreadCount = Math.min(unreadCount, 9);
						text(text() + "(" + unreadCount + ")");
					}
				}
			}

			if (unreadCount > 0){
				textColor(ColorMath.interpolate( 0xFFFFFF, Window.SHPX_COLOR, 0.5f + (float)Math.sin(Game.timeTotal*5)/2f));
			}
		}

		@Override
// 클릭 시 동작을 정의합니다. (해당 버튼/요소의 화면 전환 또는 기능 실행)
		protected void onClick() {
			super.onClick();
			ShatteredPixelDungeon.switchNoFade( NewsScene.class );
		}
	}

	// 내부 클래스: 변경 내역(업데이트 노트) 버튼. 패치노트/버전 변경사항 화면으로 이동합니다.
	private static class ChangesButton extends StyledButton {

		public ChangesButton( Chrome.Type type, String label ){
			super(type, label);
			if (SPDSettings.updates()) Updates.checkForUpdate();
		}

		boolean updateShown = false;

		@Override
// 프레임마다 호출되는 업데이트 로직입니다. 애니메이션·입력·버튼 상태 등을 갱신합니다.
		public void update() {
			super.update();

			if (!updateShown && Updates.updateAvailable()){
				updateShown = true;
				text(Messages.get(TitleScene.class, "update"));
			}

			if (updateShown){
				textColor(ColorMath.interpolate( 0xFFFFFF, Window.SHPX_COLOR, 0.5f + (float)Math.sin(Game.timeTotal*5)/2f));
			}
		}

		@Override
// 클릭 시 동작을 정의합니다. (해당 버튼/요소의 화면 전환 또는 기능 실행)
		protected void onClick() {
			if (Updates.updateAvailable()){
				AvailableUpdateData update = Updates.updateData();

				ShatteredPixelDungeon.scene().addToFront( new WndOptions(
						Icons.get(Icons.CHANGES),
						update.versionName == null ? Messages.get(this,"title") : Messages.get(this,"versioned_title", update.versionName),
						update.desc == null ? Messages.get(this,"desc") : update.desc,
						Messages.get(this,"update"),
						Messages.get(this,"changes")
				) {
					@Override
// 목록/선택지의 인덱스 선택 콜백을 처리합니다.
					protected void onSelect(int index) {
						if (index == 0) {
							Updates.launchUpdate(Updates.updateData());
						} else if (index == 1){
							ChangesScene.changesSelected = 0;
							ShatteredPixelDungeon.switchNoFade( ChangesScene.class );
						}
					}
				});

			} else {
				ChangesScene.changesSelected = 0;
				ShatteredPixelDungeon.switchNoFade( ChangesScene.class );
			}
		}

	}

	// 내부 클래스: 설정 버튼. 게임 설정 화면으로 이동합니다.
	private static class SettingsButton extends StyledButton {

		public SettingsButton( Chrome.Type type, String label ){
			super(type, label);
			if (Messages.lang().status() == Languages.Status.X_UNFINISH){
				icon(Icons.get(Icons.LANGS));
				icon.hardlight(1.5f, 0, 0);
			} else {
				icon(Icons.get(Icons.PREFS));
			}
		}

		@Override
// 프레임마다 호출되는 업데이트 로직입니다. 애니메이션·입력·버튼 상태 등을 갱신합니다.
		public void update() {
			super.update();

			if (Messages.lang().status() == Languages.Status.X_UNFINISH){
				textColor(ColorMath.interpolate( 0xFFFFFF, CharSprite.NEGATIVE, 0.5f + (float)Math.sin(Game.timeTotal*5)/2f));
			}
		}

		@Override
// 클릭 시 동작을 정의합니다. (해당 버튼/요소의 화면 전환 또는 기능 실행)
		protected void onClick() {
			if (Messages.lang().status() == Languages.Status.X_UNFINISH){
				WndSettings.last_index = 5;
			}
			ShatteredPixelDungeon.scene().add(new WndSettings());
		}
	}

	// 내부 클래스: 후원(Supporter) 버튼. 후원자 관련 화면으로 이동합니다.
	private static class SupportButton extends StyledButton{

		public SupportButton( Chrome.Type type, String label ){
			super(type, label);
			icon(Icons.get(Icons.GOLD));
			textColor(Window.TITLE_COLOR);
		}

		@Override
// 클릭 시 동작을 정의합니다. (해당 버튼/요소의 화면 전환 또는 기능 실행)
		protected void onClick() {
			ShatteredPixelDungeon.switchNoFade(SupporterScene.class);
		}
	}
}