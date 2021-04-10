public class SendCHUNKMessage implements Runnable {

    private byte[] chunkMessage;
    private int chunkNumber;

    public SendCHUNKMessage(byte[] chunkMessage, int chunkNumber) {
        this.chunkMessage = chunkMessage;
        this.chunkNumber = chunkNumber;
    }

    @Override
    public void run() {

        if (Restore.chunksAlreadySent.contains(chunkNumber)) return;

        Peer.getMDR().sendMessage(chunkMessage);
    }
}
