package io.anuke.mindustry.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.ui.fragments.BlockInventoryFragment.ItemFlow;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.mindustry.world.modules.ItemModule;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

import static io.anuke.mindustry.Vars.*;

public class OverlayRenderer{
    private static final float indicatorLength = 14f;
    private static final Rectangle rect = new Rectangle();
    private float buildFadeTime;

    public void drawBottom(){
        for(Player player : players){
            InputHandler input = control.input(player.playerIndex);

            if(world.getSector() != null){
                world.getSector().currentMission().drawOverlay();
            }

            if(!input.isDrawing() || player.isDead()) continue;

            Shaders.outline.color.set(Palette.accent);
            Graphics.beginShaders(Shaders.outline);

            input.drawOutlined();

            Graphics.endShaders();
        }
    }

    public Player showingPlayerBlocks;
    public Player[][] blockActions;

    public void showingPlayerBlocks(Player p) {
        showingPlayerBlocks = (p.equals(showingPlayerBlocks) || playerGroup.all().size<1) ? null : p;
    }

    /*public void blockModified(TileEntity t, int id){
        Player p = playerGroup.getByID(id);

        if (t == null) return;
        System.out.println(t.x + " " + t.y + " " + p.name);
        try{ //todo hacky fix, should be replaced with new blockactions[][] on loading new map
            if(p != null) {
                if (blockActions == null) blockActions = new Player[world.width()][world.height()];
                blockActions[(int) t.x/tilesize][(int) t.y/tilesize] = p;
            }
        }catch(ArrayIndexOutOfBoundsException e){
            blockActions = new Player[world.width()][world.height()];
        }
    }*/

    public void blockModified(Tile t, int id){
        Player p = playerGroup.getByID(id);
        try{
            if(p != null) {
                if (blockActions == null) blockActions = new Player[world.width()][world.height()];
                blockActions[t.x][t.y] = p;
            }
            //System.out.println(t.x + " " + t.y + " " + p.name);
        }catch(ArrayIndexOutOfBoundsException e){
            blockActions = new Player[world.width()][world.height()];
        }
    }

    void drawPlayerActions(){
        //System.out.println(showingPlayerBlocks + " " + blockActions + " " + blockActions.length);
        //if (showingPlayerBlocks != null) System.out.println(showingPlayerBlocks.name);
        //if (blockActions == null) System.out.println("nullBlockActions");
        //if (showingPlayerBlocks != null && blockActions!=null && (blockActions.length>0)){
            try{
                int i = 0;
                if (showingPlayerBlocks == null) return;
                if (blockActions == null) return;
                for(int x = 0; x < world.width(); x++){
                    for(int y = 0; y < world.height(); y++){
                        if(showingPlayerBlocks.equals(blockActions[x][y])){
                            Draw.color(Palette.remove);
                            Lines.square(x * tilesize, y * tilesize, 4);
                            i++;
                        }
                    }
                }
                //System.out.println("found " + i);
            }
            catch (Exception e){
                e.printStackTrace();
            }

    }

    int lastItems[] = new int[16];
    float itemHistory[][] = new float[16][256];
    float itemFilter[] = new float[16];

    void drawItemGraph(){
        int q = Vars.fpsLogCounter;
        Tile k = state.teams.get(players[0].getTeam()).cores.first();
        int t = 0;
        int hudOffsetX = ui.hudfrag.shown ? 286 : 44;
        int hudOffsetY = ui.hudfrag.shown ? 112 : 150;
        float z = Core.camera.zoom, s = 1.0f/Core.cameraScale;
        float xo = Core.camera.position.x + s*hudOffsetX - Core.camera.viewportWidth/2, yo = Core.camera.position.y+Core.camera.viewportHeight/2 - hudOffsetY*s;//+Core.camera.viewportHeight*s*0.2f;


        for (int item : k.entity.items.items){
            itemFilter[t] -= (itemFilter[t]-(lastItems[t]-item))/200.0;
            itemHistory[t][q] = (item-lastItems[t])*0.01f;//itemFilter[t];
            lastItems[t] = item;
            t++;
        }
        Lines.stroke(2*s);
        Draw.tscl(s);
        for (int p = 0; p < 15; p++){
            Draw.color(Items.Items[p].color);
            Lines.beginLine();
            q = Vars.fpsLogCounter;


            Draw.tcolor(Items.Items[p].color);
            Draw.text(Items.Items[p].name,z*(xo + 160), z*(yo + itemHistory[p][q]*s));

            for(int i = 0; i < 256; i++){

                Lines.linePoint(z*(xo + i*s), z*(yo + 100*itemHistory[p][q]*s));
                if(++q >= 256) q = 0;
            }
            Lines.endLine();

        }
        Draw.tcolor();
    }

    void drawFPSGraph(){
        int q = Vars.fpsLogCounter;

        int hudOffsetX = ui.hudfrag.shown ? 286 : 44;
        int hudOffsetY = ui.hudfrag.shown ? 112 : 150;
        float z = Core.camera.zoom, s = 1.0f/Core.cameraScale;
        float xo = Core.camera.position.x + s*hudOffsetX - Core.camera.viewportWidth/2, yo = Core.camera.position.y+Core.camera.viewportHeight/2 - hudOffsetY*s;//+Core.camera.viewportHeight*s*0.2f;
        //System.out.println(Core.cameraScale + " " + Core.camera.viewportWidth);
        Draw.color(Color.DARK_GRAY);
        Lines.stroke(2*s);
        for(int i = 0; i < 120; i += 20){
            Draw.tscl(s);
            Draw.text("" + i, xo - 24*s, yo + i*s + 8*s);
            Lines.line(z*xo, z*(yo + i*s) , z*(xo + 256*s) , z*(yo + i*s));
            // Lines.line(xo-246,yo+i+10,xo-256,yo+i+10);
        }

        Draw.color(Color.LIME);

        Lines.beginLine();
        for(int i = 0; i < 256; i++){
            //if (Float.isNaN(Vars.fpsLog[q])) Vars.fpsLog[q] = 0;
            Lines.linePoint(z*(xo + i*s), z*(yo + Vars.fpsLog[q]*s));
            //q++;
            if(++q >= 256) q = 0;
        }
        Lines.endLine();
        Draw.tscl(1);
        Draw.color();
    }
    public void drawTop(){
        //drawItemGraph();
        if (Vars.fpsGraph) drawFPSGraph();
        //Draw.text("this is a test", Graphics.mouseWorld().x,Graphics.mouseWorld().y);

        drawPlayerActions();

        if (ui.hudfrag.blockfrag.convFlow!=null) ui.hudfrag.blockfrag.convFlow.drawPreview();

        for(Player player : playerGroup.all()){
            if(Settings.getBool("indicators") && player != players[0] && player.getTeam() == players[0].getTeam()){
                if(!rect.setSize(Core.camera.viewportWidth * Core.camera.zoom * 0.9f, Core.camera.viewportHeight * Core.camera.zoom * 0.9f)
                .setCenter(Core.camera.position.x, Core.camera.position.y).contains(player.x, player.y)){

                    Tmp.v1.set(player.x, player.y).sub(Core.camera.position.x, Core.camera.position.y).setLength(indicatorLength);

                    Draw.color(player.getTeam().color);
                    Lines.stroke(2f);
                    Lines.lineAngle(Core.camera.position.x + Tmp.v1.x, Core.camera.position.y + Tmp.v1.y, Tmp.v1.angle(), 4f);
                    Draw.reset();
                }
            }
        }

        for(Player player : players){
            if(player.isDead()) continue; //dead players don't draw

            InputHandler input = control.input(player.playerIndex);

            //draw config selected block
            if(input.frag.config.isShown()){
                Tile tile = input.frag.config.getSelectedTile();
                tile.block().drawConfigure(tile);
            }

            input.drawTop();

            buildFadeTime = Mathf.lerpDelta(buildFadeTime, input.isPlacing() ? 1f : 0f, 0.06f);

            Draw.reset();
            Lines.stroke(buildFadeTime*2f);

            if(buildFadeTime > 0.005f){
                for(Team enemy : state.teams.enemiesOf(player.getTeam())){
                    for(Tile core : state.teams.get(enemy).cores){
                        float dst = Vector2.dst(player.x, player.y, core.drawx(), core.drawy());
                        if(dst < state.mode.enemyCoreBuildRadius * 1.5f){
                            Draw.color(Color.DARK_GRAY);
                            Lines.poly(core.drawx(), core.drawy() - 2, 200, state.mode.enemyCoreBuildRadius);
                            Draw.color(Palette.accent, enemy.color, 0.5f + Mathf.absin(Timers.time(), 10f, 0.5f));
                            Lines.poly(core.drawx(), core.drawy(), 200, state.mode.enemyCoreBuildRadius);
                        }
                    }
                }
            }

            Draw.reset();

            //draw selected block bars and info
            if(input.recipe == null && !ui.hasMouse()){
                Vector2 vec = Graphics.world(input.getMouseX(), input.getMouseY());
                Tile tile = world.tileWorld(vec.x, vec.y);

                if(tile != null && tile.block() != Blocks.air && tile.target().getTeam() == players[0].getTeam()){
                    Tile target = tile.target();

                    if(showBlockDebug && target.entity != null){
                        Draw.color(Color.RED);
                        Lines.crect(target.drawx(), target.drawy(), target.block().size * tilesize, target.block().size * tilesize);
                        Vector2 v = new Vector2();

                        Draw.tcolor(Color.YELLOW);
                        Draw.tscl(1f);
                        Array<Object> arr = target.block().getDebugInfo(target);
                        StringBuilder result = new StringBuilder();
                        for(int i = 0; i < arr.size / 2; i++){
                            result.append(arr.get(i * 2));
                            result.append(": ");
                            result.append(arr.get(i * 2 + 1));
                            result.append("\n");
                        }
                        Draw.textc(result.toString(), target.drawx(), target.drawy(), v);
                        Draw.color(0f, 0f, 0f, 0.5f);
                        Fill.rect(target.drawx(), target.drawy(), v.x, v.y);
                        Draw.textc(result.toString(), target.drawx(), target.drawy(), v);
                        Draw.tscl(1f);
                        Draw.reset();
                    }

                    Block block = target.block();
                    TileEntity entity = target.entity;

                    if(entity != null){
                        int[] values = {0, 0};
                        boolean[] doDraw = {false};

                        Runnable drawbars = () -> {
                            for(BlockBar bar : block.bars.list()){
                                float offset = Mathf.sign(bar.top) * (block.size / 2f * tilesize + 2f + (bar.top ? values[0] : values[1]));

                                float value = bar.value.get(target);

                                if(MathUtils.isEqual(value, -1f)) continue;

                                if(doDraw[0]){
                                    drawBar(bar.type.color, target.drawx(), target.drawy() + offset, value);
                                }

                                if(bar.top)
                                    values[0]++;
                                else
                                    values[1]++;
                            }
                        };

                        drawbars.run();

                        if(values[0] > 0){
                            drawEncloser(target.drawx(), target.drawy() + block.size * tilesize / 2f + 2f, values[0]);
                        }

                        if(values[1] > 0){
                            drawEncloser(target.drawx(), target.drawy() - block.size * tilesize / 2f - 2f - values[1], values[1]);
                        }

                        doDraw[0] = true;
                        values[0] = 0;
                        values[1] = 1;

                        drawbars.run();
                    }


                    target.block().drawSelect(target);

                }
            }

            if(input.isDroppingItem()){
                Vector2 v = Graphics.world(input.getMouseX(), input.getMouseY());
                float size = 8;
                Draw.rect(player.inventory.getItem().item.region, v.x, v.y, size, size);
                Draw.color(Palette.accent);
                Lines.circle(v.x, v.y, 6 + Mathf.absin(Timers.time(), 5f, 1f));
                Draw.reset();

                Tile tile = world.tileWorld(v.x, v.y);
                if(tile != null) tile = tile.target();
                if(tile != null && tile.getTeam() == player.getTeam() && tile.block().acceptStack(player.inventory.getItem().item, player.inventory.getItem().amount, tile, player) > 0){
                    Draw.color(Palette.place);
                    Lines.square(tile.drawx(), tile.drawy(), tile.block().size * tilesize / 2f + 1 + Mathf.absin(Timers.time(), 5f, 1f));
                    Draw.color();
                }
            }
        }
    }

    void drawBar(Color color, float x, float y, float finion){
        finion = Mathf.clamp(finion);

        if(finion > 0) finion = Mathf.clamp(finion + 0.2f, 0.24f, 1f);

        float len = 3;

        float w = (int) (len * 2 * finion);

        Draw.color(Color.BLACK);
        Fill.crect(x - len, y, len * 2f, 1);
        if(finion > 0){
            Draw.color(color);
            Fill.crect(x - len, y, Math.max(1, w), 1);
        }
        Draw.color();
    }

    void drawEncloser(float x, float y, float height){

        float len = 4;

        Draw.color(Palette.bar);
        Fill.crect(x - len, y - 1, len * 2f, height + 2f);
        Draw.color();
    }
}
