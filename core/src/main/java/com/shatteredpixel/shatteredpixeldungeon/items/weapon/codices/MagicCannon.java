package com.shatteredpixel.shatteredpixeldungeon.items.weapon.codices;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class MagicCannon extends Codex {
    {
        tier = 2;
        image = ItemSpriteSheet.CODEX_CANNON;
        magicImage = ItemSpriteSheet.MAGIC_CANNON;

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
