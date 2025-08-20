package com.shatteredpixel.shatteredpixeldungeon.actors.buffs.imprints;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/** Air imprint: Target evasion reduced, more likely to be hit by projectiles. */
public class Unbalanced extends FlavourBuff {
    {
        type = buffType.NEGATIVE;
    }
    @Override public int icon() { return BuffIndicator.CRIPPLE; }
    @Override public String toString() { return "Unbalanced"; }
}
