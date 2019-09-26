package com.guichaguri.trackplayer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import com.facebook.react.bridge.Promise;
import com.facebook.react.views.imagehelper.ResourceDrawableIdHelper;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.guichaguri.trackplayer.R;
import com.facebook.react.HeadlessJsTaskService;

/**
 * @author Guichaguri
 */
public class Utils {

    public static final String CONNECT_INTENT = "com.guichaguri.trackplayer.connect";
    public static final String NOTIFICATION_CHANNEL = "com.guichaguri.trackplayer";
    public static final String SETUP_NOTIFICATION_CHANNEL = "com.guichaguri.trackplayer-setup";
    public static final String LOG = "RNTrackPlayer";

    public static Runnable toRunnable(Promise promise) {
        return () -> promise.resolve(null);
    }

    public static long toMillis(double seconds) {
        return (long)(seconds * 1000);
    }

    public static double toSeconds(long millis) {
        return millis / 1000D;
    }

    public static boolean isLocal(Uri uri) {
        if(uri == null) return false;

        String scheme = uri.getScheme();
        String host = uri.getHost();

        return scheme == null ||
                scheme.equals(ContentResolver.SCHEME_FILE) ||
                scheme.equals(ContentResolver.SCHEME_ANDROID_RESOURCE) ||
                scheme.equals(ContentResolver.SCHEME_CONTENT) ||
                scheme.equals(RawResourceDataSource.RAW_RESOURCE_SCHEME) ||
                scheme.equals("res") ||
                host == null ||
                host.equals("localhost") ||
                host.equals("127.0.0.1") ||
                host.equals("[::1]");
    }

    public static Uri getUri(Context context, Bundle data, String key) {
        if(!data.containsKey(key)) return null;
        Object obj = data.get(key);

        if(obj instanceof String) {
            // Remote or Local Uri

            if(((String)obj).trim().isEmpty())
                throw new RuntimeException("The URL cannot be empty");

            return Uri.parse((String)obj);

        } else if(obj instanceof Bundle) {
            // require/import

            String uri = ((Bundle)obj).getString("uri");

            ResourceDrawableIdHelper helper = ResourceDrawableIdHelper.getInstance();
            int id = helper.getResourceDrawableId(context, uri);

            if(id > 0) {
                // In production, we can obtain the resource uri
                Resources res = context.getResources();

                return new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                        .authority(res.getResourcePackageName(id))
                        .appendPath(res.getResourceTypeName(id))
                        .appendPath(res.getResourceEntryName(id))
                        .build();
            } else {
                // During development, the resources might come directly from the metro server
                return Uri.parse(uri);
            }

        }

        return null;
    }

    public static int getRawResourceId(Context context, Bundle data, String key) {
        if(!data.containsKey(key)) return 0;
        Object obj = data.get(key);

        if(!(obj instanceof Bundle)) return 0;
        String name = ((Bundle)obj).getString("uri");

        if(name == null || name.isEmpty()) return 0;
        name = name.toLowerCase().replace("-", "_");

        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException ex) {
            return context.getResources().getIdentifier(name, "raw", context.getPackageName());
        }
    }

    public static RatingCompat getRating(Bundle data, String key, int ratingType) {
        if(!data.containsKey(key) || ratingType == RatingCompat.RATING_NONE) {
            return RatingCompat.newUnratedRating(ratingType);
        } else if(ratingType == RatingCompat.RATING_HEART) {
            return RatingCompat.newHeartRating(data.getBoolean(key, true));
        } else if(ratingType == RatingCompat.RATING_THUMB_UP_DOWN) {
            return RatingCompat.newThumbRating(data.getBoolean(key, true));
        } else if(ratingType == RatingCompat.RATING_PERCENTAGE) {
            return RatingCompat.newPercentageRating(data.getFloat(key, 0));
        } else {
            return RatingCompat.newStarRating(ratingType, data.getFloat(key, 0));
        }
    }

    public static void setRating(Bundle data, String key, RatingCompat rating) {
        if(!rating.isRated()) return;
        int ratingType = rating.getRatingStyle();

        if(ratingType == RatingCompat.RATING_HEART) {
            data.putBoolean(key, rating.hasHeart());
        } else if(ratingType == RatingCompat.RATING_THUMB_UP_DOWN) {
            data.putBoolean(key, rating.isThumbUp());
        } else if(ratingType == RatingCompat.RATING_PERCENTAGE) {
            data.putDouble(key, rating.getPercentRating());
        } else {
            data.putDouble(key, rating.getStarRating());
        }
    }

    public static void createSetupNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Utils.SETUP_NOTIFICATION_CHANNEL,
                    "Playback setup",
                    NotificationManager.IMPORTANCE_NONE
            );
            channel.setShowBadge(false);
            channel.setSound(null, null);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(channel);
        }
    }

    public static Notification createBlankSetupNotification(Context context) {
        String channel = null;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createSetupNotificationChannel(context);
            channel = Utils.SETUP_NOTIFICATION_CHANNEL;
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channel);
        Notification notification = notificationBuilder.setPriority(NotificationManager.IMPORTANCE_MIN)
                .setSmallIcon(R.drawable.play)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        return notification;
    }

    public static void emit(Context context, String event, Bundle data) {
        Intent intent = new Intent(context, MusicEventService.class);

        intent.putExtra("event", event);
        if(data != null) intent.putExtra("data", data);

        try {
            context.startService(intent);
            HeadlessJsTaskService.acquireWakeLockNow(context);
        } catch (Exception e) {
            Log.d(Utils.LOG, "Failed to start headless service...");
        }
    }
}
