package com.shatteredpixel.shatteredpixeldungeon.utils;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.ElementStance;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.watabou.utils.PathFinder;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.imprints.*;

public class ElementalBurst {
    public static void trigger(Hero h, ElementStance stance) {
        switch (stance){
            case FIRE:  burstFire(h);  break;
            case WATER: burstWater(h); break;
            case AIR:   burstAir(h);   break;
            case EARTH: burstEarth(h); break;
        }
    }

    private static void burstFire(Hero h){
        // simple AoE heat pulse around the hero
        CellEmitter.get(h.pos).burst(Speck.factory(Speck.LIGHT), 6);
        for (int off : PathFinder.NEIGHBOURS8) {
            int c = h.pos + off;
            if (c >= 0 && c < Dungeon.level.length()) {
                Char ch = Actor.findChar(c);
                if (ch != null && ch != h) Buff.affect(ch, Scalded.class, 2f);
            }
        }
    }

    private static void burstWater(Hero h){
        CellEmitter.get(h.pos).burst(Speck.factory(Speck.HEALING), 6);
        Buff.affect(h, Barrier.class).incShield(Math.max(1, h.HT/10));
    }

    private static void burstAir(Hero h){
        CellEmitter.get(h.pos).burst(Speck.factory(Speck.LIGHT), 6);
        Invisibility.dispel(); //light breeze counters invis
    }

    private static void burstEarth(Hero h){
        CellEmitter.get(h.pos).burst(Speck.factory(Speck.ROCK), 6);
        Buff.affect(h, Barrier.class).incShield(2);
    }

}
