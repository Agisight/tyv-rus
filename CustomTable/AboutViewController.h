//
//  AboutViewController.h
//
//  Created by Ali on 24/12/13.
//  Copyright (c) 2015 Ali. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface AboutViewController : UIViewController <UIWebViewDelegate>

@property (nonatomic, strong) IBOutlet UIWebView *webView;

@end
