package com.shatteredpixel.shatteredpixeldungeon.actors.buffs.aria;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * Aria's Overheat gauge controller.
 *
 * ✅ Key behaviors implemented per design:
 *  - Gauge ranges 0..100, with meltdown at 100
 *  - +8 on skill/codex use; +12/turn for up to 3 turns afterward while not idle
 *  - When idle for 3+ full turns, -12/turn
 *  - Element switch applies -25 (extraNegativeReduction lowers by additional amount)
 *  - WATER: -2 natural cooling/turn (with heal applied externally)
 *  - FIRE: +15% accumulation factor (applies to positive deltas only)
 *  - Meltdown: random element swap, reset to 60, and a pending event is queued for external handling
 *
 *  This buff does not directly handle AoE/self-damage/Exhaust on meltdown;
 *  instead it exposes a pending event via {@link #consumePendingMeltdown()}.
 *  Your codex/hero logic should poll and apply those effects (see README).
 */
public class Overheat extends Buff {

    public static final int MAX_OVERHEAT = 100;

    private static final String TAG_GAUGE = "gauge";
    private static final String TAG_ELEM  = "element";
    private static final String TAG_SINCE = "turnsSinceUse";

    /** Install or fetch the Overheat buff on a hero. */
    public static Overheat install(Hero hero){
        if (hero == null) return null;
        Overheat existing = hero.buff(Overheat.class);
        if (existing != null) return existing;
        Buff.affect(hero, Overheat.class);
        return hero.buff(Overheat.class);
    }

    private int gauge = 0;
    private int turnsSinceUse = 999; // big = idle
    private OverheatElement element = OverheatElement.FIRE;
    private int codexMaxDamageRemembered = 0;

    // meltdown notification
    private MeltdownResult pendingMeltdown = null;

    public Overheat(){ type = buffType.POSITIVE; }

    public OverheatElement element(){ return element; }
    public int gauge(){ return gauge; }

    /** Optional helper so Codex can tell us its max damage for meltdown math. */
    public void noteCodexMaxDamage(int max){
        this.codexMaxDamageRemembered = Math.max(this.codexMaxDamageRemembered, Math.max(0, max));
    }

    /** Call this when Aria uses a skill or codex (without switching element). */
    public void onSkillOrCodexUsed(Hero hero){
        turnsSinceUse = 0;
        applyDeltaInternal( Math.round( 8 * element.positiveOverheatAccumFactor() ) );
        // UI refresh is handled by the engine each tick, but we can suggest it
        try { com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.refreshHero(); } catch (Throwable t){}
    }

    /** Must be called once per actor turn; Buff.act() already does this. */
    @Override
    public boolean act() {
        // Apply "active gain" or "idle cooling" depending on how long it's been since last use.
        if (turnsSinceUse < 3){
            // Not idle yet -> gain +12 per turn while maintaining element
            applyDeltaInternal( Math.round(12 * element.positiveOverheatAccumFactor()) );
        } else {
            // Idle for 3 or more full turns -> -12 per turn
            applyDeltaInternal(-12);
        }

        // Elemental natural cooling (WATER: -2)
        int natural = element.naturalCoolingDeltaPerTurn();
        if (natural != 0) applyDeltaInternal(natural);

        turnsSinceUse = Math.min(turnsSinceUse + 1, 999);

        spend(TICK);
        return true;
    }

    /**
     * Element switch: applies the -25 base reduction (plus any extraNegativeReduction),
     * resets the active window, and returns the result (including EARTH shield %).
     *
     * @param extraNegativeReduction extra reduction to apply (e.g., from Quick Switch etc.)
     */
    public SwitchResult onElementSwitch(Hero hero, OverheatElement newElem, int extraNegativeReduction){
        if (newElem == null) newElem = OverheatElement.FIRE;

        OverheatElement prev = this.element;
        this.element = newElem;
        turnsSinceUse = 999; // reset to idle until next use

        int applied = applyDeltaInternal(-25 - Math.max(0, extraNegativeReduction));

        float shieldPct = newElem.onSwitchShieldPercent();
        return new SwitchResult(prev, newElem, applied, shieldPct);
    }

    /** Internal: creates and queues a meltdown event + applies the reset/element flip. */
    private MeltdownResult doMeltdown(){
        OverheatElement prev = this.element;
        this.element = OverheatElement.randomDifferent(prev);
        this.gauge = 60; // reset

        MeltdownResult r = new MeltdownResult();
        r.prev = prev;
        r.now  = this.element;
        r.aoeDamagePercentOfCodexMax = 25;
        r.selfDamagePercentOfMaxHP   = 25;
        r.exhaustTurns = 1;
        r.codexMaxDamageRemembered = this.codexMaxDamageRemembered;

        // queue for external handling
        pendingMeltdown = r;
        return r;
    }

    /** Retrieve and clear a pending meltdown (if any). */
    public MeltdownResult consumePendingMeltdown(){
        MeltdownResult m = pendingMeltdown;
        pendingMeltdown = null;
        return m;
    }

    /** Applies a raw delta, respecting clamping and triggering meltdown. */
    private int applyDeltaInternal(int rawDelta){
        if (rawDelta == 0) return 0;
        int before = gauge;
        gauge = Math.max(0, Math.min(MAX_OVERHEAT, gauge + rawDelta));

        // Meltdown check
        if (gauge >= MAX_OVERHEAT){
            doMeltdown();
        }

        // Suggest a UI refresh
        try { com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.refreshHero(); } catch (Throwable t){}
        return gauge - before;
    }

    /* ========================== SPD overrides & UI ========================== */

    @Override
    public int icon() {
        // Prefer per-tier icons if your BuffIndicator defines them; otherwise fall back
        // to a single generic Overheat icon or the per-element icon.
        try {
            if (gauge >= MAX_OVERHEAT) {
                return (int) BuffIndicator.class.getField("OVERHEAT_MELTDOWN").get(null);
            }
            int g = clampInt(gauge, 0, MAX_OVERHEAT);
            if      (g <= 0) return (int) BuffIndicator.class.getField("OVERHEAT_0").get(null);
            else if (g < 25) return (int) BuffIndicator.class.getField("OVERHEAT_25").get(null);
            else if (g < 50) return (int) BuffIndicator.class.getField("OVERHEAT_50").get(null);
            else if (g < 75) return (int) BuffIndicator.class.getField("OVERHEAT_75").get(null);
            else             return (int) BuffIndicator.class.getField("OVERHEAT_100").get(null);
        } catch (Throwable missing){
            // Fallback: show per-element icon if available, or NONE
            try { return element.buffIconId(); } catch (Throwable t){ return BuffIndicator.NONE; }
        }
    }

    @Override
    public String iconTextDisplay() {
        // Show 0..100
        int __pct = Math.max(0, Math.min((int)Math.round((gauge * 100.0) / (double)MAX_OVERHEAT), 100));
        return Integer.toString(__pct);
    }
    private static int clampInt(int v, int min, int max){
        return v < min ? min : (v > max ? max : v);
    }

    @Override
    public float iconFadePercent() { return 0f; }

    @Override
    public String toString() { return "Overheat (" + element.displayName() + ")"; }

    @Override
    public String desc() {
        StringBuilder sb = new StringBuilder();
        sb.append("Overheat Gauge: ").append(gauge).append("/").append(MAX_OVERHEAT).append('\n');
        sb.append("Element: ").append(element.displayName()).append('\n');
        sb.append(element.passiveSummary()).append('\n');
        sb.append("Active gain: +12/turn after using a skill/codex; +8 on use.\n");
        sb.append("Switch: -25 (improved by talents). Idle 3+ turns: -12/turn.\n");
        sb.append("Water: -2 natural cooling/turn with small self-heal (apply externally).\n");
        sb.append("Meltdown at 100: random element swap, AoE 25% of codex max (r=2), ");
        sb.append("self-damage 25% MaxHP, Exhaust 1T, resets to 60 (effects applied externally).");
        return sb.toString();
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(TAG_GAUGE, gauge);
        bundle.put(TAG_SINCE, turnsSinceUse);
        bundle.put(TAG_ELEM, OverheatElement.serialize(element));
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        gauge = bundle.getInt(TAG_GAUGE);
        turnsSinceUse = bundle.getInt(TAG_SINCE);
        element = OverheatElement.deserialize(bundle.getString(TAG_ELEM));
    }

    /* ====================== External helpers for passives ====================== */

    public float damageOutMul(){ return element.outgoingDamageMultiplier(); }
    public float damageInMul(){  return element.incomingDamageMultiplier(); }
    public int   evasionBonusPct(){ return element.evasionBonusPct(); }

    /* ============================== DTO types ============================== */

    public static class SwitchResult {
        public final OverheatElement prev;
        public final OverheatElement now;
        public final int appliedGaugeDelta;
        public final float shieldPercentOfMaxHP;
        public SwitchResult(OverheatElement prev, OverheatElement now, int appliedGaugeDelta, float shieldPercentOfMaxHP){
            this.prev = prev;
            this.now = now;
            this.appliedGaugeDelta = appliedGaugeDelta;
            this.shieldPercentOfMaxHP = shieldPercentOfMaxHP;
        }
    }

    public static class MeltdownResult {
        public OverheatElement prev, now;
        public int aoeDamagePercentOfCodexMax;
        public int selfDamagePercentOfMaxHP;
        public int exhaustTurns;
        public int codexMaxDamageRemembered;
    }
}
