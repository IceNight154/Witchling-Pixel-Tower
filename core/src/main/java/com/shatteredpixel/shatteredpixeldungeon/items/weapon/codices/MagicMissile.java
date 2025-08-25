package com.shatteredpixel.shatteredpixeldungeon.items.weapon.codices;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class MagicMissile extends Codex {
    {
        tier = 3;
        image = ItemSpriteSheet.CODEX_MISSILE;
        magicImage = ItemSpriteSheet.MAGIC_MISSILE;

        baseUses = 10;
    }

    @Override
    public int max(int lvl) {
        return super.max(lvl);
    }

    @Override
    public int min(int lvl) {
        return super.min(lvl);
    }
}
