import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;

public class Reclaim {

    private static FileStorage fileStorage;

    public static boolean checkIfNewMaxSpaceIsEnough(long newMaxUsedSpaceKB) {
        fileStorage = FileStorage.instance;
        if (fileStorage == null) System.out.println("--- FILE STORAGE IS NULL ?? ---");
        // updates maximum storage capacity (KBytes)
        fileStorage.setMaximumSpaceAvailable(newMaxUsedSpaceKB);
        System.out.println("CurrentlyKBytes Used: " + fileStorage.getCurrentlyKBytesUsedSpace());
        return fileStorage.getCurrentlyKBytesUsedSpace() <= fileStorage.getMaximumSpaceAvailable();
    }

    public static void deleteBackups(long maxUsedSpaceKB, String generalREMOVEDMessage) {
        System.out.println("Need to delete files!");
        Set<Chunk> chunksDeleted = new HashSet<>();

        // pick what chunks to delete so as to obey the new max used space
        // check if without the deleted chunk it meets the space requirements
        while (!checkIfNewMaxSpaceIsEnough(maxUsedSpaceKB)) {
            int random = new Random().nextInt(fileStorage.getStoredChunkFiles().size());
            int i = 0;
            for (Chunk c : fileStorage.getStoredChunkFiles()) {
                if (i == random) {
                    chunksDeleted.add(c); // adding chunk object to deleted set
                    System.out.println("ID to be deleted: " + c.getChunkID());
                    if (fileStorage == null) System.out.println("--- FILE STORAGE IS NULL ??? ---");
                    fileStorage.removeChunkFromStoredChunkFiles(c); // removing from fileStorage
                    Delete.deleteFileViaName(c.getChunkID());
                }
                i++;
            }
        }

        // for each chunk send message
        for (Chunk c : chunksDeleted) {

            String messageString = generalREMOVEDMessage + c.getFileID() + " " + c.getChunkNumber() + " " + "\r\n" + "\r\n";
            byte[] messageBytes = messageString.getBytes();

            System.out.println("Sending Message warning removal of chunk " + c.getChunkID());
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
