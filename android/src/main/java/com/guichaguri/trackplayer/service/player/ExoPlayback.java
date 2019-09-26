package com.guichaguri.trackplayer.service.player;

import android.os.Handler;
import android.os.Message;
import android.content.Context;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import com.facebook.react.bridge.Promise;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Timeline.Window;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.guichaguri.trackplayer.service.MusicManager;
import com.guichaguri.trackplayer.service.Utils;
import com.guichaguri.trackplayer.service.models.Track;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Guichaguri
 */
public abstract class ExoPlayback<T extends Player> implements EventListener {

    private static final int SHOW_PROGRESS = 1;

    private float mProgressUpdateInterval = 1000.0f;

    protected final Context context;
    protected final MusicManager manager;
    protected final T player;

    protected List<Track> queue = Collections.synchronizedList(new ArrayList<>());

    // https://github.com/google/ExoPlayer/issues/2728
    protected int lastKnownWindow = C.INDEX_UNSET;
    protected long lastKnownPosition = C.POSITION_UNSET;
    protected int previousState = PlaybackStateCompat.STATE_NONE;

    private final Handler progressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what != SHOW_PROGRESS) {
                return;
            }

            if (isPlaying()) {
                long pos = player.getCurrentPosition();
                long bufferedDuration = player.getBufferedPercentage() * player.getDuration() / 100;
                manager.onProgress(pos, bufferedDuration, player.getDuration());

                // Send the message again
                msg = obtainMessage(SHOW_PROGRESS);
                sendMessageDelayed(msg, Math.round(mProgressUpdateInterval));
            }
        }
    };

    public ExoPlayback(Context context, MusicManager manager, T player) {
        this.context = context;
        this.manager = manager;
        this.player = player;
    }

    public void initialize() {
        player.addListener(this);
    }

    public List<Track> getQueue() {
        return queue;
    }

    public abstract void add(Track track, int index, Promise promise);

    public abstract void add(Collection<Track> tracks, int index, Promise promise);

    public abstract void remove(List<Integer> indexes, Promise promise);

    public abstract void removeUpcomingTracks();

    public boolean isPlaying() {
        return player != null && player.getPlaybackState() == Player.STATE_READY && player.getPlayWhenReady();
    }

    public void updateTrack(int index, Track track) {
        int currentIndex = player.getCurrentWindowIndex();

        queue.set(index, track);

        if (currentIndex == index)
            manager.getMetadata().updateMetadata(track);
    }

    public Track getCurrentTrack() {
        int index = player.getCurrentWindowIndex();
        return index == C.INDEX_UNSET || index < 0 || index >= queue.size() ? null : queue.get(index);
    }

    public void skip(String id, Promise promise) {
        if (id == null || id.isEmpty()) {
            promise.reject("invalid_id", "The ID can't be null or empty");
            return;
        }

        for (int i = 0; i < queue.size(); i++) {
            if (id.equals(queue.get(i).id)) {
                lastKnownWindow = player.getCurrentWindowIndex();
                lastKnownPosition = player.getCurrentPosition();

                player.seekToDefaultPosition(i);
                promise.resolve(null);
                return;
            }
        }

        promise.reject("track_not_in_queue", "Given track ID was not found in queue");
    }

    public void skipToPrevious(Promise promise) {
        int prev = player.getPreviousWindowIndex();

        if (prev == C.INDEX_UNSET) {
            promise.reject("no_previous_track", "There is no previous track");
            return;
        }

        lastKnownWindow = player.getCurrentWindowIndex();
        lastKnownPosition = player.getCurrentPosition();

        player.seekToDefaultPosition(prev);
        promise.resolve(null);
    }

    public void skipToNext(Promise promise) {
        int next = player.getNextWindowIndex();

        if (next == C.INDEX_UNSET) {
            promise.reject("queue_exhausted", "There is no tracks left to play");
            return;
        }

        lastKnownWindow = player.getCurrentWindowIndex();
        lastKnownPosition = player.getCurrentPosition();

        player.seekToDefaultPosition(next);
        promise.resolve(null);
    }

    public void play() {
        player.setPlayWhenReady(true);
    }

    public void pause() {
        player.setPlayWhenReady(false);
    }

    public void stop() {
        lastKnownWindow = player.getCurrentWindowIndex();
        lastKnownPosition = player.getCurrentPosition();

        player.stop(false);
        player.setPlayWhenReady(false);
        player.seekTo(0, 0);
    }

    public void reset() {
        lastKnownWindow = player.getCurrentWindowIndex();
        lastKnownPosition = player.getCurrentPosition();

        player.stop(true);
        player.setPlayWhenReady(false);
    }

    public boolean isRemote() {
        return false;
    }

    public long getPosition() {
        return player.getCurrentPosition();
    }

    public long getBufferedPosition() {
        return player.getBufferedPosition();
    }

    public long getDuration() {
        return player.getDuration();
    }

    public void seekTo(long time) {
        lastKnownWindow = player.getCurrentWindowIndex();
        lastKnownPosition = player.getCurrentPosition();

        player.seekTo(time);
    }

    public abstract float getVolume();

    public abstract void setVolume(float volume);

    public float getRate() {
        return player.getPlaybackParameters().speed;
    }

    public void setRate(float rate) {
        player.setPlaybackParameters(new PlaybackParameters(rate, player.getPlaybackParameters().pitch));
    }

    public int getState() {
        switch (player.getPlaybackState()) {
        case Player.STATE_BUFFERING:
            return PlaybackStateCompat.STATE_BUFFERING;
        case Player.STATE_ENDED:
            return PlaybackStateCompat.STATE_STOPPED;
        case Player.STATE_IDLE:
            return PlaybackStateCompat.STATE_NONE;
        case Player.STATE_READY:
            return player.getPlayWhenReady() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        }
        return PlaybackStateCompat.STATE_NONE;
    }

    public void destroy() {
        if (player != null) {
            player.release();
        }
        progressHandler.removeMessages(SHOW_PROGRESS);
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
        Log.d(Utils.LOG, "onTimelineChanged: " + reason);

        if ((reason == Player.TIMELINE_CHANGE_REASON_PREPARED || reason == Player.TIMELINE_CHANGE_REASON_DYNAMIC)
                && !timeline.isEmpty()) {
            onPositionDiscontinuity(Player.DISCONTINUITY_REASON_INTERNAL);
        }
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        Log.d(Utils.LOG, "onPositionDiscontinuity: " + reason);

        if (lastKnownWindow != player.getCurrentWindowIndex()) {
            Track previous = lastKnownWindow == C.INDEX_UNSET ? null : queue.get(lastKnownWindow);
            Track next = getCurrentTrack();

            // Track changed because it ended
            // We'll use its duration instead of the last known position
            if (reason == Player.DISCONTINUITY_REASON_PERIOD_TRANSITION && lastKnownWindow != C.INDEX_UNSET) {
                if (lastKnownWindow >= player.getCurrentTimeline().getWindowCount())
                    return;
                long duration = player.getCurrentTimeline().getWindow(lastKnownWindow, new Window()).getDurationMs();
                if (duration != C.TIME_UNSET)
                    lastKnownPosition = duration;

                manager.onTrackUpdate(previous, lastKnownPosition, next, true);
            } else {
                manager.onTrackUpdate(previous, lastKnownPosition, next, false);
            }
        }

        lastKnownWindow = player.getCurrentWindowIndex();
        lastKnownPosition = player.getCurrentPosition();
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        // Do nothing
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case Player.STATE_IDLE:
                manager.onStateChange(PlaybackStateCompat.STATE_NONE);
                break;
            case Player.STATE_BUFFERING:
                manager.onBuffering(true);
                manager.onStateChange(PlaybackStateCompat.STATE_BUFFERING);
                break;
            case Player.STATE_READY:
                manager.onBuffering(false);
                if (player.getPlayWhenReady()) {
                    startProgressHandler();
                    manager.onPlay();
                    manager.onStateChange(PlaybackStateCompat.STATE_PLAYING);
                } else {
                    manager.onPause();
                    manager.onStateChange(PlaybackStateCompat.STATE_PAUSED);
                }
                break;
            case Player.STATE_ENDED:
                int next = player.getNextWindowIndex();
                Boolean isQueueFinished = next == C.INDEX_UNSET;
                Boolean isTrackFinished = playbackState == Player.STATE_ENDED;
                if (isQueueFinished) {
                    manager.onStop();
                    manager.onEnd(getCurrentTrack(), getPosition(), isTrackFinished);
                    manager.onStateChange(PlaybackStateCompat.STATE_STOPPED);
                    return;
                }

                Track previous = queue.get(lastKnownWindow);

                lastKnownWindow = player.getCurrentWindowIndex();
                lastKnownPosition = player.getCurrentPosition();

                player.seekToDefaultPosition(next);

                Track nextTrack = getCurrentTrack();
                manager.onTrackUpdate(previous, lastKnownPosition, nextTrack, true);
                break;
            default:
                break;
        }
    }

    private void startProgressHandler() {
        progressHandler.sendEmptyMessage(SHOW_PROGRESS);
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        // Repeat mode update
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        // Shuffle mode update
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        String code;

        if (error.type == ExoPlaybackException.TYPE_SOURCE) {
            code = "playback-source";
        } else if (error.type == ExoPlaybackException.TYPE_RENDERER) {
                code = "playback-renderer";
        } else {
            code = "playback"; // Other unexpected errors related to the playback
        }

        manager.onError(code, error.getCause().getMessage());
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        // Speed or pitch changes
    }

    @Override
    public void onSeekProcessed() {
        // Finished seeking
    }
}
