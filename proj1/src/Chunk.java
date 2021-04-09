public class Chunk {

    private final String fileID;
    private final int chunkNumber;
    private int perceivedReplicationDegree;
    private final byte[] content;
    private final int size;

    public Chunk(int chunkNumber, byte[] content, int size) {
        fileID = null;
        this.chunkNumber = chunkNumber;
        this.perceivedReplicationDegree = 1;
        this.content = content;
        this.size = size;
    }

    public Chunk(String[] splitHeader, byte[] body){
        this.fileID = splitHeader[3];
        this.chunkNumber = Integer.parseInt(splitHeader[4]);
        this.perceivedReplicationDegree = 1;
        this.content = body;
        this.size = body.length;
    }
    
    public Chunk(String fileID, int chunkNumber){
        this.fileID = fileID;
        this.chunkNumber = chunkNumber;
        this.perceivedReplicationDegree = 1;
        this.content = new byte[0];
        this.size = 0;
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

    public int getPerceivedReplicationDegree() {
        return perceivedReplicationDegree;
    }
    
    public void incrementPerceivedReplicationDegree(){
        perceivedReplicationDegree++;
    }

    public String getChunkID(){
        return fileID + "-" + chunkNumber; // fileId (hash)
    }

    @Override
    public String toString() {
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
