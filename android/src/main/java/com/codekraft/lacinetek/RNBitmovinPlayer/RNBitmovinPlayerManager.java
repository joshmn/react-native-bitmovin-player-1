package com.codekraft.lacinetek.RNBitmovinPlayer;

import com.bitmovin.player.api.event.data.ErrorEvent;
import com.bitmovin.player.api.event.data.FullscreenEnterEvent;
import com.bitmovin.player.api.event.data.FullscreenExitEvent;
import com.bitmovin.player.api.event.data.MutedEvent;
import com.bitmovin.player.api.event.data.PausedEvent;
import com.bitmovin.player.api.event.data.PlayEvent;
import com.bitmovin.player.api.event.data.PlaybackFinishedEvent;
import com.bitmovin.player.api.event.data.ReadyEvent;
import com.bitmovin.player.api.event.data.RenderFirstFrameEvent;
import com.bitmovin.player.api.event.data.SeekEvent;
import com.bitmovin.player.api.event.data.SeekedEvent;
import com.bitmovin.player.api.event.data.StallEndedEvent;
import com.bitmovin.player.api.event.data.StallStartedEvent;
import com.bitmovin.player.api.event.data.TimeChangedEvent;
import com.bitmovin.player.api.event.data.UnmutedEvent;
import com.bitmovin.player.api.event.listener.OnReadyListener;
import com.bitmovin.player.api.event.listener.OnPlayListener;
import com.bitmovin.player.api.event.listener.OnPausedListener;
import com.bitmovin.player.api.event.listener.OnTimeChangedListener;
import com.bitmovin.player.api.event.listener.OnStallStartedListener;
import com.bitmovin.player.api.event.listener.OnStallEndedListener;
import com.bitmovin.player.api.event.listener.OnPlaybackFinishedListener;
import com.bitmovin.player.api.event.listener.OnRenderFirstFrameListener;
import com.bitmovin.player.api.event.listener.OnErrorListener;
import com.bitmovin.player.api.event.listener.OnMutedListener;
import com.bitmovin.player.api.event.listener.OnUnmutedListener;
import com.bitmovin.player.api.event.listener.OnSeekListener;
import com.bitmovin.player.api.event.listener.OnSeekedListener;
import com.bitmovin.player.api.event.listener.OnFullscreenEnterListener;
import com.bitmovin.player.api.event.listener.OnFullscreenExitListener;
import com.bitmovin.player.config.PlayerConfiguration;
import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.BitmovinPlayerView;
import com.bitmovin.player.offline.OfflineSourceItem;
import com.bitmovin.player.ui.FullscreenHandler;
import com.bitmovin.player.config.drm.DRMSystems;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.media.SourceItem;
import com.bitmovin.player.config.drm.DRMConfiguration;
import com.bitmovin.player.config.drm.WidevineConfiguration;
import com.bitmovin.player.config.drm.PrepareLicenseCallback;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.google.gson.Gson;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import android.util.Base64;
import org.json.JSONObject;
import org.json.JSONException;

public class RNBitmovinPlayerManager extends SimpleViewManager<BitmovinPlayerView> implements FullscreenHandler, LifecycleEventListener {

    public static final String REACT_CLASS = "RNBitmovinPlayer";

    private boolean _fullscreen;
    private double _progress;
    private boolean _finished;

    private BitmovinPlayerView _playerView;
    private BitmovinPlayer _player;

    private ThemedReactContext _reactContext;
    private Gson gson = new Gson();

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    public Map getExportedCustomBubblingEventTypeConstants() {
        return MapBuilder.builder()
                .put(
                        "onReady",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onReady")))
                .put(
                        "onPlay",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onPlay")))
                .put(
                        "onPaused",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onPaused")))
                .put(
                        "onTimeChanged",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onTimeChanged")))
                .put(
                        "onStallStarted",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onStallStarted")))
                .put(
                        "onStallEnded",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onStallEnded")))
                .put(
                        "onPlaybackFinished",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onPlaybackFinished")))
                .put(
                        "onRenderFirstFrame",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onRenderFirstFrame")))
                .put(
                        "onError",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "_onPlayerError")))
                .put(
                        "onMuted",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onMuted")))
                .put(
                        "onUnmuted",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onUnmuted")))
                .put(
                        "onSeek",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onSeek")))
                .put(
                        "onSeeked",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onSeeked")))
                .put(
                        "onFullscreenEnter",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onFullscreenEnter")))
                .put(
                        "_onFullscreenExit",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "_onFullscreenExit")))
                .build();
    }

    @Override
    public BitmovinPlayerView createViewInstance(ThemedReactContext context) {
        _reactContext = context;
        _playerView = new BitmovinPlayerView(context);
        _player = _playerView.getPlayer();
        _fullscreen = false;
        _finished = false;

        _progress = 0;

        setListeners();

        return _playerView;
    }

    @Override
    public void onDropViewInstance(BitmovinPlayerView view) {
        view.onDestroy();

        super.onDropViewInstance(view);
    }

    @ReactProp(name = "initialProgress")
    public void setInitialProgress(BitmovinPlayerView view, double initialProgress) {
      _progress = initialProgress;

      _player.seek(initialProgress);

    }

    @ReactProp(name = "isFullscreen")
    public void setIsFullscreen(BitmovinPlayerView view, boolean isFullscreen) {
      _fullscreen = isFullscreen;
    }

    @ReactProp(name = "configuration")
    public void setConfiguration(BitmovinPlayerView view, ReadableMap config) {
        PlayerConfiguration configuration = new PlayerConfiguration();

        ReadableMap sourceMap = null;
        ReadableMap styleMap = null;

        if (config.hasKey("source")) {
            sourceMap = config.getMap("source");
        }

        if (config.hasKey("offlineSource") && config.getString("offlineSource").length() > 1) {
            String offlineSource = config.getString("offlineSource");
            OfflineSourceItem offlineSourceItem = this.gson.fromJson(offlineSource, OfflineSourceItem.class);

            configuration.setSourceItem(offlineSourceItem);
        }
        else if (sourceMap != null && sourceMap.getString("url") != null) {

            String userDataString = String.format("{\"userId\":\"%s\", \"sessionId\":\"%s\", \"merchant\":\"kinow_lacinetek\"}", sourceMap.getString("userId"), sourceMap.getString("sessionId"));
            Map<String, String> userDataMap = new HashMap<String, String>();
            byte[] data = Base64.encode(userDataString.getBytes(), Base64.NO_WRAP);

            userDataMap.put("dt-custom-data", new String(data));

            WidevineConfiguration widevineConfiguration = new WidevineConfiguration("https://lic.drmtoday.com/license-proxy-widevine/cenc/");
            widevineConfiguration.setHttpHeaders(userDataMap);

            widevineConfiguration.setPrepareLicenseCallback(new PrepareLicenseCallback()
            {
                @Override
                public byte[] prepareLicense(byte[] licenseResponse)
                {
                    String json = new String(licenseResponse);
                    try
                    {
                        JSONObject jsonObject = new JSONObject(json);
                        String license = jsonObject.getString("license");
                        return android.util.Base64.decode(license, android.util.Base64.DEFAULT);
                    }
                    catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                    return new byte[0];
                }
            });
            SourceItem sourceItem = new SourceItem(sourceMap.getString("url"));
            sourceItem.addDRMConfiguration(widevineConfiguration);
            SourceConfiguration sourceConfiguration = new SourceConfiguration();
            sourceConfiguration.addSourceItem(sourceItem);
            configuration.setSourceConfiguration(sourceConfiguration);

        }

        _playerView.setFullscreenHandler(this);
        _player.setup(configuration);
    }

    @Override
    public boolean isFullScreen() {
        return _fullscreen;
    }

    @Override
    public void onResume() {}

    @Override
    public void onPause() {}

    @Override
    public void onDestroy() {}

    @Override
    public void onFullscreenRequested() {
        _fullscreen = true;

        WritableMap map = Arguments.createMap();
        map.putDouble("currentTime", _progress);

        _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
          _playerView.getId(),
          "onFullscreenEnter",
          map);
    }

    @Override
    public void onFullscreenExitRequested() {
        _fullscreen = false;

        WritableMap map = Arguments.createMap();
        map.putDouble("currentTime", _progress);

        _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
          _playerView.getId(),
          "onFullscreenExit",
          map);
    }

    @Override
    public void onHostResume() {
        _playerView.onResume();
    }

    @Override
    public void onHostPause() {
        _playerView.onPause();
    }

    @Override
    public void onHostDestroy() {
        _playerView.onDestroy();
    }

    private WritableMap calculateProgress(double currentTime) {
        double currentProgress = currentTime / _player.getDuration();
        double percentage  = Math.round(currentProgress * 100);
  
        WritableMap map = Arguments.createMap();
        map.putDouble("currentTime", currentTime);
        map.putDouble("percentage", percentage);
  
        return map;
    }

    private void setListeners() {
        _player.addEventListener(new OnReadyListener() {
            public void onReady(ReadyEvent event) {
              _player.seek(_progress);
              _finished = false;

                double duration = _player.getDuration();

                WritableMap map = Arguments.createMap();
                map.putDouble("duration", duration);

                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        _playerView.getId(),
                        "onReady",
                        map);
            }
        });

        _player.addEventListener(new OnPlayListener() {
            public void onPlay(PlayEvent event) {
                WritableMap map = Arguments.createMap();

              if (_finished) {
                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                  _playerView.getId(),
                  "onReplay",
                  map);

                _finished = false;
              } else {
                map.putDouble("time", event.getTime());

                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                  _playerView.getId(),
                  "onPlay",
                  map);
              }
            }
        });

        _player.addEventListener(new OnPausedListener() {
            @Override
            public void onPaused(PausedEvent event) {
                WritableMap map = Arguments.createMap();

                map.putDouble("time", event.getTime());

                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        _playerView.getId(),
                        "onPaused",
                        map);
            }
        });

        _player.addEventListener(new OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimeChangedEvent event) {
                 _progress = event.getTime();


                WritableMap map = calculateProgress(event.getTime());

                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        _playerView.getId(),
                        "onTimeChanged",
                        map);
            }
        });

        _player.addEventListener(new OnStallStartedListener() {
            @Override
            public void onStallStarted(StallStartedEvent event) {
                WritableMap map = Arguments.createMap();

                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        _playerView.getId(),
                        "onStallStarted",
                        map);
            }
        });

        _player.addEventListener(new OnStallEndedListener() {
            @Override
            public void onStallEnded(StallEndedEvent event) {
                WritableMap map = Arguments.createMap();

                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        _playerView.getId(),
                        "onStallEnded",
                        map);
            }
        });

        _player.addEventListener(new OnPlaybackFinishedListener() {
            @Override
            public void onPlaybackFinished(PlaybackFinishedEvent event) {
                _finished = true;

                WritableMap map = Arguments.createMap();

                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        _playerView.getId(),
                        "onPlaybackFinished",
                        map);
            }
        });

        _player.addEventListener(new OnRenderFirstFrameListener() {
            @Override
            public void onRenderFirstFrame(RenderFirstFrameEvent event) {
                WritableMap map = Arguments.createMap();

                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        _playerView.getId(),
                        "onRenderFirstFrame",
                        map);
            }
        });

        _player.addEventListener(new OnErrorListener() {
            @Override
            public void onError(ErrorEvent event) {
                WritableMap map = Arguments.createMap();
                WritableMap errorMap = Arguments.createMap();

                errorMap.putInt("code", event.getCode());
                errorMap.putString("message", event.getMessage());

                map.putMap("error", errorMap);

                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        _playerView.getId(),
                        "onError",
                        map);
            }
        });

        _player.addEventListener(new OnMutedListener() {
            @Override
            public void onMuted(MutedEvent event) {
                WritableMap map = Arguments.createMap();

                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        _playerView.getId(),
                        "onMuted",
                        map);
            }
        });

        _player.addEventListener(new OnUnmutedListener() {
            @Override
            public void onUnmuted(UnmutedEvent event) {
                WritableMap map = Arguments.createMap();

                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        _playerView.getId(),
                        "onUnmuted",
                        map);
            }
        });

        _player.addEventListener(new OnSeekListener() {
            @Override
            public void onSeek(SeekEvent event) {

                _progress = event.getSeekTarget();

                WritableMap map = calculateProgress(event.getSeekTarget());

                if (map.getDouble("percentage") < 95) {
                  _finished = false;
                }

                map.putDouble("seekTarget", event.getSeekTarget());
                map.putDouble("position", event.getPosition());

                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        _playerView.getId(),
                        "onSeek",
                        map);
            }
        });

        _player.addEventListener(new OnSeekedListener() {
            @Override
            public void onSeeked(SeekedEvent event) {
                WritableMap map = Arguments.createMap();

                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        _playerView.getId(),
                        "onSeeked",
                        map);
            }
        });

        _player.addEventListener(new OnFullscreenEnterListener() {
            @Override
            public void onFullscreenEnter(FullscreenEnterEvent event) {
                WritableMap map = Arguments.createMap();

                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        _playerView.getId(),
                        "onFullscreenEnter",
                        map);
            }
        });

        _player.addEventListener(new OnFullscreenExitListener() {
            @Override
            public void onFullscreenExit(FullscreenExitEvent event) {
                WritableMap map = Arguments.createMap();

                _reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        _playerView.getId(),
                        "onFullscreenExit",
                        map);
            }
        });
    }
}
