package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/** Severe overcharge: +10% damage taken, +1 mana cost to spells. */
public class OverloadStage2 extends Buff {
    {
        type = buffType.NEGATIVE;
    }
    @Override public int icon() { return BuffIndicator.TOXIC; }
    @Override public String toString() { return "Overcharge II"; }
}
