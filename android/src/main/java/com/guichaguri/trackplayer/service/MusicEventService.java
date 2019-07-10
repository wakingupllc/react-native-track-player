package com.guichaguri.trackplayer.service;

import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.util.Log;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.os.IBinder;
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

public class MusicEventService extends HeadlessJsTaskService {

    @Nullable
    @Override
    protected HeadlessJsTaskConfig getTaskConfig(Intent intent) {
        return new HeadlessJsTaskConfig("TrackPlayer", Arguments.createMap(), 0, true);
    }

    @Override
    public void onHeadlessJsTaskFinish(int taskId) {
        // Overridden to prevent the service from being terminated
    }
}
