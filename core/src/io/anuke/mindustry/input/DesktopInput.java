package io.anuke.mindustry.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.Recipes;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.DistributionBlocks;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.input.PlaceUtils.NormalizeDrawResult;
import io.anuke.mindustry.input.PlaceUtils.NormalizeResult;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Category;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.ui.fragments.CopyPastaFragment;
import io.anuke.mindustry.ui.fragments.PlacementFragment;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Build;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.*;
import io.anuke.ucore.core.Inputs.DeviceType;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.input.Input;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Mathf;

import static com.badlogic.gdx.math.MathUtils.random;
import static com.badlogic.gdx.math.MathUtils.round;
import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.input.CursorType.*;
import static io.anuke.mindustry.input.PlaceMode.*;

public class DesktopInput extends InputHandler {
    private final String section;
    //controller info
    private float controlx, controly;
    private boolean controlling;
    public boolean copyMode = false;
    /**
     * Current cursor type.
     */
    private CursorType cursorType = normal;

    /**
     * Position where the player started dragging a line.
     */
    private int selectX, selectY;
    /**
     * Whether selecting mode is active.
     */
    public PlaceMode mode;
    /**
     * Animation scale for line.
     */
    private float selectScale;

    public DesktopInput(Player player) {
        super(player);
        this.section = "player_" + (player.playerIndex + 1);
    }

    /**
     * Draws a placement icon for a specific block.
     */
    void drawPlace(int x, int y, Block block, int rotation) {
        if (validPlace(x, y, block, rotation)) {
            Draw.color();

            TextureRegion[] regions = block.getBlockIcon();

            for (TextureRegion region : regions) {
                Draw.rect(region, x * tilesize + block.offset(), y * tilesize + block.offset(),
                region.getRegionWidth() * selectScale, region.getRegionHeight() * selectScale, block.rotate ? rotation * 90 : 0);
            }
        } else {
            Draw.color(Palette.removeBack);
            Lines.square(x * tilesize + block.offset(), y * tilesize + block.offset() - 1, block.size * tilesize / 2f);
            Draw.color(Palette.remove);
            Lines.square(x * tilesize + block.offset(), y * tilesize + block.offset(), block.size * tilesize / 2f);
        }
    }

    @Override
    public boolean isDrawing() {
        return mode != none || recipe != null;
    }

    @Override
    public void drawOutlined() {
        int cursorX = tileX(Gdx.input.getX());
        int cursorY = tileY(Gdx.input.getY());

        //draw selection(s)
        if (mode == placing && recipe != null) {
            NormalizeResult result = PlaceUtils.normalizeArea(selectX, selectY, cursorX, cursorY, rotation, true, maxLength);

            for (int i = 0; i <= result.getLength(); i += recipe.result.size) {
                int x = selectX + i * Mathf.sign(cursorX - selectX) * Mathf.bool(result.isX());
                int y = selectY + i * Mathf.sign(cursorY - selectY) * Mathf.bool(!result.isX());

                if (i + recipe.result.size > result.getLength() && recipe.result.rotate) {
                    Draw.color(!validPlace(x, y, recipe.result, result.rotation) ? Palette.remove : Palette.placeRotate);
                    Draw.grect("place-arrow", x * tilesize + recipe.result.offset(),
                    y * tilesize + recipe.result.offset(), result.rotation * 90 - 90);
                }

                drawPlace(x, y, recipe.result, result.rotation);
            }

            Draw.reset();
        } else if (mode == breaking) {
            NormalizeDrawResult result = PlaceUtils.normalizeDrawArea(Blocks.air, selectX, selectY, cursorX, cursorY, false, maxLength, 1f);
            NormalizeResult dresult = PlaceUtils.normalizeArea(selectX, selectY, cursorX, cursorY, rotation, false, maxLength);

            for (int x = dresult.x; x <= dresult.x2; x++) {
                for (int y = dresult.y; y <= dresult.y2; y++) {
                    Tile tile = world.tile(x, y);
                    if (tile == null || !validBreak(tile.x, tile.y)) continue;
                    tile = tile.target();

                    Draw.color(Palette.removeBack);
                    Lines.square(tile.drawx(), tile.drawy() - 1, tile.block().size * tilesize / 2f - 1);
                    Draw.color(Palette.remove);
                    Lines.square(tile.drawx(), tile.drawy(), tile.block().size * tilesize / 2f - 1);
                }
            }

            Draw.color(Palette.removeBack);
            Lines.rect(result.x, result.y - 1, result.x2 - result.x, result.y2 - result.y);
            Draw.color(Palette.remove);
            Lines.rect(result.x, result.y, result.x2 - result.x, result.y2 - result.y);
        } else if (mode == copying) {
            NormalizeDrawResult result = PlaceUtils.normalizeDrawArea(Blocks.air, selectX, selectY, cursorX, cursorY, false, maxLength, 1f);
            NormalizeResult dresult = PlaceUtils.normalizeArea(selectX, selectY, cursorX, cursorY, rotation, false, maxLength);

            for (int x = dresult.x; x <= dresult.x2; x++) {
                for (int y = dresult.y; y <= dresult.y2; y++) {
                    Tile tile = world.tile(x, y);
                    if (tile == null || !Build.validBreak(player.getTeam(), x, y)) continue;
                    tile = tile.target();
                    Draw.color(Color.SCARLET);
                    Lines.square(tile.drawx(), tile.drawy() - 1, tile.block().size * tilesize / 2f - 1);
                    Draw.color(Color.WHITE);
                    Lines.square(tile.drawx(), tile.drawy(), tile.block().size * tilesize / 2f - 1);
                }
            }

            Draw.color(Color.SCARLET);
            Lines.rect(result.x, result.y - 1, result.x2 - result.x, result.y2 - result.y);
            Draw.color(Color.WHITE);
            Lines.rect(result.x, result.y, result.x2 - result.x, result.y2 - result.y);
        }else if (isPlacing()) {
            if (recipe.result.rotate) {
                Draw.color(!validPlace(cursorX, cursorY, recipe.result, rotation) ? Palette.remove : Palette.placeRotate);
                Draw.grect("place-arrow", cursorX * tilesize + recipe.result.offset(),
                cursorY * tilesize + recipe.result.offset(), rotation * 90 - 90);
            }
            drawPlace(cursorX, cursorY, recipe.result, rotation);
            recipe.result.drawPlace(cursorX, cursorY, rotation, validPlace(cursorX, cursorY, recipe.result, rotation));
        }

        Draw.reset();
    }

    public boolean isCopying(){
        return (mode == copying);
    }


    @Override
    public void update() {
        if(mode==pasting) players[0].freecam = true;
        if (Net.active() && Inputs.keyTap("player_list")) {
            ui.listfrag.toggle();
        }

        if (!ui.chatfrag.chatOpen() && (state.is(State.playing))) {
            if(Inputs.keyTap("storeQue")) players[0].storeQue();
            if(Inputs.keyTap("recallQue")) players[0].recallQue();
            if(Inputs.keyTap("copyMode")){
                if(mode == breaking) mode = copying;
                else if(mode == copying) mode = breaking;
                else copyMode = !copyMode;
            }
            if(Inputs.keyTap("pasteMode")){
                if (players[0].storedQue==null||players[0].storedQue.size==0 || mode == pasting) mode = none;
                else mode = pasting;
            }
            if( Inputs.keyRelease("quickPaste")) players[0].pasteQueue();
            if(Inputs.keyTap("toggleSortQueue")){
                players[0].sortQueue = !players[0].sortQueue;
                ui.showInfo("Auto queue sort " + (players[0].sortQueue ? "enabled" : "disabled"));

            }
            if(Inputs.keyTap("sortQueue")){
                players[0].sortQueue();
            }
            if(Inputs.keyTap("reverseQueue")) players[0].reverseQueue();
        }

        if (Inputs.keyRelease(section, "select")) {
            player.isShooting = false;
        }

        if (state.is(State.menu) || ui.hasDialog()) return;

        boolean controller = KeyBinds.getSection(section).device.type == DeviceType.controller;

        //zoom and rotate things
        if (Inputs.getAxisActive("zoom") && (Inputs.keyDown(section, "zoom_hold") || controller)) {
            renderer.scaleCamera((int) Inputs.getAxisTapped(section, "zoom"));
        }

        renderer.minimap.zoomBy(-(int) Inputs.getAxisTapped(section, "zoom_minimap"));

        if (player.isDead()) return;

        pollInput();

        //deselect if not placing
        if (!isPlacing() && mode == placing) {
            mode = none;
        }

        if (player.isShooting && !canShoot()) {
            player.isShooting = false;
        }

        if (isPlacing()) {
            cursorType = hand;
            selectScale = Mathf.lerpDelta(selectScale, 1f, 0.2f);
        } else {
            selectScale = 0f;
        }

        rotation = Mathf.mod(rotation + (int) Inputs.getAxisTapped(section, "rotate"), 4);

        Tile cursor = tileAt(Gdx.input.getX(), Gdx.input.getY());

        if (player.isDead()) {
            cursorType = normal;
        } else if (cursor != null) {
            cursor = cursor.target();

            cursorType = cursor.block().getCursor(cursor);

            if (isPlacing()) {
                cursorType = hand;
            }

            if (!isPlacing() && canMine(cursor)) {
                cursorType = drill;
            }

            if (canTapPlayer(Graphics.mouseWorld().x, Graphics.mouseWorld().y)) {
                cursorType = unload;
            }
        }

        if (!ui.hasMouse()) {
            cursorType.set();
        }

        cursorType = normal;
    }


    void pollInput() {
        if (ui.copypastafrag.mouseOver) return;
        Tile selected = tileAt(Gdx.input.getX(), Gdx.input.getY());
        int cursorX = tileX(Gdx.input.getX());
        int cursorY = tileY(Gdx.input.getY());

        //gridControls();

        if (Inputs.keyTap(section, "deselect")) {
            player.setMineTile(null);
        }

        if (Inputs.keyTap("snekMode")) { //TODO this doesn't work, should seamlessly swap between the two modes
            if (mode == placing){
                Tile start = world.tileWorld(selectX,selectY);
                if (start.block()!=null){
                    ui.hudfrag.blockfrag.flowStart = start;
                    ui.hudfrag.blockfrag.snekStart();
                }
            }
            mode = none;
        }

        if ((Inputs.keyTap(section, "select") && !ui.hasMouse()) || (Inputs.keyRelease("snekMode")&&Inputs.keyDown("select"))) {

            if (isPlacing() && (!Inputs.keyDown("snekMode"))) {
                selectX = cursorX;
                selectY = cursorY;

                mode = placing;
            } else if (selected != null) {
                //only begin shooting if there's no cursor event
                if (!tileTapped(selected) && !tryTapPlayer(Graphics.mouseWorld().x, Graphics.mouseWorld().y) && player.getPlaceQueue().size == 0 && !droppingItem &&
                !tryBeginMine(selected) && player.getMineTile() == null) {
                    player.isShooting = true;
                }
            } else { //if it's out of bounds, shooting is just fine
                player.isShooting = true;
            }
        } else if (Inputs.keyTap(section, "deselect") && (recipe != null || mode != none || player.isBuilding()) &&
        !(player.getCurrentRequest() != null && player.getCurrentRequest().breaking && KeyBinds.get(section, "deselect") == KeyBinds.get(section, "break"))) {
            if (recipe == null) {
                if (mode==pasting)mode= none;
                else player.clearBuilding();
            }
            recipe = null;
            mode = none;
        } else if (Inputs.keyTap(section, "break") && !ui.hasMouse()) {
            //is recalculated because setting the mode to breaking removes potential multiblock cursor offset
            if (copyMode) {
                System.out.println("copying");
                mode = copying;
                copyMode = false;
            }
            else mode = breaking;
            selectX = tileX(Gdx.input.getX());
            selectY = tileY(Gdx.input.getY());
        }

        renderer.overlays.drawingPastePreview = (mode==pasting);

        if (Inputs.keyRelease(section, "break") || Inputs.keyRelease(section, "select")) {
            if (mode == placing) { //touch up while placing, place everything in selection
                NormalizeResult result = PlaceUtils.normalizeArea(selectX, selectY, cursorX, cursorY, rotation, true, maxLength);
                if (recipe!= null && recipe.result!=null){
                    for(int i = 0; i <= result.getLength(); i += recipe.result.size){
                        int x = selectX + i * Mathf.sign(cursorX - selectX) * Mathf.bool(result.isX());
                        int y = selectY + i * Mathf.sign(cursorY - selectY) * Mathf.bool(!result.isX());
                        rotation = result.rotation;
                        tryPlaceBlock(x, y);
                    }
                }
            } else if (mode == breaking) { //touch up while breaking, break everything in selection
                NormalizeResult result = PlaceUtils.normalizeArea(selectX, selectY, cursorX, cursorY, rotation, false, maxLength);
                for (int x = 0; x <= Math.abs(result.x2 - result.x); x++) {
                    for (int y = 0; y <= Math.abs(result.y2 - result.y); y++) {
                        int wx = selectX + x * Mathf.sign(cursorX - selectX);
                        int wy = selectY + y * Mathf.sign(cursorY - selectY);
                        tryBreakBlock(wx, wy);
                    }
                }
            }
            else if (mode == copying) { //touch up while breaking, break everything in selection
                NormalizeResult result = PlaceUtils.normalizeArea(selectX, selectY, tileX(Gdx.input.getX()), tileY(Gdx.input.getY()), rotation, false, maxLength);
                players[0].copyArea(result.x,result.y,result.x2,result.y2);
                ui.copypastafrag.fileName = ("pattern"+round(random(9999)));
                copyMode = false;
                if (players[0].storedQue.size >0) mode = pasting;
            }
            else if (mode == pasting){
                if (Inputs.keyRelease(section, "select")){
                    player.pasteQueue();
                }
                if (!Inputs.keyDown("multipaste")) mode = none;
            }

            if (selected != null) {
                tryDropItems(selected.target(), Graphics.mouseWorld().x, Graphics.mouseWorld().y);
            }
            if (mode!=pasting) mode = none;
        }
    }

    @Override
    public boolean selectedBlock() {
        return isPlacing() && mode != breaking;
    }

    @Override
    public float getMouseX() {
        return !controlling ? Gdx.input.getX() : controlx;
    }

    @Override
    public float getMouseY() {
        return !controlling ? Gdx.input.getY() : controly;
    }

    @Override
    public boolean isCursorVisible() {
        return controlling;
    }

    @Override
    public void updateController() {
        //TODO no controller support
        //TODO move controller input to new class, ControllerInput
        boolean mousemove = Gdx.input.getDeltaX() > 1 || Gdx.input.getDeltaY() > 1;

        if (state.is(State.menu)) {
            droppingItem = false;
        }

        if (KeyBinds.getSection(section).device.type == DeviceType.controller && (!mousemove || player.playerIndex > 0)) {
            if (player.playerIndex > 0) {
                controlling = true;
            }

            float xa = Inputs.getAxis(section, "cursor_x");
            float ya = Inputs.getAxis(section, "cursor_y");

            if (Math.abs(xa) > controllerMin || Math.abs(ya) > controllerMin) {
                float scl = Settings.getInt("sensitivity", 100) / 100f * Unit.dp.scl(1f);
                controlx += xa * baseControllerSpeed * scl;
                controly -= ya * baseControllerSpeed * scl;
                controlling = true;
                if (player.playerIndex == 0) Gdx.input.setCursorCatched(true);
                Inputs.getProcessor().touchDragged((int) getMouseX(), (int) getMouseY(), player.playerIndex);
            }

            controlx = Mathf.clamp(controlx, 0, Gdx.graphics.getWidth());
            controly = Mathf.clamp(controly, 0, Gdx.graphics.getHeight());
        } else {
            controlling = false;
            Gdx.input.setCursorCatched(false);
        }

        if (!controlling) {
            controlx = Gdx.input.getX();
            controly = Gdx.input.getY();
        }
    }

}
