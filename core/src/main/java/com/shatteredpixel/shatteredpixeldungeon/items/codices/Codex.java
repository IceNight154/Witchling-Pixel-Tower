package com.shatteredpixel.shatteredpixeldungeon.items.codices;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.HashSet;

public class Codex extends Item {
    private static final String TXT_STATUS	= "%d/%d";

    {
        defaultAction = AC_SHOOT;
    }

    public int tier;
    protected int maxUses;

    //variables that need to be saved
    protected int uses;

    private static final String USES = "uses";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(USES, uses);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        uses = bundle.getInt(USES);
    }

    private static final String AC_SHOOT = "SHOOT";

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

            if (hero.STR() < STRReq(level())) {
                GLog.w(Messages.get(this, "not_enough_str"));
                return;
            }
            GameScene.selectCell( shooter );

        }
    }

    @Override
    public String status() {
        if (!isIdentified()) {
            return null;
        } else {
            return Messages.format( TXT_STATUS, uses, maxUses );
        }
    }

    public int STRReq(int lvl){
        lvl = Math.max(0, lvl);

        //strength req decreases at +1,+3,+6,+10,etc.
        return (8 + tier * 2) - (int)(Math.sqrt(8 * lvl + 1) - 1)/2;
    }

    public int min(int lvl) {
        return (tier+1)+lvl;
    }

    public int min() {
        return min(buffedLvl());
    }

    public int max(int lvl) {
        return (tier+1)*(lvl+1);
    }

    public int max() {
        return max(buffedLvl());
    }

    public int magicDamage() {
        return Hero.heroDamageIntRange(min(), max());
    }

    public Magic castMagic() {
        return new Magic();
    }

    @Override
    public String info() {
        String desc = super.info();

        if (isIdentified()) {
            desc += "\n\n" + Messages.get(this, "codex_desc", tier, STRReq(buffedLvl()), min(), max());
        } else {
            desc += "\n\n" + Messages.get(this, "codex_typical_desc", tier, STRReq(0), min(0), max(0));
        }

        if (Dungeon.hero != null) {
            if (Dungeon.hero.STR() < STRReq(isIdentified() ? level() : 0)) {
                if (isIdentified()) {
                    desc += "\n\n" + Messages.get(this, "str_lack_desc");
                } else {
                    desc += "\n\n" + Messages.get(this, "probably_str_lack_desc");
                }
            }
        }

        return desc;
    }

    public class Magic extends Item {
        @Override
        protected void onThrow(int cell) {
            Char ch = Actor.findChar(cell);
            if (ch != null && ch != Dungeon.hero) {
                ch.damage(magicDamage(), this);
            }

            onCast();
        }

        public void onCast() {
            Codex.this.identify();
            uses--;

            if (uses <= 0) Codex.this.detach(Dungeon.hero.belongings.backpack);
            updateQuickslot();
        }

        @Override
        public void cast(Hero user, int dst) {
            super.cast(user, dst);
        }

        @Override
        public void throwSound() {
            super.throwSound();
        }
    }

    CellSelector.Listener shooter = new CellSelector.Listener() {

        @Override
        public void onSelect(Integer cell) {
            if (cell == null) {
                return;
            }

            if(cell == curUser.pos) return;
            castMagic().cast(Dungeon.hero, cell);
        }

        @Override
        public String prompt() {
            return Messages.get(SpiritBow.class, "prompt");
        }
    };

    public static HashSet<Class> MAGICS = new HashSet<>();
    static {
        MAGICS.add(Magic.class);
        MAGICS.add(MagicCannon.MagicCannonMagic.class);
    }
}
