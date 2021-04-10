import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

/**
 * Ran when peer is closed by exit() or SIGTERM
 */
public class ShutdownHandler implements Runnable{
    private final Registry registry;

    public ShutdownHandler(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void run() {
        try {
            registry.unbind(Peer.accessPoint);
            FileStorage.instance.saveToDisk();
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
        System.out.println("Shutting down...");
    }
}
