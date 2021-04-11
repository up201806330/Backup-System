import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;

public class Reclaim {

    private static FileStorage fileStorage;

    public static boolean checkIfNewMaxSpaceIsEnough(long newMaxUsedSpace) {

        Set<Chunk> chunks = FileStorage.instance.getStoredChunkFiles();

        long spaceCurrentlyUsed = 0;

        for (Chunk c : chunks) {
            spaceCurrentlyUsed += c.getContent().length;
        }

        return spaceCurrentlyUsed <= newMaxUsedSpace * 1000;
    }

    public static void deleteBackups(long maxUsedSpace, String generalREMOVEDMessage) {
        long maxUsedSpaceBytes = maxUsedSpace * 1000;

        Set<Chunk> chunksDeleted = new HashSet<>();

        // pick what chunks to delete so as to obey the new max used space
        // check if without the deleted chunk it meets the space requirements
        while (!checkIfNewMaxSpaceIsEnough(maxUsedSpaceBytes)) {
            int random = new Random().nextInt(fileStorage.getStoredChunkFiles().size());
            int i = 0;
            for (Chunk c : fileStorage.getStoredChunkFiles()) {
                if (i == random) {
                    chunksDeleted.add(c); // adding chunk object to deleted set
                    fileStorage.removeChunkFromStoredChunkFiles(c); // removing from fileStorage
                }
                i++;
            }
        }

        // for each chunk send message
        for (Chunk c : chunksDeleted) {

            fileStorage.removeBackedPeer(c, Peer.getId());

            String messageString = generalREMOVEDMessage + c.getFileID() + " " + c.getChunkNumber() + " " + "\r\n" + "\r\n";
            byte[] messageBytes = messageString.getBytes();

            Peer.getMC().sendMessage(messageBytes);
        }
    }


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
