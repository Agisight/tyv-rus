//
//  DetailViewController.m
//
//  Created by Ali on 23/12/13.
//  Copyright (c) 2015 Ali. All rights reserved.
//

#import "DetailViewController.h"
#import "Parser.h"
#import <UIKit/UIKit.h>

Parser* p;
NSString *Soster;

@implementation DetailViewController

- (void) viewWillAppear:(BOOL)animated {
    if (self.isSostuk) [self handleSostuk];
    else [self handleTranslator];
}

- (void) handleTranslator {
    self.title = _isTyvRus ? @"Тыва-орус" : @"Русско-тувинский";
    
    p = [[Parser alloc] init];
    NSString * first = self.isTyvRus ? self.translation.ru : self.translation.tyv;
    NSString * second = !self.isTyvRus ? self.translation.ru : self.translation.tyv;
    NSString * content = [NSString stringWithFormat:@"%@<br>/<br>%@", first, second];
    self.taiyl.attributedText = [p attrStringFromMarkup: content];
    
    
    NSMutableAttributedString *text = [[NSMutableAttributedString alloc] initWithAttributedString: self.taiyl.attributedText];
    
    
    UIColor *blue = [UIColor colorWithDisplayP3Red:.1 green:.3 blue:0.8 alpha:1];
    UIColor *green = [UIColor colorWithDisplayP3Red:.1 green:.8 blue:.3 alpha:1];
    
    [text addAttribute:NSForegroundColorAttributeName
                 value:blue
                 range:NSMakeRange(0, first.length)];
    [text addAttribute:NSForegroundColorAttributeName
                 value:green
                 range:NSMakeRange(first.length+3, second.length)];
    
    [self.taiyl setAttributedText: text];
    
    UIFont *f = self.taiyl.font;
    CGFloat size = UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone ? 18 : 24;
    f = [f fontWithSize:size];
    self.taiyl.font = f;
}

- (void) handleSostuk {
    NSString * fName = [[NSBundle mainBundle] pathForResource:@"soster" ofType:@"strings"];
    Soster = [NSString stringWithContentsOfFile:fName encoding:NSUTF8StringEncoding error:NULL];
    
    self.title = self.translation.name;
    
    NSMutableAttributedString* wordsText = [[NSMutableAttributedString alloc] initWithString:@""];
    
    NSString *str =[NSString stringWithFormat:@"%s%@", "\n", self.title];
    
    
    NSRange range = [Soster rangeOfString:str];
    NSRange searchRange = NSMakeRange(range.location+1,Soster.length - (range.location+1));
    
    NSRange range1 = [Soster rangeOfString:[NSString stringWithFormat:@"%s%@", "\n", self.translation.nextName] options:NSCaseInsensitiveSearch range:searchRange];
    
    if (range.location>Soster.length) range.location = Soster.length;
    if (range1.location+range1.length+1>Soster.length) range1.location = Soster.length-range1.length;
    
    NSRange newrange = NSMakeRange(range.location+str.length+3, range1.location-range.location-(str.length+3));
    NSString *substring = [Soster substringWithRange:newrange];
    
    NSArray *a = [[NSUserDefaults standardUserDefaults] valueForKey:@"аа"];
    
    for (NSString *ingredient in a) {
        [wordsText insertAttributedString: [p attrStringFromMarkup:ingredient] atIndex:wordsText.length];
    }
    
    p = [[Parser alloc] init];
    self.taiyl.attributedText = [p attrStringFromMarkup:substring];
    UIFont *f = self.taiyl.font;
    CGFloat size = UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone ? 18 : 24;
    f = [f fontWithSize:size];
    self.taiyl.font = f;
}

- (IBAction) shareHasTapped: (id)sender {
    NSString *texttoshare = self.taiyl.attributedText.string;
    if (self.isSostuk) {
        texttoshare = [NSString stringWithFormat:@"%@:\n\t %@", self.title, self.taiyl.attributedText.string];
    }
    
    NSArray *activityItems = @[texttoshare];
    UIActivityViewController *activityVC = [[UIActivityViewController alloc] initWithActivityItems:activityItems applicationActivities:nil];
    
    activityVC.excludedActivityTypes = @[UIActivityTypePostToTwitter];
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone) {
        [self presentViewController: activityVC animated:YES completion:nil];
    }
    else
    {
        UIPopoverController *popup = [[UIPopoverController alloc] initWithContentViewController:activityVC];
        [popup presentPopoverFromRect:CGRectMake(self.view.frame.size.width/2, self.view.frame.size.height/4, 0, 0)inView:self.view permittedArrowDirections:UIPopoverArrowDirectionAny animated:YES];
    }
}

@end

