package io.anuke.mindustry.ai;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntArray;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import java.util.ArrayList;

import static io.anuke.mindustry.Vars.world;

public class PathFlow{
    private final int width = world.width(), height = world.height(), sizeSq = width * height;
    private final int[] cardinals = new int[]{-1, 1, -width, width};
    private final int oreAvoidWeight = 8,
    convLimit = 80,
    maxSearchLength = 255; //TODO lower to optimize flowfield generation

    public PathFlow(){
    }

    public ArrayList<Vector3> getConvPath(Block convType, Tile startTile, Tile targetTile){ //TODO replace ArrayList<Vector3> with some proper(int) x,y,rotation class
        ArrayList<Vector3> paths = new ArrayList<Vector3>();
        int[] flowField = convFlow(convType, xyToI(startTile.x, startTile.y));
        int i = xyToI(targetTile.x, targetTile.y);
        int dir = nextTile(flowField, i);
        int ni = dir + i;
        Tile t = targetTile;
        int limit = convLimit;
        paths.add(new Vector3(t.x, t.y, cardinalToRot(dir)));
        while((t != startTile) && (limit-- > 0)){
            paths.add(new Vector3(ni % width, ni / width, cardinalToRot(dir)));
            dir = nextTile(flowField, ni);
            ni += dir;
            t = world.tile(ni);
        }
        paths.add(new Vector3(t.x, t.y, cardinalToRot(dir)));

        ArrayList<Vector3> reverse = new ArrayList<Vector3>();
        for(int q = paths.size() - 1; q >= 0; q--) reverse.add(paths.get(q));
        return reverse;
    }

    private int xyToI(int x, int y){
        return x + y * world.width();
    }

    private int[] iToXY(int i){
        return new int[]{i % world.width(), i / world.width()};
    }

    private boolean validIndex(int i){
        return ((i > width) && (i < sizeSq - width));
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
        if(validIndex(i)) return world.rawTile(xy[0], xy[1]);
        else return world.rawTile(1, 1); //TODO fix this, return null, handle null responses
    }

    private boolean validConvTile(Tile t, int i){
        return (validIndex(i) && (!t.solid()) && (t.block() == Blocks.air));
    }

    private int[] convFlow(Block convType, int dest){
        int[] field = new int[sizeSq];
        IntArray current = new IntArray(), next = new IntArray();
        field[dest] = maxSearchLength;
        current.add(dest);
        while(current.size > 0){
            for(int index : current.items){
                for(int cardinal : cardinals){
                    int neighbor = index + cardinal;
                    if(validIndex(neighbor)){
                        Tile t = getTile(neighbor);
                        if(validConvTile(t, neighbor)){
                            int p = field[index] - 1;
                            if(!t.floor().hasOres) p -= oreAvoidWeight;
                            if(field[neighbor] < p){
                                next.add(neighbor);
                                field[neighbor] = p;
                            }
                        }
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