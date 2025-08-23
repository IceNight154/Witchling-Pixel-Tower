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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.grimoire;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSharpshooting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;


import java.util.ArrayList;

import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

public class GrimoireAria extends Weapon {

	// --- Scroll storage pocket (acts like a tiny bag that holds up to 5 scrolls) ---
	private final ScrollPocket scrollPocket = new ScrollPocket();



	public static final String AC_SHOOT		= "SHOOT";
	public static final String AC_STORE_ONE = "STORE_SCROLL";
	public static final String AC_STORE_ALL = "STORE_ALL_SCROLLS";

	{
		image = ItemSpriteSheet.GRIMOIRE_ARIA;

		defaultAction = AC_SHOOT;
		usesTargeting = true;

		unique = true;
		bones = false;
	}

	public boolean sniperSpecial = false;
	public float sniperSpecialBonusDamage = 0f;

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> a = super.actions(hero);
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_EQUIP);
		actions.add(AC_SHOOT);
		actions.add(Bag.AC_OPEN);
		return actions;
	}

	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(Bag.AC_OPEN)) {
			scrollPocket.owner = hero;
			scrollPocket.execute(hero, Bag.AC_OPEN);
			return;

		} else if (action.equals(AC_SHOOT)) {
			curUser = hero;
			curItem = this;
			GameScene.selectCell(shooter);

		} else if (action.equals(AC_STORE_ONE)) {
			scrollPocket.owner = hero;
			GameScene.selectItem(new WndBag.ItemSelector() {
				@Override
				public String textPrompt() {
					return Messages.get(GrimoireAria.class, "store_prompt");
				}
				@Override
				public boolean itemSelectable(Item item) {
					return item instanceof Scroll;
				}
				@Override
				public void onSelect(Item item) {
					if (item == null) return;
					if (!(item instanceof Scroll)) {
						GLog.w(Messages.get(GrimoireAria.class, "not_scroll"));
						return;
					}
					if (scrollPocket.items.size() >= scrollPocket.capacity()) {
						GLog.w(Messages.get(GrimoireAria.class, "pocket_full"));
						return;
					}
					Bag backpack = hero.belongings.backpack;
					Item detached = item.detach(backpack);
					if (detached != null) {
						if (detached.collect(scrollPocket)) {
							GLog.i(Messages.get(GrimoireAria.class, "stored_one"), item.name());
						} else {
							detached.collect(backpack);
							GLog.w(Messages.get(GrimoireAria.class, "store_failed"));
						}
					}
				}
			});

		} else if (action.equals(AC_STORE_ALL)) {
			scrollPocket.owner = hero;
			Bag backpack = hero.belongings.backpack;
			int moved = 0;
			java.util.ArrayList<Item> snapshot = new java.util.ArrayList<Item>(backpack.items);
			for (Item it : snapshot) {
				if (it instanceof Scroll) {
					if (scrollPocket.items.size() >= scrollPocket.capacity()) break;
					Item detached = it.detach(backpack);
					if (detached != null && detached.collect(scrollPocket)) {
						moved++;
					} else if (detached != null) {
						detached.collect(backpack);
					}
				}
			}
			if (moved > 0) {
				GLog.i(Messages.get(GrimoireAria.class, "stored_all"), moved);
			} else {
				GLog.w(Messages.get(GrimoireAria.class, "no_scrolls"));
			}
		}
	}


	@Override
	public String info() {
		String info = super.info();

		info += "\n\n" + Messages.get( GrimoireAria.class, "stats",
				Math.round(augment.damageFactor(min())),
				Math.round(augment.damageFactor(max())),
				STRReq());

		if (STRReq() > Dungeon.hero.STR()) {
			info += " " + Messages.get(Weapon.class, "too_heavy");
		} else if (Dungeon.hero.STR() > STRReq()){
			info += " " + Messages.get(Weapon.class, "excess_str", Dungeon.hero.STR() - STRReq());
		}

		switch (augment) {
			case SPEED:
				info += "\n\n" + Messages.get(Weapon.class, "faster");
				break;
			case DAMAGE:
				info += "\n\n" + Messages.get(Weapon.class, "stronger");
				break;
			case NONE:
		}

		if (enchantment != null && (cursedKnown || !enchantment.curse())){
			info += "\n\n" + Messages.capitalize(Messages.get(Weapon.class, "enchanted", enchantment.name()));
			if (enchantHardened) info += " " + Messages.get(Weapon.class, "enchant_hardened");
			info += " " + enchantment.desc();
		} else if (enchantHardened){
			info += "\n\n" + Messages.get(Weapon.class, "hardened_no_enchant");
		}

		if (cursed && isEquipped( Dungeon.hero )) {
			info += "\n\n" + Messages.get(Weapon.class, "cursed_worn");
		} else if (cursedKnown && cursed) {
			info += "\n\n" + Messages.get(Weapon.class, "cursed");
		} else if (!isIdentified() && cursedKnown){
			info += "\n\n" + Messages.get(Weapon.class, "not_cursed");
		}

		info += "\n\n" + Messages.get(MissileWeapon.class, "distance");

		return info;
	}

	public static class MagicDamage {};

	@Override
	public int STRReq(int lvl) {
		return STRReq(1, lvl); //tier 1
	}

	@Override
	public int min(int lvl) {
		int dmg = 1 + Dungeon.hero.lvl/5
				+ RingOfSharpshooting.levelDamageBonus(Dungeon.hero)
				+ (curseInfusionBonus ? 1 + Dungeon.hero.lvl/30 : 0);
		return Math.max(0, dmg);
	}

	@Override
	public int max(int lvl) {
		int dmg = 6 + (int)(Dungeon.hero.lvl/2.5f)
				+ 2*RingOfSharpshooting.levelDamageBonus(Dungeon.hero)
				+ (curseInfusionBonus ? 2 + Dungeon.hero.lvl/15 : 0);
		return Math.max(0, dmg);
	}

	@Override
	public int targetingPos(Hero user, int dst) {
		return knockArrow().targetingPos(user, dst);
	}

	private int targetPos;

	@Override
	public int damageRoll(Char owner) {
		int damage = augment.damageFactor(super.damageRoll(owner));

		if (owner instanceof Hero) {
			int exStr = ((Hero)owner).STR() - STRReq();
			if (exStr > 0) {
				damage += Hero.heroDamageIntRange( 0, exStr );
			}
		}

		if (sniperSpecial){
			damage = Math.round(damage * (1f + sniperSpecialBonusDamage));

			switch (augment){
				case NONE:
					damage = Math.round(damage * 0.667f);
					break;
				case SPEED:
					damage = Math.round(damage * 0.5f);
					break;
				case DAMAGE:
					//as distance increases so does damage, capping at 3x:
					//1.20x|1.35x|1.52x|1.71x|1.92x|2.16x|2.43x|2.74x|3.00x
					int distance = Dungeon.level.distance(owner.pos, targetPos) - 1;
					float multiplier = Math.min(3f, 1.2f * (float)Math.pow(1.125f, distance));
					damage = Math.round(damage * multiplier);
					break;
			}
		}

		return damage;
	}

	@Override
	protected float baseDelay(Char owner) {
		if (sniperSpecial){
			switch (augment){
				case NONE: default:
					return 0f;
				case SPEED:
					return 1f;
				case DAMAGE:
					return 2f;
			}
		} else{
			return super.baseDelay(owner);
		}
	}

	@Override
	public int level() {
		int level = Dungeon.hero == null ? 0 : Dungeon.hero.lvl/5;
		if (curseInfusionBonus) level += 1 + level/6;
		return level;
	}

	@Override
	public int buffedLvl() {
		//level isn't affected by buffs/debuffs
		return level();
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	public ManaBall knockArrow(){
		return new ManaBall();
	}

	public class ManaBall extends MissileWeapon {

		{
			image = ItemSpriteSheet.MANA_BALL;

			hitSound = Assets.Sounds.HIT_ARROW;

			setID = 0;
		}

		@Override
		public int defaultQuantity() {
			return 1;
		}

		@Override
		public int damageRoll(Char owner) {
			return GrimoireAria.this.damageRoll(owner);
		}

		@Override
		public boolean hasEnchant(Class<? extends Enchantment> type, Char owner) {
			return GrimoireAria.this.hasEnchant(type, owner);
		}

		@Override
		public int proc(Char attacker, Char defender, int damage) {
			return GrimoireAria.this.proc(attacker, defender, damage);
		}

		@Override
		public float delayFactor(Char user) {
			return GrimoireAria.this.delayFactor(user);
		}

		@Override
		public float accuracyFactor(Char owner, Char target) {
			if (sniperSpecial && GrimoireAria.this.augment == Augment.DAMAGE){
				return Float.POSITIVE_INFINITY;
			} else {
				return super.accuracyFactor(owner, target);
			}
		}

		@Override
		public int STRReq(int lvl) {
			return GrimoireAria.this.STRReq();
		}

		@Override
		protected void onThrow( int cell ) {
			Char enemy = Actor.findChar( cell );
			if (enemy == null || enemy == curUser) {
				parent = null;
				Splash.at( cell, 0xCC99FFFF, 1 );
			} else {
				if (!curUser.shoot( enemy, this )) {
					Splash.at(cell, 0xCC99FFFF, 1);
				}
				if (sniperSpecial && GrimoireAria.this.augment != Augment.SPEED) sniperSpecial = false;
			}
		}

		@Override
		public void throwSound() {
			Sample.INSTANCE.play( Assets.Sounds.ATK_GRIMOIRE, 1, Random.Float(0.87f, 1.15f) );
		}

		int flurryCount = -1;
		Actor flurryActor = null;

		@Override
		public void cast(final Hero user, final int dst) {
			final int cell = throwPos( user, dst );
			GrimoireAria.this.targetPos = cell;
			if (sniperSpecial && GrimoireAria.this.augment == Augment.SPEED){
				if (flurryCount == -1) flurryCount = 3;

				final Char enemy = Actor.findChar( cell );

				if (enemy == null){
					if (user.buff(Talent.LethalMomentumTracker.class) != null){
						user.buff(Talent.LethalMomentumTracker.class).detach();
						user.next();
					} else {
						user.spendAndNext(castDelay(user, cell));
					}
					sniperSpecial = false;
					flurryCount = -1;

					if (flurryActor != null){
						flurryActor.next();
						flurryActor = null;
					}
					return;
				}

				QuickSlotButton.target(enemy);

				user.busy();

				throwSound();

				user.sprite.zap(cell);
				((MissileSprite) user.sprite.parent.recycle(MissileSprite.class)).
						reset(user.sprite,
								cell,
								this,
								new Callback() {
									@Override
									public void call() {
										if (enemy.isAlive()) {
											curUser = user;
											onThrow(cell);
										}

										flurryCount--;
										if (flurryCount > 0){
											Actor.add(new Actor() {

												{
													actPriority = VFX_PRIO-1;
												}

												@Override
												protected boolean act() {
													flurryActor = this;
													int target = QuickSlotButton.autoAim(enemy, ManaBall.this);
													if (target == -1) target = cell;
													cast(user, target);
													Actor.remove(this);
													return false;
												}
											});
											curUser.next();
										} else {
											if (user.buff(Talent.LethalMomentumTracker.class) != null){
												user.buff(Talent.LethalMomentumTracker.class).detach();
												user.next();
											} else {
												user.spendAndNext(castDelay(user, cell));
											}
											sniperSpecial = false;
											flurryCount = -1;
										}

										if (flurryActor != null){
											flurryActor.next();
											flurryActor = null;
										}
									}
								});

			} else {
				super.cast(user, dst);
			}
		}
	}

	private CellSelector.Listener shooter = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer target ) {
			if (target != null) {
				knockArrow().cast(curUser, target);
			}
		}
		@Override
		public String prompt() {
			return Messages.get(GrimoireAria.class, "prompt");
		}
	};


	@Override
	public boolean collect(Bag container) {
		boolean result = super.collect(container);
		if (result) {
			// tie the pocket to the same owner and auto-grab scrolls up to capacity
			scrollPocket.owner = container.owner;
			scrollPocket.grabItems(container);
		}
		return result;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		scrollPocket.onDetach();
	}

	private static final String SCROLL_POCKET = "scroll_pocket";
	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		Bundle b = new Bundle();
		scrollPocket.storeInBundle(b);
		bundle.put(SCROLL_POCKET, b);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		Bundle b = bundle.getBundle(SCROLL_POCKET);
		if (b != null) scrollPocket.restoreFromBundle(b);
	}


	// Inner bag class that only holds scrolls and has capacity 5
	private static class ScrollPocket extends Bag {
		@Override
		public int capacity() { return 5; }
		@Override
		public boolean canHold(Item item) { return item instanceof Scroll && super.canHold(item); }
		@Override
		public String name() { return Messages.get(GrimoireAria.class, "pocket_name"); }
		@Override
		public boolean isIdentified() { return true; }
	}

}