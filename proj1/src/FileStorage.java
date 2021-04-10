import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FileStorage implements Serializable {
    /**
     * Singleton instance of FileStorage
     */
    public static FileStorage instance;

    /**
     * Directory where chunks are stored
     */
    public static String chunksDir;

    /**
     * Directory where restored files are stored
     */
    public static String restoreDir;

    /**
     * Concurrent set of all files whose backup was initiated by this peer
     */
    public static final Set<FileParser> initiatedFiles = ConcurrentHashMap.newKeySet();

    /**
     * Concurrent map that for each chunk of each backed up file, has a concurrent set of peers that have that chunk backed up
     */
    public static final ConcurrentHashMap<Chunk, ConcurrentHashMap.KeySetView<Object, Boolean>> chunksBackedPeers = new ConcurrentHashMap<Chunk, ConcurrentHashMap.KeySetView<Object, Boolean>>();

    /**
     * Concurrent set of all chunks stored locally by peer
     */
    public static final Set<Chunk> storedChunkFiles = ConcurrentHashMap.newKeySet();

    /**
     * Concurrent map of backed up chunks and fileId
     */
    public static final ConcurrentHashMap<Chunk, String> chunkMap = new ConcurrentHashMap<>();

    /**
     * Singleton constructor
     * @throws IOException
     */
    public FileStorage() throws IOException {
        if (FileStorage.instance == null) FileStorage.instance = this;
        chunksDir = Peer.getServiceDirectory() + "/chunks";
        restoreDir = Peer.getServiceDirectory() + "/restored";

        Files.createDirectories(Paths.get(chunksDir));
        Files.createDirectories(Paths.get(restoreDir));
    }

    /**
     * Tries to store new chunk as a file locally,
     * @param chunk New chunk
     * @return True if chunk was stored successfully, false if it already existed / something went wrong
     */
    public static boolean storeChunk(Chunk chunk) {
        // File already exists locally, won't store again
        if (!addChunk(chunk)) return false;

        FileOutputStream fos;
        try {
            fos = new FileOutputStream( chunksDir + "/" + chunk.getChunkID());
            fos.write(chunk.getContent());
            fos.close();
        } catch (IOException e) {
            System.out.println("Error writing chunk to file locally");
            return false;
        }

        return true;
    }

    /**
     * Adds, if absent, new chunk to set of locally stored chunks
     * @param chunk
     * @return true if chunk did not exist in the set, false otherwise
     */
    public static synchronized boolean addChunk(Chunk chunk) {
        if (storedChunkFiles.add(chunk)) {
            incrementReplicationDegree(chunk);
            return true;
        }
        else return false;
    }

    /**
     * Gets
     * @param chunk
     * @return perceived replication degree of chunk
     */
    public static int getPerceivedReplicationDegree(Chunk chunk){
        for (Chunk key : chunkMap.keySet()){
            if (key.equals(chunk)) return key.getPerceivedReplicationDegree();
        }
        return -1;
    }

    /**
     * Either increments a chunks perceived replication degree or adds it to the map
     * @param chunk
     */
    public static void incrementReplicationDegree(Chunk chunk) {
        if (chunkMap.putIfAbsent(chunk, chunk.getFileID()) != null){
            for (Chunk key : chunkMap.keySet()){
                if (key.equals(chunk)) key.incrementPerceivedReplicationDegree();
            }
        }
    }

    /**
     * Called when a STORED message is received, updates the initiators knowledge of what peers are backing up its chunk
     * @param chunk
     * @param newBackingPeer
     */
    public static void updateChunksBackedPeers(Chunk chunk, int newBackingPeer){
        if (!isChunksInitiator(chunk)) return;

        var newPeerSet = ConcurrentHashMap.newKeySet();
        newPeerSet.add(newBackingPeer);

        var previousPeerSet = chunksBackedPeers.putIfAbsent(chunk, newPeerSet);
        if (previousPeerSet != null) previousPeerSet.add(newBackingPeer);
    }

    /**
     * Add file to set of initiator's backed up files
     * @param file
     */
    public static void initiateBackup(FileParser file) {
        initiatedFiles.add(file);
    }

    /**
     * If the file corresponding to the given ID was backed up by this peer, return the FileParser object
     * @param fileID
     * @return If found, the corresponding FileParser object, otherwise Optional.empty
     */
    public static Optional<FileParser> findInitiatedFile(String fileID){
        for (FileParser f : initiatedFiles) {
            if (f.getFileID().equals(fileID)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if a given chunk was backed up by this peer
     * @param chunk
     * @return true if chunk was initiated by this peer, otherwise false
     */
    public static boolean isChunksInitiator(Chunk chunk){
        for (FileParser file : initiatedFiles){
            if (file.getChunks().contains(chunk)) return true;
        }
        return false;
    }

    /**
     * Checks if a given file was backed up by this peer
     * @param file
     * @return true if file was initiated by this peer, otherwise false
     */
    public static boolean isFilesInitiator(FileParser file){
        return initiatedFiles.contains(file);
    }

    public static void removeInitiatedFile(FileParser fileParser) {
        initiatedFiles.remove(fileParser);
    }

    public static void removeChunkFromStoredChunkFiles(Chunk chunk) {
        storedChunkFiles.remove(chunk);
    }

    public static void removeEntryFromChunkMap(Chunk keyToRemove) {
        chunkMap.remove(keyToRemove);
    }

    public static Set<Chunk> getStoredChunkFiles() {
        return storedChunkFiles;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("BACKED UP FILES: ");

        if (initiatedFiles.size() > 0){
            for (FileParser file : initiatedFiles){
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
}