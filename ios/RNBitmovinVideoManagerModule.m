#import <Foundation/Foundation.h>
#import "RNBitmovinVideoManagerModule.h"
#import <BitmovinPlayer/BMPOfflineManagerListener.h>
#import <React/RCTLog.h>

BMPSourceItem *sourceItem;

@implementation RNBitmovinVideoManagerModule

RCT_EXPORT_MODULE();

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"onDownloadCompleted", @"onDownloadProgress", @"onDownloadError", @"onDownloadCanceled", @"onDownloadSuspended", @"onState"];
}

RCT_EXPORT_METHOD(download: (nonnull NSDictionary *)configuration){
    BMPOfflineManager *offlineManager = [BMPOfflineManager sharedInstance];
    
    if (!configuration[@"url"]) {
        [self sendEventWithName:@"onDownloadError" body:@"URL is not provided"];
        return ;
    }

    BMPHLSSource *hlsSource = [[BMPHLSSource alloc] initWithUrl:[NSURL URLWithString:configuration[@"url"]]];

    sourceItem = [[BMPSourceItem alloc] initWithHLSSource:hlsSource];

    // Fairplay config
    BMPFairplayConfiguration *fairplayConfig = [[BMPFairplayConfiguration alloc] initWithLicenseUrl:[NSURL URLWithString:@"https://lic.drmtoday.com/license-server-fairplay/?offline=true"] certificateURL:[NSURL URLWithString:@"https://lic.drmtoday.com/license-server-fairplay/cert/kinow_lacinetek"]];
    
    NSString *userDataString = [NSString stringWithFormat:@"{\"userId\":\"%@\", \"sessionId\":\"%@\", \"merchant\":\"kinow_lacinetek\"}", configuration[@"userId"], configuration[@"sessionId"]];
    NSData *userData = [userDataString dataUsingEncoding:NSUTF8StringEncoding];
    NSString *userDataBase64 = [userData base64EncodedStringWithOptions:0];

    fairplayConfig.licenseRequestHeaders = [NSDictionary dictionaryWithObject:userDataBase64 forKey:@"x-dt-custom-data"];
    
    [fairplayConfig setPrepareContentId:^NSString * _Nonnull(NSString * _Nonnull contentId) {
        NSString *pattern = @"skd://drmtoday?";
        NSString *contentIdNew = [contentId substringFromIndex:pattern.length];
        return contentIdNew;
    }];
    
    [fairplayConfig setPrepareMessage:^NSData * _Nonnull(NSData * _Nonnull spcData, NSString * _Nonnull assetID) {
        NSString *base64String = [spcData base64EncodedStringWithOptions:NSDataBase64Encoding64CharacterLineLength];
        NSString *uriEncodedMessage = [base64String stringByAddingPercentEncodingWithAllowedCharacters:NSCharacterSet.alphanumericCharacterSet];
        
        NSString *message = [NSString stringWithFormat:@"spc=%@&%@", uriEncodedMessage, assetID];

        RCTLog(@"%@", message);

        return [message dataUsingEncoding:NSUTF8StringEncoding];
    }];

    [sourceItem addDRMConfiguration:fairplayConfig];

    Boolean isPlayable = [offlineManager isSourceItemPlayableOffline:(BMPSourceItem *) sourceItem];
    Boolean downloaded = [offlineManager offlineStateForSourceItem:sourceItem] == 0;

    if (isPlayable && downloaded){
        [self sendEventWithName:@"onDownloadCompleted" body:@{@"source": [sourceItem toJsonData]}];
        return ;
    }

//    check if the status is downloaded
    /*if (downloaded) {
        [offlineManager deleteOfflineDataForSourceItem:sourceItem];
    }*/

    [offlineManager addListener:self forSourceItem:sourceItem];
    
    BMPDownloadConfiguration *config = [[BMPDownloadConfiguration alloc] init];
    config.minimumBitrate = [NSNumber numberWithInt:3990870];
    [offlineManager downloadSourceItem:sourceItem downloadConfiguration:config];

    [offlineManager syncOfflineDrmLicenseInformationForSourceItem:sourceItem];
}

- (void)offlineManager:(nonnull BMPOfflineManager *)offlineManager didFailWithError:(nullable NSError *)error {
    [self sendEventWithName:@"onDownloadError" body:[error localizedDescription]];
}

- (void)offlineManager:(nonnull BMPOfflineManager *)offlineManager didProgressTo:(double)progress {
    if (progress > 100) {
        [self sendEventWithName:@"onDownloadProgress" body:[NSNumber numberWithInt:100]];
    } else {
        [self sendEventWithName:@"onDownloadProgress" body:[NSNumber numberWithDouble:progress]];
    }
}

- (void)offlineManager:(nonnull BMPOfflineManager *)offlineManager didResumeDownloadWithProgress:(double)progress {
    [self sendEventWithName:@"onDownloadSuspended" body:@false];
    [self sendEventWithName:@"onDownloadProgress" body:[NSNumber numberWithDouble:progress]];
}

- (void)offlineManagerDidCancelDownload:(nonnull BMPOfflineManager *)offlineManager {
    [self sendEventWithName:@"onDownloadCanceled" body:@{@"": @""}];
}

- (void)offlineManagerDidFinishDownload:(nonnull BMPOfflineManager *)offlineManager {
    [self sendEventWithName:@"onDownloadCompleted" body:@{@"source": [sourceItem toJsonData]}];
}

- (void)offlineManagerDidRenewOfflineLicense:(nonnull BMPOfflineManager *)offlineManager {
    RCTLog(@"DidRenewOfflineLicense ok !");
}

- (void)offlineManagerDidSuspendDownload:(nonnull BMPOfflineManager *)offlineManager {
    [self sendEventWithName:@"onDownloadSuspended" body:@true];
}

- (void)offlineManager:(BMPOfflineManager *)offlineManager didFetchAvailableTracks:(BMPOfflineTrackSelection *)tracks {
    RCTLog(@"%lu", tracks.textTracks.count);
    RCTLog(@"%@", tracks.textTracks[0].label);
}

- (void)offlineManagerOfflineLicenseDidExpire:(nonnull BMPOfflineManager *)offlineManager {
    RCTLog(@"Offline License Did Expire");
}


RCT_EXPORT_METHOD(getState: (nonnull NSString *) url ) {
    BMPOfflineManager *offlineManager = [BMPOfflineManager sharedInstance];
    
    sourceItem = [[BMPSourceItem alloc] initWithUrl:(NSURL *) [NSURL URLWithString:url]];

    switch ([offlineManager offlineStateForSourceItem:sourceItem]) {
        case 0:
            [self sendEventWithName:@"onState" body:@"DOWNLOADED"];
            break;
        case 1:
            [self sendEventWithName:@"onState" body:@"DOWNLOADING"];
            break;
        case 2:
            [self sendEventWithName:@"onState" body:@"SUSPENDED"];
            break;
        case 3:
            [self sendEventWithName:@"onState" body:@"NOT_DOWNLOADED"];
            break;
        case 4:
            [self sendEventWithName:@"onState" body:@"CANCELING"];
            break;
        default:
            [self sendEventWithName:@"onState" body:@"STATE_UNAVAILABLE"];
            break;
    }
}

RCT_EXPORT_METHOD(delete: (nonnull NSDictionary *)itemToDelete) {
    if (!itemToDelete[@"source"]) {
        [self sendEventWithName:@"onDeleteError" body:@"Source is not provided"];
        return ;
    }
    
    BMPOfflineManager *offlineManager = [BMPOfflineManager sharedInstance];

    BMPSourceItem *sourceItemToDelete = [BMPSourceItem fromJsonData:itemToDelete[@"source"] error:NULL ];
    [offlineManager deleteOfflineDataForSourceItem:sourceItemToDelete];
}

RCT_EXPORT_METHOD(pauseDownload) {
    if(!sourceItem) {
        return ;
    }
    
    BMPOfflineManager *offlineManager = [BMPOfflineManager sharedInstance];

    [offlineManager suspendDownloadForSourceItem:sourceItem];
}

RCT_EXPORT_METHOD(resumeDownload) {
    if(!sourceItem) {
        return ;
    }
    
    BMPOfflineManager *offlineManager = [BMPOfflineManager sharedInstance];

    [offlineManager resumeDownloadForSourceItem:sourceItem];
}

RCT_EXPORT_METHOD(cancelDownload) {
    if(!sourceItem) {
        return ;
    }
    
    BMPOfflineManager *offlineManager = [BMPOfflineManager sharedInstance];

    [offlineManager cancelDownloadForSourceItem:sourceItem];
}



@end
