package com.ha.mclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/** Restart the lock service after a reboot. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        Log.i("HAMcastLock", "boot receiver: " + intent.getAction());
        Intent i = new Intent(ctx, LockService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            ctx.startForegroundService(i);
        } else {
            ctx.startService(i);
        }
    }
}
