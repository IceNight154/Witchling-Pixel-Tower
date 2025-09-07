package com.shatteredpixel.shatteredpixeldungeon.effects.particles;

import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

/** 원소 변경 연출 전용 파티클 4종 (Fire/Water/Earth/Wind)
 *  공통 재생 시간/범위 유지: ECfg에서 한 번에 조절 */
public final class ElementChangeParticles {

    /**
     * 🔧 공통 설정: 여기 숫자만 고치면 4원소가 모두 같은 톤으로 맞춰짐
     */
    public static final class ECfg {
        public static final float LIFE_MIN = 0.36f;   // 개별 파티클 수명(최소)
        public static final float LIFE_MAX = 0.44f;   // 개별 파티클 수명(최대)
        public static final float TARGET_RADIUS = 22f; // 중심→바깥 도달 목표거리(px)
        public static final float SPAWN_JITTER = 2f;   // 시작 위치 난수(px)
        // 감쇠 보정: v≈R/L로 잡되 마찰 고려 보정치
        public static final float SPEED_BOOST = 1.10f; // 1.0~1.2 권장
    }

    /* ===================== FIRE (방사 스파크) ===================== */

    public static class ElementFireBurstParticle extends PixelParticle {
        private static final int COLOR = 0xE24F2E;
        public static final Emitter.Factory FACTORY = new Factory();

        private float baseSize, phase, freq;

        public ElementFireBurstParticle() {
            super();
            color(COLOR);
            am = 0f;
        }

        public void reset(float cx, float cy) {
            revive();
            // 통일: 시작 난수
            this.x = cx + Random.Float(-ECfg.SPAWN_JITTER, ECfg.SPAWN_JITTER);
            this.y = cy + Random.Float(-ECfg.SPAWN_JITTER, ECfg.SPAWN_JITTER);

            left = lifespan = Random.Float(ECfg.LIFE_MIN, ECfg.LIFE_MAX);

            float ang = Random.Float(0f, (float) (Math.PI * 2.0));
            float v = (ECfg.TARGET_RADIUS / left) * ECfg.SPEED_BOOST;
            speed.polar(ang, v);

            // 불: 꼬리 짧게 (감쇠 강)
            acc.set(-speed.x * 2.4f, -speed.y * 2.4f);

            baseSize = Random.Float(1.2f, 2.0f);
            size(baseSize);
            phase = Random.Float(0f, (float) (Math.PI * 2.0));
            freq = Random.Float(10f, 16f);
        }

        @Override
        public void update() {
            super.update();
            float p = left / lifespan, age = lifespan - left;
            float env = (p < 0.12f) ? (p / 0.12f) : (p > 0.82f ? (1f - p) / 0.18f : 1f);
            float flicker = 0.80f + 0.20f * (float) Math.sin(phase + age * freq);
            am = Math.min(1f, flicker * env);
            size(baseSize * (0.95f + 0.30f * (1f - p)));
        }

        public static class Factory extends Emitter.Factory {
            @Override
            public void emit(Emitter emitter, int index, float x, float y) {
                ((ElementFireBurstParticle) emitter.recycle(ElementFireBurstParticle.class)).reset(x, y);
            }

            @Override
            public boolean lightMode() {
                return true;
            }
        }
    }

    /* ===================== WATER (물결 스파크) ===================== */

    public static class ElementWaterRippleParticle extends PixelParticle {
        private static final int COLOR = 0x05AAD3;
        public static final Emitter.Factory FACTORY = new Factory();

        private float baseSize, phase, freq;

        public ElementWaterRippleParticle() {
            super();
            color(COLOR);
            am = 0f;
        }

        public void reset(float cx, float cy) {
            revive();

            // 링 느낌을 살짝 주되, 통일 범위로 맞춤
            float ang = Random.Float(0f, (float) (Math.PI * 2.0));
            float r0 = Random.Float(1f, 3f);
            this.x = cx + (float) Math.cos(ang) * r0;
            this.y = cy + (float) Math.sin(ang) * r0;

            left = lifespan = Random.Float(ECfg.LIFE_MIN, ECfg.LIFE_MAX);

            float vOut = (ECfg.TARGET_RADIUS / left) * (ECfg.SPEED_BOOST * 0.9f);
            speed.polar(ang, vOut);
            // 물: 살짝 위로 떠오름
            speed.y -= vOut * 0.20f;

            // 물: 유지감(감쇠 약)
            acc.set(-speed.x * 1.4f, -speed.y * 1.4f);

            baseSize = Random.Float(1.0f, 1.7f);
            size(baseSize);
            phase = Random.Float(0f, (float) (Math.PI * 2.0));
            freq = Random.Float(6f, 9f);
        }

        @Override
        public void update() {
            super.update();
            float p = left / lifespan, age = lifespan - left;
            float env = (p < 0.15f) ? (p / 0.15f) : (p > 0.85f ? (1f - p) / 0.15f : 1f);
            float flicker = 0.45f + 0.25f * (float) Math.sin(phase + age * freq);
            am = Math.min(1f, flicker * env);
            float pulse = 0.92f + 0.22f * (float) Math.sin(phase + age * (freq * 0.8f));
            size(baseSize * pulse);
        }

        public static class Factory extends Emitter.Factory {
            @Override
            public void emit(Emitter emitter, int index, float x, float y) {
                ((ElementWaterRippleParticle) emitter.recycle(ElementWaterRippleParticle.class)).reset(x, y);
            }

            @Override
            public boolean lightMode() {
                return true;
            }
        }
    }

    /* ===================== EARTH (파편/먼지) ===================== */

    public static class ElementEarthShardParticle extends PixelParticle {
        private static final int COLOR = 0xF5CB3F;
        public static final Emitter.Factory FACTORY = new Factory();

        private float baseSize, phase, freq;

        public ElementEarthShardParticle() {
            super();
            color(COLOR);
            am = 0f;
        }

        public void reset(float cx, float cy) {
            revive();
            this.x = cx + Random.Float(-ECfg.SPAWN_JITTER, ECfg.SPAWN_JITTER);
            this.y = cy + Random.Float(-ECfg.SPAWN_JITTER, ECfg.SPAWN_JITTER);

            left = lifespan = Random.Float(ECfg.LIFE_MIN, ECfg.LIFE_MAX);

            float ang = Random.Float(0f, (float) (Math.PI * 2.0));
            float v = (ECfg.TARGET_RADIUS / left) * (ECfg.SPEED_BOOST * 0.95f);
            speed.polar(ang, v);
            // 땅: 낮게 깔리도록 약간 아래 컴포넌트
            speed.y += v * 0.30f;

            // 땅: 비가산 + 감쇠 강 (무게감)
            acc.set(-speed.x * 2.0f, -speed.y * 2.0f);

            baseSize = Random.Float(1.2f, 2.0f);
            size(baseSize);
            phase = Random.Float(0f, (float) (Math.PI * 2.0));
            freq = Random.Float(7f, 10f);
        }

        @Override
        public void update() {
            super.update();
            float p = left / lifespan, age = lifespan - left;
            float t = 1f - p;
            float env = Math.max(0f, 1f - t * 1.1f);
            float flicker = 0.65f + 0.15f * (float) Math.sin(phase + age * freq);
            am = Math.min(1f, flicker * env);
            size(baseSize * (0.95f + 0.22f * (1f - p)));
        }

        public static class Factory extends Emitter.Factory {
            @Override
            public void emit(Emitter emitter, int index, float x, float y) {
                ((ElementEarthShardParticle) emitter.recycle(ElementEarthShardParticle.class)).reset(x, y);
            }

            @Override
            public boolean lightMode() {
                return false;
            } // 땅은 비가산
        }
    }

    /* ===================== WIND (소용돌이) ===================== */

    public static class ElementWindSwirlParticle extends PixelParticle {
        private static final int COLOR = 0x14EA83;
        public static final Emitter.Factory FACTORY_CCW = new ParamFactory(+1);
        public static final Emitter.Factory FACTORY_CW = new ParamFactory(-1);

        // 🔽 범위 축소 핵심 파라미터
        // 기존: TARGET_RADIUS * 0.65~0.90  →  축소: 0.45~0.65
        private static final float R_MIN_FRAC = 0.15f;
        private static final float R_MAX_FRAC = 0.15f;

        // 반경 유지(스프링) 세기: 값↑ → 원형 궤도에 더 바짝 붙음
        private static final float R_HOLD_VEL = 6.0f;   // 반경 오차에 비례한 ‘목표 반径속도’
        private static final float STEER_K = 7.5f;  // 목표 속도에 붙는 조향 가속 계수

        private float cx, cy, rTarget;
        private int dir; // +1: CCW, -1: CW
        private float swirlSpd;
        private float baseSize, phase, freq;

        public ElementWindSwirlParticle() {
            super();
            color(COLOR);
            am = 0f;
        }

        public void resetAtCenter(float cx, float cy, int dir) {
            revive();
            this.cx = cx;
            this.cy = cy;
            this.dir = (dir >= 0 ? +1 : -1);

            // 🔽 작은 고정 반경에서 시작
            float r0 = Random.Float(ECfg.TARGET_RADIUS * R_MIN_FRAC, ECfg.TARGET_RADIUS * R_MAX_FRAC);
            this.rTarget = r0;

            float ang = Random.Float(0f, (float) (Math.PI * 2.0));
            this.x = cx + (float) Math.cos(ang) * r0;
            this.y = cy + (float) Math.sin(ang) * r0;

            // 공통 수명 유지
            left = lifespan = Random.Float(ECfg.LIFE_MIN, ECfg.LIFE_MAX);

            // 한 바퀴 근사(작은 반경용) — 반경이 작아졌으니 자연스레 속도도 약간 낮춤
            this.swirlSpd = (float) ((2 * Math.PI * r0) / left) * 0.90f;

            // 접선 초기 속도
            float nx = (cx - x), ny = (cy - y);
            float inv = 1f / (float) Math.max(1e-4, Math.sqrt(nx * nx + ny * ny));
            nx *= inv;
            ny *= inv;
            float tx = -ny * this.dir, ty = nx * this.dir;
            speed.set(tx * swirlSpd, ty * swirlSpd);

            baseSize = Random.Float(1.1f, 1.8f);
            size(baseSize);
            phase = Random.Float(0f, (float) (Math.PI * 2.0));
            freq = Random.Float(8f, 12f);
        }

        @Override
        public void update() {
            // 현재 반경/방향
            float dx = cx - x, dy = cy - y;
            float r = (float) Math.sqrt(dx * dx + dy * dy);
            if (r < 1e-4f) r = 1e-4f;
            float nx = dx / r, ny = dy / r;
            float tx = -ny * dir, ty = nx * dir;

            // 🔽 반경 유지: 반경 오차에 비례한 ‘목표 반径속도’(안쪽/바깥쪽 보정)
            float rErr = r - rTarget;              // +면 과대, -면 과소
            float vRad = -rErr * R_HOLD_VEL;       // rErr>0 → 안쪽으로, rErr<0 → 바깥으로

            // 목표 속도 = 접선 회전 + 반径 보정
            float vxTarget = tx * swirlSpd + nx * vRad;
            float vyTarget = ty * swirlSpd + ny * vRad;

            // 목표 속도에 붙도록 조향
            acc.set((vxTarget - speed.x) * STEER_K, (vyTarget - speed.y) * STEER_K);

            super.update();

            float p = left / lifespan, age = lifespan - left;
            float env = (p < 0.12f) ? (p / 0.12f) : (p > 0.85f ? (1f - p) / 0.15f : 1f);
            float flicker = 0.55f + 0.35f * (float) Math.sin(phase + age * freq);
            am = Math.min(1f, flicker * env);
            size(baseSize * (0.95f + 0.22f * (1f - p)));
        }

        private static class ParamFactory extends Emitter.Factory {
            private final int dir;

            private ParamFactory(int dir) {
                this.dir = dir >= 0 ? +1 : -1;
            }

            @Override
            public void emit(Emitter emitter, int index, float x, float y) {
                ((ElementWindSwirlParticle) emitter.recycle(ElementWindSwirlParticle.class))
                        .resetAtCenter(x, y, dir);
            }

            @Override
            public boolean lightMode() {
                return true;
            }
        }
    }
}
