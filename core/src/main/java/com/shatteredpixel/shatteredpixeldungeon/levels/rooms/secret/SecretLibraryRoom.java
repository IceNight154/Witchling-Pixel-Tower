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

package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret;

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
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ExoticCrystals;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.HashMap;

public class SecretLibraryRoom extends SecretRoom {
	
	@Override
	public int minWidth() {
		return Math.max(7, super.minWidth());
	}
	
	@Override
	public int minHeight() {
		return Math.max(7, super.minHeight());
	}
	
	private static HashMap<Class<? extends Jewel>, Float> jewelChances = new HashMap<>();
	static{
		jewelChances.put( JewelOfIdentify.class,      1f );
		jewelChances.put( JewelOfRemoveCurse.class,   2f );
		jewelChances.put( JewelOfMirrorImage.class,   3f );
		jewelChances.put( JewelOfRecharging.class,    3f );
		jewelChances.put( JewelOfTeleportation.class, 3f );
		jewelChances.put( JewelOfLullaby.class,       4f );
		jewelChances.put( JewelOfMagicMapping.class,  4f );
		jewelChances.put( JewelOfRage.class,          4f );
		jewelChances.put( JewelOfRetribution.class,   4f );
		jewelChances.put( JewelOfTerror.class,        4f );
		jewelChances.put( JewelOfTransmutation.class, 6f );
	}
	
	public void paint( Level level ) {
		
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.BOOKSHELF );
		
		Painter.fillEllipse(level, this, 2, Terrain.EMPTY_SP);
		
		Door entrance = entrance();
		if (entrance.x == left || entrance.x == right){
			Painter.drawInside(level, this, entrance, (width() - 3) / 2, Terrain.EMPTY_SP);
		} else {
			Painter.drawInside(level, this, entrance, (height() - 3) / 2, Terrain.EMPTY_SP);
		}
		entrance.set( Door.Type.HIDDEN );
		
		int n = Random.IntRange( 2, 3 );
		HashMap<Class<? extends Jewel>, Float> chances = new HashMap<>(jewelChances);
		for (int i=0; i < n; i++) {
			int pos;
			do {
				pos = level.pointToCell(random());
			} while (level.map[pos] != Terrain.EMPTY_SP || level.heaps.get( pos ) != null);
			
			Class<?extends Jewel> jewelCls = Random.chances(chances);
			chances.put(jewelCls, 0f);

			if (ExoticJewel.regToExo.containsKey(jewelCls)){
				if (Random.Float() < ExoticCrystals.consumableExoticChance()){
					jewelCls = ExoticJewel.regToExo.get(jewelCls);
				}
			}

			level.drop( Reflection.newInstance(jewelCls), pos );
		}
	}
	
}
