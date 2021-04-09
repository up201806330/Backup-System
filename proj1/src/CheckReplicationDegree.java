import java.util.concurrent.TimeUnit;

/**
 * Ensures desired replication degree is achieved for a chunk,
 * with up to 5 tries
 */
public class CheckReplicationDegree implements Runnable {

    private final byte[] tryAgainMessage;
    private final String fileId;
    private final int chunkNumber;
    private final int replicationDegree;

    private int delay;
    private int numberOfTries;

    public CheckReplicationDegree(byte[] tryAgainMessage, String fileId, int chunkNumber, int replicationDegree) {
        this.tryAgainMessage = tryAgainMessage;
        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.replicationDegree = replicationDegree;

        this.delay = 1;
        this.numberOfTries = 0;
    }

    @Override
    public void run() {
        System.out.println("Entering Check Rep Degree -> Chunk nr. " + chunkNumber);
        System.out.println("Try: " + this.numberOfTries);

        Chunk key = new Chunk(fileId, chunkNumber);
        int currentReplicationDegree = FileStorage.instance.getPerceivedReplicationDegree(key);

        if (currentReplicationDegree < replicationDegree) {
            System.out.println("More Reps");
            Peer.getMDB().sendMessage(tryAgainMessage);

            this.delay *= 2;
            if (++this.numberOfTries < 5) Peer.getExec().schedule(this, this.delay, TimeUnit.SECONDS);

        }
        else System.out.println("Chunk nr. " + chunkNumber + " Passed!");
    }
}
