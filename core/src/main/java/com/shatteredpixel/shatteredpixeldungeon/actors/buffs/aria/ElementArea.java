package com.shatteredpixel.shatteredpixeldungeon.actors.buffs.aria;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class ElementArea extends Buff {
    private ArrayList<Integer> arenaPositions = new ArrayList<>();
    private ArrayList<Emitter> arenaEmitters = new ArrayList<>();

    private static final float DURATION = 100;
    int left = 0;

    {
        type = buffType.NEUTRAL;
    }

    NewOverHeat.ElementType element;

    public void setup(int pos, int dist, NewOverHeat.ElementType type){
        this.element = type;

        PathFinder.buildDistanceMap( pos, BArray.or( Dungeon.level.passable, Dungeon.level.avoid, null ), dist );
        for (int i = 0; i < PathFinder.distance.length; i++) {
            if (PathFinder.distance[i] < Integer.MAX_VALUE && !arenaPositions.contains(i)) {
                arenaPositions.add(i);
            }
        }
        if (target != null) {
            fx(false);
            fx(true);
        }

        left = (int) DURATION;
    }

    public void extend( float duration ) {
        left += duration;
    }

    @Override
    public boolean act() {
        left--;
        if (left <= 0){
            detach();
        }

        spend(TICK);
        return true;
    }

    @Override
    public void fx(boolean on) {
        if (on){
            for (int i : arenaPositions){
                Emitter e = CellEmitter.get(i);
                switch (element) {
                    case FIRE: default:
                        e.pour(FireElementParticle.FACTORY, 0.05f);
                        break;
                    case WATER:
                        e.pour(WaterElementParticle.FACTORY, 0.05f);
                        break;
                    case WIND:
                        e.pour(WindElementParticle.FACTORY, 0.05f);
                        break;
                    case EARTH:
                        e.pour(EarthElementParticle.FACTORY, 0.05f);
                        break;
                }
                arenaEmitters.add(e);
            }
        } else {
            for (Emitter e : arenaEmitters){
                e.on = false;
            }
            arenaEmitters.clear();
        }
    }

    private static final String ARENA_POSITIONS = "arena_positions";
    private static final String LEFT = "left";
    private static final String ELEMENT = "element";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);

        int[] values = new int[arenaPositions.size()];
        for (int i = 0; i < values.length; i ++)
            values[i] = arenaPositions.get(i);
        bundle.put(ARENA_POSITIONS, values);

        bundle.put(LEFT, left);
        bundle.put(ELEMENT, NewOverHeat.ElementType.getIndexByElement(element));
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);

        int[] values = bundle.getIntArray( ARENA_POSITIONS );
        for (int value : values) {
            arenaPositions.add(value);
        }

        left = bundle.getInt(LEFT);
        element = NewOverHeat.ElementType.getElementByIndex(bundle.getInt(ELEMENT));
    }

    public static class ElementParticle extends PixelParticle.Shrinking {

        public ElementParticle() {
            super();

            lifespan = 0.6f;
        }

        public void reset( float x, float y){
            revive();

            this.x = x;
            this.y = y;

            left = lifespan;
            size = 8;

            speed.set( Random.Float( -8, +8 ), Random.Float( -16, -32 ) );
        }

        @Override
        public void update() {
            super.update();

            am = 1 - left / lifespan;
        }

    }

    public static class FireElementParticle extends ElementParticle {

        public static final Emitter.Factory FACTORY = new Emitter.Factory() {
            @Override
            public void emit( Emitter emitter, int index, float x, float y ) {
                ((FireElementParticle)emitter.recycle( FireElementParticle.class )).reset( x, y );
            }
            @Override
            public boolean lightMode() {
                return false;
            }
        };

        public FireElementParticle() {
            super();

            color(0xFF0000);
        }
    }

    public static class WaterElementParticle extends ElementParticle {

        public static final Emitter.Factory FACTORY = new Emitter.Factory() {
            @Override
            public void emit( Emitter emitter, int index, float x, float y ) {
                ((WaterElementParticle)emitter.recycle( WaterElementParticle.class )).reset( x, y );
            }
            @Override
            public boolean lightMode() {
                return false;
            }
        };

        public WaterElementParticle() {
            super();

            color(0x0000FF);
        }
    }

    public static class WindElementParticle extends ElementParticle {

        public static final Emitter.Factory FACTORY = new Emitter.Factory() {
            @Override
            public void emit( Emitter emitter, int index, float x, float y ) {
                ((WindElementParticle)emitter.recycle( WindElementParticle.class )).reset( x, y );
            }
            @Override
            public boolean lightMode() {
                return false;
            }
        };

        public WindElementParticle() {
            super();

            color(0x000000);
        }
    }

    public static class EarthElementParticle extends ElementParticle {

        public static final Emitter.Factory FACTORY = new Emitter.Factory() {
            @Override
            public void emit( Emitter emitter, int index, float x, float y ) {
                ((EarthElementParticle)emitter.recycle( EarthElementParticle.class )).reset( x, y );
            }
            @Override
            public boolean lightMode() {
                return false;
            }
        };

        public EarthElementParticle() {
            super();

            color(0x29CC29);
        }
    }
}
