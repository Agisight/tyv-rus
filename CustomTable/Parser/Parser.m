#import "Parser.h"

@implementation Parser

-(NSAttributedString*)attrStringFromMarkup:(NSString*)markup
{
    NSString *htmlString = markup;
    
    NSAttributedString *as= [[NSAttributedString alloc] initWithData:[htmlString dataUsingEncoding:NSUTF8StringEncoding]
                                                             options:@{NSDocumentTypeDocumentAttribute: NSHTMLTextDocumentType,
                                                                       NSCharacterEncodingDocumentAttribute: @(NSUTF8StringEncoding)}
                                                  documentAttributes:nil error:nil];
    
    return as;
}

@end

