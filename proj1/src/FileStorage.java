import java.io.*;
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
    public String chunksDir;

    /**
     * Directory where restored files are stored
     */
    public String restoreDir;

    /**
     * Directory where received chunks are temporarily stored to restore files
     */
    public String cacheDir;

    /**
     * Concurrent set of all files whose backup was initiated by this peer
     */
    public final Set<FileObject> initiatedFiles = ConcurrentHashMap.newKeySet();

    /**
     * Concurrent map that for each chunk of each initiated file, has a concurrent set of peers that have that chunk backed up
     */
    public final ConcurrentHashMap<Chunk, ConcurrentHashMap.KeySetView<Object, Boolean>> chunksBackedPeers = new ConcurrentHashMap<Chunk, ConcurrentHashMap.KeySetView<Object, Boolean>>();

    /**
     * Concurrent set of all chunks stored locally by peer
     */
    public final Set<Chunk> storedChunkFiles = ConcurrentHashMap.newKeySet();

    /**
     * Singleton constructor
     * @throws IOException
     */
    public FileStorage() throws IOException {
        if (FileStorage.instance == null) FileStorage.instance = this;
        chunksDir = Peer.getServiceDirectory() + "/chunks";
        restoreDir = Peer.getServiceDirectory() + "/restored";
        cacheDir = Peer.getServiceDirectory() + "/cache";

        Files.createDirectories(Paths.get(chunksDir));
        Files.createDirectories(Paths.get(restoreDir));
        Files.createDirectories(Paths.get(cacheDir));
    }

    /**
     * Tries to store new chunk as a file locally,
     * @param chunk New chunk
     * @return True if chunk was stored successfully, false if it already existed / something went wrong
     */
    public boolean storeChunk(Chunk chunk) {
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
    public synchronized boolean addChunk(Chunk chunk) {
        boolean result = storedChunkFiles.add(chunk);
        incrementReplicationDegree(chunk);
        return result;
    }

    /**
     * Gets
     * @param chunk
     * @return perceived replication degree of chunk, be it a stored locally backed up chunk or a part of an initiated file
     */
    public int getPerceivedReplicationDegree(Chunk chunk){
        Optional<Optional<Object>> repDegreeIfItsInitiatedChunk =
        findInitiatedFile(chunk.getFileID()).
                map(fileParser -> fileParser.findChunk(chunk).map(Chunk::getPerceivedReplicationDegree));

        if (repDegreeIfItsInitiatedChunk.isPresent()){
            return (int)repDegreeIfItsInitiatedChunk.get().get();
        }

        for (Chunk key : storedChunkFiles){
            if (key.equals(chunk)) return key.getPerceivedReplicationDegree();
        }
        return -1;
    }

    /**
     * If the chunk is part of a file initiated by this peer, increments its perceived replication degree
     * Else tries to increment perceived replication degree of locally stored chunk. If its missing, add it to the set
     * @param chunk
     */
    public void incrementReplicationDegree(Chunk chunk) {
        if (findInitiatedFile(chunk.getFileID()).map(fileParser -> fileParser.incrementReplicationDegree(chunk)).isPresent())
            return;

        if (!storedChunkFiles.add(chunk)){
            for (Chunk key : storedChunkFiles){
                if (key.equals(chunk)) key.incrementPerceivedReplicationDegree();
            }
        }
    }

    /**
     * If the chunk is part of a file initiated by this peer, decrements its perceived replication degree
     * Else tries to decrement perceived replication degree of locally stored chunk.
     * @param chunk
     */
    public void decrementReplicationDegree(Chunk chunk) {
        if (findInitiatedFile(chunk.getFileID()).map(fileParser -> fileParser.decrementReplicationDegree(chunk)).isPresent())
            return;

        if (storedChunkFiles.contains(chunk)){
            for (Chunk key : storedChunkFiles){
                if (key.equals(chunk)) key.decrementPerceivedReplicationDegree();
            }
        }
    }

    /**
     * Called when a STORED message is received, updates the initiators knowledge of what peers are backing up its chunk
     * @param chunk
     * @param newBackingPeer
     */
    public void updateChunksBackedPeers(Chunk chunk, int newBackingPeer){
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
    public void initiateBackup(FileObject file) {
        initiatedFiles.add(file);
    }

    /**
     * If the file corresponding to the given ID was backed up by this peer, return the FileParser object
     * @param fileID
     * @return If found, the corresponding FileParser object, otherwise Optional.empty
     */
    public Optional<FileObject> findInitiatedFile(String fileID){
        for (FileObject f : initiatedFiles) {
            if (f.getFileID().equals(fileID)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if this peer is a given chunk's initiator
     * @param chunk
     * @return true if chunk was initiated by this peer, otherwise false
     */
    public boolean isChunksInitiator(Chunk chunk){
        for (FileObject file : initiatedFiles){
            if (file.getChunks().contains(chunk)) return true;
        }
        return false;
    }

    /**
     * Checks if this peer is a given file's initiator
     * @param file
     * @return true if file was initiated by this peer, otherwise false
     */
    public boolean isFilesInitiator(FileObject file){
        return initiatedFiles.contains(file);
    }

    /**
     * Checks if a given chunk is backed up by this peer
     * @param chunk
     * @return the stored chunk object if it exists, otherwise Optional.empty
     */
    public Optional<Chunk> hasChunkBackedUp(Chunk chunk){
        for(Chunk c : storedChunkFiles){
            if (c.equals(chunk)) return Optional.of(c);
        }
        return Optional.empty();
    }

    public void removeInitiatedFile(FileObject fileObject) {
        initiatedFiles.remove(fileObject);
    }

    public void removeChunkFromStoredChunkFiles(Chunk chunk) {
        storedChunkFiles.remove(chunk);
    }

    public static void saveToDisk(){
        try{
            FileOutputStream fs = new FileOutputStream(Peer.serviceDirectory + "/" + "State");
            ObjectOutputStream os = new ObjectOutputStream(fs);
            os.writeObject(instance);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static FileStorage loadFromDisk() throws IOException {
        try{
            FileInputStream fs = new FileInputStream(Peer.serviceDirectory + "/" + "State");
            ObjectInputStream os = new ObjectInputStream(fs);
            FileStorage.instance = (FileStorage) os.readObject();
            return FileStorage.instance;
        } catch (FileNotFoundException e){
            System.out.println("File Storage not found ; Creating new one");
            return new FileStorage();
        }
        catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
            return new FileStorage();
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("BACKED UP FILES: ");

        if (initiatedFiles.size() > 0){
            for (FileObject file : initiatedFiles){
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