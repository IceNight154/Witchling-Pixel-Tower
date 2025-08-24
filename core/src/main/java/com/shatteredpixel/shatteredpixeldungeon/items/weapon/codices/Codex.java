package com.shatteredpixel.shatteredpixeldungeon.items.weapon.codices;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.grimoire.GrimoireAria;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSharpshooting;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ParchmentScrap;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Explosive;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.InventoryPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;

abstract public class Codex extends Weapon {
    /** Marker for anti-magic RESISTS checks (e.g., AntiMagic.RESISTS.add(Codex.MagicDamage.class)). */
    public static class MagicDamage { /* no fields; used as a tag type */ }


    {
        stackable = true;
        quantity = defaultQuantity();

        bones = true;

        defaultAction = AC_THROW;
        usesTargeting = true;
    }

    public long setID = new SecureRandom().nextLong();

    public boolean spawnedForEffect = false;

    protected boolean sticky = true;

    public static final float MAX_DURABILITY = 100;
    protected float durability = MAX_DURABILITY;
    protected float baseUses = 8;

    public boolean holster;
    public Codex parent;
    public int tier;

    protected int usesToID(){
        return 10; //half of a melee weapon
    }
    /**
     * Returns the magic damage roll for this Codex based on its current (buffed) level.
     */
    public int magicDamage() {
        // Use a standard damage roll based on min()/max() for this weapon
        return com.watabou.utils.Random.NormalIntRange(min(), max());
    }

    @Override
    protected void onThrow(int cell) {
        Char ch = Actor.findChar(cell);
        if (ch != null && ch != Dungeon.hero) {
            ch.damage(magicDamage(), new Codex.MagicDamage());
        }
        decrementDurability();
    }

    @Override
    public int min() {
        if (Dungeon.hero != null){
            return Math.max(0, min(buffedLvl() + RingOfSharpshooting.levelDamageBonus(Dungeon.hero)));
        } else {
            return Math.max(0 , min( buffedLvl() ));
        }
    }

    @Override
    public int min(int lvl) {
        return  2 * tier +                      //base
                lvl;                            //level scaling
    }

    @Override
    public int max() {
        if (Dungeon.hero != null){
            return Math.max(0, max( buffedLvl() + RingOfSharpshooting.levelDamageBonus(Dungeon.hero) ));
        } else {
            return Math.max(0 , max( buffedLvl() ));
        }
    }

    @Override
    public int max(int lvl) {
        return  5 * tier +                      //base
                tier*lvl;                       //level scaling
    }

    public int STRReq(int lvl) {
        int req = STRReq(tier, lvl) - 1; // 같은 티어 무기보다 1 낮게
        if (masteryPotionBonus) req -= 2;
        return req;
    }

    //use the parent item if this has been thrown from a parent
    public int buffedLvl(){
        if (parent != null) {
            return parent.buffedLvl();
        } else {
            return super.buffedLvl();
        }
    }

    public Item upgrade( boolean enchant ) {
        if (!bundleRestoring) {
            durability = MAX_DURABILITY;
            extraThrownLeft = false;
            quantity = defaultQuantity();
            Buff.affect(Dungeon.hero, UpgradedSetTracker.class).levelThresholds.put(setID, trueLevel()+1);
        }
        //thrown weapons don't get curse weakened
        boolean wasCursed = cursed;
        super.upgrade( enchant );
        if (wasCursed && hasCurseEnchant()){
            cursed = wasCursed;
        }
        return this;
    }

    @Override
    public Item upgrade() {
        if (!bundleRestoring) {
            durability = MAX_DURABILITY;
            extraThrownLeft = false;
            quantity = defaultQuantity();
            Buff.affect(Dungeon.hero, UpgradedSetTracker.class).levelThresholds.put(setID, trueLevel()+1);
        }
        return super.upgrade();
    }

    @Override
    public ArrayList<String> actions( Hero hero ) {
        ArrayList<String> actions = super.actions( hero );
        actions.remove( AC_EQUIP );
        return actions;
    }

    @Override
    public boolean collect(Bag container) {
        if (container instanceof MagicalHolster) holster = true;
        return super.collect(container);
    }

    public boolean isSimilar( Item item ) {
        return trueLevel() == item.trueLevel() && getClass() == item.getClass() && setID == (((Codex) item).setID);
    }

    @Override
    public int throwPos(Hero user, int dst) {

        int projecting = 0;
        if (hasEnchant(Projecting.class, user)){
            projecting += 4;
        }

        if (projecting > 0
                && (Dungeon.level.passable[dst] || Dungeon.level.avoid[dst] || Actor.findChar(dst) != null)
                && Dungeon.level.distance(user.pos, dst) <= Math.round(projecting * Enchantment.genericProcChanceMultiplier(user))){
            return dst;
        } else {
            return super.throwPos(user, dst);
        }
    }

    @Override
    public float accuracyFactor(Char owner, Char target) {
        float accFactor = super.accuracyFactor(owner, target);

        accFactor *= adjacentAccFactor(owner, target);

        return accFactor;
    }

    protected float adjacentAccFactor(Char owner, Char target){
        if (Dungeon.level.adjacent( owner.pos, target.pos )) {
            return 0.5f;
        } else {
            return 1.5f;
        }
    }

    @Override
    public void doThrow(Hero hero) {
        parent = null; //reset parent before throwing, just in case
        if (((levelKnown && level() > 0) || hasGoodEnchant() || masteryPotionBonus || enchantHardened)
                && !extraThrownLeft && quantity() == 1 && durabilityLeft() <= durabilityPerUse()){
            GameScene.show(new WndOptions(new ItemSprite(this), Messages.titleCase(title()),
                    Messages.get(Codex.class, "break_upgraded_warn_desc"),
                    Messages.get(Codex.class, "break_upgraded_warn_yes"),
                    Messages.get(Codex.class, "break_upgraded_warn_no")){
                @Override
                protected void onSelect(int index) {
                    if (index == 0){
                        Codex.super.doThrow(hero);
                    } else {
                        QuickSlotButton.cancel();
                        InventoryPane.cancelTargeting();
                    }
                }

                @Override
                public void onBackPressed() {
                    super.onBackPressed();
                    QuickSlotButton.cancel();
                    InventoryPane.cancelTargeting();
                }
            });

        } else {
            super.doThrow(hero);
        }
    }

    @Override
    public int proc(Char attacker, Char defender, int MaGicDamage) {if ((cursed || hasCurseEnchant()) && !cursedKnown){
        GLog.n(Messages.get(this, "curse_discover"));
    }
        cursedKnown = true;
        if (parent != null) parent.cursedKnown = true;

        int result = super.proc(attacker, defender, MaGicDamage);

        //handle ID progress over parent/child
        if (parent != null && parent.usesLeftToID > usesLeftToID){
            float diff = parent.usesLeftToID - usesLeftToID;
            parent.usesLeftToID -= diff;
            parent.availableUsesToID -= diff;
            if (usesLeftToID <= 0) {
                if (ShardOfOblivion.passiveIDDisabled()){
                    parent.setIDReady();
                } else {
                    parent.identify();
                }
            }
        }

        return result;
    }

    @Override
    public Item virtual() {
        Item item = super.virtual();

        ((Codex)item).setID = setID;

        return item;
    }

    public int defaultQuantity(){
        return 3;
    }

    //mainly used to track warnings relating to throwing the last upgraded one, not super accurate
    public boolean extraThrownLeft = false;

    @Override
    public Item random() {
        //+0: 75% (3/4)
        //+1: 20% (4/20)
        //+2: 5%  (1/20)
        int n = 0;
        if (Random.Int(4) == 0) {
            n++;
            if (Random.Int(5) == 0) {
                n++;
            }
        }
        level(n);

        //we use a separate RNG here so that variance due to things like parchment scrap
        //does not affect levelgen
        Random.pushGenerator(Random.Long());

        //30% chance to be cursed
        //10% chance to be enchanted
        float effectRoll = Random.Float();
        if (effectRoll < 0.3f * ParchmentScrap.curseChanceMultiplier()) {
            enchant(Enchantment.randomCurse());
            cursed = true;
        } else if (effectRoll >= 1f - (0.1f * ParchmentScrap.enchantChanceMultiplier())){
            enchant();
        }

        Random.popGenerator();

        return this;
    }

    public String status() {
        //show quantity even when it is 1
        return Integer.toString( quantity );
    }

    @Override
    public float castDelay(Char user, int cell) {
        if (Actor.findChar(cell) != null && Actor.findChar(cell) != user){
            return delayFactor( user );
        } else {
            return super.castDelay(user, cell);
        }
    }

    public float durabilityLeft(){
        return durability;
    }

    public void repair( float amount ){
        durability += amount;
        durability = Math.min(durability, MAX_DURABILITY);
    }

    public void damage(float amount){
        durability -= amount;
        durability = Math.max(durability, 1); //cannot break from doing this
    }

    public final float durabilityPerUse(){
        return durabilityPerUse(level());
    }

    //classes that add steps onto durabilityPerUse can turn rounding off, to do their own rounding after more logic
    protected boolean useRoundingInDurabilityCalc = true;

    public float durabilityPerUse( int level ){
        float usages = baseUses * (float)(Math.pow(1.5f, level));

        if (holster) {
            usages *= MagicalHolster.HOLSTER_DURABILITY_FACTOR;
        }

        //+50% durability on speed aug, -33% durability on magicDamage aug
        usages /= augment.delayFactor(1f);

        if (Dungeon.hero != null) {
            usages *= RingOfSharpshooting.durabilityMultiplier(Dungeon.hero);
        }
        //at 100 uses, items just last forever.
        if (usages >= 100f) return 0;

        if (useRoundingInDurabilityCalc){
            usages = Math.round(usages);
            return (MAX_DURABILITY / usages) + 0.001f; // 1/3 같은 반올림 오차 보정
        } else {
            return MAX_DURABILITY / usages;
        }
    }

    protected void decrementDurability(){
        //if this weapon was thrown from a source stack, degrade that stack.
        //unless a weapon is about to break, then break the one being thrown
        if (parent != null){
            if (parent.durability <= parent.durabilityPerUse()){
                durability = 0;
                parent.durability = MAX_DURABILITY;
                parent.extraThrownLeft = false;
                if (parent.durabilityPerUse() < 100f) {
                    GLog.n(Messages.get(this, "has_broken"));
                }
            } else {
                parent.durability -= parent.durabilityPerUse();
                if (parent.durability > 0 && parent.durability <= parent.durabilityPerUse()){
                    GLog.w(Messages.get(this, "about_to_break"));
                }
            }
            parent = null;
        } else {
            durability -= durabilityPerUse();
            if (durability > 0 && durability <= durabilityPerUse()){
                GLog.w(Messages.get(this, "about_to_break"));
            } else if (durabilityPerUse() < 100f && durability <= 0){
                GLog.n(Messages.get(this, "has_broken"));
            }
        }
    }

    @Override
    public int damageRoll(Char owner) {
        int MaGicDamage = augment.damageFactor(super.damageRoll( owner ));

        if (owner instanceof Hero) {
            int exStr = ((Hero)owner).STR() - STRReq();
            if (exStr > 0) {
                MaGicDamage += Hero.heroDamageIntRange( 0, exStr );
            }
        }

        return MaGicDamage;
    }

    @Override
    public void reset() {
        super.reset();
        durability = MAX_DURABILITY;
    }

    @Override
    public Item merge(Item other) {
        super.merge(other);
        if (isSimilar(other)) {
            extraThrownLeft = false;

            durability += ((Codex)other).durability;
            durability -= MAX_DURABILITY;
            while (durability <= 0){
                quantity -= 1;
                durability += MAX_DURABILITY;
            }

            //hashcode check is for pre-3.2 saves, 0 check is for darts
            if (quantity > defaultQuantity() && setID != 0 && setID != getClass().getSimpleName().hashCode()){
                quantity = defaultQuantity();
                durability = MAX_DURABILITY;
            }

            masteryPotionBonus = masteryPotionBonus || ((Codex) other).masteryPotionBonus;
            levelKnown = levelKnown || other.levelKnown;
            cursedKnown = cursedKnown || other.cursedKnown;
            enchantHardened = enchantHardened || ((Codex) other).enchantHardened;

            //if other has a curse/enchant status that's a higher priority, copy it. in the following order:
            //curse infused
            if (!curseInfusionBonus && ((Codex) other).curseInfusionBonus && ((Codex) other).hasCurseEnchant()){
                enchantment = ((Codex) other).enchantment;
                curseInfusionBonus = true;
                cursed = cursed || other.cursed;
                //enchanted
            } else if (!curseInfusionBonus && !hasGoodEnchant() && ((Codex) other).hasGoodEnchant()){
                enchantment = ((Codex) other).enchantment;
                cursed = other.cursed;
                //nothing
            } else if (!curseInfusionBonus && hasCurseEnchant() && !((Codex) other).hasCurseEnchant()){
                enchantment = ((Codex) other).enchantment;
                cursed = other.cursed;
            }
            //cursed (no copy as other cannot have a higher priority status)

            //special case for explosive, as it tracks a variable
            if (((Codex) other).enchantment instanceof Explosive
                    && enchantment instanceof Explosive){
                ((Explosive) enchantment).merge((Explosive) ((Codex) other).enchantment);
            }
        }
        return this;
    }

    @Override
    public Item split(int amount) {
        bundleRestoring = true;
        Item split = super.split(amount);
        bundleRestoring = false;

        //unless the thrown weapon will break, split off a max durability item and
        //have it reduce the durability of the main stack. Cleaner to the player this way
        if (split != null){
            Codex m = (Codex)split;
            m.durability = MAX_DURABILITY;
            m.parent = this;
            extraThrownLeft = m.extraThrownLeft = true;
        }

        return split;
    }

    @Override
    public boolean doPickUp(Hero hero, int pos) {
        parent = null;
        if (!UpgradedSetTracker.pickupValid(hero, this)){
            Sample.INSTANCE.play( Assets.Sounds.ITEM );
            hero.spendAndNext( TIME_TO_PICK_UP );
            GLog.w(Messages.get(this, "dust"));
            quantity(0);
            return true;
        } else {
            extraThrownLeft = false;
            return super.doPickUp(hero, pos);
        }
    }

    @Override
    public boolean isIdentified() {
        return levelKnown && cursedKnown;
    }

    @Override
    public String info() {

        String info = super.info();

        if (levelKnown) {
            info += "\n\n" + Messages.get(Codex.class, "stats_known", tier, augment.damageFactor(min()), augment.damageFactor(max()), STRReq());
            if (Dungeon.hero != null) {
                if (STRReq() > Dungeon.hero.STR()) {
                    info += " " + Messages.get(Weapon.class, "too_heavy");
                } else if (Dungeon.hero.STR() > STRReq()) {
                    info += " " + Messages.get(Weapon.class, "excess_str", Dungeon.hero.STR() - STRReq());
                }
            }
        } else {
            info += "\n\n" + Messages.get(Codex.class, "stats_unknown", tier, min(0), max(0), STRReq(0));
            if (Dungeon.hero != null && STRReq(0) > Dungeon.hero.STR()) {
                info += " " + Messages.get(Codex.class, "probably_too_heavy");
            }
        }

        if (enchantment != null && (cursedKnown || !enchantment.curse())){
            info += "\n\n" + Messages.get(Weapon.class, "enchanted", enchantment.name());
            if (enchantHardened) info += " " + Messages.get(Weapon.class, "enchant_hardened");
            info += " " + enchantment.desc();
        } else if (enchantHardened){
            info += "\n\n" + Messages.get(Weapon.class, "hardened_no_enchant");
        }

        if (cursedKnown && cursed) {
            info += "\n\n" + Messages.get(Weapon.class, "cursed");
        } else if (!isIdentified() && cursedKnown){
            info += "\n\n" + Messages.get(Weapon.class, "not_cursed");
        }

        info += "\n\n";
        String statsInfo = Messages.get(this, "stats_desc");
        if (!statsInfo.equals("")) info += statsInfo + " ";
        info += Messages.get(Codex.class, "distance");

        switch (augment) {
            case SPEED:
                info += " " + Messages.get(Weapon.class, "faster");
                break;
            case DAMAGE:
                info += " " + Messages.get(Weapon.class, "stronger");
                break;
            case NONE:
        }

        if (levelKnown) {
            if (durabilityPerUse() > 0) {
                info += "\n\n" + Messages.get(this, "uses_left",
                        (int) Math.ceil(durability / durabilityPerUse()),
                        (int) Math.ceil(MAX_DURABILITY / durabilityPerUse()));
            } else {
                info += "\n\n" + Messages.get(this, "unlimited_uses");
            }
        }  else {
            if (durabilityPerUse(0) > 0) {
                info += "\n\n" + Messages.get(this, "unknown_uses", (int) Math.ceil(MAX_DURABILITY / durabilityPerUse(0)));
            } else {
                info += "\n\n" + Messages.get(this, "unlimited_uses");
            }
        }

        return info;
    }

    @Override
    public int value() {
        int price = 5 * tier * quantity;
        if (hasGoodEnchant()) {
            price *= 1.5;
        }
        if (cursedKnown && (cursed || hasCurseEnchant())) {
            price /= 2;
        }
        if (levelKnown && level() > 0) {
            price *= (level() + 1);
        }
        if (price < 1) {
            price = 1;
        }
        return price;
    }

    private static final String SET_ID = "set_id";

    private static final String SPAWNED = "spawned";
    private static final String DURABILITY = "durability";
    private static final String EXTRA_LEFT = "extra_left";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(SET_ID, setID);
        bundle.put(SPAWNED, spawnedForEffect);
        bundle.put(DURABILITY, durability);
        bundle.put(EXTRA_LEFT, extraThrownLeft);
    }

    private static boolean bundleRestoring = false;

    @Override
    public void restoreFromBundle(Bundle bundle) {
        bundleRestoring = true;
        super.restoreFromBundle(bundle);
        bundleRestoring = false;

        if (bundle.contains(SET_ID)){
            setID = bundle.getLong(SET_ID);
            //pre v3.2.0 logic
        } else {
            if (level() > 0){
                quantity = defaultQuantity();

            }
        }

        spawnedForEffect = bundle.getBoolean(SPAWNED);
        durability = bundle.getFloat(DURABILITY);
        extraThrownLeft = bundle.getBoolean(EXTRA_LEFT);
    }

    public static class GrimoireAria extends Codex {

        {
            image = ItemSpriteSheet.GRIMOIRE_ARIA;
        }

        @Override
        public boolean isSimilar(Item item) {
            //yes, even though it uses a dart outline
            return item instanceof Codex;
        }

        @Override
        public String status() {
            return null;
        }

        @Override
        public String info() {
            return "";
        }
    }

    //also used by liquid metal crafting to track when a set is consumed
    public static class UpgradedSetTracker extends Buff {

        public HashMap<Long, Integer> levelThresholds = new HashMap<>();

        public static boolean pickupValid(Hero h, Codex w){
            if (h.buff(UpgradedSetTracker.class) != null){
                HashMap<Long, Integer> levelThresholds = h.buff(UpgradedSetTracker.class).levelThresholds;
                if (levelThresholds.containsKey(w.setID)){
                    return w.trueLevel() >= levelThresholds.get(w.setID);
                }
                return true;
            }
            return true;
        }

        public static final String SET_IDS = "set_ids";
        public static final String SET_LEVELS = "set_levels";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            long[] IDs = new long[levelThresholds.size()];
            int[] levels = new int[levelThresholds.size()];
            int i = 0;
            for (Long ID : levelThresholds.keySet()){
                IDs[i] = ID;
                levels[i] = levelThresholds.get(ID);
                i++;
            }
            bundle.put(SET_IDS, IDs);
            bundle.put(SET_LEVELS, levels);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            long[] IDs = bundle.getLongArray(SET_IDS);
            int[] levels = bundle.getIntArray(SET_LEVELS);
            levelThresholds.clear();
            for (int i = 0; i <IDs.length; i++){
                levelThresholds.put(IDs[i], levels[i]);
            }
        }
    }
}