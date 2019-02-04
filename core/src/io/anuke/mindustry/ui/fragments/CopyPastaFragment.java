package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.core.SavePattern;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.input.DesktopInput;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.ui.dialogs.PatternsDialog;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Timer;
import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class CopyPastaFragment extends Fragment{
    public SavePattern pwin = new SavePattern();
    private Table content = new Table();
    private Timer timer = new Timer();
    public String fileName = "";
    boolean up = true;
    public boolean mouseOver = false;
    public boolean noRotate = false;

    PatternsDialog patternsDialog = new PatternsDialog();
    @Override
    public void build(Group parent){
        parent.fill(cont -> {
            up = false;
            cont.center().bottom();
            cont.update(() -> {
                if (pwin.discordDownloaded) pwin.discordDownloaded = false;

                if(timer.get(10)){
                    rebuild();
                    content.pack();
                    content.act(Gdx.graphics.getDeltaTime());
                    Core.scene.act(0f);

                }
                if (!ui.chatfrag.chatOpen()){
                    if((Inputs.keyTap("copyMode") || Inputs.keyTap("quickPaste") || Inputs.keyTap("pasteMode"))){
                        up = true;
                        updateAction(cont);
                    }
                }
                if (!state.is(State.playing)){
                    up = false;
                    updateAction(cont);
                }
                if (Inputs.keyTap("openPastaMenu")){
                    up = !up;
                    updateAction(cont);
                }
            });

            cont.hovered(()->{
                mouseOver = true;
            });


            cont.exited(()->{
                mouseOver = false;
            });

            cont.table("button", pane -> {
                pane.pane(content).left();
                pane.marginTop(8);
                pane.align(Align.left);
                pane.margin(16);
                pane.defaults().padRight(5);
                pane.row().left();

                /*
                pane.addImageButton("icon-rotate-left", "clear-partial", 32f, () -> {
                    players[0].rotateStoredLeft();
                });

                pane.addImageButton("icon-rotate-right", "clear-partial", 32f, () -> {
                    players[0].rotateStoredRight();
                });*/

                pane.addImageButton("icon-pick", "clear-toggle-partial", 32f, () -> {
                    ((DesktopInput) control.input(0)).copyMode = !((DesktopInput) control.input(0)).copyMode;//true;
                    ((DesktopInput) control.input(0)).mode = PlaceMode.none;
                }).update(c -> {
                    c.setChecked(((DesktopInput) control.input(0)).copyMode);
                });

                pane.addImageButton("icon-logic", "clear-toggle-partial", 32f, () -> {
                    Timers.run(10, ()->{
                        if(((DesktopInput) control.input(0)).mode == PlaceMode.pasting)
                            ((DesktopInput) control.input(0)).mode = PlaceMode.none;
                        else if (players[0].storedQue!=null&&players[0].storedQue.size!=0) ((DesktopInput) control.input(0)).mode = PlaceMode.pasting;
                        ((DesktopInput) control.input(0)).recipe = null;
                    });
                }).update(c -> {
                        c.setChecked(PlaceMode.pasting == ((DesktopInput) control.input(0)).mode);

                });

                /*fileName = new TextField("pattern1", text -> {
                }).padLeft(10).size(170f, 32f);
                fileName.setFocusTraversal(false);
                fileName.setDisabled(true);

                fileName.hovered(() -> {
                    fileName.setDisabled(false);
                });
                fileName.exited(() -> {
                    if(!Inputs.keyRelease(Input.MOUSE_LEFT)){
                        fileName.setDisabled(true);
                    }
                });*/

                pane.addImageButton("icon-floppy", "clear-partial", 32f, () -> {
                    noRotate = true;
                        ui.showTextInputOption("Pattern name","Name:", "",e->{
                            savePattern();
                            boolean useDiscord = e.contains("discord|");
                            if (useDiscord) fileName = e.replace("discord|","");
                            else fileName = e;
                            ((DesktopInput) control.input(0)).mode = PlaceMode.none;
                            ((DesktopInput) control.input(0)).copyMode = false;
                            noRotate = false;
                            up = true;
                            updateAction(cont);
                            pwin.upload = useDiscord;
                            //ui.showConfirm("Upload pattern" , "Pattern " + fileName + " saved. Would you like to upload  to discord as well (only good submissions will be kept)?", ()->{

                        //});
                    });

                });

                /*
                pane.addImageButton("icon-load", "clear-partial", 32f, () -> {
                    ((DesktopInput) control.input(0)).mode = PlaceMode.none;
                    File file = getFileImg();
                    if (file.exists()) loadPattern(file);
                    else ui.showInfoFade2(file.getAbsolutePath() + " not found");
                });*/

                pane.addButton("...", "clear-partial-2", patternsDialog::show).width(40);

                /*pane.addButton("?", "clear-partial-2", () -> {
                    new FloatingDialog("instructions"){{
                        shouldPause = true;
                        setFillParent(false);
                        getCell(content()).growX();
                        content().add("test line").width(400f);
                        content().addRowImageTextButton("rotate left","icon-rotate-left",32f,null);
                        content().addRowImageTextButton("rotate right","icon-rotate-right",32f,null);
                        content.row();
                        content().margin(15).add("test line2").width(400f);
                        content.row();
                        content().add("test line3").width(400f);
                        content().addImage("icon-rotate-right");
                        content().add("test line4").width(400f);
                        buttons().addButton("$text.continue", this::hide).size(140, 60).pad(4);
                    }}.show();
                }).width(32);*/
            });
        });
        rebuild();
    }

    void savePattern() {
        try{
            Queue<BuildRequest> pattern = new Queue<>();
            for(BuildRequest br : players[0].storedQue) pattern.addLast(br);
            pwin.pattern = pattern;
            pwin.doRender = true;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public File getFile(){
        return new File(dataDirectory + "/patterns/" + fileName + ".mpat");
    }

    public File getFileImg(){
        return new File(dataDirectory + "/patterns/" + fileName + ".png");
    }

    public void loadPattern(File file){
        ((DesktopInput) control.input(0)).recipe = null;
        ((DesktopInput) control.input(0)).mode = null;
        BufferedReader reader;
        try{
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            if(players[0].storedQue == null) players[0].storedQue = new Queue<>();
            players[0].storedQue.clear();
            if(file.getAbsolutePath().contains(".png")){
                while(line != null && !line.contains("MPAT")){
                    line = reader.readLine();
                }
                line = reader.readLine();
                while(line != null){
                    String split[] = line.split(",");
                    if(split.length == 4){
                        int x = Integer.parseInt(split[0]), y = Integer.parseInt(split[1]), rot = Integer.parseInt(split[2]);
                        Block b = Vars.content.getByName(ContentType.block, split[3]);
                        if(b != null){
                            players[0].storedQue.addLast(new BuildRequest(x, y, rot, Recipe.getByResult(b)));
                            System.out.println(line + " | " + players[0].storedQue.size);
                            line = reader.readLine();
                        }else ui.hudfrag.showToast("couldn't find block, " + split[3]);
                    }else ui.hudfrag.showToast("bad line parse, " + split.length);
                }
                ui.hudfrag.showToast("Loaded " + file.getName() + " - " + players[0].storedQue.size + " blocks");
                fileName = (file.getName().replaceAll(".png",""));
                Timers.run(10f,()->{((DesktopInput) control.input(0)).mode = PlaceMode.pasting;});
            }
            reader.close();

        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void rebuild(){
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

    public void toggle(Table cont){

        up = !up;
        updateAction(cont);
    }

    void updateAction(Table cont){
        cont.clearActions();
        float dur = 0.3f;
        Interpolation in = Interpolation.pow3Out;
        if(up)cont.actions(Actions.moveTo(0, -5, dur, in));
        else cont.actions(Actions.moveTo(0,-100, dur, in));
    }
}
