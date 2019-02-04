package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.Align;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.input.Input;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.ui.TextField;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Timer;

import static io.anuke.mindustry.Vars.players;
import static io.anuke.mindustry.Vars.ui;


public class QueueFragment extends Fragment{
    private boolean visible = Vars.showQueFragment;
    private Table content = new Table();
    private Timer timer = new Timer();

    @Override
    public void build(Group parent){
        parent.fill(cont -> {
            cont.visible(() -> visible);
            cont.center().bottom();
            cont.update(() -> {
                if(updateVisible() && timer.get(10)){
                    rebuild();
                    content.pack();
                    content.act(Gdx.graphics.getDeltaTime());
                    Core.scene.act(0f);
                }

            });

            cont.table("button", pane -> {
                pane.pane(content).left();
                pane.align(Align.left);
                pane.row();
                pane.addCheck("autoSort", false, autoSorting -> {
                    Settings.putBool("autoSorting", autoSorting);
                    Settings.save();
                });

                pane.addField("_", text -> {
                    Settings.putString("ip", text);
                    Settings.save();
                }).padLeft(10).size(220f, 54f).get();

                pane.addButton("?", ()->{
                    ui.hudfrag.showTextDialog("instructions for dummies");
                }).padLeft(10);

                pane.row();
                pane.addCheck("freeCam", players[0].freecam, freeCamming -> {
                    freeCamming = players[0].freecam;
                    Settings.putBool("freeCamming", players[0].freecam);
                    Settings.save();
                });
                pane.row();
                pane.addButton("reverseQueue", ()->{
                    players[0].reverseQueue();
                }).width(200);

                pane.addButton("save", ()->{
                    players[0].reverseQueue();
                }).width(80);

                pane.addButton("load", ()->{
                    players[0].reverseQueue();
                }).width(80);

                pane.addButton("..", ()->{
                    players[0].reverseQueue();
                }).width(40);

                pane.row();

                pane.addImageButton("icon-rotate-left",24f, ()->{
                    players[0].reverseQueue();
                }).left().padLeft(5);

                pane.addImageButton("icon-rotate-right",24f, ()->{
                    players[0].reverseQueue();
                }).marginLeft(5).get();


                //Platform.instance.addDialog(field, 100);
            });
        });
        rebuild();
    }

    public void rebuild(){
        updateVisible();
        content.clear();
        if(Net.server()) return;
        Table table = new Table(){
            @Override
            public void draw(Batch batch, float parentAlpha){
                super.draw(batch, parentAlpha);
            }
        };
        content.add(table);
    }

    public void toggle(){
        updateVisible();
    }

    public boolean updateVisible(){
        //(Vars.showQueFragment) && (Vars.state.is(State.playing));
        visible = true;
        return visible;
    }
}
