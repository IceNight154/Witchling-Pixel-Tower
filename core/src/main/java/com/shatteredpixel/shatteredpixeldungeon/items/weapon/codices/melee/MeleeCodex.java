package com.shatteredpixel.shatteredpixeldungeon.items.weapon.codices.melee;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.codices.Codex;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

public class MeleeCodex extends Codex {
    {
        defaultAction = AC_CAST;
    }

    protected static final String AC_CAST = "cast";

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_CAST);
        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);

        if (action.equals(AC_CAST)) {

            curUser = hero;
            curItem = this;
            usesTargeting = true;
            GameScene.selectCell( selector );

        }
    }

    protected void onCodexAttack(Char enemy, int cell) {
        onAttackComplete(enemy, cell, curUser.codexAttack(enemy, this));
        onUse();
    }

    @Override
    protected void onAttackComplete(Char enemy, int cell, boolean hit) {
        usesTargeting = false;
    }

    private CellSelector.Listener selector = new CellSelector.Listener() {
        @Override
        public void onSelect( Integer target ) {
            if (target == null || target == curUser.pos) return;


            Char enemy = Actor.findChar(target);
            if (enemy == null) {
                GLog.w(Messages.get(MeleeCodex.class, "no_target"));
                return;
            }

            if (enemy.alignment != Char.Alignment.ENEMY) {
                GLog.w(Messages.get(MeleeCodex.class, "no_enemy"));
                return;
            }

            if (!Dungeon.level.adjacent(curUser.pos, enemy.pos)) {
                GLog.w(Messages.get(MeleeCodex.class, "not_adjacent"));
                return;
            }

            onCodexAttack(enemy, target);
            curUser.sprite.zap(target);
        }
        @Override
        public String prompt() {
            return Messages.get(SpiritBow.class, "prompt");
        }
    };
}
