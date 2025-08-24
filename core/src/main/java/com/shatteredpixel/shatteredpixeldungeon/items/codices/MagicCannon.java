package com.shatteredpixel.shatteredpixeldungeon.items.codices;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class MagicCannon extends Codex{
    {
        image = ItemSpriteSheet.CODEX_CANNON;

        tier = 1;
        uses = maxUses = 20;
    }

    @Override
    public Magic castMagic() {
        return new MagicCannonMagic();
    }

    public class MagicCannonMagic extends Magic {
        {
            image = ItemSpriteSheet.MAGIC_CANNON;
        }
    }
}
