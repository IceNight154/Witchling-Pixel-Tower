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

package com.shatteredpixel.shatteredpixeldungeon.items.spells;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.Jewel;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfMirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfRetribution;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfTerror;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.exotic.ExoticJewel;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class UnstableSpell extends Spell {

	{
		image = ItemSpriteSheet.UNSTABLE_SPELL;
	}
	
	private static HashMap<Class<? extends Jewel>, Float> jewelChances = new HashMap<>();
	static{
		jewelChances.put( JewelOfIdentify.class,      3f );
		jewelChances.put( JewelOfRemoveCurse.class,   2f );
		jewelChances.put( JewelOfMagicMapping.class,  2f );
		jewelChances.put( JewelOfMirrorImage.class,   2f );
		jewelChances.put( JewelOfRecharging.class,    2f );
		jewelChances.put( JewelOfLullaby.class,       2f );
		jewelChances.put( JewelOfRetribution.class,   2f );
		jewelChances.put( JewelOfRage.class,          2f );
		jewelChances.put( JewelOfTeleportation.class, 2f );
		jewelChances.put( JewelOfTerror.class,        2f );
		jewelChances.put( JewelOfTransmutation.class, 1f );
	}

	private static HashSet<Class<? extends Jewel>> nonCombatJewels = new HashSet<>();
	static {
		nonCombatJewels.add( JewelOfIdentify.class );
		nonCombatJewels.add( JewelOfRemoveCurse.class );
		nonCombatJewels.add( JewelOfMagicMapping.class );
		nonCombatJewels.add( JewelOfRecharging.class );
		nonCombatJewels.add( JewelOfLullaby.class );
		nonCombatJewels.add( JewelOfTeleportation.class );
		nonCombatJewels.add( JewelOfTransmutation.class );
	}

	private static HashSet<Class<? extends Jewel>> combatJewels = new HashSet<>();
	static {
		combatJewels.add( JewelOfMirrorImage.class );
		combatJewels.add( JewelOfRecharging.class );
		combatJewels.add( JewelOfLullaby.class );
		combatJewels.add( JewelOfRetribution.class );
		combatJewels.add( JewelOfRage.class );
		combatJewels.add( JewelOfTeleportation.class );
		combatJewels.add( JewelOfTerror.class );
	}
	
	@Override
	protected void onCast(Hero hero) {
		
		detach( curUser.belongings.backpack );
		updateQuickslot();
		
		Jewel s = Reflection.newInstance(Random.chances(jewelChances));

		//reroll the jewel until it is relevant for the situation (whether there are visible enemies)
		if (hero.visibleEnemies() == 0){
			while (!nonCombatJewels.contains(s.getClass())){
				s = Reflection.newInstance(Random.chances(jewelChances));
			}
		} else {
			while (!combatJewels.contains(s.getClass())){
				s = Reflection.newInstance(Random.chances(jewelChances));
			}
		}

		s.anonymize();
		curItem = s;
		s.doRead();
		Invisibility.dispel();

		Catalog.countUse(getClass());
		if (Random.Float() < talentChance){
			Talent.onJewelUsed(curUser, curUser.pos, talentFactor, getClass());
		}
	}

	//lower values, as it's cheaper to make
	@Override
	public int value() {
		return 40 * quantity;
	}

	@Override
	public int energyVal() {
		return 8 * quantity;
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe {

		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			boolean jewel = false;
			boolean stone = false;

			for (Item i : ingredients){
				if (i instanceof Runestone){
					stone = true;
					//if it is a regular or exotic potion
				} else if (ExoticJewel.regToExo.containsKey(i.getClass())
						|| ExoticJewel.regToExo.containsValue(i.getClass())) {
					jewel = true;
				}
			}

			return jewel && stone;
		}
		
		@Override
		public int cost(ArrayList<Item> ingredients) {
			return 1;
		}

		@Override
		public Item brew(ArrayList<Item> ingredients) {

			for (Item i : ingredients){
				i.quantity(i.quantity()-1);
			}

			return sampleOutput(null);
		}

		@Override
		public Item sampleOutput(ArrayList<Item> ingredients) {
			return new UnstableSpell();
		}
	}
}
