import java.util.*;
import java.util.concurrent.TimeUnit;

public class Reclaim {
    private static final FileStorage fileStorage = FileStorage.instance;

    public static void deleteBackups(long maxUsedSpaceKB, String generalREMOVEDMessage) {
        var orderedChunks = new ArrayList<>(fileStorage.storedChunks);
        orderedChunks.sort(new ChunkOrder());

        // pick what chunks to delete so as to obey the new max used space
        // check if without the deleted chunk it meets the space requirements
        int i = 0;
        while (!checkIfNewMaxSpaceIsEnough(maxUsedSpaceKB)) {
            Chunk c = orderedChunks.get(i);
            fileStorage.removeChunkFromStoredChunkFiles(c);
            fileStorage.deleteFileViaName(c.getChunkID());
            byte[] messageBytes = (generalREMOVEDMessage + c.getFileID() + " " + c.getChunkNumber() + " " + "\r\n" + "\r\n").getBytes();

            System.out.println("Sending Message warning removal of chunk " + c.getChunkID());
            Peer.getMC().sendMessage(messageBytes);

            i++;
        }
    }

    public static void processREMOVED(String[] splitHeader) {
        if (Peer.getId() == Integer.parseInt(splitHeader[2])) {
            return ;
        }

        System.out.println("Processing REMOVED Packet");

        int peerID = Integer.parseInt(splitHeader[2]);
        String fileId = splitHeader[3];
        int chunkNr = Integer.parseInt(splitHeader[4]);

        // update own local count of the chunk
        Chunk chunkToSearch = new Chunk(fileId, chunkNr);
        fileStorage.decrementReplicationDegree(chunkToSearch);
        fileStorage.removeBackedPeerFromChunk(chunkToSearch, peerID);

        // check if new count is lower than desired replication degree
        var chunkInThisPeerOpt = fileStorage.findChunk(chunkToSearch);
        if (chunkInThisPeerOpt.isEmpty()){
            System.out.println("Something went wrong in chunk nr. " + chunkNr);
            return;
        }

        if (fileStorage.getPerceivedReplicationDegree(chunkToSearch) < chunkInThisPeerOpt.get().getDesiredReplicationDegree()) {
            int rand = new Random().nextInt(401);
            Peer.getExec().schedule(() -> Peer.initiatePUTCHUNK(fileId, chunkInThisPeerOpt.get(), 1), rand, TimeUnit.MILLISECONDS);
        }
    }

    public static boolean checkIfNewMaxSpaceIsEnough(long newMaxUsedSpaceKB) {
        fileStorage.setMaximumSpaceAvailable(newMaxUsedSpaceKB);
        return fileStorage.getCurrentlyKBytesUsedSpace() <= fileStorage.getMaximumSpaceAvailable();
    }
}
