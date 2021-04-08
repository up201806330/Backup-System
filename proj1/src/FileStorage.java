import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class FileStorage {
    public static FileStorage instance;

    private final ArrayList<Chunk> storedChunkFiles = new ArrayList<>();
    private final ConcurrentHashMap<String, Integer> chunkReplicationMap = new ConcurrentHashMap<>();
    // < key, perceivedReplicationDegree >
    // key = fileId-chunknr

    public FileStorage() {
        if (FileStorage.instance != null) return;
        else FileStorage.instance = this;
    }

    public boolean storeChunk(Chunk c) {

        // File already exists locally, won't store again
        if (!addChunk(c)) return false;

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(c.getChunkFullName());
            fos.write(c.getContent());
            fos.close();
        } catch (IOException e) {
            System.out.println("Error writing chunk to file locally");
            return false;
        }

        return true;
    }

    public ConcurrentHashMap<String, Integer> getChunkReplicationMap() {
        return chunkReplicationMap;
    }

    public ArrayList<Chunk> getStoredChunkFiles() {
        return storedChunkFiles;
    }

    public synchronized boolean addChunk(Chunk chunk) {

        if (!storedChunkFiles.contains(chunk)) {
            // Mark chunk as stored locally
            storedChunkFiles.add(chunk);

            incrementReplicationDegree(chunk.getChunkFullName());

            return true;
        }
        return false;
    }

    public void incrementReplicationDegree(String chunkFileName) {
        // if chunk doesnt exist in hashmap, adds it and put its replication value as 1. Otherwise sums 1.
        chunkReplicationMap.merge(chunkFileName, 1, Integer::sum);
    }
}