//
//  TyvanServerController.m
//  tyv-rus
//
//  Created by Али Кужугет on 08/03/2019.
//  Copyright © 2019 Appcoda. All rights reserved.
//

// Working with my Server www.tyvan.ru
#import "TyvanServerController.h"
#import "TranslationViewController.h"
#import "Translation.h"

@implementation TyvanServerController // dont worry about warnings, no need to create abstract methods, which will be rewrited in heirs

- (void)viewDidLoad {
    [super viewDidLoad];
    
    // Subscribe to keyboard notifications
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(keyboardWillShow:)
                                                 name:UIKeyboardWillShowNotification
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(keyboardWillHide:)
                                                 name:UIKeyboardWillHideNotification
                                               object:nil];
}

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

#pragma mark - handle UIKeyboard

- (void)keyboardWillShow:(NSNotification *)notification { // define it in subclasses
}

- (void)keyboardWillHide:(NSNotification *)notification { // define it in subclasses
}




#pragma mark - Server methods
- (void)getServer:(NSString *)urlString with: (BOOL) isTyvRus {
    NSURL * url = [[NSURL alloc] initWithString:urlString];
    
    NSURLSessionConfiguration *defaultConfig = [NSURLSessionConfiguration defaultSessionConfiguration];
    __weak typeof(self) welf = self;
    NSURLSession *session = [NSURLSession sessionWithConfiguration:defaultConfig];
    NSURLSessionDataTask *dataTask = [session dataTaskWithURL:url completionHandler:^(NSData *data,
                                                                                      NSURLResponse *response,
                                                                                      NSError *error) {
        NSHTTPURLResponse *httpResp = (NSHTTPURLResponse*) response;
        if (!error) {
            if (httpResp.statusCode == 200 && data.length > 0) {
                // handle the response here
                [welf handleAfterResponseWith:data and:isTyvRus];
            }
        } else NSLog(@"%@", error);
    }
                                      ];
    [dataTask resume];
}

-(void) handleAfterResponseWith: (NSData*) data and: (BOOL) isTyvRus {
    TranslationViewController * tableVC = (TranslationViewController *) self;
    
    NSError *er = nil;
    NSDictionary *jsonArray = [NSJSONSerialization JSONObjectWithData:data options:kNilOptions error:&er];
    
    if (!er && jsonArray.count > 0) {
        NSArray * a = (NSArray *) [jsonArray objectForKey:@"data"];
        
        int i = 0;
        NSMutableArray *words = [[NSMutableArray alloc] init];
        for (NSObject *obj in a) {
            Translation *r = [Translation new];
            r.tyv = [obj valueForKey:@"tyv"];
            
            r.ru = [obj valueForKey:@"ru"];
            r.name = isTyvRus ? r.ru : r.tyv;
            [r trim];
            [words addObject:r];
            i++;
        }
        
        tableVC.searchResults = [NSArray arrayWithArray:words];
    }
    
    __weak typeof(self) welf = self;
    dispatch_async(dispatch_get_main_queue(), ^{
        [welf.tableView reloadData];
    });
}

- (NSArray *)searchFromServer:(NSString *)sentence with: (BOOL) isTyvRus {
    NSString * unescaped = [sentence stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
    NSString *postString = [NSString stringWithFormat:@"sos=%@&dir=%d&approved=false&getSos=1", unescaped, !isTyvRus];
    NSString *urlString = @"https://tyvan.ru/translate/get.php";
    urlString = [NSString stringWithFormat:@"%@?%@", urlString, postString];
    [self getServer:urlString with:isTyvRus];
    return [NSArray array];
}


@end
