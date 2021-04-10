public class Reclaim {


    public static void processPacketREMOVED(String[] splitHeader) {
        // FileId + ChunkNr

        String fileId = splitHeader[3];
        int chunkNr = Integer.parseInt(splitHeader[4]);

        // update own local count of the chunk
        Chunk chunkToSearch = new Chunk(fileId, chunkNr);
        FileStorage.instance.decrementReplicationDegree(chunkToSearch);

        // check if new count is lower than desired replication degree


        // if so, random delay (0-400ms) and then initiate Backup
        // Abort sending the backup msg if it receives a PUTCHUNK packet
        // while in the delay
    }
}
