#import "FirebaseRestAuthPlugin.h"
#if __has_include(<firebase_rest_auth/firebase_rest_auth-Swift.h>)
#import <firebase_rest_auth/firebase_rest_auth-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "firebase_rest_auth-Swift.h"
#endif

@implementation FirebaseRestAuthPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFirebaseRestAuthPlugin registerWithRegistrar:registrar];
}
@end
