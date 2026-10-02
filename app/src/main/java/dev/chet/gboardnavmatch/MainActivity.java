package dev.chet.gboardnavmatch;

import android.app.Activity;
import android.graphics.Color;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView value;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        SharedPreferences p = getSharedPreferences(ColorProvider.PREFS, 0);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);
        root.setGravity(Gravity.TOP);

        TextView title = new TextView(this);
        title.setText("Gboard Nav Match");
        title.setTextSize(26);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Matches Gboard's keyboard surface to the current app navigation bar colour.\n\nScope Gboard plus every app whose navigation colour you want captured.");
        desc.setTextSize(16);
        desc.setPadding(0, 18, 0, 24);
        root.addView(desc);

        Switch enabled = new Switch(this);
        enabled.setText("Enable keyboard colour matching");
        enabled.setChecked(p.getBoolean("enabled", true));
        enabled.setOnCheckedChangeListener((v, checked) -> p.edit().putBoolean("enabled", checked).apply());
        root.addView(enabled);

        value = new TextView(this);
        int adj = p.getInt("adjust", 0);
        value.setText("Brightness adjustment: " + adj + "%");
        value.setPadding(0, 28, 0, 0);
        root.addView(value);

        SeekBar seek = new SeekBar(this);
        seek.setMax(40);
        seek.setProgress(adj + 20);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                int a = progress - 20;
                value.setText("Brightness adjustment: " + a + "%");
                if (fromUser) p.edit().putInt("adjust", a).apply();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(seek, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button reset = new Button(this);
        reset.setText("Reset to exact colour");
        reset.setOnClickListener(v -> { seek.setProgress(20); p.edit().putInt("adjust", 0).apply(); });
        root.addView(reset);

        TextView note = new TextView(this);
        note.setText("v1.0.2 diagnostic build\nVector/LSPosed log tag: GboardNavMatch");
        note.setPadding(0, 32, 0, 0);
        root.addView(note);
        setContentView(root);
    }
}
