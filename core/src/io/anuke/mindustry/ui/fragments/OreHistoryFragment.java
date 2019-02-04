package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetConnection;
import io.anuke.mindustry.net.Packets.AdminAction;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Timer;

import static io.anuke.mindustry.Vars.*;

public class OreHistoryFragment extends Fragment{
    private boolean visible = false;
    private Table content = new Table().marginRight(13f).marginLeft(13f);
    private Timer timer = new Timer();

    @Override
    public void build(Group parent){
        Core.scene.table().addRect((a, b, w, h) -> {
            Draw.colorl(0.1f);
            Draw.color(Palette.accent);
            Lines.line(0,0,control.input(0).getMouseX(),Gdx.graphics.getHeight()-control.input(0).getMouseY() );
        }).top().visible(() -> visible).grow();

        parent.fill(cont -> {
            cont.visible(() -> visible);
            cont.update(() -> {
                if(!(Net.active() && !state.is(State.menu))){
                    visible = false;
                    return;
                }
                if(visible && timer.get(20)){
                    rebuild();
                    content.pack();
                    content.act(Gdx.graphics.getDeltaTime());
                    //TODO hack
                    Core.scene.act(0f);
                }
            });

            cont.table("button", pane -> {
                pane.label(() -> Bundles.format(playerGroup.size() == 1 ? "text.players.single" : "text.players", playerGroup.size()));
                pane.row();
                pane.pane(content).grow().get().setScrollingDisabled(true, false);
                pane.row();

                pane.table(menu -> {
                    menu.defaults().size(400,200).growX().height(50f).fillY();
                    menu.addButton("$text.close", this::toggle);
                }).margin(0f).pad(10f).growX();

            }).touchable(Touchable.enabled).margin(14f);
        });

    }

    public void rebuild(){

    }


    public void toggle(){
        visible = !visible;
        if(visible){
            rebuild();
        }
    }

}
