//
//  DetailViewController.h
//
//  Created by Ali on 23/12/13.
//  Copyright (c) 2015 Ali. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "Translation.h"
#import <Foundation/Foundation.h>

@interface DetailViewController : UIViewController

@property (weak, nonatomic) IBOutlet UITextView *taiyl;
@property (nonatomic) BOOL isTyvRus;
@property (nonatomic) BOOL isSostuk;
@property (nonatomic, strong) Translation *translation;

@end
