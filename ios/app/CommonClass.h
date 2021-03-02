//
//  CommonClass.h
//  app
//
//  Created by MacMini on 10/23/20.
//  Copyright © 2020 Facebook. All rights reserved.
//
#import <Foundation/Foundation.h>

@interface CommonClass : NSObject {
}
+ (CommonClass *)sharedObject;
//@property  NSString *UniqueToken;
@property (atomic, strong, readwrite)  NSString *UniqueToken;
@end
