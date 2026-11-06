package com.ha.mclock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/**
 * Foreground service that holds a non-reference-counted MulticastLock for as
 * long as it runs. The lock is process-wide, so releasing it would let Android
 * re-install the APF program that drops all inbound multicast frames.
 */
public class LockService extends Service {

    private static final String TAG = "HAMcastLock";
    private static final String CHANNEL = "mclock";
    private static final int NOTIF_ID = 4711;

    private static volatile boolean sHeld = false;

    private WifiManager.MulticastLock lock;

    public static boolean isHeld() {
        return sHeld;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIF_ID, buildNotification());
        acquire();
    }

    private void acquire() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wm == null) {
                Log.e(TAG, "no WifiManager");
                return;
            }
            if (lock != null && lock.isHeld()) {
                sHeld = true;
                return;
            }
            lock = wm.createMulticastLock("ha-mclock");
            // not reference counted: a single acquire() is enough and it stays
            // held until release()/process death regardless of other callers.
            lock.setReferenceCounted(false);
            lock.acquire();
            sHeld = true;
            Log.i(TAG, "MulticastLock acquired: " + lock);
        } catch (Throwable t) {
            sHeld = false;
            Log.e(TAG, "acquire failed", t);
        }
    }

    private void release() {
        try {
            if (lock != null && lock.isHeld()) {
                lock.release();
            }
        } catch (Throwable t) {
            Log.e(TAG, "release failed", t);
        } finally {
            lock = null;
            sHeld = false;
            Log.i(TAG, "MulticastLock released");
        }
    }

    private Notification buildNotification() {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL, "Multicast Lock", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            if (nm != null) {
                nm.createNotificationChannel(ch);
            }
            b = new Notification.Builder(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
        }
        return b.setContentTitle("Multicast Lock held")
                .setContentText("HomeKit mDNS discovery is enabled")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setOngoing(true)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        acquire();
        // restart if the system kills us
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
