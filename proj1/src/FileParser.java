import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class FileParser {

    private final int MAX_CHUNK_SIZE = 64000;

    private String id;
    private File file;
    private int replicationDegree;
    private ArrayList<Chunk> chunks;
    boolean hasExtraEmptyChunk;

    public FileParser(String filepath, int replicationDegree) {

        this.file = new File(filepath);
        this.replicationDegree = replicationDegree;
        this.chunks = getFileChunks();
        this.hasExtraEmptyChunk = checkForEmptyEndingChunk();
        System.out.println("pls");
        getFileIdHashed();

        System.out.println("End of Constructor");
    }

    private ArrayList<Chunk> getFileChunks() {
        byte[] chunkBuffer = new byte[MAX_CHUNK_SIZE];
        int currentChunkNumber = 0;

        ArrayList<Chunk> allChunks = new ArrayList<>();

        try {
            FileInputStream fis = new FileInputStream(this.file);

            int size;
            while( (size = fis.read(chunkBuffer)) > 0) {

                System.out.println(size);

                Chunk createdChunk = new Chunk(++currentChunkNumber, Arrays.copyOf(chunkBuffer, size), size);
                allChunks.add(createdChunk);

                chunkBuffer = new byte[MAX_CHUNK_SIZE];
            }

            if (this.hasExtraEmptyChunk) {
                Chunk chunk = new Chunk(++currentChunkNumber, null, 0);
                this.chunks.add(chunk);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return allChunks;
    }

    private void getFileIdHashed() {

        // using file name, date modified and owner as suggested in handout
        String idToHash = this.file.getName() + String.valueOf(this.file.lastModified()) + this.file.getParent();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(idToHash.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (byte aHash : hash) {
                String hex = Integer.toHexString(0xff & aHash);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            this.id = hexString.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkForEmptyEndingChunk() {
        return ((this.file.length() % MAX_CHUNK_SIZE) == 0);
    }

    public String getId() {
        return id;
    }

    public File getFile() {
        return file;
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public ArrayList<Chunk> getChunks() {
        return chunks;
    }


}
