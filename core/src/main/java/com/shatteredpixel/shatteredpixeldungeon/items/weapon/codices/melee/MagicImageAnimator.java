
package com.shatteredpixel.shatteredpixeldungeon.items.weapon.codices.melee;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Game;
import com.watabou.utils.PointF;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ManaSlashParticle;
import com.watabou.noosa.particles.Emitter;

import java.lang.reflect.Method;

/**
 * MagicImageAnimator
 *
 * - "베기(sweep)" 반원 회전 애니메이션 + "찌르기(stab)" 전진 애니메이션을 지원.
 * - 외부에서는 정적 헬퍼(sweep*, stab*)로 호출합니다.
 */
public class MagicImageAnimator extends Group {

    /* === 기본 파라미터 === */
    public static final float DEFAULT_DURATION = 0.18f;      // 전체 애니메이션 시간(초)
    private static final float BASE_DIAGONAL_DEG = 45f;      // 리소스 기본 기울기 보정(NE)
    private static final boolean FLIP_FACING = false;        // 좌우 반전 필요시 true

    // 이미지 내부 피벗(손잡이 지점)에 대한 기본 비율(0..1)
    private static final float DEFAULT_ORIGIN_FRACTION_X = 0.20f;
    private static final float DEFAULT_ORIGIN_FRACTION_Y = 0.80f;

    // 찌르기(STAB) 기본 보조 파라미터
    private static final float STAB_BACK_RATIO  = 0.28f; // 전진 전, 살짝 뒤로 빠지는 거리 비율
    private static final float STAB_FADE_START  = 0.90f; // 사라지기 시작하는 비율(0..1)

    // --- 모드(애니메이션 종류) ---
    private enum Mode { SWEEP, STAB }

    /* === 참조/상태 === */
    private final CharSprite owner;
    private final Image img;
    private final int magicImageIndex;
    private final Mode mode;

    // 공통 진행/시간
    private float elapsed = 0f;
    private final float duration;

    // 공통: 피벗 위치 보정(캐릭터 중심 대비, 픽셀)
    private final float pivotAdjustX;
    private final float pivotAdjustY;

    // 공통: 바라보는 각도(보정 전)
    private final float faceDeg;

    // --- SWEEP 전용 ---
    private float startDeg;
    private float endDeg;
    private boolean topToBottom;

    // --- STAB 전용 ---
    private float stabMaxDist;      // 최대 전진 거리(픽셀)
    private float stabRetractBias;  // 0.0~1.0: (0.5 = 전진/후퇴 시간 동일)

    // 이펙트(선택적)
    private Emitter manaEmitter;
    private float afterImageCooldown = 0f;
    private float particleCooldown  = 0f;

    /* **********************************************************************
     * 정적 헬퍼: 8방향 베기
     ********************************************************************** */
    public static void sweep(CharSprite owner, int magicImageIndex, int dir8) {
        float dx = -6f * owner.scale.x;
        float dy = -8f * owner.scale.y;
        float faceDeg = dir8ToDeg(dir8);
        sweepAngle(owner, magicImageIndex, faceDeg, DEFAULT_DURATION,
                dx, dy, DEFAULT_ORIGIN_FRACTION_X, DEFAULT_ORIGIN_FRACTION_Y,
                0f, true);
    }

    public static void sweep(CharSprite owner, int magicImageIndex, int dir8,
                             float duration, float pivotAdjustX, float pivotAdjustY,
                             float originFracX, float originFracY) {
        float faceDeg = dir8ToDeg(dir8);
        sweepAngle(owner, magicImageIndex, faceDeg, duration,
                pivotAdjustX, pivotAdjustY, originFracX, originFracY,
                0f, true);
    }

    public static void sweepVector(CharSprite owner, int magicImageIndex, int dx, int dy, boolean snapTo8) {
        float face = (float)Math.toDegrees(Math.atan2(dy, dx));
        if (snapTo8) face = Math.round(face / 45f) * 45f;
        float adjX = -6f * owner.scale.x;
        float adjY = -8f * owner.scale.y;
        sweepAngle(owner, magicImageIndex, face, DEFAULT_DURATION,
                adjX, adjY, DEFAULT_ORIGIN_FRACTION_X, DEFAULT_ORIGIN_FRACTION_Y,
                0f, true);
    }

    public static void sweepAngle(CharSprite owner, int magicImageIndex, float faceDeg,
                                  float duration, float pivotAdjustX, float pivotAdjustY,
                                  float originFracX, float originFracY,
                                  float angleOffsetDeg, boolean topToBottom) {
        MagicImageAnimator fx = new MagicImageAnimator(
                owner, magicImageIndex, Mode.SWEEP, faceDeg, duration,
                pivotAdjustX, pivotAdjustY, originFracX, originFracY,
                angleOffsetDeg, topToBottom,
                /* stab params */ 0f, 0.5f
        );
        owner.parent.add(fx);
        fx.attachEmitterIfPossible();
    }

    /* **********************************************************************
     * 정적 헬퍼: 8방향 찌르기
     ********************************************************************** */
    public static void stab(CharSprite owner, int magicImageIndex, int dir8) {
        float dx = -6f * owner.scale.x;
        float dy = -8f * owner.scale.y;
        float faceDeg = dir8ToDeg(dir8);
        float maxDist = 48f * owner.scale.x;  // 기본 전진 거리
        stabAngle(owner, magicImageIndex, faceDeg, DEFAULT_DURATION,
                dx, dy, DEFAULT_ORIGIN_FRACTION_X, DEFAULT_ORIGIN_FRACTION_Y,
                maxDist, 0.25f);
    }

    public static void stabVector(CharSprite owner, int magicImageIndex, int dx, int dy, boolean snapTo8) {
        float face = (float)Math.toDegrees(Math.atan2(dy, dx));
        if (snapTo8) face = Math.round(face / 45f) * 45f;
        float adjX = -6f * owner.scale.x;
        float adjY = -8f * owner.scale.y;
        float maxDist = 48f * owner.scale.x;
        stabAngle(owner, magicImageIndex, face, DEFAULT_DURATION,
                adjX, adjY, DEFAULT_ORIGIN_FRACTION_X, DEFAULT_ORIGIN_FRACTION_Y,
                maxDist, 0.25f);
    }

    /**
     * @param stabMaxDist      최대 전진 거리(픽셀). 피벗에서 이만큼 전/후진
     * @param retractBias01    0..1. 0.5=전진/후퇴 동일, 작으면 전진이 길고, 크면 후퇴가 김
     */
    public static void stabAngle(CharSprite owner, int magicImageIndex, float faceDeg,
                                 float duration, float pivotAdjustX, float pivotAdjustY,
                                 float originFracX, float originFracY,
                                 float stabMaxDist, float retractBias01) {
        MagicImageAnimator fx = new MagicImageAnimator(
                owner, magicImageIndex, Mode.STAB, faceDeg, duration,
                pivotAdjustX, pivotAdjustY, originFracX, originFracY,
                /* angleOffset/topToBottom for sweep (ignored) */ 0f, true,
                /* stab params */ stabMaxDist, clamp01(retractBias01)
        );
        owner.parent.add(fx);
        fx.attachEmitterIfPossible();
    }

    /* **********************************************************************
     * 내부 생성자
     ********************************************************************** */
    private MagicImageAnimator(CharSprite owner,
                               int magicImageIndex,
                               Mode mode,
                               float faceDeg,
                               float duration,
                               float pivotAdjustX,
                               float pivotAdjustY,
                               float originFracX,
                               float originFracY,
                               float angleOffsetDeg,
                               boolean topToBottom,
                               float stabMaxDist,
                               float stabRetractBias) {
        this.owner = owner;
        this.magicImageIndex = magicImageIndex;
        this.mode = mode;
        this.faceDeg = faceDeg;
        this.duration = Math.max(0.001f, duration);
        this.pivotAdjustX = pivotAdjustX;
        this.pivotAdjustY = pivotAdjustY;
        this.stabMaxDist = Math.max(0f, stabMaxDist);
        this.stabRetractBias = clamp01(stabRetractBias);

        this.img = new ItemSprite(magicImageIndex);
        add(img);

        // 이미지 내부 피벗(origin) 세팅
        float ox = img.width  * clamp01(originFracX);
        float oy = img.height * clamp01(originFracY);
        img.origin.set(ox, oy);

        // 초기 각도/세팅
        float base = (FLIP_FACING ? -faceDeg : faceDeg) - BASE_DIAGONAL_DEG;

        if (mode == Mode.SWEEP) {
            // sweep은 (base + 추가 오프셋) 기준의 반원 회전
            float sweepBase = base + angleOffsetDeg;
            if (topToBottom) {
                this.startDeg = sweepBase - 90f;
                this.endDeg   = sweepBase + 90f;
            } else {
                this.startDeg = sweepBase + 90f;
                this.endDeg   = sweepBase - 90f;
            }
            this.topToBottom = topToBottom;
            img.angle = startDeg;
        } else {
            // stab은 기본적으로 바라보는 각도에 맞춰 고정
            img.angle = base;
        }

        // 최초 위치 동기화
        syncToOwner(0f);
    }

    private void attachEmitterIfPossible() {
        try {
            manaEmitter = new Emitter();
            if (owner != null && owner.parent != null) {
                owner.parent.add(manaEmitter);
            } else {
                add(manaEmitter);
            }
        } catch (Throwable t) {
            // 이펙트가 없는 변형 빌드 대비 (무시)
            manaEmitter = null;
        }
    }

    /* **********************************************************************
     * 프레임 업데이트
     ********************************************************************** */
    @Override
    public void update() {
        super.update();

        if (owner == null || !owner.visible) {
            dispose();
            return;
        }

        float dt = Game.elapsed;
        elapsed += dt;
        float t = elapsed / duration;
        if (t >= 1f) {
            // 마지막 프레임 한 번 동기화 후 제거
            syncToOwner(1f);
            dispose();
            return;
        }

        syncToOwner(t);
        spawnEffects(dt, t);
    }

    /* **********************************************************************
     * 위치/각도 동기화
     ********************************************************************** */
    private void syncToOwner(float t) {
        // 캐릭터 중심 + 보정
        PointF c = owner.center();
        float px = c.x + pivotAdjustX;
        float py = c.y + pivotAdjustY;

        if (mode == Mode.SWEEP) {
            float a = lerp(startDeg, endDeg, t);
            img.angle = a;
            img.x = px - img.origin.x;
            img.y = py - img.origin.y;
        } else {
            // 찌르기: 피벗에서 바라보는 방향으로 전진/후퇴
            // 살짝 뒤로 → 앞으로 '돌진' 형태 (뒤로 약간 물러난 후 급가속 전진)
            float tb = clamp01(stabRetractBias);
            float back = stabMaxDist * STAB_BACK_RATIO; // 뒤로 빠질 거리
            float d;
            if (t <= tb) {
                // 후퇴 구간: 빠르게 뒤로 살짝 물러남
                float tt = t / Math.max(0.0001f, tb);
                d = -easeOutCubic(tt) * back;
            } else {
                // 돌진 구간: 점점 가속하며 전진(-back → +stabMaxDist)
                float tt = (t - tb) / Math.max(0.0001f, (1f - tb));
                d = -back + easeInCubic(tt) * (stabMaxDist + back);
            }

            float rad = (float)Math.toRadians(FLIP_FACING ? -faceDeg : faceDeg);
            float dx = (float)Math.sin(rad) * d;
            float dy = (float)-Math.cos(rad) * d;

            img.x = px - img.origin.x + dx;
            img.y = py - img.origin.y + dy;

            // 각도는 기본 바라보는 방향에 고정
            img.angle = (FLIP_FACING ? -faceDeg : faceDeg) - BASE_DIAGONAL_DEG;

            // 후반부에 자연스럽게 사라지는 느낌
            if (t >= STAB_FADE_START) {
                float ft = (t - STAB_FADE_START) / Math.max(0.0001f, (1f - STAB_FADE_START));
                float fade = 1f - easeInCubic(clamp01(ft));
                try { img.alpha(fade); } catch (Throwable ignore) {}
            }
        }
    }

    /* **********************************************************************
     * 이펙트(선택): 잔상/파티클 간격으로 소량 뿌리기
     * - ManaSlashAfterImage.spawn(...)은 빌드마다 시그니처가 달라질 수 있어
     *   리플렉션으로 최대한 맞춰 호출하고, 실패 시 조용히 무시합니다.
     ********************************************************************** */
    private void spawnEffects(float dt, float t) {
        // 잔상
        afterImageCooldown -= dt;
        if (afterImageCooldown <= 0f) {
            afterImageCooldown = 0.03f;
            trySpawnAfterImage(0.65f);
        }

        // 파티클
        particleCooldown -= dt;
        if (particleCooldown <= 0f) {
            particleCooldown = 0.02f;
            try {
                if (manaEmitter != null) {
                    manaEmitter.pos(img.x, img.y, img.width, img.height);
                    manaEmitter.burst(ManaSlashParticle.FACTORY, 1);
                }
            } catch (Throwable ignore) {}
        }
    }

    /**
     * 빌드별 ManaSlashAfterImage.spawn(...) 오버로드에 최대한 맞춰 호출.
     * 실패하면 아무 것도 하지 않음(컴파일 안전).
     */
    private void trySpawnAfterImage(float alpha) {
        try {
            Class<?> cls = Class.forName("com.shatteredpixel.shatteredpixeldungeon.effects.ManaSlashAfterImage");
            Method[] methods = cls.getMethods();
            Group parentGroup = (owner != null && owner.parent != null) ? owner.parent : this;

            // 후보 1: spawn(Group,int,float,float,float,float,float,float,float,float,float) [총 12 인자]
            for (Method m : methods) {
                if (!"spawn".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length == 12 && Group.class.isAssignableFrom(ps[0]) && ps[1] == int.class) {
                    m.invoke(null,
                            parentGroup, 1,
                            img.x, img.y,
                            img.width, img.height,
                            img.origin.x, img.origin.y,
                            img.angle,
                            1f,         // scale
                            alpha       // alpha
                    );
                    return;
                }
            }
            // 후보 2: spawn(Group,int,float,float,float,float,float,float,float) [총 10 인자]
            for (Method m : methods) {
                if (!"spawn".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length == 10 && Group.class.isAssignableFrom(ps[0]) && ps[1] == int.class) {
                    m.invoke(null,
                            parentGroup, 1,
                            img.x, img.y,
                            img.width, img.height,
                            img.angle,
                            1f,         // scale
                            alpha       // alpha
                    );
                    return;
                }
            }
        } catch (Throwable ignore) {
            // 잔상 효과는 옵션이므로 조용히 무시
        }
    }

    /* **********************************************************************
     * 파괴/정리
     ********************************************************************** */
    private void dispose() {
        if (parent != null) parent.remove(this);
        kill();
        if (manaEmitter != null) {
            try {
                manaEmitter.kill();
                if (manaEmitter.parent != null) manaEmitter.parent.remove(manaEmitter);
            } catch (Throwable ignore) {}
            manaEmitter = null;
        }
    }

    /* **********************************************************************
     * 유틸
     ********************************************************************** */
    private static float dir8ToDeg(int dir8) {
        switch (dir8 & 7) {
            case 0:  return   0f;   // N
            case 1:  return  45f;   // NE
            case 2:  return  90f;   // E
            case 3:  return 135f;   // SE
            case 4:  return 180f;   // S
            case 5:  return-135f;   // SW
            case 6:  return -90f;   // W
            case 7:  return -45f;   // NW
            default: return   0f;
        }
    }

    private static float lerp(float a, float b, float t) {
        if (t <= 0f) return a;
        if (t >= 1f) return b;
        return a + (b - a) * t;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static float easeOutCubic(float x) {
        float f = 1f - x;
        return 1f - f*f*f;
    }

    private static float easeInCubic(float x) {
        return x*x*x;
    }
}
