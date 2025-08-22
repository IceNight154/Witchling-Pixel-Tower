/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.items.jewels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.ItemStatusHandler;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.exotic.ExoticJewel;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.exotic.JewelOfAntiMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAugmentation;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlast;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfClairvoyance;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDeepSleep;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDetectMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfFear;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfFlock;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfShock;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.AlchemyScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

public abstract class Jewel extends Item {
	
	public static final String AC_READ	= "READ";
	
	protected static final float TIME_TO_READ	= 1f;

	private static final LinkedHashMap<String, Integer> runes = new LinkedHashMap<String, Integer>() {
		{
			put("KAUNAN",ItemSpriteSheet.JEWEL_KAUNAN);
			put("SOWILO",ItemSpriteSheet.JEWEL_SOWILO);
			put("LAGUZ",ItemSpriteSheet.JEWEL_LAGUZ);
			put("YNGVI",ItemSpriteSheet.JEWEL_YNGVI);
			put("GYFU",ItemSpriteSheet.JEWEL_GYFU);
			put("RAIDO",ItemSpriteSheet.JEWEL_RAIDO);
			put("ISAZ",ItemSpriteSheet.JEWEL_ISAZ);
			put("MANNAZ",ItemSpriteSheet.JEWEL_MANNAZ);
			put("NAUDIZ",ItemSpriteSheet.JEWEL_NAUDIZ);
			put("BERKANAN",ItemSpriteSheet.JEWEL_BERKANAN);
			put("ODAL",ItemSpriteSheet.JEWEL_ODAL);
			put("TIWAZ",ItemSpriteSheet.JEWEL_TIWAZ);
		}
	};
	
	protected static ItemStatusHandler<Jewel> handler;
	
	protected String rune;

	//affects how strongly on-jewel talents trigger from this jewel
	protected float talentFactor = 1;
	//the chance (0-1) of whether on-jewel talents trigger from this potion
	protected float talentChance = 1;
	
	{
		stackable = true;
		defaultAction = AC_READ;
	}
	
	@SuppressWarnings("unchecked")
	public static void initLabels() {
		handler = new ItemStatusHandler<>( (Class<? extends Jewel>[])Generator.Category.Jewel.classes, runes );
	}

	public static void clearLabels(){
		handler = null;
	}
	
	public static void save( Bundle bundle ) {
		handler.save( bundle );
	}

	public static void saveSelectively( Bundle bundle, ArrayList<Item> items ) {
		ArrayList<Class<?extends Item>> classes = new ArrayList<>();
		for (Item i : items){
			if (i instanceof ExoticJewel){
				if (!classes.contains(ExoticJewel.exoToReg.get(i.getClass()))){
					classes.add(ExoticJewel.exoToReg.get(i.getClass()));
				}
			} else if (i instanceof Jewel){
				if (!classes.contains(i.getClass())){
					classes.add(i.getClass());
				}
			}
		}
		handler.saveClassesSelectively( bundle, classes );
	}

	@SuppressWarnings("unchecked")
	public static void restore( Bundle bundle ) {
		handler = new ItemStatusHandler<>( (Class<? extends Jewel>[])Generator.Category.Jewel.classes, runes, bundle );
	}
	
	public Jewel() {
		super();
		reset();
	}
	
	//anonymous jewels are always IDed, do not affect ID status,
	//and their sprite is replaced by a placeholder if they are not known,
	//useful for items that appear in UIs, or which are only spawned for their effects
	protected boolean anonymous = false;
	public void anonymize(){
		if (!isKnown()) image = ItemSpriteSheet.JEWEL_HOLDER;
		anonymous = true;
	}
	
	
	@Override
	public void reset(){
		super.reset();
		if (handler != null && handler.contains(this)) {
			image = handler.image(this);
			rune = handler.label(this);
		} else {
			image = ItemSpriteSheet.JEWEL_KAUNAN;
			rune = "KAUNAN";
		}
	}
	
	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_READ );
		return actions;
	}
	
	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals( AC_READ )) {
			
			if (hero.buff(MagicImmune.class) != null){
				GLog.w( Messages.get(this, "no_magic") );
			} else if (hero.buff( Blindness.class ) != null) {
				GLog.w( Messages.get(this, "blinded") );
			} else if (hero.buff(UnstableSpellbook.bookRecharge.class) != null
					&& hero.buff(UnstableSpellbook.bookRecharge.class).isCursed()
					&& !(this instanceof JewelOfRemoveCurse || this instanceof JewelOfAntiMagic)){
				GLog.n( Messages.get(this, "cursed") );
			} else {
				doRead();
			}
			
		}
	}
	
	public abstract void doRead();

	public void readAnimation() {
		//if jewel is being created for its effect, depend on creating item to dispel
		if (!anonymous) Invisibility.dispel();
		curUser.spend( TIME_TO_READ );
		curUser.busy();
		((HeroSprite)curUser.sprite).read();

		if (!anonymous) {
			Catalog.countUse(getClass());
			if (Random.Float() < talentChance) {
				Talent.onJewelUsed(curUser, curUser.pos, talentFactor, getClass());
			}
		}

	}
	
	public boolean isKnown() {
		return anonymous || (handler != null && handler.isKnown( this ));
	}
	
	public void setKnown() {
		if (!anonymous) {
			if (!isKnown()) {
				handler.know(this);
				updateQuickslot();
			}
			
			if (Dungeon.hero.isAlive()) {
				Catalog.setSeen(getClass());
				Statistics.itemTypesDiscovered.add(getClass());
			}
		}
	}
	
	@Override
	public Item identify( boolean byHero ) {
		super.identify(byHero);

		if (!isKnown()) {
			setKnown();
		}
		return this;
	}
	
	@Override
	public String name() {
		return isKnown() ? super.name() : Messages.get(this, rune);
	}

	@Override
	public String info() {
		//skip custom notes if anonymized and un-Ided
		return (anonymous && (handler == null || !handler.isKnown( this ))) ? desc() : super.info();
	}

	@Override
	public String desc() {
		return isKnown() ? super.desc() : Messages.get(this, "unknown_desc");
	}
	
	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	@Override
	public boolean isIdentified() {
		return isKnown();
	}
	
	public static HashSet<Class<? extends Jewel>> getKnown() {
		return handler.known();
	}
	
	public static HashSet<Class<? extends Jewel>> getUnknown() {
		return handler.unknown();
	}
	
	public static boolean allKnown() {
		return handler != null && handler.known().size() == Generator.Category.Jewel.classes.length;
	}
	
	@Override
	public int value() {
		return 30 * quantity;
	}

	@Override
	public int energyVal() {
		return 6 * quantity;
	}
	
	public static class PlaceHolder extends Jewel {
		
		{
			image = ItemSpriteSheet.JEWEL_HOLDER;
		}
		
		@Override
		public boolean isSimilar(Item item) {
			return ExoticJewel.regToExo.containsKey(item.getClass())
					|| ExoticJewel.regToExo.containsValue(item.getClass());
		}
		
		@Override
		public void doRead() {}
		
		@Override
		public String info() {
			return "";
		}
	}
	
	public static class JewelToStone extends Recipe {
		
		private static HashMap<Class<?extends Jewel>, Class<?extends Runestone>> stones = new HashMap<>();
		static {
			stones.put(JewelOfIdentify.class,      StoneOfIntuition.class);
			stones.put(JewelOfLullaby.class,       StoneOfDeepSleep.class);
			stones.put(JewelOfMagicMapping.class,  StoneOfClairvoyance.class);
			stones.put(JewelOfMirrorImage.class,   StoneOfFlock.class);
			stones.put(JewelOfRetribution.class,   StoneOfBlast.class);
			stones.put(JewelOfRage.class,          StoneOfAggression.class);
			stones.put(JewelOfRecharging.class,    StoneOfShock.class);
			stones.put(JewelOfRemoveCurse.class,   StoneOfDetectMagic.class);
			stones.put(JewelOfTeleportation.class, StoneOfBlink.class);
			stones.put(JewelOfTerror.class,        StoneOfFear.class);
			stones.put(JewelOfTransmutation.class, StoneOfAugmentation.class);
			stones.put(JewelOfUpgrade.class,       StoneOfEnchantment.class);
		}
		
		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			if (ingredients.size() != 1
					|| !(ingredients.get(0) instanceof Jewel)
					|| !stones.containsKey(ingredients.get(0).getClass())){
				return false;
			}
			
			return true;
		}
		
		@Override
		public int cost(ArrayList<Item> ingredients) {
			return 0;
		}
		
		@Override
		public Item brew(ArrayList<Item> ingredients) {
			if (!testIngredients(ingredients)) return null;
			
			Jewel s = (Jewel) ingredients.get(0);
			
			s.quantity(s.quantity() - 1);
			if (ShatteredPixelDungeon.scene() instanceof AlchemyScene){
				if (!s.isIdentified()){
					((AlchemyScene) ShatteredPixelDungeon.scene()).showIdentify(s);
				}
			} else {
				s.identify();
			}
			
			return Reflection.newInstance(stones.get(s.getClass())).quantity(2);
		}
		
		@Override
		public Item sampleOutput(ArrayList<Item> ingredients) {
			if (!testIngredients(ingredients)) return null;
			
			Jewel s = (Jewel) ingredients.get(0);

			if (!s.isKnown()){
				return new Runestone.PlaceHolder().quantity(2);
			} else {
				return Reflection.newInstance(stones.get(s.getClass())).quantity(2);
			}
		}
	}
}
