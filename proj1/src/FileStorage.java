import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class FileStorage {
    private ArrayList<FileParser> files;
    private ArrayList<Chunk> storedChunks;
    private ArrayList<Chunk> receivedChunks;
    private ConcurrentHashMap<String, Integer> storedOccurrences;
    private int spaceAvailable;

    public FileStorage() {
        this.files = new ArrayList<>();
        this.storedChunks = new ArrayList<>();
        this.receivedChunks = new ArrayList<>();
        this.storedOccurrences = new ConcurrentHashMap<>();
        this.spaceAvailable = 1000000000;
    }

    public ArrayList<FileParser> getFiles() {
        return this.files;
    }

    public synchronized ArrayList<Chunk> getStoredChunks() {
        return this.storedChunks;
    }

    public ArrayList<Chunk> getReceivedChunks() {
        return this.receivedChunks;
    }

    public synchronized ConcurrentHashMap<String, Integer> getStoredOccurrences() {
        return this.storedOccurrences;
    }


    public void addFile(FileParser f) {
        this.files.add(f);
    }

    public synchronized boolean addStoredChunk(Chunk chunk) {

        for (Chunk storedChunk : this.storedChunks) {
            if (storedChunk.getFileID().equals(chunk.getFileID()) && storedChunk.getNr() == chunk.getNr())
                return false;
        }
        this.storedChunks.add(chunk);
        return true;
    }

    public void deleteStoredChunks(String fileID) {
        for (Iterator<Chunk> iter = this.storedChunks.iterator(); iter.hasNext(); ) {
            Chunk chunk = iter.next();
            if (chunk.getFileID().equals(fileID)) {
                String filename = Peer.getId() + "/" + fileID + "_" + chunk.getNr();
                File file = new File(filename);
                file.delete();
                removeStoredOccurrencesEntry(fileID, chunk.getNr());
                incSpaceAvailable(fileID, chunk.getNr());
                iter.remove();
            }
        }
    }

    public synchronized void incStoredOccurrences(String fileID, int chunkNr) {

        String key = fileID + '_' + chunkNr;

        if (!Peer.getStorage().getStoredOccurrences().containsKey(key)) {
            Peer.getStorage().getStoredOccurrences().put(key, 1);
        } else {
            int total = this.storedOccurrences.get(key) + 1;
            this.storedOccurrences.replace(key, total);
        }

    }

    public synchronized void decStoredOccurrences(String fileID, int chunkNr) {
        String key = fileID + '_' + chunkNr;
        int total = this.storedOccurrences.get(key) - 1;
        this.storedOccurrences.replace(key, total);
    }

    public synchronized void removeStoredOccurrencesEntry(String fileID, int chunkNr){
        String key = fileID + '_' + chunkNr;
        this.storedOccurrences.remove(key);
    }

    //for every stored chunk, gets his currents replication degree from the stored occurences table
    public void fillCurrRDChunks() {
        for (Chunk storedChunk : this.storedChunks) {
            String key = storedChunk.getFileID() + "_" + storedChunk.getNr();
            storedChunk.setCurrReplicationDegree(this.storedOccurrences.get(key));
        }
    }

    public synchronized int getSpaceAvailable() {
        return this.spaceAvailable;
    }

    public synchronized void setSpaceAvailable(int spaceAvailable) {
        this.spaceAvailable = spaceAvailable;
    }

    public synchronized void decSpaceAvailable(int chunkSize){
        spaceAvailable = spaceAvailable - chunkSize;
    }

    public synchronized void incSpaceAvailable(String fileId, int chunkNr){
        for (Chunk storedChunk : this.storedChunks) {
            if (storedChunk.getFileID().equals(fileId) && storedChunk.getNr() == chunkNr)
                this.spaceAvailable = this.spaceAvailable + storedChunk.getSize();
        }
    }

    public synchronized int getOccupiedSpace(){
        int occupiedSpace = 0;
        for (Chunk storedChunk : this.storedChunks) {
            occupiedSpace = occupiedSpace + storedChunk.getSize();
        }
        return occupiedSpace;
    }
}
