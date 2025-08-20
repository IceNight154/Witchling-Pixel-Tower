package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

public enum ElementStance {
    FIRE, WATER, AIR, EARTH;

    public ElementStance next() {
        switch (this) {
            case FIRE:  return WATER;
            case WATER: return AIR;
            case AIR:   return EARTH;
            default:    return FIRE;
        }
    }

    public ElementStance prev() {
        switch (this) {
            case FIRE:  return EARTH;
            case WATER: return FIRE;
            case AIR:   return WATER;
            default:    return AIR;
        }
    }
}