package com.guichaguri.trackplayer.service;

import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaButtonReceiver;

import com.guichaguri.trackplayer.service.Utils;

import android.support.v4.media.session.MediaSessionCompat;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import javax.annotation.Nullable;

/**
 * @author Guichaguri
 */
public class MusicService extends HeadlessJsTaskService {
    public static final int NOTIFICATION_ID = 412;
    private boolean mServiceInStartedState = false;
    
    MusicManager manager;
    Handler handler;

    @Nullable
    @Override
    protected HeadlessJsTaskConfig getTaskConfig(Intent intent) {
        return new HeadlessJsTaskConfig("TrackPlayer", Arguments.createMap(), 0, true);
    }

    @Override
    public void onHeadlessJsTaskFinish(int taskId) {
        // Overridden to prevent the service from being terminated
    }

    @Override
    public void onCreate() {
        super.onCreate();
        manager = new MusicManager(this);
        handler = new Handler();
        this.start();
    }

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(Utils.CONNECT_INTENT);
        return intent;
    }

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if(Utils.CONNECT_INTENT.equals(intent.getAction())) {
            return new MusicBinder(this, manager);
        }

        return super.onBind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroy();
        this.stop();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (manager == null || manager.shouldStopWithApp()) {
            stopForeground(true);
            stopSelf();
        }
    }

    public void start() { 
        Notification notification = Utils.createBlankSetupNotification(this);
        startForeground(MusicService.NOTIFICATION_ID, notification);
        mServiceInStartedState = true;
    }

    public void stop() { 
        stopForeground(true);
        stopSelf();
        mServiceInStartedState = false;
    }

    public boolean isServiceStarted() {
        return mServiceInStartedState;
    }
}
