import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FileStorage {
    /**
     * Singleton instance of FileStorage
     */
    public static FileStorage instance;

    /**
     * List of all chunks stored locally by peer
     */
    private final ArrayList<Chunk> storedChunkFiles = new ArrayList<>();

    /**
     * Concurrent map of backed up chunks and fileId
     */
    private final ConcurrentHashMap<Chunk, String> chunkMap = new ConcurrentHashMap<>();

    public FileStorage() {
        if (FileStorage.instance == null) FileStorage.instance = this;
    }

    public boolean storeChunk(Chunk c) {

        // File already exists locally, won't store again
        if (!addChunk(c)) return false;

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(c.getChunkID());
            fos.write(c.getContent());
            fos.close();
        } catch (IOException e) {
            System.out.println("Error writing chunk to file locally");
            return false;
        }

        return true;
    }

    public ConcurrentHashMap<Chunk, String> getChunkMap() {
        return chunkMap;
    }

    public int getPerceivedReplicationDegree(Chunk chunk){
        for (Chunk key : chunkMap.keySet()){
            if (key.equals(chunk)) return key.getPerceivedReplicationDegree();
        }
        return -1;
    }

    public ArrayList<Chunk> getStoredChunkFiles() {
        return storedChunkFiles;
    }

    public synchronized boolean addChunk(Chunk chunk) {

        if (!storedChunkFiles.contains(chunk)) {
            // Mark chunk as stored locally
            storedChunkFiles.add(chunk);
            incrementReplicationDegree(chunk);
            return true;
        }
        return false;
    }

    public void incrementReplicationDegree(Chunk chunk) {
        // if chunk doesnt exist in map, adds it and put its perceived replication value as 1. Otherwise increment it
        if (chunkMap.putIfAbsent(chunk, chunk.getFileID()) != null){
            for (Chunk key : chunkMap.keySet()){
                if (key.equals(chunk)) key.incrementPerceivedReplicationDegree();
            }
        }
    }
}