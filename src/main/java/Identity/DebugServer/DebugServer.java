package Identity.DebugServer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class DebugServer implements DebugServerInterface {

    int port;
    int nextId = 0;
    DebugServerGUI gui;

    public static void main(String[] args) {
        DebugServer server = new DebugServer(5170);
    }

    public DebugServer(int port) {
        this.port = port;
        bind();
        gui = new DebugServerGUI();
    }
    @Override
    public int log(int serverId, String message) throws RemoteException {
        gui.logToConsole(serverId,message);
        return 1;
    }

    @Override
    public int connect() throws RemoteException {
        return nextId++;
    }

    @Override
    public int logClient(String message) throws RemoteException {
        gui.logClient(message);
        return 1;
    }
    /**
     * Binds the server to the registry
     */
    public void bind() {
        try {
            DebugServerInterface server = (DebugServerInterface) UnicastRemoteObject.exportObject(this, 0);

            Registry registry = LocateRegistry.createRegistry(this.port);

            registry.rebind("DebugServer", server);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception occurred: " + e);
        }
    }
}
