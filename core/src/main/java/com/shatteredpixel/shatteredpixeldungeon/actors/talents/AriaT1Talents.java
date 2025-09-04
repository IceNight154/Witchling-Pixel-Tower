package com.shatteredpixel.shatteredpixeldungeon.actors.talents;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.aria.Overheat;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * Aria - Tier 1 Talents (T1 5종)
 * Spec source: GitHub issue "아리아 #1" (2025-09-04).
 *
 * 본 파일은 각 T1 특성을 Buff로 보관하면서, 외부 시스템에서 쉽게 연동하도록
 * 정적 Hooks를 제공합니다. 게임 쪽에서 다음 훅을 호출해 주세요.
 *
 * 1) 원소 전환 시:
 *    int extra = AriaT1Talents.Hooks.extraReductionOnSwitch(hero);
 *    // 전환 로직에서 과부하 감쇠치에 extra를 더하세요(음수).
 *    int haste = AriaT1Talents.Hooks.onElementSwitch(hero);
 *    // haste > 0이면 외부에서 Haste를 적용하세요.
 *
 * 2) 영웅 턴 처리 1회:
 *    AriaT1Talents.Hooks.onHeroTurn(hero);
 *
 * 3) 스킬/코덱스 사용 직후:
 *    int delta = AriaT1Talents.Hooks.onSkillOrCodexUsed(hero);
 *    // 과부하 게이지에 delta(음수일 수 있음)를 더하세요.
 *
 * 4) 코덱스 수치 보정:
 *    float dmgMul = AriaT1Talents.Hooks.codexDamageMultiplier(hero);
 *    int   hitAdd = AriaT1Talents.Hooks.attackSkillHitBonus(hero);
 *
 * 5) 상태이상 저항 보정:
 *    // applyChance *= (1f - bonus) 형태로 사용
 *    float bonus = AriaT1Talents.Hooks.statusResistBonus(hero, AriaT1Talents.Status.BURN, currentElem);
 *
 * 6) 코덱스 장탄/환급/층전환:
 *    baseCharges += AriaT1Talents.Hooks.codexBonusBaseCharges(hero);
 *    int refundPct = AriaT1Talents.Hooks.codexKillRefundChance(hero);
 *    int floorRefill = AriaT1Talents.Hooks.onFloorTransitionRefill(hero);
 */
public class AriaT1Talents {

    // ===== 내부 상태/분류 =====
    public enum Status { BURN, POISON, SLOW, ROOT, STUN, OTHER }

    /** 공통 유틸: Buff 확보 */
    private static <T extends Buff> T ensure(Hero h, Class<T> c){
        T b = Buff.affect(h, c);
        return b;
    }

    // ------------------------------------------------------------
    // T1-1. 신속 전환 (Quick Switch)
    // 전환 시 추가 과부하 감쇠 –5/–6/–7/–8.
    // 전환 직후 Haste 1/1/1/2턴(내부 재발동 8/7/6/6턴).
    // ------------------------------------------------------------
    public static class QuickSwitch extends Buff {
        private static final String L   = "lvl";
        private static final String CD  = "icd";
        public int lvl;            // 0~4 (0 = 미보유)
        private int icdTurns = 0;  // 내부 재사용 대기(턴)

        // 수치 테이블
        private static final int[] EXTRA_REDUCE = { 0, 5, 6, 7, 8 };
        private static final int[] HASTE_TURNS  = { 0, 1, 1, 1, 2 };
        private static final int[] ICD_TURNS    = { 0, 8, 7, 6, 6 };

        /** 전환 시 추가 감쇠치(음수로 더할 것) */
        public int extraReductionOnSwitch(){
            if (lvl <= 0) return 0;
            return -EXTRA_REDUCE[lvl];
        }
        /** 전환 시 부여할 Haste 턴수(내부 쿨이 0일 때만) */
        public int hasteOnSwitch(){
            if (lvl <= 0) return 0;
            if (icdTurns > 0) return 0;
            icdTurns = ICD_TURNS[lvl];
            return HASTE_TURNS[lvl];
        }

        @Override
        public boolean act(){
            spend( TICK );
            if (icdTurns > 0) icdTurns--;
            return true;
        }

        @Override public int icon(){ return BuffIndicator.NONE; }
        @Override public float iconFadePercent(){ return 0f; }

        @Override public void storeInBundle(Bundle b){
            super.storeInBundle(b);
            b.put(L, lvl);
            b.put(CD, icdTurns);
        }
        @Override public void restoreFromBundle(Bundle b){
            super.restoreFromBundle(b);
            lvl = b.getInt(L);
            icdTurns = b.getInt(CD);
        }
    }

    // ------------------------------------------------------------
    // T1-2. 신중한 연구 (Careful Scholar)
    // 같은 속성 유지 과부하 축적 –2/–3/–4/–5.
    // 스킬/코덱스 사용 시 과부하 –1/–2/–3/–4(가산).
    // ------------------------------------------------------------
    public static class CarefulScholar extends Buff {
        private static final String L = "lvl";
        public int lvl;

        private static final int[] SAME_ELEM_REDUCE = { 0, 2, 3, 4, 5 };
        private static final int[] SKILL_DELTA      = { 0, -1, -2, -3, -4 };

        /** 같은 속성 유지로 인한 기본 축적(+12 등)에서 뺄 추가 감쇠 */
        public int onSameElementAccumulationReduce(){
            if (lvl <= 0) return 0;
            return SAME_ELEM_REDUCE[lvl];
        }
        /** 스킬/코덱스 사용 시 과부하 게이지에 더할 델타(음수) */
        public int onSkillOrCodexUsedDelta(){
            if (lvl <= 0) return 0;
            return SKILL_DELTA[lvl];
        }

        @Override public boolean act(){ spend(TICK); return true; }
        @Override public int icon(){ return BuffIndicator.NONE; }
        @Override public float iconFadePercent(){ return 0f; }

        @Override public void storeInBundle(Bundle b){
            super.storeInBundle(b);
            b.put(L, lvl);
        }
        @Override public void restoreFromBundle(Bundle b){
            super.restoreFromBundle(b);
            lvl = b.getInt(L);
        }
    }

    // ------------------------------------------------------------
    // T1-3. 코덱스 기초 (Codex Fundamentals)
    // 코덱스 피해 +5/8/11/15%, 적중 +8/12/16/20.
    // ------------------------------------------------------------
    public static class CodexFundamentals extends Buff {
        private static final String L = "lvl";
        public int lvl;
        private static final float[] DMG_MUL = { 1f, 1.05f, 1.08f, 1.11f, 1.15f };
        private static final int[]   HIT_ADD = { 0, 8, 12, 16, 20 };

        public float damageMultiplier(){ return DMG_MUL[Math.max(0, Math.min(lvl, 4))]; }
        public int attackSkillHitBonus(){ return HIT_ADD[Math.max(0, Math.min(lvl, 4))]; }

        @Override public boolean act(){ spend(TICK); return true; }
        @Override public int icon(){ return BuffIndicator.NONE; }
        @Override public float iconFadePercent(){ return 0f; }

        @Override public void storeInBundle(Bundle b){
            super.storeInBundle(b); b.put(L, lvl);
        }
        @Override public void restoreFromBundle(Bundle b){
            super.restoreFromBundle(b); lvl = b.getInt(L);
        }
    }

    // ------------------------------------------------------------
    // T1-4. 원소 수호 I (Elemental Guard I)
    // 상태이상 저항 총합 +10/15/20/25%.
    // 현재 속성 대응 추가저항 +10%(불: 화상 / 물: 중독 / 바람: 둔화·속박 / 땅: 스턴)
    // ------------------------------------------------------------
    public static class ElementalGuardI extends Buff {
        private static final String L = "lvl";
        public int lvl;

        private static final float[] RES_ALL = { 0f, 0.10f, 0.15f, 0.20f, 0.25f };
        private static final float   ELEM_MATCH_BONUS = 0.10f;

        public float resistBonusFor(Status st, Overheat.Element current){
            float base = RES_ALL[Math.max(0, Math.min(lvl, 4))];
            if (isElementMatched(current, st)) base += ELEM_MATCH_BONUS;
            if (base < 0f) base = 0f;
            if (base > 0.90f) base = 0.90f;
            return base;
        }

        private boolean isElementMatched(Overheat.Element elem, Status st){
            if (elem == null) return false;
            switch (elem){
                case FIRE:  return st == Status.BURN;
                case WATER: return st == Status.POISON;
                case WIND:  return st == Status.SLOW || st == Status.ROOT;
                case EARTH: return st == Status.STUN;
                default:    return false;
            }
        }

        @Override public boolean act(){ spend(TICK); return true; }
        @Override public int icon(){ return BuffIndicator.NONE; }
        @Override public float iconFadePercent(){ return 0f; }

        @Override public void storeInBundle(Bundle b){
            super.storeInBundle(b); b.put(L, lvl);
        }
        @Override public void restoreFromBundle(Bundle b){
            super.restoreFromBundle(b); lvl = b.getInt(L);
        }
    }

    // ------------------------------------------------------------
    // T1-5. 페이지 절약 (Page Economy)
    // 코덱스 기본 사용 횟수 +2/+3/+4/+5.
    // 코덱스로 처치 시 25/35/45/55% 확률로 사용 1 회복.
    // 층 전환 시 +1/+1/+2/+3 자동 회복.
    // ------------------------------------------------------------
    public static class PageEconomy extends Buff {
        private static final String L = "lvl";
        public int lvl;
        private static final int[] BONUS_BASE_CHARGES = { 0, 2, 3, 4, 5 };
        private static final int[] KILL_REFUND_PCT    = { 0, 25, 35, 45, 55 };
        private static final int[] FLOOR_REFILL       = { 0, 1, 1, 2, 3 };

        public int bonusBaseCharges(){ return BONUS_BASE_CHARGES[Math.max(0, Math.min(lvl, 4))]; }
        public int killRefundChancePct(){ return KILL_REFUND_PCT[Math.max(0, Math.min(lvl, 4))]; }
        public int onFloorTransitionRefill(){ return FLOOR_REFILL[Math.max(0, Math.min(lvl, 4))]; }

        @Override public boolean act(){ spend(TICK); return true; }
        @Override public int icon(){ return BuffIndicator.NONE; }
        @Override public float iconFadePercent(){ return 0f; }

        @Override public void storeInBundle(Bundle b){
            super.storeInBundle(b); b.put(L, lvl);
        }
        @Override public void restoreFromBundle(Bundle b){
            super.restoreFromBundle(b); lvl = b.getInt(L);
        }
    }

    // ============================================================
    // Public Hooks
    // ============================================================
    public static class Hooks {
        // -- 설치/레벨 세팅: 외부에서 각 재능 레벨을 알려줄 때 사용
        public static void setLevels(Hero h, int quickSwitch, int carefulScholar, int codexFund, int elemGuard1, int pageEconomy){
            ensure(h, QuickSwitch.class).lvl        = clamp01to04(quickSwitch);
            ensure(h, CarefulScholar.class).lvl     = clamp01to04(carefulScholar);
            ensure(h, CodexFundamentals.class).lvl  = clamp01to04(codexFund);
            ensure(h, ElementalGuardI.class).lvl    = clamp01to04(elemGuard1);
            ensure(h, PageEconomy.class).lvl        = clamp01to04(pageEconomy);
        }

        // -- 매 턴 1회 호출
        public static void onHeroTurn(Hero h){
            // 내부 쿨다운 감소는 QuickSwitch.act()에서 처리됨
            // 여기서는 아무 것도 하지 않아도 됨(유지 용도)
            ensure(h, QuickSwitch.class);
            ensure(h, CarefulScholar.class);
            ensure(h, CodexFundamentals.class);
            ensure(h, ElementalGuardI.class);
            ensure(h, PageEconomy.class);
        }

        // -- 원소 전환 훅
        public static int onElementSwitch(Hero h){
            return ensure(h, QuickSwitch.class).hasteOnSwitch();
        }
        public static int extraReductionOnSwitch(Hero h){
            return ensure(h, QuickSwitch.class).extraReductionOnSwitch();
        }

        // -- 같은 속성 유지 축적 보정(기본 +12 등에서 이 값만큼 추가 감쇠)
        public static int sameElementAccumulationReduce(Hero h){
            return ensure(h, CarefulScholar.class).onSameElementAccumulationReduce();
        }

        // -- 스킬/코덱스 사용 시 과부하 델타(음수 가능)
        public static int onSkillOrCodexUsed(Hero h){
            return ensure(h, CarefulScholar.class).onSkillOrCodexUsedDelta();
        }

        // -- 코덱스 수치 보정
        public static float codexDamageMultiplier(Hero h){
            return ensure(h, CodexFundamentals.class).damageMultiplier();
        }
        public static int attackSkillHitBonus(Hero h){
            return ensure(h, CodexFundamentals.class).attackSkillHitBonus();
        }

        // -- 상태이상 저항 보정
        public static float statusResistBonus(Hero h, Status st, Overheat.Element currentElem){
            return ensure(h, ElementalGuardI.class).resistBonusFor(st, currentElem);
        }

        // -- 코덱스 장탄/환급/층전환 훅
        public static int codexBonusBaseCharges(Hero h){
            return ensure(h, PageEconomy.class).bonusBaseCharges();
        }
        public static int codexKillRefundChance(Hero h){
            return ensure(h, PageEconomy.class).killRefundChancePct();
        }
        public static int onFloorTransitionRefill(Hero h){
            return ensure(h, PageEconomy.class).onFloorTransitionRefill();
        }

        private static int clamp01to04(int v){ if (v < 0) v = 0; if (v > 4) v = 4; return v; }
    }
}
