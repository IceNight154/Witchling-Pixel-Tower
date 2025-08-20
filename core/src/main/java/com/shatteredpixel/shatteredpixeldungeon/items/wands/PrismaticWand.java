package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.imprints.Brittled;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.imprints.Sapped;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.imprints.Scalded;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.imprints.Unbalanced;

public class PrismaticWand extends Wand {

    {
        // 유효한 스프라이트 상수 사용
        image = ItemSpriteSheet.WAND_BLAST_WAVE;
    }

    @Override
    public void onZap(Ballistica bolt) {
    }

    @Override
    public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {

        if (attacker instanceof Hero) {
            Hero h = (Hero) attacker;

            try {
                java.lang.reflect.Field f = h.getClass().getField("stance");
                Object st = f.get(h);
                if (st != null) {
                    String s = st.toString();
                    if ("FIRE".equals(s)) {
                        Buff.affect(defender, Scalded.class, 2f);
                    } else if ("WATER".equals(s)) {
                        Buff.affect(defender, Sapped.class, 2f);
                    } else if ("AIR".equals(s)) {
                        Buff.affect(defender, Unbalanced.class, 2f);
                    } else if ("EARTH".equals(s)) {
                        Buff.affect(defender, Brittled.class, 2f);
                    }
                }
            } catch (Exception ignore) {
            }
        }
    }
}
