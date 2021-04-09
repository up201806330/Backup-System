import java.util.concurrent.TimeUnit;

/**
 * Ensures desired replication degree is achieved for a chunk,
 * with up to 5 tries
 */
public class CheckReplicationDegree implements Runnable {

    private byte[] messageSent;
    private int replicationDegree;
    private int delay;
    private int chunkNumber;
    private int numberOfTries;
    private String fileId;

    public CheckReplicationDegree(byte[] fullMessage, String fileId, int number, int replicationDegree) {
        this.messageSent = fullMessage;
        this.chunkNumber = number;
        this.delay = 1;
        this.numberOfTries = 0;
        this.replicationDegree = replicationDegree;
        this.fileId = fileId;
    }

    @Override
    public void run() {
        System.out.println("Entering Check Rep Degree -> Chunk nr. " + chunkNumber);
        System.out.println("Try: " + this.numberOfTries);

        Chunk key = new Chunk(fileId, chunkNumber);
        int currentReplicationDegree = FileStorage.instance.getPerceivedReplicationDegree(key);

        if (currentReplicationDegree < replicationDegree) {
            System.out.println("More Reps");
            Peer.getMDB().sendMessage(messageSent);

            this.delay *= 2;
            if (++this.numberOfTries < 5) Peer.getExec().schedule(this, this.delay, TimeUnit.SECONDS);

        }
        else System.out.printf("Chunk nr. " + chunkNumber + " Passed!");
    }
}
