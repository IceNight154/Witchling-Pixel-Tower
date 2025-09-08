package com.shatteredpixel.shatteredpixeldungeon.items.bags.grimoire;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.aria.NewOverHeat;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.codices.Codex;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.watabou.noosa.particles.Emitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ManaballElementTrailParticles;

public class GrimoireAria extends Bag {

    private static final String AC_SHOOT = "SHOOT";

    {
        image = ItemSpriteSheet.GRIMOIRE_ARIA;

        defaultAction = AC_SHOOT;

        levelKnown = true;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_SHOOT);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {

        super.execute(hero, action);

        if (action.equals(AC_SHOOT)) {

            curUser = hero;
            curItem = this;
            GameScene.selectCell( shooter );

        }
    }

    @Override
    public boolean canHold( Item item ) {
        if (item instanceof Codex){
            return super.canHold(item);
        } else {
            return false;
        }
    }

    public int capacity(){
        return 5;
    }

    @Override
    public void onDetach( ) {
        super.onDetach();
    }

    @Override
    public int value() {
        return 40;
    }

    @Override
    public int level() { //영웅 레벨에 따라 강화수치 증가
        return Dungeon.hero == null ? 0 : Dungeon.hero.lvl/5;
    }

    @Override
    public int visiblyUpgraded() {
        return level();
    }

    @Override
    public int buffedVisiblyUpgraded() {
        return buffedLvl();
    }

    @Override
    public int buffedLvl() {
        return level();
    }

    public int min(int lvl) {
        return 5+lvl;
    }

    public int max(int lvl) {
        return 25+5*lvl;
    }

    public int magicDamage() {
        return Hero.heroDamageIntRange(min(buffedLvl()), max(buffedLvl()));
    }

    public ManaBall knockBall(){
        return new ManaBall();
    }


    // === ManaBall Color Helper ===
    private static int manaBallColor(Hero hero){
        if (hero != null){
            // Preferred: NewOverHeat
            try {
                NewOverHeat oh = hero.buff(NewOverHeat.class);
                if (oh != null){
                    NewOverHeat.ElementType e = oh.getElement();
                    switch (e){
                        case FIRE:  return 0xE24F2E;
                        case WATER: return 0x05AAD3;
                        case EARTH: return 0xF5CB3F;
                        case WIND:  return 0x14EA83;
                    }
                }
            } catch (Throwable ignored){}
            // Legacy: Overheat with OverheatElement enum
            try {
                Class<?> cls = Class.forName("com.shatteredpixel.shatteredpixeldungeon.actors.buffs.aria.Overheat");
                Object ob = hero.buff((Class)cls);
                if (ob != null){
                    Object e = null;
                    try { e = ob.getClass().getMethod("getElement").invoke(ob); } catch (Throwable ignored) {}
                    if (e == null) try { e = ob.getClass().getMethod("element").invoke(ob); } catch (Throwable ignored) {}
                    if (e == null) try { e = ob.getClass().getField("element").get(ob); } catch (Throwable ignored) {}
                    if (e != null){
                        String name = e.toString();
                        if ("WATER".equals(name)) return 0x05AAD3;
                        if ("EARTH".equals(name)) return 0xF5CB3F;
                        if ("WIND".equals(name))  return 0x14EA83;
                        return 0xE24F2E; // FIRE or default
                    }
                }
            } catch (Throwable ignored){}
        }
        return 0xE24F2E;
    }
    // === Manaball element key helper (for trail) ===
    private static String manaBallElementKey(Hero hero){
        if (hero != null){
            try {
                NewOverHeat oh = hero.buff(NewOverHeat.class);
                if (oh != null){
                    NewOverHeat.ElementType e = oh.getElement();
                    switch (e){
                        case FIRE:  return "fire";
                        case WATER: return "water";
                        case EARTH: return "earth";
                        case WIND:  return "wind";
                    }
                }
            } catch (Throwable ignored){}
        }
        return "fire";
    }

    // Bresenham line helper (grid path from 'from' to 'to')
    private static java.util.List<Integer> lineBetween(int from, int to){
        java.util.ArrayList<Integer> list = new java.util.ArrayList<>();
        int w = Dungeon.level.width();
        int x0 = from % w, y0 = from / w;
        int x1 = to   % w, y1 = to   / w;
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true){
            int pos = y0 * w + x0;
            if (pos >= 0 && pos < Dungeon.level.length()) list.add(pos);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy){ err += dy; x0 += sx; }
            if (e2 <= dx){ err += dx; y0 += sy; }
        }
        return list;
    }

    // Spawn elemental trail along flight path (visual-only)
    private static void spawnManaballTrail(Hero hero, int from, int to){
        Emitter.Factory fx = ManaballElementTrailParticles.factoryFor(manaBallElementKey(hero));
        java.util.List<Integer> path = lineBetween(from, to);
        int count = path.size();
        for (int i = 0; i < count; i++){
            int cell = path.get(i);
            // small stagger density: more near source and target
            int n = (i == 0 || i == count-1) ? 3 : (i % 3 == 0 ? 2 : 1);
            CellEmitter.center(cell).burst(fx, n);
        }
    }
    public class ManaBall extends Item {
        {
            image = ItemSpriteSheet.MANA_BALL;
        }


        // Color-tinted projectile sprite only (no logic changes)
        private final ItemSprite.Glowing manaGlow;

        public ManaBall(){
            manaGlow = new ItemSprite.Glowing(manaBallColor(Dungeon.hero), 1.5f);
        }

        @Override
        public ItemSprite.Glowing glowing(){
            return manaGlow;
        }
        @Override
        protected void onThrow(int cell) {
            Char ch = Actor.findChar(cell);
            if (ch != null && ch != Dungeon.hero) {
                ch.damage(GrimoireAria.this.magicDamage(), this);
                Buff.affect(Dungeon.hero, NewOverHeat.class).heat(2);
            }
        }

        @Override
        public void cast(Hero user, int dst) {
            // Visual trail along the flight path (element-dependent)
            try {
                spawnManaballTrail(user, user.pos, dst);
            } catch (Throwable ignored){}
            // Keep default projectile/throw logic
            super.cast(user, dst);
        }

        @Override
        public void throwSound() {
            Sample.INSTANCE.play( Assets.Sounds.ATK_GRIMOIRE, 1, Random.Float(0.87f, 1.15f) );
        }
    }

    CellSelector.Listener shooter = new CellSelector.Listener() {

        @Override
        public void onSelect(Integer cell) {
            if (cell == null) {
                return;
            }

            if (cell == curUser.pos) return;

            Char targetChar = Actor.findChar(cell);
            if (targetChar == null || !targetChar.isAlive() || targetChar.alignment == Char.Alignment.ALLY) {
                int autoCell = findNearestEnemyCell(curUser);
                if (autoCell != -1 && autoCell != curUser.pos) {
                    cell = autoCell; // prefer closest visible hostile
                }
            }

            knockBall().cast(Dungeon.hero, cell);

        }

        @Override
        public String prompt() {
            return Messages.get(SpiritBow.class, "prompt");
        }
    };


    /**
     * Returns the cell position of the nearest hostile, living mob within hero's FOV; -1 if none.
     */
    private int findNearestEnemyCell(Hero hero) {
        if (hero == null || Dungeon.level == null || Dungeon.level.mobs == null) return -1;

        int bestCell = -1;
        int bestDist = Integer.MAX_VALUE;

        for (Mob mob : Dungeon.level.mobs) {
            if (mob == null || !mob.isAlive()) continue;
            if (mob.alignment == Char.Alignment.ALLY) continue;

            int pos = mob.pos;
            if (pos < 0 || pos >= Dungeon.level.length()) continue;

            // must be in hero's field of view, if available
            if (Dungeon.level.heroFOV != null && !Dungeon.level.heroFOV[pos]) continue;

            int dist;
            try {
                dist = Dungeon.level.distance(hero.pos, pos);
            } catch (Throwable t) {
                // Fallback: Manhattan distance if level.distance is unavailable
                int w = Dungeon.level.width();
                int x1 = hero.pos % w, y1 = hero.pos / w;
                int x2 = pos % w,   y2 = pos / w;
                dist = Math.abs(x1 - x2) + Math.abs(y1 - y2);
            }

            if (dist < bestDist) {
                bestDist = dist;
                bestCell = pos;
            }
        }
        return bestCell;
    }

}