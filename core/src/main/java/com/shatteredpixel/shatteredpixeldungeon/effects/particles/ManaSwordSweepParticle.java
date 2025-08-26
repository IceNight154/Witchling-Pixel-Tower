package com.shatteredpixel.shatteredpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

public class ManaSwordSweepParticle extends PixelParticle {

    // 푸른 마나 색
    private static final int COLOR = 0x38ABAB;

    // 가산합성으로 번쩍이게
    public static final Emitter.Factory FACTORY = new Factory();

    private float baseSize;
    private float phase, freq;

    public ManaSwordSweepParticle() {
        super();
        color(COLOR);
        am = 0f; // update에서 제어
    }

    /** dirAngle(라디안) 방향으로 강하게 길게 뿌려지는 스파크 */
    public void resetAt(float x, float y, float dirAngle) {
        revive();

        // 스폰 위치(조금 두께감 주기 위해 노멀 오프셋)
        float nx = (float)Math.cos(dirAngle + (float)Math.PI * 0.5f);
        float ny = (float)Math.sin(dirAngle + (float)Math.PI * 0.5f);
        float thickness = Random.Float(-2.0f, 2.0f);
        this.x = x + nx * thickness;
        this.y = y + ny * thickness;

        // 짧고 강한 슬래시 수명
        left = lifespan = Random.Float(0.12f, 0.22f);

        // 방향으로 강하게 가속(검 자국처럼 길게)
        float v = Random.Float(140f, 220f);
        speed.polar(dirAngle, v);

        // 빠르게 감속 → 길게 늘어지는 잔상 느낌
        acc.set(-speed.x * 7f, -speed.y * 7f);

        // 작은 입자(검광 느낌)
        baseSize = Random.Float(1.2f, 2.0f);
        size(baseSize);

        // 미세한 깜빡임
        phase = Random.Float(0f, (float)(Math.PI * 2.0));
        freq  = Random.Float(10f, 16f);
    }

    @Override
    public void update() {
        super.update();

        float p   = left / lifespan;        // 1 -> 0
        float age = lifespan - left;

        // 초반 급히 밝아지고 급히 사그라지는 슬래시 감
        float envelope;
        if      (p < 0.12f) envelope = p / 0.12f;          // 페이드-인
        else if (p > 0.70f) envelope = (1.0f - p) / 0.30f; // 꼬리 감쇠
        else                envelope = 1f;

        float flicker = 0.8f + 0.2f * (float)Math.sin(phase + age * freq);
        am = Math.max(0f, Math.min(1f, flicker * envelope));

        // 진행 방향으로 길게 보이는 느낌(시간에 따라 약간 커졌다가 줄어듦)
        float stretch = 1.0f + (1.0f - p) * 0.4f;
        size(baseSize * stretch);
    }

    /** 기본 팩토리(단일 점에서 임의 방향으로 튀는 슬래시 스파크) */
    public static class Factory extends Emitter.Factory {
        @Override
        public void emit(Emitter emitter, int index, float x, float y) {
            float ang = Random.Float(0f, (float)(Math.PI * 2.0));
            ((ManaSwordSweepParticle) emitter.recycle(ManaSwordSweepParticle.class)).resetAt(x, y, ang);
        }
        @Override public boolean lightMode() { return true; }
    }

    /** 부채꼴 구간 [startAng, endAng]을 따라 '순차적으로' 휘두르며 그려주는 스윕 팩토리 */
    public static class SweepFactory extends Emitter.Factory {
        private final float startAng, endAng, radius;
        private final int dirSign;   // +1 CCW, -1 CW (탄젠트 방향)
        private final int samples;   // 스윕 동안 몇 번 찍을지
        private int i = 0;

        /**
         * @param startAng 시작각(rad), @param endAng 끝각(rad), @param radius 아크 반경(px)
         * @param samples 스윕 샘플 수(방출 횟수), @param dirSign +1 반시계 / -1 시계
         */
        public SweepFactory(float startAng, float endAng, float radius, int samples, int dirSign) {
            this.startAng = startAng;
            this.endAng   = endAng;
            this.radius   = Math.max(4f, radius);
            this.samples  = Math.max(4,  samples);
            this.dirSign  = dirSign >= 0 ? +1 : -1;
        }

        @Override
        public void emit(Emitter emitter, int index, float cx, float cy) {
            float t = Math.min(1f, i / (float)(samples - 1));
            float ang = startAng + (endAng - startAng) * t;

            // 아크 위 포인트에서 탄젠트 방향으로 발사
            float px = cx + (float)Math.cos(ang) * radius;
            float py = cy + (float)Math.sin(ang) * radius;

            float tangent = ang + (float)Math.PI * 0.5f * dirSign;

            ((ManaSwordSweepParticle) emitter.recycle(ManaSwordSweepParticle.class))
                    .resetAt(px, py, tangent);

            i++;
        }
        @Override public boolean lightMode() { return true; }
    }

    /** 부채꼴 전체를 '한 번에' 번쩍 채우는 버스트 아크 팩토리 */
    public static class BurstArcFactory extends Emitter.Factory {
        private final float startAng, endAng, rMin, rMax;
        private final int dirSign;

        /**
         * @param startAng 시작각(rad), @param endAng 끝각(rad)
         * @param rMin~rMax 아크 두께(px), @param dirSign +1 CCW / -1 CW
         */
        public BurstArcFactory(float startAng, float endAng, float rMin, float rMax, int dirSign) {
            this.startAng = startAng;
            this.endAng   = endAng;
            this.rMin     = Math.max(2f, Math.min(rMin, rMax));
            this.rMax     = Math.max(this.rMin, rMax);
            this.dirSign  = dirSign >= 0 ? +1 : -1;
        }

        @Override
        public void emit(Emitter emitter, int index, float cx, float cy) {
            float ang = Random.Float(startAng, endAng);
            float r   = Random.Float(rMin, rMax);

            float px = cx + (float)Math.cos(ang) * r;
            float py = cy + (float)Math.sin(ang) * r;
            float tangent = ang + (float)Math.PI * 0.5f * dirSign;

            ((ManaSwordSweepParticle) emitter.recycle(ManaSwordSweepParticle.class))
                    .resetAt(px, py, tangent);
        }
        @Override public boolean lightMode() { return true; }
    }
}
