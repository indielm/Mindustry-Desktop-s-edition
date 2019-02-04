package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.input.DesktopInput;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.maps.MapMeta;
import io.anuke.mindustry.maps.MapTileData;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.ImageTextButton;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.utils.UIUtils;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Strings;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static io.anuke.mindustry.Vars.*;

public class PatternsDialog extends FloatingDialog{
    private FloatingDialog dialog;
    String path;
    public PatternsDialog(){
        super("$text.patterns");
        path = dataDirectory.path() + "/patterns/";
        //addCloseButton();
        shown(this::setup);
        onResize(() -> {
            if(dialog != null){
                dialog.hide();
            }
        });
    }



    void setup(){
        File f = new File(path);
        f.mkdir();
        content().clear();

        Table table = new Table();
        table.marginRight(24);

        ScrollPane pane = new ScrollPane(table);
        pane.setFadeScrollBars(false);

        float maxwidth = Gdx.graphics.getWidth()*0.48f;

        HashMap<String,Texture> previews = new HashMap<>();
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        int widthAccum = 0;
        for (File pfile : listOfFiles){

            if (pfile.isFile()){

                String patName = pfile.getName().replace(".png", "");
                Texture t = new Texture(pfile.getAbsolutePath());
                widthAccum += t.getWidth()+64;
                previews.put(pfile.getName(), t);

                if(widthAccum >= maxwidth){
                    table.row();
                    widthAccum = 0;
                }
                TextButton button = table.addButton("", "clear", () ->{
                    ui.copypastafrag.loadPattern(pfile);

                    hide();
                }).width(previews.get(pfile.getName()).getWidth()).pad(8).get();
                button.clearChildren();
                //button.margin(9);
               button.add(patName).growX().center().get().setEllipsis(true);

                button.row();
                button.addImage("white").growX().pad(1).color(Color.GRAY);
                button.row();
                button.stack(new Image(t));//.setScaling(Scaling.fit), new BorderImage(t).setScaling(Scaling.fit));//.size(mapsize - 20f);
                button.row();
            }
            else {
                widthAccum += maxwidth;
                table.addImageTextButton(pfile.getName(), "icon-folder", 64, ()->{
                    path = pfile.getAbsolutePath();//dataDirectory.path() + "/patterns/";
                    setup();
                    //hide();
                } ).size(330f, 200f).pad(8).get();
                    /*
                    path = dataDirectory.path() + "/patterns/";
                    hide();
                } ).size(230f, 64f).get();
                    //ui.copypastafrag.loadPattern(pfile);
                    path = pfile.getAbsolutePath();
                    setup();
                    //hide();

                }).width(200).height(32).pad(8).get();*/

            }
        }
        //content().top().center();
        content().center();
        //content().debug();
        Table buttons = new Table();
        content().add(path).row();
        buttons.addImageTextButton("$text.back", "icon-arrow-left", 30f, ()->{
            path = dataDirectory.path() + "/patterns/";
            hide();
        } ).size(130f, 48f).padRight(24f);
        buttons.addImageTextButton("Download discord repository", "icon-discord", 30f, ()->{
            ui.showInfoFade2("Downloading repo...");
            ui.copypastafrag.pwin.getDiscordPatterns();
            path = dataDirectory.path() + "/patterns/";
            hide();
        } ).size(380f, 48f).padRight(24f);
        buttons.addImageTextButton("Open discord folder", "icon-discord", 30f, ()->{
            path = dataDirectory.path() + "/patterns/discordCache/";
            File fp = new File(path);
            if (!(fp.exists())) fp.mkdir();
            setup();
            //hide();
        } ).size(380f, 48f);

        content().add(buttons);
        content().row();

        content().add(pane).uniformX();



    }
}
