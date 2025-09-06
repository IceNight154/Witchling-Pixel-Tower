package com.shatteredpixel.shatteredpixeldungeon.ui.aria;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.aria.Overheat;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.aria.OverheatElement;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;

/** UI helper for switching Aria's element. Wire this to your button/Hotkey. */
public class OverheatElementSwitch {

    public static void show(final Hero hero){
        if (hero == null) return;
        GameScene.show(new WndOptions(
                "Aria: Element Switch",
                "Switching elements reduces Overheat and may grant a short effect.",
                new String[]{"Fire", "Water", "Wind", "Earth"}
        ){
            @Override
            protected void onSelect(int index){
                OverheatElement picked = new OverheatElement[]{OverheatElement.FIRE, OverheatElement.WATER, OverheatElement.WIND, OverheatElement.EARTH}[index];
                Overheat oh = Overheat.install(hero);
                // In future, pull modifiers from talents:
                int extraReduction = 0; // e.g., Quick Switch levels
                Overheat.SwitchResult res = oh.onElementSwitch(hero, picked, extraReduction);

                // Post-effects: Haste on Quick Switch can be applied here if desired.
                // EARTH barrier 1 turn
                if (res.shieldPercentOfMaxHP > 0f){
                    Object barrier = Buff.affect(
                            hero,
                            com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class
                    );
                    int amount = (int)Math.ceil(hero.HT * res.shieldPercentOfMaxHP);
                    try {
                        // Some versions expose a public 'set' or 'inc' style API
                        try {
                            com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class
                                    .getMethod("set", int.class)
                                    .invoke(barrier, amount);
                        } catch (NoSuchMethodException noSet){
                            com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class
                                    .getMethod("inc", int.class)
                                    .invoke(barrier, amount);
                        }
                    } catch (Throwable ignored) {}
                }

                // Refresh UI
                try {
                    com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.refreshHero();
                } catch (Throwable ignored){}
            }
        });
    }
}
