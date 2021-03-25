import java.io.IOException;
import java.lang.invoke.MutableCallSite;
import java.net.MulticastSocket;

public class Control implements Runnable {

    private int port;

    @Override
    public void run() {


        try {

            MulticastSocket socket = new MulticastSocket(port);


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
