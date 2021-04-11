import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Ensures desired replication degree is achieved for a chunk,
 * with up to 5 tries
 */
public class CheckReplicationDegree implements Callable {

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
    public Object call() throws Exception {
        System.out.println("Chunk nr. " + targetChunk.getChunkNumber() +  " Try: " + this.numberOfTries);

        int currentReplicationDegree = FileStorage.instance.getPerceivedReplicationDegree(targetChunk);

        if (currentReplicationDegree < targetChunk.getDesiredReplicationDegree()) {
            Peer.getMDB().sendMessage(tryAgainMessage);

            this.delay *= 2;
            if (++this.numberOfTries < 5) {
                Peer.futures.add(Peer.getExec().schedule(this, this.delay, TimeUnit.SECONDS));
                return true;
            }
            else System.out.println("Chunk nr. " + targetChunk.getChunkNumber() + " timed out");
            return false;
        }
        else System.out.println("Chunk nr. " + targetChunk.getChunkNumber() + " Passed!");
        return true;
    }
}
