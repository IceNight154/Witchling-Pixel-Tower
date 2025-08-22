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

package com.shatteredpixel.shatteredpixeldungeon.items.jewels.exotic;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
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
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfUpgrade;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public abstract class ExoticJewel extends Jewel {
	
	
	public static final LinkedHashMap<Class<?extends Jewel>, Class<?extends ExoticJewel>> regToExo = new LinkedHashMap<>();
	public static final LinkedHashMap<Class<?extends ExoticJewel>, Class<?extends Jewel>> exoToReg = new LinkedHashMap<>();
	static{
		regToExo.put(JewelOfUpgrade.class, JewelOfEnchantment.class);
		exoToReg.put(JewelOfEnchantment.class, JewelOfUpgrade.class);

		regToExo.put(JewelOfIdentify.class, JewelOfDivination.class);
		exoToReg.put(JewelOfDivination.class, JewelOfIdentify.class);
		
		regToExo.put(JewelOfRemoveCurse.class, JewelOfAntiMagic.class);
		exoToReg.put(JewelOfAntiMagic.class, JewelOfRemoveCurse.class);

		regToExo.put(JewelOfMirrorImage.class, JewelOfPrismaticImage.class);
		exoToReg.put(JewelOfPrismaticImage.class, JewelOfMirrorImage.class);

		regToExo.put(JewelOfRecharging.class, JewelOfMysticalEnergy.class);
		exoToReg.put(JewelOfMysticalEnergy.class, JewelOfRecharging.class);

		regToExo.put(JewelOfTeleportation.class, JewelOfPassage.class);
		exoToReg.put(JewelOfPassage.class, JewelOfTeleportation.class);

		regToExo.put(JewelOfLullaby.class, JewelOfSirensSong.class);
		exoToReg.put(JewelOfSirensSong.class, JewelOfLullaby.class);

		regToExo.put(JewelOfMagicMapping.class, JewelOfForesight.class);
		exoToReg.put(JewelOfForesight.class, JewelOfMagicMapping.class);

		regToExo.put(JewelOfRage.class, JewelOfChallenge.class);
		exoToReg.put(JewelOfChallenge.class, JewelOfRage.class);

		regToExo.put(JewelOfRetribution.class, JewelOfPsionicBlast.class);
		exoToReg.put(JewelOfPsionicBlast.class, JewelOfRetribution.class);
		
		regToExo.put(JewelOfTerror.class, JewelOfDread.class);
		exoToReg.put(JewelOfDread.class, JewelOfTerror.class);
		
		regToExo.put(JewelOfTransmutation.class, JewelOfMetamorphosis.class);
		exoToReg.put(JewelOfMetamorphosis.class, JewelOfTransmutation.class);
	}
	
	@Override
	public boolean isKnown() {
		return anonymous || (handler != null && handler.isKnown( exoToReg.get(this.getClass()) ));
	}
	
	@Override
	public void setKnown() {
		if (!isKnown()) {
			handler.know(exoToReg.get(this.getClass()));
			updateQuickslot();
		}
	}
	
	@Override
	public void reset() {
		super.reset();
		if (handler != null && handler.contains(exoToReg.get(this.getClass()))) {
			image = handler.image(exoToReg.get(this.getClass())) + 32;
			rune = handler.label(exoToReg.get(this.getClass()));
		}
	}
	
	@Override
	//20 gold more than its none-exotic equivalent
	public int value() {
		return (Reflection.newInstance(exoToReg.get(getClass())).value() + 30) * quantity;
	}

	@Override
	//6 more energy than its none-exotic equivalent
	public int energyVal() {
		return (Reflection.newInstance(exoToReg.get(getClass())).energyVal() + 6) * quantity;
	}
	
	public static class JewelToExotic extends Recipe {
		
		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			if (ingredients.size() == 1 && regToExo.containsKey(ingredients.get(0).getClass())){
				return true;
			}

			return false;
		}
		
		@Override
		public int cost(ArrayList<Item> ingredients) {
			return 6;
		}
		
		@Override
		public Item brew(ArrayList<Item> ingredients) {
			for (Item i : ingredients){
				i.quantity(i.quantity()-1);
			}

			return Reflection.newInstance(regToExo.get(ingredients.get(0).getClass()));
		}
		
		@Override
		public Item sampleOutput(ArrayList<Item> ingredients) {
			return Reflection.newInstance(regToExo.get(ingredients.get(0).getClass()));
		}
	}
}
