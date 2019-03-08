//
//  MyUITabBarC.m
//  tyv-rus
//
//  Created by Ali Kuzhuget on 26.07.15.
//  Copyright (c) 2015 Ali. All rights reserved.
//

#import "MyUITabBarC.h"

@implementation MyUITabBarC

- (void)viewDidLoad {
    [super viewDidLoad];
    [self initialSettings];
}

-(void) initialSettings {
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone) {
        for (UITabBarItem *tb in self.tabBar.items) {
            int fontSize = 14;
            [tb setTitleTextAttributes:[NSDictionary dictionaryWithObjectsAndKeys:
                                        [UIFont systemFontOfSize: fontSize], NSFontAttributeName, nil] forState:UIControlStateNormal];
            [tb setTitlePositionAdjustment:UIOffsetMake(0.0, fontSize/2 - [self.tabBar frame].size.height/2)];
        }
    }
}

@end
