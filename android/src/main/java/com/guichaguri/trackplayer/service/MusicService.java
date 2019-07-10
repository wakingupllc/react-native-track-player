package com.guichaguri.trackplayer.service;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import javax.annotation.Nullable;

import com.facebook.react.HeadlessJsTaskService;

/**
 * @author Guichaguri
 */
public class MusicService extends Service {

    MusicManager manager;
    Handler handler;

    public void emit(String event, Bundle data) {
        Utils.emit(this, event, data);
    }

    public void destroy() {
        if(handler != null) {
            handler.removeMessages(0);
            handler = null;
        }

        if(manager != null) {
            manager.destroy();
            manager = null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        manager = new MusicManager(this);
        handler = new Handler();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if(Utils.CONNECT_INTENT.equals(intent.getAction())) {
            return new MusicBinder(this, manager);
        }

        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Utils.LOG, "Service command (" + (intent != null ? intent.getAction() : "Unknown") + ")");

        if (intent == null) {
            Log.d(Utils.TAG, "The service is probably restarting. The player is being safely destroyed");
            stopForeground(true);
        }

        return intent == null ? START_NOT_STICKY : START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroy();
    }
}
