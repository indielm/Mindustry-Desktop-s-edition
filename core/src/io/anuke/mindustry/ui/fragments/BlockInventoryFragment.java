package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.ui.ItemImage;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.BooleanProvider;
import io.anuke.ucore.input.Input;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.event.HandCursorListener;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

import java.text.DecimalFormat;
import java.util.Arrays;

import static com.badlogic.gdx.math.MathUtils.floor;
import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.util.Mathf.clamp;
import static io.anuke.ucore.util.Mathf.pow;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.signum;
import static java.lang.StrictMath.abs;

public class BlockInventoryFragment extends Fragment{
    private final static float holdWithdraw = 40f;

    private Table table;
    private Tile tile;
    private InputHandler input;
    private float holdTime = 0f;
    private boolean holding;
    private Item lastItem;

    private final int itemTypesTot = content.items().size;
    private ItemFlow[] itemFilters;
    private static final DecimalFormat flowFormat = new DecimalFormat("#.#");
    private Color flowColor = new Color();
    private final float stayAlive = 60 * 16, altHoldTime = 60 * 1.25f;
    private final DecimalFormat df = new DecimalFormat("#.#");
    private final int textUpdateSpeed = 50; // ms

    public BlockInventoryFragment(InputHandler input){
        this.input = input;
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void requestItem(Player player, Tile tile, Item item, int amount){
        if(player == null || tile == null) return;

        int removed = tile.block().removeStack(tile, item, amount);

        player.inventory.addItem(item, removed);
        for(int j = 0; j < clamp(removed / 3, 1, 8); j++){
            Timers.run(j * 3f, () -> Call.transferItemEffect(item, tile.drawx(), tile.drawy(), player));
        }

    }

    @Override
    public void build(Group parent){
        table = new Table();
        table.visible(() -> !state.is(State.menu));
        table.setTransform(true);
        parent.setTransform(true);
        parent.addChild(table);

        int totTypes = content.items().size;
        itemFilters = new ItemFlow[itemTypesTot];
        for(int i = 0; i < itemTypesTot; i++) itemFilters[i] = new ItemFlow(content.item(i));
    }

    public void showFor(Tile t){
        this.tile = t.target();
        if(tile == null || tile.entity == null || !tile.block().isAccessible() || tile.entity.items.total() == 0)
            return;
        rebuild(true);
    }

    public void hide(){
        table.actions(Actions.scaleTo(0f, 1f, 0.06f, Interpolation.pow3Out), Actions.visible(false), Actions.run(() -> {
            table.clear();
            table.update(null);
        }));
        table.setTouchable(Touchable.disabled);
        tile = null;
    }

    public class ItemFlow{


        final Item item;
        private float itemLast, itemAverages, itemLPF, sustain, hold;
        public boolean showing = false, fresh = true;
        private int init = 0;
        private long lastTextTime;
        private String text;
        ItemFlow(Item itemRef){
            item = itemRef;
            resetFilter();
        }

        public boolean filter(){
            if(fresh || Inputs.keyTap("altMenu")) resetFilter();
            float time = (1f / 60f) / Gdx.graphics.getDeltaTime();
            sustain = tile.entity.items.has(item) ? stayAlive : max(sustain - time, -1.0f);
            if(isActive()){
                final int currentItems = tile.entity.items.get(item);
                if (init>0) init--;
                if (fresh || init > 12) {
                    itemLast = tile.entity.items.get(item);
                    fresh = false;
                    return isActive();
                }
                if(itemLast == 0) itemLast = currentItems;
                float dif = (currentItems - itemLast)  * 60 * time;
                itemLast = currentItems;
                // float d2 = (pow(abs(itemLPF - itemAverages),2)/3000); // PID experiment to converge on average quicker
                // p = clamp((p+d2)/2,0.002f,0.22f);
                if (init>0 ){
                    itemAverages -= (itemAverages - dif) /3;
                    itemLPF -= (itemLPF -itemAverages)/3;///(itemLPF -itemAverages)/8;
                }  else {
                    itemAverages -= (itemAverages - dif)/8;
                    itemLPF -= (itemLPF -itemAverages)/120;
                }
                /*if (item.name.equals("copper")) { //debugging
                    System.out.println(init + " " + (itemAverages) + " " + itemLPF);
                }*/
            }
            return isActive();
        }

        public void resetFilter(){
            itemLast = 0.0f;
            itemAverages = 0.0f;
            itemLPF = 0.0f;
            sustain = 0.0f;
            hold = 0;
            init = 24;
        }

        public boolean showFilter(){
            boolean showingOld = showing;
            hold = clamp(hold + (Inputs.keyDown("select") ? 1 : -4) * 60 * Gdx.graphics.getDeltaTime(), 0, altHoldTime);
            if(hold >= altHoldTime) showing = true;
            else if((hold <= 0) && !Inputs.keyDown("altMenu")) showing = false;
            if(!showing && Inputs.keyTap("altMenu")) showing = true;
            if (!showingOld && showing) fresh = true;
            return showing;
        }

        private boolean isActive(){
            return tile.entity.items.has(item) || (sustain > 0.0f);
        }

        private String getFilterText(){
            final float intensity = 40.0f; //at this item/sec, full color lerp
            flowColor.set(Color.WHITE);
            flowColor.lerp(itemLPF < 0 ? Color.SCARLET : Color.LIME, clamp(Math.abs((float) itemLPF), 0, intensity) / intensity);//looks good but can be hard to read
            if ((abs(itemLPF) < 0.006f) || (init>0))return "[accent]  *";
            if (System.currentTimeMillis() - lastTextTime > textUpdateSpeed){
                text = ((signum(itemLPF) > 0 && abs(itemLPF) > 0.09f) ? "[LIME]+" : "[SCARLET]-") + "[#" + flowColor.toString() + "]" + flowFormat.format(abs(itemLPF));
                lastTextTime = System.currentTimeMillis();
            }
            return text;
        }

        public String updateText(){
            filter();
            return showFilter() ? getFilterText() : round(tile.entity.items.get(item));
        }
    }

    private void rebuild(boolean actions){
        Player player = input.player;
        IntSet container = new IntSet();
        table.clearChildren();
        table.background("inventory");
        table.setTouchable(Touchable.enabled);

        table.update(() -> {

            if(state.is(State.menu) || tile == null || tile.entity == null || !tile.block().isAccessible() || tile.entity.items.total() == 0){
                hide();
                //System.out.println("rebuild");
                for(int i = 0; i < content.items().size; i++){
                    itemFilters[i].fresh = true;
                }
            }else{
                if(holding && lastItem != null){
                    holdTime += Timers.delta();

                    if(holdTime >= holdWithdraw){
                        int amount = min(tile.entity.items.get(lastItem), player.inventory.itemCapacityUsed(lastItem));
                        Call.requestItem(player, tile, lastItem, amount);
                        holding = false;
                        holdTime = 0f;
                    }
                }

                updateTablePosition();

                if(tile.block().hasItems){
                    for(int i = 0; i < content.items().size; i++){

                        boolean has = tile.entity.items.has(content.item(i));
                        if(has != container.contains(i)){
                            rebuild(false);
                        }
                    }
                }
            }
        });

        int cols = 3;
        int row = 0;

        table.margin(6f);
        table.defaults().

        //size(mobile ? 16 * 3 : 16 * 3).
        size(16 * 3).space(6f);
        //space(6f);

        if(tile.block().hasItems){
            for(int i = 0; i < content.items().size; i++){
                Item item = content.item(i);

                //if (actions) itemFilters[i].resetFilter();
                //itemFilters[i].fresh = 12;
                //itemFilters[i].resetFilter();
                //itemFilters[i].updateText();

                if(!itemFilters[i].isActive()) continue;
                //if (itemFilters[i].sustain==-1)  continue;

                container.add(i);

                BooleanProvider canPick = () -> player.inventory.canAcceptItem(item);

                HandCursorListener l = new HandCursorListener();
                l.setEnabled(canPick);

                ItemImage image = new ItemImage(item.region, () -> {
                    if(tile == null || tile.entity == null) return "";

                    return itemFilters[item.id].updateText();
                });


                image.addListener(l);

                image.addListener(new InputListener(){
                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button){
                        if(!canPick.get() || !tile.entity.items.has(item)) return false;
                        int amount = min(1, player.inventory.itemCapacityUsed(item));
                        Call.requestItem(player, tile, item, amount);
                        lastItem = item;
                        holding = true;
                        holdTime = 0f;
                        return true;
                    }

                    @Override
                    public void touchUp(InputEvent event, float x, float y, int pointer, int button){
                        holding = false;
                        lastItem = null;
                    }
                });
                table.add(image);

                if(row++ % cols == cols - 1) table.row();
            }
        }

        if(row == 0)

        {
            table.setSize(0f, 0f);
        }

        updateTablePosition();

        if(actions)

        {
            table.actions(Actions.scaleTo(0f, 1f), Actions.visible(true),
            Actions.scaleTo(1f, 1f, 0.07f, Interpolation.pow3Out));
        }

    }

    private static double round(float value, int precision){
        int scale = (int) Math.pow(10, precision);
        return (float) Math.round(value * scale) / scale;
    }

    private String round(float f){
        f = (int) f;
        if(f >= 1000000){
            return Strings.toFixed(f / 1000000f, 1) + "[gray]mil[]";
        }else if(f >= 1000){
            return Strings.toFixed(f / 1000, 1) + "k";
        }else{
            return (int) f + "";
        }
    }

    private void updateTablePosition(){
        Vector2 v = Graphics.screen(tile.drawx() + tile.block().size * tilesize / 2f, tile.drawy() + tile.block().size * tilesize / 2f);
        table.pack();
        table.setPosition(v.x, v.y, Align.topLeft);
    }
}
