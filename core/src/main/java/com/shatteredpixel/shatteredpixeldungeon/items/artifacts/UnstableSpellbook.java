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

package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ElmoParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.JewelHolder;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.Jewel;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfTerror;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.JewelOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.items.jewels.exotic.ExoticJewel;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public class UnstableSpellbook extends Artifact {

	{
		image = ItemSpriteSheet.ARTIFACT_SPELLBOOK;

		levelCap = 10;

		charge = (int)(level()*0.6f)+2;
		partialCharge = 0;
		chargeCap = (int)(level()*0.6f)+2;

		defaultAction = AC_READ;
	}

	public static final String AC_READ = "READ";
	public static final String AC_ADD = "ADD";

	private final ArrayList<Class> jewels = new ArrayList<>();

	public UnstableSpellbook() {
		super();

		setupJewels();
	}

	private void setupJewels(){
		jewels.clear();

		Class<?>[] jewelClasses = Generator.Category.Jewel.classes;
		float[] probs = Generator.Category.Jewel.defaultProbsTotal.clone(); //array of primitives, clone gives deep copy.
		int i = Random.chances(probs);

		while (i != -1){
			jewels.add(jewelClasses[i]);
			probs[i] = 0;

			i = Random.chances(probs);
		}
		jewels.remove(JewelOfTransmutation.class);
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		if (isEquipped( hero ) && charge > 0 && !cursed && hero.buff(MagicImmune.class) == null) {
			actions.add(AC_READ);
		}
		if (isEquipped( hero ) && level() < levelCap && !cursed && hero.buff(MagicImmune.class) == null) {
			actions.add(AC_ADD);
		}
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (hero.buff(MagicImmune.class) != null) return;

		if (action.equals( AC_READ )) {

			if (hero.buff( Blindness.class ) != null) GLog.w( Messages.get(this, "blinded") );
			else if (!isEquipped( hero ))             GLog.i( Messages.get(Artifact.class, "need_to_equip") );
			else if (charge <= 0)                     GLog.i( Messages.get(this, "no_charge") );
			else if (cursed)                          GLog.i( Messages.get(this, "cursed") );
			else {
				doReadEffect(hero);
			}

		} else if (action.equals( AC_ADD )) {
			GameScene.selectItem(itemSelector);
		}
	}

	public void doReadEffect(Hero hero){
		charge--;

		Jewel jewel;
		do {
			jewel = (Jewel) Generator.randomUsingDefaults(Generator.Category.Jewel);
		} while (jewel == null
				//reduce the frequency of these jewels by half
				||((jewel instanceof JewelOfIdentify ||
				jewel instanceof JewelOfRemoveCurse ||
				jewel instanceof JewelOfMagicMapping) && Random.Int(2) == 0)
				//cannot roll transmutation
				|| (jewel instanceof JewelOfTransmutation));

		jewel.anonymize();
		curItem = jewel;
		curUser = hero;

		//if there are charges left and the jewel has been given to the book
		if (charge > 0 && !jewels.contains(jewel.getClass())) {
			final Jewel fJewel = jewel;

			final ExploitHandler handler = Buff.affect(hero, ExploitHandler.class);
			handler.jewel = jewel;

			GameScene.show(new WndOptions(new ItemSprite(this),
					Messages.get(this, "prompt"),
					Messages.get(this, "read_empowered"),
					jewel.trueName(),
					Messages.get(ExoticJewel.regToExo.get(jewel.getClass()), "name")){
				@Override
				protected void onSelect(int index) {
					handler.detach();
					if (index == 1){
						Jewel jewel = Reflection.newInstance(ExoticJewel.regToExo.get(fJewel.getClass()));
						curItem = jewel;
						charge--;
						jewel.anonymize();
						checkForArtifactProc(curUser, jewel);
						jewel.doRead();
						Invisibility.dispel();
						Talent.onArtifactUsed(Dungeon.hero);
					} else {
						checkForArtifactProc(curUser, fJewel);
						fJewel.doRead();
						Invisibility.dispel();
						Talent.onArtifactUsed(Dungeon.hero);
					}
					updateQuickslot();
				}

				@Override
				public void onBackPressed() {
					//do nothing
				}
			});
		} else {
			checkForArtifactProc(curUser, jewel);
			jewel.doRead();
			Invisibility.dispel();
			Talent.onArtifactUsed(Dungeon.hero);
		}

		updateQuickslot();
	}

	private void checkForArtifactProc(Hero user, Jewel jewel){
		//if the base jewel (exotics all match) is an AOE effect, then also trigger illuminate
		if (jewel instanceof JewelOfLullaby
				|| jewel instanceof JewelOfRemoveCurse || jewel instanceof JewelOfTerror) {
			for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )) {
				if (Dungeon.level.heroFOV[mob.pos]) {
					artifactProc(mob, visiblyUpgraded(), 1);
				}
			}
		//except rage, which affects everything even if it isn't visible
		} else if (jewel instanceof JewelOfRage){
			for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )) {
				artifactProc(mob, visiblyUpgraded(), 1);
			}
		}
	}

	//forces the reading of a regular jewel if the player tried to exploit by quitting the game when the menu was up
	public static class ExploitHandler extends Buff {
		{ actPriority = VFX_PRIO; }

		public Jewel jewel;

		@Override
		public boolean act() {
			curUser = Dungeon.hero;
			curItem = jewel;
			jewel.anonymize();
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					jewel.doRead();
					Invisibility.dispel();
					Item.updateQuickslot();
				}
			});
			detach();
			return true;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put( "jewel", jewel );
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			jewel = (Jewel)bundle.get("jewel");
		}
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new bookRecharge();
	}
	
	@Override
	public void charge(Hero target, float amount) {
		if (charge < chargeCap && !cursed && target.buff(MagicImmune.class) == null){
			partialCharge += 0.1f*amount;
			while (partialCharge >= 1){
				partialCharge--;
				charge++;
			}
			if (charge >= chargeCap){
				partialCharge = 0;
			}
			updateQuickslot();
		}
	}

	@Override
	public Item upgrade() {
		chargeCap = (int)((level()+1)*0.6f)+2;

		//for artifact transmutation.
		while (!jewels.isEmpty() && jewels.size() > (levelCap-1-level())) {
			jewels.remove(0);
		}

		return super.upgrade();
	}

	@Override
	public void resetForTrinity(int visibleLevel) {
		super.resetForTrinity(visibleLevel);
		setupJewels();
		while (!jewels.isEmpty() && jewels.size() > (levelCap-1-level())) {
			jewels.remove(0);
		}
	}

	@Override
	public String desc() {
		String desc = super.desc();

		if (isEquipped(Dungeon.hero)) {
			if (cursed) {
				desc += "\n\n" + Messages.get(this, "desc_cursed");
			}
			
			if (level() < levelCap && jewels.size() > 0) {
				desc += "\n\n" + Messages.get(this, "desc_index");
				desc += "\n" + "_" + Messages.get(jewels.get(0), "name") + "_";
				if (jewels.size() > 1)
					desc += "\n" + "_" + Messages.get(jewels.get(1), "name") + "_";
			}
		}
		
		if (level() > 0) {
			desc += "\n\n" + Messages.get(this, "desc_empowered");
		}

		return desc;
	}

	private static final String JEWELS =   "jewels";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put( JEWELS, jewels.toArray(new Class[jewels.size()]) );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		jewels.clear();
		if (bundle.contains(JEWELS) && bundle.getClassArray(JEWELS) != null) {
			for (Class<?> jewel : bundle.getClassArray(JEWELS)) {
				if (jewel != null) jewels.add(jewel);
			}
		}
	}

	public class bookRecharge extends ArtifactBuff{
		@Override
		public boolean act() {
			if (charge < chargeCap
					&& !cursed
					&& target.buff(MagicImmune.class) == null
					&& Regeneration.regenOn()) {
				//120 turns to charge at full, 80 turns to charge at 0/8
				float chargeGain = 1 / (120f - (chargeCap - charge)*5f);
				chargeGain *= RingOfEnergy.artifactChargeMultiplier(target);
				partialCharge += chargeGain;

				while (partialCharge >= 1) {
					partialCharge --;
					charge ++;

					if (charge == chargeCap){
						partialCharge = 0;
					}
				}
			}

			updateQuickslot();

			spend( TICK );

			return true;
		}
	}

	protected WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return Messages.get(UnstableSpellbook.class, "prompt");
		}

		@Override
		public Class<?extends Bag> preferredBag(){
			return JewelHolder.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof Jewel && item.isIdentified() && jewels.contains(item.getClass());
		}

		@Override
		public void onSelect(Item item) {
			if (item != null && item instanceof Jewel && item.isIdentified()){
				Hero hero = Dungeon.hero;
				for (int i = 0; ( i <= 1 && i < jewels.size() ); i++){
					if (jewels.get(i).equals(item.getClass())){
						hero.sprite.operate( hero.pos );
						hero.busy();
						hero.spend( 2f );
						Sample.INSTANCE.play(Assets.Sounds.BURNING);
						hero.sprite.emitter().burst( ElmoParticle.FACTORY, 12 );

						jewels.remove(i);
						item.detach(hero.belongings.backpack);

						upgrade();
						Catalog.countUse(UnstableSpellbook.class);
						GLog.i( Messages.get(UnstableSpellbook.class, "infuse_jewel") );
						return;
					}
				}
				GLog.w( Messages.get(UnstableSpellbook.class, "unable_jewel") );
			} else if (item instanceof Jewel && !item.isIdentified()) {
				GLog.w( Messages.get(UnstableSpellbook.class, "unknown_jewel") );
			}
		}
	};
}
