import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Reclaim {


    public static void processPacketREMOVED(String[] splitHeader) {
        // FileId + ChunkNr

        FileStorage fileStorage = FileStorage.instance;

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
