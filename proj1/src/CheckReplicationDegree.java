import java.util.concurrent.TimeUnit;

/**
 * Ensures desired replication degree is achieved for a chunk,
 * with up to 5 tries
 */
public class CheckReplicationDegree implements Runnable {

    private final byte[] tryAgainMessage;
    private final Chunk targetChunk;

    private int delay;
    private int numberOfTries;

    public CheckReplicationDegree(byte[] tryAgainMessage, Chunk targetChunk) {
        this.tryAgainMessage = tryAgainMessage;
        this.targetChunk = targetChunk;

        this.delay = 1;
        this.numberOfTries = 0;
    }

    @Override
    public void run() {
        System.out.println(targetChunk.getChunkNumber() +  " Try: " + this.numberOfTries);

        int currentReplicationDegree = FileStorage.instance.getPerceivedReplicationDegree(targetChunk);

        if (currentReplicationDegree < targetChunk.getDesiredReplicationDegree()) {
            Peer.getMDB().sendMessage(tryAgainMessage);

            this.delay *= 2;
            if (++this.numberOfTries < 5) Peer.getExec().schedule(this, this.delay, TimeUnit.SECONDS);
            else System.out.println("Chunk nr. " + targetChunk.getChunkNumber() + " timed out");
        }
        else System.out.println("Chunk nr. " + targetChunk.getChunkNumber() + " Passed!");
    }
}
