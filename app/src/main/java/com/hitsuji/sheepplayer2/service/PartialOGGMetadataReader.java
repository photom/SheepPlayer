package com.hitsuji.sheepplayer2.service;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

public class PartialOGGMetadataReader {

    public static class OGGMetadata {
        public String artist;
        public String title;
        public String album;
        public String trackNumber;
        public String genre;
        public String date;
        public byte[] artwork;
    }

    public static OGGMetadata readPartialOGGMetadata(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            // Check minimum file size
            if (raf.length() < 100) return null;

            // Find and read comment header across multiple pages
            byte[] commentData = findAndReadVorbisComments(raf);
            if (commentData == null) return null;

            return parseVorbisCommentsSafe(commentData);

        } catch (Exception e) {
            Log.d("PartialOGGMetadataReader", "Failed to read OGG metadata", e);
            return null;
        }
    }

    private static OGGPageInfo readOGGPage(RandomAccessFile raf) throws IOException {
        long startPos = raf.getFilePointer();
        if (startPos + 27 > raf.length()) return null;

        // Check OGG signature "OggS"
        byte[] capture = new byte[4];
        raf.read(capture);
        if (!Arrays.equals(capture, new byte[]{'O', 'g', 'g', 'S'})) {
            return null;
        }

        // Read page header
        int version = raf.readByte() & 0xFF;
        int headerType = raf.readByte() & 0xFF;
        
        // Skip granule position (8 bytes)
        raf.skipBytes(8);
        
        // Read serial number (4 bytes)
        int serialNumber = bytesToIntLE(raf);
        
        // Read page sequence number (4 bytes)
        int pageSequence = bytesToIntLE(raf);
        
        // Skip CRC checksum (4 bytes)
        raf.skipBytes(4);
        
        // Read segment count
        int segments = raf.readByte() & 0xFF;
        
        if (raf.getFilePointer() + segments > raf.length()) {
            return null;
        }

        // Read segment table
        byte[] segmentTable = new byte[segments];
        raf.read(segmentTable);

        // Calculate data size
        int dataSize = 0;
        for (byte segment : segmentTable) {
            dataSize += (segment & 0xFF);
        }

        // Check if we have enough data
        if (raf.getFilePointer() + dataSize > raf.length()) {
            // Partial data available
            dataSize = (int)(raf.length() - raf.getFilePointer());
        }

        // Read page data
        byte[] data = new byte[dataSize];
        raf.read(data);
        
        // Check if this is a continued packet (last segment is 255)
        boolean isContinued = segments > 0 && (segmentTable[segments-1] & 0xFF) == 255;
        
        return new OGGPageInfo(headerType, serialNumber, pageSequence, data, isContinued, segmentTable);
    }

    private static byte[] findAndReadVorbisComments(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        List<byte[]> commentPageData = new ArrayList<>();
        boolean foundCommentHeader = false;
        int maxPagesToCheck = 10; // Limit search to first 10 pages
        
        for (int pageCount = 0; pageCount < maxPagesToCheck; pageCount++) {
            OGGPageInfo page = readOGGPage(raf);
            if (page == null) break;
            
            // Check if this page contains vorbis comment header
            if (page.data.length > 7 && page.data[0] == 3 && 
                Arrays.equals(Arrays.copyOfRange(page.data, 1, 7), "vorbis".getBytes())) {
                foundCommentHeader = true;
                commentPageData.add(page.data);
                
                // If packet continues to next page, read it
                if (page.isContinued) {
                    collectContinuedPacket(raf, commentPageData, page.serialNumber, maxPagesToCheck - pageCount - 1);
                }
                break;
            }
        }
        
        if (!foundCommentHeader || commentPageData.isEmpty()) {
            return null;
        }
        
        // Combine all comment data from multiple pages
        return combinePageData(commentPageData);
    }
    
    private static void collectContinuedPacket(RandomAccessFile raf, List<byte[]> commentPageData, 
                                             int serialNumber, int maxPages) throws IOException {
        for (int i = 0; i < maxPages; i++) {
            OGGPageInfo page = readOGGPage(raf);
            if (page == null || page.serialNumber != serialNumber) break;
            
            commentPageData.add(page.data);
            
            // Stop if packet is complete (not continued)
            if (!page.isContinued) break;
        }
    }
    
    private static byte[] combinePageData(List<byte[]> pageDataList) {
        int totalSize = 0;
        for (byte[] data : pageDataList) {
            totalSize += data.length;
        }
        
        byte[] combined = new byte[totalSize];
        int pos = 0;
        for (byte[] data : pageDataList) {
            System.arraycopy(data, 0, combined, pos, data.length);
            pos += data.length;
        }
        
        return combined;
    }
    
    private static int bytesToIntLE(RandomAccessFile raf) throws IOException {
        byte[] bytes = new byte[4];
        raf.read(bytes);
        return bytesToIntLE(bytes, 0);
    }
    
    private static class OGGPageInfo {
        final int headerType;
        final int serialNumber;
        final int pageSequence;
        final byte[] data;
        final boolean isContinued;
        final byte[] segmentTable;
        
        OGGPageInfo(int headerType, int serialNumber, int pageSequence, byte[] data, 
                   boolean isContinued, byte[] segmentTable) {
            this.headerType = headerType;
            this.serialNumber = serialNumber;
            this.pageSequence = pageSequence;
            this.data = data;
            this.isContinued = isContinued;
            this.segmentTable = segmentTable;
        }
    }

    private static OGGMetadata parseVorbisCommentsSafe(byte[] data) {
        if (data.length < 10) return null;

        // Check vorbis comment header
        if (data[0] != 3 || !Arrays.equals(Arrays.copyOfRange(data, 1, 7), "vorbis".getBytes())) {
            return null;
        }

        OGGMetadata metadata = new OGGMetadata();

        try {
            int pos = 7;

            // Skip vendor string safely
            if (pos + 4 > data.length) return metadata;
            int vendorLength = bytesToIntLE(data, pos);
            pos += 4;

            if (vendorLength < 0 || pos + vendorLength > data.length) {
                return metadata; // Return what we have
            }
            pos += vendorLength;

            // Read comment count
            if (pos + 4 > data.length) return metadata;
            int commentCount = bytesToIntLE(data, pos);
            pos += 4;

            // Parse available comments
            for (int i = 0; i < commentCount && pos + 4 < data.length; i++) {
                int commentLength = bytesToIntLE(data, pos);
                pos += 4;

                if (commentLength < 0 || pos + commentLength > data.length) {
                    break; // Stop here, return what we have
                }

                String comment = new String(data, pos, commentLength, StandardCharsets.UTF_8);
                pos += commentLength;

                String[] parts = comment.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].toUpperCase();
                    String value = parts[1];

                    switch (key) {
                        case "TITLE":
                            metadata.title = value;
                            break;
                        case "ARTIST":
                            metadata.artist = value;
                            break;
                        case "ALBUM":
                            metadata.album = value;
                            break;
                        case "TRACKNUMBER":
                            metadata.trackNumber = value;
                            break;
                        case "GENRE":
                            metadata.genre = value;
                            break;
                        case "DATE":
                            metadata.date = value;
                            break;
                        case "METADATA_BLOCK_PICTURE":
                            if (metadata.artwork == null) {
                                metadata.artwork = extractArtworkFromBase64Safe(value);
                            }
                            break;
                    }
                }
            }
        } catch (Exception e) {
            Log.d("PartialOGGMetadataReader", "Failed to parse OGG metadata", e);
        }

        return metadata;
    }

    private static int bytesToIntLE(byte[] data, int offset) {
        if (offset + 3 >= data.length) return 0;
        return (data[offset] & 0xFF) |
                ((data[offset + 1] & 0xFF) << 8) |
                ((data[offset + 2] & 0xFF) << 16) |
                ((data[offset + 3] & 0xFF) << 24);
    }

    private static int bytesToIntBE(byte[] data, int offset) {
        if (offset + 3 >= data.length) return 0;
        return ((data[offset] & 0xFF) << 24) |
                ((data[offset + 1] & 0xFF) << 16) |
                ((data[offset + 2] & 0xFF) << 8) |
                (data[offset + 3] & 0xFF);
    }

    private static byte[] extractArtworkFromBase64Safe(String base64Value) {
        try {
            if (base64Value == null || base64Value.length() < 50) return null;

            byte[] pictureBlock = Base64.getDecoder().decode(base64Value);
            if (pictureBlock.length < 32) return null;

            int pos = 0;

            // Skip picture type (4 bytes)
            pos += 4;

            // Read MIME type length
            int mimeLength = bytesToIntBE(pictureBlock, pos);
            pos += 4;
            if (mimeLength < 0 || mimeLength > 100 || pos + mimeLength > pictureBlock.length)
                return null;

            // Skip MIME type
            pos += mimeLength;
            if (pos + 4 > pictureBlock.length) return null;

            // Read description length
            int descLength = bytesToIntBE(pictureBlock, pos);
            pos += 4;
            if (descLength < 0 || descLength > 1000 || pos + descLength > pictureBlock.length)
                return null;

            // Skip description
            pos += descLength;
            if (pos + 16 > pictureBlock.length) return null;

            // Skip width, height, color depth, colors used (16 bytes)
            pos += 16;

            // Read image data length
            int imageLength = bytesToIntBE(pictureBlock, pos);
            pos += 4;
            if (imageLength <= 0 || imageLength > 10000000 || pos + imageLength > pictureBlock.length)
                return null;

            // Extract image data
            if (imageLength < 100) return null; // Too small to be a real image

            return Arrays.copyOfRange(pictureBlock, pos, pos + imageLength);

        } catch (Exception e) {
            Log.d("PartialOGGMetadataReader", "Failed to extract artwork from OGG metadata", e);
            return null;
        }
    }
}