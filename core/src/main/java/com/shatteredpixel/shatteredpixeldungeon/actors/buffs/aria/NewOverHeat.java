package com.shatteredpixel.shatteredpixeldungeon.actors.buffs.aria;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

public class NewOverHeat extends Buff implements ActionIndicator.Action {
    {
        type = buffType.NEUTRAL;

        revivePersists = true;
    }

    public enum ElementType {
        FIRE("fire"),
        WATER("water"),
        EARTH("earth"),
        WIND("wind");

        final String name;

        ElementType(String name) {
            this.name = name;
        }

        public String getName() {
            return Messages.get(NewOverHeat.class, "element_"+name);
        }
    }

    private static final int OVERHEAT_MAX = 100;
    private static final String TXT_STATUS = "%d/%d";

    private int gauge = 0;
    private ElementType element = ElementType.FIRE;

    public ElementType randomElement() {
        ElementType newElement;

        do {
            switch (Random.Int(4)) {
                case 0: default:
                    newElement = ElementType.FIRE;
                    break;
                case 1:
                    newElement = ElementType.WATER;
                    break;
                case 2:
                    newElement = ElementType.EARTH;
                    break;
                case 3:
                    newElement = ElementType.WIND;
                    break;
            }
        } while (newElement == element);

        element = newElement;
        return element;
    }

    public void heat(int amount) {
        gauge = Math.min(OVERHEAT_MAX, gauge + amount);
    }

    public void cool(int amount) {
        gauge = Math.max(0, gauge - amount);
    }

    public int heatLevel() {
        if (gauge == 0) { //0
            return 0;
        } else if (gauge < 25) { //1~24
            return 1;
        } else if (gauge < 50) { //25~49
            return 2;
        } else if (gauge < 75) { //50~74
            return 3;
        } else if (gauge < 100) { //75~99
            return 4;
        } else { //100
            return 5;
        }
    }

    public boolean isMeltDown() {
        return heatLevel() == 5;
    }

    @Override
    public boolean attachTo(Char target) { //버프가 붙을 때 호출
        ActionIndicator.setAction(this);
        return super.attachTo(target);
    }

    @Override
    public void detach() { //버프가 제거될 때 호출, 그러나 여기에서 딱히 쓰일 일은 없음
        ActionIndicator.clearAction();
        super.detach();
    }

    @Override
    public boolean act() {
        spend(TICK);
        gauge = Math.max(0, gauge - 1);
        return true;
    }

    @Override
    public int icon() {
        switch (heatLevel()) {
            case 0: default:
                return BuffIndicator.OVERHEAT_0;
            case 1:
                return BuffIndicator.OVERHEAT_25;
            case 2:
                return BuffIndicator.OVERHEAT_50;
            case 3:
                return BuffIndicator.OVERHEAT_75;
            case 4:
                return BuffIndicator.OVERHEAT_100;
            case 5:
                return BuffIndicator.OVERHEAT_MELTDOWN;
        }
    }

    @Override
    public void tintIcon(Image icon) { //버프 아이콘 색상을 물들이는 메서드. 딱히 쓸 일은 없으니 변경하지 않음
        super.tintIcon(icon);
    }

    @Override
    public String name() {
        if (isMeltDown()) {
            return Messages.get(this, "name_meltDown", element.getName());
        } else {
            return Messages.get(this, "name", heatLevel(), element.getName());
        }
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc", heatLevel(), element.getName());
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString(gauge);
    }

    private static final String GAUGE = "gauge";
    private static final String ELEMENT = "element";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);

        bundle.put(GAUGE, gauge);
        bundle.put(ELEMENT, element);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);

        gauge = bundle.getInt(GAUGE);
        element = bundle.getEnum(ELEMENT, ElementType.class);

        ActionIndicator.setAction(this);
    }

    // 액션 인디케이터 관련 구현

    @Override
    public String actionName() {
        return Messages.get(this, "action_name");
    }

    @Override
    public int actionIcon() {
        switch (element) {
            case FIRE: default:
                return HeroIcon.OVERHEAT_FIRE;
            case WATER:
                return HeroIcon.OVERHEAT_WATER;
            case EARTH:
                return HeroIcon.OVERHEAT_EARTH;
            case WIND:
                return HeroIcon.OVERHEAT_WIND;
        }
    }

    @Override
    public Visual secondaryVisual() {
        BitmapText txt = new BitmapText(PixelScene.pixelFont);
        txt.text(Messages.format(TXT_STATUS, gauge, OVERHEAT_MAX));
        switch (heatLevel()) {
            case 0: default:
                txt.hardlight(0x4DE1BC);
                break;
            case 1:
                txt.hardlight(0x4CD040);
                break;
            case 2:
                txt.hardlight(0xD8DF46);
                break;
            case 3:
                txt.hardlight(0xD08340);
                break;
            case 4:
                txt.hardlight(0xD04540);
                break;
            case 5:
                txt.hardlight(0xEA265D);
                break;
        }

        txt.scale = new PointF(0.75f, 0.75f);
        txt.measure();
        return txt;
    }

    @Override
    public int indicatorColor() {
        switch (element) {
            case FIRE: default:
                return 0xE24F2E;
            case WATER:
                return 0x05AAD3;
            case EARTH:
                return 0x14EA83;
            case WIND:
                return 0x5CB3F;
        }
    }

    @Override
    public void doAction() {
        randomElement();

        //이펙트, 사운드 등 추가

        BuffIndicator.refresh(target);
        ActionIndicator.refresh();
    }
}
