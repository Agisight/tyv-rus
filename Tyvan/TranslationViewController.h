//
//  TranslationViewController.h
//
//  Created by Ali on 7/12/13.
//  Copyright (c) 2015 Ali. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "TyvanServerController.h"
@interface TranslationViewController : TyvanServerController <UISearchBarDelegate, UISearchResultsUpdating>
    
@property NSArray *translations;
@property NSMutableArray *words;
@end
