# Image Magic Number Validation

## Overview

SheepPlayer implements comprehensive image validation using magic number detection to ensure that only valid image files are processed and displayed. This prevents security issues and crashes from malformed or malicious files masquerading as images.

## Supported Image Formats

The app validates the following image formats by checking their magic numbers (file signatures):

### JPEG
- **Validation**: FF D8 FF
- **Description**: Standard photo format (JPEG/JFIF).
- **Extensions**: `.jpg`, `.jpeg`

### PNG
- **Validation**: 89 50 4E 47 0D 0A 1A 0A
- **Description**: Portable Network Graphics, supporting transparency.
- **Extensions**: `.png`

### GIF
- **Validation**: 47 49 46 38
- **Description**: Graphics Interchange Format, commonly used for animations.
- **Extensions**: `.gif`

### WebP
- **Validation**: RIFF...WEBP signature (52 49 46 46 ... 57 45 42 50)
- **Description**: Google's modern high-compression web image format.
- **Extensions**: `.webp`

### AVIF
- **Validation**: `00 00 00 20 66 74 79 70 61 76 69 66`
- **Description**: Next-generation high-efficiency image format.
- **Extensions**: `.avif`

### HEIF / HEIC
- **Validation**: `00 00 00 18 66 74 79 70 68 65 69 63`
- **Description**: High Efficiency Image File Format.
- **Extensions**: `.heif`, `.heic`

### BMP
- **Validation**: 42 4D (BM)
- **Description**: Windows Bitmap format.
- **Extensions**: `.bmp`

### ICO
- **Validation**: 00 00 01 00
- **Description**: Windows Icon format.
- **Extensions**: `.ico`

### TIFF
- **Validation**: 49 49 2A 00 (Little-endian) or 4D 4D 00 2A (Big-endian)
- **Description**: Tagged Image File Format, used for high-quality graphics.
- **Extensions**: `.tiff`, `.tif`

## Search Strategy

### Quoted Artist Names
The image search service utilizes quoted artist names (e.g., `"Artist Name"`) to significantly improve the accuracy and relevance of search results.

**Search Benefits**:
- **Exact Matching**: Quotes force search engines to treat the artist name as an exact phrase.
- **Noise Reduction**: Prevents unrelated results from matching individual words within a multi-word artist name.
- **Relevant Context**: Combinations like `"Artist Name" musician` or `"Artist Name" concert` provide focused imagery.

## Implementation

### Validation Process
1.  **Download Phase**: The application downloads the complete file as a byte array.
2.  **Magic Number Check**: The first 12 to 16 bytes are inspected and compared against the known signatures for supported image formats.
3.  **Security Decision**: If a match is found, the image is processed; otherwise, it is immediately discarded to prevent security issues.

### Security & Logging
The validation logic is centrally located in the `ArtistImageService`. When a valid image is detected, the service logs the specific format identified (e.g., "Detected JPEG image") and the final dimensions after processing. If a file is rejected, the service logs the source URL and the unknown magic bytes in hexadecimal format to aid in security monitoring.

## 🖼️ User Feedback & UI Handling

-   **Silent Rejection**: To ensure a smooth browsing experience, images that fail magic number validation are rejected silently without showing an error message to the user.
-   **Fallback**: If an image is rejected, the circular buffer automatically proceeds to the next URL in the pool.
-   **Loading Indicator**: The animated "Sheep" placeholder remains visible until at least one valid image is successfully downloaded and validated.
-   **Shimmer Effect**: Individual image slots use a shimmer effect during the download and validation phase to provide visual feedback that work is in progress.

## Circular Buffer System

The image gallery employs a circular buffer to provide a continuous and high-performance browsing experience:

-  **Buffer Management**: The system maintains a fixed pool of exactly 10 images at any time.
-  **Seamless Rotation**: When a user scrolls to the bottom, the oldest image is removed, and a new one is added to the buffer.
-  **Pre-fetching**: To ensure smooth transitions, the service initially identifies up to 50 candidate URLs to rotate through the buffer.
-  **Error Resilience**: If a download fails or validation is rejected, the system automatically skips to the next candidate URL.

## Performance & Security Impact

-  **Efficiency**: Magic number validation is extremely fast, requiring only a small byte inspection before expensive processing (like bitmap decoding) begins.
-  **Resource Protection**: Prevents loading non-image data into memory, maintaining a stable memory footprint.
-  **Attack Mitigation**: Defends against malformed files, disguised HTML pages, and potential buffer overflow attacks from malicious binary data.
