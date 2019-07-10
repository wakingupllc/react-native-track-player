package com.guichaguri.trackplayer.service;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.ReactInstanceManager;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;

import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;

import javax.annotation.Nullable;

public class MusicEventService extends HeadlessJsTaskService {
    @Nullable
    @Override
    protected HeadlessJsTaskConfig getTaskConfig(Intent intent) {
        if(intent == null) {
            return null;
        }

        String event = intent.getStringExtra("event");
        Bundle bundle = intent.getBundleExtra("data");

        if(event == null && bundle == null) {
            stopSelf();
            return null;
        }

        Log.d(Utils.LOG, "Sending event " + event + "...");

        WritableMap map = bundle != null ? Arguments.fromBundle(bundle) : Arguments.createMap();
        map.putString("event", event);

        // ReactInstanceManager reactInstanceManager = getReactNativeHost().getReactInstanceManager();
        // ReactContext reactContext = reactInstanceManager.getCurrentReactContext();

        // if (reactContext != null ) {
        //     reactContext.getJSModule(RCTDeviceEventEmitter.class).emit(event, map);
        // }

        return new HeadlessJsTaskConfig("TrackPlayer", map, 0, true);
    }

}