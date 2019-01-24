package io.anuke.mindustry.ai;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntArray;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Build;
import io.anuke.mindustry.world.Tile;

import java.util.ArrayList;

import static io.anuke.mindustry.Vars.players;
import static io.anuke.mindustry.Vars.world;

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

    public void calcPath(Block convType, Tile startTile, Tile targetTile){
        if(startTile.equals(start) && targetTile.equals(target)) return; //optimization to run only once per new target tile
        start = startTile;
        target = targetTile;
        path.clear();
        int[] flowField = convFlow(convType, xyToI(startTile.x, startTile.y));
        int i = xyToI(targetTile.x, targetTile.y);
        if (!validIndex(i)) return;
        int dir = nextTile(flowField, i);
        int ni = dir + i;
        Tile t = targetTile;
        int limit = convLimit;
        path.add(new Vector3(t.x, t.y, cardinalToRot(dir)));
        while((t != start) && (limit-- > 0)){
            int[] xy = iToXY(ni);
            int rot = cardinalToRot(dir);
            if(!validIndex(ni) ||!Build.validPlace(players[0].getTeam(), xy[0], xy[1], convType, rot)){
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
        return ((i > width) && (i < sizeSq - width) && (i%width>0));
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

    private Tile getTile(int i){ // retrieved tile from packed coordinate (i)
        int[] xy = iToXY(i);
        if(validIndex(i)) return world.rawTile(xy[0], xy[1]); //TODO fix this to use packed i instead
        else return world.rawTile(1, 1); //TODO fix this, return null, handle null responses
    }

    private boolean validConvTile(Tile t, int i){
        //TODO better validity checks, rob more checks from Build.validPlace(), don't use validPlace because its massive and slow
        return (validIndex(i) && (!t.solid()) && (t.block() == Blocks.air));
    }

    private int[] convFlow(Block convType, int dest){
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