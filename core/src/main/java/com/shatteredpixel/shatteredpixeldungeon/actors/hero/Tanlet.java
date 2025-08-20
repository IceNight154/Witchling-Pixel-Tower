package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.OverloadStage1;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.OverloadStage2;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ElementalCodex;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.PrismaticWand;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.FocusCharm;
import com.shatteredpixel.shatteredpixeldungeon.utils.ElementalBurst;

/**
 * Tanlet - Elemental stance hero. Wire this into your HeroClass registry to enable selection.
 * NOTE: Method names follow SPD patterns; adjust imports/names to your local fork as needed.
 */
public class Tanlet extends Hero {
    public ElementStance stance = ElementStance.FIRE;
    public int overcharge = 0;
    public static final int OC_STAGE1 = 4, OC_STAGE2 = 7, OC_STAGE3 = 10;
    private int quickshiftCD = 0;
    private boolean shiftedThisTurn = false;

    public Tanlet() {
        HP = HT = 20;
        STR = 10;

        PrismaticWand wand = new PrismaticWand();
        if (!wand.collect()) Dungeon.level.drop(wand, pos).sprite.drop();

        FocusCharm charm = new FocusCharm();
        if (!charm.collect()) Dungeon.level.drop(charm, pos).sprite.drop();

        ElementalCodex codex = new ElementalCodex();
        if (!codex.collect()) Dungeon.level.drop(codex, pos).sprite.drop();

        updateStanceBonuses();
    }

    public boolean canQuickshift(){ return quickshiftCD <= 0 && !shiftedThisTurn; }

    public void quickshift(ElementStance next) {
        if (stance == next || !canQuickshift()) return;
        overcharge = Math.max(0, overcharge - 3);
        stance = next;
        quickshiftCD = Math.max(3, 4 - talentReduction());
        shiftedThisTurn = true;
        ElementalBurst.trigger(this, stance);
        updateStanceBonuses();
    }

    public void onOperateComplete() {
        // called after taking a turn; reset shifted flag & tick cooldown
        super.onOperateComplete();
        shiftedThisTurn = false;
        if (quickshiftCD > 0) quickshiftCD--;
        tickOvercharge();
    }

    private void tickOvercharge(){
        // stance pacing
        if (stance == ElementStance.FIRE) overcharge += 2;
        else if (stance == ElementStance.EARTH) overcharge += 0;
        else overcharge += 1;
        checkOvercharge();
    }

    private void checkOvercharge(){
        if (overcharge >= OC_STAGE3) {
            // TODO: force-random shift and apply a short debuff/stun if desired
            overcharge = OC_STAGE1; // fall back to soften loop
        } else if (overcharge >= OC_STAGE2) {
            Buff.affect(this, OverloadStage2.class);
        } else if (overcharge >= OC_STAGE1) {
            Buff.affect(this, OverloadStage1.class);
        } else {
            Buff.detach(this, OverloadStage1.class);
            Buff.detach(this, OverloadStage2.class);
        }
    }

    private void updateStanceBonuses(){
        ElementBonuses.apply(this, stance);
    }

    private int talentReduction(){
        // hook into your talent lookups if desired
        return 0;
    }
}
