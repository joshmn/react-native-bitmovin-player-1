package com.codekraft.lacinetek.RNBitmovinPlayer;

import android.content.ContextWrapper;

import com.bitmovin.player.IllegalOperationException;
import com.bitmovin.player.NoConnectionException;
import com.bitmovin.player.api.event.data.ErrorEvent;
import com.bitmovin.player.config.media.HLSSource;
import com.bitmovin.player.config.media.SourceItem;
import com.bitmovin.player.offline.OfflineContentManager;
import com.bitmovin.player.offline.OfflineContentManagerListener;
import com.bitmovin.player.offline.OfflineSourceItem;
import com.bitmovin.player.offline.options.AudioOfflineOptionEntry;
import com.bitmovin.player.offline.options.OfflineContentOptions;
import com.bitmovin.player.offline.options.OfflineOptionEntryAction;
import com.bitmovin.player.offline.options.OfflineOptionEntryState;
import com.bitmovin.player.offline.options.TextOfflineOptionEntry;
import com.bitmovin.player.offline.options.VideoOfflineOptionEntry;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.Source;

import androidx.annotation.NonNull;

public class RNBitmovinVideoManagerModule extends ReactContextBaseJavaModule implements OfflineContentManagerListener {

    private final ReactApplicationContext reactContext;
    private DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;
    private File rootFolder;
    private Gson gson = new Gson();
    private String currentAction;

    private OfflineContentOptions offlineOptions;
    private OfflineContentManager offlineContentManager;

    @NonNull
    @Override
    public String getName() {
        return "RNBitmovinVideoManagerModule";
    }

    public RNBitmovinVideoManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @ReactMethod
    public void download(ReadableMap configuration) {
        this.currentAction = "DOWNLOAD";
        this.eventEmitter = this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        this.rootFolder = this.getReactApplicationContext().getDir("offline", ContextWrapper.MODE_PRIVATE);

        String url = configuration.getString("url");
        if (url == null) {
            this.eventEmitter.emit("onDownloadError", "URL is not provided");
        } else {
            SourceItem sourceItem = new SourceItem(url);

            if (configuration.hasKey("title") && configuration.getString("title") != null) {
                sourceItem.setTitle(configuration.getString("title"));
            }

            this.offlineContentManager = OfflineContentManager.getOfflineContentManager(sourceItem, this.rootFolder.getPath(), url, this, this.getReactApplicationContext());
            this.offlineContentManager.getOptions();
        }
    }

    @ReactMethod
    public void pauseDownload() {
        if(this.offlineContentManager == null) {
            return;
        }

        this.offlineContentManager.suspend();
    }

    @ReactMethod
    public void resumeDownload() {
        if(this.offlineContentManager == null) {
            return;
        }

        this.offlineContentManager.resume();
    }


    @ReactMethod
    public void delete(String source) {
        this.currentAction = "DELETE";
        this.eventEmitter = this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        this.rootFolder = this.getReactApplicationContext().getDir("offline", ContextWrapper.MODE_PRIVATE);


        if (source == null || source.equals("")) {
            return;
        } else {
            SourceItem offlineSourceItem = this.gson.fromJson(source, SourceItem.class);

            HLSSource hls = offlineSourceItem.getHlsSource();
            String url = hls.getUrl();

            this.offlineContentManager = OfflineContentManager.getOfflineContentManager(offlineSourceItem, this.rootFolder.getAbsolutePath(), url, this, this.getReactApplicationContext());
            this.offlineContentManager.deleteAll();
        }
    }

    @ReactMethod
    public void cancelDownload() {
        return;
    }

    @ReactMethod
    public void getState(String url) {
        this.currentAction = "GET_STATE";
        this.eventEmitter = this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        this.rootFolder = this.getReactApplicationContext().getDir("offline", ContextWrapper.MODE_PRIVATE);

        SourceItem sourceItem = new SourceItem(url);

        this.offlineContentManager = OfflineContentManager.getOfflineContentManager(sourceItem, this.rootFolder.getPath(), url, this, this.getReactApplicationContext());
        this.offlineContentManager.getOptions();
    }

    @Override
    public void onCompleted(SourceItem sourceItem, OfflineContentOptions offlineContentOptions) {
        this.offlineOptions = offlineContentOptions;

        try {
            OfflineSourceItem offlineSourceItem = this.offlineContentManager.getOfflineSourceItem();
            Object data = this.gson.toJson(offlineSourceItem);

            if (offlineSourceItem != null && !data.equals("null")) {
              eventEmitter.emit("onDownloadCompleted", data);
            }
        } catch (IOException e) {
            e.printStackTrace();
            eventEmitter.emit("onDownloadError", e.getMessage());
        }
    }

    @Override
    public void onError(SourceItem sourceItem, ErrorEvent errorEvent) {
        eventEmitter.emit("onDownloadError", errorEvent.getMessage());
    }

    @Override
    public void onProgress(SourceItem sourceItem, float progress) {
        eventEmitter.emit("onDownloadProgress", progress);
    }

    private VideoOfflineOptionEntry getVideoToDownload(OfflineContentOptions offlineContentOptions, int qualityHeight) {
        for (VideoOfflineOptionEntry option : offlineContentOptions.getVideoOptions()) {
            if (option.getHeight() == qualityHeight) {
                return option;
            }
        }

        return offlineContentOptions.getVideoOptions().get(0);
    }

    @Override
    public void onOptionsAvailable(SourceItem sourceItem, OfflineContentOptions offlineContentOptions) {
        this.offlineOptions = offlineContentOptions;

        VideoOfflineOptionEntry videoEntry = this.getVideoToDownload(offlineContentOptions, 360);
        AudioOfflineOptionEntry audioEntry = (AudioOfflineOptionEntry) offlineContentOptions.getAudioOptions().get(0);

        OfflineOptionEntryState offlineOptionEntryState = videoEntry.getState();

        if (this.currentAction.equals("DOWNLOAD")) {
            switch (offlineOptionEntryState.name()) {
                case "NOT_DOWNLOADED":
                    try {
                        videoEntry.setAction(OfflineOptionEntryAction.DOWNLOAD);
                        audioEntry.setAction(OfflineOptionEntryAction.DOWNLOAD);
                    } catch (IllegalOperationException e) {
                        e.printStackTrace();
                        eventEmitter.emit("onDownloadError", e.getMessage());
                    }


                    try {
                        this.offlineContentManager.process(offlineContentOptions);
                    } catch (NoConnectionException e) {
                        e.printStackTrace();
                        eventEmitter.emit("onDownloadError", e.getMessage());
                    }

                    break;
                case "DOWNLOADED":
                    try {
                        OfflineSourceItem offlineSourceItem = this.offlineContentManager.getOfflineSourceItem();
                        Object data = this.gson.toJson(offlineSourceItem);


                        eventEmitter.emit("onDownloadCompleted", data);
                    } catch (IOException e) {
                        e.printStackTrace();
                        eventEmitter.emit("onDownloadError", e.getMessage());
                    }
                    break;
                case "FAILED":
                    this.currentAction = "DELETE";
                    this.eventEmitter.emit("onDownloadError", "Error trying to download video");
                    this.offlineContentManager.deleteAll();
                    break;
                default:
                    break;
            }
        } else if (this.currentAction.equals("GET_STATE")){
            this.eventEmitter.emit("onState", offlineOptionEntryState.name());
            this.currentAction = "";
        }
    }

    @Override
    public void onDrmLicenseUpdated(SourceItem sourceItem) {
    }

    @Override
    public void onSuspended(SourceItem sourceItem) {
        eventEmitter.emit("onDownloadSuspended", true);
    }

    @Override
    public void onResumed(SourceItem sourceItem) {
        eventEmitter.emit("onDownloadSuspended", false);
    }
}