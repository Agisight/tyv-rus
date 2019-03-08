//
//  Translation.m
//
//  Created by Ali on 25/12/15.
//  Copyright (c) 2015 Ali. All rights reserved.
//

#import "Translation.h"

@implementation Translation

-(void) trim {
    NSCharacterSet * set = NSCharacterSet.whitespaceAndNewlineCharacterSet;
    self.ru = [self.ru stringByTrimmingCharactersInSet:set];
    self.tyv = [self.tyv stringByTrimmingCharactersInSet:set];
    self.name = [self.name stringByTrimmingCharactersInSet:set];
}
@end
