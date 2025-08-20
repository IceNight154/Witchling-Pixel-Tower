package com.shatteredpixel.shatteredpixeldungeon.actors.buffs.imprints;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/** Fire imprint: Target takes more fire damage, small burn. */
public class Scalded extends FlavourBuff {
    {
        type = buffType.NEGATIVE;
    }
    @Override public int icon() { return BuffIndicator.FIRE; }
    @Override public String toString() { return "Scalded"; }
}
