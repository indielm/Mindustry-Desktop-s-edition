package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.Vars;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;

public class ThreadHandler{
    private long lastFrameTime;

    public ThreadHandler(){
        Timers.setDeltaProvider(() -> {
            float result = 1;//Gdx.graphics.getDeltaTime() * 60f;
            return Float.isNaN(result) || Float.isInfinite(result) ? 1f : Math.min(result, 60f / 10f);
        });
    }

    public void run(Runnable r){
        r.run();
    }

    public void runGraphics(Runnable r){
        r.run();
    }

    public void runDelay(Runnable r){
        Gdx.app.postRunnable(r);
    }

    public long getFrameID(){
        return Gdx.graphics.getFrameId();
    }

    public void handleBeginRender(){
        lastFrameTime = TimeUtils.millis();
    }

    long lastNano = 0;
    public void handleEndRender(){
        final int fpsCap = Settings.getInt("fpscap", 1000);
        final long nanoWait =(long)((1.0000000000d/(long)fpsCap)* 1000000000L );//16,666,666
        if(fpsCap <= 1000){
            long target = 1000/fpsCap;
            long elapsed = TimeUtils.timeSinceMillis(lastFrameTime);
            if(elapsed < target){
                try {
                    Thread.sleep((int)((target - elapsed)*0.75f)); //TODO adjust this float for less CPU usage
                }
                catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
            int i = 0;
            long nextTime = nanoWait+lastNano;
            while(System.nanoTime() < nextTime) i++;
            Vars.realFPS = (System.nanoTime()-lastNano)/1000000000.0d;
            Vars.realFPS = Vars.realFPS*fpsCap*fpsCap;

            Vars.fpsLogCounter++;
            if (Vars.fpsLogCounter>=256) Vars.fpsLogCounter =0;
            Vars.fpsLog[Vars.fpsLogCounter] = (float)Vars.realFPS;

            lastNano = System.nanoTime();
            //System.out.println(Vars.realFPS + " , " + correct);

        }
    }

}
