package com.shatteredpixel.shatteredpixeldungeon.items.weapon.codices;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class MagicBullet extends Codex {
    {
        tier = 1;
        image = ItemSpriteSheet.CODEX_BULLET;
        magicImage = ItemSpriteSheet.MAGIC_BULLET;

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
