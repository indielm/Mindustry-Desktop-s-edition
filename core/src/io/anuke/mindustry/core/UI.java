package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;
import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.editor.MapEditorDialog;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.game.EventType.ResizeEvent;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.ui.dialogs.*;
import io.anuke.mindustry.ui.fragments.*;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.core.*;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.Skin;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.ui.CheckBox;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.TextField;
import io.anuke.ucore.scene.ui.TextField.TextFieldFilter;
import io.anuke.ucore.scene.ui.TooltipManager;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Strings;

import java.util.Arrays;
import java.util.List;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.scene.actions.Actions.*;

public class UI extends SceneModule{
    private FreeTypeFontGenerator generator;

    public final MenuFragment menufrag = new MenuFragment();
    public final HudFragment hudfrag = new HudFragment();
    public final ChatFragment chatfrag = new ChatFragment();
    public final PlayerListFragment listfrag = new PlayerListFragment();
    public final BackgroundFragment backfrag = new BackgroundFragment();
    public final LoadingFragment loadfrag = new LoadingFragment();
    public final GraphFragment graphfrag = new GraphFragment();
    public final QueueFragment queuefrag = new QueueFragment();
    public final CopyPastaFragment copypastafrag = new CopyPastaFragment();

    public AboutDialog about;
    public RestartDialog restart;
    public CustomGameDialog levels;
    public MapsDialog maps;
    public LoadDialog load;
    public DiscordDialog discord;
    public JoinDialog join;
    public HostDialog host;
    public PausedDialog paused;
    public SettingsMenuDialog settings;
    public ControlsDialog controls;
    public MapEditorDialog editor;
    public LanguageDialog language;
    public BansDialog bans;
    public AdminsDialog admins;
    public TraceDialog traces;
    public ChangelogDialog changelog;
    public LocalPlayerDialog localplayers;
    public UnlocksDialog unlocks;
    public ContentInfoDialog content;
    public SectorsDialog sectors;
    public MissionDialog missions;

    public UI(){
        Dialog.setShowAction(() -> sequence(
            alpha(0f),
            originCenter(),
            moveToAligned(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, Align.center),
            scaleTo(0.0f, 1f),
            parallel(
                scaleTo(1f, 1f, 0.1f, Interpolation.fade),
                fadeIn(0.1f, Interpolation.fade)
            )
        ));

        Dialog.setHideAction(() -> sequence(
            parallel(
                scaleTo(0.01f, 0.01f, 0.1f, Interpolation.fade),
                fadeOut(0.1f, Interpolation.fade)
            )
        ));

        TooltipManager.getInstance().animations = false;

        Settings.setErrorHandler(() -> Timers.run(1f, () -> showError("[crimson]Failed to access local storage.\nSettings will not be saved.")));

        Dialog.closePadR = -1;
        Dialog.closePadT = 5;

        Colors.put("accent", Palette.accent);
    }
    
    void generateFonts(){
        generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/pixel.ttf"));
        FreeTypeFontParameter param = new FreeTypeFontParameter();
        param.size = (int)(14*2 * Math.max(Unit.dp.scl(1f), 0.5f));
        param.shadowColor = Color.DARK_GRAY;
        param.shadowOffsetY = 2;
        param.incremental = true;

        skin.add("default-font", generator.generateFont(param));
        skin.add("default-font-chat", generator.generateFont(param));
        skin.getFont("default-font").getData().markupEnabled = true;
        skin.getFont("default-font").setOwnsTexture(false);
    }

    @Override
    protected void loadSkin(){
        skin = new Skin(Core.atlas);
        generateFonts();
        skin.load(Gdx.files.internal("ui/uiskin.json"));

        for(BitmapFont font : skin.getAll(BitmapFont.class).values()){
            font.setUseIntegerPositions(true);
            //font.getData().setScale(Vars.fontScale);
        }
    }

    @Override
    public void update(){
        if(disableUI) return;

        if(Graphics.drawing()) Graphics.end();

        act();

        Graphics.begin();

        for(int i = 0; i < players.length; i++){
            InputHandler input = control.input(i);

            if(input.isCursorVisible()){
                Draw.color();

                float scl = Unit.dp.scl(3f);

                Draw.rect("controller-cursor", input.getMouseX(), Gdx.graphics.getHeight() - input.getMouseY(), 16 * scl, 16 * scl);
            }
        }

        Graphics.end();
        Draw.color();
    }

    @Override
    public void init(){
        editor = new MapEditorDialog();
        controls = new ControlsDialog();
        restart = new RestartDialog();
        join = new JoinDialog();
        discord = new DiscordDialog();
        load = new LoadDialog();
        levels = new CustomGameDialog();
        language = new LanguageDialog();
        unlocks = new UnlocksDialog();
        settings = new SettingsMenuDialog();
        host = new HostDialog();
        paused = new PausedDialog();
        changelog = new ChangelogDialog();
        about = new AboutDialog();
        bans = new BansDialog();
        admins = new AdminsDialog();
        traces = new TraceDialog();
        maps = new MapsDialog();
        localplayers = new LocalPlayerDialog();
        content = new ContentInfoDialog();
        sectors = new SectorsDialog();
        missions = new MissionDialog();

        Group group = Core.scene.getRoot();

        backfrag.build(group);
        control.input(0).getFrag().build(Core.scene.getRoot());
        hudfrag.build(group);
        menufrag.build(group);
        chatfrag.container().build(group);
        listfrag.build(group);
        loadfrag.build(group);
        graphfrag.build(group);
        //queuefrag.build(group);
        copypastafrag.build(group);
    }

    @Override
    public void resize(int width, int height){
        super.resize(width, height);

        Events.fire(new ResizeEvent());
    }

    @Override
    public void dispose(){
        super.dispose();
        generator.dispose();
    }

    public void loadGraphics(Runnable call){
        loadGraphics("$text.loading", call);
    }

    public void loadGraphics(String text, Runnable call){
        loadfrag.show(text);
        Timers.runTask(7f, () -> {
            call.run();
            loadfrag.hide();
        });
    }

    public void loadLogic(Runnable call){
        loadLogic("$text.loading", call);
    }

    public void loadLogic(String text, Runnable call){
        loadfrag.show(text);
        Timers.runTask(7f, () ->
            threads.run(() -> {
                call.run();
                threads.runGraphics(loadfrag::hide);
            }));
    }

    public void showTextInput(String title, String text, String def, TextFieldFilter filter, Consumer<String> confirmed){
        new Dialog(title, "dialog"){{
            content().margin(30).add(text).padRight(6f);
            TextField field = content().addField(def, t -> {
            }).size(170f, 50f).get();
            field.setTextFieldFilter((f, c) -> field.getText().length() < 12 && filter.acceptChar(f, c));
            Platform.instance.addDialog(field);
            buttons().defaults().size(120, 54).pad(4);
            buttons().addButton("$text.ok", () -> {
                confirmed.accept(field.getText());
                hide();
            }).disabled(b -> field.getText().isEmpty());
            buttons().addButton("$text.cancel", this::hide);
        }}.show();
    }

    public void showTextInputOption(String title, String text, String def, TextFieldFilter filter, Consumer<String> confirmed){
        new Dialog(title, "dialog"){{
            content().margin(30).add(text).padRight(6f);
            TextField field = content().addField(def, t -> {
            }).size(170f, 50f).get();
            field.setTextFieldFilter((f, c) -> field.getText().length() < 12 && filter.acceptChar(f, c));
            Platform.instance.addDialog(field);
            buttons().defaults().size(120, 54).pad(4);
            content().row();
            CheckBox check = content().addCheck("Upload to discord",false,o->{

            }).get();
            content().row();
            buttons().row();
            buttons().row();
            buttons().addButton("$text.ok", () -> {
                String useDiscord = check.isChecked() ? "discord|":"";
                confirmed.accept(useDiscord+field.getText());
                hide();
            }).disabled(b -> field.getText().isEmpty());

            buttons().addButton("$text.cancel", this::hide);
        }}.show();
    }

    public void showTextInputOption(String title, String text, String def, Consumer<String> confirmed){
        showTextInputOption(title, text, def, (field, c) -> true, confirmed);
    }

    public void showTextInput(String title, String text, String def, Consumer<String> confirmed){
        showTextInput(title, text, def, (field, c) -> true, confirmed);
    }

    public void showInfoFade(String info){
        Table table = new Table();
        table.setFillParent(true);
        table.actions(Actions.fadeOut(7f, Interpolation.fade), Actions.removeActor());
        table.top().add(info).padTop(8);
        Core.scene.add(table);
    }

    public void showInfoFade2(String info){
        Table table = new Table();
        table.setFillParent(true);
        table.actions(moveTo(0,-100));
        table.actions(Actions.fadeOut(6f, Interpolation.fade), Actions.removeActor());
        table.actions(Actions.moveBy(0,100f,7f,Interpolation.fastSlow));//7f, Interpolation.fade), Actions.removeActor());;
        table.top().add(info).padTop(8);
        Core.scene.add(table);
    }

    int fade3step = 0;
    public void showInfoFade3(String info){
        Table table = new Table();
        table.setFillParent(true);
        table.actions(moveTo(fade3step*400 - 500,-900));
        table.actions(Actions.fadeOut(7f, Interpolation.slowFast), Actions.removeActor());
        table.actions(Actions.moveBy(0,800f,7f));//7f, Interpolation.fade), Actions.removeActor());;
        table.top().add(info).padTop(8);
        fade3step++;
        if (fade3step==3) fade3step = 0;
        Core.scene.add(table);
    }

    public void showInfo(String info){
        new Dialog("", "dialog"){{
            getCell(content()).growX();
            content().margin(15).add(info).width(400f).wrap().get().setAlignment(Align.center, Align.center);
            buttons().addButton("$text.ok", this::hide).size(90, 50).pad(4);
        }}.show();
    }

    List<Block> expensiveBlocks = Arrays.asList(PowerBlocks.thoriumReactor, PowerBlocks.rtgGenerator, PowerBlocks.turbineGenerator, PowerBlocks.thermalGenerator,
    ProductionBlocks.blastDrill, ProductionBlocks.oilExtractor,
    StorageBlocks.container, StorageBlocks.vault, StorageBlocks.core,
    DefenseBlocks.forceProjector, DefenseBlocks.mendProjector, DefenseBlocks.overdriveProjector,
    CraftingBlocks.arcsmelter, CraftingBlocks.blastMixer, CraftingBlocks.siliconsmelter, CraftingBlocks.pyratiteMixer, CraftingBlocks.plastaniumCompressor, CraftingBlocks.phaseWeaver, CraftingBlocks.centrifuge,
    TurretBlocks.ripple, TurretBlocks.meltdown, TurretBlocks.spectre, TurretBlocks.fuse, TurretBlocks.cyclone,
    LiquidBlocks.rotaryPump, LiquidBlocks.thermalPump,
    UnitBlocks.commandCenter, UnitBlocks.fortressFactory, UnitBlocks.revenantFactory, UnitBlocks.ghoulFactory, UnitBlocks.spiritFactory, UnitBlocks.phantomFactory,
    DistributionBlocks.massDriver
    );
    Player lastPlayerAlert = null;//players[0];
    long lastAlertTime = 0;
    public void showAlert(Player p, Block b, BuildRequest br, String action){
        Table table = new Table();
        table.setFillParent(true);
        table.left().top();
        table.actions(Actions.fadeOut(8, Interpolation.fade), Actions.removeActor());
        table.actions(Actions.moveTo(0, 170, 8f, Interpolation.fastSlow));
        if(b != null && expensiveBlocks.contains(b) && p != null){// && (action.equals(" began") ||(action.equals(" destroyed")))){
            if(action.equals(" began")){
                table.addImage(b.getEditorIcon()).size(32, 32).padTop(280).padLeft(4);
                //System.out.println(lastAlertTime - System.currentTimeMillis());
                if(!p.equals(lastPlayerAlert) || ((System.currentTimeMillis() - lastAlertTime) > 1000)){
                    String text = p.name;//"(" + br.x + "," + br.y + ")" + p.name;
                    table.top().add(text).padTop(280).padLeft(4);
                }
                lastAlertTime = System.currentTimeMillis();
                lastPlayerAlert = p;
            }
        }
        Core.scene.add(table);
    }

    public void showInfo(String info, Runnable clicked){
        new Dialog("", "dialog"){{
            getCell(content()).growX();
            content().margin(15).add(info).width(400f).wrap().get().setAlignment(Align.center, Align.center);
            buttons().addButton("$text.ok", () -> {
                clicked.run();
                hide();
            }).size(90, 50).pad(4);
        }}.show();
    }

    public void showError(String text){
        new Dialog("$text.error.title", "dialog"){{
            content().margin(15).add(text).width(400f).wrap().get().setAlignment(Align.center, Align.center);
            buttons().addButton("$text.ok", this::hide).size(90, 50).pad(4);
        }}.show();
    }

    public void showText(String title, String text){
        new Dialog(title, "dialog"){{
            content().margin(15).add(text).width(400f).wrap().get().setAlignment(Align.center, Align.center);
            buttons().addButton("$text.ok", this::hide).size(90, 50).pad(4);
        }}.show();
    }

    public void showConfirm(String title, String text, Runnable confirmed){
        FloatingDialog dialog = new FloatingDialog(title);
        dialog.content().add(text).width(400f).wrap().pad(4f).get().setAlignment(Align.center, Align.center);
        dialog.buttons().defaults().size(200f, 54f).pad(2f);
        dialog.setFillParent(false);
        dialog.buttons().addButton("$text.cancel", dialog::hide);
        dialog.buttons().addButton("$text.ok", () -> {
            dialog.hide();
            confirmed.run();
        });
        dialog.keyDown(Keys.ESCAPE, dialog::hide);
        dialog.keyDown(Keys.BACK, dialog::hide);
        dialog.show();
    }

    public String formatAmount(int number){
        if(number >= 1000000){
            return Strings.toFixed(number / 1000000f, 1) + "[gray]mil[]";
        }else if(number >= 10000){
            return number / 1000 + "[gray]k[]";
        }else if(number >= 1000){
            return Strings.toFixed(number / 1000f, 1) + "[gray]k[]";
        }else{
            return number + "";
        }
    }
}
