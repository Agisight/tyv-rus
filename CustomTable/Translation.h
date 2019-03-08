//
//  Translation.h
//
//  Created by Ali on 25/12/15.
//  Copyright (c) 2015 Ali. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface Translation : NSObject

@property (nonatomic, strong) NSString *name; // name
@property (nonatomic, strong) NSString *nextName; // preparation time
@property (nonatomic, strong) NSString *image; // image filename of translation
@property (nonatomic, strong) NSString *tyv; // tyvan language
@property (nonatomic, strong) NSString *ru; // russian language

-(void) trim;
@end
