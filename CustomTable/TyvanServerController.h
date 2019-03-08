//
//  TyvanServerController.h
//  tyv-rus
//
//  Created by Али Кужугет on 08/03/2019.
//  Copyright © 2019 Appcoda. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface TyvanServerController : UITableViewController
@property BOOL isSostuk;
@property BOOL isTyvRus;
@property NSArray *searchResults;

#pragma mark - abstract methods
- (NSInteger) getNumberOfRowsInSection:(NSInteger)section;
- (CGFloat) getCellHeight;
- (UITableViewCell *) getCellForRowAtIndexPath:(NSIndexPath *)indexPath;
- (void) configure:(UIStoryboardSegue *) segue;
- (void)keyboardWillShow:(NSNotification *)notification;
- (void)keyboardWillHide:(NSNotification *)notification;


#pragma mark - Server methods
- (NSArray *)searchFromServer:(NSString *)sentence with: (BOOL) isTyvRus;
- (void)getServer:(NSString *)urlString with: (BOOL) isTyvRus;
@end

NS_ASSUME_NONNULL_END
