# Image Magic Number Validation

## Overview

SheepPlayer implements comprehensive image validation using magic number detection to ensure that
only valid image files are processed and displayed. This prevents security issues and crashes from
malformed or malicious files masquerading as images.

## Supported Image Formats

The app validates the following image formats by checking their magic numbers (file signatures):

### JPEG

- **Magic Numbers**: `FF D8 FF`
- **Description**: JPEG/JFIF image format
- **File Extensions**: `.jpg`, `.jpeg`

### PNG

- **Magic Numbers**: `89 50 4E 47 0D 0A 1A 0A`
- **Description**: Portable Network Graphics
- **File Extensions**: `.png`

### GIF

- **Magic Numbers**: `47 49 46 38` (ASCII: "GIF8")
- **Description**: Graphics Interchange Format
- **File Extensions**: `.gif`

### WebP

- **Magic Numbers**: `52 49 46 46` ... `57 45 42 50` (RIFF...WEBP)
- **Description**: Modern web image format by Google
- **File Extensions**: `.webp`

### BMP

- **Magic Numbers**: `42 4D` (ASCII: "BM")
- **Description**: Windows Bitmap
- **File Extensions**: `.bmp`

### ICO

- **Magic Numbers**: `00 00 01 00`
- **Description**: Windows Icon format
- **File Extensions**: `.ico`

### TIFF

- **Magic Numbers**:
    - Little-endian: `49 49 2A 00`
    - Big-endian: `4D 4D 00 2A`
- **Description**: Tagged Image File Format
- **File Extensions**: `.tiff`, `.tif`

## Search Strategy

### Quoted Artist Names

The image search uses quoted artist names to improve search accuracy and relevance:

**Search Terms Generated:**

- `"Artist Name" musician artist photos`
- `"Artist Name" band music images`
- `"Artist Name" concert live photos`
- `"Artist Name" album cover photos`
- `"Artist Name" press photos`
- `"Artist Name"`

**Benefits:**

- **Exact Matching**: Quotes ensure search engines treat artist name as exact phrase
- **Reduced Noise**: Prevents false matches from individual words in artist name
- **Better Relevance**: More accurate results for multi-word artist names
- **Proper URL Encoding**: Quotes are properly encoded in search URLs

**Examples:**

- Without quotes: `Led Zeppelin` might match "Led pencils" + "Zeppelin airship"
- With quotes: `"Led Zeppelin"` only matches the exact band name

## Implementation

### Validation Process

1. **Download Phase**: When downloading images from search results, the app:
    - Downloads the complete file as bytes
    - Validates magic numbers using `isValidImageMagicNumber()`
    - Excludes files that don't match any supported image format

2. **Magic Number Check**: The validation function:
    - Checks the first 12 bytes of the file
    - Compares against known magic number patterns
    - Logs the detected image format
    - Returns `false` for non-image files

3. **Security Benefits**:
    - Prevents processing of HTML error pages disguised as images
    - Blocks malicious files with image extensions
    - Ensures only legitimate image data is processed
    - Reduces crashes from corrupted or invalid files

### Code Location

The validation logic is implemented in:

- **File**: `app/src/main/java/com/hitsuji/sheepplayer2/service/ArtistImageService.kt`
- **Method**: `isValidImageMagicNumber(bytes: ByteArray): Boolean`
- **Usage**: Called in `downloadImage()` before bitmap processing

### Logging

The app provides detailed logging for debugging:

- Valid images: Format detection (e.g., "Detected JPEG image")
- Invalid files: Magic bytes logged in hexadecimal format
- Security events: Files excluded due to invalid magic numbers

### Example Log Output

```
Valid image detected:
D/ArtistImageService: Valid image magic number detected for: https://example.com/image.jpg
D/ArtistImageService: Detected JPEG image
D/ArtistImageService: Successfully downloaded and validated image: 800x600

Invalid file excluded:
W/ArtistImageService: Invalid image magic number, excluding file from: https://example.com/fake.jpg
W/ArtistImageService: Unknown file format with magic bytes: 3C 68 74 6D 6C 3E 3C 68 65 61 64 3E
```

## Circular Buffer System

The image gallery implements a circular buffer system for optimal performance and continuous
browsing:

### Buffer Management

- **Fixed Size**: Maintains exactly 10 images at any time
- **Scroll Detection**: Monitors scroll position to detect bottom reach
- **Automatic Loading**: Downloads next image when scrolled to bottom
- **Memory Optimization**: Removes oldest image when adding new one

### Implementation Details

- **URL Pool**: Downloads up to 50 URLs initially for buffer rotation
- **Index Tracking**: Maintains current position in URL list
- **Smooth Transition**: Removes top image and adds bottom image seamlessly
- **Error Handling**: Skips failed downloads and continues to next URL
- **Quoted Search Terms**: Uses quoted artist names for precise search matching

## Performance Impact

- **Minimal Overhead**: Magic number validation only checks the first 12 bytes
- **Early Rejection**: Invalid files are excluded before expensive bitmap processing
- **Memory Efficient**: Prevents loading of non-image data into memory, maintains fixed buffer size
- **Network Efficient**: Failed downloads are detected immediately
- **Continuous Browsing**: Infinite scrolling experience with circular buffer

## Security Considerations

This validation provides protection against:

- **Malformed Files**: Files with incorrect extensions
- **HTML Error Pages**: Web server error responses disguised as images
- **Malicious Content**: Non-image files that could cause crashes
- **False Positives**: URL-based validation failures due to dynamic URLs

## Future Enhancements

Potential improvements could include:

- AVIF format support (`00 00 00 20 66 74 79 70 61 76 69 66`)
- HEIF format support (`00 00 00 18 66 74 79 70 68 65 69 63`)
- More robust JPEG variant detection
- Content-Type header validation as secondary check