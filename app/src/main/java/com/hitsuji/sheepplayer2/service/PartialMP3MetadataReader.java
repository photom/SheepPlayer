package com.hitsuji.sheepplayer2.service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PartialMP3MetadataReader {
    
    public static class MP3Metadata {
        public String artist;
        public String title;
        public String album;
        public String trackNumber;
        public byte[] artwork;
        public String genre;
        public String year;
    }
    
    public static MP3Metadata readPartialMP3Metadata(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            return readID3v2Safe(raf);
        } catch (Exception e) {
            return null; // Graceful failure
        }
    }
    
    private static MP3Metadata readID3v2Safe(RandomAccessFile raf) throws IOException {
        if (raf.length() < 10) return null;
        
        raf.seek(0);
        byte[] header = new byte[10];
        raf.read(header);
        
        // Check ID3v2 signature
        if (header[0] != 'I' || header[1] != 'D' || header[2] != '3') {
            return null;
        }
        
        // Calculate tag size
        int tagSize = ((header[6] & 0x7F) << 21) |
                     ((header[7] & 0x7F) << 14) |
                     ((header[8] & 0x7F) << 7) |
                     (header[9] & 0x7F);
        
        // Check if we have enough data for the complete tag
        long availableData = raf.length() - 10;
        int readSize = (int) Math.min(tagSize, availableData);
        
        if (readSize <= 0) return null;
        
        // Read available tag data
        byte[] tagData = new byte[readSize];
        raf.read(tagData);
        
        return parseID3v2FramesSafe(tagData, header[3]);
    }
    
    private static MP3Metadata parseID3v2FramesSafe(byte[] data, int version) {
        MP3Metadata metadata = new MP3Metadata();
        int pos = 0;
        
        while (pos < data.length - 10) {
            try {
                // Check if we have enough data for frame header
                if (pos + 10 > data.length) break;
                
                String frameId = new String(data, pos, 4);
                pos += 4;
                
                // Handle different size encoding for different versions
                int frameSize;
                if (version == 4) {
                    // ID3v2.4 uses synchsafe integers
                    frameSize = ((data[pos] & 0x7F) << 21) |
                               ((data[pos + 1] & 0x7F) << 14) |
                               ((data[pos + 2] & 0x7F) << 7) |
                               (data[pos + 3] & 0x7F);
                } else {
                    // ID3v2.3 and earlier use regular integers
                    frameSize = ((data[pos] & 0xFF) << 24) |
                               ((data[pos + 1] & 0xFF) << 16) |
                               ((data[pos + 2] & 0xFF) << 8) |
                               (data[pos + 3] & 0xFF);
                }
                pos += 4;
                
                pos += 2; // Skip flags
                
                // Safety check for frame size
                if (frameSize <= 0 || frameSize > 1000000 || pos + frameSize > data.length) {
                    // Try to skip this frame and continue
                    pos += Math.min(frameSize, data.length - pos);
                    continue;
                }
                
                // Extract frame data safely
                byte[] frameData = Arrays.copyOfRange(data, pos, pos + frameSize);
                
                switch (frameId) {
                    case "TIT2": // Title
                        metadata.title = decodeTextFrameSafe(frameData);
                        break;
                    case "TPE1": // Artist
                        metadata.artist = decodeTextFrameSafe(frameData);
                        break;
                    case "TALB": // Album
                        metadata.album = decodeTextFrameSafe(frameData);
                        break;
                    case "TRCK": // Track number
                        metadata.trackNumber = decodeTextFrameSafe(frameData);
                        break;
                    case "TCON": // Genre
                        metadata.genre = decodeTextFrameSafe(frameData);
                        break;
                    case "TYER": // Year
                    case "TDRC": // Recording time (ID3v2.4)
                        metadata.year = decodeTextFrameSafe(frameData);
                        break;
                    case "APIC": // Attached picture
                        // Only try to extract artwork if frame seems complete
                        if (frameSize > 100 && frameSize == frameData.length) {
                            metadata.artwork = extractArtworkSafe(frameData);
                        }
                        break;
                }
                
                pos += frameSize;
                
            } catch (Exception e) {
                // Skip problematic frame and continue
                break;
            }
        }
        
        return metadata;
    }
    
    private static String decodeTextFrameSafe(byte[] data) {
        if (data.length < 2) return "";
        
        try {
            int encoding = data[0] & 0xFF;
            byte[] textData = Arrays.copyOfRange(data, 1, data.length);
            
            // Remove null terminators
            int endPos = textData.length;
            for (int i = 0; i < textData.length; i++) {
                if (textData[i] == 0) {
                    endPos = i;
                    break;
                }
            }
            
            if (endPos == 0) return "";
            textData = Arrays.copyOfRange(textData, 0, endPos);
            
            switch (encoding) {
                case 0: return new String(textData, "ISO-8859-1").trim();
                case 1: return new String(textData, "UTF-16").trim();
                case 2: return new String(textData, "UTF-16BE").trim();
                case 3: return new String(textData, "UTF-8").trim();
                default: return new String(textData, "ISO-8859-1").trim();
            }
        } catch (Exception e) {
            return "";
        }
    }
    
    private static byte[] extractArtworkSafe(byte[] data) {
        if (data.length < 20) return null;
        
        try {
            int pos = 1; // Skip encoding
            
            // Find MIME type end
            while (pos < data.length && data[pos] != 0) pos++;
            if (pos >= data.length) return null;
            pos++; // Skip null
            
            pos++; // Skip picture type
            
            // Find description end
            while (pos < data.length && data[pos] != 0) pos++;
            if (pos >= data.length) return null;
            pos++; // Skip null
            
            // Check if we have reasonable image data
            int imageSize = data.length - pos;
            if (imageSize < 100) return null; // Too small to be a real image
            
            return Arrays.copyOfRange(data, pos, data.length);
            
        } catch (Exception e) {
            return null;
        }
    }
}