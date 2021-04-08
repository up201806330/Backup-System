import java.util.concurrent.TimeUnit;

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
        System.out.println("Entering Check Rep Degree");
        System.out.println("Try: " + this.numberOfTries);

        String key = this.fileId + "-" + this.chunkNumber;
        int currentReplicationDegree = FileStorage.instance.getChunkReplicationMap().get(key);

        if (currentReplicationDegree < replicationDegree) {
            System.out.println("More Reps");
            Peer.getMDB().sendMessage(messageSent);

            this.delay *= 2;
            if (++this.numberOfTries < 5) Peer.getExec().schedule(this, this.delay, TimeUnit.SECONDS);

        }
    }
}
