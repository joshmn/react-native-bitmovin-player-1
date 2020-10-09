#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <BitmovinPlayer/BitmovinPlayer.h>
#import <BitmovinPlayer/BMPOfflineManager.h>
#import <BitmovinPlayer/BMPOfflineManagerListener.h>

#ifndef RNBitmovinVideoManagerModule_h
#define RNBitmovinVideoManagerModule_h


#endif /* RNBitmovinVideoManagerModule_h */


@interface RNBitmovinVideoManagerModule : RCTEventEmitter <RCTBridgeModule, BMPOfflineManagerListener>
@end