import { NativeModules, NativeEventEmitter } from 'react-native';

const { RNBitmovinVideoManagerModule } = NativeModules;
const EventEmitter = new NativeEventEmitter(RNBitmovinVideoManagerModule);

let RNBitmovinVideoManager = {};

const allEvents = [
  "onDownloadCompleted",
  "onDownloadProgress",
  "onDownloadError",
  "onDownloadCanceled",
  "onDownloadSuspended",
  "onState"
];

RNBitmovinVideoManager.download = configuration => RNBitmovinVideoManagerModule.download(configuration);
RNBitmovinVideoManager.delete = (source) => RNBitmovinVideoManagerModule.delete(source);
RNBitmovinVideoManager.pauseDownload = () => RNBitmovinVideoManagerModule.pauseDownload();
RNBitmovinVideoManager.resumeDownload = () => RNBitmovinVideoManagerModule.resumeDownload();
RNBitmovinVideoManager.cancelDownload = () => RNBitmovinVideoManagerModule.cancelDownload();
RNBitmovinVideoManager.getState = (url) => RNBitmovinVideoManagerModule.getState(url);

allEvents.forEach(event => {
  RNBitmovinVideoManager[event] = (callback) => {
    const nativeEvent = event;
    if (!nativeEvent) {
      throw new Error("Invalid event");
    }

    EventEmitter.removeAllListeners(nativeEvent);
    return EventEmitter.addListener(nativeEvent, callback);
  }
});

export default RNBitmovinVideoManager;