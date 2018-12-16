package io.anuke.mindustry.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntArray;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.blocks.DistributionBlocks;
import io.anuke.mindustry.core.World;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.world.Build.validPlace;

public class IncrementalFlow {
    float [] field;
    int TARGET;
    int count = 0; // for alternating bias
    int [] cardinals;
    int wd, ht;
    int sizeSq, tileSizeHalf;
    IntArray validTiles = new IntArray();
    IntArray targetTiles = new IntArray();


    IncrementalFlow(int totCells, int target, int w, int h) {
        field = new float[totCells];
        TARGET = target;
        wd = w;
        ht = h;
        cardinals = new int[] {-1, 1, -wd, wd};
        tileSizeHalf = Vars.tilesize/2;
        sizeSq = w*h;
        for (int i = 0; i < totCells; i++) field[i] = 0;
        Block blockPath = DistributionBlocks.conveyor;

        for (int i = 0; i < sizeSq; i++) if (validPlace(Team.blue,i%w,i/w,blockPath,0)) validTiles.add(i);
        //for (int i = 0; i < sizeSq; i++) if (tiles[i]==TARGET) targetTiles.add(i);
        targetTiles.add(target);
    }

    void step() {
        for (int tile : targetTiles.items) field[tile]+=65536;
        for (int tile : validTiles.items) processTile(tile);
    }

    void processTile(int xy) {
        int neighbor;
        for (int cardinal : cardinals) {
            neighbor = xy + cardinal;
            if (((neighbor>0 && neighbor<sizeSq) && field[neighbor] > field[xy]))  field[xy] = (field[neighbor]+field[xy]*2)/3; // LPF adjust here
        }
        field[xy]*= 0.95;
    }

    Vector2 getHeading(int i) {
        Vector2 l = new Vector2(0, 0);
        if ((i>wd)&&(i<sizeSq-wd)) {
            l.y -= field[i-wd]-field[i+wd];
            l.x -= field[i-1]-field[i+1];
        }
        return l;
    }

    float [] heading(int i){
        return new float[] {field[i-wd]-field[i+wd], field[i-1]-field[i+1]};
    }

    void setHeading(Vector2 heading, int i){
        heading.set(field[i+1]-field[i-1],field[i+wd]-field[i-wd]).nor().scl(0.5f,0.5f);
    }

    void clearField() {
        for (int i = 0; i < field.length; i++) {
            field[i] = 0;
        }
    }


}
