import java.io.Serializable;

public class Chunk implements Serializable {

    private final String fileID;
    private final int chunkNumber;
    private final int desiredReplicationDegree;
    private int perceivedReplicationDegree;
    private byte[] content;

    public Chunk(String fileID, int chunkNumber, int desiredReplicationDegree, byte[] content){
        this.fileID = fileID;
        this.chunkNumber = chunkNumber;
        this.desiredReplicationDegree = desiredReplicationDegree;
        this.perceivedReplicationDegree = 0;
        this.content = content;
    }

    public Chunk(String fileID, int chunkNumber, int desiredReplicationDegree){
        this.fileID = fileID;
        this.chunkNumber = chunkNumber;
        this.desiredReplicationDegree = desiredReplicationDegree;
        this.perceivedReplicationDegree = 0;
        this.content = null;
    }

    public Chunk(String fileID, int chunkNumber) {
        this.fileID = fileID;
        this.chunkNumber = chunkNumber;
        this.desiredReplicationDegree = -1;
        this.perceivedReplicationDegree = 0;
        this.content = null;
    }

    public Chunk(Chunk c){
        this.fileID = c.getFileID();
        this.chunkNumber = c.getChunkNumber();
        this.desiredReplicationDegree = c.getDesiredReplicationDegree();
        this.perceivedReplicationDegree = c.getPerceivedReplicationDegree();
        this.content = c.getContent();
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
        return fileID + "-" + chunkNumber;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public void setPerceivedReplicationDegree(int perceivedReplicationDegree){
        this.perceivedReplicationDegree = perceivedReplicationDegree;
    }

    public void incrementPerceivedReplicationDegree(){
        perceivedReplicationDegree++;
    }

    public void decrementPerceivedReplicationDegree() {
        perceivedReplicationDegree--;
    }

    /**
     * Shows full chunk info
     */
    @Override
    public String toString() {
        String sizeUnit = getContent().length > 1000 ? "(KB)" : "(B) ";
        return  "--------\n" +
                "\tID                  : " + getChunkID() + "\n" +
                "\tSize " + sizeUnit + "           : " + (getContent().length > 1000 ? getContent().length/ 1000 : getContent().length) + "\n" +
                "\tDesired Rep Degree  : " + getDesiredReplicationDegree() + "\n" +
                "\tPerceived Rep Degree: " + getPerceivedReplicationDegree() + "\n" +
                "--------\n";
    }

    /**
     * Shows info seen from initiator
     */
    public String toSimpleString(){
        return "--------\n" +
                "\tID: " + getChunkID() + "\n" +
                "\tPerceived Rep Degree: " + getPerceivedReplicationDegree() + "\n" +
                "--------\n";
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
