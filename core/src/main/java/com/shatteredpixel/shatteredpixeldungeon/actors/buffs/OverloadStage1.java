package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/** Mild overcharge: +5% damage taken, reduces elemental baseline bonuses. */
public class OverloadStage1 extends Buff {
    {
        type = buffType.NEGATIVE;
    }
    @Override public int icon() { return BuffIndicator.POISON; }
    @Override public String toString() { return "Overcharge I"; }
}
