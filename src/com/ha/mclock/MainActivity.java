package com.ha.mclock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Shows the current lock state; pressing the big button keeps it alive. */
public class MainActivity extends Activity {

    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(48, 96, 48, 48);
        scroll.addView(box);

        TextView title = new TextView(this);
        title.setText("HA Multicast Lock");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        status = new TextView(this);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 48, 0, 48);
        box.addView(status);

        TextView btn = new TextView(this);
        btn.setText("  HOLD MULTICAST LOCK  ");
        btn.setTextSize(20);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(48, 32, 48, 32);
        btn.setBackgroundColor(Color.parseColor("#2196F3"));
        btn.setTextColor(Color.WHITE);
        btn.setOnClickListener(v -> startLockService());
        box.addView(btn);

        TextView btnRel = new TextView(this);
        btnRel.setText("  RELEASE  ");
        btnRel.setTextSize(16);
        btnRel.setGravity(Gravity.CENTER);
        btnRel.setPadding(48, 24, 48, 24);
        btnRel.setBackgroundColor(Color.parseColor("#9E9E9E"));
        btnRel.setTextColor(Color.WHITE);
        btnRel.setOnClickListener(v -> {
            stopService(new Intent(this, LockService.class));
            refresh();
        });
        box.addView(btnRel);

        TextView hint = new TextView(this);
        hint.setTextSize(12);
        hint.setPadding(0, 48, 0, 0);
        hint.setText("Holding the lock disables Android's APF multicast filter.\n"
                + "This lets Home Assistant (in Termux/Docker) receive\n"
                + "iOS HomeKit mDNS queries again.\n\n"
                + "To keep it alive after reboot, open the app once more\n"
                + "or disable battery optimisation for this app.");
        box.addView(hint);

        setContentView(scroll);
        startLockService();
        refresh();
    }

    private void startLockService() {
        Intent i = new Intent(this, LockService.class);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            startForegroundService(i);
        } else {
            startService(i);
        }
        status.postDelayed(this::refresh, 500);
    }

    private void refresh() {
        WifiManager wm = (WifiManager) getApplicationContext()
                .getSystemService(WIFI_SERVICE);
        boolean held = LockService.isHeld();
        String apf = "";
        try {
            // Surface the intent setting only; APF state needs root dumpsys.
            apf = "";
        } catch (Throwable ignored) {}

        status.setText((held ? "LOCK HELD" : "lock NOT held")
                + "\n\nWifiManager multicast state: "
                + (wm == null ? "?" : (held ? "enabled" : "filtered"))
                + apf);
        status.setTextColor(held
                ? Color.parseColor("#4CAF50")
                : Color.parseColor("#F44336"));
    }
}
