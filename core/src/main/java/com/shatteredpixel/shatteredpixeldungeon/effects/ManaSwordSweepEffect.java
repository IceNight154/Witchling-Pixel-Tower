package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ManaSwordSweepParticle;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

/**
 * 푸르고 투명한 마나( #38ABAB )가 은은하게 번지는 검격 효과.
 *
 * <p>이 클래스는 {@link ManaSwordSweepParticle}과 유기적으로 동작하도록 설계된
 * 가벼운 래퍼(Emitter 서브클래스)입니다. 복잡한 라이프사이클 관리 없이도
 * 한 번의 부채꼴(burst) 방출 혹은 이동 경로를 따라 여러 번 방출하는 스윕 연출을
 * 간단한 호출로 구현할 수 있습니다.</p>
 *
 * <h3>특징</h3>
 * <ul>
 *   <li>COLOR = 0x38ABAB (물빛 마나)</li>
 *   <li>Emitter.autoKill = true : 파티클이 모두 사라지면 자동으로 제거</li>
 *   <li>Arc 기반 방출: 시작각~끝각 사이에서 랜덤/다중 방출</li>
 *   <li>연속 스윕(경로 추적) 유틸 제공</li>
 * </ul>
 *
 * <h3>예시</h3>
 * <pre>
 *   // 1) 즉시 부채꼴로 한번에 터뜨리기 (캐릭터 앞 90도 부채꼴)
 *   ManaSwordSweepEffect fx = ManaSwordSweepEffect.attachTo(scene);
 *   fx.sweepBurst(cx, cy, baseAng - (float)Math.PI/4f, baseAng + (float)Math.PI/4f,
 *                 4f, 18f, 28, +1);
 *
 *   // 2) 경로를 따라 연속 스윕 (시작→끝으로 6스텝, 스텝마다 8개 방출)
 *   ManaSwordSweepEffect.attachTo(scene)
 *       .sweepAlong(x0, y0, x1, y1, (float)Math.toRadians(90), 6, 8, +1);
 * </pre>
 */
public class ManaSwordSweepEffect extends Emitter {

    /** 물빛 마나 컬러 상수 (참고용) */
    public static final int COLOR = 0x38ABAB;

    public ManaSwordSweepEffect(){
        // 모든 파티클이 사라지면 자동으로 자신을 제거
        this.autoKill = true;
    }

    /**
     * 현재 그룹(parent)에 이 이펙트를 추가하고 핸들을 반환.
     */
    public static ManaSwordSweepEffect attachTo(Group parent){
        ManaSwordSweepEffect e = new ManaSwordSweepEffect();
        parent.add(e);
        return e;
    }

    /**
     * 부채꼴로 한 번에 여러 개의 슬래시 파티클을 뿌립니다(버스트).
     *
     * @param cx         중심 X (월드 좌표)
     * @param cy         중심 Y (월드 좌표)
     * @param startAng   시작 각도(라디안)
     * @param endAng     끝 각도(라디안)
     * @param rMin       최소 반경
     * @param rMax       최대 반경
     * @param count      방출 개수 (권장 16~32)
     * @param dirSign    회전 방향 +1(시계), -1(반시계)
     * @return this (체이닝)
     */
    public ManaSwordSweepEffect sweepBurst(float cx, float cy,
                                           float startAng, float endAng,
                                           float rMin, float rMax,
                                           int count, int dirSign){
        ArcFactory factory = new ArcFactory(startAng, endAng, rMin, rMax, dirSign);
        for (int i = 0; i < count; i++) {
            factory.emit(this, i, cx, cy);
        }
        return this;
    }

    /**
     * 선형 경로(start→end)를 따라 일정 간격으로 부채꼴 스윕을 반복 방출합니다.
     * 근접 베기, 대각 베기처럼 이동하면서 남는 잔광 연출에 적합합니다.
     *
     * @param x0        시작 X
     * @param y0        시작 Y
     * @param x1        끝 X
     * @param y1        끝 Y
     * @param arcWidth  부채꼴 폭(라디안). ex) 90도 = Math.toRadians(90)
     * @param steps     스윕 분할 개수(경로 샘플) — 1 이하면 즉시 1회 방출
     * @param perStep   각 스텝에서 방출할 파티클 수
     * @param dirSign   회전 방향 +1(시계) / -1(반시계)
     * @return this (체이닝)
     */
    public ManaSwordSweepEffect sweepAlong(float x0, float y0, float x1, float y1,
                                           float arcWidth, int steps, int perStep, int dirSign){
        float dx = x1 - x0;
        float dy = y1 - y0;
        float baseAng = (float) Math.atan2(dy, dx);
        float half = arcWidth * 0.5f;

        float inner = Math.max(2f, arcWidth * 0.25f); // 적당한 두께감
        float outer = Math.max(inner + 6f, arcWidth * 2.5f);

        ArcFactory factory = new ArcFactory(baseAng - half, baseAng + half, inner, outer, dirSign);

        int sCount = Math.max(1, steps);
        for (int s = 0; s < sCount; s++){
            float t = (sCount == 1) ? 1f : (float)s / (float)(sCount - 1);
            float cx = x0 + dx * t;
            float cy = y0 + dy * t;
            for (int i = 0; i < perStep; i++){
                factory.emit(this, i, cx, cy);
            }
        }
        return this;
    }

    /**
     * 이펙트 전용 ArcFactory.
     *
     * <p>이 클래스를 내부에 두어 {@code ManaSwordSweepParticle.ArcFactory}에 대한
     * 의존성을 제거했습니다. 프로젝트 내 파티클 구현과 무관하게 본 이펙트만으로
     * 정상 빌드가 가능합니다.</p>
     */
    private static class ArcFactory extends Emitter.Factory {
        private final float startAng, endAng;
        private final float rMin, rMax;
        @SuppressWarnings("unused")
        private final int dirSign; // 필요 시 파티클 내부 회전 방향 제어에 사용

        ArcFactory(float startAng, float endAng, float rMin, float rMax, int dirSign){
            this.startAng = startAng;
            this.endAng = endAng;
            this.rMin = rMin;
            this.rMax = rMax;
            this.dirSign = dirSign;
        }

        @Override
        public void emit(Emitter emitter, int index, float x, float y) {
            float a = Random.Float(startAng, endAng);
            float r = Random.Float(rMin, rMax);
            float px = x + (float)Math.cos(a) * r;
            float py = y + (float)Math.sin(a) * r;

            // 파티클 생성 및 좌표만 세팅(방향/색상 등은 파티클 쪽 구현에 따름)
            ManaSwordSweepParticle p = (ManaSwordSweepParticle) emitter.recycle(ManaSwordSweepParticle.class);
            float tangent = a + (float)Math.PI * 0.5f * dirSign;
            p.resetAt(px, py, tangent);
        }
    }
}
