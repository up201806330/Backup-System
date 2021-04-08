import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class FileStorage {
    public static FileStorage instance;

    private ArrayList<Chunk> storedChunks;
    private ConcurrentHashMap<String, Integer> storedOccurences;
    // < key, perceivedReplicationDegree >
    // key = fileId-chunknr

    public FileStorage(ArrayList<Chunk> storedChunks, ConcurrentHashMap<String, Integer> storedOccurences) {
        if (FileStorage.instance != null) return;
        else FileStorage.instance = this;

        this.storedChunks = storedChunks;
        this.storedOccurences = storedOccurences;
    }

    public boolean storeChunk(Chunk c) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(c.getChunkFileName());
            fos.write(c.getContent());
            fos.close();
        } catch (IOException e) {
            System.out.println("Error writing chunk to file locally");
            return false;
        }

        return addChunkToStored(c);
    }

    public ConcurrentHashMap<String, Integer> getStoredOccurences() {
        return storedOccurences;
    }

    public ArrayList<Chunk> getStoredChunks() {
        return storedChunks;
    }

    public synchronized boolean addChunkToStored(Chunk chunk) {

        if (!storedChunks.contains(chunk)) {
            storedChunks.add(chunk);
            return true;
        }
        return false;
    }
}