/*
 * Copyright @ 2018-present 8x8, Inc.
 * Copyright @ 2017-2018 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import <Availability.h>

@import CoreSpotlight;
@import MobileCoreServices;
@import Intents;  // Needed for NSUserActivity suggestedInvocationPhrase

@import JitsiMeet;

#import "Types.h"
#import "ViewController.h"
#import "AppDelegate.h"
#import "CommonClass.h"


@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];

    JitsiMeetView *view = (JitsiMeetView *) self.view;
    view.delegate = self;

    [view join:[[JitsiMeet sharedInstance] getInitialConferenceOptions]];
}

// JitsiMeetViewDelegate

- (void)_onJitsiMeetViewDelegateEvent:(NSString *)name
                             withData:(NSDictionary *)data {
    NSLog(
        @"[%s:%d] JitsiMeetViewDelegate %@ %@",
        __FILE__, __LINE__, name, data);

#if DEBUG
    NSAssert(
        [NSThread isMainThread],
        @"JitsiMeetViewDelegate %@ method invoked on a non-main thread",
        name);
#endif
}

- (void)conferenceJoined:(NSDictionary *)data {
    [self _onJitsiMeetViewDelegateEvent:@"CONFERENCE_JOINED" withData:data];

    // Register a NSUserActivity for this conference so it can be invoked as a
    // Siri shortcut. This is only supported in iOS >= 12.
#ifdef __IPHONE_12_0
    if (@available(iOS 12.0, *)) {
      NSUserActivity *userActivity
        = [[NSUserActivity alloc] initWithActivityType:JitsiMeetConferenceActivityType];

      NSString *urlStr = data[@"url"];
      NSURL *url = [NSURL URLWithString:urlStr];
      NSString *conference = [url.pathComponents lastObject];

      userActivity.title = [NSString stringWithFormat:@"Join %@", conference];
      userActivity.suggestedInvocationPhrase = @"Join my Jitsi meeting";
      userActivity.userInfo = @{@"url": urlStr};
      [userActivity setEligibleForSearch:YES];
      [userActivity setEligibleForPrediction:YES];
      [userActivity setPersistentIdentifier:urlStr];

      // Subtitle
      CSSearchableItemAttributeSet *attributes
        = [[CSSearchableItemAttributeSet alloc] initWithItemContentType:(NSString *)kUTTypeItem];
      attributes.contentDescription = urlStr;
      userActivity.contentAttributeSet = attributes;

      self.userActivity = userActivity;
      [userActivity becomeCurrent];
    }
#endif
  
  [self sendRequestFromURL:false completion:^(NSData *data, NSURLResponse *response, NSError *error)
   {
   [self showAlert: [[NSString alloc] initWithData: data
                                          encoding: NSUTF8StringEncoding]];
     //self.responseText
  }];
  

}

- (void)conferenceTerminated:(NSDictionary *)data {
    [self _onJitsiMeetViewDelegateEvent:@"CONFERENCE_TERMINATED" withData:data];
  [self sendRequestFromURL: true completion:^(NSData *data, NSURLResponse *response, NSError *error)
   {
   [self showAlert: [[NSString alloc] initWithData: data
                                          encoding: NSUTF8StringEncoding]];
     //self.responseText
  }];
}

- (void)conferenceWillJoin:(NSDictionary *)data {
    [self _onJitsiMeetViewDelegateEvent:@"CONFERENCE_WILL_JOIN" withData:data];
}

#if 0
- (void)enterPictureInPicture:(NSDictionary *)data {
    [self _onJitsiMeetViewDelegateEvent:@"ENTER_PICTURE_IN_PICTURE" withData:data];
}
#endif

#pragma mark - Helpers

- (void)terminate {
  
  [self sendRequestFromURL: true completion:^(NSData *data, NSURLResponse *response, NSError *error)
   {
   [self showAlert: [[NSString alloc] initWithData: data
                                          encoding: NSUTF8StringEncoding]];
     //self.responseText
  }];
    JitsiMeetView *view = (JitsiMeetView *) self.view;
    [view leave];
}




- (void) showAlert:(NSString *)msg
{
  
  //ViewController *rootController = (ViewController *)self.window.rootViewController;
 
  
  NSLog(@"Alerrrrrrrrrrrrrrrrrrrt :   %@", msg);
  
  
  /*
  UIAlertController *alert = [UIAlertController
                               alertControllerWithTitle:@"test message"
                              message:msg
                              preferredStyle:UIAlertControllerStyleAlert
                        ];
  
  UIAlertAction* noButton = [UIAlertAction
                             actionWithTitle:@"Cancel"
                             style:UIAlertActionStyleDefault
                             handler:^(UIAlertAction * action)
                             {
                                 [alert dismissViewControllerAnimated:YES completion:nil];
                             }];
  [alert addAction:noButton];
  [self presentViewController:alert animated:YES completion:nil];
  */
  
  
  
  
  
  //[self presentViewController:alertvc animated:YES completion:nil];
}

NSString *responseText=@"rrrr";

  



- (void)sendRequestFromURL
                          :(BOOL ) isLeft
      completion:  (void (^)(NSData *data, NSURLResponse *response, NSError *error)) completion
  {
    NSString * url= @"http://192.168.120.5:45455/Testing";
    
    // Get the shared instance
   // AppDelegate *appdelegate = [AppDelegate sharedInstance];
   
    
    
    //NSString *token=  [appdelegate getUniqueToken];
    NSString *token= [CommonClass sharedObject].UniqueToken;
    [self showAlert:token];
    NSString *isLeftString = isLeft ? @"true" : @"false";
    url=[NSString stringWithFormat:@"https://cnn.eppm.com.tn/SofticwMeet/API/WmeetApi/LogParticipantWithToken?token=%@&left=%@",token, isLeftString];

    
    NSURL *myURL = [NSURL URLWithString: url];
    NSMutableURLRequest  *request = [[NSMutableURLRequest  alloc] initWithURL: myURL];
      NSURLSession *session = [NSURLSession sharedSession];

    [request setValue:@"true" forHTTPHeaderField:@"IsWmeetMobile"];
    
    
      NSURLSessionDataTask *task = [session dataTaskWithRequest: request
        completionHandler:^(NSData *data, NSURLResponse *response, NSError *error)
        {
        responseText = [[NSString alloc] initWithData: data
            encoding: NSUTF8StringEncoding];
          if (completion != nil)
          {
             //The data task's completion block runs on a background thread
             //by default, so invoke the completion handler on the main thread
             //for safety
            //dispatch_async(dispatch_get_main_queue(), completion(data,response,error));
            completion(data,response,error);
          }
        }];
        [task resume];
  

}




@end
