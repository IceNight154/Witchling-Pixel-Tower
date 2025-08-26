
package com.shatteredpixel.shatteredpixeldungeon.items.weapon.codices;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ManaSwordSweepParticle;
import com.watabou.noosa.particles.Emitter;

/**
 * Magic Slash (instant, short‑range Codex)
 * - 즉발형(instant): castDelay() == 0
 * - 전방 2타: 선택한 방향으로 1칸/2칸 순서로 최대 2회 공격
 *   * 1칸에 적이 있으면 먼저 타격, 2칸에도 적이 있으면 이어서 타격
 *   * 1칸에만 적이 있으면 같은 대상에게 2번째 타를 한번 더 시도
 * - 근거리 전용: 원거리 투척/발사 대신 onThrow()에서 인접/직선 2칸만 처리
 */
public class MagicSlash extends Codex {

    {
        tier = 2;
        image = ItemSpriteSheet.CODEX_ARROW;
        baseUses = 50;
    }

    @Override
    public int max(int lvl) {
        return super.max(lvl);
    }

    @Override
    public int min(int lvl) {
        return super.min(lvl);
    }

    public void throwSound() {
        Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1, Random.Float(0.87f, 1.15f));
    }

    @Override
    public String name() {
        return "Magic Slash";
    }

    @Override
    public String desc() {
        return "즉발형 근거리 코덱스.\n" +
                "지정한 방향으로 2연속의 마법 참격을 가합니다. " +
                "전방 1칸과 2칸에 있는 적을 차례로 벤 뒤, 2칸째에 적이 없다면 같은 대상을 한 번 더 벱니다.";
    }

    // 즉발형: 시전 지연 0
    @Override
    public float castDelay(Char user, int cell) {
        return 0f;
    }

    // 투척 처리 대신, 지정 칸을 기준으로 '방향'을 잡아 2연속 근거리 공격을 수행
    @Override
    protected void onThrow(int cell) {
        if (!(curUser instanceof Hero)) {
            parent = null;
            return;
        }

        final Hero hero = (Hero) curUser;
        final int w = Dungeon.level.width();

        // 방향 벡터 계산(4방 우선): 더 큰 축을 택해 대각 입력을 4방으로 정규화
        int hx = hero.pos % w, hy = hero.pos / w;
        int tx = cell % w,  ty = cell / w;
        int dx = Integer.signum(tx - hx);
        int dy = Integer.signum(ty - hy);
        if (Math.abs(tx - hx) > Math.abs(ty - hy)) {
            dy = 0;
        } else if (Math.abs(ty - hy) > Math.abs(tx - hx)) {
            dx = 0;
        } else if (dx == 0 && dy == 0) {
            // 같은 칸을 지정한 경우: 마지막 바라보는 방향이 없다면 아무 일도 하지 않음
            parent = null;
            return;
        }

        int cell1 = hero.pos + dx + dy * w;        // 전방 1칸
        int cell2 = cell1  + dx + dy * w;          // 전방 2칸

        int hitsDone = 0;

        // 1타: 1칸에 적이 있으면 먼저 공격
        Char c1 = Actor.findChar(cell1);
        if (c1 != null && c1 != hero) {
            performStrike(hero, c1, cell1);
            hitsDone++;
        }

        // 2타: 2칸에 적이 있으면 그 적을, 아니면 1칸 대상에게 다시 한 번
        if (hitsDone < 2) {
            Char c2 = Actor.findChar(cell2);
            if (c2 != null && c2 != hero) {
                performStrike(hero, c2, cell2);
                hitsDone++;
            } else if (c1 != null && c1.isAlive() && c1 != hero) {
                performStrike(hero, c1, cell1);
                hitsDone++;
            }
        }

        if (hitsDone == 0) {
            // 빗나감 처리(효과만 소모)
            rangedMiss(cell1);
        }

        // 사용 1회 소모(내구도/스택 처리)
        spendUse();

        // 다시 가방으로
        if (durabilityLeft() > 0) {
            this.collect();
        }

        updateQuickslot();
        parent = null;
    }

    // 단일 타를 수행하는 헬퍼
    private void performStrike(Hero hero, Char target, int atCell) {

        // ManaSwordSweepParticle: generate a forward arc sweep at the target cell
        {
            final int w = Dungeon.level.width();
            int hx = hero.pos % w, hy = hero.pos / w;
            int tx = atCell % w,  ty = atCell / w;
            float baseAng = (float)Math.atan2(ty - hy, tx - hx);
            float span = (float)Math.PI / 3f; // 60 degrees
            float startAng = baseAng - span * 0.5f;
            float endAng   = baseAng + span * 0.5f;
            // rMin/rMax tuned for a bold sweep look
            Emitter.Factory arc = new ManaSwordSweepParticle.BurstArcFactory(startAng, endAng, 4f, 12f, +1);
            CellEmitter.center(atCell).burst(arc, 18);
            // 시전자 스프라이트 기준으로도 은은한 잔광 스윕을 한 겹 더
            try {
                com.shatteredpixel.shatteredpixeldungeon.effects.ManaSwordSweepEffect efx =
                        com.shatteredpixel.shatteredpixeldungeon.effects.ManaSwordSweepEffect.attachTo(hero.sprite.parent);
                efx.sweepBurst(hero.sprite.center().x, hero.sprite.center().y,
                        startAng, endAng, 6f, 14f, 12, +1);
            } catch (Throwable t) {
                // 이펙트가 누락되어 있거나 시그니처가 다를 경우에도 안전하게 무시
            }
        }
// magicalShoot는 Codex의 정확도/대미지 규칙을 그대로 사용해 1타 판정을 수행
        if (!hero.magicalShoot(target, this)) {
            rangedMiss(atCell);
        } else {
            // 기본 rangedHit는 비어 있으므로, 여기서는 맞췄을 때 별도 연출 없이 통과
            rangedHit(target, atCell);
        }
    }

    // 근접 판정이므로 사거리 정확도 보정
    @Override
    protected float adjacentAccFactor(Char owner, Char target) {
        // 근접에서는 명중률을 조금 더 높임
        return 1.25f * super.adjacentAccFactor(owner, target);
    }

    // 대미지 롤은 Codex 기본을 사용하되, 근거리 보너스 약간
    @Override
    public int damageRoll(Char owner) {
        int dmg = super.damageRoll(owner);
        // 짧은 사거리 특성상 소폭의 고정 보정
        return Math.round(dmg * 1.10f);
    }

    @Override
    public boolean isIdentified() {
        // 간단한 전투 코덱스이니 즉시 식별
        return true;
    }

    @Override
    public boolean isUpgradable() { return true; }

    public boolean isEnchantable() { return true; }
}