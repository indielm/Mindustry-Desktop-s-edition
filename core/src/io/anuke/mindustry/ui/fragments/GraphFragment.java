package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetConnection;
import io.anuke.mindustry.net.Packets.AdminAction;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timer;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.Vars.netServer;
import static io.anuke.ucore.core.Core.scene;

public class GraphFragment extends Fragment{
    private boolean visible = Vars.fpsGraph;
    private Table content = new Table().marginRight(13f).marginLeft(13f);
    private Timer timer = new Timer();

    @Override
    public void build(Group parent){
        parent.fill(cont -> {
            cont.visible(() -> visible);
            cont.update(() -> {
                visible = (Vars.fpsGraph);// && (state.is(State.playing));
                if(visible && timer.get(10)){
                    rebuild();
                    content.pack();
                    content.act(Gdx.graphics.getDeltaTime());
                    //TODO hack
                    Core.scene.act(0f);

                    cont.left().marginLeft(ui.hudfrag.shown? 246:4);
                    cont.top().marginTop(ui.hudfrag.shown? -16:40);
                }
            });

            cont.table("button", pane -> {
                pane.pane(content).grow().size(300, 140).get().setScrollingDisabled(true, false);
            }).margin(14f);
            //cont.left().marginLeft(ui.hudfrag.shown? 120:10);
            //cont.top().marginTop(ui.hudfrag.shown? 10:50);
        });

        rebuild();
    }

    public void rebuild(){
        visible = (Vars.fpsGraph) ;//&& (state.is(State.playing));
        content.clear();
        if(Net.server()) return;
        Table table = new Table(){
            @Override
            public void draw(Batch batch, float parentAlpha){
                super.draw(batch, parentAlpha);
                Draw.reset();
                drawGraph();
            }
        };
        table.margin(8);
        table.left();
        table.top();
        content.add(table);//.padBottom(-6).width(350f).maxHeight(h + 14);
        content.row();
        content.marginBottom(5);
    }

    public void toggle(){
        visible  = (Vars.fpsGraph);// && (state.is(State.playing));
    }

    public void drawGraph(){
        float xo = 40, yo = 20;
        Draw.color(Color.LIGHT_GRAY);

        Lines.stroke(2);
        for(int i = 0; i < 120; i += 20){
            Draw.tscl(1);
            Draw.text("" + i, xo - 24, yo + i + 8);
            Lines.line(xo, (yo + i), (xo + 256), (yo + i));
        }

        Draw.color(Color.LIME);
        Lines.beginLine();
        int q = Vars.fpsLogCounter;
        for(int i = 0; i < 256; i++){
            Lines.linePoint((xo + i), (yo + Vars.fpsLog[q]));
            if(++q >= 256) q = 0;
        }
        Lines.endLine();
        Draw.tscl(1);
        Draw.color();
    }
}
