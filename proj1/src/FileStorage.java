import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FileStorage {
    /**
     * Directory where chunks are stored
     */
    String chunksDir;

    /**
     * Directory where restored files are stored
     */
    String restoreDir;

    /**
     * Singleton instance of FileStorage
     */
    public static FileStorage instance;

    /**
     * Concurrent set of all files whose backup was initiated by this peer
     */
    private final Set<FileParser> backedUpFiles = ConcurrentHashMap.newKeySet();

    /**
     * Concurrent set of all chunks stored locally by peer
     */
    private final Set<Chunk> storedChunkFiles = ConcurrentHashMap.newKeySet();

    /**
     * Concurrent map of backed up chunks and fileId
     */
    private final ConcurrentHashMap<Chunk, String> chunkMap = new ConcurrentHashMap<>();

    public FileStorage() throws IOException {
        if (FileStorage.instance == null) FileStorage.instance = this;
        String serviceDirectory = "service-" + Peer.getId();
        this.chunksDir = serviceDirectory + "/chunks";
        this.restoreDir = serviceDirectory + "/restored";

        Files.createDirectories(Paths.get(chunksDir));
        Files.createDirectories(Paths.get(restoreDir));
    }

    public boolean storeChunk(Chunk c) {

        // File already exists locally, won't store again
        if (!addChunk(c)) return false;

        FileOutputStream fos;
        try {
            fos = new FileOutputStream( chunksDir + "/" + c.getChunkID());
            fos.write(c.getContent());
            fos.close();
        } catch (IOException e) {
            System.out.println("Error writing chunk to file locally");
            return false;
        }

        return true;
    }

    public int getPerceivedReplicationDegree(Chunk chunk){
        for (Chunk key : chunkMap.keySet()){
            if (key.equals(chunk)) return key.getPerceivedReplicationDegree();
        }
        return -1;
    }

    public synchronized boolean addChunk(Chunk chunk) {
        if (storedChunkFiles.add(chunk)) {
            incrementReplicationDegree(chunk);
            return true;
        }
        else return false;
    }

    public void incrementReplicationDegree(Chunk chunk) {
        // if chunk doesnt exist in map, adds it and put its perceived replication value as 1. Otherwise increment it
        if (chunkMap.putIfAbsent(chunk, chunk.getFileID()) != null){
            for (Chunk key : chunkMap.keySet()){
                if (key.equals(chunk)) key.incrementPerceivedReplicationDegree();
            }
        }
    }

    public void backupFile(FileParser file) {
        backedUpFiles.add(file);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("BACKED UP FILES: ");

        if (backedUpFiles.size() > 0){
            for (FileParser file : backedUpFiles){
                result.append("\n").append(file.toString());
            }
        }
        else result.append("None\n");

        result.append("STORED CHUNKS: ");

        if (storedChunkFiles.size() > 0){
            for (Chunk chunk : storedChunkFiles){
                result.append("\n").append(chunk.toString());
            }
        }
        else result.append("None\n");

        return result.toString();
    }

    public ConcurrentHashMap<Chunk, String> getChunkMap() {
        return chunkMap;
    }

    public Set<FileParser> getBackedUpFiles() {
        return backedUpFiles;
    }

    public Optional<FileParser> findBackedUpFile(String fileID){
        for (FileParser f : backedUpFiles) {
            if (f.getFileID().equals(fileID)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    public void removeFileParserFromBackedUpFiles(FileParser fileParser) {
        this.backedUpFiles.remove(fileParser);
    }

    public void removeChunkFromStoredChunkFiles(Chunk chunk) {
        this.storedChunkFiles.remove(chunk);
    }

    public void removeEntryFromChunkMap(Chunk keyToRemove) {
        this.chunkMap.remove(keyToRemove);
    }

    public Set<Chunk> getStoredChunkFiles() {
        return storedChunkFiles;
    }
}