public class Chunk {

    private final int chunkNumber;
    private final byte[] content;
    private String fileID;
    private int desiredReplicationDegree;
    private int currReplicationDegree = 0;
    private final int size;

    public Chunk(int chunkNumber, byte[] content, int size) {
        this.chunkNumber = chunkNumber;
        this.content = content;
        this.size = size;
    }

    public Chunk(String[] splitHeader, byte[] body){
        this.chunkNumber = Integer.parseInt(splitHeader[4]);
        this.fileID = splitHeader[3];
        this.size = body.length;
        this.content = body;
    }


    public int getChunkNumber() {
        return chunkNumber;
    }

    public byte[] getContent() {
        return content;
    }

    public String getFileID() {
        return fileID;
    }

    public String getChunkFullName(){
        return fileID + "-" + chunkNumber; // fileId (hash)
    }

    public int getDesiredReplicationDegree() {
        return desiredReplicationDegree;
    }

    public void setDesiredReplicationDegree(int desiredReplicationDegree) {
        this.desiredReplicationDegree = desiredReplicationDegree;
    }

    public int getCurrReplicationDegree() {
        return currReplicationDegree;
    }

    public void setCurrReplicationDegree(int currReplicationDegree) {
        this.currReplicationDegree = currReplicationDegree;
    }

    public int getSize() {
        return size;
    }

    @Override
    public int hashCode() {
        return getChunkFullName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        Chunk other = (Chunk) obj;
        return getChunkFullName().equals(other.getChunkFullName());
    }
}
