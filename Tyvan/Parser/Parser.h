#import <CoreText/CoreText.h>

@interface Parser : NSObject

-(NSAttributedString*) attrStringFromMarkup:(NSString*) html;

@end
