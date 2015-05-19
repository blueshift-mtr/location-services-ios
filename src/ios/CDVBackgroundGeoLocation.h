#import <Cordova/CDVPlugin.h>
#import "CDVLocation.h"
#import <AudioToolbox/AudioToolbox.h>

@interface CDVBackgroundGeoLocation : CDVPlugin <CLLocationManagerDelegate>

@property (nonatomic, strong) NSString* syncCallbackId;

- (void) configure:(CDVInvokedUrlCommand*)command;
- (void) init:(CDVInvokedUrlCommand*)command;
- (void) start:(CDVInvokedUrlCommand*)command;
- (void) stop:(CDVInvokedUrlCommand*)command;
- (void) finish:(CDVInvokedUrlCommand*)command;
- (void) onSuspend:(NSNotification *)notification;
- (void) onResume:(NSNotification *)notification;

@end

