
package com.shatteredpixel.shatteredpixeldungeon.items.weapon.codices.melee;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Game;
import com.watabou.utils.PointF;
import com.shatteredpixel.shatteredpixeldungeon.effects.ManaSlashAfterImage;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ManaSlashParticle;
import com.watabou.noosa.particles.Emitter;

/**
 * MagicImageAnimator (마법 이미지 반원 회전 애니메이션)
 *
 * 목적:
 *  - 무기의 magicImage(아이템 스프라이트 한 프레임)를 캐릭터를 중심축으로 하여
 *    8방향(방위)에 맞춰 "위 → 아래"로 반원(180°) 회전시키는 시각 효과를 보여준다.
 *  - 스윕 궤적을 이동 에펙으로 그리는 것이 아니라, 이미지 자체를 캐릭터 중심에서
 *    회전시키는 방식.
 *
 * 스프라이트 크기 보정(중심축 드리프트 보정):
 *  - 캐릭터 스프라이트가 12×16px → 24×32px로 커지고, 화면 출력은 0.35배로 축소되는 경우
 *    시각적 중심이 우하단으로 치우치는 경향이 있다.
 *  - 기본값으로 대략 (-6, -8) * scale 픽셀만큼 좌상단 쪽으로 피벗을 옮겨 중심을 맞춘다.
 *    필요시 오버로드 메서드로 쉽게 튜닝 가능.
 *
 * 사용 예시(코덱스 하위 무기에서):
 *   // dir8: 0=N,1=NE,2=E,3=SE,4=S,5=SW,6=W,7=NW
 *   MagicImageAnimator.sweep(curUser.sprite, magicImage, dir8);
 */
public class MagicImageAnimator extends Group {

    /* === 상수: 기본 스윕 지속 시간(초) === */
    public static final float DEFAULT_DURATION = 0.18f;

    /* === 상수: magicImage의 기본 기울기(NE, +45°) 보정 값 === */
    private static final float BASE_DIAGONAL_DEG = 45f;

    // 공격 방향이 반대로 보일 때 true로 설정하여 좌우를 뒤집습니다.
    private static final boolean FLIP_FACING = false;

    /* === 상수: 이미지 내부의 '손잡이(피벗)' 위치(비율 0..1) ===
       - 이 값은 이미지의 어느 지점을 회전 중심으로 둘지 정한다.
       - 보통 칼의 손잡이 쪽(하단/내측)에 피벗을 둔다. */
    private static final float DEFAULT_ORIGIN_FRACTION_X = 0.20f; // 가로 기준(왼쪽→오른쪽)
    private static final float DEFAULT_ORIGIN_FRACTION_Y = 0.80f; // 세로 기준(위→아래)

    /* === 참조: 소유자 캐릭터 스프라이트 & 회전시킬 이미지 === */
    private final CharSprite owner;
    private final Image img;
    private final int magicImageIndex;


    /* === 애니메이션 각도/시간 정보 === */
    private final float startDeg;
    private final float endDeg;
    private final float duration;

    /* === 피벗 보정값(픽셀): 캐릭터 중심에서 추가로 이동할 양 === */
    private final float pivotAdjustX;
    private final float pivotAdjustY;

    /* === 경과 시간 누적 === */
    private float elapsed = 0f;

    // 클래스 필드에 추가
    private Emitter manaEmitter;
    private float afterImageCooldown = 0f;   // 잔상 간격
    private float particleCooldown  = 0f;   // 파편 간격


    /* **********************************************************************
     * [정적 헬퍼] 가장 간단한 사용법
     *  - dir8 방향에 맞춰, 기본 지속시간/보정값/피벗 원점을 사용해 반원 회전한다.
     * ********************************************************************** */
    public static void sweep(CharSprite owner, int magicImageIndex, int dir8) {
        // 24×32 스프라이트를 ~0.35배로 그리는 상황에서의 기본 우하단 치우침 보정
        float dx = -6f * owner.scale.x;
        float dy = -8f * owner.scale.y;
        sweep(owner, magicImageIndex, dir8, DEFAULT_DURATION, dx, dy,
                DEFAULT_ORIGIN_FRACTION_X, DEFAULT_ORIGIN_FRACTION_Y);
    }

    /* **********************************************************************
     * [정적 헬퍼] 풀 커스텀 버전
     *
     * @param owner         : 회전 중심이 될 캐릭터 스프라이트
     * @param magicImageIndex: 아이템 스프라이트 시트 인덱스
     * @param dir8          : 8방향(0=N,1=NE,2=E,3=SE,4=S,5=SW,6=W,7=NW)
     * @param duration      : 반원 회전에 걸리는 시간(초)
     * @param pivotAdjustX  : 캐릭터 중심에서 X축 보정(픽셀)
     * @param pivotAdjustY  : 캐릭터 중심에서 Y축 보정(픽셀)
     * @param originFracX   : 이미지 내부 피벗의 X 비율(0..1)
     * @param originFracY   : 이미지 내부 피벗의 Y 비율(0..1)
     * ********************************************************************** */
    public static void sweep(CharSprite owner,
                             int magicImageIndex,
                             int dir8,
                             float duration,
                             float pivotAdjustX,
                             float pivotAdjustY,
                             float originFracX,
                             float originFracY) {
        MagicImageAnimator fx = new MagicImageAnimator(owner, magicImageIndex, dir8, duration,
                pivotAdjustX, pivotAdjustY, originFracX, originFracY);
        // 같은 부모 그룹에 추가하여 소유자 위에 그려지도록 한다.
        owner.parent.add(fx);
    }

    /* **********************************************************************
     * [정적 헬퍼] 벡터(dx,dy)로 방향을 지정하는 버전
     *  - 9×9 등 격자에서 타깃 셀까지의 델타(dx,dy)를 넣으면
     *    그 벡터 각도를 기반으로 반원 회전이 수행된다.
     *  - snapTo8=true면 8방향(45°) 단위로 스냅한다.
     * ********************************************************************** */
    public static void sweepVector(CharSprite owner,
                                   int magicImageIndex,
                                   int dx, int dy,
                                   boolean snapTo8) {
        float faceDeg = (float)Math.toDegrees(Math.atan2(dy, dx)); // 오른쪽=0°, 아래=+90°
        if (snapTo8) {
            faceDeg = Math.round(faceDeg / 45f) * 45f;
        }
        sweepAngle(owner, magicImageIndex, faceDeg, DEFAULT_DURATION,
                -6f * owner.scale.x, -8f * owner.scale.y,
                DEFAULT_ORIGIN_FRACTION_X, DEFAULT_ORIGIN_FRACTION_Y,
                0f, true);
    }

    /* **********************************************************************
     * [정적 헬퍼] 각도(deg)로 직접 지정하는 버전
     *  - angleOffsetDeg로 기저 각도 추가 보정
     *  - topToBottom=true면 "위→아래" 반원, false면 "아래→위" 반원
     * ********************************************************************** */
    public static void sweepAngle(CharSprite owner,
                                  int magicImageIndex,
                                  float faceDeg,
                                  float duration,
                                  float pivotAdjustX,
                                  float pivotAdjustY,
                                  float originFracX,
                                  float originFracY,
                                  float angleOffsetDeg,
                                  boolean topToBottom) {
        // 내부적으로는 dir8이 아니라 faceDeg를 직접 넘기는 전용 생성자를 사용
        MagicImageAnimator fx = new MagicImageAnimator(owner, magicImageIndex, faceDeg, duration,
                pivotAdjustX, pivotAdjustY, originFracX, originFracY, angleOffsetDeg, topToBottom);
        owner.parent.add(fx);

        fx.manaEmitter = new Emitter();
        owner.parent.add(fx.manaEmitter);
    }

    /* **********************************************************************
     * [생성자] 내부 상태 초기화
     *  - 이미지 생성, 피벗(origin) 설정, 시작/종료 각도 계산을 수행한다.
     * ********************************************************************** */

    /* **********************************************************************
     * [생성자] faceDeg(방향 각도) 직접 지정 버전
     * ********************************************************************** */
    private MagicImageAnimator(CharSprite owner,
                               int magicImageIndex,
                               float faceDeg,
                               float duration,
                               float pivotAdjustX,
                               float pivotAdjustY,
                               float originFracX,
                               float originFracY,
                               float angleOffsetDeg,
                               boolean topToBottom) {
        this.owner = owner;
        this.duration = Math.max(0.001f, duration);
        this.pivotAdjustX = pivotAdjustX;
        this.pivotAdjustY = pivotAdjustY;
        this.magicImageIndex = magicImageIndex;

        this.img = new ItemSprite(magicImageIndex);
        add(img);

        float ox = img.width * clamp01(originFracX);
        float oy = img.height * clamp01(originFracY);
        img.origin.set(ox, oy);

        // 반원 시작/끝 각도 계산
        float base = (FLIP_FACING ? -faceDeg : faceDeg) + angleOffsetDeg - BASE_DIAGONAL_DEG;
        if (topToBottom) {
            this.startDeg = base - 90f;
            this.endDeg   = base + 90f;
        } else {
            this.startDeg = base + 90f;
            this.endDeg   = base - 90f;
        }

        img.angle = startDeg;
        syncToOwner();
    }
    private MagicImageAnimator(CharSprite owner,
                               int magicImageIndex,
                               int dir8,
                               float duration,
                               float pivotAdjustX,
                               float pivotAdjustY,
                               float originFracX,
                               float originFracY) {
        this.owner = owner;
        this.duration = Math.max(0.001f, duration); // 0에 근접한 시간 방지
        this.pivotAdjustX = pivotAdjustX;
        this.pivotAdjustY = pivotAdjustY;
        this.magicImageIndex = magicImageIndex;
        // 아이템 스프라이트 인덱스로부터 회전 가능한 이미지 생성
        this.img = new ItemSprite(magicImageIndex);
        add(img);

        // 이미지 내부 피벗(원점) 설정: 손잡이(내측)에 피벗을 둬 휘두르는 느낌을 강화
        float ox = img.width * clamp01(originFracX);
        float oy = img.height * clamp01(originFracY);
        img.origin.set(ox, oy);

        // 8방향을 각도(우측=0°, 아래=+90°)로 변환. 화면 좌표계(y+가 아래) 기준 시계방향 양수.
        float faceDeg = dir8ToDeg(dir8);
        if (FLIP_FACING) faceDeg = -faceDeg;

        // "위 → 아래" 반원을 만들기 위해 facing-90°에서 시작해 facing+90°로 끝낸다.
        // magicImage가 NE(+45°)를 기본으로 그려졌으므로 마지막에 45°를 빼서 보정한다.
        this.startDeg = faceDeg - 90f - BASE_DIAGONAL_DEG;
        this.endDeg   = faceDeg + 90f - BASE_DIAGONAL_DEG;

        // 초기 각도와 위치 동기화
        img.angle = startDeg;
        syncToOwner();
    }

    /* **********************************************************************
     * [프레임 업데이트] 각도 보간 + 위치 동기화
     *  - Game.elapsed(프레임 경과 시간)를 누적해 0..1 진행률을 계산한다.
     *  - 진행 완료 시 이미지 정리(dispose)로 자신을 제거한다.
     * ********************************************************************** */
    @Override
    public void update() {
        super.update();

        // 소유자가 사라지거나 보이지 않으면 즉시 정리
        if (owner == null || !owner.visible) {
            dispose();
            return;
        }

        // 진행률 계산(0..1)
        elapsed += Game.elapsed;
        float t = elapsed / duration;
        if (t >= 1f) {
            // 마지막 프레임: 최종 각도 적용 후 정리
            img.angle = endDeg;
            syncToOwner();
            dispose();
            return;
        }

        // 선형 보간으로 각도 갱신
        img.angle = startDeg + (endDeg - startDeg) * t;

        // 스윙 중에도 캐릭터가 움직일 수 있으므로 매 프레임 위치 재동기화
        syncToOwner();

        // --- ManaSlash 잔상/파편 가벼운 연동 시작 ---

// 시간 경과
        afterImageCooldown -= Game.elapsed;
        particleCooldown   -= Game.elapsed;

// 현재 회전 이미지의 화면 좌표/원점/스케일/각도
        final float ix = img.x;
        final float iy = img.y;
        final float angDeg = img.angle;     // 도 단위
        final float ox = img.origin.x;
        final float oy = img.origin.y;
        final float sx = img.scale.x;
        final float sy = img.scale.y;

// 1) 잔상: 너무 과하지 않게 0.05~0.07s 간격으로 한 장씩
        if (afterImageCooldown <= 0f) {
            ManaSlashAfterImage.spawn(
                    owner.parent,          // Group parent
                    magicImageIndex,       // 아이템 스프라이트 프레임 index
                    ix, iy,                // 이미지 좌표
                    angDeg,                // 이미지 각도(도)
                    ox, oy,                // origin
                    sx, sy                 // scale
            );
            afterImageCooldown = 0.06f;
        }

// 2) 파편: 각도 정렬된 짧은 파편 2~3개씩
        if (manaEmitter != null && particleCooldown <= 0f) {
            manaEmitter.pos(ix + ox, iy + oy); // 대략 이미지 기준점 근처
            manaEmitter.burst(ManaSlashParticle.oriented(angDeg), 3);
            particleCooldown = 0.016f; // 약 60fps라면 매 프레임
        }

// --- ManaSlash 잔상/파편 가벼운 연동 끝 ---

    }

    /* **********************************************************************
     * [정리] 그룹에서 자신을 제거하고 kill() 호출
     *  - 엔진 버전에 따라 제거 메서드가 다를 수 있어, 안전하게 parent.remove() 사용
     * ********************************************************************** */
    private void dispose() {
        if (parent != null) {
            parent.remove(this);
        }
        kill();

        if (manaEmitter != null) {
            manaEmitter.kill();   // 또는 parent.remove(manaEmitter);
            manaEmitter = null;
        }
    }

    /* **********************************************************************
     * [위치 동기화] 캐릭터 중심 + 피벗 보정 → 이미지 좌표로 변환
     *  - img.origin을 기준으로 실제 그려질 x,y를 계산한다.
     * ********************************************************************** */
    private void syncToOwner() {
        PointF c = owner.center();

        // 스프라이트 크기/스케일에 따른 중심 드리프트를 보정
        float px = c.x + pivotAdjustX;
        float py = c.y + pivotAdjustY;

        // 이미지의 원점(origin)이 피벗(px,py)에 오도록 위치 설정
        img.x = px - img.origin.x;
        img.y = py - img.origin.y;
    }

    /* **********************************************************************
     * [유틸] 8방향 → 각도 변환
     *  - 매핑: 0=N,1=NE,2=E,3=SE,4=S,5=SW,6=W,7=NW
     *  - 기준: 오른쪽=0°, 아래=+90°, 시계방향(+) / 반시계(-)
     * ********************************************************************** */
    private static float dir8ToDeg(int dir8) {
        switch (dir8 & 7) {

            case 0:  return    0f;   // N
            case 1:  return   45f;   // NE
            case 2:  return   90f;   // E
            case 3:  return  135f;   // SE
            case 4:  return  180f;   // S
            case 5:  return -135f;   // SW
            case 6:  return  -90f;   // W
            case 7:  return  -45f;   // NW
            default: return   0f;
        }
    }

    /* **********************************************************************
     * [유틸] 0..1 클램프
     * ********************************************************************** */
    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
