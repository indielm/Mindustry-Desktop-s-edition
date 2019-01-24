package io.anuke.mindustry.ui;

import io.anuke.ucore.util.Bundles;

import java.text.DecimalFormat;

/**
 * A low-garbage way to format bundle strings.
 */
public class FloatFormat{
    private final StringBuilder builder = new StringBuilder();
    private final String text;
    private double lastValue = Integer.MIN_VALUE;
    DecimalFormat df;
    public FloatFormat(String text){
        this.text = text;
        df = new DecimalFormat("###.#######");
    }

    public CharSequence get(double value){
        if(lastValue != value){
            builder.setLength(0);
            builder.append("FPS: ");
            builder.append(df.format(value));

            //builder.append(Bundles.format(text, value));
        }
        lastValue = value;
        return builder;
    }
}
