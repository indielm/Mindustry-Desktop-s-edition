package io.anuke.mindustry.DE;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntArray;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.DistributionBlocks;
import io.anuke.mindustry.entities.traits.BuilderTrait;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Build;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.input.Input;

import java.util.ArrayList;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.ui;

public class PathFlow{
    public int[] field;
    public boolean success = false;
    private final int width, height;
    public final int sizeSq;
    private final int[] cardinals;
    private final int oreAvoidWeight = 3,
    convLimit = 200,
    maxSearchLength = 255; //TODO lower to optimize flowfield generation time
    public ArrayList<Vector3> path = new ArrayList<>(); //TODO replace ArrayList<Vector3> with some proper(int) x,y,rotation class
    private ArrayList<Vector3> reverse = new ArrayList<>();
    Tile start, target;

    public PathFlow(){
        width = world.width(); //TODO move this to a function to call on map load
        height = world.height();
        sizeSq = width * height;
        cardinals = new int[]{-1, 1, -width, width};
        field = new int[sizeSq];
    }

    boolean withinPlaceDist(float x2, float y2){
        if(players[0].freecam) return true;
        final float dist2 = BuilderTrait.placeDistance * BuilderTrait.placeDistance;
        float x1 = players[0].x, y1 = players[0].y;
        return (((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)) < dist2);
    }

    public void drawPreview(){
        InputHandler input = control.input(0);
        Tile endTile = world.tileWorld(Graphics.mouseWorld().x, Graphics.mouseWorld().y);
        if(!ui.hudfrag.blockfrag.flowReady || input.recipe == null || endTile == null) return;
        calcPath(DistributionBlocks.titaniumconveyor, ui.hudfrag.blockfrag.flowStart, endTile.target());
        if(!success) return;
        Block block = input.recipe.result;
        Lines.stroke(1);
        final float rectSize = block.size * tilesize / 2f - 1;
        for(Vector3 t : path){
            float tfx = t.x * tilesize, tfy = t.y * tilesize;
            if(withinPlaceDist(tfx, tfy)){
                Draw.color(Palette.accent);
                Lines.square(tfx, tfy, 5);
            }else{
                Lines.square(tfx, tfy - 1, rectSize);
                Draw.color(Palette.removeBack);
                Lines.square(tfx, tfy - 1, rectSize);
                Draw.color(Palette.remove);
                Lines.square(tfx, tfy, rectSize);
            }
        }
        Draw.color();
        String name = input.recipe.result.name;
        int lastRot = (path.size() < 1) ? input.rotation : (int) path.get(0).z;

        int animFrame = (int) ((System.currentTimeMillis() % 200) / 50);
                        /*    1
                            2   0
                              3  rotation directions*/
        //TODO replace truth tables with logic
        final int[][] turnTable = {  // vert = rot, horz = lastRot
        {0 * 90, 3 * 90, 0 * 90, 0 * 90},
        {1 * 90, 1 * 90, 0 * 90, 1 * 90},
        {2 * 90, 2 * 90, 2 * 90, 1 * 90},
        {2 * 90, 3 * 90, 3 * 90, 3 * 90}};
        final int[][] backwardsAnim = {
        {0, 1, 0, 0},
        {0, 0, 1, 0},
        {0, 0, 0, 1},
        {1, 0, 0, 0}};
        float x, y;
        int rot;
        final float blockOffset = block.offset();
        if(name.contains("conv")){
            TextureRegion region;
            for(Vector3 t : path){
                x = t.x * tilesize + blockOffset;
                y = t.y * tilesize + blockOffset;
                rot = (int) t.z;
                region = Draw.region(name + "-" + (rot == lastRot ? 0 : 1) + "-" + (backwardsAnim[rot][lastRot] == 0 ? animFrame : 3 - animFrame));
                if(!withinPlaceDist(x, y))
                    continue; //TODO move this to PathFlow, bake boolean into future path object
                Draw.rect(region, x, y, region.getRegionWidth(), region.getRegionHeight(), turnTable[rot][lastRot]);
                lastRot = rot;
            }
        }else{
            TextureRegion conduitBottom = Draw.region("conduit-bottom");
            TextureRegion region;
            for(Vector3 t : path){
                x = t.x * tilesize + blockOffset;
                y = t.y * tilesize + blockOffset;
                rot = (int) t.z;
                if(!withinPlaceDist(x, y)) continue;
                Draw.rect(conduitBottom, x, y, conduitBottom.getRegionWidth(), conduitBottom.getRegionHeight(), turnTable[rot][lastRot]);
                region = Draw.region(name + "-top-" + (rot == lastRot ? 0 : 1));
                Draw.rect(region, x, y, region.getRegionWidth(), region.getRegionHeight(), turnTable[rot][lastRot]);
                lastRot = rot;
            }
        }

        if(Inputs.keyDown(Input.SHIFT_LEFT)){
            Color temp = new Color();
            for(int i = 0; i < sizeSq; i++){
                temp.fromHsv(6f * field[i], 1.0f, 1.0f);
                temp.a = field[i] / 455.0f;
                Draw.color(temp);
                Fill.rect((i % world.width()) * tilesize, (i / world.width()) * tilesize, tilesize, tilesize);
            }
        }
    }

    public void calcPath(Block convType, Tile startTile, Tile targetTile){
        if(startTile.equals(start) && targetTile.equals(target))
            return; //optimization to run only once per new target tile
        start = startTile;
        target = targetTile;
        path.clear();
        int[] flowField = convFlow(convType, xyToI(startTile.x, startTile.y));
        int i = xyToI(targetTile.x, targetTile.y);
        if(!validIndex(i)) return;
        int dir = nextTile(flowField, i);
        int ni = dir + i;
        Tile t = targetTile;
        int limit = convLimit;
        path.add(new Vector3(t.x, t.y, cardinalToRot(dir)));
        while((t != start) && (limit-- > 0)){
            int[] xy = iToXY(ni);
            int rot = cardinalToRot(dir);
            if(!validIndex(ni) || !Build.validPlace(players[0].getTeam(), xy[0], xy[1], convType, rot)){
                success = false;
                return;
            }
            path.add(new Vector3(xy[0], xy[1], rot));
            dir = nextTile(flowField, ni);
            ni += dir;
            t = world.tile(ni);
        }
        success = t.equals(start);
        if(!success) return;
        path.add(new Vector3(t.x, t.y, cardinalToRot(dir)));
        reverse = new ArrayList<>();
        for(int q = path.size() - 1; q >= 0; q--) reverse.add(path.get(q));
        path = reverse;
    }

    private int xyToI(int x, int y){ //TODO  my xy packed index is (weirdly?) incompatible with world.tiles packed tiles' indices, fix for optimization, BEGONE ALL xyToI/iToXY DEGENERACY
        return x + y * world.width();
    }

    private int[] iToXY(int i){
        return new int[]{i % world.width(), i / world.width()};
    }

    private boolean validIndex(int i){
        return ((i > width) && (i < sizeSq - width) && (i % width > 0));
    }

    private int nextTile(int[] flow, int i){ // return the cardinal direction with the highest flow value from packed coordinate i
        int highestDir = -1;
        int highestVal = -1;
        for(int cardinal : cardinals){
            Integer neighbor = flow[i + cardinal];
            if(neighbor > highestVal){
                highestVal = neighbor;
                highestDir = cardinal;
            }
        }
        return highestDir;
    }

    private byte cardinalToRot(int dir){
        if(dir == -1) return (byte) 0;
        if(dir == 1) return (byte) 2;
        if(dir < 0) return (byte) 1;
        return (byte) 3;
    }

    private Tile getTile(int i){
        return (validIndex(i) ? world.tile(i) : world.tile(0));
    }

    public void test(){
        InputHandler input = control.input(0);
        Vector2 vec = Graphics.world(input.getMouseX(), input.getMouseY());
        Tile tile = world.tileWorld(vec.x, vec.y);
        System.out.println(tile.x + " , " + tile.y);
        int i = xyToI(tile.x,tile.y);
        System.out.println(i);
        Tile t2 = world.tile(i);
        System.out.println(t2.x + " , " + t2.y);
    }


    private boolean validConvTile(Tile t, int i){
        //TODO better validity checks, rob more checks from Build.validPlace(), don't use validPlace because its massive and slow
        return validIndex(i) && (!t.solid()) && (t.block() == Blocks.air);
    }

    private int[] convFlow(Block convType, int dest){
        test();
        for(int i = 0; i < sizeSq; i++) field[i] = 0;
        IntArray current = new IntArray(), next = new IntArray();
        field[dest] = maxSearchLength;
        current.add(dest);
        while(current.size > 0){
            for(int index : current.items){
                for(int cardinal : cardinals){
                    int neighbor = index + cardinal;
                    if(!validIndex(neighbor)) continue;
                    Tile t = getTile(neighbor);
                    if(!validConvTile(t, neighbor)) continue;
                    int p = field[index] - 1;
                    //if (p==0) return field; //TODO optimize for search length
                    if(!t.floor().hasOres) p -= oreAvoidWeight;
                    if(field[neighbor] < p){
                        next.add(neighbor);
                        field[neighbor] = p;
                    }
                }
            }
            IntArray swap = current;
            current = next;
            swap.clear();
            next = swap;
        }
        return field;
    }
}