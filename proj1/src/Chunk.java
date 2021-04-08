public class Chunk {

    private int nr;
    private byte[] content;
    private String fileID;
    private int desiredReplicationDegree;
    private int currReplicationDegree = 0;
    private int size;

    public Chunk(int nr, byte[] content, int size) {
        this.nr = nr;
        this.content = content;
        this.size = size;
    }

    public Chunk(int nr, byte[] content, String fileID, int size) {
        this.nr = nr;
        this.content = content;
        this.fileID = fileID;
        this.size = size;
    }

//
//    public Chunk(int nr, byte[] content, int desiredReplicationDegree, int size) {
//        this.nr = nr;
//        this.content = content;
//        this.desiredReplicationDegree = desiredReplicationDegree;
//        this.size = size;
//    }

    public int getNr() {
        return nr;
    }

    public byte[] getContent() {
        return content;
    }

    public String getFileID() {
        return fileID;
    }

    public String getChunkFileName(){
        return fileID + "-" + nr; // fileId (hash)
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
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        Chunk other = (Chunk) obj;
        return getChunkFileName().equals(other.getChunkFileName());
    }
}
