package com.shatteredpixel.shatteredpixeldungeon.items.weapon.codices;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class MagicCannon extends Codex {

    public MagicCannon() {
        super();
        image = ItemSpriteSheet.SHURIKEN;
        hitSound = Assets.Sounds.HIT_STAB;
        hitSoundPitch = 1.2f;
        tier = 1;
        baseUses = 5;
    }

    public int proc(Hero attacker, Char defender, int damage) {
        applyMagicDamageSafe(defender, damage);
        return 0;
    }

    private void applyMagicDamageSafe(Char target, int dmg) {
        if (target == null || dmg <= 0) return;
        try {
            java.lang.reflect.Method m = Char.class.getMethod("magicDamage", int.class, Object.class);
            m.invoke(target, dmg, this);
        } catch (Exception e) {
            target.damage(dmg, this);
        }
    }
}
