package io.anuke.mindustry.core;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.ui.GridImage;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.graphics.Draw;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.restaction.pagination.MessagePaginationAction;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;
import processing.opengl.FrameBuffer;
import processing.opengl.PGraphicsOpenGL;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static io.anuke.mindustry.Vars.*;

public class SavePattern extends PApplet {
    PImage spriteSheet;
    int w = 0, h = 0,  w2 = 0, h2 = 0;
    int scl = 2;
    PGraphics pg;
    boolean ready = false;
    public boolean doRender= false, upload = false;
    HashMap<String,PImage> sprites;
    TextureRegion region;
    int k = 0;
    public Queue<BuildRequest> pattern;
    JDA bot;
    PImage america;
    PShape cube;
    public boolean discordDownloaded = false;
    public SavePattern(){
        super();
        PApplet.runSketch(new String[]{this.getClass().getSimpleName()+round(random(1000))}, this);

    }

    public void settings(){
        size(100,100,P3D);
        noSmooth();
    }

    String tokenEnc = "noToken4u";

    int RGBAtoARGB(int c){
        return color(alpha(c),red(c),green(c),blue(c));
    }

    public void setup(){

        Texture t = Core.atlas.getTextures().first();
        t.getTextureData().prepare();

        spriteSheet = createImage(t.getWidth(),t.getHeight(),ARGB);
        Pixmap px = t.getTextureData().consumePixmap();
        for (int x = 0; x < spriteSheet.width; x++){
            for (int y = 0; y < spriteSheet.height; y++){
                spriteSheet.set(x,y,RGBAtoARGB(px.getPixel(x,y)));
            }
        }
        spriteSheet.updatePixels();

        ready = true;
        pg  = createGraphics(w*tilesize*scl, h*tilesize*scl,P2D);
        ((PGraphicsOpenGL)pg).textureSampling(2);
        ((PGraphicsOpenGL)g).textureSampling(3);
        sprites = new HashMap<>();
        frameRate(15);
        //https://imgoat.com/uploads/d686fd640b/192411.jpg

        america = requestImage("https://imgoat.com/uploads/d686fd640b/192411.jpg", "jpg");
        initCube();

    }

    public void findSizes(){
        float lx=10000,hx=-10000,ly=10000,hy=-10000;
        for (BuildRequest br : pattern){
            float x = br.x;//+br.recipe.result.size*4;
            float y = br.y;//+br.recipe.result.size*4;
            if (x>hx) hx = x;
            if (x<lx) lx = x;
            if (y>hy) hy = y;
            if (y<ly) ly = y;
        }
        w = ceil(hx-lx)+4;
        h = ceil(hy-ly)+4;
        w2 = w/2;
        h2 = h/2;
    }

    public void init(){

    }

    public void draw(){

        clear();
        image(pg,0,0);
        if (doRender) render();
        noLights();
        if (k==0){
            lights();
            translate(width / 2, height / 2);

            rotateX(frameCount * 0.015f);
            rotateY(frameCount * 0.023f);
            //rotateZ(frameCount * 0.019f);
            texture(getBlockIconSprite("router"));
            scale(width / (6+sin(frameCount*0.1f)*2));
            noStroke();
        }
        shape(cube);
        //box(width/3);
        if (upload){
           uploadPattern();
        }
    }

    void uploadPattern(){
        File imgFile = ui.copypastafrag.getFileImg();
        try{
            bot = new JDABuilder(rot13(tokenEnc)).build();
            bot.awaitReady();
        }
        catch(Exception e){
            e.printStackTrace();
        }


        upload = false;
        MessageChannel tc = bot.getTextChannelById("541161587583746088");

        Message msg;
        try{
            String name = players[0].name;
            String split[] = name.split("\\[");
            name = "";
            for (String s : split){
                if (!s.contains("]")){
                    name+=s;
                }
                else {
                    String split2[] = s.split("\\]");
                    if (split2.length>1){
                        name+=split2[1];
                    }
                }
            }
            msg = tc.sendMessage(name + " - " + ui.copypastafrag.fileName).addFile(imgFile, ui.copypastafrag.fileName + ".png").complete();
        }
        catch(Exception e){
            e.printStackTrace();
        }

        MessageHistory  hist = tc.getHistory();
        hist.retrievePast(100);
        for (Message msgs : hist.getRetrievedHistory()){
            List<Attachment> attachments = msgs.getAttachments();
            String f = dataDirectory + "/discord/"  + attachments.get(0).getFileName();
            File file = new File(f);
            attachments.get(0).download(file);
        }
        bot.shutdown();
        ui.showInfoFade2("Pattern " +  imgFile.getName() + " uploaded to discord.");
    }

    PImage getBlockIconSprite(String contentName){
        if (!sprites.containsKey(contentName)){
            region = Core.atlas.getRegion("block-icon-" + contentName);
            sprites.put(contentName, spriteSheet.get(region.getRegionX(),region.getRegionY(), region.getRegionWidth(), region.getRegionHeight()));
        }
        return sprites.get(contentName);
    }

    public void render(){
        k++;
        doRender = false;
        findSizes();
        pg  = createGraphics(w*tilesize*scl+tilesize, h*tilesize*scl+tilesize,P2D);
       // pg.noSmooth();
        ((PGraphicsOpenGL)pg).textureSampling(2);
        if (pg == null){
            println("why is this");
            return;
        }
        pg.beginDraw();
        pg.translate(scl*w2*tilesize+tilesize,scl*(h2)*tilesize+tilesize);
        System.out.println(mouseX + " " + w2 + " " + mouseY + " " + h2);
        pg.scale(scl,scl);
        pg.imageMode(CENTER);
        pg.translate(1,1);
        for (BuildRequest br : pattern){
            float xo = 0.5f, yo = 0.5f;
            if (br.recipe.result.size == 4 || br.recipe.result.size==2){
                yo+=0.5f;
                xo-=0.5f;
            }

            pg.pushMatrix();
            pg.translate((br.x-xo)*tilesize,(-br.y-yo)*tilesize);
            pg.rotate(br.recipe.result.rotate ? br.rotation * HALF_PI : 0);
            pg.image(getBlockIconSprite(br.recipe.getContentName()),0,0);
            pg.popMatrix();
        }
        pg.endDraw();
        System.out.println("saving to " + ui.copypastafrag.getFileImg().getAbsolutePath());
        surface.setSize(max(100,pg.width),max(100,pg.height));
        File imgFile = ui.copypastafrag.getFileImg();
        pg.save(imgFile.getAbsolutePath());

        try(FileWriter fw = new FileWriter(imgFile.getAbsolutePath(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println();
            out.println("MPAT");
            for (BuildRequest br : pattern){
                String line = br.x + "," + br.y + "," + br.rotation + "," + br.recipe.result.name;
                out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void getDiscordPatterns(){
        thread("discordThread");
    }
    String lastID = "";
    public void discordThread(){
        try{
            bot = new JDABuilder(rot13(tokenEnc)).build();
            bot.awaitReady();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        MessageChannel tc = bot.getTextChannelById("541161587583746088");
        tc.getLatestMessageId();
        if (tc.getLatestMessageId().equals(lastID)){
            discordDownloaded = true;
            return;
        }
        System.out.println(lastID + " " + tc.getLatestMessageId());
        lastID = tc.getLatestMessageId();
        MessagePaginationAction mpa = tc.getIterableHistory();
        List<Message> mg = mpa.complete();


        System.out.println("found messages: " + mg.size());
        //bot.addEventListener(new MyListener());
        File dir = new File(dataDirectory + "/patterns/discordCache/");
        dir.mkdirs();
        int c = 0;
        try{
            File folder = new File(dataDirectory.path() + "/patterns/discordCache/");
            File[] listOfFiles = folder.listFiles();
            String names = "";
            for (File f : listOfFiles) names += " " + f.getName();
            for(Message m : mg){
                List<Attachment> a = m.getAttachments();
                if(a.size() > 0){
                    File img = new File(dataDirectory + "/patterns/discordCache/" + m.getContentRaw() + ".png");// a.get(0).getFileName());
                    if(names.contains(img.getName())) System.out.println("already have " + img.getName());
                    else{
                        if(img.exists()) img.delete();
                        a.get(0).download(img);
                        c++;
                        System.out.println("downloaded " + img.getCanonicalFile());
                        ui.showInfoFade3(m.getContentRaw());
                    }
                }
                System.out.println(m.getContentRaw());

                //ui.showInfoFade( "downloaded" + m.getContentRaw());
            }

        }
        catch(Exception e){
            e.printStackTrace();
        }
        bot.shutdown();
        ui.showInfoFade2( "downloaded " + c + " patterns");
        discordDownloaded = true;
    }

    public class MyListener extends ListenerAdapter
    {
        @Override
        public void onMessageReceived(MessageReceivedEvent event)
        {
            if (event.getAuthor().isBot()) return;
            // We don't want to respond to other bot accounts, including ourself
            Message message = event.getMessage();
            String content = message.getContentRaw();
            // getContentRaw() is an atomic getter
            // getContentDisplay() is a lazy getter which modifies the content for e.g. console view (strip discord formatting)
            //if (content.equals("!ping"))
            //{
            System.out.println(content);
                //MessageChannel channel = event.getChannel();
                //channel.sendMessage("Pong!").queue(); // Important to call .queue() on the RestAction returned by sendMessage(...)
            //}
        }
    }

    public static String rot13(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if       (c >= 'a' && c <= 'm') c += 13;
            else if  (c >= 'A' && c <= 'M') c += 13;
            else if  (c >= 'n' && c <= 'z') c -= 13;
            else if  (c >= 'N' && c <= 'Z') c -= 13;
            sb.append(c);
        }
        return sb.toString();
    }
    void gdxSave2(){
        //com.badlogic.gdx.graphics.glutils.FrameBuffer buffer = new com.badlogic.gdx.graphics.glutils.FrameBuffer(Format.RGBA8888, pg.width/2,pg.height/2, false);

    }
    void gdxSave(){
        Pixmap test = new Pixmap(pg.width/2,pg.height/2, Format.RGBA8888);
        //SpriteBatch btest = new SpriteBatch();
        //btest.begin();
        for (BuildRequest br : pattern){
            TextureRegion tr = Core.atlas.getRegion("block-icon-" + br.recipe.result.getContentName());
            int rot = br.recipe.result.rotate ? br.rotation * 90 : 0;
            tr.getTexture().getTextureData().prepare();
            test.drawPixmap(tr.getTexture().getTextureData().consumePixmap(),(w2+br.x)*tilesize,(h2-br.y)*tilesize, tr.getRegionX(), tr.getRegionY(),tr.getRegionWidth(),tr.getRegionHeight());

        }
        //btest.end();
        //test.drawPixMap(btest.)/
        //test.drawPixmap(btest,0,0);
        //GridImage image = new GridImage(0, 0);
        //image.draw(btest,1);

        //BufferUtils.copy(lines, 0, fullPixmap.getPixels(), lines.length);
        FileHandle file = screenshotDirectory.child("TESTING-" + TimeUtils.millis() + ".png");
        PixmapIO.writePNG(file, test);
        test.dispose();
    }

    void initCube() {
        cube = createShape(GROUP);
        PShape routers = createShape();
        routers.beginShape(QUADS);
        routers.noStroke();
        routers.texture(america);
        routers.texture(getBlockIconSprite("router"));
        routers.textureMode(NORMAL);

        routers.vertex(-1, -1,  1, 0, 0);
        routers.vertex( 1, -1,  1, 1, 0);
        routers.vertex( 1,  1,  1, 1, 1);
        routers.vertex(-1,  1,  1, 0, 1);

        // -Z "back" face
        routers.vertex( 1, -1, -1, 0, 0);
        routers.vertex(-1, -1, -1, 1, 0);
        routers.vertex(-1,  1, -1, 1, 1);
        routers.vertex( 1,  1, -1, 0, 1);

        // +Y "bottom" face
        routers.vertex(-1,  1,  1, 0, 0);
        routers.vertex( 1,  1,  1, 1, 0);
        routers.vertex( 1,  1, -1, 1, 1);
        routers.vertex(-1,  1, -1, 0, 1);

        // -Y "top" face
        routers.vertex(-1, -1, -1, 0, 0);
        routers.vertex( 1, -1, -1, 1, 0);
        routers.vertex( 1, -1,  1, 1, 1);
        routers.vertex(-1, -1,  1, 0, 1);

        // +X "right" face
        routers.vertex( 1, -1,  1, 0, 0);
        routers.vertex( 1, -1, -1, 1, 0);
        routers.vertex( 1,  1, -1, 1, 1);
        routers.vertex( 1,  1,  1, 0, 1);

        // -X "left" face
        routers.endShape();
        cube.addChild(routers);

        PShape a = createShape();
        a.beginShape(QUAD);
        a.textureMode(NORMAL);
        a.texture(america);
            a.texture(america);

             a.vertex(-1, -1, -1, 0, 0);
        a.vertex(-1, -1,  1, 1, 0);
        a.vertex(-1,  1,  1, 1, 1);
        a.vertex(-1,  1, -1, 0, 1);
        a.endShape();
        cube.addChild(a);
    }
}