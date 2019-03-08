//
//  TranslationViewController.m
//
//  Created by Ali on 7/12/13.
//  Copyright (c) 2015 Ali. All rights reserved.
//

#import "TranslationViewController.h"
#import "CYRKeyboardButton.h"
#import "TableCell.h"
#import "DetailViewController.h"

@interface TranslationViewController ()
@property NSString* adata;
@property (nonatomic, strong) NSMutableArray *keyboardButtons;
@property (nonatomic, strong) UIInputView *numberView;
@property (nonatomic, strong) UITextField *textView;
@property (retain, nonatomic) UISearchController *searchController;
@end

@implementation TranslationViewController
{
    NSArray *translations, *utka;
    NSMutableArray *words;
    NSMutableArray *soster;
    CGFloat fontSize;
    IBOutlet UITableView *tv;
}

@synthesize isSostuk, isTyvRus, searchResults;

- (void)viewDidLoad
{
    [super viewDidLoad];
    fontSize = UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone ? 18 : 24;
    isSostuk = [self.title isEqualToString:@"Сөстүк"];
    if (isSostuk) {
        [self firstSettings];
    } else {
        isTyvRus = self.view.tag == 0;
    }
    
    [self initSearcher];
    if (isSostuk || isTyvRus) [self initKeyboardAccessory];
}

- (void)viewWillAppear:(BOOL)animated {
    NSString *cancelTxt = (isSostuk || isTyvRus) ? @"Ойталал" : @"Отмена";
    [[UIBarButtonItem appearanceWhenContainedIn:[UISearchBar class], nil] setTitle:cancelTxt];
}

- (void) initSearcher {
    self.searchController = [[UISearchController alloc] initWithSearchResultsController:nil];
    self.searchController.searchBar.delegate = self;
    self.searchController.searchResultsUpdater = self;
    [self.searchController.searchBar sizeToFit];
    self.searchController.dimsBackgroundDuringPresentation = NO;
    self.definesPresentationContext = YES;
    self.tableView.tableHeaderView = self.searchController.searchBar;
    NSString *backTxt = (isSostuk || isTyvRus) ? @"Дедир" : @"Назад";
    NSString *placeholderTxt = (isSostuk || isTyvRus) ? @"Сөстү киириңер" : @"Введите слово";
    NSString *cancelTxt = (isSostuk || isTyvRus) ? @"Ойталал" : @"Отмена";
    
    self.navigationItem.backBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:backTxt style:UIBarButtonItemStylePlain target:nil action:nil];
    self.searchController.searchBar.placeholder = placeholderTxt;
    
    for (UIView *view in self.searchController.searchBar.subviews)
    {
        for (id subview in view.subviews)
        {
            if ( [subview isKindOfClass:[UIButton class]] )
            {
                UIButton *cancelButton = (UIButton*)subview;
                [cancelButton setTitle:cancelTxt forState:UIControlStateNormal];
            }
        }
    }
}

/// Add additional key buttons on keyboard layout as accessory
- (void) initKeyboardAccessory {
    NSArray *keys = @[@"-", @"Ө", @"ө", @"ң", @"ү", @"Ү", @"Ң"];
    self.keyboardButtons = [NSMutableArray arrayWithCapacity:keys.count];
    
    self.numberView = [[UIInputView alloc] initWithFrame:CGRectMake(0, 0, CGRectGetWidth(self.view.frame), 40) inputViewStyle:UIInputViewStyleKeyboard];
    
    self.textView = [self.searchController.searchBar valueForKey:@"_searchField"];
    //if (!self.textView) return;
    [keys enumerateObjectsUsingBlock:^(NSString *keyString, NSUInteger idx, BOOL *stop) {
        CYRKeyboardButton *keyboardButton = [CYRKeyboardButton new];
        keyboardButton.translatesAutoresizingMaskIntoConstraints = NO;
        keyboardButton.input = keyString;
        //keyboardButton.inputOptions = @[@"A", @"B", @"C", @"D"];
        keyboardButton.textInput = self.textView;
        
        [self.numberView addSubview:keyboardButton];
        
        [self.keyboardButtons addObject:keyboardButton];
    }];
    
    [self updateConstraintsForOrientation:[[UIApplication sharedApplication] statusBarOrientation]];
    self.textView.inputAccessoryView = self.numberView;
}

- (void) firstSettings {
    words = [[NSMutableArray alloc] init];
    NSString * fName = [[NSBundle mainBundle] pathForResource:@"words" ofType:@"strings"];
    if (fName) {
        NSString *fh = [NSString stringWithContentsOfFile:fName encoding:NSUTF8StringEncoding error:NULL];
        NSArray *filearray =[fh componentsSeparatedByString:@"\n"];
        int i = 0;
        for (NSString *line in filearray) {
            Translation *r = [Translation new];
            r.name = line;
            if (i+2<filearray.count) r.nextName = [filearray objectAtIndex:i+1];
            else r.nextName = @"end";
            [words addObject:r];
            i++;
        }
    }
    
    translations = [[NSArray alloc] initWithArray:words];
}



#pragma mark - definitions of abstract methods of super class

- (NSInteger) getNumberOfRowsInSection:(NSInteger)section {
    if(self.searchController.active){
        if (searchResults.count == 0)
            for (UIView *view in self.tableView.subviews) {
                if ([view isKindOfClass:[UILabel class]])
                    ((UILabel *)view).text = @"Ындыг сөс чок";
            }
        return [searchResults count];
    }else{
        return [translations count];
    }
}

- (CGFloat) getCellHeight {
    CGFloat size = UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone ? 71 : 100;
    return isSostuk ? 71 : size;
}

- (UITableViewCell *) getCellForRowAtIndexPath:(NSIndexPath *)indexPath {
    static NSString *CellIdentifier = @"TableCell";
    TableCell *cell = (TableCell *)[self.tableView dequeueReusableCellWithIdentifier:CellIdentifier];
    
    // Configure the cell...
    if (cell == nil) {
        cell = [[TableCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:CellIdentifier];
    }
    
    // Display obj in the table cell
    Translation *translation = nil;
    
    if (self.searchController.active) {
        translation = [searchResults objectAtIndex:indexPath.row];
    } else {
        translation = [translations objectAtIndex:indexPath.row];
    }
    cell.nameLabel.text = translation.name;
    
    UIFont *f = cell.nameLabel.font;
    CGFloat size = UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone ? 18 : 24;
    f = [f fontWithSize:size];
    
    cell.nameLabel.font = f;
    return cell;
}

- (void) configure:(UIStoryboardSegue *) segue {
    if ([segue.identifier isEqualToString:@"showDetail"]) {
        NSIndexPath *indexPath = nil;
        Translation *translation = nil;
        
        if (self.searchController.active) {
            indexPath = [self.tableView indexPathForSelectedRow];
            translation = [searchResults objectAtIndex:indexPath.row];
        } else {
            indexPath = [self.tableView indexPathForSelectedRow];
            translation = [translations objectAtIndex:indexPath.row];
        }
        
        DetailViewController *destViewController = segue.destinationViewController;
        destViewController.translation = translation;
        destViewController.isTyvRus = isTyvRus;
        destViewController.isSostuk = isSostuk;
    }
}






- (void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    [CATransaction begin];
    [self updateConstraintsForOrientation:toInterfaceOrientation];
    [CATransaction commit];
    
    __weak TranslationViewController * welf = self;
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(duration * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        for (UITabBarItem *tb in welf.tabBarController.tabBar.items) {
            int fontSize = 20;
            [tb setTitleTextAttributes:[NSDictionary dictionaryWithObjectsAndKeys:
                                        [UIFont systemFontOfSize: fontSize], NSFontAttributeName, nil] forState:UIControlStateNormal];
            [tb setTitlePositionAdjustment:UIOffsetMake(0.0, fontSize/2 - [welf.tabBarController.tabBar frame].size.height/2)];
        }
    });
}

#pragma mark - Constraint Management of accessory keyboard

- (void)updateConstraintsForOrientation:(UIInterfaceOrientation)orientation
{
    if (isSostuk || isTyvRus) [self rotateKeyboardView: orientation];
}

/// Handle changes of constraints on rotation
- (void) rotateKeyboardView:(UIInterfaceOrientation) orientation {
    // Remove any existing constraints
    [self.numberView removeConstraints:self.numberView.constraints];
    
    // Create our constraints
    NSMutableDictionary *bindings = [NSMutableDictionary dictionary];
    NSMutableString *visualFormatConstants = [NSMutableString string];
    NSDictionary *metrics = nil;
    
    // Setup our metrics based on orientation
    if (UIInterfaceOrientationIsPortrait(orientation)) {
        metrics = @{
                    @"margin" : @(3),
                    @"spacing" : @(6)
                    };
    } else {
        metrics = @{
                    @"margin" : @(22),
                    @"spacing" : @(5)
                    };
    }
    
    // Build the visual format string
    [self.keyboardButtons enumerateObjectsUsingBlock:^(CYRKeyboardButton *button, NSUInteger idx, BOOL *stop) {
        NSString *binding = [NSString stringWithFormat:@"keyboardButton%lu", (unsigned long)idx];
        [bindings setObject:button forKey:binding];
        
        if (idx == 0) {
            [visualFormatConstants appendString:[NSString stringWithFormat:@"H:|-margin-[%@]", binding]];
        } else if (idx < self.keyboardButtons.count - 1) {
            [visualFormatConstants appendString:[NSString stringWithFormat:@"-spacing-[%@]", binding]];
        } else {
            [visualFormatConstants appendString:[NSString stringWithFormat:@"-spacing-[%@]-margin-|", binding]];
        }
    }];
    
    // Apply horizontal constraints
    [self.numberView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:visualFormatConstants options:0 metrics:metrics views:bindings]];
    
    // Apply vertical constraints
    [bindings enumerateKeysAndObjectsUsingBlock:^(NSString *binding, id obj, BOOL *stop) {
        NSString *format = [NSString stringWithFormat:@"V:|-6-[%@]|", binding];
        [self.numberView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:format options:0 metrics:nil views:bindings]];
    }];
    
    // Add width constraint
    [self.keyboardButtons enumerateObjectsUsingBlock:^(CYRKeyboardButton *button, NSUInteger idx, BOOL *stop) {
        if (idx > 0) {
            CYRKeyboardButton *previousButton = self.keyboardButtons[idx - 1];
            
            [self.numberView addConstraint:[NSLayoutConstraint constraintWithItem:button attribute:NSLayoutAttributeWidth relatedBy:NSLayoutRelationEqual toItem:previousButton attribute:NSLayoutAttributeWidth multiplier:1 constant:0]];
        }
    }];
}

#pragma mark - UITextViewDelegate

- (void)textViewDidBeginEditing:(UITextView *)textView
{
    UIBarButtonItem *doneButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemDone target:textView action:@selector(resignFirstResponder)];
    
    [self.navigationItem setRightBarButtonItem:doneButton animated:YES];
}

- (void)textViewDidEndEditing:(UITextView *)textView
{
    [self.navigationItem setRightBarButtonItem:nil animated:YES];
}

#pragma mark - handle UIKeyboard

- (void)keyboardWillShow:(NSNotification *)notification
{
    if ([self.textView isFirstResponder]) {
        NSDictionary *info = [notification userInfo];
        //        CGSize kbSize = [[info objectForKey:UIKeyboardFrameBeginUserInfoKey] CGRectValue].size;
        CGFloat duration = [[info objectForKey:UIKeyboardAnimationDurationUserInfoKey] doubleValue];
        
        [UIView animateWithDuration:duration
                         animations:^{
                             // TODO : animations if needed
                             //                             self.textView.contentInset = UIEdgeInsetsMake(self.textView.contentInset.top, self.textView.contentInset.left, kbSize.height, 0);
                             //                             self.textView.scrollIndicatorInsets = UIEdgeInsetsMake(self.textView.contentInset.top, self.textView.scrollIndicatorInsets.left, kbSize.height, 0);
                         }];
    }
}

- (void)keyboardWillHide:(NSNotification *)notification
{
    if ([self.textView isFirstResponder]) {
        NSDictionary *info = [notification userInfo];
        CGFloat duration = [[info objectForKey:UIKeyboardAnimationDurationUserInfoKey] doubleValue];
        
        [UIView animateWithDuration:duration
                         animations:^{
                             //                             self.textView.contentInset = UIEdgeInsetsMake(self.textView.contentInset.top, self.textView.contentInset.left, 0, 0);
                             //                             self.textView.scrollIndicatorInsets = UIEdgeInsetsMake(self.textView.contentInset.top, self.textView.scrollIndicatorInsets.left, 0, 0);
                         }];
    }
}

#pragma mark - Searching & Flitering
- (void) filterContentForSearchText:(NSString*)searchText scope:(NSString*)scope
{
    NSPredicate *resultPredicate = [NSPredicate predicateWithFormat:@"name BEGINSWITH[c] %@", searchText];
    searchResults = [translations filteredArrayUsingPredicate:resultPredicate];
}

- (void) doSearch {
    NSString *s = [self.searchController.searchBar.text stringByTrimmingCharactersInSet:NSCharacterSet.whitespaceAndNewlineCharacterSet];
    
    if (isSostuk) {
        if (s.length>0)
            [self filterContentForSearchText:s
                                       scope:[[self.searchController.searchBar scopeButtonTitles]
                                              objectAtIndex:[self.searchController.searchBar selectedScopeButtonIndex]]];
        else searchResults = translations;
        [self.tableView reloadData];
    } else {
        if (s.length > 0) {
            [self searchFromServer:s with:isTyvRus];
        } else {
            searchResults = [NSArray array];
            [self.tableView reloadData];
        }
    }
}

- (void)updateSearchResultsForSearchController:(nonnull UISearchController *)searchController {
    [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(doSearch) object:nil];
    [self performSelector:@selector(doSearch) withObject:nil afterDelay:0.5];
}

@end
