public class Chunk {

    private final String fileID;
    private final int chunkNumber;
    private final int desiredReplicationDegree;
    private int perceivedReplicationDegree;
    private byte[] content;

    public Chunk(String fileID, int chunkNumber, int desiredReplicationDegree, byte[] content){
        this.fileID = fileID;
        this.chunkNumber = chunkNumber;
        this.perceivedReplicationDegree = 1;
        this.content = content;
        this.desiredReplicationDegree = desiredReplicationDegree;
    }

    public Chunk(String fileID, int chunkNumber, int desiredReplicationDegree){
        this.fileID = fileID;
        this.chunkNumber = chunkNumber;
        this.perceivedReplicationDegree = 1;
        this.content = null;
        this.desiredReplicationDegree = desiredReplicationDegree;
    }

    public String getFileID() {
        return fileID;
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    public byte[] getContent() {
        return content;
    }

    public int getDesiredReplicationDegree() {
        return desiredReplicationDegree;
    }

    public int getPerceivedReplicationDegree() {
        return perceivedReplicationDegree;
    }

    public String getChunkID(){
        return fileID + "-" + chunkNumber; // fileId (hash)
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public void incrementPerceivedReplicationDegree(){
        perceivedReplicationDegree++;
    }

    /**
     * Shows full chunk info
     */
    @Override
    public String toString() {
        return  "ID                  : " + getChunkID() + "\n" +
                "Size (KB)           : " + (getContent().length > 1000 ? getContent().length/ 1000 : getContent().length) + "\n" +
                "Desired Rep Degree  : " + getDesiredReplicationDegree() + "\n" +
                "Perceived Rep Degree: " + getPerceivedReplicationDegree();
    }

    /**
     * Shows backed up chunk info
     */
    public String toSimpleString(){
        return "ID: " + getChunkID() + "\n" +
                "Perceived Rep Degree: " + getPerceivedReplicationDegree();
    }

    @Override
    public int hashCode() {
        return getChunkID().hashCode();
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
        return getChunkID().equals(other.getChunkID());
    }
}
