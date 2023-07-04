package org.xmlvm.ios;

import java.util.*;
import org.xmlvm.XMLVMSkeletonOnly;

@XMLVMSkeletonOnly
public class CMAudioFormatDescription extends CMFormatDescription {

    /**
	 * OSStatus CMAudioFormatDescriptionCreateSummary( CFAllocatorRef allocator, CFArrayRef formatDescriptionArray, uint32_t flags, CMAudioFormatDescriptionRef *summaryFormatDescriptionOut ) ;
	 */
    public static int createSummary(CFAllocator allocator, CFArray formatDescriptionArray, int flags, CMAudioFormatDescription summaryFormatDescriptionOut) {
        throw new RuntimeException("Stub");
    }

    /**
	 * OSStatus CMAudioFormatDescriptionCreate( CFAllocatorRef allocator, const AudioStreamBasicDescription *asbd, size_t layoutSize, const AudioChannelLayout *layout, size_t magicCookieSize, const void *magicCookie, CFDictionaryRef extensions, CMAudioFormatDescriptionRef *outDesc) ;
	 */
    public CMAudioFormatDescription(CFAllocator allocator, Reference<AudioStreamBasicDescription> asbd, int layoutSize, Reference<AudioChannelLayout> layout, int magicCookieSize, byte[] magicCookie, CFDictionary extensions, CMAudioFormatDescription outDesc) {
    }

    /** Default constructor */
    CMAudioFormatDescription() {
    }

    /**
	 * const AudioStreamBasicDescription * CMAudioFormatDescriptionGetStreamBasicDescription( CMAudioFormatDescriptionRef desc) ;
	 */
    public Reference<AudioStreamBasicDescription> getStreamBasicDescription() {
        throw new RuntimeException("Stub");
    }

    /**
	 * const void * CMAudioFormatDescriptionGetMagicCookie( CMAudioFormatDescriptionRef desc, size_t *cookieSizeOut) ;
	 */
    public byte[] getMagicCookie(int[] cookieSizeOut) {
        throw new RuntimeException("Stub");
    }

    /**
	 * const AudioChannelLayout * CMAudioFormatDescriptionGetChannelLayout( CMAudioFormatDescriptionRef desc, size_t *layoutSize) ;
	 */
    public Reference<AudioChannelLayout> getChannelLayout(int[] layoutSize) {
        throw new RuntimeException("Stub");
    }

    /**
	 * const AudioFormatListItem * CMAudioFormatDescriptionGetFormatList( CMAudioFormatDescriptionRef desc, size_t *formatListSize) ;
	 */
    public Reference<AudioFormatListItem> getFormatList(int[] formatListSize) {
        throw new RuntimeException("Stub");
    }

    /**
	 * const AudioFormatListItem * CMAudioFormatDescriptionGetRichestDecodableFormat( CMAudioFormatDescriptionRef desc) ;
	 */
    public Reference<AudioFormatListItem> getRichestDecodableFormat() {
        throw new RuntimeException("Stub");
    }

    /**
	 * const AudioFormatListItem * CMAudioFormatDescriptionGetMostCompatibleFormat( CMAudioFormatDescriptionRef desc) ;
	 */
    public Reference<AudioFormatListItem> getMostCompatibleFormat() {
        throw new RuntimeException("Stub");
    }

    /**
	 * Boolean CMAudioFormatDescriptionEqual( CMAudioFormatDescriptionRef desc1, CMAudioFormatDescriptionRef desc2, CMAudioFormatDescriptionMask equalityMask, CMAudioFormatDescriptionMask *equalityMaskOut) ;
	 */
    public byte equal(CMAudioFormatDescription desc2, int equalityMask, int[] equalityMaskOut) {
        throw new RuntimeException("Stub");
    }
}
