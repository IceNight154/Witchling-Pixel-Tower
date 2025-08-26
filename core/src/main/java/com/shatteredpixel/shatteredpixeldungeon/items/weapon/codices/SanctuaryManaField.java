package com.shatteredpixel.shatteredpixeldungeon.items.weapon.codices;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ManaBuffFireflyParticle;
import com.watabou.noosa.particles.Emitter;

import java.util.ArrayList;

public class SanctuaryManaField extends Codex {

    private static final String AC_CAST = "CAST";

    // 효과 범위(타일), 지속, 수치
    private static final int RADIUS = 2;
    private static final float DURATION_BLESS = 20f;
    private static final float DURATION_HASTE = 5f;
    private static final int SHIELD_AMOUNT_BASE = 10;    // 기본 쉴드량
    private static final int SHIELD_AMOUNT_PER_TIER = 6; // 티어당 증가

    {
        image = ItemSpriteSheet.CODEX_MANAFIRELD;
        tier = 5;
        baseUses = 10;
        spawnedForEffect = false;

        // 기본 액션 CAST로
        defaultAction = AC_CAST;
    }

    @Override
    public int defaultQuantity() {
        return 1; // 소비형 1개 기본. 필요시 변경
    }

    //항상 baseUses 기준으로 소모되도록 고정
    @Override
    public float durabilityPerUse(int level) {
        float usages = baseUses; // 시전 횟수는 항상 baseUses 기준
        if (useRoundingInDurabilityCalc){
            usages = Math.max(1f, Math.round(usages));
            return (MAX_DURABILITY / usages) + 0.001f; // Codex와 동일하게 epsilon 추가
        } else {
            return MAX_DURABILITY / Math.max(1f, usages);
        }
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        // 즉발 아이템이므로 던지기/장착 제거
        actions.remove(AC_THROW);
        actions.remove(AC_EQUIP);
        if (!actions.contains(AC_CAST)) actions.add(AC_CAST);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        if (AC_CAST.equals(action)) {
            curUser = hero;              // ★ 중요: 분리(detach) 경로 보장
            onCast(hero);
        } else {
            super.execute(hero, action);
        }
    }

    // onCast(...)의 끝부분을 아래처럼 보강
    private void onCast(Hero hero) {
        if (hero == null) {
            GLog.w(Messages.get(this, "cant_use"));
            return;
        }

        // 연출
        Sample.INSTANCE.play(Assets.Sounds.READ);
        hero.sprite.centerEmitter().burst(Speck.factory(Speck.LIGHT), 12);
        CellEmitter.center(hero.pos).burst(Speck.factory(Speck.HEALING), 6);

        // 적용 수치 계산(티어/강화 반영 여지)
        int shield = SHIELD_AMOUNT_BASE + SHIELD_AMOUNT_PER_TIER * Math.max(1, tier);

        // 범위 내 아군에게 버프 적용
        for (Char ch : Actor.chars()) {
            if (ch == null || !isAlly(hero, ch)) continue;
            if (!inRange(hero.pos, ch.pos, RADIUS)) continue;

            // 버프들
            Buff.affect(ch, Barrier.class).setShield(shield);
            Buff.prolong(ch, Bless.class, DURATION_BLESS);
            Buff.prolong(ch, Haste.class, DURATION_HASTE);

            // 파티클
            Emitter em = ch.sprite.centerEmitter();                // 대상 중심 2미터
            Emitter.Factory WIDEST = new ManaBuffFireflyParticle.ParamFactory(2.5f);
            em.start(ManaBuffFireflyParticle.FACTORY, 1f, 14);  // 흩뿌림 시간
            // 한 번 더 순간 터뜨리는 느낌
            em.burst(ManaBuffFireflyParticle.FACTORY, 6);
        }

        hero.spendAndNext(1f);
        spendUse();                              // 1회 시전 = 1회 소모

        // 버프 적용 로그
        GLog.p(Messages.get(this, "buff_applied"));

        // PATCH: 남은 횟수 로그 동기화 (unlimited 방지용 가드 포함)
        float perUse = durabilityPerUse();
        if (perUse > 0f && quantity > 0) {       // 파괴되지 않았을 때만 출력
            int left = Math.max(0, (int)Math.ceil(durabilityLeft() / perUse));
            int total = Math.max(1, (int)Math.ceil(MAX_DURABILITY / perUse));
            // Codex의 공용 메시지 키 사용 (번역/서식 재활용)
            GLog.i(Messages.get(Codex.class, "uses_left", left, total));
        }
    }

    // 아군 판정: 영웅 본인 또는 적이 아닌 유닛(ALLY/NEUTRAL/소환수 등)
    private boolean isAlly(Hero hero, Char ch) {
        if (ch == hero) return true;
        // Alignment가 있는 빌드 기준(프로젝트에 맞게 보완 가능)
        return ch.alignment != Char.Alignment.ENEMY;
    }

    private boolean inRange(int from, int to, int r) {
        int w = Dungeon.level.width();
        int fx = from % w, fy = from / w;
        int tx = to % w, ty = to / w;
        int dx = fx - tx, dy = fy - ty;
        return dx*dx + dy*dy <= r*r;
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc");
    }
}
