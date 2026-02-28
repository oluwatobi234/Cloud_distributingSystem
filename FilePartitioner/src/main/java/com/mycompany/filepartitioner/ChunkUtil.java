/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.filepartitioner;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

/**
 *
 * @author ntu-user
 */
public class ChunkUtil {
     private static final int CHUNK_SIZE = 1024 * 1024;
    
    /**
     * Split data into chunks with CRC32 checksums
     */
    public static List<Chunk> split(byte[] data) {
        List<Chunk> chunks = new ArrayList<>();
        int offset = 0;
        int chunkIndex = 0;
        
        while (offset < data.length) {
            int size = Math.min(CHUNK_SIZE, data.length - offset);
            byte[] chunkData = new byte[size];
            System.arraycopy(data, offset, chunkData, 0, size);
            
            // Calculate CRC32 checksum
            CRC32 crc32 = new CRC32();
            crc32.update(chunkData);
            long checksum = crc32.getValue();
            
            chunks.add(new Chunk(chunkIndex, chunkData, checksum));
            
            offset += size;
            chunkIndex++;
        }
        
        return chunks;
    }
    
    /**
     * Reassemble chunks into single byte array
     */
    public static byte[] reassemble(List<Chunk> chunks) {
        // Sort by index
        chunks.sort((a, b) -> Integer.compare(a.index, b.index));
        
        // Calculate total size
        int totalSize = chunks.stream().mapToInt(c -> c.data.length).sum();
        byte[] result = new byte[totalSize];
        
        int pos = 0;
        for (Chunk chunk : chunks) {
            System.arraycopy(chunk.data, 0, result, pos, chunk.data.length);
            pos += chunk.data.length;
        }
        
        return result;
    }
    
    /**
     * Validate chunk checksums
     */
    public static boolean validateChunks(List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            CRC32 crc32 = new CRC32();
            crc32.update(chunk.data);
            if (crc32.getValue() != chunk.checksum) {
                System.out.println("Chunk " + chunk.index + " checksum failed!");
                return false;
            }
        }
        return true;
    }
    
    // Inner class to hold chunk data
    public static class Chunk {
        public int index;
        public byte[] data;
        public long checksum;
        
        public Chunk(int index, byte[] data, long checksum) {
            this.index = index;
            this.data = data;
            this.checksum = checksum;
        }
    }
}
