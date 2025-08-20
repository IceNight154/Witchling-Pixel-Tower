package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class FocusCharm extends Artifact {

    {
        image = ItemSpriteSheet.RING_AGATE; // 기본 반지 아이콘 사용
    }

    @Override
    public String name() {
        return "Focus Charm";
    }

    @Override
    public String desc() {
        return "A charm that helps Tanlet maintain balance when shifting elements.";
    }
}
