package com.shatteredpixel.shatteredpixeldungeon.actors.buffs.imprints;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/** Water imprint: Target deals less physical damage, heals received by caster improved. */
public class Sapped extends FlavourBuff {
    {
        type = buffType.NEGATIVE;
    }
    @Override public int icon() { return BuffIndicator.WEAKNESS; }
    @Override public String toString() { return "Sapped"; }
}
