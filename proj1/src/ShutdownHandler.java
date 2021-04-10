import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

public class ShutdownHandler implements Runnable{
    private final Registry registry;

    public ShutdownHandler(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void run() {
        try {
            registry.unbind(Peer.accessPoint);
            Peer.saveFileStorageToDisk();
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
        System.out.println("Shutting down...");
    }
}
