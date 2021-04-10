import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Reclaim {
    private static FileStorage fileStorage;

    public static void processPacketREMOVED(String[] splitHeader) {
        fileStorage = FileStorage.instance;
        if ( Peer.getId() == Integer.parseInt(splitHeader[2]) ) {
            return ;
        }

        System.out.println("Processing REMOVED Packet");

        int peerId = Integer.parseInt(splitHeader[2]);
        String fileId = splitHeader[3];
        int chunkNr = Integer.parseInt(splitHeader[4]);

        // update own local count of the chunk
        Chunk chunkToSearch = new Chunk(fileId, chunkNr);
        fileStorage.decrementReplicationDegree(chunkToSearch);
        fileStorage.removeBackedPeer(chunkToSearch, peerId);

        // check if new count is lower than desired replication degree
        var chunkInThisPeerOpt = fileStorage.findChunk(chunkToSearch);
        if (chunkInThisPeerOpt.isEmpty()){
            System.out.println("Something went wrong in chunk nr. " + chunkNr);
            return;
        }

        if (fileStorage.getPerceivedReplicationDegree(chunkToSearch) < chunkInThisPeerOpt.get().getDesiredReplicationDegree()) {
            int rand = new Random().nextInt(401);
            Peer.getExec().schedule(() -> Peer.initiatePUTCHUNK(fileId, chunkInThisPeerOpt.get()), rand, TimeUnit.MILLISECONDS);
        }

        // if so, random delay (0-400ms) and then initiate Backup
        // Abort sending the backup msg if it receives a PUTCHUNK packet
        // while in the delay
    }
}
