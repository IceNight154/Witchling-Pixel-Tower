package com.shatteredpixel.shatteredpixeldungeon.items.weapon.codices.ranged;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

/**
 * MagicBullet – 한 턴 3연타(동기 실행)
 *
 * - 같은 대상에게 최대 3번 연속 타격(지연/트위너/busy 없음)
 * - 각 타마다 throwSound + 히트/미스 사운드 재생
 * - 필수 필드 사용: tier, image=ItemSpriteSheet.*, magicImage, baseUses
 * - MagicImageAnimator 미사용
 */
public class MagicBullet extends RangedCodex {

    {
        tier = 1;
        image = ItemSpriteSheet.CODEX_BULLET;
        magicImage = ItemSpriteSheet.MAGIC_BULLET;
        baseUses = 30;
    }

    @Override
    public int min(int lvl) { return (tier + 1) + lvl; }

    @Override
    public int max(int lvl) { return 5 * (tier + 1) + lvl * (tier + 1); }

    public void throwSound() {
        Sample.INSTANCE.play(Assets.Sounds.ATK_GRIMOIRE, 1f, Random.Float(0.87f, 1.15f));
    }

    @Override
    protected void onThrow(final int cell) {
        final Char enemy = Actor.findChar(cell);

        // 유효 대상 없으면 정상 소비만
        if (enemy == null || enemy == curUser) {
            parent = null;
            onUse();
            if (durabilityLeft() > 0) this.collect();
            updateQuickslot();
            return;
        }

        // === 3연타: 즉시(동기)로 3회 처리 ===
        for (int i = 0; i < 3; i++) {
            if (!enemy.isAlive()) break;

            // magicImage 표기를 위해 잠깐 casting on
            boolean prevCasting = this.casting;
            this.casting = true;

            boolean hit = curUser.codexAttack(enemy, this);
            onAttackComplete(enemy, cell, hit);

            // 히트/미스 사운드
            if (hit) {
                Sample.INSTANCE.play(Assets.Sounds.HIT_BULLET, 0.92f, 1.08f);
            } else {
                Sample.INSTANCE.play(Assets.Sounds.MISS,  0.92f, 1.18f);
            }

            this.casting = prevCasting;
        }

        // 한 번만 턴 소모/내구도 감소/퀵슬롯 갱신
        onUse();
        if (durabilityLeft() > 0) this.collect();
        updateQuickslot();
    }
}
