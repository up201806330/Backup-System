public class Chunk {

    public int CHUNK_MAX_SIZE = 64 * 1000; // 64KB = 64*1000 B

    private int fileId;
    private int number;
    private int replicDegree;
    private byte[] data;


    public Chunk(int fileId, int number, int replicDegree, byte[] data) {
        this.fileId = fileId;
        this.number = number;
        this.replicDegree = replicDegree;
        this.data = data;
    }


    public int getFileId() {
        return fileId;
    }

    public int getNumber() {
        return number;
    }

    public int getReplicDegree() {
        return replicDegree;
    }

    public byte[] getData() {
        return data;
    }
}
