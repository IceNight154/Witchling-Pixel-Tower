package com.shatteredpixel.shatteredpixeldungeon.actors.buffs.imprints;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/** Earth imprint: Reduces target armor, increases impact damage taken. */
public class Brittled extends FlavourBuff {
    {
        type = buffType.NEGATIVE;
    }
    @Override public int icon() { return BuffIndicator.VULNERABLE; }
    @Override public String toString() { return "Brittled"; }
}
