package com.shatteredpixel.shatteredpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

/**
 * ManaSlashParticle
 * - 마나 검이 지나간 자리에서 아주 짧게 남는 파편 느낌.
 * - 색상: #38ABAB
 */
public class ManaSlashParticle extends PixelParticle {

    // 0xRRGGBB (SPD/Noosa는 보통 알파는 am으로 다룸)
    private static final int TEAL = 0x38ABAB;

    // 간단히 쓰는 기본 팩토리(방향성 없이 점파티클만 남김)
    public static final Emitter.Factory TRAIL = new Emitter.Factory() {
        @Override
        public void emit(Emitter emitter, int index, float x, float y) {
            ((ManaSlashParticle) emitter.recycle(ManaSlashParticle.class)).reset(x, y);
        }
        @Override
        public boolean lightMode() {
            return true; // 추가광원 합성
        }
    };

    public ManaSlashParticle() {
        super();
        color(TEAL);
        size(2);
    }

    public void reset(float x, float y) {
        revive();
        this.x = x;
        this.y = y;

        // 0.18~0.30초 남았다 사라짐
        left = lifespan = Random.Float(0.18f, 0.30f);

        // 방향성 없는 아주 약한 번짐. (원하면 speed.polar로 각도 부여 가능)
        speed.set(Random.Float(-10f, 10f), Random.Float(-10f, 10f));
        // 빠르게 멈추도록 가속 반대로
        acc.set(-speed.x * 4f, -speed.y * 4f);

        // 시작은 약간 작은 점
        size = Random.Float(1.2f, 2.0f);
        am = 0.85f; // 시작 알파
    }

    @Override
    public void update() {
        super.update();
        float p = left / lifespan; // 1 -> 0

        // 서서히 사라지며 살짝 퍼지는 느낌
        am = p;
        size(1.0f + (1.0f - p) * 1.8f);
    }

    /* 방향성 있는 변형이 필요하면 이 팩토리를 사용.
       현재 img.angle(도 단위)을 넘겨서 칼 진행 방향으로 가늘게 흩어지게 함. */
    public static Emitter.Factory oriented(final float angleDeg){
        return new Emitter.Factory() {
            @Override
            public void emit(Emitter emitter, int index, float x, float y) {
                ManaSlashParticle p = (ManaSlashParticle) emitter.recycle(ManaSlashParticle.class);
                p.resetOriented(x, y, angleDeg);
            }
            @Override
            public boolean lightMode() { return true; }
        };
    }

    public void resetOriented(float x, float y, float angleDeg) {
        revive();
        this.x = x;
        this.y = y;

        left = lifespan = Random.Float(0.16f, 0.26f);
        color(TEAL);
        am = 0.9f;

        // 칼 진행방향을 기준으로 좁은 각도 범위로 튀게
        float spread = 10f; // 도
        float a = (float) Math.toRadians(angleDeg + Random.Float(-spread, spread));
        float v = Random.Float(22f, 38f);
        speed.set((float)Math.cos(a) * v, (float)Math.sin(a) * v);
        acc.set(-speed.x * 5f, -speed.y * 5f);

        size = Random.Float(1.0f, 1.8f);
    }
}
