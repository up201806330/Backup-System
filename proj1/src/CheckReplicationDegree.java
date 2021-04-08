public class CheckReplicationDegree implements Runnable {

    private byte[] messageSent;
    private int replicationDegree;


    public CheckReplicationDegree(byte[] fullMessage, String id, int nr, int replicationDegree) {
        this.messageSent = fullMessage;
        // this.delay = 1;

        this.replicationDegree = replicationDegree;
    }

    @Override
    public void run() {

        // get from storage number of occurences
        int currentReplicationDegree = 1;

        if (currentReplicationDegree < replicationDegree) {
            Peer.getMDB().sendMessage(messageSent);

        }
    }
}
