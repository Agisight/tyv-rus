//
//  AboutViewController.m
//
//  Created by Ali on 24/12/13.
//  Copyright (c) 2015 Ali. All rights reserved.
//

#import "AboutViewController.h"

@implementation AboutViewController

- (BOOL)webView:(UIWebView *)webView shouldStartLoadWithRequest:(NSURLRequest *)request navigationType:(UIWebViewNavigationType)navigationType {
    if (navigationType == UIWebViewNavigationTypeLinkClicked ) {
        UIApplication *application = [UIApplication sharedApplication];
        [application openURL:[request URL] options:@{} completionHandler:nil];
        return NO;
    }
    
    return YES;
}

- (IBAction)goToSite:(UIButton *)sender {
    [[UIApplication sharedApplication]
     openURL:[NSURL URLWithString: @"https://www.tyvan.ru"]];
}

@end

