package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** Grants Quickshift and shows Overcharge; minimal scaffold. */
public class ElementalCodex extends Artifact {
    {
        image = ItemSpriteSheet.MASTERY;
    }

    @Override
    public String name() {
        return "Elemental Codex";
    }

    @Override
    public String desc() {
        return "An ancient tome that allows Tanlet to control elemental stances.";
    }

}
